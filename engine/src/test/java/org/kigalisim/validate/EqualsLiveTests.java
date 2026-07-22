/**
 * Live tests for the equals directive.
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
 * Tests that validate the equals directive, including its interaction with
 * year qualifiers.
 *
 * <p>These tests check a reported bug where adding a year qualifier to the
 * kwh / unit directive (e.g. "equals 1 kwh / unit during year 2021") produces
 * different energy consumption than the unqualified version. The expected
 * energy consumption is 100 * (year - 2020) kwh, reflecting 100 units of
 * import accumulating each year with 1 kwh / unit.</p>
 */
public class EqualsLiveTests {

  private static final String APPLICATION = "Domestic Refrigeration";
  private static final String SUBSTANCE = "HFC-134a";

  /**
   * Run a QTA file and return the list of engine results.
   *
   * @param qtaPath path to the QTA file
   * @return list of engine results
   * @throws IOException if the file cannot be read
   */
  private List<EngineResult> runScenario(String qtaPath) throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "BAU", progress -> {});
    return results.collect(Collectors.toList());
  }

  /**
   * Assert that energy consumption matches 100 * (year - 2020) kwh for each
   * year in the simulation.
   *
   * @param resultsList the list of engine results
   * @param label description of the scenario for assertion messages
   */
  private void assertExpectedEnergy(List<EngineResult> resultsList, String label) {
    for (int year = 2021; year <= 2030; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year,
          APPLICATION, SUBSTANCE);
      assertNotNull(result, label + ": should have result for year " + year);
      double actual = result.getEnergyConsumption().getValue().doubleValue();
      double expected = 100.0 * (year - 2020);
      assertEquals(expected, actual, 0.0001,
          label + ": year " + year + " energy consumption should be " + expected
          + " kwh but was " + actual);
    }
  }

  /**
   * Baseline test: kwh / unit without a year qualifier.
   * This should produce 100 * (year - 2020) kwh each year.
   */
  @Test
  public void testKwhUnitWithoutYearQualifier() throws IOException {
    List<EngineResult> resultsList = runScenario(
        "../examples/equals_kwh_unit_no_year.qta");
    assertExpectedEnergy(resultsList, "kwh/unit no year qualifier");
  }

  /**
   * Bug reproduction test: kwh / unit with a year qualifier.
   * This should produce the same energy as the baseline (100 * (year - 2020))
   * but is suspected to fail due to a bug.
   */
  @Test
  public void testKwhUnitWithYearQualifier() throws IOException {
    List<EngineResult> resultsList = runScenario(
        "../examples/equals_kwh_unit_year_2021.qta");
    assertExpectedEnergy(resultsList, "kwh/unit with year 2021 qualifier");
  }

  /**
   * Comparison test: initial charge with a year qualifier.
   * Suspected to pass (produce 100 * (year - 2020) kwh).
   */
  @Test
  public void testInitialChargeWithYearQualifier() throws IOException {
    List<EngineResult> resultsList = runScenario(
        "../examples/initial_charge_year_2021.qta");
    assertExpectedEnergy(resultsList, "initial charge with year 2021 qualifier");
  }
}
