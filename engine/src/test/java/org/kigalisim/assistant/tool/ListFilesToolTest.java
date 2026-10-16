/**
 * Unit tests for the ListFilesTool class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.workspace.AssistantWorkspaceBuilder;

/**
 * Tests for the ListFilesTool class.
 */
public class ListFilesToolTest {

  /**
   * Test the tool constant and schema.
   */
  @Test
  public void testNameAndSchema() {
    ListFilesTool tool = new ListFilesTool(new AssistantWorkspaceBuilder().build());
    assertEquals("list_files", ListFilesTool.NAME);
    assertEquals("list_files", tool.getName());
    JsonObject schema = tool.getParameterSchema();
    assertEquals("object", schema.get("type"));
    assertTrue(schema.containsKey("properties"));
  }

  /**
   * Test that the files are listed in order.
   */
  @Test
  public void testListsFiles() {
    var workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile("a.qta");
    workspace.createFile("b.qta");
    ListFilesTool tool = new ListFilesTool(workspace);

    List<AssistantResult> results = new ArrayList<>();
    tool.execute(new JsonObject(), results::add);

    assertEquals(1, results.size());
    assertTrue(results.get(0).getWasSuccessful());
    assertEquals(
        "a.qta\nb.qta",
        results.get(0)
            .getRawResponse()
            .orElseThrow()
            .getContent()
            .orElse(null)
    );
  }

  /**
   * Test the message for an empty workspace.
   */
  @Test
  public void testEmptyWorkspace() {
    ListFilesTool tool = new ListFilesTool(new AssistantWorkspaceBuilder().build());
    List<AssistantResult> results = new ArrayList<>();
    tool.execute(new JsonObject(), results::add);

    assertEquals(1, results.size());
    assertEquals(
        "No files in the workspace.",
        results.get(0)
            .getRawResponse()
            .orElseThrow()
            .getContent()
            .orElse(null)
    );
  }
}
