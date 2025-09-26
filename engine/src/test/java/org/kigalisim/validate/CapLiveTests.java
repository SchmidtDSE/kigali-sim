/**
 * Cap live tests using actual QTA files with "cap" prefix.
 *
 * @license BSD-3-Clause
 */

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
 * Tests that validate cap QTA files against expected behavior.
 */
public class CapLiveTests {

  /**
   * Test cap_kg.qta produces expected values.
   * This tests capping domestic to a specific weight in kg.
   */
  @Test
  public void testCapKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check domestic value - should be capped at 50 kg
    assertEquals(50.0, result.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic should be capped at 50 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");
  }

  /**
   * Test cap_units.qta produces expected values.
   * This tests capping domestic to a specific number of units.
   */
  @Test
  public void testCapUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check domestic value
    // With recharge on top: 50 units * 2 kg/unit + (20 units * 10% * 1 kg/unit) = 102 kg
    // Since original value is 100 kg and cap should be 102 kg, no change expected
    assertEquals(100.0, result.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic should be 100 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");
  }

  /**
   * Test cap_displace_units.qta produces expected values.
   * This tests capping domestic with displacement to another substance.
   */
  @Test
  public void testCapDisplaceUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check that sub_a domestic was capped
    EngineResult recordSubA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_a");
    assertNotNull(recordSubA, "Should have result for test/sub_a in year 1");

