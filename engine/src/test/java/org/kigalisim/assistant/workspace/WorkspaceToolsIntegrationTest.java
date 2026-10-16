/**
 * Integration tests for the workspace tools.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.tool.EditFileTool;
import org.kigalisim.assistant.tool.ListFilesTool;
import org.kigalisim.assistant.tool.OutlineFileTool;
import org.kigalisim.assistant.tool.ReadFileTool;

/**
 * Exercises the workspace tools together over an in-memory workspace.
 */
public class WorkspaceToolsIntegrationTest {

  private static final String BASELINE = String.join("\n",
      "start default",
      "  set sales to 1",
      "end default");

  private String execute(Object tool, JsonObject call) {
    List<AssistantResult> results = new ArrayList<>();
    if (tool instanceof ListFilesTool listFilesTool) {
      listFilesTool.execute(call, results::add);
    } else if (tool instanceof OutlineFileTool outlineFileTool) {
      outlineFileTool.execute(call, results::add);
    } else if (tool instanceof ReadFileTool readFileTool) {
      readFileTool.execute(call, results::add);
    } else if (tool instanceof EditFileTool editFileTool) {
      editFileTool.execute(call, results::add);
    }
    AssistantResult result = results.get(0);
    assertTrue(result.getWasSuccessful(), result.getError().orElse("expected success"));
    return result.getRawResponse().orElseThrow().getContent().orElse(null);
  }

  /**
   * Test the full list, edit, read, and outline flow.
   */
  @Test
  public void testScenario() {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile("simulation.qta");

    assertEquals("simulation.qta", execute(new ListFilesTool(workspace), new JsonObject()));

    String patch = String.join("\n",
        "--- simulation.qta",
        "+++ simulation.qta",
        "@@ -1 +1,3 @@",
        "-",
        "+start default",
        "+  set sales to 1",
        "+end default");
    execute(new EditFileTool(workspace), callWith("simulation.qta", patch));

    assertEquals(BASELINE, execute(new ReadFileTool(workspace), callWith("simulation.qta")));

    JsonObject outlineCall = callWith("simulation.qta");
    assertEquals("default", execute(new OutlineFileTool(workspace), outlineCall));

    JsonObject sectionCall = callWith("simulation.qta");
    sectionCall.put("section", "default");
    assertEquals(BASELINE, execute(new ReadFileTool(workspace), sectionCall));
  }

  private JsonObject callWith(String filename) {
    JsonObject call = new JsonObject();
    call.put("filename", filename);
    return call;
  }

  private JsonObject callWith(String filename, String patch) {
    JsonObject call = callWith(filename);
    call.put("patch", patch);
    return call;
  }
}
