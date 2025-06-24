/**
 * Unit tests for the TimePointRealized class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for the TimePointRealized class.
 */
public class TimePointRealizedTest {

  /**
   * Test that TimePointRealized can be initialized with a point value.
   */
  @Test
  public void testInitializesWithPointValue() {
    EngineNumber pointValue = new EngineNumber(BigDecimal.valueOf(2020), "");
    TimePointRealized timePoint = new TimePointRealized(pointValue);
    assertNotNull(timePoint, "TimePointRealized should be constructable with a point value");
  }

  /**
   * Test that TimePointRealized can be initialized with a dynamic cap.
   */
  @Test
  public void testInitializesWithDynamicCap() {
    TimePointRealized timePoint = new TimePointRealized("beginning");
    assertNotNull(timePoint, "TimePointRealized should be constructable with a dynamic cap");
  }

  /**
   * Test the isDynamicCap method with a point value.
   */
  @Test
  public void testIsDynamicCapWithPointValue() {
    EngineNumber pointValue = new EngineNumber(BigDecimal.valueOf(2020), "");
    TimePointRealized timePoint = new TimePointRealized(pointValue);
    assertFalse(timePoint.isDynamicCap(), "isDynamicCap should return false for a point value");
  }

  /**
   * Test the isDynamicCap method with a dynamic cap.
   */
  @Test
  public void testIsDynamicCapWithDynamicCap() {
    TimePointRealized timePoint = new TimePointRealized("beginning");
    assertTrue(timePoint.isDynamicCap(), "isDynamicCap should return true for a dynamic cap");
  }

  /**
   * Test the getPointValue method with a point value.
   */
  @Test
  public void testGetPointValue() {
    EngineNumber pointValue = new EngineNumber(BigDecimal.valueOf(2020), "");
    TimePointRealized timePoint = new TimePointRealized(pointValue);
    assertEquals(pointValue, timePoint.getPointValue(), "getPointValue should return the correct point value");
  }

  /**
   * Test that getPointValue throws an exception with a dynamic cap.
   */
  @Test
  public void testGetPointValueWithDynamicCap() {
    TimePointRealized timePoint = new TimePointRealized("beginning");
    assertThrows(Exception.class, () -> timePoint.getPointValue(),
        "getPointValue should throw an exception for a dynamic cap");
  }

  /**
   * Test the getDynamicCap method with a dynamic cap.
   */
  @Test
  public void testGetDynamicCap() {
    String dynamicCap = "beginning";
    TimePointRealized timePoint = new TimePointRealized(dynamicCap);
    assertEquals(dynamicCap, timePoint.getDynamicCap(), "getDynamicCap should return the correct dynamic cap");
  }

  /**
   * Test that getDynamicCap throws an exception with a point value.
   */
  @Test
  public void testGetDynamicCapWithPointValue() {
    EngineNumber pointValue = new EngineNumber(BigDecimal.valueOf(2020), "");
    TimePointRealized timePoint = new TimePointRealized(pointValue);
    assertThrows(Exception.class, () -> timePoint.getDynamicCap(),
        "getDynamicCap should throw an exception for a point value");
  }
}