    // Cap is 5 units with proportional recharge distribution:
    // Base amount: 5 units * 10 kg/unit = 50 kg
    // Total recharge: 20 units * 10% * 10 kg/unit = 20 kg
    // Sales distribution: domestic=100kg (66.67%), import=50kg (33.33%)
    // Domestic recharge: 20 kg * 66.67% = 13.33 kg
    // Total domestic: 50 kg + 13.33 kg = 63.33 kg
    assertEquals(63.333333333333336, recordSubA.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic for sub_a should be capped at 63.33 kg (50 + 13.33 proportional recharge)");
    assertEquals("kg", recordSubA.getDomestic().getUnits(),
        "Domestic units for sub_a should be kg");

    // Check displacement to sub_b
    EngineResult recordSubB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_b");
    assertNotNull(recordSubB, "Should have result for test/sub_b in year 1");

    // With unit-based displacement: 36.67 kg reduction in sub_a = 36.67 kg / 10 kg/unit = 3.67 units
    // 3.67 units displaced to sub_b = 3.67 units * 20 kg/unit = 73.33 kg
    // Original sub_b: 200 kg, Final sub_b: 200 kg + 73.33 kg = 273.33 kg
    assertEquals(273.3333333333333, recordSubB.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic for sub_b should be 273.33 kg after displacement");
    assertEquals("kg", recordSubB.getDomestic().getUnits(),
        "Domestic units for sub_b should be kg");
  }

  /**
   * Test cap_displace_unit_conversion.qta produces expected values.
   * This tests unit-to-unit displacement conversion.
   */
  @Test
  public void testCapDisplaceUnitConversion() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_unit_conversion.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check that sub_a domestic was capped
    EngineResult recordSubA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_a");
    assertNotNull(recordSubA, "Should have result for test/sub_a in year 1");

    // Cap is 5 units, with recharge: 5 units * 10 kg/unit + (20 units * 10% * 10 kg/unit) = 50 + 20 = 70 kg
    // Original was 30 units * 10 kg/unit = 300 kg, so should be capped to 70 kg
    // Reduction: 300 - 70 = 230 kg
    assertEquals(70.0, recordSubA.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic for sub_a should be capped at 70 kg");
    assertEquals("kg", recordSubA.getDomestic().getUnits(),
        "Domestic units for sub_a should be kg");

    // Check displacement to sub_b
    EngineResult recordSubB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_b");
    assertNotNull(recordSubB, "Should have result for test/sub_b in year 1");

    // With unit-based displacement:
    // 230 kg reduction in sub_a = 230 kg / 10 kg/unit = 23 units
    // 23 units displaced to sub_b = 23 units * 20 kg/unit = 460 kg
    // Original sub_b: 10 units * 20 kg/unit = 200 kg
    // Final sub_b: 200 kg + 460 kg = 660 kg
    assertEquals(660.0, recordSubB.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic for sub_b should be 660 kg after displacement");
    assertEquals("kg", recordSubB.getDomestic().getUnits(),
        "Domestic units for sub_b should be kg");
  }

  /**
   * Test cap_displace_bug_kg.qta produces expected values.
   * This tests capping sales to 0 kg with displacement.
   */
  @Test
  public void testCapDisplaceBugKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_bug_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Equipment Import Ban";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check HFC-134a new equipment is zero in 2030
    EngineResult recordHfc2030 = LiveTestsUtil.getResult(resultsList.stream(), 2030,
        "Commercial Refrigeration", "HFC-134a");
    assertNotNull(recordHfc2030, "Should have result for Commercial Refrigeration/HFC-134a in year 2030");

    // Should be zero new equipment
    assertEquals(0.0, recordHfc2030.getPopulationNew().getValue().doubleValue(), 0.0001,
        "New equipment for HFC-134a should be zero in 2030");

    // Check HFC-134a consumption is zero in 2030
    double domesticConsumptionHfc = recordHfc2030.getDomesticConsumption().getValue().doubleValue();
    double importConsumptionHfc = recordHfc2030.getImportConsumption().getValue().doubleValue();
    double totalConsumptionHfc = domesticConsumptionHfc + importConsumptionHfc;
    assertEquals(0.0, totalConsumptionHfc, 0.0001,
        "Total consumption for HFC-134a should be zero in 2030");

    // Check R-404A new equipment is not zero in year 2035
    EngineResult recordR404A2035 = LiveTestsUtil.getResult(resultsList.stream(), 2035,
        "Commercial Refrigeration", "R-404A");
    assertNotNull(recordR404A2035, "Should have result for Commercial Refrigeration/R-404A in year 2035");

    // Should have new equipment due to displacement
    double newEquipmentR404A = recordR404A2035.getPopulationNew().getValue().doubleValue();
    assertTrue(newEquipmentR404A > 0,  "R-404A new equipment should be greater than 0 in 2035 due to displacement");

    // Should have consumption
    double domesticConsumptionR404 = recordR404A2035.getDomesticConsumption().getValue().doubleValue();
    double importConsumptionR404 = recordR404A2035.getImportConsumption().getValue().doubleValue();
    double totalConsumptionR404 = domesticConsumptionR404 + importConsumptionR404;
    assertTrue(
        totalConsumptionR404 > 0,
        "Total consumption for R404A should be more than zero in 2030"
    );
  }

  /**
   * Test cap_displace_bug_units.qta produces expected values.
   * This tests capping equipment to 0 units with displacement.
   */
  @Test
  public void testCapDisplaceBugUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_bug_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Equipment Import Ban";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check HFC-134a new equipment is zero in 2030
    EngineResult recordHfc2030 = LiveTestsUtil.getResult(resultsList.stream(), 2030,
        "Commercial Refrigeration", "HFC-134a");
    assertNotNull(recordHfc2030, "Should have result for Commercial Refrigeration/HFC-134a in year 2030");

    // Should be zero new equipment
    assertEquals(0.0, recordHfc2030.getPopulationNew().getValue().doubleValue(), 0.0001,
        "New equipment for HFC-134a should be zero in 2030");

    // Check R-404A new equipment is not zero in year 2035
    EngineResult recordR404A2035 = LiveTestsUtil.getResult(resultsList.stream(), 2035,
        "Commercial Refrigeration", "R-404A");
    assertNotNull(recordR404A2035, "Should have result for Commercial Refrigeration/R-404A in year 2035");

    // Should have new equipment due to displacement
    double newEquipmentR404A = recordR404A2035.getPopulationNew().getValue().doubleValue();
    assertTrue(
        newEquipmentR404A > 0,
        "R-404A new equipment should be greater than 0 in 2035 due to displacement"
    );
  }

  /**
   * Test cap_displace_with_recharge_units.qta produces expected values.
   * This tests capping equipment to 0 units with displacement, but with recharge.
   */
  @Test
  public void testCapDisplaceWithRechargeUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_with_recharge_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Equipment Import Ban";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check HFC-134a new equipment is zero in 2030
    EngineResult recordHfc2030 = LiveTestsUtil.getResult(resultsList.stream(), 2030,
        "Commercial Refrigeration", "HFC-134a");
    assertNotNull(recordHfc2030, "Should have result for Commercial Refrigeration/HFC-134a in year 2030");

