/**
 * Live tests for getStream operations including conversion and indirect conversion.
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
 * Tests that validate getStream operations work correctly.
 * Tests both getStreamConversion (get stream as units) and
 * getStreamIndirectConversion (get stream of "name" as units).
 */
public class GetStreamLiveTests {

  /**
   * Test basic getStreamConversion with direct stream access and unit conversion.
   * Uses get_stream_conversion.qta which tests:
   * <ul>
   *   <li>get equipment as units (in priorEquipment calculation)</li>
   * </ul>
   *
   * <p>Note: This test verifies that the get stream syntax works without causing parse errors.
   * It tests the basic case that already exists in reference_simplified.qta.
   */
  @Test
  public void testGetStreamConversion() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/get_stream_conversion.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario
    String scenarioName = "test";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: Base values set directly
    EngineResult year1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test app", "test substance");
    assertNotNull(year1, "Should have result for year 1");
    assertEquals(500.0, year1.getDomestic().getValue().doubleValue(), 0.0001,
        "Year 1 domestic should be 500 kg");
    assertEquals(300.0, year1.getImport().getValue().doubleValue(), 0.0001,
        "Year 1 import should be 300 kg");

    // Verify equipment tracking works with get equipment as units in priorEquipment calculation
    // The priorEquipment is set to get equipment as units * 2
    // This creates a feedback loop that multiplies equipment
    assertTrue(year1.getPopulation().getValue().doubleValue() > 0,
        "Equipment population should be positive in year 1");

    // Verify equipment continues in subsequent years
    EngineResult year2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test app", "test substance");
    assertNotNull(year2, "Should have result for year 2");
    assertTrue(year2.getPopulation().getValue().doubleValue() > 0,
        "Equipment population should be positive in year 2");
  }

  /**
   * Test getStreamIndirectConversion with cross-substance stream access.
   * Uses get_stream_indirect_conversion.qta which tests:
   * <ul>
   *   <li>get domestic of "substance a" as kg</li>
   *   <li>get domestic of "substance a" as mt</li>
   * </ul>
   */
  @Test
  public void testGetStreamIndirectConversion() throws IOException {
    String qtaPath = "../examples/get_stream_indirect_conversion.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "indirect test";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: substance a has 200 kg domestic
    EngineResult year1SubA = LiveTestsUtil.getResult(resultsList.stream(), 1,
        "multi substance app", "substance a");
    assertNotNull(year1SubA, "Should have result for substance a in year 1");
    assertEquals(200.0, year1SubA.getDomestic().getValue().doubleValue(), 0.0001,
        "Substance a domestic should be 200 kg in year 1");

    // Year 1: substance b domestic = (get domestic of "substance a" as kg) * 1.5 = 200 * 1.5 = 300 kg
    EngineResult year1SubB = LiveTestsUtil.getResult(resultsList.stream(), 1,
        "multi substance app", "substance b");
    assertNotNull(year1SubB, "Should have result for substance b in year 1");
    assertEquals(300.0, year1SubB.getDomestic().getValue().doubleValue(), 0.0001,
        "Substance b domestic should be 300 kg in year 1 (200 * 1.5)");

    // Year 2: substance b domestic = (get domestic of "substance a" as mt) * 1200
    // substance a year 2 domestic stays at 200 kg (no set for year 2) = 0.2 mt
    // substance b year 2 domestic = 0.2 * 1200 = 240 kg
    EngineResult year2SubA = LiveTestsUtil.getResult(resultsList.stream(), 2,
        "multi substance app", "substance a");
    assertEquals(200.0, year2SubA.getDomestic().getValue().doubleValue(), 0.0001,
        "Substance a domestic should still be 200 kg in year 2");

    EngineResult year2SubB = LiveTestsUtil.getResult(resultsList.stream(), 2,
        "multi substance app", "substance b");
    double domesticB2 = year2SubB.getDomestic().getValue().doubleValue();
    assertEquals(240.0, domesticB2, 1.0,
        "Substance b domestic in year 2 should be around 240 kg");
  }

  /**
   * Test mixed usage of getStreamConversion and getStreamIndirectConversion.
   * Uses get_stream_mixed.qta which tests:
   * <ul>
   *   <li>Both direct and indirect stream access in same substance</li>
   *   <li>get equipment of "substance" as units</li>
   * </ul>
   */
  @Test
  public void testGetStreamMixed() throws IOException {
    String qtaPath = "../examples/get_stream_mixed.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "mixed test";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: base substance has 1000 kg domestic, 500 kg import
    EngineResult year1Base = LiveTestsUtil.getResult(resultsList.stream(), 1,
        "complex app", "base substance alpha");
    assertNotNull(year1Base, "Should have result for base substance alpha in year 1");
    assertEquals(1000.0, year1Base.getDomestic().getValue().doubleValue(), 0.0001,
        "Base substance alpha domestic should be 1000 kg in year 1");

    // Derived substance domestic = get equipment of "base substance alpha" as units * 12 kg
    // Base equipment comes from sales (1500 kg) / initial charge (10 kg/unit)
    double baseEquipment = year1Base.getPopulation().getValue().doubleValue();

    EngineResult year1Derived = LiveTestsUtil.getResult(resultsList.stream(), 1,
        "complex app", "derived substance beta");
    double derivedDomestic = year1Derived.getDomestic().getValue().doubleValue();
    double expectedDomestic = baseEquipment * 12.0;
    assertEquals(expectedDomestic, derivedDomestic, 0.1,
        "Derived substance beta domestic should equal base equipment * 12 kg");

    // Year 2: uses both direct (get domestic as kg) and continuation from year 1
    EngineResult year2Derived = LiveTestsUtil.getResult(resultsList.stream(), 2,
        "complex app", "derived substance beta");
    double year2Domestic = year2Derived.getDomestic().getValue().doubleValue();
    // Year 2: get domestic as kg + 100 kg (domestic from year 1 + 100)
    double expectedYear2 = derivedDomestic + 100.0;
    assertEquals(expectedYear2, year2Domestic, 1.0,
        "Year 2 derived substance beta domestic should be year 1 domestic + 100 kg");
  }

  /**
   * Test that getStream operations work correctly with priorEquipment.
   * This validates that stream access can be used in equipment initialization.
   * This mirrors the pattern in reference_simplified.qta lines 34, 55, etc.
   */
  @Test
  public void testGetStreamWithPriorEquipment() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/get_stream_conversion.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario
    String scenarioName = "test";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Year 1: priorEquipment = get equipment as units * 2
    // This creates a feedback loop that multiplies equipment
    EngineResult year1 = LiveTestsUtil.getResult(resultsList.stream(), 1, "test app", "test substance");
    assertNotNull(year1, "Should have result for year 1");

    double equipment1 = year1.getPopulation().getValue().doubleValue();
    assertTrue(equipment1 > 0, "Equipment should be positive in year 1");

    // Verify equipment continues in subsequent years
    EngineResult year2 = LiveTestsUtil.getResult(resultsList.stream(), 2, "test app", "test substance");
    assertNotNull(year2, "Should have result for year 2");
    double equipment2 = year2.getPopulation().getValue().doubleValue();

    // Equipment should persist across years (with some retirement)
    assertTrue(equipment2 > 0,
        String.format("Equipment in year 2 (%.2f) should be positive", equipment2));
  }
}
