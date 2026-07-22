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

  /**
   * Sanity check for BAU scenario with precharge.
   *
   * <p>With 100 units import and 5% precharge at 1 kg/unit, import should be
   * 105 kg (100 units * 1 kg/unit initial charge + 5 units * 1 kg/unit precharge)
   * flat across all years.</p>
   */
  @Test
  public void testSanityCheckBau() throws IOException {
    String qtaPath = "../examples/precharge_sanity_check.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check each year from 2021 to 2030
    for (int year = 2021; year <= 2030; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      assertNotNull(result, "Should have result for year " + year);
      double importValue = result.getImport().getValue().doubleValue();
      assertEquals(105.0, importValue, 0.0001,
          "BAU import should be 105 kg in year " + year + " but was " + importValue);
    }
  }

  /**
   * Sanity check for With Repair scenario.
   *
   * <p>With repair policy adding recharge of priorEquipment, import should be
   * higher than BAU and each year should be larger than the last (as equipment
   * population grows, more recharge is needed).</p>
   */
  @Test
  public void testSanityCheckWithRepair() throws IOException {
    String qtaPath = "../examples/precharge_sanity_check.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Get BAU results for comparison
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauList = bauResults.collect(Collectors.toList());

    String scenarioName = "With Repair";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    double previousYearImport = -1;
    for (int year = 2021; year <= 2030; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      assertNotNull(result, "Should have result for year " + year);
      double importValue = result.getImport().getValue().doubleValue();

      // Import should be higher than BAU
      EngineResult bauResult = LiveTestsUtil.getResult(bauList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      double bauImport = bauResult.getImport().getValue().doubleValue();
      assertTrue(importValue > bauImport,
          "With Repair import (" + importValue + ") should be higher than BAU ("
              + bauImport + ") in year " + year);

      // Each year should be larger than the last (after year 1)
      if (previousYearImport > 0) {
        assertTrue(importValue > previousYearImport,
            "With Repair import should increase each year. Year " + year
                + ": " + importValue + " vs previous: " + previousYearImport);
      }
      previousYearImport = importValue;
    }
  }

  /**
   * Sanity check for With Increase scenario.
   *
   * <p>With 5% increase in import each year from 2022, import should be 105 kg
   * in 2021 and then increase each year after.</p>
   */
  @Test
  public void testSanityCheckWithIncrease() throws IOException {
    String qtaPath = "../examples/precharge_sanity_check.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "With Increase";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    double previousYearImport = -1;
    for (int year = 2021; year <= 2030; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      assertNotNull(result, "Should have result for year " + year);
      double importValue = result.getImport().getValue().doubleValue();

      if (year == 2021) {
        assertEquals(105.0, importValue, 0.0001,
            "With Increase import should be 105 kg in 2021 but was " + importValue);
      } else {
        assertTrue(importValue > previousYearImport,
            "With Increase import should increase each year after 2021. Year " + year
                + ": " + importValue + " vs previous: " + previousYearImport);
      }
      previousYearImport = importValue;
    }
  }

  /**
   * Sanity check for With Decrease scenario.
   *
   * <p>With 5% decrease in import each year from 2022, import should be 105 kg
   * in 2021 and then decrease each year after.</p>
   */
  @Test
  public void testSanityCheckWithDecrease() throws IOException {
    String qtaPath = "../examples/precharge_sanity_check.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "With Decrease";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    double previousYearImport = -1;
    for (int year = 2021; year <= 2030; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      assertNotNull(result, "Should have result for year " + year);
      double importValue = result.getImport().getValue().doubleValue();

      if (year == 2021) {
        assertEquals(105.0, importValue, 0.0001,
            "With Decrease import should be 105 kg in 2021 but was " + importValue);
      } else {
        assertTrue(importValue < previousYearImport,
            "With Decrease import should decrease each year after 2021. Year " + year
                + ": " + importValue + " vs previous: " + previousYearImport);
      }
      previousYearImport = importValue;
    }
  }
}
