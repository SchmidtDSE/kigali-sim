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
import org.kigalisim.ProgressReportCallback;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResult;
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

  private static final String NO_SCENARIOS_TEMPLATE =
      "Simulation '%s' defines no scenarios to run";
  private static final String RUN_FAILED_TEMPLATE = "Simulation run failed: %s";
  private static final String TRUNCATION_PREFIX = "... [truncated: ";
  private static final String TRUNCATION_SUFFIX = " characters omitted]";
  private static final String YEAR_RANGE_TEMPLATE =
      "start_year must not be after end_year; received %d and %d";
  private static final String YEAR_TYPE_TEMPLATE =
      "Parameter '%s' must be a whole number";
  private static final ProgressReportCallback NO_OP_PROGRESS = progress -> {
    // Progress reporting is not surfaced by the assistant tools.
  };

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
    return "Run a QubecTalk simulation file and return the results as CSV text, optionally "
        + "limited to a start and end year";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject filename = new JsonObject();
    filename.put("type", "string");
    filename.put("description", "The name of the QubecTalk simulation file to run");

    JsonObject startYear = new JsonObject();
    startYear.put("type", "integer");
    startYear.put("description", "Optional first simulation year to include in the returned CSV");

    JsonObject endYear = new JsonObject();
    endYear.put("type", "integer");
    endYear.put("description", "Optional last simulation year to include in the returned CSV");

    JsonObject properties = new JsonObject();
    properties.put("filename", filename);
    properties.put("start_year", startYear);
    properties.put("end_year", endYear);

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

    final Integer startYear;
    final Integer endYear;
    try {
      startYear = getOptionalYear(call, "start_year");
      endYear = getOptionalYear(call, "end_year");
    } catch (IllegalArgumentException e) {
      AssistantResult result = AssistantResultBuilder.forToolCallError(e.getMessage()).build();
      callback.onActionComplete(result);
      return;
    }
    if (startYear != null && endYear != null && startYear > endYear) {
      String message = String.format(YEAR_RANGE_TEMPLATE, startYear, endYear);
      AssistantResult result = AssistantResultBuilder.forToolCallError(message).build();
      callback.onActionComplete(result);
      return;
    }

    Optional<ParsedProgram> loaded = QtaSimulationSupport.load(workspace, filename, callback);
    if (loaded.isEmpty()) {
      return;
    }
    ParsedProgram program = loaded.get();

    if (program.getScenarios().isEmpty()) {
      String message = String.format(NO_SCENARIOS_TEMPLATE, filename);
      AssistantResult result = AssistantResultBuilder.forToolCallError(message).build();
      callback.onActionComplete(result);
      return;
    }

    final String csv;
    try {
      List<EngineResult> allResults = new ArrayList<>();
      for (String scenarioName : program.getScenarios()) {
        List<EngineResult> scenarioResults =
            KigaliSimFacade.runScenario(program, scenarioName, NO_OP_PROGRESS)
                .collect(Collectors.toList());
        allResults.addAll(scenarioResults);
      }
      List<EngineResult> filtered = filterByYear(allResults, startYear, endYear);
      csv = KigaliSimFacade.convertResultsToCsv(filtered);
    } catch (RuntimeException e) {
      String message = String.format(RUN_FAILED_TEMPLATE, QtaSimulationSupport.getMessageOf(e));
      AssistantResult result = AssistantResultBuilder.forToolCallError(message).build();
      callback.onActionComplete(result);
      return;
    }

    String truncated = truncateCsv(csv, maxCsvCharacters);
    AssistantResult result = AssistantResultBuilder.forToolCall(truncated).build();
    callback.onActionComplete(result);
  }

  /**
   * Reads an optional whole-number parameter.
   *
   * @param call The tool call arguments.
   * @param name The name of the parameter.
   * @return The value, or null when the parameter is absent.
   * @throws IllegalArgumentException If the parameter is present but not a whole number.
   */
  private static Integer getOptionalYear(JsonObject call, String name) {
    Object raw = call.get(name);
    if (raw == null) {
      return null;
    }
    if (raw instanceof Number number) {
      return number.intValue();
    }
    if (raw instanceof String text && !text.isBlank()) {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException e) {
        String message = String.format(YEAR_TYPE_TEMPLATE, name);
        throw new IllegalArgumentException(message);
      }
    }
    String message = String.format(YEAR_TYPE_TEMPLATE, name);
    throw new IllegalArgumentException(message);
  }

  /**
   * Filters results to the requested inclusive year range.
   *
   * @param results The results to filter.
   * @param startYear The first year to include, or null for no lower bound.
   * @param endYear The last year to include, or null for no upper bound.
   * @return The results within the range, or the original list when no bounds were given.
   */
  private static List<EngineResult> filterByYear(List<EngineResult> results, Integer startYear,
      Integer endYear) {
    if (startYear == null && endYear == null) {
      return results;
    }
    List<EngineResult> filtered = new ArrayList<>();
    for (EngineResult result : results) {
      int year = result.getYear();
      boolean isAfterStart = startYear == null || year >= startYear;
      boolean isBeforeEnd = endYear == null || year <= endYear;
      if (isAfterStart && isBeforeEnd) {
        filtered.add(result);
      }
    }
    return filtered;
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
