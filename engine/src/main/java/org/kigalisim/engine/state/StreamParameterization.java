/**
 * Interface for managing stream-specific parameters and settings.
 *
 * <p>Handles configuration of GHG intensity, initial charge, recharge rates,
 * recovery rates, and other stream-specific values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Interface for managing stream-specific parameters and settings.
 *
 * <p>Implemented by {@link MutableStreamParameterization} for live, mutable
 * substance parameterization during a simulation and by
 * {@link FrozenStreamParameterization} for immutable snapshots captured for
 * prior-year lookups.</p>
 */
public interface StreamParameterization {

  /**
   * Determine whether a stream name is a sales-related substream on which
   * initial charge may be set.
   *
   * <p>Shared between {@link MutableStreamParameterization} and
   * {@link FrozenStreamParameterization} so the set of valid substream names
   * cannot drift between the two implementations.</p>
   *
   * @param name The stream name to check
   * @return True if the stream is a sales substream on which initial charge
   *     is allowed, false otherwise
   */
  static boolean isInitialChargeStreamAllowed(String name) {
    return switch (name) {
      case "domestic", "import", "export", "recycle", "recycleRecharge", "recycleEol", "virgin" -> true;
      default -> false;
    };
  }

  /**
   * Set the greenhouse gas intensity.
   *
   * @param newValue The new GHG intensity value
   */
  void setGhgIntensity(EngineNumber newValue);

  /**
   * Get the greenhouse gas intensity.
   *
   * @return The current GHG intensity value
   */
  EngineNumber getGhgIntensity();

  /**
   * Set the energy intensity.
   *
   * @param newValue The new energy intensity value
   */
  void setEnergyIntensity(EngineNumber newValue);

  /**
   * Get the energy intensity.
   *
   * @return The current energy intensity value
   */
  EngineNumber getEnergyIntensity();

  /**
   * Set the initial charge for a stream.
   *
   * @param stream The stream identifier ('domestic' or 'import')
   * @param newValue The new initial charge value
   * @throws IllegalArgumentException If the stream is not a sales substream
   */
  void setInitialCharge(String stream, EngineNumber newValue);

  /**
   * Get the initial charge for a stream.
   *
   * @param stream The stream identifier ('domestic' or 'import')
   * @return The initial charge value for the stream
   * @throws IllegalArgumentException If the stream is not a sales substream
   */
  EngineNumber getInitialCharge(String stream);

  /**
   * Set the recharge population percentage.
   *
   * @param newValue The new recharge population value
   */
  void setRechargePopulation(EngineNumber newValue);

  /**
   * Get the recharge population percentage.
   *
   * @return The current recharge population value
   */
  EngineNumber getRechargePopulation();

  /**
   * Set the recharge intensity.
   *
   * @param newValue The new recharge intensity value
   */
  void setRechargeIntensity(EngineNumber newValue);

  /**
   * Get the recharge intensity.
   *
   * @return The current recharge intensity value
   */
  EngineNumber getRechargeIntensity();

  /**
   * Set the precharge population percentage.
   *
   * @param newValue The new precharge population value
   */
  void setPrechargePopulation(EngineNumber newValue);

  /**
   * Get the precharge population percentage.
   *
   * @return The current precharge population value
   */
  EngineNumber getPrechargePopulation();

  /**
   * Set the precharge intensity.
   *
   * @param newValue The new precharge intensity value
   */
  void setPrechargeIntensity(EngineNumber newValue);

  /**
   * Get the precharge intensity.
   *
   * @return The current precharge intensity value
   */
  EngineNumber getPrechargeIntensity();

  /**
   * Set the recovery rate percentage.
   *
   * @param newValue The new recovery rate value
   */
  void setRecoveryRate(EngineNumber newValue);

  /**
   * Set the recovery rate percentage for a specific stage.
   *
   * @param newValue The new recovery rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  void setRecoveryRate(EngineNumber newValue, RecoveryStage stage);

  /**
   * Get the recovery rate percentage.
   *
   * @return The current recovery rate value
   */
  EngineNumber getRecoveryRate();

  /**
   * Get the recovery rate percentage for a specific stage.
   *
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current recovery rate value
   */
  EngineNumber getRecoveryRate(RecoveryStage stage);

  /**
   * Set the yield rate percentage for recycling.
   *
   * @param newValue The new yield rate value
   */
  void setYieldRate(EngineNumber newValue);

