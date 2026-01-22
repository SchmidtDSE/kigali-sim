/**
 * Executor for cap and floor limit operations on streams.
 *
 * <p>This class handles applying maximum (cap) and minimum (floor) constraints to
 * stream values. Limit operations enable policy scenarios like manufacturing caps, import quotas,
 * and minimum production requirements. Both absolute values and percentage-based limits are
 * supported, with optional displacement to offset changes in one stream with changes in
 * another.</p>
 *
 * <p>Limit operations support two specification modes:</p>
 * <ul>
 *   <li><strong>Percentage-based limits</strong>: Applied relative to lastSpecifiedValue,
 * enabling compounding effects year-over-year (e.g., "cap to 85%" means 85% of the last
 * user-specified value, not 85% of current calculated value)</li> <li><strong>Absolute value
 * limits</strong>: Direct numeric constraints (e.g., "cap to 1000 mt" sets hard maximum regardless
 * of previous values)</li>
 * </ul>
 *
 * <p>When limits are exceeded:</p>
 * <ol>
 *   <li>The stream value is adjusted to meet the limit via StreamUpdate</li>
 *   <li>If displacement is specified, the change is offset through DisplaceExecutor</li>
 * </ol>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.operation.DisplacementType;

/**
 * Executor for cap and floor limit operations on streams.
 */
public class LimitExecutor {
  private final Engine engine;
  private final StreamUpdateShortcuts shortcuts;
  private final DisplaceExecutor displaceExecutor;

  /**
   * Creates a new LimitExecutor for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public LimitExecutor(Engine engine) {
    this.engine = engine;
    this.shortcuts = new StreamUpdateShortcuts(engine);
    this.displaceExecutor = new DisplaceExecutor(engine);
  }

  /**
   * Validates that the stream is not the equipment stream.
   *
   * <p>Equipment streams should never be processed by this executor as they are handled
   * separately by EquipmentChangeUtil. This validation should never fail if routing from
   * SingleThreadEngine is correct.</p>
   *
   * @param stream The stream identifier to validate
   * @throws IllegalStateException if stream is "equipment"
   */
  private void checkIsNotEquipment(String stream) {
    if ("equipment".equals(stream)) {
      throw new IllegalStateException(
          "Equipment stream operations should be handled by EquipmentChangeUtil");
    }
  }

  /**
   * Executes a cap operation to limit a stream to a maximum value.
   *
   * <p>Cap operations enforce maximum constraints on stream values, reducing the
   * stream if it exceeds the specified limit. This is commonly used for policy scenarios like
   * manufacturing caps, import quotas, and consumption limits.</p>
   *
   * <p>For percentage-based caps (e.g., "cap to 85%"), the limit is calculated
   * relative to the lastSpecifiedValue rather than the current calculated value.
   * This enables year-over-year compounding effects. For example, if the user specified "set domestic
   * to 1000 mt" in a previous year, then "cap to 85%" in the current year, the cap will be 850 mt
   * (85% of 1000), not 85% of the current calculated domestic value which may include recharge
   * adjustments.</p>
   *
   * <p>If the current stream value exceeds the cap:</p>
   * <ol>
   *   <li>The stream is reduced to the cap value via StreamUpdate</li>
   *   <li>If displacement is specified, the reduction is offset through DisplaceExecutor</li>
   * </ol>
   *
   * @param stream The stream identifier to cap (e.g., "domestic", "import", "sales")
   * @param amount The maximum value (percentage or absolute)
   * @param yearMatcher Matcher to determine if the cap applies to current year
   * @param displaceTarget Optional target for displacement (stream or substance), or null
   * @param displacementType The type of displacement (EQUIVALENT, BY_VOLUME, or BY_UNITS)
   */
  public void executeCap(String stream, EngineNumber amount, YearMatcher yearMatcher,
      String displaceTarget, DisplacementType displacementType) {
    if (!EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear())) {
      return;
    }

