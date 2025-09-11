/**
 * JavaScript utility class for parsing numbers with flexible thousands and decimal separators.
 * Mirrors the Java NumberParseUtil implementation to ensure consistency
 * between front-end and back-end.
 *
 * This class handles various number formats including:
 * - US format: "123,456.78"
 * - European format: "123.456,78"
 * - Mixed formats with proper precedence rules
 * - Detection of ambiguous formats like "123,456" vs "123.456"
 */
export class NumberParseUtil {
  /**
   * Parse a number string with flexible thousands/decimal separator handling.
   *
   * @param {string} numberString - The number string to parse
   * @returns {number} The parsed number
   * @throws {Error} For ambiguous or invalid number formats
   */
  parseFlexibleNumber(numberString) {
    if (!numberString || typeof numberString !== "string") {
      throw new Error("Number string cannot be null or empty");
    }

    const trimmed = numberString.trim();
    if (trimmed === "") {
      throw new Error("Number string cannot be empty");
    }

    // Handle sign
    const isNegative = trimmed.startsWith("-");
    const isPositive = trimmed.startsWith("+");
    const numberPart = isNegative || isPositive ? trimmed.substring(1) : trimmed;

    // If no separators, parse directly
    if (!numberPart.includes(",") && !numberPart.includes(".")) {
      const result = parseFloat(numberPart);
      if (isNaN(result)) {
        throw new Error(`Invalid number format: '${numberString}'`);
      }
      return isNegative ? -result : result;
    }

    // Handle cases with separators
    const result = this._parseWithSeparators(numberPart, numberString);
    return isNegative ? -result : result;
  }

  /**
   * Check if a number string format is ambiguous.
   *
   * @param {string} numberString - The number string to check
   * @returns {boolean} True if the number format is ambiguous
   */
  isAmbiguous(numberString) {
    if (!numberString || typeof numberString !== "string") {
      return false;
    }

    try {
      this.parseFlexibleNumber(numberString);
      return false;
    } catch (error) {
      return error.message.includes("Ambiguous number format");
    }
  }

  /**
   * Get disambiguation suggestions for an ambiguous number.
   *
   * @param {string} numberString - The ambiguous number string
   * @returns {string} Suggestion message for resolving ambiguity
   */
  getDisambiguationSuggestion(numberString) {
    if (!this.isAmbiguous(numberString)) {
      return "Number format is not ambiguous";
    }

    const trimmed = numberString.trim();
    const separator = trimmed.includes(",") ? "," : ".";

    return `Use '${trimmed}.0' for thousands separator or change format to disambiguate`;
  }

  /**
   * Parse number string with comma and/or period separators.
   *
   * @param {string} numberPart - Number part without sign
   * @param {string} originalString - Original string for error messages
   * @returns {number} Parsed number
   * @private
   */
  _parseWithSeparators(numberPart, originalString) {
    const commaCount = this._countOccurrences(numberPart, ",");
    const periodCount = this._countOccurrences(numberPart, ".");

    // Both separators present - apply precedence rules
    if (commaCount > 0 && periodCount > 0) {
      return this._parseMixedSeparators(numberPart, originalString);
    }

    // Only comma present
    if (commaCount > 0) {
      return this._parseSingleSeparatorType(numberPart, ",", originalString);
    }

    // Only period present
    if (periodCount > 0) {
      return this._parseSingleSeparatorType(numberPart, ".", originalString);
    }

    // Shouldn't reach here based on calling conditions
    throw new Error(`Unexpected parsing state for: '${originalString}'`);
  }