  /**
   * Set the yield rate percentage for recycling for a specific stage.
   *
   * @param newValue The new yield rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  void setYieldRate(EngineNumber newValue, RecoveryStage stage);

  /**
   * Get the yield rate percentage for recycling.
   *
   * @return The current yield rate value
   */
  EngineNumber getYieldRate();

  /**
   * Get the yield rate percentage for recycling for a specific stage.
   *
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current yield rate value
   */
  EngineNumber getYieldRate(RecoveryStage stage);

  /**
   * Set the induction rate percentage for recycling.
   *
   * @param newValue The new induction rate value
   */
  void setInductionRate(EngineNumber newValue);

  /**
   * Set the induction rate percentage for recycling for a specific stage.
   *
   * @param newValue The new induction rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  void setInductionRate(EngineNumber newValue, RecoveryStage stage);

  /**
   * Get the induction rate percentage for recycling.
   *
   * @return The current induction rate value
   */
  EngineNumber getInductionRate();

  /**
   * Get the induction rate percentage for recycling for a specific stage.
   *
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current induction rate value
   */
  EngineNumber getInductionRate(RecoveryStage stage);

  /**
   * Set the retirement rate percentage.
   *
   * <p>On a live (mutable) parameterization, this accumulates retirement
   * rates across multiple retire commands in the same year to support
   * cumulative retirement behavior. If the resulting retirement rate is
   * negative, it is clamped to zero (no retirement).</p>
   *
   * @param newValue The new retirement rate value to add
   */
  void setRetirementRate(EngineNumber newValue);

  /**
   * Get the retirement rate percentage.
   *
   * @return The current retirement rate value
   */
  EngineNumber getRetirementRate();

  /**
   * Get the retirement base population for cumulative calculations.
   *
   * @return The base population, or empty if not yet captured this step
   */
  Optional<EngineNumber> getRetirementBasePopulation();

  /**
   * Set the retirement base population for cumulative calculations.
   *
   * @param value The base population value
   */
  void setRetirementBasePopulation(EngineNumber value);

  /**
   * Get the applied retirement amount for cumulative calculations.
   *
   * @return The total amount already retired this step
   */
  Optional<EngineNumber> getAppliedRetirementAmount();

  /**
   * Set the applied retirement amount for cumulative calculations.
   *
   * @param value The total amount retired this step
   */
  void setAppliedRetirementAmount(EngineNumber value);

  /**
   * Get the replacement mode for this step's retire commands.
   *
   * @return true if with replacement, false if without replacement
   */
  boolean getHasReplacementThisStep();

  /**
   * Set the replacement mode for this step's retire commands.
   *
   * @param value true for with replacement, false for without replacement
   */
  void setHasReplacementThisStep(boolean value);

  /**
   * Get whether retire has been calculated this step.
   *
   * @return true if retire was calculated, false otherwise
   */
  boolean getRetireCalculatedThisStep();

  /**
   * Set whether retire has been calculated this step.
   *
   * @param calculated true if retire was calculated, false otherwise
   */
  void setRetireCalculatedThisStep(boolean calculated);

  /**
   * Get the recharge base population for cumulative calculations.
   *
   * @return The base population, or empty if not yet captured this step
   */
  Optional<EngineNumber> getRechargeBasePopulation();

  /**
   * Set the recharge base population for cumulative calculations.
   *
   * @param value The base population value
   */
  void setRechargeBasePopulation(EngineNumber value);

  /**
   * Get the applied recharge amount for cumulative calculations.
   *
   * @return The total amount already recharged this step in kg
   */
  Optional<EngineNumber> getAppliedRechargeAmount();

  /**
   * Set the applied recharge amount for cumulative calculations.
   *
   * @param value The total amount recharged this step in kg
   */
  void setAppliedRechargeAmount(EngineNumber value);

  /**
   * Get the precharge base population for cumulative calculations.
   *
   * @return The base population, or empty if not yet captured this year
   */
  Optional<EngineNumber> getPrechargeBasePopulation();

  /**
   * Set the precharge base population for cumulative calculations.
   *
   * @param value The base population value
   */
  void setPrechargeBasePopulation(EngineNumber value);

  /**
   * Get the applied precharge amount for cumulative calculations.
   *
   * @return The total amount already precharged this step in kg
   */
  Optional<EngineNumber> getAppliedPrechargeAmount();

