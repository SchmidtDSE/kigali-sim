/**
 * Callback for reporting the progress of a single assistant action.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.callback;

import org.kigalisim.assistant.result.AssistantResult;

/**
 * Callback through which tools and strategies report the outcome of an action.
 *
 * <p>An action is a single model query or a single tool call. This callback is intentionally
 * narrower than {@link AssistantTurnCallback} so that tools and model strategies cannot end a
 * turn even by accident; only the turn strategy is permitted to do so.</p>
 */
public interface AssistantActionCallback {

  /**
   * Reports the completion of an action.
   *
   * @param result The result of the action, carrying its own transport outcome.
   */
  void onActionComplete(AssistantResult result);
}
