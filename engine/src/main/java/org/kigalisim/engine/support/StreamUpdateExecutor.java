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
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.SimulationStateUpdate;
import org.kigalisim.engine.state.SimulationStateUpdateBuilder;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

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
   * Executes a complete stream update operation with all side effects.
   *
   * <p>This method consolidates all stream update logic including implicit recharge,
   * state updates, units target tracking, and change propagation into a single call.</p>
   *
   * @param update The StreamUpdate containing all update parameters
   */
  public void execute(StreamUpdate update) {
    final String name = update.getName();
    final EngineNumber value = update.getValue();
    final Optional<YearMatcher> yearMatcher = update.getYearMatcher();
    final Optional<UseKey> key = update.getKey();
    final boolean propagateChanges = update.getPropagateChanges();
    final boolean subtractRecycling = update.getSubtractRecycling();

    boolean isInRange = EngineSupportUtils.isInRange(
        yearMatcher.orElse(null),
        engine.getStreamKeeper().getCurrentYear()
    );
    if (!isInRange) {
      return;
    }

    UseKey keyEffective = key.orElse(engine.getScope());

    // Calculate stream type flags
    boolean isSales = EngineSupportUtils.getIsSalesStream(name, true);
    boolean isUnits = value.hasEquipmentUnits();
    boolean isSalesSubstream = EngineSupportUtils.isSalesSubstream(name);

    ImplicitRechargeUpdate rechargeUpdate = handleImplicitRecharge(
        keyEffective,
        name,
        value,
        isSales,
        isUnits,
        isSalesSubstream
    );

    EngineNumber valueToSet = rechargeUpdate.getValueToSet();

    rechargeUpdate.getImplicitRechargeStateUpdate()
        .ifPresent(engine.getStreamKeeper()::update);

    SimulationStateUpdate simulationStateUpdate = new SimulationStateUpdateBuilder()
        .setUseKey(keyEffective)
        .setName(name)
        .setValue(valueToSet)
        .setSubtractRecycling(subtractRecycling)
        .setDistribution(update.getDistribution().orElse(null))
        .build();
    engine.getStreamKeeper().update(simulationStateUpdate);

    if (!propagateChanges) {
      return;
    }

    // Update last specified value and units target for sales streams
    if (isSales) {
      engine.getStreamKeeper().setLastSpecifiedValue(keyEffective, name, value);
      updateUnitsTarget(keyEffective, name, value);
    }

    propagateChanges(keyEffective, name, isSales, isUnits);
  }

  /**
   * Gets the other stream value (domestic or import).
   *
   * <p>When setting one of the sales substreams (domestic or import), this retrieves
   * the last specified value for the complementary stream.</p>
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream being set (must be "domestic" or "import")
   * @return The last specified value for the other stream, or null if not set
   */
  private EngineNumber getOtherValue(UseKey useKey, String streamName) {
    String otherStream = "domestic".equals(streamName) ? "import" : "domestic";
    return engine.getStreamKeeper().getLastSpecifiedValue(useKey, otherStream);
  }

  /**
   * Updates units target when setting domestic or import streams with equipment units.
   *
   * <p>Some calculations require knowing what the last value was for a stream as specified
   * by the user to determine intent like setting a specific number of new units of
   * equipment sold on top of recharge or a total volume (kg) of substance sold either for
   * servicing or new equipment initial charge. For users specifying specific number of units
   * of equipment sold, sales is a combination of import and domestic production and this
   * method updates that sales intent regardless of if sales itself was specified or its
   * components (domestic or import) were specified.</p>
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream being set (must be "domestic" or "import")
   * @param value The value being set with equipment units
   */
  private void updateUnitsTarget(UseKey useKey, String streamName, EngineNumber value) {
    boolean hasEquipmentUnitsTarget = value.hasEquipmentUnits();
    boolean userSetSales = EngineSupportUtils.isSalesSubstream(streamName);
    boolean hasUnitsTarget = hasEquipmentUnitsTarget && userSetSales;

    if (!hasUnitsTarget) {
      return;
    }

    SimulationState simulationState = engine.getStreamKeeper();

    // When setting manufacture or import, combine with the other to create sales intent
    EngineNumber otherValue = getOtherValue(useKey, streamName);

    boolean otherGiven = otherValue != null;
    boolean otherHasUnitsTarget = otherGiven && otherValue.hasEquipmentUnits();
    if (otherHasUnitsTarget) {
      // Convert both to the same units (prefer the current stream's units)
      String targetUnits = value.getUnits();
      UnitConverter converter = EngineSupportUtils.createUnitConverterWithTotal(engine, streamName);
      EngineNumber otherConverted = converter.convert(otherValue, targetUnits);

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
   * Handles implicit recharge logic for sales streams with equipment units.
   *
   * <p>When users have specified how much equipment is to be sold (not total volume of
   * substance sales), this method determines the implicit recharge needed. In this case,
   * servicing is implied to have enough substance to succeed when the recharge command
   * runs. For non-unit-based or non-sales streams, clears or skips implicit recharge
   * logic as needed.</p>
   *
   * @param useKey The use key containing application and substance
   * @param streamName The name of the stream being set
   * @param value The original value specified by the user
   * @param isSales Whether this is a sales stream
   * @param isUnits Whether the value is in equipment units
   * @param isSalesSubstream Whether this is a sales substream (domestic/import)
   * @return ImplicitRechargeUpdate containing adjusted value and optional state update
   */
  public ImplicitRechargeUpdate handleImplicitRecharge(UseKey useKey, String streamName,
      EngineNumber value, boolean isSales, boolean isUnits, boolean isSalesSubstream) {

    boolean isSalesWithUnitsTarget = isSales && isUnits;

    if (isSalesWithUnitsTarget) {
      return updateAndApplyImplicitRecharge(useKey, streamName, value, isSalesSubstream);
    } else {
      return clearImplicitRecharge(useKey, isSales, value);
    }
  }

  /**
   * Calculates and applies implicit recharge volume for sales with units target.
   *
   * <p>This internal method performs the actual recharge calculation when a user has
   * specified equipment units for sales.</p>
   *
   * @param useKey The use key containing application and substance
   * @param streamName The name of the stream being set
   * @param value The original value specified by the user
   * @param isSalesSubstream Whether this is a sales substream (domestic/import)
   * @return ImplicitRechargeUpdate containing adjusted value and state update
   */
  private ImplicitRechargeUpdate updateAndApplyImplicitRecharge(UseKey useKey,
      String streamName,
      EngineNumber value,
      boolean isSalesSubstream) {

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
   * Clears implicit recharge when not using units target.
   *
   * <p>When a sales stream is set without units (volume-based), the implicit recharge
   * needs to be cleared. For non-sales streams, no update is needed.</p>
   *
   * @param useKey The use key containing application and substance
   * @param isSales Whether this is a sales stream
   * @param value The value to return
   * @return ImplicitRechargeUpdate with clear operation if needed
   */
  private ImplicitRechargeUpdate clearImplicitRecharge(UseKey useKey, boolean isSales,
      EngineNumber value) {

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

  /**
   * Gets the distributed recharge portion for a sales substream.
   *
   * <p>Distributes the total recharge volume between domestic and import based on
   * their current distribution percentages.</p>
   *
   * @param streamName The stream name (domestic or import)
   * @param totalRecharge The total recharge volume to distribute
   * @param useKey The use key containing application and substance
   * @return The portion of recharge volume for this substream
   */
  private BigDecimal getDistributedSalesRecharge(String streamName, EngineNumber totalRecharge,
      UseKey useKey) {
    SimulationState simulationState = engine.getStreamKeeper();
    SalesStreamDistribution distribution = simulationState.getDistribution(useKey);
    BigDecimal percentage = switch (streamName) {
      case "domestic" -> distribution.getPercentDomestic();
      case "import" -> distribution.getPercentImport();
      default -> throw new IllegalArgumentException("Unknown sales substream: " + streamName);
    };
    return totalRecharge.getValue().multiply(percentage);
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
      return getDistributedSalesRecharge(streamName, totalRecharge, useKey);
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
  public void propagateChanges(UseKey useKey, String streamName, boolean isSales,
      boolean isUnits) {

    if (EngineSupportUtils.getIsSalesStream(streamName, false)) {
      boolean useImplicitRecharge = isSales && isUnits;
      propagateChangesFromSales(useKey, useImplicitRecharge);
      return;
    }

    switch (streamName) {
      case "consumption" -> propagateChangesFromConsumption(useKey);
      case "equipment" -> propagateChangesFromEquipment(useKey);
      case "priorEquipment" -> propagateChangesFromPriorEquipment(useKey);
      default -> {
        // Other streams require no propagation
      }
    }
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
