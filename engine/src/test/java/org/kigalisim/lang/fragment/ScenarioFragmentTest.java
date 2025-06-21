/**
 * Unit tests for the ScenarioFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.program.ParsedScenario;

/**
 * Tests for the ScenarioFragment class.
 */
public class ScenarioFragmentTest {

  /**
   * Helper method to create a simple ParsedScenario for testing.
   *
   * @return A simple ParsedScenario
   */
  private ParsedScenario createTestScenario() {
    return new ParsedScenario("testScenario", Collections.emptyList(), 1, 3, 1);
  }

  /**
   * Test that ScenarioFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    ParsedScenario scenario = createTestScenario();
    ScenarioFragment fragment = new ScenarioFragment(scenario);
    assertNotNull(fragment, "ScenarioFragment should be constructable");
  }

  /**
   * Test the getScenario method.
   */
  @Test
  public void testGetScenario() {
    ParsedScenario scenario = createTestScenario();
    ScenarioFragment fragment = new ScenarioFragment(scenario);
    assertEquals(scenario, fragment.getScenario(),
        "getScenario should return the correct ParsedScenario");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    ParsedScenario scenario = createTestScenario();
    ScenarioFragment fragment = new ScenarioFragment(scenario);
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }

  /**
   * Test that getDuring throws RuntimeException.
   */
  @Test
  public void testGetDuringThrows() {
    ParsedScenario scenario = createTestScenario();
    ScenarioFragment fragment = new ScenarioFragment(scenario);
    assertThrows(RuntimeException.class, () -> fragment.getDuring(),
        "getDuring should throw RuntimeException");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    ParsedScenario scenario = createTestScenario();
    ScenarioFragment fragment = new ScenarioFragment(scenario);
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }

  /**
   * Test that getProgram throws RuntimeException.
   */
  @Test
  public void testGetProgramThrows() {
    ParsedScenario scenario = createTestScenario();
    ScenarioFragment fragment = new ScenarioFragment(scenario);
    assertThrows(RuntimeException.class, () -> fragment.getProgram(),
        "getProgram should throw RuntimeException");
  }

  /**
   * Test that getPolicy throws RuntimeException.
   */
  @Test
  public void testGetPolicyThrows() {
    ParsedScenario scenario = createTestScenario();
    ScenarioFragment fragment = new ScenarioFragment(scenario);
    assertThrows(RuntimeException.class, () -> fragment.getPolicy(),
        "getPolicy should throw RuntimeException");
  }

  /**
   * Test that getApplication throws RuntimeException.
   */
  @Test
  public void testGetApplicationThrows() {
    ParsedScenario scenario = createTestScenario();
    ScenarioFragment fragment = new ScenarioFragment(scenario);
    assertThrows(RuntimeException.class, () -> fragment.getApplication(),
        "getApplication should throw RuntimeException");
  }

  /**
   * Test that getSubstance throws RuntimeException.
   */
  @Test
  public void testGetSubstanceThrows() {
    ParsedScenario scenario = createTestScenario();
    ScenarioFragment fragment = new ScenarioFragment(scenario);
    assertThrows(RuntimeException.class, () -> fragment.getSubstance(),
        "getSubstance should throw RuntimeException");
  }
}
