/**
 * Mutable state for a single assistant turn.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.List;
import org.kigalisim.assistant.callback.AssistantTurnCallback;
import org.kigalisim.assistant.tool.AssistantToolbelt;

/**
 * Mutable state for a single assistant turn.
 *
 * <p>Tracks the collaborators captured at the start of a turn along with the running action count
 * and completion flag. Access is through getters so that the turn strategy does not reach into the
 * fields directly.</p>
 */
class TurnState {

  private final AssistantToolbelt toolbelt;
  private final String chatName;
  private final String userMessage;
  private final AssistantTurnCallback callback;
  private final List<JsonObject> history;
  private int actions;
  private boolean completed;

  /**
   * Constructs turn state.
   *
   * @param toolbelt The tools available to the assistant.
   * @param chatName The chat name.
   * @param userMessage The user message which started the turn.
   * @param callback The callback which observes the turn.
   * @param history The in-memory message history.
   */
  TurnState(
      AssistantToolbelt toolbelt,
      String chatName,
      String userMessage,
      AssistantTurnCallback callback,
      List<JsonObject> history) {
    this.toolbelt = toolbelt;
    this.chatName = chatName;
    this.userMessage = userMessage;
    this.callback = callback;
    this.history = history;
  }

  /**
   * Gets the tools available to the assistant.
   *
   * @return The tools available to the assistant.
   */
  AssistantToolbelt getToolbelt() {
    return toolbelt;
  }

  /**
   * Gets the chat name.
   *
   * @return The chat name.
   */
  String getChatName() {
    return chatName;
  }

  /**
   * Gets the user message which started the turn.
   *
   * @return The user message which started the turn.
   */
  String getUserMessage() {
    return userMessage;
  }

  /**
   * Gets the callback which observes the turn.
   *
   * @return The callback which observes the turn.
   */
  AssistantTurnCallback getCallback() {
    return callback;
  }

  /**
   * Gets the in-memory message history.
   *
   * @return The in-memory message history.
   */
  List<JsonObject> getHistory() {
    return history;
  }

  /**
   * Gets the number of actions taken so far in this turn.
   *
   * @return The number of actions taken so far.
   */
  int getActions() {
    return actions;
  }

  /**
   * Records that one more action has been taken.
   */
  void incrementActions() {
    actions++;
  }

  /**
   * Gets whether the turn has completed.
   *
   * @return True when the turn has completed.
   */
  boolean getCompleted() {
    return completed;
  }

  /**
   * Marks the turn as completed.
   */
  void markCompleted() {
    completed = true;
  }
}
