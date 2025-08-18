/**
 * Energy consumption live tests using actual QTA files with "energy" prefix.
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
 * Tests that validate energy consumption calculations in QTA files.
 */
public class EnergyLiveTests {

  /**
   * Test volume-based energy consumption using kwh / kg units.
   * Validates point values and rate-of-change behavior as equipment population grows.
   */
  @Test
  public void testVolumeBasedEnergyConsumption() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/energy_volume_test.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Get results for year 1, 2, 9, and 10
    EngineResult resultYear1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "SubA");
    assertNotNull(resultYear1, "Should have result for Test/SubA in year 1");

    EngineResult resultYear2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "Test", "SubA");
    assertNotNull(resultYear2, "Should have result for Test/SubA in year 2");

    EngineResult resultYear9 = LiveTestsUtil.getResult(resultsList.stream(), 9, "Test", "SubA");
    assertNotNull(resultYear9, "Should have result for Test/SubA in year 9");

    EngineResult resultYear10 = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "SubA");
    assertNotNull(resultYear10, "Should have result for Test/SubA in year 10");

    // Validate year 1 energy consumption equals 500.0 kWh
    // 1 mt = 1000 kg, 1000 kg * 0.5 kwh/kg = 500 kwh
    assertEquals(500.0, resultYear1.getEnergyConsumption().getValue().doubleValue(), 0.0001,
        "Year 1 energy consumption should be 500.0 kWh");
    assertEquals("kwh", resultYear1.getEnergyConsumption().getUnits(),
        "Energy consumption units should be kwh");

    // Calculate year-to-year differences
    double energyYear1 = resultYear1.getEnergyConsumption().getValue().doubleValue();
    double energyYear2 = resultYear2.getEnergyConsumption().getValue().doubleValue();
    double energyYear9 = resultYear9.getEnergyConsumption().getValue().doubleValue();
    double energyYear10 = resultYear10.getEnergyConsumption().getValue().doubleValue();

    double differenceYear1To2 = energyYear2 - energyYear1;
    double differenceYear9To10 = energyYear10 - energyYear9;

    // Assert both differences are positive (increasing consumption)
    assertTrue(differenceYear1To2 > 0,
        "Energy consumption should increase from year 1 to year 2. Difference was: " + differenceYear1To2);
    assertTrue(differenceYear9To10 > 0,
        "Energy consumption should increase from year 9 to year 10. Difference was: " + differenceYear9To10);

    // Assert second derivative is negative (rate of increase is slowing)
    // The year 9-10 difference should be smaller than year 1-2 difference
    assertTrue(differenceYear9To10 < differenceYear1To2,
        "Rate of energy consumption increase should be slowing (second derivative negative). "
        + "Year 1-2 difference: " + differenceYear1To2 + ", Year 9-10 difference: " + differenceYear9To10);
  }
}