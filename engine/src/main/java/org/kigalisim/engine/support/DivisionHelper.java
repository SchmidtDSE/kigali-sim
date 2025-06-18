/**
 * Helper class for mathematical division operations.
 *
 * <p>This class provides utility methods for division operations with
 * special handling for edge cases like division by zero.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helper class for mathematical division operations.
 */
public class DivisionHelper {

  /**
   * Private constructor to prevent instantiation of utility class.
   */
  private DivisionHelper() {
    // Utility class - no instantiation
  }

  /**
   * Divide with a check for division by zero.
   *
   * @param numerator The numerator to use in the operation.
   * @param denominator The denominator to use in the operation.
   * @return Zero if denominator is zero, otherwise the result of regular division.
   */
  public static BigDecimal divideWithZero(BigDecimal numerator, BigDecimal denominator) {
    boolean denominatorIsZero = denominator.compareTo(BigDecimal.ZERO) == 0;
    if (denominatorIsZero) {
      return BigDecimal.ZERO;
    } else {
      return numerator.divide(denominator, 10, RoundingMode.HALF_UP);
    }
  }
}