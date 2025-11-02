/**
 * Utility class for parsing numbers with flexible thousands and decimal separators.
 * Mirrors the Java NumberParseUtil implementation to ensure consistency
 * between front-end and back-end.
 *
 * This class handles various number formats including:
 * - US format: "123,456.78"
 * - European format: "123.456,78"
 * - Mixed formats with proper precedence rules
 * - Detection of ambiguous formats like "123,456" vs "123.456"
 */

/**
 * Result class for number parsing operations that can succeed or fail.
 */
class NumberParseResult {
  /**
   * Create a NumberParseResult.
   *
   * @param {number|null} number - The parsed number, or null if parsing failed
   * @param {string|null} error - The error message, or null if parsing succeeded
   */
  constructor(number, error) {
    const self = this;
    self._number = number;
    self._error = error;
  }

  /**
   * Get the parsed number.
   *
   * @returns {number|null} The parsed number, or null if parsing failed
   */
  getNumber() {
    const self = this;
    return self._number;
  }

  /**
   * Get the error message.
   *
   * @returns {string|null} The error message, or null if parsing succeeded
   */
  getError() {
    const self = this;
    return self._error;
  }

  /**
   * Check if parsing was successful.
   *
   * @returns {boolean} True if parsing succeeded, false otherwise
   */
  isSuccess() {
    const self = this;
    return self._error === null;
  }

  /**
   * Create a successful result.
   *
   * @param {number} number - The parsed number
   * @returns {NumberParseResult} A successful result
   */
  static success(number) {
    return new NumberParseResult(number, null);
  }

  /**
   * Create a failed result.
   *
   * @param {string} error - The error message
   * @returns {NumberParseResult} A failed result
   */
  static error(error) {
    return new NumberParseResult(null, error);
  }
}
class NumberParseUtil {
  /**
   * Parse a number string with flexible thousands/decimal separator handling.
   *
   * @param {string} numberString - The number string to parse
   * @returns {NumberParseResult} The parsing result containing the number or error
   */
  parseFlexibleNumber(numberString) {
    const self = this;
    if (!numberString || typeof numberString !== "string") {
      return NumberParseResult.error("Number string cannot be null or empty");
    }

    const trimmed = numberString.trim();
    if (trimmed === "") {
      return NumberParseResult.error("Number string cannot be empty");
    }

    // Handle sign
    const isNegative = trimmed.startsWith("-");
    const isPositive = trimmed.startsWith("+");
    const numberPart = isNegative || isPositive ? trimmed.substring(1) : trimmed;

    // If no separators, parse directly
    const fullyNumeric = !numberPart.includes(",") && !numberPart.includes(".");
    if (fullyNumeric) {
      const result = parseFloat(numberPart);
      if (isNaN(result)) {
        return NumberParseResult.error(`Invalid number format: '${numberString}'`);
      }
      return NumberParseResult.success(isNegative ? -result : result);
    }

    // Handle cases with separators
    const separatorResult = self._parseWithSeparators(numberPart, numberString);
    if (!separatorResult.isSuccess()) {
      return separatorResult;
    }
    const finalNumber = isNegative ? -separatorResult.getNumber() : separatorResult.getNumber();
    return NumberParseResult.success(finalNumber);
  }

  /**
   * Check if a number string format is ambiguous.
   *
   * @param {string} numberString - The number string to check
   * @returns {boolean} True if the number format is ambiguous
   */
  isAmbiguous(numberString) {
    const self = this;
    if (!numberString || typeof numberString !== "string") {
      return false;
    }

    const result = self.parseFlexibleNumber(numberString);
    if (result.isSuccess()) {
      return false;
    }
    return result.getError().includes("Ambiguous number format");
  }

