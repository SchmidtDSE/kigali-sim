/**
 * Utility class for handling equipment stream modifications.
 *
 * <p>This class provides logic for set, change, cap, and floor operations on
 * equipment streams. It converts equipment operations into appropriate sales
 * and retirement operations to ensure recharge needs are properly handled.</p>
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
import org.kigalisim.engine.recalc.RecalcKit;
import org.kigalisim.engine.recalc.RecalcKitBuilder;
import org.kigalisim.engine.recalc.RecalcOperation;
import org.kigalisim.engine.recalc.RecalcOperationBuilder;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Handles equipment stream operations with proper recharge accounting.
 *
 * <p>This class converts equipment-level operations into sales and retirement
 * operations that properly account for recharge needs. When equipment levels
 * change, the corresponding sales distributions and retirement rates are
 * calculated to maintain consistency with the simulation's recharge logic.</p>
 */
public class EquipmentChangeUtil {
  private final Engine engine;

  /**
   * Creates a new EquipmentChangeUtil for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public EquipmentChangeUtil(Engine engine) {
    this.engine = engine;
  }

  /**
   * Handle setting equipment to a target value.
   *
   * <p>Compares target equipment level with current level:
   * - If higher: converts delta to sales operation (handles recharge automatically)
   * - If lower: sets sales to 0 and retires excess from priorEquipment
   * - If equal: no action needed</p>
   *
   * @param targetEquipment The target equipment level in units
   * @param yearMatcher Optional year matcher for conditional setting
   */
  public void handleSet(EngineNumber targetEquipment, Optional<YearMatcher> yearMatcher) {
    // Check year range with EngineSupportUtils
    if (yearMatcher.isPresent()
        && !EngineSupportUtils.isInRange(yearMatcher.get(), engine.getYear())) {
      return;
    }

    // Get current equipment level
    EngineNumber currentEquipmentRaw = engine.getStream("equipment");
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine, "equipment");

    // Convert both to units
    EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");
    EngineNumber targetUnits = unitConverter.convert(targetEquipment, "units");

    // Calculate delta
    BigDecimal delta = targetUnits.getValue().subtract(currentEquipment.getValue());

