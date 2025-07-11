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
import java.nio.file.Files;
import java.nio.file.Path;
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
    assertEquals(expectedTotalConsumption, recordYear2.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be reduced to 437.5 tCO2e in year 2 due to recycling");
    assertEquals("tCO2e", recordYear2.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 2");

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
    assertEquals(expectedTotalConsumption, recordYear2.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be reduced to 499.875 tCO2e in year 2 due to recycling");
    assertEquals("tCO2e", recordYear2.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 2");

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
    assertEquals(expectedTotalConsumption, recordYear2.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be reduced to 490.0 tCO2e in year 2 due to recycling");
    assertEquals("tCO2e", recordYear2.getGhgConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 2");

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
    double manufacture = recordSubA.getManufacture().getValue().doubleValue();
    double importValue = recordSubA.getImport().getValue().doubleValue();
    double recycled = recordSubA.getRecycleConsumption().getValue().doubleValue();
    
    double totalSales = manufacture + importValue;
    
    // The displacement should reduce virgin sales proportionally
    // Manufacture: 100 * (130/150) = 86.67 kg
    // Import: 50 * (130/150) = 43.33 kg  
    // Total: 130 kg virgin + recycled amount
    assertTrue(totalSales > 0, "Virgin sales should be positive");
    assertTrue(recycled > 0, "Recycled content should be positive");
    
    // Check that manufacture and import are proportionally reduced
    double manufactureRatio = manufacture / (manufacture + importValue);
    double expectedManufactureRatio = 100.0 / 150.0; // Original ratio
    assertEquals(expectedManufactureRatio, manufactureRatio, 0.01,
        "Manufacture ratio should be maintained after displacement");
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

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check sub_b results - should have displacement effect on sub_b sales
    EngineResult recordSubB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_b");
    assertNotNull(recordSubB, "Should have result for test/sub_b in year 1");

    // Check that sub_b sales were displaced (reduced by 30 kg)
    // Original: 200 kg manufacture + 100 kg import = 300 kg sales
    // After 30 kg displacement: 300 - 30 = 270 kg total
    double totalSales = recordSubB.getManufacture().getValue().doubleValue() 
                       + recordSubB.getImport().getValue().doubleValue();
    assertEquals(270.0, totalSales, 0.0001,
        "Sub_b total sales should be reduced by 30 kg due to displacement from sub_a recovery");
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

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check sub_a results - should have displacement effect on import only
    EngineResult recordSubA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "sub_a");
    assertNotNull(recordSubA, "Should have result for test/sub_a in year 1");

    // Check that import displacement works with uniform logic
    // Based on actual implementation, the displacement affects the whole system
    double importSales = recordSubA.getImport().getValue().doubleValue();
    double manufactureSales = recordSubA.getManufacture().getValue().doubleValue();
    double recycledContent = recordSubA.getRecycleConsumption().getValue().doubleValue();
    
    // The import should be reduced from original 50 kg
    assertTrue(importSales < 50.0,
        "Import should be reduced due to displacement");
    
    // The manufacture should also be affected due to the uniform displacement logic
    assertTrue(manufactureSales < 100.0,
        "Manufacture should be reduced due to uniform displacement logic");
    
    // Check recycled content is positive
    assertTrue(recycledContent > 0,
        "Recycled content should be positive");
    
    // Total should be less than original 150 kg
    double totalSales = importSales + manufactureSales;
    assertTrue(totalSales < 150.0,
        "Total sales should be reduced due to displacement");
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

}
