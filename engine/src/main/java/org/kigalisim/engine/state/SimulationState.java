/**
 * Interface for the state for a single scenario within a single trial.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Interface for the state for a single scenario within a single trial.
 *
 * <p>Implemented by {@link MutableSimulationState} for live, mutable simulation
 * state and by {@link FrozenSimulationState} for immutable snapshots captured for
 * prior-year lookups (see {@link #getAtPrior(int)}).</p>
 */
public interface SimulationState {

  /**
   * The zero value used to initialize volume-denominated streams (kg).
   */
  EngineNumber ZERO_VOLUME = new EngineNumber(BigDecimal.ZERO, "kg");

  /**
   * Get all registered substance-application pairs.
   *
   * @return Array of substance identifiers
   */
  List<SubstanceInApplicationId> getRegisteredSubstances();

  /**
   * Check if a substance exists for a key.
   *
   * @param useKey The key containing application and substance
   * @return true if the substance exists for the key
   */
  boolean hasSubstance(UseKey useKey);

  /**
   * Ensure a substance exists for a key, creating it if needed.
   *
   * @param useKey The key containing application and substance
   */
  void ensureSubstance(UseKey useKey);

  /**
   * Set a stream using pre-computed stream data.
   *
   * <p>This method replaces setStream, setOutcomeStream, and setSalesStream
   * with a unified interface that accepts pre-computed stream values.
   * The SimulationStateUpdate object encapsulates all necessary parameters including distribution
   * logic and recycling behavior.</p>
   *
   * <p>This method provides clear architectural separation between calculation
   * instructions (StreamUpdate) and pre-computed results (SimulationStateUpdate).</p>
   *
   * @param stateUpdate Pre-computed stream data with all parameters
   */
  void update(SimulationStateUpdate stateUpdate);

  /**
   * Get the value of a specific stream using key. Uses current year.
   *
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @return The stream value
   */
  EngineNumber getStream(UseKey useKey, String name);

  /**
   * Get the value of a specific stream using key from this or prior year.
   *
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @param priorYear If true, returns prior year value if available, returns current year if no
   *     prior year exists.
   * @return The stream value
   */
  EngineNumber getStream(UseKey useKey, String name, boolean priorYear);

  /**
   * Check if a stream exists for a key.
   *
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @return true if the stream exists
   */
  boolean isKnownStream(UseKey useKey, String name);

  /**
   * Get the induction stream value for a specific recovery stage. Uses current year.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The induction stream value in kg
   */
  EngineNumber getInductionStream(UseKey useKey, RecoveryStage stage);

  /**
   * Get the induction stream value for a specific recovery stage.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @param priorYear If true, returns prior year value if available, returns current year if no
   *     prior year exists.
   * @return The induction stream value in kg
   */
  EngineNumber getInductionStream(UseKey useKey, RecoveryStage stage, boolean priorYear);

  /**
   * Get total induction across all stages. Uses current year.
   *
   * @param useKey The key containing application and substance
   * @return Total induction in kg
   */
  EngineNumber getTotalInductionStream(UseKey useKey);

  /**
   * Get total induction across all stages.
   *
   * @param useKey The key containing application and substance
   * @param priorYear If true, returns prior year value if available, returns current year if no
   *     prior year exists.
   * @return Total induction in kg
   */
  EngineNumber getTotalInductionStream(UseKey useKey, boolean priorYear);

  /**
   * Get a sales stream distribution for the given substance/application.
   *
   * <p>This method centralizes the logic for creating sales distributions by getting
   * the current domestic and import values, determining their enabled status, and building an
   * appropriate distribution using the builder pattern.
   * Exports are excluded for backward compatibility.</p>
   *
   * @param useKey The key containing application and substance
   * @return A SalesStreamDistribution with appropriate percentages
   */
  SalesStreamDistribution getDistribution(UseKey useKey);

  /**
   * Get a sales stream distribution for the given substance/application.
   *
   * <p>This method centralizes the logic for creating sales distributions by getting
   * the current domestic, import, and optionally export values, determining their enabled status, and
   * building an appropriate distribution using the builder pattern.</p>
   *
   * @param useKey The key containing application and substance
   * @param includeExports Whether to include exports in the distribution calculation
   * @return A SalesStreamDistribution with appropriate percentages
   */
  SalesStreamDistribution getDistribution(UseKey useKey, boolean includeExports);

  /**
   * Check if any sales streams have been enabled for the given substance/application.
   *
   * @param useKey The key containing application and substance
   * @return True if any of domestic, import, or export streams are enabled
   */
  boolean hasStreamsEnabled(UseKey useKey);

