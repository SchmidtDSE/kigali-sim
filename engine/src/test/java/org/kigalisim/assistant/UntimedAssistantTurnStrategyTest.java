/**
 * Unit tests for the UntimedAssistantTurnStrategy class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.callback.AssistantTurnCallback;
import org.kigalisim.assistant.log.InMemoryLogStrategy;
import org.kigalisim.assistant.model.AssistantModel;
import org.kigalisim.assistant.model.MockModelWireStrategy;
import org.kigalisim.assistant.model.OpenAiModelProtocolStrategy;
import org.kigalisim.assistant.result.AssistantActionType;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.tool.AssistantToolInvocation;
import org.kigalisim.assistant.tool.EndTurnTool;
import org.kigalisim.assistant.tool.MessageUserTool;
import org.kigalisim.assistant.tool.MockToolStrategy;
import org.kigalisim.assistant.tool.MutableAssistantToolbelt;

/**
 * Tests for the UntimedAssistantTurnStrategy class.
 */
public class UntimedAssistantTurnStrategyTest {

  /**
   * A callback which records every result it receives.
   */
  private static final class RecordingCallback implements AssistantTurnCallback {

    private final List<AssistantResult> actions = new ArrayList<>();
    private final List<AssistantResult> completions = new ArrayList<>();

    @Override
    public void onActionComplete(AssistantResult result) {
      actions.add(result);
    }

    @Override
    public void onTurnComplete(AssistantResult result) {
      completions.add(result);
    }
  }

  /**
   * A test harness wiring the real model and turn strategy to a mock wire.
   */
  private static final class Harness {

    private final MockModelWireStrategy wire = new MockModelWireStrategy();
    private final InMemoryLogStrategy log = new InMemoryLogStrategy();
    private final AssistantModel model;
    private final UntimedAssistantTurnStrategy strategy;
    private final MutableAssistantToolbelt toolbelt = new MutableAssistantToolbelt();
    private final RecordingCallback callback = new RecordingCallback();

    private Harness(int maxActions) {
      model = new AssistantModel(
          wire,
          new OpenAiModelProtocolStrategy("test-model"),
          "test-model",
          "test-service");
      strategy =
          new UntimedAssistantTurnStrategy(model, log, new SystemContextProvider(), maxActions);
      toolbelt.addTool(new EndTurnTool());
      toolbelt.addTool(new MessageUserTool());
      log.createChat("chat");
    }
  }

  /**
   * Builds a provider response envelope.
   *
   * @param message The assistant message.
   * @param finishReason The finish reason.
   * @return The serialized response.
   */
  private String buildResponse(JsonObject message, String finishReason) {
    JsonObject choice = new JsonObject();
    choice.put("index", 0);
    choice.put("message", message);
    choice.put("finish_reason", finishReason);
    JsonArray choices = new JsonArray();
    choices.add(choice);
    JsonObject root = new JsonObject();
    root.put("id", "chatcmpl-test");
    root.put("object", "chat.completion");
    root.put("model", "test-model");
    root.put("choices", choices);
    return Jsoner.serialize(root);
  }

  /**
   * Builds an assistant message carrying the given tool calls.
   *
   * @param toolCalls The tool calls, or null for a plain text message.
   * @param content The content, or null.
   * @return The assistant message.
   */
  private JsonObject assistantMessage(JsonArray toolCalls, String content) {
    JsonObject message = new JsonObject();
    message.put("role", "assistant");
    message.put("content", content);
    if (toolCalls != null) {
      message.put("tool_calls", toolCalls);
    }
    return message;
  }

  /**
   * Builds a tool call object.
   *
   * @param id The call identifier.
   * @param name The function name.
   * @param argumentsJson The serialized arguments.
   * @return The tool call.
   */
  private JsonObject toolCall(String id, String name, String argumentsJson) {
    JsonObject function = new JsonObject();
    function.put("name", name);
    function.put("arguments", argumentsJson);
    JsonObject call = new JsonObject();
    call.put("id", id);
    call.put("type", "function");
    call.put("function", function);
    return call;
  }

