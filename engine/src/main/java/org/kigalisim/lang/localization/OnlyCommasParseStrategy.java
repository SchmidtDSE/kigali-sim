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

/**
 * Parse strategy for numbers with only comma separators.
 */
public class OnlyCommasParseStrategy extends SingleDelimiterTemplate {

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
  protected char getSeparator() {
    return ',';
  }

  @Override
  protected String getStrategyName() {
    return "OnlyCommasParseStrategy";
  }

  @Override
  protected int getSeparatorCount(int commaCount, int periodCount) {
    return commaCount;
  }

  @Override
  protected FlexibleNumberParseResult handleMultipleSeparators(String numberStr, char separator) {
    FlexibleNumberParseResult validationResult = validateThousandsSeparatorPositions(numberStr, separator);
    if (validationResult.isError()) {
      return validationResult;
    }
    return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(String.valueOf(separator), "")));
  }

  @Override
  protected FlexibleNumberParseResult handleLeadingSeparator(String numberStr) {
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

  @Override
  protected FlexibleNumberParseResult handleAmbiguousCase(String numberStr, char separator,
                                                           int separatorIndex, int digitsBefore) {
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
      return new FlexibleNumberParseResult(new BigDecimal(numberStr.replace(String.valueOf(separator), "")));
    }
  }

  @Override
  protected FlexibleNumberParseResult handleSingleNonAmbiguous(String numberStr) {
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

  /**
   * Detect if the pattern represents European decimal format (comma as decimal separator).
   *
   * <p>For the OnlyCommas strategy, single comma patterns are ambiguous and should be interpreted
   * as UK decimal format according to requirements. The main European format detection
   * (like 1.234,56) happens in MixedParseStrategy. Patterns are conservatively not rejected
   * and are converted to UK decimal format (comma to period).</p>
   *
   * @param numberStr The number string
   * @param separatorIndex The index of the comma separator
   * @return true if this appears to be European decimal format
   */
  private boolean isEuropeanDecimalPattern(String numberStr, int separatorIndex) {
    return false;
  }

  /**
   * Generate UK format suggestion for European decimal input.
   *
   * <p>Converts European decimal format to UK format by replacing commas with periods.
   * Example: "123,45" converts to "123.45".</p>
   *
   * @param europeanInput The European format input
   * @return UK format suggestion
   */
  private String generateUkFormatSuggestion(String europeanInput) {
    return europeanInput.replace(",", ".");
  }
}
