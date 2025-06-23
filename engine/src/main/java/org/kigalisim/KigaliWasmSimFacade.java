/**
 * Facade which makes exports available to JS clients.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.interpret.QubecTalkInterpreter;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;
import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.parse.ParseResult;
import org.kigalisim.lang.parse.QubecTalkParser;
import org.kigalisim.lang.program.ParsedApplication;
import org.kigalisim.lang.program.ParsedPolicy;
import org.kigalisim.lang.program.ParsedProgram;
import org.kigalisim.lang.program.ParsedScenario;
import org.kigalisim.lang.program.ParsedSubstance;
import org.kigalisim.util.EmulatedStringJoiner;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;


/**
 * Facade which offers access to JS clients.
 *
 * <p>Entry point for the KigaliSim command line application which can run simulations from within
 * the browser.</p>
 */
public class KigaliWasmSimFacade {

  private static final String DEFAULT_STANZA = "default";

  /**
   * Returns the version of KigaliSim.
   *
   * @return The version string "0.0.1".
   */
  @JSExport
  public static String getVersion() {
    return "0.0.1";
  }

  /**
   * Validates QubecTalk code.
   *
   * <p>Parse and interpret QubecTalk code to check for syntax errors.</p>
   *
   * @param code The QubecTalk code to validate.
   * @return An empty string if the code is valid, or an error message if not.
   */
  @JSExport
  public static String validate(String code) {
    try {
      ParseResult parseResult = parse(code);

      if (parseResult.hasErrors()) {
        return "Validation failed: Parse errors detected";
      }

      interpret(parseResult);
      return "";
    } catch (Exception e) {
      return "Validation failed: " + e.getMessage();
    }
  }

  /**
   * Executes all scenarios in the provided QubecTalk code and returns the results as CSV.
   *
   * @param code The QubecTalk code to execute.
   * @return CSV string containing the simulation results.
   */
  @JSExport
  public static String execute(String code) {
    try {
      ParseResult parseResult = parse(code);

      if (parseResult.hasErrors()) {
        reportError("Parse errors detected");
        return "";
      }

      ParsedProgram program = interpret(parseResult);

      List<EngineResult> allResults = new ArrayList<>();

      // Run all scenarios
      for (String scenarioName : program.getScenarios()) {
        List<EngineResult> scenarioResults = runScenario(program, scenarioName)
            .collect(Collectors.toList());
        allResults.addAll(scenarioResults);
      }

      // Convert results to CSV
      return convertResultsToCsv(allResults);
    } catch (Exception e) {
      reportError(e.getMessage());
      return "";
    }
  }

  /**
   * Parse QubecTalk code.
   *
   * @param code String code to parse as QubecTalk source.
   * @return The parse result containing either the parse tree or errors.
   */
  private static ParseResult parse(String code) {
    QubecTalkParser parser = new QubecTalkParser();
    return parser.parse(code);
  }

  /**
   * Interpret a parsed QubecTalk script to Java objects which can run the simulation.
   *
   * @param parseResult The parse result from parsing the QubecTalk source.
   * @return The parsed program which can be used to run a specific simulation.
   */
  private static ParsedProgram interpret(ParseResult parseResult) {
    QubecTalkInterpreter interpreter = new QubecTalkInterpreter();
    return interpreter.interpret(parseResult);
  }

  /**
   * Run a scenario from the provided program and return results.
   *
   * @param program The parsed program containing the simulation to run.
   * @param scenarioName The name of the simulation to execute from the program.
   * @return Stream of EngineResult objects containing the simulation results
   */
  private static java.util.stream.Stream<EngineResult> runScenario(
      ParsedProgram program, String scenarioName) {
    // Get the scenario from the program
    if (!program.getScenarios().contains(scenarioName)) {
      throw new IllegalArgumentException("Scenario not found: " + scenarioName);
    }

    // Get the scenario
    ParsedScenario scenario = program.getScenario(scenarioName);

    // Run trials
    int numTrials = scenario.getTrials();
    java.util.stream.Stream<EngineResult> results = java.util.stream.Stream.empty();
    for (int i = 0; i < numTrials; i++) {
      results = java.util.stream.Stream.concat(results, runTrial(program, scenario, i + 1));
      reportProgress(i + 1, numTrials);
    }
    return results;
  }

