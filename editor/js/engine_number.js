/**
 * Structures to represent numbers with units inside the engine.
 *
 * @license BSD, see LICENSE.md.
 */

/**
 * Representation of a number with units within the engine.
 */
class EngineNumber {
  /**
   * Create a new number with units.
   *
   * @param value - The numeric value (float, or int).
   * @param units - The units to associate with this value like kg.
   * @param originalString - Optional original string format to preserve user formatting.
   */
  constructor(value, units, originalString) {
    const self = this;
    self._value = value;
    self._units = units;
    self._originalString = originalString;
  }

  /**
   * Get the value of this number.
   *
   * @returns Value as an integer or float.
   */
  getValue() {
    const self = this;
    return self._value;
  }

  /**
   * Get the units associated with this number.
   *
   * @returns The units as a string like "mt".
   */
  getUnits() {
    const self = this;
    return self._units;
  }

  /**
   * Check if this number has equipment units.
   *
   * @returns {boolean} True if the units represent equipment units.
   */
  hasEquipmentUnits() {
    const self = this;
    return self._units.startsWith("unit");
  }

  /**
   * Get the original string format if available.
   *
   * @returns {string|null} The original string format, or null if not available.
   */
  getOriginalString() {
    const self = this;
    return self._originalString || null;
  }

  /**
   * Check if this number has an original string format preserved.
   *
   * @returns {boolean} True if original string format is available.
   */
  hasOriginalString() {
    const self = this;
    return self._originalString !== undefined && self._originalString !== null;
  }
}

/**
 * Converts a numeric value to an unambiguous US-format string.
 *
 * Ensures numbers are formatted to avoid ambiguity (e.g., 1.234 becomes
 * "1.2340", 1234 becomes "1,234.0"). Used when we need to generate
 * formatted strings but don't have access to the user's original
 * formatting.
 *
 * @param {number} numericValue - The numeric value to format
 * @returns {string} The unambiguous US-format number string
 */
function makeNumberUnambiguousString(numericValue) {
  if (numericValue === null || numericValue === undefined || isNaN(numericValue)) {
    return String(numericValue);
  }

  const absValue = Math.abs(numericValue);
  const isNegative = numericValue < 0;

  // Handle integers (no decimal part)
  if (Number.isInteger(numericValue)) {
    const formatted = absValue.toLocaleString("en-US") + ".0";
    return isNegative ? "-" + formatted : formatted;
  }

  // Handle decimals - ensure at least one trailing zero for disambiguation
  let formatted = absValue.toLocaleString("en-US", {
    minimumFractionDigits: 1,
    maximumFractionDigits: 20,
  });

  // If the number doesn't end with 0, add a trailing 0 for disambiguation
  if (!formatted.endsWith("0")) {
    formatted += "0";
  }

  return isNegative ? "-" + formatted : formatted;
}

export {EngineNumber, makeNumberUnambiguousString};