  /**
   * Get the current year for this simulation state.
   *
   * @return The current year
   */
  int getCurrentYear();

  /**
   * Set the current year for this simulation state.
   *
   * @param year The current year
   */
  void setCurrentYear(int year);

  /**
   * Get the simulation state from N years ago.
   *
   * <p>Returns Optional.empty() if years is negative. Returns Optional.of(this) if years is 0.
   * Otherwise traverses the linked list of prior states: getAtPrior(1) returns the state from
   * the previous year, getAtPrior(2) returns the state from two years ago, and so on.
   * If the linked list is exhausted before reaching the requested year, returns Optional.empty().</p>
   *
   * @param years The number of years to look back
   * @return Optional.of(this) if years is 0, Optional.of(priorState) if years is 1,
   *     traversing the linked list for larger values, or Optional.empty() if not available
   */
  Optional<SimulationState> getAtPrior(int years);

  /**
   * Increment the year, updating populations and resetting internal params.
   *
   * <p>Freezes the current state before modifications to build the linked list of
   * prior states, enabling lookback N years via getAtPrior().</p>
   */
  void incrementYear();

  /**
   * Set the greenhouse gas intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new GHG intensity value
   */
  void setGhgIntensity(UseKey useKey, EngineNumber newValue);

  /**
   * Set the energy intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new energy intensity value
   */
  void setEnergyIntensity(UseKey useKey, EngineNumber newValue);

  /**
   * Get the greenhouse gas intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @return The GHG intensity value
   */
  EngineNumber getGhgIntensity(UseKey useKey);

  /**
   * Get the energy intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @return The energy intensity value
   */
  EngineNumber getEnergyIntensity(UseKey useKey);

  /**
   * Set the initial charge for a key's stream.
   *
   * @param useKey The key containing application and substance
   * @param substream The stream identifier ('domestic' or 'import')
   * @param newValue The new initial charge value
   */
  void setInitialCharge(UseKey useKey, String substream, EngineNumber newValue);

  /**
   * Get the initial charge for a key.
   *
   * @param useKey The key containing application and substance
   * @param substream The substream name
   * @return The initial charge value
   */
  EngineNumber getInitialCharge(UseKey useKey, String substream);

  /**
   * Set the recharge population percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new recharge population value
   */
  void setRechargePopulation(UseKey useKey, EngineNumber newValue);

  /**
   * Get the recharge population percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current recharge population value
   */
  EngineNumber getRechargePopulation(UseKey useKey);

  /**
   * Set the recharge intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new recharge intensity value
   */
  void setRechargeIntensity(UseKey useKey, EngineNumber newValue);

  /**
   * Get the recharge intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current recharge intensity value
   */
  EngineNumber getRechargeIntensity(UseKey useKey);

  /**
   * Accumulate recharge parameters. Sets when not previously set, accumulates otherwise.
   *
   * <p>Multiple calls accumulate rates (addition) and intensities (weighted-average).
   * Rates add linearly and intensities use weighted-average with absolute value weights to handle
   * negative adjustments correctly.</p>
   *
   * @param useKey The key containing application and substance
   * @param population The recharge population rate to add
   * @param intensity The recharge intensity for this rate
   */
  void accumulateRecharge(UseKey useKey, EngineNumber population, EngineNumber intensity);

  /**
   * Get the recharge base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The base population, or null if not yet captured this year
   */
  Optional<EngineNumber> getRechargeBasePopulation(UseKey useKey);

  /**
   * Set the recharge base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The base population value
   */
  void setRechargeBasePopulation(UseKey useKey, EngineNumber value);

  /**
   * Get the applied recharge amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The total amount already recharged this year in kg
   */
  Optional<EngineNumber> getAppliedRechargeAmount(UseKey useKey);

  /**
   * Set the applied recharge amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The total amount recharged this year in kg
   */
  void setAppliedRechargeAmount(UseKey useKey, EngineNumber value);

  /**
   * Set the precharge population percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new precharge population value
   */
  void setPrechargePopulation(UseKey useKey, EngineNumber newValue);

  /**
   * Get the precharge population percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current precharge population value
   */
  EngineNumber getPrechargePopulation(UseKey useKey);

  /**
   * Set the precharge intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new precharge intensity value
   */
  void setPrechargeIntensity(UseKey useKey, EngineNumber newValue);

  /**
   * Get the precharge intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current precharge intensity value
   */
  EngineNumber getPrechargeIntensity(UseKey useKey);

