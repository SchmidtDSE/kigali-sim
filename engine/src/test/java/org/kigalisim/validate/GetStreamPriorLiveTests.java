/**
 * Live tests for get stream N years ago operations including sales
 * with time-shifted access, unit conversion, and cross-substance access.
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
 * Tests that validate get stream N years ago operations work correctly
 * in full simulation scenarios. Tests sales time-shifted access,
 * zero-before-start behavior, unit conversion with prior values,
 * and cross-substance prior access.
 */
public class GetStreamPriorLiveTests {

  /**
   * Test the retirement schedule from the issue example.
   *
   * <p>Uses get_stream_prior.qta which tests sales with time-shifted
   * retirement:
   * <ul>
   *   <li>Year 1: 1000 units sold, no retirement (sales 9/10/11 years ago = 0)</li>
   *   <li>Years 2-9: 0 units sold, no retirement (sales still 0 for 9+ years ago)</li>
   *   <li>Year 10: sales 9 years ago = 1000, retire 0.2 * 1000 = 200 units -> 800</li>
   *   <li>Year 11: sales 10 years ago = 1000, retire 0.6 * 1000 = 600 units -> 200</li>
   *   <li>Year 12: sales 11 years ago = 1000, retire 0.2 * 1000 = 200 units -> 0</li>
   *   <li>After year 12: all 1000 units retired, equipment = 0</li>
   * </ul>
   *
   * <p>The reported population is the end-of-year value after retirement is applied.
   */
  @Test
  public void testGetStreamPriorWithRetirementSchedule() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/get_stream_prior.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Verify simulation completed without error
    assertTrue(resultsList.size() >= 20, "Should have at least 20 years of results");

