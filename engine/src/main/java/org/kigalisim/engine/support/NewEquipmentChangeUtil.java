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
import org.kigalisim.lang.operation.DisplacementType;

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
   * Handle capping newEquipment to a maximum value (absolute or percent-of-own-history).
   *
   * <p>Caller is responsible for checking year range before calling this method
   * (SingleThreadEngine.cap already checks getIsInRange before dispatching, same as it does for
   * the "equipment" case).</p>
   *
   * <p>Per decision 2, all four percent forms ({@code "%"}, {@code "% prior year"},
   * {@code "% current year"}, {@code "% current"}) resolve against newEquipment's own raw
   * stream history (prior-year or current-year), never against sales's lastSpecifiedValue or
   * UnitConverter's percent machinery -- see {@link #resolveLimitTargetUnits}. Absolute values are
   * converted to a units delta and applied via {@code changeStream("sales", ...)}; no explicit
   * recharge/precharge adjustment is needed since a marginal newEquipment delta always equals the
   * same marginal sales delta (recharge/precharge are computed from priorEquipment, fixed for the
   * year, and cancel out of any delta-based comparison).</p>
   *
   * <p>Displacement reuses {@link DisplaceExecutor} with {@code "sales"} as the displaced-stream
   * identifier (not {@code "newEquipment"}) since the reduction was just applied as a sales
   * change, and passing the derived stream name would be silently clobbered by the destination
   * substance's next population recalc.</p>
   *
   * @param capValue The maximum newEquipment level: absolute in units/kg/mt/tCO2e, or one of the
   *     four percent forms ("%", "% prior year", "% current year", "% current")
   * @param displaceTarget Optional substance/stream to displace the excess to (null for no
   *     displacement)
   * @param displacementType The displacement mode (EQUIVALENT, BY_VOLUME, or BY_UNITS)
   */
  public void handleCap(EngineNumber capValue, String displaceTarget,
      DisplacementType displacementType) {
    EngineNumber currentNewEquipment = engine.getStream("newEquipment");
    UnitConverter unitConverter = createNewEquipmentUnitConverter();

    EngineNumber capUnits = resolveLimitTargetUnits(capValue, currentNewEquipment, unitConverter);

    BigDecimal excessUnits = currentNewEquipment.getValue().subtract(capUnits.getValue());
    if (excessUnits.compareTo(BigDecimal.ZERO) <= 0) {
      return; // Cap already satisfied - no action, matching every other cap's no-op behavior.
    }

    BigDecimal deltaUnits = clampDeltaAtZero(currentNewEquipment.getValue(), excessUnits.negate());
    if (deltaUnits.compareTo(BigDecimal.ZERO) == 0) {
      return;
    }

    EngineNumber deltaUnitsNumber = new EngineNumber(deltaUnits, "units");
    engine.changeStream("sales", deltaUnitsNumber, null);

    if (displaceTarget != null) {
      BigDecimal changeInKg = unitConverter.convert(deltaUnitsNumber, "kg").getValue();
      DisplaceExecutor displaceExecutor = new DisplaceExecutor(engine);
      displaceExecutor.execute("sales", deltaUnitsNumber, changeInKg, displaceTarget,
          displacementType);
    }
  }

  /**
   * Resolve a cap/floor amount (percent or absolute) into an absolute newEquipment target in
   * "units", per decision 2. Shared with Component 4's handleFloor (identical resolution logic,
   * only the excess/deficit direction differs).
   *
   * @param limitValue The cap/floor amount (percent or absolute in units/kg/mt/tCO2e)
   * @param currentNewEquipment This year's already-computed (pre-limit) raw newEquipment value
   * @param unitConverter A unit converter configured for the newEquipment stream
   * @return The absolute target in "units", clamped at zero (decision 1)
   */
  EngineNumber resolveLimitTargetUnits(EngineNumber limitValue,
      EngineNumber currentNewEquipment, UnitConverter unitConverter) {
    String units = limitValue.getUnits();
    EngineNumber targetUnits;
    if (isPercentUnits(units)) {
      EngineNumber basis = isPriorYearBasis(units) ? getPriorNewEquipmentRaw() : currentNewEquipment;
      BigDecimal targetValue = calculatePercentageChange(basis, limitValue);
      targetUnits = new EngineNumber(targetValue, basis.getUnits()); // basis.getUnits() is "units"
    } else {
      targetUnits = unitConverter.convert(limitValue, "units");
    }
    return new EngineNumber(clampAtZero(targetUnits.getValue()), "units");
  }

  /**
   * Check whether a unit string is one of the four cap/floor percent forms.
   *
   * @param units The units string to check
   * @return True if units is "%", "%prioryear", "%currentyear", or "%current"
   */
  private boolean isPercentUnits(String units) {
    return switch (units) {
      case "%", "%prioryear", "%currentyear", "%current" -> true;
      default -> false;
    };
  }

  /**
   * Check whether a percent unit string resolves against the prior-year basis.
   *
   * <p>Per the existing cap/floor convention elsewhere in the engine, bare "%" aliases to the
   * prior-year basis (not the current-year basis used by change/set's bare "%"); "% current year"
   * and "% current" both resolve against the current-year basis instead.</p>
   *
   * @param units The units string to check
   * @return True for "%" or "%prioryear", false for "%currentyear" or "%current"
   */
  private boolean isPriorYearBasis(String units) {
    return "%".equals(units) || "%prioryear".equals(units);
  }

  /**
   * Fetch newEquipment's raw prior-year stream value (falls back to the current year's value if
   * no prior year exists yet, e.g. a cap that first takes effect in year 1).
   *
   * @return The prior-year (or current-year fallback) raw newEquipment value, in units
   */
  private EngineNumber getPriorNewEquipmentRaw() {
    return engine.getStreamKeeper().getStream(engine.getScope(), "newEquipment", true);
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