    // Handle based on delta direction
    if (delta.compareTo(BigDecimal.ZERO) > 0) {
      // Increase: convert to sales
      EngineNumber deltaUnits = new EngineNumber(delta, "units");
      convertToSalesIncrease(deltaUnits, yearMatcher);
    } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
      // Decrease: retire equipment
      EngineNumber unitsToRetire = new EngineNumber(delta.abs(), "units");
      retireEquipment(unitsToRetire, yearMatcher);
    }
    // If delta == 0, no action needed
  }

  /**
   * Handle changing equipment by a delta amount.
   *
   * <p>Supports both percentage and absolute changes:
   * - Percentage: change equipment by +8%
   * - Absolute: change equipment by +100 units</p>
   *
   * @param changeAmount The change amount (percentage or units)
   * @param yearMatcher Matcher to determine if change applies to current year
   */
  public void handleChange(EngineNumber changeAmount, YearMatcher yearMatcher) {
    // Check year range
    if (!EngineSupportUtils.isInRange(yearMatcher, engine.getYear())) {
      return;
    }

    // Get current equipment level
    EngineNumber currentEquipmentRaw = engine.getStream("equipment");
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine, "equipment");
    EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");

    // Calculate delta based on units
    BigDecimal delta;
    if ("%".equals(changeAmount.getUnits())) {
      // Percentage change
      delta = calculatePercentageChange(currentEquipment, changeAmount);
    } else {
      // Absolute unit change
      EngineNumber changeUnits = unitConverter.convert(changeAmount, "units");
      delta = changeUnits.getValue();
    }

    // Handle based on delta direction
    if (delta.compareTo(BigDecimal.ZERO) > 0) {
      EngineNumber deltaUnits = new EngineNumber(delta, "units");
      convertToSalesIncrease(deltaUnits, Optional.of(yearMatcher));
    } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
      EngineNumber unitsToRetire = new EngineNumber(delta.abs(), "units");
      retireEquipment(unitsToRetire, Optional.of(yearMatcher));
    }
  }

  /**
   * Handle capping equipment to a maximum value.
   *
   * <p>If current equipment exceeds cap, retires excess. Supports displacement
   * to move retired equipment to another substance.</p>
   *
   * @param capValue The maximum equipment level
   * @param yearMatcher Matcher to determine if cap applies to current year
   * @param displaceTarget Optional substance/stream to displace to (null for no displacement)
   */
  public void handleCap(EngineNumber capValue, YearMatcher yearMatcher, String displaceTarget) {
    // Check year range
    if (!EngineSupportUtils.isInRange(yearMatcher, engine.getYear())) {
      return;
    }

    // Get current equipment level
    EngineNumber currentEquipmentRaw = engine.getStream("equipment");
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine, "equipment");
    EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");
    EngineNumber capUnits = unitConverter.convert(capValue, "units");

    // Only act if current exceeds cap
    if (currentEquipment.getValue().compareTo(capUnits.getValue()) > 0) {
      // Calculate excess
      BigDecimal excess = currentEquipment.getValue().subtract(capUnits.getValue());
      EngineNumber excessUnits = new EngineNumber(excess, "units");

      // Retire excess equipment (returns actual amount retired, may be less than requested)
      EngineNumber actualRetired = retireEquipment(excessUnits, Optional.of(yearMatcher));

      // Handle displacement if specified - only displace what was actually retired
      if (displaceTarget != null) {
        handleDisplacement(actualRetired, displaceTarget, true);
      }
    }
  }

  /**
   * Handle flooring equipment to a minimum value.
   *
   * <p>If current equipment is below floor, increases sales to meet minimum.
   * Supports displacement to offset the increase.</p>
   *
   * @param floorValue The minimum equipment level
   * @param yearMatcher Matcher to determine if floor applies to current year
   * @param displaceTarget Optional substance/stream to displace from (null for no displacement)
   */
  public void handleFloor(EngineNumber floorValue, YearMatcher yearMatcher,
      String displaceTarget) {
    // Check year range
    if (!EngineSupportUtils.isInRange(yearMatcher, engine.getYear())) {
      return;
    }

    // Get current equipment level
    EngineNumber currentEquipmentRaw = engine.getStream("equipment");
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine, "equipment");
    EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");
    EngineNumber floorUnits = unitConverter.convert(floorValue, "units");

    // Only act if current is below floor
    if (currentEquipment.getValue().compareTo(floorUnits.getValue()) < 0) {
      // Calculate deficit
      BigDecimal deficit = floorUnits.getValue().subtract(currentEquipment.getValue());
      EngineNumber deficitUnits = new EngineNumber(deficit, "units");

      // Increase sales to meet floor
      convertToSalesIncrease(deficitUnits, Optional.of(yearMatcher));

      // Handle displacement if specified
      if (displaceTarget != null) {
        handleDisplacement(deficitUnits, displaceTarget, false);
      }
    }
  }

  /**
   * Convert equipment increase to sales operation.
   *
   * <p>Sets sales to the delta in units, which automatically handles recharge
   * through existing sales distribution logic.</p>
   *
   * @param deltaUnits The number of units to add
   * @param yearMatcher Optional year matcher
   */
  private void convertToSalesIncrease(EngineNumber deltaUnits,
      Optional<YearMatcher> yearMatcher) {
    // Get scope from engine
    UseKey scope = engine.getScope();

    // Update lastSpecifiedValue for sales to maintain unit-based tracking
    SimulationState simulationState = engine.getStreamKeeper();
    simulationState.setLastSpecifiedValue(scope, "sales", deltaUnits);

    // Use SetExecutor to distribute to domestic/import
    SetExecutor setExecutor = new SetExecutor(engine);
    setExecutor.handleSalesSet(scope, "sales", deltaUnits, yearMatcher);
  }

  /**
   * Retire equipment from priorEquipment.
   *
   * <p>Sets sales to 0 units and retires the specified amount from priorEquipment.
   * If insufficient priorEquipment, retires only what's available.</p>
   *
   * @param unitsToRetire The number of units to retire
   * @param yearMatcher Optional year matcher
   * @return The actual number of units retired (may be less than requested if insufficient priorEquipment)
   */
  private EngineNumber retireEquipment(EngineNumber unitsToRetire, Optional<YearMatcher> yearMatcher) {
    UseKey scope = engine.getScope();
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine, "equipment");

    // Set sales to 0 units (maintains unit-based tracking)
    EngineNumber zeroSales = new EngineNumber(BigDecimal.ZERO, "units");
    SimulationState simulationState = engine.getStreamKeeper();
    simulationState.setLastSpecifiedValue(scope, "sales", zeroSales);

    SetExecutor setExecutor = new SetExecutor(engine);
    setExecutor.handleSalesSet(scope, "sales", zeroSales, yearMatcher);

    // Calculate retirement from priorEquipment
    EngineNumber priorEquipmentRaw = engine.getStream("priorEquipment");
    EngineNumber priorEquipment = unitConverter.convert(priorEquipmentRaw, "units");
    BigDecimal actualRetirement = priorEquipment.getValue().min(unitsToRetire.getValue());

    // Convert to percentage of priorEquipment for retire command
    BigDecimal retirementPercentage;
    if (priorEquipment.getValue().compareTo(BigDecimal.ZERO) == 0) {
      retirementPercentage = BigDecimal.ZERO;
    } else {
      retirementPercentage = actualRetirement
          .divide(priorEquipment.getValue(), MathContext.DECIMAL128)
          .multiply(BigDecimal.valueOf(100));
    }

    // Execute retirement
    EngineNumber retirementRate = new EngineNumber(retirementPercentage, "%");
    simulationState.setRetirementRate(scope, retirementRate);

    // Trigger retirement recalc using RecalcKit
    RecalcKit recalcKit = new RecalcKitBuilder()
        .setStreamKeeper(simulationState)
        .setUnitConverter(engine.getUnitConverter())
        .setStateGetter(engine.getStateGetter())
        .build();

    RecalcOperation operation = new RecalcOperationBuilder()
        .setRecalcKit(recalcKit)
        .recalcRetire()
        .thenPropagateToSales()
        .build();
    operation.execute(engine);

    // Return the actual amount retired
    return new EngineNumber(actualRetirement, "units");
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
   * Handle displacement logic for cap/floor operations.
   *
   * <p>For equipment displacement, this changes the equipment level in the target substance.
   * For cap operations, we increase the target substance's equipment by the displaced amount.
   * For floor operations, we decrease the target substance's equipment by the displaced amount.</p>
   *
   * @param amount The amount to displace (in units)
   * @param displaceTarget The target substance for displacement
   * @param isCap True if this is a cap operation (add to target), false for floor (subtract from target)
   */
  private void handleDisplacement(EngineNumber amount, String displaceTarget, boolean isCap) {
    if (displaceTarget == null) {
      return;
    }

    // Get original scope
    UseKey originalScope = engine.getScope();

    // Create scope for target substance
    org.kigalisim.engine.state.Scope currentScopeObj =
        (org.kigalisim.engine.state.Scope) originalScope;
    org.kigalisim.engine.state.Scope targetScope = currentScopeObj.getWithSubstance(displaceTarget);

    try {
      // Switch to target substance scope
      engine.setSubstance(displaceTarget, true);

      // For cap: we removed equipment from current substance, add it to target
      // For floor: we added equipment to current substance, remove it from target
      EngineNumber changeAmount = isCap ? amount : new EngineNumber(amount.getValue().negate(), "units");

      // Get current equipment in target substance
      EngineNumber targetCurrentRaw = engine.getStream("equipment");
      UnitConverter targetConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, "equipment");
      EngineNumber targetCurrent = targetConverter.convert(targetCurrentRaw, "units");

      // Calculate new target equipment level
      BigDecimal newTargetLevel = targetCurrent.getValue().add(changeAmount.getValue());
      EngineNumber newTarget = new EngineNumber(newTargetLevel, "units");

      // Set the new equipment level in target substance (this will trigger sales/recharge)
      handleSet(newTarget, Optional.empty());

    } finally {
      // Restore original scope
      String originalSubstance = currentScopeObj.getSubstance();
      if (originalSubstance != null) {
        engine.setSubstance(originalSubstance, true);
      }
    }
  }
}
