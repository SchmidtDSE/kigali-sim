/**
 * Tests for the AssistantResult class and its builder.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonObject;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.model.AssistantModel;
import org.kigalisim.assistant.model.AssistantModelQuery;
import org.kigalisim.assistant.model.AssistantModelResponse;
import org.kigalisim.assistant.model.AssistantModelResponseFactory;
import org.kigalisim.assistant.model.OpenAiModelProtocolStrategy;
import org.kigalisim.assistant.tool.AssistantToolInvocation;

/**
 * Tests for the AssistantResult class and its builder.
 */
public class AssistantResultTest {

  /**
   * Builds a model for query construction.
   *
   * @return The model.
   */
  private AssistantModel buildModel() {
    return new AssistantModel(
        (body, callback) -> () -> {},
        new OpenAiModelProtocolStrategy("test"),
        "test",
        "test-service"
    );
  }

  /**
   * Test a successful query result.
   */
  @Test
  public void testForLlmQuery() {
    AssistantModelResponse response = AssistantModelResponseFactory.forRawBody("{}");
    AssistantResult result = AssistantResultBuilder.forLlmQuery(response).build();

    assertEquals(AssistantActionType.LLM_QUERY, result.getActionType());
    assertTrue(result.getWasSuccessful());
    assertEquals(response, result.getRawResponse().orElseThrow());
    assertTrue(result.getMessage().isEmpty());
    assertTrue(result.getToolInvocation().isEmpty());
  }

  /**
   * Test a failed query result.
   */
  @Test
  public void testForLlmQueryError() {
    AssistantResult result = AssistantResultBuilder.forLlmQueryError("boom").build();

    assertEquals(AssistantActionType.LLM_QUERY, result.getActionType());
    assertFalse(result.getWasSuccessful());
    assertEquals("boom", result.getError().orElseThrow());
    assertTrue(result.getRawResponse().isEmpty());
  }

  /**
   * Test a successful tool call result.
   */
  @Test
  public void testForToolCall() {
    AssistantResult result = AssistantResultBuilder.forToolCall("output").build();

    assertEquals(AssistantActionType.TOOL_CALL, result.getActionType());
    assertTrue(result.getWasSuccessful());
    assertEquals("output", result.getRawResponse().orElseThrow().getContent().orElseThrow());
  }

  /**
   * Test a failed tool call result.
   */
  @Test
  public void testForToolCallError() {
    AssistantResult result = AssistantResultBuilder.forToolCallError("bad args").build();
    assertFalse(result.getWasSuccessful());
    assertEquals("bad args", result.getError().orElseThrow());
  }

  /**
   * Test the maximum action exceeded result.
   */
  @Test
  public void testForTurnActionsExceeded() {
    AssistantResult result = AssistantResultBuilder.forTurnActionsExceeded("too many").build();
    assertEquals(AssistantActionType.TURN_ACTIONS_EXCEEDED, result.getActionType());
    assertFalse(result.getWasSuccessful());
    assertEquals("too many", result.getError().orElseThrow());
  }

  /**
   * Test that attaching a message copies rather than mutates.
   */
  @Test
  public void testWithMessageIsCopyOnWrite() {
    AssistantResult original = AssistantResultBuilder.forLlmQuery(
        AssistantModelResponseFactory.forRawBody("{}")).build();
    AssistantModelQuery query =
        new AssistantModelQuery("chat", "context", "user", buildModel());
    AssistantResult updated = new AssistantResultBuilder(original).withMessage(query).build();

    assertTrue(original.getMessage().isEmpty());
    assertEquals(query, updated.getMessage().orElseThrow());
    assertEquals(original.getActionType(), updated.getActionType());
    assertEquals(
        original.getRawResponse().orElseThrow(), updated.getRawResponse().orElseThrow());
  }

  /**
   * Test that attaching a tool invocation copies rather than mutates.
   */
  @Test
  public void testWithToolInvocationIsCopyOnWrite() {
    AssistantResult original = AssistantResultBuilder.forToolCall("output").build();
    AssistantToolInvocation invocation = new AssistantToolInvocation(
        "echo",
        "call_1",
        new JsonObject()
    );
    AssistantResult updated = new AssistantResultBuilder(original)
        .withToolInvocation(invocation)
        .build();

    assertTrue(original.getToolInvocation().isEmpty());
    assertEquals(invocation, updated.getToolInvocation().orElseThrow());
    assertEquals("output", updated.getRawResponse().orElseThrow().getContent().orElseThrow());
  }
}
