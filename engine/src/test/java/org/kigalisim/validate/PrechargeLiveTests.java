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
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Live tests for precharge (recharge of newEquipment) functionality.
 *
 * <p>These tests verify the behavior of precharge calculations using the
 * "of newEquipment" clause on recharge statements.</p>
 */
public class PrechargeLiveTests {

  /**
   * Test that recharge with "of priorEquipment" produces same results as bare recharge.
   */
  @Test
  public void testRechargeOfPriorEqualsBareRecharge() throws IOException {
    String qtaPath = "../examples/recharge_of_prior.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Should match the results from recharge.qta (which uses bare "recharge 10% with 1 kg / unit")
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(resultYear1, "Should have result for test/test in year 1");
    assertEquals(100000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 100000 units in year 1 (same as bare recharge)");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");

    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(resultYear2, "Should have result for test/test in year 2");
    assertEquals(190000.0, resultYear2.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 190000 units in year 2 (same as bare recharge)");
  }

  /**
   * Test basic precharge with units-based tracking.
   *
   * <p>With 1000 new units, 5% precharge = 50 units × 1 kg/unit = 50 kg precharge.
   * Equipment should be 10000 prior + 1000 new = 11000 units.
   * Recharge emissions should include precharge contribution (50 kg × 1 tCO2e/kg = 50 tCO2e).</p>
   */
  @Test
  public void testPrechargeUnits() throws IOException {
    String qtaPath = "../examples/precharge_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: Equipment should be 11000 units (10000 prior + 1000 new)
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub1");
    assertNotNull(resultYear1, "Should have result for App/Sub1 in year 1");
    assertEquals(11000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 11000 units in year 1 (10000 prior + 1000 new)");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");

    // Year 1: RechargeEmissions should be 50 tCO2e (50 kg precharge x 1 tCO2e / kg)
    EngineNumber rechargeEmissions = resultYear1.getRechargeEmissions();
    assertNotNull(rechargeEmissions, "Recharge emissions should not be null");
    assertEquals(50.0, rechargeEmissions.getValue().doubleValue(), 0.0001,
        "Recharge emissions should be 50 tCO2e (from precharge)");
  }

  /**
   * Test precharge combined with recharge (both prior and new equipment).
   */
  @Test
  public void testPrechargeWithRecharge() throws IOException {
    String qtaPath = "../examples/precharge_with_recharge.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: Equipment should be 11000 units (10000 prior + 1000 new)
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub1");
    assertNotNull(resultYear1, "Should have result for App/Sub1 in year 1");
    assertEquals(11000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 11000 units in year 1");

    // Year 1: RechargeEmissions should include both recharge (prior) and precharge (new):
    // recharge = 5% of 10000 prior units * 1 kg/unit = 500 kg
    // precharge = 3% of 1000 new units * 1 kg/unit = 30 kg
    // total = 530 kg * 1 tCO2e/kg = 530 tCO2e
    EngineNumber rechargeEmissions = resultYear1.getRechargeEmissions();
    assertNotNull(rechargeEmissions, "Recharge emissions should not be null");
    assertEquals(530.0, rechargeEmissions.getValue().doubleValue(), 0.0001,
        "Recharge emissions should be 530 tCO2e (500 recharge + 30 precharge)");
  }

  /**
   * Test precharge with a change statement that modifies sales.
   */
  @Test
  public void testPrechargeWithChange() throws IOException {
    String qtaPath = "../examples/precharge_with_change.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: Equipment should be 11000 units (10000 prior + 1000 new)
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub1");
    assertNotNull(resultYear1, "Should have result for App/Sub1 in year 1");
    assertEquals(11000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 11000 units in year 1");

    // Year 2: Should have more units due to +10% change in domestic
    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "App", "Sub1");
    assertNotNull(resultYear2, "Should have result for App/Sub1 in year 2");
    assertTrue(resultYear2.getPopulation().getValue().doubleValue() > 11000.0,
        "Equipment should increase in year 2 due to +10% change: "
            + resultYear2.getPopulation().getValue());
  }

  /**
   * Test precharge combined with recharge-stage recovery (recycling).
   *
   * <p>Setup: import = 105 kg flat (year 2021 onward), initial charge 1 kg/unit,
   * precharge 5% of newEquipment with 1 kg/unit, and an absolute recover of
   * 20 kg with 100% reuse and 0% induction (full displacement) every year.</p>
   *
   * <p>Because recovery is specified as an absolute 20 kg (not a percentage of
   * recharge volume) and no recharge of priorEquipment is configured, the recovered
   * amount does not depend on rechargeVolume. With 0% induction the recycled
   * material fully displaces virgin material 1-for-1: the "sales" stream used by
   * the population calculation (domestic + import + recycle) is unaffected by
   * recycling, so newEquipment is identical to the no-recycling case:</p>
   *
   * <pre>
   * newEquipment = (salesKg - rechargeKg) / (initialCharge + prechargeRatio * prechargeIntensity)
   *              = (105 - 0) / (1 + 0.05 * 1) = 100 units/year
   * </pre>
   *
   * <p>Only the virgin/recycled split of sales changes: virgin import = 105 - 20 =
   * 85 kg and recycle = 20 kg, while total sales remains 105 kg.</p>
   */
  @Test
  public void testPrechargeWithRecover() throws IOException {
    String qtaPath = "../examples/precharge_with_recover.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    for (int year = 2021; year <= 2023; year++) {
      EngineResult result = LiveTestsUtil.getResult(
          resultsList.stream(), year, "Domestic Refrigeration", "HFC-134a");
      assertNotNull(result, "Should have result for year " + year);

      assertEquals(100.0, result.getPopulationNew().getValue().doubleValue(), 0.0001,
          "newEquipment should be 100 units in year " + year
              + " (recovery with 0% induction should not change population growth)");

      assertEquals(85.0, result.getImport().getValue().doubleValue(), 0.0001,
          "Virgin import should be 105 - 20 (recycled) = 85 kg in year " + year);

      assertEquals(20.0, result.getRecycle().getValue().doubleValue(), 0.0001,
          "Recycled amount should be 20 kg (full recovery, 100% reuse) in year " + year);
    }

    // Cumulative population should grow by the same 100 units/year increment
    EngineResult resultYear2023 = LiveTestsUtil.getResult(
        resultsList.stream(), 2023, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(resultYear2023, "Should have result for year 2023");
    assertEquals(300.0, resultYear2023.getPopulation().getValue().doubleValue(), 0.0001,
        "Population should be 300 units by year 2023 (100 units/year for 3 years)");
  }

  /**
   * Pin down that the "sales" total stays reconciled with actual newEquipment needs
   * even when recover (recycling) recalculation runs before the population-change
   * closed form for the circular percent-precharge + kg-tracked-sales case.
   *
   * <p>Background (see issue #807 / PR #808): {@code PrechargeVolumeCalculator}, used by
   * {@code SalesRecalcStrategy} to size domestic/import demand, reads whatever
   * {@code newEquipment} is currently persisted in engine state rather than deriving it
   * from the closed-form formula. For {@code recover}, the engine calls
   * {@code recalcSales()} BEFORE {@code thenPropagateToPopulationChange()}
   * (see {@code SingleThreadEngine.recycle}), so the precharge contribution baked into
   * that call's sales total is based on the newEquipment count from the previous
   * recalculation, not the one that population-change is about to compute.</p>
   *
   * <p>This does not manifest as an observable inconsistency: {@code
   * PopulationChangeRecalcStrategy} always solves for newEquipment from whatever
   * "sales" total ends up persisted using the exact closed-form denominator
   * {@code (initialCharge + prechargeRatio * prechargeIntensity)}, so
   * {@code domestic + import + recycle} is guaranteed by construction to equal
   * {@code recharge + precharge(newEquipment) + initialCharge * newEquipment} for the
   * FINAL newEquipment value, regardless of which (possibly stale) newEquipment
   * estimate fed into sizing sales upstream. The stale estimate only affects how the
   * population-growth target set by {@code recalcSales} is intended to be, not whether
   * the final books balance.</p>
   *
   * <p>Setup: recharge 5% of priorEquipment with 1 kg/unit AND precharge 5% of
   * newEquipment with 1 kg/unit (the circular case) with import tracked in kg (100 kg
   * flat), then recover 50% with 100% reuse at the recharge stage (0% induction)
   * starting in year 2023 - the year staleness would be most visible since the
   * recharge base has already grown for two years by then.</p>
   */
  @Test
  public void testPercentPrechargeSalesInvariantHoldsThroughRecover() throws IOException {
    String qtaPath = "../examples/precharge_staleness_check.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    double priorEquipment = 0.0;
    for (int year = 2021; year <= 2026; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year, "Test", "Sub");
      assertNotNull(result, "Should have result for year " + year);

      double domestic = result.getDomestic().getValue().doubleValue();
      double importKg = result.getImport().getValue().doubleValue();
      double recycle = result.getRecycle().getValue().doubleValue();
      double salesTotal = domestic + importKg + recycle;

      double newEquipment = result.getPopulationNew().getValue().doubleValue();
      double expectedRecharge = 0.05 * priorEquipment;
      double expectedPrecharge = 0.05 * newEquipment;
      double expectedInitialCharge = 1.0 * newEquipment;
      double expectedSalesTotal = expectedRecharge + expectedPrecharge + expectedInitialCharge;

      assertEquals(expectedSalesTotal, salesTotal, 0.0001,
          "domestic + import + recycle should equal recharge + precharge + initial charge "
              + "for the actual newEquipment in year " + year);

      priorEquipment = result.getPopulation().getValue().doubleValue();
    }
  }

  /**
   * Test precharge combined with a "cap" policy that reduces import sales.
   *
   * <p>Setup: BAU import = 105 kg flat, initial charge 1 kg/unit, precharge 5% of
   * newEquipment with 1 kg/unit. The "Cap Import" policy caps import to 63 kg
   * every year, which is below the BAU value so the cap is always binding.</p>
   *
   * <p>Cap is applied by directly setting the stream to the cap value and
   * re-running the same population recalculation used by "set" (the circular
   * percent-precharge case), so:</p>
   *
   * <pre>
   * BAU newEquipment      = 105 / (1 + 0.05 * 1) = 100 units/year
   * With Cap newEquipment =  63 / (1 + 0.05 * 1) =  60 units/year
   * </pre>
   *
   * <p>Precharge emissions follow directly from newEquipment: precharge kg =
   * newEquipment * 5% * 1 kg/unit, converted to tCO2e using 1430 kgCO2e / kg.</p>
   */
  @Test
  public void testPrechargeWithCap() throws IOException {
    String qtaPath = "../examples/precharge_with_cap.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    Stream<EngineResult> bauStream = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResults = bauStream.collect(Collectors.toList());

    Stream<EngineResult> capStream = KigaliSimFacade.runScenario(program, "With Cap", progress -> {});
    List<EngineResult> capResults = capStream.collect(Collectors.toList());

    for (int year = 2021; year <= 2023; year++) {
      EngineResult bauResult = LiveTestsUtil.getResult(
          bauResults.stream(), year, "Domestic Refrigeration", "HFC-134a");
      assertNotNull(bauResult, "Should have BAU result for year " + year);
      assertEquals(100.0, bauResult.getPopulationNew().getValue().doubleValue(), 0.0001,
          "BAU newEquipment should be 100 units in year " + year);
      assertEquals(105.0, bauResult.getImport().getValue().doubleValue(), 0.0001,
          "BAU import should be 105 kg in year " + year);

      EngineResult capResult = LiveTestsUtil.getResult(
          capResults.stream(), year, "Domestic Refrigeration", "HFC-134a");
      assertNotNull(capResult, "Should have Cap result for year " + year);
      assertEquals(60.0, capResult.getPopulationNew().getValue().doubleValue(), 0.0001,
          "Capped newEquipment should be 60 units in year " + year
              + " (63 kg / 1.05 kg-per-unit)");
      assertEquals(63.0, capResult.getImport().getValue().doubleValue(), 0.0001,
          "Capped import should be 63 kg in year " + year);

      // Precharge kg = newEquipment * 5% * 1 kg/unit; emissions = precharge kg * 1430 kgCO2e/kg.
      double expectedCapRechargeEmissionsTco2e = 60.0 * 0.05 * 1.0 * 1430.0 / 1000.0;
      assertEquals(expectedCapRechargeEmissionsTco2e,
          capResult.getRechargeEmissions().getValue().doubleValue(), 0.0001,
          "Capped recharge (precharge) emissions should be " + expectedCapRechargeEmissionsTco2e
              + " tCO2e in year " + year);
    }

    // Cap should strictly reduce cumulative population relative to BAU by year 2023.
    EngineResult bauYear2023 = LiveTestsUtil.getResult(
        bauResults.stream(), 2023, "Domestic Refrigeration", "HFC-134a");
    EngineResult capYear2023 = LiveTestsUtil.getResult(
        capResults.stream(), 2023, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bauYear2023, "Should have BAU result for year 2023");
    assertNotNull(capYear2023, "Should have Cap result for year 2023");
    assertEquals(300.0, bauYear2023.getPopulation().getValue().doubleValue(), 0.0001,
        "BAU population should be 300 units by year 2023");
    assertEquals(180.0, capYear2023.getPopulation().getValue().doubleValue(), 0.0001,
        "Capped population should be 180 units by year 2023");
  }

  /**
   * Test precharge combined with a "floor" policy that raises import sales.
   *
   * <p>Setup: BAU import = 21 kg flat, initial charge 1 kg/unit, precharge 5% of
   * newEquipment with 1 kg/unit. The "Floor Import" policy floors import to 73.5 kg
   * every year, which is above the BAU value so the floor is always binding.</p>
   *
   * <pre>
   * BAU newEquipment        = 21 / (1 + 0.05 * 1) = 20 units/year
   * With Floor newEquipment = 73.5 / (1 + 0.05 * 1) = 70 units/year
   * </pre>
   */
  @Test
  public void testPrechargeWithFloor() throws IOException {
    String qtaPath = "../examples/precharge_with_floor.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    Stream<EngineResult> bauStream = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResults = bauStream.collect(Collectors.toList());

    Stream<EngineResult> floorStream = KigaliSimFacade.runScenario(program, "With Floor", progress -> {});
    List<EngineResult> floorResults = floorStream.collect(Collectors.toList());

    for (int year = 2021; year <= 2023; year++) {
      EngineResult bauResult = LiveTestsUtil.getResult(
          bauResults.stream(), year, "Domestic Refrigeration", "HFC-134a");
      assertNotNull(bauResult, "Should have BAU result for year " + year);
      assertEquals(20.0, bauResult.getPopulationNew().getValue().doubleValue(), 0.0001,
          "BAU newEquipment should be 20 units in year " + year);
      assertEquals(21.0, bauResult.getImport().getValue().doubleValue(), 0.0001,
          "BAU import should be 21 kg in year " + year);

      EngineResult floorResult = LiveTestsUtil.getResult(
          floorResults.stream(), year, "Domestic Refrigeration", "HFC-134a");
      assertNotNull(floorResult, "Should have Floor result for year " + year);
      assertEquals(70.0, floorResult.getPopulationNew().getValue().doubleValue(), 0.0001,
          "Floored newEquipment should be 70 units in year " + year
              + " (73.5 kg / 1.05 kg-per-unit)");
      assertEquals(73.5, floorResult.getImport().getValue().doubleValue(), 0.0001,
          "Floored import should be 73.5 kg in year " + year);

      // Precharge kg = newEquipment * 5% * 1 kg/unit; emissions = precharge kg * 1430 kgCO2e/kg.
      double expectedFloorRechargeEmissionsTco2e = 70.0 * 0.05 * 1.0 * 1430.0 / 1000.0;
      assertEquals(expectedFloorRechargeEmissionsTco2e,
          floorResult.getRechargeEmissions().getValue().doubleValue(), 0.0001,
          "Floored recharge (precharge) emissions should be " + expectedFloorRechargeEmissionsTco2e
              + " tCO2e in year " + year);
    }

    // Floor should strictly increase cumulative population relative to BAU by year 2023.
    EngineResult bauYear2023 = LiveTestsUtil.getResult(
        bauResults.stream(), 2023, "Domestic Refrigeration", "HFC-134a");
    EngineResult floorYear2023 = LiveTestsUtil.getResult(
        floorResults.stream(), 2023, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bauYear2023, "Should have BAU result for year 2023");
    assertNotNull(floorYear2023, "Should have Floor result for year 2023");
    assertEquals(60.0, bauYear2023.getPopulation().getValue().doubleValue(), 0.0001,
        "BAU population should be 60 units by year 2023");
    assertEquals(210.0, floorYear2023.getPopulation().getValue().doubleValue(), 0.0001,
        "Floored population should be 210 units by year 2023");
  }

  /**
   * Test that recharge of newEquipment runs and produces results.
   */
  @Test
  public void testRechargeOfNewEquipmentRuns() throws IOException {
    String qtaPath = "../examples/recharge_of_new.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    assertTrue(resultsList.size() > 0, "Should have results for recharge of newEquipment");
  }

  // ===== Sanity check helpers =====

  private static final String APP = "Domestic Refrigeration";
  private static final String SUB = "HFC-134a";

  private List<EngineResult> runScenarioFromQta(String qtaPath, String scenarioName)
      throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null: " + qtaPath);
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName,
        progress -> {});
    return results.collect(Collectors.toList());
  }

  private double getImport(List<EngineResult> results, int year) {
    EngineResult result = LiveTestsUtil.getResult(results.stream(), year, APP, SUB);
    assertNotNull(result, "Should have result for year " + year);
    return result.getImport().getValue().doubleValue();
  }

  private double getNewEquipment(List<EngineResult> results, int year) {
    EngineResult result = LiveTestsUtil.getResult(results.stream(), year, APP, SUB);
    assertNotNull(result, "Should have result for year " + year);
    return result.getPopulationNew().getValue().doubleValue();
  }

  private void assertBauFlat(List<EngineResult> results, double expectedImport,
      Double expectedNewEquipment, String label) {
    for (int year = 2021; year <= 2030; year++) {
      double importValue = getImport(results, year);
      assertEquals(expectedImport, importValue, 0.0001,
          label + " BAU import should be " + expectedImport + " kg in year " + year
              + " but was " + importValue);
      if (expectedNewEquipment != null) {
        double newEquip = getNewEquipment(results, year);
        assertEquals(expectedNewEquipment, newEquip, 0.01,
            label + " BAU newEquipment should be " + expectedNewEquipment + " units in year "
                + year + " but was " + newEquip);
      }
    }
  }

  private void assertRepairIncreasing(List<EngineResult> repairResults,
      List<EngineResult> bauResults, String label, boolean unitsBasedSales) {
    double previous = -1;
    for (int year = 2021; year <= 2030; year++) {
      double importValue = getImport(repairResults, year);
      double bauImport = getImport(bauResults, year);

      if (unitsBasedSales) {
        // For units-based sales, repair recharge is added on top → import should increase
        if (year == 2021) {
          assertTrue(importValue >= bauImport,
              label + " With Repair import (" + importValue + ") should be >= BAU ("
                  + bauImport + ") in year 1");
        } else {
          assertTrue(importValue > bauImport,
              label + " With Repair import (" + importValue + ") should be higher than BAU ("
                  + bauImport + ") in year " + year);
        }
      } else {
        // For volume-based sales, import is fixed by user; repair reduces new equipment instead
        assertEquals(bauImport, importValue, 0.0001,
            label + " With Repair import should equal BAU in year " + year
                + " (volume-based sales are fixed)");
        double repairNewEquip = getNewEquipment(repairResults, year);
        double bauNewEquip = getNewEquipment(bauResults, year);
        if (year > 2021) {
          assertTrue(repairNewEquip < bauNewEquip,
              label + " With Repair newEquipment (" + repairNewEquip
                  + ") should be lower than BAU (" + bauNewEquip + ") in year " + year
                  + " (repair consumes volume)");
        }
      }

      if (previous > 0 && unitsBasedSales) {
        assertTrue(importValue > previous,
            label + " With Repair import should increase each year. Year " + year
                + ": " + importValue + " vs previous: " + previous);
      }
      previous = importValue;
    }
  }

  private void assertIncreaseTrend(List<EngineResult> results, double baselineImport,
      String label) {
    double previous = -1;
    for (int year = 2021; year <= 2030; year++) {
      double importValue = getImport(results, year);
      if (year == 2021) {
        assertEquals(baselineImport, importValue, 0.0001,
            label + " With Increase import should be " + baselineImport
                + " kg in 2021 but was " + importValue);
      } else {
        assertTrue(importValue > previous,
            label + " With Increase import should increase after 2021. Year " + year
                + ": " + importValue + " vs previous: " + previous);
      }
      previous = importValue;
    }
  }

  private void assertDecreaseTrend(List<EngineResult> results, double baselineImport,
      String label) {
    double previous = -1;
    for (int year = 2021; year <= 2030; year++) {
      double importValue = getImport(results, year);
      if (year == 2021) {
        assertEquals(baselineImport, importValue, 0.0001,
            label + " With Decrease import should be " + baselineImport
                + " kg in 2021 but was " + importValue);
      } else {
        assertTrue(importValue < previous,
            label + " With Decrease import should decrease after 2021. Year " + year
                + ": " + importValue + " vs previous: " + previous);
      }
      previous = importValue;
    }
  }

  // ===== percent_units: set import to 100 units + recharge 5 % of newEquipment =====
  // BAU: import = 105 kg flat, newEquipment = 100 units flat

  @Test
  public void testSanityCheckPercentUnitsBau() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_percent_units.qta", "BAU");
    assertBauFlat(results, 105.0, 100.0, "percent_units");
  }

  @Test
  public void testSanityCheckPercentUnitsRepair() throws IOException {
    String qta = "../examples/precharge_sanity_percent_units.qta";
    List<EngineResult> bau = runScenarioFromQta(qta, "BAU");
    List<EngineResult> repair = runScenarioFromQta(qta, "With Repair");
    assertRepairIncreasing(repair, bau, "percent_units", true);
  }

  @Test
  public void testSanityCheckPercentUnitsIncrease() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_percent_units.qta", "With Increase");
    assertIncreaseTrend(results, 105.0, "percent_units");
  }

  @Test
  public void testSanityCheckPercentUnitsDecrease() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_percent_units.qta", "With Decrease");
    assertDecreaseTrend(results, 105.0, "percent_units");
  }

  // ===== percent_kg: set import to 100 kg + recharge 5 % of newEquipment =====
  // BAU: import = 100 kg flat, newEquipment = 100/1.05 ≈ 95.238 units (circular)

  @Test
  public void testSanityCheckPercentKgBau() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_percent_kg.qta", "BAU");
    assertBauFlat(results, 100.0, 95.2381, "percent_kg");
  }

  @Test
  public void testSanityCheckPercentKgRepair() throws IOException {
    String qta = "../examples/precharge_sanity_percent_kg.qta";
    List<EngineResult> bau = runScenarioFromQta(qta, "BAU");
    List<EngineResult> repair = runScenarioFromQta(qta, "With Repair");
    assertRepairIncreasing(repair, bau, "percent_kg", false);
  }

  @Test
  public void testSanityCheckPercentKgIncrease() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_percent_kg.qta", "With Increase");
    assertIncreaseTrend(results, 100.0, "percent_kg");
  }

  @Test
  public void testSanityCheckPercentKgDecrease() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_percent_kg.qta", "With Decrease");
    assertDecreaseTrend(results, 100.0, "percent_kg");
  }

  // ===== units_units: set import to 100 units + recharge 5 units of newEquipment =====
  // BAU: import = 105 kg flat, newEquipment = 100 units flat

  @Test
  public void testSanityCheckUnitsUnitsBau() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_units_units.qta", "BAU");
    assertBauFlat(results, 105.0, 100.0, "units_units");
  }

  @Test
  public void testSanityCheckUnitsUnitsRepair() throws IOException {
    String qta = "../examples/precharge_sanity_units_units.qta";
    List<EngineResult> bau = runScenarioFromQta(qta, "BAU");
    List<EngineResult> repair = runScenarioFromQta(qta, "With Repair");
    assertRepairIncreasing(repair, bau, "units_units", true);
  }

  @Test
  public void testSanityCheckUnitsUnitsIncrease() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_units_units.qta", "With Increase");
    assertIncreaseTrend(results, 105.0, "units_units");
  }

  @Test
  public void testSanityCheckUnitsUnitsDecrease() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_units_units.qta", "With Decrease");
    assertDecreaseTrend(results, 105.0, "units_units");
  }

  // ===== units_kg: set import to 100 kg + recharge 5 units of newEquipment =====
  // BAU: import = 100 kg flat, newEquipment = (100-5)/1 = 95 units

  @Test
  public void testSanityCheckUnitsKgBau() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_units_kg.qta", "BAU");
    assertBauFlat(results, 100.0, 95.0, "units_kg");
  }

  @Test
  public void testSanityCheckUnitsKgRepair() throws IOException {
    String qta = "../examples/precharge_sanity_units_kg.qta";
    List<EngineResult> bau = runScenarioFromQta(qta, "BAU");
    List<EngineResult> repair = runScenarioFromQta(qta, "With Repair");
    assertRepairIncreasing(repair, bau, "units_kg", false);
  }

  @Test
  public void testSanityCheckUnitsKgIncrease() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_units_kg.qta", "With Increase");
    assertIncreaseTrend(results, 100.0, "units_kg");
  }

  @Test
  public void testSanityCheckUnitsKgDecrease() throws IOException {
    List<EngineResult> results = runScenarioFromQta(
        "../examples/precharge_sanity_units_kg.qta", "With Decrease");
    assertDecreaseTrend(results, 100.0, "units_kg");
  }
}
