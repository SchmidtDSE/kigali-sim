/**
 * Abstract template for parsing numbers with a single delimiter type.
 *
 * <p>This abstract class implements the template method pattern to consolidate
 * common parsing logic for numbers containing only commas or only periods.
 * Subclasses customize behavior through abstract methods for separator-specific
 * handling.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.localization;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Abstract base class for single-separator number parsing strategies.
 */
public abstract class SingleDelimiterTemplate implements NumberParseUtilStrategy {

  /**
   * Constructs a new SingleDelimiterTemplate.
   */
  public SingleDelimiterTemplate() {
  }

  @Override
  public FlexibleNumberParseResult parseNumber(String numberStr, int commaCount, int periodCount) {
    if (!canHandle(numberStr, commaCount, periodCount)) {
      return new FlexibleNumberParseResult(getStrategyName() + " cannot handle this pattern");
    }
    return handleSingleSeparatorType(numberStr, getSeparator(), getSeparatorCount(commaCount, periodCount));
  }

  /**
   * Get the separator character used by this strategy.
   *
   * @return The separator character (',' or '.')
   */
  protected abstract char getSeparator();

  /**
   * Get the name of this strategy for error messages.
   *
   * @return The strategy name
   */
  protected abstract String getStrategyName();

  /**
   * Get the count of this strategy's separator from the total counts.
   *
   * @param commaCount Number of commas in the string
   * @param periodCount Number of periods in the string
   * @return The count relevant to this strategy
   */
  protected abstract int getSeparatorCount(int commaCount, int periodCount);

  /**
   * Handle numbers with multiple separators.
   *
   * @param numberStr The number string
   * @param separator The separator character
   * @return FlexibleNumberParseResult containing either parsed number or error
   */
  protected abstract FlexibleNumberParseResult handleMultipleSeparators(String numberStr, char separator);

  /**
   * Handle numbers with a leading separator.
   *
   * @param numberStr The number string starting with separator
   * @return FlexibleNumberParseResult containing either parsed number or error
   */
  protected abstract FlexibleNumberParseResult handleLeadingSeparator(String numberStr);

  /**
   * Handle numbers where the separator separates digits in a non-3-digit pattern.
   *
   * @param numberStr The number string
   * @return FlexibleNumberParseResult containing either parsed number or error
   */
  protected abstract FlexibleNumberParseResult handleSingleNonAmbiguous(String numberStr);

  /**
   * Handle ambiguous case where exactly 3 digits follow a single separator.
   *
   * @param numberStr The number string
   * @param separator The separator character
   * @param separatorIndex The index of the separator
   * @param digitsBefore Number of digits before the separator
   * @return FlexibleNumberParseResult containing either parsed number or error
   */
  protected abstract FlexibleNumberParseResult handleAmbiguousCase(String numberStr, char separator,
                                                                     int separatorIndex, int digitsBefore);

  /**
   * Handle numbers with only one type of separator.
   *
   * <p>This template method implements the core parsing algorithm for single-separator numbers.
   * It delegates to strategy-specific abstract methods for customization points.
   *
   * @param numberStr The number string
   * @param separator The separator character
   * @param count Number of occurrences of the separator
   * @return FlexibleNumberParseResult containing either parsed number or error
   */
  protected final FlexibleNumberParseResult handleSingleSeparatorType(String numberStr,
                                                                       char separator, int count) {
    if (count > 1) {
      return handleMultipleSeparators(numberStr, separator);
    } else {
      int separatorIndex = numberStr.indexOf(separator);
      int digitsAfter = numberStr.length() - separatorIndex - 1;
      int digitsBefore = separatorIndex;

      if (digitsBefore == 0) {
        return handleLeadingSeparator(numberStr);
      }

      if (digitsAfter == 3) {
        return handleAmbiguousCase(numberStr, separator, separatorIndex, digitsBefore);
      } else {
        return handleSingleNonAmbiguous(numberStr);
      }
    }
  }

  /**
   * Determine if a single separator with 3 digits after is likely a thousands separator.
   *
   * <div>
   * <ul>
   * <li>Numbers starting with 0 (like 0,035 or 0.035) are considered decimals, not thousands separators</li>
   * <li>Only consider as thousands separator if 4 or more digits appear before the separator</li>
   * </ul>
   * </div>
   *
   * @param numberStr The complete number string
   * @param separatorIndex The index of the separator
   * @return true if it's likely a thousands separator
   */
  protected final boolean isLikelyThousandsSeparator(String numberStr, int separatorIndex) {
    int digitsBefore = separatorIndex;
    String leadingPattern = "0" + getSeparator();
    if (numberStr.startsWith(leadingPattern)) {
      return false;
    }
    return digitsBefore >= 4;
  }

  /**
   * Validate that thousands separators are positioned correctly.
   *
   * <div>
   * <ul>
   * <li>First part must be 1-3 digits</li>
   * <li>All subsequent parts must be exactly 3 digits</li>
   * </ul>
   * </div>
   *
   * @param numberStr The number string
   * @param separator The thousands separator character
   * @return FlexibleNumberParseResult with success or error
   */
  protected final FlexibleNumberParseResult validateThousandsSeparatorPositions(String numberStr,
                                                                                 char separator) {
    if (numberStr == null || numberStr.isEmpty() || !numberStr.contains(String.valueOf(separator))) {
      return new FlexibleNumberParseResult(new BigDecimal("0"));
    }

    String[] parts = numberStr.split(Pattern.quote(String.valueOf(separator)));

    if (parts.length < 2) {
      return new FlexibleNumberParseResult(new BigDecimal("0"));
    }

    if (parts[0].isEmpty() || parts[0].length() > 3) {
      return new FlexibleNumberParseResult("Invalid thousands separator format: '" + numberStr + "'");
    }

    for (int i = 1; i < parts.length; i++) {
      if (parts[i].length() != 3 || !parts[i].matches("\\d{3}")) {
        return new FlexibleNumberParseResult("Invalid thousands separator format: '" + numberStr + "'");
      }
    }

    return new FlexibleNumberParseResult(new BigDecimal("0"));
  }
}