  /**
   * Get disambiguation suggestions for an ambiguous number.
   *
   * @param {string} numberString - The ambiguous number string
   * @returns {string} Suggestion message for resolving ambiguity
   */
  getDisambiguationSuggestion(numberString) {
    const self = this;
    if (!self.isAmbiguous(numberString)) {
      return "Number format is not ambiguous";
    }

    const trimmed = numberString.trim();
    const separator = trimmed.includes(",") ? "," : ".";
    const other = separator === "," ? "." : ",";

    return `Use '${trimmed}${separator}0' or '${trimmed}${other}0' to disambiguate`;
  }

  /**
   * Parse number string with comma and/or period separators.
   *
   * @param {string} numberPart - Number part without sign
   * @param {string} originalString - Original string for error messages
   * @returns {NumberParseResult} Parsed number result
   * @private
   */
  _parseWithSeparators(numberPart, originalString) {
    const self = this;
    const hasCommas = self._countOccurrences(numberPart, ",") > 0;
    const hasPeriods = self._countOccurrences(numberPart, ".") > 0;
    const bothSeparatorsPresent = hasCommas && hasPeriods;

    if (bothSeparatorsPresent) {
      return self._parseMixedSeparators(numberPart, originalString);
    } else if (hasCommas) {
      return self._parseSingleSeparatorType(numberPart, ",", originalString);
    } else if (hasPeriods) {
      return self._parseSingleSeparatorType(numberPart, ".", originalString);
    } else {
      // Shouldn't reach here based on calling conditions
      return NumberParseResult.error(`Unexpected parsing state for: '${originalString}'`);
    }
  }

  /**
   * Handle numbers with both comma and period separators.
   *
   * Validates that we have a valid mixed separator pattern:
   * - US format: One or more commas as thousands, one period as decimal
   *   (period must be last)
   * - European format: One or more periods as thousands, one comma as decimal
   *   (comma must be last)
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} originalString - Original string for error messages
   * @returns {NumberParseResult} Parsed number result
   * @private
   */
  _parseMixedSeparators(numberPart, originalString) {
    const self = this;
    const commaCount = self._countOccurrences(numberPart, ",");
    const periodCount = self._countOccurrences(numberPart, ".");
    const lastComma = numberPart.lastIndexOf(",");
    const lastPeriod = numberPart.lastIndexOf(".");

    const usesUsFormat = lastPeriod > lastComma;
    const multiplePeriods = periodCount !== 1;
    const periodsBeforeComma = numberPart.indexOf(".") < lastComma;

    if (usesUsFormat) {
      if (multiplePeriods) {
        return NumberParseResult.error(
          `Invalid number format: '${originalString}' - multiple decimal separators not allowed`,
        );
      }

      if (periodsBeforeComma) {
        return NumberParseResult.error(
          `Invalid number format: '${originalString}' - periods cannot ` +
            "appear before commas in US format",
        );
      }

      const withoutThousands = numberPart.replace(/,/g, "");
      const result = parseFloat(withoutThousands);
      if (isNaN(result)) {
        return NumberParseResult.error(`Invalid number format: '${originalString}'`);
      }
      return NumberParseResult.success(result);
    } else {
      const ukSuggestion = self._convertEuropeanToUkFormat(numberPart);
      return NumberParseResult.error(
        `Unsupported number format: '${originalString}'. ` +
        `Please use: '${ukSuggestion}'. ` +
        "Kigali Sim requires comma for thousands separator and period for decimal point.",
      );
    }
  }

