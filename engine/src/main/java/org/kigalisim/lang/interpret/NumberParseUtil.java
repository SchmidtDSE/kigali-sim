/**
 * Utility class for parsing numbers with flexible thousands and decimal separators.
 *
 * <p>This class provides functionality to parse number strings that may use either
 * comma (,) or period (.) as thousands separators or decimal separators, based on
 * context analysis of the input string. It handles European format (123.456,789)
 * and US format (123,456.789) intelligently.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.interpret;

import java.math.BigDecimal;

/**
 * Utility for parsing numbers with flexible thousands and decimal separators.
 */
public class NumberParseUtil {

  /**
   * Parse a number string with flexible thousands and decimal separators.
   *
   * <p>This method analyzes the input string to determine whether commas and periods
   * are being used as thousands separators or decimal separators. It supports both
   * European format (123.456,789) and US format (123,456.789).</p>
   *
   * <p>Logic:</p>
   * <ul>
   *   <li>Multiple occurrences of same character = thousands separator</li>
   *   <li>Single occurrence with ≠3 digits after = decimal separator</li>
   *   <li>Mixed separators: precedence rules apply (comma before period = comma thousands)</li>
   *   <li>Single occurrence with exactly 3 digits after = ambiguous (throws exception)</li>
   * </ul>
   *
   * @param numberString The number string to parse
   * @return BigDecimal representation of the parsed number
   * @throws NumberFormatException if the number format is ambiguous or invalid
   */
  public BigDecimal parseFlexibleNumber(String numberString) throws NumberFormatException {
    if (numberString == null || numberString.trim().isEmpty()) {
      throw new NumberFormatException("Number string cannot be null or empty");
    }

    // Clean whitespace and handle sign
    String cleanedInput = numberString.trim();
    boolean isNegative = cleanedInput.startsWith("-");
    boolean hasPositiveSign = cleanedInput.startsWith("+");

    if (isNegative || hasPositiveSign) {
      cleanedInput = cleanedInput.substring(1);
    }

    // Check for invalid start/end patterns
    // Allow numbers that start with decimal point like .5 but not with comma
    if (cleanedInput.startsWith(",") || cleanedInput.endsWith(",") || cleanedInput.endsWith(".")) {
      throw new NumberFormatException("Invalid number format: '" + numberString + "'");
    }

    // If no separators, parse directly
    if (!cleanedInput.contains(",") && !cleanedInput.contains(".")) {
      try {
        BigDecimal result = new BigDecimal(cleanedInput);
        return isNegative ? result.negate() : result;
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Invalid number format: '" + numberString + "'");
      }
    }

    // Count occurrences of comma and period
    int commaCount = countOccurrences(cleanedInput, ',');
    int periodCount = countOccurrences(cleanedInput, '.');

    // Determine format and parse accordingly
    String parsedNumber = determineFormatAndParse(cleanedInput, commaCount, periodCount);

    try {
      BigDecimal result = new BigDecimal(parsedNumber);
      return isNegative ? result.negate() : result;
    } catch (NumberFormatException e) {
      throw new NumberFormatException("Invalid number format: '" + numberString + "'");
    }
  }

  /**
   * Determine the format of the number string and return a cleaned version for parsing.
   *
   * @param numberStr The number string (without sign)
   * @param commaCount Number of commas in the string
   * @param periodCount Number of periods in the string
   * @return Cleaned number string ready for BigDecimal parsing
   * @throws NumberFormatException if format is ambiguous or invalid
   */
  private String determineFormatAndParse(String numberStr, int commaCount, int periodCount)
      throws NumberFormatException {

    // Case 1: Only commas (no periods)
    if (commaCount > 0 && periodCount == 0) {
      return handleSingleSeparatorType(numberStr, ',', commaCount);
    }

    // Case 2: Only periods (no commas)
    if (periodCount > 0 && commaCount == 0) {
      return handleSingleSeparatorType(numberStr, '.', periodCount);
    }

    // Case 3: Both commas and periods present - apply precedence rules
    if (commaCount > 0 && periodCount > 0) {
      return handleMixedSeparators(numberStr);
    }

    // Should not reach here
    return numberStr;
  }

  /**
   * Handle numbers with only one type of separator (either comma or period).
   *
   * @param numberStr The number string
   * @param separator The separator character (',' or '.')
   * @param count Number of occurrences of the separator
   * @return Cleaned number string for parsing
   * @throws NumberFormatException if format is ambiguous
   */
  private String handleSingleSeparatorType(String numberStr, char separator, int count)
      throws NumberFormatException {

    if (count > 1) {
      // Multiple occurrences = thousands separator
      validateThousandsSeparatorPositions(numberStr, separator);
      return numberStr.replace(String.valueOf(separator), "");
    } else {
      // Single occurrence - check digit count after and position
      int separatorIndex = numberStr.indexOf(separator);
      int digitsAfter = numberStr.length() - separatorIndex - 1;
      int digitsBefore = separatorIndex;

      // Special case: if number starts with separator, it's always a decimal separator
      if (digitsBefore == 0) {
        if (separator == ',') {
          // Convert comma decimal to period decimal
          return numberStr.replace(",", ".");
        } else {
          // Already a period decimal
          return numberStr;
        }
      }

      if (digitsAfter == 3) {
        // Special case: if we have exactly 3 digits after AND the number before is >= 4 digits
        // OR if it's a common thousands separator pattern (like 1,000 or 10,000)
        // then treat it as thousands separator, not decimal
        if (digitsBefore >= 4 || isLikelyThousandsSeparator(numberStr, separatorIndex)) {
          return numberStr.replace(String.valueOf(separator), "");
        } else {
          // Truly ambiguous case - could be 123,456 (thousands) or 123.456 (decimal)
          throw new NumberFormatException(String.format(
              "Ambiguous number format: '%s'. Cannot determine if '%s' is a thousands "
              + "separator or decimal separator. Suggestions: Use '%s.0' for thousands "
              + "separator or change format to disambiguate.",
              numberStr, separator, numberStr
          ));
        }
      } else {
        // Not exactly 3 digits after = decimal separator
        if (separator == ',') {
          // Convert comma decimal to period decimal
          return numberStr.replace(",", ".");
        } else {
          // Already a period decimal
          return numberStr;
        }
      }
    }
  }

