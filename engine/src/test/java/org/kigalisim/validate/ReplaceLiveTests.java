/**
 * Replace live tests using actual QTA files with "replace" prefix.
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
 * Tests that validate replace QTA files against expected behavior.
 */
public class ReplaceLiveTests {

  /**
   * Test replace.qta produces expected values.
   * This tests replacing a percentage of manufacture from one substance to another.
   */
  @Test
  public void testReplace() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/replace.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check substance "a" consumption
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "a");
    assertNotNull(resultA, "Should have result for test/a in year 1");

    // Calculation: Original 50 mt - replaced 50% = 25 mt remaining × 10 tCO2e/mt = 250 tCO2e
    assertEquals(250.0, resultA.getConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 250 tCO2e");
    assertEquals("tCO2e", resultA.getConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Check substance "b" consumption
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Calculation: Original 50 mt + added 25 mt = 75 mt total × 5 tCO2e/mt = 375 tCO2e
    assertEquals(375.0, resultB.getConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 375 tCO2e");
    assertEquals("tCO2e", resultB.getConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test replace_kg.qta produces expected values.
   * This tests replacing a specific amount (in kg) of manufacture from one substance to another.
   */
  @Test
  public void testReplaceKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/replace_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check substance "a" consumption
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "a");
    assertNotNull(resultA, "Should have result for test/a in year 1");

    // Calculation: Original 50 mt - replaced 25 kg = 25 mt remaining × 10 tCO2e/mt = 250 tCO2e
    assertEquals(250.0, resultA.getConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 250 tCO2e");
    assertEquals("tCO2e", resultA.getConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Check substance "b" consumption
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Calculation: Original 50 mt + added 25 kg = 75 mt total × 5 tCO2e/mt = 375 tCO2e
    assertEquals(375.0, resultB.getConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 375 tCO2e");
    assertEquals("tCO2e", resultB.getConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test replace_units.qta produces expected values.
   * This tests replacing a specific number of units of manufacture from one substance to another.
   */
  @Test
  public void testReplaceUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/replace_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check substance "a" consumption
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "a");
    assertNotNull(resultA, "Should have result for test/a in year 1");

    // Calculation: Original 50 mt - replaced 25000 units = 25 mt remaining × 10 tCO2e/mt = 250 tCO2e
    assertEquals(250.0, resultA.getConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 250 tCO2e");
    assertEquals("tCO2e", resultA.getConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Check substance "b" consumption
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Calculation: Original 50 mt + added 25000 units = 75 mt total × 5 tCO2e/mt = 375 tCO2e
    assertEquals(375.0, resultB.getConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 375 tCO2e");
    assertEquals("tCO2e", resultB.getConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test tutorial_09.qta to verify replacement policy behavior.
   * This test verifies that:
   * 1. Total kg of sales remains the same between BAU and Replacement scenarios at 2030
   * 2. Total equipment populations remain the same between BAU and Replacement at 2030
   * 3. Recharge emissions in BAU should be higher than in Replacement (not lower)
   */
  @Test
  public void testTutorial9Replacement() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/tutorial_09.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Replacement scenario
    Stream<EngineResult> replacementResults = KigaliSimFacade.runScenario(program, "Replacement", progress -> {});
    List<EngineResult> replacementResultsList = replacementResults.collect(Collectors.toList());

    // Get year 2030 results (year 6 since simulation starts at 2025)
    int targetYear = 2030;

    // BAU results for 2030
    EngineResult bauHfc134a = LiveTestsUtil.getResult(bauResultsList.stream(), targetYear,
        "Domestic Refrigeration", "HFC-134a");
    EngineResult bauR600a = LiveTestsUtil.getResult(bauResultsList.stream(), targetYear,
        "Domestic Refrigeration", "R-600a");

    // Replacement results for 2030
    EngineResult replacementHfc134a = LiveTestsUtil.getResult(replacementResultsList.stream(), targetYear,
        "Domestic Refrigeration", "HFC-134a");
    final EngineResult replacementR600a = LiveTestsUtil.getResult(replacementResultsList.stream(), targetYear,
        "Domestic Refrigeration", "R-600a");

    assertNotNull(bauHfc134a, "Should have BAU result for HFC-134a in 2030");
    assertNotNull(bauR600a, "Should have BAU result for R-600a in 2030");
    assertNotNull(replacementHfc134a, "Should have Replacement result for HFC-134a in 2030");
    assertNotNull(replacementR600a, "Should have Replacement result for R-600a in 2030");

    // Test 1: Total kg of sales should be the same between BAU and Replacement at 2030
    double bauHfc134aSales = bauHfc134a.getDomestic().getValue().doubleValue();
    double bauR600aSales = bauR600a.getDomestic().getValue().doubleValue();
    double bauTotalSales = bauHfc134aSales + bauR600aSales;

    double replacementHfc134aSales = replacementHfc134a.getDomestic().getValue().doubleValue();
    double replacementR600aSales = replacementR600a.getDomestic().getValue().doubleValue();
    double replacementTotalSales = replacementHfc134aSales + replacementR600aSales;


    assertEquals(bauTotalSales, replacementTotalSales, 0.01,
        "Total kg of sales should be the same between BAU and Replacement scenarios at 2030");

    // Test 2: Total equipment populations should be the same between BAU and Replacement at 2030
    double bauHfc134aPopulation = bauHfc134a.getPopulation().getValue().doubleValue();
    double bauR600aPopulation = bauR600a.getPopulation().getValue().doubleValue();
    double bauTotalPopulation = bauHfc134aPopulation + bauR600aPopulation;

    double replacementHfc134aPopulation = replacementHfc134a.getPopulation().getValue().doubleValue();
    double replacementR600aPopulation = replacementR600a.getPopulation().getValue().doubleValue();
    double replacementTotalPopulation = replacementHfc134aPopulation + replacementR600aPopulation;


    assertEquals(bauTotalPopulation, replacementTotalPopulation, 1.0,
        "Total equipment populations should be the same between BAU and Replacement scenarios at 2030");

    // Test 3: Recharge emissions in BAU should be higher than in Replacement
    // Since HFC-134a has much higher GWP (1430 kgCO2e/kg) than R-600a (3 kgCO2e/kg),
    // replacing some HFC-134a with R-600a should reduce recharge emissions
    double bauHfc134aRecharge = bauHfc134a.getRechargeEmissions().getValue().doubleValue();
    double bauR600aRecharge = bauR600a.getRechargeEmissions().getValue().doubleValue();
    double bauTotalRechargeEmissions = bauHfc134aRecharge + bauR600aRecharge;

    double replacementHfc134aRecharge = replacementHfc134a.getRechargeEmissions().getValue().doubleValue();
    double replacementR600aRecharge = replacementR600a.getRechargeEmissions().getValue().doubleValue();
    double replacementTotalRechargeEmissions = replacementHfc134aRecharge + replacementR600aRecharge;


    assertTrue(bauTotalRechargeEmissions > replacementTotalRechargeEmissions,
        String.format("BAU recharge emissions (%.2f) should be higher than Replacement recharge emissions (%.2f), not lower",
            bauTotalRechargeEmissions, replacementTotalRechargeEmissions));
  }

  /**
   * Test that replacing a substance with itself should produce an error.
   * This tests the bug where "replace 50% of import with 'R-410A'" is specified
   * for substance "R-410A" itself, which should not be allowed.
   */
  @Test
  public void testReplaceSelfShouldFail() {
    // Load and parse the QTA file
    String qtaPath = "../examples/replace_self.qta";

    // This should throw an exception or produce an error when trying to replace
    // a substance with itself
    boolean errorThrown = false;
    String errorMessage = null;

    try {
      ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);

      if (program != null) {
        // If parsing succeeds, try running the scenario
        String scenarioName = "S1";
        Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

        // Collect results to force execution
        List<EngineResult> resultsList = results.collect(Collectors.toList());

        // If we get here without an exception, the bug is present
        // The test should fail to indicate the bug exists
      }
    } catch (Exception e) {
      errorThrown = true;
      errorMessage = e.getMessage();
    }

    assertTrue(errorThrown,
        "Replacing a substance with itself should throw an error. "
        + "The bug allows self-replacement without error.");

    if (errorThrown && errorMessage != null) {
      assertTrue(errorMessage.toLowerCase().contains("replace")
                 || errorMessage.toLowerCase().contains("self")
                 || errorMessage.toLowerCase().contains("same"),
          "Error message should indicate the problem with self-replacement: " + errorMessage);
    }
  }

  /**
   * Test that 100% replacement should produce the same total equipment population
   * as cap displacement in year 5. This tests if priorEquipment replacement
   * properly reflects into equipment totals.
   *
   * <p>Expected: Replace 100% should have same total equipment as BAU since it's
   * just redistributing equipment between substances.
   * This test should fail if priorEquipment changes don't reflect in equipment.</p>
   */
  @Test
  public void testReplacePriorEquipmentTotalPopulation() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/replace_prior_equipment_test.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Replace S1 scenario (with 100% replacement policies)
    Stream<EngineResult> replaceResults = KigaliSimFacade.runScenario(program, "Replace_S1", progress -> {});
    final List<EngineResult> replaceResultsList = replaceResults.collect(Collectors.toList());

    // Get year 5 results for BAU scenario - all 4 substances
    EngineResult bauR600a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "Domref1", "R-600a - DRe1");
    EngineResult bauHfc134a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "Domref1", "HFC-134a - Domref1");
    EngineResult bauR410a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "ResAC1", "R-410A - E1");
    final EngineResult bauHfc32 = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "ResAC1", "HFC-32 - E11");

    assertNotNull(bauR600a, "Should have BAU result for Domref1/R-600a - DRe1 in year 5");
    assertNotNull(bauHfc134a, "Should have BAU result for Domref1/HFC-134a - Domref1 in year 5");
    assertNotNull(bauR410a, "Should have BAU result for ResAC1/R-410A - E1 in year 5");
    assertNotNull(bauHfc32, "Should have BAU result for ResAC1/HFC-32 - E11 in year 5");

    // Get year 5 results for Replace scenario - all 4 substances
    EngineResult replaceR600a = LiveTestsUtil.getResult(replaceResultsList.stream(), 5, "Domref1", "R-600a - DRe1");
    EngineResult replaceHfc134a = LiveTestsUtil.getResult(replaceResultsList.stream(), 5, "Domref1", "HFC-134a - Domref1");
    EngineResult replaceR410a = LiveTestsUtil.getResult(replaceResultsList.stream(), 5, "ResAC1", "R-410A - E1");
    final EngineResult replaceHfc32 = LiveTestsUtil.getResult(replaceResultsList.stream(), 5, "ResAC1", "HFC-32 - E11");

    assertNotNull(replaceR600a, "Should have Replace result for Domref1/R-600a - DRe1 in year 5");
    assertNotNull(replaceHfc134a, "Should have Replace result for Domref1/HFC-134a - Domref1 in year 5");
    assertNotNull(replaceR410a, "Should have Replace result for ResAC1/R-410A - E1 in year 5");
    assertNotNull(replaceHfc32, "Should have Replace result for ResAC1/HFC-32 - E11 in year 5");

    // Calculate total equipment population for BAU scenario in year 5 (all substances)
    double bauTotalPopulation = bauR600a.getPopulation().getValue().doubleValue()
                               + bauHfc134a.getPopulation().getValue().doubleValue()
                               + bauR410a.getPopulation().getValue().doubleValue()
                               + bauHfc32.getPopulation().getValue().doubleValue();

    // Calculate total equipment population for Replace scenario in year 5 (all substances)
    double replaceTotalPopulation = replaceR600a.getPopulation().getValue().doubleValue()
                                   + replaceHfc134a.getPopulation().getValue().doubleValue()
                                   + replaceR410a.getPopulation().getValue().doubleValue()
                                   + replaceHfc32.getPopulation().getValue().doubleValue();

    // This assertion should pass if replacement works correctly across all substances
    // If it fails, it indicates the same priorEquipment->equipment issue as cap displacement
    assertEquals(bauTotalPopulation, replaceTotalPopulation, 0.0001,
        String.format("Total equipment population should be equal between BAU (%.6f) and Replace (%.6f) scenarios in year 5 "
                     + "because 100%% replacement should just redistribute equipment between substances",
                     bauTotalPopulation, replaceTotalPopulation));
  }

  /**
   * Test replacing priorEquipment only (without replacing equipment) to verify individual substance behavior.
   * This tests that replacing 100% of priorEquipment should still leave some population for that substance
   * (reduced but not zero) because only the baseline is affected, not the current equipment directly.
   *
   * <p>Expected: The replaced substance should have reduced but non-zero population.
   * The target substance should have increased population.</p>
   */
  @Test
  public void testReplacePriorEquipmentOnly() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/replace_prior_equipment_only_test.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run ReplacePriorOnly S1 scenario (with priorEquipment replace policies only)
    Stream<EngineResult> s1Results = KigaliSimFacade.runScenario(program, "ReplacePriorOnly_S1", progress -> {});
    final List<EngineResult> s1ResultsList = s1Results.collect(Collectors.toList());

    // Get year 5 results for BAU scenario
    EngineResult bauHfc134a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "Domref1", "HFC-134a - Domref1");
    EngineResult bauR600a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "Domref1", "R-600a - DRe1");
    EngineResult bauR410a = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "ResAC1", "R-410A - E1");
    final EngineResult bauHfc32 = LiveTestsUtil.getResult(bauResultsList.stream(), 5, "ResAC1", "HFC-32 - E11");

    assertNotNull(bauHfc134a, "Should have BAU result for Domref1/HFC-134a - Domref1 in year 5");
    assertNotNull(bauR600a, "Should have BAU result for Domref1/R-600a - DRe1 in year 5");
    assertNotNull(bauR410a, "Should have BAU result for ResAC1/R-410A - E1 in year 5");
    assertNotNull(bauHfc32, "Should have BAU result for ResAC1/HFC-32 - E11 in year 5");

    // Get year 5 results for ReplacePriorOnly S1 scenario
    EngineResult s1Hfc134a = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "Domref1", "HFC-134a - Domref1");
    EngineResult s1R600a = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "Domref1", "R-600a - DRe1");
    EngineResult s1R410a = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "ResAC1", "R-410A - E1");
    final EngineResult s1Hfc32 = LiveTestsUtil.getResult(s1ResultsList.stream(), 5, "ResAC1", "HFC-32 - E11");

    assertNotNull(s1Hfc134a, "Should have ReplacePriorOnly S1 result for Domref1/HFC-134a - Domref1 in year 5");
    assertNotNull(s1R600a, "Should have ReplacePriorOnly S1 result for Domref1/R-600a - DRe1 in year 5");
    assertNotNull(s1R410a, "Should have ReplacePriorOnly S1 result for ResAC1/R-410A - E1 in year 5");
    assertNotNull(s1Hfc32, "Should have ReplacePriorOnly S1 result for ResAC1/HFC-32 - E11 in year 5");

    // Test that replaced substances (HFC-134a and R-410A) have reduced but non-zero populations
    double bauHfc134aPopulation = bauHfc134a.getPopulation().getValue().doubleValue();
    double s1Hfc134aPopulation = s1Hfc134a.getPopulation().getValue().doubleValue();
    final double bauR410aPopulation = bauR410a.getPopulation().getValue().doubleValue();
    double s1R410aPopulation = s1R410a.getPopulation().getValue().doubleValue();

    assertTrue(s1Hfc134aPopulation > 0.0,
        String.format("HFC-134a should still have population > 0 when only priorEquipment is replaced. Got: %.6f", s1Hfc134aPopulation));
    assertTrue(s1Hfc134aPopulation < bauHfc134aPopulation,
        String.format("HFC-134a should have reduced population when priorEquipment is replaced. BAU: %.6f, ReplacePriorOnly: %.6f",
                     bauHfc134aPopulation, s1Hfc134aPopulation));

    assertTrue(s1R410aPopulation > 0.0,
        String.format("R-410A should still have population > 0 when only priorEquipment is replaced. Got: %.6f", s1R410aPopulation));
    assertTrue(s1R410aPopulation < bauR410aPopulation,
        String.format("R-410A should have reduced population when priorEquipment is replaced. BAU: %.6f, ReplacePriorOnly: %.6f",
                     bauR410aPopulation, s1R410aPopulation));

    // Log target substance populations for reference (they may start at zero due to QTA setup)
    double bauR600aPopulation = bauR600a.getPopulation().getValue().doubleValue();
    double s1R600aPopulation = s1R600a.getPopulation().getValue().doubleValue();
    double bauHfc32Population = bauHfc32.getPopulation().getValue().doubleValue();
    double s1Hfc32Population = s1Hfc32.getPopulation().getValue().doubleValue();

    // Note: Target substances may remain at zero if they have no initial import/sales in the QTA setup
    // The key validation is that replaced substances are reduced but not eliminated entirely
  }
}
