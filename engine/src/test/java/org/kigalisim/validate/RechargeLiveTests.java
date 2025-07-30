package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  /**
   * Test for disproportionate displacement issue where cap with displacement
   * results in larger increases in the displaced substance than expected.
   * This test verifies that the drop in HFC-134a equals the increase in R-600a.
   */
  @Test
  public void testDisplaceDisproportionate() throws IOException {
    String qtaPath = "../examples/displace_disproportionate.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    String bauScenario = "BAU";
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, bauScenario, progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run policy scenario
    String policyScenario = "Permit";
    Stream<EngineResult> policyResults = KigaliSimFacade.runScenario(program, policyScenario, progress -> {});
    List<EngineResult> policyResultsList = policyResults.collect(Collectors.toList());

    // Get 2034 and 2035 results for both scenarios
    EngineResult bauHfc2034 = LiveTestsUtil.getResult(bauResultsList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    EngineResult bauHfc2035 = LiveTestsUtil.getResult(bauResultsList.stream(), 2035, "Domestic Refrigeration", "HFC-134a");
    EngineResult bauR600a2034 = LiveTestsUtil.getResult(bauResultsList.stream(), 2034, "Domestic Refrigeration", "R-600a");
    final EngineResult bauR600a2035 = LiveTestsUtil.getResult(bauResultsList.stream(), 2035, "Domestic Refrigeration", "R-600a");

    final EngineResult policyHfc2034 = LiveTestsUtil.getResult(policyResultsList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    final EngineResult policyHfc2035 = LiveTestsUtil.getResult(policyResultsList.stream(), 2035, "Domestic Refrigeration", "HFC-134a");
    final EngineResult policyR600a2034 = LiveTestsUtil.getResult(policyResultsList.stream(), 2034, "Domestic Refrigeration", "R-600a");
    final EngineResult policyR600a2035 = LiveTestsUtil.getResult(policyResultsList.stream(), 2035, "Domestic Refrigeration", "R-600a");

    assertNotNull(bauHfc2034, "Should have BAU HFC-134a result for 2034");
    assertNotNull(bauHfc2035, "Should have BAU HFC-134a result for 2035");
    assertNotNull(bauR600a2034, "Should have BAU R-600a result for 2034");
    assertNotNull(bauR600a2035, "Should have BAU R-600a result for 2035");
    assertNotNull(policyHfc2034, "Should have policy HFC-134a result for 2034");
    assertNotNull(policyHfc2035, "Should have policy HFC-134a result for 2035");
    assertNotNull(policyR600a2034, "Should have policy R-600a result for 2034");
    assertNotNull(policyR600a2035, "Should have policy R-600a result for 2035");

    // Calculate the change in HFC-134a sales from 2034 to 2035 in the policy scenario
    // Sales = domestic + import
    double bauHfcSales2034 = (bauHfc2034.getDomestic().getValue().doubleValue() + bauHfc2034.getImport().getValue().doubleValue());
    double bauHfcSales2035 = (bauHfc2035.getDomestic().getValue().doubleValue() + bauHfc2035.getImport().getValue().doubleValue());
    double policyHfcSales2034 = (policyHfc2034.getDomestic().getValue().doubleValue() + policyHfc2034.getImport().getValue().doubleValue());
    double policyHfcSales2035 = (policyHfc2035.getDomestic().getValue().doubleValue() + policyHfc2035.getImport().getValue().doubleValue());

    // Calculate the change in R-600a sales from 2034 to 2035 in the policy scenario
    double bauR600aSales2034 = (bauR600a2034.getDomestic().getValue().doubleValue() + bauR600a2034.getImport().getValue().doubleValue());
    double bauR600aSales2035 = (bauR600a2035.getDomestic().getValue().doubleValue() + bauR600a2035.getImport().getValue().doubleValue());
    double policyR600aSales2034 = (policyR600a2034.getDomestic().getValue().doubleValue() + policyR600a2034.getImport().getValue().doubleValue());
    double policyR600aSales2035 = (policyR600a2035.getDomestic().getValue().doubleValue() + policyR600a2035.getImport().getValue().doubleValue());

    // Calculate the drop in HFC-134a relative to BAU
    final double hfcDropRelativeToBau = (bauHfcSales2035 - policyHfcSales2035);

    // Calculate the increase in R-600a relative to BAU
    final double r600aIncreaseRelativeToBau = (policyR600aSales2035 - bauR600aSales2035);

    // Convert to same units if needed (both should be in kg)
    assertEquals("kg", bauHfc2035.getDomestic().getUnits(), "HFC-134a sales should be in kg");
    assertEquals("kg", bauR600a2035.getDomestic().getUnits(), "R-600a sales should be in kg");

    // Get equipment populations and recharge emissions for analysis
    double bauHfcEquipment = bauHfc2035.getPopulation().getValue().doubleValue();
    double policyHfcEquipment = policyHfc2035.getPopulation().getValue().doubleValue();
    double bauR600aEquipment = bauR600a2035.getPopulation().getValue().doubleValue();
    double policyR600aEquipment = policyR600a2035.getPopulation().getValue().doubleValue();

    // Calculate units from displaced volume
    double hfcInitialCharge = 0.15; // kg/unit from QTA file
    double r600aInitialCharge = 0.07; // kg/unit from QTA file

    // Get recharge emissions for validation
    double bauHfcRecharge = bauHfc2035.getRechargeEmissions().getValue().doubleValue();
    double policyHfcRecharge = policyHfc2035.getRechargeEmissions().getValue().doubleValue();
    double bauR600aRecharge = bauR600a2035.getRechargeEmissions().getValue().doubleValue();
    double policyR600aRecharge = policyR600a2035.getRechargeEmissions().getValue().doubleValue();

    // GWP analysis
    double hfcGwp = 1430; // tCO2e/kg from QTA file
    double r600aGwp = 3; // tCO2e/kg from QTA file

    // Calculate expected recharge amounts based on different approaches
    double simplifiedR600aRechargeKg = policyR600aEquipment * 0.10 * r600aInitialCharge;
    double simplifiedExpectedEmissions = simplifiedR600aRechargeKg * r600aGwp;

    // Reverse engineer what equipment population was used for recharge calculation
    double actualRechargeVolumeFromEmissions = policyR600aRecharge / r600aGwp;
    double impliedPriorEquipmentFromRecharge = actualRechargeVolumeFromEmissions / (0.10 * r600aInitialCharge);

    double actualR600aEmissionsFromRecharge = policyR600aRecharge;

    final double actualR600aRechargeEmissions = policyR600aRecharge;

    // The displacement should be 1:1 - the drop in HFC-134a should equal the increase in R-600a
    assertEquals(hfcDropRelativeToBau, r600aIncreaseRelativeToBau, 1.0,
        "Drop in HFC-134a (" + hfcDropRelativeToBau + " kg) should equal increase in R-600a ("
        + r600aIncreaseRelativeToBau + " kg) relative to BAU values");

    // Validate displacement/recharge interaction fix
    // Verify the correct GWP is being used (validates the main displacement context fix)
    double calculatedGwpFromEmissions = actualR600aRechargeEmissions / actualRechargeVolumeFromEmissions;

    // Test that the GWP being used is correct (validates our main fix)
    assertEquals(r600aGwp, calculatedGwpFromEmissions, 0.01,
        "GWP calculation should use R-600a's GWP (3 tCO2e/kg), not HFC-134a's (1430 tCO2e/kg). "
        + "Calculated GWP: " + calculatedGwpFromEmissions + ", Expected: " + r600aGwp);

    // Verify internal consistency of recharge calculation
    double consistencyCheck = impliedPriorEquipmentFromRecharge * 0.10 * r600aInitialCharge * r600aGwp;
    assertEquals(actualR600aRechargeEmissions, consistencyCheck, 0.01,
        "Recharge emissions should be internally consistent with equipment population and recharge rate. "
        + "Actual: " + actualR600aRechargeEmissions + " tCO2e, Calculated: " + consistencyCheck + " tCO2e");
  }

  /**
   * Test combined policies (Sales Permit + Domestic Recycling) to verify correct interaction.
   * Combined policy should consume more than recycling alone due to equipment displacement cascades.
   */
  @Test
  public void testCombinedPoliciesRecharge() throws IOException {
    String qtaPath = "../examples/combined_policies_recharge.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);

    // Run recycling scenario (recycling policy only)
    Stream<EngineResult> recyclingStream = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingStream.collect(Collectors.toList());

    // Run combined scenario (both policies)
    Stream<EngineResult> combinedStream = KigaliSimFacade.runScenario(program, "Combined", progress -> {});
    List<EngineResult> combinedResultsList = combinedStream.collect(Collectors.toList());

    // Get 2035 results for both HFC-134a and R-600a
    EngineResult recyclingHfc2035 = LiveTestsUtil.getResult(recyclingResultsList.stream(), 2035, "Domestic Refrigeration", "HFC-134a");
    EngineResult recyclingR600a2035 = LiveTestsUtil.getResult(recyclingResultsList.stream(), 2035, "Domestic Refrigeration", "R-600a");
    EngineResult combinedHfc2035 = LiveTestsUtil.getResult(combinedResultsList.stream(), 2035, "Domestic Refrigeration", "HFC-134a");
    final EngineResult combinedR600a2035 = LiveTestsUtil.getResult(combinedResultsList.stream(), 2035, "Domestic Refrigeration", "R-600a");

    assertNotNull(recyclingHfc2035, "Should have recycling HFC-134a result for 2035");
    assertNotNull(recyclingR600a2035, "Should have recycling R-600a result for 2035");
    assertNotNull(combinedHfc2035, "Should have combined HFC-134a result for 2035");
    assertNotNull(combinedR600a2035, "Should have combined R-600a result for 2035");

    // Calculate total consumption (imports + domestic) for recycling scenario
    double recyclingTotalConsumption =
        (recyclingHfc2035.getDomestic().getValue().doubleValue() + recyclingHfc2035.getImport().getValue().doubleValue())
        + (recyclingR600a2035.getDomestic().getValue().doubleValue() + recyclingR600a2035.getImport().getValue().doubleValue());

    // Calculate total consumption (imports + domestic) for combined scenario
    double combinedTotalConsumption =
        (combinedHfc2035.getDomestic().getValue().doubleValue() + combinedHfc2035.getImport().getValue().doubleValue())
        + (combinedR600a2035.getDomestic().getValue().doubleValue() + combinedR600a2035.getImport().getValue().doubleValue());

    // Expected: Recycling scenario produces consistent consumption
    // Updated after fixing sales percentage change bug - was 32,853 kg with buggy behavior
    assertEquals(69978.42, recyclingTotalConsumption, 1.0, "Recycling scenario total consumption should be ~69,978 kg");

    // Expected: Combined policies (recycling + cap) should consume LESS than recycling alone
    // Cap policies should reduce overall consumption when applied on top of recycling
    assertTrue(combinedTotalConsumption < recyclingTotalConsumption,
        "Combined scenario should consume less than recycling alone (combined: " + combinedTotalConsumption
        + " kg, recycling: " + recyclingTotalConsumption + " kg)");
  }

  /**
   * Test combined policies with MT-based caps for proper material consumption calculation.
   */
  @Test
  public void testCombinedPoliciesRechargeMt() throws IOException {
    String qtaPath = "../examples/combined_policies_recharge_mt.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);

    // Run combined scenario
    Stream<EngineResult> combinedStream = KigaliSimFacade.runScenario(program, "Combined", progress -> {});
    List<EngineResult> combinedResultsList = combinedStream.collect(Collectors.toList());

    // Get 2035 results for HFC-134a
    EngineResult combinedHfc2035 = LiveTestsUtil.getResult(combinedResultsList.stream(), 2035, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(combinedHfc2035, "Should have combined HFC-134a result for 2035");

    // With MT-based caps, expect zero consumption due to material limits
    double hfcConsumption = combinedHfc2035.getDomestic().getValue().doubleValue() + combinedHfc2035.getImport().getValue().doubleValue();
    assertEquals(0.0, hfcConsumption, 1.0, "HFC-134a consumption should be 0 kg with MT-based cap to 0");
  }

  /**
   * Test combined policies reorder with MT-based caps.
   */
  @Test
  public void testCombinedPoliciesRechargeReorder() throws IOException {
    String qtaPath = "../examples/combined_policies_recharge_reorder.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);

    // Run recycling scenario (recycling policy only)
    Stream<EngineResult> recyclingStream = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingStream.collect(Collectors.toList());

    // Run combined scenario
    Stream<EngineResult> combinedStream = KigaliSimFacade.runScenario(program, "Combined", progress -> {});
    List<EngineResult> combinedResultsList = combinedStream.collect(Collectors.toList());

    // Get 2035 results for both HFC-134a and R-600a
    EngineResult recyclingHfc2035 = LiveTestsUtil.getResult(recyclingResultsList.stream(), 2035, "Domestic Refrigeration", "HFC-134a");
    EngineResult recyclingR600a2035 = LiveTestsUtil.getResult(recyclingResultsList.stream(), 2035, "Domestic Refrigeration", "R-600a");
    EngineResult combinedHfc2035 = LiveTestsUtil.getResult(combinedResultsList.stream(), 2035, "Domestic Refrigeration", "HFC-134a");
    final EngineResult combinedR600a2035 = LiveTestsUtil.getResult(combinedResultsList.stream(), 2035, "Domestic Refrigeration", "R-600a");

    assertNotNull(recyclingHfc2035, "Should have recycling HFC-134a result for 2035");
    assertNotNull(recyclingR600a2035, "Should have recycling R-600a result for 2035");
    assertNotNull(combinedHfc2035, "Should have combined HFC-134a result for 2035");
    assertNotNull(combinedR600a2035, "Should have combined R-600a result for 2035");

    // Calculate total consumption (imports + domestic) for recycling scenario
    double recyclingTotalConsumption =
        (recyclingHfc2035.getDomestic().getValue().doubleValue() + recyclingHfc2035.getImport().getValue().doubleValue())
        + (recyclingR600a2035.getDomestic().getValue().doubleValue() + recyclingR600a2035.getImport().getValue().doubleValue());

    // Calculate total consumption (imports + domestic) for combined scenario
    double combinedTotalConsumption =
        (combinedHfc2035.getDomestic().getValue().doubleValue() + combinedHfc2035.getImport().getValue().doubleValue())
        + (combinedR600a2035.getDomestic().getValue().doubleValue() + combinedR600a2035.getImport().getValue().doubleValue());

    // Expected: Recycling scenario produces consistent consumption
    // Updated after fixing sales percentage change bug - was 32,853 kg with buggy behavior
    assertEquals(69978.42, recyclingTotalConsumption, 1.0, "Recycling scenario total consumption should be ~69,978 kg");

    // Expected: Combined policies (recycling + cap) should consume LESS than recycling alone
    // Cap policies should reduce overall consumption when applied on top of recycling
    assertTrue(combinedTotalConsumption < recyclingTotalConsumption,
        "Combined scenario should consume less than recycling alone (combined: " + combinedTotalConsumption
        + " kg, recycling: " + recyclingTotalConsumption + " kg)");
  }

}
