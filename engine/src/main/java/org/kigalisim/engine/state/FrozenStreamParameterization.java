/**
 * Immutable snapshot implementation of stream-specific parameters and settings.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Immutable snapshot of {@link StreamParameterization} state.
 *
 * <p>Holds only {@code final} fields populated at construction and throws
 * {@link UnsupportedOperationException} from every mutator, so a snapshot
 * captured for a prior-year lookup can never be silently corrupted.</p>
 */
public class FrozenStreamParameterization implements StreamParameterization {

  private static final String FROZEN_MESSAGE =
      "Cannot mutate a frozen StreamParameterization snapshot.";

  private final EngineNumber ghgIntensity;
  private final EngineNumber energyIntensity;
  private final Map<String, EngineNumber> initialCharge;
  private final EngineNumber rechargePopulation;
  private final EngineNumber rechargeIntensity;
  private final EngineNumber prechargePopulation;
  private final EngineNumber prechargeIntensity;
  private final EngineNumber recoveryRateRecharge;
  private final EngineNumber yieldRateRecharge;
  private final EngineNumber recoveryRateEol;
  private final EngineNumber yieldRateEol;
  private final EngineNumber retirementRate;
  private final EngineNumber inductionRateRecharge;
  private final EngineNumber inductionRateEol;
  private final Map<String, EngineNumber> lastSpecifiedValue;
  private final Set<String> enabledStreams;
  private final boolean salesIntentFreshlySet;
  private final PriorEquipmentBases priorEquipmentBases;

