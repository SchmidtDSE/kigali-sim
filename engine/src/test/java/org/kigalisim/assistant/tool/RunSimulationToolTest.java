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
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.workspace.AssistantWorkspace;
import org.kigalisim.assistant.workspace.AssistantWorkspaceBuilder;

/**
 * Tests for the RunSimulationTool class.
 */
public class RunSimulationToolTest {

  private static final String VALID = String.join("\n",
      "start default",
      "",
      "  define application \"test\"",
      "",
      "    uses substance \"test\"",
      "      enable domestic",
      "      initial charge with 5 kg / unit for domestic",
      "      set domestic to 100 mt",
      "      equals 5 tCO2e / mt",
      "    end substance",
      "",
      "  end application",
      "",
      "end default",
      "",
      "start simulations",
      "",
      "  simulate \"business as usual\" from years 1 to 3",
      "",
      "end simulations");

  private static final String MULTI = String.join("\n",
      "start default",
      "",
      "  define application \"test\"",
      "",
      "    uses substance \"test\"",
      "      enable domestic",
      "      initial charge with 5 kg / unit for domestic",
      "      set domestic to 100 mt",
      "      equals 5 tCO2e / mt",
      "    end substance",
      "",
      "  end application",
      "",
      "end default",
      "",
      "start simulations",
      "",
      "  simulate \"business as usual\" from years 1 to 2",
      "  simulate \"with permit\" from years 1 to 2",
      "",
      "end simulations");

  private static final String NO_SCENARIOS = String.join("\n",
      "start default",
      "end default",
      "",
      "start simulations",
      "end simulations");

  private static final String SYNTAX_ERROR = "this is not QubecTalk at all";

  private static final String INTERPRET_ERROR = String.join("\n",
      "start default",
      "",
      "  define application \"test\"",
      "",
      "    uses substance \"test\"",
      "      enable domestic",
      "      initial charge with 1 kg / unit for domestic",
      "      set priorEquipment to 100 units during year 1",
      "      retire 20 year old mean weibull",
      "      equals 5 tCO2e / mt",
      "    end substance",
      "",
      "  end application",
      "",
      "end default",
      "",
      "start simulations",
      "",
      "  simulate \"business as usual\" from years 1 to 5",
      "",
      "end simulations");

  private AssistantWorkspace workspaceWith(String filename, String contents) {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile(filename, contents);
    return workspace;
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
   * Test the tool constant, description, and required parameter schema.
   */
  @Test
  public void testNameDescriptionAndSchema() {
    RunSimulationTool tool = new RunSimulationTool(new AssistantWorkspaceBuilder().build());
    assertEquals("run_simulation", RunSimulationTool.NAME);
    assertEquals("run_simulation", tool.getName());
    assertEquals("Run a QubecTalk simulation file and return the results as CSV text",
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
   * Test that a valid program returns the expected CSV.
   */
  @Test
  public void testValidRun() {
    AssistantWorkspace workspace = workspaceWith("basic.qta", VALID);
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
    AssistantWorkspace workspace = workspaceWith("multi.qta", MULTI);
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
    AssistantWorkspace workspace = workspaceWith("basic.qta", VALID);
    AssistantResult result = execute(workspace, callWith("basic.qta"));
    assertTrue(result.getWasSuccessful());
    assertFalse(content(result).contains("omitted"));
  }

  /**
   * Test that output larger than the transport cap is truncated on a newline boundary.
   */
  @Test
  public void testTruncation() {
    AssistantWorkspace workspace = workspaceWith("basic.qta", VALID);
    String header = KigaliSimFacade.convertResultsToCsv(List.of());
    RunSimulationTool tool = new RunSimulationTool(workspace, header.length() + 10);
    AssistantResult result = execute(tool, callWith("basic.qta"));
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    String output = content(result);
    assertTrue(output.startsWith(header));
    assertTrue(output.endsWith("characters omitted]"));
  }

  /**
   * Test that missing, blank, and non-string filenames are failed results.
   */
  @Test
  public void testMissingFilename() {
    AssistantWorkspace workspace = workspaceWith("basic.qta", VALID);
    assertFalse(execute(workspace, callWith(null)).getWasSuccessful());
    assertFalse(execute(workspace, callWith("   ")).getWasSuccessful());
    assertFalse(execute(workspace, callWith(5)).getWasSuccessful());
  }

  /**
   * Test that a filename without the QubecTalk extension is a failed result.
   */
  @Test
  public void testNonQtaFilename() {
    AssistantWorkspace workspace = workspaceWith("basic.qta", VALID);
    AssistantResult result = execute(workspace, callWith("notes.txt"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("QubecTalk"));
  }

  /**
   * Test that a file absent from the workspace is a failed result.
   */
  @Test
  public void testMissingFile() {
    AssistantWorkspace workspace = workspaceWith("basic.qta", VALID);
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
    AssistantWorkspace workspace = workspaceWith("interpret.qta", INTERPRET_ERROR);
    AssistantResult result = execute(workspace, callWith("interpret.qta"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("Failed to interpret QubecTalk code"));
  }

  /**
   * Test that a valid program with no scenarios is a failed result.
   */
  @Test
  public void testNoScenarios() {
    AssistantWorkspace workspace = workspaceWith("empty.qta", NO_SCENARIOS);
    AssistantResult result = execute(workspace, callWith("empty.qta"));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains("defines no scenarios"));
  }
}
