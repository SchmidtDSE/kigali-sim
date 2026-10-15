/**
 * Unit tests for the OpenAiModelProtocolStrategy class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.tool.AssistantToolInvocation;
import org.kigalisim.assistant.tool.AssistantToolStrategy;
import org.kigalisim.assistant.tool.EndTurnTool;
import org.kigalisim.assistant.tool.MessageUserTool;

/**
 * Tests for the OpenAiModelProtocolStrategy class.
 */
public class OpenAiModelProtocolStrategyTest {

  private static final String FIXTURE_DIR = "/org/kigalisim/assistant/model/";

  /**
   * Reads a captured provider payload from the test resources.
   *
   * @param name The fixture filename.
   * @return The fixture contents.
   * @throws IOException If the fixture cannot be read.
   */
  private String readFixture(String name) throws IOException {
    try (InputStream stream = getClass().getResourceAsStream(FIXTURE_DIR + name)) {
      assertNotNull(stream, "Fixture should be present: " + name);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Test that buildRequest assembles the system message, verbatim history, tools, and user message.
   *
   * @throws Exception If the request cannot be parsed.
   */
  @Test
  public void testBuildRequestStructure() throws Exception {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    JsonObject priorAssistant =
        (JsonObject) Jsoner.deserialize("{\"role\":\"assistant\",\"content\":\"prior\"}");
    List<JsonObject> history = List.of(priorAssistant);
    List<AssistantToolStrategy> tools = List.of(new MessageUserTool());

    String request = strategy.buildRequest(history, tools, "SYSTEM", "USER");
    JsonObject parsed = (JsonObject) Jsoner.deserialize(request);

    assertEquals("test-model", parsed.get("model"));
    assertEquals("required", parsed.get("tool_choice"));

    JsonArray messages = (JsonArray) parsed.get("messages");
    assertEquals(3, messages.size(), "Should have system, history, and user messages");

    JsonObject system = (JsonObject) messages.get(0);
    assertEquals("system", system.get("role"));
    assertEquals("SYSTEM", system.get("content"));

    assertEquals(priorAssistant, messages.get(1), "History should be replayed unchanged");

    JsonObject user = (JsonObject) messages.get(2);
    assertEquals("user", user.get("role"));
    assertEquals("USER", user.get("content"));

    assertEquals(1, history.size(), "History should not be mutated");

    JsonArray toolDefinitions = (JsonArray) parsed.get("tools");
    assertEquals(1, toolDefinitions.size());
    JsonObject definition = (JsonObject) toolDefinitions.get(0);
    assertEquals("function", definition.get("type"));
    JsonObject function = (JsonObject) definition.get("function");
    assertEquals("message_user", function.get("name"));
    assertNotNull(function.get("description"));
    JsonObject parameters = (JsonObject) function.get("parameters");
    assertEquals("object", parameters.get("type"));
  }

  /**
   * Test that buildRequest omits the user message when null.
   *
   * @throws Exception If the request cannot be parsed.
   */
  @Test
  public void testBuildRequestWithoutUserMessage() throws Exception {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    String request =
        strategy.buildRequest(List.of(), List.of(new EndTurnTool()), "SYSTEM", null);
    JsonObject parsed = (JsonObject) Jsoner.deserialize(request);
    JsonArray messages = (JsonArray) parsed.get("messages");
    assertEquals(1, messages.size(), "Should have only the system message");
  }

  /**
   * Test parsing a captured tool call payload.
   *
   * @throws IOException If the fixture cannot be read.
   */
  @Test
  public void testParseToolCallFixture() throws IOException {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    String raw = readFixture("openai_response_tool_call.json");
    AssistantModelResponse response = strategy.parseResponse(raw);

    assertTrue(response.getContent().isEmpty(), "Content should be absent");
    assertEquals(1, response.getToolCalls().size());
    AssistantToolInvocation invocation = response.getToolCalls().get(0);
    assertEquals("message_user", invocation.getToolName());
    assertEquals("call_abc123", invocation.getCallId());
    assertEquals("Hello", invocation.getCall().get("message"));
    assertEquals("tool_calls", response.getFinishReason().orElse(null));
    assertEquals(raw, response.getRaw());
    assertEquals("assistant", response.getUninterpreted().get("role"));
    assertNotNull(response.getUninterpreted().get("tool_calls"));
  }

  /**
   * Test parsing a captured plain text payload.
   *
   * @throws IOException If the fixture cannot be read.
   */
  @Test
  public void testParseTextFixture() throws IOException {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    String raw = readFixture("openai_response_text.json");
    AssistantModelResponse response = strategy.parseResponse(raw);

    assertEquals("Here is a plain text reply.", response.getContent().orElse(null));
    assertTrue(response.getToolCalls().isEmpty());
    assertEquals("stop", response.getFinishReason().orElse(null));
  }

  /**
   * Test that multiple tool calls are parsed in order.
   *
   * @throws IOException If the fixture cannot be read.
   */
  @Test
  public void testParseMultipleToolCallsInOrder() throws IOException {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    AssistantModelResponse response =
        strategy.parseResponse(readFixture("openai_response_multi_tool_calls.json"));

    assertEquals(2, response.getToolCalls().size());
    assertEquals("message_user", response.getToolCalls().get(0).getToolName());
    assertEquals("call_first", response.getToolCalls().get(0).getCallId());
    assertEquals("end_turn", response.getToolCalls().get(1).getToolName());
    assertEquals("call_second", response.getToolCalls().get(1).getCallId());
  }

  /**
   * Test that malformed arguments cause parsing to fail.
   *
   * @throws IOException If the fixture cannot be read.
   */
  @Test
  public void testParseMalformedArgumentsThrows() throws IOException {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    String raw = readFixture("openai_response_malformed_arguments.json");
    assertThrows(IllegalStateException.class, () -> strategy.parseResponse(raw));
  }

  /**
   * Test that an unparseable envelope throws.
   */
  @Test
  public void testUnparseableEnvelopeThrows() {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    assertThrows(RuntimeException.class, () -> strategy.parseResponse("{not json"));
  }

  /**
   * Test that a response missing choices throws.
   */
  @Test
  public void testMissingChoicesThrows() {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    assertThrows(RuntimeException.class, () -> strategy.parseResponse("{\"id\":\"abc\"}"));
  }

  /**
   * Test that the human transcript rendering is not yet implemented.
   */
  @Test
  public void testRenderHumanTranscriptThrows() {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    assertThrows(
        UnsupportedOperationException.class, () -> strategy.renderHumanTranscript("[]"));
  }

  /**
   * Test that an absent content field is reported as empty rather than null.
   *
   * @throws Exception If the payload cannot be parsed.
   */
  @Test
  public void testAbsentContentIsEmpty() throws Exception {
    OpenAiModelProtocolStrategy strategy = new OpenAiModelProtocolStrategy("test-model");
    String raw =
        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"tool_calls\":[]},"
            + "\"finish_reason\":\"tool_calls\"}]}";
    AssistantModelResponse response = strategy.parseResponse(raw);
    assertFalse(response.getContent().isPresent());
    assertTrue(response.getToolCalls().isEmpty());
  }
}