  /**
   * Handle numbers with only one type of separator (all commas or all periods).
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} separator - The separator character (',' or '.')
   * @param {string} originalString - Original string for error messages
   * @returns {NumberParseResult} Parsed number result
   * @private
   */
  _parseSingleSeparatorType(numberPart, separator, originalString) {
    const self = this;
    const separatorCount = self._countOccurrences(numberPart, separator);

    if (separatorCount > 1) {
      return self._parseSingleSeparatorMultipleOccurrences(
        numberPart,
        separator,
        originalString,
      );
    }

    const separatorIndex = numberPart.indexOf(separator);
    const digitsBefore = separatorIndex;
    const digitsAfter = numberPart.length - separatorIndex - 1;

    if (digitsAfter !== 3) {
      return self._parseSingleSeparatorSingleOccurrence(
        numberPart,
        separator,
        digitsBefore,
        digitsAfter,
        originalString,
      );
    }

    if (digitsBefore === 0) {
      return self._parseStartsWithSeparator(numberPart, separator, originalString);
    }

    if (digitsBefore >= 4) {
      return self._parseUnambiguousPriorDigits(numberPart, separator, originalString);
    }

    if (numberPart.startsWith("0" + separator)) {
      return self._parseWithLeadingZero(numberPart, separator, originalString);
    }

    return self._interpretAmbiguous(numberPart, separator, originalString);
  }

  /**
   * Parse single separator with multiple occurrences.
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} separator - The separator character (',' or '.')
   * @param {string} originalString - Original string for error messages
   * @returns {NumberParseResult} Parsed number result
   * @private
   */
  _parseSingleSeparatorMultipleOccurrences(numberPart, separator, originalString) {
    const self = this;
    if (separator === ".") {
      const ukSuggestion = numberPart.replace(/\./g, ",");
      return NumberParseResult.error(
        self._generateEuropeanFormatError(originalString, ukSuggestion),
      );
    } else {
      const cleaned = numberPart.replace(new RegExp(`\\${separator}`, "g"), "");
      const result = parseFloat(cleaned);
      if (isNaN(result)) {
        return NumberParseResult.error(`Invalid number format: '${originalString}'`);
      }
      return NumberParseResult.success(result);
    }
  }

  /**
   * Parse single separator with single occurrence and ambiguous digit count.
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} separator - The separator character (',' or '.')
   * @param {number} digitsBefore - Number of digits before separator
   * @param {number} digitsAfter - Number of digits after separator
   * @param {string} originalString - Original string for error messages
   * @returns {NumberParseResult} Parsed number result
   * @private
   */
  _parseSingleSeparatorSingleOccurrence(
    numberPart,
    separator,
    digitsBefore,
    digitsAfter,
    originalString,
  ) {
    const self = this;
    if (separator === ",") {
      if (digitsAfter <= 2 && digitsBefore > 0) {
        const ukSuggestion = numberPart.replace(",", ".");
        return NumberParseResult.error(
          self._generateEuropeanFormatError(originalString, ukSuggestion),
        );
      } else {
        const result = parseFloat(numberPart.replace(/,/g, ""));
        if (isNaN(result)) {
          return NumberParseResult.error(`Invalid number format: '${originalString}'`);
        }
        return NumberParseResult.success(result);
      }
    } else {
      const result = parseFloat(numberPart);
      if (isNaN(result)) {
        return NumberParseResult.error(`Invalid number format: '${originalString}'`);
      }
      return NumberParseResult.success(result);
    }
  }

  /**
   * Parse number that starts with separator.
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} separator - The separator character (',' or '.')
   * @param {string} originalString - Original string for error messages
   * @returns {NumberParseResult} Parsed number result
   * @private
   */
  _parseStartsWithSeparator(numberPart, separator, originalString) {
    const self = this;
    if (separator === ",") {
      const ukSuggestion = numberPart.replace(",", ".");
      return NumberParseResult.error(
        self._generateEuropeanFormatError(originalString, ukSuggestion),
      );
    } else {
      const result = parseFloat(numberPart);
      if (isNaN(result)) {
        return NumberParseResult.error(`Invalid number format: '${originalString}'`);
      }
      return NumberParseResult.success(result);
    }
  }

