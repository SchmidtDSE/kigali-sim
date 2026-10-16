/**
 * Unit tests for the MessageUserTool class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.result.AssistantResult;

/**
 * Tests for the MessageUserTool class.
 */
public class MessageUserToolTest {

  /**
   * Builds a call with the given message value.
   *
   * @param message The message value, or null to omit it.
   * @return The call object.
   */
  private JsonObject callWithMessage(String message) {
    JsonObject call = new JsonObject();
    if (message != null) {
      call.put("message", message);
    }
    return call;
  }

  /**
   * Test the tool constant and required parameter schema.
   */
  @Test
  public void testNameAndSchema() {
    MessageUserTool tool = new MessageUserTool();
    assertEquals("message_user", MessageUserTool.NAME);
    assertEquals("message_user", tool.getName());
    JsonObject schema = tool.getParameterSchema();
    assertEquals("object", schema.get("type"));
    JsonArray required = (JsonArray) schema.get("required");
    assertTrue(required.contains("message"));
  }

  /**
   * Test that a present message is reported as the tool output.
   */
  @Test
  public void testExecuteWithMessage() {
    MessageUserTool tool = new MessageUserTool();
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(callWithMessage("Hello"), results::add);

    assertEquals(1, results.size());
    assertTrue(results.get(0).getWasSuccessful());
    assertEquals("Hello", results.get(0).getRawResponse().orElseThrow().getContent().orElse(null));
  }

  /**
   * Test that a missing message is reported as an error.
   */
  @Test
  public void testExecuteWithMissingMessage() {
    MessageUserTool tool = new MessageUserTool();
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(callWithMessage(null), results::add);

    assertEquals(1, results.size());
    assertFalse(results.get(0).getWasSuccessful());
    assertTrue(results.get(0).getError().orElse("").contains("message"));
  }

  /**
   * Test that a blank message is reported as an error.
   */
  @Test
  public void testExecuteWithBlankMessage() {
    MessageUserTool tool = new MessageUserTool();
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(callWithMessage("   "), results::add);

    assertEquals(1, results.size());
    assertFalse(results.get(0).getWasSuccessful());
  }
}
