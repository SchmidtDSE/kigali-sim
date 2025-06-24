/**
 * Unit tests for the SubstanceFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.program.ParsedSubstance;

/**
 * Tests for the SubstanceFragment class.
 */
public class SubstanceFragmentTest {

  /**
   * Helper method to create a simple ParsedSubstance for testing.
   *
   * @return A simple ParsedSubstance
   */
  private ParsedSubstance createTestSubstance() {
    return new ParsedSubstance("testSubstance", Collections.emptyList());
  }

  /**
   * Test that SubstanceFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    ParsedSubstance substance = createTestSubstance();
    SubstanceFragment fragment = new SubstanceFragment(substance);
    assertNotNull(fragment, "SubstanceFragment should be constructable");
  }

  /**
   * Test the getSubstance method.
   */
  @Test
  public void testGetSubstance() {
    ParsedSubstance substance = createTestSubstance();
    SubstanceFragment fragment = new SubstanceFragment(substance);
    assertEquals(substance, fragment.getSubstance(),
        "getSubstance should return the correct ParsedSubstance");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    ParsedSubstance substance = createTestSubstance();
    SubstanceFragment fragment = new SubstanceFragment(substance);
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }

  /**
   * Test that getDuring throws RuntimeException.
   */
  @Test
  public void testGetDuringThrows() {
    ParsedSubstance substance = createTestSubstance();
    SubstanceFragment fragment = new SubstanceFragment(substance);
    assertThrows(RuntimeException.class, () -> fragment.getDuring(),
        "getDuring should throw RuntimeException");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    ParsedSubstance substance = createTestSubstance();
    SubstanceFragment fragment = new SubstanceFragment(substance);
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }

  /**
   * Test that getProgram throws RuntimeException.
   */
  @Test
  public void testGetProgramThrows() {
    ParsedSubstance substance = createTestSubstance();
    SubstanceFragment fragment = new SubstanceFragment(substance);
    assertThrows(RuntimeException.class, () -> fragment.getProgram(),
        "getProgram should throw RuntimeException");
  }

  /**
   * Test that getPolicy throws RuntimeException.
   */
  @Test
  public void testGetPolicyThrows() {
    ParsedSubstance substance = createTestSubstance();
    SubstanceFragment fragment = new SubstanceFragment(substance);
    assertThrows(RuntimeException.class, () -> fragment.getPolicy(),
        "getPolicy should throw RuntimeException");
  }

  /**
   * Test that getApplication throws RuntimeException.
   */
  @Test
  public void testGetApplicationThrows() {
    ParsedSubstance substance = createTestSubstance();
    SubstanceFragment fragment = new SubstanceFragment(substance);
    assertThrows(RuntimeException.class, () -> fragment.getApplication(),
        "getApplication should throw RuntimeException");
  }

  /**
   * Test that getScenario throws RuntimeException.
   */
  @Test
  public void testGetScenarioThrows() {
    ParsedSubstance substance = createTestSubstance();
    SubstanceFragment fragment = new SubstanceFragment(substance);
    assertThrows(RuntimeException.class, () -> fragment.getScenario(),
        "getScenario should throw RuntimeException");
  }
}