  /**
   * Determine if a single separator with 3 digits after is likely a thousands separator.
   *
   * @param numberStr The complete number string
   * @param separatorIndex The index of the separator
   * @return true if it's likely a thousands separator
   */
  private boolean isLikelyThousandsSeparator(String numberStr, int separatorIndex) {
    int digitsBefore = separatorIndex;

    // Common thousands patterns: 1,000  10,000  100,000 etc.
    // If digits before is 1-3 and we have exactly 3 zeros after, likely thousands
    if (digitsBefore >= 1 && digitsBefore <= 3) {
      String afterSeparator = numberStr.substring(separatorIndex + 1);
      // Check for common thousands patterns like 1,000 or 10,000
      if ("000".equals(afterSeparator)) {
        return true;
      }
    }

    // If we have 4 or more digits before the separator, it's likely thousands
    return digitsBefore >= 4;
  }

  /**
   * Handle numbers with both commas and periods present.
   *
   * @param numberStr The number string containing both separators
   * @return Cleaned number string for parsing
   * @throws NumberFormatException if format is invalid
   */
  private String handleMixedSeparators(String numberStr) throws NumberFormatException {
    int lastCommaIndex = numberStr.lastIndexOf(',');
    int lastPeriodIndex = numberStr.lastIndexOf('.');

    if (lastCommaIndex < lastPeriodIndex) {
      // Comma comes before period: comma = thousands, period = decimal (US format)
      // Example: 123,456.789
      String withoutThousands = numberStr.replace(",", "");
      validateSingleDecimalSeparator(withoutThousands, '.');
      // Also validate thousands separator positioning if any commas
      validateThousandsSeparatorPositions(numberStr.substring(0, lastPeriodIndex), ',');
      return withoutThousands;
    } else {
      // Period comes before comma: period = thousands, comma = decimal (European format)
      // Example: 123.456,789
      String withoutThousands = numberStr.replace(".", "");
      validateSingleDecimalSeparator(withoutThousands, ',');
      // Validate thousands separator positioning for periods
      validateThousandsSeparatorPositions(numberStr.substring(0, lastCommaIndex), '.');
      // Convert comma decimal to period decimal
      return withoutThousands.replace(",", ".");
    }
  }

  /**
   * Validate that thousands separators are positioned correctly.
   *
   * @param numberStr The number string
   * @param separator The thousands separator character
   * @throws NumberFormatException if positioning is invalid
   */
  private void validateThousandsSeparatorPositions(String numberStr, char separator)
      throws NumberFormatException {
    // Skip validation if no separators or empty string
    if (numberStr == null || numberStr.isEmpty() || !numberStr.contains(String.valueOf(separator))) {
      return;
    }

    String[] parts = numberStr.split("\\" + separator);

    // Must have at least 2 parts for a valid thousands separator
    if (parts.length < 2) {
      return;
    }

    // First part can be 1-3 digits
    if (parts[0].isEmpty() || parts[0].length() > 3) {
      throw new NumberFormatException("Invalid thousands separator format: '" + numberStr + "'");
    }

    // All subsequent parts must be exactly 3 digits
    for (int i = 1; i < parts.length; i++) {
      if (parts[i].length() != 3 || !parts[i].matches("\\d{3}")) {
        throw new NumberFormatException("Invalid thousands separator format: '" + numberStr + "'");
      }
    }
  }

  /**
   * Validate that there's only one decimal separator after removing thousands separators.
   *
   * @param numberStr The number string with thousands separators removed
   * @param decimalSeparator The expected decimal separator
   * @throws NumberFormatException if there are multiple decimal separators
   */
  private void validateSingleDecimalSeparator(String numberStr, char decimalSeparator)
      throws NumberFormatException {
    int count = countOccurrences(numberStr, decimalSeparator);
    if (count > 1) {
      throw new NumberFormatException("Invalid number format: multiple decimal separators in '"
          + numberStr + "'");
    }
  }

  /**
   * Count occurrences of a character in a string.
   *
   * @param str The string to search
   * @param ch The character to count
   * @return Number of occurrences
   */
  private int countOccurrences(String str, char ch) {
    int count = 0;
    for (int i = 0; i < str.length(); i++) {
      if (str.charAt(i) == ch) {
        count++;
      }
    }
    return count;
  }
}
