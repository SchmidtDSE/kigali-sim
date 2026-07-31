/**
 * Immutable snapshot implementation of the state for a single scenario within a single trial.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.recalc.SalesStreamDistributionBuilder;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Immutable snapshot of {@link SimulationState}.
 *
 * <p>Holds only {@code final} fields populated at construction (each substance's
 * {@link StreamParameterization} already frozen, and {@code priorState} captured by
 * reference since the prior chain is already frozen) and throws
 * {@link UnsupportedOperationException} from every mutator, so a snapshot captured
 * for a prior-year lookup can never be silently corrupted.</p>
 */
public class FrozenSimulationState implements SimulationState {

  private static final String FROZEN_MESSAGE =
      "Cannot mutate a frozen SimulationState snapshot.";

  private final Map<String, StreamParameterization> substances;
  private final Map<String, EngineNumber> streams;
  private final Optional<SimulationState> priorState;
  private final OverridingConverterStateGetter stateGetter;
  private final UnitConverter unitConverter;
  private final int currentYear;

  /**
   * Create a new immutable snapshot with the specified field values.
   *
   * @param substances The frozen per-substance parameterizations, keyed by application/substance
   * @param streams The stream values at the time of the snapshot
   * @param priorState The (already frozen) prior year's state, if any
   * @param stateGetter Structure to retrieve state information, shared with the live state
   * @param unitConverter Converter for handling unit transformations, shared with the live state
   * @param currentYear The year this snapshot was captured at
   */
  public FrozenSimulationState(Map<String, StreamParameterization> substances,
      Map<String, EngineNumber> streams, Optional<SimulationState> priorState,
      OverridingConverterStateGetter stateGetter, UnitConverter unitConverter, int currentYear) {
    this.substances = substances;
    this.streams = streams;
    this.priorState = priorState;
    this.stateGetter = stateGetter;
    this.unitConverter = unitConverter;
    this.currentYear = currentYear;
  }

