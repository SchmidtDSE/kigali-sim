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
  @Test
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

    // Check year 2 - retire commands should continue to be additive
    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(resultYear2, "Should have result for test/test in year 2");
    
    // Year 2: Starting with 92.5 units from year 1
    // Retire another 15 kg (7.5 units)
    // Expected remaining: 92.5 - 7.5 = 85 units
    assertEquals(85.0, resultYear2.getPopulation().getValue().doubleValue(), 0.0001,
        "Equipment should be 85 units in year 2 after continued additive retire");
    assertEquals("units", resultYear2.getPopulation().getUnits(),
        "Equipment units should be units");
  }
}
