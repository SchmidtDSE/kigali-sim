/**
 * Class for managing stream-specific parameters and settings.
 *
 * <p>Handles configuration of GHG intensity, initial charge, recharge rates,
 * recovery rates, and other stream-specific values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Class for managing stream-specific parameters and settings.
 *
 * <p>Handles configuration of GHG intensity, initial charge, recharge rates,
 * recovery rates, and other stream-specific values.</p>
 */
public class StreamParameterization {

  private EngineNumber ghgIntensity;
  private EngineNumber energyIntensity;
  private final Map<String, EngineNumber> initialCharge;
  private EngineNumber rechargePopulation;
  private EngineNumber rechargeIntensity;
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

  // Cumulative retirement and recharge tracking (independent bases)
  private PriorEquipmentBases priorEquipmentBases;

  /**
   * Create a new stream parameterization instance.
   */
  public StreamParameterization() {
    this.initialCharge = new HashMap<>();
    this.enabledStreams = new HashSet<>();
    this.lastSpecifiedValue = new HashMap<>();
    this.salesIntentFreshlySet = false;

    // Initialize all parameters with default values
    ghgIntensity = new EngineNumber(BigDecimal.ZERO, "tCO2e / kg");
    energyIntensity = new EngineNumber(BigDecimal.ZERO, "kwh / kg");

    initialCharge.put("domestic", new EngineNumber(BigDecimal.ONE, "kg / unit"));
    initialCharge.put("import", new EngineNumber(BigDecimal.ONE, "kg / unit"));

    rechargePopulation = new EngineNumber(BigDecimal.ZERO, "%");
    rechargeIntensity = new EngineNumber(BigDecimal.ZERO, "kg / unit");
    recoveryRateRecharge = new EngineNumber(BigDecimal.ZERO, "%");
    yieldRateRecharge = new EngineNumber(BigDecimal.ZERO, "%");
    recoveryRateEol = new EngineNumber(BigDecimal.ZERO, "%");
    yieldRateEol = new EngineNumber(BigDecimal.ZERO, "%");
    retirementRate = new EngineNumber(BigDecimal.ZERO, "%");
    inductionRateRecharge = getDefaultInductionRate();
    inductionRateEol = getDefaultInductionRate();

    priorEquipmentBases = new PriorEquipmentBases();
  }


  /**
   * Set the greenhouse gas intensity.
   *
   * @param newValue The new GHG intensity value
   */
  public void setGhgIntensity(EngineNumber newValue) {
    ghgIntensity = newValue;
  }

  /**
   * Get the greenhouse gas intensity.
   *
   * @return The current GHG intensity value
   */
  public EngineNumber getGhgIntensity() {
    return ghgIntensity;
  }

  /**
   * Set the energy intensity.
   *
   * @param newValue The new energy intensity value
   */
  public void setEnergyIntensity(EngineNumber newValue) {
    energyIntensity = newValue;
  }

  /**
   * Get the energy intensity.
   *
   * @return The current energy intensity value
   */
  public EngineNumber getEnergyIntensity() {
    return energyIntensity;
  }

  /**
   * Set the initial charge for a stream.
   *
   * @param stream The stream identifier ('domestic' or 'import')
   * @param newValue The new initial charge value
   */
  public void setInitialCharge(String stream, EngineNumber newValue) {
    ensureSalesStreamAllowed(stream);
    initialCharge.put(stream, newValue);
  }

  /**
   * Get the initial charge for a stream.
   *
   * @param stream The stream identifier ('domestic' or 'import')
   * @return The initial charge value for the stream
   */
  public EngineNumber getInitialCharge(String stream) {
    ensureSalesStreamAllowed(stream);
    return initialCharge.get(stream);
  }

  /**
   * Set the recharge population percentage.
   *
   * @param newValue The new recharge population value
   */
  public void setRechargePopulation(EngineNumber newValue) {
    rechargePopulation = newValue;
  }

