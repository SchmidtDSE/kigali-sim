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
import org.kigalisim.assistant.result.AssistantResult;
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

  private static final String ONLY_QTA_TEMPLATE =
      "Only QubecTalk simulation files can be edited; received '%s'";
  private static final String NO_HUNKS_MESSAGE =
      "The patch could not be applied: include --- / +++ headers and @@ hunks";
  private static final String PATCH_FAILED_TEMPLATE = "The patch could not be applied: %s";
  private static final String APPLIED_TEMPLATE = "Applied %d hunk(s) to %s (%d lines).";

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
    if (AssistantToolUtils.isMissing(call, "filename")) {
      callback.onActionComplete(AssistantToolUtils.buildMissingParameterResult("filename"));
      return;
    }
    String filename = AssistantToolUtils.getString(call, "filename");

    String patch = AssistantToolUtils.getString(call, "patch");
    if (patch == null) {
      callback.onActionComplete(AssistantToolUtils.buildMissingParameterResult("patch"));
      return;
    }

    if (!filename.toLowerCase(Locale.ROOT).endsWith(QtaFileTypeStrategy.EXTENSION)) {
      String message = String.format(ONLY_QTA_TEMPLATE, filename);
      AssistantResult result = AssistantResultBuilder.forToolCallError(message).build();
      callback.onActionComplete(result);
      return;
    }
    try {
      applyEdit(filename, patch, callback);
    } catch (NoSuchElementException e) {
      AssistantResult result = AssistantResultBuilder.forToolCallError(e.getMessage()).build();
      callback.onActionComplete(result);
    } catch (PatchFailedException e) {
      String message = String.format(PATCH_FAILED_TEMPLATE, e.getMessage());
      AssistantResult result = AssistantResultBuilder.forToolCallError(message).build();
      callback.onActionComplete(result);
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
      AssistantResult result =
          AssistantResultBuilder.forToolCallError(NO_HUNKS_MESSAGE).build();
      callback.onActionComplete(result);
      return;
    }
    List<String> revised = parsed.applyTo(originalLines);
    workspace.writeFile(filename, String.join("\n", revised));
    String message = String.format(APPLIED_TEMPLATE, hunks, filename, revised.size());
    AssistantResult result = AssistantResultBuilder.forToolCall(message).build();
    callback.onActionComplete(result);
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
