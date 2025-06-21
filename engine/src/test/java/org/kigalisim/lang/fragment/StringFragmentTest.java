/**
 * Unit tests for the StringFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for the StringFragment class.
 */
public class StringFragmentTest {

  /**
   * Test that StringFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    String value = "test string";
    StringFragment fragment = new StringFragment(value);
    assertNotNull(fragment, "StringFragment should be constructable");
  }

  /**
   * Test the getString method.
   */
  @Test
  public void testGetString() {
    String value = "test string";
    StringFragment fragment = new StringFragment(value);
    assertEquals(value, fragment.getString(), "getString should return the correct string value");
  }

  /**
   * Test the getString method with empty string.
   */
  @Test
  public void testGetStringEmpty() {
    String value = "";
    StringFragment fragment = new StringFragment(value);
    assertEquals(value, fragment.getString(), "getString should return empty string when initialized with empty string");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    String value = "test string";
    StringFragment fragment = new StringFragment(value);
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    String value = "test string";
    StringFragment fragment = new StringFragment(value);
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }
}