  /**
   * Get the recharge population percentage.
   *
   * @return The current recharge population value
   */
  public EngineNumber getRechargePopulation() {
    return rechargePopulation;
  }

  /**
   * Set the recharge intensity.
   *
   * @param newValue The new recharge intensity value
   */
  public void setRechargeIntensity(EngineNumber newValue) {
    rechargeIntensity = newValue;
  }

  /**
   * Get the recharge intensity.
   *
   * @return The current recharge intensity value
   */
  public EngineNumber getRechargeIntensity() {
    return rechargeIntensity;
  }

  /**
   * Set the recovery rate percentage.
   *
   * @param newValue The new recovery rate value
   */
  public void setRecoveryRate(EngineNumber newValue) {
    recoveryRateRecharge = newValue;
  }

  /**
   * Set the recovery rate percentage for a specific stage.
   *
   * @param newValue The new recovery rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  public void setRecoveryRate(EngineNumber newValue, RecoveryStage stage) {
    switch (stage) {
      case EOL -> recoveryRateEol = newValue;
      case RECHARGE -> recoveryRateRecharge = newValue;
      default -> throw new IllegalArgumentException("Unknown recovery stage: " + stage);
    }
  }

  /**
   * Get the recovery rate percentage.
   *
   * @return The current recovery rate value
   */
  public EngineNumber getRecoveryRate() {
    return recoveryRateRecharge;
  }

  /**
   * Get the recovery rate percentage for a specific stage.
   *
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current recovery rate value
   */
  public EngineNumber getRecoveryRate(RecoveryStage stage) {
    return switch (stage) {
      case EOL -> recoveryRateEol;
      case RECHARGE -> recoveryRateRecharge;
    };
  }

  /**
   * Set the yield rate percentage for recycling.
   *
   * @param newValue The new yield rate value
   */
  public void setYieldRate(EngineNumber newValue) {
    yieldRateRecharge = newValue;
  }

  /**
   * Set the yield rate percentage for recycling for a specific stage.
   *
   * @param newValue The new yield rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  public void setYieldRate(EngineNumber newValue, RecoveryStage stage) {
    switch (stage) {
      case EOL -> yieldRateEol = newValue;
      case RECHARGE -> yieldRateRecharge = newValue;
      default -> throw new IllegalArgumentException("Unknown recovery stage: " + stage);
    }
  }

  /**
   * Get the yield rate percentage for recycling.
   *
   * @return The current yield rate value
   */
  public EngineNumber getYieldRate() {
    return yieldRateRecharge;
  }

  /**
   * Get the yield rate percentage for recycling for a specific stage.
   *
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current yield rate value
   */
  public EngineNumber getYieldRate(RecoveryStage stage) {
    return switch (stage) {
      case EOL -> yieldRateEol;
      case RECHARGE -> yieldRateRecharge;
    };
  }

  /**
   * Set the induction rate percentage for recycling.
   *
   * @param newValue The new induction rate value
   */
  public void setInductionRate(EngineNumber newValue) {
    inductionRateRecharge = newValue;
  }

  /**
   * Set the induction rate percentage for recycling for a specific stage.
   *
   * @param newValue The new induction rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  public void setInductionRate(EngineNumber newValue, RecoveryStage stage) {
    switch (stage) {
      case EOL -> inductionRateEol = newValue;
      case RECHARGE -> inductionRateRecharge = newValue;
      default -> throw new IllegalArgumentException("Unknown recovery stage: " + stage);
    }
  }

  /**
   * Get the induction rate percentage for recycling.
   *
   * @return The current induction rate value
   */
  public EngineNumber getInductionRate() {
    return inductionRateRecharge;
  }

  /**
   * Get the induction rate percentage for recycling for a specific stage.
   *
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current induction rate value
   */
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