  /**
   * Handle numbers with both comma and period separators.
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} originalString - Original string for error messages
   * @returns {number} Parsed number
   * @private
   */
  _parseMixedSeparators(numberPart, originalString) {
    const commaCount = this._countOccurrences(numberPart, ",");
    const periodCount = this._countOccurrences(numberPart, ".");
    const lastComma = numberPart.lastIndexOf(",");
    const lastPeriod = numberPart.lastIndexOf(".");

    // Validate that we have a valid mixed separator pattern
    // Valid patterns:
    // - US format: One or more commas as thousands, one period as decimal
    //   (period must be last)
    // - European format: One or more periods as thousands, one comma as decimal
    //   (comma must be last)

    // Period comes after comma - US format (period as decimal)
    if (lastPeriod > lastComma) {
      // Validate US format: comma(s) for thousands, period for decimal
      if (periodCount !== 1) {
        throw new Error(
          `Invalid number format: '${originalString}' - multiple decimal separators not allowed`,
        );
      }

      // Check if there are any periods before the last comma (invalid in US format)
      const firstPeriod = numberPart.indexOf(".");
      if (firstPeriod < lastComma) {
        throw new Error(
          `Invalid number format: '${originalString}' - periods cannot ` +
            "appear before commas in US format",
        );
      }

      // Format: 123,456.78 - comma is thousands, period is decimal
      const withoutThousands = numberPart.replace(/,/g, "");
      const result = parseFloat(withoutThousands);
      if (isNaN(result)) {
        throw new Error(`Invalid number format: '${originalString}'`);
      }
      return result;
    } else {
      // Comma comes after period - European format (comma as decimal)
      if (commaCount !== 1) {
        throw new Error(
          `Invalid number format: '${originalString}' - multiple decimal separators not allowed`,
        );
      }

      // Check if there are any commas before the last period (invalid in European format)
      const firstComma = numberPart.indexOf(",");
      if (firstComma < lastPeriod) {
        throw new Error(
          `Invalid number format: '${originalString}' - commas cannot ` +
            "appear before periods in European format",
        );
      }

      // Format: 123.456,78 - period is thousands, comma is decimal
      const withoutThousands = numberPart.replace(/\./g, "");
      const result = parseFloat(withoutThousands.replace(",", "."));
      if (isNaN(result)) {
        throw new Error(`Invalid number format: '${originalString}'`);
      }
      return result;
    }
  }

  /**
   * Handle numbers with only one type of separator (all commas or all periods).
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} separator - The separator character (',' or '.')
   * @param {string} originalString - Original string for error messages
   * @returns {number} Parsed number
   * @private
   */
  _parseSingleSeparatorType(numberPart, separator, originalString) {
    const separatorCount = this._countOccurrences(numberPart, separator);

    // Multiple occurrences - definitely thousands separator
    if (separatorCount > 1) {
      const cleaned = numberPart.replace(new RegExp(`\\${separator}`, "g"), "");
      const result = parseFloat(cleaned);
      if (isNaN(result)) {
        throw new Error(`Invalid number format: '${originalString}'`);
      }
      return result;
    }

    // Single occurrence - need to determine if thousands or decimal
    const separatorIndex = numberPart.indexOf(separator);
    const digitsBefore = separatorIndex;
    const digitsAfter = numberPart.length - separatorIndex - 1;

    // If not exactly 3 digits after, treat as decimal separator
    if (digitsAfter !== 3) {
      if (separator === ",") {
        // European decimal format
        const result = parseFloat(numberPart.replace(",", "."));
        if (isNaN(result)) {
          throw new Error(`Invalid number format: '${originalString}'`);
        }
        return result;
      } else {
        // US decimal format
        const result = parseFloat(numberPart);
        if (isNaN(result)) {
          throw new Error(`Invalid number format: '${originalString}'`);
        }
        return result;
      }
    }

    // Exactly 3 digits after separator - check for likely thousands patterns
    if (digitsAfter === 3) {
      // If we have >= 4 digits before separator, likely thousands
      if (
        digitsBefore >= 4 ||
        this._isLikelyThousandsSeparator(numberPart, separatorIndex)
      ) {
        const cleaned = numberPart.replace(separator, "");
        const result = parseFloat(cleaned);
        if (isNaN(result)) {
          throw new Error(`Invalid number format: '${originalString}'`);
        }
        return result;
      } else {
        // Truly ambiguous case
        throw new Error(
          `Ambiguous number format: '${originalString}'. Cannot determine if '${separator}' ` +
            "is a thousands separator or decimal separator. Suggestions: " +
            `Use '${originalString}.0' ` +
            "for thousands separator or change format to disambiguate.",
        );
      }
    }

    // Fallback - shouldn't reach here
    throw new Error(`Unable to parse number format: '${originalString}'`);
  }

  /**
   * Check if a separator pattern looks like a thousands separator.
   *
   * @param {string} numberStr - The number string
   * @param {number} separatorIndex - Index of the separator
   * @returns {boolean} True if it looks like a thousands separator
   * @private
   */
  _isLikelyThousandsSeparator(numberStr, separatorIndex) {
    const beforeSeparator = numberStr.substring(0, separatorIndex);

    // Common thousands patterns: 1,000 or 10,000 or similar
    if (beforeSeparator === "1" || beforeSeparator === "10") {
      return true;
    }

    // If the number before separator ends with patterns like x,xxx
    if (beforeSeparator.length >= 4) {
      return true;
    }

    return false;
  }

  /**
   * Count occurrences of a character in a string.
   *
   * @param {string} str - The string to search
   * @param {string} char - The character to count
   * @returns {number} Number of occurrences
   * @private
   */
  _countOccurrences(str, char) {
    return (str.match(new RegExp(`\\${char}`, "g")) || []).length;
  }
}