  /**
   * Set the applied precharge amount for cumulative calculations.
   *
   * @param value The total amount precharged this step in kg
   */
  void setAppliedPrechargeAmount(EngineNumber value);

  /**
   * Get whether recycling has been calculated this step.
   *
   * @return true if recycling was calculated, false otherwise
   */
  boolean isRecyclingCalculatedThisStep();

  /**
   * Set whether recycling has been calculated this step.
   *
   * @param calculated true if recycling was calculated, false otherwise
   */
  void setRecyclingCalculatedThisStep(boolean calculated);

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
  void accumulateRecharge(EngineNumber population, EngineNumber intensity);

  /**
   * Accumulate precharge parameters. Sets when not previously set, accumulates otherwise.
   *
   * <p>Multiple calls accumulate rates (addition) and intensities (weighted-average).
   * Population rates are added, intensities are weighted-averaged using absolute values for weights
   * to handle negative adjustments correctly.</p>
   *
   * <p>Weighted average formula: (|rate1| × intensity1 + |rate2| × intensity2) / (|rate1| + |rate2|)</p>
   *
   * @param population The precharge population rate to add
   * @param intensity The precharge intensity for this rate
   */
  void accumulatePrecharge(EngineNumber population, EngineNumber intensity);

  /**
   * Set the last specified value for a stream.
   *
   * <p>This tracks the value and units last used when setting streams
   * to preserve user intent across carry-over years. Percentage-unit values
   * are ignored so they do not impact last recorded values.</p>
   *
   * @param streamName The name of the stream
   * @param value The last specified value with units
   */
  void setLastSpecifiedValue(String streamName, EngineNumber value);

  /**
   * Get the last specified value for a stream.
   *
   * @param streamName The name of the stream
   * @return The last specified value with units, or null if not set
   */
  EngineNumber getLastSpecifiedValue(String streamName);

  /**
   * Check if a stream has a last specified value.
   *
   * @param streamName The name of the stream
   * @return true if the stream has a last specified value, false otherwise
   */
  boolean hasLastSpecifiedValue(String streamName);

  /**
   * Mark a stream as having been enabled (set to non-zero value).
   *
   * @param streamName The name of the stream to mark as enabled
   */
  void markStreamAsEnabled(String streamName);

  /**
   * Check if a stream has ever been enabled (set to non-zero value).
   *
   * @param streamName The name of the stream to check
   * @return true if the stream has been enabled, false otherwise
   */
  boolean hasStreamBeenEnabled(String streamName);

  /**
   * Check if sales intent has been freshly set in the current processing cycle.
   *
   * @return true if sales intent was freshly set, false otherwise
   */
  boolean isSalesIntentFreshlySet();

  /**
   * Set the flag indicating whether sales intent has been freshly set.
   *
   * @param freshlySet true if sales intent was freshly set, false otherwise
   */
  void setSalesIntentFreshlySet(boolean freshlySet);

  /**
   * Reset state at the beginning of a timestep.
   *
   * <p>This resets recovery rate to 0% and induction rate to 100% between steps since
   * recycling programs may cease and should not be expected to continue unchanged, but default
   * induction behavior should return to induced demand (100%).</p>
   */
  void resetStateAtTimestep();

  /**
   * Clear the last specified value in this parameterization.
   *
   * <p>The last specified value tracks the user specified target for a stream such that commands
   * changing those values respect user directives like maintaining units-based tracking with
   * implicit recharge. This method clears that directive so that, for example, a set command can
   * override a prior given value. This, for example, allows the user to switch from units-based
   * to volume-based tracking.</p>
   *
   * <p>This will clear the stream and those dependent upon it. Therefore, clearing sales will
   * also clear substreams domestic and import. Similarly, clearing domestic will clear sales but
   * not import.</p>
   *
   * @param stream The name of the stream like "sales" or "import" in which to clear.
   */
  void clearLastSpecifiedValue(String stream);

  /**
   * Get an immutable snapshot of this instance.
   *
   * <p>The snapshot shares immutable {@link EngineNumber}-typed values by reference
   * but copies mutable containers (maps/sets) and recursively freezes the nested
   * prior equipment bases, so later mutation of this instance does not affect the
   * snapshot.</p>
   *
   * @return An immutable snapshot backed by {@link FrozenStreamParameterization}, or
   *     this same instance if it is already frozen
   */
  StreamParameterization freeze();
}