    checkIsNotEquipment(stream);

    if ("%".equals(amount.getUnits())) {
      capWithPercent(stream, amount, displaceTarget, displacementType);
    } else {
      capWithValue(stream, amount, displaceTarget, displacementType);
    }
  }

  /**
   * Executes a floor operation to enforce a minimum value on a stream.
   *
   * <p>Floor operations enforce minimum constraints on stream values, increasing the
   * stream if it falls below the specified limit. This is commonly used for policy scenarios like
   * minimum production requirements, guaranteed import levels, and baseline consumption mandates.</p>
   *
   * <p>For percentage-based floors (e.g., "floor to 80%"), the limit is calculated
   * relative to the lastSpecifiedValue rather than the current calculated value.
   * This enables year-over-year compounding effects. For example, if the user specified "set domestic
   * to 1000 mt" in a previous year, then "floor to 80%" in the current year, the floor will be 800 mt
   * (80% of 1000), not 80% of the current calculated domestic value which may include recharge
   * adjustments.</p>
   *
   * <p>If the current stream value falls below the floor:</p>
   * <ol>
   *   <li>The stream is increased to the floor value via StreamUpdate</li>
   *   <li>If displacement is specified, the increase is offset through DisplaceExecutor</li>
   * </ol>
   *
   * @param stream The stream identifier to floor (e.g., "domestic", "import", "sales")
   * @param amount The minimum value (percentage or absolute)
   * @param yearMatcher Matcher to determine if the floor applies to current year
   * @param displaceTarget Optional target for displacement (stream or substance), or null
   * @param displacementType The type of displacement (EQUIVALENT, BY_VOLUME, or BY_UNITS)
   */
  public void executeFloor(String stream, EngineNumber amount, YearMatcher yearMatcher,
      String displaceTarget, DisplacementType displacementType) {
    if (!EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear())) {
      return;
    }

    checkIsNotEquipment(stream);

    if ("%".equals(amount.getUnits())) {
      floorWithPercent(stream, amount, displaceTarget, displacementType);
    } else {
      floorWithValue(stream, amount, displaceTarget, displacementType);
    }
  }

  /**
   * Applies percentage-based cap operation using prior year's value for compounding effect.
   *
   * <p>This method implements the cap logic for percentage-based specifications. The
   * percentage is applied to the prior year's actual stream value rather than the current
   * calculated value. This approach enables proper year-over-year compounding where policy
   * restrictions build on previous results.</p>
   *
   * <p>Algorithm:</p>
   * <ol>
   *   <li>If prior year value exists: Calculate cap as percentage of that value,
   *       apply StreamUpdate if current exceeds cap, handle displacement</li>
   *   <li>If no prior year exists: Treat percentage as volume (legacy behavior),
   *       apply reduction if needed, handle displacement</li>
   * </ol>
   *
   * <p>Example: Stream was 1000 mt in year 4. In year 5, "cap domestic to 85%"
   * will cap to 850 mt (85% of prior year's 1000 mt).</p>
   *
   * @param stream The stream name to cap
   * @param amount The percentage cap amount (e.g., 85 for 85%)
   * @param displaceTarget The target substance/stream for displacement, or null
   * @param displacementType The type of displacement (EQUIVALENT, BY_VOLUME, or BY_UNITS)
   */
  private void capWithPercent(String stream, EngineNumber amount, String displaceTarget,
      DisplacementType displacementType) {
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine, stream);
    EngineNumber currentValueRaw = engine.getStream(stream);
    EngineNumber currentValue = unitConverter.convert(currentValueRaw, "kg");

    SimulationState simulationState = engine.getStreamKeeper();
    Scope scope = engine.getScope();
    EngineNumber lastSpecified = simulationState.getStream(scope, stream, true);
    boolean hasPrior = lastSpecified != null;

    if (hasPrior) {
      EngineNumber percentPriorYear = new EngineNumber(amount.getValue(), "% prior year");
      EngineNumber newCappedValue = unitConverter.convert(percentPriorYear, lastSpecified.getUnits());

      EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
      EngineNumber newCappedInKg = unitConverter.convert(newCappedValue, "kg");

      boolean capSatisfied = currentInKg.getValue().compareTo(newCappedInKg.getValue()) <= 0;
      if (capSatisfied) {
        return;
      }

      StreamUpdate update = new StreamUpdateBuilder()
          .setName(stream)
          .setValue(newCappedValue)
          .setYearMatcher(Optional.empty())
          .inferSubtractRecycling()
          .build();
      engine.executeStreamUpdate(update);

      // Update lastSpecified so subsequent percentage changes use the capped value
      simulationState.setLastSpecifiedValue(scope, stream, newCappedValue);

      // For "sales" stream, also update lastSpecified for component streams (domestic/import)
      if ("sales".equals(stream)) {
        updateComponentStreamsLastSpecified(simulationState, scope);
      }

      if (displaceTarget != null) {
        EngineNumber finalInKg = engine.getStream(stream);
        BigDecimal changeInKg = finalInKg.getValue().subtract(currentInKg.getValue());
        displaceExecutor.execute(stream, amount, changeInKg, displaceTarget, displacementType);
      }
    } else {
      EngineNumber convertedMax = unitConverter.convert(amount, "kg");
      BigDecimal changeAmountRaw = convertedMax.getValue().subtract(currentValue.getValue());
      BigDecimal changeAmount = changeAmountRaw.min(BigDecimal.ZERO);

      boolean capSatisfied = changeAmount.compareTo(BigDecimal.ZERO) >= 0;
      if (capSatisfied) {
        return;
      }

      EngineNumber changeWithUnits = new EngineNumber(changeAmount, "kg");
      shortcuts.changeStreamWithoutReportingUnits(
          stream,
          changeWithUnits,
          Optional.empty(),
          Optional.empty()
      );

      // Update lastSpecified so subsequent percentage changes use the capped value
      EngineNumber cappedValue = engine.getStream(stream);
      simulationState.setLastSpecifiedValue(scope, stream, cappedValue);

      // For "sales" stream, also update lastSpecified for component streams (domestic/import)
      if ("sales".equals(stream)) {
        updateComponentStreamsLastSpecified(simulationState, scope);
      }

      displaceExecutor.execute(stream, amount, changeAmount, displaceTarget, displacementType);
    }
  }

  /**
   * Applies absolute value-based cap operation.
   *
   * <p>This method implements the cap logic for absolute value specifications (e.g.,
   * "cap to 1000 mt"). The cap is applied directly as a hard maximum constraint regardless of
   * previous user specifications or calculated values.</p>
   *
   * <p>Algorithm:</p>
   * <ol>
   *   <li>Convert current stream value to same units as cap amount</li>
   *   <li>If current exceeds cap: Apply StreamUpdate to set stream to cap value</li>
   *   <li>If displacement specified: Calculate change in kg and apply displacement</li>
   * </ol>
   *
   * @param stream The stream name to cap
   * @param amount The absolute cap amount (e.g., 1000 mt)
   * @param displaceTarget The target substance/stream for displacement, or null
   * @param displacementType The type of displacement (EQUIVALENT, BY_VOLUME, or BY_UNITS)
   */
  private void capWithValue(String stream, EngineNumber amount, String displaceTarget,
      DisplacementType displacementType) {
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine, stream);
    EngineNumber currentValueRaw = engine.getStream(stream);
    EngineNumber currentValueInAmountUnits = unitConverter.convert(
        currentValueRaw, amount.getUnits());

    boolean capSatisfied = currentValueInAmountUnits.getValue().compareTo(amount.getValue()) <= 0;
    if (capSatisfied) {
      return;
    }

    EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(amount)
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update);

    // Update lastSpecified so subsequent percentage changes use the capped value
    SimulationState simulationState = engine.getStreamKeeper();
    Scope scope = engine.getScope();
    EngineNumber cappedValue = engine.getStream(stream);
    simulationState.setLastSpecifiedValue(scope, stream, cappedValue);

    // For "sales" stream, also update lastSpecified for component streams (domestic/import)
    if ("sales".equals(stream)) {
      updateComponentStreamsLastSpecified(simulationState, scope);
    }

    if (displaceTarget != null) {
      EngineNumber cappedInKg = engine.getStream(stream);
      BigDecimal changeInKg = cappedInKg.getValue().subtract(currentInKg.getValue());
      displaceExecutor.execute(stream, amount, changeInKg, displaceTarget, displacementType);
    }
  }

  /**
   * Applies percentage-based floor operation using prior year's value for compounding effect.
   *
   * <p>This method implements the floor logic for percentage-based specifications. The
   * percentage is applied to the prior year's actual stream value rather than the current
   * calculated value. This approach enables proper year-over-year compounding where policy
   * requirements build on previous results.</p>
   *
   * <p>Algorithm:</p>
   * <ol>
   *   <li>If prior year value exists: Calculate floor as percentage of that value,
   *       apply StreamUpdate if current is below floor, handle displacement</li>
   *   <li>If no prior year exists: Treat percentage as volume (legacy behavior),
   *       apply increase if needed, handle displacement</li>
   * </ol>
   *
   * <p>Example: Stream was 1000 mt in year 4. In year 5, "floor domestic to 80%"
   * will floor to 800 mt (80% of prior year's 1000 mt).</p>
   *
   * @param stream The stream name to floor
   * @param amount The percentage floor amount (e.g., 80 for 80%)
   * @param displaceTarget The target substance/stream for displacement, or null
   * @param displacementType The type of displacement (EQUIVALENT, BY_VOLUME, or BY_UNITS)
   */
  private void floorWithPercent(String stream, EngineNumber amount, String displaceTarget,
      DisplacementType displacementType) {
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine, stream);
    EngineNumber currentValueRaw = engine.getStream(stream);
    EngineNumber currentValue = unitConverter.convert(currentValueRaw, "kg");

    SimulationState simulationState = engine.getStreamKeeper();
    Scope scope = engine.getScope();
    EngineNumber lastSpecified = simulationState.getStream(scope, stream, true);
    boolean hasPrior = lastSpecified != null;

    if (hasPrior) {
      EngineNumber percentPriorYear = new EngineNumber(amount.getValue(), "% prior year");
      EngineNumber newFloorValue = unitConverter.convert(percentPriorYear, lastSpecified.getUnits());

      EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
      EngineNumber newFloorInKg = unitConverter.convert(newFloorValue, "kg");

      boolean floorSatisfied = currentInKg.getValue().compareTo(newFloorInKg.getValue()) >= 0;
      if (floorSatisfied) {
        return;
      }

      StreamUpdate update = new StreamUpdateBuilder()
          .setName(stream)
          .setValue(newFloorValue)
          .setYearMatcher(Optional.empty())
          .inferSubtractRecycling()
          .build();
      engine.executeStreamUpdate(update);

      // Update lastSpecified so subsequent percentage changes use the floored value
      simulationState.setLastSpecifiedValue(scope, stream, newFloorValue);

      // For "sales" stream, also update lastSpecified for component streams (domestic/import)
      if ("sales".equals(stream)) {
        updateComponentStreamsLastSpecified(simulationState, scope);
      }

      if (displaceTarget != null) {
        EngineNumber finalInKg = engine.getStream(stream);
        BigDecimal changeInKg = finalInKg.getValue().subtract(currentInKg.getValue());
        displaceExecutor.execute(stream, amount, changeInKg, displaceTarget, displacementType);
      }
    } else {
      EngineNumber convertedMin = unitConverter.convert(amount, "kg");
      BigDecimal changeAmountRaw = convertedMin.getValue().subtract(currentValue.getValue());
      BigDecimal changeAmount = changeAmountRaw.max(BigDecimal.ZERO);

      boolean floorSatisfied = changeAmount.compareTo(BigDecimal.ZERO) <= 0;
      if (floorSatisfied) {
        return;
      }

      EngineNumber changeWithUnits = new EngineNumber(changeAmount, "kg");
      shortcuts.changeStreamWithoutReportingUnits(
          stream,
          changeWithUnits,
          Optional.empty(),
          Optional.empty()
      );

      // Update lastSpecified so subsequent percentage changes use the floored value
      EngineNumber flooredValue = engine.getStream(stream);
      simulationState.setLastSpecifiedValue(scope, stream, flooredValue);

      // For "sales" stream, also update lastSpecified for component streams (domestic/import)
      if ("sales".equals(stream)) {
        updateComponentStreamsLastSpecified(simulationState, scope);
      }

      displaceExecutor.execute(stream, amount, changeAmount, displaceTarget, displacementType);
    }
  }

  /**
   * Applies absolute value-based floor operation.
   *
   * <p>This method implements the floor logic for absolute value specifications (e.g.,
   * "floor to 800 mt"). The floor is applied directly as a hard minimum constraint regardless of
   * previous user specifications or calculated values.</p>
   *
   * <p>Algorithm:</p>
   * <ol>
   *   <li>Convert current stream value to same units as floor amount</li>
   *   <li>If current is below floor: Apply StreamUpdate to set stream to floor value</li>
   *   <li>If displacement specified: Calculate change in kg and apply displacement</li>
   * </ol>
   *
   * @param stream The stream name to floor
   * @param amount The absolute floor amount (e.g., 800 mt)
   * @param displaceTarget The target substance/stream for displacement, or null
   * @param displacementType The type of displacement (EQUIVALENT, BY_VOLUME, or BY_UNITS)
   */
  private void floorWithValue(String stream, EngineNumber amount, String displaceTarget,
      DisplacementType displacementType) {
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine, stream);
    EngineNumber currentValueRaw = engine.getStream(stream);
    EngineNumber currentValueInAmountUnits = unitConverter.convert(
        currentValueRaw, amount.getUnits());

    boolean floorSatisfied = currentValueInAmountUnits.getValue().compareTo(amount.getValue()) >= 0;
    if (floorSatisfied) {
      return;
    }

    EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(amount)
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update);

    // Update lastSpecified so subsequent percentage changes use the floored value
    SimulationState simulationState = engine.getStreamKeeper();
    Scope scope = engine.getScope();
    EngineNumber flooredValue = engine.getStream(stream);
    simulationState.setLastSpecifiedValue(scope, stream, flooredValue);

    // For "sales" stream, also update lastSpecified for component streams (domestic/import)
    if ("sales".equals(stream)) {
      updateComponentStreamsLastSpecified(simulationState, scope);
    }

    if (displaceTarget != null) {
      EngineNumber newInKg = engine.getStream(stream);
      BigDecimal changeInKg = newInKg.getValue().subtract(currentInKg.getValue());
      displaceExecutor.execute(stream, amount, changeInKg, displaceTarget, displacementType);
    }
  }

  /**
   * Updates lastSpecified for domestic and import streams based on their current values.
   *
   * <p>This is called when a "sales" stream cap/floor is applied, to ensure subsequent
   * percentage changes to component streams use the capped/floored values.</p>
   *
   * @param simulationState The simulation state to update
   * @param scope The current scope (UseKey) for the update
   */
  private void updateComponentStreamsLastSpecified(SimulationState simulationState, Scope scope) {
    EngineNumber domesticValue = engine.getStream("domestic");
    EngineNumber importValue = engine.getStream("import");
    simulationState.setLastSpecifiedValue(scope, "domestic", domesticValue);
    simulationState.setLastSpecifiedValue(scope, "import", importValue);
  }

}
