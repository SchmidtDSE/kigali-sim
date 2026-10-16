/**
 * Tool which outlines the sections of a workspace file.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.workspace.AssistantWorkspace;

/**
 * Lists the sections which can be requested from a workspace file.
 */
public class OutlineFileTool implements AssistantToolStrategy {

  /**
   * The name by which the tool is offered to the model.
   */
  public static final String NAME = "outline_file";

  private final AssistantWorkspace workspace;

  /**
   * Constructs the tool over a workspace.
   *
   * @param workspace The workspace holding the file to outline.
   */
  public OutlineFileTool(AssistantWorkspace workspace) {
    this.workspace = Objects.requireNonNull(workspace, "workspace");
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "List the sections of a file so they can be read individually";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject filename = new JsonObject();
    filename.put("type", "string");
    filename.put("description", "The name of the file to outline");

    JsonObject properties = new JsonObject();
    properties.put("filename", filename);

    JsonArray required = new JsonArray();
    required.add("filename");

    JsonObject schema = new JsonObject();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", required);
    return schema;
  }

  @Override
  public void execute(JsonObject call, AssistantActionCallback callback) {
    Object raw = call.get("filename");
    if (!(raw instanceof String filename) || filename.isBlank()) {
      callback.onActionComplete(
          AssistantResultBuilder.forToolCallError("Missing required parameter: filename").build());
      return;
    }
    try {
      List<String> sections = workspace.outlineFile(filename);
      callback.onActionComplete(
          AssistantResultBuilder.forToolCall(String.join("\n", sections)).build());
    } catch (NoSuchElementException e) {
      callback.onActionComplete(
          AssistantResultBuilder.forToolCallError(e.getMessage()).build());
    }
  }
}