  /**
   * Reads the stored transcript for a chat.
   *
   * @param logStrategy The log strategy.
   * @param chatName The chat name.
   * @return The transcript array.
   */
  private JsonArray transcript(InMemoryLogStrategy logStrategy, String chatName) {
    try {
      return (JsonArray) Jsoner.deserialize(logStrategy.readChat(chatName).getWireTranscript());
    } catch (JsonException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Test that end_turn completes exactly once after the whole batch is appended.
   */
  @Test
  public void testEndTurnCompletesOnceAfterBatch() {
    Harness harness = new Harness(25);
    JsonArray calls = new JsonArray();
    calls.add(toolCall("call_1", "message_user", "{\"message\":\"Hello\"}"));
    calls.add(toolCall("call_2", "end_turn", "{}"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(calls, null), "tool_calls"));

    harness.strategy.runTurn(harness.toolbelt, "chat", "hi", harness.callback);

    assertEquals(1, harness.callback.completions.size(), "Turn should complete exactly once");
    assertTrue(harness.callback.completions.get(0).getWasSuccessful());

    List<AssistantResult> actions = harness.callback.actions;
    assertEquals(3, actions.size());
    assertEquals(AssistantActionType.LLM_QUERY, actions.get(0).getActionType());
    assertTrue(actions.get(0).getMessage().isPresent());
    assertEquals(AssistantActionType.TOOL_CALL, actions.get(1).getActionType());
    assertEquals("message_user", actions.get(1).getToolInvocation().orElseThrow().getToolName());
    assertEquals(AssistantActionType.TOOL_CALL, actions.get(2).getActionType());
    assertEquals("end_turn", actions.get(2).getToolInvocation().orElseThrow().getToolName());

    JsonArray transcript = transcript(harness.log, "chat");
    assertEquals(4, transcript.size(), "Should have user, assistant, and two tool messages");
    assertEquals("user", ((JsonObject) transcript.get(0)).get("role"));
    assertEquals("assistant", ((JsonObject) transcript.get(1)).get("role"));
    assertEquals("call_1", ((JsonObject) transcript.get(2)).get("tool_call_id"));
    assertEquals("call_2", ((JsonObject) transcript.get(3)).get("tool_call_id"));
  }

  /**
   * Test that multiple tool calls execute in order and append results in order.
   */
  @Test
  public void testMultiCallOrder() {
    Harness harness = new Harness(25);
    MockToolStrategy echo = new MockToolStrategy("echo", "echoed");
    harness.toolbelt.addTool(echo);

    JsonArray calls = new JsonArray();
    calls.add(toolCall("call_1", "echo", "{}"));
    calls.add(toolCall("call_2", "message_user", "{\"message\":\"Hi\"}"));
    calls.add(toolCall("call_3", "end_turn", "{}"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(calls, null), "tool_calls"));

    harness.strategy.runTurn(harness.toolbelt, "chat", "go", harness.callback);

    List<AssistantResult> actions = harness.callback.actions;
    assertEquals(4, actions.size());
    assertEquals("echo", actions.get(1).getToolInvocation().orElseThrow().getToolName());
    assertEquals("message_user", actions.get(2).getToolInvocation().orElseThrow().getToolName());
    assertEquals("end_turn", actions.get(3).getToolInvocation().orElseThrow().getToolName());

    JsonArray transcript = transcript(harness.log, "chat");
    assertEquals("call_1", ((JsonObject) transcript.get(2)).get("tool_call_id"));
    assertEquals("echoed", ((JsonObject) transcript.get(2)).get("content"));
    assertEquals("call_2", ((JsonObject) transcript.get(3)).get("tool_call_id"));
    assertEquals("call_3", ((JsonObject) transcript.get(4)).get("tool_call_id"));
    assertEquals(1, echo.getInvocations().size(), "Echo should record its invocation");
  }

  /**
   * Test that a plain text reply becomes an implicit message_user plus end of turn.
   */
  @Test
  public void testPlainTextBecomesImplicitMessage() {
    Harness harness = new Harness(25);
    harness.wire.enqueuePayload(
        buildResponse(assistantMessage(null, "Hello there"), "stop"));

    harness.strategy.runTurn(harness.toolbelt, "chat", "hi", harness.callback);

    assertEquals(1, harness.callback.completions.size());
    assertTrue(harness.callback.completions.get(0).getWasSuccessful());

    List<AssistantResult> actions = harness.callback.actions;
    assertEquals(2, actions.size());
    assertEquals(AssistantActionType.TOOL_CALL, actions.get(1).getActionType());
    AssistantToolInvocation invocation = actions.get(1).getToolInvocation().orElseThrow();
    assertEquals("message_user", invocation.getToolName());
    assertEquals("Hello there", invocation.getCall().get("message"));

    JsonArray transcript = transcript(harness.log, "chat");
    assertEquals(2, transcript.size(), "Should have user and assistant only");
    assertEquals("assistant", ((JsonObject) transcript.get(1)).get("role"));
  }

  /**
   * Test that an unknown tool produces a failed tool call while keeping the batch complete.
   */
  @Test
  public void testUnknownToolProducesFailure() {
    Harness harness = new Harness(25);
    JsonArray firstCalls = new JsonArray();
    firstCalls.add(toolCall("call_bad", "does_not_exist", "{}"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(firstCalls, null), "tool_calls"));

    JsonArray secondCalls = new JsonArray();
    secondCalls.add(toolCall("call_end", "end_turn", "{}"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(secondCalls, null), "tool_calls"));

    harness.strategy.runTurn(harness.toolbelt, "chat", "hi", harness.callback);

    List<AssistantResult> actions = harness.callback.actions;
    assertEquals(4, actions.size());
    assertFalse(actions.get(1).getWasSuccessful(), "Unknown tool should fail");
    assertEquals("does_not_exist", actions.get(1).getToolInvocation().orElseThrow().getToolName());

    JsonArray transcript = transcript(harness.log, "chat");
    assertEquals(5, transcript.size());
    JsonObject failedTool = (JsonObject) transcript.get(2);
    assertEquals("tool", failedTool.get("role"));
    assertEquals("call_bad", failedTool.get("tool_call_id"));
    assertTrue(((String) failedTool.get("content")).contains("does_not_exist"));
    assertEquals(1, harness.callback.completions.size());
  }

  /**
   * Test that malformed tool arguments fail the query rather than being silently ignored.
   */
  @Test
  public void testMalformedArgumentsFailQuery() {
    Harness harness = new Harness(25);
    JsonArray firstCalls = new JsonArray();
    firstCalls.add(toolCall("call_bad", "message_user", "{\"message\":"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(firstCalls, null), "tool_calls"));

    harness.strategy.runTurn(harness.toolbelt, "chat", "hi", harness.callback);

    List<AssistantResult> actions = harness.callback.actions;
    assertEquals(1, actions.size(), "Malformed arguments should fail the model query");
    assertFalse(actions.get(0).getWasSuccessful(), "Malformed arguments should fail");
    assertEquals(1, harness.callback.completions.size());
  }

  /**
   * Test that a tool which throws becomes a failed tool call while keeping the batch complete.
   */
  @Test
  public void testThrowingToolProducesFailure() {
    Harness harness = new Harness(25);
    MockToolStrategy exploding =
        new MockToolStrategy("explode", "unused", new RuntimeException("tool boom"));
    harness.toolbelt.addTool(exploding);

    JsonArray firstCalls = new JsonArray();
    firstCalls.add(toolCall("call_bad", "explode", "{}"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(firstCalls, null), "tool_calls"));

    JsonArray secondCalls = new JsonArray();
    secondCalls.add(toolCall("call_end", "end_turn", "{}"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(secondCalls, null), "tool_calls"));

    harness.strategy.runTurn(harness.toolbelt, "chat", "hi", harness.callback);

    List<AssistantResult> actions = harness.callback.actions;
    assertFalse(actions.get(1).getWasSuccessful(), "A throwing tool should fail");
    assertEquals("tool boom", actions.get(1).getError().orElse(null));

    JsonArray transcript = transcript(harness.log, "chat");
    JsonObject failedTool = (JsonObject) transcript.get(2);
    assertEquals("call_bad", failedTool.get("tool_call_id"));
    assertTrue(((String) failedTool.get("content")).contains("tool boom"));
    assertEquals(1, harness.callback.completions.size());
  }

  /**
   * Test that the turn stops with TURN_ACTIONS_EXCEEDED at the maximum.
   */
  @Test
  public void testMaxActionsExceeded() {
    Harness harness = new Harness(1);
    JsonArray calls = new JsonArray();
    calls.add(toolCall("call_1", "message_user", "{\"message\":\"Hi\"}"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(calls, null), "tool_calls"));

    harness.strategy.runTurn(harness.toolbelt, "chat", "hi", harness.callback);

    assertEquals(1, harness.callback.completions.size());
    AssistantResult terminal = harness.callback.completions.get(0);
    assertEquals(AssistantActionType.TURN_ACTIONS_EXCEEDED, terminal.getActionType());
    assertFalse(terminal.getWasSuccessful());
    assertTrue(terminal.getError().isPresent());
    assertEquals(1, harness.wire.getSentBodies().size(), "No second request should be sent");
  }

  /**
   * Test that the assistant message round-trips verbatim into the next request body.
   */
  @Test
  public void testAssistantMessageRoundTripsVerbatim() {
    Harness harness = new Harness(25);
    JsonArray calls = new JsonArray();
    calls.add(toolCall("call_1", "message_user", "{\"message\":\"Hi\"}"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(calls, null), "tool_calls"));

    JsonArray endCalls = new JsonArray();
    endCalls.add(toolCall("call_2", "end_turn", "{}"));
    harness.wire.enqueuePayload(buildResponse(assistantMessage(endCalls, null), "tool_calls"));

    harness.strategy.runTurn(harness.toolbelt, "chat", "hi", harness.callback);

    assertEquals(2, harness.wire.getSentBodies().size());

    String firstBody = harness.wire.getSentBodies().get(0);
    String secondBody = harness.wire.getSentBodies().get(1);
    try {
      JsonObject first = (JsonObject) Jsoner.deserialize(firstBody);
      JsonArray firstMessages = (JsonArray) first.get("messages");
      assertEquals(2, firstMessages.size(), "First request should have system and user");

      JsonObject second = (JsonObject) Jsoner.deserialize(secondBody);
      JsonArray secondMessages = (JsonArray) second.get("messages");
      assertEquals(4, secondMessages.size(), "Should replay user, assistant, and tool messages");

      JsonObject replayedAssistant = (JsonObject) secondMessages.get(2);
      assertEquals("assistant", replayedAssistant.get("role"));
      JsonArray replayedCalls = (JsonArray) replayedAssistant.get("tool_calls");
      assertEquals(1, replayedCalls.size());
      JsonObject replayedCall = (JsonObject) replayedCalls.get(0);
      assertEquals("call_1", replayedCall.get("id"));
      JsonObject function = (JsonObject) replayedCall.get("function");
      assertEquals("message_user", function.get("name"));

      JsonObject replayedTool = (JsonObject) secondMessages.get(3);
      assertEquals("tool", replayedTool.get("role"));
      assertEquals("call_1", replayedTool.get("tool_call_id"));
    } catch (JsonException e) {
      throw new AssertionError(e);
    }
  }
}
