/**
 * Unit tests for the ValidateSimulationTool class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.AssistantTestResources;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.workspace.AssistantWorkspace;
import org.kigalisim.assistant.workspace.AssistantWorkspaceBuilder;

/**
 * Tests for the ValidateSimulationTool class.
 */
public class ValidateSimulationToolTest {

  private static final String SYNTAX_ERROR = "this is not QubecTalk at all";

  private AssistantWorkspace workspaceWith(String filename, String contents) {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile(filename, contents);
    return workspace;
  }

  private AssistantWorkspace basicWorkspace() {
    return workspaceWith("basic.qta", AssistantTestResources.readQta("basic.qta"));
  }

  private JsonObject callWith(Object filename) {
    JsonObject call = new JsonObject();
    if (filename != null) {
      call.put("filename", filename);
    }
    return call;
  }

  private AssistantResult execute(AssistantWorkspace workspace, JsonObject call) {
    ValidateSimulationTool tool = new ValidateSimulationTool(workspace);
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(call, results::add);
    assertEquals(1, results.size(), "the callback should be invoked exactly once");
    return results.get(0);
  }

  private String content(AssistantResult result) {
    return result.getRawResponse().flatMap(response -> response.getContent()).orElse(null);
  }

  /**
   * Test the tool constant, description, and required parameter schema.
   */
  @Test
  public void testNameDescriptionAndSchema() {
    ValidateSimulationTool tool =
        new ValidateSimulationTool(new AssistantWorkspaceBuilder().build());
    assertEquals("validate_simulation", ValidateSimulationTool.NAME);
    assertEquals("validate_simulation", tool.getName());
    assertEquals("Validate a QubecTalk simulation file, reporting syntax and interpretation errors",
        tool.getDescription());
    JsonObject schema = tool.getParameterSchema();
    assertEquals("object", schema.get("type"));
    JsonObject properties = (JsonObject) schema.get("properties");
    JsonObject filename = (JsonObject) properties.get("filename");
    assertEquals("string", filename.get("type"));
    JsonArray required = (JsonArray) schema.get("required");
    assertTrue(required.contains("filename"));
  }

  /**
   * Test that a valid program is reported as valid with its scenario names.
   */
  @Test
  public void testValidProgram() {
    AssistantWorkspace workspace = basicWorkspace();
    AssistantResult result = execute(workspace, callWith("basic.qta"));
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    String output = content(result);
    assertNotNull(output);
    assertTrue(output.contains("basic.qta"));
    assertTrue(output.contains("valid"));
    assertTrue(output.contains("business as usual"));
  }

  /**
   * Test that a valid program with multiple scenarios lists every scenario in a stable order.
   */
  @Test
  public void testMultipleScenarios() {
    AssistantWorkspace workspace =
        workspaceWith("multi.qta", AssistantTestResources.readQta("multi.qta"));
    AssistantResult result = execute(workspace, callWith("multi.qta"));
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    assertEquals(
        "Simulation 'multi.qta' is valid. Scenarios: business as usual, with permit.",
        content(result));
  }

  /**
   * Test that a valid program with no scenarios still validates.
   */
  @Test
  public void testNoScenariosIsValid() {
    AssistantWorkspace workspace =
        workspaceWith("empty.qta", AssistantTestResources.readQta("no_scenarios.qta"));
    AssistantResult result = execute(workspace, callWith("empty.qta"));
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    assertTrue(content(result).contains("No scenarios"));
  }

  /**
   * Test that missing, blank, and non-string filenames are failed results.
   */
  @Test
  public void testMissingFilename() {
    AssistantWorkspace workspace = basicWorkspace();
    assertFalse(execute(workspace, callWith(null)).getWasSuccessful());
    assertFalse(execute(workspace, callWith("   ")).getWasSuccessful());
    assertFalse(execute(workspace, callWith(5)).getWasSuccessful());
  }

  /**
   * Test that a filename without the QubecTalk extension is a failed result.
   */
  @Test
  public void testNonQtaFilename() {
    AssistantWorkspace workspace = basicWorkspace();
    AssistantResult result = execute(workspace, callWith("notes.txt"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("QubecTalk"));
  }

  /**
   * Test that a file absent from the workspace is a failed result.
   */
  @Test
  public void testMissingFile() {
    AssistantWorkspace workspace = basicWorkspace();
    AssistantResult result = execute(workspace, callWith("missing.qta"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("No file named"));
  }

  /**
   * Test that a syntax error is reported with a line-numbered parse message.
   */
  @Test
  public void testSyntaxError() {
    AssistantWorkspace workspace = workspaceWith("bad.qta", SYNTAX_ERROR);
    AssistantResult result = execute(workspace, callWith("bad.qta"));
    assertFalse(result.getWasSuccessful());
    String error = result.getError().orElse("");
    assertTrue(error.contains("Failed to parse QubecTalk code"));
    assertTrue(error.contains("line"));
  }

  /**
   * Test that an interpretation error is reported as a failed result.
   */
  @Test
  public void testInterpretError() {
    AssistantWorkspace workspace =
        workspaceWith("interpret.qta", AssistantTestResources.readQta("interpret_error.qta"));
    AssistantResult result = execute(workspace, callWith("interpret.qta"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("Failed to interpret QubecTalk code"));
  }
}
