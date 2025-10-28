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

/**
 * Parse strategy for numbers with only comma separators.
 */
public class OnlyCommasParseStrategy implements NumberParseUtilStrategy {

  /**
   * Constructs a new OnlyCommasParseStrategy.
   */
  public OnlyCommasParseStrategy() {
  }

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

      // Special case: if number starts with separator, it's European decimal format
      if (digitsBefore == 0) {
        String suggestion = generateUkFormatSuggestion(numberStr);
        return new FlexibleNumberParseResult(
            String.format(
                "Unsupported number format: '%s'. Please use: '%s'. "
                + "Kigali Sim requires comma for thousands separator and period for decimal point.",
                numberStr,
                suggestion
            )
        );
      }

      if (digitsAfter == 3) {
        // If digits before are 4 or more like 1234,5, it could be European decimal - reject
        if (digitsBefore >= 4) {
          String suggestion = generateUkFormatSuggestion(numberStr);
          return new FlexibleNumberParseResult(
              String.format(
                  "Unsupported format detected: '%s'. Please use format: '%s'. "
                  + "Kigali Sim requires comma for thousands and period for decimal.",
                  numberStr,
                  suggestion
              )
          );
        } else if (isLikelyThousandsSeparator(numberStr, separatorIndex)) {
          return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(String.valueOf(separator), "")));
        } else if (numberStr.startsWith("0,")) {
          // Numbers starting with 0, are European decimals (like 0,035) - reject
          String suggestion = generateUkFormatSuggestion(numberStr);
          return new FlexibleNumberParseResult(
              String.format(
                  "Unsupported format detected: '%s'. Please use format: '%s'. "
                  + "Kigali Sim requires comma for thousands and period for decimal.",
                  numberStr,
                  suggestion
              )
          );
        } else {
          // Previously ambiguous case - now interpret as UK format (thousands separator)
          return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(String.valueOf(separator), "")));
        }
      } else {
        // Not exactly 3 digits after - this is European decimal format, reject with UK suggestion
        String suggestion = generateUkFormatSuggestion(numberStr);
        return new FlexibleNumberParseResult(
            String.format(
                "Unsupported number format: '%s'. Please use: '%s'. "
                + "Kigali Sim requires comma for thousands separator and period for decimal point.",
                numberStr,
                suggestion
            )
        );
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

    // Don't make assumptions about N,000 patterns - treat them as ambiguous
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
   * Detect if the pattern represents European decimal format (comma as decimal separator).
   *
   * @param numberStr The number string
   * @param separatorIndex The index of the comma separator
   * @return true if this appears to be European decimal format
   */
  private boolean isEuropeanDecimalPattern(String numberStr, int separatorIndex) {
    // For the OnlyCommas strategy, single comma patterns are ambiguous
    // and should be interpreted as UK decimal format according to requirements
    // The main European format detection (like 1.234,56) happens in MixedParseStrategy
    // We only reject clear European-specific patterns here, which are rare with only commas

    // For now, be conservative and don't reject single comma patterns
    // They will be converted to UK decimal format (comma -> period)
    return false;
  }

  /**
   * Generate UK format suggestion for European decimal input.
   *
   * @param europeanInput The European format input
   * @return UK format suggestion
   */
  private String generateUkFormatSuggestion(String europeanInput) {
    // Convert European decimal format to UK format
    // Example: "123,45" → "123.45"
    return europeanInput.replace(",", ".");
  }
}
