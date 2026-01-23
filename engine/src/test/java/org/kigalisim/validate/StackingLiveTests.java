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
   * Test interaction between change and cap commands with percentage-based changes.
   * This validates that applying policies in different orders produces different results,
   * and that caps applied after changes behave differently than caps applied before changes.
   */
  @Test
  public void testChangeAndCapPercent() throws IOException {
    String qtaPath = "../examples/stacking_change_and_cap_percent.qta";
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
   * Test interaction between change and cap commands with explicit (absolute) changes.
   * This validates that applying policies in different orders produces different results,
   * using explicit mt values instead of percentages for change operations.
   */
  @Test
  public void testChangeAndCapExplicit() throws IOException {
    String qtaPath = "../examples/stacking_change_and_cap_explicit.qta";
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
   * Test interaction between change and floor commands with percentage-based changes.
   * This validates that floor operations enforce minimum thresholds while change operations
   * apply relative increases, with stacking order determining whether the floor triggers.
   * This is the mirror-image of testChangeAndCapPercent, testing minimum enforcement
   * instead of maximum enforcement.
   */
  @Test
  public void testChangeAndFloor() throws IOException {
    String qtaPath = "../examples/stacking_change_and_floor.qta";
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

    // Run Floor Policy scenario
    Stream<EngineResult> floorResults = KigaliSimFacade.runScenario(
        program, "Floor Policy", progress -> {});
    List<EngineResult> floorList = floorResults.collect(Collectors.toList());
    EngineResult floor2031 = LiveTestsUtil.getResult(
        floorList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(floor2031, "Should have Floor Policy result for 2031");
    final double floorDomestic = floor2031.getDomestic().getValue().doubleValue();

    // Run Before scenario (Big Change then Floor Policy)
    Stream<EngineResult> beforeResults = KigaliSimFacade.runScenario(
        program, "Before", progress -> {});
    List<EngineResult> beforeList = beforeResults.collect(Collectors.toList());
    EngineResult before2031 = LiveTestsUtil.getResult(
        beforeList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(before2031, "Should have Before result for 2031");
    final double beforeDomestic = before2031.getDomestic().getValue().doubleValue();

    // Run After scenario (Floor Policy then Big Change)
    Stream<EngineResult> afterResults = KigaliSimFacade.runScenario(
        program, "After", progress -> {});
    List<EngineResult> afterList = afterResults.collect(Collectors.toList());
    EngineResult after2031 = LiveTestsUtil.getResult(
        afterList.stream(), 2031, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(after2031, "Should have After result for 2031");
    final double afterDomestic = after2031.getDomestic().getValue().doubleValue();

    // Assert: Floor Policy should have higher domestic consumption than Big Change in 2031
    // This is the mirror of cap behavior - floor enforces minimums, increasing values
    assertTrue(floorDomestic > bigChangeDomestic,
        String.format(
            "Floor Policy domestic (%.2f kg) should be higher than "
            + "Big Change domestic (%.2f kg) in 2031",
            floorDomestic, bigChangeDomestic));

    // Assert: Floor Policy should be within 0.01 mt (10 kg) of Before in 2031
    // When change is applied first, floor may still trigger if change didn't reach minimum
    double tolerance = 10.0; // 0.01 mt = 10 kg
    assertEquals(floorDomestic, beforeDomestic, tolerance,
        String.format(
            "Floor Policy domestic (%.2f kg) should be within 10 kg of "
            + "Before domestic (%.2f kg) in 2031",
            floorDomestic, beforeDomestic));

    // Assert: After should have higher domestic consumption than Floor Policy in 2031
    // Floor establishes minimum, then change increases further
    assertTrue(afterDomestic > floorDomestic,
        String.format(
            "After domestic (%.2f kg) should be higher than "
            + "Floor Policy domestic (%.2f kg) in 2031",
            afterDomestic, floorDomestic));

    // Assert: All scenarios should have non-negative domestic values at year 2035
    // Even with floor operations forcing increases, values should remain valid
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(
        program, "BAU", progress -> {});
    List<EngineResult> bauList = bauResults.collect(Collectors.toList());
    assertDomesticNonNegative(bauList, 2035, "BAU");

    assertDomesticNonNegative(bigChangeList, 2035, "Big Change");
    assertDomesticNonNegative(floorList, 2035, "Floor Policy");
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
   * Test that multiple cap commands stack correctly, with the most restrictive cap winning.
   * This validates that when two policies both cap a stream to different values,
   * the most restrictive (lowest) cap takes effect regardless of application order.
   */
  @Test
  public void testDoubleCap() throws IOException {
    String qtaPath = "../examples/stacking_double_cap.qta";
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

    // Run Top Cap scenario
    Stream<EngineResult> topCapResults = KigaliSimFacade.runScenario(
        program, "Top Cap", progress -> {});
    List<EngineResult> topCapList = topCapResults.collect(Collectors.toList());
    EngineResult topCap2 = LiveTestsUtil.getResult(
        topCapList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(topCap2, "Should have Top Cap result for year 2");
    final double topCapDomestic = topCap2.getDomestic().getValue().doubleValue();

    // Run Bottom Cap scenario
    Stream<EngineResult> bottomCapResults = KigaliSimFacade.runScenario(
        program, "Bottom Cap", progress -> {});
    List<EngineResult> bottomCapList = bottomCapResults.collect(Collectors.toList());
    EngineResult bottomCap2 = LiveTestsUtil.getResult(
        bottomCapList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bottomCap2, "Should have Bottom Cap result for year 2");
    final double bottomCapDomestic = bottomCap2.getDomestic().getValue().doubleValue();

    // Run Combined Strict scenario (Top Cap then Bottom Cap)
    Stream<EngineResult> combinedStrictResults = KigaliSimFacade.runScenario(
        program, "Combined Strict", progress -> {});
    List<EngineResult> combinedStrictList = combinedStrictResults.collect(Collectors.toList());
    EngineResult combinedStrict2 = LiveTestsUtil.getResult(
        combinedStrictList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(combinedStrict2, "Should have Combined Strict result for year 2");
    final double combinedStrictDomestic = combinedStrict2.getDomestic().getValue().doubleValue();

    // Run Combined Loose scenario (Bottom Cap then Top Cap)
    Stream<EngineResult> combinedLooseResults = KigaliSimFacade.runScenario(
        program, "Combined Loose", progress -> {});
    List<EngineResult> combinedLooseList = combinedLooseResults.collect(Collectors.toList());
    EngineResult combinedLoose2 = LiveTestsUtil.getResult(
        combinedLooseList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(combinedLoose2, "Should have Combined Loose result for year 2");
    final double combinedLooseDomestic = combinedLoose2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.01; // kg

    assertEquals(10.0, bauDomestic, tolerance, "BAU should have 10 kg in year 2");
    assertEquals(5.0, topCapDomestic, tolerance, "Top Cap should have 5 kg in year 2");
    assertEquals(2.0, bottomCapDomestic, tolerance, "Bottom Cap should have 2 kg in year 2");
    assertEquals(2.0, combinedStrictDomestic, tolerance,
        "Combined Strict should have 2 kg in year 2");
    assertEquals(2.0, combinedLooseDomestic, tolerance,
        "Combined Loose should have 2 kg in year 2");
  }

  /**
   * Test that multiple floor commands stack correctly, with the least restrictive floor winning.
   * This validates that when two policies both floor a stream to different values,
   * the least restrictive (highest) floor takes effect regardless of application order.
   */
  @Test
  public void testDoubleFloor() throws IOException {
    String qtaPath = "../examples/stacking_double_floor.qta";
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

    // Run Top Floor scenario
    Stream<EngineResult> topFloorResults = KigaliSimFacade.runScenario(
        program, "Top Floor", progress -> {});
    List<EngineResult> topFloorList = topFloorResults.collect(Collectors.toList());
    EngineResult topFloor2 = LiveTestsUtil.getResult(
        topFloorList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(topFloor2, "Should have Top Floor result for year 2");
    final double topFloorDomestic = topFloor2.getDomestic().getValue().doubleValue();

    // Run Bottom Floor scenario
    Stream<EngineResult> bottomFloorResults = KigaliSimFacade.runScenario(
        program, "Bottom Floor", progress -> {});
    List<EngineResult> bottomFloorList = bottomFloorResults.collect(Collectors.toList());
    EngineResult bottomFloor2 = LiveTestsUtil.getResult(
        bottomFloorList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bottomFloor2, "Should have Bottom Floor result for year 2");
    final double bottomFloorDomestic = bottomFloor2.getDomestic().getValue().doubleValue();

    // Run Combined Strict scenario (Top Floor then Bottom Floor)
    Stream<EngineResult> combinedStrictResults = KigaliSimFacade.runScenario(
        program, "Combined Strict", progress -> {});
    List<EngineResult> combinedStrictList = combinedStrictResults.collect(Collectors.toList());
    EngineResult combinedStrict2 = LiveTestsUtil.getResult(
        combinedStrictList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(combinedStrict2, "Should have Combined Strict result for year 2");
    final double combinedStrictDomestic = combinedStrict2.getDomestic().getValue().doubleValue();

    // Run Combined Loose scenario (Bottom Floor then Top Floor)
    Stream<EngineResult> combinedLooseResults = KigaliSimFacade.runScenario(
        program, "Combined Loose", progress -> {});
    List<EngineResult> combinedLooseList = combinedLooseResults.collect(Collectors.toList());
    EngineResult combinedLoose2 = LiveTestsUtil.getResult(
        combinedLooseList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(combinedLoose2, "Should have Combined Loose result for year 2");
    final double combinedLooseDomestic = combinedLoose2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.01; // kg

    assertEquals(10.0, bauDomestic, tolerance, "BAU should have 10 kg in year 2");
    assertEquals(50.0, topFloorDomestic, tolerance, "Top Floor should have 50 kg in year 2");
    assertEquals(20.0, bottomFloorDomestic, tolerance, "Bottom Floor should have 20 kg in year 2");
    assertEquals(50.0, combinedStrictDomestic, tolerance,
        "Combined Strict should have 50 kg in year 2");
    assertEquals(50.0, combinedLooseDomestic, tolerance,
        "Combined Loose should have 50 kg in year 2");
  }

  /**
   * Test that explicit and percentage-based change commands stack additively.
   * This validates that when two policies both change a stream, their changes are applied
   * additively, and that the order of application affects the final value due to the
   * percentage being calculated from different base values.
   */
  @Test
  public void testDoubleChange() throws IOException {
    String qtaPath = "../examples/stacking_double_change.qta";
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

    // Run Explicit Change scenario
    Stream<EngineResult> explicitChangeResults = KigaliSimFacade.runScenario(
        program, "Explicit Change", progress -> {});
    List<EngineResult> explicitChangeList = explicitChangeResults.collect(Collectors.toList());
    EngineResult explicitChange2 = LiveTestsUtil.getResult(
        explicitChangeList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(explicitChange2, "Should have Explicit Change result for year 2");
    final double explicitChangeDomestic = explicitChange2.getDomestic().getValue().doubleValue();

    // Run Percent Change scenario
    Stream<EngineResult> percentChangeResults = KigaliSimFacade.runScenario(
        program, "Percent Change", progress -> {});
    List<EngineResult> percentChangeList = percentChangeResults.collect(Collectors.toList());
    EngineResult percentChange2 = LiveTestsUtil.getResult(
        percentChangeList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(percentChange2, "Should have Percent Change result for year 2");
    final double percentChangeDomestic = percentChange2.getDomestic().getValue().doubleValue();

    // Run Explicit First scenario (Explicit Change then Percent Change)
    Stream<EngineResult> explicitFirstResults = KigaliSimFacade.runScenario(
        program, "Explicit First", progress -> {});
    List<EngineResult> explicitFirstList = explicitFirstResults.collect(Collectors.toList());
    EngineResult explicitFirst2 = LiveTestsUtil.getResult(
        explicitFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(explicitFirst2, "Should have Explicit First result for year 2");
    final double explicitFirstDomestic = explicitFirst2.getDomestic().getValue().doubleValue();

    // Run Percent First scenario (Percent Change then Explicit Change)
    Stream<EngineResult> percentFirstResults = KigaliSimFacade.runScenario(
        program, "Percent First", progress -> {});
    List<EngineResult> percentFirstList = percentFirstResults.collect(Collectors.toList());
    EngineResult percentFirst2 = LiveTestsUtil.getResult(
        percentFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(percentFirst2, "Should have Percent First result for year 2");
    final double percentFirstDomestic = percentFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.01; // kg

    assertEquals(10.0, bauDomestic, tolerance, "BAU should have 10 kg in year 2");
    assertEquals(20.0, explicitChangeDomestic, tolerance,
        "Explicit Change should have 20 kg in year 2");
    assertEquals(11.0, percentChangeDomestic, tolerance,
        "Percent Change should have 11 kg in year 2");
    assertEquals(22.0, explicitFirstDomestic, tolerance,
        "Explicit First should have 22 kg in year 2");
    assertEquals(21.0, percentFirstDomestic, tolerance,
        "Percent First should have 21 kg in year 2");
  }

  /**
   * Test interaction between set and change commands when policies are stacked.
   * This validates that set commands establish absolute values while change commands
   * apply relative modifications, with stacking order determining final outcome.
   */
  @Test
  public void testSetAndChange() throws IOException {
    String qtaPath = "../examples/stacking_set_and_change.qta";
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

    // Run Set scenario
    Stream<EngineResult> setResults = KigaliSimFacade.runScenario(
        program, "Set", progress -> {});
    List<EngineResult> setList = setResults.collect(Collectors.toList());
    EngineResult set2 = LiveTestsUtil.getResult(
        setList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(set2, "Should have Set result for year 2");
    final double setDomestic = set2.getDomestic().getValue().doubleValue();

    // Run Change scenario
    Stream<EngineResult> changeResults = KigaliSimFacade.runScenario(
        program, "Change", progress -> {});
    List<EngineResult> changeList = changeResults.collect(Collectors.toList());
    EngineResult change2 = LiveTestsUtil.getResult(
        changeList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(change2, "Should have Change result for year 2");
    final double changeDomestic = change2.getDomestic().getValue().doubleValue();

    // Run Set First scenario (Set then Change)
    Stream<EngineResult> setFirstResults = KigaliSimFacade.runScenario(
        program, "Set First", progress -> {});
    List<EngineResult> setFirstList = setFirstResults.collect(Collectors.toList());
    EngineResult setFirst2 = LiveTestsUtil.getResult(
        setFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(setFirst2, "Should have Set First result for year 2");
    final double setFirstDomestic = setFirst2.getDomestic().getValue().doubleValue();

    // Run Change First scenario (Change then Set)
    Stream<EngineResult> changeFirstResults = KigaliSimFacade.runScenario(
        program, "Change First", progress -> {});
    List<EngineResult> changeFirstList = changeFirstResults.collect(Collectors.toList());
    EngineResult changeFirst2 = LiveTestsUtil.getResult(
        changeFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(changeFirst2, "Should have Change First result for year 2");
    final double changeFirstDomestic = changeFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.01; // kg

    assertEquals(10.0, bauDomestic, tolerance, "BAU should have 10 kg in year 2");
    assertEquals(20.0, setDomestic, tolerance, "Set should have 20 kg in year 2");
    assertEquals(11.0, changeDomestic, tolerance, "Change should have 11 kg in year 2");
    assertEquals(22.0, setFirstDomestic, tolerance,
        "Set First should have 22 kg in year 2");
    // Note: Set overwrites previous value, so Change First results in 20 kg, not 21 kg
    assertEquals(20.0, changeFirstDomestic, tolerance,
        "Change First should have 20 kg in year 2");
  }

  /**
   * Test that explicit and percentage-based change commands with explicit current syntax
   * stack identically to the default percentage syntax. This validates that using
   * "% current" produces the same results as using "%" in change operations.
   */
  @Test
  public void testDoubleChangeCurrent() throws IOException {
    String qtaPath = "../examples/stacking_double_change_current.qta";
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

    // Run Explicit Change scenario
    Stream<EngineResult> explicitChangeResults = KigaliSimFacade.runScenario(
        program, "Explicit Change", progress -> {});
    List<EngineResult> explicitChangeList = explicitChangeResults.collect(Collectors.toList());
    EngineResult explicitChange2 = LiveTestsUtil.getResult(
        explicitChangeList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(explicitChange2, "Should have Explicit Change result for year 2");
    final double explicitChangeDomestic = explicitChange2.getDomestic().getValue().doubleValue();

    // Run Percent Change scenario
    Stream<EngineResult> percentChangeResults = KigaliSimFacade.runScenario(
        program, "Percent Change", progress -> {});
    List<EngineResult> percentChangeList = percentChangeResults.collect(Collectors.toList());
    EngineResult percentChange2 = LiveTestsUtil.getResult(
        percentChangeList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(percentChange2, "Should have Percent Change result for year 2");
    final double percentChangeDomestic = percentChange2.getDomestic().getValue().doubleValue();

    // Run Explicit First scenario (Explicit Change then Percent Change)
    Stream<EngineResult> explicitFirstResults = KigaliSimFacade.runScenario(
        program, "Explicit First", progress -> {});
    List<EngineResult> explicitFirstList = explicitFirstResults.collect(Collectors.toList());
    EngineResult explicitFirst2 = LiveTestsUtil.getResult(
        explicitFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(explicitFirst2, "Should have Explicit First result for year 2");
    final double explicitFirstDomestic = explicitFirst2.getDomestic().getValue().doubleValue();

    // Run Percent First scenario (Percent Change then Explicit Change)
    Stream<EngineResult> percentFirstResults = KigaliSimFacade.runScenario(
        program, "Percent First", progress -> {});
    List<EngineResult> percentFirstList = percentFirstResults.collect(Collectors.toList());
    EngineResult percentFirst2 = LiveTestsUtil.getResult(
        percentFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(percentFirst2, "Should have Percent First result for year 2");
    final double percentFirstDomestic = percentFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.01; // kg

    assertEquals(10.0, bauDomestic, tolerance, "BAU should have 10 kg in year 2");
    assertEquals(20.0, explicitChangeDomestic, tolerance,
        "Explicit Change should have 20 kg in year 2");
    assertEquals(11.0, percentChangeDomestic, tolerance,
        "Percent Change should have 11 kg in year 2");
    assertEquals(22.0, explicitFirstDomestic, tolerance,
        "Explicit First should have 22 kg in year 2");
    assertEquals(21.0, percentFirstDomestic, tolerance,
        "Percent First should have 21 kg in year 2");
  }

  /**
   * Test interaction between set (units-based) and cap (percentage-based) policies.
   * This validates the units/kg switching behavior when set commands use units
   * and cap commands use percentages, with stacking order determining final outcome.
   */
  @Test
  public void testSetWithCapUnitsSwitch() throws IOException {
    String qtaPath = "../examples/stacking_set_with_cap_units_switch.qta";
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

    // Run Set scenario
    Stream<EngineResult> setResults = KigaliSimFacade.runScenario(
        program, "Set", progress -> {});
    List<EngineResult> setList = setResults.collect(Collectors.toList());
    EngineResult set2 = LiveTestsUtil.getResult(
        setList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(set2, "Should have Set result for year 2");
    final double setDomestic = set2.getDomestic().getValue().doubleValue();

    // Run Cap scenario
    Stream<EngineResult> capResults = KigaliSimFacade.runScenario(
        program, "Cap", progress -> {});
    List<EngineResult> capList = capResults.collect(Collectors.toList());
    EngineResult cap2 = LiveTestsUtil.getResult(
        capList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(cap2, "Should have Cap result for year 2");
    final double capDomestic = cap2.getDomestic().getValue().doubleValue();

    // Run Set First scenario (Set then Cap)
    Stream<EngineResult> setFirstResults = KigaliSimFacade.runScenario(
        program, "Set First", progress -> {});
    List<EngineResult> setFirstList = setFirstResults.collect(Collectors.toList());
    EngineResult setFirst2 = LiveTestsUtil.getResult(
        setFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(setFirst2, "Should have Set First result for year 2");
    final double setFirstDomestic = setFirst2.getDomestic().getValue().doubleValue();

    // Run Cap First scenario (Cap then Set)
    Stream<EngineResult> capFirstResults = KigaliSimFacade.runScenario(
        program, "Cap First", progress -> {});
    List<EngineResult> capFirstList = capFirstResults.collect(Collectors.toList());
    EngineResult capFirst2 = LiveTestsUtil.getResult(
        capFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(capFirst2, "Should have Cap First result for year 2");
    final double capFirstDomestic = capFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.1; // kg

    // BAU should have approximately 10 kg (baseline with recharge)
    assertEquals(10.0, bauDomestic, tolerance, "BAU should have approximately 10 kg in year 2");

    // Set should be at least 10 kg (10 units converted to kg plus recharge)
    assertTrue(setDomestic >= 10.0,
        String.format("Set (%.2f kg) should be >= 10 kg in year 2", setDomestic));

    // Cap should be approximately 0 kg (0% of BAU)
    assertEquals(0.0, capDomestic, tolerance,
        "Cap should have approximately 0 kg in year 2");

    // Set First should be approximately 0 kg (cap to 0% applied after set, last policy wins)
    assertEquals(0.0, setFirstDomestic, tolerance,
        String.format("Set First (%.2f kg) should be approximately 0 kg in year 2",
            setFirstDomestic));

    // Cap First should be at least 10 kg (set to 10 units overrides cap, last policy wins)
    assertTrue(capFirstDomestic >= 10.0,
        String.format("Cap First (%.2f kg) should be >= 10 kg in year 2",
            capFirstDomestic));
  }

  /**
   * Test interaction between set (units-based) and floor (percentage-based) policies.
   * This validates the units/kg switching behavior when set commands use units
   * and floor commands use percentages, with stacking order determining final outcome.
   * This is the mirror-image of testSetWithCapUnitsSwitch, testing minimum enforcement
   * instead of maximum enforcement.
   */
  @Test
  public void testSetWithFloorUnitsSwitch() throws IOException {
    String qtaPath = "../examples/stacking_set_with_floor_units_switch.qta";
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

    // Run Set scenario
    Stream<EngineResult> setResults = KigaliSimFacade.runScenario(
        program, "Set", progress -> {});
    List<EngineResult> setList = setResults.collect(Collectors.toList());
    EngineResult set2 = LiveTestsUtil.getResult(
        setList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(set2, "Should have Set result for year 2");
    final double setDomestic = set2.getDomestic().getValue().doubleValue();

    // Run Floor scenario
    Stream<EngineResult> floorResults = KigaliSimFacade.runScenario(
        program, "Floor", progress -> {});
    List<EngineResult> floorList = floorResults.collect(Collectors.toList());
    EngineResult floor2 = LiveTestsUtil.getResult(
        floorList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(floor2, "Should have Floor result for year 2");
    final double floorDomestic = floor2.getDomestic().getValue().doubleValue();

    // Run Set First scenario (Set then Floor)
    Stream<EngineResult> setFirstResults = KigaliSimFacade.runScenario(
        program, "Set First", progress -> {});
    List<EngineResult> setFirstList = setFirstResults.collect(Collectors.toList());
    EngineResult setFirst2 = LiveTestsUtil.getResult(
        setFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(setFirst2, "Should have Set First result for year 2");
    final double setFirstDomestic = setFirst2.getDomestic().getValue().doubleValue();

    // Run Floor First scenario (Floor then Set)
    Stream<EngineResult> floorFirstResults = KigaliSimFacade.runScenario(
        program, "Floor First", progress -> {});
    List<EngineResult> floorFirstList = floorFirstResults.collect(Collectors.toList());
    EngineResult floorFirst2 = LiveTestsUtil.getResult(
        floorFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(floorFirst2, "Should have Floor First result for year 2");
    final double floorFirstDomestic = floorFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.1; // kg

    // BAU should have approximately 10 kg (baseline with recharge)
    assertEquals(10.0, bauDomestic, tolerance, "BAU should have approximately 10 kg in year 2");

    // Set should be at least 10 kg (10 units converted to kg plus recharge)
    assertTrue(setDomestic >= 10.0,
        String.format("Set (%.2f kg) should be >= 10 kg in year 2", setDomestic));

    // Floor should be approximately 10 kg (100% of prior year, which is 10 kg)
    assertEquals(10.0, floorDomestic, tolerance,
        "Floor should have approximately 10 kg in year 2");

    // Set First should be at least 10 kg (floor enforces minimum after set, last policy wins)
    assertTrue(setFirstDomestic >= 10.0,
        String.format("Set First (%.2f kg) should be >= 10 kg in year 2",
            setFirstDomestic));

    // Floor First should be approximately 10 kg (set to 10 units overrides floor, last policy wins)
    assertEquals(10.0, floorFirstDomestic, tolerance,
        String.format("Floor First (%.2f kg) should be approximately 10 kg in year 2",
            floorFirstDomestic));
  }

  /**
   * Test interaction between change (units-based) and cap (percentage-based) policies.
   * This validates that change commands can specify units and that when stacked with
   * percentage-based caps, the units are properly converted and the cap is applied correctly.
   * This test ensures that change reports in the same units as the original value.
   */
  @Test
  public void testChangeWithUnitsSwitch() throws IOException {
    String qtaPath = "../examples/stacking_change_with_units_switch.qta";
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

    // Run Change scenario
    Stream<EngineResult> changeResults = KigaliSimFacade.runScenario(
        program, "Change", progress -> {});
    List<EngineResult> changeList = changeResults.collect(Collectors.toList());
    EngineResult change2 = LiveTestsUtil.getResult(
        changeList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(change2, "Should have Change result for year 2");
    final double changeDomestic = change2.getDomestic().getValue().doubleValue();

    // Run Cap scenario
    Stream<EngineResult> capResults = KigaliSimFacade.runScenario(
        program, "Cap", progress -> {});
    List<EngineResult> capList = capResults.collect(Collectors.toList());
    EngineResult cap2 = LiveTestsUtil.getResult(
        capList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(cap2, "Should have Cap result for year 2");
    final double capDomestic = cap2.getDomestic().getValue().doubleValue();

    // Run Change First scenario (Change then Cap)
    Stream<EngineResult> changeFirstResults = KigaliSimFacade.runScenario(
        program, "Change First", progress -> {});
    List<EngineResult> changeFirstList = changeFirstResults.collect(Collectors.toList());
    EngineResult changeFirst2 = LiveTestsUtil.getResult(
        changeFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(changeFirst2, "Should have Change First result for year 2");
    final double changeFirstDomestic = changeFirst2.getDomestic().getValue().doubleValue();

    // Run Cap First scenario (Cap then Change)
    Stream<EngineResult> capFirstResults = KigaliSimFacade.runScenario(
        program, "Cap First", progress -> {});
    List<EngineResult> capFirstList = capFirstResults.collect(Collectors.toList());
    EngineResult capFirst2 = LiveTestsUtil.getResult(
        capFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(capFirst2, "Should have Cap First result for year 2");
    final double capFirstDomestic = capFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.1; // kg

    // BAU should have 10 kg (baseline from year 1)
    assertEquals(10.0, bauDomestic, tolerance,
        "BAU should have 10 kg in year 2");

    // Change should have 20 kg (10 units * 1 kg/unit = 10 kg additional)
    assertEquals(20.0, changeDomestic, tolerance,
        "Change should have 20 kg in year 2");

    // Cap should have 0 kg (capped to 0% of prior year)
    assertEquals(0.0, capDomestic, tolerance,
        "Cap should have 0 kg in year 2");

    // Change First should have 0 kg (change increases, then cap reduces to 0%)
    assertEquals(0.0, changeFirstDomestic, tolerance,
        "Change First should have 0 kg in year 2");

    // Cap First should have 10 kg (cap to 0%, then change adds 10 units)
    assertEquals(10.0, capFirstDomestic, tolerance,
        "Cap First should have 10 kg in year 2");
  }

  /**
   * Test displace with cap in scenario A: original substance in kg, cap in units with displace.
   * This validates that cap with units and displace reports in the same units as the target
   * substance prior to displace, and that cap/floor changes units only if triggered.
   * Original: 10 kg, other: 2 kg, cap to 5 units displacing, set to 5 kg.
   * All combined cases should be 5 kg.
   */
  @Test
  public void testDisplaceCapScenarioA() throws IOException {
    String qtaPath = "../examples/stacking_displace_cap_scenario_a.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(
        program, "BAU", progress -> {});
    List<EngineResult> bauList = bauResults.collect(Collectors.toList());
    EngineResult bau2 = LiveTestsUtil.getResult(
        bauList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bau2, "Should have BAU result for year 2");

    // Run Cap scenario
    Stream<EngineResult> capResults = KigaliSimFacade.runScenario(
        program, "Cap", progress -> {});
    List<EngineResult> capList = capResults.collect(Collectors.toList());
    EngineResult cap2 = LiveTestsUtil.getResult(
        capList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(cap2, "Should have Cap result for year 2");

    // Run Set scenario
    Stream<EngineResult> setResults = KigaliSimFacade.runScenario(
        program, "Set", progress -> {});
    List<EngineResult> setList = setResults.collect(Collectors.toList());
    EngineResult set2 = LiveTestsUtil.getResult(
        setList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(set2, "Should have Set result for year 2");
    final double setDomestic = set2.getDomestic().getValue().doubleValue();

    // Run Cap First scenario (Cap then Set)
    Stream<EngineResult> capFirstResults = KigaliSimFacade.runScenario(
        program, "Cap First", progress -> {});
    List<EngineResult> capFirstList = capFirstResults.collect(Collectors.toList());
    EngineResult capFirst2 = LiveTestsUtil.getResult(
        capFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(capFirst2, "Should have Cap First result for year 2");
    final double capFirstDomestic = capFirst2.getDomestic().getValue().doubleValue();

    // Run Set First scenario (Set then Cap)
    Stream<EngineResult> setFirstResults = KigaliSimFacade.runScenario(
        program, "Set First", progress -> {});
    List<EngineResult> setFirstList = setFirstResults.collect(Collectors.toList());
    EngineResult setFirst2 = LiveTestsUtil.getResult(
        setFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(setFirst2, "Should have Set First result for year 2");
    final double setFirstDomestic = setFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.1; // kg

    // All combined cases should be 5 kg
    assertEquals(5.0, setDomestic, tolerance,
        "Set should have 5 kg in year 2");
    assertEquals(5.0, capFirstDomestic, tolerance,
        "Cap First should have 5 kg in year 2");
    assertEquals(5.0, setFirstDomestic, tolerance,
        "Set First should have 5 kg in year 2");
  }

  /**
   * Test displace with cap in scenario B: original substance in units, cap in kg with displace.
   * This validates that cap with kg (when original is in units) and displace reports in the
   * same units as the target substance prior to displace.
   * Original: 10 units, other: 2 kg, cap to 5 kg displacing, set to 5 kg.
   * All combined cases should be 5 kg.
   */
  @Test
  public void testDisplaceCapScenarioB() throws IOException {
    String qtaPath = "../examples/stacking_displace_cap_scenario_b.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run BAU scenario
    Stream<EngineResult> bauResults = KigaliSimFacade.runScenario(
        program, "BAU", progress -> {});
    List<EngineResult> bauList = bauResults.collect(Collectors.toList());
    EngineResult bau2 = LiveTestsUtil.getResult(
        bauList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(bau2, "Should have BAU result for year 2");

    // Run Cap scenario
    Stream<EngineResult> capResults = KigaliSimFacade.runScenario(
        program, "Cap", progress -> {});
    List<EngineResult> capList = capResults.collect(Collectors.toList());
    EngineResult cap2 = LiveTestsUtil.getResult(
        capList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(cap2, "Should have Cap result for year 2");

    // Run Set scenario
    Stream<EngineResult> setResults = KigaliSimFacade.runScenario(
        program, "Set", progress -> {});
    List<EngineResult> setList = setResults.collect(Collectors.toList());
    EngineResult set2 = LiveTestsUtil.getResult(
        setList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(set2, "Should have Set result for year 2");
    final double setDomestic = set2.getDomestic().getValue().doubleValue();

    // Run Cap First scenario (Cap then Set)
    Stream<EngineResult> capFirstResults = KigaliSimFacade.runScenario(
        program, "Cap First", progress -> {});
    List<EngineResult> capFirstList = capFirstResults.collect(Collectors.toList());
    EngineResult capFirst2 = LiveTestsUtil.getResult(
        capFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(capFirst2, "Should have Cap First result for year 2");
    final double capFirstDomestic = capFirst2.getDomestic().getValue().doubleValue();

    // Run Set First scenario (Set then Cap)
    Stream<EngineResult> setFirstResults = KigaliSimFacade.runScenario(
        program, "Set First", progress -> {});
    List<EngineResult> setFirstList = setFirstResults.collect(Collectors.toList());
    EngineResult setFirst2 = LiveTestsUtil.getResult(
        setFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(setFirst2, "Should have Set First result for year 2");
    final double setFirstDomestic = setFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.1; // kg

    // All combined cases should be 5 kg
    assertEquals(5.0, setDomestic, tolerance,
        "Set should have 5 kg in year 2");
    assertEquals(5.0, capFirstDomestic, tolerance,
        "Cap First should have 5 kg in year 2");
    assertEquals(5.0, setFirstDomestic, tolerance,
        "Set First should have 5 kg in year 2");
  }

  /**
   * Test displace with cap in scenario C: cap that doesn't trigger.
   * This validates that when cap doesn't trigger, units don't change, but when set is applied,
   * the result is as expected.
   * Original: 10 units, other: 2 kg, cap to 50 kg (doesn't trigger) displacing, set to 5 kg.
   * Without set: should be > 10 kg (recharge happens).
   * With set: 5 kg.
   */
  @Test
  public void testDisplaceCapScenarioC() throws IOException {
    String qtaPath = "../examples/stacking_displace_cap_scenario_c.qta";
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

    // Run Cap scenario (cap doesn't trigger)
    Stream<EngineResult> capResults = KigaliSimFacade.runScenario(
        program, "Cap", progress -> {});
    List<EngineResult> capList = capResults.collect(Collectors.toList());
    EngineResult cap2 = LiveTestsUtil.getResult(
        capList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(cap2, "Should have Cap result for year 2");
    final double capDomestic = cap2.getDomestic().getValue().doubleValue();

    // Run Set scenario
    Stream<EngineResult> setResults = KigaliSimFacade.runScenario(
        program, "Set", progress -> {});
    List<EngineResult> setList = setResults.collect(Collectors.toList());
    EngineResult set2 = LiveTestsUtil.getResult(
        setList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(set2, "Should have Set result for year 2");
    final double setDomestic = set2.getDomestic().getValue().doubleValue();

    // Run Cap First scenario (Cap then Set)
    Stream<EngineResult> capFirstResults = KigaliSimFacade.runScenario(
        program, "Cap First", progress -> {});
    List<EngineResult> capFirstList = capFirstResults.collect(Collectors.toList());
    EngineResult capFirst2 = LiveTestsUtil.getResult(
        capFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(capFirst2, "Should have Cap First result for year 2");
    final double capFirstDomestic = capFirst2.getDomestic().getValue().doubleValue();

    // Run Set First scenario (Set then Cap)
    Stream<EngineResult> setFirstResults = KigaliSimFacade.runScenario(
        program, "Set First", progress -> {});
    List<EngineResult> setFirstList = setFirstResults.collect(Collectors.toList());
    EngineResult setFirst2 = LiveTestsUtil.getResult(
        setFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(setFirst2, "Should have Set First result for year 2");
    final double setFirstDomestic = setFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.1; // kg

    // Cap doesn't trigger, so should be > 10 kg (with recharge)
    assertTrue(capDomestic > 10.0,
        String.format("Cap (%.2f kg) should be > 10 kg in year 2", capDomestic));

    // Set should override to 5 kg
    assertEquals(5.0, setDomestic, tolerance,
        "Set should have 5 kg in year 2");

    // Cap First then Set should be 5 kg (set overwrites)
    assertEquals(5.0, capFirstDomestic, tolerance,
        "Cap First should have 5 kg in year 2");

    // Set First then Cap should be 5 kg (cap doesn't trigger, set value remains)
    assertEquals(5.0, setFirstDomestic, tolerance,
        "Set First should have 5 kg in year 2");
  }

  /**
   * Test displace with floor in scenario A: original substance in kg, floor in units with displace.
   * This validates that floor with units and displace reports in the same units as the target
   * substance prior to displace, and that floor changes units only if triggered.
   * Original: 10 kg, other: 2 kg, floor to 15 units displacing, set to 20 kg.
   * Floor should trigger, increasing values.
   */
  @Test
  public void testDisplaceFloorScenarioA() throws IOException {
    String qtaPath = "../examples/stacking_displace_floor_scenario_a.qta";
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

    // Run Floor scenario
    Stream<EngineResult> floorResults = KigaliSimFacade.runScenario(
        program, "Floor", progress -> {});
    List<EngineResult> floorList = floorResults.collect(Collectors.toList());
    EngineResult floor2 = LiveTestsUtil.getResult(
        floorList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(floor2, "Should have Floor result for year 2");
    final double floorDomestic = floor2.getDomestic().getValue().doubleValue();

    // Run Set scenario
    Stream<EngineResult> setResults = KigaliSimFacade.runScenario(
        program, "Set", progress -> {});
    List<EngineResult> setList = setResults.collect(Collectors.toList());
    EngineResult set2 = LiveTestsUtil.getResult(
        setList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(set2, "Should have Set result for year 2");
    final double setDomestic = set2.getDomestic().getValue().doubleValue();

    // Run Floor First scenario (Floor then Set)
    Stream<EngineResult> floorFirstResults = KigaliSimFacade.runScenario(
        program, "Floor First", progress -> {});
    List<EngineResult> floorFirstList = floorFirstResults.collect(Collectors.toList());
    EngineResult floorFirst2 = LiveTestsUtil.getResult(
        floorFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(floorFirst2, "Should have Floor First result for year 2");
    final double floorFirstDomestic = floorFirst2.getDomestic().getValue().doubleValue();

    // Run Set First scenario (Set then Floor)
    Stream<EngineResult> setFirstResults = KigaliSimFacade.runScenario(
        program, "Set First", progress -> {});
    List<EngineResult> setFirstList = setFirstResults.collect(Collectors.toList());
    EngineResult setFirst2 = LiveTestsUtil.getResult(
        setFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(setFirst2, "Should have Set First result for year 2");
    final double setFirstDomestic = setFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.1; // kg

    // Floor should enforce minimum (15 units with recharge)
    assertTrue(floorDomestic >= 15.0,
        String.format("Floor (%.2f kg) should be >= 15 kg in year 2", floorDomestic));

    // Set should be 20 kg
    assertEquals(20.0, setDomestic, tolerance,
        "Set should have 20 kg in year 2");

    // Floor First then Set should be 20 kg (set overwrites)
    assertEquals(20.0, floorFirstDomestic, tolerance,
        "Floor First should have 20 kg in year 2");

    // Set First then Floor should be 20 kg (set already above floor)
    assertEquals(20.0, setFirstDomestic, tolerance,
        "Set First should have 20 kg in year 2");
  }

  /**
   * Test displace with floor in scenario B: original substance in units, floor in kg with displace.
   * This validates that floor with kg (when original is in units) and displace reports in the
   * same units as the target substance prior to displace.
   * Original: 10 units, other: 2 kg, floor to 15 kg displacing, set to 20 kg.
   */
  @Test
  public void testDisplaceFloorScenarioB() throws IOException {
    String qtaPath = "../examples/stacking_displace_floor_scenario_b.qta";
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

    // Run Floor scenario
    Stream<EngineResult> floorResults = KigaliSimFacade.runScenario(
        program, "Floor", progress -> {});
    List<EngineResult> floorList = floorResults.collect(Collectors.toList());
    EngineResult floor2 = LiveTestsUtil.getResult(
        floorList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(floor2, "Should have Floor result for year 2");
    final double floorDomestic = floor2.getDomestic().getValue().doubleValue();

    // Run Set scenario
    Stream<EngineResult> setResults = KigaliSimFacade.runScenario(
        program, "Set", progress -> {});
    List<EngineResult> setList = setResults.collect(Collectors.toList());
    EngineResult set2 = LiveTestsUtil.getResult(
        setList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(set2, "Should have Set result for year 2");
    final double setDomestic = set2.getDomestic().getValue().doubleValue();

    // Run Floor First scenario (Floor then Set)
    Stream<EngineResult> floorFirstResults = KigaliSimFacade.runScenario(
        program, "Floor First", progress -> {});
    List<EngineResult> floorFirstList = floorFirstResults.collect(Collectors.toList());
    EngineResult floorFirst2 = LiveTestsUtil.getResult(
        floorFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(floorFirst2, "Should have Floor First result for year 2");
    final double floorFirstDomestic = floorFirst2.getDomestic().getValue().doubleValue();

    // Run Set First scenario (Set then Floor)
    Stream<EngineResult> setFirstResults = KigaliSimFacade.runScenario(
        program, "Set First", progress -> {});
    List<EngineResult> setFirstList = setFirstResults.collect(Collectors.toList());
    EngineResult setFirst2 = LiveTestsUtil.getResult(
        setFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(setFirst2, "Should have Set First result for year 2");
    final double setFirstDomestic = setFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.1; // kg

    // Floor should enforce minimum of 15 kg
    assertEquals(15.0, floorDomestic, tolerance,
        "Floor should have 15 kg in year 2");

    // Set should be 20 kg
    assertEquals(20.0, setDomestic, tolerance,
        "Set should have 20 kg in year 2");

    // Floor First then Set should be 20 kg (set overwrites)
    assertEquals(20.0, floorFirstDomestic, tolerance,
        "Floor First should have 20 kg in year 2");

    // Set First then Floor should be 20 kg (set already above floor)
    assertEquals(20.0, setFirstDomestic, tolerance,
        "Set First should have 20 kg in year 2");
  }

  /**
   * Test displace with floor in scenario C: floor that doesn't trigger.
   * This validates that when floor doesn't trigger, units don't change, but when set is applied,
   * the result is as expected.
   * Original: 10 units, other: 2 kg, floor to 1 kg (doesn't trigger) displacing, set to 5 kg.
   */
  @Test
  public void testDisplaceFloorScenarioC() throws IOException {
    String qtaPath = "../examples/stacking_displace_floor_scenario_c.qta";
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

    // Run Floor scenario (floor doesn't trigger)
    Stream<EngineResult> floorResults = KigaliSimFacade.runScenario(
        program, "Floor", progress -> {});
    List<EngineResult> floorList = floorResults.collect(Collectors.toList());
    EngineResult floor2 = LiveTestsUtil.getResult(
        floorList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(floor2, "Should have Floor result for year 2");
    final double floorDomestic = floor2.getDomestic().getValue().doubleValue();

    // Run Set scenario
    Stream<EngineResult> setResults = KigaliSimFacade.runScenario(
        program, "Set", progress -> {});
    List<EngineResult> setList = setResults.collect(Collectors.toList());
    EngineResult set2 = LiveTestsUtil.getResult(
        setList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(set2, "Should have Set result for year 2");
    final double setDomestic = set2.getDomestic().getValue().doubleValue();

    // Run Floor First scenario (Floor then Set)
    Stream<EngineResult> floorFirstResults = KigaliSimFacade.runScenario(
        program, "Floor First", progress -> {});
    List<EngineResult> floorFirstList = floorFirstResults.collect(Collectors.toList());
    EngineResult floorFirst2 = LiveTestsUtil.getResult(
        floorFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(floorFirst2, "Should have Floor First result for year 2");
    final double floorFirstDomestic = floorFirst2.getDomestic().getValue().doubleValue();

    // Run Set First scenario (Set then Floor)
    Stream<EngineResult> setFirstResults = KigaliSimFacade.runScenario(
        program, "Set First", progress -> {});
    List<EngineResult> setFirstList = setFirstResults.collect(Collectors.toList());
    EngineResult setFirst2 = LiveTestsUtil.getResult(
        setFirstList.stream(), 2, "Domestic Refrigeration", "HFC-134a");
    assertNotNull(setFirst2, "Should have Set First result for year 2");
    final double setFirstDomestic = setFirst2.getDomestic().getValue().doubleValue();

    // Assertions with tolerance for floating-point comparisons
    double tolerance = 0.1; // kg

    // Floor doesn't trigger, should be close to BAU (with recharge)
    assertTrue(floorDomestic >= bauDomestic,
        String.format("Floor (%.2f kg) should be >= BAU (%.2f kg) in year 2",
            floorDomestic, bauDomestic));

    // Set should be 5 kg
    assertEquals(5.0, setDomestic, tolerance,
        "Set should have 5 kg in year 2");

    // Floor First then Set should be 5 kg (set overwrites)
    assertEquals(5.0, floorFirstDomestic, tolerance,
        "Floor First should have 5 kg in year 2");

    // Set First then Floor should be 5 kg (floor doesn't trigger, set value remains)
    assertEquals(5.0, setFirstDomestic, tolerance,
        "Set First should have 5 kg in year 2");
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
