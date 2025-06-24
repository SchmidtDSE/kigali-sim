/**
 * Unit tests for the PolicyFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.program.ParsedPolicy;

/**
 * Tests for the PolicyFragment class.
 */
public class PolicyFragmentTest {

  /**
   * Helper method to create a simple ParsedPolicy for testing.
   *
   * @return A simple ParsedPolicy
   */
  private ParsedPolicy createTestPolicy() {
    return new ParsedPolicy("testPolicy", Collections.emptyList());
  }

  /**
   * Test that PolicyFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    ParsedPolicy policy = createTestPolicy();
    PolicyFragment fragment = new PolicyFragment(policy);
    assertNotNull(fragment, "PolicyFragment should be constructable");
  }

  /**
   * Test the getPolicy method.
   */
  @Test
  public void testGetPolicy() {
    ParsedPolicy policy = createTestPolicy();
    PolicyFragment fragment = new PolicyFragment(policy);
    assertEquals(policy, fragment.getPolicy(),
        "getPolicy should return the correct ParsedPolicy");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    ParsedPolicy policy = createTestPolicy();
    PolicyFragment fragment = new PolicyFragment(policy);
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }

  /**
   * Test that getDuring throws RuntimeException.
   */
  @Test
  public void testGetDuringThrows() {
    ParsedPolicy policy = createTestPolicy();
    PolicyFragment fragment = new PolicyFragment(policy);
    assertThrows(RuntimeException.class, () -> fragment.getDuring(),
        "getDuring should throw RuntimeException");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    ParsedPolicy policy = createTestPolicy();
    PolicyFragment fragment = new PolicyFragment(policy);
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }

  /**
   * Test that getProgram throws RuntimeException.
   */
  @Test
  public void testGetProgramThrows() {
    ParsedPolicy policy = createTestPolicy();
    PolicyFragment fragment = new PolicyFragment(policy);
    assertThrows(RuntimeException.class, () -> fragment.getProgram(),
        "getProgram should throw RuntimeException");
  }

  /**
   * Test that getApplication throws RuntimeException.
   */
  @Test
  public void testGetApplicationThrows() {
    ParsedPolicy policy = createTestPolicy();
    PolicyFragment fragment = new PolicyFragment(policy);
    assertThrows(RuntimeException.class, () -> fragment.getApplication(),
        "getApplication should throw RuntimeException");
  }

  /**
   * Test that getScenario throws RuntimeException.
   */
  @Test
  public void testGetScenarioThrows() {
    ParsedPolicy policy = createTestPolicy();
    PolicyFragment fragment = new PolicyFragment(policy);
    assertThrows(RuntimeException.class, () -> fragment.getScenario(),
        "getScenario should throw RuntimeException");
  }

  /**
   * Test that getSubstance throws RuntimeException.
   */
  @Test
  public void testGetSubstanceThrows() {
    ParsedPolicy policy = createTestPolicy();
    PolicyFragment fragment = new PolicyFragment(policy);
    assertThrows(RuntimeException.class, () -> fragment.getSubstance(),
        "getSubstance should throw RuntimeException");
  }
}
