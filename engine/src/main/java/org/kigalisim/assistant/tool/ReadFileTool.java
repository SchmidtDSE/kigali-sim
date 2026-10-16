/**
 * Tool which reads a workspace file.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.workspace.AssistantWorkspace;

/**
 * Reads an entire file or one section of it.
 */
public class ReadFileTool implements AssistantToolStrategy {

  /**
   * The name by which the tool is offered to the model.
   */
  public static final String NAME = "read_file";

  private final AssistantWorkspace workspace;

  /**
   * Constructs the tool over a workspace.
   *
   * @param workspace The workspace holding the file to read.
   */
  public ReadFileTool(AssistantWorkspace workspace) {
    this.workspace = Objects.requireNonNull(workspace, "workspace");
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "Read a whole file or one named section of a file";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject filename = new JsonObject();
    filename.put("type", "string");
    filename.put("description", "The name of the file to read");

    JsonObject section = new JsonObject();
    section.put("type", "string");
    section.put("description", "The optional section to read; the whole file when omitted");

    JsonObject properties = new JsonObject();
    properties.put("filename", filename);
    properties.put("section", section);

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
    Object rawFilename = call.get("filename");
    if (!(rawFilename instanceof String filename) || filename.isBlank()) {
      callback.onActionComplete(
          AssistantResultBuilder.forToolCallError("Missing required parameter: filename").build());
      return;
    }
    Object rawSection = call.get("section");
    String section = rawSection instanceof String value ? value : null;
    try {
      String output = section == null || section.isBlank()
          ? workspace.readFile(filename)
          : workspace.readFile(filename, section);
      callback.onActionComplete(AssistantResultBuilder.forToolCall(output).build());
    } catch (NoSuchElementException e) {
      callback.onActionComplete(
          AssistantResultBuilder.forToolCallError(e.getMessage()).build());
    }
  }
}
