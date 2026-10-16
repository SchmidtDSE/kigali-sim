/**
 * Unit tests for the CalculateTool class.
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
 * Tests for the CalculateTool class.
 */
public class CalculateToolTest {

  /**
   * Builds a call with the given expression value.
   *
   * @param expression The expression value, or null to omit it.
   * @return The call object.
   */
  private JsonObject callWithExpression(Object expression) {
    JsonObject call = new JsonObject();
    if (expression != null) {
      call.put("expression", expression);
    }
    return call;
  }

  /**
   * Runs the tool and returns the single result produced.
   *
   * @param call The call to execute.
   * @return The result reported by the tool.
   */
  private AssistantResult execute(JsonObject call) {
    CalculateTool tool = new CalculateTool();
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(call, results::add);
    assertEquals(1, results.size());
    return results.get(0);
  }

  /**
   * Test the tool constant and required parameter schema.
   */
  @Test
  public void testNameAndSchema() {
    CalculateTool tool = new CalculateTool();
    assertEquals("calculate", CalculateTool.NAME);
    assertEquals("calculate", tool.getName());
    JsonObject schema = tool.getParameterSchema();
    assertEquals("object", schema.get("type"));
    JsonObject properties = (JsonObject) schema.get("properties");
    JsonObject expression = (JsonObject) properties.get("expression");
    assertEquals("string", expression.get("type"));
    JsonArray required = (JsonArray) schema.get("required");
    assertTrue(required.contains("expression"));
  }

  /**
   * Test a simple expression honoring operator precedence.
   */
  @Test
  public void testSimpleExpression() {
    AssistantResult result = execute(callWithExpression("2 + 3 * 4"));
    assertTrue(result.getWasSuccessful());
    assertEquals("14", result.getRawResponse().orElseThrow().getContent().orElse(null));
  }

  /**
   * Test decimal division within parentheses.
   */
  @Test
  public void testDecimalAndParentheses() {
    AssistantResult result = execute(callWithExpression("(1 + 2) / 4"));
    assertTrue(result.getWasSuccessful());
    assertEquals("0.75", result.getRawResponse().orElseThrow().getContent().orElse(null));
  }

  /**
   * Test exponentiation.
   */
  @Test
  public void testPower() {
    AssistantResult result = execute(callWithExpression("2 ^ 10"));
    assertTrue(result.getWasSuccessful());
    assertEquals("1024", result.getRawResponse().orElseThrow().getContent().orElse(null));
  }

  /**
   * Test that a missing expression is reported as an error.
   */
  @Test
  public void testMissingExpression() {
    AssistantResult result = execute(new JsonObject());
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("expression"));
  }

  /**
   * Test that a blank expression is reported as an error.
   */
  @Test
  public void testBlankExpression() {
    AssistantResult result = execute(callWithExpression("   "));
    assertFalse(result.getWasSuccessful());
  }

  /**
   * Test that a non-string expression is reported as an error.
   */
  @Test
  public void testNonStringExpression() {
    AssistantResult result = execute(callWithExpression(5));
    assertFalse(result.getWasSuccessful());
  }

  /**
   * Test that a malformed expression is reported as an error.
   */
  @Test
  public void testMalformedExpression() {
    AssistantResult result = execute(callWithExpression("2 +"));
    assertFalse(result.getWasSuccessful());
  }

  /**
   * Test that division by zero is reported as an error.
   */
  @Test
  public void testDivisionByZero() {
    AssistantResult result = execute(callWithExpression("1 / 0"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("Division by zero"));
  }

  /**
   * Test that a non-numeric result is reported as an error.
   */
  @Test
  public void testNonNumericResult() {
    AssistantResult result = execute(callWithExpression("1 == 1"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("did not evaluate to a number"));
  }
}