  /**
   * Parse number with 4 or more digits before separator (unambiguous decimal).
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} separator - The separator character (',' or '.')
   * @param {string} originalString - Original string for error messages
   * @returns {NumberParseResult} Parsed number result
   * @private
   */
  _parseUnambiguousPriorDigits(numberPart, separator, originalString) {
    const self = this;
    if (separator === ",") {
      const ukSuggestion = numberPart.replace(",", ".");
      return NumberParseResult.error(
        self._generateEuropeanFormatError(originalString, ukSuggestion),
      );
    } else {
      const result = parseFloat(numberPart);
      if (isNaN(result)) {
        return NumberParseResult.error(`Invalid number format: '${originalString}'`);
      }
      return NumberParseResult.success(result);
    }
  }

  /**
   * Parse number starting with zero and separator (clearly a decimal).
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} separator - The separator character (',' or '.')
   * @param {string} originalString - Original string for error messages
   * @returns {NumberParseResult} Parsed number result
   * @private
   */
  _parseWithLeadingZero(numberPart, separator, originalString) {
    const self = this;
    if (separator === ",") {
      const ukSuggestion = numberPart.replace(",", ".");
      return NumberParseResult.error(
        self._generateEuropeanFormatError(originalString, ukSuggestion),
      );
    } else {
      const result = parseFloat(numberPart);
      if (isNaN(result)) {
        return NumberParseResult.error(`Invalid number format: '${originalString}'`);
      }
      return NumberParseResult.success(result);
    }
  }

  /**
   * Interpret ambiguous format with 3 digits after separator (like 123,456).
   *
   * @param {string} numberPart - Number part to parse
   * @param {string} separator - The separator character (',' or '.')
   * @param {string} originalString - Original string for error messages
   * @returns {NumberParseResult} Parsed number result
   * @private
   */
  _interpretAmbiguous(numberPart, separator, originalString) {
    const self = this;
    if (separator === ",") {
      const result = parseFloat(numberPart.replace(/,/g, ""));
      if (isNaN(result)) {
        return NumberParseResult.error(`Invalid number format: '${originalString}'`);
      }
      return NumberParseResult.success(result);
    } else {
      const result = parseFloat(numberPart);
      if (isNaN(result)) {
        return NumberParseResult.error(`Invalid number format: '${originalString}'`);
      }
      return NumberParseResult.success(result);
    }
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
    const self = this;
    const beforeSeparator = numberStr.substring(0, separatorIndex);

    // Use length-based check as suggested in feedback, but adjusted to handle common cases
    return beforeSeparator.length >= 1;
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
    const self = this;
    return str.length - str.replaceAll(char, "").length;
  }

  /**
   * Convert European format number to UK format suggestion.
   *
   * Handle different European patterns:
   * - "1.234,56" to "1,234.56"
   * - "123,45" to "123.45"
   * - "1.234.567,89" to "1,234,567.89"
   *
   * @param {string} numberPart - European format number part
   * @returns {string} UK format equivalent
   * @private
   */
  _convertEuropeanToUkFormat(numberPart) {
    const lastCommaIndex = numberPart.lastIndexOf(",");
    const lastPeriodIndex = numberPart.lastIndexOf(".");

    const europeanMixed = lastCommaIndex > lastPeriodIndex && lastCommaIndex !== -1;
    const singleComma = lastCommaIndex !== -1 && lastPeriodIndex === -1;

    if (europeanMixed) {
      return numberPart.replace(/\./g, ",").replace(/,([^,]*)$/, ".$1");
    } else if (singleComma) {
      return numberPart.replace(",", ".");
    }

    return numberPart;
  }

  /**
   * Generate standardized error message for European format detection.
   *
   * @param {string} originalInput - The original user input
   * @param {string} ukSuggestion - The UK format suggestion
   * @returns {string} Formatted error message
   * @private
   */
  _generateEuropeanFormatError(originalInput, ukSuggestion) {
    return `Unsupported number format: '${originalInput}'. ` +
           `Please use: '${ukSuggestion}'. ` +
           "Kigali Sim requires comma for thousands separator and period for decimal point.";
  }
}

export {NumberParseUtil, NumberParseResult};
