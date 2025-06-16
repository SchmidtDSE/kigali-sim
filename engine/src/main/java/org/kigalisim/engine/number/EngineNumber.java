/**
 * Structures to represent numbers with units inside the engine.
 *
 * <p>This class provides a representation of a number with units within the engine,
 * using BigDecimal for numerical stability.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.number;

import java.math.BigDecimal;

/**
 * Representation of a number with units within the engine.
 *
 * <p>This class encapsulates a numeric value (using BigDecimal for precision) and its
 * associated units string. It provides methods to access the value, units, and check
 * for specific unit types.</p>
 */
public class EngineNumber {
  private final BigDecimal value;
  private final String units;

  /**
   * Create a new number with units.
   *
   * @param value The numeric value as a BigDecimal
   * @param units The units to associate with this value like "kg"
   */
  public EngineNumber(BigDecimal value, String units) {
    this.value = value;
    this.units = units;
  }

  /**
   * Create a new number with units from a double value.
   *
   * <p>This constructor converts the double to BigDecimal for internal storage.</p>
   *
   * @param value The numeric value as a double
   * @param units The units to associate with this value like "kg"
   */
  public EngineNumber(double value, String units) {
    this.value = BigDecimal.valueOf(value);
    this.units = units;
  }

  /**
   * Get the value of this number.
   *
   * @return Value as a BigDecimal
   */
  public BigDecimal getValue() {
    return value;
  }

  /**
   * Get the units associated with this number.
   *
   * @return The units as a string like "mt"
   */
  public String getUnits() {
    return units;
  }

  /**
   * Check if this number has equipment units.
   *
   * @return true if the units represent equipment units (start with "unit")
   */
  public boolean hasEquipmentUnits() {
    return units.startsWith("unit");
  }
}