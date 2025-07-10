package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Live tests for recharge functionality in the Kigali Simulator.
 * These tests verify the behavior of recharge calculations and equipment populations.
 */
public class RechargeLiveTests {

  /**
   * Basic test for recharge functionality using the standard recharge example.
   */
  @Test
  public void testRecharge() throws IOException {
    String qtaPath = "../examples/recharge.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Check year 1 equipment (population) value
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(resultYear1, "Should have result for test/test in year 1");
    assertEquals(100000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 100000 units in year 1");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");

    // Check year 2 equipment (population) value
    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(resultYear2, "Should have result for test/test in year 2");
    assertEquals(190000.0, resultYear2.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 190000 units in year 2");
    assertEquals("units", resultYear2.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test for recharge import issue where recharge can go negative when sales exceed total demand.
   */
  @Test
  public void testRechargeImportIssue() throws IOException {
    String qtaPath = "../examples/recharge_import_issue.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Check year 2025 equipment (population) value
    // Should have at least 20000 units (the priorEquipment value)
    EngineResult resultYear2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2025, "Should have result for Domestic AC/HFC-32 in year 2025");
    
    double unitsIn2025 = resultYear2025.getPopulation().getValue().doubleValue();
    
    // Assert that units should be at least 20000 (the priorEquipment value)
    assertEquals(true, unitsIn2025 >= 20000.0,
        "Equipment should be at least 20000 units in year 2025 (priorEquipment value), but was " + unitsIn2025);
    assertEquals("units", resultYear2025.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test for recharge on top functionality ensuring recharge is added to sales.
   */
  @Test
  public void testRechargeOnTop() throws IOException {
    String qtaPath = "../examples/recharge_on_top.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Check year 1 equipment (population) value
    // Should have 10000 (prior) + 1000 (manufacture) = 11000 units in year 1
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub1");
    assertNotNull(resultYear1, "Should have result for App/Sub1 in year 1");
    assertEquals(11000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 11000 units in year 1");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test that import settings work correctly with recharge requirements.
   * This verifies that unit-based imports are properly handled.
   */
  @Test
  public void testRechargeUnitsNoChange() throws IOException {
    String qtaPath = "../examples/recharge_units_no_change.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "No Policy";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Find year 2025 result
    EngineResult resultYear2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, 
        "Commercial Refrigeration", "HFC-134a");
    assertNotNull(resultYear2025, "Should have result for Commercial Refrigeration/HFC-134a in year 2025");
    
    // Check new equipment - should be 2667 (the import amount)
    double newEquipment = resultYear2025.getPopulationNew().getValue().doubleValue();
    assertEquals(2667.0, newEquipment, 0.01,
        "New equipment for HFC-134a should be 2667 in year 2025 (matching import amount)");
    
    // Verify equipment units remain at prior level
    double unitsIn2025 = resultYear2025.getPopulation().getValue().doubleValue();
    
    // Assert that units should be at least 20000 (the priorEquipment value)
    assertEquals(true, unitsIn2025 >= 20000.0,
        "Equipment should be at least 20000 units in year 2025 (priorEquipment value), but was " + unitsIn2025);
    assertEquals("units", resultYear2025.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test for recharge equipment growth bug where recharge incorrectly increases equipment count.
   * Without recharge, total equipment should be 20800 in 2025 and 21600 in 2026.
   * With recharge, the equipment count should NOT increase beyond expected values.
   */
  @Test
  public void testRechargeEquipmentGrowthBug() throws IOException {
    String qtaPath = "../examples/recharge_equipment_growth.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Check year 2025 equipment (population) value
    // Should be 20800 units (20000 prior + 800 import)
    EngineResult resultYear2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2025, "Should have result for Domestic AC/HFC-32 in year 2025");
    assertEquals(20800.0, resultYear2025.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 20800 units in year 2025 (20000 prior + 800 import)");
    assertEquals("units", resultYear2025.getPopulation().getUnits(),
        "Equipment units should be units");

    // Check year 2026 equipment (population) value
    // Should be 21600 units (20800 from 2025 + 800 import for 2026)
    EngineResult resultYear2026 = LiveTestsUtil.getResult(resultsList.stream(), 2026, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2026, "Should have result for Domestic AC/HFC-32 in year 2026");
    assertEquals(21600.0, resultYear2026.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 21600 units in year 2026 (20800 from 2025 + 800 import for 2026)");
    assertEquals("units", resultYear2026.getPopulation().getUnits(),
        "Equipment units should be units");

    // Check year 2027 equipment (population) value
    // Should be 22400 units (20000 + 800 * 3) with continued implicit recharge
    EngineResult resultYear2027 = LiveTestsUtil.getResult(resultsList.stream(), 2027, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2027, "Should have result for Domestic AC/HFC-32 in year 2027");
    assertEquals(22400.0, resultYear2027.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 22400 units in year 2027 (20000 + 800 * 3) with continued implicit recharge");
    assertEquals("units", resultYear2027.getPopulation().getUnits(),
        "Equipment units should be units");
    
    // Check year 2028 equipment (population) value
    // Should be 23200 units (20000 + 800 * 4) with continued implicit recharge
    EngineResult resultYear2028 = LiveTestsUtil.getResult(resultsList.stream(), 2028, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2028, "Should have result for Domestic AC/HFC-32 in year 2028");
    assertEquals(23200.0, resultYear2028.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 23200 units in year 2028 (20000 + 800 * 4) with continued implicit recharge");
    assertEquals("units", resultYear2028.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test the reordered version of recharge_equipment_growth to see if order matters.
   */
  @Test
  public void testRechargeEquipmentGrowthReordered() throws IOException {
    String qtaPath = "../examples/recharge_equipment_growth_reordered.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Check year 2025 equipment (population) value
    EngineResult resultYear2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2025, "Should have result for Domestic AC/HFC-32 in year 2025");
    assertEquals(20800.0, resultYear2025.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 20800 units in year 2025 when recharge comes before import");
    
    // Check year 2026 equipment (population) value
    EngineResult resultYear2026 = LiveTestsUtil.getResult(resultsList.stream(), 2026, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2026, "Should have result for Domestic AC/HFC-32 in year 2026");
    assertEquals(21600.0, resultYear2026.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 21600 units in year 2026 when recharge comes before import");
    
    // Check year 2027 equipment (population) value
    EngineResult resultYear2027 = LiveTestsUtil.getResult(resultsList.stream(), 2027, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2027, "Should have result for Domestic AC/HFC-32 in year 2027");
    assertEquals(22400.0, resultYear2027.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 22400 units in year 2027 when recharge comes before import");
    
    // Check year 2028 equipment (population) value
    EngineResult resultYear2028 = LiveTestsUtil.getResult(resultsList.stream(), 2028, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2028, "Should have result for Domestic AC/HFC-32 in year 2028");
    assertEquals(23200.0, resultYear2028.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 23200 units in year 2028 when recharge comes before import");
  }

  /**
   * Test the reordered version of recharge_units_no_change to see if order matters.
   */
  @Test
  public void testRechargeUnitsNoChangeReordered() throws IOException {
    String qtaPath = "../examples/recharge_units_no_change_reordered.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "No Policy";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Find year 2025 result
    EngineResult resultYear2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, 
        "Commercial Refrigeration", "HFC-134a");
    assertNotNull(resultYear2025, "Should have result for Commercial Refrigeration/HFC-134a in year 2025");
    
    // Check new equipment - should be 2667 (the import amount)
    double newEquipment = resultYear2025.getPopulationNew().getValue().doubleValue();
    assertEquals(2667.0, newEquipment, 0.01,
        "New equipment for HFC-134a should be 2667 in year 2025 when recharge comes after set import");
  }

  /**
   * Test multiple recharge operations back-to-back in the same year.
   * This helps determine if the bug is about consecutive recharges vs carry-over years.
   */
  @Test
  public void testMultipleRechargeBackToBack() throws IOException {
    String qtaPath = "../examples/multiple_recharge_test.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Check year 2027 equipment (year with multiple recharges)
    EngineResult resultYear2027 = LiveTestsUtil.getResult(resultsList.stream(), 2027, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2027, "Should have result for Domestic AC/HFC-32 in year 2027");
    
    // Check year 2028 equipment (to see if multiple recharges caused accumulation)
    EngineResult resultYear2028 = LiveTestsUtil.getResult(resultsList.stream(), 2028, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2028, "Should have result for Domestic AC/HFC-32 in year 2028");
    
    // Log actual values for investigation
    double equipment2027 = resultYear2027.getPopulation().getValue().doubleValue();
    double equipment2028 = resultYear2028.getPopulation().getValue().doubleValue();
    System.out.println("Equipment 2027 (multiple recharge year): " + equipment2027);
    System.out.println("Equipment 2028 (after multiple recharge): " + equipment2028);
  }

  /**
   * Test kg-based imports with recharge to verify correct behavior.
   * With kg-based imports, recharge needs are subtracted first.
   * If imports are insufficient for recharge, equipment decreases.
   */
  @Test
  public void testRechargeEquipmentGrowthKg() throws IOException {
    String qtaPath = "../examples/recharge_equipment_growth_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Check year 2025 equipment (population) value
    // Recharge needed: 20000 * 0.1 * 0.85 = 1700 kg
    // Import available: 800 kg
    // Deficit: 900 kg = 1058.82 units
    // Equipment: 20000 - 1058.82 = 18941.18 units
    EngineResult resultYear2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2025, "Should have result for Domestic AC/HFC-32 in year 2025");
    assertEquals(18941.18, resultYear2025.getPopulation().getValue().doubleValue(), 0.01,
        "Equipment should decrease to 18941 units due to insufficient imports for recharge");
    
    // Check year 2026 equipment (population) value
    // Recharge needed: 18941.18 * 0.1 * 0.85 = 1610 kg
    // Import available: 800 kg
    // Deficit: 810 kg = 952.94 units
    // Equipment: 18941.18 - 952.94 = 17988.24 units
    EngineResult resultYear2026 = LiveTestsUtil.getResult(resultsList.stream(), 2026, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2026, "Should have result for Domestic AC/HFC-32 in year 2026");
    assertEquals(17988.24, resultYear2026.getPopulation().getValue().doubleValue(), 0.01,
        "Equipment should continue decreasing to 17988 units");
    
    // Check year 2027 equipment (population) value
    // Similar calculation continues
    EngineResult resultYear2027 = LiveTestsUtil.getResult(resultsList.stream(), 2027, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2027, "Should have result for Domestic AC/HFC-32 in year 2027");
    assertEquals(17130.59, resultYear2027.getPopulation().getValue().doubleValue(), 0.01,
        "Equipment should be 17131 units in year 2027");
    
    // Check year 2028 equipment (population) value
    // Equipment continues to decrease as imports remain insufficient
    EngineResult resultYear2028 = LiveTestsUtil.getResult(resultsList.stream(), 2028, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2028, "Should have result for Domestic AC/HFC-32 in year 2028");
    assertEquals(16358.71, resultYear2028.getPopulation().getValue().doubleValue(), 0.01,
        "Equipment should be 16359 units in year 2028");
  }

  /**
   * Test recharge with initial charge set during year 2025.
   */
  @Test
  public void testRechargeEquipmentGrowthInitialCharge2025() throws IOException {
    String qtaPath = "../examples/recharge_equipment_growth_initialcharge_2025.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Check year 2025 equipment (population) value
    EngineResult resultYear2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2025, "Should have result for Domestic AC/HFC-32 in year 2025");
    assertEquals(20800.0, resultYear2025.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 20800 units in year 2025");
    
    // Check year 2026 equipment (population) value
    EngineResult resultYear2026 = LiveTestsUtil.getResult(resultsList.stream(), 2026, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2026, "Should have result for Domestic AC/HFC-32 in year 2026");
    assertEquals(21600.0, resultYear2026.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 21600 units in year 2026");
    
    // Check year 2028 equipment (population) value
    EngineResult resultYear2028 = LiveTestsUtil.getResult(resultsList.stream(), 2028, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2028, "Should have result for Domestic AC/HFC-32 in year 2028");
    assertEquals(23200.0, resultYear2028.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 23200 units in year 2028 with initial charge during 2025");
  }


}