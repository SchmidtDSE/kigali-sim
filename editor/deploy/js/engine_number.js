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
   */
  constructor(value, units) {
    const self = this;
    self._value = value;
    self._units = units;
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
}

export {EngineNumber};