  /**
   * Accumulate precharge parameters. Sets when not previously set, accumulates otherwise.
   *
   * @param useKey The key containing application and substance
   * @param population The precharge population rate to add
   * @param intensity The precharge intensity for this rate
   */
  void accumulatePrecharge(UseKey useKey, EngineNumber population, EngineNumber intensity);

  /**
   * Get the precharge base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The base population, or null if not yet captured this year
   */
  Optional<EngineNumber> getPrechargeBasePopulation(UseKey useKey);

  /**
   * Set the precharge base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The base population value
   */
  void setPrechargeBasePopulation(UseKey useKey, EngineNumber value);

  /**
   * Get the applied precharge amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The total amount already precharged this year in kg
   */
  Optional<EngineNumber> getAppliedPrechargeAmount(UseKey useKey);

  /**
   * Set the applied precharge amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The total amount precharged this year in kg
   */
  void setAppliedPrechargeAmount(UseKey useKey, EngineNumber value);

  /**
   * Get whether recycling has been calculated this step.
   *
   * @param useKey The key containing application and substance
   * @return true if recycling was calculated, false otherwise
   */
  boolean isRecyclingCalculatedThisStep(UseKey useKey);

  /**
   * Set whether recycling has been calculated this step.
   *
   * @param useKey The key containing application and substance
   * @param calculated true if recycling was calculated, false otherwise
   */
  void setRecyclingCalculatedThisStep(UseKey useKey, boolean calculated);

  /**
   * Set the recovery rate percentage for a key.
   *
   * <div>If a recovery rate is already set, this method implements additive recycling:
   * <ul>
   *   <li>Recovery rates are added together.</li>
   *   <li>Both rates are converted to percentage units before addition.</li>
   *   <li>The combined rate is stored as a percentage.</li>
   * </ul>
   * </div>
   *
   * @param useKey The key containing application and substance
   * @param newValue The new recovery rate value
   */
  void setRecoveryRate(UseKey useKey, EngineNumber newValue);

  /**
   * Set the recovery rate percentage for a key with a specific stage.
   *
   * <div>Implements additive behavior for multiple recovery commands on the same stage:
   * <ul>
   *   <li>When a recovery rate is already set for this stage, the new rate is added to the existing
   *   one.</li>
   *   <li>The first recovery rate for a timestep is set directly without addition.</li>
   * </ul>
   * </div>
   *
   * @param useKey The key containing application and substance
   * @param newValue The new recovery rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  void setRecoveryRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage);

  /**
   * Get the recovery rate percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current recovery rate value
   */
  EngineNumber getRecoveryRate(UseKey useKey);

  /**
   * Get the recovery rate percentage for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current recovery rate value
   */
  EngineNumber getRecoveryRate(UseKey useKey, RecoveryStage stage);

  /**
   * Set the yield rate percentage for recycling for a key.
   *
   * <p>Convenience method that sets the yield rate for the RECHARGE recovery stage.
   * Delegates to {@link #setYieldRate(UseKey, EngineNumber, RecoveryStage)} with
   * RecoveryStage.RECHARGE.</p>
   *
   * @param useKey The key containing application and substance
   * @param newValue The new yield rate value
   * @see #setYieldRate(UseKey, EngineNumber, RecoveryStage)
   */
  void setYieldRate(UseKey useKey, EngineNumber newValue);

  /**
   * Set the yield rate percentage for recycling for a key with a specific stage.
   *
   * <p>When an existing yield rate is set for this stage, combines them using a weighted average
   * approach that uses equal weighting, which is a reasonable approximation for efficiency rates.</p>
   *
   * @param useKey The key containing application and substance
   * @param newValue The new yield rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  void setYieldRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage);

  /**
   * Get the yield rate percentage for recycling for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current yield rate value
   */
  EngineNumber getYieldRate(UseKey useKey);

  /**
   * Get the yield rate percentage for recycling for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current yield rate value
   */
  EngineNumber getYieldRate(UseKey useKey, RecoveryStage stage);

  /**
   * Set the induction rate percentage for recycling for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new induction rate value
   */
  void setInductionRate(UseKey useKey, EngineNumber newValue);

  /**
   * Set the induction rate percentage for recycling for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new induction rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  void setInductionRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage);

  /**
   * Get the induction rate percentage for recycling for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current induction rate value
   */
  EngineNumber getInductionRate(UseKey useKey);

  /**
   * Get the induction rate percentage for recycling for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current induction rate value
   */
  EngineNumber getInductionRate(UseKey useKey, RecoveryStage stage);

  /**
   * Set the retirement rate percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new retirement rate value
   */
  void setRetirementRate(UseKey useKey, EngineNumber newValue);

