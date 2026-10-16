/**
 * Unit tests for the ReadFileTool class.
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
 * Tests for the ReadFileTool class.
 */
public class ReadFileToolTest {

  private static final String SAMPLE = """
      start about
      end about

      start default
      end default
      """.stripTrailing();

  private AssistantWorkspace workspaceWithSample() {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile("simulation.qta", SAMPLE);
    return workspace;
  }

  private String execute(AssistantWorkspace workspace, JsonObject call) {
    ReadFileTool tool = new ReadFileTool(workspace);
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(call, results::add);
    return results.get(0).getRawResponse()
        .flatMap(response -> response.getContent())
        .orElse(null);
  }

  /**
   * Test the tool constant and schema.
   */
  @Test
  public void testNameAndSchema() {
    ReadFileTool tool = new ReadFileTool(new AssistantWorkspaceBuilder().build());
    assertEquals("read_file", ReadFileTool.NAME);
    assertEquals("read_file", tool.getName());
    JsonObject schema = tool.getParameterSchema();
    assertEquals("object", schema.get("type"));
    JsonArray required = (JsonArray) schema.get("required");
    assertTrue(required.contains("filename"));
  }

  /**
   * Test reading a whole file when no section is given.
   */
  @Test
  public void testReadWholeFile() {
    JsonObject call = new JsonObject();
    call.put("filename", "simulation.qta");
    assertEquals(SAMPLE, execute(workspaceWithSample(), call));
  }

  /**
   * Test reading a named section.
   */
  @Test
  public void testReadSection() {
    JsonObject call = new JsonObject();
    call.put("filename", "simulation.qta");
    call.put("section", "default");
    assertEquals("start default\nend default", execute(workspaceWithSample(), call));
  }

  /**
   * Test that a blank section falls back to the whole file.
   */
  @Test
  public void testBlankSectionReadsWholeFile() {
    JsonObject call = new JsonObject();
    call.put("filename", "simulation.qta");
    call.put("section", "   ");
    assertEquals(SAMPLE, execute(workspaceWithSample(), call));
  }

  /**
   * Test that parameters and files which cannot be resolved are failed results.
   */
  @Test
  public void testFailures() {
    AssistantWorkspace workspace = workspaceWithSample();

    JsonObject missingFilename = new JsonObject();
    List<AssistantResult> results = new ArrayList<>();
    new ReadFileTool(workspace).execute(missingFilename, results::add);
    assertFalse(results.get(0).getWasSuccessful());

    JsonObject missingFile = new JsonObject();
    missingFile.put("filename", "missing.qta");
    results.clear();
    new ReadFileTool(workspace).execute(missingFile, results::add);
    assertFalse(results.get(0).getWasSuccessful());

    JsonObject missingSection = new JsonObject();
    missingSection.put("filename", "simulation.qta");
    missingSection.put("section", "nope");
    results.clear();
    new ReadFileTool(workspace).execute(missingSection, results::add);
    assertFalse(results.get(0).getWasSuccessful());
  }
}
