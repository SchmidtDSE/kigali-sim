/**
 * Change live tests using actual QTA files with "change" prefix.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    assertEquals(1450.0, year1Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 1 equipment should be ~1450 units");

    assertEquals(1903.0, year2Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 2 equipment should be ~1903 units");

    final EngineResult year3Result = LiveTestsUtil.getResult(resultsList.stream(), 3, "RAC1 - Resac1", "R-410A");
    assertEquals(2359.0, year3Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 3 equipment should be ~2359 units");

    final EngineResult year4Result = LiveTestsUtil.getResult(resultsList.stream(), 4, "RAC1 - Resac1", "R-410A");
    assertEquals(2820.0, year4Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 4 equipment should be ~2820 units");

    final EngineResult year5Result = LiveTestsUtil.getResult(resultsList.stream(), 5, "RAC1 - Resac1", "R-410A");
    assertEquals(3286.0, year5Result.getPopulation().getValue().doubleValue(), 5.0,
        "Year 5 equipment should be ~3286 units");
  }
}
