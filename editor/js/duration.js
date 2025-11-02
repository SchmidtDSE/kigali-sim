/**
 * Duration parsing utilities and classes for handling year values.
 *
 * @license BSD, see LICENSE.md.
 */

/**
 * Class representing a parsed year value that can handle numeric years,
 * special string values ("beginning", "onwards"), and null values.
 * Provides methods for comparison, display, and type checking while
 * preserving original string formatting for user experience.
 */
class ParsedYear {
  /**
   * Create a new ParsedYear instance.
   *
   * @param {number|string|null} year - The year value. Can be a numeric year,
   *   special strings ("beginning", "onwards"), or null for unbounded ranges.
   * @param {string|null} originalString - The original string representation
   *   to preserve user formatting. If not provided, will be derived from year.
   */
  constructor(year, originalString) {
    const self = this;

    self._year = self._determineYear(year);
    self._originalString = originalString || (year !== null ? String(year) : null);
  }

  /**
   * Determine the year value from input, converting numeric strings to numbers.
   *
   * @private
   * @param {number|string|null} year - The year value to process.
   * @returns {number|string|null} The processed year value.
   */
  _determineYear(year) {
    // Convert numeric strings to numbers for proper handling
    // while preserving special strings like "beginning" and "onwards"
    if (typeof year === "string" && year !== "beginning" && year !== "onwards") {
      const numericValue = parseFloat(year);
      if (!isNaN(numericValue) && isFinite(numericValue)) {
        return numericValue;
      } else {
        return year;
      }
    } else {
      return year;
    }
  }

  /**
   * Check if this ParsedYear represents a finite numeric year.
   *
   * @returns {boolean} True if the year is a finite number, false for
   *   special strings ("beginning", "onwards") or null values.
   */
  hasFiniteNumericYear() {
    const self = this;
    return typeof self._year === "number" && isFinite(self._year);
  }

  /**
   * Get the numeric year value.
   *
   * @returns {number|null} The numeric year if hasFiniteNumericYear() is true,
   *   null otherwise.
   */
  getNumericYear() {
    const self = this;
    return self.hasFiniteNumericYear() ? self._year : null;
  }

  /**
   * Get the string representation of the year for display or code generation.
   *
   * @returns {string} The original string representation if available,
   *   otherwise a string representation of the year value.
   */
  getYearStr() {
    const self = this;
    if (self._originalString !== null) {
      return self._originalString;
    }
    return self._year !== null ? String(self._year) : "";
  }

  /**
   * Compare this ParsedYear with another for equality.
   * For numeric years, compares the numeric values.
   * For special strings and null, compares string representations.
   *
   * @param {ParsedYear} otherParsedYear - The other ParsedYear to compare against.
   * @returns {boolean} True if the years are considered equal.
   */
  equals(otherParsedYear) {
    const self = this;

    if (!(otherParsedYear instanceof ParsedYear)) {
      return false;
    }

    const bothFinite = self.hasFiniteNumericYear() && otherParsedYear.hasFiniteNumericYear();
    if (bothFinite) {
      return self.getNumericYear() === otherParsedYear.getNumericYear();
    } else {
      return self.getYearStr() === otherParsedYear.getYearStr();
    }
  }
}

/**
 * Class representing a range of years where inclusion can be tested.
 */
class YearMatcher {
  /**
   * Create a new year range.
   *
   * Create a new year range between start and end where null in either means positive or negative
   * infinity.
   *
   * @param {ParsedYear|null} start The starting year (inclusive) in this range or null if
   *   no min year.
   * @param {ParsedYear|null} end The ending year (inclusive) in this range or null if no max year.
   */
  constructor(start, end) {
    const self = this;

    const hasNull = start === null || end === null;
    const startHasSpecial = start && !start.hasFiniteNumericYear();
    const endHasSpecial = end && !end.hasFiniteNumericYear();

    if (hasNull || startHasSpecial || endHasSpecial) {
      self._start = start;
      self._end = end;
    } else {
      const startValue = start.getNumericYear();
      const endValue = end.getNumericYear();
      const startRearrange = Math.min(startValue, endValue);
      const endRearrange = Math.max(startValue, endValue);

      self._start = new ParsedYear(startRearrange);
      self._end = new ParsedYear(endRearrange);
    }
  }

  /**
   * Determine if a year is included in this range.
   *
   * @param year The year to test for inclusion.
   * @returns True if this value is between getStart and getEnd.
   */
  getInRange(year) {
    const self = this;
    const startIsNumeric = self._start && self._start.hasFiniteNumericYear();
    const endIsNumeric = self._end && self._end.hasFiniteNumericYear();
    const startValue = startIsNumeric ? self._start.getNumericYear() : self._start;
    const endValue = endIsNumeric ? self._end.getNumericYear() : self._end;
    const meetsMin = startValue === null || startValue <= year;
    const meetsMax = endValue === null || endValue >= year;
    return meetsMin && meetsMax;
  }

  /**
   * Get the start of the year range.
   *
   * @returns The minimum included year in this range or null if negative infinity.
   */
  getStart() {
    const self = this;
    return self._start;
  }

  /**
   * Get the end of the year range.
   *
   * @returns The maximum included year in this range or null if positive infinity.
   */
  getEnd() {
    const self = this;
    return self._end;
  }
}

export {ParsedYear, YearMatcher};
