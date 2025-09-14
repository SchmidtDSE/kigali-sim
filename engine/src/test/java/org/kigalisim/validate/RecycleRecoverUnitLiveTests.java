/**
 * Live tests for recycle and recover operations using units-based specifications.
 *
 * <p>This file contains tests that use units-based specifications (recover X units) where
 * recycling behavior is tied to equipment unit counts rather than substance volume/mass.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Tests that validate recycle and recover operations in QTA files using units-based specifications.
 */
public class RecycleRecoverUnitLiveTests {

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
    assertEquals(500.0, recordYear1.getConsumption().getValue().doubleValue(), 0.0001,
        "GHG consumption should be 500 tCO2e in year 1");
    assertEquals("tCO2e", recordYear1.getConsumption().getUnits(),
        "GHG consumption units should be tCO2e in year 1");

    // Check year 2 - recycling active
    EngineResult recordYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(recordYear2, "Should have result for test/test in year 2");

    // With recycling, virgin material should be reduced
    double expectedTotalConsumption = 490.0; // Reduced due to recycling displacing virgin material
    assertEquals(expectedTotalConsumption, recordYear2.getConsumption().getValue().doubleValue(), 0.0001,
        "Virgin material consumption should be reduced to 490.0 tCO2e in year 2 due to recycling");
    assertEquals("tCO2e", recordYear2.getConsumption().getUnits(),
        "Virgin material consumption units should be tCO2e in year 2");

    // Check recycled consumption in year 2
    // 1000 units * 2 kg/unit = 2000 kg, 2000 kg * 5 tCO2e/(1000 kg) = 10 tCO2e
    assertEquals(10.0, recordYear2.getRecycleConsumption().getValue().doubleValue(), 0.0001,
        "Recycled consumption should be 10 tCO2e in year 2");
    assertEquals("tCO2e", recordYear2.getRecycleConsumption().getUnits(),
        "Recycled consumption units should be tCO2e in year 2");
  }

  /**
   * Test 0% induction units-based recycling scenario.
   * Validates that BAU and Recycling scenarios have identical equipment populations
   * when induction rate is 0%, confirming proper displacement behavior for units-based specs.
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
   * Test 0% induction units-based recycling scenario with recalc idempotence.
   * Validates that multiple policy changes maintain population equality and that
   * recalculation operations are idempotent for units-based specifications.
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
   * Test units-based full induction (100%).
   * This test confirms that units-based specifications with 100% induction
   * create induced demand on top of baseline consumption.
   */
  @Test
  public void testUnitsBasedFullInduction() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/test_100_induction_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Recycling scenario with units-based 100% induction
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

      // With units-based specs and 100% induction, recycling population should be higher than BAU
      // This demonstrates that recycled material adds to total supply (induced demand behavior)
      assertTrue(recyclingPopulation > bauPopulation,
          String.format("Year %d: Units-based recycling population (%.2f) should be higher than BAU population (%.2f) "
                       + "with 100%% induction. Recycled material should create induced demand.",
                       year, recyclingPopulation, bauPopulation));

      // Validate recycling stream values
      double recyclingAmount = recyclingResult.getRecycle().getValue().doubleValue();
      assertTrue(recyclingAmount > 0,
          "Year " + year + ": Should have positive recycling amount");

      // With 100% induction, total supply should be higher than baseline
      double bauDomestic = bauResult.getDomestic().getValue().doubleValue();
      double bauImport = bauResult.getImport().getValue().doubleValue();
      double bauTotal = bauDomestic + bauImport;

      double recyclingDomestic = recyclingResult.getDomestic().getValue().doubleValue();
      double recyclingImport = recyclingResult.getImport().getValue().doubleValue();
      double recyclingVirgin = recyclingDomestic + recyclingImport;
      double recyclingTotal = recyclingVirgin + recyclingAmount;

      // For units-based specs with 100% induction, total supply should be higher
      assertTrue(recyclingTotal > bauTotal,
          String.format("Year %d: Total supply with units-based recycling (%.2f) should be higher than BAU (%.2f) "
                       + "with 100%% induction due to induced demand being added on top",
                       year, recyclingTotal, bauTotal));
    }
  }

  /**
   * Test recharge with recycling interaction for units-based imports.
   * This test verifies that the existing behavior is preserved when using units-based import policies.
   */
  @Test
  public void testRechargeWithRecyclingUnitsBasedImports() throws IOException {
    // Use the test example we created
    String examplePath = "../examples/test_recharge_recycle_units_bug.qta";
    File exampleFile = new File(examplePath);
    assertTrue(exampleFile.exists(), "Example file should exist: " + examplePath);
    // Parse and interpret the example file
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(exampleFile.getPath());
    assertNotNull(program, "Program should not be null");
    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(java.util.stream.Collectors.toList());
    // Run policy scenario
    Stream<EngineResult> policyResults = KigaliSimFacade.runScenario(program, "With Recycling", progress -> {});
    List<EngineResult> policyResultsList = policyResults.collect(java.util.stream.Collectors.toList());
    // Find year 3 results for both scenarios
    EngineResult bauYear3 = bauResultsList.stream()
        .filter(r -> r.getYear() == 3 && r.getScenarioName().equals("BAU"))
        .findFirst()
        .orElse(null);
    EngineResult policyYear3 = policyResultsList.stream()
        .filter(r -> r.getYear() == 3 && r.getScenarioName().equals("With Recycling"))
        .findFirst()
        .orElse(null);
    assertNotNull(bauYear3, "BAU year 3 result should exist");
    assertNotNull(policyYear3, "Policy year 3 result should exist");
    // Find year 5 results for both scenarios to check equipment
    EngineResult bauYear5 = bauResultsList.stream()
        .filter(r -> r.getYear() == 5 && r.getScenarioName().equals("BAU"))
        .findFirst()
        .orElse(null);
    EngineResult policyYear5 = policyResultsList.stream()
        .filter(r -> r.getYear() == 5 && r.getScenarioName().equals("With Recycling"))
        .findFirst()
        .orElse(null);
    assertNotNull(bauYear5, "BAU year 5 result should exist");
    assertNotNull(policyYear5, "Policy year 5 result should exist");
    // Get import values
    double bauImports = bauYear3.getImport().getValue().doubleValue();
    double policyImports = policyYear3.getImport().getValue().doubleValue();
    // With 50% recovery and 90% reuse, imports should be lower with the policy
    assertTrue(policyImports < bauImports,
        String.format("Imports with recycling policy (%.2f) should be less than BAU (%.2f)",
                      policyImports, bauImports));
    // Also check consumption
    double bauConsumption = bauYear3.getImportConsumption().getValue().doubleValue();
    double policyConsumption = policyYear3.getImportConsumption().getValue().doubleValue();
    // Check equipment (population) at year 5 - policy should be lower due to retirement
    double bauEquipmentYear5 = bauYear5.getPopulation().getValue().doubleValue();
    double policyEquipmentYear5 = policyYear5.getPopulation().getValue().doubleValue();
    // Policy includes 10% retirement per year, so population should be lower
    assertTrue(policyEquipmentYear5 < bauEquipmentYear5,
        String.format("Policy equipment population (%.2f) should be lower than BAU (%.2f) due to retirement",
                      policyEquipmentYear5, bauEquipmentYear5));
  }
}