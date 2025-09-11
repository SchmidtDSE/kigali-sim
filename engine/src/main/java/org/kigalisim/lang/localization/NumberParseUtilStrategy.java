/**
 * Strategy interface for parsing numbers with different separator conventions.
 *
 * <p>This interface defines the contract for different number parsing strategies
 * that handle various combinations of thousands and decimal separators.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.localization;

import org.kigalisim.lang.localization.FlexibleNumberParseResult;

/**
 * Strategy for parsing numbers with specific separator patterns.
 */
public interface NumberParseUtilStrategy {

  /**
   * Parse a number string using this strategy's specific rules.
   *
   * @param numberStr The cleaned number string (without sign)
   * @param commaCount Number of commas in the string
   * @param periodCount Number of periods in the string
   * @return FlexibleNumberParseResult containing either parsed number or error
   */
  FlexibleNumberParseResult parseNumber(String numberStr, int commaCount, int periodCount);

  /**
   * Determine if this strategy can handle the given separator pattern.
   *
   * @param numberStr The number string to analyze
   * @param commaCount Number of commas in the string
   * @param periodCount Number of periods in the string
   * @return true if this strategy can handle the pattern
   */
  boolean canHandle(String numberStr, int commaCount, int periodCount);
}