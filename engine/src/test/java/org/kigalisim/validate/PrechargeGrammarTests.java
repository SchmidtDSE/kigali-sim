/**
 * Tests for precharge grammar (recharge with of clause).
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
 * Tests for the "of" clause on recharge statements (precharge grammar).
 *
 * <p>These tests verify that the grammar accepts the optional "of priorEquipment" and
 * "of newEquipment" clause on recharge statements.</p>
 */
public class PrechargeGrammarTests {

  /**
   * Test that recharge with "of priorEquipment" clause parses.
   */
  @Test
  public void testRechargeOfPriorEquipmentParses() throws IOException {
    String qtaPath = "../examples/recharge_of_prior.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program with 'of priorEquipment' should parse");
  }

  /**
   * Test that recharge with "of newEquipment" clause parses.
   */
  @Test
  public void testRechargeOfNewEquipmentParses() throws IOException {
    String qtaPath = "../examples/recharge_of_new.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program with 'of newEquipment' should parse");
  }

  /**
   * Test that recharge with "of priorEquipment" runs and produces results.
   */
  @Test
  public void testRechargeOfPriorEquipmentRuns() throws IOException {
    String qtaPath = "../examples/recharge_of_prior.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    assertTrue(resultsList.size() > 0, "Should have results for recharge of priorEquipment");
  }

  /**
   * Test that recharge with "of newEquipment" runs and produces results.
   */
  @Test
  public void testRechargeOfNewEquipmentRuns() throws IOException {
    String qtaPath = "../examples/recharge_of_new.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName, progress -> {});
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    assertTrue(resultsList.size() > 0, "Should have results for recharge of newEquipment");
  }
}
