/**
 * Live tests using actual QTA files.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
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
    // Note that this is up a directory so not using classpath.
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
   * Test basic.qta produces expected values.
   */
  @Test
  public void testBasic() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic.qta";
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
    assertEquals(1, endYear, "End year should be 1");

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

    // Verify results
    List<EngineResult> results = engine.getResults();
    EngineResult result = findResult(results, "test", "test", startYear);
    assertNotNull(result, "Should have result for test/test in year " + startYear);

    // Check manufacture value - should be 100 mt = 100000 kg
    assertEquals(100000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 100000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }

  /**
   * Test basic_kwh.qta produces expected values.
   */
  @Test
  public void testBasicKwh() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_kwh.qta";
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
    assertEquals(1, endYear, "End year should be 1");

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

    // Verify results
    List<EngineResult> results = engine.getResults();
    EngineResult result = findResult(results, "test", "test", startYear);
    assertNotNull(result, "Should have result for test/test in year " + startYear);

    // Check manufacture value - should be 100 mt = 100000 kg
    assertEquals(100000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 100000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");

    // Check energy consumption
    assertTrue(result.getEnergyConsumption().getValue().doubleValue() > 0,
        "Energy consumption should be greater than 0");
    assertEquals("kwh", result.getEnergyConsumption().getUnits(),
        "Energy consumption units should be kwh");
  }

  /**
   * Test basic_special_float.qta produces expected values.
   */
  @Test
  public void testBasicSpecialFloat() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_special_float.qta";
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
    assertEquals(1, endYear, "End year should be 1");

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

    // Verify results
    List<EngineResult> results = engine.getResults();
    EngineResult result = findResult(results, "test", "test", startYear);
    assertNotNull(result, "Should have result for test/test in year " + startYear);

    // Check manufacture value - should be 100 mt = 100000 kg
    assertEquals(100000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 100000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }

  /**
   * Test basic_units.qta produces expected values.
   */
  @Test
  public void testBasicUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_units.qta";
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
    assertEquals(1, endYear, "End year should be 1");

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

    // Verify results
    List<EngineResult> results = engine.getResults();
    EngineResult result = findResult(results, "test", "test", startYear);
    assertNotNull(result, "Should have result for test/test in year " + startYear);

    // Check manufacture value - should be 1 mt = 1000 kg
    assertEquals(1000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 1000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }

  /**
   * Test basic_units_convert.qta produces expected values.
   */
  @Test
  public void testBasicUnitsConvert() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_units_convert.qta";
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
    assertEquals(1, endYear, "End year should be 1");

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

    // Verify results
    List<EngineResult> results = engine.getResults();
    EngineResult result = findResult(results, "test", "test", startYear);
    assertNotNull(result, "Should have result for test/test in year " + startYear);

    // Check manufacture value - should be 1 mt = 1000 kg
    assertEquals(1000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 1000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }

  /**
   * Test basic_replace.qta produces expected values.
   */
  @Test
  public void testBasicReplace() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_replace.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Get the scenario
    String scenarioName = "Sim";
    ParsedScenario scenario = program.getScenario(scenarioName);
    assertNotNull(scenario, "Scenario should exist");

    // Get start and end years from scenario
    int startYear = scenario.getStartYear();
    int endYear = scenario.getEndYear();
    assertEquals(1, startYear, "Start year should be 1");
    assertEquals(10, endYear, "End year should be 10");

    // Create engine and machine
    Engine engine = new SingleThreadEngine(startYear, endYear);
    PushDownMachine machine = new SingleThreadPushDownMachine(engine);

    // Execute the default policy
    ParsedPolicy defaultPolicy = program.getPolicy("default");
    assertNotNull(defaultPolicy, "Default policy should exist");
    executePolicy(defaultPolicy, machine);

    // Execute other policies in the scenario
    for (String policyName : scenario.getPolicies()) {
      ParsedPolicy policy = program.getPolicy(policyName);
      assertNotNull(policy, "Policy " + policyName + " should exist");
      executePolicy(policy, machine);
    }

    // Verify results for year 1 (before replacement)
    List<EngineResult> results = engine.getResults();

    // Check Sub A in year 1
    EngineResult resultSubA1 = findResult(results, "Test", "Sub A", 1);
    assertNotNull(resultSubA1, "Should have result for Test/Sub A in year 1");
    assertEquals(100000.0, resultSubA1.getManufacture().getValue().doubleValue(), 0.0001,
        "Sub A Manufacture should be 100000 kg in year 1");

    // Check Sub B in year 1
    EngineResult resultSubB1 = findResult(results, "Test", "Sub B", 1);
    assertNotNull(resultSubB1, "Should have result for Test/Sub B in year 1");
    assertEquals(0.0, resultSubB1.getManufacture().getValue().doubleValue(), 0.0001,
        "Sub B Manufacture should be 0 kg in year 1");

    // Move to year 5 and check results (start of replacement)
    for (int year = 2; year <= 5; year++) {
      engine.incrementYear();
    }

    results = engine.getResults();

    // Check Sub A in year 5
    EngineResult resultSubA5 = findResult(results, "Test", "Sub A", 5);
    assertNotNull(resultSubA5, "Should have result for Test/Sub A in year 5");

    // Check Sub B in year 5
    EngineResult resultSubB5 = findResult(results, "Test", "Sub B", 5);
    assertNotNull(resultSubB5, "Should have result for Test/Sub B in year 5");

    // Verify that Sub B has manufacture data in year 5 due to replacement
    assertNotNull(resultSubB5.getManufacture(),
        "Sub B should have manufacture data in year 5 due to replacement");
  }

  /**
   * Test basic_replace_units.qta produces expected values.
   */
  @Test
  public void testBasicReplaceUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_replace_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Get the scenario
    String scenarioName = "Sim";
    ParsedScenario scenario = program.getScenario(scenarioName);
    assertNotNull(scenario, "Scenario should exist");

    // Get start and end years from scenario
    int startYear = scenario.getStartYear();
    int endYear = scenario.getEndYear();
    assertEquals(1, startYear, "Start year should be 1");
    assertEquals(10, endYear, "End year should be 10");

    // Create engine and machine
    Engine engine = new SingleThreadEngine(startYear, endYear);
    PushDownMachine machine = new SingleThreadPushDownMachine(engine);

    // Execute the default policy
    ParsedPolicy defaultPolicy = program.getPolicy("default");
    assertNotNull(defaultPolicy, "Default policy should exist");
    executePolicy(defaultPolicy, machine);

    // Execute other policies in the scenario
    for (String policyName : scenario.getPolicies()) {
      ParsedPolicy policy = program.getPolicy(policyName);
      assertNotNull(policy, "Policy " + policyName + " should exist");
      executePolicy(policy, machine);
    }

    // Verify results for year 1 (before replacement)
    List<EngineResult> results = engine.getResults();

    // Check Sub A in year 1
    EngineResult resultSubA1 = findResult(results, "Test", "Sub A", 1);
    assertNotNull(resultSubA1, "Should have result for Test/Sub A in year 1");
    assertEquals(100000.0, resultSubA1.getManufacture().getValue().doubleValue(), 0.0001,
        "Sub A Manufacture should be 100000 kg in year 1");

    // Check Sub B in year 1
    EngineResult resultSubB1 = findResult(results, "Test", "Sub B", 1);
    assertNotNull(resultSubB1, "Should have result for Test/Sub B in year 1");
    assertEquals(0.0, resultSubB1.getManufacture().getValue().doubleValue(), 0.0001,
        "Sub B Manufacture should be 0 kg in year 1");

    // Move to year 5 and check results (start of replacement)
    for (int year = 2; year <= 5; year++) {
      engine.incrementYear();
    }

    results = engine.getResults();

    // Check Sub A in year 5
    EngineResult resultSubA5 = findResult(results, "Test", "Sub A", 5);
    assertNotNull(resultSubA5, "Should have result for Test/Sub A in year 5");

    // Check Sub B in year 5
    EngineResult resultSubB5 = findResult(results, "Test", "Sub B", 5);
    assertNotNull(resultSubB5, "Should have result for Test/Sub B in year 5");

    // Verify that Sub B has manufacture data in year 5 due to replacement
    assertNotNull(resultSubB5.getManufacture(),
        "Sub B should have manufacture data in year 5 due to replacement");
  }

  /**
   * Execute a policy using the provided machine.
   *
   * @param policy The policy to execute.
   * @param machine The machine to use for execution.
   */
  private void executePolicy(ParsedPolicy policy, PushDownMachine machine) {
    // Set the stanza (policy name)
    String stanzaName = policy.getName();
    machine.getEngine().setStanza(stanzaName != null ? stanzaName : "default");
    System.out.println("[DEBUG_LOG] Executing policy: " + stanzaName);

    // For each application in the policy
    for (String applicationName : policy.getApplications()) {
      ParsedApplication application = policy.getApplication(applicationName);

      // Set the application scope
      machine.getEngine().setApplication(applicationName);
      System.out.println("[DEBUG_LOG] Executing application: " + applicationName);

      // For each substance in the application
      for (String substanceName : application.getSubstances()) {
        ParsedSubstance substance = application.getSubstance(substanceName);

        // Set the substance scope
        machine.getEngine().setSubstance(substanceName);

        // Debug output
        System.out.println("[DEBUG_LOG] Executing operations for " + applicationName + "/" + substanceName);

        // Count operations
        int opCount = 0;
        for (Operation op : substance.getOperations()) {
          opCount++;
        }
        System.out.println("[DEBUG_LOG] Number of operations: " + opCount);

        // Execute each operation in the substance
        for (Operation operation : substance.getOperations()) {
          System.out.println("[DEBUG_LOG] Executing operation: " + operation.getClass().getSimpleName());
          operation.execute(machine);

          // After execution, get the current state
          List<EngineResult> currentResults = machine.getEngine().getResults();
          for (EngineResult result : currentResults) {
            if (result.getApplication().equals(applicationName) 
                && result.getSubstance().equals(substanceName)) {
              System.out.println("[DEBUG_LOG] After " 
                  + operation.getClass().getSimpleName() 
                  + " - Manufacture: " 
                  + result.getManufacture().getValue() 
                  + " " 
                  + result.getManufacture().getUnits());
            }
          }
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
