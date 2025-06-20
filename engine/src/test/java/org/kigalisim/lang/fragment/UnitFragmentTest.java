/**
 * Unit tests for the UnitFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for the UnitFragment class.
 */
public class UnitFragmentTest {

  /**
   * Test that UnitFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    String unit = "kg";
    UnitFragment fragment = new UnitFragment(unit);
    assertNotNull(fragment, "UnitFragment should be constructable");
  }

  /**
   * Test the getUnit method.
   */
  @Test
  public void testGetUnit() {
    String unit = "kg";
    UnitFragment fragment = new UnitFragment(unit);
    assertEquals(unit, fragment.getUnit(), "getUnit should return the correct unit");
  }

  /**
   * Test the getUnit method with empty unit.
   */
  @Test
  public void testGetUnitEmpty() {
    String unit = "";
    UnitFragment fragment = new UnitFragment(unit);
    assertEquals(unit, fragment.getUnit(), "getUnit should return empty string when initialized with empty string");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    String unit = "kg";
    UnitFragment fragment = new UnitFragment(unit);
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }

  /**
   * Test that getDuring throws RuntimeException.
   */
  @Test
  public void testGetDuringThrows() {
    String unit = "kg";
    UnitFragment fragment = new UnitFragment(unit);
    assertThrows(RuntimeException.class, () -> fragment.getDuring(),
        "getDuring should throw RuntimeException");
  }
}