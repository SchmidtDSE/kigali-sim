/**
 * Fluent builder for {@link ImplicitRechargeUpdate} instances.
 *
 * <p>This builder takes over the implicit recharge and precharge calculation logic previously
 * found in {@link StreamUpdateExecutor}. It produces updates that adjust a stream value by the
 * implicit servicing (recharge and, when configured, precharge) required for unit-based sales
 * specifications, as well as clear operations that zero out the implicit streams for
 * volume-based sales.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.SimulationStateUpdate;
import org.kigalisim.engine.state.SimulationStateUpdateBuilder;
import org.kigalisim.engine.state.StateGetter;
import org.kigalisim.engine.state.UseKey;

/**
 * Fluent builder for implicit recharge and precharge calculations.
 *
 * <p>Configure the builder with the stream context (use key, stream name, value, flags) and the
 * supporting state access (simulation state, unit converter, base state getter, and the
 * precomputed recharge volume), then call {@link #buildUpdate} for unit-based sales updates or
 * {@link #buildClear} to clear the implicit streams for volume-based sales.</p>
 */
public final class ImplicitRechargeUpdateBuilder {

  private UseKey useKey;
  private String streamName;
  private EngineNumber value;
  private boolean isSalesSubstream;
  private boolean isSales;
  private SimulationState simulationState;
  private UnitConverter unitConverter;
  private StateGetter stateGetter;
  private EngineNumber rechargeVolume;

  /**
   * Sets the use key containing application and substance.
   *
   * @param useKey The use key for the operation
   * @return This builder for fluent chaining
   */
  public ImplicitRechargeUpdateBuilder setUseKey(UseKey useKey) {
    this.useKey = useKey;
    return this;
  }

  /**
   * Sets the name of the stream being set.
   *
   * @param streamName The stream name being updated
   * @return This builder for fluent chaining
   */
  public ImplicitRechargeUpdateBuilder setStreamName(String streamName) {
    this.streamName = streamName;
    return this;
  }

  /**
   * Sets the original value specified by the user.
   *
   * @param value The value being set for the stream
   * @return This builder for fluent chaining
   */
  public ImplicitRechargeUpdateBuilder setValue(EngineNumber value) {
    this.value = value;
    return this;
  }

  /**
   * Sets whether this is a sales substream (domestic or import).
   *
   * @param isSalesSubstream True if the stream is a sales substream
   * @return This builder for fluent chaining
   */
  public ImplicitRechargeUpdateBuilder setIsSalesSubstream(boolean isSalesSubstream) {
    this.isSalesSubstream = isSalesSubstream;
    return this;
  }

  /**
   * Sets whether this is a sales stream.
   *
   * @param isSales True if the stream is a sales stream
   * @return This builder for fluent chaining
   */
  public ImplicitRechargeUpdateBuilder setIsSales(boolean isSales) {
    this.isSales = isSales;
    return this;
  }

  /**
   * Sets the simulation state used to query recharge and precharge configuration.
   *
   * @param simulationState The simulation state (stream keeper) to query
   * @return This builder for fluent chaining
   */
  public ImplicitRechargeUpdateBuilder setSimulationState(SimulationState simulationState) {
    this.simulationState = simulationState;
    return this;
  }

  /**
   * Sets the unit converter used to convert the user-specified value.
   *
   * @param unitConverter The unit converter for value conversion
   * @return This builder for fluent chaining
   */
  public ImplicitRechargeUpdateBuilder setUnitConverter(UnitConverter unitConverter) {
    this.unitConverter = unitConverter;
    return this;
  }

  /**
   * Sets the base state getter used when constructing the precharge converter.
   *
   * @param stateGetter The base state getter for precharge conversions
   * @return This builder for fluent chaining
   */
  public ImplicitRechargeUpdateBuilder setStateGetter(StateGetter stateGetter) {
    this.stateGetter = stateGetter;
    return this;
  }

  /**
   * Sets the precomputed recharge volume to apply.
   *
   * @param rechargeVolume The recharge volume in kg, as computed by
   *     {@link RechargeVolumeCalculator}
   * @return This builder for fluent chaining
   */
  public ImplicitRechargeUpdateBuilder setRechargeVolume(EngineNumber rechargeVolume) {
    this.rechargeVolume = rechargeVolume;
    return this;
  }

  /**
   * Builds the implicit servicing update for a unit-based sales stream.
   *
   * <p>This combines the recharge portion (always applied) and, when the user has configured a
   * non-zero precharge population, the precharge portion into a single
   * {@link ImplicitRechargeUpdate} that adjusts the stream value and records the implicit
   * servicing volumes.</p>
   *
   * @return An ImplicitRechargeUpdate with the adjusted value and state updates
   */
  public ImplicitRechargeUpdate buildUpdate() {
    EngineNumber valueInKg = unitConverter.convert(value, "kg");

    ServicingUpdateComponent rechargeComponent = getForRecharge(rechargeVolume);

    EngineNumber prechargePopRaw = simulationState.getPrechargePopulation(useKey);
    boolean hasPrecharge = getHasPrecharge(prechargePopRaw);

    Optional<SimulationStateUpdate> prechargeUpdate;
    BigDecimal prechargeToAdd;
    if (hasPrecharge) {
      ServicingUpdateComponent prechargeComponent = getForPrecharge(prechargePopRaw);
      prechargeUpdate = prechargeComponent.getUpdate();
      prechargeToAdd = prechargeComponent.getValue();
    } else {
      prechargeUpdate = Optional.empty();
      prechargeToAdd = BigDecimal.ZERO;
    }

    BigDecimal totalWithRecharge = valueInKg.getValue()
        .add(rechargeComponent.getValue())
        .add(prechargeToAdd);
    EngineNumber valueToSet = new EngineNumber(totalWithRecharge, "kg");

    return new ImplicitRechargeUpdate(
        valueToSet,
        rechargeComponent.getUpdate(),
        prechargeUpdate
    );
  }

