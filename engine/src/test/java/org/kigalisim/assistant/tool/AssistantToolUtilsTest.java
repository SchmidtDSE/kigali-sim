/**
 * Unit tests for the AssistantToolUtils class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonObject;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.result.AssistantActionType;
import org.kigalisim.assistant.result.AssistantResult;

/**
 * Tests for the AssistantToolUtils class.
 */
public class AssistantToolUtilsTest {

  /**
   * Test that required strings are recognized and blanks and wrong types are rejected.
   */
  @Test
  public void testIsMissing() {
    JsonObject call = new JsonObject();
    call.put("present", "value");
    call.put("blank", "   ");
    call.put("number", 5);

    assertFalse(AssistantToolUtils.isMissing(call, "present"));
    assertTrue(AssistantToolUtils.isMissing(call, "blank"));
    assertTrue(AssistantToolUtils.isMissing(call, "number"));
    assertTrue(AssistantToolUtils.isMissing(call, "absent"));
  }

  /**
   * Test that string values are returned and other values become null.
   */
  @Test
  public void testGetString() {
    JsonObject call = new JsonObject();
    call.put("present", "value");
    call.put("blank", "   ");
    call.put("number", 5);

    assertEquals("value", AssistantToolUtils.getString(call, "present"));
    assertEquals("   ", AssistantToolUtils.getString(call, "blank"));
    assertNull(AssistantToolUtils.getString(call, "number"));
    assertNull(AssistantToolUtils.getString(call, "absent"));
  }

  /**
   * Test that the missing-parameter result reports a failed tool call.
   */
  @Test
  public void testBuildMissingParameterResult() {
    AssistantResult result = AssistantToolUtils.buildMissingParameterResult("filename");
    assertEquals(AssistantActionType.TOOL_CALL, result.getActionType());
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("filename"));
  }
}
