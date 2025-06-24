/**
 * Unit tests for the ProgramFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Tests for the ProgramFragment class.
 */
public class ProgramFragmentTest {

  /**
   * Helper method to create a simple ParsedProgram for testing.
   *
   * @return A simple ParsedProgram
   */
  private ParsedProgram createTestProgram() {
    return new ParsedProgram(Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Test that ProgramFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    ParsedProgram program = createTestProgram();
    ProgramFragment fragment = new ProgramFragment(program);
    assertNotNull(fragment, "ProgramFragment should be constructable");
  }

  /**
   * Test the getProgram method.
   */
  @Test
  public void testGetProgram() {
    ParsedProgram program = createTestProgram();
    ProgramFragment fragment = new ProgramFragment(program);
    assertEquals(program, fragment.getProgram(),
        "getProgram should return the correct ParsedProgram");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    ParsedProgram program = createTestProgram();
    ProgramFragment fragment = new ProgramFragment(program);
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }

  /**
   * Test that getDuring throws RuntimeException.
   */
  @Test
  public void testGetDuringThrows() {
    ParsedProgram program = createTestProgram();
    ProgramFragment fragment = new ProgramFragment(program);
    assertThrows(RuntimeException.class, () -> fragment.getDuring(),
        "getDuring should throw RuntimeException");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    ParsedProgram program = createTestProgram();
    ProgramFragment fragment = new ProgramFragment(program);
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }

  /**
   * Test that getPolicy throws RuntimeException.
   */
  @Test
  public void testGetPolicyThrows() {
    ParsedProgram program = createTestProgram();
    ProgramFragment fragment = new ProgramFragment(program);
    assertThrows(RuntimeException.class, () -> fragment.getPolicy(),
        "getPolicy should throw RuntimeException");
  }

  /**
   * Test that getApplication throws RuntimeException.
   */
  @Test
  public void testGetApplicationThrows() {
    ParsedProgram program = createTestProgram();
    ProgramFragment fragment = new ProgramFragment(program);
    assertThrows(RuntimeException.class, () -> fragment.getApplication(),
        "getApplication should throw RuntimeException");
  }

  /**
   * Test that getScenario throws RuntimeException.
   */
  @Test
  public void testGetScenarioThrows() {
    ParsedProgram program = createTestProgram();
    ProgramFragment fragment = new ProgramFragment(program);
    assertThrows(RuntimeException.class, () -> fragment.getScenario(),
        "getScenario should throw RuntimeException");
  }

  /**
   * Test that getSubstance throws RuntimeException.
   */
  @Test
  public void testGetSubstanceThrows() {
    ParsedProgram program = createTestProgram();
    ProgramFragment fragment = new ProgramFragment(program);
    assertThrows(RuntimeException.class, () -> fragment.getSubstance(),
        "getSubstance should throw RuntimeException");
  }
}
