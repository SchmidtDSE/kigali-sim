/**
 * Retire live tests using actual QTA files.
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
 * Tests that validate retire QTA files against expected behavior.
 */
public class RetireLiveTests {

  /**
   * Test retire.qta produces expected values.
   */
  @Test
  public void testRetire() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/retire.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 equipment (population) value
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(resultYear1, "Should have result for test/test in year 1");
    assertEquals(100000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 100000 units in year 1");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");

    // Check year 2 equipment (population) value
    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(resultYear2, "Should have result for test/test in year 2");
    assertEquals(190000.0, resultYear2.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 190000 units in year 2");
    assertEquals("units", resultYear2.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test retire_prior.qta produces expected values.
   */
  // TODO: Re-enable in Component 6 after manual priorEquipment invalidation is implemented
  // This test explicitly sets priorEquipment after retire, which requires base invalidation
  // @Test
  public void testRetirePrior() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/retire_prior.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 equipment (population) value
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(resultYear1, "Should have result for test/test in year 1");
    assertEquals(190000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 190000 units in year 1");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test retire_multiple.qta produces expected values.
   */
  @Test
  public void testRetireMultiple() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/retire_multiple.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 equipment (population) value
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(resultYear1, "Should have result for test/test in year 1");
    assertTrue(resultYear1.getPopulation().getValue().doubleValue() < 190000.0,
        "Equipment should be less than 190000 units in year 1");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test that multiple retire commands are additive and properly trigger recalculation.
   */
  @Test
  public void testAdditiveRetire() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/test_additive_retire.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 equipment (population) value
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(resultYear1, "Should have result for test/test in year 1");

    // Initial: 100 units * 2 kg/unit = 200 kg
    // Retire: 5 kg + 10 kg = 15 kg total
    // Expected remaining: 200 - 15 = 185 kg
    // In units: 185 kg / 2 kg/unit = 92.5 units
    assertEquals(92.5, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 92.5 units after additive retire commands (15 kg total)");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");

    // Check year 2 - retire commands should continue to be additive with sales carryover
    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(resultYear2, "Should have result for test/test in year 2");

    // Year 2: Starting with 92.5 units from year 1 (185 kg)
    // New sales: 100 units (200 kg) added in year 2
    // Total before retire: 185 + 200 = 385 kg
    // Retire commands apply again: 15 kg total (5 kg + 10 kg)
    // Expected remaining: 385 - 15 = 370 kg = 185 units
    assertEquals(185.0, resultYear2.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 185 units in year 2 (92.5 from year 1 + 100 new sales - 7.5 retired)");
    assertEquals("units", resultYear2.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test cumulative retire behavior across multiple years.
   */
  @Test
  public void testCumulativeRetire() throws IOException {
    String qtaPath = "../examples/test_cumulative_retire.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "business as usual", p -> {});
    List<EngineResult> list = results.collect(Collectors.toList());

    // Year 1: 100 - (5%+10%=15% of 100) + 10 new = 95
    EngineResult y1 = LiveTestsUtil.getResult(list.stream(), 1, "test", "test");
    assertEquals(95.0, y1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 95 after cumulative 15% retire");

    // Year 2: 95 - (15% of 95) + 10 new = 90.75
    EngineResult y2 = LiveTestsUtil.getResult(list.stream(), 2, "test", "test");
    assertEquals(90.75, y2.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 90.75 after year 2 retire");

    // Year 3: 90.75 - (15% of 90.75) + 10 new = 87.1375
    EngineResult y3 = LiveTestsUtil.getResult(list.stream(), 3, "test", "test");
    assertEquals(87.1375, y3.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 87.1375 after year 3 retire");
  }

  /**
   * Test cumulative retire + recharge in same year (separate bases).
   */
  @Test
  public void testCumulativeRetireAndRecharge() throws IOException {
    String qtaPath = "../examples/test_cumulative_retire_recharge.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "business as usual", p -> {});
    List<EngineResult> list = results.collect(Collectors.toList());

    // Year 1:
    // - Start: 100 units
    // - Retire 15% of 100 = 15 units → 85 units
    // - Add new: 10 units → 95 units
    // - Recharge 15% of 95 = 14.25 units
    EngineResult y1 = LiveTestsUtil.getResult(list.stream(), 1, "test", "test");
    assertEquals(95.0, y1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment after retire and new sales");
  }

  /**
   * Test that manual priorEquipment changes invalidate bases correctly.
   *
   * <p>This edge case tests the scenario where a user explicitly modifies
   * priorEquipment mid-year after cumulative retire operations have already
   * captured their base population. The invalidation logic should proportionally
   * scale both the base and applied amounts to maintain mathematical consistency.</p>
   */
  @Test
  public void testRetireWithManualPriorEquipmentChange() throws IOException {
    String qtaPath = "../examples/test_retire_manual_prior.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    Stream<EngineResult> results = KigaliSimFacade.runScenario(
        program, "business as usual", p -> {});
    List<EngineResult> list = results.collect(Collectors.toList());

    // Year 1 Execution Flow:
    // 1. set priorEquipment to 100 units → prior = 100
    // 2. retire 10% → base=100, applied=10, prior = 90
    // 3. set priorEquipment to 50 units → invalidation scales: base=50, applied=5, prior=50
    // 4. retire 5% → cumulative 15% of base 50 = 7.5, delta = 7.5-5 = 2.5, prior = 47.5
    //
    // Final equipment: 47.5 units (NOT 42.5 which would be 50 - 15% without scaling)
    EngineResult y1 = LiveTestsUtil.getResult(list.stream(), 1, "test", "test");
    assertEquals(47.5, y1.getPopulation().getValue().doubleValue(), 0.0001,
        "Manual priorEquipment change should proportionally scale cumulative base");
  }

  /**
   * Test cumulative retirement with negative adjustment.
   * Tests that retire 10% followed by retire -5% results in net 5% retirement.
   */
  @Test
  public void testNegativeRetire() throws IOException {
    String qtaPath = "../examples/test_negative_retire.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "business as usual", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: retire 10% - 5% = 5% of 100 base = 5 units retired
    // Equipment: 100 base - 5 retired + 10 sales = 105 units
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(resultYear1, "Should have result for test/test in year 1");
    assertEquals(105.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 105 units after net 5% retirement (10% - 5%)");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");
  }
}
