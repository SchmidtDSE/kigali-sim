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
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Live tests for precharge (recharge of newEquipment) functionality.
 *
 * <p>These tests verify the behavior of precharge calculations using the
 * "of newEquipment" clause on recharge statements.</p>
 */
public class PrechargeLiveTests {

  /**
   * Test that recharge with "of priorEquipment" produces same results as bare recharge.
   */
  @Test
  public void testRechargeOfPriorEqualsBareRecharge() throws IOException {
    String qtaPath = "../examples/recharge_of_prior.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Should match the results from recharge.qta (which uses bare "recharge 10% with 1 kg / unit")
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "test");
    assertNotNull(resultYear1, "Should have result for test/test in year 1");
    assertEquals(100000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 100000 units in year 1 (same as bare recharge)");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");

    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(resultYear2, "Should have result for test/test in year 2");
    assertEquals(190000.0, resultYear2.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 190000 units in year 2 (same as bare recharge)");
  }

  /**
   * Test basic precharge with units-based tracking.
   *
   * <p>With 1000 new units, 5% precharge = 50 units × 1 kg/unit = 50 kg precharge.
   * Equipment should be 10000 prior + 1000 new = 11000 units.
   * Recharge emissions should include precharge contribution (50 kg × 1 tCO2e/kg = 50 tCO2e).</p>
   */
  @Test
  public void testPrechargeUnits() throws IOException {
    String qtaPath = "../examples/precharge_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: Equipment should be 11000 units (10000 prior + 1000 new)
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub1");
    assertNotNull(resultYear1, "Should have result for App/Sub1 in year 1");
    assertEquals(11000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 11000 units in year 1 (10000 prior + 1000 new)");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");

    // Year 1: RechargeEmissions should be non-zero (from precharge)
    EngineNumber rechargeEmissions = resultYear1.getRechargeEmissions();
    assertNotNull(rechargeEmissions, "Recharge emissions should not be null");
    assertTrue(rechargeEmissions.getValue().doubleValue() > 0,
        "Recharge emissions should be positive (from precharge): " + rechargeEmissions.getValue());
  }

  /**
   * Test precharge combined with recharge (both prior and new equipment).
   */
  @Test
  public void testPrechargeWithRecharge() throws IOException {
    String qtaPath = "../examples/precharge_with_recharge.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: Equipment should be 11000 units (10000 prior + 1000 new)
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub1");
    assertNotNull(resultYear1, "Should have result for App/Sub1 in year 1");
    assertEquals(11000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 11000 units in year 1");

    // Year 1: RechargeEmissions should include both recharge (prior) and precharge (new)
    EngineNumber rechargeEmissions = resultYear1.getRechargeEmissions();
    assertNotNull(rechargeEmissions, "Recharge emissions should not be null");
    assertTrue(rechargeEmissions.getValue().doubleValue() > 0,
        "Recharge emissions should be positive (from both recharge and precharge)");
  }

  /**
   * Test precharge with a change statement that modifies sales.
   */
  @Test
  public void testPrechargeWithChange() throws IOException {
    String qtaPath = "../examples/precharge_with_change.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: Equipment should be 11000 units (10000 prior + 1000 new)
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub1");
    assertNotNull(resultYear1, "Should have result for App/Sub1 in year 1");
    assertEquals(11000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 11000 units in year 1");

    // Year 2: Should have more units due to +10% change in domestic
    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "App", "Sub1");
    assertNotNull(resultYear2, "Should have result for App/Sub1 in year 2");
    assertTrue(resultYear2.getPopulation().getValue().doubleValue() > 11000.0,
        "Equipment should increase in year 2 due to +10% change: "
            + resultYear2.getPopulation().getValue());
  }

  /**
   * Test that recharge of newEquipment runs and produces results.
   */
  @Test
  public void testRechargeOfNewEquipmentRuns() throws IOException {
    String qtaPath = "../examples/recharge_of_new.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    assertTrue(resultsList.size() > 0, "Should have results for recharge of newEquipment");
  }
}
