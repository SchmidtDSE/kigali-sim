/**
 * Replace live tests using actual QTA files with "replace" prefix.
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
 * Tests that validate replace QTA files against expected behavior.
 */
public class ReplaceLiveTests {

  /**
   * Test replace.qta produces expected values.
   * This tests replacing a percentage of manufacture from one substance to another.
   */
  @Test
  public void testReplace() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/replace.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check substance "a" consumption
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "a");
    assertNotNull(resultA, "Should have result for test/a in year 1");

    // Calculation: Original 50 mt - replaced 50% = 25 mt remaining × 10 tCO2e/mt = 250 tCO2e
    assertEquals(250.0, resultA.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 250 tCO2e");
    assertEquals("tCO2e", resultA.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Check substance "b" consumption
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Calculation: Original 50 mt + added 25 mt = 75 mt total × 5 tCO2e/mt = 375 tCO2e
    assertEquals(375.0, resultB.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 375 tCO2e");
    assertEquals("tCO2e", resultB.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test replace_kg.qta produces expected values.
   * This tests replacing a specific amount (in kg) of manufacture from one substance to another.
   */
  @Test
  public void testReplaceKg() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/replace_kg.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check substance "a" consumption
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "a");
    assertNotNull(resultA, "Should have result for test/a in year 1");

    // Calculation: Original 50 mt - replaced 25 kg = 25 mt remaining × 10 tCO2e/mt = 250 tCO2e
    assertEquals(250.0, resultA.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 250 tCO2e");
    assertEquals("tCO2e", resultA.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Check substance "b" consumption
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Calculation: Original 50 mt + added 25 kg = 75 mt total × 5 tCO2e/mt = 375 tCO2e
    assertEquals(375.0, resultB.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 375 tCO2e");
    assertEquals("tCO2e", resultB.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }

  /**
   * Test replace_units.qta produces expected values.
   * This tests replacing a specific number of units of manufacture from one substance to another.
   */
  @Test
  public void testReplaceUnits() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/replace_units.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "result";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check substance "a" consumption
    EngineResult resultA = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "a");
    assertNotNull(resultA, "Should have result for test/a in year 1");

    // Calculation: Original 50 mt - replaced 25000 units = 25 mt remaining × 10 tCO2e/mt = 250 tCO2e
    assertEquals(250.0, resultA.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance a should be 250 tCO2e");
    assertEquals("tCO2e", resultA.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");

    // Check substance "b" consumption
    EngineResult resultB = LiveTestsUtil.getResult(resultsList.stream(), 1, "test", "b");
    assertNotNull(resultB, "Should have result for test/b in year 1");

    // Calculation: Original 50 mt + added 25000 units = 75 mt total × 5 tCO2e/mt = 375 tCO2e
    assertEquals(375.0, resultB.getGhgConsumption().getValue().doubleValue(), 0.0001,
        "Consumption for substance b should be 375 tCO2e");
    assertEquals("tCO2e", resultB.getGhgConsumption().getUnits(),
        "Consumption units should be tCO2e");
  }
}
