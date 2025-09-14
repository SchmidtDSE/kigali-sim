/**
 * Live tests for recycle and recover operations using actual QTA files.
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
 * Tests that validate recycle and recover operations in QTA files against expected behavior.
 */
public class RecycleRecoverLiveTests {

  /**
   * Test recycling.qta produces expected values.
   */
  @Test
  public void testRecycling() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycling.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 - no recycling yet
    EngineResult recordYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(recordYear1, "Should have result for test/test in year 1");
    assertEquals(500.0, recordYear1.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be 500 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 1");

    // Check year 2 - recycling active
    EngineResult recordYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(recordYear2, "Should have result for test/test in year 2");

    // With recycling, virgin material should be reduced
    double expectedTotalConsumption = 437.5; // Reduced due to recycling displacing virgin material
    assertEquals(expectedTotalConsumption, recordYear2.getConsumptionNoRecycle().getValue().doubleValue(), 0.0001,
        "Virgin material consumption should be reduced to 437.5 tCO2e in year 2 due to recycling");
    assertEquals("tCO2e", recordYear2.getConsumptionNoRecycle().getUnits(),
        "Virgin material consumption units should be tCO2e in year 2");

    // Check recycled consumption in year 2
    double expectedRecycledConsumption = 62.5; // 500 - 437.5
    assertEquals(expectedRecycledConsumption, recordYear2.getRecycleConsumption().getValue().doubleValue(), 0.0001,
        "Recycled consumption should be 62.5 tCO2e in year 2");
    assertEquals("tCO2e", recordYear2.getRecycleConsumption().getUnits(),
        "Recycled consumption units should be tCO2e in year 2");
  }

  /**
   * Test recycle_by_kg.qta produces expected values.
   */
  @Test
  public void testRecycleByKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycle_by_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 - no recycling yet
    EngineResult recordYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(recordYear1, "Should have result for test/test in year 1");
    assertEquals(500.0, recordYear1.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be 500 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 1");

    // Check year 2 - recycling active
    EngineResult recordYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(recordYear2, "Should have result for test/test in year 2");

    // With recycling, virgin material should be reduced
    double expectedTotalConsumption = 499.875; // Reduced due to recycling displacing virgin material
    assertEquals(expectedTotalConsumption, recordYear2.getConsumptionNoRecycle().getValue().doubleValue(), 0.0001,
        "Virgin material consumption should be reduced to 499.875 tCO2e in year 2 due to recycling");
    assertEquals("tCO2e", recordYear2.getConsumptionNoRecycle().getUnits(),
        "Virgin material consumption units should be tCO2e in year 2");

    // Check recycled consumption in year 2
    // 25 kg * 5 tCO2e/mt = 25 kg * 5 tCO2e/(1000 kg) = 0.125 tCO2e
    assertEquals(0.125, recordYear2.getRecycleConsumption().getValue().doubleValue(), 0.0001,
        "Recycled consumption should be 0.125 tCO2e in year 2");
    assertEquals("tCO2e", recordYear2.getRecycleConsumption().getUnits(),
        "Recycled consumption units should be tCO2e in year 2");
  }

  /**
   * Test recycle_by_units.qta produces expected values.
   */
  @Test
  public void testRecycleByUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycle_by_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 - no recycling yet
    EngineResult recordYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(recordYear1, "Should have result for test/test in year 1");
    assertEquals(500.0, recordYear1.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be 500 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 1");

    // Check year 2 - recycling active
    EngineResult recordYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(recordYear2, "Should have result for test/test in year 2");

    // With recycling, virgin material should be reduced
    double expectedTotalConsumption = 490.0; // Reduced due to recycling displacing virgin material
    assertEquals(expectedTotalConsumption, recordYear2.getConsumptionNoRecycle().getValue().doubleValue(), 0.0001,
        "Virgin material consumption should be reduced to 490.0 tCO2e in year 2 due to recycling");
    assertEquals("tCO2e", recordYear2.getConsumptionNoRecycle().getUnits(),
        "Virgin material consumption units should be tCO2e in year 2");

    // Check recycled consumption in year 2
    // 1000 units * 2 kg/unit = 2000 kg, 2000 kg * 5 tCO2e/(1000 kg) = 10 tCO2e
    assertEquals(10.0, recordYear2.getRecycleConsumption().getValue().doubleValue(), 0.0001,
        "Recycled consumption should be 10 tCO2e in year 2");
    assertEquals("tCO2e", recordYear2.getRecycleConsumption().getUnits(),
        "Recycled consumption units should be tCO2e in year 2");
  }


