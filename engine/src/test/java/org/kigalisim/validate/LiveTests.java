/**
 * Live tests using actual QTA files.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;
import org.kigalisim.lang.program.ParsedScenario;

/**
 * Tests that validate QTA files against expected behavior.
 */
public class LiveTests {

  /**
   * Test minimal_interpreter.qta produces expected manufacture values.
   */
  @Test
  public void testMinimalInterpreterExample() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/minimal_interpreter.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Get the "business as usual" scenario
    String scenarioName = "business as usual";
    ParsedScenario scenario = program.getScenario(scenarioName);
    assertNotNull(scenario, "Scenario should exist");

    // Get start and end years from scenario
    int startYear = scenario.getStartYear();
    int endYear = scenario.getEndYear();
    assertEquals(1, startYear, "Start year should be 1");
    assertEquals(3, endYear, "End year should be 3");

    // Run the scenario and get results
    List<EngineResult> results = KigaliSimFacade.runScenarioWithResults(program, scenarioName)
        .collect(Collectors.toList());

    // Verify results for each year
    for (int year = startYear; year <= endYear; year++) {
      // Find the result for testApp/testSubstance
      EngineResult result = findResult(results, "testApp", "testSubstance", year);
      assertNotNull(result, "Should have result for testApp/testSubstance in year " + year);
      
      // Check manufacture value - should be 100 mt = 100000 kg
      assertEquals(100000.0, result.getManufacture().getValue().doubleValue(), 0.0001,
          "Manufacture should be 100000 kg in year " + year);
      assertEquals("kg", result.getManufacture().getUnits(),
          "Manufacture units should be kg in year " + year);
    }
  }

  /**
   * Find a result for the specified application and substance.
   *
   * @param results List of engine results
   * @param application Application name
   * @param substance Substance name
   * @param year Year
   * @return The matching EngineResult or null if not found
   */
  private EngineResult findResult(List<EngineResult> results, String application, 
      String substance, int year) {
    return results.stream()
        .filter(r -> r.getApplication().equals(application))
        .filter(r -> r.getSubstance().equals(substance))
        .filter(r -> r.getYear() == year)
        .findFirst()
        .orElse(null);
  }
}