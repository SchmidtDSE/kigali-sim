/**
 * Mutable, live implementation of stream-specific parameters and settings.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.kigalisim.engine.state.SimulationState.ZERO_VOLUME;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Live, mutable stream-specific parameterization for a substance.
 *
 * <p>Handles configuration of GHG intensity, initial charge, recharge rates,
 * recovery rates, and other stream-specific values.</p>
 */
public class MutableStreamParameterization implements StreamParameterization {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private EngineNumber ghgIntensity;
  private EngineNumber energyIntensity;
  private final Map<String, EngineNumber> initialCharge;
  private EngineNumber rechargePopulation;
  private EngineNumber rechargeIntensity;
  private EngineNumber prechargePopulation;
  private EngineNumber prechargeIntensity;
  private EngineNumber recoveryRateRecharge;
  private EngineNumber yieldRateRecharge;
  private EngineNumber recoveryRateEol;
  private EngineNumber yieldRateEol;
  private EngineNumber retirementRate;
  private EngineNumber inductionRateRecharge;
  private EngineNumber inductionRateEol;
  private final Map<String, EngineNumber> lastSpecifiedValue;
  private final Set<String> enabledStreams;
  private boolean salesIntentFreshlySet;
  private PriorEquipmentBases priorEquipmentBases;

  /**
   * Create a new stream parameterization instance.
   */
  public MutableStreamParameterization() {
    this.initialCharge = new HashMap<>();
    this.enabledStreams = new HashSet<>();
    this.lastSpecifiedValue = new HashMap<>();
    this.salesIntentFreshlySet = false;

    ghgIntensity = new EngineNumber(BigDecimal.ZERO, "tCO2e / kg");
    energyIntensity = new EngineNumber(BigDecimal.ZERO, "kwh / kg");

    initialCharge.put("domestic", new EngineNumber(BigDecimal.ONE, "kg / unit"));
    initialCharge.put("import", new EngineNumber(BigDecimal.ONE, "kg / unit"));

    rechargePopulation = new EngineNumber(BigDecimal.ZERO, "%");
    rechargeIntensity = new EngineNumber(BigDecimal.ZERO, "kg / unit");
    prechargePopulation = new EngineNumber(BigDecimal.ZERO, "%");
    prechargeIntensity = new EngineNumber(BigDecimal.ZERO, "kg / unit");
    recoveryRateRecharge = new EngineNumber(BigDecimal.ZERO, "%");
    yieldRateRecharge = new EngineNumber(BigDecimal.ZERO, "%");
    recoveryRateEol = new EngineNumber(BigDecimal.ZERO, "%");
    yieldRateEol = new EngineNumber(BigDecimal.ZERO, "%");
    retirementRate = new EngineNumber(BigDecimal.ZERO, "%");
    inductionRateRecharge = getDefaultInductionRate();
    inductionRateEol = getDefaultInductionRate();

    priorEquipmentBases = new MutablePriorEquipmentBases();

    setLastSpecifiedValue("domestic", ZERO_VOLUME);
    setLastSpecifiedValue("import", ZERO_VOLUME);
    setLastSpecifiedValue("export", ZERO_VOLUME);
    setSalesIntentFreshlySet(false);
  }

