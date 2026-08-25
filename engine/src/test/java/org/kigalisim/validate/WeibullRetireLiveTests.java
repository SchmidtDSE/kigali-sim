/**
 * Live tests for Weibull retirement.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * Tests for the Weibull retirement grammar and semantics.
 */
public class WeibullRetireLiveTests {

  private static final String PRIOR_MESSAGE =
      "Weibull retirement requires equipment ages, which are derived from simulated sales";

  /**
   * Test that a Weibull retire with a singular "year" parses and interprets.
   */
  @Test
  public void testWeibullRetireParses() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/weibull_retire.qta");
    assertNotNull(program, "Weibull retire should parse and interpret");
  }

  /**
   * Test that a Weibull retire with plural "years" parses and interprets.
   */
  @Test
  public void testWeibullRetireYearsPluralParses() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_years_plural.qta");
    assertNotNull(program, "Weibull retire with plural years should parse and interpret");
  }

  /**
   * Test that a Weibull retire with a duration parses and interprets.
   */
  @Test
  public void testWeibullRetireDurationParses() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_duration.qta");
    assertNotNull(program, "Weibull retire with duration should parse and interpret");
  }

  /**
   * Test that a Weibull retire combined with a priorEquipment set raises an error.
   */
  @Test
  public void testPriorEquipmentWithWeibullRaises() {
    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_prior_error.qta"));
    assertTrue(ex.getMessage().contains(PRIOR_MESSAGE), "error should mention unknown ages");
    assertTrue(ex.getMessage().contains("priorEquipment"), "error should mention priorEquipment");
    assertTrue(ex.getMessage().contains("before this substance entered service"), "error should give remedy 1");
    assertTrue(ex.getMessage().contains("% / year"), "error should give remedy 2");
    assertTrue(ex.getMessage().contains("assuming new"), "error should give remedy 3");
    assertFalse(KigaliSimFacade.validate("../examples/weibull_retire_prior_error.qta"),
        "validate should report the prior-equipment conflict");
  }

  /**
   * Test that a zero-valued priorEquipment set still raises with Weibull.
   */
  @Test
  public void testPriorEquipmentZeroValueRaises() {
    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_prior_error_zero.qta"));
    assertTrue(ex.getMessage().contains(PRIOR_MESSAGE));
  }

  /**
   * Test that a change priorEquipment raises with Weibull.
   */
  @Test
  public void testChangePriorEquipmentRaises() {
    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_prior_error_change.qta"));
    assertTrue(ex.getMessage().contains(PRIOR_MESSAGE));
  }

  /**
   * Test that the conflict is order-independent (Weibull before the set).
   */
  @Test
  public void testReversedOrderRaises() {
    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_prior_error_reversed.qta"));
    assertTrue(ex.getMessage().contains(PRIOR_MESSAGE));
  }

  /**
   * Test that the conflict is caught when the two statements are in different stanzas.
   *
   * <p>A scenario stacks its policies on top of the default stanza, so a
   * {@code set priorEquipment} there meets a Weibull retire added by a policy.</p>
   */
  @Test
  public void testPriorEquipmentAcrossStanzasRaises() {
    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_prior_error_policy.qta"));
    assertTrue(ex.getMessage().contains(PRIOR_MESSAGE), "error should mention unknown ages");
    assertFalse(KigaliSimFacade.validate("../examples/weibull_retire_prior_error_policy.qta"),
        "validate should report the cross-stanza prior-equipment conflict");
  }

  /**
   * Test that a policy no scenario applies does not raise a cross-stanza conflict.
   */
  @Test
  public void testUnusedPolicyDoesNotRaise() throws IOException {
    ParsedProgram program =
        KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_unused_policy.qta");
    assertNotNull(program, "an unapplied policy should not create a conflict");
    assertTrue(KigaliSimFacade.validate("../examples/weibull_retire_unused_policy.qta"),
        "validate should accept a Weibull retire in a policy no scenario uses");
  }

  /**
   * Test that a set bank (equipment) does not conflict with Weibull.
   */
  @Test
  public void testSetBankWithWeibullIsValid() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_bank.qta");
    assertNotNull(program, "set bank with Weibull should be valid");
    assertTrue(KigaliSimFacade.validate("../examples/weibull_retire_bank.qta"),
        "validate should accept set bank with Weibull");
  }

  /**
   * Test that assuming new suppresses the prior-equipment error.
   */
  @Test
  public void testAssumingNewSuppressesError() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_assuming_new.qta");
    assertNotNull(program, "assuming new should suppress the prior-equipment error");
  }

  /**
   * Test the worked-value equipment sequence for a 5-year mean.
   */
  @Test
  public void testWorkedValueSequence() throws IOException {
    double[] expected = {100.000, 196.907, 285.098, 360.470, 420.962};
    for (int year = 1; year <= 5; year++) {
      assertEquals(expected[year - 1],
          getPopulation("../examples/weibull_retire.qta", year), 0.01,
          "equipment at year " + year);
    }
  }

  /**
   * Test that the years-plural and duration variants produce the same sequence.
   */
  @Test
  public void testVariantsMatchSequence() throws IOException {
    for (int year = 1; year <= 5; year++) {
      double plural = getPopulation("../examples/weibull_retire_years_plural.qta", year);
      double duration = getPopulation("../examples/weibull_retire_duration.qta", year);
      assertEquals(plural, duration, 0.01, "plural and duration variants at year " + year);
    }
  }

  /**
   * Test that the Weibull engine matches a hand-built explicit retirement schedule.
   */
  @Test
  public void testEquivalenceToExplicitSchedule() throws IOException {
    for (int year = 1; year <= 12; year++) {
      double weibull = getPopulation("../examples/weibull_retire.qta", year);
      double explicit = getPopulation("../examples/weibull_retire_equivalence.qta", year);
      assertEquals(weibull, explicit, 0.001, "equivalence at year " + year);
    }
  }

  /**
   * Test that a sales cap flows through the recorded history identically in both models.
   */
  @Test
  public void testCapFlowsThroughHistory() throws IOException {
    for (int year = 1; year <= 12; year++) {
      double cap = getPopulation("../examples/weibull_retire_cap.qta", year);
      double ref = getPopulation("../examples/weibull_retire_cap_reference.qta", year);
      assertEquals(cap, ref, 0.001, "cap equivalence at year " + year);
    }
  }

  /**
   * Test that "with replacement" parses and interprets for a Weibull retire.
   */
  @Test
  public void testWithReplacementParses() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_with_replacement.qta");
    assertNotNull(program, "Weibull retire with replacement should parse and interpret");
  }

  /**
   * Test that "with replacement" increases sales to offset Weibull retirement, unlike the
   * non-replacing baseline where the flat sales rate never grows.
   */
  @Test
  public void testWithReplacementIncreasesSales() throws IOException {
    double baselineSales = getDomesticSales("../examples/weibull_retire.qta", 5);
    double withReplacementSales = getDomesticSales("../examples/weibull_retire_with_replacement.qta", 5);
    assertTrue(withReplacementSales > baselineSales,
        "with replacement should increase sales above the flat non-replacing baseline");
  }

  /**
   * Test that competing retirement over-retires relative to the Weibull-only baseline.
   */
  @Test
  public void testCompetingRetirementOverRetires() throws IOException {
    int[] years = {5, 10, 15};
    for (int year : years) {
      double baseline = getPopulation("../examples/weibull_retire_20yr.qta", year);
      double competing = getPopulation("../examples/weibull_retire_competing.qta", year);
      assertTrue(competing < baseline, "competing equipment should be lower at year " + year);
    }
  }

  /**
   * Test that the truncation sweep retires the entire population.
   */
  @Test
  public void testMassBalance() throws IOException {
    double equipment = getPopulation("../examples/weibull_retire_mass_balance.qta", 21);
    assertEquals(0.0, equipment, 1e-6, "all equipment should be retired by year 21");
  }

  /**
   * Test the assuming-new placement for a 20-year mean.
   */
  @Test
  public void testAssumingNewPlacement() throws IOException {
    double year1 = getPopulation("../examples/weibull_retire_assuming_new.qta", 1);
    double year2 = getPopulation("../examples/weibull_retire_assuming_new.qta", 2);
    assertEquals(952.098, year1, 0.01, "year-1 equipment after assuming-new retirement");
    assertEquals(902.938, year2, 0.01, "year-2 equipment after assuming-new retirement");
  }

  /**
   * Test that assuming new does nothing when the sales history explains the whole fleet.
   *
   * <p>The modifier only speaks for stock entered by hand, so a model whose equipment all
   * came from simulated sales must retire identically with and without it.</p>
   */
  @Test
  public void testAssumingNewIsNoOpWithoutPriorStock() throws IOException {
    for (int year = 1; year <= 15; year++) {
      double baseline = getPopulation("../examples/weibull_retire_20yr.qta", year);
      double assumingNew = getPopulation(
          "../examples/weibull_retire_assuming_new_no_prior.qta", year);
      assertEquals(baseline, assumingNew, 1e-6,
          "assuming new should not change a fleet with no manually entered stock at year " + year);
    }
  }

  /**
   * Test that a sub-year mean lifetime retires rather than producing a negative amount.
   *
   * <p>A mean under about 0.79 years rounds the synthetic cohort offset to zero, which
   * previously placed the pseudo-cohort at age zero and yielded a negative hazard.</p>
   */
  @Test
  public void testShortMeanAssumingNewRetires() throws IOException {
    assertEquals(43.214, getPopulation("../examples/weibull_retire_short_mean.qta", 1), 0.01,
        "year-1 equipment for a half-year mean lifetime");
    assertEquals(0.0, getPopulation("../examples/weibull_retire_short_mean.qta", 2), 1e-6,
        "the remainder should be swept up at the truncation age");
  }

  /**
   * Test that Monte Carlo trials produce identical stream values.
   */
  @Test
  public void testMonteCarloParity() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/weibull_retire_mc.qta");
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "x", progress -> {});
    List<EngineResult> list = results.collect(Collectors.toList());
    int[] years = {1, 5, 10};
    for (int year : years) {
      double trial1 = LiveTestsUtil.getResultWithTrial(list.stream(), 1, year, "test", "test")
          .getPopulation().getValue().doubleValue();
      double trial2 = LiveTestsUtil.getResultWithTrial(list.stream(), 2, year, "test", "test")
          .getPopulation().getValue().doubleValue();
      double trial3 = LiveTestsUtil.getResultWithTrial(list.stream(), 3, year, "test", "test")
          .getPopulation().getValue().doubleValue();
      assertEquals(trial1, trial2, 0.0, "trials 1/2 identical at year " + year);
      assertEquals(trial1, trial3, 0.0, "trials 1/3 identical at year " + year);
    }
  }

  private static double getPopulation(String path, int year) throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(path);
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "business as usual", progress -> {});
    List<EngineResult> list = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(list.stream(), year, "test", "test");
    assertNotNull(result, "should have a result for test/test at year " + year);
    return result.getPopulation().getValue().doubleValue();
  }

  private static double getDomesticSales(String path, int year) throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(path);
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "business as usual", progress -> {});
    List<EngineResult> list = results.collect(Collectors.toList());
    EngineResult result = LiveTestsUtil.getResult(list.stream(), year, "test", "test");
    assertNotNull(result, "should have a result for test/test at year " + year);
    return result.getDomestic().getValue().doubleValue();
  }
}
