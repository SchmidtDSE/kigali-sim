/**
 * Unit tests for the KigaliAssistantBuilder class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.log.InMemoryLogStrategy;
import org.kigalisim.assistant.model.AssistantModel;
import org.kigalisim.assistant.model.MockModelWireStrategy;
import org.kigalisim.assistant.model.OpenAiModelProtocolStrategy;
import org.kigalisim.assistant.tool.AssistantToolStrategy;
import org.kigalisim.assistant.tool.EndTurnTool;
import org.kigalisim.assistant.tool.MessageUserTool;
import org.kigalisim.assistant.tool.MockToolStrategy;

/**
 * Tests for the KigaliAssistantBuilder class.
 */
public class KigaliAssistantBuilderTest {

  /**
   * Builds a model over a no-op wire.
   *
   * @return The model.
   */
  private AssistantModel buildModel() {
    return new AssistantModel(
        new MockModelWireStrategy(),
        new OpenAiModelProtocolStrategy("test-model"),
        "test-model",
        "test-service");
  }

  /**
   * Builds a builder with the required fields set.
   *
   * @return The builder.
   */
  private KigaliAssistantBuilder buildBuilder() {
    return new KigaliAssistantBuilder()
        .setModel(buildModel())
        .setTurnStrategy((toolbelt, chatName, message, callback) -> null)
        .setChatLogStrategy(new InMemoryLogStrategy());
  }

  /**
   * Test that the built-in tools are registered first.
   */
  @Test
  public void testBuiltInsRegisteredFirst() {
    KigaliAssistant assistant = buildBuilder().build();
    List<AssistantToolStrategy> tools = new ArrayList<>();
    assistant.getToolbelt().getTools().forEach(tools::add);

    assertEquals(2, tools.size());
    assertInstanceOf(EndTurnTool.class, tools.get(0));
    assertInstanceOf(MessageUserTool.class, tools.get(1));
  }

  /**
   * Test that additional tools are appended after the built-ins.
   */
  @Test
  public void testAdditionalToolsAppended() {
    KigaliAssistant assistant =
        buildBuilder().addTool(new MockToolStrategy("custom", "ok")).build();
    List<String> names = new ArrayList<>();
    assistant.getToolbelt().getTools().forEach(tool -> names.add(tool.getName()));

    assertEquals(List.of("end_turn", "message_user", "custom"), names);
  }

  /**
   * Test that duplicate tool names are rejected.
   */
  @Test
  public void testDuplicateToolRejected() {
    KigaliAssistantBuilder builder = buildBuilder();
    assertThrows(IllegalArgumentException.class, () -> builder.addTool(new EndTurnTool()));
  }

  /**
   * Test that missing required fields prevent building.
   */
  @Test
  public void testMissingRequiredFieldsThrow() {
    assertThrows(IllegalStateException.class, () -> new KigaliAssistantBuilder().build());

    KigaliAssistantBuilder missingTurn =
        new KigaliAssistantBuilder().setModel(buildModel());
    assertThrows(IllegalStateException.class, missingTurn::build);

    KigaliAssistantBuilder missingLog =
        new KigaliAssistantBuilder()
            .setModel(buildModel())
            .setTurnStrategy((toolbelt, chatName, message, callback) -> null);
    assertThrows(IllegalStateException.class, missingLog::build);
  }

  /**
   * Test that a fully configured builder produces an assistant.
   */
  @Test
  public void testBuildSucceeds() {
    KigaliAssistant assistant = buildBuilder().build();
    assertTrue(assistant.getWorkspace().isEmpty(), "Workspace is optional");
    assertEquals("test-model", assistant.getModel().getModelName());
  }
}
