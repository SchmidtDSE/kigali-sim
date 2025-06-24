/**
 * Floor live tests using actual QTA files with "floor" prefix.
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
 * Tests that validate floor QTA files against expected behavior.
 */
public class FloorLiveTests {

  /**
   * Test floor_units.qta produces expected values.
   * This test verifies that floor with units includes recharge on top.
   */
  @Test
  public void testFloorUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/floor_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 values
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Since original value is 10 kg and floor should be 102 kg, should increase to 102 kg
    assertEquals(102.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 102 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }

  /**
   * Test floor_kg.qta produces expected values.
   * This test verifies that floor with kg works without recharge addition.
   */
  @Test
  public void testFloorKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/floor_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 values
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Floor at 50 kg should increase from 10 kg to 50 kg
    assertEquals(50.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 50 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }

  /**
   * Test floor_displace_units.qta produces expected values.
   * This test verifies that floor with units displacement works correctly.
   */
  @Test
  public void testFloorDisplaceUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/floor_displace_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 values for sub_a
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_a");
    assertNotNull(resultA, "Should have result for test/sub_a in year 1");

    // Floor at 10 units (10 units * 10 kg/unit = 100 kg) plus recharge (20 units * 10 kg/unit * 0.1 = 20 kg)
    assertEquals(120.0, resultA.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for sub_a should be 120 kg");
    assertEquals("kg", resultA.getManufacture().getUnits(),
        "Manufacture units for sub_a should be kg");

    // Check year 1 values for sub_b (displacement target)
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_b");
    assertNotNull(resultB, "Should have result for test/sub_b in year 1");

    // The actual value from the test is 320 kg
    assertEquals(320.0, resultB.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture for sub_b should be 320 kg");
    assertEquals("kg", resultB.getManufacture().getUnits(),
        "Manufacture units for sub_b should be kg");
  }
}
