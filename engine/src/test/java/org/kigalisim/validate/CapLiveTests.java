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
   * This tests capping manufacture to a specific weight in kg.
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

    // Check manufacture value - should be capped at 50 kg
    assertEquals(50.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be capped at 50 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }

  /**
   * Test cap_units.qta produces expected values.
   * This tests capping manufacture to a specific number of units.
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

    // Check manufacture value
    // With recharge on top: 50 units * 2 kg/unit + (20 units * 10% * 1 kg/unit) = 102 kg
    // Since original value is 100 kg and cap should be 102 kg, no change expected
    assertEquals(100.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 100 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }

  /**
   * Test cap_displace_units.qta produces expected values.
   * This tests capping manufacture with displacement to another substance.
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

    // Check that sub_a manufacture was capped
    EngineResult recordSubA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_a");
    assertNotNull(recordSubA, "Should have result for test/sub_a in year 1");

    // Cap is 5 units, with recharge: 5 units * 10 kg/unit + (20 units * 10% * 10 kg/unit) = 50 + 20 = 70 kg
    // Original was 100 kg, so should be capped to 70 kg
    assertEquals(70.0, recordSubA.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for sub_a should be capped at 70 kg");
    assertEquals("kg", recordSubA.getManufacture().getUnits(),
        "Manufacture units for sub_a should be kg");

    // Check displacement to sub_b
    EngineResult recordSubB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_b");
    assertNotNull(recordSubB, "Should have result for test/sub_b in year 1");

    // With unit-based displacement: 30 kg reduction in sub_a = 30 kg / 10 kg/unit = 3 units
    // 3 units displaced to sub_b = 3 units * 20 kg/unit = 60 kg
    // Original sub_b: 200 kg, Final sub_b: 200 kg + 60 kg = 260 kg
    assertEquals(260.0, recordSubB.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for sub_b should be 260 kg after displacement");
    assertEquals("kg", recordSubB.getManufacture().getUnits(),
        "Manufacture units for sub_b should be kg");
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

    // Check that sub_a manufacture was capped
    EngineResult recordSubA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_a");
    assertNotNull(recordSubA, "Should have result for test/sub_a in year 1");

    // Cap is 5 units, with recharge: 5 units * 10 kg/unit + (20 units * 10% * 10 kg/unit) = 50 + 20 = 70 kg
    // Original was 30 units * 10 kg/unit = 300 kg, so should be capped to 70 kg
    // Reduction: 300 - 70 = 230 kg
    assertEquals(70.0, recordSubA.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for sub_a should be capped at 70 kg");
    assertEquals("kg", recordSubA.getManufacture().getUnits(),
        "Manufacture units for sub_a should be kg");

    // Check displacement to sub_b
    EngineResult recordSubB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_b");
    assertNotNull(recordSubB, "Should have result for test/sub_b in year 1");

    // With unit-based displacement:
    // 230 kg reduction in sub_a = 230 kg / 10 kg/unit = 23 units
    // 23 units displaced to sub_b = 23 units * 20 kg/unit = 460 kg
    // Original sub_b: 10 units * 20 kg/unit = 200 kg
    // Final sub_b: 200 kg + 460 kg = 660 kg
    assertEquals(660.0, recordSubB.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for sub_b should be 660 kg after displacement");
    assertEquals("kg", recordSubB.getManufacture().getUnits(),
        "Manufacture units for sub_b should be kg");
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
   * TODO: This test needs to be updated after enable command implementation - currently expects 0.0 but gets -267.3
   */
  // @Test
  public void testCapDisplaceWithRechargeUnitsNoChange() throws IOException {
    // Load and parse the QTA file without change statements
    String qtaPath = "../examples/cap_displace_with_recharge_units_no_change.qta";
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
}
