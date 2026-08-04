/**
 * Utility class for handling newEquipment stream modifications.
 *
 * <p>This class provides logic for set, change, cap, and floor operations on the
 * newEquipment stream. It converts newEquipment operations into appropriate sales
 * operations since newEquipment is a purely derived (marginal) quantity recomputed
 * every year from priorEquipment, sales, recharge, and precharge.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.math.MathContext;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;

/**
 * Handles newEquipment stream operations by converting them into sales operations.
 *
 * <p>Unlike {@link EquipmentChangeUtil}, this class never needs to retire prior stock:
 * newEquipment is a purely marginal (delta) quantity recomputed fresh each year, so every
 * operation bottoms out in a sales change or set rather than a retirement.</p>
 */
public class NewEquipmentChangeUtil {
  private final Engine engine;

  /**
   * Creates a new NewEquipmentChangeUtil for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public NewEquipmentChangeUtil(Engine engine) {
    this.engine = engine;
  }

  /**
   * Handle changing newEquipment by a delta amount (percent or absolute, any units).
   *
   * <p>Caller is responsible for checking year range before calling this method.</p>
   *
   * @param changeAmount The change amount (bare "%" or absolute in units/kg/mt/tCO2e)
   */
  public void handleChange(EngineNumber changeAmount) {
    EngineNumber currentNewEquipment = engine.getStream("newEquipment");
    UnitConverter unitConverter = createNewEquipmentUnitConverter();

    BigDecimal deltaUnits;
    if ("%".equals(changeAmount.getUnits())) {
      deltaUnits = calculatePercentageChange(currentNewEquipment, changeAmount);
    } else {
      EngineNumber changeUnits = unitConverter.convert(changeAmount, "units");
      deltaUnits = changeUnits.getValue();
    }

    BigDecimal clampedDeltaUnits = clampDeltaAtZero(currentNewEquipment.getValue(), deltaUnits);
    if (clampedDeltaUnits.compareTo(BigDecimal.ZERO) == 0) {
      return;
    }

    engine.changeStream("sales", new EngineNumber(clampedDeltaUnits, "units"), null);
  }

  /**
   * Calculate absolute change from percentage.
   *
   * @param currentValue The current value
   * @param percentChange The percentage change (e.g., 8 for +8%)
   * @return The absolute change in same units as currentValue
   */
  private BigDecimal calculatePercentageChange(EngineNumber currentValue,
      EngineNumber percentChange) {
    return currentValue.getValue()
        .multiply(percentChange.getValue())
        .divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
  }

  /**
   * Clamp a proposed delta so that currentValue + delta never goes below zero.
   *
   * @param currentValue The current value the delta will be applied to
   * @param proposedDelta The proposed delta before clamping
   * @return The clamped delta, landing exactly at zero if the unclamped result would be negative
   */
  private BigDecimal clampDeltaAtZero(BigDecimal currentValue, BigDecimal proposedDelta) {
    if (currentValue.add(proposedDelta).compareTo(BigDecimal.ZERO) < 0) {
      return currentValue.negate();
    }
    return proposedDelta;
  }

  /**
   * Create a unit converter for the newEquipment stream.
   *
   * @return A UnitConverter configured for the newEquipment stream
   */
  private UnitConverter createNewEquipmentUnitConverter() {
    return EngineSupportUtils.createUnitConverterWithTotal(engine, "newEquipment");
  }
}
