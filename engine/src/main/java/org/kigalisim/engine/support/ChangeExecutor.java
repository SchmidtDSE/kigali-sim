/**
 * Utility class for executing change operations on streams.
 *
 * <p>This class provides change operation logic for Engine implementations
 * to provide better separation of concerns and testability. It handles routing
 * of different change types (percentage, units, volume) to appropriate handlers.</p>
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
    if (!EngineSupportUtils.isInRange(config.getYearMatcher(), engine.getYear())) {
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
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());
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
   * <p>Apply percentage directly to lastSpecifiedValue and let setStream handle recharge.</p>
   *
   * @param config The configuration containing all parameters for the change operation
   */
  private void handlePercentageChange(ChangeExecutorConfig config) {
    String stream = config.getStream();
    EngineNumber amount = config.getAmount();
    YearMatcher yearMatcher = config.getYearMatcher();
    UseKey useKeyEffective = config.getUseKeyEffective();

    SimulationState streamKeeper = engine.getStreamKeeper();
    EngineNumber lastSpecified = streamKeeper.getLastSpecifiedValue(useKeyEffective, stream);
    if (lastSpecified == null) {
      return; // No base value, no change
    }

    // Apply percentage directly to lastSpecified value (user intent)
    BigDecimal percentageValue = amount.getValue();
    BigDecimal changeAmount = lastSpecified.getValue().multiply(percentageValue).divide(new BigDecimal("100"));
    BigDecimal newTotalValue = lastSpecified.getValue().add(changeAmount);
    EngineNumber newTotal = new EngineNumber(newTotalValue, lastSpecified.getUnits());

    // Let executeStreamUpdate handle unit conversion and recharge addition properly
    // This eliminates double counting - recharge calculated only in executeStreamUpdate
    // For units-based specifications, enable recycling logic
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
   * This avoids double recharge by letting each component stream handle its own recharge.
   *
   * @param config The configuration containing all parameters for the change operation
   */
  private void handleSalesChange(ChangeExecutorConfig config) {
    EngineNumber amount = config.getAmount();
    YearMatcher yearMatcher = config.getYearMatcher();
    UseKey useKeyEffective = config.getUseKeyEffective();

    // Get the distribution ratios for domestic and import
    SimulationState streamKeeper = engine.getStreamKeeper();
    SalesStreamDistribution distribution = streamKeeper.getDistribution(useKeyEffective);
    BigDecimal percentDomestic = distribution.getPercentDomestic();
    BigDecimal percentImport = distribution.getPercentImport();

    // Calculate proportional amounts for each component
    if (amount.getUnits() != null && amount.getUnits().contains("%")) {
      // Percentage changes: apply same percentage to both components
      // This ensures that 5% to sales means both domestic and import grow by 5%
      engine.changeStream("domestic", amount, yearMatcher, useKeyEffective);
      engine.changeStream("import", amount, yearMatcher, useKeyEffective);
    } else {
      // Units or kg changes: distribute proportionally
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
   * <p>Apply change to lastSpecifiedValue and let setStream handle recharge.</p>
   *
   * @param config The configuration containing all parameters for the change operation
   */
  private void handleUnitsChange(ChangeExecutorConfig config) {
    String stream = config.getStream();
    EngineNumber amount = config.getAmount();
    YearMatcher yearMatcher = config.getYearMatcher();
    UseKey useKeyEffective = config.getUseKeyEffective();

    SimulationState streamKeeper = engine.getStreamKeeper();
    EngineNumber lastSpecified = streamKeeper.getLastSpecifiedValue(useKeyEffective, stream);

    if (lastSpecified == null) {
      // Fallback: apply change to current stream value
      EngineNumber currentValue = engine.getStream(stream, Optional.of(useKeyEffective), Optional.empty());
      UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);
      EngineNumber currentInUnits = unitConverter.convert(currentValue, "units");
      BigDecimal newUnits = currentInUnits.getValue().add(amount.getValue());
      EngineNumber newTotal = new EngineNumber(newUnits, "units");
      // Units-based change should use recycling logic
      StreamUpdate update = new StreamUpdateBuilder()
          .setName(stream)
          .setValue(newTotal)
          .setYearMatcher(Optional.ofNullable(yearMatcher))
          .setSubtractRecycling(true)
          .build();
      engine.executeStreamUpdate(update);
    } else {
      // Apply units change to lastSpecified value
      BigDecimal newTotalValue = lastSpecified.getValue().add(amount.getValue());
      EngineNumber newTotal = new EngineNumber(newTotalValue, lastSpecified.getUnits());

      // Let executeStreamUpdate handle unit conversion and recharge addition properly
      // For units-based specifications, enable recycling logic
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
    YearMatcher yearMatcher = config.getYearMatcher();
    UseKey useKeyEffective = config.getUseKeyEffective();

    // Get current stream value and apply change, calling setStream with kg
    EngineNumber currentValue = engine.getStream(stream, Optional.of(useKeyEffective), Optional.empty());
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);
    EngineNumber convertedDelta = unitConverter.convert(amount, "kg");
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());
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
