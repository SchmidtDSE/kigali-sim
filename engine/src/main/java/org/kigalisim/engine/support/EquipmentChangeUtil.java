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
import org.kigalisim.engine.state.Scope;
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
    boolean isInRange = !yearMatcher.isPresent()
        || EngineSupportUtils.isInRange(yearMatcher.get(), engine.getYear());
    if (!isInRange) {
      return;
    }

    UnitConverter unitConverter = createEquipmentUnitConverter();

    // Convert target to units
    EngineNumber targetUnits = unitConverter.convert(targetEquipment, "units");

    // Get priorEquipment to calculate the required sales
    EngineNumber priorEquipmentRaw = engine.getStream("priorEquipment");
    EngineNumber priorEquipment = unitConverter.convert(priorEquipmentRaw, "units");

    // Calculate required sales to reach target
    BigDecimal requiredSales = targetUnits.getValue().subtract(priorEquipment.getValue());

    // Determine if we need to increase sales or retire equipment
    BigDecimal delta = requiredSales;

    // Handle based on delta direction
    if (delta.compareTo(BigDecimal.ZERO) > 0) {
      // Increase: set sales to achieve target
      EngineNumber salesUnits = new EngineNumber(delta, "units");
      setSales(salesUnits, yearMatcher);
    } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
      // Decrease: set sales to 0 and retire equipment
      EngineNumber zeroSales = new EngineNumber(BigDecimal.ZERO, "units");
      setSales(zeroSales, yearMatcher);
      EngineNumber unitsToRetire = new EngineNumber(delta.abs(), "units");
      retireFromPriorEquipment(unitsToRetire, yearMatcher);
    }
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
    handleChange(changeAmount, Optional.of(yearMatcher));
  }

  /**
   * Handle changing equipment by a delta amount (internal version).
   *
   * <p>This is the core implementation used by both the public handleChange method
   * and the refactored handleCap/handleFloor methods. It supports both percentage
   * and absolute changes and handles optional year matching.</p>
   *
   * <p>Supports both percentage and absolute changes:
   * - Percentage: change equipment by +8%
   * - Absolute: change equipment by +100 units</p>
   *
   * @param changeAmount The change amount (percentage or units)
   * @param yearMatcher Optional matcher to determine if change applies to current year
   */
  private void handleChange(EngineNumber changeAmount, Optional<YearMatcher> yearMatcher) {
    // Check year range if yearMatcher is present
    if (yearMatcher.isPresent()) {
      boolean isInRange = EngineSupportUtils.isInRange(yearMatcher.get(), engine.getYear());
      if (!isInRange) {
        return;
      }
    }

    // Get current equipment level
    EngineNumber currentEquipmentRaw = engine.getStream("equipment");
    UnitConverter unitConverter = createEquipmentUnitConverter();
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
      changeSales(deltaUnits, yearMatcher);
    } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
      EngineNumber unitsToRetire = new EngineNumber(delta.abs(), "units");
      retireEquipment(unitsToRetire, yearMatcher);
    }
  }

  /**
   * Handle capping equipment to a maximum value.
   *
   * <p>If current equipment exceeds cap, reduces equipment by the excess amount.
   * Supports displacement to move retired equipment to another substance.</p>
   *
   * <p>This method now uses change semantics: it calculates the delta (excess over cap)
   * and calls handleChange with a negative delta to reduce equipment.</p>
   *
   * @param capValue The maximum equipment level
   * @param yearMatcher Matcher to determine if cap applies to current year
   * @param displaceTarget Optional substance/stream to displace to (null for no displacement)
   */
  public void handleCap(EngineNumber capValue, YearMatcher yearMatcher, String displaceTarget) {
    // Check year range
    boolean isInRange = EngineSupportUtils.isInRange(yearMatcher, engine.getYear());
    if (!isInRange) {
      return;
    }

    // Get current equipment level
    EngineNumber currentEquipmentRaw = engine.getStream("equipment");
    UnitConverter unitConverter = createEquipmentUnitConverter();
    EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");
    EngineNumber capUnits = unitConverter.convert(capValue, "units");

    // Only act if current exceeds cap
    if (currentEquipment.getValue().compareTo(capUnits.getValue()) > 0) {
      // Calculate excess (amount over the cap)
      BigDecimal excess = currentEquipment.getValue().subtract(capUnits.getValue());
      EngineNumber excessUnits = new EngineNumber(excess, "units");

      // Get equipment level before change for displacement calculation
      EngineNumber equipmentBefore = unitConverter.convert(engine.getStream("equipment"), "units");

      // Use handleChange to reduce equipment by the excess amount
      EngineNumber negativeChange = new EngineNumber(excess.negate(), "units");
      handleChange(negativeChange, Optional.of(yearMatcher));

      // Calculate actual change in equipment for displacement
      EngineNumber equipmentAfter = unitConverter.convert(engine.getStream("equipment"), "units");
      BigDecimal actualChange = equipmentBefore.getValue().subtract(equipmentAfter.getValue());
      EngineNumber actualReduction = new EngineNumber(actualChange, "units");

      // Handle displacement if specified - only displace what was actually changed
      if (displaceTarget != null && actualChange.compareTo(BigDecimal.ZERO) > 0) {
        handleDisplacement(actualReduction, displaceTarget, true);
      }
    }
  }

  /**
   * Handle flooring equipment to a minimum value.
   *
   * <p>If current equipment is below floor, increases equipment by the deficit amount.
   * Supports displacement to offset the increase.</p>
   *
   * <p>This method now uses change semantics: it calculates the delta (deficit under floor)
   * and calls handleChange with a positive delta to increase equipment.</p>
   *
   * @param floorValue The minimum equipment level
   * @param yearMatcher Matcher to determine if floor applies to current year
   * @param displaceTarget Optional substance/stream to displace from (null for no displacement)
   */
  public void handleFloor(EngineNumber floorValue, YearMatcher yearMatcher,
      String displaceTarget) {
    // Check year range
    boolean isInRange = EngineSupportUtils.isInRange(yearMatcher, engine.getYear());
    if (!isInRange) {
      return;
    }

    // Get current equipment level
    EngineNumber currentEquipmentRaw = engine.getStream("equipment");
    UnitConverter unitConverter = createEquipmentUnitConverter();
    EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");
    EngineNumber floorUnits = unitConverter.convert(floorValue, "units");

    // Only act if current is below floor
    if (currentEquipment.getValue().compareTo(floorUnits.getValue()) < 0) {
      // Calculate deficit (amount under the floor)
      BigDecimal deficit = floorUnits.getValue().subtract(currentEquipment.getValue());
      EngineNumber deficitUnits = new EngineNumber(deficit, "units");

      // Get equipment level before change for displacement calculation
      EngineNumber equipmentBefore = unitConverter.convert(engine.getStream("equipment"), "units");

      // Use handleChange to increase equipment by the deficit amount
      handleChange(deficitUnits, Optional.of(yearMatcher));

      // Calculate actual change in equipment for displacement
      EngineNumber equipmentAfter = unitConverter.convert(engine.getStream("equipment"), "units");
      BigDecimal actualChange = equipmentAfter.getValue().subtract(equipmentBefore.getValue());
      EngineNumber actualIncrease = new EngineNumber(actualChange, "units");

      // Handle displacement if specified - only displace what was actually changed
      if (displaceTarget != null && actualChange.compareTo(BigDecimal.ZERO) > 0) {
        handleDisplacement(actualIncrease, displaceTarget, false);
      }
    }
  }

  /**
   * Set sales to an absolute value (used by handleSet).
   *
   * <p>Sets sales to the required value in units, which automatically handles recharge
   * through existing sales distribution logic.</p>
   *
   * @param salesUnits The absolute sales value to set
   * @param yearMatcher Optional year matcher
   */
  private void setSales(EngineNumber salesUnits, Optional<YearMatcher> yearMatcher) {
    UseKey scope = engine.getScope();
    SimulationState simulationState = engine.getStreamKeeper();
    simulationState.setLastSpecifiedValue(scope, "sales", salesUnits);

    SetExecutor setExecutor = new SetExecutor(engine);
    setExecutor.handleSalesSet(scope, "sales", salesUnits, yearMatcher);
  }

  /**
   * Change sales by a delta amount (used by handleChange).
   *
   * <p>Uses changeStream to incrementally change sales, allowing multiple
   * operations to stack properly.</p>
   *
   * @param salesDelta The sales change in units (can be positive or negative)
   * @param yearMatcher Optional year matcher
   */
  private void changeSales(EngineNumber salesDelta, Optional<YearMatcher> yearMatcher) {
    // Use changeStream to add to existing sales
    YearMatcher yearMatcherEffective = yearMatcher.orElse(null);
    engine.changeStream("sales", salesDelta, yearMatcherEffective);
  }

  /**
   * Retire equipment from priorEquipment only (used by handleSet).
   *
   * <p>Retires the specified amount from priorEquipment only. Does not modify sales.
   * If insufficient priorEquipment, retires only what's available.</p>
   *
   * @param unitsToRetire The number of units to retire
   * @param yearMatcher Optional year matcher
   * @return The actual number of units retired
   */
  private EngineNumber retireFromPriorEquipment(EngineNumber unitsToRetire, Optional<YearMatcher> yearMatcher) {
    UseKey scope = engine.getScope();
    UnitConverter unitConverter = createEquipmentUnitConverter();
    SimulationState simulationState = engine.getStreamKeeper();

    // Calculate retirement from priorEquipment
    EngineNumber priorEquipmentRaw = engine.getStream("priorEquipment");
    EngineNumber priorEquipment = unitConverter.convert(priorEquipmentRaw, "units");
    BigDecimal actualRetirement = priorEquipment.getValue().min(unitsToRetire.getValue());

    // Execute retirement if there's anything to retire from priorEquipment
    if (actualRetirement.compareTo(BigDecimal.ZERO) > 0) {
      // Convert to percentage of priorEquipment for retire command
      BigDecimal retirementPercentage = actualRetirement
          .divide(priorEquipment.getValue(), MathContext.DECIMAL128)
          .multiply(BigDecimal.valueOf(100));

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
    }

    // Return the actual amount retired
    return new EngineNumber(actualRetirement, "units");
  }

  /**
   * Retire equipment from priorEquipment (used by handleChange).
   *
   * <p>Retires the specified amount from priorEquipment. If priorEquipment is insufficient
   * to cover the full retirement, retires what's available from priorEquipment and uses
   * changeStream to reduce sales by the remainder. This enables proper handling of
   * percentage-based equipment changes.</p>
   *
   * @param unitsToRetire The number of units to retire
   * @param yearMatcher Optional year matcher
   * @return The actual number of units retired (may be less than requested if insufficient priorEquipment)
   */
  private EngineNumber retireEquipment(EngineNumber unitsToRetire, Optional<YearMatcher> yearMatcher) {
    UseKey scope = engine.getScope();
    UnitConverter unitConverter = createEquipmentUnitConverter();
    SimulationState simulationState = engine.getStreamKeeper();

    // Calculate retirement from priorEquipment
    EngineNumber priorEquipmentRaw = engine.getStream("priorEquipment");
    EngineNumber priorEquipment = unitConverter.convert(priorEquipmentRaw, "units");
    BigDecimal actualRetirement = priorEquipment.getValue().min(unitsToRetire.getValue());

    // Check if we need to reduce sales for the remainder
    BigDecimal remainder = unitsToRetire.getValue().subtract(actualRetirement);
    if (remainder.compareTo(BigDecimal.ZERO) > 0) {
      // Not enough priorEquipment - use changeStream to reduce sales by the remainder
      EngineNumber salesDecrease = new EngineNumber(remainder.negate(), "units");
      YearMatcher yearMatcherEffective = yearMatcher.orElse(null);
      engine.changeStream("sales", salesDecrease, yearMatcherEffective);
    }

    // Execute retirement if there's anything to retire from priorEquipment
    if (actualRetirement.compareTo(BigDecimal.ZERO) > 0) {
      // Convert to percentage of priorEquipment for retire command
      BigDecimal retirementPercentage = actualRetirement
          .divide(priorEquipment.getValue(), MathContext.DECIMAL128)
          .multiply(BigDecimal.valueOf(100));

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
    }

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
   * Create a unit converter for equipment stream.
   *
   * @return A UnitConverter configured for equipment stream
   */
  private UnitConverter createEquipmentUnitConverter() {
    return EngineSupportUtils.createUnitConverterWithTotal(
        engine,
        "equipment"
    );
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
    Scope currentScope = engine.getScope();

    // Switch to target substance scope
    engine.setSubstance(displaceTarget, true);

    // For cap: we removed equipment from current substance, add it to target
    // For floor: we added equipment to current substance, remove it from target
    EngineNumber changeAmount = isCap ? amount : new EngineNumber(amount.getValue().negate(), "units");

    // Get current equipment in target substance
    EngineNumber targetCurrentRaw = engine.getStream("equipment");
    UnitConverter targetConverter = createEquipmentUnitConverter();
    EngineNumber targetCurrent = targetConverter.convert(targetCurrentRaw, "units");

    // Calculate new target equipment level
    BigDecimal newTargetLevel = targetCurrent.getValue().add(changeAmount.getValue());
    EngineNumber newTarget = new EngineNumber(newTargetLevel, "units");

    // Set the new equipment level in target substance (this will trigger sales/recharge)
    handleSet(newTarget, Optional.empty());

    // Restore original scope
    String originalSubstance = currentScope.getSubstance();
    if (originalSubstance != null) {
      engine.setSubstance(originalSubstance, true);
    }
  }
}
