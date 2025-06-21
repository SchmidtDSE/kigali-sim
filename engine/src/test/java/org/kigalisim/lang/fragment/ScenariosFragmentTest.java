/**
 * Unit tests for the ScenariosFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.program.ParsedScenario;
import org.kigalisim.lang.program.ParsedScenarios;

/**
 * Tests for the ScenariosFragment class.
 */
public class ScenariosFragmentTest {

  /**
   * Helper method to create a simple ParsedScenarios for testing.
   *
   * @return A simple ParsedScenarios
   */
  private ParsedScenarios createTestScenarios() {
    return new ParsedScenarios(Collections.emptyList());
  }

  /**
   * Test that ScenariosFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertNotNull(fragment, "ScenariosFragment should be constructable");
  }

  /**
   * Test the getScenarios method.
   */
  @Test
  public void testGetScenarios() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertEquals(scenarios, fragment.getScenarios(),
        "getScenarios should return the correct ParsedScenarios");
  }

  /**
   * Test the getIsStanzaScenarios method.
   */
  @Test
  public void testGetIsStanzaScenarios() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertTrue(fragment.getIsStanzaScenarios(),
        "getIsStanzaScenarios should return true");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }

  /**
   * Test that getDuring throws RuntimeException.
   */
  @Test
  public void testGetDuringThrows() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertThrows(RuntimeException.class, () -> fragment.getDuring(),
        "getDuring should throw RuntimeException");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }

  /**
   * Test that getProgram throws RuntimeException.
   */
  @Test
  public void testGetProgramThrows() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertThrows(RuntimeException.class, () -> fragment.getProgram(),
        "getProgram should throw RuntimeException");
  }

  /**
   * Test that getPolicy throws RuntimeException.
   */
  @Test
  public void testGetPolicyThrows() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertThrows(RuntimeException.class, () -> fragment.getPolicy(),
        "getPolicy should throw RuntimeException");
  }

  /**
   * Test that getApplication throws RuntimeException.
   */
  @Test
  public void testGetApplicationThrows() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertThrows(RuntimeException.class, () -> fragment.getApplication(),
        "getApplication should throw RuntimeException");
  }

  /**
   * Test that getScenario throws RuntimeException.
   */
  @Test
  public void testGetScenarioThrows() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertThrows(RuntimeException.class, () -> fragment.getScenario(),
        "getScenario should throw RuntimeException");
  }

  /**
   * Test that getSubstance throws RuntimeException.
   */
  @Test
  public void testGetSubstanceThrows() {
    ParsedScenarios scenarios = createTestScenarios();
    ScenariosFragment fragment = new ScenariosFragment(scenarios);
    assertThrows(RuntimeException.class, () -> fragment.getSubstance(),
        "getSubstance should throw RuntimeException");
  }
}