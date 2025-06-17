/**
 * Unit tests for the YearMatcher class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the YearMatcher class.
 */
public class YearMatcherTest {

  /**
   * Test that YearMatcher can be initialized.
   */
  @Test
  public void testInitializes() {
    YearMatcher matcher = new YearMatcher(null, null);
    assertNotNull(matcher, "YearMatcher should be constructable");
  }

  /**
   * Test that matcher matches any year when both bounds are null.
   */
  @Test
  public void testMatchesAny() {
    YearMatcher matcher = new YearMatcher(null, null);
    assertTrue(matcher.getInRange(1), "Should match any year when unbounded");
  }

  /**
   * Test that matcher matches years after a given start.
   */
  @Test
  public void testMatchesAfter() {
    YearMatcher matcher = new YearMatcher(2, null);
    assertFalse(matcher.getInRange(1), "Should not match year before start");
    assertTrue(matcher.getInRange(2), "Should match year at start");
    assertTrue(matcher.getInRange(3), "Should match year after start");
  }

  /**
   * Test that matcher matches years before a given end.
   */
  @Test
  public void testMatchesBefore() {
    YearMatcher matcher = new YearMatcher(null, 2);
    assertTrue(matcher.getInRange(1), "Should match year before end");
    assertTrue(matcher.getInRange(2), "Should match year at end");
    assertFalse(matcher.getInRange(3), "Should not match year after end");
  }

  /**
   * Test that matcher matches years within a given range.
   */
  @Test
  public void testMatchesWithin() {
    YearMatcher matcher = new YearMatcher(2, 3);
    assertFalse(matcher.getInRange(1), "Should not match year before range");
    assertTrue(matcher.getInRange(2), "Should match year at start of range");
    assertTrue(matcher.getInRange(3), "Should match year at end of range");
    assertFalse(matcher.getInRange(4), "Should not match year after range");
  }

  /**
   * Test that matcher matches years within a range when given in reverse order.
   */
  @Test
  public void testMatchesWithinReverse() {
    YearMatcher matcher = new YearMatcher(3, 2);
    assertFalse(matcher.getInRange(1), "Should not match year before range");
    assertTrue(matcher.getInRange(2), "Should match year at start of range");
    assertTrue(matcher.getInRange(3), "Should match year at end of range");
    assertFalse(matcher.getInRange(4), "Should not match year after range");
  }

  /**
   * Test getStart method.
   */
  @Test
  public void testGetStart() {
    YearMatcher matcher1 = new YearMatcher(2, 5);
    assertEquals(Integer.valueOf(2), matcher1.getStart(), "Should return start year");

    YearMatcher matcher2 = new YearMatcher(null, 5);
    assertEquals(null, matcher2.getStart(), "Should return null for unbounded start");
  }

  /**
   * Test getEnd method.
   */
  @Test
  public void testGetEnd() {
    YearMatcher matcher1 = new YearMatcher(2, 5);
    assertEquals(Integer.valueOf(5), matcher1.getEnd(), "Should return end year");

    YearMatcher matcher2 = new YearMatcher(2, null);
    assertEquals(null, matcher2.getEnd(), "Should return null for unbounded end");
  }
}
