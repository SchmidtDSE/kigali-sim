/**
 * Factory for assistant action results.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.result;

import java.util.Optional;
import org.kigalisim.assistant.model.AssistantModelResponse;
import org.kigalisim.assistant.model.AssistantModelResponseFactory;

/**
 * Creates results describing the outcome of an assistant action.
 *
 * <p>Each result records the action type, the transport outcome, and the model query or tool
 * invocation associated with the action when one applies.</p>
 */
public class AssistantResultFactory {

  /**
   * Creates a result describing a successful model query.
   *
   * @param response The parsed response.
   * @return A successful query result.
   */
  public static AssistantResult forLlmQuery(AssistantModelResponse response) {
    return new AssistantResult(
        AssistantActionType.LLM_QUERY,
        Optional.empty(),
        Optional.empty(),
        Optional.of(response),
        Optional.empty()
    );
  }

  /**
   * Creates a result describing a failed model query.
   *
   * @param error The details of the failure.
   * @return A failed query result.
   */
  public static AssistantResult forLlmQueryError(String error) {
    return new AssistantResult(
        AssistantActionType.LLM_QUERY,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(error)
    );
  }

  /**
   * Creates a result describing a successful tool call.
   *
   * @param output The textual output of the tool.
   * @return A successful tool result.
   */
  public static AssistantResult forToolCall(String output) {
    return new AssistantResult(
        AssistantActionType.TOOL_CALL,
        Optional.empty(),
        Optional.empty(),
        Optional.of(AssistantModelResponseFactory.forToolOutput(output)),
        Optional.empty()
    );
  }

  /**
   * Creates a result describing a failed tool call.
   *
   * @param error The details of the failure.
   * @return A failed tool result.
   */
  public static AssistantResult forToolCallError(String error) {
    return new AssistantResult(
        AssistantActionType.TOOL_CALL,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(error)
    );
  }

  /**
   * Creates a terminal result for a turn ended by the maximum action guard.
   *
   * @param error The details of the guard violation.
   * @return An unsuccessful turn-level result.
   */
  public static AssistantResult forTurnActionsExceeded(String error) {
    return new AssistantResult(
        AssistantActionType.TURN_ACTIONS_EXCEEDED,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(error)
    );
  }
}
