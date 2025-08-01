/**
 * Live tests for recycle and recover operations using actual QTA files.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
   * Test recover_displace_sales_kg.qta produces expected displacement values.
   */
  @Test
  public void testRecoverDisplaceSalesKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recover_displace_sales_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check sub_a results - should have displacement effect
    EngineResult recordSubA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_a");
    assertNotNull(recordSubA, "Should have result for test/sub_a in year 1");

    // Check that sales displacement works with uniform logic
    // Based on debug output, the actual displacement behavior distributes proportionally
    // Original: 100 kg manufacture + 50 kg import = 150 kg total
    // After displacement: the total should be reduced by the displacement amount
    double domestic = recordSubA.getDomestic().getValue().doubleValue();
    double importValue = recordSubA.getImport().getValue().doubleValue();
    double recycled = recordSubA.getRecycleConsumption().getValue().doubleValue();

    double totalSales = domestic + importValue;

    // The displacement should reduce virgin sales proportionally
    // Domestic: 100 * (130/150) = 86.67 kg
    // Import: 50 * (130/150) = 43.33 kg
    // Total: 130 kg virgin + recycled amount
    assertTrue(totalSales > 0, "Virgin sales should be positive");
    assertTrue(recycled > 0, "Recycled content should be positive");

    // Check that domestic and import are proportionally reduced
    double domesticRatio = domestic / (domestic + importValue);
    double expectedDomesticRatio = 100.0 / 150.0; // Original ratio
    assertEquals(expectedDomesticRatio, domesticRatio, 0.01,
        "Domestic ratio should be maintained after displacement");
  }

  /**
   * Test multiple recycles with additive recycling behavior.
   */
  @Test
  public void testMultipleRecycles() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/test_multiple_recycles.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run both scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    Stream<EngineResult> policyResults = KigaliSimFacade.runScenario(program, "Multiple Recycles", progress -> {});
    List<EngineResult> policyResultsList = policyResults.collect(Collectors.toList());

    // Check year 1 results
    EngineResult bauYear1 = LiveTestsUtil.getResult(bauResultsList.stream(), 1, "TestApp", "HFC-134a");
    EngineResult policyYear1 = LiveTestsUtil.getResult(policyResultsList.stream(), 1, "TestApp", "HFC-134a");

    assertNotNull(bauYear1, "Should have BAU result for year 1");
    assertNotNull(policyYear1, "Should have policy result for year 1");

    // Multiple recycles should provide more recycled material than single recycle
    // Recovery rates: 30% + 20% = 50%
    // Yield rates: weighted average of 80% and 90% = (30*80 + 20*90)/(30+20) = 84%
    double bauImports = bauYear1.getImport().getValue().doubleValue();
    double policyImports = policyYear1.getImport().getValue().doubleValue();
    double policyRecycled = policyYear1.getRecycleConsumption().getValue().doubleValue();

    // With additive recycling, policy should have lower imports and higher recycled content
    assertTrue(policyImports < bauImports,
        String.format("Policy imports (%.2f) should be less than BAU imports (%.2f)",
                      policyImports, bauImports));
    assertTrue(policyRecycled > 0,
        String.format("Policy should have recycled content (%.2f)", policyRecycled));
  }

  /**
   * Test recover_displace_substance.qta produces expected displacement values.
   */
  @Test
  public void testRecoverDisplaceSubstance() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recover_displace_substance.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Expect UnsupportedOperationException when using substance displacement
    assertThrows(UnsupportedOperationException.class, () -> {
      String scenarioName = "result";
      Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
      results.collect(Collectors.toList()); // Force evaluation
    }, "Should throw exception for substance displacement in recycling");
  }

  /**
   * Test recover_displace_import_kg.qta produces expected import displacement values.
   */
  @Test
  public void testRecoverDisplaceImportKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recover_displace_import_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Expect UnsupportedOperationException when using import displacement
    assertThrows(UnsupportedOperationException.class, () -> {
      String scenarioName = "result";
      Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
      results.collect(Collectors.toList()); // Force evaluation
    }, "Should throw exception for import displacement in recycling");
  }

  /**
   * Test recover_displace_domestic_kg.qta produces expected domestic displacement values.
   */
  @Test
  public void testRecoverDisplaceDomesticKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recover_displace_domestic_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Expect UnsupportedOperationException when using domestic displacement
    assertThrows(UnsupportedOperationException.class, () -> {
      String scenarioName = "result";
      Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
      results.collect(Collectors.toList()); // Force evaluation
    }, "Should throw exception for domestic displacement in recycling");
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
        "GHG consumption should be 500 tCO2e in year 1 (100 kg * 5 tCO2e/kg)");
    assertEquals("tCO2e", recordYear1.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 1");

    // Check year 2 - recycling at EOL active
    EngineResult recordYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(recordYear2, "Should have result for test/test in year 2");

    // With recycling at EOL, retired material from year 1 should be recycled in year 2
    // Year 1 retired: 100 kg * 10% = 10 kg
    // Recovered: 10 kg * 50% = 5 kg
    // Virgin material needed: 100 kg - 5 kg = 95 kg
    // Total GHG: 95 kg * 5 tCO2e/kg = 475 tCO2e
    double expectedTotalConsumption = 475.0;
    assertEquals(expectedTotalConsumption, recordYear2.getConsumptionNoRecycle().getValue().doubleValue(), 0.0001,
        "Virgin material consumption should be reduced to 475.0 tCO2e in year 2 due to recycling at EOL");
    assertEquals("tCO2e", recordYear2.getConsumptionNoRecycle().getUnits(),
        "Virgin material consumption units should be tCO2e in year 2");

    // Check recycled consumption in year 2
    // Recycled content: 5 kg * 5 tCO2e/kg = 25 tCO2e
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
        "GHG consumption should be 500 tCO2e in year 1 (100 kg * 5 tCO2e/kg)");
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
    System.out.println("=== STARTING COMBINED SCENARIO (Sales Permit → Domestic Recycling) ===");
    Stream<EngineResult> combinedResults = KigaliSimFacade.runScenario(program, "Combined", progress -> {});
    List<EngineResult> combinedResultsList = combinedResults.collect(Collectors.toList());
    System.out.println("=== COMPLETED COMBINED SCENARIO ===");

    // Run Combined Reverse scenario (Domestic Recycling then Sales Permit)
    System.out.println("=== STARTING COMBINED REVERSE SCENARIO (Domestic Recycling → Sales Permit) ===");
    Stream<EngineResult> combinedReverseResults = KigaliSimFacade.runScenario(program, "Combined Reverse", progress -> {});
    List<EngineResult> combinedReverseResultsList = combinedReverseResults.collect(Collectors.toList());
    System.out.println("=== COMPLETED COMBINED REVERSE SCENARIO ===");

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

    System.out.printf("=== FINAL RESULTS WITH R-600a RECYCLING ===\n");
    System.out.printf("Combined: %.2f kg, Combined Reverse: %.2f kg, Difference: %.2f%%\n",
        combinedTotal, combinedReverseTotal, percentageDifference);

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

}
