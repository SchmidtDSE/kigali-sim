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
    EngineResult bigChange2031 = LiveTestsUtil.getResult(
        bigChangeList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bigChange2031, "Should have Big Change result for 2031");
    final double bigChangeDomestic = bigChange2031.getDomestic().getValue().doubleValue();

    // Run Permit scenario
    Stream<EngineResult> permitResults = KigaliSimFacade.runScenario(
        program, "Permit", progress -> {});
    List<EngineResult> permitList = permitResults.collect(Collectors.toList());
    EngineResult permit2031 = LiveTestsUtil.getResult(
        permitList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(permit2031, "Should have Permit result for 2031");
    final double permitDomestic = permit2031.getDomestic().getValue().doubleValue();

    // Run Before scenario (Big Change then Permit)
    Stream<EngineResult> beforeResults = KigaliSimFacade.runScenario(
        program, "Before", progress -> {});
    List<EngineResult> beforeList = beforeResults.collect(Collectors.toList());
    EngineResult before2031 = LiveTestsUtil.getResult(
        beforeList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(before2031, "Should have Before result for 2031");
    final double beforeDomestic = before2031.getDomestic().getValue().doubleValue();

    // Run After scenario (Permit then Big Change)
    Stream<EngineResult> afterResults = KigaliSimFacade.runScenario(
        program, "After", progress -> {});
    List<EngineResult> afterList = afterResults.collect(Collectors.toList());
    EngineResult after2031 = LiveTestsUtil.getResult(
        afterList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(after2031, "Should have After result for 2031");
    final double afterDomestic = after2031.getDomestic().getValue().doubleValue();

    // Assert: Permit should have lower domestic consumption than Big Change in 2031
    assertTrue(permitDomestic < bigChangeDomestic,
        String.format("Permit domestic (%.2f kg) should be lower than Big Change domestic (%.2f kg) in 2031",
            permitDomestic, bigChangeDomestic));

    // Assert: Permit should be within 0.01 mt (10 kg) of Before in 2031
    double tolerance = 10.0; // 0.01 mt = 10 kg
    assertEquals(permitDomestic, beforeDomestic, tolerance,
        String.format("Permit domestic (%.2f kg) should be within 10 kg of Before domestic (%.2f kg) in 2031",
            permitDomestic, beforeDomestic));

    // Assert: After should have lower domestic consumption than Permit in 2031
    assertTrue(afterDomestic < permitDomestic,
        String.format("After domestic (%.2f kg) should be lower than Permit domestic (%.2f kg) in 2031",
            afterDomestic, permitDomestic));

    // Assert: All scenarios should have non-negative domestic values at year 2035
    // Year 2035 is critical because Permit policy caps sales to 0 mt at this point
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(
        program, "BAU", progress -> {});
    List<EngineResult> bauList = bauResults.collect(Collectors.toList());
    assertDomesticNonNegative(bauList, 2035, "BAU");

    assertDomesticNonNegative(bigChangeList, 2035, "Big Change");
    assertDomesticNonNegative(permitList, 2035, "Permit");
    assertDomesticNonNegative(beforeList, 2035, "Before");
    assertDomesticNonNegative(afterList, 2035, "After");
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
    EngineResult bigChange2031 = LiveTestsUtil.getResult(
        bigChangeList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bigChange2031, "Should have Big Change result for 2031");
    final double bigChangeDomestic = bigChange2031.getDomestic().getValue().doubleValue();

    // Run Permit scenario
    Stream<EngineResult> permitResults = KigaliSimFacade.runScenario(
        program, "Permit", progress -> {});
    List<EngineResult> permitList = permitResults.collect(Collectors.toList());
    EngineResult permit2031 = LiveTestsUtil.getResult(
        permitList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(permit2031, "Should have Permit result for 2031");
    final double permitDomestic = permit2031.getDomestic().getValue().doubleValue();

    // Run Before scenario (Big Change then Permit)
    Stream<EngineResult> beforeResults = KigaliSimFacade.runScenario(
        program, "Before", progress -> {});
    List<EngineResult> beforeList = beforeResults.collect(Collectors.toList());
    EngineResult before2031 = LiveTestsUtil.getResult(
        beforeList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(before2031, "Should have Before result for 2031");
    final double beforeDomestic = before2031.getDomestic().getValue().doubleValue();

    // Run After scenario (Permit then Big Change)
    Stream<EngineResult> afterResults = KigaliSimFacade.runScenario(
        program, "After", progress -> {});
    List<EngineResult> afterList = afterResults.collect(Collectors.toList());
    EngineResult after2031 = LiveTestsUtil.getResult(
        afterList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(after2031, "Should have After result for 2031");
    final double afterDomestic = after2031.getDomestic().getValue().doubleValue();

    // Assert: Permit should have lower domestic consumption than Big Change in 2031
    assertTrue(permitDomestic < bigChangeDomestic,
        String.format("Permit domestic (%.2f kg) should be lower than Big Change domestic (%.2f kg) in 2031",
            permitDomestic, bigChangeDomestic));

    // Assert: Permit should be within 0.01 mt (10 kg) of Before in 2031
    double tolerance = 10.0; // 0.01 mt = 10 kg
    assertEquals(permitDomestic, beforeDomestic, tolerance,
        String.format("Permit domestic (%.2f kg) should be within 10 kg of Before domestic (%.2f kg) in 2031",
            permitDomestic, beforeDomestic));

    // Assert: After should have lower domestic consumption than Permit in 2031
    assertTrue(afterDomestic < permitDomestic,
        String.format("After domestic (%.2f kg) should be lower than Permit domestic (%.2f kg) in 2031",
            afterDomestic, permitDomestic));

    // Assert: All scenarios should have non-negative domestic values at year 2035
    // Year 2035 is critical because Permit policy caps sales to 0 mt at this point
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(
        program, "BAU", progress -> {});
    List<EngineResult> bauList = bauResults.collect(Collectors.toList());
    assertDomesticNonNegative(bauList, 2035, "BAU");

    assertDomesticNonNegative(bigChangeList, 2035, "Big Change");
    assertDomesticNonNegative(permitList, 2035, "Permit");
    assertDomesticNonNegative(beforeList, 2035, "Before");
    assertDomesticNonNegative(afterList, 2035, "After");
  }

  /**
   * Test that multiple set commands stack correctly, with the last policy determining the value.
   * This validates that when two policies both set a stream to different values,
   * the last policy applied in the stacking order wins.
   */
  @Test
  public void testDoubleSet() throws IOException {
    String qtaPath = "../examples/stacking_double_set.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(
        program, "BAU", progress -> {});
    List<EngineResult> bauList = bauResults.collect(Collectors.toList());
    EngineResult bau2 = LiveTestsUtil.getResult(
        bauList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bau2, "Should have BAU result for year 2");
    final double bauDomestic = bau2.getDomestic().getValue().doubleValue();

    // Run Small Set scenario
    Stream<EngineResult> smallSetResults = KigaliSimFacade.runScenario(
        program, "Small Set", progress -> {});
    List<EngineResult> smallSetList = smallSetResults.collect(Collectors.toList());
    EngineResult smallSet2 = LiveTestsUtil.getResult(
        smallSetList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(smallSet2, "Should have Small Set result for year 2");
    final double smallSetDomestic = smallSet2.getDomestic().getValue().doubleValue();

    // Run Large Set scenario
    Stream<EngineResult> largeSetResults = KigaliSimFacade.runScenario(
        program, "Large Set", progress -> {});
    List<EngineResult> largeSetList = largeSetResults.collect(Collectors.toList());
    EngineResult largeSet2 = LiveTestsUtil.getResult(
        largeSetList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(largeSet2, "Should have Large Set result for year 2");
    final double largeSetDomestic = largeSet2.getDomestic().getValue().doubleValue();

    // Run Combined Increase scenario (Small Set then Large Set)
    Stream<EngineResult> combinedIncreaseResults = KigaliSimFacade.runScenario(
        program, "Combined Increase", progress -> {});
    List<EngineResult> combinedIncreaseList = combinedIncreaseResults.collect(Collectors.toList());
    EngineResult combinedIncrease2 = LiveTestsUtil.getResult(
        combinedIncreaseList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(combinedIncrease2, "Should have Combined Increase result for year 2");
    final double combinedIncreaseDomestic = combinedIncrease2.getDomestic().getValue().doubleValue();

    // Run Combined Decrease scenario (Large Set then Small Set)
    Stream<EngineResult> combinedDecreaseResults = KigaliSimFacade.runScenario(
        program, "Combined Decrease", progress -> {});
    List<EngineResult> combinedDecreaseList = combinedDecreaseResults.collect(Collectors.toList());
    EngineResult combinedDecrease2 = LiveTestsUtil.getResult(
        combinedDecreaseList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(combinedDecrease2, "Should have Combined Decrease result for year 2");
    final double combinedDecreaseDomestic = combinedDecrease2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.01; // kg

    assertEquals(10.0, bauDomestic, tolerance, "BAU should have 10 kg in year 2");
    assertEquals(15.0, smallSetDomestic, tolerance, "Small Set should have 15 kg in year 2");
    assertEquals(20.0, largeSetDomestic, tolerance, "Large Set should have 20 kg in year 2");
    assertEquals(20.0, combinedIncreaseDomestic, tolerance,
        "Combined Increase should have 20 kg in year 2");
    assertEquals(15.0, combinedDecreaseDomestic, tolerance,
        "Combined Decrease should have 15 kg in year 2");
  }

  /**
   * Assert that domestic kg value is non-negative for a scenario result.
   *
   * @param resultsList The list of all results from the scenario
   * @param year The year to check
   * @param scenarioName The scenario name for error messages
   */
  private void assertDomesticNonNegative(
      List<EngineResult> resultsList, int year, String scenarioName) {
    EngineResult result = LiveTestsUtil.getResult(
        resultsList.stream(), year, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(result,
        String.format("Should have %s result for %d", scenarioName, year));
    double domestic = result.getDomestic().getValue().doubleValue();
    assertTrue(domestic >= 0.0,
        String.format("%s domestic should be non-negative at year %d, but was %.4f kg",
            scenarioName, year, domestic));
  }
}
