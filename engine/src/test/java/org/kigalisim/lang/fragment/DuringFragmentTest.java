/**
 * Unit tests for the DuringFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.time.ParsedDuring;
import org.kigalisim.lang.time.TimePointFuture;

/**
 * Tests for the DuringFragment class.
 */
public class DuringFragmentTest {

  /**
   * Test that DuringFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    DuringFragment fragment = new DuringFragment(during);
    assertNotNull(fragment, "DuringFragment should be constructable");
  }

  /**
   * Test the getDuring method.
   */
  @Test
  public void testGetDuring() {
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    DuringFragment fragment = new DuringFragment(during);
    assertEquals(during, fragment.getDuring(), "getDuring should return the correct ParsedDuring");
  }

  /**
   * Test that getOperation throws RuntimeException.
   */
  @Test
  public void testGetOperationThrows() {
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    DuringFragment fragment = new DuringFragment(during);
    assertThrows(RuntimeException.class, () -> fragment.getOperation(),
        "getOperation should throw RuntimeException");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    DuringFragment fragment = new DuringFragment(during);
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }
}