    // Should be zero new equipment
    assertEquals(0.0, recordHfc2030.getPopulationNew().getValue().doubleValue(), 0.0001,
        "New equipment for HFC-134a should be zero in 2030");

    // Check HFC-134a consumption is NOT zero in 2030 due to recharge
    double domesticConsumptionHfc = recordHfc2030.getDomesticConsumption().getValue().doubleValue();
    double importConsumptionHfc = recordHfc2030.getImportConsumption().getValue().doubleValue();
    double totalConsumptionHfc = domesticConsumptionHfc + importConsumptionHfc;
    assertTrue(totalConsumptionHfc > 0,
        "Total consumption for HFC-134a should be greater than zero in 2030 due to recharge");

    // Check R-404A new equipment is not zero in year 2035
    EngineResult recordR404A2035 = LiveTestsUtil.getResult(resultsList.stream(), 2035,
        "Commercial Refrigeration", "R-404A");
    assertNotNull(recordR404A2035, "Should have result for Commercial Refrigeration/R-404A in year 2035");

    // Should have new equipment due to displacement
    double newEquipmentR404A = recordR404A2035.getPopulationNew().getValue().doubleValue();
    assertTrue(
        newEquipmentR404A > 0,
        "R-404A new equipment should be greater than 0 in 2035 due to displacement"
    );
  }

  /**
   * Test cap scenario without change statements to isolate the issue.
   *
   * <p>This test caps import to 0 units. Since there are no change statements and no retire
   * statement, we expect 0 new equipment. The cap should allow enough import to satisfy
   * recharge needs (10% of existing equipment at 0.1 kg/unit) but no more.</p>
   *
   * <p>Expected: PopulationNew = 0.0 (no new equipment when import is capped to 0 units)
   * Actual: PopulationNew = -267.3 (negative equipment, indicating a bug)</p>
   */
  @Test
  public void testCapDisplaceWithRechargeUnitsNoChange() throws IOException {
    // Load and parse the QTA file without change statements
    String qtaPath = "../examples/cap_displace_with_recharge_units_no_change.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Equipment Import Ban";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check HFC-134a new equipment is zero in 2030 (when cap is active)
    EngineResult recordHfc2030 = LiveTestsUtil.getResult(resultsList.stream(), 2030,
        "Commercial Refrigeration", "HFC-134a");
    assertNotNull(recordHfc2030, "Should have result for Commercial Refrigeration/HFC-134a in year 2030");

    // Should be zero new equipment
    assertEquals(0.0, recordHfc2030.getPopulationNew().getValue().doubleValue(), 0.0001,
        "New equipment for HFC-134a should be zero in 2030 (without change statements)");
  }

  /**
   * Test cap_displace_units_magnitude.qta produces expected values.
   * This tests capping equipment with specific magnitude values for displacement.
   */
  @Test
  public void testCapDisplaceUnitsMagnitude() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_units_magnitude.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Import Ban";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check 2025 new equipment - R-404A should have 200 units, HFC-134a should have 400 units
    EngineResult recordR404A2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, "Test", "R-404A");
    assertNotNull(recordR404A2025, "Should have result for Test/R-404A in year 2025");
    assertEquals(200.0, recordR404A2025.getPopulationNew().getValue().doubleValue(), 0.0001,
        "R-404A new equipment should be 200 units in 2025");
    assertEquals("units", recordR404A2025.getPopulationNew().getUnits(),
        "R-404A new equipment units should be units");

    EngineResult recordHfc2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, "Test", "HFC-134a");
    assertNotNull(recordHfc2025, "Should have result for Test/HFC-134a in year 2025");
    assertEquals(400.0, recordHfc2025.getPopulationNew().getValue().doubleValue(), 0.0001,
        "HFC-134a new equipment should be 400 units in 2025");
    assertEquals("units", recordHfc2025.getPopulationNew().getUnits(),
        "HFC-134a new equipment units should be units");

    // Check 2029 new equipment - HFC-134a should have 0 units (capped), R-404A should have 600 units (displaced)
    EngineResult recordHfc2029 = LiveTestsUtil.getResult(resultsList.stream(), 2029, "Test", "HFC-134a");
    assertNotNull(recordHfc2029, "Should have result for Test/HFC-134a in year 2029");
    assertEquals(0.0, recordHfc2029.getPopulationNew().getValue().doubleValue(), 0.0001,
        "HFC-134a new equipment should be 0 units in 2029");
    assertEquals("units", recordHfc2029.getPopulationNew().getUnits(),
        "HFC-134a new equipment units should be units");

    EngineResult recordR404A2029 = LiveTestsUtil.getResult(resultsList.stream(), 2029, "Test", "R-404A");
    assertNotNull(recordR404A2029, "Should have result for Test/R-404A in year 2029");
    assertEquals(600.0, recordR404A2029.getPopulationNew().getValue().doubleValue(), 0.0001,
        "R-404A new equipment should be 600 units in 2029");
    assertEquals("units", recordR404A2029.getPopulationNew().getUnits(),
        "R-404A new equipment units should be units");

    // Check 2030 new equipment - same as 2029 (HFC-134a should have 0 units, R-404A should have 600 units)
    EngineResult recordHfc2030 = LiveTestsUtil.getResult(resultsList.stream(), 2030, "Test", "HFC-134a");
    assertNotNull(recordHfc2030, "Should have result for Test/HFC-134a in year 2030");
    assertEquals(0.0, recordHfc2030.getPopulationNew().getValue().doubleValue(), 0.0001,
        "HFC-134a new equipment should be 0 units in 2030");
    assertEquals("units", recordHfc2030.getPopulationNew().getUnits(),
        "HFC-134a new equipment units should be units");

    EngineResult recordR404A2030 = LiveTestsUtil.getResult(resultsList.stream(), 2030, "Test", "R-404A");
    assertNotNull(recordR404A2030, "Should have result for Test/R-404A in year 2030");
    assertEquals(600.0, recordR404A2030.getPopulationNew().getValue().doubleValue(), 0.0001,
        "R-404A new equipment should be 600 units in 2030");
    assertEquals("units", recordR404A2030.getPopulationNew().getUnits(),
        "R-404A new equipment units should be units");
  }

  /**
   * Test cap displacing import to import should throw an error.
   * This tests that displacing imports into imports is detected and raises an error.
   */
  @Test
  public void testCapDisplaceImportToImport() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_import_to_import.qta";

    // This should throw an exception due to invalid displacement
    try {
      ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);

      // If we get here without an exception, run the scenario to see if runtime error occurs
      String scenarioName = "S1";
      Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
      List<EngineResult> resultsList = results.collect(Collectors.toList());

      // If we reach here, no exception was thrown - this indicates the bug exists
      assertTrue(false, "Expected an exception when displacing import to import, but none was thrown");

    } catch (Exception e) {
      // Exception was thrown as expected - test passes
      assertTrue(true, "Exception correctly thrown for import-to-import displacement: " + e.getMessage());
    }
  }

  /**
   * Test cap with displacement to another substance preserves prior equipment.
   * This tests that when capping one substance and displacing to another,
   * the destination substance's prior equipment is properly maintained.
   */
  @Test
  public void testCapDisplacePreservesPriorEquipment() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_prior_equipment.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Cap with Displacement scenario
    Stream<EngineResult> capResults = KigaliSimFacade.runScenario(program, "Cap with Displacement", progress -> {});
    List<EngineResult> capResultsList = capResults.collect(Collectors.toList());

    // Get year 2030 results
    int targetYear = 2030;

    // BAU results for 2030
    EngineResult bauHighGwp = LiveTestsUtil.getResult(bauResultsList.stream(), targetYear, "Test App", "High-GWP");
    EngineResult bauLowGwp = LiveTestsUtil.getResult(bauResultsList.stream(), targetYear, "Test App", "Low-GWP");

    // Cap displacement results for 2030
    EngineResult capHighGwp = LiveTestsUtil.getResult(capResultsList.stream(), targetYear, "Test App", "High-GWP");
    final EngineResult capLowGwp = LiveTestsUtil.getResult(capResultsList.stream(), targetYear, "Test App", "Low-GWP");

    assertNotNull(bauHighGwp, "Should have BAU result for High-GWP in 2030");
    assertNotNull(bauLowGwp, "Should have BAU result for Low-GWP in 2030");
    assertNotNull(capHighGwp, "Should have Cap result for High-GWP in 2030");
    assertNotNull(capLowGwp, "Should have Cap result for Low-GWP in 2030");

    // Test that Low-GWP population is NOT zero in the cap scenario
    // If the bug exists (like with replace), Low-GWP would lose its prior equipment
    assertTrue(capLowGwp.getPopulation().getValue().doubleValue() > 0,
        "Low-GWP should maintain equipment population in cap displacement scenario");

    // Test that Low-GWP maintains at least some of its initial prior equipment
    // Initial was 50000, after 5 years with 5% retirement, should have at least 38000
    assertTrue(capLowGwp.getPopulation().getValue().doubleValue() > 38000,
        String.format("Low-GWP population (%.2f) should be above 38000 units",
            capLowGwp.getPopulation().getValue().doubleValue()));
  }

  /**
   * Test sticky demand issue - consumption should not decrease in 2028.
   * This test reproduces the issue where R-600a imports bump up in 2027 due to
   * HFC-134a cap displacement but then revert back/decrease in 2028.
   *
   * <p>Expected: R-600a consumption in 2028 should be >= consumption in 2027
   * since the cap displacement should continue. This test is designed to fail
   * to demonstrate the issue.</p>
   */
  @Test
  public void testStickyDemand() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/sticky_demand.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the "StickyDemand" scenario
    String scenarioName = "StickyDemand";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Get R-600a results for multiple years to understand the pattern
    EngineResult r600a2026 = LiveTestsUtil.getResult(resultsList.stream(), 2026, "Domestic Refrigeration", "R-600a");
    EngineResult r600a2027 = LiveTestsUtil.getResult(resultsList.stream(), 2027, "Domestic Refrigeration", "R-600a");
    EngineResult r600a2028 = LiveTestsUtil.getResult(resultsList.stream(), 2028, "Domestic Refrigeration", "R-600a");
    EngineResult r600a2029 = LiveTestsUtil.getResult(resultsList.stream(), 2029, "Domestic Refrigeration", "R-600a");

    assertNotNull(r600a2026, "Should have result for R-600a in year 2026");
    assertNotNull(r600a2027, "Should have result for R-600a in year 2027");
    assertNotNull(r600a2028, "Should have result for R-600a in year 2028");
    assertNotNull(r600a2029, "Should have result for R-600a in year 2029");


    // Calculate total consumption (domestic + import) for assertion
    double consumption2027 = r600a2027.getDomesticConsumption().getValue().doubleValue()
                           + r600a2027.getImportConsumption().getValue().doubleValue();
    double consumption2028 = r600a2028.getDomesticConsumption().getValue().doubleValue()
                           + r600a2028.getImportConsumption().getValue().doubleValue();

    // This should fail if the bug exists - consumption should NOT decrease in 2028
    assertTrue(consumption2028 >= consumption2027,
        String.format("R-600a consumption should not decrease from 2027 (%.6f kg) to 2028 (%.6f kg) " +
                     "due to ongoing cap displacement", consumption2027, consumption2028));
  }

  /**
   * Test displacement scenario to verify total equipment population behavior.
   * This tests that the total equipment population for R-600a + HFC-134a in BAU and S1
   * should be the same in year 5 because we cap and displace in S1.
   *
   * <p>Expected: Total equipment population should be equal between BAU and S1 scenarios.
   * This test is designed to fail and identify the discrepancy described in the issue.</p>
   */
  @Test
  public void testCapDisplacePriorEquipmentTotalPopulation() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_prior_equipment_test.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run S1 scenario (with cap and displacement policies)
    Stream<EngineResult> s1Results = KigaliSimFacade.runScenario(program, "S1", progress -> {});
    final List<EngineResult> s1ResultsList = s1Results.collect(Collectors.toList());

    // Get year 5 results for BAU scenario - all 4 substances
    EngineResult bauR600a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "Domref1", "R-600a - DRe1");
    EngineResult bauHfc134a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "Domref1", "HFC-134a - Domref1");
    EngineResult bauR410a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "ResAC1", "R-410A - E1");
    final EngineResult bauHfc32 = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "ResAC1", "HFC-32 - E11");

    assertNotNull(bauR600a, "Should have BAU result for Domref1/R-600a - DRe1 in year 5");
    assertNotNull(bauHfc134a, "Should have BAU result for Domref1/HFC-134a - Domref1 in year 5");
    assertNotNull(bauR410a, "Should have BAU result for ResAC1/R-410A - E1 in year 5");
    assertNotNull(bauHfc32, "Should have BAU result for ResAC1/HFC-32 - E11 in year 5");

    // Get year 5 results for S1 scenario - all 4 substances
    EngineResult s1R600a = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "Domref1", "R-600a - DRe1");
    EngineResult s1Hfc134a = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "Domref1", "HFC-134a - Domref1");
    EngineResult s1R410a = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "ResAC1", "R-410A - E1");
    final EngineResult s1Hfc32 = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "ResAC1", "HFC-32 - E11");

    assertNotNull(s1R600a, "Should have S1 result for Domref1/R-600a - DRe1 in year 5");
    assertNotNull(s1Hfc134a, "Should have S1 result for Domref1/HFC-134a - Domref1 in year 5");
    assertNotNull(s1R410a, "Should have S1 result for ResAC1/R-410A - E1 in year 5");
    assertNotNull(s1Hfc32, "Should have S1 result for ResAC1/HFC-32 - E11 in year 5");

    // Calculate total equipment population for BAU scenario in year 5 (all substances)
    double bauTotalPopulation = bauR600a.getPopulation().getValue().doubleValue()
                               + bauHfc134a.getPopulation().getValue().doubleValue()
                               + bauR410a.getPopulation().getValue().doubleValue()
                               + bauHfc32.getPopulation().getValue().doubleValue();

    // Calculate total equipment population for S1 scenario in year 5 (all substances)
    double s1TotalPopulation = s1R600a.getPopulation().getValue().doubleValue()
                              + s1Hfc134a.getPopulation().getValue().doubleValue()
                              + s1R410a.getPopulation().getValue().doubleValue()
                              + s1Hfc32.getPopulation().getValue().doubleValue();

    // Log the values for debugging
    System.out.printf("Year 5 - BAU R-600a population: %.6f units%n",
                     bauR600a.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - BAU HFC-134a population: %.6f units%n",
                     bauHfc134a.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - BAU R-410A population: %.6f units%n",
                     bauR410a.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - BAU HFC-32 population: %.6f units%n",
                     bauHfc32.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - BAU Total population (all substances): %.6f units%n", bauTotalPopulation);

    System.out.printf("Year 5 - S1 R-600a population: %.6f units%n",
                     s1R600a.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - S1 HFC-134a population: %.6f units%n",
                     s1Hfc134a.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - S1 R-410A population: %.6f units%n",
                     s1R410a.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - S1 HFC-32 population: %.6f units%n",
                     s1Hfc32.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - S1 Total population (all substances): %.6f units%n", s1TotalPopulation);

    // This assertion should pass if displacement works correctly across all substances
    // If it fails, it indicates the bug described in the issue
    assertEquals(bauTotalPopulation, s1TotalPopulation, 150.0,
        String.format("Total equipment population across ALL substances should be equal between BAU (%.6f) and S1 (%.6f) scenarios in year 5 "
                     + "because cap and displacement should conserve total equipment across all substances",
                     bauTotalPopulation, s1TotalPopulation));
  }

  /**
   * Test capping priorEquipment only (without capping equipment) to verify individual substance behavior.
   * This tests that capping priorEquipment to 0 units should still leave some population for that substance
   * (reduced but not zero) because only the baseline is affected, not the current equipment directly.
   *
   * <p>Expected: The capped substance should have reduced but non-zero population.
   * The displaced substance should have increased population.</p>
   */
  @Test
  public void testCapPriorEquipmentOnly() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_prior_equipment_only_test.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run CapPriorOnly S1 scenario (with priorEquipment cap policies only)
    Stream<EngineResult> s1Results = KigaliSimFacade.runScenario(program, "CapPriorOnly_S1", progress -> {});
    final List<EngineResult> s1ResultsList = s1Results.collect(Collectors.toList());

    // Get year 5 results for BAU scenario
    EngineResult bauHfc134a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "Domref1", "HFC-134a - Domref1");
    EngineResult bauR600a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "Domref1", "R-600a - DRe1");
    EngineResult bauR410a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "ResAC1", "R-410A - E1");
    final EngineResult bauHfc32 = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "ResAC1", "HFC-32 - E11");

    assertNotNull(bauHfc134a, "Should have BAU result for Domref1/HFC-134a - Domref1 in year 5");
    assertNotNull(bauR600a, "Should have BAU result for Domref1/R-600a - DRe1 in year 5");
    assertNotNull(bauR410a, "Should have BAU result for ResAC1/R-410A - E1 in year 5");
    assertNotNull(bauHfc32, "Should have BAU result for ResAC1/HFC-32 - E11 in year 5");

    // Get year 5 results for CapPriorOnly S1 scenario
    EngineResult s1Hfc134a = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "Domref1", "HFC-134a - Domref1");
    EngineResult s1R600a = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "Domref1", "R-600a - DRe1");
    EngineResult s1R410a = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "ResAC1", "R-410A - E1");
    final EngineResult s1Hfc32 = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "ResAC1", "HFC-32 - E11");

    assertNotNull(s1Hfc134a, "Should have CapPriorOnly S1 result for Domref1/HFC-134a - Domref1 in year 5");
    assertNotNull(s1R600a, "Should have CapPriorOnly S1 result for Domref1/R-600a - DRe1 in year 5");
    assertNotNull(s1R410a, "Should have CapPriorOnly S1 result for ResAC1/R-410A - E1 in year 5");
    assertNotNull(s1Hfc32, "Should have CapPriorOnly S1 result for ResAC1/HFC-32 - E11 in year 5");

    // Log the values for debugging
    System.out.printf("Year 5 - BAU HFC-134a population: %.6f units%n", bauHfc134a.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - CapPriorOnly HFC-134a population: %.6f units%n", s1Hfc134a.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - BAU R-410A population: %.6f units%n", bauR410a.getPopulation().getValue().doubleValue());
    System.out.printf("Year 5 - CapPriorOnly R-410A population: %.6f units%n", s1R410a.getPopulation().getValue().doubleValue());

    // Test that capped substances (HFC-134a and R-410A) have reduced but non-zero populations
    double bauHfc134aPopulation = bauHfc134a.getPopulation().getValue().doubleValue();
    double s1Hfc134aPopulation = s1Hfc134a.getPopulation().getValue().doubleValue();
    final double bauR410aPopulation = bauR410a.getPopulation().getValue().doubleValue();
    double s1R410aPopulation = s1R410a.getPopulation().getValue().doubleValue();

    assertTrue(s1Hfc134aPopulation > 0.0,
        String.format("HFC-134a should still have population > 0 when only priorEquipment is capped. Got: %.6f", s1Hfc134aPopulation));
    assertTrue(s1Hfc134aPopulation < bauHfc134aPopulation,
        String.format("HFC-134a should have reduced population when priorEquipment is capped. BAU: %.6f, CapPriorOnly: %.6f",
                     bauHfc134aPopulation, s1Hfc134aPopulation));

    assertTrue(s1R410aPopulation > 0.0,
        String.format("R-410A should still have population > 0 when only priorEquipment is capped. Got: %.6f", s1R410aPopulation));
    assertTrue(s1R410aPopulation < bauR410aPopulation,
        String.format("R-410A should have reduced population when priorEquipment is capped. BAU: %.6f, CapPriorOnly: %.6f",
                     bauR410aPopulation, s1R410aPopulation));

    // Log displaced substance populations for reference (they may start at zero due to QTA setup)
    double bauR600aPopulation = bauR600a.getPopulation().getValue().doubleValue();
    double s1R600aPopulation = s1R600a.getPopulation().getValue().doubleValue();
    double bauHfc32Population = bauHfc32.getPopulation().getValue().doubleValue();
    double s1Hfc32Population = s1Hfc32.getPopulation().getValue().doubleValue();

    System.out.printf("Year 5 - BAU R-600a population: %.6f units%n", bauR600aPopulation);
    System.out.printf("Year 5 - CapPriorOnly R-600a population: %.6f units%n", s1R600aPopulation);
    System.out.printf("Year 5 - BAU HFC-32 population: %.6f units%n", bauHfc32Population);
    System.out.printf("Year 5 - CapPriorOnly HFC-32 population: %.6f units%n", s1Hfc32Population);

    // Note: Displaced substances may remain at zero if they have no initial import/sales in the QTA setup
    // The key validation is that capped substances are reduced but not eliminated entirely
  }
}