  /**
   * Run a single trial for a single scenario.
   *
   * @param program The parsed program containing the simulation to run.
   * @param scenario The scenario to execute in a single trial.
   * @param trialNumber The trial number for this run.
   * @return Stream of EngineResult objects containing the simulation results
   */
  private static java.util.stream.Stream<EngineResult> runTrial(
      ParsedProgram program, ParsedScenario scenario, int trialNumber) {
    // Get startYear and endYear from ParsedScenario
    int startYear = scenario.getStartYear();
    int endYear = scenario.getEndYear();

    // Create the engine and machine
    Engine engine = new SingleThreadEngine(startYear, endYear);

    // Set scenario name and trial number
    engine.setScenarioName(scenario.getName());
    engine.setTrialNumber(trialNumber);

    PushDownMachine machine = new SingleThreadPushDownMachine(engine);

    // Store results as we iterate through years
    List<EngineResult> results = new ArrayList<>();

    // Run simulation through all years
    while (!engine.getIsDone()) {
      // Execute the default policy first
      ParsedPolicy defaultPolicy = program.getPolicy(DEFAULT_STANZA);
      executePolicy(defaultPolicy, machine);

      // Execute the other named policies in the scenario
      for (String policyName : scenario.getPolicies()) {
        ParsedPolicy policy = program.getPolicy(policyName);
        executePolicy(policy, machine);
      }

      // Collect results for this year using the engine's built-in method
      List<EngineResult> yearResults = engine.getResults();
      results.addAll(yearResults);

      // Increment to the next year
      engine.incrementYear();
    }

    return results.stream();
  }

  /**
   * Execute a policy using the provided machine.
   *
   * @param policy The policy to execute.
   * @param machine The machine to use for execution.
   */
  private static void executePolicy(ParsedPolicy policy, PushDownMachine machine) {
    // Set the stanza (policy name)
    String stanzaName = policy.getName();
    machine.getEngine().setStanza(stanzaName);

    // For each application in the policy
    for (String applicationName : policy.getApplications()) {
      ParsedApplication application = policy.getApplication(applicationName);

      // Set the application scope
      machine.getEngine().setApplication(applicationName);

      // For each substance in the application
      for (String substanceName : application.getSubstances()) {
        ParsedSubstance substance = application.getSubstance(substanceName);

        // Set the substance scope
        machine.getEngine().setSubstance(substanceName);

        // Execute each operation in the substance
        for (Operation operation : substance.getOperations()) {
          try {
            operation.execute(machine);
          } catch (Exception e) {
            reportError("Error executing operation: " + e.getMessage());
            throw e;
          }
        }
      }
    }
  }

  /**
   * Convert a list of EngineResult objects to a CSV string.
   *
   * @param results The list of EngineResult objects to convert.
   * @return A CSV string representation of the results.
   */
  private static String convertResultsToCsv(List<EngineResult> results) {
    if (results.isEmpty()) {
      return "";
    }

    // Create header row
    EmulatedStringJoiner headerJoiner = new EmulatedStringJoiner(",");
    headerJoiner.add("scenario");
    headerJoiner.add("trial");
    headerJoiner.add("year");
    headerJoiner.add("application");
    headerJoiner.add("substance");
    headerJoiner.add("manufacture");
    headerJoiner.add("import");
    headerJoiner.add("recycle");
    headerJoiner.add("domesticConsumption");
    headerJoiner.add("importConsumption");
    headerJoiner.add("recycleConsumption");
    headerJoiner.add("population");
    headerJoiner.add("populationNew");
    headerJoiner.add("rechargeEmissions");
    headerJoiner.add("eolEmissions");
    headerJoiner.add("energyConsumption");

    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append(headerJoiner.toString()).append("\n");

    // Add data rows
    for (EngineResult result : results) {
      EmulatedStringJoiner rowJoiner = new EmulatedStringJoiner(",");
      rowJoiner.add(result.getScenarioName());
      rowJoiner.add(String.valueOf(result.getTrialNumber()));
      rowJoiner.add(String.valueOf(result.getYear()));
      rowJoiner.add(result.getApplication());
      rowJoiner.add(result.getSubstance());
      rowJoiner.add(result.getManufacture().getValue().toString());
      rowJoiner.add(result.getImport().getValue().toString());
      rowJoiner.add(result.getRecycle().getValue().toString());
      rowJoiner.add(result.getDomesticConsumption().getValue().toString());
      rowJoiner.add(result.getImportConsumption().getValue().toString());
      rowJoiner.add(result.getRecycleConsumption().getValue().toString());
      rowJoiner.add(result.getPopulation().getValue().toString());
      rowJoiner.add(result.getPopulationNew().getValue().toString());
      rowJoiner.add(result.getRechargeEmissions().getValue().toString());
      rowJoiner.add(result.getEolEmissions().getValue().toString());
      rowJoiner.add(result.getEnergyConsumption().getValue().toString());

      csvBuilder.append(rowJoiner.toString()).append("\n");
    }

    return csvBuilder.toString();
  }

  /**
   * Report progress of simulation execution.
   *
   * @param current Current trial number.
   * @param total Total number of trials.
   */
  @JSBody(params = { "current", "total" }, script = "if (typeof reportProgress === 'function') { reportProgress(current, total); }")
  private static native void reportProgress(int current, int total);

  /**
   * Report an error from the WASM execution.
   *
   * @param message Error message.
   */
  @JSBody(params = { "message" }, script = "if (typeof reportError === 'function') { reportError(message); }")
  private static native void reportError(String message);

  /**
   * Required entrypoint for wasm.
   *
   * @param args ignored arguments
   */
  public static void main(String[] args) {}
}
