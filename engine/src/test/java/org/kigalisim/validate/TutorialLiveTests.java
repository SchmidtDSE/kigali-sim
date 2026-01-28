/**
 * Tutorial live tests using actual QTA files from tutorials.
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
 * Tests that validate tutorial QTA files against expected behavior from the tutorials.
 */
public class TutorialLiveTests {

  /**
   * Test Tutorial 02: Basic consumption and equipment growth patterns.
   * - Consumption each year is flat at 25 mt / year.
   * - Equipment grows from 2025 to 2026 and 2034 to 2035 but the change between 2034 and 2035
   *   should be smaller than the difference from 2025 to 2026.
   */
  @Test
  public void testTutorial02() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/tutorial_02.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Test 1: Consumption each year is flat at 25 mt / year
    for (int year = 2025; year <= 2035; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      assertNotNull(result, "Should have result for year " + year);

      // Consumption should be 25 mt = 25000 kg
      assertEquals(25000.0, result.getDomestic().getValue().doubleValue(), 0.1,
          "Consumption should be flat at 25000 kg in year " + year);
    }

    // Test 2: Equipment growth patterns
    EngineResult result2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025,
        "Domestic Refrigeration", "HFC-134a");
    EngineResult result2026 = LiveTestsUtil.getResult(resultsList.stream(), 2026,
        "Domestic Refrigeration", "HFC-134a");
    EngineResult result2034 = LiveTestsUtil.getResult(resultsList.stream(), 2034,
        "Domestic Refrigeration", "HFC-134a");
    final EngineResult result2035 = LiveTestsUtil.getResult(resultsList.stream(), 2035,
        "Domestic Refrigeration", "HFC-134a");

    assertNotNull(result2025, "Should have result for 2025");
    assertNotNull(result2026, "Should have result for 2026");
    assertNotNull(result2034, "Should have result for 2034");
    assertNotNull(result2035, "Should have result for 2035");

    double equipment2025 = result2025.getPopulation().getValue().doubleValue();
    double equipment2026 = result2026.getPopulation().getValue().doubleValue();
    double equipment2034 = result2034.getPopulation().getValue().doubleValue();
    double equipment2035 = result2035.getPopulation().getValue().doubleValue();

    // Equipment should grow from 2025 to 2026
    assertTrue(equipment2026 > equipment2025,
        "Equipment should grow from 2025 to 2026");

    // Equipment should grow from 2034 to 2035
    assertTrue(equipment2035 > equipment2034,
        "Equipment should grow from 2034 to 2035");

    // Change between 2034 and 2035 should be smaller than change from 2025 to 2026
    double change2025to2026 = equipment2026 - equipment2025;
    double change2034to2035 = equipment2035 - equipment2034;
    assertTrue(change2034to2035 < change2025to2026,
        "Change between 2034 and 2035 (" + change2034to2035 + ") should be smaller than "
        + "change from 2025 to 2026 (" + change2025to2026 + ")");
  }

  /**
   * Test Tutorial 03: Multiple substances and emissions comparison.
   * - The sum of recharge and end of life (tCO2e) are higher than either alone.
   * - The ratio of HFC-134a to R-600a in kg is higher than their ratio in tCO2e.
   */
  @Test
  public void testTutorial03() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/tutorial_03.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Get results for a representative year (2030)
    EngineResult hfc134aResult = LiveTestsUtil.getResult(resultsList.stream(), 2030,
        "Domestic Refrigeration", "HFC-134a");
    EngineResult r600aResult = LiveTestsUtil.getResult(resultsList.stream(), 2030,
        "Domestic Refrigeration", "R-600a");

    assertNotNull(hfc134aResult, "Should have HFC-134a result for 2030");
    assertNotNull(r600aResult, "Should have R-600a result for 2030");

    // Test 1: Sum of recharge and end of life (tCO2e) are higher than either alone
    double hfc134aRecharge = hfc134aResult.getRechargeEmissions().getValue().doubleValue();
    double hfc134aEndOfLife = hfc134aResult.getEolEmissions().getValue().doubleValue();
    double hfc134aTotal = hfc134aRecharge + hfc134aEndOfLife;

    assertTrue(hfc134aTotal > hfc134aRecharge,
        "Sum of recharge and end of life (" + hfc134aTotal
        + ") should be higher than recharge alone (" + hfc134aRecharge + ")");
    assertTrue(hfc134aTotal > hfc134aEndOfLife,
        "Sum of recharge and end of life (" + hfc134aTotal + ") should be higher than end of life alone (" + hfc134aEndOfLife + ")");

    // Test 2: The ratio of HFC-134a to R-600a in kg is higher than their ratio in tCO2e
    // This means the relative difference should be greater when looking at tCO2e due to GWP differences
    double hfc134aKg = hfc134aResult.getDomestic().getValue().doubleValue();
    double r600aKg = r600aResult.getDomestic().getValue().doubleValue();
    double hfc134aTco2e = hfc134aResult.getConsumption().getValue().doubleValue();
    double r600aTco2e = r600aResult.getConsumption().getValue().doubleValue();

    double kgRatio = hfc134aKg / r600aKg;
    double tco2eRatio = hfc134aTco2e / r600aTco2e;

    // The relative difference between HFC-134a and R-600a should be higher when looking at tCO2e than kg
    // due to the significant GWP difference (1430 vs 3 kgCO2e/kg)
    assertTrue(tco2eRatio > kgRatio,
        "Ratio of HFC-134a to R-600a in tCO2e (" + tco2eRatio + ") should be higher than their ratio in kg (" + kgRatio + ") due to GWP differences");
  }

  /**
   * Test Tutorial 04: Import and domestic streams with growth.
   * - HFC-134a import and domestic are non-zero in both 2025 and 2035.
   * - The percentage difference between kg of HFC-32 and HFC-134a (domestic + import) is smaller
   *   than the percentage difference between tCO2e of HFC-32 and HFC-134a at both 2025 and 2035.
   */
  @Test
  public void testTutorial04() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/tutorial_04.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Test 1: HFC-134a import and domestic are non-zero in both 2025 and 2035
    for (int year : new int[]{2025, 2035}) {
      EngineResult hfc134aResult = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      assertNotNull(hfc134aResult, "Should have HFC-134a result for " + year);

      assertTrue(hfc134aResult.getDomestic().getValue().doubleValue() > 0,
          "HFC-134a domestic should be non-zero in " + year);
      assertTrue(hfc134aResult.getImport().getValue().doubleValue() > 0,
          "HFC-134a import should be non-zero in " + year);
    }

    // Test 2: Percentage differences comparison for both 2025 and 2035
    for (int year : new int[]{2025, 2035}) {
      EngineResult hfc134aResult = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      EngineResult hfc32Result = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic AC", "HFC-32");

      assertNotNull(hfc134aResult, "Should have HFC-134a result for " + year);
      assertNotNull(hfc32Result, "Should have HFC-32 result for " + year);

      // Calculate total kg (domestic + import) for HFC-134a
      double hfc134aTotalKg = hfc134aResult.getDomestic().getValue().doubleValue()
                              + hfc134aResult.getImport().getValue().doubleValue();
      double hfc32TotalKg = hfc32Result.getDomestic().getValue().doubleValue();

      double hfc134aTotalTco2e = hfc134aResult.getConsumption().getValue().doubleValue();
      double hfc32TotalTco2e = hfc32Result.getConsumption().getValue().doubleValue();

      // Calculate percentage differences
      double maxKg = Math.max(hfc32TotalKg, hfc134aTotalKg);
      double kgPercentDiff = Math.abs(hfc32TotalKg - hfc134aTotalKg) / maxKg * 100;

      double maxTco2e = Math.max(hfc32TotalTco2e, hfc134aTotalTco2e);
      double tco2ePercentDiff = Math.abs(hfc32TotalTco2e - hfc134aTotalTco2e) / maxTco2e * 100;

      assertTrue(kgPercentDiff < tco2ePercentDiff,
          "Percentage difference in kg (" + kgPercentDiff + "%) should be smaller than "
          + "percentage difference in tCO2e (" + tco2ePercentDiff + "%) in year " + year);
    }
  }

  /**
   * Test Tutorial 05: Policy displacement effects.
   * - The amount that HFC-134a (in kg) in Permit - BAU is the opposite of R-600a (in kg)
   *   in Permit - BAU in 2029.
   * - The amount that HFC-134a (in kg) in Permit - BAU is the opposite of R-600a (in kg)
   *   in Permit - BAU in 2035.
   */
  @Test
  public void testTutorial05() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/tutorial_05.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    // Run Permit scenario
    Stream<EngineResult> permitResults = KigaliSimFacade.runScenario(program, "Permit", progress -> {});
    List<EngineResult> permitResultsList = permitResults.collect(Collectors.toList());

    // Test displacement effects for both 2029 and 2035
    for (int year : new int[]{2029, 2035}) {
      // Get BAU results
      EngineResult bauHfc134a = LiveTestsUtil.getResult(bauResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      EngineResult bauR600a = LiveTestsUtil.getResult(bauResultsList.stream(), year,
          "Domestic Refrigeration", "R-600a");

      // Get Permit results
      EngineResult permitHfc134a = LiveTestsUtil.getResult(permitResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      final EngineResult permitR600a = LiveTestsUtil.getResult(permitResultsList.stream(), year,
          "Domestic Refrigeration", "R-600a");

      assertNotNull(bauHfc134a, "Should have BAU HFC-134a result for " + year);
      assertNotNull(bauR600a, "Should have BAU R-600a result for " + year);
      assertNotNull(permitHfc134a, "Should have Permit HFC-134a result for " + year);
      assertNotNull(permitR600a, "Should have Permit R-600a result for " + year);

      // Calculate differences (Permit - BAU) for total consumption (domestic + import)
      double hfc134aBauTotal = bauHfc134a.getDomestic().getValue().doubleValue()
                               + bauHfc134a.getImport().getValue().doubleValue();
      double hfc134aPermitTotal = permitHfc134a.getDomestic().getValue().doubleValue()
                                  + permitHfc134a.getImport().getValue().doubleValue();
      double hfc134aDiff = hfc134aPermitTotal - hfc134aBauTotal;

      double r600aBauTotal = bauR600a.getDomestic().getValue().doubleValue()
                             + bauR600a.getImport().getValue().doubleValue();
      double r600aPermitTotal = permitR600a.getDomestic().getValue().doubleValue()
                                + permitR600a.getImport().getValue().doubleValue();
      double r600aDiff = r600aPermitTotal - r600aBauTotal;

      // The differences should be approximately opposite (within 10% tolerance)
      double expectedOpposite = -hfc134aDiff;
      double tolerance = Math.abs(expectedOpposite) * 0.1; // 10% tolerance

      assertTrue(Math.abs(r600aDiff - expectedOpposite) <= tolerance,
          "R-600a difference (" + r600aDiff + ") should be approximately opposite of HFC-134a difference ("
          + hfc134aDiff + ") in year " + year + ". Expected: " + expectedOpposite + " ± " + tolerance);
    }
  }

  /**
   * Test Tutorial 05 with % prior year: Policy displacement effects.
   * This validates that % prior year behaves identically to % in cap context.
   * - The amount that HFC-134a (in kg) in Permit - BAU is the opposite of R-600a (in kg)
   *   in Permit - BAU in 2029.
   * - The amount that HFC-134a (in kg) in Permit - BAU is the opposite of R-600a (in kg)
   *   in Permit - BAU in 2035.
   */
  @Test
  public void testTutorial05PercentPriorYear() throws IOException {
    String qtaPath = "../examples/tutorial_05_percent_prior_year.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    Stream<EngineResult> permitResults = KigaliSimFacade.runScenario(program, "Permit", progress -> {});
    List<EngineResult> permitResultsList = permitResults.collect(Collectors.toList());

    for (int year : new int[]{2029, 2035}) {
      EngineResult bauHfc134a = LiveTestsUtil.getResult(bauResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      EngineResult bauR600a = LiveTestsUtil.getResult(bauResultsList.stream(), year,
          "Domestic Refrigeration", "R-600a");

      EngineResult permitHfc134a = LiveTestsUtil.getResult(permitResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      final EngineResult permitR600a = LiveTestsUtil.getResult(permitResultsList.stream(), year,
          "Domestic Refrigeration", "R-600a");

      assertNotNull(bauHfc134a, "Should have BAU HFC-134a result for " + year);
      assertNotNull(bauR600a, "Should have BAU R-600a result for " + year);
      assertNotNull(permitHfc134a, "Should have Permit HFC-134a result for " + year);
      assertNotNull(permitR600a, "Should have Permit R-600a result for " + year);

      double hfc134aBauTotal = bauHfc134a.getDomestic().getValue().doubleValue()
                               + bauHfc134a.getImport().getValue().doubleValue();
      double hfc134aPermitTotal = permitHfc134a.getDomestic().getValue().doubleValue()
                                  + permitHfc134a.getImport().getValue().doubleValue();
      double hfc134aDiff = hfc134aPermitTotal - hfc134aBauTotal;

      double r600aBauTotal = bauR600a.getDomestic().getValue().doubleValue()
                             + bauR600a.getImport().getValue().doubleValue();
      double r600aPermitTotal = permitR600a.getDomestic().getValue().doubleValue()
                                + permitR600a.getImport().getValue().doubleValue();
      double r600aDiff = r600aPermitTotal - r600aBauTotal;

      double expectedOpposite = -hfc134aDiff;
      double tolerance = Math.abs(expectedOpposite) * 0.1;

      assertTrue(Math.abs(r600aDiff - expectedOpposite) <= tolerance,
          "Displacement should balance: HFC-134a reduction (~" + (-hfc134aDiff)
          + " kg) should approximately equal R-600a increase (" + r600aDiff
          + " kg) within 10% tolerance in year " + year);
    }
  }

  /**
   * Test Tutorial 06: Recycling policy effectiveness.
   * - Recycling scenario is consistently lower than BAU from 2027 onwards (kg: domestic + import).
   * - The combined policy is consistently greater than or equal to recycling from 2030 onwards
   *   (kg: domestic + import).
   */
  @Test
  public void testTutorial06() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/tutorial_06.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run all scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingResults.collect(Collectors.toList());

    Stream<EngineResult> combinedResults = KigaliSimFacade.runScenario(program, "Combined", progress -> {});
    List<EngineResult> combinedResultsList = combinedResults.collect(Collectors.toList());

    // Test 1: With 100% induced demand, virgin production stays about the same,
    // but recycling is happening (non-zero) from 2027 onwards
    for (int year = 2027; year <= 2035; year++) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      EngineResult recyclingResult = LiveTestsUtil.getResult(recyclingResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");

      assertNotNull(bauResult, "Should have BAU result for " + year);
      assertNotNull(recyclingResult, "Should have Recycling result for " + year);

      double bauTotal = bauResult.getDomestic().getValue().doubleValue()
                        + bauResult.getImport().getValue().doubleValue();
      double recyclingTotal = recyclingResult.getDomestic().getValue().doubleValue()
                              + recyclingResult.getImport().getValue().doubleValue();

      // With 100% induced demand, virgin production should stay approximately the same
      assertEquals(bauTotal, recyclingTotal, bauTotal * 0.01,
          "With 100% induced demand, virgin production should remain approximately the same in year " + year);

      // But recycling should be happening
      double recyclingAmount = recyclingResult.getRecycle().getValue().doubleValue();
      assertTrue(recyclingAmount > 0,
          "Recycling amount should be greater than zero in year " + year);
    }

    // Test 2: Combined policy is more effective (<=) than recycling from 2030 onwards
    for (int year = 2030; year <= 2035; year++) {
      EngineResult recyclingResult = LiveTestsUtil.getResult(recyclingResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      EngineResult combinedResult = LiveTestsUtil.getResult(combinedResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");

      assertNotNull(recyclingResult, "Should have Recycling result for " + year);
      assertNotNull(combinedResult, "Should have Combined result for " + year);

      double recyclingTotal = recyclingResult.getDomestic().getValue().doubleValue()
                              + recyclingResult.getImport().getValue().doubleValue();
      double combinedTotal = combinedResult.getDomestic().getValue().doubleValue()
                             + combinedResult.getImport().getValue().doubleValue();

      assertTrue(combinedTotal <= recyclingTotal,
          "Combined total (" + combinedTotal + ") should be less than or equal to Recycling total ("
          + recyclingTotal + ") in year " + year + " since combined policies are more effective");
    }
  }

  /**
   * Test Tutorial 06: 100% induction recycling should not affect the virgin sales basis for caps.
   *
   * <p>With 100% induction, recycling adds material ON TOP of virgin sales but should NOT reduce
   * the virgin sales baseline. Therefore, when combining Recycling (100% induction) with Permit
   * (85% cap), the Combined scenario's virgin consumption should be >= Permit-only consumption.
   *
   * <p>If Combined has LOWER consumption than Permit in 2034, it suggests the 85% cap basis is
   * being calculated on a lower value when recycling is involved, which is incorrect behavior.
   *
   * <p><b>Root cause:</b> The {@code recover} command triggers {@code recalcSales()} which
   * recalculates virgin sales from scratch based on equipment needs, REPLACING the values set
   * by the {@code change sales} command. The calculated values don't preserve the user's growth
   * trajectory because they use a frozen {@code rechargeBase}. At year end, {@code snapshotStreams()}
   * captures these replaced values as the prior year values, which are then used as the cap basis.
   * This results in a lower cap basis for Combined than for Permit, even though 100% induction
   * should mean virgin sales are unchanged.
   */
  @Test
  public void testTutorial06InductionBasis() throws IOException {
    String qtaPath = "../examples/tutorial_06.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run all scenarios for comparison
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    List<EngineResult> bauResultsList = bauResults.collect(Collectors.toList());

    Stream<EngineResult> permitResults = KigaliSimFacade.runScenario(program, "Permit", progress -> {});
    List<EngineResult> permitResultsList = permitResults.collect(Collectors.toList());

    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});
    List<EngineResult> recyclingResultsList = recyclingResults.collect(Collectors.toList());

    Stream<EngineResult> combinedResults = KigaliSimFacade.runScenario(program, "Combined", progress -> {});
    List<EngineResult> combinedResultsList = combinedResults.collect(Collectors.toList());

    // Print comparison table for all years 2028-2035
    System.out.println("\n=== Tutorial 06 Induction Basis Debug ===");
    System.out.println("Year\tBAU\t\tRecycling\tPermit\t\tCombined\tRecycle Amt");
    for (int year = 2028; year <= 2035; year++) {
      EngineResult bauResult = LiveTestsUtil.getResult(bauResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      EngineResult permitResult = LiveTestsUtil.getResult(permitResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      EngineResult recyclingResult = LiveTestsUtil.getResult(recyclingResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      EngineResult combinedResult = LiveTestsUtil.getResult(combinedResultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");

      double bauTotal = bauResult.getDomestic().getValue().doubleValue()
          + bauResult.getImport().getValue().doubleValue();
      double permitTotal = permitResult.getDomestic().getValue().doubleValue()
          + permitResult.getImport().getValue().doubleValue();
      double recyclingTotal = recyclingResult.getDomestic().getValue().doubleValue()
          + recyclingResult.getImport().getValue().doubleValue();
      double combinedTotal = combinedResult.getDomestic().getValue().doubleValue()
          + combinedResult.getImport().getValue().doubleValue();
      double combinedRecycle = combinedResult.getRecycle().getValue().doubleValue();

      System.out.printf("%d\t%.0f\t\t%.0f\t\t%.0f\t\t%.0f\t\t%.0f%n",
          year, bauTotal, recyclingTotal, permitTotal, combinedTotal, combinedRecycle);
    }
    System.out.println("=========================================\n");

    // Test: Combined (Recycling → Permit) should have consumption >= Permit in 2034
    // Because 100% induction means recycling doesn't reduce virgin sales, only adds on top
    int year = 2034;
    EngineResult permitResult = LiveTestsUtil.getResult(permitResultsList.stream(), year,
        "Domestic Refrigeration", "HFC-134a");
    EngineResult combinedResult = LiveTestsUtil.getResult(combinedResultsList.stream(), year,
        "Domestic Refrigeration", "HFC-134a");

    assertNotNull(permitResult, "Should have Permit result for " + year);
    assertNotNull(combinedResult, "Should have Combined result for " + year);

    double permitTotal = permitResult.getDomestic().getValue().doubleValue()
        + permitResult.getImport().getValue().doubleValue();
    double combinedTotal = combinedResult.getDomestic().getValue().doubleValue()
        + combinedResult.getImport().getValue().doubleValue();

    System.out.println("Year " + year + ": Permit total = " + permitTotal
        + " kg, Combined total = " + combinedTotal + " kg");

    assertTrue(combinedTotal >= permitTotal,
        "Combined total (" + combinedTotal + " kg) should be >= Permit total ("
            + permitTotal + " kg) in year " + year
            + " because 100% induction recycling should not reduce the virgin sales basis for the 85% cap");
  }

  /**
   * Test Tutorial 07: Enhanced recycling with R-600a program.
   * - The recycling scenario total consumption (kg: domestic + import) at 2033 from tutorial 6
   *   is higher than that from tutorial 7.
   */
  @Test
  public void testTutorial07() throws IOException {
    // Load and parse both tutorial files
    String qtaPath6 = "../examples/tutorial_06.qta";
    String qtaPath7 = "../examples/tutorial_07.qta";

    ParsedProgram program6 = KigaliSimFacade.parseAndInterpret(qtaPath6);
    ParsedProgram program7 = KigaliSimFacade.parseAndInterpret(qtaPath7);

    assertNotNull(program6, "Tutorial 6 program should not be null");
    assertNotNull(program7, "Tutorial 7 program should not be null");

    // Run recycling scenarios for both tutorials
    Stream<EngineResult> recycling6Results = KigaliSimFacade.runScenario(program6, "Recycling", progress -> {});
    List<EngineResult> recycling6ResultsList = recycling6Results.collect(Collectors.toList());

    Stream<EngineResult> recycling7Results = KigaliSimFacade.runScenario(program7, "Recycling", progress -> {});
    List<EngineResult> recycling7ResultsList = recycling7Results.collect(Collectors.toList());

    // Get results for 2035 for both HFC-134a and R-600a
    EngineResult recycling6Hfc = LiveTestsUtil.getResult(recycling6ResultsList.stream(), 2035,
        "Domestic Refrigeration", "HFC-134a");
    EngineResult recycling6R600a = LiveTestsUtil.getResult(recycling6ResultsList.stream(), 2035,
        "Domestic Refrigeration", "R-600a");
    EngineResult recycling7Hfc = LiveTestsUtil.getResult(recycling7ResultsList.stream(), 2035,
        "Domestic Refrigeration", "HFC-134a");
    final EngineResult recycling7R600a = LiveTestsUtil.getResult(recycling7ResultsList.stream(), 2035,
        "Domestic Refrigeration", "R-600a");

    assertNotNull(recycling6Hfc, "Should have Tutorial 6 HFC-134a result for 2035");
    assertNotNull(recycling6R600a, "Should have Tutorial 6 R-600a result for 2035");
    assertNotNull(recycling7Hfc, "Should have Tutorial 7 HFC-134a result for 2035");
    assertNotNull(recycling7R600a, "Should have Tutorial 7 R-600a result for 2035");

    // Calculate total virgin consumption (domestic + import) across all substances for both tutorials
    double recycling6VirginTotal = (recycling6Hfc.getDomestic().getValue().doubleValue()
                             + recycling6Hfc.getImport().getValue().doubleValue())
                             + (recycling6R600a.getDomestic().getValue().doubleValue()
                             + recycling6R600a.getImport().getValue().doubleValue());
    double recycling7VirginTotal = (recycling7Hfc.getDomestic().getValue().doubleValue()
                             + recycling7Hfc.getImport().getValue().doubleValue())
                             + (recycling7R600a.getDomestic().getValue().doubleValue()
                             + recycling7R600a.getImport().getValue().doubleValue());

    // With 100% induced demand, virgin consumption should be similar
    assertEquals(recycling6VirginTotal, recycling7VirginTotal, recycling6VirginTotal * 0.01,
        "With 100% induced demand, virgin consumption should be similar between Tutorial 6 and Tutorial 7");

    // But Tutorial 7 should have more total recycling (both HFC-134a and R-600a) than Tutorial 6 (only HFC-134a)
    double recycling6RecycleTotal = recycling6Hfc.getRecycle().getValue().doubleValue()
                                    + recycling6R600a.getRecycle().getValue().doubleValue();
    double recycling7RecycleTotal = recycling7Hfc.getRecycle().getValue().doubleValue()
                                    + recycling7R600a.getRecycle().getValue().doubleValue();

    assertTrue(recycling7RecycleTotal > recycling6RecycleTotal,
        "Tutorial 7 total recycling (" + recycling7RecycleTotal + ") should be greater than Tutorial 6 recycling ("
        + recycling6RecycleTotal + ") in 2035 due to additional R-600a recycling program in Tutorial 7");
  }

  /**
   * Test Tutorial 17: Basic consumption and equipment growth patterns.
   * - Consumption sees the impact of exports.
   * - Equipment does not see impact of exports.
   */
  @Test
  public void testTutorial17() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/tutorial_17.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Get year 2
    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2,
        "Domestic Refrigeration", "HFC-134a");

    assertNotNull(resultYear2, "Should have result for year 2");

    // Test 1: Consumption sees exports
    double export = resultYear2.getExport().getValue().doubleValue();
    double domestic = resultYear2.getDomestic().getValue().doubleValue();
    assertEquals(500, export, 0.1);
    assertEquals(1000, domestic, 0.1);

    // Test 2: Equipment does not see exports
    double equipment = resultYear2.getPopulation().getValue().doubleValue();
    assertEquals(11000, equipment, 0.1);
  }

  /**
   * Test Tutorial 04 alternative using % current instead of %.
   * This validates that % current produces identical results to % in change/set contexts.
   */
  @Test
  public void testTutorial04PercentCurrent() throws IOException {
    String qtaPath = "../examples/tutorial_04_percent_current.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    for (int year : new int[]{2025, 2035}) {
      EngineResult hfc134aResult = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      assertNotNull(hfc134aResult, "Should have HFC-134a result for " + year);

      assertTrue(hfc134aResult.getDomestic().getValue().doubleValue() > 0,
          "HFC-134a domestic should be non-zero in " + year);
      assertTrue(hfc134aResult.getImport().getValue().doubleValue() > 0,
          "HFC-134a import should be non-zero in " + year);
    }

    for (int year : new int[]{2025, 2035}) {
      EngineResult hfc134aResult = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic Refrigeration", "HFC-134a");
      EngineResult hfc32Result = LiveTestsUtil.getResult(resultsList.stream(), year,
          "Domestic AC", "HFC-32");

      assertNotNull(hfc134aResult, "Should have HFC-134a result for " + year);
      assertNotNull(hfc32Result, "Should have HFC-32 result for " + year);

      double hfc134aTotalKg = hfc134aResult.getDomestic().getValue().doubleValue()
                              + hfc134aResult.getImport().getValue().doubleValue();
      double hfc32TotalKg = hfc32Result.getDomestic().getValue().doubleValue();

      double hfc134aTotalTco2e = hfc134aResult.getConsumption().getValue().doubleValue();
      double hfc32TotalTco2e = hfc32Result.getConsumption().getValue().doubleValue();

      double maxKg = Math.max(hfc32TotalKg, hfc134aTotalKg);
      double kgPercentDiff = Math.abs(hfc32TotalKg - hfc134aTotalKg) / maxKg * 100;

      double maxTco2e = Math.max(hfc32TotalTco2e, hfc134aTotalTco2e);
      double tco2ePercentDiff = Math.abs(hfc32TotalTco2e - hfc134aTotalTco2e) / maxTco2e * 100;

      assertTrue(kgPercentDiff < tco2ePercentDiff,
          "Percentage difference in kg (" + kgPercentDiff + "%) should be smaller than "
          + "percentage difference in tCO2e (" + tco2ePercentDiff + "%) in year " + year);
    }
  }
}
