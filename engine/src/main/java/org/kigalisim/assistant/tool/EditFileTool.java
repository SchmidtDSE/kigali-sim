/**
 * Tool which applies a unified diff to a workspace file.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.workspace.AssistantWorkspace;
import org.kigalisim.assistant.workspace.QtaFileTypeStrategy;

/**
 * Applies a standard unified diff to a QubecTalk simulation file.
 *
 * <p>The patch is parsed with {@code UnifiedDiffUtils.parseUnifiedDiff} and applied with
 * {@code Patch.applyTo}. Only string-native {@code .qta} files can be edited for now.</p>
 */
public class EditFileTool implements AssistantToolStrategy {

  /**
   * The name by which the tool is offered to the model.
   */
  public static final String NAME = "edit_file";

  private final AssistantWorkspace workspace;

  /**
   * Constructs the tool over a workspace.
   *
   * @param workspace The workspace holding the file to edit.
   */
  public EditFileTool(AssistantWorkspace workspace) {
    this.workspace = Objects.requireNonNull(workspace, "workspace");
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "Apply a unified diff patch to a QubecTalk simulation file";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject filename = new JsonObject();
    filename.put("type", "string");
    filename.put("description", "The name of the simulation file to edit");

    JsonObject patch = new JsonObject();
    patch.put("type", "string");
    patch.put("description", "A unified diff with --- / +++ headers and @@ hunks");

    JsonObject properties = new JsonObject();
    properties.put("filename", filename);
    properties.put("patch", patch);

    JsonArray required = new JsonArray();
    required.add("filename");
    required.add("patch");

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
    Object rawPatch = call.get("patch");
    if (!(rawPatch instanceof String patch)) {
      callback.onActionComplete(
          AssistantResultBuilder.forToolCallError("Missing required parameter: patch").build());
      return;
    }
    if (!filename.toLowerCase(Locale.ROOT).endsWith(QtaFileTypeStrategy.EXTENSION)) {
      callback.onActionComplete(AssistantResultBuilder.forToolCallError(
          "Only QubecTalk simulation files can be edited; received '" + filename + "'").build());
      return;
    }
    try {
      applyEdit(filename, patch, callback);
    } catch (NoSuchElementException e) {
      callback.onActionComplete(
          AssistantResultBuilder.forToolCallError(e.getMessage()).build());
    } catch (PatchFailedException e) {
      callback.onActionComplete(AssistantResultBuilder.forToolCallError(
          "The patch could not be applied: " + e.getMessage()).build());
    }
  }

  /**
   * Applies the patch and reports the outcome.
   *
   * @param filename The name of the file to edit.
   * @param patch The unified diff to apply.
   * @param callback The callback through which the outcome is reported.
   * @throws NoSuchElementException If the file does not exist.
   * @throws PatchFailedException If the patch does not apply to the current contents.
   */
  private void applyEdit(String filename, String patch, AssistantActionCallback callback)
      throws PatchFailedException {
    String original = workspace.readFile(filename);
    List<String> originalLines = List.of(normalizeNewlines(original).split("\n", -1));
    List<String> patchLines = List.of(normalizeNewlines(patch).split("\n", -1));
    Patch<String> parsed = UnifiedDiffUtils.parseUnifiedDiff(patchLines);
    int hunks = parsed.getDeltas().size();
    if (hunks == 0) {
      callback.onActionComplete(AssistantResultBuilder.forToolCallError(
          "The patch could not be applied: include --- / +++ headers and @@ hunks").build());
      return;
    }
    List<String> revised = parsed.applyTo(originalLines);
    workspace.writeFile(filename, String.join("\n", revised));
    String message = "Applied " + hunks + " hunk(s) to " + filename + " ("
        + revised.size() + " lines).";
    callback.onActionComplete(AssistantResultBuilder.forToolCall(message).build());
  }

  /**
   * Normalizes Windows and legacy line endings to newline characters.
   *
   * @param value The text to normalize.
   * @return The normalized text.
   */
  private String normalizeNewlines(String value) {
    return value.replace("\r\n", "\n").replace('\r', '\n');
  }
}
