/**
 * Change live tests using actual QTA files with "change" prefix.
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
 * Tests that validate change QTA files against expected behavior.
 */
public class ChangeLiveTests {

  /**
   * Test change.qta produces expected values.
   */
  @Test
  public void testChange() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/change.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 110 mt = 110000 kg (10% increase from 100 mt)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(110000.0, result.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic should be 110000 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");

    // Check year 2 consumption value - should be 550 tCO2e (110 mt * 5 tCO2e/mt)
    assertEquals(550.0, result.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption should be 550 tCO2e");
    assertEquals("tCO2e", result.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test change_add_kg.qta produces expected values.
   */
  @Test
  public void testChangeAddKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/change_add_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 110 kg (100 kg + 10 kg)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(110.0, result.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic should be 110 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");
  }

  /**
   * Test change_subtract_kg.qta produces expected values.
   */
  @Test
  public void testChangeSubtractKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/change_subtract_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 90 kg (100 kg - 10 kg)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(90.0, result.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic should be 90 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");
  }

  /**
   * Test change_add_units.qta produces expected values.
   */
  @Test
  public void testChangeAddUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/change_add_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 110 kg (100 units + 10 units = 110 units * 1 kg/unit)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(110.0, result.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic should be 110 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");
  }

  /**
   * Test change_subtract_units.qta produces expected values.
   */
  @Test
  public void testChangeSubtractUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/change_subtract_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 90 kg (100 units - 10 units = 90 units * 1 kg/unit)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(90.0, result.getDomestic().getValue().doubleValue(), 0.0001,
        "Domestic should be 90 kg");
    assertEquals("kg", result.getDomestic().getUnits(),
        "Domestic units should be kg");
  }

  /**
   * Test Monte Carlo syntax in change statements - currently fails but documents the issue.
   * This test is expected to fail until the grammar properly supports Monte Carlo in change statements.
   */
  @Test
  public void testChangeMonteCarloSyntax() throws IOException {
    // This test documents the current limitation with Monte Carlo syntax in change statements
    // The QTA file ../examples/test_change_monte_carlo.qta contains:
    // change equipment by sample normally from mean of 6 % std of 1 % / year during years 2025 to 2030

    String qtaPath = "../examples/test_change_monte_carlo.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);

    // If we reach here, the syntax is now supported
    assertNotNull(program, "Program should not be null if syntax is supported");

    // Run a basic validation
    String scenarioName = "test";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Should have 10 trials * 2 years = 20 results
    assertEquals(20, resultsList.size(), "Should have 20 results (10 trials * 2 years)");
  }

  /**
   * Test that change operations properly respect units when last specified value was in units.
   * This tests the bug where recharge causes change operations to use kg instead of units.
   */
  @Test
  public void testChangeRechargeUnitsRespected() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/change_recharge_units_bug.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "S1";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Expected values: 500, 525, 551.25, 578.8125, 607.753125 (5% each year)
    // Actual problematic values: 500, 604.8, 750.8, 946.5, 1204.0

    // Check year 2 import value - should be 525 units, not 604.8
    EngineResult year2Result = LiveTestsUtil.getResult(resultsList.stream(), 2, "RAC1 - Resac1", "R-410A");
    assertNotNull(year2Result, "Should have result for RAC1 - Resac1/R-410A in year 2");

    // Check equipment values against expected progression with 5% retirement and 5% import growth
    // Expected: 1450, 1903, 2359, 2820, 3286 units (with rounding)
    final EngineResult year1Result = LiveTestsUtil.getResult(resultsList.stream(), 1, "RAC1 - Resac1", "R-410A");

    final EngineResult year3Result = LiveTestsUtil.getResult(resultsList.stream(), 3, "RAC1 - Resac1", "R-410A");
    final EngineResult year4Result = LiveTestsUtil.getResult(resultsList.stream(), 4, "RAC1 - Resac1", "R-410A");
    final EngineResult year5Result = LiveTestsUtil.getResult(resultsList.stream(), 5, "RAC1 - Resac1", "R-410A");

    assertEquals(1450.0, year1Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 1 equipment should be ~1450 units");

    assertEquals(1903.0, year2Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 2 equipment should be ~1903 units");

    assertEquals(2359.0, year3Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 3 equipment should be ~2359 units");

    assertEquals(2820.0, year4Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 4 equipment should be ~2820 units");

    assertEquals(3286.0, year5Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 5 equipment should be ~3286 units");
  }

  /**
   * Test sales cap with change statements to verify compounding percentage decreases.
   * Tests the issue where sales cap may not properly interact with change statements.
   */
  @Test
  public void testChangeCap() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/change_cap.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run both scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    Stream<EngineResult> permitResults = KigaliSimFacade.runScenario(program, "Permit", progress -> {});

    List<EngineResult> bauList = bauResults.collect(Collectors.toList());
    List<EngineResult> permitList = permitResults.collect(Collectors.toList());

    // Get HFC-134a consumption (sales) for key years 2029, 2030, 2031
    final EngineResult bau2029 = LiveTestsUtil.getResult(bauList.stream(), 2029, "Domestic Refrigeration", "HFC-134a");
    final EngineResult permit2029 = LiveTestsUtil.getResult(permitList.stream(), 2029, "Domestic Refrigeration", "HFC-134a");
    final EngineResult bau2030 = LiveTestsUtil.getResult(bauList.stream(), 2030, "Domestic Refrigeration", "HFC-134a");
    final EngineResult permit2030 = LiveTestsUtil.getResult(permitList.stream(), 2030, "Domestic Refrigeration", "HFC-134a");
    final EngineResult bau2031 = LiveTestsUtil.getResult(bauList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    final EngineResult permit2031 = LiveTestsUtil.getResult(permitList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");

    assertNotNull(bau2029, "Should have BAU result for 2029");
    assertNotNull(permit2029, "Should have Permit result for 2029");
    assertNotNull(bau2030, "Should have BAU result for 2030");
    assertNotNull(permit2030, "Should have Permit result for 2030");
    assertNotNull(bau2031, "Should have BAU result for 2031");
    assertNotNull(permit2031, "Should have Permit result for 2031");

    // Calculate percentage differences (BAU - Permit) / BAU for sales (domestic + import)
    double bauSales2029 = bau2029.getDomestic().getValue().doubleValue() + bau2029.getImport().getValue().doubleValue();
    double permitSales2029 = permit2029.getDomestic().getValue().doubleValue() + permit2029.getImport().getValue().doubleValue();
    double bauSales2030 = bau2030.getDomestic().getValue().doubleValue() + bau2030.getImport().getValue().doubleValue();
    double permitSales2030 = permit2030.getDomestic().getValue().doubleValue() + permit2030.getImport().getValue().doubleValue();
    double bauSales2031 = bau2031.getDomestic().getValue().doubleValue() + bau2031.getImport().getValue().doubleValue();
    double permitSales2031 = permit2031.getDomestic().getValue().doubleValue() + permit2031.getImport().getValue().doubleValue();

    double diff2029 = (bauSales2029 - permitSales2029) / bauSales2029;
    double diff2030 = (bauSales2030 - permitSales2030) / bauSales2030;
    double diff2031 = (bauSales2031 - permitSales2031) / bauSales2031;

    // Assert that the cap is creating a compounding effect (difference increases each year)
    // Should see 15% reduction compounding year over year while policy is active
    assertTrue(diff2030 > diff2029 + 0.05,
        "2030 gap should be more than 5% larger than 2029 gap (compounding effect)");
    assertTrue(diff2031 > diff2030 + 0.05,
        "2031 gap should be more than 5% larger than 2030 gap (compounding effect)");
  }

  /**
   * Test recycling with change statements to verify proper interaction.
   * Tests the issue where recycling may not properly persist its effect with change statements.
   */
  @Test
  public void testChangeRecycle() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/change_recycle.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run both scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});

    List<EngineResult> bauList = bauResults.collect(Collectors.toList());
    List<EngineResult> recyclingList = recyclingResults.collect(Collectors.toList());

    // Get HFC-134a consumption for key years 2026, 2027, 2028
    final EngineResult bau2026 = LiveTestsUtil.getResult(bauList.stream(), 2026, "Domestic Refrigeration", "HFC-134a");
    final EngineResult recycling2026 = LiveTestsUtil.getResult(recyclingList.stream(), 2026, "Domestic Refrigeration", "HFC-134a");
    final EngineResult bau2027 = LiveTestsUtil.getResult(bauList.stream(), 2027, "Domestic Refrigeration", "HFC-134a");
    final EngineResult recycling2027 = LiveTestsUtil.getResult(recyclingList.stream(), 2027, "Domestic Refrigeration", "HFC-134a");
    final EngineResult bau2028 = LiveTestsUtil.getResult(bauList.stream(), 2028, "Domestic Refrigeration", "HFC-134a");
    final EngineResult recycling2028 = LiveTestsUtil.getResult(recyclingList.stream(), 2028, "Domestic Refrigeration", "HFC-134a");

    assertNotNull(bau2026, "Should have BAU result for 2026");
    assertNotNull(recycling2026, "Should have Recycling result for 2026");
    assertNotNull(bau2027, "Should have BAU result for 2027");
    assertNotNull(recycling2027, "Should have Recycling result for 2027");
    assertNotNull(bau2028, "Should have BAU result for 2028");
    assertNotNull(recycling2028, "Should have Recycling result for 2028");

    // Calculate total sales (domestic + import) for both scenarios - ensure we're using kg
    // First verify all values are in kg
    assertEquals("kg", bau2026.getDomestic().getUnits(), "Domestic should be in kg");
    assertEquals("kg", bau2026.getImport().getUnits(), "Import should be in kg");
    
    double bauSales2026 = bau2026.getDomestic().getValue().doubleValue() + bau2026.getImport().getValue().doubleValue();
    double recyclingSales2026 = recycling2026.getDomestic().getValue().doubleValue() + recycling2026.getImport().getValue().doubleValue();
    double bauSales2027 = bau2027.getDomestic().getValue().doubleValue() + bau2027.getImport().getValue().doubleValue();
    double recyclingSales2027 = recycling2027.getDomestic().getValue().doubleValue() + recycling2027.getImport().getValue().doubleValue();
    double bauSales2028 = bau2028.getDomestic().getValue().doubleValue() + bau2028.getImport().getValue().doubleValue();
    double recyclingSales2028 = recycling2028.getDomestic().getValue().doubleValue() + recycling2028.getImport().getValue().doubleValue();


    // Calculate percentage differences (BAU - Recycling) / BAU for total GHG consumption
    double diff2026 = (bau2026.getGhgConsumption().getValue().doubleValue() - recycling2026.getGhgConsumption().getValue().doubleValue())
        / bau2026.getGhgConsumption().getValue().doubleValue();
    double diff2027 = (bau2027.getGhgConsumption().getValue().doubleValue() - recycling2027.getGhgConsumption().getValue().doubleValue())
        / bau2027.getGhgConsumption().getValue().doubleValue();
    double diff2028 = (bau2028.getGhgConsumption().getValue().doubleValue() - recycling2028.getGhgConsumption().getValue().doubleValue())
        / bau2028.getGhgConsumption().getValue().doubleValue();

    // Calculate sales differences between BAU and Recycling
    double salesDiff2026 = Math.abs(bauSales2026 - recyclingSales2026) / bauSales2026;
    double salesDiff2027 = (bauSales2027 - recyclingSales2027) / bauSales2027;
    double salesDiff2028 = (bauSales2028 - recyclingSales2028) / bauSales2028;

    // Assert that recycling effect on sales is sustained
    // 2027 should show significant reduction compared to 2026 (recycling starts in 2027)
    assertTrue(salesDiff2027 > salesDiff2026 + 0.05,
        "2027 sales difference should be at least 5% larger than 2026 (recycling starts in 2027)");
    // 2028 sales difference should be larger than or equal to 2027 (recycling should persist)
    assertTrue(salesDiff2028 >= salesDiff2027 - 0.01,
        "2028 sales difference should be larger than or equal to 2027 (recycling should persist)");

    // Assert that recycling effect is sustained and grows (or stays constant) over time
    // 2027 should show larger gap than 2026 (recycling starts in 2027)
    assertTrue(diff2027 > diff2026,
        "2027 gap should be larger than 2026 gap (recycling starts in 2027)");
    // 2028 gap should be larger than or equal to 2027 gap (recycling should persist/grow)
    assertTrue(diff2028 >= diff2027,
        "2028 gap should be larger than or equal to 2027 gap (recycling should persist)");
  }

  /**
   * Test recycling with change statements using units-based specifications.
   * Tests the same issue as testChangeRecycle but with units instead of mt/kg.
   */
  @Test
  public void testChangeRecycleUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/change_recycle_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run both scenarios
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    Stream<EngineResult> recyclingResults = KigaliSimFacade.runScenario(program, "Recycling", progress -> {});

    List<EngineResult> bauList = bauResults.collect(Collectors.toList());
    List<EngineResult> recyclingList = recyclingResults.collect(Collectors.toList());

    // Get HFC-134a results for key years 2026, 2027, 2028
    final EngineResult bau2026 = LiveTestsUtil.getResult(bauList.stream(), 2026, "Domestic Refrigeration", "HFC-134a");
    final EngineResult recycling2026 = LiveTestsUtil.getResult(recyclingList.stream(), 2026, "Domestic Refrigeration", "HFC-134a");
    final EngineResult bau2027 = LiveTestsUtil.getResult(bauList.stream(), 2027, "Domestic Refrigeration", "HFC-134a");
    final EngineResult recycling2027 = LiveTestsUtil.getResult(recyclingList.stream(), 2027, "Domestic Refrigeration", "HFC-134a");
    final EngineResult bau2028 = LiveTestsUtil.getResult(bauList.stream(), 2028, "Domestic Refrigeration", "HFC-134a");
    final EngineResult recycling2028 = LiveTestsUtil.getResult(recyclingList.stream(), 2028, "Domestic Refrigeration", "HFC-134a");

    assertNotNull(bau2026, "Should have BAU result for 2026");
    assertNotNull(recycling2026, "Should have Recycling result for 2026");
    assertNotNull(bau2027, "Should have BAU result for 2027");
    assertNotNull(recycling2027, "Should have Recycling result for 2027");
    assertNotNull(bau2028, "Should have BAU result for 2028");
    assertNotNull(recycling2028, "Should have Recycling result for 2028");

    // Calculate total sales by adding domestic + import explicitly
    double bauSales2026 = bau2026.getDomestic().getValue().doubleValue() + bau2026.getImport().getValue().doubleValue();
    double recyclingSales2026 = recycling2026.getDomestic().getValue().doubleValue() + recycling2026.getImport().getValue().doubleValue();
    double bauSales2027 = bau2027.getDomestic().getValue().doubleValue() + bau2027.getImport().getValue().doubleValue();
    double recyclingSales2027 = recycling2027.getDomestic().getValue().doubleValue() + recycling2027.getImport().getValue().doubleValue();
    double bauSales2028 = bau2028.getDomestic().getValue().doubleValue() + bau2028.getImport().getValue().doubleValue();
    double recyclingSales2028 = recycling2028.getDomestic().getValue().doubleValue() + recycling2028.getImport().getValue().doubleValue();

    // Calculate percentage differences (BAU - Recycling) / BAU for total GHG consumption
    double diff2026 = (bau2026.getGhgConsumption().getValue().doubleValue() - recycling2026.getGhgConsumption().getValue().doubleValue())
        / bau2026.getGhgConsumption().getValue().doubleValue();
    double diff2027 = (bau2027.getGhgConsumption().getValue().doubleValue() - recycling2027.getGhgConsumption().getValue().doubleValue())
        / bau2027.getGhgConsumption().getValue().doubleValue();
    double diff2028 = (bau2028.getGhgConsumption().getValue().doubleValue() - recycling2028.getGhgConsumption().getValue().doubleValue())
        / bau2028.getGhgConsumption().getValue().doubleValue();

    // Calculate sales differences between BAU and Recycling
    double salesDiff2026 = Math.abs(bauSales2026 - recyclingSales2026) / bauSales2026;
    double salesDiff2027 = (bauSales2027 - recyclingSales2027) / bauSales2027;
    double salesDiff2028 = (bauSales2028 - recyclingSales2028) / bauSales2028;

    // Assert that recycling effect on sales is sustained
    // 2027 should show significant reduction compared to 2026 (recycling starts in 2027)
    assertTrue(salesDiff2027 > salesDiff2026 + 0.03,
        "2027 sales difference should be at least 3% larger than 2026 (recycling starts in 2027)");
    // 2028 sales difference should be positive (recycling should reduce sales, not increase them)
    assertTrue(salesDiff2028 > 0,
        "2028 sales should be lower in recycling scenario than BAU (recycling should persist)");

    // Assert that recycling effect is sustained and grows (or stays constant) over time
    // 2027 should show larger gap than 2026 (recycling starts in 2027)
    assertTrue(diff2027 > diff2026,
        "2027 gap should be larger than 2026 gap (recycling starts in 2027)");
    // 2028 gap should be positive (recycling should reduce consumption)
    assertTrue(diff2028 > 0,
        "2028 should show positive reduction (recycling should reduce consumption)");
  }
}
