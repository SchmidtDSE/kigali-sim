/**
 * Strategy for parsing numbers that contain both comma and period separators.
 *
 * <p>This strategy handles numbers like 123,456.789 (US format) or 123.456,789 (European format)
 * where both commas and periods are used as separators.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.localization;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Parse strategy for numbers with both comma and period separators.
 */
public class MixedParseStrategy implements NumberParseUtilStrategy {

  @Override
  public boolean canHandle(String numberStr, int commaCount, int periodCount) {
    return commaCount > 0 && periodCount > 0;
  }

  @Override
  public FlexibleNumberParseResult parseNumber(String numberStr, int commaCount, int periodCount) {
    if (!canHandle(numberStr, commaCount, periodCount)) {
      return new FlexibleNumberParseResult("MixedParseStrategy cannot handle this pattern");
    }

    return handleMixedSeparators(numberStr);
  }

  /**
   * Handle numbers with both commas and periods present.
   *
   * @param numberStr The number string containing both separators
   * @return FlexibleNumberParseResult containing either parsed number or error
   */
  private FlexibleNumberParseResult handleMixedSeparators(String numberStr) {
    int lastCommaIndex = numberStr.lastIndexOf(',');
    int lastPeriodIndex = numberStr.lastIndexOf('.');

    if (lastCommaIndex < lastPeriodIndex) {
      // Comma comes before period: comma = thousands, period = decimal (US format)
      // Example: 123,456.789
      String withoutThousands = numberStr.replace(",", "");
      FlexibleNumberParseResult decimalValidation = validateSingleDecimalSeparator(withoutThousands, '.');
      if (decimalValidation.isError()) {
        return decimalValidation;
      }
      // Also validate thousands separator positioning if any commas
      FlexibleNumberParseResult thousandsValidation = validateThousandsSeparatorPositions(numberStr.substring(0, lastPeriodIndex), ',');
      if (thousandsValidation.isError()) {
        return thousandsValidation;
      }
      return new FlexibleNumberParseResult(new BigDecimal(withoutThousands));
    } else {
      // Period comes before comma: European format - reject with UK suggestion
      // Example: 123.456,789 → suggest 123,456.789
      String suggestion = convertEuropeanToUk(numberStr);
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
   * Validate that there's only one decimal separator after removing thousands separators.
   *
   * @param numberStr The number string with thousands separators removed
   * @param decimalSeparator The expected decimal separator
   * @return FlexibleNumberParseResult with success or error
   */
  private FlexibleNumberParseResult validateSingleDecimalSeparator(String numberStr, char decimalSeparator) {
    int count = countOccurrences(numberStr, decimalSeparator);
    if (count > 1) {
      return new FlexibleNumberParseResult("Invalid number format: multiple decimal separators in '"
          + numberStr + "'");
    }
    return new FlexibleNumberParseResult(new BigDecimal("0")); // Success case
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

  /**
   * Convert European mixed format to UK format.
   *
   * @param europeanNumber The European format number (e.g., "1.234,56")
   * @return UK format suggestion (e.g., "1,234.56")
   */
  private String convertEuropeanToUk(String europeanNumber) {
    // Convert European mixed format to UK format
    // European: 1.234.567,89 (periods for thousands, comma for decimal)
    // UK:       1,234,567.89 (commas for thousands, period for decimal)

    int lastCommaIndex = europeanNumber.lastIndexOf(',');
    if (lastCommaIndex == -1) {
      // No comma, just convert periods to commas
      return europeanNumber.replace(".", ",");
    }

    // Split at last comma (decimal separator in European format)
    String thousandsPart = europeanNumber.substring(0, lastCommaIndex);
    String decimalPart = europeanNumber.substring(lastCommaIndex + 1);

    // Convert periods in thousands part to commas, and use period for decimal
    String ukThousands = thousandsPart.replace(".", ",");
    return ukThousands + "." + decimalPart;
  }
}
