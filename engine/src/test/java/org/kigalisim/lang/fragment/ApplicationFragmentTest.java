/**
 * Unit tests for the ApplicationFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.program.ParsedApplication;

/**
 * Tests for the ApplicationFragment class.
 */
public class ApplicationFragmentTest {

  /**
   * Helper method to create a simple ParsedApplication for testing.
   *
   * @return A simple ParsedApplication
   */
  private ParsedApplication createTestApplication() {
    return new ParsedApplication("testApp", Collections.emptyList());
  }

  /**
   * Test that ApplicationFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    ParsedApplication application = createTestApplication();
    ApplicationFragment fragment = new ApplicationFragment(application);
    assertNotNull(fragment, "ApplicationFragment should be constructable");
  }

  /**
   * Test the getApplication method.
   */
  @Test
  public void testGetApplication() {
    ParsedApplication application = createTestApplication();
    ApplicationFragment fragment = new ApplicationFragment(application);
    assertEquals(application, fragment.getApplication(),
        "getApplication should return the correct ParsedApplication");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    ParsedApplication application = createTestApplication();
    ApplicationFragment fragment = new ApplicationFragment(application);
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }

  /**
   * Test that getDuring throws RuntimeException.
   */
  @Test
  public void testGetDuringThrows() {
    ParsedApplication application = createTestApplication();
    ApplicationFragment fragment = new ApplicationFragment(application);
    assertThrows(RuntimeException.class, () -> fragment.getDuring(),
        "getDuring should throw RuntimeException");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    ParsedApplication application = createTestApplication();
    ApplicationFragment fragment = new ApplicationFragment(application);
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }

  /**
   * Test that getProgram throws RuntimeException.
   */
  @Test
  public void testGetProgramThrows() {
    ParsedApplication application = createTestApplication();
    ApplicationFragment fragment = new ApplicationFragment(application);
    assertThrows(RuntimeException.class, () -> fragment.getProgram(),
        "getProgram should throw RuntimeException");
  }

  /**
   * Test that getPolicy throws RuntimeException.
   */
  @Test
  public void testGetPolicyThrows() {
    ParsedApplication application = createTestApplication();
    ApplicationFragment fragment = new ApplicationFragment(application);
    assertThrows(RuntimeException.class, () -> fragment.getPolicy(),
        "getPolicy should throw RuntimeException");
  }

  /**
   * Test that getScenario throws RuntimeException.
   */
  @Test
  public void testGetScenarioThrows() {
    ParsedApplication application = createTestApplication();
    ApplicationFragment fragment = new ApplicationFragment(application);
    assertThrows(RuntimeException.class, () -> fragment.getScenario(),
        "getScenario should throw RuntimeException");
  }

  /**
   * Test that getSubstance throws RuntimeException.
   */
  @Test
  public void testGetSubstanceThrows() {
    ParsedApplication application = createTestApplication();
    ApplicationFragment fragment = new ApplicationFragment(application);
    assertThrows(RuntimeException.class, () -> fragment.getSubstance(),
        "getSubstance should throw RuntimeException");
  }
}
