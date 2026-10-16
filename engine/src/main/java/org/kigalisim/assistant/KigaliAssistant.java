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
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.tool.AssistantToolbelt;
import org.kigalisim.assistant.workspace.AssistantWorkspace;

/**
 * Entry point into the built-in assistant.
 *
 * <p>This is a thin facade which encapsulates the complexity of the harness from the rest of the
 * codebase. It delegates storage to the {@link ChatLogStrategy} and the turn loop to the
 * {@link AssistantTurnStrategy}. It appends to the chat log internally as the turn progresses.</p>
 */
public class KigaliAssistant {

  private final AssistantModel model;
  private final AssistantTurnStrategy turnStrategy;
  private final ChatLogStrategy chatLogStrategy;
  private final AssistantToolbelt toolbelt;
  private final AssistantWorkspace workspace;
  private final Set<String> inFlight;

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
    this.inFlight = Collections.synchronizedSet(new HashSet<>());
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
   * <p>The returned query lets the user interface render the user's message optimistically. The
   * result is empty when the message was rejected because a turn is already in flight for the
   * requested chat, or when the turn strategy threw while starting the turn. In both of those
   * cases the supplied callback has already received a failed terminal result before the empty
   * value is returned, and other chats are unaffected.</p>
   *
   * <p>An empty result is deliberate rather than an exception: the callback is the authoritative
   * outcome channel — it behaves identically on the JVM and on the WASM / TeaVM event loop — and
   * it has already reported the rejection, so throwing as well would double-report the same
   * outcome on two channels. The return value exists only for optimistic rendering, so an empty
   * value means there is no query to render, not that something failed. Compare with
   * {@link #createChat(String)} and {@link #readChat(String)}, which throw because they are
   * synchronous and have no callback channel.</p>
   *
   * @param chatName The name of the chat to which the message belongs.
   * @param message The user message.
   * @param callback The callback which observes progress and completion.
   * @return The query for the first request, or empty when the message was rejected.
   * @throws java.util.NoSuchElementException If the chat does not exist.
   */
  public Optional<AssistantModelQuery> sendMessage(
      String chatName,
      String message,
      AssistantTurnCallback callback) {
    synchronized (inFlight) {
      if (inFlight.contains(chatName)) {
        AssistantResult rejection = AssistantResultBuilder.forLlmQueryError(
            "A turn is already in flight for chat '" + chatName + "'").build();
        callback.onTurnComplete(rejection);
        return Optional.empty();
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
      AssistantModelQuery query = turnStrategy.runTurn(toolbelt, chatName, message, wrapped);
      return Optional.ofNullable(query);
    } catch (RuntimeException e) {
      inFlight.remove(chatName);
      if (!completed[0]) {
        callback.onTurnComplete(AssistantResultBuilder.forLlmQueryError(describe(e)).build());
      }
      return Optional.empty();
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
   * Describes a throwable, falling back to its class name when there is no message.
   *
   * @param throwable The throwable to describe.
   * @return The throwable message, or its class name as a fallback.
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