  /** {@inheritDoc} */
  @Override
  public List<SubstanceInApplicationId> getRegisteredSubstances() {
    return substances.keySet().stream()
        .map(key -> {
          String[] keyComponents = key.split("\t");
          boolean correctKeyLength = keyComponents.length == 2;
          if (!correctKeyLength) {
            return null;
          }

          boolean firstKeyOk = keyComponents[0] != null && !keyComponents[0].trim().isEmpty();
          boolean secondKeyOk = keyComponents[1] != null && !keyComponents[1].trim().isEmpty();
          boolean bothKeysOk = firstKeyOk && secondKeyOk;
          if (!bothKeysOk) {
            return null;
          }

          return new SubstanceInApplicationId(keyComponents[0], keyComponents[1]);
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasSubstance(UseKey useKey) {
    return substances.containsKey(SimulationStateSupport.getKey(useKey));
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void ensureSubstance(UseKey useKey) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void update(SimulationStateUpdate stateUpdate) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getStream(UseKey useKey, String name) {
    return getStream(useKey, name, false);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getStream(UseKey useKey, String name, boolean priorYear) {
    return SimulationStateSupport.getStream(
        this, streams, priorState, unitConverter, useKey, name, priorYear);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isKnownStream(UseKey useKey, String name) {
    return streams.containsKey(SimulationStateSupport.getKey(useKey, name));
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionStream(UseKey useKey, RecoveryStage stage) {
    return getInductionStream(useKey, stage, false);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionStream(UseKey useKey, RecoveryStage stage, boolean priorYear) {
    String streamName = SimulationStateSupport.getInductionStreamName(stage);
    return getStream(useKey, streamName, priorYear);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getTotalInductionStream(UseKey useKey) {
    return getTotalInductionStream(useKey, false);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getTotalInductionStream(UseKey useKey, boolean priorYear) {
    EngineNumber inductionEol = getInductionStream(useKey, RecoveryStage.EOL, priorYear);
    EngineNumber inductionRecharge = getInductionStream(useKey, RecoveryStage.RECHARGE, priorYear);

    EngineNumber eolConverted = unitConverter.convert(inductionEol, "kg");
    EngineNumber rechargeConverted = unitConverter.convert(inductionRecharge, "kg");

    BigDecimal total = eolConverted.getValue().add(rechargeConverted.getValue());
    return new EngineNumber(total, "kg");
  }

  /** {@inheritDoc} */
  @Override
  public SalesStreamDistribution getDistribution(UseKey useKey) {
    return getDistribution(useKey, false);
  }

  /** {@inheritDoc} */
  @Override
  public SalesStreamDistribution getDistribution(UseKey useKey, boolean includeExports) {
    EngineNumber domesticValueRaw = getStream(useKey, "domestic");
    EngineNumber importValueRaw = getStream(useKey, "import");
    EngineNumber exportValueRaw = getStream(useKey, "export");

    EngineNumber domesticValue = unitConverter.convert(domesticValueRaw, "kg");
    EngineNumber importValue = unitConverter.convert(importValueRaw, "kg");
    EngineNumber exportValue;
    if (exportValueRaw == null) {
      exportValue = new EngineNumber(BigDecimal.ZERO, "kg");
    } else {
      exportValue = unitConverter.convert(exportValueRaw, "kg");
    }

    boolean domesticEnabled = hasStreamBeenEnabled(useKey, "domestic");
    boolean importEnabled = hasStreamBeenEnabled(useKey, "import");
    boolean exportEnabled = hasStreamBeenEnabled(useKey, "export");

    return new SalesStreamDistributionBuilder()
        .setDomesticSales(domesticValue)
        .setImportSales(importValue)
        .setExportSales(exportValue)
        .setDomesticEnabled(domesticEnabled)
        .setImportEnabled(importEnabled)
        .setExportEnabled(exportEnabled)
        .setIncludeExports(includeExports)
        .build();
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasStreamsEnabled(UseKey useKey) {
    return hasStreamBeenEnabled(useKey, "domestic")
        || hasStreamBeenEnabled(useKey, "import")
        || hasStreamBeenEnabled(useKey, "export");
  }

  /** {@inheritDoc} */
  @Override
  public int getCurrentYear() {
    return currentYear;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setCurrentYear(int year) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<SimulationState> getAtPrior(int years) {
    if (years < 0) {
      return Optional.empty();
    } else if (years == 0) {
      return Optional.of(this);
    } else if (priorState.isEmpty()) {
      return Optional.empty();
    } else {
      return priorState.get().getAtPrior(years - 1);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void incrementYear() {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setGhgIntensity(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setEnergyIntensity(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getGhgIntensity(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getGhgIntensity();
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getEnergyIntensity(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getEnergyIntensity();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setInitialCharge(UseKey useKey, String substream, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInitialCharge(UseKey useKey, String substream) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getInitialCharge(substream);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRechargePopulation(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRechargePopulation(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getRechargePopulation();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRechargeIntensity(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRechargeIntensity(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getRechargeIntensity();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void accumulateRecharge(UseKey useKey, EngineNumber population, EngineNumber intensity) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRechargeBasePopulation(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getRechargeBasePopulation();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRechargeBasePopulation(UseKey useKey, EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRechargeAmount(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getAppliedRechargeAmount();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setAppliedRechargeAmount(UseKey useKey, EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setPrechargePopulation(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getPrechargePopulation(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getPrechargePopulation();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setPrechargeIntensity(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getPrechargeIntensity(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getPrechargeIntensity();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void accumulatePrecharge(UseKey useKey, EngineNumber population, EngineNumber intensity) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getPrechargeBasePopulation(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getPrechargeBasePopulation();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setPrechargeBasePopulation(UseKey useKey, EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedPrechargeAmount(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getAppliedPrechargeAmount();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setAppliedPrechargeAmount(UseKey useKey, EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isRecyclingCalculatedThisStep(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .isRecyclingCalculatedThisStep();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRecyclingCalculatedThisStep(UseKey useKey, boolean calculated) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRecoveryRate(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRecoveryRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRecoveryRate(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getRecoveryRate();
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRecoveryRate(UseKey useKey, RecoveryStage stage) {
    return SimulationStateSupport.getParameterization(substances, useKey).getRecoveryRate(stage);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setYieldRate(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setYieldRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getYieldRate(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getYieldRate();
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getYieldRate(UseKey useKey, RecoveryStage stage) {
    return SimulationStateSupport.getParameterization(substances, useKey).getYieldRate(stage);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setInductionRate(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setInductionRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionRate(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getInductionRate();
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionRate(UseKey useKey, RecoveryStage stage) {
    return SimulationStateSupport.getParameterization(substances, useKey).getInductionRate(stage);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRetirementRate(UseKey useKey, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRetirementRate(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey).getRetirementRate();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRetirementBasePopulation(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getRetirementBasePopulation();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRetirementBasePopulation(UseKey useKey, EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRetirementAmount(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getAppliedRetirementAmount();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setAppliedRetirementAmount(UseKey useKey, EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getHasReplacementThisStep(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getHasReplacementThisStep();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setHasReplacementThisStep(UseKey useKey, boolean value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getRetireCalculatedThisStep(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getRetireCalculatedThisStep();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRetireCalculatedThisStep(UseKey useKey, boolean calculated) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setLastSpecifiedValue(UseKey useKey, String streamName, EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getLastSpecifiedValue(UseKey useKey, String streamName) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .getLastSpecifiedValue(streamName);
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasLastSpecifiedValue(UseKey useKey, String streamName) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .hasLastSpecifiedValue(streamName);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSalesIntentFreshlySet(UseKey useKey) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .isSalesIntentFreshlySet();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void resetSalesIntentFlag(UseKey useKey) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasStreamBeenEnabled(UseKey useKey, String streamName) {
    return SimulationStateSupport.getParameterization(substances, useKey)
        .hasStreamBeenEnabled(streamName);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void markStreamAsEnabled(UseKey useKey, String streamName) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void clearLastSpecifiedValue(UseKey useKey, String stream) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @return this same instance, since it is already frozen
   */
  @Override
  public SimulationState freeze() {
    return this;
  }
}
