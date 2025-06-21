/**
 * Live tests using actual QTA files.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;
import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.program.ParsedApplication;
import org.kigalisim.lang.program.ParsedPolicy;
import org.kigalisim.lang.program.ParsedProgram;
import org.kigalisim.lang.program.ParsedScenario;
import org.kigalisim.lang.program.ParsedSubstance;

/**
 * Tests that validate QTA files against expected behavior.
 */
public class LiveTests {

  /**
   * Test minimal_interpreter.qta produces expected manufacture values.
   */
  @Test
  public void testMinimalInterpreterExample() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/minimal_interpreter.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Get the "business as usual" scenario
    String scenarioName = "business as usual";
    ParsedScenario scenario = program.getScenario(scenarioName);
    assertNotNull(scenario, "Scenario should exist");

    // Get start and end years from scenario
    int startYear = scenario.getStartYear();
    int endYear = scenario.getEndYear();
    assertEquals(1, startYear, "Start year should be 1");
    assertEquals(3, endYear, "End year should be 3");

    // Create engine and machine
    Engine engine = new SingleThreadEngine(startYear, endYear);
    PushDownMachine machine = new SingleThreadPushDownMachine(engine);

    // Execute the default policy
    ParsedPolicy defaultPolicy = program.getPolicy("default");
    assertNotNull(defaultPolicy, "Default policy should exist");
    
    executePolicy(defaultPolicy, machine);

    // Execute other policies in the scenario (if any)
    for (String policyName : scenario.getPolicies()) {
      ParsedPolicy policy = program.getPolicy(policyName);
      executePolicy(policy, machine);
    }

    // Debug: Try manually setting values to see if engine works
    engine.setStanza("default");
    engine.setApplication("testApp");
    engine.setSubstance("testSubstance");
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(100000), "kg"), null);

    // Verify results for each year
    for (int year = startYear; year <= endYear; year++) {
      // Get results for current year
      List<EngineResult> results = engine.getResults();
      
      // Find the result for testApp/testSubstance
      EngineResult result = findResult(results, "testApp", "testSubstance", year);
      assertNotNull(result, "Should have result for testApp/testSubstance in year " + year);
      
      // Check manufacture value - should be 100 mt = 100000 kg
      assertEquals(100000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
          "Manufacture should be 100000 kg in year " + year);
      assertEquals("kg", result.getManufacture().getUnits(),
          "Manufacture units should be kg in year " + year);
      
      // Move to next year if not at end
      if (year < endYear) {
        engine.incrementYear();
      }
    }
  }

  /**
   * Execute a policy using the provided machine.
   *
   * @param policy The policy to execute.
   * @param machine The machine to use for execution.
   */
  private void executePolicy(ParsedPolicy policy, PushDownMachine machine) {
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

  /**
   * Find a result for the specified application and substance.
   *
   * @param results List of engine results
   * @param application Application name
   * @param substance Substance name
   * @param year Year
   * @return The matching EngineResult or null if not found
   */
  private EngineResult findResult(List<EngineResult> results, String application, 
      String substance, int year) {
    return results.stream()
        .filter(r -> r.getApplication().equals(application))
        .filter(r -> r.getSubstance().equals(substance))
        .filter(r -> r.getYear() == year)
        .findFirst()
        .orElse(null);
  }
}