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
 * Tests that validate feature-specific QTA files against expected behavior.
 */
public class FeatureLiveTests {

  /**
   * Test combination.qta produces expected values.
   */
  @Test
  public void testCombination() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/combination.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check consumption value - should be 125 tCO2e (25% of original 500 tCO2e)
    assertEquals(125.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 125 tCO2e");
    assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test conditional.qta produces expected values.
   */
  //@Test
  public void testConditional() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/conditional.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check consumption value - should be 250 tCO2e (50 mt * 5 tCO2e/mt)
    assertEquals(250.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 250 tCO2e");
    assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test init_units.qta produces expected values.
   */
  @Test
  public void testInitUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/init_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 - should have 1M units
    EngineResult result1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub1");
    assertNotNull(result1, "Should have result for App/Sub1 in year 1");
    assertEquals(1000000.0, result1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 1000000 units in year 1");
    assertEquals("units", result1.getPopulation().getUnits(),
        "Equipment units should be units");

    // Check year 2 - should have 2M units
    EngineResult result2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "App", "Sub1");
    assertNotNull(result2, "Should have result for App/Sub1 in year 2");
    assertEquals(2000000.0, result2.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 2000000 units in year 2");
    assertEquals("units", result2.getPopulation().getUnits(),
        "Equipment units should be units");

    // Check year 3 - should have 3M units
    EngineResult result3 = LiveTestsUtil.getResult(resultsList.stream(), 3, "App", "Sub1");
    assertNotNull(result3, "Should have result for App/Sub1 in year 3");
    assertEquals(3000000.0, result3.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 3000000 units in year 3");
    assertEquals("units", result3.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test logical_operators.qta produces expected values.
   */
  //@Test
  public void testLogicalOperators() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/logical_operators.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Test AND: 1 and 0 = false, so manufacture should be 30 (else branch)
    EngineResult result1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result1, "Should have result for test/test in year 1");
    assertEquals(30.0, result1.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 30 tCO2e in year 1");
    assertEquals("tCO2e", result1.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Test OR: 1 or 0 = true, so manufacture should be 50 (if branch)
    EngineResult result2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result2, "Should have result for test/test in year 2");
    assertEquals(50.0, result2.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 50 tCO2e in year 2");
    assertEquals("tCO2e", result2.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Test XOR: 1 xor 2 = false (both are truthy), so manufacture should be 40 (else branch)
    EngineResult result3 = LiveTestsUtil.getResult(resultsList.stream(), 3, "test", "test");
    assertNotNull(result3, "Should have result for test/test in year 3");
    assertEquals(40.0, result3.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 40 tCO2e in year 3");
    assertEquals("tCO2e", result3.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Test precedence with parentheses: (testA or testB) and testC = (1 or 0) and 2 =
    // 1 and 2 = true, so manufacture should be 70 (if branch)
    EngineResult result4 = LiveTestsUtil.getResult(resultsList.stream(), 4, "test", "test");
    assertNotNull(result4, "Should have result for test/test in year 4");
    assertEquals(70.0, result4.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 70 tCO2e in year 4");
    assertEquals("tCO2e", result4.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Test precedence without parentheses: testA or testB and testC =
    // testA or (testB and testC) = 1 or (0 and 2) = 1 or 0 = true,
    // so manufacture should be 80 (if branch)
    EngineResult result5 = LiveTestsUtil.getResult(resultsList.stream(), 5, "test", "test");
    assertNotNull(result5, "Should have result for test/test in year 5");
    assertEquals(80.0, result5.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 80 tCO2e in year 5");
    assertEquals("tCO2e", result5.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Test mixed comparison and logical: testA > 0 and testB == 0 =
    // 1 > 0 and 0 == 0 = true and true = true, so manufacture should be 90 (if branch)
    EngineResult result6 = LiveTestsUtil.getResult(resultsList.stream(), 6, "test", "test");
    assertNotNull(result6, "Should have result for test/test in year 6");
    assertEquals(90.0, result6.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 90 tCO2e in year 6");
    assertEquals("tCO2e", result6.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Test complex parentheses: (testA > 0 or testB > 0) and (testC == 2) =
    // (true or false) and (true) = true and true = true,
    // so manufacture should be 100 (if branch)
    EngineResult result7 = LiveTestsUtil.getResult(resultsList.stream(), 7, "test", "test");
    assertNotNull(result7, "Should have result for test/test in year 7");
    assertEquals(100.0, result7.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 100 tCO2e in year 7");
    assertEquals("tCO2e", result7.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test order_check_units.qta produces expected values.
   */
  @Test
  public void testOrderCheckUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/order_check_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Test Sub2 (A, C, B order) - should have same results as Sub1
    // Year 1 - 1M units
    EngineResult result1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub2");
    assertNotNull(result1, "Should have result for App/Sub2 in year 1");
    assertEquals(1000000.0, result1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 1000000 units in year 1");
    assertEquals("units", result1.getPopulation().getUnits(),
        "Equipment units should be units");

    // Year 2 - 2M units
    EngineResult result2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "App", "Sub2");
    assertNotNull(result2, "Should have result for App/Sub2 in year 2");
    assertEquals(2000000.0, result2.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 2000000 units in year 2");
    assertEquals("units", result2.getPopulation().getUnits(),
        "Equipment units should be units");

    // Year 3 - 3M units
    EngineResult result3 = LiveTestsUtil.getResult(resultsList.stream(), 3, "App", "Sub2");
    assertNotNull(result3, "Should have result for App/Sub2 in year 3");
    assertEquals(3000000.0, result3.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 3000000 units in year 3");
    assertEquals("units", result3.getPopulation().getUnits(),
        "Equipment units should be units");

    // Test Sub3 (C, A, B order) - should have same results as Sub1 and Sub2
    // Year 1 - 1M units
    EngineResult result4 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub3");
    assertNotNull(result4, "Should have result for App/Sub3 in year 1");
    assertEquals(1000000.0, result4.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 1000000 units in year 1");
    assertEquals("units", result4.getPopulation().getUnits(),
        "Equipment units should be units");

    // Year 2 - 2M units
    EngineResult result5 = LiveTestsUtil.getResult(resultsList.stream(), 2, "App", "Sub3");
    assertNotNull(result5, "Should have result for App/Sub3 in year 2");
    assertEquals(2000000.0, result5.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 2000000 units in year 2");
    assertEquals("units", result5.getPopulation().getUnits(),
        "Equipment units should be units");

    // Year 3 - 3M units
    EngineResult result6 = LiveTestsUtil.getResult(resultsList.stream(), 3, "App", "Sub3");
    assertNotNull(result6, "Should have result for App/Sub3 in year 3");
    assertEquals(3000000.0, result6.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 3000000 units in year 3");
    assertEquals("units", result6.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test order_check_volume.qta produces expected values.
   */
  @Test
  public void testOrderCheckVolume() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/order_check_volume.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Test Sub1 (A, B, C order) produces 1 MtCO2e across all years
    for (int year = 1; year <= 3; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year, "App", "Sub1");
      assertNotNull(result, "Should have result for App/Sub1 in year " + year);
      assertEquals(1000000.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
          "Consumption should be 1000000 tCO2e in year " + year);
      assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
          "Consumption units should be tCO2e");
    }

    // Test Sub2 (A, C, B order) produces 1 MtCO2e across all years
    for (int year = 1; year <= 3; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year, "App", "Sub2");
      assertNotNull(result, "Should have result for App/Sub2 in year " + year);
      assertEquals(1000000.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
          "Consumption should be 1000000 tCO2e in year " + year);
      assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
          "Consumption units should be tCO2e");
    }

    // Test Sub3 (C, A, B order) produces 1 MtCO2e across all years
    for (int year = 1; year <= 3; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year, "App", "Sub3");
      assertNotNull(result, "Should have result for App/Sub3 in year " + year);
      assertEquals(1000000.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
          "Consumption should be 1000000 tCO2e in year " + year);
      assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
          "Consumption units should be tCO2e");
    }
  }

  /**
   * Test ordering_sensitive_emissions.qta produces expected values.
   */
  @Test
  public void testOrderingSensitiveEmissions() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/ordering_sensitive_emissions.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Get results for all substances in year 2035
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 2035, "test", "SubA");
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 2035, "test", "SubB");
    EngineResult resultC = LiveTestsUtil.getResult(resultsList.stream(), 2035, "test", "SubC");
    final EngineResult resultD = LiveTestsUtil.getResult(resultsList.stream(), 2035, "test", "SubD");

    assertNotNull(resultA, "Should have result for test/SubA in year 2035");
    assertNotNull(resultB, "Should have result for test/SubB in year 2035");
    assertNotNull(resultC, "Should have result for test/SubC in year 2035");
    assertNotNull(resultD, "Should have result for test/SubD in year 2035");

    // Check that recharge emissions are positive for all substances
    assertTrue(resultA.getRechargeEmissions().getValue().doubleValue() > 0,
        "SubA recharge emissions should be positive");
    assertTrue(resultB.getRechargeEmissions().getValue().doubleValue() > 0,
        "SubB recharge emissions should be positive");
    assertTrue(resultC.getRechargeEmissions().getValue().doubleValue() > 0,
        "SubC recharge emissions should be positive");
    assertTrue(resultD.getRechargeEmissions().getValue().doubleValue() > 0,
        "SubD recharge emissions should be positive");

    // Check that all recharge emissions have tCO2e units
    assertEquals("tCO2e", resultA.getRechargeEmissions().getUnits(),
        "SubA recharge emissions units should be tCO2e");
    assertEquals("tCO2e", resultB.getRechargeEmissions().getUnits(),
        "SubB recharge emissions units should be tCO2e");
    assertEquals("tCO2e", resultC.getRechargeEmissions().getUnits(),
        "SubC recharge emissions units should be tCO2e");
    assertEquals("tCO2e", resultD.getRechargeEmissions().getUnits(),
        "SubD recharge emissions units should be tCO2e");

    // Check that all recharge emissions are approximately equal
    double emissionsA = resultA.getRechargeEmissions().getValue().doubleValue();
    double emissionsB = resultB.getRechargeEmissions().getValue().doubleValue();
    double emissionsC = resultC.getRechargeEmissions().getValue().doubleValue();
    double emissionsD = resultD.getRechargeEmissions().getValue().doubleValue();

    assertEquals(emissionsA, emissionsB, 0.001, "SubA and SubB recharge emissions should be equal");
    assertEquals(emissionsB, emissionsC, 0.001, "SubB and SubC recharge emissions should be equal");
    assertEquals(emissionsC, emissionsD, 0.001, "SubC and SubD recharge emissions should be equal");

    // Check that EOL emissions are positive for all substances
    assertTrue(resultA.getEolEmissions().getValue().doubleValue() > 0,
        "SubA EOL emissions should be positive");
    assertTrue(resultB.getEolEmissions().getValue().doubleValue() > 0,
        "SubB EOL emissions should be positive");
    assertTrue(resultC.getEolEmissions().getValue().doubleValue() > 0,
        "SubC EOL emissions should be positive");
    assertTrue(resultD.getEolEmissions().getValue().doubleValue() > 0,
        "SubD EOL emissions should be positive");

    // Check that all EOL emissions have tCO2e units
    assertEquals("tCO2e", resultA.getEolEmissions().getUnits(),
        "SubA EOL emissions units should be tCO2e");
    assertEquals("tCO2e", resultB.getEolEmissions().getUnits(),
        "SubB EOL emissions units should be tCO2e");
    assertEquals("tCO2e", resultC.getEolEmissions().getUnits(),
        "SubC EOL emissions units should be tCO2e");
    assertEquals("tCO2e", resultD.getEolEmissions().getUnits(),
        "SubD EOL emissions units should be tCO2e");

    // Check that all EOL emissions are approximately equal
    double eolEmissionsA = resultA.getEolEmissions().getValue().doubleValue();
    double eolEmissionsB = resultB.getEolEmissions().getValue().doubleValue();
    double eolEmissionsC = resultC.getEolEmissions().getValue().doubleValue();
    double eolEmissionsD = resultD.getEolEmissions().getValue().doubleValue();

    assertEquals(eolEmissionsA, eolEmissionsB, 0.001, "SubA and SubB EOL emissions should be equal");
    assertEquals(eolEmissionsB, eolEmissionsC, 0.001, "SubB and SubC EOL emissions should be equal");
    assertEquals(eolEmissionsC, eolEmissionsD, 0.001, "SubC and SubD EOL emissions should be equal");
  }

  /**
   * Test policies.qta produces expected values.
   */
  @Test
  public void testPolicies() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/policies.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check consumption value - should be 250 tCO2e (50% of original 500 tCO2e)
    assertEquals(250.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 250 tCO2e");
    assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test real_years.qta produces expected values.
   */
  @Test
  public void testRealYears() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/real_years.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2026 - should have 110 mt = 110000 kg manufacture
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2026, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2026");
    assertEquals(110000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 110000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");

    // Check consumption value - should be 550 tCO2e (110 mt * 5 tCO2e/mt)
    assertEquals(550.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 550 tCO2e");
    assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test simple_and.qta produces expected values.
   */
  //@Test
  public void testSimpleAnd() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/simple_and.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 1");

    // Check consumption value - should be 30 tCO2e (30 mt * 1 tCO2e/mt)
    assertEquals(30.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 30 tCO2e");
    assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test trials.qta produces expected values.
   */
  //@Test
  public void testTrials() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/trials.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Filter results for test/test in year 1
    List<EngineResult> testResults = resultsList.stream()
        .filter(r -> r.getYear() == 1)
        .filter(r -> r.getApplication().equals("test"))
        .filter(r -> r.getSubstance().equals("test"))
        .collect(Collectors.toList());

    // Should have at least 2 results (one for each trial)
    assertTrue(testResults.size() >= 2, "Should have at least 2 results for test/test in year 1");

    // Check consumption values - should be between 300 and 700 tCO2e
    for (EngineResult result : testResults) {
      double consumption = result.getGhgConsumption().getValue().doubleValue();
      assertTrue(consumption >= 300.0 && consumption <= 700.0,
          "Consumption should be between 300 and 700 tCO2e");
      assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
          "Consumption units should be tCO2e");
    }
  }
}
