/**
 * Unit tests for the AboutStanzaFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for the AboutStanzaFragment class.
 */
public class AboutStanzaFragmentTest {

  /**
   * Test that AboutStanzaFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertNotNull(fragment, "AboutStanzaFragment should be constructable");
  }

  /**
   * Test the getIsStanzaScenarios method.
   */
  @Test
  public void testGetIsStanzaScenarios() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertFalse(fragment.getIsStanzaScenarios(),
        "getIsStanzaScenarios should return false");
  }

  /**
   * Test the getIsStanzaPolicyOrDefault method.
   */
  @Test
  public void testGetIsStanzaPolicyOrDefault() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertFalse(fragment.getIsStanzaPolicyOrDefault(),
        "getIsStanzaPolicyOrDefault should return false");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }

  /**
   * Test that getDuring throws RuntimeException.
   */
  @Test
  public void testGetDuringThrows() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertThrows(RuntimeException.class, () -> fragment.getDuring(),
        "getDuring should throw RuntimeException");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }

  /**
   * Test that getProgram throws RuntimeException.
   */
  @Test
  public void testGetProgramThrows() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertThrows(RuntimeException.class, () -> fragment.getProgram(),
        "getProgram should throw RuntimeException");
  }

  /**
   * Test that getPolicy throws RuntimeException.
   */
  @Test
  public void testGetPolicyThrows() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertThrows(RuntimeException.class, () -> fragment.getPolicy(),
        "getPolicy should throw RuntimeException");
  }

  /**
   * Test that getApplication throws RuntimeException.
   */
  @Test
  public void testGetApplicationThrows() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertThrows(RuntimeException.class, () -> fragment.getApplication(),
        "getApplication should throw RuntimeException");
  }

  /**
   * Test that getScenario throws RuntimeException.
   */
  @Test
  public void testGetScenarioThrows() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertThrows(RuntimeException.class, () -> fragment.getScenario(),
        "getScenario should throw RuntimeException");
  }

  /**
   * Test that getSubstance throws RuntimeException.
   */
  @Test
  public void testGetSubstanceThrows() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertThrows(RuntimeException.class, () -> fragment.getSubstance(),
        "getSubstance should throw RuntimeException");
  }

  /**
   * Test that getScenarios throws RuntimeException.
   */
  @Test
  public void testGetScenariosThrows() {
    AboutStanzaFragment fragment = new AboutStanzaFragment();
    assertThrows(RuntimeException.class, () -> fragment.getScenarios(),
        "getScenarios should throw RuntimeException");
  }
}