  /**
   * Get the retirement rate percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current retirement rate value
   */
  EngineNumber getRetirementRate(UseKey useKey);

  /**
   * Get the retirement base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The base population, or null if not yet captured
   */
  Optional<EngineNumber> getRetirementBasePopulation(UseKey useKey);

  /**
   * Set the retirement base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The base population value
   */
  void setRetirementBasePopulation(UseKey useKey, EngineNumber value);

  /**
   * Get the applied retirement amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The total amount already retired this year
   */
  Optional<EngineNumber> getAppliedRetirementAmount(UseKey useKey);

  /**
   * Set the applied retirement amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The total amount retired this year
   */
  void setAppliedRetirementAmount(UseKey useKey, EngineNumber value);

  /**
   * Get the replacement mode for retire commands this step.
   *
   * @param useKey The key containing application and substance
   * @return null if no retire yet, true if with replacement, false if without replacement
   */
  boolean getHasReplacementThisStep(UseKey useKey);

  /**
   * Set the replacement mode for retire commands this step.
   *
   * @param useKey The key containing application and substance
   * @param value true for with replacement, false for without replacement
   */
  void setHasReplacementThisStep(UseKey useKey, boolean value);

  /**
   * Get whether retire has been calculated this step.
   *
   * @param useKey The key containing application and substance
   * @return true if retire was calculated, false otherwise
   */
  boolean getRetireCalculatedThisStep(UseKey useKey);

  /**
   * Set whether retire has been calculated this step.
   *
   * @param useKey The key containing application and substance
   * @param calculated true if retire was calculated, false otherwise
   */
  void setRetireCalculatedThisStep(UseKey useKey, boolean calculated);

  /**
   * Tracks the last specified value for sales-related streams.
   *
   * <p>This method preserves user intent across carry-over years by storing the
   * units and values that were explicitly specified by the user. This is essential for maintaining
   * correct behavior when sales values carry over to subsequent years, particularly for unit-based
   * specifications where recharge calculations need to be applied consistently.</p>
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream (e.g., "sales", "domestic", "import")
   * @param value The value being specified with its units
   */
  void setLastSpecifiedValue(UseKey useKey, String streamName, EngineNumber value);

  /**
   * Get the last specified value for a stream.
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream
   * @return The last specified value with units, or null if not set
   */
  EngineNumber getLastSpecifiedValue(UseKey useKey, String streamName);

  /**
   * Check if a stream has a last specified value.
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream
   * @return true if the stream has a last specified value, false otherwise
   */
  boolean hasLastSpecifiedValue(UseKey useKey, String streamName);

  /**
   * Check if sales intent has been freshly set for the given scope.
   *
   * @param useKey The key containing application and substance
   * @return true if sales intent was freshly set, false otherwise
   */
  boolean isSalesIntentFreshlySet(UseKey useKey);

  /**
   * Reset the sales intent flag for the given scope.
   *
   * @param useKey The key containing application and substance
   */
  void resetSalesIntentFlag(UseKey useKey);

  /**
   * Check if a stream has ever been enabled (set to non-zero value).
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream to check
   * @return true if the stream has been enabled, false otherwise
   */
  boolean hasStreamBeenEnabled(UseKey useKey, String streamName);

  /**
   * Mark a stream as having been enabled (set to non-zero value).
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream to mark as enabled
   */
  void markStreamAsEnabled(UseKey useKey, String streamName);

  /**
   * Clear the last specified value in this parameterization.
   *
   * <p>The last specified value tracks the user specified target for a stream such that commands
   * changing those values respect user directives like maintaining units-based tracking with
   * implicit recharge. This method clears that directive so that, for example, a set command can
   * override a prior given value. This, for example, allows the user to switch from units-based
   * to volume-based tracking.</p>
   *
   * @param useKey The substance / application pair in which to clear last specified value.
   * @param stream The name of the stream like "sales" or "import" in which to clear.
   */
  void clearLastSpecifiedValue(UseKey useKey, String stream);

  /**
   * Get an immutable snapshot of this instance.
   *
   * <p>Container fields (maps) are copied so the mutable original can keep mutating
   * its own containers independently. Immutable values ({@link EngineNumber}) are
   * shared by reference. Each substance's {@link StreamParameterization} is frozen
   * recursively, and {@code priorState} is captured by reference (the prior chain is
   * already frozen, so sharing is safe and O(1) per year).</p>
   *
   * @return An immutable snapshot backed by {@link FrozenSimulationState}, or this
   *     same instance if it is already frozen
   */
  SimulationState freeze();
}
