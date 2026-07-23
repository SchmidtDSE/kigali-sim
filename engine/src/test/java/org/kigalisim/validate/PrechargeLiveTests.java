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

    // Year 1: RechargeEmissions should be non-zero (from precharge)
    EngineNumber rechargeEmissions = resultYear1.getRechargeEmissions();
    assertNotNull(rechargeEmissions, "Recharge emissions should not be null");
    assertTrue(rechargeEmissions.getValue().doubleValue() > 0,
        "Recharge emissions should be positive (from precharge): " + rechargeEmissions.getValue());
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

    // Year 1: RechargeEmissions should include both recharge (prior) and precharge (new)
    EngineNumber rechargeEmissions = resultYear1.getRechargeEmissions();
    assertNotNull(rechargeEmissions, "Recharge emissions should not be null");
    assertTrue(rechargeEmissions.getValue().doubleValue() > 0,
        "Recharge emissions should be positive (from both recharge and precharge)");
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
