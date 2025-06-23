/**
 * Minimal live tests using actual QTA files.
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
 * Tests that validate minimal QTA files against expected behavior.
 */
public class MinimalLiveTests {

  /**
   * Test minimal_interpreter.qta produces expected manufacture values.
   */
  @Test
  public void testMinimalInterpreterExample() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/minimal_interpreter.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    // Convert to list for checking results across multiple years
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Verify results for each year
    for (int year = 1; year <= 3; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year, "testApp", "testSubstance");
      assertNotNull(result, "Should have result for testApp/testSubstance in year " + year);

      // Check manufacture value - should be 100 mt = 100000 kg
      assertEquals(100000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
          "Manufacture should be 100000 kg in year " + year);
      assertEquals("kg", result.getManufacture().getUnits(),
          "Manufacture units should be kg in year " + year);
    }
  }

  /**
   * Debug test to check actual values for basic_replace_simple.qta.
   */
  @Test
  public void debugBasicReplaceSimple() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/basic_replace_simple.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Sim";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Print actual values for year 1
    EngineResult recordYear1A = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "Sub A");
    if (recordYear1A != null) {
      System.out.println("Sub A Year 1 - GHG: " + recordYear1A.getGhgConsumption().getValue() + " " + recordYear1A.getGhgConsumption().getUnits());
      System.out.println("Sub A Year 1 - Manufacture: " + recordYear1A.getManufacture().getValue() + " " + recordYear1A.getManufacture().getUnits());
    }

    EngineResult recordYear1B = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "Sub B");
    if (recordYear1B != null) {
      System.out.println("Sub B Year 1 - GHG: " + recordYear1B.getGhgConsumption().getValue() + " " + recordYear1B.getGhgConsumption().getUnits());
      System.out.println("Sub B Year 1 - Manufacture: " + recordYear1B.getManufacture().getValue() + " " + recordYear1B.getManufacture().getUnits());
    }

    // Print actual values for year 10
    EngineResult recordYear10A = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "Sub A");
    if (recordYear10A != null) {
      System.out.println("Sub A Year 10 - GHG: " + recordYear10A.getGhgConsumption().getValue() + " " + recordYear10A.getGhgConsumption().getUnits());
      System.out.println("Sub A Year 10 - Manufacture: " + recordYear10A.getManufacture().getValue() + " " + recordYear10A.getManufacture().getUnits());
    }

    EngineResult recordYear10B = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "Sub B");
    if (recordYear10B != null) {
      System.out.println("Sub B Year 10 - GHG: " + recordYear10B.getGhgConsumption().getValue() + " " + recordYear10B.getGhgConsumption().getUnits());
      System.out.println("Sub B Year 10 - Manufacture: " + recordYear10B.getManufacture().getValue() + " " + recordYear10B.getManufacture().getUnits());
    }

    // Print all available results for inspection
    System.out.println("All results:");
    for (EngineResult result : resultsList) {
      System.out.println("Year " + result.getYear() + " " + result.getApplication() + "/" + result.getSubstance()
          + " - GHG: " + result.getGhgConsumption().getValue() + " " + result.getGhgConsumption().getUnits());
    }
  }
}
