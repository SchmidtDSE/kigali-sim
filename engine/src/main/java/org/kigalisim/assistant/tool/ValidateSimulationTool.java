/**
 * Tool which validates a QubecTalk simulation file.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.workspace.AssistantWorkspace;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Validates QubecTalk simulation files held in the assistant workspace.
 *
 * <p>Parses and interprets the file, reporting both syntax and interpretation errors without
 * executing any scenario.</p>
 */
public class ValidateSimulationTool implements AssistantToolStrategy {

  /**
   * The name by which the tool is offered to the model.
   */
  public static final String NAME = "validate_simulation";

  private static final String VALID_TEMPLATE = "Simulation '%s' is valid. %s";

  private final AssistantWorkspace workspace;

  /**
   * Constructs the tool over a workspace.
   *
   * @param workspace The workspace holding the simulation to validate.
   */
  public ValidateSimulationTool(AssistantWorkspace workspace) {
    this.workspace = Objects.requireNonNull(workspace, "workspace");
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "Validate a QubecTalk simulation file, reporting syntax and interpretation errors";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject filename = new JsonObject();
    filename.put("type", "string");
    filename.put("description", "The name of the QubecTalk simulation file to validate");

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
    if (AssistantToolUtils.isMissing(call, "filename")) {
      callback.onActionComplete(AssistantToolUtils.buildMissingParameterResult("filename"));
      return;
    }
    String filename = AssistantToolUtils.getString(call, "filename");

    Optional<ParsedProgram> loaded = QtaSimulationSupport.load(workspace, filename, callback);
    if (loaded.isEmpty()) {
      return;
    }

    List<String> scenarios = new ArrayList<>(loaded.get().getScenarios());
    Collections.sort(scenarios);
    boolean hasScenarios = !scenarios.isEmpty();
    String scenarioSummary;
    if (hasScenarios) {
      scenarioSummary = "Scenarios: " + String.join(", ", scenarios) + ".";
    } else {
      scenarioSummary = "No scenarios are defined.";
    }
    String message = String.format(VALID_TEMPLATE, filename, scenarioSummary);
    AssistantResult result = AssistantResultBuilder.forToolCall(message).build();
    callback.onActionComplete(result);
  }
}
