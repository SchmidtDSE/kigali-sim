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
 * <p>Instances are immutable. The {@code getWithMessage} and {@code getWithToolInvocation} methods
 * perform copy-on-write updates.</p>
 */
public class AssistantResult {

  private final AssistantActionType actionType;
  private final Optional<AssistantModelQuery> message;
  private final Optional<AssistantToolInvocation> toolInvocation;
  private final Optional<AssistantModelResponse> rawResponse;
  private final Optional<String> error;

  AssistantResult(
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
   * Creates a copy with the model query attached.
   *
   * @param query The query responsible for this result.
   * @return A copy carrying the query.
   */
  public AssistantResult getWithMessage(AssistantModelQuery query) {
    return new AssistantResult(actionType, Optional.of(query), toolInvocation, rawResponse, error);
  }

  /**
   * Creates a copy with the tool invocation attached.
   *
   * @param invocation The invocation responsible for this result.
   * @return A copy carrying the invocation.
   */
  public AssistantResult getWithToolInvocation(AssistantToolInvocation invocation) {
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
