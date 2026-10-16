/**
 * Callback for reporting the progress and completion of an assistant turn.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.callback;

import org.kigalisim.assistant.result.AssistantResult;

/**
 * Callback supplied by callers of the assistant to observe a turn.
 *
 * <p>Callbacks are used instead of a future because a callback works on both the JVM and
 * WASM / TeaVM, where the completion may arrive on the event loop. Only the turn strategy holds
 * this interface: it passes a narrowed {@link AssistantActionCallback} down to strategies and
 * tools.</p>
 */
public interface AssistantTurnCallback extends AssistantActionCallback {

  /**
   * Reports the completion of a turn.
   *
   * <p>This fires exactly once per turn, after the final action callback, to carry the
   * terminal result. It fires even when the turn fails.</p>
   *
   * @param result The terminal result for the turn.
   */
  void onTurnComplete(AssistantResult result);
}
