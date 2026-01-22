/**
 * Live tests for policy stacking behavior.
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
 * Tests that validate policy stacking behavior in QTA files.
 */
public class StackingLiveTests {

  /**
   * Test policy stacking order affects outcomes with percentage-based changes.
   * This validates that applying policies in different orders produces different results,
   * and that caps applied after changes behave differently than caps applied before changes.
   */
  @Test
  public void testPolicyStackingOrderPercent() throws IOException {
    String qtaPath = "../examples/stacking.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run Big Change scenario
    Stream<EngineResult> bigChangeResults = KigaliSimFacade.runScenario(
        program, "Big Change", progress -> {});
    List<EngineResult> bigChangeList = bigChangeResults.collect(Collectors.toList());
    EngineResult bigChange2034 = LiveTestsUtil.getResult(
        bigChangeList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bigChange2034, "Should have Big Change result for 2034");
    double bigChangeDomestic = bigChange2034.getDomestic().getValue().doubleValue();

    // Run Permit scenario
    Stream<EngineResult> permitResults = KigaliSimFacade.runScenario(
        program, "Permit", progress -> {});
    List<EngineResult> permitList = permitResults.collect(Collectors.toList());
    EngineResult permit2034 = LiveTestsUtil.getResult(
        permitList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(permit2034, "Should have Permit result for 2034");
    double permitDomestic = permit2034.getDomestic().getValue().doubleValue();

    // Run Before scenario (Big Change then Permit)
    Stream<EngineResult> beforeResults = KigaliSimFacade.runScenario(
        program, "Before", progress -> {});
    List<EngineResult> beforeList = beforeResults.collect(Collectors.toList());
    EngineResult before2034 = LiveTestsUtil.getResult(
        beforeList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(before2034, "Should have Before result for 2034");
    double beforeDomestic = before2034.getDomestic().getValue().doubleValue();

    // Run After scenario (Permit then Big Change)
    Stream<EngineResult> afterResults = KigaliSimFacade.runScenario(
        program, "After", progress -> {});
    List<EngineResult> afterList = afterResults.collect(Collectors.toList());
    EngineResult after2034 = LiveTestsUtil.getResult(
        afterList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(after2034, "Should have After result for 2034");
    double afterDomestic = after2034.getDomestic().getValue().doubleValue();

    // Assert: Permit should have lower domestic consumption than Big Change in 2034
    assertTrue(permitDomestic < bigChangeDomestic,
        String.format("Permit domestic (%.2f kg) should be lower than Big Change domestic (%.2f kg) in 2034",
            permitDomestic, bigChangeDomestic));

    // Assert: Permit should be within 0.01 mt (10 kg) of Before in 2034
    double tolerance = 10.0; // 0.01 mt = 10 kg
    assertEquals(permitDomestic, beforeDomestic, tolerance,
        String.format("Permit domestic (%.2f kg) should be within 10 kg of Before domestic (%.2f kg) in 2034",
            permitDomestic, beforeDomestic));

    // Assert: After should have lower domestic consumption than Permit in 2034
    assertTrue(afterDomestic < permitDomestic,
        String.format("After domestic (%.2f kg) should be lower than Permit domestic (%.2f kg) in 2034",
            afterDomestic, permitDomestic));
  }

  /**
   * Test policy stacking order affects outcomes with explicit (absolute) changes.
   * This validates that applying policies in different orders produces different results,
   * using explicit mt values instead of percentages.
   */
  @Test
  public void testPolicyStackingOrderExplicit() throws IOException {
    String qtaPath = "../examples/stacking_explicit.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run Big Change scenario
    Stream<EngineResult> bigChangeResults = KigaliSimFacade.runScenario(
        program, "Big Change", progress -> {});
    List<EngineResult> bigChangeList = bigChangeResults.collect(Collectors.toList());
    EngineResult bigChange2034 = LiveTestsUtil.getResult(
        bigChangeList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bigChange2034, "Should have Big Change result for 2034");
    double bigChangeDomestic = bigChange2034.getDomestic().getValue().doubleValue();

    // Run Permit scenario
    Stream<EngineResult> permitResults = KigaliSimFacade.runScenario(
        program, "Permit", progress -> {});
    List<EngineResult> permitList = permitResults.collect(Collectors.toList());
    EngineResult permit2034 = LiveTestsUtil.getResult(
        permitList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(permit2034, "Should have Permit result for 2034");
    double permitDomestic = permit2034.getDomestic().getValue().doubleValue();

    // Run Before scenario (Big Change then Permit)
    Stream<EngineResult> beforeResults = KigaliSimFacade.runScenario(
        program, "Before", progress -> {});
    List<EngineResult> beforeList = beforeResults.collect(Collectors.toList());
    EngineResult before2034 = LiveTestsUtil.getResult(
        beforeList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(before2034, "Should have Before result for 2034");
    double beforeDomestic = before2034.getDomestic().getValue().doubleValue();

    // Run After scenario (Permit then Big Change)
    Stream<EngineResult> afterResults = KigaliSimFacade.runScenario(
        program, "After", progress -> {});
    List<EngineResult> afterList = afterResults.collect(Collectors.toList());
    EngineResult after2034 = LiveTestsUtil.getResult(
        afterList.stream(), 2034, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(after2034, "Should have After result for 2034");
    double afterDomestic = after2034.getDomestic().getValue().doubleValue();

    // Assert: Permit should have lower domestic consumption than Big Change in 2034
    assertTrue(permitDomestic < bigChangeDomestic,
        String.format("Permit domestic (%.2f kg) should be lower than Big Change domestic (%.2f kg) in 2034",
            permitDomestic, bigChangeDomestic));

    // Assert: Permit should be within 0.01 mt (10 kg) of Before in 2034
    double tolerance = 10.0; // 0.01 mt = 10 kg
    assertEquals(permitDomestic, beforeDomestic, tolerance,
        String.format("Permit domestic (%.2f kg) should be within 10 kg of Before domestic (%.2f kg) in 2034",
            permitDomestic, beforeDomestic));

    // Assert: After should have lower domestic consumption than Permit in 2034
    assertTrue(afterDomestic < permitDomestic,
        String.format("After domestic (%.2f kg) should be lower than Permit domestic (%.2f kg) in 2034",
            afterDomestic, permitDomestic));
  }
}
