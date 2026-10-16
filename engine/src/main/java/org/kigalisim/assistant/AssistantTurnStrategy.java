/**
 * Strategy encapsulating the assistant turn loop.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import org.kigalisim.assistant.callback.AssistantTurnCallback;
import org.kigalisim.assistant.model.AssistantModelQuery;
import org.kigalisim.assistant.tool.AssistantToolbelt;

/**
 * Encapsulates how the assistant runs a turn.
 *
 * <p>A turn is the period during which the assistant may take actions and send messages before
 * returning control to the user.</p>
 */
public interface AssistantTurnStrategy {

  /**
   * Runs a single turn.
   *
   * @param toolbelt The tools available to the assistant.
   * @param chatName The name of the chat to which the turn belongs.
   * @param message The new user message.
   * @param callback The callback which observes progress and completion.
   * @return The query for the first request so the user interface can render optimistically.
   */
  AssistantModelQuery runTurn(
      AssistantToolbelt toolbelt,
      String chatName,
      String message,
      AssistantTurnCallback callback);
}
