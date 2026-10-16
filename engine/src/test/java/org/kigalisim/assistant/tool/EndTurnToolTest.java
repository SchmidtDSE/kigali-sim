/**
 * Unit tests for the EndTurnTool class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.result.AssistantActionType;
import org.kigalisim.assistant.result.AssistantResult;

/**
 * Tests for the EndTurnTool class.
 */
public class EndTurnToolTest {

  /**
   * Test the tool constant and schema.
   */
  @Test
  public void testNameAndSchema() {
    EndTurnTool tool = new EndTurnTool();
    assertEquals("end_turn", EndTurnTool.NAME);
    assertEquals("end_turn", tool.getName());
    JsonObject schema = tool.getParameterSchema();
    assertEquals("object", schema.get("type"));
    assertTrue(schema.containsKey("properties"));
  }

  /**
   * Test that execution reports a successful tool call.
   */
  @Test
  public void testExecuteReportsCompletion() {
    EndTurnTool tool = new EndTurnTool();
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(new JsonObject(), results::add);

    assertEquals(1, results.size());
    assertEquals(AssistantActionType.TOOL_CALL, results.get(0).getActionType());
    assertTrue(results.get(0).getWasSuccessful());
  }
}
