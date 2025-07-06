/**
 * Recharge live tests using actual QTA files.
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
 * Tests that validate recharge QTA files against expected behavior.
 */
public class RechargeLiveTests {

  /**
   * Test recharge.qta produces expected values.
   */
  @Test
  public void testRecharge() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recharge.qta";
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
   * Test recharge_on_top.qta produces expected values.
   */
  @Test
  public void testRechargeOnTop() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recharge_on_top.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 1 equipment (population) value
    // Should have 10000 (prior) + 1000 (manufacture) = 11000 units in year 1
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "App", "Sub1");
    assertNotNull(resultYear1, "Should have result for App/Sub1 in year 1");
    assertEquals(11000.0, resultYear1.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 11000 units in year 1");
    assertEquals("units", resultYear1.getPopulation().getUnits(),
        "Equipment units should be units");
  }

  /**
   * Test recharge_import_issue.qta to verify units with recharge and import.
   * This test validates that when we have priorEquipment of 20000 units and import set,
   * the total units should be at least 20000 in 2025 (not decrease).
   */
  @Test
  public void testRechargeImportIssue() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/recharge_import_issue.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2025 equipment (population) value
    // Should have at least 20000 units (the priorEquipment value)
    EngineResult resultYear2025 = LiveTestsUtil.getResult(resultsList.stream(), 2025, "Domestic AC", "HFC-32");
    assertNotNull(resultYear2025, "Should have result for Domestic AC/HFC-32 in year 2025");
    
    double unitsIn2025 = resultYear2025.getPopulation().getValue().doubleValue();
    
    // Assert that units should be at least 20000 (the priorEquipment value)
    assertEquals(true, unitsIn2025 >= 20000.0,
        "Equipment should be at least 20000 units in year 2025 (priorEquipment value), but was " + unitsIn2025);
    assertEquals("units", resultYear2025.getPopulation().getUnits(),
        "Equipment units should be units");
  }

}
