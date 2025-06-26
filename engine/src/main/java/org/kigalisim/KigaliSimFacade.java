/**
 * Facade for interacting with the Kigali platform via code.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
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

/**
 * Entry point into the Kigali platform when used as a library.
 *
 * <p>Facade which helps facilitate common operations within the Kigali simulation platform when used
 * as a library as opposed to as an interactive / command-line tool.</p>
 */
public class KigaliSimFacade {

  private static final String DEFAULT_STANZA = "default";

  /**
   * Parse a QubecTalk script.
   *
   * @param code String code to parse as a QubecTalk source.
   * @return The parse result containing either the parse tree or errors.
   */
  public static ParseResult parse(String code) {
    QubecTalkParser parser = new QubecTalkParser();
    return parser.parse(code);
  }

  /**
   * Interpret a parsed QubecTalk script to Java objects which can run the simulation.
   *
   * @param parseResult The parse result from parsing the QubecTalk source.
   * @return The parsed program which can be used to run a specific simulation.
   */
  public static ParsedProgram interpret(ParseResult parseResult) {
    QubecTalkInterpreter interpreter = new QubecTalkInterpreter();
    return interpreter.interpret(parseResult);
  }

  /**
   * Parse and interpret a QubecTalk script from a file.
   *
   * @param filePath The path to the QubecTalk script file.
   * @return The parsed program which can be used to run a specific simulation.
   * @throws IOException If there is an error reading the file.
   */
  public static ParsedProgram parseAndInterpret(String filePath) throws IOException {
    String code = new String(Files.readAllBytes(Paths.get(filePath)));
    ParseResult parseResult = parse(code);
    return interpret(parseResult);
  }

  /**
   * Run a scenario from the provided program and return results.
   *
   * <p>Creates and executes a simulation using the provided program and simulation name where this
   * name refers to a scenario indicating the set of policies to be stacked. The simulation will
   * iterate through all years from the scenario's start year to end year and collect results
   * for all applications and substances for each year.</p>
   *
   * @param program The parsed program containing the simulation to run.
   * @param scenarioName The name of the simulation to execute from the program.
   * @return Stream of EngineResult objects containing the simulation results
   */
  public static Stream<EngineResult> runScenario(ParsedProgram program, String scenarioName) {
    // Get the scenario from the program
    if (!program.getScenarios().contains(scenarioName)) {
      throw new IllegalArgumentException("Scenario not found: " + scenarioName);
    }

    // Get the scenario
    ParsedScenario scenario = program.getScenario(scenarioName);

    // Run trials
    int numTrials = scenario.getTrials();
    Stream<EngineResult> results = Stream.empty();
    for (int i = 0; i < numTrials; i++) {
      results = Stream.concat(results, runTrial(program, scenario, i + 1));
    }
    return results;
  }

  /**
   * Validate a QubecTalk script.
   *
   * <p>Parse and interpret a QubecTalk script to check for syntax errors.</p>
   *
   * @param filePath The path to the QubecTalk script file.
   * @return True if the script is valid, false otherwise.
   */
  public static boolean validate(String filePath) {
    try {
      String code = new String(Files.readAllBytes(Paths.get(filePath)));
      ParseResult parseResult = parse(code);

      if (parseResult.hasErrors()) {
        System.err.println("Validation failed: Parse errors detected");
        return false;
      }

      interpret(parseResult);
      return true;
    } catch (Exception e) {
      // Print the exception for debugging
      System.err.println("Validation failed: " + e.getMessage());
      return false;
    }
  }

  /**
   * Execute a policy using the provided machine.
   *
   * <p>For each application in the policy, for each substance in the application,
   * execute each operation in the substance using the provided machine.</p>
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
            System.err.println("Error executing operation: " + e.getMessage());
            e.printStackTrace();
            throw e;
          }
        }
      }
    }
  }

  /**
   * Run a single trial for a single scenario.
   *
   * @param program The parsed program containing the simulation to run.
   * @param scenario The scenario to execute in a single trial.
   * @param trialNumber The trial number for this run.
   * @return Stream of EngineResult objects containing the simulation results
   */
  private static Stream<EngineResult> runTrial(ParsedProgram program, ParsedScenario scenario, int trialNumber) {
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
   * Convert a list of EngineResult objects to a CSV string.
   *
   * @param results The list of EngineResult objects to convert.
   * @return A CSV string representation of the results.
   */
  public static String convertResultsToCsv(List<EngineResult> results) {
    if (results.isEmpty()) {
      return "";
    }

    // Estimate capacity: header + (rows * estimated_chars_per_row)
    // 13 decimal fields (15 chars each) + strings (~45) + integers (~6) + 19 overhead = ~265 chars per row
    int estimatedCapacity = 300 + (results.size() * 265);
    StringBuilder csvBuilder = new StringBuilder(estimatedCapacity);

    // Create header row - direct append with commas
    csvBuilder.append("scenario,trial,year,application,substance,manufacture,import,recycle,")
              .append("domesticConsumption,importConsumption,recycleConsumption,population,")
              .append("populationNew,rechargeEmissions,eolEmissions,energyConsumption,")
              .append("initialChargeValue,initialChargeConsumption,importNewPopulation\n");

    // Add data rows - direct append with commas
    for (EngineResult result : results) {
      csvBuilder.append(result.getScenarioName()).append(',')
                .append(result.getTrialNumber()).append(',')
                .append(result.getYear()).append(',')
                .append(result.getApplication()).append(',')
                .append(result.getSubstance()).append(',')
                .append(result.getManufacture().getValue()).append(',')
                .append(result.getImport().getValue()).append(',')
                .append(result.getRecycle().getValue()).append(',')
                .append(result.getDomesticConsumption().getValue()).append(',')
                .append(result.getImportConsumption().getValue()).append(',')
                .append(result.getRecycleConsumption().getValue()).append(',')
                .append(result.getPopulation().getValue()).append(',')
                .append(result.getPopulationNew().getValue()).append(',')
                .append(result.getRechargeEmissions().getValue()).append(',')
                .append(result.getEolEmissions().getValue()).append(',')
                .append(result.getEnergyConsumption().getValue()).append(',')
                .append(result.getImportSupplement().getInitialChargeValue().getValue()).append(',')
                .append(result.getImportSupplement().getInitialChargeConsumption().getValue()).append(',')
                .append(result.getImportSupplement().getNewPopulation().getValue()).append('\n');
    }

    return csvBuilder.toString();
  }
}
