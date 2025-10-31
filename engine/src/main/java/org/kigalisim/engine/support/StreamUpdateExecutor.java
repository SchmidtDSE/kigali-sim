/**
 * Utility class for executing stream update operations.
 *
 * <p>This class provides stream update logic for Engine implementations
 * to provide better separation of concerns and testability. It handles
 * stream combination tracking for unit-based carry-over operations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.RecalcKit;
import org.kigalisim.engine.recalc.RecalcOperation;
import org.kigalisim.engine.recalc.RecalcOperationBuilder;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.SimulationStateUpdate;
import org.kigalisim.engine.state.SimulationStateUpdateBuilder;
import org.kigalisim.engine.state.UseKey;

/**
 * Handles stream update operations for sales carry-over logic.
 *
 * <p>This class provides methods to handle sales stream combinations
 * when setting domestic or import streams with unit-based values.</p>
 */
public class StreamUpdateExecutor {

  private final Engine engine;

  /**
   * Creates a new StreamUpdateExecutor for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public StreamUpdateExecutor(Engine engine) {
    this.engine = engine;
  }

  /**
   * Handles stream combinations for unit preservation during carry-over operations.
   *
   * <p>When setting manufacture (domestic) or import streams with unit-based values,
   * this method combines them to create a unified sales intent. This ensures that
   * when both domestic and import are specified in units, the total sales value
   * correctly reflects the combined equipment count. This combination is critical
   * for maintaining accurate recharge calculations across carry-over years.</p>
   *
   * <p>Only processes unit-based specifications for domestic and import streams.
   * Volume-based (kg/mt) specifications are handled separately and do not require
   * combination logic.</p>
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream being set (must be "domestic" or "import")
   * @param value The value being set with equipment units
   */
  public void updateSalesCarryOver(UseKey useKey, String streamName, EngineNumber value) {
    // Only process unit-based values for combination tracking
    if (!value.hasEquipmentUnits()) {
      return;
    }

    // Only handle manufacture and import streams for combination
    if (!EngineSupportUtils.isSalesSubstream(streamName)) {
      return;
    }

    SimulationState simulationState = engine.getStreamKeeper();

    // When setting manufacture or import, combine with the other to create sales intent
    String otherStream = "domestic".equals(streamName) ? "import" : "domestic";
    EngineNumber otherValue = simulationState.getLastSpecifiedValue(useKey, otherStream);

    if (otherValue != null && otherValue.hasEquipmentUnits()) {
      // Both streams have unit-based values - combine them
      // Convert both to the same units (prefer the current stream's units)
      String targetUnits = value.getUnits();
      UnitConverter converter = EngineSupportUtils.createUnitConverterWithTotal(engine, streamName);
      EngineNumber otherConverted = converter.convert(otherValue, targetUnits);

      // Create combined sales value
      BigDecimal combinedValue = value.getValue().add(otherConverted.getValue());
      EngineNumber salesIntent = new EngineNumber(combinedValue, targetUnits);

      // Track the combined sales intent
      simulationState.setLastSpecifiedValue(useKey, "sales", salesIntent);
    } else {
      // Only one stream has units - use it as the sales intent
      simulationState.setLastSpecifiedValue(useKey, "sales", value);
    }
  }

