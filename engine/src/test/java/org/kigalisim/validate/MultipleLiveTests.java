/**
 * Multiple live tests using actual QTA files with "multiple" prefix.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Tests that validate multiple QTA files against expected behavior.
 */
public class MultipleLiveTests {

  /**
   * Test multiple.qta produces expected values.
   */
  @Test
  public void testMultiple() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/multiple.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Check substance "a"
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "a");
    assertNotNull(resultA, "Should have result for test/a in year 1");

    // Check consumption value for substance "a"
    assertEquals(500.0, resultA.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 500 tCO2e");
    assertEquals("tCO2e", resultA.getGhgConsumption().getUnits(),
        "Consumption units for substance a should be tCO2e");

    // Check manufacture value for substance "a" - should be 100 mt = 100000 kg
    assertEquals(100000.0, resultA.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for substance a should be 100000 kg");
    assertEquals("kg", resultA.getManufacture().getUnits(),
        "Manufacture units for substance a should be kg");
    
    // Check substance "b"
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Check consumption value for substance "b"
    assertEquals(1000.0, resultB.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 1000 tCO2e");
    assertEquals("tCO2e", resultB.getGhgConsumption().getUnits(),
        "Consumption units for substance b should be tCO2e");

    // Check manufacture value for substance "b" - should be 100 mt = 100000 kg
    assertEquals(100000.0, resultB.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for substance b should be 100000 kg");
    assertEquals("kg", resultB.getManufacture().getUnits(),
        "Manufacture units for substance b should be kg");
  }

  /**
   * Test multiple_with_policies.qta produces expected values.
   */
  @Test
  public void testMultipleWithPolicies() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/multiple_with_policies.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the BAU scenario using KigaliSimFacade
    String bauScenarioName = "bau";
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, bauScenarioName);
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());
    
    // Check BAU scenario for appA/subA
    EngineResult bauResultA = LiveTestsUtil.getResult(bauResultsList.stream(), 1, "appA", "subA");
    assertNotNull(bauResultA, "Should have result for appA/subA in year 1 for BAU scenario");

    // Check manufacture value for appA/subA in BAU - should be 100 mt = 100000 kg
    assertEquals(100000.0, bauResultA.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for appA/subA in BAU should be 100000 kg");
    assertEquals("kg", bauResultA.getManufacture().getUnits(),
        "Manufacture units for appA/subA in BAU should be kg");
    
    // Check BAU scenario for appB/subB
    EngineResult bauResultB = LiveTestsUtil.getResult(bauResultsList.stream(), 1, "appB", "subB");
    assertNotNull(bauResultB, "Should have result for appB/subB in year 1 for BAU scenario");

    // Check manufacture value for appB/subB in BAU - should be 100 mt = 100000 kg
    assertEquals(100000.0, bauResultB.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for appB/subB in BAU should be 100000 kg");
    assertEquals("kg", bauResultB.getManufacture().getUnits(),
        "Manufacture units for appB/subB in BAU should be kg");

    // Run the policy scenario using KigaliSimFacade
    String policyScenarioName = "sim";
    Stream<EngineResult> policyResults = KigaliSimFacade.runScenario(program, policyScenarioName);
    List<EngineResult> policyResultsList = policyResults.collect(Collectors.toList());
    
    // Check policy scenario for appA/subA
    EngineResult policyResultA = LiveTestsUtil.getResult(policyResultsList.stream(), 1, "appA", "subA");
    assertNotNull(policyResultA, "Should have result for appA/subA in year 1 for policy scenario");

    // Check manufacture value for appA/subA in policy scenario - should be capped to 50% = 50 mt = 50000 kg
    assertEquals(50000.0, policyResultA.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for appA/subA in policy scenario should be 50000 kg (50% cap)");
    assertEquals("kg", policyResultA.getManufacture().getUnits(),
        "Manufacture units for appA/subA in policy scenario should be kg");
    
    // Check policy scenario for appB/subB
    EngineResult policyResultB = LiveTestsUtil.getResult(policyResultsList.stream(), 1, "appB", "subB");
    assertNotNull(policyResultB, "Should have result for appB/subB in year 1 for policy scenario");

    // Check manufacture value for appB/subB in policy scenario - should be capped to 50% = 50 mt = 50000 kg
    assertEquals(50000.0, policyResultB.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for appB/subB in policy scenario should be 50000 kg (50% cap)");
    assertEquals("kg", policyResultB.getManufacture().getUnits(),
        "Manufacture units for appB/subB in policy scenario should be kg");
  }

  /**
   * Test multiple_with_policies_split.qta produces expected values.
   */
  @Test
  public void testMultipleWithPoliciesSplit() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/multiple_with_policies_split.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the BAU scenario using KigaliSimFacade
    String bauScenarioName = "bau";
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, bauScenarioName);
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());
    
    // Check BAU scenario for appA/subA
    EngineResult bauResultA = LiveTestsUtil.getResult(bauResultsList.stream(), 1, "appA", "subA");
    assertNotNull(bauResultA, "Should have result for appA/subA in year 1 for BAU scenario");

    // Check manufacture value for appA/subA in BAU - should be 90 mt = 90000 kg
    assertEquals(90000.0, bauResultA.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for appA/subA in BAU should be 90000 kg");
    assertEquals("kg", bauResultA.getManufacture().getUnits(),
        "Manufacture units for appA/subA in BAU should be kg");
        
    // Check import value for appA/subA in BAU - should be 10 mt = 10000 kg
    assertEquals(10000.0, bauResultA.getImport().getValue().doubleValue(), 0.0001,
        "Import for appA/subA in BAU should be 10000 kg");
    assertEquals("kg", bauResultA.getImport().getUnits(),
        "Import units for appA/subA in BAU should be kg");
    
    // Check BAU scenario for appB/subB
    EngineResult bauResultB = LiveTestsUtil.getResult(bauResultsList.stream(), 1, "appB", "subB");
    assertNotNull(bauResultB, "Should have result for appB/subB in year 1 for BAU scenario");

    // Check manufacture value for appB/subB in BAU - should be 90 mt = 90000 kg
    assertEquals(90000.0, bauResultB.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for appB/subB in BAU should be 90000 kg");
    assertEquals("kg", bauResultB.getManufacture().getUnits(),
        "Manufacture units for appB/subB in BAU should be kg");
        
    // Check import value for appB/subB in BAU - should be 10 mt = 10000 kg
    assertEquals(10000.0, bauResultB.getImport().getValue().doubleValue(), 0.0001,
        "Import for appB/subB in BAU should be 10000 kg");
    assertEquals("kg", bauResultB.getImport().getUnits(),
        "Import units for appB/subB in BAU should be kg");

    // Run the policy scenario using KigaliSimFacade
    String policyScenarioName = "sim";
    Stream<EngineResult> policyResults = KigaliSimFacade.runScenario(program, policyScenarioName);
    List<EngineResult> policyResultsList = policyResults.collect(Collectors.toList());
    
    // Check policy scenario for appA/subA
    EngineResult policyResultA = LiveTestsUtil.getResult(policyResultsList.stream(), 1, "appA", "subA");
    assertNotNull(policyResultA, "Should have result for appA/subA in year 1 for policy scenario");

    // Check manufacture value for appA/subA in policy scenario - should be capped to 50% = 45 mt = 45000 kg
    assertEquals(45000.0, policyResultA.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for appA/subA in policy scenario should be 45000 kg (50% cap)");
    assertEquals("kg", policyResultA.getManufacture().getUnits(),
        "Manufacture units for appA/subA in policy scenario should be kg");
        
    // Check import value for appA/subA in policy scenario - should remain unchanged at 10 mt = 10000 kg
    assertEquals(10000.0, policyResultA.getImport().getValue().doubleValue(), 0.0001,
        "Import for appA/subA in policy scenario should remain at 10000 kg (unchanged)");
    assertEquals("kg", policyResultA.getImport().getUnits(),
        "Import units for appA/subA in policy scenario should be kg");
    
    // Check policy scenario for appB/subB
    EngineResult policyResultB = LiveTestsUtil.getResult(policyResultsList.stream(), 1, "appB", "subB");
    assertNotNull(policyResultB, "Should have result for appB/subB in year 1 for policy scenario");

    // Check manufacture value for appB/subB in policy scenario - should be capped to 50% = 45 mt = 45000 kg
    assertEquals(45000.0, policyResultB.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for appB/subB in policy scenario should be 45000 kg (50% cap)");
    assertEquals("kg", policyResultB.getManufacture().getUnits(),
        "Manufacture units for appB/subB in policy scenario should be kg");
        
    // Check import value for appB/subB in policy scenario - should remain unchanged at 10 mt = 10000 kg
    assertEquals(10000.0, policyResultB.getImport().getValue().doubleValue(), 0.0001,
        "Import for appB/subB in policy scenario should remain at 10000 kg (unchanged)");
    assertEquals("kg", policyResultB.getImport().getUnits(),
        "Import units for appB/subB in policy scenario should be kg");
  }
}