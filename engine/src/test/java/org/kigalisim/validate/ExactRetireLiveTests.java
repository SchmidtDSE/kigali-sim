/**
 * Live tests for the exact retirement age shortcut.
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
 * Tests for the {@code retire N year old exact} shortcut, which desugars to
 * {@code retire (get newEquipment N years ago as units) units / year}.
 */
public class ExactRetireLiveTests {

  private static final String EXACT_PRIOR_MESSAGE =
      "Exact-age retirement requires equipment ages, which are derived from simulated sales";

  /**
   * Test that an exact retire fully retires a cohort exactly N years after its sale and
   * that population is unaffected before that age.
   */
  @Test
  public void testExactRetireRetiresCohortAtAge() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/exact_retire.qta");
    assertNotNull(program, "Exact retire should parse and interpret");

    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "business as usual", progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    for (int year = 1; year <= 5; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year, "test", "test");
      assertEquals(1000, result.getPopulation().getValue().intValue(),
          "Population should remain at 1000 before the cohort reaches age 5 (year " + year + ")");
    }

    for (int year = 6; year <= 10; year++) {
      EngineResult result = LiveTestsUtil.getResult(resultsList.stream(), year, "test", "test");
      assertEquals(0, result.getPopulation().getValue().intValue(),
          "Population should drop to 0 once the cohort reaches age 5 (year " + year + ")");
    }
  }

  /**
   * Test that plural "years" is accepted by the shortcut, matching the Weibull form's
   * singular/plural flexibility.
   */
  @Test
  public void testExactRetireYearsPluralParses() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/exact_retire_years_plural.qta");
    assertNotNull(program, "Exact retire with plural years should parse and interpret");
  }

  /**
   * Test that a duration-scoped exact retire parses and interprets.
   */
  @Test
  public void testExactRetireDurationParses() throws IOException {
    ParsedProgram program = KigaliSimFacade.parseAndInterpret("../examples/exact_retire_duration.qta");
    assertNotNull(program, "Exact retire with duration should parse and interpret");
  }

  /**
   * Test that the shortcut produces results identical to the equivalent full form written
   * out by hand, confirming the sugar changes nothing about engine behavior.
   */
  @Test
  public void testExactRetireMatchesFullForm() throws IOException {
    ParsedProgram shortcutProgram = KigaliSimFacade.parseAndInterpret("../examples/exact_retire.qta");
    ParsedProgram fullFormProgram =
        KigaliSimFacade.parseAndInterpret("../examples/exact_retire_full_form.qta");

    List<EngineResult> shortcutResults = KigaliSimFacade
        .runScenario(shortcutProgram, "business as usual", progress -> {})
        .collect(Collectors.toList());
    List<EngineResult> fullFormResults = KigaliSimFacade
        .runScenario(fullFormProgram, "business as usual", progress -> {})
        .collect(Collectors.toList());

    for (int year = 1; year <= 10; year++) {
      EngineResult shortcutResult = LiveTestsUtil.getResult(shortcutResults.stream(), year, "test", "test");
      EngineResult fullFormResult = LiveTestsUtil.getResult(fullFormResults.stream(), year, "test", "test");
      assertEquals(
          fullFormResult.getPopulation().getValue(),
          shortcutResult.getPopulation().getValue(),
          "Shortcut and full form should produce identical population in year " + year);
    }
  }

  /**
   * Test that an exact retire combined with a priorEquipment set raises an error.
   *
   * <p>Manually entered priorEquipment has no sales record, so it can never match the
   * exact-age cohort lookup this shortcut relies on and would silently never retire.</p>
   */
  @Test
  public void testPriorEquipmentWithExactRetireRaises() {
    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        KigaliSimFacade.parseAndInterpret("../examples/exact_retire_prior_error.qta"));
    assertTrue(ex.getMessage().contains(EXACT_PRIOR_MESSAGE), "error should mention unknown ages");
    assertTrue(ex.getMessage().contains("priorEquipment"), "error should mention priorEquipment");
    assertTrue(ex.getMessage().contains("% / year"), "error should give a remedy");
    assertFalse(KigaliSimFacade.validate("../examples/exact_retire_prior_error.qta"),
        "validate should report the prior-equipment conflict");
  }

  /**
   * Test that a priorEquipment set in one stanza and an exact retire added by a policy in
   * another still raises, mirroring the cross-stanza check for Weibull retirement.
   */
  @Test
  public void testPriorEquipmentAcrossStanzasRaises() {
    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        KigaliSimFacade.parseAndInterpret("../examples/exact_retire_prior_error_policy.qta"));
    assertTrue(ex.getMessage().contains(EXACT_PRIOR_MESSAGE), "error should mention unknown ages");
    assertFalse(KigaliSimFacade.validate("../examples/exact_retire_prior_error_policy.qta"),
        "validate should report the cross-stanza prior-equipment conflict");
  }

  /**
   * Test that "with replacement" parses and interprets for an exact retire.
   */
  @Test
  public void testWithReplacementParses() throws IOException {
    ParsedProgram program =
        KigaliSimFacade.parseAndInterpret("../examples/exact_retire_with_replacement.qta");
    assertNotNull(program, "Exact retire with replacement should parse and interpret");
  }

  /**
   * Test that "with replacement" increases sales to offset exact-age retirement, unlike the
   * non-replacing baseline where the flat sales rate never grows.
   */
  @Test
  public void testWithReplacementIncreasesSales() throws IOException {
    double baselineSales = getDomesticSales("../examples/exact_retire_steady_sales.qta", 6);
    double withReplacementSales = getDomesticSales("../examples/exact_retire_with_replacement.qta", 6);
    assertTrue(withReplacementSales > baselineSales,
        "with replacement should increase sales above the flat non-replacing baseline");
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
