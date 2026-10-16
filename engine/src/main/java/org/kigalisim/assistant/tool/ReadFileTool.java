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
import org.kigalisim.assistant.result.AssistantResult;
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
    if (AssistantToolUtils.isMissing(call, "filename")) {
      callback.onActionComplete(AssistantToolUtils.buildMissingParameterResult("filename"));
      return;
    }
    String filename = AssistantToolUtils.getString(call, "filename");
    String section = AssistantToolUtils.getString(call, "section");
    boolean isUsingSection = section != null && !section.isBlank();
    try {
      String output;
      if (isUsingSection) {
        output = workspace.readFile(filename, section);
      } else {
        output = workspace.readFile(filename);
      }
      AssistantResult result = AssistantResultBuilder.forToolCall(output).build();
      callback.onActionComplete(result);
    } catch (NoSuchElementException e) {
      AssistantResult result = AssistantResultBuilder.forToolCallError(e.getMessage()).build();
      callback.onActionComplete(result);
    }
  }
}
