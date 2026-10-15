/**
 * Unit tests for the AssistantModel class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.result.AssistantActionType;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.tool.AssistantToolStrategy;
import org.kigalisim.assistant.tool.MessageUserTool;

/**
 * Tests for the AssistantModel class.
 */
public class AssistantModelTest {

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
   * Builds a model over a mock wire with the real protocol strategy.
   *
   * @param wire The mock wire.
   * @return The model.
   */
  private AssistantModel buildModel(MockModelWireStrategy wire) {
    return new AssistantModel(
        wire, new OpenAiModelProtocolStrategy("test-model"), "test-model", "test-service");
  }

  /**
   * Test that a successful transport payload is parsed and forwarded.
   *
   * @throws IOException If the fixture cannot be read.
   */
  @Test
  public void testSendMessageParsesPayload() throws IOException {
    MockModelWireStrategy wire = new MockModelWireStrategy();
    wire.enqueuePayload(readFixture("openai_response_tool_call.json"));
    AssistantModel model = buildModel(wire);

    List<AssistantResult> results = new ArrayList<>();
    model.sendMessage("request-body", results::add);

    assertEquals(1, results.size());
    AssistantResult result = results.get(0);
    assertEquals(AssistantActionType.LLM_QUERY, result.getActionType());
    assertTrue(result.getWasSuccessful());
    assertTrue(result.getRawResponse().isPresent());
    assertEquals(1, result.getRawResponse().get().getToolCalls().size());
    assertEquals(List.of("request-body"), wire.getSentBodies());
  }

  /**
   * Test that a transport error is forwarded unchanged.
   */
  @Test
  public void testSendMessageForwardsError() {
    MockModelWireStrategy wire = new MockModelWireStrategy();
    wire.enqueueError("transport down");
    AssistantModel model = buildModel(wire);

    List<AssistantResult> results = new ArrayList<>();
    model.sendMessage("body", results::add);

    assertEquals(1, results.size());
    AssistantResult result = results.get(0);
    assertFalse(result.getWasSuccessful());
    assertEquals("transport down", result.getError().orElse(null));
    assertEquals(AssistantActionType.LLM_QUERY, result.getActionType());
  }

  /**
   * Test that an exception thrown by the wire becomes a failed query result.
   */
  @Test
  public void testSendMessageWrapsThrownException() {
    MockModelWireStrategy wire = new MockModelWireStrategy();
    wire.enqueueException(new RuntimeException("wire exploded"));
    AssistantModel model = buildModel(wire);

    List<AssistantResult> results = new ArrayList<>();
    model.sendMessage("body", results::add);

    assertEquals(1, results.size());
    assertFalse(results.get(0).getWasSuccessful());
    assertEquals("wire exploded", results.get(0).getError().orElse(null));
  }

  /**
   * Test that an unparseable payload becomes a failed query result.
   */
  @Test
  public void testSendMessageWrapsParseFailure() {
    MockModelWireStrategy wire = new MockModelWireStrategy();
    wire.enqueuePayload("{not json");
    AssistantModel model = buildModel(wire);

    List<AssistantResult> results = new ArrayList<>();
    model.sendMessage("body", results::add);

    assertEquals(1, results.size());
    assertFalse(results.get(0).getWasSuccessful());
    assertEquals(AssistantActionType.LLM_QUERY, results.get(0).getActionType());
  }

  /**
   * Test that buildRequest delegates to the protocol strategy.
   */
  @Test
  public void testBuildRequestDelegates() {
    MockModelWireStrategy wire = new MockModelWireStrategy();
    AssistantModel model = buildModel(wire);
    List<AssistantToolStrategy> tools = List.of(new MessageUserTool());

    String request = model.buildRequest(List.of(), tools, "SYS", "USER");

    assertTrue(request.contains("\"role\":\"system\""));
    assertTrue(request.contains("\"model\":\"test-model\""));
    assertEquals("test-model", model.getModelName());
    assertEquals("test-service", model.getModelService());
  }
}
