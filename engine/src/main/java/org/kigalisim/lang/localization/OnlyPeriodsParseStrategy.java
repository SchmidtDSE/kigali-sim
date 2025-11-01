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

/**
 * Parse strategy for numbers with only period separators.
 */
public class OnlyPeriodsParseStrategy extends SingleDelimiterTemplate {

  /**
   * Constructs a new OnlyPeriodsParseStrategy.
   */
  public OnlyPeriodsParseStrategy() {
  }

  @Override
  public boolean canHandle(String numberStr, int commaCount, int periodCount) {
    return periodCount > 0 && commaCount == 0;
  }

  @Override
  protected char getSeparator() {
    return '.';
  }

  @Override
  protected String getStrategyName() {
    return "OnlyPeriodsParseStrategy";
  }

  @Override
  protected int getSeparatorCount(int commaCount, int periodCount) {
    return periodCount;
  }

  @Override
  protected FlexibleNumberParseResult handleMultipleSeparators(String numberStr, char separator) {
    if (isEuropeanThousandsPattern(numberStr)) {
      String suggestion = generateUkFormatSuggestion(numberStr);
      return new FlexibleNumberParseResult(String.format(
          "Unsupported number format: '%s'. Please use: '%s'. "
          + "Kigali Sim requires comma for thousands separator and period for decimal point.",
          numberStr, suggestion
      ));
    }
    FlexibleNumberParseResult validationResult = validateThousandsSeparatorPositions(numberStr, separator);
    if (validationResult.isError()) {
      return validationResult;
    }
    return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(String.valueOf(separator), "")));
  }

  @Override
  protected FlexibleNumberParseResult handleLeadingSeparator(String numberStr) {
    return new FlexibleNumberParseResult(new BigDecimal(numberStr));
  }

  @Override
  protected FlexibleNumberParseResult handleAmbiguousCase(String numberStr, char separator,
                                                           int separatorIndex, int digitsBefore) {
    if (digitsBefore >= 4) {
      return new FlexibleNumberParseResult(new BigDecimal(numberStr));
    } else if (isLikelyThousandsSeparator(numberStr, separatorIndex)) {
      return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(String.valueOf(separator), "")));
    } else if (numberStr.startsWith("0.")) {
      return new FlexibleNumberParseResult(new BigDecimal(numberStr));
    } else {
      return new FlexibleNumberParseResult(new BigDecimal(numberStr));
    }
  }

  @Override
  protected FlexibleNumberParseResult handleSingleNonAmbiguous(String numberStr) {
    return new FlexibleNumberParseResult(new BigDecimal(numberStr));
  }

  /**
   * Detect if the pattern represents European thousands separator format.
   *
   * <div>
   * Multiple periods like "1.234.567" indicate European thousands separator format in this context.
   * </div>
   *
   * @param numberStr The number string
   * @return true if this appears to be European thousands separator format
   */
  private boolean isEuropeanThousandsPattern(String numberStr) {
    return numberStr.contains(".") && countOccurrences(numberStr, '.') > 1;
  }

  /**
   * Generate UK format suggestion for European thousands separator input.
   *
   * <div>
   * Converts European thousands separator format to UK format by replacing periods with commas.
   * For example: "1.234.567" becomes "1,234,567".
   * </div>
   *
   * @param europeanInput The European format input with period thousands separators
   * @return UK format suggestion
   */
  private String generateUkFormatSuggestion(String europeanInput) {
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
