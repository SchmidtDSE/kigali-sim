/**
 * Unit tests for the EditFileTool class.
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
import org.kigalisim.assistant.workspace.AssistantWorkspace;
import org.kigalisim.assistant.workspace.AssistantWorkspaceBuilder;

/**
 * Tests for the EditFileTool class.
 */
public class EditFileToolTest {

  private AssistantWorkspace workspaceWith(String contents) {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile("simulation.qta", contents);
    return workspace;
  }

  private AssistantResult edit(AssistantWorkspace workspace, JsonObject call) {
    EditFileTool tool = new EditFileTool(workspace);
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(call, results::add);
    return results.get(0);
  }

  private JsonObject callWith(String filename, String patch) {
    JsonObject call = new JsonObject();
    if (filename != null) {
      call.put("filename", filename);
    }
    if (patch != null) {
      call.put("patch", patch);
    }
    return call;
  }

  /**
   * Test the tool constant and schema.
   */
  @Test
  public void testNameAndSchema() {
    EditFileTool tool = new EditFileTool(new AssistantWorkspaceBuilder().build());
    assertEquals("edit_file", EditFileTool.NAME);
    assertEquals("edit_file", tool.getName());
    JsonObject schema = tool.getParameterSchema();
    assertEquals("object", schema.get("type"));
    JsonArray required = (JsonArray) schema.get("required");
    assertTrue(required.contains("filename"));
    assertTrue(required.contains("patch"));
  }

  /**
   * Test a replacement patch.
   */
  @Test
  public void testReplacement() {
    AssistantWorkspace workspace = workspaceWith("start default\n  set sales to 1\nend default");
    String patch = String.join("\n",
        "--- simulation.qta",
        "+++ simulation.qta",
        "@@ -1,3 +1,3 @@",
        " start default",
        "-  set sales to 1",
        "+  set sales to 2",
        " end default");
    AssistantResult result = edit(workspace, callWith("simulation.qta", patch));

    assertTrue(result.getWasSuccessful());
    assertEquals("start default\n  set sales to 2\nend default",
        workspace.readFile("simulation.qta"));
  }

  /**
   * Test an insertion patch.
   */
  @Test
  public void testInsertion() {
    AssistantWorkspace workspace = workspaceWith("start default\nend default");
    String patch = String.join("\n",
        "--- simulation.qta",
        "+++ simulation.qta",
        "@@ -1,2 +1,3 @@",
        " start default",
        "+  set sales to 1",
        " end default");
    AssistantResult result = edit(workspace, callWith("simulation.qta", patch));

    assertTrue(result.getWasSuccessful());
    assertEquals("start default\n  set sales to 1\nend default",
        workspace.readFile("simulation.qta"));
  }

  /**
   * Test a deletion patch.
   */
  @Test
  public void testDeletion() {
    AssistantWorkspace workspace = workspaceWith("start default\n  set sales to 1\nend default");
    String patch = String.join("\n",
        "--- simulation.qta",
        "+++ simulation.qta",
        "@@ -1,3 +1,2 @@",
        " start default",
        "-  set sales to 1",
        " end default");
    AssistantResult result = edit(workspace, callWith("simulation.qta", patch));

    assertTrue(result.getWasSuccessful());
    assertEquals("start default\nend default", workspace.readFile("simulation.qta"));
  }

  /**
   * Test that a non-simulation filename is rejected.
   */
  @Test
  public void testNonQtaRejected() {
    AssistantWorkspace workspace = workspaceWith("hello");
    AssistantResult result = edit(workspace, callWith("notes.txt", ""));
    assertFalse(result.getWasSuccessful());
    assertTrue(result.getError().orElse("").contains(".qta")
        || result.getError().orElse("").contains("QubecTalk"));
  }

  /**
   * Test that a headerless patch and a context mismatch are failed results.
   */
  @Test
  public void testBadPatches() {
    AssistantWorkspace workspace = workspaceWith("start default\nend default");
    AssistantResult headerless = edit(workspace, callWith("simulation.qta", "just some text"));
    assertFalse(headerless.getWasSuccessful());

    String mismatch = String.join("\n",
        "--- simulation.qta",
        "+++ simulation.qta",
        "@@ -1,1 +1,1 @@",
        "-nonexistent line",
        "+something else");
    AssistantResult failed = edit(workspace, callWith("simulation.qta", mismatch));
    assertFalse(failed.getWasSuccessful());
  }

  /**
   * Test that missing parameters and a missing file are failed results.
   */
  @Test
  public void testMissingInputs() {
    AssistantWorkspace workspace = workspaceWith("start default\nend default");

    assertFalse(edit(workspace, callWith(null, "patch")).getWasSuccessful());
    assertFalse(edit(workspace, callWith("simulation.qta", null)).getWasSuccessful());
    assertFalse(edit(workspace, callWith("missing.qta", "patch")).getWasSuccessful());
  }
}
