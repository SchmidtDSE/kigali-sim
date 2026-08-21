/**
 * Live tests for the exact retirement age shortcut.
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
 * Tests for the {@code retire N year old exact} shortcut, which desugars to
 * {@code retire (get newEquipment N years ago as units) units / year}.
 */
public class ExactRetireLiveTests {

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
}
