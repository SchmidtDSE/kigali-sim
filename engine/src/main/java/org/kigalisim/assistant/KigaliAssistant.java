/**
 * Facade providing entry into the built-in AI assistant.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.kigalisim.assistant.callback.AssistantTurnCallback;
import org.kigalisim.assistant.log.ChatLog;
import org.kigalisim.assistant.log.ChatLogStrategy;
import org.kigalisim.assistant.model.AssistantModel;
import org.kigalisim.assistant.model.AssistantModelQuery;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.tool.AssistantToolbelt;
import org.kigalisim.assistant.workspace.AssistantWorkspace;

/**
 * Entry point into the built-in assistant.
 *
 * <p>This is a thin facade which encapsulates the complexity of the harness from the rest of the
 * codebase. It delegates storage to the {@link ChatLogStrategy} and the turn loop to the
 * {@link AssistantTurnStrategy}. It appends to the chat log internally as the turn progresses.</p>
 */
public final class KigaliAssistant {

  private final AssistantModel model;
  private final AssistantTurnStrategy turnStrategy;
  private final ChatLogStrategy chatLogStrategy;
  private final AssistantToolbelt toolbelt;
  private final AssistantWorkspace workspace;
  private final Set<String> inFlight = Collections.synchronizedSet(new HashSet<>());

  /**
   * Constructs an assistant from its collaborators.
   *
   * @param model The model used to communicate with the provider.
   * @param turnStrategy The turn strategy.
   * @param chatLogStrategy The chat log persistence strategy.
   * @param toolbelt The registered tools.
   * @param workspace The optional workspace.
   */
  KigaliAssistant(
      AssistantModel model,
      AssistantTurnStrategy turnStrategy,
      ChatLogStrategy chatLogStrategy,
      AssistantToolbelt toolbelt,
      AssistantWorkspace workspace) {
    this.model = model;
    this.turnStrategy = turnStrategy;
    this.chatLogStrategy = chatLogStrategy;
    this.toolbelt = toolbelt;
    this.workspace = workspace;
  }

  /**
   * Creates a chat.
   *
   * @param name The name of the chat to create.
   * @throws IllegalArgumentException If a chat by that name already exists.
   */
  public void createChat(String name) {
    chatLogStrategy.createChat(name);
  }

  /**
   * Reads the log for a chat.
   *
   * @param name The chat name.
   * @return The chat log.
   * @throws java.util.NoSuchElementException If the chat does not exist.
   */
  public ChatLog readChat(String name) {
    return chatLogStrategy.readChat(name);
  }

  /**
   * Sends a message and starts a turn.
   *
   * <p>The returned query lets the user interface render the user's message optimistically. If a
   * turn is already in flight for the requested chat the supplied callback receives a failed
   * terminal result immediately and null is returned; other chats are unaffected.</p>
   *
   * @param chatName The name of the chat to which the message belongs.
   * @param message The user message.
   * @param callback The callback which observes progress and completion.
   * @return The query for the first request, or null when the message was rejected.
   * @throws java.util.NoSuchElementException If the chat does not exist.
   */
  public AssistantModelQuery sendMessage(
      String chatName, String message, AssistantTurnCallback callback) {
    synchronized (inFlight) {
      if (inFlight.contains(chatName)) {
        callback.onTurnComplete(
            AssistantResult.forLlmQueryError(
                "A turn is already in flight for chat '" + chatName + "'"));
        return null;
      }
      inFlight.add(chatName);
    }

    boolean[] completed = {false};
    AssistantTurnCallback wrapped = new AssistantTurnCallback() {
      @Override
      public void onActionComplete(AssistantResult result) {
        callback.onActionComplete(result);
      }

      @Override
      public void onTurnComplete(AssistantResult result) {
        completed[0] = true;
        inFlight.remove(chatName);
        callback.onTurnComplete(result);
      }
    };

    try {
      return turnStrategy.runTurn(toolbelt, chatName, message, wrapped);
    } catch (RuntimeException e) {
      inFlight.remove(chatName);
      if (!completed[0]) {
        callback.onTurnComplete(AssistantResult.forLlmQueryError(describe(e)));
      }
      return null;
    }
  }

  /**
   * Deletes a chat.
   *
   * @param name The name of the chat to delete.
   * @throws java.util.NoSuchElementException If the chat does not exist.
   */
  public void deleteChat(String name) {
    chatLogStrategy.deleteChat(name);
  }

  /**
   * Gets the model used by the assistant.
   *
   * @return The model.
   */
  public AssistantModel getModel() {
    return model;
  }

  /**
   * Gets the optional workspace.
   *
   * @return The workspace or empty when none was configured.
   */
  public Optional<AssistantWorkspace> getWorkspace() {
    return Optional.ofNullable(workspace);
  }

  /**
   * Gets the toolbelt used by the assistant.
   *
   * @return The registered tools.
   */
  AssistantToolbelt getToolbelt() {
    return toolbelt;
  }

  /**
   * Produces a concise description of a throwable.
   *
   * @param throwable The throwable to describe.
   * @return The throwable message or class name when the message is absent.
   */
  private static String describe(Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null || message.isBlank()) {
      return throwable.getClass().getName();
    } else {
      return message;
    }
  }
}
