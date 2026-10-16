/**
 * Unit tests for the RunSimulationTool class.
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
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.assistant.AssistantTestResources;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.workspace.AssistantWorkspace;
import org.kigalisim.assistant.workspace.AssistantWorkspaceBuilder;

/**
 * Tests for the RunSimulationTool class.
 */
public class RunSimulationToolTest {

  private static final String SYNTAX_ERROR = "this is not QubecTalk at all";

  private AssistantWorkspace workspaceWith(String filename, String contents) {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile(filename, contents);
    return workspace;
  }

  private AssistantWorkspace basicWorkspace() {
    return workspaceWith("basic.qta", AssistantTestResources.readQta("basic.qta"));
  }

  private AssistantWorkspace multiWorkspace() {
    return workspaceWith("multi.qta", AssistantTestResources.readQta("multi.qta"));
  }

  private JsonObject callWith(Object filename) {
    JsonObject call = new JsonObject();
    if (filename != null) {
      call.put("filename", filename);
    }
    return call;
  }

  private AssistantResult execute(AssistantWorkspace workspace, JsonObject call) {
    return execute(new RunSimulationTool(workspace), call);
  }

  private AssistantResult execute(RunSimulationTool tool, JsonObject call) {
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(call, results::add);
    assertEquals(1, results.size(), "the callback should be invoked exactly once");
    return results.get(0);
  }

  private String content(AssistantResult result) {
    return result.getRawResponse().flatMap(response -> response.getContent()).orElse(null);
  }

  /**
   * Test the tool constant, description, and parameter schema.
   */
  @Test
  public void testNameDescriptionAndSchema() {
    RunSimulationTool tool = new RunSimulationTool(new AssistantWorkspaceBuilder().build());
    assertEquals("run_simulation", RunSimulationTool.NAME);
    assertEquals("run_simulation", tool.getName());
    assertEquals(
        "Run a QubecTalk simulation file and return the results as CSV text, optionally "
            + "limited to a start and end year",
        tool.getDescription());
    JsonObject schema = tool.getParameterSchema();
    assertEquals("object", schema.get("type"));
    JsonObject properties = (JsonObject) schema.get("properties");
    JsonObject filename = (JsonObject) properties.get("filename");
    assertEquals("string", filename.get("type"));
    JsonObject startYear = (JsonObject) properties.get("start_year");
    assertEquals("integer", startYear.get("type"));
    JsonObject endYear = (JsonObject) properties.get("end_year");
    assertEquals("integer", endYear.get("type"));
    JsonArray required = (JsonArray) schema.get("required");
    assertTrue(required.contains("filename"));
    assertFalse(required.contains("start_year"));
    assertFalse(required.contains("end_year"));
  }

  /**
   * Test that a valid program returns the expected CSV.
   */
  @Test
  public void testValidRun() {
    AssistantWorkspace workspace = basicWorkspace();
    AssistantResult result = execute(workspace, callWith("basic.qta"));
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    String output = content(result);
    assertNotNull(output);
    String header = KigaliSimFacade.convertResultsToCsv(List.of());
    assertTrue(output.startsWith(header));
    assertTrue(output.contains("business as usual,1,1,test,test,"));
  }

  /**
   * Test that all scenarios are included in one CSV table.
   */
  @Test
  public void testAllScenarios() {
    AssistantWorkspace workspace = multiWorkspace();
    AssistantResult result = execute(workspace, callWith("multi.qta"));
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    String output = content(result);
    assertTrue(output.contains("business as usual"));
    assertTrue(output.contains("with permit"));
  }

  /**
   * Test that a short run is not truncated.
   */
  @Test
  public void testShortRunUntouched() {
    AssistantWorkspace workspace = basicWorkspace();
    AssistantResult result = execute(workspace, callWith("basic.qta"));
    assertTrue(result.getWasSuccessful());
    assertFalse(content(result).contains("omitted"));
  }

  /**
   * Test that output larger than the transport cap is truncated on a newline boundary.
   */
  @Test
  public void testTruncation() {
    AssistantWorkspace workspace = basicWorkspace();
    String header = KigaliSimFacade.convertResultsToCsv(List.of());
    RunSimulationTool tool = new RunSimulationTool(workspace, header.length() + 10);
    AssistantResult result = execute(tool, callWith("basic.qta"));
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    String output = content(result);
    assertTrue(output.startsWith(header));
    assertTrue(output.endsWith("characters omitted]"));
  }

  /**
   * Test that the optional year range filters the returned rows.
   */
  @Test
  public void testYearRangeFiltering() {
    AssistantWorkspace workspace = basicWorkspace();
    JsonObject call = callWith("basic.qta");
    call.put("start_year", 2);
    call.put("end_year", 2);
    AssistantResult result = execute(workspace, call);
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    String output = content(result);
    assertNotNull(output);
    String[] lines = output.stripTrailing().split("\n");
    assertTrue(lines.length > 1, "expected at least one data row");
    for (int i = 1; i < lines.length; i++) {
      assertEquals("2", lines[i].split(",")[2]);
    }
  }

  /**
   * Test that a start year alone drops earlier rows.
   */
  @Test
  public void testStartYearOnly() {
    AssistantWorkspace workspace = basicWorkspace();
    JsonObject call = callWith("basic.qta");
    call.put("start_year", "2");
    AssistantResult result = execute(workspace, call);
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    String output = content(result);
    assertFalse(output.contains("business as usual,1,1,"));
    assertTrue(output.contains("business as usual,1,2,"));
  }

  /**
   * Test that bad year parameters are failed results.
   */
  @Test
  public void testInvalidYearParameters() {
    AssistantWorkspace workspace = basicWorkspace();

    JsonObject nonNumeric = callWith("basic.qta");
    nonNumeric.put("start_year", "later");
    assertFalse(execute(workspace, nonNumeric).getWasSuccessful());

    JsonObject reversed = callWith("basic.qta");
    reversed.put("start_year", 3);
    reversed.put("end_year", 1);
    assertFalse(execute(workspace, reversed).getWasSuccessful());
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
   * Test that a syntax error is reported as a failed result.
   */
  @Test
  public void testSyntaxError() {
    AssistantWorkspace workspace = workspaceWith("bad.qta", SYNTAX_ERROR);
    AssistantResult result = execute(workspace, callWith("bad.qta"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("Failed to parse QubecTalk code"));
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

  /**
   * Test that a valid program with no scenarios is a failed result.
   */
  @Test
  public void testNoScenarios() {
    AssistantWorkspace workspace =
        workspaceWith("empty.qta", AssistantTestResources.readQta("no_scenarios.qta"));
    AssistantResult result = execute(workspace, callWith("empty.qta"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("defines no scenarios"));
  }
}
