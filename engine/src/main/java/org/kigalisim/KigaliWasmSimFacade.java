/**
 * Facade which makes exports available to JS clients.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.parse.ParseResult;
import org.kigalisim.lang.program.ParsedProgram;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;

/**
 * Facade which offers access to JS clients.
 *
 * <p>Entry point for the KigaliSim command line application which can run simulations from within
 * the browser via TeaVM.</p>
 */
public class KigaliWasmSimFacade {

  /**
   * Constructs a new KigaliWasmSimFacade.
   */
  public KigaliWasmSimFacade() {
  }

  /**
   * Returns the version of KigaliSim.
   *
   * @return The version string "0.1.1".
   */
  @JSExport
  public static String getVersion() {
    return "0.1.1";
  }

  /**
   * Reports progress to JavaScript by calling the global reportProgress function.
   *
   * @param progress The progress value between 0.0 and 1.0.
   */
  @JSBody(params = {"progress"}, script = "if (typeof reportProgress === 'function') { reportProgress(progress); }")
  private static native void reportProgressToJavaScript(double progress);

  /**
   * Executes all scenarios in the provided QubecTalk code and returns the results.
   *
   * <div>This method parses and executes the provided QubecTalk code, running all scenarios defined
   * in the code. It returns a formatted string containing:
   * 
   * <ul>
   *   <li>
   * A description of the execution result: either "OK" for success or "Error: message" for failure.
   *   </li>
   *   <li>A blank line</li>
   *   <li>The CSV contents with the simulation results</li>
   * </ul>
   * 
   * </div>
   *
   * @param code The QubecTalk code to execute.
   * @return A formatted string with execution status and CSV results.
   */
  @JSExport
  public static String execute(String code) {
    try {
      // Parse the code
      ParseResult parseResult = KigaliSimFacade.parse(code);

      if (parseResult.hasErrors()) {
        String detailedError = KigaliSimFacade.getDetailedErrorMessage(parseResult);
        return "Error: " + detailedError + "\n\n";
      }

      // Interpret the parsed code
      ParsedProgram program = KigaliSimFacade.interpret(parseResult);

      List<EngineResult> allResults = new ArrayList<>();

      // Create progress callback that reports to JavaScript
      ProgressReportCallback progressCallback = progress -> reportProgressToJavaScript(progress);

      // Run all scenarios
      for (String scenarioName : program.getScenarios()) {
        List<EngineResult> scenarioResults = KigaliSimFacade.runScenario(program, scenarioName, progressCallback)
            .collect(Collectors.toList());
        allResults.addAll(scenarioResults);
      }

      // Convert results to CSV
      String csvResults = KigaliSimFacade.convertResultsToCsv(allResults);

      // Return success message followed by CSV results
      return "OK\n\n" + csvResults;
    } catch (Exception e) {
      // Return error message
      return "Error: " + e.getMessage() + "\n\n";
    }
  }

  /**
   * Executes a single scenario from the provided QubecTalk code.
   *
   * @param code The QubecTalk code to execute.
   * @param scenarioName The name of the specific scenario to execute.
   * @return A formatted string with execution status and CSV results for the single scenario.
   */
  @JSExport
  public static String executeScenario(String code, String scenarioName) {
    try {
      // Parse the code
      ParseResult parseResult = KigaliSimFacade.parse(code);

      if (parseResult.hasErrors()) {
        String detailedError = KigaliSimFacade.getDetailedErrorMessage(parseResult);
        return "Error: " + detailedError + "\n\n";
      }

      // Interpret the parsed code
      ParsedProgram program = KigaliSimFacade.interpret(parseResult);

      // Verify scenario exists
      if (!program.getScenarios().contains(scenarioName)) {
        return "Error: Scenario not found: " + scenarioName + "\n\n";
      }

      // Create progress callback that reports to JavaScript
      ProgressReportCallback progressCallback = progress -> reportProgressToJavaScript(progress);

      // Run the specific scenario
      List<EngineResult> scenarioResults = KigaliSimFacade.runScenario(
          program, scenarioName, progressCallback)
          .collect(Collectors.toList());

      // Convert results to CSV
      String csvResults = KigaliSimFacade.convertResultsToCsv(scenarioResults);

      // Return success message followed by CSV results
      return "OK\n\n" + csvResults;
    } catch (Exception e) {
      // Return error message
      return "Error: " + e.getMessage() + "\n\n";
    }
  }

  /**
   * Required entrypoint for wasm.
   *
   * @param args ignored arguments
   */
  public static void main(String[] args) {}
}
