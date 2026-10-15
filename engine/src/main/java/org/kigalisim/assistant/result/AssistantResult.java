/**
 * Data transfer object describing the outcome of an assistant action.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.result;

import java.util.Optional;
import org.kigalisim.assistant.model.AssistantModelQuery;
import org.kigalisim.assistant.model.AssistantModelResponse;
import org.kigalisim.assistant.tool.AssistantToolInvocation;

/**
 * The result of an action taken during an assistant turn.
 *
 * <p>Each layer fills only the fields it owns. The wire and tool strategies populate the transport
 * outcome while the turn strategy attaches the query and tool invocation as it forwards a result
 * because it holds the query and knows which tool it dispatched.</p>
 *
 * <p>Instances are immutable. The {@code withMessage} and {@code withToolInvocation} methods
 * perform copy-on-write updates.</p>
 */
public final class AssistantResult {

  private final AssistantActionType actionType;
  private final Optional<AssistantModelQuery> message;
  private final Optional<AssistantToolInvocation> toolInvocation;
  private final Optional<AssistantModelResponse> rawResponse;
  private final Optional<String> error;

  private AssistantResult(
      AssistantActionType actionType,
      Optional<AssistantModelQuery> message,
      Optional<AssistantToolInvocation> toolInvocation,
      Optional<AssistantModelResponse> rawResponse,
      Optional<String> error) {
    this.actionType = actionType;
    this.message = message;
    this.toolInvocation = toolInvocation;
    this.rawResponse = rawResponse;
    this.error = error;
  }

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
        Optional.empty());
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
        Optional.of(error));
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
        Optional.of(AssistantModelResponse.forToolOutput(output)),
        Optional.empty());
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
        Optional.of(error));
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
        Optional.of(error));
  }

  /**
   * Creates a copy with the model query attached.
   *
   * @param query The query responsible for this result.
   * @return A copy carrying the query.
   */
  public AssistantResult withMessage(AssistantModelQuery query) {
    return new AssistantResult(actionType, Optional.of(query), toolInvocation, rawResponse, error);
  }

  /**
   * Creates a copy with the tool invocation attached.
   *
   * @param invocation The invocation responsible for this result.
   * @return A copy carrying the invocation.
   */
  public AssistantResult withToolInvocation(AssistantToolInvocation invocation) {
    return new AssistantResult(actionType, message, Optional.of(invocation), rawResponse, error);
  }

  /**
   * Gets the type of action taken.
   *
   * @return The action type.
   */
  public AssistantActionType getActionType() {
    return actionType;
  }

  /**
   * Gets the query sent to the model.
   *
   * @return The query or empty when this is not a model query.
   */
  public Optional<AssistantModelQuery> getMessage() {
    return message;
  }

  /**
   * Gets the tool invocation.
   *
   * @return The invocation or empty when this is not a tool call.
   */
  public Optional<AssistantToolInvocation> getToolInvocation() {
    return toolInvocation;
  }

  /**
   * Gets the parsed response body for a query or the tool output for a tool call.
   *
   * @return The response or empty when there was an error.
   */
  public Optional<AssistantModelResponse> getRawResponse() {
    return rawResponse;
  }

  /**
   * Gets details of the error, if any.
   *
   * @return The error message or empty when the action succeeded.
   */
  public Optional<String> getError() {
    return error;
  }

  /**
   * Indicates whether the action succeeded.
   *
   * @return True when no error is present.
   */
  public boolean getWasSuccessful() {
    return error.isEmpty();
  }
}
