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
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;

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
   * Handle setting newEquipment to a target value (absolute in units/kg/mt/tCO2e,
   * or bare percent).
   *
   * <p>Caller is responsible for checking year range before calling this method.</p>
   *
   * <p>Unit-based targets ("unit"/"units") are set directly as an absolute sales
   * value in units: the existing implicit-recharge machinery for unit-tracked sales
   * automatically adds recharge and precharge volume on top, so no extra accounting
   * is needed here.</p>
   *
   * <p>Mass-based targets (kg/mt/tCO2e) represent only the equipment-forming portion
   * of sales (matching how newEquipment itself is defined as
   * {@code (salesKg - rechargeKg - prechargeKg) / initialCharge}). To land sales at a
   * value that reproduces exactly this target after the next population recalc
   * subtracts recharge/precharge again, this method explicitly adds the current
   * recharge and precharge volume (in kg) on top before setting sales in kg.</p>
   *
   * <p>Bare percent (decision 3) resolves against this year's already-computed
   * newEquipment value (pre-adjustment), mirroring how
   * {@link EquipmentChangeUtil#handleChange} resolves percent against
   * currentEquipment for the equipment stream.</p>
   *
   * @param targetNewEquipment The target value: bare "%" or absolute in units/kg/mt/tCO2e
   */
  public void handleSet(EngineNumber targetNewEquipment) {
    EngineNumber currentNewEquipment = engine.getStream("newEquipment");
    UnitConverter unitConverter = createNewEquipmentUnitConverter();

    EngineNumber targetAbsolute;
    if ("%".equals(targetNewEquipment.getUnits())) {
      BigDecimal targetValue = calculatePercentageChange(currentNewEquipment, targetNewEquipment);
      targetAbsolute = new EngineNumber(targetValue, currentNewEquipment.getUnits());
    } else {
      targetAbsolute = targetNewEquipment;
    }

    String targetUnitsString = targetAbsolute.getUnits();
    boolean isUnitBased = "unit".equals(targetUnitsString) || "units".equals(targetUnitsString);

    if (isUnitBased) {
      EngineNumber targetUnits = unitConverter.convert(targetAbsolute, "units");
      BigDecimal clampedUnits = clampAtZero(targetUnits.getValue());
      setSalesAbsolute(new EngineNumber(clampedUnits, "units"));
    } else {
      EngineNumber targetKg = unitConverter.convert(targetAbsolute, "kg");
      BigDecimal clampedTargetKg = clampAtZero(targetKg.getValue());

      EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
          engine.getScope(), engine.getStateGetter(), engine.getStreamKeeper(), engine);
      EngineNumber prechargeVolume = PrechargeVolumeCalculator.calculatePrechargeVolume(
          engine.getScope(), engine.getStateGetter(), engine.getStreamKeeper(), engine);

      BigDecimal salesKg = clampedTargetKg
          .add(rechargeVolume.getValue())
          .add(prechargeVolume.getValue());

      engine.setStream("sales", new EngineNumber(salesKg, "kg"), Optional.empty());
    }
  }

  /**
   * Set sales to an absolute value in units (used by handleSet's unit-based path).
   *
   * <p>Mirrors {@link EquipmentChangeUtil}'s private {@code setSales} helper:
   * marking lastSpecifiedValue as unit-tracked before delegating to
   * {@link SetExecutor#handleSalesSet} ensures sales is recorded as unit-tracked, so
   * the existing implicit-recharge machinery adds recharge/precharge on top
   * automatically on future recalcs.</p>
   *
   * @param salesUnits The absolute sales value to set, in "units"
   */
  private void setSalesAbsolute(EngineNumber salesUnits) {
    UseKey scope = engine.getScope();
    SimulationState simulationState = engine.getStreamKeeper();
    simulationState.setLastSpecifiedValue(scope, "sales", salesUnits);

    SetExecutor setExecutor = new SetExecutor(engine);
    setExecutor.handleSalesSet(scope, "sales", salesUnits, Optional.empty());
  }

  /**
   * Clamp an absolute value at zero (decision 1: silent clamp, no error).
   *
   * <p>Delegates to {@link #clampDeltaAtZero} with a zero base value, which already
   * implements exactly this "never go below zero" logic when called that way.</p>
   *
   * @param value The value to clamp
   * @return The value if non-negative, otherwise zero
   */
  private BigDecimal clampAtZero(BigDecimal value) {
    return clampDeltaAtZero(BigDecimal.ZERO, value);
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