  /**
   * Calculates and applies implicit recharge logic for sales streams with equipment units.
   *
   * <p>When setting sales streams (domestic, import, sales) with equipment units,
   * this method calculates the implicit recharge volume needed to service the
   * existing equipment population and adds it to the specified value. For
   * non-unit-based or non-sales streams, returns the original value unchanged.</p>
   *
   * <p>Implicit recharge ensures that when users specify equipment sales in units,
   * the simulation automatically accounts for the substance needed to recharge
   * (service) the existing equipment population, in addition to the initial charge
   * for the new equipment.</p>
   *
   * @param useKey The use key containing application and substance
   * @param streamName The name of the stream being set
   * @param value The original value specified by the user
   * @param isSales Whether this is a sales stream
   * @param isUnits Whether the value is in equipment units
   * @param isSalesSubstream Whether this is a sales substream (domestic/import)
   * @return ImplicitRechargeUpdate containing adjusted value and optional state update
   */
  public ImplicitRechargeUpdate updateAndApplyImplicitRecharge(
      UseKey useKey,
      String streamName,
      EngineNumber value,
      boolean isSales,
      boolean isUnits,
      boolean isSalesSubstream) {

    if (!isSales || !isUnits) {
      // Not a sales stream with units - check if we need to clear implicit recharge
      if (isSales) {
        // Sales stream without units - clear implicit recharge
        SimulationStateUpdate clearImplicitRechargeStream = new SimulationStateUpdateBuilder()
            .setUseKey(useKey)
            .setName("implicitRecharge")
            .setValue(new EngineNumber(BigDecimal.ZERO, "kg"))
            .setSubtractRecycling(false)
            .build();
        return new ImplicitRechargeUpdate(value, Optional.of(clearImplicitRechargeStream));
      } else {
        // Not a sales stream - no implicit recharge needed
        return new ImplicitRechargeUpdate(value, Optional.empty());
      }
    }

    // Calculate recharge volume for existing equipment
    SimulationState simulationState = engine.getStreamKeeper();
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, streamName);
    EngineNumber valueInKg = unitConverter.convert(value, "kg");
    EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
        useKey,
        engine.getStateGetter(),
        simulationState,
        engine
    );

    // Store recharge volume in implicitRecharge stream
    SimulationStateUpdate implicitRechargeStream = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("implicitRecharge")
        .setValue(rechargeVolume)
        .setSubtractRecycling(false)
        .build();

    // Calculate portion of recharge for this stream
    BigDecimal rechargeToAdd;
    if (isSalesSubstream) {
      rechargeToAdd = getDistributedRecharge(streamName, rechargeVolume, useKey);
    } else {
      rechargeToAdd = rechargeVolume.getValue();
    }

    // Add recharge to the original value
    BigDecimal totalWithRecharge = valueInKg.getValue().add(rechargeToAdd);
    EngineNumber valueToSet = new EngineNumber(totalWithRecharge, "kg");

    return new ImplicitRechargeUpdate(valueToSet, Optional.of(implicitRechargeStream));
  }

  /**
   * Gets the distributed portion of recharge for a specific sales stream.
   *
   * <p>When setting domestic or import streams, the total recharge volume needs to be
   * distributed between the streams based on their current distribution percentages.
   * For the sales stream itself, returns the full recharge volume.</p>
   *
   * @param streamName The stream name (domestic, import, or sales)
   * @param totalRecharge The total recharge volume to distribute
   * @param useKey The use key containing application and substance
   * @return The portion of recharge volume for this stream
   */
  private BigDecimal getDistributedRecharge(String streamName, EngineNumber totalRecharge,
                                           UseKey useKey) {
    if ("sales".equals(streamName)) {
      return totalRecharge.getValue();
    } else if (EngineSupportUtils.isSalesSubstream(streamName)) {
      SimulationState simulationState = engine.getStreamKeeper();
      SalesStreamDistribution distribution = simulationState.getDistribution(useKey);
      BigDecimal percentage;
      if ("domestic".equals(streamName)) {
        percentage = distribution.getPercentDomestic();
      } else if ("import".equals(streamName)) {
        percentage = distribution.getPercentImport();
      } else {
        throw new IllegalArgumentException("Unknown sales substream: " + streamName);
      }
      return totalRecharge.getValue().multiply(percentage);
    } else {
      throw new IllegalArgumentException("Stream is not a sales stream: " + streamName);
    }
  }

  /**
   * Propagates stream changes to dependent calculations based on stream type.
   *
   * <p>Different stream types require different recalculation operations:
   * <ul>
   *   <li>Sales streams (domestic, import, sales): recalc population change, propagate to consumption</li>
   *   <li>Consumption stream: recalc sales, propagate to population change</li>
   *   <li>Equipment stream: recalc sales, propagate to consumption</li>
   *   <li>PriorEquipment stream: recalc retirement only</li>
   *   <li>Other streams: no propagation</li>
   * </ul>
   *
   * @param useKey The use key containing application and substance
   * @param streamName The name of the stream that was updated
   * @param isSales Whether this is a sales stream
   * @param isUnits Whether the value was in equipment units
   */
  public void propagateChanges(
      UseKey useKey,
      String streamName,
      boolean isSales,
      boolean isUnits) {

    if (EngineSupportUtils.getIsSalesStream(streamName, false)) {
      boolean useImplicitRecharge = isSales && isUnits;
      propagateChangesFromSales(useKey, useImplicitRecharge);
    } else if ("consumption".equals(streamName)) {
      propagateChangesFromConsumption(useKey);
    } else if ("equipment".equals(streamName)) {
      propagateChangesFromEquipment(useKey);
    } else if ("priorEquipment".equals(streamName)) {
      propagateChangesFromPriorEquipment(useKey);
    }
    // Other streams require no propagation
  }

  /**
   * Propagates changes from sales streams (domestic, import, sales).
   * Recalculates population change and propagates to consumption.
   *
   * @param useKey The use key containing application and substance
   * @param useImplicitRecharge Whether to use implicit recharge (true if units were used)
   */
  private void propagateChangesFromSales(UseKey useKey, boolean useImplicitRecharge) {
    RecalcKit recalcKit = createRecalcKit();
    RecalcOperationBuilder builder = new RecalcOperationBuilder()
        .setScopeEffective(useKey)
        .setUseExplicitRecharge(!useImplicitRecharge)
        .setRecalcKit(recalcKit)
        .recalcPopulationChange()
        .thenPropagateToConsumption();

    if (!engine.getOptimizeRecalcs()) {
      builder = builder.thenPropagateToSales();
    }

    RecalcOperation operation = builder.build();
    operation.execute(engine);
  }

  /**
   * Propagates changes from consumption stream.
   * Recalculates sales and propagates to population change.
   *
   * @param useKey The use key containing application and substance
   */
  private void propagateChangesFromConsumption(UseKey useKey) {
    RecalcKit recalcKit = createRecalcKit();
    RecalcOperationBuilder builder = new RecalcOperationBuilder()
        .setScopeEffective(useKey)
        .setRecalcKit(recalcKit)
        .recalcSales()
        .thenPropagateToPopulationChange();

    if (!engine.getOptimizeRecalcs()) {
      builder = builder.thenPropagateToConsumption();
    }

    RecalcOperation operation = builder.build();
    operation.execute(engine);
  }

  /**
   * Propagates changes from equipment stream.
   * Recalculates sales and propagates to consumption.
   *
   * @param useKey The use key containing application and substance
   */
  private void propagateChangesFromEquipment(UseKey useKey) {
    RecalcKit recalcKit = createRecalcKit();
    RecalcOperationBuilder builder = new RecalcOperationBuilder()
        .setScopeEffective(useKey)
        .setRecalcKit(recalcKit)
        .recalcSales()
        .thenPropagateToConsumption();

    if (!engine.getOptimizeRecalcs()) {
      builder = builder.thenPropagateToPopulationChange();
    }

    RecalcOperation operation = builder.build();
    operation.execute(engine);
  }

  /**
   * Propagates changes from priorEquipment stream.
   * Recalculates retirement only.
   *
   * @param useKey The use key containing application and substance
   */
  private void propagateChangesFromPriorEquipment(UseKey useKey) {
    RecalcKit recalcKit = createRecalcKit();
    RecalcOperation operation = new RecalcOperationBuilder()
        .setScopeEffective(useKey)
        .setRecalcKit(recalcKit)
        .recalcRetire()
        .build();
    operation.execute(engine);
  }

  /**
   * Creates a RecalcKit with dependencies from the engine.
   *
   * @return A configured RecalcKit instance
   */
  private RecalcKit createRecalcKit() {
    return new RecalcKit(
        engine.getStreamKeeper(),
        engine.getUnitConverter(),
        engine.getStateGetter()
    );
  }
}