  /** {@inheritDoc} */
  @Override
  public void setGhgIntensity(EngineNumber newValue) {
    ghgIntensity = newValue;
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getGhgIntensity() {
    return ghgIntensity;
  }

  /** {@inheritDoc} */
  @Override
  public void setEnergyIntensity(EngineNumber newValue) {
    energyIntensity = newValue;
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getEnergyIntensity() {
    return energyIntensity;
  }

  /** {@inheritDoc} */
  @Override
  public void setInitialCharge(String stream, EngineNumber newValue) {
    ensureSalesStreamAllowed(stream);
    initialCharge.put(stream, newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInitialCharge(String stream) {
    ensureSalesStreamAllowed(stream);
    return initialCharge.get(stream);
  }

  /** {@inheritDoc} */
  @Override
  public void setRechargePopulation(EngineNumber newValue) {
    rechargePopulation = clampRate(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRechargePopulation() {
    return rechargePopulation;
  }

  /** {@inheritDoc} */
  @Override
  public void setRechargeIntensity(EngineNumber newValue) {
    rechargeIntensity = newValue;
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRechargeIntensity() {
    return rechargeIntensity;
  }

  /** {@inheritDoc} */
  @Override
  public void setPrechargePopulation(EngineNumber newValue) {
    prechargePopulation = clampRate(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getPrechargePopulation() {
    return prechargePopulation;
  }

  /** {@inheritDoc} */
  @Override
  public void setPrechargeIntensity(EngineNumber newValue) {
    prechargeIntensity = newValue;
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getPrechargeIntensity() {
    return prechargeIntensity;
  }

  /** {@inheritDoc} */
  @Override
  public void setRecoveryRate(EngineNumber newValue) {
    recoveryRateRecharge = newValue;
  }

  /** {@inheritDoc} */
  @Override
  public void setRecoveryRate(EngineNumber newValue, RecoveryStage stage) {
    switch (stage) {
      case EOL -> recoveryRateEol = newValue;
      case RECHARGE -> recoveryRateRecharge = newValue;
      default -> throw new IllegalArgumentException("Unknown recovery stage: " + stage);
    }
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

  /** {@inheritDoc} */
  @Override
  public void setYieldRate(EngineNumber newValue) {
    yieldRateRecharge = newValue;
  }

  /** {@inheritDoc} */
  @Override
  public void setYieldRate(EngineNumber newValue, RecoveryStage stage) {
    switch (stage) {
      case EOL -> yieldRateEol = newValue;
      case RECHARGE -> yieldRateRecharge = newValue;
      default -> throw new IllegalArgumentException("Unknown recovery stage: " + stage);
    }
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

  /** {@inheritDoc} */
  @Override
  public void setInductionRate(EngineNumber newValue) {
    inductionRateRecharge = newValue;
  }

  /** {@inheritDoc} */
  @Override
  public void setInductionRate(EngineNumber newValue, RecoveryStage stage) {
    switch (stage) {
      case EOL -> inductionRateEol = newValue;
      case RECHARGE -> inductionRateRecharge = newValue;
      default -> throw new IllegalArgumentException("Unknown recovery stage: " + stage);
    }
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
   * Get the default induction rate (100% - induced demand behavior).
   *
   * @return EngineNumber representing 100% induction
   */
  private static EngineNumber getDefaultInductionRate() {
    return new EngineNumber(new BigDecimal("100"), "%");
  }

  /** {@inheritDoc} */
  @Override
  public void setRetirementRate(EngineNumber newValue) {
    BigDecimal currentValue = retirementRate.getValue();
    BigDecimal newTotal = currentValue.add(newValue.getValue());
    EngineNumber candidateRetirementRate = new EngineNumber(newTotal, newValue.getUnits());
    retirementRate = clampRate(candidateRetirementRate);
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

  /** {@inheritDoc} */
  @Override
  public void setRetirementBasePopulation(EngineNumber value) {
    priorEquipmentBases.setRetirementBasePopulation(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRetirementAmount() {
    return priorEquipmentBases.getAppliedRetirementAmount();
  }

  /** {@inheritDoc} */
  @Override
  public void setAppliedRetirementAmount(EngineNumber value) {
    priorEquipmentBases.setAppliedRetirementAmount(value);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getHasReplacementThisStep() {
    return priorEquipmentBases.getHasReplacementThisStep();
  }

  /** {@inheritDoc} */
  @Override
  public void setHasReplacementThisStep(boolean value) {
    priorEquipmentBases.setHasReplacementThisStep(value);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getRetireCalculatedThisStep() {
    return priorEquipmentBases.getRetireCalculatedThisStep();
  }

  /** {@inheritDoc} */
  @Override
  public void setRetireCalculatedThisStep(boolean calculated) {
    priorEquipmentBases.setRetireCalculatedThisStep(calculated);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRechargeBasePopulation() {
    return priorEquipmentBases.getRechargeBasePopulation();
  }

  /** {@inheritDoc} */
  @Override
  public void setRechargeBasePopulation(EngineNumber value) {
    priorEquipmentBases.setRechargeBasePopulation(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRechargeAmount() {
    return priorEquipmentBases.getAppliedRechargeAmount();
  }

  /** {@inheritDoc} */
  @Override
  public void setAppliedRechargeAmount(EngineNumber value) {
    priorEquipmentBases.setAppliedRechargeAmount(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getPrechargeBasePopulation() {
    return priorEquipmentBases.getPrechargeBasePopulation();
  }

  /** {@inheritDoc} */
  @Override
  public void setPrechargeBasePopulation(EngineNumber value) {
    priorEquipmentBases.setPrechargeBasePopulation(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedPrechargeAmount() {
    return priorEquipmentBases.getAppliedPrechargeAmount();
  }

  /** {@inheritDoc} */
  @Override
  public void setAppliedPrechargeAmount(EngineNumber value) {
    priorEquipmentBases.setAppliedPrechargeAmount(value);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isRecyclingCalculatedThisStep() {
    return priorEquipmentBases.getRecyclingCalculatedThisStep();
  }

  /** {@inheritDoc} */
  @Override
  public void setRecyclingCalculatedThisStep(boolean calculated) {
    priorEquipmentBases.setRecyclingCalculatedThisStep(calculated);
  }

  /** {@inheritDoc} */
  @Override
  public void accumulateRecharge(EngineNumber population, EngineNumber intensity) {
    ServicingInformation currentInfo = new ServicingInformation(
        rechargePopulation,
        rechargeIntensity
    );
    ServicingInformation result = currentInfo.add(population, intensity);

    rechargePopulation = clampRate(result.getPopulation());
    rechargeIntensity = result.getIntensity();
  }

  /** {@inheritDoc} */
  @Override
  public void accumulatePrecharge(EngineNumber population, EngineNumber intensity) {
    ServicingInformation currentInfo = new ServicingInformation(
        prechargePopulation,
        prechargeIntensity
    );
    ServicingInformation result = currentInfo.add(population, intensity);

    prechargePopulation = clampRate(result.getPopulation());
    prechargeIntensity = result.getIntensity();
  }

  /** {@inheritDoc} */
  @Override
  public void setLastSpecifiedValue(String streamName, EngineNumber value) {
    // Ignore percentage units to avoid impacting last recorded values
    if (value != null && value.getUnits() != null && value.getUnits().contains("%")) {
      return;
    }
    lastSpecifiedValue.put(streamName, value);

    // Set the flag if this is a sales-related stream
    if (getIsSalesStream(streamName)) {
      salesIntentFreshlySet = true;
    }
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

  /** {@inheritDoc} */
  @Override
  public void markStreamAsEnabled(String streamName) {
    enabledStreams.add(streamName);
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

  /** {@inheritDoc} */
  @Override
  public void setSalesIntentFreshlySet(boolean freshlySet) {
    this.salesIntentFreshlySet = freshlySet;
  }

  /** {@inheritDoc} */
  @Override
  public void resetStateAtTimestep() {
    // Reset recovery to 0% between steps since recycling programs may cease
    recoveryRateRecharge = new EngineNumber(BigDecimal.ZERO, "%");
    recoveryRateEol = new EngineNumber(BigDecimal.ZERO, "%");

    // Reset induction to 100% (default induced demand behavior)
    inductionRateRecharge = getDefaultInductionRate();
    inductionRateEol = getDefaultInductionRate();

    // Reset retirement tracking for new step
    retirementRate = new EngineNumber(BigDecimal.ZERO, "%");

    // Reset recharge tracking for new step
    rechargePopulation = new EngineNumber(BigDecimal.ZERO, "%");
    rechargeIntensity = new EngineNumber(BigDecimal.ZERO, "kg / unit");

    // Reset precharge tracking for new step
    prechargePopulation = new EngineNumber(BigDecimal.ZERO, "%");
    prechargeIntensity = new EngineNumber(BigDecimal.ZERO, "kg / unit");

    // Reset cumulative tracking
    priorEquipmentBases.resetStateAtTimestep();
  }

  /**
   * Check if a stream name is a sales stream.
   *
   * @param name The stream name to check
   * @return True if the stream is a sales stream, false otherwise
   */
  private boolean getIsSalesStreamAllowed(String name) {
    return StreamParameterization.isInitialChargeStreamAllowed(name);
  }

  /**
   * Ensure the stream name is a sales substream.
   *
   * @param name The stream name to validate
   * @throws IllegalArgumentException If the stream name is not a sales substream
   */
  private void ensureSalesStreamAllowed(String name) {
    if (!getIsSalesStreamAllowed(name)) {
      throw new IllegalArgumentException("Must address a sales substream.");
    }
  }

  /**
   * Ensure that a value is positive and, if a percent, 100 or less.
   *
   * @param target The value to clamp to [0,] or [0, 100] if percent.
   * @return The value after clamping.
   */
  private EngineNumber clampRate(EngineNumber target) {
    if (target.getValue().compareTo(BigDecimal.ZERO) < 0) {
      return new EngineNumber(BigDecimal.ZERO, target.getUnits());
    }

    boolean isOver100 = target.getValue().compareTo(ONE_HUNDRED) > 0;
    if (isOver100 && target.getUnits().equals("%")) {
      return new EngineNumber(ONE_HUNDRED, target.getUnits());
    }

    return target;
  }

  /** {@inheritDoc} */
  @Override
  public void clearLastSpecifiedValue(String stream) {
    switch (stream) {
      case "sales", "virgin" -> {
        lastSpecifiedValue.remove("sales");
        lastSpecifiedValue.remove("import");
        lastSpecifiedValue.remove("domestic");
        lastSpecifiedValue.remove("virgin");
        setSalesIntentFreshlySet(false);
      }
      case "domestic", "import" -> {
        lastSpecifiedValue.remove(stream);
        lastSpecifiedValue.remove("sales");
        lastSpecifiedValue.remove("virgin");
        setSalesIntentFreshlySet(false);
      }
      default -> lastSpecifiedValue.remove(stream);
    }
  }

  /**
   * Check if a stream name is a sales stream.
   *
   * @param streamName The stream name to check
   * @return True if the stream is a sales stream, false otherwise
   */
  private boolean getIsSalesStream(String streamName) {
    return switch (streamName) {
      case "sales", "domestic", "import", "virgin" -> true;
      default -> false;
    };
  }

  /**
   * Create an immutable snapshot of this instance.
   *
   * <p>The mutable maps and sets are copied so that additions and removals on the
   * snapshot's original do not affect the snapshot. EngineNumber instances are
   * immutable, so their references can be shared. The prior equipment bases are
   * frozen recursively.</p>
   *
   * @return An immutable {@link FrozenStreamParameterization} snapshot
   */
  @Override
  public StreamParameterization freeze() {
    return new FrozenStreamParameterization(
        ghgIntensity,
        energyIntensity,
        new HashMap<>(initialCharge),
        rechargePopulation,
        rechargeIntensity,
        prechargePopulation,
        prechargeIntensity,
        recoveryRateRecharge,
        yieldRateRecharge,
        recoveryRateEol,
        yieldRateEol,
        retirementRate,
        inductionRateRecharge,
        inductionRateEol,
        new HashMap<>(lastSpecifiedValue),
        new HashSet<>(enabledStreams),
        salesIntentFreshlySet,
        priorEquipmentBases.freeze());
  }
}
