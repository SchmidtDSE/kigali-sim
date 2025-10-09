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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 values
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Since original value is 10 kg and floor should be 102 kg, should increase to 102 kg
    assertEquals(102.0, result.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic should be 102 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");
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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 values
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Floor at 50 kg should increase from 10 kg to 50 kg
    assertEquals(50.0, result.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic should be 50 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");
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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 values for sub_a
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_a");
    assertNotNull(resultA, "Should have result for test/sub_a in year 1");

    // Floor at 10 units with proportional recharge distribution:
    // Base amount: 10 units * 10 kg/unit = 100 kg
    // Total recharge: 20 units * 10% * 10 kg/unit = 20 kg
    // Sales distribution: domestic=10kg (66.67%), import=5kg (33.33%)
    // Domestic recharge: 20 kg * 66.67% = 13.33 kg
    // Total domestic: 100 kg + 13.33 kg = 113.33 kg
    assertEquals(113.33333333333333, resultA.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic for sub_a should be 113.33 kg (100 + 13.33 proportional recharge)");
    assertEquals("kg", resultA.getDomestic().getUnits(),
        "Domestic units for sub_a should be kg");

    // Check year 1 values for sub_b (displacement target)
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_b");
    assertNotNull(resultB, "Should have result for test/sub_b in year 1");

    // The actual value from the test is 320 kg
    assertEquals(0., resultB.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic for sub_b should be zero");
    assertEquals("kg", resultB.getDomestic().getUnits(),
        "Domestic units for sub_b should be kg");
  }

  /**
   * Test floor on equipment stream when no action is required.
   * This verifies that when equipment is already above the floor, no changes occur.
   */
  @Test
  public void testFloorEquipmentNoAction() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/floor_equipment_no_action.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Equipment should be unchanged (100 new + 50 prior = 150 units total)
    // Since floor is 50 units and we're at 150, no action should be taken
    assertEquals(150.0, result.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment population should be unchanged at 150 units (above floor)");
    assertEquals("units", result.getPopulation().getUnits(),
        "Equipment units should be units");

    // New equipment should also be unchanged at 100 units
    assertEquals(100.0, result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "New equipment should be unchanged at 100 units");
  }

  /**
   * Test floor on equipment stream when action is required.
   * This verifies that when equipment is below the floor, it is increased appropriately.
   */
  @Test
  public void testFloorEquipmentWithAction() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/floor_equipment_with_action.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Equipment should meet the floor at 100 units (increased from 70)
    // Original: 50 new + 20 prior = 70 units
    // After floor: should be 100 units total (30 units added via sales increase)
    assertEquals(100.0, result.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment population should meet floor at 100 units");
    assertEquals("units", result.getPopulation().getUnits(),
        "Equipment units should be units");

    // New equipment should be 80 units (original 50 + 30 added)
    assertEquals(80.0, result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "New equipment should be 80 units (50 original + 30 from floor increase)");
  }
}
