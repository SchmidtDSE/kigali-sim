/**
 * Facade for interacting with the Kigali platform via code.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
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

/**
 * Entry point into the Kigali platform when used as a library.
 *
 * <p>Facade which helps facilitate common operations within the Kigali simulation platform when used
 * as a library as opposed to as an interactive / command-line tool.</p>
 */
public class KigaliSimFacade {

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
   * Run a scenario from the provided program.
   *
   * <p>Creates and executes a simulation using the provided program and simulation name where this
   * name refers to a scenario indicating the set of policies to be stacked.</p>
   *
   * @param program The parsed program containing the simulation to run.
   * @param scenarioName The name of the simulation to execute from the program.
   */
  public static void runScenario(ParsedProgram program, String scenarioName) {
    // Get the scenario from the program
    if (!program.getScenarios().contains(scenarioName)) {
      throw new IllegalArgumentException("Scenario not found: " + scenarioName);
    }

    // Get the scenario
    ParsedScenario scenario = program.getScenario(scenarioName);

    // Get startYear and endYear from ParsedScenario
    int startYear = scenario.getStartYear();
    int endYear = scenario.getEndYear();

    // Create the engine and machine
    Engine engine = new SingleThreadEngine(startYear, endYear);
    PushDownMachine machine = new SingleThreadPushDownMachine(engine);

    // Execute the default policy first
    ParsedPolicy defaultPolicy = program.getPolicy("default");
    executePolicy(defaultPolicy, machine);

    // Execute the other named policies in the scenario
    for (String policyName : scenario.getPolicies()) {
      ParsedPolicy policy = program.getPolicy(policyName);
      executePolicy(policy, machine);
    }
  }

  /**
   * Run all scenarios from the provided program.
   *
   * <p>Creates and executes simulations for all scenarios in the program.</p>
   *
   * @param program The parsed program containing the simulations to run.
   */
  public static void runAllScenarios(ParsedProgram program) {
    for (String scenarioName : program.getScenarios()) {
      runScenario(program, scenarioName);
    }
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
    // For each application in the policy
    for (String applicationName : policy.getApplications()) {
      ParsedApplication application = policy.getApplication(applicationName);

      // For each substance in the application
      for (String substanceName : application.getSubstances()) {
        ParsedSubstance substance = application.getSubstance(substanceName);

        // Execute each operation in the substance
        for (Operation operation : substance.getOperations()) {
          operation.execute(machine);
        }
      }
    }
  }
}