  /**
   * Builds a clear operation for a volume-based sales stream.
   *
   * <p>For sales streams set without equipment units, the implicit recharge and precharge
   * streams are zeroed out. For non-sales streams, no implicit servicing is needed and an empty
   * update is returned.</p>
   *
   * @return An ImplicitRechargeUpdate clearing the implicit streams if needed
   */
  public ImplicitRechargeUpdate buildClear() {
    if (isSales) {
      SimulationStateUpdate clearImplicitRechargeStream = new SimulationStateUpdateBuilder()
          .setUseKey(useKey)
          .setName("implicitRecharge")
          .setValue(new EngineNumber(BigDecimal.ZERO, "kg"))
          .setSubtractRecycling(false)
          .build();
      SimulationStateUpdate clearImplicitPrechargeStream = new SimulationStateUpdateBuilder()
          .setUseKey(useKey)
          .setName("implicitPrecharge")
          .setValue(new EngineNumber(BigDecimal.ZERO, "kg"))
          .setSubtractRecycling(false)
          .build();
      return new ImplicitRechargeUpdate(
          value,
          Optional.of(clearImplicitRechargeStream),
          Optional.of(clearImplicitPrechargeStream)
      );
    } else {
      return new ImplicitRechargeUpdate(value, Optional.empty());
    }
  }

  /**
   * Determines whether a precharge population has been configured and is non-zero.
   *
   * @param prechargePopRaw The raw precharge population value, or null if unspecified
   * @return True if a non-zero precharge population is configured
   */
  private boolean getHasPrecharge(EngineNumber prechargePopRaw) {
    if (prechargePopRaw == null) {
      return false;
    }
    if (prechargePopRaw.getValue().compareTo(BigDecimal.ZERO) == 0) {
      return false;
    }
    return true;
  }

  /**
   * Builds the recharge servicing component for existing equipment.
   *
   * <p>This records the recharge volume in the implicitRecharge stream and determines the
   * portion of that volume to add to the stream being set, distributing across substreams when
   * appropriate.</p>
   *
   * @param rechargeVolume The total recharge volume in kg
   * @return A ServicingUpdateComponent with the amount to add and the implicitRecharge update
   */
  private ServicingUpdateComponent getForRecharge(EngineNumber rechargeVolume) {
    SimulationStateUpdate implicitRechargeStream = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("implicitRecharge")
        .setValue(rechargeVolume)
        .setSubtractRecycling(false)
        .build();

    BigDecimal rechargeToAdd;
    if (isSalesSubstream) {
      rechargeToAdd = EngineSupportUtils.getDistributedRecharge(
          streamName,
          rechargeVolume,
          useKey,
          simulationState
      );
    } else {
      rechargeToAdd = rechargeVolume.getValue();
    }

    return new ServicingUpdateComponent(rechargeToAdd, Optional.of(implicitRechargeStream));
  }

  /**
   * Builds the precharge servicing component for new equipment.
   *
   * <p>This computes the precharge volume from the user-specified units (treating the value as
   * the new equipment population), records it in the implicitPrecharge stream, and determines the
   * portion to add to the stream being set, distributing across substreams when appropriate.</p>
   *
   * @param prechargePopRaw The raw precharge population value, known to be non-null and non-zero
   * @return A ServicingUpdateComponent with the amount to add and the implicitPrecharge update
   */
  private ServicingUpdateComponent getForPrecharge(EngineNumber prechargePopRaw) {
    EngineNumber valueInUnits = unitConverter.convert(value, "units");

    OverridingConverterStateGetter prechargeStateGetter =
        new OverridingConverterStateGetter(stateGetter);
    UnitConverter prechargeConverter = new UnitConverter(prechargeStateGetter);

    prechargeStateGetter.setPopulation(valueInUnits);
    EngineNumber prechargePop = prechargeConverter.convert(prechargePopRaw, "units");
    prechargeStateGetter.setPopulation(prechargePop);

    EngineNumber prechargeIntensityRaw = simulationState.getPrechargeIntensity(useKey);
    EngineNumber prechargeVolume = prechargeConverter.convert(prechargeIntensityRaw, "kg");
    prechargeStateGetter.clearPopulation();

    SimulationStateUpdate implicitPrechargeStream = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("implicitPrecharge")
        .setValue(prechargeVolume)
        .setSubtractRecycling(false)
        .build();

    BigDecimal prechargeToAdd;
    if (isSalesSubstream) {
      prechargeToAdd = EngineSupportUtils.getDistributedRecharge(
          streamName,
          prechargeVolume,
          useKey,
          simulationState
      );
    } else {
      prechargeToAdd = prechargeVolume.getValue();
    }

    return new ServicingUpdateComponent(prechargeToAdd, Optional.of(implicitPrechargeStream));
  }
}
