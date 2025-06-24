/**
 * Cap live tests using actual QTA files with "cap" prefix.
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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

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
}
