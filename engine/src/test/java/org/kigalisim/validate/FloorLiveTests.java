/**
 * Floor live tests using actual QTA files with "floor" prefix.
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
   * Test floor_virgin.qta produces expected values.
   * This tests flooring virgin (domestic + import) to a specific weight in kg.
   */
  @Test
  public void testFloorVirgin() throws IOException {
    String qtaPath = "../examples/floor_virgin.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    double virginTotal = result.getDomestic().getValue().doubleValue()
                       + result.getImport().getValue().doubleValue();
    assertEquals(20.0, virginTotal, 0.0001,
        "Virgin (domestic + import) should be floored to 20 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");
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

  /**
   * Test floor_newequipment_absolute.qta: an absolute-units floor on newEquipment raises
   * newEquipment up to the target when it would otherwise fall short, and is a no-op when it's
   * already above the floor.
   */
  @Test
  public void testFloorNewEquipmentAbsolute() throws IOException {
    String qtaPath = "../examples/floor_newequipment_absolute.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "Result", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult belowFloor = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test",
        "BelowFloor");
    assertNotNull(belowFloor, "Should have result for Test/BelowFloor in year 1");
    assertEquals(500.0, belowFloor.getPopulationNew().getValue().doubleValue(), 0.0001,
        "BelowFloor new equipment should be raised up to exactly 500 units (from 300)");

    EngineResult aboveFloor = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test",
        "AboveFloor");
    assertNotNull(aboveFloor, "Should have result for Test/AboveFloor in year 1");
    assertEquals(1000.0, aboveFloor.getPopulationNew().getValue().doubleValue(), 0.0001,
        "AboveFloor new equipment should remain unchanged at 1000 units (floor already "
            + "satisfied)");
  }

  /**
   * Test floor_newequipment_precharge_composition.qta produces expected, self-consistent values.
   *
   * <p>Combines a precharge configuration ("recharge X% of newEquipment with Y kg/unit") with a
   * "floor newEquipment" on the same substance -- the mirror image of {@code CapLiveTests
   * .testCapNewEquipmentPrechargeComposition}. Per this design's delta-based math (Background's
   * "Key mechanical insight"), a floor's deficit-to-raise is computed purely as a units delta with
   * no explicit recharge/precharge adjustment, so precharge should not be double-counted -- but
   * this is confirmed empirically here (mirroring {@code SetLiveTests
   * .testSetNewEquipmentPrechargeComposition}'s caution about self-referential precharge), not
   * hand-derived. Year 1: domestic is set to 20 units directly (a low starting point,
   * deliberately below where year 2's floor will land), with precharge riding on top (10% of
   * newEquipment's own 20 units == 2 kg), giving domestic = 22 kg. Year 2: the floor to 50 units
   * lands populationNew at exactly 50 (confirming the floor is not fighting the precharge
   * machinery), with domestic = 55 kg (50 + 10% precharge of the now-50-unit newEquipment == 5
   * kg) -- landing on the same 55 kg / 50-unit result as the cap version once flipped, a nice
   * confirmation that this is a true mirror image.</p>
   */
  @Test
  public void testFloorNewEquipmentPrechargeComposition() throws IOException {
    String qtaPath = "../examples/floor_newequipment_precharge_composition.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult year2Result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2Result, "Should have result for test/test in year 2");
    assertEquals(50.0, year2Result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "Year 2 populationNew should be floored at exactly 50 units, not double-counting "
            + "precharge");
    assertEquals(55.0, year2Result.getDomestic().getValue().doubleValue(), 0.0001,
        "Year 2 domestic should be 55 kg (50 target + 5 precharge, i.e. 10% of the 50-unit "
            + "floor)");
  }

  /**
   * Test floor_newequipment_self_displace.qta: "floor newEquipment to X displacing sales" (bare,
   * unquoted) is a genuine self-referential contradiction (raise sales, then re-remove it from
   * sales), since a "floor newEquipment" raise is applied as a sales change internally (see
   * decision 4 / the "Displacement invocation" design note). This mirrors {@link
   * CapLiveTests#testCapNewEquipmentSelfDisplaceThrows}, confirming {@code ExceptionsGenerator
   * .raiseSelfDisplacement} fires for {@code newEquipment}'s displacement path exactly as it does
   * on {@code handleCap}'s.
   */
  @Test
  public void testFloorNewEquipmentSelfDisplaceThrows() throws IOException {
    String qtaPath = "../examples/floor_newequipment_self_displace.qta";

    try {
      ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
      Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "Result", progress -> {});
      List<EngineResult> resultsList = results.collect(Collectors.toList());

      assertTrue(false,
          "Expected an exception when flooring newEquipment displacing its own sales stream, "
          + "but none was thrown (got " + resultsList.size() + " results)");
    } catch (Exception e) {
      assertTrue(true,
          "Exception correctly thrown for newEquipment-displacing-sales self-displacement: "
          + e.getMessage());
    }
  }

  /**
   * Test floor_newequipment_displace.qta: displacement/compounding coverage for
   * "floor newEquipment ... displacing", the mirror image of {@code CapLiveTests
   * .testCapNewEquipmentDisplace}.
   *
   * <p>Setup: SubA sells 500 units in year 1 and SubB sells 1000 units in year 1 (no further
   * growth statement on either), so SubA's uncapped newEquipment stays flat at ~500 units/year
   * (recharge/precharge ride on top of the 500-unit sales basis and cancel out of the marginal
   * newEquipment computation). A policy floors SubA's {@code newEquipment} to "110% prior year"
   * starting in year 3, displacing "SubB". Because the percent basis is resolved directly against
   * newEquipment's own raw stream history (decision 2) rather than sales's lastSpecifiedValue,
   * year 3's target lands at exactly 110% of year 2's raw ~500 -- a true 50-unit deficit -- giving
   * SubA exactly 550 and (since both substances share a 1 kg/unit domestic initial charge) SubB
   * exactly 950 (1000 - 50).</p>
   *
   * <p>Year 4+ compounding (empirically confirmed, not hand-derived, exactly matching the
   * hand-derived prediction): the floor's raise is applied via {@code changeStream("sales",
   * delta)}, which permanently rebases SubA's sales at the new, higher level (matching how a
   * one-time "set ... during year 1" statement establishes a flat baseline). Since nothing else
   * changes SubA's sales afterward, each subsequent year's "prior year" newEquipment lookup reads
   * the previous year's already-floored raw value, not the original ~500 -- so the floor compounds
   * geometrically at 110%/year from year 3 onward ({@code 500 * 1.1^(year - 2)}), and SubB's
   * cumulative displaced-away total telescopes to exactly {@code 1500 - SubA_current} every year
   * (the starting combined total of 500 + 1000).</p>
   */
  @Test
  public void testFloorNewEquipmentDisplace() throws IOException {
    String qtaPath = "../examples/floor_newequipment_displace.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(
        program, "With Permit", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult year3SubA = LiveTestsUtil.getResult(resultsList.stream(), 3, "Test", "SubA");
    assertNotNull(year3SubA, "Should have result for Test/SubA in year 3");
    assertEquals(550.0, year3SubA.getPopulationNew().getValue().doubleValue(), 0.0001,
        "SubA new equipment should be exactly 550 units in year 3 (110% of prior year's 500)");

    EngineResult year3SubB = LiveTestsUtil.getResult(resultsList.stream(), 3, "Test", "SubB");
    assertNotNull(year3SubB, "Should have result for Test/SubB in year 3");
    assertEquals(950.0, year3SubB.getPopulationNew().getValue().doubleValue(), 0.0001,
        "SubB new equipment should be exactly 950 units in year 3 (1000 - 50 displaced deficit)");

    // Year 10 compounding check: 500 * 1.1^8 = 1071.794405 exactly (see class-level comment on
    // the geometric compounding pattern), and SubB telescopes to exactly 1500 - SubA.
    EngineResult year10SubA = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "SubA");
    assertNotNull(year10SubA, "Should have result for Test/SubA in year 10");
    assertEquals(1071.794405, year10SubA.getPopulationNew().getValue().doubleValue(), 0.0001,
        "SubA new equipment should be exactly 500 * 1.1^8 = 1071.794405 units by year 10");

    EngineResult year10SubB = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "SubB");
    assertNotNull(year10SubB, "Should have result for Test/SubB in year 10");
    assertEquals(428.205595, year10SubB.getPopulationNew().getValue().doubleValue(), 0.0001,
        "SubB new equipment should be exactly 1500 - 1071.794405 = 428.205595 units by year 10");
  }
}
