/**
 * Live test reproducing a cap displacement + recharge scenario where the
 * displaced substance's consumption drops sharply the year after the cap window
 * ends.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

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
 * Tests that validate a cap-and-displace scenario with recharge where SubB
 * starts with zero sales. SubA is capped (displacing to SubB) during years 3
 * to 4.
 *
 * <p>Displacement writes the displaced volume onto SubB's sales last-specified
 * value (in the same units as the value it overwrites), so SubB's sales carry
 * over after the cap window ends rather than collapsing back to zero in year 5.</p>
 */
public class CapLiveTest {

  /**
   * Confirm that when the SubA cap (displacing to SubB) ends after year 4, SubB
   * continues to sell equipment built from the displaced demand instead of
   * collapsing to only its own recharge. This documents the sales carry-over
   * behavior on displacement.
   */
  @Test
  public void testSubConsumptionCarriesOverAfterCapWindow() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/cap_displace_sales_drop_year5.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the "With Permit" scenario
    String scenarioName = "With Permit";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // QTA names: application "Test", substances "SubA" and "SubB".
    EngineResult subA3 = LiveTestsUtil.getResult(resultsList.stream(), 3, "Test", "SubA");
    EngineResult subB3 = LiveTestsUtil.getResult(resultsList.stream(), 3, "Test", "SubB");
    EngineResult subB4 = LiveTestsUtil.getResult(resultsList.stream(), 4, "Test", "SubB");

    assertNotNull(subA3, "Should have result for Test/SubA in year 3");
    assertNotNull(subB3, "Should have result for Test/SubB in year 3");
    assertNotNull(subB4, "Should have result for Test/SubB in year 4");

    // During the cap window (years 3-4) SubB receives displaced sales from SubA,
    // so its consumption is substantial.
    double subB3cons = subB3.getDomestic().getValue().doubleValue();
    double subB4cons = subB4.getDomestic().getValue().doubleValue();
    assertTrue(subB3cons > 0, "SubB consumption should be > 0 in year 3 (displacement active)");
    assertTrue(subB4cons > 0, "SubB consumption should be > 0 in year 4 (displacement active)");

    // Once the cap window ends (year 5), SubB must carry over the displaced sales
    // rather than collapsing to only its own recharge: it continues selling new
    // equipment and its consumption stays around (or above) the displaced level.
    EngineResult subB5 = LiveTestsUtil.getResult(resultsList.stream(), 5, "Test", "SubB");
    EngineResult subB6 = LiveTestsUtil.getResult(resultsList.stream(), 6, "Test", "SubB");
    assertNotNull(subB5, "Should have result for Test/SubB in year 5");
    assertNotNull(subB6, "Should have result for Test/SubB in year 6");

    double subB5cons = subB5.getDomestic().getValue().doubleValue();
    double subB6cons = subB6.getDomestic().getValue().doubleValue();
    double subB5popNew = subB5.getPopulationNew().getValue().doubleValue();
    assertTrue(subB5popNew > 0,
        "SubB should keep adding new equipment in year 5 (displaced sales carry over)");
    assertTrue(subB5cons >= subB4cons,
        "SubB consumption should not drop in year 5 after the cap window ends; displaced "
            + "sales carry over (year 4: " + subB4cons + ", year 5: " + subB5cons + ")");
    assertTrue(subB6cons >= subB4cons,
        "SubB consumption should remain at the carried-over displaced level in year 6 "
            + "(year 6: " + subB6cons + ")");
  }
}