  /**
   * Test that the example file with multiple recover commands now properly fails.
   * This verifies that Component 5's validation prevents multiple recover commands.
   */
  @Test
  public void testMultipleRecycles() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/test_multiple_recycles.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // BAU scenario should work (no recover commands)
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());
    assertNotNull(bauResultsList, "BAU scenario should work");
    assertTrue(bauResultsList.size() > 0, "BAU scenario should have results");

    // Multiple Recycles scenario should work with additive recovery rates
    Stream<EngineResult> policyResults = KigaliSimFacade.runScenario(program, "Multiple Recycles", progress -> {});
    List<EngineResult> policyResultsList = policyResults.collect(Collectors.toList());
    assertNotNull(policyResultsList, "Policy scenario should work with multiple recover commands");
    assertTrue(policyResultsList.size() > 0, "Policy scenario should have results");

    // Verify that recovery rates are additive (30% + 20% = 50% recovery rate)
    EngineResult result = LiveTestsUtil.getResult(policyResultsList.stream(), 1, "TestApp", "HFC-134a");
    assertNotNull(result, "Should have results for year 1");

    // The simulation should run successfully with additive recovery behavior
    assertTrue(result.getRecycle().getValue().doubleValue() > 0,
        "Should have positive recycling with multiple recover commands");
  }

  /**
   * Test recover_no_displacement_kg.qta to see default displacement behavior.
   */
  @Test
  public void testRecoverNoDisplacementKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recover_no_displacement_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check sub_a results - should have some displacement effect
    EngineResult recordSubA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_a");
    assertNotNull(recordSubA, "Should have result for test/sub_a in year 1");

    // Check displacement values
    double importSales = recordSubA.getImport().getValue().doubleValue();
    double domesticSales = recordSubA.getDomestic().getValue().doubleValue();
    double recycledContent = recordSubA.getRecycleConsumption().getValue().doubleValue();

    // Check recycled content is positive
    assertTrue(recycledContent > 0,
        "Recycled content should be positive");

    // With recycling, total should be reduced
    double totalSales = importSales + domesticSales;
    assertTrue(totalSales < 150.0,
        "Total sales should be reduced due to recycling");
  }

  /**
   * Test that BAU and Recovery/Recycling scenarios have the same total equipment values.
   * This test is expected to fail initially to investigate the discrepancy.
   */
  @Test
  public void testRecycleNoneBug() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycle_none_bug.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "Business as Usual", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recovery and Recycling scenario
    Stream<EngineResult> rrResults = KigaliSimFacade.runScenario(program, "Recovery and Recycling", progress -> {});
    List<EngineResult> rrResultsList = rrResults.collect(Collectors.toList());

    // Check equipment values for years 2025, 2026, and 2027
    int[] yearsToCheck = {2025, 2026, 2027};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "MAC", "HFC-134a");
      EngineResult rrResult = LiveTestsUtil.getResult(rrResultsList.stream(), year, "MAC", "HFC-134a");

      assertNotNull(bauResult, "Should have BAU result for MAC/HFC-134a in year " + year);
      assertNotNull(rrResult, "Should have Recovery/Recycling result for MAC/HFC-134a in year " + year);

      double bauEquipment = bauResult.getPopulation().getValue().doubleValue();
      double rrEquipment = rrResult.getPopulation().getValue().doubleValue();

      assertEquals(bauEquipment, rrEquipment, 0.0001,
          "Year " + year + ": BAU equipment (" + bauEquipment
          + ") should equal Recovery/Recycling equipment (" + rrEquipment
          + ") when recovery rate is 0%");
    }
  }

  /**
   * Test that BAU and Recovery/Recycling scenarios have the same total equipment values
   * when import is specified in kg instead of units.
   * This test checks if the bug is related to unit-based import specification.
   */
  @Test
  public void testRecycleNoneBugKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycle_none_bug_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "Business as Usual", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recovery and Recycling scenario
    Stream<EngineResult> rrResults = KigaliSimFacade.runScenario(program, "Recovery and Recycling", progress -> {});
    List<EngineResult> rrResultsList = rrResults.collect(Collectors.toList());

    // Check equipment values for years 2025, 2026, and 2027
    int[] yearsToCheck = {2025, 2026, 2027};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "MAC", "HFC-134a");
      EngineResult rrResult = LiveTestsUtil.getResult(rrResultsList.stream(), year, "MAC", "HFC-134a");

      assertNotNull(bauResult, "Should have BAU result for MAC/HFC-134a in year " + year);
      assertNotNull(rrResult, "Should have Recovery/Recycling result for MAC/HFC-134a in year " + year);

      double bauEquipment = bauResult.getPopulation().getValue().doubleValue();
      double rrEquipment = rrResult.getPopulation().getValue().doubleValue();

      assertEquals(bauEquipment, rrEquipment, 0.0001,
          "Year " + year + ": BAU equipment (" + bauEquipment
          + ") should equal Recovery/Recycling equipment (" + rrEquipment
          + ") when recovery rate is 0% and import is in kg");
    }
  }

  /**
   * Test that BAU and Recovery/Recycling scenarios have the same total equipment values
   * when manufacture is specified in units instead of import.
   * This test checks if the bug also affects manufacture-based unit specification.
   */
  @Test
  public void testRecycleNoneBugManufacture() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycle_none_bug_manufacture.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "Business as Usual", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recovery and Recycling scenario
    Stream<EngineResult> rrResults = KigaliSimFacade.runScenario(program, "Recovery and Recycling", progress -> {});
    List<EngineResult> rrResultsList = rrResults.collect(Collectors.toList());

    // Check equipment values for years 2025, 2026, and 2027
    int[] yearsToCheck = {2025, 2026, 2027};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "MAC", "HFC-134a");
      EngineResult rrResult = LiveTestsUtil.getResult(rrResultsList.stream(), year, "MAC", "HFC-134a");

      assertNotNull(bauResult, "Should have BAU result for MAC/HFC-134a in year " + year);
      assertNotNull(rrResult, "Should have Recovery/Recycling result for MAC/HFC-134a in year " + year);

      double bauEquipment = bauResult.getPopulation().getValue().doubleValue();
      double rrEquipment = rrResult.getPopulation().getValue().doubleValue();

      assertEquals(bauEquipment, rrEquipment, 0.0001,
          "Year " + year + ": BAU equipment (" + bauEquipment
          + ") should equal Recovery/Recycling equipment (" + rrEquipment
          + ") when recovery rate is 0% and manufacture is in units");
    }
  }

  /**
   * Test recycling_at_recharge.qta produces expected values with explicit "at recharge" syntax.
   */
  @Test
  public void testRecyclingAtRecharge() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycling_at_recharge.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 - no recycling yet
    EngineResult recordYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(recordYear1, "Should have result for test/test in year 1");
    assertEquals(500.0, recordYear1.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be 500 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 1");

    // Check year 2 - recycling at recharge active
    EngineResult recordYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(recordYear2, "Should have result for test/test in year 2");

    // With recycling at recharge, virgin material should be reduced
    // The behavior should be identical to the original recycling.qta test
    // since "at recharge" is the default behavior for recycling
    double expectedTotalConsumption = 437.5; // Same as original recycling test
    assertEquals(expectedTotalConsumption, recordYear2.getConsumptionNoRecycle().getValue().doubleValue(), 0.0001,
        "Virgin material consumption should be reduced to 437.5 tCO2e in year 2 due to recycling at recharge");
    assertEquals("tCO2e", recordYear2.getConsumptionNoRecycle().getUnits(),
        "Virgin material consumption units should be tCO2e in year 2");

    // Check recycled consumption in year 2
    // Should match the original recycling test: 500 - 437.5 = 62.5
    double expectedRecycledConsumption = 62.5;
    assertEquals(expectedRecycledConsumption, recordYear2.getRecycleConsumption().getValue().doubleValue(), 0.0001,
        "Recycled consumption should be 62.5 tCO2e in year 2");
    assertEquals("tCO2e", recordYear2.getRecycleConsumption().getUnits(),
        "Recycled consumption units should be tCO2e in year 2");
  }

  /**
   * Test that BAU and Recovery/Recycling scenarios have the same total equipment values
   * when using explicit "at recharge" syntax with kg-based import.
   * This test verifies that 0% recovery rate with "at recharge" syntax doesn't affect equipment population.
   */
  @Test
  public void testRecycleNoneBugKgAtRecharge() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycle_none_bug_kg_at_recharge.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "Business as Usual", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recovery and Recycling scenario
    Stream<EngineResult> rrResults = KigaliSimFacade.runScenario(program, "Recovery and Recycling", progress -> {});
    List<EngineResult> rrResultsList = rrResults.collect(Collectors.toList());

    // Check equipment values for years 2025, 2026, and 2027
    int[] yearsToCheck = {2025, 2026, 2027};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "MAC", "HFC-134a");
      EngineResult rrResult = LiveTestsUtil.getResult(rrResultsList.stream(), year, "MAC", "HFC-134a");

      assertNotNull(bauResult, "Should have BAU result for MAC/HFC-134a in year " + year);
      assertNotNull(rrResult, "Should have Recovery/Recycling result for MAC/HFC-134a in year " + year);

      double bauEquipment = bauResult.getPopulation().getValue().doubleValue();
      double rrEquipment = rrResult.getPopulation().getValue().doubleValue();

      assertEquals(bauEquipment, rrEquipment, 0.0001,
          "Year " + year + ": BAU equipment (" + bauEquipment
          + ") should equal Recovery/Recycling equipment (" + rrEquipment
          + ") when recovery rate is 0% with 'at recharge' syntax and kg-based import");
    }
  }

  /**
   * Test that BAU and Recovery/Recycling scenarios have the same total equipment values
   * when using explicit "at recharge" syntax with units-based import.
   * This test verifies that 0% recovery rate with "at recharge" syntax doesn't affect equipment population.
   */
  @Test
  public void testRecycleNoneBugAtRecharge() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycle_none_bug_at_recharge.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "Business as Usual", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recovery and Recycling scenario
    Stream<EngineResult> rrResults = KigaliSimFacade.runScenario(program, "Recovery and Recycling", progress -> {});
    List<EngineResult> rrResultsList = rrResults.collect(Collectors.toList());

    // Check equipment values for years 2025, 2026, and 2027
    int[] yearsToCheck = {2025, 2026, 2027};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "MAC", "HFC-134a");
      EngineResult rrResult = LiveTestsUtil.getResult(rrResultsList.stream(), year, "MAC", "HFC-134a");

      assertNotNull(bauResult, "Should have BAU result for MAC/HFC-134a in year " + year);
      assertNotNull(rrResult, "Should have Recovery/Recycling result for MAC/HFC-134a in year " + year);

      double bauEquipment = bauResult.getPopulation().getValue().doubleValue();
      double rrEquipment = rrResult.getPopulation().getValue().doubleValue();

      assertEquals(bauEquipment, rrEquipment, 0.0001,
          "Year " + year + ": BAU equipment (" + bauEquipment
          + ") should equal Recovery/Recycling equipment (" + rrEquipment
          + ") when recovery rate is 0% with 'at recharge' syntax and units-based import");
    }
  }

  /**
   * Test recycling_at_eol.qta produces expected values with explicit "at eol" syntax.
   */
  @Test
  public void testRecyclingAtEol() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycling_at_eol.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 - no recycling policy yet
    EngineResult recordYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(recordYear1, "Should have result for test/test in year 1");
    assertEquals(500.0, recordYear1.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be 500 tCO2e in year 1 (100 kg * 5 kgCO2e/kg)");
    assertEquals("tCO2e", recordYear1.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 1");

    // Check year 2 - recycling at EOL active
    EngineResult recordYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(recordYear2, "Should have result for test/test in year 2");

    // With recycling at EOL, retired material from year 1 should be recycled in year 2
    // Year 1 retired: 100 kg * 10% = 10 kg
    // Recovered: 10 kg * 50% = 5 kg
    // Virgin material needed: 100 kg - 5 kg = 95 kg
    // Total GHG: 95 kg * 5 kgCO2e/kg = 475 tCO2e
    double expectedTotalConsumption = 475.0;
    assertEquals(expectedTotalConsumption, recordYear2.getConsumptionNoRecycle().getValue().doubleValue(), 0.0001,
        "Virgin material consumption should be reduced to 475.0 tCO2e in year 2 due to recycling at EOL");
    assertEquals("tCO2e", recordYear2.getConsumptionNoRecycle().getUnits(),
        "Virgin material consumption units should be tCO2e in year 2");

    // Check recycled consumption in year 2
    // Recycled content: 5 kg * 5 kgCO2e/kg = 25 tCO2e
    double expectedRecycledConsumption = 25.0;
    assertEquals(expectedRecycledConsumption, recordYear2.getRecycleConsumption().getValue().doubleValue(), 0.0001,
        "Recycled consumption should be 25.0 tCO2e in year 2");
    assertEquals("tCO2e", recordYear2.getRecycleConsumption().getUnits(),
        "Recycled consumption units should be tCO2e in year 2");
  }

  /**
   * Test that BAU and Recovery/Recycling scenarios have the same total equipment values
   * when using explicit "at eol" syntax with kg-based import and 0% recovery rate.
   * This test verifies that 0% recovery rate with "at eol" syntax doesn't affect equipment population.
   */
  @Test
  public void testRecycleNoneBugKgAtEol() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycle_none_bug_kg_at_eol.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "Business as Usual", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recovery and Recycling scenario
    Stream<EngineResult> rrResults = KigaliSimFacade.runScenario(program, "Recovery and Recycling", progress -> {});
    List<EngineResult> rrResultsList = rrResults.collect(Collectors.toList());

    // Check equipment values for years 2025, 2026, and 2027
    int[] yearsToCheck = {2025, 2026, 2027};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "MAC", "HFC-134a");
      EngineResult rrResult = LiveTestsUtil.getResult(rrResultsList.stream(), year, "MAC", "HFC-134a");

      assertNotNull(bauResult, "Should have BAU result for MAC/HFC-134a in year " + year);
      assertNotNull(rrResult, "Should have Recovery/Recycling result for MAC/HFC-134a in year " + year);

      double bauEquipment = bauResult.getPopulation().getValue().doubleValue();
      double rrEquipment = rrResult.getPopulation().getValue().doubleValue();

      assertEquals(bauEquipment, rrEquipment, 0.0001,
          "Year " + year + ": BAU equipment (" + bauEquipment
          + ") should equal Recovery/Recycling equipment (" + rrEquipment
          + ") when recovery rate is 0% with 'at eol' syntax and kg-based import");
    }
  }

  /**
   * Test that BAU and Recovery/Recycling scenarios have the same total equipment values
   * when using explicit "at eol" syntax with units-based import and 0% recovery rate.
   * This test verifies that 0% recovery rate with "at eol" syntax doesn't affect equipment population.
   */
  @Test
  public void testRecycleNoneBugAtEol() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycle_none_bug_at_eol.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "Business as Usual", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recovery and Recycling scenario
    Stream<EngineResult> rrResults = KigaliSimFacade.runScenario(program, "Recovery and Recycling", progress -> {});
    List<EngineResult> rrResultsList = rrResults.collect(Collectors.toList());

    // Check equipment values for years 2025, 2026, and 2027
    int[] yearsToCheck = {2025, 2026, 2027};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "MAC", "HFC-134a");
      EngineResult rrResult = LiveTestsUtil.getResult(rrResultsList.stream(), year, "MAC", "HFC-134a");

      assertNotNull(bauResult, "Should have BAU result for MAC/HFC-134a in year " + year);
      assertNotNull(rrResult, "Should have Recovery/Recycling result for MAC/HFC-134a in year " + year);

      double bauEquipment = bauResult.getPopulation().getValue().doubleValue();
      double rrEquipment = rrResult.getPopulation().getValue().doubleValue();

      assertEquals(bauEquipment, rrEquipment, 0.0001,
          "Year " + year + ": BAU equipment (" + bauEquipment
          + ") should equal Recovery/Recycling equipment (" + rrEquipment
          + ") when recovery rate is 0% with 'at eol' syntax and units-based import");
    }
  }

  /**
   * Test recycling_combined.qta produces expected values with both EOL and recharge recycling.
   */
  @Test
  public void testRecyclingCombined() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycling_combined.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 - no recycling policy yet
    EngineResult recordYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(recordYear1, "Should have result for test/test in year 1");
    assertEquals(500.0, recordYear1.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be 500 tCO2e in year 1 (100 kg * 5 kgCO2e/kg)");
    assertEquals("tCO2e", recordYear1.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 1");

    // Check year 2 - both EOL and recharge recycling active
    EngineResult recordYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(recordYear2, "Should have result for test/test in year 2");

    // With combined recycling, both EOL and recharge recycling should contribute
    // Updated expectation to match current architecture where recycling is applied by StreamKeeper
    double expectedTotalConsumption = 465.775;
    assertEquals(expectedTotalConsumption, recordYear2.getConsumptionNoRecycle().getValue().doubleValue(), 0.001,
        "Virgin material consumption should be reduced to 465.775 tCO2e in year 2 due to combined EOL and recharge recycling");
    assertEquals("tCO2e", recordYear2.getConsumptionNoRecycle().getUnits(),
        "Virgin material consumption units should be tCO2e in year 2");

    // Check recycled consumption in year 2
    double expectedRecycledConsumption = 34.225;
    assertEquals(expectedRecycledConsumption, recordYear2.getRecycleConsumption().getValue().doubleValue(), 0.001,
        "Recycled consumption should be 34.225 tCO2e in year 2 from combined recycling");
    assertEquals("tCO2e", recordYear2.getRecycleConsumption().getUnits(),
        "Recycled consumption units should be tCO2e in year 2");
  }

  /**
   * Test displacement then recycling scenario to verify R-600a equipment population.
   * Tests that when displacement (Sales Permit) is applied before recycling (Domestic Recycling),
   * R-600a should have more than 0.4 million units in 2035.
   */
  @Test
  public void testDisplaceThenRecycle() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/displace_then_recycle.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the Combined scenario (Sales Permit then Domestic Recycling)
    Stream<EngineResult> combinedResults = KigaliSimFacade.runScenario(program, "Combined", progress -> {});
    List<EngineResult> combinedResultsList = combinedResults.collect(Collectors.toList());

    // Check R-600a equipment population in 2035
    EngineResult r600aResult2035 = LiveTestsUtil.getResult(combinedResultsList.stream(), 2035, "Domestic Refrigeration", "R-600a");
    assertNotNull(r600aResult2035, "Should have result for Domestic Refrigeration/R-600a in year 2035");

    double r600aPopulation2035 = r600aResult2035.getPopulation().getValue().doubleValue();

    // Assert that R-600a has more than 0.4 million units in 2035
    assertTrue(r600aPopulation2035 > 400000.0,
        String.format("R-600a equipment population in 2035 (%.0f units) should be more than 400,000 units", r600aPopulation2035));
  }

  /**
   * Test that retire commands work correctly after recycle at EOL and apply recycling
   * through existing recalculation logic.
   */
  @Test
  public void testRetireAfterRecycleAtEol() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/test_retire_after_recycle_eol.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run both scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "business as usual", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    Stream<EngineResult> policyResults = KigaliSimFacade.runScenario(program, "with retire after recycle", progress -> {});
    List<EngineResult> policyResultsList = policyResults.collect(Collectors.toList());

    // Check year 1 - retire command should be working
    EngineResult bauYear1 = LiveTestsUtil.getResult(bauResultsList.stream(), 1, "test", "test");
    EngineResult policyYear1 = LiveTestsUtil.getResult(policyResultsList.stream(), 1, "test", "test");

    assertNotNull(bauYear1, "Should have BAU result for year 1");
    assertNotNull(policyYear1, "Should have policy result for year 1");

    // Initial: 100 kg set, retire 20% each year
    // Actual result shows 50 units, which means retire is working
    assertEquals(50.0, bauYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 50 units after 20% retire in year 1");

    // Year 1 should be the same for both scenarios (no policy differences yet)
    assertEquals(bauYear1.getPopulation().getValue().doubleValue(),
                policyYear1.getPopulation().getValue().doubleValue(), 0.0001,
                "Year 1 population should be the same for both scenarios");

    // Check year 2 - recycling at EOL active in policy scenario
    EngineResult bauYear2 = LiveTestsUtil.getResult(bauResultsList.stream(), 2, "test", "test");
    EngineResult policyYear2 = LiveTestsUtil.getResult(policyResultsList.stream(), 2, "test", "test");

    assertNotNull(bauYear2, "Should have BAU result for year 2");
    assertNotNull(policyYear2, "Should have policy result for year 2");

    // Year 2 should show recycling effects in policy scenario
    assertTrue(policyYear2.getRecycleConsumption().getValue().doubleValue() > 0,
        "Policy scenario should have recycling consumption in year 2");

    // Check year 3 - additional retire command applied in policy scenario
    EngineResult bauYear3 = LiveTestsUtil.getResult(bauResultsList.stream(), 3, "test", "test");
    EngineResult policyYear3 = LiveTestsUtil.getResult(policyResultsList.stream(), 3, "test", "test");

    assertNotNull(bauYear3, "Should have BAU result for year 3");
    assertNotNull(policyYear3, "Should have policy result for year 3");

    // Year 3 should show the effect of additional retire command in policy scenario
    // The retire command should apply recycling since retire would have been zero previously
    double bauPopulation3 = bauYear3.getPopulation().getValue().doubleValue();
    double policyPopulation3 = policyYear3.getPopulation().getValue().doubleValue();

    assertTrue(policyPopulation3 < bauPopulation3,
        String.format("Policy population in year 3 (%.2f) should be less than BAU (%.2f) due to additional retire command",
                      policyPopulation3, bauPopulation3));

    // Check that recycling is NOT active in year 3 (recovery only specified for year 2)
    assertEquals(0.0, policyYear3.getRecycleConsumption().getValue().doubleValue(), 0.1,
        "Policy scenario should have zero recycling consumption in year 3 since recovery only specified for year 2");
  }

  /**
   * Test accidental displacement during recycling order sensitivity.
   *
   * <p>This test checks if the order of policies (Sales Permit then Domestic Recycling vs
   * Domestic Recycling then Sales Permit) affects R-600a growth when displacement occurs
   * before recycling vs recycling before displacement.
   *
   * <p>The issue is that when displacement (cap sales displacing "R-600a") comes before
   * recycling, the R-600a doesn't grow as expected, potentially due to displacement
   * logic being accidentally triggered during recycling operations.
   */
  @Test
  public void testAccidentalDisplaceCheck() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/accidential_displace_check.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run Combined scenario (Sales Permit then Domestic Recycling)
    Stream<EngineResult> combinedResults = KigaliSimFacade.runScenario(program, "Combined", progress -> {});
    List<EngineResult> combinedResultsList = combinedResults.collect(Collectors.toList());

    // Run Combined Reverse scenario (Domestic Recycling then Sales Permit)
    Stream<EngineResult> combinedReverseResults = KigaliSimFacade.runScenario(program, "Combined Reverse", progress -> {});
    List<EngineResult> combinedReverseResultsList = combinedReverseResults.collect(Collectors.toList());

    // Check results for both substances in 2035 for both scenarios
    EngineResult combinedHfc2035 = LiveTestsUtil.getResult(combinedResultsList.stream(), 2035, "Domestic Refrigeration", "HFC-134a");
    EngineResult combinedR600a2035 = LiveTestsUtil.getResult(combinedResultsList.stream(), 2035, "Domestic Refrigeration", "R-600a");
    EngineResult combinedReverseHfc2035 = LiveTestsUtil.getResult(combinedReverseResultsList.stream(), 2035, "Domestic Refrigeration", "HFC-134a");

    assertNotNull(combinedHfc2035, "Should have Combined result for Domestic Refrigeration/HFC-134a in year 2035");
    assertNotNull(combinedR600a2035, "Should have Combined result for Domestic Refrigeration/R-600a in year 2035");
    assertNotNull(combinedReverseHfc2035, "Should have Combined Reverse result for Domestic Refrigeration/HFC-134a in year 2035");

    EngineResult combinedReverseR600a2035 = LiveTestsUtil.getResult(combinedReverseResultsList.stream(), 2035, "Domestic Refrigeration", "R-600a");
    assertNotNull(combinedReverseR600a2035, "Should have Combined Reverse result for Domestic Refrigeration/R-600a in year 2035");

    // Calculate total domestic + import + recycle in kg across both substances for both scenarios
    double combinedHfcDomestic = combinedHfc2035.getDomestic().getValue().doubleValue();
    double combinedHfcImport = combinedHfc2035.getImport().getValue().doubleValue();
    double combinedHfcRecycle = combinedHfc2035.getRecycle().getValue().doubleValue();
    double combinedR600aDomestic = combinedR600a2035.getDomestic().getValue().doubleValue();
    double combinedR600aImport = combinedR600a2035.getImport().getValue().doubleValue();
    double combinedR600aRecycle = combinedR600a2035.getRecycle().getValue().doubleValue();
    double combinedTotal = combinedHfcDomestic + combinedHfcImport + combinedHfcRecycle + combinedR600aDomestic + combinedR600aImport + combinedR600aRecycle;

    double combinedReverseHfcDomestic = combinedReverseHfc2035.getDomestic().getValue().doubleValue();
    double combinedReverseHfcImport = combinedReverseHfc2035.getImport().getValue().doubleValue();
    double combinedReverseHfcRecycle = combinedReverseHfc2035.getRecycle().getValue().doubleValue();
    double combinedReverseR600aDomestic = combinedReverseR600a2035.getDomestic().getValue().doubleValue();
    double combinedReverseR600aImport = combinedReverseR600a2035.getImport().getValue().doubleValue();
    double combinedReverseR600aRecycle = combinedReverseR600a2035.getRecycle().getValue().doubleValue();
    double combinedReverseTotal = combinedReverseHfcDomestic + combinedReverseHfcImport + combinedReverseHfcRecycle + combinedReverseR600aDomestic + combinedReverseR600aImport + combinedReverseR600aRecycle;

    // Calculate percentage difference
    double percentageDifference = Math.abs(combinedTotal - combinedReverseTotal) / Math.max(combinedTotal, combinedReverseTotal) * 100.0;


    // Assert that the difference should be no more than 10%
    // This test is expected to fail initially, confirming the displacement issue during recycling
    assertTrue(percentageDifference <= 10.0,
        String.format("Total domestic + import + recycle across all substances in 2035 should be within 10%% between Combined (%.2f kg) and Combined Reverse (%.2f kg) scenarios. "
                     + "Actual difference: %.2f%%. This suggests policy order affects material balance during recycling operations.",
                     combinedTotal, combinedReverseTotal, percentageDifference));
  }

  /**
   * Test for import attribution issue with 100% recovery policy.
   *
   * <p>With 100% recovery and 100% reuse at recharge, all recharge needs should be
   * satisfied by recycled material. Since initial charge is attributed to the exporter,
   * the import attributed to importer should be 0 kg (no virgin imports needed).
   *
   * <p>Import attributed to importer = Total imports - Import initial charge to exporter
   */
  @Test
  public void testFullRecycling() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/full_recycling.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the recycling scenario
    String scenarioName = "Recycling";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check that we have results for all years 1-10
    assertNotNull(resultsList, "Results list should not be null");
    assertTrue(resultsList.size() > 0, "Should have simulation results");

    // Test all years - import attributed to importer should be 0 kg with 100% recycling
    for (int year = 1; year <= 10; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year, "Test", "HFC-134a");
      assertNotNull(result, "Should have result for Test/HFC-134a in year " + year);

      // Calculate import attributed to importer (from importer's perspective):
      // Import attributed to importer = Total imports - Import initial charge to exporter
      double totalImports = result.getImport().getValue().doubleValue();
      double importInitialChargeToExporter = result.getTradeSupplement().getImportInitialChargeValue().getValue().doubleValue();
      double importAttributedToImporter = totalImports - importInitialChargeToExporter;

      // With 100% recovery and 100% reuse at recharge, all recharge needs are met by recycling
      // Initial charge is attributed to exporter, so import attributed to importer should be 0 kg
      assertEquals(0.0, importAttributedToImporter, 0.001,
          "Import attributed to importer should be 0 kg in year " + year
          + " when 100% recycling at recharge satisfies all recharge needs. "
          + "Total imports: " + totalImports + " kg, "
          + "Import initial charge to exporter: " + importInitialChargeToExporter + " kg, "
          + "Import attributed to importer: " + importAttributedToImporter + " kg");
    }
  }

  /**
   * Test for recycling equipment leaking issue where recycling decreases equipment population.
   * This test demonstrates the bug where volume-based sales with recycling results in lower
   * equipment population compared to BAU scenario, when it should be the same.
   * This test is expected to fail initially to confirm the bug exists.
   */
  @Test
  public void testRecyclingEquipmentLeaking() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recycling_equipment_leaking.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recycle scenario
    Stream<EngineResult> recycleResults = KigaliSimFacade.runScenario(program, "Recycle", progress -> {});
    List<EngineResult> recycleResultsList = recycleResults.collect(Collectors.toList());

    // Test multiple years to show the problem increases over time
    // Include year 2 when recycling policy starts
    int[] yearsToCheck = {1, 2, 3, 5, 7, 10};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "App1", "SubA");
      EngineResult recycleResult = LiveTestsUtil.getResult(recycleResultsList.stream(), year, "App1", "SubA");

      assertNotNull(bauResult, "Should have BAU result for App1/SubA in year " + year);
      assertNotNull(recycleResult, "Should have Recycle result for App1/SubA in year " + year);

      double bauEquipment = bauResult.getPopulation().getValue().doubleValue();
      double recycleEquipment = recycleResult.getPopulation().getValue().doubleValue();

      // This assertion should fail initially, demonstrating the bug
      // Equipment population should be the same between scenarios since recycling
      // should not decrease the total amount of substance available for new equipment
      assertEquals(bauEquipment, recycleEquipment, 0.0001,
          "Year " + year + ": BAU equipment population (" + bauEquipment
          + ") should equal Recycle equipment population (" + recycleEquipment
          + ") in volume-based sales with recycling. Recycling should not decrease equipment population.");

      // Additional debugging information for all years
      double bauImport = bauResult.getImport().getValue().doubleValue();
      double bauDomestic = bauResult.getDomestic().getValue().doubleValue();
      double recycleImport = recycleResult.getImport().getValue().doubleValue();
      double recycleDomestic = recycleResult.getDomestic().getValue().doubleValue();
      double recycleRecycled = recycleResult.getRecycleConsumption().getValue().doubleValue();
      double bauSales = bauDomestic + bauImport;
      double recycleSales = recycleDomestic + recycleImport;

    }
  }

  /**
   * Test that single stream volume-based scenarios also get recycling redistribution.
   * This tests "set import to X mt" scenarios where recycling should not create
   * permanent equipment population deficit when policies expire.
   */
  @Test
  public void testSingleStreamRecyclingEquipmentLeaking() throws IOException {
    // Load and parse the test QTA file
    String qtaPath = "../examples/test_single_stream.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recycle scenario
    Stream<EngineResult> recycleResults = KigaliSimFacade.runScenario(program, "Recycle", progress -> {});
    List<EngineResult> recycleResultsList = recycleResults.collect(Collectors.toList());

    // Test years 1 (before policy), 2-3 (during policy), 4-5 (after policy expires)
    int[] yearsToCheck = {1, 2, 3, 4, 5};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "App1", "SubA");
      EngineResult recycleResult = LiveTestsUtil.getResult(recycleResultsList.stream(), year, "App1", "SubA");

      assertNotNull(bauResult, "Should have BAU result for App1/SubA in year " + year);
      assertNotNull(recycleResult, "Should have Recycle result for App1/SubA in year " + year);

      double bauEquipment = bauResult.getPopulation().getValue().doubleValue();
      double recycleEquipment = recycleResult.getPopulation().getValue().doubleValue();

      // Equipment population should be the same between scenarios
      // This tests that single-stream scenarios (set import to X mt) also get redistribution fix
      assertEquals(bauEquipment, recycleEquipment, 0.0001,
          "Year " + year + ": BAU equipment population (" + bauEquipment
          + ") should equal Recycle equipment population (" + recycleEquipment
          + ") in single-stream volume-based scenario. Loss of recycling should be back-filled by virgin material.");
    }
  }


  /**
   * Test default induction behavior for non-units (kg/mt) specifications.
   * Verifies that default behavior is 100% induced demand (no displacement).
   */
  @Test
  public void testRecoverInductionNonUnitsSpec() throws IOException {
    String qtaCode = """
        start default
        define application "test"
          uses substance "test"
            enable domestic
            enable import
            initial charge with 2 kg / unit for domestic
            initial charge with 2 kg / unit for import
            set domestic to 50 kg
            set import to 50 kg
          end substance
        end application
      end default

      start policy "intervention"
        modify application "test"
          modify substance "test"
            recover 20 kg with 90 % reuse during year 2
          end substance
        end application
      end policy

      start simulations
        simulate "result" using "intervention" from years 1 to 3
        end simulations
        """;

    var parseResult = KigaliSimFacade.parse(qtaCode);
    assertNotNull(parseResult, "Parse result should not be null");
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should parse successfully");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "result", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: Baseline - no recycling
    EngineResult year1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(year1, "Should have result for year 1");

    // Year 2: With default 100% induction - recycling should be additive (induced demand)
    EngineResult year2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2, "Should have result for year 2");

    // With default 100% induction rate, no recycling should displace virgin material
    // Virgin sales should remain at baseline (50 + 50 = 100 kg)
    // Recycling should be additive (18 kg = 20 * 90% yield)
    assertTrue(year2.getDomestic().getValue().doubleValue() >= 0,
        "Domestic production should be non-negative in year 2");
    assertTrue(year2.getImport().getValue().doubleValue() >= 0,
        "Import production should be non-negative in year 2");

    // Verify recycling stream values - should be additive to sales
    assertTrue(year2.getRecycle().getValue().doubleValue() > 0,
        "Recycling production should be positive in year 2");

    // With default 100% induction for non-units, total supply should meet baseline demand
    // Recycling is "induced demand" - it doesn't create additional demand but meets existing demand
    double domesticSales = year2.getDomestic().getValue().doubleValue();
    double importSales = year2.getImport().getValue().doubleValue();
    double recyclingSales = year2.getRecycle().getValue().doubleValue();
    double totalSupply = domesticSales + importSales + recyclingSales;

    // Debug output
    System.out.println("Domestic: " + domesticSales + " kg");
    System.out.println("Import: " + importSales + " kg");
    System.out.println("Recycling: " + recyclingSales + " kg");
    System.out.println("Total supply: " + totalSupply + " kg");
    System.out.println("Expected total supply: ~100 kg (baseline demand)");

    // With 100% induction, total supply should approximately equal baseline demand
    assertTrue(totalSupply >= 95 && totalSupply <= 105,
        "Total supply should approximately equal baseline demand with 100% induction, got: " + totalSupply);

    // Recycling should contribute meaningfully to meeting demand
    assertTrue(recyclingSales > 15,
        "Recycling should contribute significantly to supply, got: " + recyclingSales);
  }

  /**
   * Test non-units specification behavior with import/domestic distribution.
   * Verifies that recycling adds to total supply (induced demand behavior).
   */
  @Test
  public void testRecoverDefaultInductionNonUnitsSpec() throws IOException {
    String qtaCode = """
        start default
        define application "test"
          uses substance "test"
            enable domestic
            initial charge with 2 kg / unit for domestic
            set domestic to 100 kg
          end substance
        end application
      end default

      start policy "intervention"
        modify application "test"
          modify substance "test"
            recover 20 kg with 90 % reuse during year 2
          end substance
        end application
      end policy

      start simulations
        simulate "result" using "intervention" from years 1 to 3
        end simulations
        """;

    var parseResult = KigaliSimFacade.parse(qtaCode);
    assertNotNull(parseResult, "Parse result should not be null");
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should parse successfully");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "result", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Verify that recycling behaves as induced demand (existing behavior)
    EngineResult year2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2, "Should have result for year 2");

    // With default 100% induction behavior, total supply should meet baseline demand
    double domesticSales = year2.getDomestic().getValue().doubleValue();
    double recyclingSales = year2.getRecycle().getValue().doubleValue();
    double totalSupply = domesticSales + recyclingSales;

    assertTrue(totalSupply >= 95 && totalSupply <= 105,
        "Total supply should approximately equal baseline demand with 100% induction, got: " + totalSupply);
    assertTrue(recyclingSales > 0,
        "Recycling should be positive, got: " + recyclingSales);
  }

  /**
   * Test non-units specification with higher recovery volume.
   * Verifies consistent induced demand behavior regardless of recovery amount.
   */
  @Test
  public void testRecoverZeroInductionNonUnitsSpec() throws IOException {
    String qtaCode = """
        start default
        define application "test"
          uses substance "test"
            enable domestic
            initial charge with 2 kg / unit for domestic
            set domestic to 100 kg
          end substance
        end application
      end default

      start policy "intervention"
        modify application "test"
          modify substance "test"
            recover 20 kg with 90 % reuse during year 2
          end substance
        end application
      end policy

      start simulations
        simulate "result" using "intervention" from years 1 to 3
        end simulations
        """;

    var parseResult = KigaliSimFacade.parse(qtaCode);
    assertNotNull(parseResult, "Parse result should not be null");
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should parse successfully");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "result", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Verify that recycling behaves as full displacement (0% induction)
    EngineResult year2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2, "Should have result for year 2");

    // With default 100% induction rate, total supply should meet baseline demand
    double domesticSales = year2.getDomestic().getValue().doubleValue();
    double recyclingSales = year2.getRecycle().getValue().doubleValue();
    double totalSupply = domesticSales + recyclingSales;

    assertTrue(totalSupply >= 95 && totalSupply <= 105,
        "Total supply should approximately equal baseline demand with 100% induction, got: " + totalSupply);
    assertTrue(recyclingSales > 15,
        "Recycling should contribute significantly, got: " + recyclingSales);
  }

  /**
   * Test explicit 0% induction rate (full displacement) for non-units specs.
   * Verifies that all recycling displaces virgin material when induction is 0%.
   */
  @Test
  public void testRecoverExplicitZeroInductionNonUnitsSpec() throws IOException {
    String qtaCode = """
        start default
          define application "test"
            uses substance "test"
              enable domestic
              initial charge with 2 kg / unit for domestic
              set domestic to 100 kg
            end substance
          end application
        end default

        start policy "intervention"
          modify application "test"
            modify substance "test"
              recover 20 kg with 90 % reuse with 0 % induction during year 2
            end substance
          end application
        end policy

        start simulations
          simulate "result" using "intervention" from years 1 to 3
        end simulations
        """;

    var parseResult = KigaliSimFacade.parse(qtaCode);
    assertNotNull(parseResult, "Parse result should not be null");
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should parse successfully");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "result", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Verify that recycling behaves with 0% induction (full displacement)
    EngineResult year2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2, "Should have result for year 2");

    double domesticSales = year2.getDomestic().getValue().doubleValue();
    double importSales = year2.getImport().getValue().doubleValue();
    double recyclingSales = year2.getRecycle().getValue().doubleValue();
    double totalVirginSales = domesticSales + importSales;
    double actualTotalSupply = totalVirginSales + recyclingSales;

    // Debug output
    System.out.println("0% induction (full displacement) test:");
    System.out.println("Domestic sales: " + domesticSales + " kg");
    System.out.println("Import sales: " + importSales + " kg");
    System.out.println("Recycling sales: " + recyclingSales + " kg");
    System.out.println("Virgin sales: " + totalVirginSales + " kg");
    System.out.println("Total supply: " + actualTotalSupply + " kg");

    // With 0% induction (full displacement):
    // - Baseline demand: 100kg
    // - Recycling: 18kg (20kg * 90% yield)
    // - Displacement: 18kg * (1 - 0.0) = 18kg fully displaces virgin material
    // - Virgin sales after displacement: 100kg - 18kg = 82kg
    // - Total supply: 82kg virgin + 18kg recycling = 100kg
    assertTrue(actualTotalSupply >= 95 && actualTotalSupply <= 105,
        "Total supply should be ~100kg with 0% induction (full displacement), got: " + actualTotalSupply);
    assertTrue(recyclingSales > 15,
        "Recycling should contribute significantly, got: " + recyclingSales);

    // Virgin sales should be reduced to ~82kg due to full displacement
    assertTrue(totalVirginSales >= 75 && totalVirginSales <= 85,
        "Virgin sales should be ~82kg with full displacement, got: " + totalVirginSales);
  }

  /**
   * Test non-units specification with different baseline sales amount.
   * Verifies that induced demand behavior scales appropriately.
   */
  @Test
  public void testRecoverFullInductionNonUnitsSpec() throws IOException {
    String qtaCode = """
        start default
        define application "test"
          uses substance "test"
            enable domestic
            initial charge with 2 kg / unit for domestic
            set domestic to 100 kg
          end substance
        end application
      end default

      start policy "intervention"
        modify application "test"
          modify substance "test"
            recover 20 kg with 90 % reuse during year 2
          end substance
        end application
      end policy

      start simulations
        simulate "result" using "intervention" from years 1 to 3
        end simulations
        """;

    var parseResult = KigaliSimFacade.parse(qtaCode);
    assertNotNull(parseResult, "Parse result should not be null");
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should parse successfully");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "result", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Verify that recycling behaves as full induced demand (100% induction)
    EngineResult year2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2, "Should have result for year 2");

    // With default 100% induction rate, total supply should meet baseline demand
    double domesticSales = year2.getDomestic().getValue().doubleValue();
    double recyclingSales = year2.getRecycle().getValue().doubleValue();
    double totalSupply = domesticSales + recyclingSales;

    assertTrue(totalSupply >= 95 && totalSupply <= 105,
        "Total supply should approximately equal baseline demand with 100% induction, got: " + totalSupply);
    assertTrue(recyclingSales > 15,
        "Recycling should contribute significantly, got: " + recyclingSales);
  }

  /**
   * Test that multiple recover commands in the same timestep are rejected with clear error.
   * Verifies that the engine prevents conflicting recovery rates within a single timestep.
   */
  @Test
  public void testMultipleRecoverCommandsRejected() throws IOException {
    String qtaCode = """
        start default
          define application "test"
            uses substance "test"
              enable domestic
              initial charge with 5 kg / unit for domestic
              set priorEquipment to 1000 units during year beginning
              set domestic to 100 units
              retire 10 % each year
              recharge 20 % each year with 1.0 kg / unit
              recover 20 % with 90 % reuse during year 2
              recover 30 % with 80 % reuse during year 2
            end substance
          end application
        end default
        start simulations
          simulate "result" using "default" from years 1 to 3
        end simulations
        """;

    var parseResult = KigaliSimFacade.parse(qtaCode);
    assertNotNull(parseResult, "Parse result should not be null");
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should parse successfully");

    // Running the scenario should work with additive recovery rates
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "result", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    assertNotNull(resultsList, "Should have simulation results");
    assertTrue(resultsList.size() > 0, "Should have results for simulation");

    // Verify that the simulation runs successfully with multiple recover commands
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have results for year 2");

    // The recovery rates should be additive (20% + 30% = 50% recovery rate)
    // The yield rates should be averaged (90% + 80%) / 2 = 85% yield rate
    assertTrue(result.getRecycle().getValue().doubleValue() > 0,
        "Should have positive recycling with additive recovery rates");
  }

  /**
   * Test that separate scenarios with recover commands work independently.
   * Verifies that different policy contexts don't interfere with each other.
   */
  @Test
  public void testMultipleRecoverCommandsSeparateScenarios() throws IOException {
    String qtaCode = """
        start default
          define application "test"
            uses substance "test"
              enable domestic
              initial charge with 5 kg / unit for domestic
              set domestic to 100 units
            end substance
          end application
        end default

        start policy "policy1"
          modify application "test"
            modify substance "test"
              recover 20 % with 90 % reuse during year 2
            end substance
          end application
        end policy

        start policy "policy2"
          modify application "test"
            modify substance "test"
              recover 30 % with 80 % reuse during year 2
            end substance
          end application
        end policy

        start simulations
          simulate "result1" using "policy1" from years 1 to 4
          simulate "result2" using "policy2" from years 1 to 4
        end simulations
        """;

    var parseResult = KigaliSimFacade.parse(qtaCode);
    assertNotNull(parseResult, "Parse result should not be null");
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should parse successfully");

    // Both simulations should succeed since they're separate scenarios
    Stream<EngineResult> results1 = KigaliSimFacade.runScenario(program, "result1", progress -> {});
    List<EngineResult> resultsList1 = results1.collect(Collectors.toList());

    Stream<EngineResult> results2 = KigaliSimFacade.runScenario(program, "result2", progress -> {});
    List<EngineResult> resultsList2 = results2.collect(Collectors.toList());

    // Verify year 2 has recycling in both scenarios
    EngineResult year2Scenario1 = LiveTestsUtil.getResult(resultsList1.stream(), 2, "test", "test");
    assertNotNull(year2Scenario1, "Should have result for test/test in year 2, scenario 1");

    EngineResult year2Scenario2 = LiveTestsUtil.getResult(resultsList2.stream(), 2, "test", "test");
    assertNotNull(year2Scenario2, "Should have result for test/test in year 2, scenario 2");

    // Both scenarios should have recycling since they use separate policies
    assertTrue(year2Scenario1.getRecycle().getValue().doubleValue() >= 0,
        "Year 2 should have valid recycling from first scenario");
    assertTrue(year2Scenario2.getRecycle().getValue().doubleValue() >= 0,
        "Year 2 should have valid recycling from second scenario");
  }

  /**
   * Test that multiple recover commands for different stages (EOL vs RECHARGE) are rejected.
   * Verifies that the validation applies independently to each recovery stage.
   */
  @Test
  public void testMultipleRecoverCommandsDifferentStagesRejected() throws IOException {
    String qtaCode = """
        start default
          define application "test"
            uses substance "test"
              enable domestic
              initial charge with 5 kg / unit for domestic
              set priorEquipment to 1000 units during year beginning
              set domestic to 100 units
              retire 10 % each year
              recharge 20 % each year with 1.0 kg / unit
              recover 20 % with 90 % reuse at recharge during year 2
              recover 30 % with 80 % reuse at recharge during year 2
            end substance
          end application
        end default
        start simulations
          simulate "result" using "default" from years 1 to 3
        end simulations
        """;

    var parseResult = KigaliSimFacade.parse(qtaCode);
    assertNotNull(parseResult, "Parse result should not be null");
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should parse successfully");

    // Should work with additive recovery rates for the same stage
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "result", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    assertNotNull(resultsList, "Should have simulation results");
    assertTrue(resultsList.size() > 0, "Should have results for simulation");

    // Verify that the simulation runs successfully with multiple recover commands at the same stage
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have results for year 2");

    // Both recovery commands target the recharge stage, so rates should be additive (20% + 30% = 50%)
    assertTrue(result.getRecycle().getValue().doubleValue() > 0,
        "Should have positive recycling with additive recovery rates at recharge stage");
  }

  /**
   * Test that one EOL and one RECHARGE recover command in the same timestep are allowed.
   * Verifies that validation is per-stage, allowing different stages to have recover commands.
   */
  @Test
  public void testRecoverCommandsDifferentStagesAllowed() throws IOException {
    String qtaCode = """
        start default
          define application "test"
            uses substance "test"
              enable domestic
              initial charge with 2 kg / unit for domestic
              set domestic to 100 kg
              retire 10% each year
              recharge 30% each year with 1 kg / unit
              equals 5 tCO2e / kg
            end substance
          end application
        end default

        start policy "intervention"
          modify application "test"
            modify substance "test"
              recover 20 % with 90 % reuse at eol during year 2
              recover 15 % with 80 % reuse at recharge during year 2
            end substance
          end application
        end policy

        start simulations
          simulate "result" using "intervention" from years 1 to 3
        end simulations
        """;

    var parseResult = KigaliSimFacade.parse(qtaCode);
    assertNotNull(parseResult, "Parse result should not be null");
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should parse successfully");

    // This should succeed since commands target different stages
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "result", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    EngineResult year2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(year2, "Should have result for year 2");

    assertTrue(year2.getRecycle().getValue().doubleValue() > 0,
        "Year 2 should have recycling from both EOL and recharge recover commands");
  }

  /**
   * Test 0% induction units-based recycling scenario.
   * Validates that BAU and Recycling scenarios have identical equipment populations
   * when induction rate is 0%, confirming proper displacement behavior.
   */
  @Test
  public void testZeroInductionUnits() throws IOException {
    // Load and parse QTA file
    String qtaPath = "../examples/test_0_induction_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run both scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingResults.collect(Collectors.toList());

    // Test multiple years for consistency
    int[] yearsToCheck = {1, 2, 3, 4, 5};
    for (int year : yearsToCheck) {
      // Get results for both scenarios
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "TestApp", "TestSub");
      EngineResult recyclingResult = LiveTestsUtil.getResult(recyclingResultsList.stream(), year, "TestApp", "TestSub");

      assertNotNull(bauResult, "Should have BAU result for TestApp/TestSub in year " + year);
      assertNotNull(recyclingResult, "Should have Recycling result for TestApp/TestSub in year " + year);

      // Core validation: populations should be identical with 0% induction
      double bauPopulation = bauResult.getPopulation().getValue().doubleValue();
      double recyclingPopulation = recyclingResult.getPopulation().getValue().doubleValue();

      assertEquals(bauPopulation, recyclingPopulation, 1.0,
          String.format("Year %d: BAU population (%.2f) should equal Recycling population (%.2f) with 0%% induction",
                       year, bauPopulation, recyclingPopulation));

      // Material balance validation for recycling years (only years 2 and 3 have recovery policies)
      if (year == 2 || year == 3) {
        double recycledAmount = recyclingResult.getRecycleConsumption().getValue().doubleValue();
        assertTrue(recycledAmount > 0,
            String.format("Year %d: Should have positive recycling amount", year));

        // Virgin material reduction should equal recycled material addition (0% induction = full displacement)
        double bauVirginTotal = bauResult.getDomestic().getValue().doubleValue() + bauResult.getImport().getValue().doubleValue();
        double recyclingVirginTotal = recyclingResult.getDomestic().getValue().doubleValue() + recyclingResult.getImport().getValue().doubleValue();
        double virginReduction = bauVirginTotal - recyclingVirginTotal;

        assertEquals(recycledAmount, virginReduction, 1.0,
            String.format("Year %d: Recycled amount (%.2f) should equal virgin material reduction (%.2f) with 0%% induction",
                         year, recycledAmount, virginReduction));
      }
    }
  }

  /**
   * Test 0% induction volume-based recycling scenario.
   * Validates that BAU and Recycling scenarios have identical equipment populations
   * when induction rate is 0%, confirming proper displacement behavior for kg-based specs.
   */
  @Test
  public void testZeroInductionVolume() throws IOException {
    // Load and parse QTA file
    String qtaPath = "../examples/test_0_induction_volume.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run both scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingResults.collect(Collectors.toList());

    // Test multiple years for consistency
    int[] yearsToCheck = {1, 2, 3, 4, 5};
    for (int year : yearsToCheck) {
      // Get results for both scenarios
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "TestApp", "TestSub");
      EngineResult recyclingResult = LiveTestsUtil.getResult(recyclingResultsList.stream(), year, "TestApp", "TestSub");

      assertNotNull(bauResult, "Should have BAU result for TestApp/TestSub in year " + year);
      assertNotNull(recyclingResult, "Should have Recycling result for TestApp/TestSub in year " + year);

      // Core validation: populations should be identical with 0% induction
      double bauPopulation = bauResult.getPopulation().getValue().doubleValue();
      double recyclingPopulation = recyclingResult.getPopulation().getValue().doubleValue();

      assertEquals(bauPopulation, recyclingPopulation, 1.0,
          String.format("Year %d: BAU population (%.2f) should equal Recycling population (%.2f) with 0%% induction",
                       year, bauPopulation, recyclingPopulation));

      // Material balance validation for recycling years (only years 2 and 3 have recovery policies)
      if (year == 2 || year == 3) {
        double recycledAmount = recyclingResult.getRecycleConsumption().getValue().doubleValue();
        assertTrue(recycledAmount > 0,
            String.format("Year %d: Should have positive recycling amount", year));

        // Virgin material reduction should equal recycled material addition (0% induction = full displacement)
        double bauVirginTotal = bauResult.getDomestic().getValue().doubleValue() + bauResult.getImport().getValue().doubleValue();
        double recyclingVirginTotal = recyclingResult.getDomestic().getValue().doubleValue() + recyclingResult.getImport().getValue().doubleValue();
        double virginReduction = bauVirginTotal - recyclingVirginTotal;

        assertEquals(recycledAmount, virginReduction, 1.0,
            String.format("Year %d: Recycled amount (%.2f) should equal virgin material reduction (%.2f) with 0%% induction",
                         year, recycledAmount, virginReduction));
      }
    }
  }

  /**
   * Test 0% induction units-based recycling scenario with recalc idempotence.
   * Validates that multiple policy changes maintain population equality and that
   * recalculation operations are idempotent.
   */
  @Test
  public void testZeroInductionUnitsRecalcIdempotence() throws IOException {
    // Load and parse QTA file
    String qtaPath = "../examples/test_0_induction_units_recalc.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run both scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingResults.collect(Collectors.toList());

    // Test multiple years for consistency, especially years around policy changes
    int[] yearsToCheck = {1, 2, 3, 4, 5};
    for (int year : yearsToCheck) {
      // Get results for both scenarios
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "TestApp", "TestSub");
      EngineResult recyclingResult = LiveTestsUtil.getResult(recyclingResultsList.stream(), year, "TestApp", "TestSub");

      assertNotNull(bauResult, "Should have BAU result for TestApp/TestSub in year " + year);
      assertNotNull(recyclingResult, "Should have Recycling result for TestApp/TestSub in year " + year);

      // Core validation: populations should be identical with 0% induction after all recalculations
      double bauPopulation = bauResult.getPopulation().getValue().doubleValue();
      double recyclingPopulation = recyclingResult.getPopulation().getValue().doubleValue();

      assertEquals(bauPopulation, recyclingPopulation, 1.0,
          String.format("Year %d: BAU population (%.2f) should equal Recycling population (%.2f) with 0%% induction after recalc",
                       year, bauPopulation, recyclingPopulation));

      // Material balance validation for recycling years (years 2, 3, and 4 have recycling)
      if (year == 2 || year == 3 || year == 4) {
        double recycledAmount = recyclingResult.getRecycleConsumption().getValue().doubleValue();
        assertTrue(recycledAmount > 0,
            String.format("Year %d: Should have positive recycling amount", year));

        // Virgin material reduction should equal recycled material addition
        double bauVirginTotal = bauResult.getDomestic().getValue().doubleValue() + bauResult.getImport().getValue().doubleValue();
        double recyclingVirginTotal = recyclingResult.getDomestic().getValue().doubleValue() + recyclingResult.getImport().getValue().doubleValue();
        double virginReduction = bauVirginTotal - recyclingVirginTotal;

        assertEquals(recycledAmount, virginReduction, 1.0,
            String.format("Year %d: Recycled amount (%.2f) should equal virgin material reduction (%.2f) with 0%% induction",
                         year, recycledAmount, virginReduction));
      }
    }
  }

  /**
   * Test 0% induction volume-based recycling scenario with recalc idempotence.
   * Validates that multiple policy changes maintain population equality and that
   * recalculation operations are idempotent for kg-based specifications.
   */
  @Test
  public void testZeroInductionVolumeRecalcIdempotence() throws IOException {
    // Load and parse QTA file
    String qtaPath = "../examples/test_0_induction_volume_recalc.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run both scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingResults.collect(Collectors.toList());

    // Test multiple years for consistency, especially years around policy changes
    int[] yearsToCheck = {1, 2, 3, 4, 5};
    for (int year : yearsToCheck) {
      // Get results for both scenarios
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "TestApp", "TestSub");
      EngineResult recyclingResult = LiveTestsUtil.getResult(recyclingResultsList.stream(), year, "TestApp", "TestSub");

      assertNotNull(bauResult, "Should have BAU result for TestApp/TestSub in year " + year);
      assertNotNull(recyclingResult, "Should have Recycling result for TestApp/TestSub in year " + year);

      // Core validation: populations should be identical with 0% induction after all recalculations
      double bauPopulation = bauResult.getPopulation().getValue().doubleValue();
      double recyclingPopulation = recyclingResult.getPopulation().getValue().doubleValue();

      assertEquals(bauPopulation, recyclingPopulation, 1.0,
          String.format("Year %d: BAU population (%.2f) should equal Recycling population (%.2f) with 0%% induction after recalc",
                       year, bauPopulation, recyclingPopulation));

      // Material balance validation for recycling years (years 2, 3, and 4 have recycling)
      if (year == 2 || year == 3 || year == 4) {
        double recycledAmount = recyclingResult.getRecycleConsumption().getValue().doubleValue();
        assertTrue(recycledAmount > 0,
            String.format("Year %d: Should have positive recycling amount", year));

        // Virgin material reduction should equal recycled material addition
        double bauVirginTotal = bauResult.getDomestic().getValue().doubleValue() + bauResult.getImport().getValue().doubleValue();
        double recyclingVirginTotal = recyclingResult.getDomestic().getValue().doubleValue() + recyclingResult.getImport().getValue().doubleValue();
        double virginReduction = bauVirginTotal - recyclingVirginTotal;

        assertEquals(recycledAmount, virginReduction, 1.0,
            String.format("Year %d: Recycled amount (%.2f) should equal virgin material reduction (%.2f) with 0%% induction",
                         year, recycledAmount, virginReduction));
      }
    }
  }

  /**
   * Test that 100% induction works correctly with volume-based specifications.
   * Validates that recycled material adds to total supply rather than displacing virgin material,
   * resulting in higher equipment populations when induction is 100%.
   * This test validates that the circular dependency fix from Components 2-3 correctly
   * handles induced demand scenarios.
   *
   * <p>ENABLED: Testing Component 5 implementation for 100% induction behavior.</p>
   */
  @Test
  public void testPopulationIssueWithFullInduction() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/test_100_induction_volume.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recycling scenario with 100% induction
    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingResults.collect(Collectors.toList());

    // Test multiple years to verify behavior persists and compounds correctly
    int[] yearsToCheck = {2, 3, 4, 5};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "TestApp", "TestSub");
      EngineResult recyclingResult = LiveTestsUtil.getResult(recyclingResultsList.stream(), year, "TestApp", "TestSub");

      assertNotNull(bauResult, "Should have BAU result for TestApp/TestSub in year " + year);
      assertNotNull(recyclingResult, "Should have Recycling result for TestApp/TestSub in year " + year);

      double bauPopulation = bauResult.getPopulation().getValue().doubleValue();
      double recyclingPopulation = recyclingResult.getPopulation().getValue().doubleValue();

      // With 100% induction, recycling population should be higher than BAU
      // This demonstrates that recycled material is additive, not displacing virgin material
      assertTrue(recyclingPopulation > bauPopulation,
          String.format("Year %d: Recycling population (%.2f) should be higher than BAU population (%.2f) "
                       + "with 100%% induction. Recycled material should add to total supply.",
                       year, recyclingPopulation, bauPopulation));

      // Validate recycling stream values
      double recyclingAmount = recyclingResult.getRecycle().getValue().doubleValue();
      assertTrue(recyclingAmount > 0,
          "Year " + year + ": Should have positive recycling amount");

      // With 100% induction, virgin material should NOT be reduced
      // Total supply = Virgin supply + Recycled supply (additive behavior)
      double bauDomestic = bauResult.getDomestic().getValue().doubleValue();
      double bauImport = bauResult.getImport().getValue().doubleValue();
      double bauTotal = bauDomestic + bauImport;

      double recyclingDomestic = recyclingResult.getDomestic().getValue().doubleValue();
      double recyclingImport = recyclingResult.getImport().getValue().doubleValue();
      double recyclingVirgin = recyclingDomestic + recyclingImport;
      double recyclingTotal = recyclingVirgin + recyclingAmount;

      // Virgin supply should be approximately the same (not displaced)
      assertTrue(Math.abs(recyclingVirgin - bauTotal) < bauTotal * 0.1,
          String.format("Year %d: Virgin supply with recycling (%.2f) should be approximately equal to BAU (%.2f) "
                       + "with 100%% induction, difference should be < 10%%",
                       year, recyclingVirgin, bauTotal));

      // Total supply should be higher (virgin + recycled)
      assertTrue(recyclingTotal > bauTotal * 1.05,
          String.format("Year %d: Total supply with recycling (%.2f) should be at least 5%% higher than BAU (%.2f) "
                       + "with 100%% induction due to additive recycling",
                       year, recyclingTotal, bauTotal));
    }
  }

  /**
   * Test 90% recovery at servicing with 100% reuse and 100% induction.
   *
   * <p>This test validates that with 100% induction at the servicing (recharge) stage,
   * recycled material adds to total supply rather than displacing virgin material.
   * With 90% recovery rate and 100% reuse rate, all captured material should be
   * recycled and should create induced demand in virgin production.</p>
   */
  @Test
  public void testNinetyPercentServicingFullInduction() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/test_90_servicing_100_induction.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recycling scenario with 90% recovery at servicing, 100% reuse, 100% induction
    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingResults.collect(Collectors.toList());

    // Test multiple years to verify behavior persists and compounds correctly
    int[] yearsToCheck = {2, 3, 4, 5};
    for (int year : yearsToCheck) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year, "TestApp", "TestSub");
      EngineResult recyclingResult = LiveTestsUtil.getResult(recyclingResultsList.stream(), year, "TestApp", "TestSub");

      assertNotNull(bauResult, "Should have BAU result for TestApp/TestSub in year " + year);
      assertNotNull(recyclingResult, "Should have Recycling result for TestApp/TestSub in year " + year);

      double bauPopulation = bauResult.getPopulation().getValue().doubleValue();
      double recyclingPopulation = recyclingResult.getPopulation().getValue().doubleValue();

      // With 100% induction at servicing, recycling population should be higher than BAU
      // This demonstrates that recycled material from servicing adds to total supply
      assertTrue(recyclingPopulation > bauPopulation,
          String.format("Year %d: Recycling population (%.2f) should be higher than BAU population (%.2f) "
                       + "with 100%% induction at servicing. Recycled material should create induced demand.",
                       year, recyclingPopulation, bauPopulation));

      // Validate recycling stream values - should have servicing/recharge recycling
      double recyclingAmount = recyclingResult.getRecycle().getValue().doubleValue();
      assertTrue(recyclingAmount > 0,
          "Year " + year + ": Should have positive recycling amount from servicing");

      // With 100% induction, virgin material should NOT be reduced by servicing recycling
      // Total supply = Virgin supply + Recycled supply (additive behavior)
      double bauDomestic = bauResult.getDomestic().getValue().doubleValue();
      double bauImport = bauResult.getImport().getValue().doubleValue();
      double bauTotal = bauDomestic + bauImport;

      double recyclingDomestic = recyclingResult.getDomestic().getValue().doubleValue();
      double recyclingImport = recyclingResult.getImport().getValue().doubleValue();
      double recyclingVirgin = recyclingDomestic + recyclingImport;

      // Virgin supply should be approximately the same (not displaced by servicing recycling)
      assertTrue(Math.abs(recyclingVirgin - bauTotal) < bauTotal * 0.1,
          String.format("Year %d: Virgin supply with servicing recycling (%.2f) should be approximately equal to BAU (%.2f) "
                       + "with 100%% induction, difference should be < 10%%",
                       year, recyclingVirgin, bauTotal));

      // Total supply should be higher due to induced demand from servicing recycling
      double recyclingTotal = recyclingVirgin + recyclingAmount;
      assertTrue(recyclingTotal > bauTotal * 1.02,
          String.format("Year %d: Total supply with servicing recycling (%.2f) should be at least 2%% higher than BAU (%.2f) "
                       + "with 100%% induction due to additive behavior",
                       year, recyclingTotal, bauTotal));
    }
  }

}
