/**
 * Utility class for parsing numbers with flexible thousands and decimal separators.
 *
 * <p>This class provides functionality to parse number strings that may use either
 * comma (,) or period (.) as thousands separators or decimal separators, based on context analysis
 * of the input string. It handles European format (123.456,789) and US format (123,456.789)
 * intelligently using a strategy pattern.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.localization;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility for parsing numbers with flexible thousands and decimal separators.
 */
public class NumberParseUtil {

  private final List<NumberParseUtilStrategy> strategies;

  /**
   * Constructor that initializes the parsing strategies.
   */
  public NumberParseUtil() {
    this.strategies = Arrays.asList(
        new OnlyCommasParseStrategy(),
        new OnlyPeriodsParseStrategy(),
        new MixedParseStrategy()
    );
  }

  /**
   * Parse a number string with flexible thousands and decimal separators.
   *
   * <p>This method analyzes the input string to determine whether commas and periods
 * are being used as thousands separators or decimal separators. It supports both European format
 * (123.456,789) and US format (123,456.789).
   *
   * <div>Logic:
   * <ul>
   *   <li>Multiple occurrences of same character = thousands separator</li>
   *   <li>Single occurrence with ≠3 digits after = decimal separator</li>
   *   <li>Mixed separators: precedence rules apply (comma before period = comma thousands)</li>
   *   <li>Single occurrence with exactly 3 digits after = ambiguous (returns error)</li>
   * </ul>
   * </div>
   *
   * @param numberString The number string to parse
   * @return FlexibleNumberParseResult containing either parsed number or error message
   */
  public FlexibleNumberParseResult parseFlexibleNumber(String numberString) {
    if (numberString == null || numberString.trim().isEmpty()) {
      return new FlexibleNumberParseResult("Number string cannot be null or empty");
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
      return new FlexibleNumberParseResult("Invalid number format: '" + numberString + "'");
    }

    // If no separators, parse directly
    boolean noComma = !cleanedInput.contains(",");
    boolean noPeriod = !cleanedInput.contains(".");
    if (noComma && noPeriod) {
      try {
        BigDecimal result = new BigDecimal(cleanedInput);
        return new FlexibleNumberParseResult(isNegative ? result.negate() : result);
      } catch (NumberFormatException e) {
        return new FlexibleNumberParseResult("Invalid number format: '" + numberString + "'");
      }
    }

    // Count occurrences of comma and period
    int commaCount = countOccurrences(cleanedInput, ',');
    int periodCount = countOccurrences(cleanedInput, '.');

    // Use strategy pattern to determine format and parse accordingly
    NumberParseUtilStrategy strategy = sniffStrategy(cleanedInput, commaCount, periodCount);
    FlexibleNumberParseResult parseResult = strategy.parseNumber(cleanedInput, commaCount, periodCount);
    if (parseResult.isError()) {
      return parseResult;
    }

    try {
      BigDecimal result = parseResult.getParsedNumber().get();
      return new FlexibleNumberParseResult(isNegative ? result.negate() : result);
    } catch (NumberFormatException e) {
      return new FlexibleNumberParseResult("Invalid number format: '" + numberString + "'");
    }
  }

  /**
   * Determine which strategy to use based on the separator patterns in the input.
   *
   * @param numberStr The number string to analyze
   * @param commaCount Number of commas in the string
   * @param periodCount Number of periods in the string
   * @return NumberParseUtilStrategy that can handle the pattern
   */
  private NumberParseUtilStrategy sniffStrategy(String numberStr, int commaCount, int periodCount) {
    for (NumberParseUtilStrategy strategy : strategies) {
      if (strategy.canHandle(numberStr, commaCount, periodCount)) {
        return strategy;
      }
    }
    // Fallback to mixed strategy if no other strategy matches
    return new MixedParseStrategy();
  }

  /**
   * Count occurrences of a character in a string.
   *
   * @param str The string to search
   * @param ch The character to count
   * @return Number of occurrences
   */
  private int countOccurrences(String str, char ch) {
    String target = String.valueOf(ch);
    int originalLength = str.length();
    int lengthWithoutTarget = str.replaceAll(Pattern.quote(target), "").length();
    return originalLength - lengthWithoutTarget;
  }
}
