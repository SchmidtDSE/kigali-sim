/**
 * Test for DivisionHelper utility class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

/**
 * Tests for DivisionHelper.
 */
public class DivisionHelperTest {

  @Test
  public void testDivideWithZeroDenominator() {
    BigDecimal numerator = new BigDecimal("10.5");
    BigDecimal denominator = BigDecimal.ZERO;
    BigDecimal result = DivisionHelper.divideWithZero(numerator, denominator);
    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  public void testDivideWithNormalValues() {
    BigDecimal numerator = new BigDecimal("10.0");
    BigDecimal denominator = new BigDecimal("3.0");
    BigDecimal result = DivisionHelper.divideWithZero(numerator, denominator);
    BigDecimal expected = new BigDecimal("10.0")
        .divide(new BigDecimal("3.0"), 10, RoundingMode.HALF_UP);
    assertEquals(expected, result);
  }

  @Test
  public void testDivideWithZeroNumerator() {
    BigDecimal numerator = BigDecimal.ZERO;
    BigDecimal denominator = new BigDecimal("5.0");
    BigDecimal result = DivisionHelper.divideWithZero(numerator, denominator);
    assertEquals(0, result.compareTo(BigDecimal.ZERO));
  }

  @Test
  public void testDivideWithEqualValues() {
    BigDecimal numerator = new BigDecimal("7.5");
    BigDecimal denominator = new BigDecimal("7.5");
    BigDecimal result = DivisionHelper.divideWithZero(numerator, denominator);
    assertEquals(0, result.compareTo(BigDecimal.ONE));
  }
}
