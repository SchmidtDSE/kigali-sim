/**
 * Strategy for parsing numbers that contain only comma separators.
 *
 * <p>This strategy handles numbers like 1,234 or 1,234,567 or 123,45 (European decimal)
 * where only commas are used as separators.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.localization;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.kigalisim.lang.localization.FlexibleNumberParseResult;

/**
 * Parse strategy for numbers with only comma separators.
 */
public class OnlyCommasParseStrategy implements NumberParseUtilStrategy {

  @Override
  public boolean canHandle(String numberStr, int commaCount, int periodCount) {
    return commaCount > 0 && periodCount == 0;
  }

  @Override
  public FlexibleNumberParseResult parseNumber(String numberStr, int commaCount, int periodCount) {
    if (!canHandle(numberStr, commaCount, periodCount)) {
      return new FlexibleNumberParseResult("OnlyCommasParseStrategy cannot handle this pattern");
    }

    return handleSingleSeparatorType(numberStr, ',', commaCount);
  }

  /**
   * Handle numbers with only comma separators.
   *
   * @param numberStr The number string
   * @param separator The separator character (always comma for this strategy)
   * @param count Number of occurrences of the separator
   * @return FlexibleNumberParseResult containing either parsed number or error
   */
  private FlexibleNumberParseResult handleSingleSeparatorType(String numberStr, char separator, int count) {
    if (count > 1) {
      // Multiple occurrences = thousands separator
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
        // Convert comma decimal to period decimal
        return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(",", ".")));
      }

      if (digitsAfter == 3) {
        // If digits before are 4 or more like 1234,5, treat as decimal separator
        if (digitsBefore >= 4) {
          // Convert comma decimal to period decimal
          return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(",", ".")));
        } else if (isLikelyThousandsSeparator(numberStr, separatorIndex)) {
          return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(String.valueOf(separator), "")));
        } else if (numberStr.startsWith("0,")) {
          // Numbers starting with 0, are clearly decimals (like 0,035)
          return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(",", ".")));
        } else {
          // Truly ambiguous case - could be 123,456 (thousands) or 123,456 (decimal)
          return new FlexibleNumberParseResult(String.format(
              "Ambiguous number format: '%s'. Cannot determine if '%s' is a thousands "
              + "separator or decimal separator. Suggestions: Use '%s.0' for thousands "
              + "separator or change format to disambiguate.",
              numberStr, separator, numberStr
          ));
        }
      } else {
        // Not exactly 3 digits after = decimal separator
        // Convert comma decimal to period decimal
        return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(",", ".")));
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

    // If number starts with 0 (like 0,035), it's clearly a decimal
    if (numberStr.startsWith("0,")) {
      return false;
    }

    // Common thousands patterns: 1,000  10,000  100,000 etc.
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
}