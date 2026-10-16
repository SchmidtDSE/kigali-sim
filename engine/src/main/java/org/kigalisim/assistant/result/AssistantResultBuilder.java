/**
 * Builder for assistant action results.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.result;

import java.util.Optional;
import org.kigalisim.assistant.model.AssistantModelQuery;
import org.kigalisim.assistant.model.AssistantModelResponse;
import org.kigalisim.assistant.model.AssistantModelResponseFactory;
import org.kigalisim.assistant.tool.AssistantToolInvocation;

/**
 * Builds results describing the outcome of an assistant action.
 *
 * <p>Each result records the action type, the transport outcome, and the model query or tool
 * invocation associated with the action when one applies. Start from one of the {@code for*}\n * statics describing a fresh outcome, or from the constructor copying an existing result, then\n * attach the query or tool invocation before calling {@link #build()}.</p>
 */
public class AssistantResultBuilder {

  private AssistantActionType actionType;
  private Optional<AssistantModelQuery> message;
  private Optional<AssistantToolInvocation> toolInvocation;
  private Optional<AssistantModelResponse> rawResponse;
  private Optional<String> error;

  /**
   * Starts a builder which copies an existing result.
   *
   * <p>Use this to attach a model query or tool invocation to a result which has already been
   * produced by a lower layer.</p>
   *
   * @param base The result to copy.
   */
  public AssistantResultBuilder(AssistantResult base) {
    this(
        base.getActionType(),
        base.getMessage(),
        base.getToolInvocation(),
        base.getRawResponse(),
        base.getError());
  }

  /**
   * Constructs a builder over explicit field values.
   *
   * @param actionType The type of action taken.
   * @param message The query sent to the model, or empty if not an LLM query.
   * @param toolInvocation The tool invocation, or empty if not a tool call.
   * @param rawResponse The parsed response or tool output, or empty on error.
   * @param error The error details, or empty when the action succeeded.
   */
  private AssistantResultBuilder(
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
   * Creates a builder describing a successful model query.
   *
   * @param response The parsed response.
   * @return A builder for a successful query result.
   */
  public static AssistantResultBuilder forLlmQuery(AssistantModelResponse response) {
    return new AssistantResultBuilder(
        AssistantActionType.LLM_QUERY,
        Optional.empty(),
        Optional.empty(),
        Optional.of(response),
        Optional.empty()
    );
  }

  /**
   * Creates a builder describing a failed model query.
   *
   * @param error The details of the failure.
   * @return A builder for a failed query result.
   */
  public static AssistantResultBuilder forLlmQueryError(String error) {
    return new AssistantResultBuilder(
        AssistantActionType.LLM_QUERY,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(error)
    );
  }

  /**
   * Creates a builder describing a successful tool call.
   *
   * @param output The textual output of the tool.
   * @return A builder for a successful tool result.
   */
  public static AssistantResultBuilder forToolCall(String output) {
    return new AssistantResultBuilder(
        AssistantActionType.TOOL_CALL,
        Optional.empty(),
        Optional.empty(),
        Optional.of(AssistantModelResponseFactory.forToolOutput(output)),
        Optional.empty()
    );
  }

  /**
   * Creates a builder describing a failed tool call.
   *
   * @param error The details of the failure.
   * @return A builder for a failed tool result.
   */
  public static AssistantResultBuilder forToolCallError(String error) {
    return new AssistantResultBuilder(
        AssistantActionType.TOOL_CALL,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(error)
    );
  }

  /**
   * Creates a builder for a turn ended by the maximum action guard.
   *
   * @param error The details of the guard violation.
   * @return A builder for an unsuccessful turn-level result.
   */
  public static AssistantResultBuilder forTurnActionsExceeded(String error) {
    return new AssistantResultBuilder(
        AssistantActionType.TURN_ACTIONS_EXCEEDED,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(error)
    );
  }

  /**
   * Attaches the model query responsible for this result.
   *
   * @param query The query responsible for this result.
   * @return This builder for chaining.
   */
  public AssistantResultBuilder withMessage(AssistantModelQuery query) {
    this.message = Optional.of(query);
    return this;
  }

  /**
   * Attaches the tool invocation responsible for this result.
   *
   * @param invocation The invocation responsible for this result.
   * @return This builder for chaining.
   */
  public AssistantResultBuilder withToolInvocation(AssistantToolInvocation invocation) {
    this.toolInvocation = Optional.of(invocation);
    return this;
  }

  /**
   * Builds the result.
   *
   * @return The result carrying the attached query or tool invocation when one was provided.
   */
  public AssistantResult build() {
    return new AssistantResult(actionType, message, toolInvocation, rawResponse, error);
  }
}