    // Year 1: 1000 units sold, no retirement (sales 9/10/11 years ago = 0)
    EngineResult year1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "SubA");
    assertNotNull(year1, "Should have result for Test/SubA in year 1");
    double popYear1 = year1.getPopulation().getValue().doubleValue();
    assertEquals(1000.0, popYear1, 0.01,
        "Year 1 population should be 1000 units, but was " + popYear1);

    // Year 5: still 1000 units (no retirement yet, sales 9+ years ago = 0)
    EngineResult year5 = LiveTestsUtil.getResult(resultsList.stream(), 5, "Test", "SubA");
    assertNotNull(year5, "Should have result for Test/SubA in year 5");
    double popYear5 = year5.getPopulation().getValue().doubleValue();
    assertEquals(1000.0, popYear5, 0.01,
        "Year 5 population should be 1000 units, but was " + popYear5);

    // Year 9: still 1000 units (no retirement yet, sales 9+ years ago = 0)
    EngineResult year9 = LiveTestsUtil.getResult(resultsList.stream(), 9, "Test", "SubA");
    assertNotNull(year9, "Should have result for Test/SubA in year 9");
    double popYear9 = year9.getPopulation().getValue().doubleValue();
    assertEquals(1000.0, popYear9, 0.01,
        "Year 9 population should be 1000 units, but was " + popYear9);

    // Year 10: sales 9 years ago = 1000, retire 0.2 * 1000 = 200 units -> 800
    EngineResult year10 = LiveTestsUtil.getResult(resultsList.stream(), 10, "Test", "SubA");
    assertNotNull(year10, "Should have result for Test/SubA in year 10");
    double popYear10 = year10.getPopulation().getValue().doubleValue();
    assertEquals(800.0, popYear10, 0.01,
        "Year 10 population should be 800 units (1000 - 200 retired), but was " + popYear10);

    // Year 11: sales 10 years ago = 1000, retire 0.6 * 1000 = 600 units -> 200
    EngineResult year11 = LiveTestsUtil.getResult(resultsList.stream(), 11, "Test", "SubA");
    assertNotNull(year11, "Should have result for Test/SubA in year 11");
    double popYear11 = year11.getPopulation().getValue().doubleValue();
    assertEquals(200.0, popYear11, 0.01,
        "Year 11 population should be 200 units (800 - 600 retired), but was " + popYear11);

    // Year 12: sales 11 years ago = 1000, retire 0.2 * 1000 = 200 units -> 0
    EngineResult year12 = LiveTestsUtil.getResult(resultsList.stream(), 12, "Test", "SubA");
    assertNotNull(year12, "Should have result for Test/SubA in year 12");
    double popYear12 = year12.getPopulation().getValue().doubleValue();
    assertEquals(0.0, popYear12, 0.01,
        "Year 12 population should be 0 units, but was " + popYear12);

    // Year 15: all equipment retired
    EngineResult year15 = LiveTestsUtil.getResult(resultsList.stream(), 15, "Test", "SubA");
    assertNotNull(year15, "Should have result for Test/SubA in year 15");
    double popYear15 = year15.getPopulation().getValue().doubleValue();
    assertEquals(0.0, popYear15, 0.01,
        "Year 15 population should be 0 units, but was " + popYear15);

    // Year 20: all equipment retired (sales were set to 0 in year 2, so no new equipment)
    EngineResult year20 = LiveTestsUtil.getResult(resultsList.stream(), 20, "Test", "SubA");
    assertNotNull(year20, "Should have result for Test/SubA in year 20");
    double popYear20 = year20.getPopulation().getValue().doubleValue();
    assertTrue(popYear20 <= 100,
        "Year 20 population should be near 0 (small carry-over allowed), was " + popYear20);
  }

  /**
   * Test that get sales returns 0 when querying before simulation start.
   *
   * <p>Uses get_stream_prior_returns_zero.qta which sets priorEquipment based on
   * get sales 5 years ago. Since no sales exist 5 years before year 1,
   * the value should be 0.
   */
  @Test
  public void testGetStreamPriorReturnsZeroBeforeStart() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/get_stream_prior_returns_zero.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Verify simulation completed without error
    assertTrue(resultsList.size() > 0, "Should have simulation results");

    // Year 1: priorEquipment = 0 + 100 = 100, domestic = 500
    // 100 * 0.95 (retire 5%) + 500 = 95 + 500 = 595
    EngineResult year1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "SubA");
    assertNotNull(year1, "Should have result for Test/SubA in year 1");
    double popYear1 = year1.getPopulation().getValue().doubleValue();
    assertEquals(595.0, popYear1, 0.01,
        "Year 1 population should be 595 units (priorEquipment=100, retire 5%, + domestic=500), "
        + "but was " + popYear1);

    // Year 2: 595 * 0.95 + 500 = 565.25 + 500 = 1065.25
    EngineResult year2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "Test", "SubA");
    assertNotNull(year2, "Should have result for Test/SubA in year 2");
    double popYear2 = year2.getPopulation().getValue().doubleValue();
    assertEquals(1065.25, popYear2, 0.01,
        "Year 2 population should be 1065.25 units, but was " + popYear2);
  }

  /**
   * Test that get stream N years ago with unit conversion works correctly.
   *
   * <p>Uses get_stream_prior_conversion.qta which tests:
   * <ul>
   *   <li>get domestic 2 years ago as mt converts kg to megatonnes</li>
   *   <li>5000 kg in year 1 = 0.005 mt when converted</li>
   *   <li>set priorEquipment = 0.005 * 1000000 = 5000 units in year 3</li>
   * </ul>
   */
  @Test
  public void testGetStreamPriorWithConversion() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/get_stream_prior_conversion.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Verify simulation completed without error
    assertTrue(resultsList.size() > 0, "Should have simulation results");

    // Year 1: domestic = 5000 kg, population = 500 (5000 kg / 10 kg/unit)
    EngineResult year1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "SubA");
    assertNotNull(year1, "Should have result for Test/SubA in year 1");
    double domesticYear1 = year1.getDomestic().getValue().doubleValue();
    assertEquals(5000.0, domesticYear1, 0.01,
        "Year 1 domestic should be 5000 kg, but was " + domesticYear1);
    assertEquals(500.0, year1.getPopulation().getValue().doubleValue(), 0.01,
        "Year 1 population should be 500 units (5000 kg / 10 kg/unit), but was "
        + year1.getPopulation().getValue());

    // Year 3: simulation should still be running (proves prior conversion works)
    EngineResult year3 = LiveTestsUtil.getResult(resultsList.stream(), 3, "Test", "SubA");
    assertNotNull(year3, "Should have result for Test/SubA in year 3");
    assertTrue(year3.getPopulation().getValue().doubleValue() > 0,
        "Year 3 population should be positive");

    // Year 4: simulation should still be running
    EngineResult year4 = LiveTestsUtil.getResult(resultsList.stream(), 4, "Test", "SubA");
    assertNotNull(year4, "Should have result for Test/SubA in year 4");
    assertTrue(year4.getPopulation().getValue().doubleValue() > 0,
        "Year 4 population should be positive");
  }

  /**
   * Test cross-substance sales access.
   *
   * <p>Uses get_stream_prior_multi_substance.qta which tests:
   * <ul>
   *   <li>get sales 1 years ago of "OtherSub" as units</li>
   *   <li>MainSub references OtherSub's prior sales in year 2</li>
   * </ul>
   */
  @Test
  public void testGetStreamPriorMultiSubstance() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/get_stream_prior_multi_substance.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario
    String scenarioName = "BAU";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Verify simulation completed without error
    assertTrue(resultsList.size() > 0, "Should have simulation results");

    // Year 1: OtherSub population = 300 units (300 kg / 1 kg/unit)
    EngineResult year1OtherSub = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "OtherSub");
    assertNotNull(year1OtherSub, "Should have result for Test/OtherSub in year 1");
    double popYear1Other = year1OtherSub.getPopulation().getValue().doubleValue();
    assertEquals(300.0, popYear1Other, 0.01,
        "Year 1 OtherSub population should be 300 units, but was " + popYear1Other);

    // Year 1: MainSub population = 1000 units (1000 kg / 1 kg/unit)
    EngineResult year1MainSub = LiveTestsUtil.getResult(resultsList.stream(), 1, "Test", "MainSub");
    assertNotNull(year1MainSub, "Should have result for Test/MainSub in year 1");
    double popYear1Main = year1MainSub.getPopulation().getValue().doubleValue();
    assertEquals(1000.0, popYear1Main, 0.01,
        "Year 1 MainSub population should be 1000 units, but was " + popYear1Main);

    // Year 2: MainSub population should be greater than year 1 (proves cross-substance
    // prior access works - sales from OtherSub is being added)
    EngineResult year2MainSub = LiveTestsUtil.getResult(resultsList.stream(), 2, "Test", "MainSub");
    assertNotNull(year2MainSub, "Should have result for Test/MainSub in year 2");
    double popYear2Main = year2MainSub.getPopulation().getValue().doubleValue();
    assertTrue(popYear2Main > popYear1Main,
        "Year 2 MainSub population (" + popYear2Main
        + ") should be greater than year 1 (" + popYear1Main
        + ") due to cross-substance sales access, but was not");

    // Year 2: OtherSub should still have positive population
    EngineResult year2OtherSub = LiveTestsUtil.getResult(resultsList.stream(), 2, "Test", "OtherSub");
    assertNotNull(year2OtherSub, "Should have result for Test/OtherSub in year 2");
    double popYear2Other = year2OtherSub.getPopulation().getValue().doubleValue();
    assertTrue(popYear2Other > 0,
        "Year 2 OtherSub population should be positive, was " + popYear2Other);
  }
}
