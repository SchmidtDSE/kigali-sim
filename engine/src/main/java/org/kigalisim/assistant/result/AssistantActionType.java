/**
 * Types of actions which may be reported during an assistant turn.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.result;

/**
 * The kind of action represented by an {@code AssistantResult}.
 */
public enum AssistantActionType {

  /**
   * A query sent to the language model.
   */
  LLM_QUERY,

  /**
   * A tool call executed on behalf of the language model.
   */
  TOOL_CALL,

  /**
   * The turn was ended by the maximum action guard rather than by {@code end_turn}.
   *
   * <p>This is the only turn-level member of this enum. Its result has an error populated and is
   * therefore unsuccessful.</p>
   */
  TURN_ACTIONS_EXCEEDED
}
