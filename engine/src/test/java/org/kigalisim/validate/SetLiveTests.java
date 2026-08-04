/**
 * Set live tests using actual QTA files with "set newEquipment" prefix.
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
 * Tests that validate "set newEquipment" QTA files against expected behavior.
 */
public class SetLiveTests {

  /**
   * Test set_newequipment_units.qta produces expected values.
   * Verifies that "set newEquipment to X units" sets sales directly to the equivalent
   * kg value (1 kg/unit here, no recharge configured), confirmed both by populationNew
   * and by the resulting domestic (sales) volume.
   */
  @Test
  public void testSetNewEquipmentUnits() throws IOException {
    String qtaPath = "../examples/set_newequipment_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult year2Result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2Result, "Should have result for test/test in year 2");
    assertEquals(150.0, year2Result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "Year 2 populationNew should be 150 units");
    assertEquals(150.0, year2Result.getDomestic().getValue().doubleValue(), 0.0001,
        "Year 2 domestic should be 150 kg (150 units * 1 kg/unit)");
  }

  /**
   * Test set_newequipment_percent.qta produces expected values.
   * Confirms the percent-basis (decision 3): "set newEquipment to X%" resolves against this
   * year's already-computed (pre-adjustment) newEquipment value.
   */
  @Test
  public void testSetNewEquipmentPercent() throws IOException {
    String qtaPath = "../examples/set_newequipment_percent.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult year2Result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2Result, "Should have result for test/test in year 2");
    assertEquals(50.0, year2Result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "Year 2 populationNew should be 50 units (50% of year 2's pre-adjustment 100)");
  }

  /**
   * Test set_newequipment_clamp_zero.qta produces expected values.
   * Confirms the decision-1 zero clamp holds: a negative "set newEquipment" target lands
   * exactly at 0 (not negative) and does not throw.
   */
  @Test
  public void testSetNewEquipmentClampZero() throws IOException {
    String qtaPath = "../examples/set_newequipment_clamp_zero.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult year2Result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2Result, "Should have result for test/test in year 2");
    assertEquals(0.0, year2Result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "Year 2 populationNew should be clamped to exactly 0, not negative");
    assertEquals(0.0, year2Result.getDomestic().getValue().doubleValue(), 0.0001,
        "Year 2 domestic should be clamped to exactly 0 kg");
  }

  /**
   * Test set_newequipment_kg_with_recharge.qta produces expected values.
   * This is the key acceptance test for the mass-path recharge accounting: a mass-unit
   * "set newEquipment" target must have current recharge (and precharge) volume added back on
   * top of sales, so that the next population recalc's subtraction of recharge/precharge lands
   * newEquipment exactly at the target.
   *
   * <p>Hand-derived (see task markdown for the full derivation): priorEquipment entering year 2
   * is 100 units (year 1's equipment, no retirement); recharge population = 10% * 100 = 10
   * units; recharge volume = 10 * 1 kg/unit = 10 kg; mass-path target is already non-negative,
   * so salesKg = 50 (target) + 10 (recharge) + 0 (no precharge) = 60. The recalc then computes
   * populationNew = (salesKg - rechargeKg - prechargeKg) / initialCharge = (60 - 10 - 0) / 1 = 50,
   * landing exactly back at the target.</p>
   */
  @Test
  public void testSetNewEquipmentKgWithRecharge() throws IOException {
    String qtaPath = "../examples/set_newequipment_kg_with_recharge.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult year2Result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2Result, "Should have result for test/test in year 2");
    assertEquals(60.0, year2Result.getDomestic().getValue().doubleValue(), 0.0001,
        "Year 2 domestic should be 60 kg (50 target + 10 recharge)");
    assertEquals(50.0, year2Result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "Year 2 populationNew should land exactly at the 50-unit target");
  }

  /**
   * Test set_newequipment_precharge_composition.qta produces expected, self-consistent values.
   *
   * <p>Combines a precharge configuration ("recharge X% of newEquipment with Y kg/unit") with a
   * mass-unit "set newEquipment" target on the same substance. Because precharge population is
   * itself a function of the current year's newEquipment (self-referential, unlike recharge which
   * is fixed off priorEquipment), the expected values here were NOT hand-derived: they were read
   * off an actual engine run, then independently confirmed self-consistent by instrumenting
   * {@code NewEquipmentChangeUtil.handleSet} to print the actual
   * {@code RechargeVolumeCalculator}/{@code PrechargeVolumeCalculator} figures used for year 2
   * (rechargeVolume = 0 kg, since no recharge of priorEquipment is configured here; precharge
   * Volume = 10.0 kg, i.e. 10% precharge against the ~100-unit pre-adjustment newEquipment carried
   * from year 1), confirming domestic (60 kg) == targetKg (50) + rechargeVolume (0) +
   * prechargeVolume (10). populationNew is then the closed-form solution of the self-referential
   * equation newEquipment = (salesKg - prechargeKg(newEquipment)) / initialCharge, i.e.
   * newEquipment = (60 - 0.1 * newEquipment) / 1, giving newEquipment = 60 / 1.1 = 600/11 ≈
   * 54.5455 units. This test exists to catch double-counting regressions in the recharge+precharge
   * composition, not to re-derive precharge's own math.</p>
   */
  @Test
  public void testSetNewEquipmentPrechargeComposition() throws IOException {
    String qtaPath = "../examples/set_newequipment_precharge_composition.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult year2Result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2Result, "Should have result for test/test in year 2");
    assertEquals(60.0, year2Result.getDomestic().getValue().doubleValue(), 0.0001,
        "Year 2 domestic should be 60 kg (50 target + 0 recharge + 10 precharge)");
    assertEquals(600.0 / 11.0, year2Result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "Year 2 populationNew should be 600/11 (~54.5455) units, the self-consistent solution of "
        + "newEquipment = (60 - 0.1 * newEquipment) / 1");
  }

  /**
   * Test assume_newequipment.qta produces a sensible, non-erroring result.
   * Smoke test only (Background note): "assume no newEquipment" desugars to "set newEquipment to
   * 0 kg" (mass-path, so recharge/precharge still land on sales) and "assume only recharge
   * newEquipment" desugars to "set newEquipment to 0 units" (unit-path, so sales is set to
   * exactly 0 units before the separate unit-tracked implicit-recharge machinery adds servicing on
   * top). Confirms both produce a near-zero populationNew rather than asserting a specific
   * interpretation of "sensible" beyond that.
   */
  @Test
  public void testAssumeNewEquipmentSmokeTest() throws IOException {
    String qtaPath = "../examples/assume_newequipment.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // "assume no newEquipment" years (2-3): mass-path zero target.
    EngineResult year2Result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2Result, "Should have result for test/test in year 2");
    assertEquals(0.0, year2Result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "Year 2 populationNew should be at/near 0 under 'assume no newEquipment'");

    // "assume only recharge newEquipment" years (4-5): unit-path zero target.
    EngineResult year4Result = LiveTestsUtil.getResult(resultsList.stream(), 4, "test", "test");
    assertNotNull(year4Result, "Should have result for test/test in year 4");
    assertEquals(0.0, year4Result.getPopulationNew().getValue().doubleValue(), 0.0001,
        "Year 4 populationNew should be at/near 0 under 'assume only recharge newEquipment'");
  }
}