  /**
   * Set the retirement rate percentage.
   *
   * <p>This method accumulates retirement rates across multiple retire commands
   * in the same year to support cumulative retirement behavior. If the resulting
   * retirement rate is negative, it is clamped to zero (no retirement).</p>
   *
   * @param newValue The new retirement rate value to add
   */
  public void setRetirementRate(EngineNumber newValue) {
    BigDecimal currentValue = retirementRate.getValue();
    BigDecimal newTotal = currentValue.add(newValue.getValue());

    // Clamp negative retirement to zero (no retirement)
    if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
      newTotal = BigDecimal.ZERO;
    }

    retirementRate = new EngineNumber(newTotal, newValue.getUnits());
  }

  /**
   * Get the retirement rate percentage.
   *
   * @return The current retirement rate value
   */
  public EngineNumber getRetirementRate() {
    return retirementRate;
  }

  /**
   * Get the retirement base population for cumulative calculations.
   *
   * @return The base population, or empty if not yet captured this step
   */
  public Optional<EngineNumber> getRetirementBasePopulation() {
    return priorEquipmentBases.getRetirementBasePopulation();
  }

  /**
   * Set the retirement base population for cumulative calculations.
   *
   * @param value The base population value
   */
  public void setRetirementBasePopulation(EngineNumber value) {
    priorEquipmentBases.setRetirementBasePopulation(value);
  }

  /**
   * Get the applied retirement amount for cumulative calculations.
   *
   * @return The total amount already retired this step
   */
  public Optional<EngineNumber> getAppliedRetirementAmount() {
    return priorEquipmentBases.getAppliedRetirementAmount();
  }

  /**
   * Set the applied retirement amount for cumulative calculations.
   *
   * @param value The total amount retired this step
   */
  public void setAppliedRetirementAmount(EngineNumber value) {
    priorEquipmentBases.setAppliedRetirementAmount(value);
  }

  /**
   * Get the replacement mode for this step's retire commands.
   *
   * @return true if with replacement, false if without replacement
   */
  public boolean getHasReplacementThisStep() {
    return priorEquipmentBases.getHasReplacementThisStep();
  }

  /**
   * Set the replacement mode for this step's retire commands.
   *
   * @param value true for with replacement, false for without replacement
   */
  public void setHasReplacementThisStep(boolean value) {
    priorEquipmentBases.setHasReplacementThisStep(value);
  }

  /**
   * Get whether retire has been calculated this step.
   *
   * @return true if retire was calculated, false otherwise
   */
  public boolean getRetireCalculatedThisStep() {
    return priorEquipmentBases.getRetireCalculatedThisStep();
  }

  /**
   * Set whether retire has been calculated this step.
   *
   * @param calculated true if retire was calculated, false otherwise
   */
  public void setRetireCalculatedThisStep(boolean calculated) {
    priorEquipmentBases.setRetireCalculatedThisStep(calculated);
  }

  /**
   * Get the recharge base population for cumulative calculations.
   *
   * @return The base population, or empty if not yet captured this step
   */
  public Optional<EngineNumber> getRechargeBasePopulation() {
    return priorEquipmentBases.getRechargeBasePopulation();
  }

  /**
   * Set the recharge base population for cumulative calculations.
   *
   * @param value The base population value
   */
  public void setRechargeBasePopulation(EngineNumber value) {
    priorEquipmentBases.setRechargeBasePopulation(value);
  }

  /**
   * Get the applied recharge amount for cumulative calculations.
   *
   * @return The total amount already recharged this step in kg
   */
  public Optional<EngineNumber> getAppliedRechargeAmount() {
    return priorEquipmentBases.getAppliedRechargeAmount();
  }

  /**
   * Set the applied recharge amount for cumulative calculations.
   *
   * @param value The total amount recharged this step in kg
   */
  public void setAppliedRechargeAmount(EngineNumber value) {
    priorEquipmentBases.setAppliedRechargeAmount(value);
  }

  /**
   * Get whether recycling has been calculated this step.
   *
   * @return true if recycling was calculated, false otherwise
   */
  public boolean isRecyclingCalculatedThisStep() {
    return priorEquipmentBases.getRecyclingCalculatedThisStep();
  }

  /**
   * Set whether recycling has been calculated this step.
   *
   * @param calculated true if recycling was calculated, false otherwise
   */
  public void setRecyclingCalculatedThisStep(boolean calculated) {
    priorEquipmentBases.setRecyclingCalculatedThisStep(calculated);
  }

  /**
   * Accumulate recharge parameters. Sets when not previously set, accumulates otherwise.
   *
   * <p>Multiple calls accumulate rates (addition) and intensities (weighted-average).
   * Population rates are added, intensities are weighted-averaged using absolute values for weights
   * to handle negative adjustments correctly.</p>
   *
   * <p>Weighted average formula: (|rate1| × intensity1 + |rate2| × intensity2) / (|rate1| + |rate2|)</p>
   *
   * @param population The recharge population rate to add
   * @param intensity The recharge intensity for this rate
   */
  public void accumulateRecharge(EngineNumber population, EngineNumber intensity) {
    RechargeInformation currentInfo = new RechargeInformation(
        rechargePopulation,
        rechargeIntensity
    );
    RechargeInformation result = currentInfo.add(population, intensity);

    rechargePopulation = result.getPopulation();
    rechargeIntensity = result.getIntensity();
  }

  /**
   * Set the last specified value for a stream.
   *
   * <p>This tracks the value and units last used when setting streams
   * to preserve user intent across carry-over years.</p>
   *
   * @param streamName The name of the stream
   * @param value The last specified value with units
   */
  public void setLastSpecifiedValue(String streamName, EngineNumber value) {
    // Ignore percentage units to avoid impacting last recorded values
    if (value != null && value.getUnits() != null && value.getUnits().contains("%")) {
      return;
    }
    lastSpecifiedValue.put(streamName, value);

    // Set the flag if this is a sales-related stream
    if ("sales".equals(streamName) || "import".equals(streamName) || "domestic".equals(streamName)) {
      salesIntentFreshlySet = true;
    }
  }

  /**
   * Get the last specified value for a stream.
   *
   * @param streamName The name of the stream
   * @return The last specified value with units, or null if not set
   */
  public EngineNumber getLastSpecifiedValue(String streamName) {
    return lastSpecifiedValue.get(streamName);
  }

  /**
   * Check if a stream has a last specified value.
   *
   * @param streamName The name of the stream
   * @return true if the stream has a last specified value, false otherwise
   */
  public boolean hasLastSpecifiedValue(String streamName) {
    return lastSpecifiedValue.containsKey(streamName);
  }


  /**
   * Mark a stream as having been enabled (set to non-zero value).
   *
   * @param streamName The name of the stream to mark as enabled
   */
  public void markStreamAsEnabled(String streamName) {
    enabledStreams.add(streamName);
  }

  /**
   * Check if a stream has ever been enabled (set to non-zero value).
   *
   * @param streamName The name of the stream to check
   * @return true if the stream has been enabled, false otherwise
   */
  public boolean hasStreamBeenEnabled(String streamName) {
    return enabledStreams.contains(streamName);
  }

  /**
   * Check if sales intent has been freshly set in the current processing cycle.
   *
   * @return true if sales intent was freshly set, false otherwise
   */
  public boolean isSalesIntentFreshlySet() {
    return salesIntentFreshlySet;
  }

  /**
   * Set the flag indicating whether sales intent has been freshly set.
   *
   * @param freshlySet true if sales intent was freshly set, false otherwise
   */
  public void setSalesIntentFreshlySet(boolean freshlySet) {
    this.salesIntentFreshlySet = freshlySet;
  }

  /**
   * Reset state at the beginning of a timestep.
   *
   * <p>This method resets recovery rate to 0% and induction rate to 100% between steps since
   * recycling programs may cease and should not be expected to continue unchanged, but default
   * induction behavior should return to induced demand (100%).</p>
   */
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
    return switch (name) {
      case "domestic", "import", "export", "recycle", "recycleRecharge", "recycleEol" -> true;
      default -> false;
    };
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
}
