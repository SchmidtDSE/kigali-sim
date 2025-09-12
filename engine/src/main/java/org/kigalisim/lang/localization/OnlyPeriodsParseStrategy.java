/**
 * Strategy for parsing numbers that contain only period separators.
 *
 * <p>This strategy handles numbers like 1.234 or 1.234.567 or 123.45 (US decimal)
 * where only periods are used as separators.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.localization;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.kigalisim.lang.localization.FlexibleNumberParseResult;

/**
 * Parse strategy for numbers with only period separators.
 */
public class OnlyPeriodsParseStrategy implements NumberParseUtilStrategy {

  @Override
  public boolean canHandle(String numberStr, int commaCount, int periodCount) {
    return periodCount > 0 && commaCount == 0;
  }

  @Override
  public FlexibleNumberParseResult parseNumber(String numberStr, int commaCount, int periodCount) {
    if (!canHandle(numberStr, commaCount, periodCount)) {
      return new FlexibleNumberParseResult("OnlyPeriodsParseStrategy cannot handle this pattern");
    }

    return handleSingleSeparatorType(numberStr, '.', periodCount);
  }

  /**
   * Handle numbers with only period separators.
   *
   * @param numberStr The number string
   * @param separator The separator character (always period for this strategy)
   * @param count Number of occurrences of the separator
   * @return FlexibleNumberParseResult containing either parsed number or error
   */
  private FlexibleNumberParseResult handleSingleSeparatorType(String numberStr, char separator, int count) {
    if (count > 1) {
      // Multiple occurrences could be European thousands separators - reject with suggestion
      if (isEuropeanThousandsPattern(numberStr)) {
        String suggestion = generateUkFormatSuggestion(numberStr);
        return new FlexibleNumberParseResult(String.format(
            "Unsupported number format: '%s'. Please use: '%s'. "
            + "Kigali Sim requires comma for thousands separator and period for decimal point.",
            numberStr, suggestion
        ));
      }
      // Multiple occurrences = thousands separator (should not happen in UK format)
      FlexibleNumberParseResult validationResult = validateThousandsSeparatorPositions(numberStr, separator);
      if (validationResult.isError()) {
        return validationResult;
      }
      return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(String.valueOf(separator), "")));
    } else {
      // Single occurrence - check digit count after and position
      int separatorIndex = numberStr.indexOf(separator);
      int digitsAfter = numberStr.length() - separatorIndex - 1;
      int digitsBefore = separatorIndex;

      // Special case: if number starts with separator, it's always a decimal separator
      if (digitsBefore == 0) {
        // Already a period decimal
        return new FlexibleNumberParseResult(new BigDecimal(numberStr));
      }

      if (digitsAfter == 3) {
        // If digits before are 4 or more like 1234.5, treat as decimal separator
        if (digitsBefore >= 4) {
          // Already a period decimal
          return new FlexibleNumberParseResult(new BigDecimal(numberStr));
        } else if (isLikelyThousandsSeparator(numberStr, separatorIndex)) {
          return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(String.valueOf(separator), "")));
        } else if (numberStr.startsWith("0.")) {
          // Numbers starting with 0. are clearly decimals (like 0.035)
          return new FlexibleNumberParseResult(new BigDecimal(numberStr));
        } else {
          // Previously ambiguous case - now interpret as UK format (decimal separator)
          return new FlexibleNumberParseResult(new BigDecimal(numberStr));
        }
      } else {
        // Not exactly 3 digits after = decimal separator
        // Already a period decimal
        return new FlexibleNumberParseResult(new BigDecimal(numberStr));
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

    // If number starts with 0 (like 0.035), it's clearly a decimal
    if (numberStr.startsWith("0.")) {
      return false;
    }

    // Don't make assumptions about N.000 patterns - treat them as ambiguous
    // Only consider thousands separator if we have 4 or more digits before the separator
    return digitsBefore >= 4;
  }

  /**
   * Validate that thousands separators are positioned correctly.
   *
   * @param numberStr The number string
   * @param separator The thousands separator character
   * @return FlexibleNumberParseResult with success or error
   */
  private FlexibleNumberParseResult validateThousandsSeparatorPositions(String numberStr, char separator) {
    // Skip validation if no separators or empty string
    if (numberStr == null || numberStr.isEmpty() || !numberStr.contains(String.valueOf(separator))) {
      return new FlexibleNumberParseResult(new BigDecimal("0")); // Success case
    }

    String[] parts = numberStr.split(Pattern.quote(String.valueOf(separator)));

    // Must have at least 2 parts for a valid thousands separator
    if (parts.length < 2) {
      return new FlexibleNumberParseResult(new BigDecimal("0")); // Success case
    }

    // First part can be 1-3 digits
    if (parts[0].isEmpty() || parts[0].length() > 3) {
      return new FlexibleNumberParseResult("Invalid thousands separator format: '" + numberStr + "'");
    }

    // All subsequent parts must be exactly 3 digits
    for (int i = 1; i < parts.length; i++) {
      if (parts[i].length() != 3 || !parts[i].matches("\\d{3}")) {
        return new FlexibleNumberParseResult("Invalid thousands separator format: '" + numberStr + "'");
      }
    }
    
    return new FlexibleNumberParseResult(new BigDecimal("0")); // Success case
  }

  /**
   * Detect if the pattern represents European thousands separator format (periods as thousands separators).
   *
   * @param numberStr The number string
   * @return true if this appears to be European thousands separator format
   */
  private boolean isEuropeanThousandsPattern(String numberStr) {
    // European pattern: multiple periods like "1.234.567"
    // This is always European thousands separator format in our context
    return numberStr.contains(".") && countOccurrences(numberStr, '.') > 1;
  }

  /**
   * Generate UK format suggestion for European thousands separator input.
   *
   * @param europeanInput The European format input with period thousands separators
   * @return UK format suggestion
   */
  private String generateUkFormatSuggestion(String europeanInput) {
    // Convert European thousands separator format to UK format
    // Example: "1.234.567" → "1,234,567"
    return europeanInput.replace(".", ",");
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