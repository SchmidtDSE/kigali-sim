/**
 * Basic live tests using actual QTA files with "basic" prefix.
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
 * Tests that validate basic QTA files against expected behavior.
 */
public class BasicLiveTests {

  /**
   * Test basic.qta produces expected values.
   */
  @Test
  public void testBasic() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check equipment (population) value
    assertEquals(20000.0, result.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 20000 units");
    assertEquals("units", result.getPopulation().getUnits(),
        "Equipment units should be units");

    // Check consumption value
    assertEquals(500.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 500 tCO2e");
    assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

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

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check manufacture value - should be 100 mt = 100000 kg
    assertEquals(100000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 100000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");

    // Check energy consumption
    assertEquals(500.0, result.getEnergyConsumption().getValue().doubleValue(), 0.0001,
        "Energy consumption should be 500 kwh");
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

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check equipment (population) value
    assertEquals(200000.0, result.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 200000 units");
    assertEquals("units", result.getPopulation().getUnits(),
        "Equipment units should be units");

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

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

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

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check manufacture value - should be 1 mt = 1000 kg
    assertEquals(1000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 1000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }

  /**
   * Test basic_replace.qta produces expected values.
   * This test uses KigaliSimFacade.runScenarioWithResults to properly run the simulation.
   */
  @Test
  public void testBasicReplace() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_replace.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Sim";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 consumption (following JS test pattern)
    EngineResult recordYear1A = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "Sub A");
    assertNotNull(recordYear1A, "Should have result for Test/Sub A in year 1");

    assertEquals(10000000.0, recordYear1A.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub A GHG consumption should be 10000000 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1A.getGhgConsumption().getUnits(),
        "Sub A GHG consumption units should be tCO2e in year 1");

    EngineResult recordYear1B = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "Sub B");
    assertNotNull(recordYear1B, "Should have result for Test/Sub B in year 1");
    assertEquals(0.0, recordYear1B.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub B GHG consumption should be 0 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1B.getGhgConsumption().getUnits(),
        "Sub B GHG consumption units should be tCO2e in year 1");

    // Check year 10 consumption
    EngineResult recordYear10A = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "Sub A");
    assertNotNull(recordYear10A, "Should have result for Test/Sub A in year 10");
    assertEquals(0.0, recordYear10A.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub A GHG consumption should be 0 tCO2e in year 10");
    assertEquals("tCO2e", recordYear10A.getGhgConsumption().getUnits(),
        "Sub A GHG consumption units should be tCO2e in year 10");

    EngineResult recordYear10B = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "Sub B");
    assertNotNull(recordYear10B, "Should have result for Test/Sub B in year 10");
    assertEquals(1000000.0, recordYear10B.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub B GHG consumption should be 1000000 tCO2e in year 10");
    assertEquals("tCO2e", recordYear10B.getGhgConsumption().getUnits(),
        "Sub B GHG consumption units should be tCO2e in year 10");
  }

  /**
   * Test basic_replace_simple.qta produces expected values.
   * This test uses KigaliSimFacade.runScenarioWithResults to properly run the simulation.
   */
  @Test
  public void testBasicReplaceSimple() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_replace_simple.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Sim";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 - no replacement yet (following JS test pattern)
    EngineResult recordYear1A = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "Sub A");
    assertNotNull(recordYear1A, "Should have result for Test/Sub A in year 1");
    assertEquals(10000000.0, recordYear1A.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub A GHG consumption should be 10000000 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1A.getGhgConsumption().getUnits(),
        "Sub A GHG consumption units should be tCO2e in year 1");

