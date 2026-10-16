/**
 * Integration tests for the simulation tools.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.tool.EditFileTool;
import org.kigalisim.assistant.tool.RunSimulationTool;
import org.kigalisim.assistant.tool.ValidateSimulationTool;

/**
 * Exercises the simulation tools together over an in-memory workspace.
 */
public class SimulationToolsIntegrationTest {

  private static final String BASELINE = String.join("\n",
      "start default",
      "  define application \"test\"",
      "    uses substance \"test\"",
      "      enable domestic",
      "      initial charge with 5 kg / unit for domestic",
      "      set domestic to 100 units",
      "      equals 5 tCO2e / mt",
      "    end substance",
      "  end application",
      "end default",
      "",
      "start simulations",
      "  simulate \"business as usual\" from years 1 to 2",
      "end simulations");

  private JsonObject callWith(String filename) {
    JsonObject call = new JsonObject();
    call.put("filename", filename);
    return call;
  }

  private JsonObject patchCall(String filename, String patch) {
    JsonObject call = callWith(filename);
    call.put("patch", patch);
    return call;
  }

  private AssistantResult validate(AssistantWorkspace workspace) {
    List<AssistantResult> results = new ArrayList<>();
    new ValidateSimulationTool(workspace).execute(callWith("simulation.qta"), results::add);
    assertEquals(1, results.size());
    return results.get(0);
  }

  private AssistantResult run(AssistantWorkspace workspace) {
    List<AssistantResult> results = new ArrayList<>();
    new RunSimulationTool(workspace).execute(callWith("simulation.qta"), results::add);
    assertEquals(1, results.size());
    return results.get(0);
  }

  private AssistantResult edit(AssistantWorkspace workspace, String patch) {
    List<AssistantResult> results = new ArrayList<>();
    new EditFileTool(workspace).execute(patchCall("simulation.qta", patch), results::add);
    assertEquals(1, results.size());
    return results.get(0);
  }

  private String content(AssistantResult result) {
    return result.getRawResponse().flatMap(response -> response.getContent()).orElse(null);
  }

  /**
   * Test the validate, edit, validate, repair, and run flow.
   */
  @Test
  public void testValidateEditRunFlow() {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile("simulation.qta", BASELINE);

    AssistantResult valid = validate(workspace);
    assertTrue(valid.getWasSuccessful(), valid.getError().orElse("expected success"));
    assertTrue(content(valid).contains("business as usual"));

    String breakPatch = String.join("\n",
        "--- simulation.qta",
        "+++ simulation.qta",
        "@@ -3,5 +3,5 @@",
        "     uses substance \"test\"",
        "       enable domestic",
        "       initial charge with 5 kg / unit for domestic",
        "-      set domestic to 100 units",
        "+      set domestic to ???",
        "       equals 5 tCO2e / mt");
    AssistantResult broken = edit(workspace, breakPatch);
    assertTrue(broken.getWasSuccessful(), broken.getError().orElse("expected edit to apply"));

    AssistantResult invalid = validate(workspace);
    assertFalse(invalid.getWasSuccessful());

    String repairPatch = String.join("\n",
        "--- simulation.qta",
        "+++ simulation.qta",
        "@@ -3,5 +3,5 @@",
        "     uses substance \"test\"",
        "       enable domestic",
        "       initial charge with 5 kg / unit for domestic",
        "-      set domestic to ???",
        "+      set domestic to 100 units",
        "       equals 5 tCO2e / mt");
    AssistantResult repaired = edit(workspace, repairPatch);
    assertTrue(repaired.getWasSuccessful(), repaired.getError().orElse("expected edit to apply"));

    AssistantResult run = run(workspace);
    assertTrue(run.getWasSuccessful(), run.getError().orElse("expected success"));
    String csv = content(run);
    assertTrue(csv.startsWith("scenario,trial,year,application,substance,"));
    assertTrue(csv.contains("business as usual"));
  }
}
