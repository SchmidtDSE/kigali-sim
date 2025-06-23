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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 110 mt = 110000 kg (10% increase from 100 mt)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(110000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 110000 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");

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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 110 kg (100 kg + 10 kg)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(110.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 110 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 90 kg (100 kg - 10 kg)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(90.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 90 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 110 kg (100 units + 10 units = 110 units * 1 kg/unit)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(110.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 110 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
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
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Check year 2 manufacture value - should be 90 kg (100 units - 10 units = 90 units * 1 kg/unit)
    EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), 2, "test", "test");
    assertNotNull(result, "Should have result for test/test in year 2");
    assertEquals(90.0, result.getManufacture().getValue().doubleValue(), 0.0001,
        "Manufacture should be 90 kg");
    assertEquals("kg", result.getManufacture().getUnits(),
        "Manufacture units should be kg");
  }
}