    EngineResult recordYear1B = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "Sub B");
    assertNotNull(recordYear1B, "Should have result for Test/Sub B in year 1");
    assertEquals(0.0, recordYear1B.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub B GHG consumption should be 0 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1B.getGhgConsumption().getUnits(),
        "Sub B GHG consumption units should be tCO2e in year 1");

    // Check year 10 - replacement should result in complete shift from A to B
    EngineResult recordYear10A = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "Sub A");
    assertNotNull(recordYear10A, "Should have result for Test/Sub A in year 10");
    assertEquals(0.0, recordYear10A.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub A GHG consumption should be 0 tCO2e in year 10");
    assertEquals("tCO2e", recordYear10A.getGhgConsumption().getUnits(),
        "Sub A GHG consumption units should be tCO2e in year 10");

    EngineResult recordYear10B = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "Sub B");
    assertNotNull(recordYear10B, "Should have result for Test/Sub B in year 10");
    assertEquals(1000000.0, recordYear10B.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub B GHG consumption should be 1000000 tCO2e in year 10");
    assertEquals("tCO2e", recordYear10B.getGhgConsumption().getUnits(),
        "Sub B GHG consumption units should be tCO2e in year 10");
  }

  /**
   * Test basic_replace_units.qta produces expected values.
   * This test verifies units-based replacement using KigaliSimFacade.
   */
  @Test
  public void testBasicReplaceUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_replace_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Sim";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 - no replacement yet (following JS test pattern)
    EngineResult recordYear1A = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "Sub A");
    assertNotNull(recordYear1A, "Should have result for Test/Sub A in year 1");
    assertEquals(10000000.0, recordYear1A.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub A GHG consumption should be 10000000 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1A.getGhgConsumption().getUnits(),
        "Sub A GHG consumption units should be tCO2e in year 1");

    EngineResult recordYear1B = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "Sub B");
    assertNotNull(recordYear1B, "Should have result for Test/Sub B in year 1");
    assertEquals(0.0, recordYear1B.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub B GHG consumption should be 0 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1B.getGhgConsumption().getUnits(),
        "Sub B GHG consumption units should be tCO2e in year 1");

    // Check year 10 - replacement active for years 5-10 (6 years total)
    // Sub A: Original 100 mt, replaced 6 × (1000 units × 10 kg/unit) = 60 mt
    // Remaining: 40 mt × 100 tCO2e/mt = 4,000,000 tCO2e
    EngineResult recordYear10A = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "Sub A");
    assertNotNull(recordYear10A, "Should have result for Test/Sub A in year 10");
    assertEquals(4000000.0, recordYear10A.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub A GHG consumption should be 4000000 tCO2e in year 10");
    assertEquals("tCO2e", recordYear10A.getGhgConsumption().getUnits(),
        "Sub A GHG consumption units should be tCO2e in year 10");

    // Sub B: Added 6 × (1000 units × 20 kg/unit) = 120 mt
    // Total: 120 mt × 10 tCO2e/mt = 1,200,000 tCO2e
    EngineResult recordYear10B = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "Sub B");
    assertNotNull(recordYear10B, "Should have result for Test/Sub B in year 10");
    assertEquals(1200000.0, recordYear10B.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Sub B GHG consumption should be 1200000 tCO2e in year 10");
    assertEquals("tCO2e", recordYear10B.getGhgConsumption().getUnits(),
        "Sub B GHG consumption units should be tCO2e in year 10");
  }

  /**
   * Test basic_kwh_units.qta produces expected values.
   */
  @Test
  public void testBasicKwhUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_kwh_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check energy consumption
    assertEquals(500.0, result.getEnergyConsumption().getValue().doubleValue(), 0.0001,
        "Energy consumption should be 500 kwh");
    assertEquals("kwh", result.getEnergyConsumption().getUnits(),
        "Energy consumption units should be kwh");
  }

  /**
   * Test basic_kwh_units.qta produces expected values.
   */
  @Test
  public void testSetByImport() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/set_by_import.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "SubA");
    assertNotNull(result, "Should have result for test/test in year 10");

    // Check imports
    assertTrue(
        result.getImport().getValue().doubleValue() > 0,
        "Should have imports"
    );
  }

  /**
   * Test basic_set_manufacture_units.qta - equipment should increase over time.
   */
  @Test
  public void testBasicSetManufactureUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_set_manufacture_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should parse successfully");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Get results for year 2025 (start year)
    EngineResult firstRecord = LiveTestsUtil.getResult(resultsList.stream(), 2025,
        "Test", "HFC-134a");
    assertNotNull(firstRecord, "Should have result for Test/HFC-134a in year 2025");

    // Get results for year 2030 (mid-way)
    EngineResult secondRecord = LiveTestsUtil.getResult(resultsList.stream(), 2030,
        "Test", "HFC-134a");
    assertNotNull(secondRecord, "Should have result for Test/HFC-134a in year 2030");

    // Verify units are the same
    assertEquals(firstRecord.getPopulation().getUnits(), secondRecord.getPopulation().getUnits(),
        "Equipment units should be consistent");

    // Verify equipment population increases over time
    double firstPopulation = firstRecord.getPopulation().getValue().doubleValue();
    double secondPopulation = secondRecord.getPopulation().getValue().doubleValue();

    // Debug output
    System.out.println("Population in 2025: " + firstPopulation);
    System.out.println("Population in 2030: " + secondPopulation);

    assertTrue(firstPopulation < secondPopulation,
        "Equipment population should increase from 2025 to 2030. Was " + firstPopulation + " in 2025 and " + secondPopulation + " in 2030");
  }

  /**
   * Test basic_exporter.qta produces expected values with export streams.
   */
  @Test
  public void testBasicExporter() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_exporter.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "exporter scenario";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "commercial refrigeration", "HFC-134a");
    assertNotNull(result, "Should have result for commercial refrigeration/HFC-134a in year 1");

    // Check manufacture value - should be 1600 mt = 1600000 kg
    assertEquals(1600000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 1600000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");

    // Check import value - should be 400 mt = 400000 kg
    assertEquals(400000.0, result.getImport().getValue().doubleValue(), 0.0001,
        "Import should be 400000 kg");
    assertEquals("kg", result.getImport().getUnits(),
        "Import units should be kg");

    // Check export value - should be 200 mt = 200000 kg
    assertEquals(200000.0, result.getExport().getValue().doubleValue(), 0.0001,
        "Export should be 200000 kg");
    assertEquals("kg", result.getExport().getUnits(),
        "Export units should be kg");

    // Check export consumption value - should be 200 mt * 500 tCO2e/mt = 100000 tCO2e
    assertEquals(100000.0, result.getExportConsumption().getValue().doubleValue(), 0.0001,
        "Export consumption should be 100000 tCO2e");
    assertEquals("tCO2e", result.getExportConsumption().getUnits(),
        "Export consumption units should be tCO2e");

    // Check trade supplement contains export data
    assertNotNull(result.getTradeSupplement(), "Trade supplement should not be null");
    assertNotNull(result.getTradeSupplement().getExportInitialChargeValue(),
        "Export initial charge value should not be null");
    assertNotNull(result.getTradeSupplement().getExportInitialChargeConsumption(),
        "Export initial charge consumption should not be null");
  }
}
