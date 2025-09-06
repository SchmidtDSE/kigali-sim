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
    assertEquals(250.0, resultA.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 250 tCO2e");
    assertEquals("tCO2e", resultA.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Check substance "b" consumption
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Calculation: Original 50 mt + added 25 mt = 75 mt total × 5 tCO2e/mt = 375 tCO2e
    assertEquals(375.0, resultB.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 375 tCO2e");
    assertEquals("tCO2e", resultB.getGhgConsumption().getUnits(),
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
    assertEquals(250.0, resultA.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 250 tCO2e");
    assertEquals("tCO2e", resultA.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Check substance "b" consumption
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Calculation: Original 50 mt + added 25 kg = 75 mt total × 5 tCO2e/mt = 375 tCO2e
    assertEquals(375.0, resultB.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 375 tCO2e");
    assertEquals("tCO2e", resultB.getGhgConsumption().getUnits(),
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
    assertEquals(250.0, resultA.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 250 tCO2e");
    assertEquals("tCO2e", resultA.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Check substance "b" consumption
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Calculation: Original 50 mt + added 25000 units = 75 mt total × 5 tCO2e/mt = 375 tCO2e
    assertEquals(375.0, resultB.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 375 tCO2e");
    assertEquals("tCO2e", resultB.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test tutorial_11.qta to verify replacement policy behavior.
   * This test verifies that:
   * 1. Total kg of sales remains the same between BAU and Replacement scenarios at 2030
   * 2. Total equipment populations remain the same between BAU and Replacement at 2030
   * 3. Recharge emissions in BAU should be higher than in Replacement (not lower)
   */
  @Test
  public void testTutorial11Replacement() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/tutorial_11.qta";
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
}
