/**
 * Tool which lists the files in the workspace.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.List;
import java.util.Objects;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.workspace.AssistantWorkspace;

/**
 * Lists the files available in the assistant's workspace.
 */
public class ListFilesTool implements AssistantToolStrategy {

  /**
   * The name by which the tool is offered to the model.
   */
  public static final String NAME = "list_files";

  private final AssistantWorkspace workspace;

  /**
   * Constructs the tool over a workspace.
   *
   * @param workspace The workspace whose files are listed.
   */
  public ListFilesTool(AssistantWorkspace workspace) {
    this.workspace = Objects.requireNonNull(workspace, "workspace");
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "List the files in the workspace";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject schema = new JsonObject();
    schema.put("type", "object");
    schema.put("properties", new JsonObject());
    return schema;
  }

  @Override
  public void execute(JsonObject call, AssistantActionCallback callback) {
    List<String> files = workspace.listFiles();
    String output = files.isEmpty() ? "No files in the workspace." : String.join("\n", files);
    callback.onActionComplete(AssistantResultBuilder.forToolCall(output).build());
  }
}