  /**
   * Create a new immutable snapshot with the specified field values.
   *
   * @param ghgIntensity The greenhouse gas intensity
   * @param energyIntensity The energy intensity
   * @param initialCharge The initial charge per sales substream
   * @param rechargePopulation The recharge population percentage
   * @param rechargeIntensity The recharge intensity
   * @param prechargePopulation The precharge population percentage
   * @param prechargeIntensity The precharge intensity
   * @param recoveryRateRecharge The recovery rate for the RECHARGE stage
   * @param yieldRateRecharge The yield rate for the RECHARGE stage
   * @param recoveryRateEol The recovery rate for the EOL stage
   * @param yieldRateEol The yield rate for the EOL stage
   * @param retirementRate The retirement rate percentage
   * @param inductionRateRecharge The induction rate for the RECHARGE stage
   * @param inductionRateEol The induction rate for the EOL stage
   * @param lastSpecifiedValue The last specified value per stream
   * @param enabledStreams The set of streams that have been enabled
   * @param salesIntentFreshlySet Whether sales intent was freshly set
   * @param priorEquipmentBases The frozen prior equipment bases snapshot
   */
  public FrozenStreamParameterization(EngineNumber ghgIntensity, EngineNumber energyIntensity,
      Map<String, EngineNumber> initialCharge, EngineNumber rechargePopulation,
      EngineNumber rechargeIntensity, EngineNumber prechargePopulation,
      EngineNumber prechargeIntensity, EngineNumber recoveryRateRecharge,
      EngineNumber yieldRateRecharge, EngineNumber recoveryRateEol, EngineNumber yieldRateEol,
      EngineNumber retirementRate, EngineNumber inductionRateRecharge,
      EngineNumber inductionRateEol, Map<String, EngineNumber> lastSpecifiedValue,
      Set<String> enabledStreams, boolean salesIntentFreshlySet,
      PriorEquipmentBases priorEquipmentBases) {
    this.ghgIntensity = ghgIntensity;
    this.energyIntensity = energyIntensity;
    this.initialCharge = initialCharge;
    this.rechargePopulation = rechargePopulation;
    this.rechargeIntensity = rechargeIntensity;
    this.prechargePopulation = prechargePopulation;
    this.prechargeIntensity = prechargeIntensity;
    this.recoveryRateRecharge = recoveryRateRecharge;
    this.yieldRateRecharge = yieldRateRecharge;
    this.recoveryRateEol = recoveryRateEol;
    this.yieldRateEol = yieldRateEol;
    this.retirementRate = retirementRate;
    this.inductionRateRecharge = inductionRateRecharge;
    this.inductionRateEol = inductionRateEol;
    this.lastSpecifiedValue = lastSpecifiedValue;
    this.enabledStreams = enabledStreams;
    this.salesIntentFreshlySet = salesIntentFreshlySet;
    this.priorEquipmentBases = priorEquipmentBases;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setGhgIntensity(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getGhgIntensity() {
    return ghgIntensity;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setEnergyIntensity(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getEnergyIntensity() {
    return energyIntensity;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setInitialCharge(String stream, EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInitialCharge(String stream) {
    if (!StreamParameterization.isInitialChargeStreamAllowed(stream)) {
      throw new IllegalArgumentException("Must address a sales substream.");
    }
    return initialCharge.get(stream);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRechargePopulation(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRechargePopulation() {
    return rechargePopulation;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRechargeIntensity(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRechargeIntensity() {
    return rechargeIntensity;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setPrechargePopulation(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getPrechargePopulation() {
    return prechargePopulation;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setPrechargeIntensity(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getPrechargeIntensity() {
    return prechargeIntensity;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRecoveryRate(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRecoveryRate(EngineNumber newValue, RecoveryStage stage) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRecoveryRate() {
    return recoveryRateRecharge;
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRecoveryRate(RecoveryStage stage) {
    return switch (stage) {
      case EOL -> recoveryRateEol;
      case RECHARGE -> recoveryRateRecharge;
    };
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setYieldRate(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setYieldRate(EngineNumber newValue, RecoveryStage stage) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getYieldRate() {
    return yieldRateRecharge;
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getYieldRate(RecoveryStage stage) {
    return switch (stage) {
      case EOL -> yieldRateEol;
      case RECHARGE -> yieldRateRecharge;
    };
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setInductionRate(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setInductionRate(EngineNumber newValue, RecoveryStage stage) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionRate() {
    return inductionRateRecharge;
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionRate(RecoveryStage stage) {
    return switch (stage) {
      case EOL -> inductionRateEol;
      case RECHARGE -> inductionRateRecharge;
    };
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRetirementRate(EngineNumber newValue) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRetirementRate() {
    return retirementRate;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRetirementBasePopulation() {
    return priorEquipmentBases.getRetirementBasePopulation();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRetirementBasePopulation(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRetirementAmount() {
    return priorEquipmentBases.getAppliedRetirementAmount();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setAppliedRetirementAmount(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getHasReplacementThisStep() {
    return priorEquipmentBases.getHasReplacementThisStep();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setHasReplacementThisStep(boolean value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getRetireCalculatedThisStep() {
    return priorEquipmentBases.getRetireCalculatedThisStep();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRetireCalculatedThisStep(boolean calculated) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRechargeBasePopulation() {
    return priorEquipmentBases.getRechargeBasePopulation();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRechargeBasePopulation(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRechargeAmount() {
    return priorEquipmentBases.getAppliedRechargeAmount();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setAppliedRechargeAmount(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getPrechargeBasePopulation() {
    return priorEquipmentBases.getPrechargeBasePopulation();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setPrechargeBasePopulation(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedPrechargeAmount() {
    return priorEquipmentBases.getAppliedPrechargeAmount();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setAppliedPrechargeAmount(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isRecyclingCalculatedThisStep() {
    return priorEquipmentBases.getRecyclingCalculatedThisStep();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRecyclingCalculatedThisStep(boolean calculated) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void accumulateRecharge(EngineNumber population, EngineNumber intensity) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void accumulatePrecharge(EngineNumber population, EngineNumber intensity) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setLastSpecifiedValue(String streamName, EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getLastSpecifiedValue(String streamName) {
    return lastSpecifiedValue.get(streamName);
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasLastSpecifiedValue(String streamName) {
    return lastSpecifiedValue.containsKey(streamName);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void markStreamAsEnabled(String streamName) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasStreamBeenEnabled(String streamName) {
    return enabledStreams.contains(streamName);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSalesIntentFreshlySet() {
    return salesIntentFreshlySet;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setSalesIntentFreshlySet(boolean freshlySet) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void resetStateAtTimestep() {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void clearLastSpecifiedValue(String stream) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @return this same instance, since it is already frozen
   */
  @Override
  public StreamParameterization freeze() {
    return this;
  }
}
