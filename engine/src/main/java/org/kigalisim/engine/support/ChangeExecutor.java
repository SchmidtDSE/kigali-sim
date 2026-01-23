/**
 * Utility class for executing change operations on streams.
 *
 * <p>This class provides change operation logic for Engine implementations
 * to provide better separation of concerns and testability. It handles routing of different change
 * types (percentage, units, volume) to appropriate handlers.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Handles change operations for different types of streams.
 *
 * <p>This class provides methods to handle changes to component streams (domestic, import),
 * derived streams (equipment), sales streams, and different change types (percentage, units, volume).</p>
 */
public class ChangeExecutor {

  private final Engine engine;

  /**
   * Creates a new ChangeExecutor for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public ChangeExecutor(Engine engine) {
    this.engine = engine;
  }

  /**
   * Clamps a change amount to prevent negative stream values.
   *
   * <p>This method ensures that applying the change will not result in a negative stream value.
   * If the proposed change would make the stream negative, it is clamped to bring the stream
   * to exactly zero instead.</p>
   *
   * @param currentValue The current stream value in kg
   * @param proposedChange The proposed change amount in kg (can be negative)
   * @return The clamped change amount in kg that will not result in negative values
   */
  private BigDecimal clampChangeToPreventNegative(EngineNumber currentValue,
      BigDecimal proposedChange) {
    BigDecimal currentKg = currentValue.getValue();
    BigDecimal newValue = currentKg.add(proposedChange);

    // If new value would be negative, clamp change to bring stream to exactly 0
    if (newValue.compareTo(BigDecimal.ZERO) < 0) {
      return currentKg.negate();  // Change of -currentKg brings stream to 0
    }

    return proposedChange;  // No clamping needed
  }

  /**
   * Execute a change operation by routing to the appropriate handler.
   *
   * @param stream The stream identifier to modify
   * @param amount The change amount (percentage, units, or kg)
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param useKeyEffective The effective UseKey for the operation
   */
  public void executeChange(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKeyEffective) {
    ChangeExecutorConfig config = new ChangeExecutorConfigBuilder()
        .setStream(stream)
        .setAmount(amount)
        .setYearMatcher(yearMatcher)
        .setUseKeyEffective(useKeyEffective)
        .build();
    executeChange(config);
  }

  /**
   * Execute a change operation using configuration object.
   *
   * @param config The configuration containing all parameters for the change operation
   */
  public void executeChange(ChangeExecutorConfig config) {
    boolean inRange = EngineSupportUtils.getIsInRange(
        config.getYearMatcher(),
        engine.getYear()
    );
    if (!inRange) {
      return;
    }

    String stream = config.getStream();
    if ("sales".equals(stream)) {
      handleSalesChange(config);
    } else if (EngineSupportUtils.isSalesSubstream(stream) || "export".equals(stream)) {
      handleComponentStream(config);
    } else {
      handleDerivedStream(config);
    }
  }

  /**
   * Handle component stream changes (domestic, import, export).
   * Routes to specific handlers based on the units of the amount.
   *
   * @param config The configuration containing all parameters for the change operation
   */
  private void handleComponentStream(ChangeExecutorConfig config) {
    EngineNumber amount = config.getAmount();
    if (amount.getUnits() != null && amount.getUnits().contains("%")) {
      handlePercentageChange(config);
    } else if ("units".equals(amount.getUnits())) {
      handleUnitsChange(config);
    } else {
      handleVolumeChange(config);
    }
  }

  /**
   * Handle derived stream changes (equipment, priorEquipment, etc.).
   *
   * @param config The configuration containing all parameters for the change operation
   */
  private void handleDerivedStream(ChangeExecutorConfig config) {
    String stream = config.getStream();
    EngineNumber amount = config.getAmount();
    UseKey useKeyEffective = config.getUseKeyEffective();

    // Use original changeStream logic to preserve displacement
    EngineNumber currentValue = engine.getStream(stream, Optional.of(useKeyEffective), Optional.empty());
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());

