/**
 * Tool which runs a QubecTalk simulation file.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.workspace.AssistantWorkspace;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Runs QubecTalk simulation files held in the assistant workspace.
 *
 * <p>Executes every scenario in the program, combines the results into a single CSV table, and
 * returns that text to the model. Output larger than the transport cap is truncated on a newline
 * boundary with an omission marker.</p>
 */
public class RunSimulationTool implements AssistantToolStrategy {

  /**
   * The name by which the tool is offered to the model.
   */
  public static final String NAME = "run_simulation";

  /**
   * The maximum number of CSV characters returned to the model before truncation.
   */
  static final int DEFAULT_MAX_CSV_CHARACTERS = 100_000;

  private static final String TRUNCATION_PREFIX = "... [truncated: ";
  private static final String TRUNCATION_SUFFIX = " characters omitted]";

  private final AssistantWorkspace workspace;
  private final int maxCsvCharacters;

  /**
   * Constructs the tool over a workspace.
   *
   * @param workspace The workspace holding the simulation to run.
   */
  public RunSimulationTool(AssistantWorkspace workspace) {
    this(workspace, DEFAULT_MAX_CSV_CHARACTERS);
  }

  /**
   * Constructs the tool over a workspace with a custom CSV transport cap.
   *
   * @param workspace The workspace holding the simulation to run.
   * @param maxCsvCharacters The maximum number of CSV characters to return.
   */
  RunSimulationTool(AssistantWorkspace workspace, int maxCsvCharacters) {
    this.workspace = Objects.requireNonNull(workspace, "workspace");
    this.maxCsvCharacters = maxCsvCharacters;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "Run a QubecTalk simulation file and return the results as CSV text";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject filename = new JsonObject();
    filename.put("type", "string");
    filename.put("description", "The name of the QubecTalk simulation file to run");

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
    Object rawFilename = call.get("filename");
    if (!(rawFilename instanceof String filename) || filename.isBlank()) {
      callback.onActionComplete(
          AssistantResultBuilder.forToolCallError("Missing required parameter: filename").build());
      return;
    }

    Optional<ParsedProgram> loaded = QtaSimulationSupport.load(workspace, filename, callback);
    if (loaded.isEmpty()) {
      return;
    }
    ParsedProgram program = loaded.get();

    if (program.getScenarios().isEmpty()) {
      callback.onActionComplete(AssistantResultBuilder.forToolCallError(
          "Simulation '" + filename + "' defines no scenarios to run").build());
      return;
    }

    final String csv;
    try {
      List<EngineResult> allResults = new ArrayList<>();
      for (String scenarioName : program.getScenarios()) {
        allResults.addAll(KigaliSimFacade.runScenario(program, scenarioName, progress -> {})
            .collect(Collectors.toList()));
      }
      csv = KigaliSimFacade.convertResultsToCsv(allResults);
    } catch (RuntimeException e) {
      callback.onActionComplete(AssistantResultBuilder.forToolCallError(
          "Simulation run failed: " + QtaSimulationSupport.messageOf(e)).build());
      return;
    }

    callback.onActionComplete(
        AssistantResultBuilder.forToolCall(truncateCsv(csv, maxCsvCharacters)).build());
  }

  /**
   * Truncates CSV output on a newline boundary at or before the transport cap.
   *
   * @param csv The full CSV text.
   * @param maxCharacters The maximum number of characters to retain.
   * @return The original CSV when it fits, otherwise a prefix ending in an omission marker.
   */
  private static String truncateCsv(String csv, int maxCharacters) {
    if (csv.length() <= maxCharacters) {
      return csv;
    }
    int cut = csv.lastIndexOf('\n', maxCharacters - 1);
    final String retained;
    if (cut < 0) {
      retained = "";
    } else {
      retained = csv.substring(0, cut + 1);
    }
    int omitted = csv.length() - retained.length();
    return retained + TRUNCATION_PREFIX + omitted + TRUNCATION_SUFFIX;
  }
}
