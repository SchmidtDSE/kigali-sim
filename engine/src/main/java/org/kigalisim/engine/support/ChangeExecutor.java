/**
 * Utility class for executing change operations on streams.
 *
 * <p>This class extracts the change operation logic from SingleThreadEngine
 * to provide better separation of concerns and testability. It handles routing
 * of different change types (percentage, units, volume) to appropriate handlers.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.EngineSupportUtils;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Handles change operations for different types of streams.
 *
 * <p>This class provides methods to handle changes to component streams (domestic, import),
 * derived streams (equipment), sales streams, and different change types (percentage, units, volume).</p>
 */
public class ChangeExecutor {

  private final SingleThreadEngine engine;

  /**
   * Creates a new ChangeExecutor for the given engine.
   *
   * @param engine The SingleThreadEngine instance to operate on
   */
  public ChangeExecutor(SingleThreadEngine engine) {
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
    if (!EngineSupportUtils.isInRange(yearMatcher, engine.getYear())) {
      return;
    }

    if ("sales".equals(stream)) {
      handleSalesChange(stream, amount, yearMatcher, useKeyEffective);
    } else if (EngineSupportUtils.isSalesSubstream(stream) || "export".equals(stream)) {
      handleComponentStream(stream, amount, yearMatcher, useKeyEffective);
    } else {
      handleDerivedStream(stream, amount, yearMatcher, useKeyEffective);
    }
  }

  /**
   * Handle component stream changes (domestic, import, export).
   * Routes to specific handlers based on the units of the amount.
   *
   * @param stream The stream identifier to modify
   * @param amount The change amount (percentage, units, or kg)
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param useKeyEffective The effective UseKey for the operation
   */
  private void handleComponentStream(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKeyEffective) {
    if (amount.getUnits() != null && amount.getUnits().contains("%")) {
      handlePercentageChange(stream, amount, yearMatcher, useKeyEffective);
    } else if ("units".equals(amount.getUnits())) {
      handleUnitsChange(stream, amount, yearMatcher, useKeyEffective);
    } else {
      handleVolumeChange(stream, amount, yearMatcher, useKeyEffective);
    }
  }

  /**
   * Handle derived stream changes (equipment, priorEquipment, etc.).
   *
   * @param stream The stream identifier to modify
   * @param amount The change amount (percentage, units, or kg)
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param useKeyEffective The effective UseKey for the operation
   */
  private void handleDerivedStream(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKeyEffective) {
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
   * SIMPLIFIED VERSION: Apply percentage directly to lastSpecifiedValue and let setStream handle recharge.
   *
   * @param stream The stream identifier to modify
   * @param amount The percentage amount (e.g., "5%")
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param useKeyEffective The effective UseKey for the operation
   */
  private void handlePercentageChange(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKeyEffective) {
    StreamKeeper streamKeeper = engine.getStreamKeeper();
    EngineNumber lastSpecified = streamKeeper.getLastSpecifiedValue(useKeyEffective, stream);
    if (lastSpecified == null) {
      return; // No base value, no change
    }

    // Apply percentage directly to lastSpecified value (user intent)
    BigDecimal percentageValue = amount.getValue();
    BigDecimal changeAmount = lastSpecified.getValue().multiply(percentageValue).divide(new BigDecimal("100"));
    BigDecimal newTotalValue = lastSpecified.getValue().add(changeAmount);
    EngineNumber newTotal = new EngineNumber(newTotalValue, lastSpecified.getUnits());

    // Let setStream handle unit conversion and recharge addition properly
    // This eliminates double counting - recharge calculated only in setStream
    engine.setStream(stream, newTotal, Optional.ofNullable(yearMatcher));
  }

  /**
   * Handle sales stream changes by distributing proportionally to domestic and import.
   * This avoids double recharge by letting each component stream handle its own recharge.
   *
   * @param stream The stream identifier (should be "sales")
   * @param amount The change amount (percentage, units, or kg)
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param useKeyEffective The effective UseKey for the operation
   */
  private void handleSalesChange(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKeyEffective) {
    // Get the distribution ratios for domestic and import
    StreamKeeper streamKeeper = engine.getStreamKeeper();
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
   * SIMPLIFIED VERSION: Apply change to lastSpecifiedValue and let setStream handle recharge.
   *
   * @param stream The stream identifier to modify
   * @param amount The units amount (e.g., "25 units")
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param useKeyEffective The effective UseKey for the operation
   */
  private void handleUnitsChange(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKeyEffective) {
    StreamKeeper streamKeeper = engine.getStreamKeeper();
    EngineNumber lastSpecified = streamKeeper.getLastSpecifiedValue(useKeyEffective, stream);
    if (lastSpecified == null) {
      // Fallback: apply change to current stream value
      EngineNumber currentValue = engine.getStream(stream, Optional.of(useKeyEffective), Optional.empty());
      UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);
      EngineNumber currentInUnits = unitConverter.convert(currentValue, "units");
      BigDecimal newUnits = currentInUnits.getValue().add(amount.getValue());
      EngineNumber newTotal = new EngineNumber(newUnits, "units");
      engine.setStream(stream, newTotal, Optional.ofNullable(yearMatcher));
      return;
    }

    // Apply units change to lastSpecified value
    BigDecimal newTotalValue = lastSpecified.getValue().add(amount.getValue());
    EngineNumber newTotal = new EngineNumber(newTotalValue, lastSpecified.getUnits());

    // Let setStream handle unit conversion and recharge addition properly
    engine.setStream(stream, newTotal, Optional.ofNullable(yearMatcher));
  }

  /**
   * Handle volume-based change operations (kg, mt, etc.).
   * Uses existing logic but ensures lastSpecifiedValue is updated.
   *
   * @param stream The stream identifier to modify
   * @param amount The volume amount (e.g., "50 kg")
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param useKeyEffective The effective UseKey for the operation
   */
  private void handleVolumeChange(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKeyEffective) {
    // Get current stream value and apply change, calling setStream with kg
    EngineNumber currentValue = engine.getStream(stream, Optional.of(useKeyEffective), Optional.empty());
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);
    EngineNumber convertedDelta = unitConverter.convert(amount, "kg");
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());
    EngineNumber newTotal = new EngineNumber(newAmount, "kg");

    // Use setStream to handle the change and update lastSpecifiedValue
    engine.setStream(stream, newTotal, Optional.ofNullable(yearMatcher));
  }
}