    // Clamp the change to prevent negative values
    BigDecimal clampedDelta = clampChangeToPreventNegative(currentValue, convertedDelta.getValue());
    BigDecimal newAmount = currentValue.getValue().add(clampedDelta);
    EngineNumber outputWithUnits = new EngineNumber(newAmount, currentValue.getUnits());

    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(outputWithUnits)
        .setKey(useKeyEffective)
        .setUnitsToRecord(amount.getUnits())
        .build();
    engine.executeStreamUpdate(update);
  }

  /**
   * Handle percentage-based change operations.
   *
   * <p>Applies percentage to the last specified value, with recharge handled by
   * executeStreamUpdate to avoid double counting. For units-based specifications, enables recycling
   * logic.</p>
   *
   * @param config The configuration containing all parameters for the change operation
   */
  private void handlePercentageChange(ChangeExecutorConfig config) {
    String stream = config.getStream();
    EngineNumber amount = config.getAmount();
    YearMatcher yearMatcher = config.getYearMatcher().orElse(null);
    UseKey useKeyEffective = config.getUseKeyEffective();

    SimulationState simulationState = engine.getStreamKeeper();
    EngineNumber lastSpecified = simulationState.getLastSpecifiedValue(useKeyEffective, stream);
    if (lastSpecified == null) {
      return;
    }

    String units = amount.getUnits();
    boolean isPriorYearBasis = units != null && units.contains("prioryear");

    if (isPriorYearBasis) {
      applyPriorYearPercentageChange(config, lastSpecified);
    } else {
      applyStandardPercentageChange(config, lastSpecified);
    }
  }

  /**
   * Apply percentage change based on prior year value.
   *
   * <p>Calculates the change amount from lastSpecified value but applies it to the current
   * stream value. This preserves the original lastSpecified value for future calculations.</p>
   *
   * @param config The configuration containing all parameters for the change operation
   * @param lastSpecified The last specified value for the stream
   */
  private void applyPriorYearPercentageChange(ChangeExecutorConfig config, EngineNumber lastSpecified) {
    String stream = config.getStream();
    EngineNumber amount = config.getAmount();
    YearMatcher yearMatcher = config.getYearMatcher().orElse(null);
    UseKey useKeyEffective = config.getUseKeyEffective();

    EngineNumber currentValue = engine.getStream(stream, Optional.of(useKeyEffective), Optional.empty());
    BigDecimal percentageValue = amount.getValue();
    BigDecimal changeAmount = lastSpecified.getValue().multiply(percentageValue).divide(new BigDecimal("100"));

    // Clamp to prevent negative values
    BigDecimal proposedNewValue = currentValue.getValue().add(changeAmount);
    BigDecimal clampedNewValue = proposedNewValue.compareTo(BigDecimal.ZERO) < 0
        ? BigDecimal.ZERO
        : proposedNewValue;

    EngineNumber newTotal = new EngineNumber(clampedNewValue, lastSpecified.getUnits());
    boolean subtractRecycling = "units".equals(lastSpecified.getUnits());
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(newTotal)
        .setYearMatcher(Optional.ofNullable(yearMatcher))
        .setSubtractRecycling(subtractRecycling)
        .build();
    engine.executeStreamUpdate(update);

    SimulationState simulationState = engine.getStreamKeeper();
    simulationState.setLastSpecifiedValue(useKeyEffective, stream, lastSpecified);
  }

  /**
   * Apply percentage change based on last specified value.
   *
   * <p>Calculates the change amount from lastSpecified value and applies it to that same value,
   * updating the stream with the new total. The updated value becomes the new lastSpecified.</p>
   *
   * @param config The configuration containing all parameters for the change operation
   * @param lastSpecified The last specified value for the stream
   */
  private void applyStandardPercentageChange(ChangeExecutorConfig config, EngineNumber lastSpecified) {
    String stream = config.getStream();
    EngineNumber amount = config.getAmount();
    YearMatcher yearMatcher = config.getYearMatcher().orElse(null);

    BigDecimal percentageValue = amount.getValue();
    BigDecimal changeAmount = lastSpecified.getValue().multiply(percentageValue).divide(new BigDecimal("100"));

    // Clamp to prevent negative values
    BigDecimal proposedNewValue = lastSpecified.getValue().add(changeAmount);
    BigDecimal clampedNewValue = proposedNewValue.compareTo(BigDecimal.ZERO) < 0
        ? BigDecimal.ZERO
        : proposedNewValue;

    EngineNumber newTotal = new EngineNumber(clampedNewValue, lastSpecified.getUnits());
    boolean subtractRecycling = "units".equals(lastSpecified.getUnits());
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(newTotal)
        .setYearMatcher(Optional.ofNullable(yearMatcher))
        .setSubtractRecycling(subtractRecycling)
        .build();
    engine.executeStreamUpdate(update);
  }

  /**
   * Handle sales stream changes by distributing proportionally to domestic and import.
   *
   * <p>Percentage changes apply the same percentage to both domestic and import.
   * Units or kg changes are distributed proportionally based on current ratios.
   * Each component stream handles its own recharge to avoid double counting.</p>
   *
   * @param config The configuration containing all parameters for the change operation
   */
  private void handleSalesChange(ChangeExecutorConfig config) {
    EngineNumber amount = config.getAmount();
    YearMatcher yearMatcher = config.getYearMatcher().orElse(null);
    UseKey useKeyEffective = config.getUseKeyEffective();

    boolean inRange = EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear());
    if (!inRange) {
      return;
    }

    SimulationState simulationState = engine.getStreamKeeper();
    SalesStreamDistribution distribution = simulationState.getDistribution(useKeyEffective);
    BigDecimal percentDomestic = distribution.getPercentDomestic();
    BigDecimal percentImport = distribution.getPercentImport();

    if (amount.getUnits() != null && amount.getUnits().contains("%")) {
      engine.changeStream("domestic", amount, yearMatcher, useKeyEffective);
      engine.changeStream("import", amount, yearMatcher, useKeyEffective);
    } else {
      BigDecimal domesticAmount = amount.getValue().multiply(percentDomestic);
      BigDecimal importAmount = amount.getValue().multiply(percentImport);

      EngineNumber domesticChange = new EngineNumber(domesticAmount, amount.getUnits());
      EngineNumber importChange = new EngineNumber(importAmount, amount.getUnits());

      engine.changeStream("domestic", domesticChange, yearMatcher, useKeyEffective);
      engine.changeStream("import", importChange, yearMatcher, useKeyEffective);
    }
  }

  /**
   * Handle units-based change operations.
   *
   * <p>Applies change to the last specified value, or falls back to current stream
   * value if no specification exists. Recharge is handled by executeStreamUpdate, with recycling
   * logic enabled for units-based specifications.</p>
   *
   * @param config The configuration containing all parameters for the change operation
   */
  private void handleUnitsChange(ChangeExecutorConfig config) {
    String stream = config.getStream();
    EngineNumber amount = config.getAmount();
    YearMatcher yearMatcher = config.getYearMatcher().orElse(null);
    UseKey useKeyEffective = config.getUseKeyEffective();

    boolean inRange = EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear());
    if (!inRange) {
      return;
    }

    SimulationState simulationState = engine.getStreamKeeper();
    EngineNumber lastSpecified = simulationState.getLastSpecifiedValue(useKeyEffective, stream);

    if (lastSpecified == null) {
      EngineNumber currentValue = engine.getStream(stream, Optional.of(useKeyEffective), Optional.empty());
      UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);
      EngineNumber currentInUnits = unitConverter.convert(currentValue, "units");

      // Clamp the units change to prevent negative values
      BigDecimal proposedNewUnits = currentInUnits.getValue().add(amount.getValue());
      BigDecimal clampedNewUnits = proposedNewUnits.compareTo(BigDecimal.ZERO) < 0
          ? BigDecimal.ZERO
          : proposedNewUnits;

      EngineNumber newTotal = new EngineNumber(clampedNewUnits, "units");
      StreamUpdate update = new StreamUpdateBuilder()
          .setName(stream)
          .setValue(newTotal)
          .setYearMatcher(Optional.ofNullable(yearMatcher))
          .setSubtractRecycling(true)
          .build();
      engine.executeStreamUpdate(update);
    } else {
      // Clamp the units change to prevent negative values
      BigDecimal proposedNewUnits = lastSpecified.getValue().add(amount.getValue());
      BigDecimal clampedNewUnits = proposedNewUnits.compareTo(BigDecimal.ZERO) < 0
          ? BigDecimal.ZERO
          : proposedNewUnits;

      EngineNumber newTotal = new EngineNumber(clampedNewUnits, lastSpecified.getUnits());

      boolean subtractRecycling = "units".equals(lastSpecified.getUnits());
      StreamUpdate update = new StreamUpdateBuilder()
          .setName(stream)
          .setValue(newTotal)
          .setYearMatcher(Optional.ofNullable(yearMatcher))
          .setSubtractRecycling(subtractRecycling)
          .build();
      engine.executeStreamUpdate(update);
    }
  }

  /**
   * Handle volume-based change operations (kg, mt, etc.).
   * Uses existing logic but ensures lastSpecifiedValue is updated.
   *
   * @param config The configuration containing all parameters for the change operation
   */
  private void handleVolumeChange(ChangeExecutorConfig config) {
    String stream = config.getStream();
    EngineNumber amount = config.getAmount();
    YearMatcher yearMatcher = config.getYearMatcher().orElse(null);
    UseKey useKeyEffective = config.getUseKeyEffective();

    // Get current stream value and apply change, calling setStream with kg
    EngineNumber currentValue = engine.getStream(stream, Optional.of(useKeyEffective), Optional.empty());
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);
    EngineNumber convertedDelta = unitConverter.convert(amount, "kg");

    // Clamp the change to prevent negative values
    BigDecimal clampedDelta = clampChangeToPreventNegative(currentValue, convertedDelta.getValue());
    BigDecimal newAmount = currentValue.getValue().add(clampedDelta);
    EngineNumber newTotal = new EngineNumber(newAmount, "kg");

    // Use executeStreamUpdate to handle the change and update lastSpecifiedValue
    // Volume-based changes use the default recycling behavior
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(newTotal)
        .setYearMatcher(Optional.ofNullable(yearMatcher))
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update);
  }
}
