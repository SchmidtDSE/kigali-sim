/**
 * Unit tests for the OutlineFileTool class.
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
 * Tests for the OutlineFileTool class.
 */
public class OutlineFileToolTest {

  private static final String SAMPLE = String.join("\n",
      "start about",
      "end about",
      "",
      "start default",
      "end default");

  /**
   * Test the tool constant and schema.
   */
  @Test
  public void testNameAndSchema() {
    OutlineFileTool tool = new OutlineFileTool(new AssistantWorkspaceBuilder().build());
    assertEquals("outline_file", OutlineFileTool.NAME);
    assertEquals("outline_file", tool.getName());
    JsonObject schema = tool.getParameterSchema();
    assertEquals("object", schema.get("type"));
    JsonArray required = (JsonArray) schema.get("required");
    assertTrue(required.contains("filename"));
  }

  /**
   * Test outlining a populated file.
   */
  @Test
  public void testOutline() {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile("simulation.qta", SAMPLE);
    OutlineFileTool tool = new OutlineFileTool(workspace);
    JsonObject call = new JsonObject();
    call.put("filename", "simulation.qta");

    List<AssistantResult> results = new ArrayList<>();
    tool.execute(call, results::add);

    assertEquals("about\ndefault",
        results.get(0).getRawResponse().orElseThrow().getContent().orElse(null));
  }

  /**
   * Test that a missing filename is a failed result.
   */
  @Test
  public void testMissingFilename() {
    OutlineFileTool tool = new OutlineFileTool(new AssistantWorkspaceBuilder().build());
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(new JsonObject(), results::add);

    assertEquals(1, results.size());
    assertFalse(results.get(0).getWasSuccessful());
  }

  /**
   * Test that a missing file is a failed result.
   */
  @Test
  public void testMissingFile() {
    OutlineFileTool tool = new OutlineFileTool(new AssistantWorkspaceBuilder().build());
    JsonObject call = new JsonObject();
    call.put("filename", "missing.qta");
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(call, results::add);

    assertEquals(1, results.size());
    assertFalse(results.get(0).getWasSuccessful());
  }
}
