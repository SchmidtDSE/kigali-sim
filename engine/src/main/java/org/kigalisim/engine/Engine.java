/**
 * Interface defining the contract for the Montreal Protocol simulation engine.
 *
 * <p>This interface provides methods for managing the engine lifecycle, setting simulation
 * parameters, manipulating substance streams, and retrieving results. It supports operations across
 * different scopes (stanza/application/substance) and time periods. Implementations should handle
 * the core simulation mechanics including substance flows, equipment tracking, and emissions
 * calculations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine;

import java.util.List;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Engine entry-point which maintains and updates simulation state.
 *
 * <p>Coordinator which ensures the state of the simulation as accounted for by
 * {@link org.kigalisim.engine.state.SimulationState} by performing operations on that state within
 * a current scope, delegating to "Executor" and "Recalc" strategies.</p>
 */
public interface Engine {

  /**
   * Get the starting year of the simulation.
   *
   * <p>Get the starting year of the simulation as defined in the simulation stanza or similar.
   * Actual execution will start at this value and increment one timestep (year) at a time until end
   * year such that commands outside this range will not run.</p>
   *
   * @return The start year like 2025.
   */
  int getStartYear();

  /**
   * Get the ending year of the simulation.
   *
   * <p>Get the starting year of the simulation as defined in the simulation stanza or similar.
   * Actual execution will end at this value, incrementing one timestep (year) at a time until end
   * year such that commands outside this range will not run.</p>
   *
   * @return The end year like 2050.
   */
  int getEndYear();

  /**
   * Get the scenario name.
   *
   * <p>Get the name of the scenario currently being run as defined in the input script or similar.
   * This is used to refer to the set of policy stanzas active within a simluation.</p>
   *
   * @return The name of the scenario being run like business as usual.
   */
  String getScenarioName();

  /**
   * Set the scenario name.
   *
   * <p>Specify the name of the scenario currently being run as defined in the input script or
   * similar. This is used to refer to the set of policy stanzas active within a simluation.</p>
   *
   * @param scenarioName The name of the scenario being run like business as usual.
   */
  void setScenarioName(String scenarioName);

  /**
   * Get the trial number.
   *
   * <p>Get the Monte Carlo trial number being run. This is typically a one-indexed positive number
   * and, if no Monte Carlo is specified, is one.</p>
   *
   * @return The trial number of the current run like 1.
   */
  int getTrialNumber();

  /**
   * Set the trial number.
   *
   * <p>Set the Monte Carlo trial number being run. This is typically a one-indexed positive number
   * and, if no Monte Carlo is specified, is one.</p>
   *
   * @param trialNumber The trial number of the current run like 1.
   */
  void setTrialNumber(int trialNumber);

  /**
   * Set the stanza for the engine current scope.
   *
   * <p>Specify the stanza in which the engine is currently running and which can be used to
   * describe the current variable scope. Note that this may be used in some error reporting and
   * should match the input script or similar.</p>
   *
   * @param newStanza The new stanza name such as default for the baseline scenario.
   */
  void setStanza(String newStanza);

  /**
   * Set the application for the engine current scope.
   *
   * <p>Specify the name of the application which is currently being evaluated as indicated in
   * the input script or similar. Note that this may be used in some error reporting and should match
   * the user specified name.</p>
   *
   * @param newApplication The new application name like commercial refrigeration.
   */
  void setApplication(String newApplication);

  /**
   * Set the substance for the engine current scope.
   *
   * <p>Specify the substance that is currently being evaluated by the engine where a substance
   * may appear in multiple applications. Note that this is used in some error reporting and should
   * match the user specified name.</p>
   *
   * @param newSubstance The new substance name like HFC-134a.
   * @param checkValid True if an error should be thrown if the app/substance is not previously
   *     registered or false if it should be registered if not found. Defaults to false.
   */
  void setSubstance(String newSubstance, boolean checkValid);

  /**
   * Set the substance for the engine current scope with default validation behavior.
   *
   * <p>Specify the substance that is currently being evaluated by the engine where a substance
   * may appear in multiple applications. Note that this is used in some error reporting and should
   * match the user specified name.</p>
   *
   * @param newSubstance The new substance name like HFC-134a.
   */
  void setSubstance(String newSubstance);

  /**
   * Get the engine's current scope.
   *
   * <p>Get information about the current "location" in which the Engine is performing
   * calculations. This specifically refers to the set of variables in scope where scope may indicate,
   * for example, which application the Engine is evaluating in order to determine the correct initial
   * charge to use.</p>
   *
   * @return Scope object describing current location in user script or similar.
   */
  Scope getScope();

  /**
   * Get the state getter for converter operations.
   *
   * <p>QubecTalk numbers have both a numeric value and a type such as kilograms. Many values
   * require conversion to alternative units like metric tonnes and some of those conversions such as
   * from number of units of equipment to tCO2e require information about equipment like charge
   * levels. This retrieves an instance which supports querying of information needed to perform
   * conversions specific to equipment, substance, or stream.</p>
   *
   * @return ConverterStateGetter The state getter which can be used for state-dependent
   *     conversions.
   */
  ConverterStateGetter getStateGetter();

  /**
   * Get the unit converter for this engine.
   *
   * <p>QubecTalk numbers have both a numeric value and a type such as kilograms. Many values
   * require conversion to alternative units like metric tonnes. This retrieves an object which can be
   * used with this Engine for those conversions.</p>
   *
   * @return UnitConverter Facade which can perform unit conversions for operations with this
   *     Engine.
   */
  UnitConverter getUnitConverter();

  /**
   * Get the simulation state for this engine.
   *
   * @return SimulationState Accounting object which describes the current state of this siulation
   *     across all applications and substances.
   */
  SimulationState getStreamKeeper();

  /**
   * Increment the engine to simulate the next year.
   */
  void incrementYear();

  /**
   * Get the year that the engine is currently simulating.
   *
   * @return Current year being simulated like 2025.
   */
  int getYear();

  /**
   * Determine if the engine has reached its final year.
   *
   * <p>The simulation goes through one timestep (year) at a time from start year to end year and
   * is considered finished when reaching end year. This evaluates if that terminal state has been
   * reached.</p>
   *
   * @return True if reached the end year and false otherwise.
   */
  boolean getIsDone();

  /**
   * Execute a stream update operation using a StreamUpdate object.
   *
   * @param update The StreamUpdate describing how to update a stream and which contains all
   *     parameters for the operation.
   */
  void executeStreamUpdate(StreamUpdate update);

  /**
   * Set a stream for explicit user operations (applies user-level processing like SetExecutor).
   *
   * @param name The stream name
   * @param value The value to set for the stream
   * @param yearMatcher Optional year matcher for conditional setting
   */
  void setStream(String name, EngineNumber value, Optional<YearMatcher> yearMatcher);

  /**
   * Enable a stream without setting its value.
   *
   * <p>This method marks a stream as enabled, allowing it to be included in distribution
   * calculations for operations like recharge, retire, and recover without having to set an actual
   * value to the stream.</p>
   *
   * @param name The name of the stream to enable
   * @param yearMatcher The year matcher object or empty
   */
  void enable(String name, Optional<YearMatcher> yearMatcher);

  /**
   * Get the stream value for a given application and substance key.
   *
   * <p>Get the current value of a stream where a stream is a type of stock in the stock and flow
   * model such as sales or equipment.</p>
   *
   * @param name The name of the stream to retrieve like sales.
   * @param useKey The key containing application and substance information.
   * @param conversion The conversion specification for units (like mt), or empty for no
   *     conversion.
   * @return The value of the stream, possibly converted like to kg.
   */
  EngineNumber getStream(String name, Optional<UseKey> useKey, Optional<String> conversion);

  /**
   * Get the stream value with default scope and no conversion.
   *
   * <p>Get the current value of a stream where a stream is a type of stock in the stock and flow
   * model such as sales or equipment. This gets the value of the stream automatically converted to
   * kilograms in the current Engine scope.</p>
   *
   * @param name The name of the stream to retrieve like sales.
   * @return The value of the stream in kg.
   */
  EngineNumber getStream(String name);

  /**
   * Get the stream value without any conversion.
   *
   * <p>Get the current value of a stream where a stream is a type of stock in the stock and flow
   * model such as sales or equipment.</p>
   *
   * @param useKey The application and substance name for which the stream should be returned.
   * @param stream The name of the stream to get like recycle.
   * @return The value of the given combination without conversion (as last saved).
   */
  EngineNumber getStreamFor(UseKey useKey, String stream);

  /**
   * Create a user-defined variable in the current scope.
   *
   * @param name The name of the variable to define
   * @throws RuntimeException When trying to define protected variables 'yearsElapsed'
   *     or 'yearAbsolute'
   */
  void defineVariable(String name);

  /**
   * Get the value of a user-defined variable in the current scope.
   *
   * @param name The name of the variable to retrieve
   * @return The value of the variable, or special values for 'yearsElapsed' and 'yearAbsolute' in years.
   */
  EngineNumber getVariable(String name);

  /**
   * Set the value of a variable in the current scope.
   *
   * @param name The name of the variable to set
   * @param value The value to assign to the variable
   * @throws RuntimeException When trying to set protected variables 'yearsElapsed'
   *     or 'yearAbsolute'
   */
  void setVariable(String name, EngineNumber value);

  /**
   * Get the initial charge value for a given stream.
   *
   * <p>Get the current initial charge in the current state for a given stream, typically a sales
   * stream like sales, import, export, domestic. This is looked up in the current scope.</p>
   *
   * @param stream The stream identifier to get the initial charge for like sales.
   * @return The initial charge value for the stream like in kg / unit.
   */
  EngineNumber getInitialCharge(String stream);

  /**
   * Get the initial charge for a specific application and substance.
   *
   * <p>Get the current initial charge in the current state for a given stream, typically a sales
   * stream like sales, import, export, domestic.</p>
   *
   * @param key Application and substance for which initial charge is requested
   * @param stream The stream in which the initial charge is requested and must be realized
   * @return The initial charge for the stream in the given application and substance like in
   *     kg / unit.
   */
  EngineNumber getRawInitialChargeFor(UseKey key, String stream);

  /**
   * Set the initial charge for a stream.
   *
   * <p>Set the initial charge to use for a stream in the current scope. This is typically a sales
   * stream like sales, import, export, domestic. This operation will only be applied if the year
   * matcher passes, otherwise a no-op.</p>
   *
   * @param value The initial charge value to set like 5 kg / unit.
   * @param stream The stream identifier to set the initial charge for like sales.
   * @param yearMatcher Matcher to determine if the change applies to current year. If not matching
   *     the current year, will be a no-op.
   */
  void setInitialCharge(EngineNumber value, String stream, YearMatcher yearMatcher);

  /**
   * Get the recharge volume for the current application and substance.
   *
   * <p>Get the servicing volume (across all recharge commands) within the current scope in the
   * units last saved.</p>
   *
   * @return The recharge volume value like percent.
   */
  EngineNumber getRechargeVolume();

  /**
   * Get the recharge intensity for the current application and substance.
   *
   * <p>Get the servicing intensity (across all recharge commands) within the current scope in the
   * units last saved.</p>
   *
   * @return The recharge intensity value in kg per unit.
   */
  EngineNumber getRechargeIntensity();

  /**
   * Set recharge parameters for the current application and substance.
   *
   * <p>Recharge represents the substance needed to service existing equipment due to
   * leakage, maintenance, or repair. This method configures both the percentage of equipment
   * requiring recharge (volume) and the amount of substance needed per unit (intensity).</p>
   *
   * <p>Note that, when users specify sales in equipment units (e.g., "800 units"), they indicate how many new equipment units should be
   * sold. This recharge method must calculate substance needed for exiting equipment and add it on
   * top. For example, if a user specifies "set import to 800 units during year 2025" and provides no
   * specification for 2026, the carry over mechanism will automatically continue 800 new units in
   * 2026, while recalculating and adding the appropriate recharge volume based on the growing
   * equipment population.</p>
   *
   * @param volume The recharge volume to set (percentage of equipment requiring recharge)
   * @param intensity The recharge intensity to set (substance amount per unit recharged)
   * @param yearMatcher Matcher to determine if the change applies to current year
   */
  void recharge(EngineNumber volume, EngineNumber intensity, YearMatcher yearMatcher);

  /**
   * Set retirement rate for the current application and substance.
   *
   * <p>Set the retirement (also called) scrap rate within the current scope. This is the hazard
   * rate rate to use for retirement and can be specified in any units but are typically given in
   * percentages (probability). Note that these may also be the result of a stateful formula like for
   * users which have an age-dependent hazard for their survival analysis.</p>
   *
   * @param amount The retirement rate to set in the current scope.
   * @param yearMatcher Matcher to determine if the change applies to current year and, if not
   *     matching the current year, this is a no-op.
   */
  void retire(EngineNumber amount, YearMatcher yearMatcher);

  /**
   * Get the retirement rate for the current application and substance.
   *
   * <p>Get the current hazard rate for equipment retirment (also called scrap in some
   * communities).
   *
   * @return The retirement rate value like as percentage.
   */
  EngineNumber getRetirementRate();

  /**
   * Set recycling parameters for the current application and substance.
   *
   * <p>Recycling allows for recovery of some amount of substance to be reused, potentially in
   * place of virgin material. However, this is controlled through an induction (induced demand)
   * parameter. Note that some may use recycling with 0% yield to model destruction in which substance
   * is prevented from emitting but is not salvaged for reuse.</p>
   *
   * @param recoveryWithUnits The recovery rate, typically in percentage or probability.
   * @param yieldWithUnits The yield rate indicating how much recovered is actually reused.
   * @param yearMatcher Matcher to determine if the change applies to current year and, if not
   *     matching the current year, turns this into a no-op.
   * @param stage The recovery stage (EOL for end of life or RECHARGE for servicing) at which the
   *     substance is captured.
   */
  void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
      YearMatcher yearMatcher, RecoveryStage stage);

  /**
   * Set the induction rate for recycling operations.
   *
   * <p>This parameter allows for modeling induced demand, an economic effect in which increased
   * supply (in our case through recycling / secondary production) actually causes demand to also
   * increase such that one kg of secondary material does not fully reduce or offset one kg of virgin
   * production.</p>
   *
   * <div>Induction rate determines how recycled material affects virgin production:
   *
   *   <ul>
   *     <li>100% induction (default): Recycled material does not displace virgin material,
   *         adding to total supply (induced demand behavior)</li>
   *     <li>0% induction: Recycled material fully displaces virgin material, maintaining
   *         steady population (displacement behavior)</li>
   *     <li>Partial induction: Mixed behavior with proportional effects</li>
   *   </ul>
   * </div>
   *
   * @param inductionRate The induction rate as an EngineNumber with percentage units,
   *     or null for default behavior (100% induced demand).
   * @param stage The recovery stage (EOL or RECHARGE) for which to set the induction rate.
   */
  void setInductionRate(EngineNumber inductionRate, RecoveryStage stage);

  /**
   * Reset the induction rate to default behavior for recycling operations.
   *
   * <p>This method sets the induction rate to 100%, which represents default induced demand
   * behavior where recycled material adds to total supply rather than displacing virgin material.
   * This is the recommended setting when users are uncertain about induction effects.</p>
   *
   * <div>Induction rate determines how recycled material affects virgin production:
   *
   *   <ul>
   *     <li>100% induction (default): Recycled material does not displace virgin material,
   *         adding to total supply (induced demand behavior)</li>
   *     <li>0% induction: Recycled material fully displaces virgin material, maintaining
   *         steady population (displacement behavior)</li>
   *     <li>Partial induction: Mixed behavior with proportional effects</li>
   *   </ul>
   * </div>
   *
   * @param stage The recovery stage (EOL or RECHARGE) to reset the induction rate for
   */
  void resetInductionRate(RecoveryStage stage);

  /**
   * Set GHG equivalency (GWP - Global Warming Potential) for the current application and substance.
   *
   * <p>Set GHG equivalency (GWP - Global Warming Potential) for the current application and
   * substance as defined by the current scope. Will only execute if the year matcher matches the
   * current year. Otherwise, this is a no-op.</p>
   *
   * <p>Note that this is intended only for direct emissions though useres may calculate indirect
   * or secondary emissions through energy mix outside of Kigali Sim.</p>
   *
   * @param amount The GHG intensity value to set (typically in kgCO2e / kg).
   * @param yearMatcher Matcher to determine if the change applies to current year.
   */
  void equals(EngineNumber amount, YearMatcher yearMatcher);

  /**
   * Get the GHG intensity (GWP - Global Warming Potential) associated with a substance.
   *
   * <p>Set GHG equivalency (GWP - Global Warming Potential) for the specified application and
   * substance (regradless of current scope). Will only execute if the year matcher matches the
   * current year. Otherwise, this is a no-op.</p>
   *
   * <p>Note that this is intended only for direct emissions though useres may calculate indirect
   * or secondary emissions through energy mix outside of Kigali Sim.</p>
   *
   * @param useKey The UseKey containing application and substance information
   * @return The GHG intensity value associated with the given combination in tCO2e per kg.
   */
  EngineNumber getGhgIntensity(UseKey useKey);

  /**
   * Retrieve the tCO2e intensity (GWP - Global Warming Potential) for the current application and substance.
   *
   * <p>Retrieve the primary GHG intensity (GWP) for the current application and substance where this value
   * is not expected to cover secondary or indirect emissions. That said, users may calculate those
   * additional emissions through energy mix data from outside Kigali Sim.</p>
   *
   * @return The GHG intensity value with volume normalized GHG in tCO2e per kg.
   */
  EngineNumber getEqualsGhgIntensity();

  /**
   * Retrieve the tCO2e intensity (GWP - Global Warming Potential) for the given UseKey.
   *
   * <p>Retrieve the primary GHG intensity (GWP) for the given application and substance where this value
   * is not expected to cover secondary or indirect emissions. That said, users may calculate those
   * additional emissions through energy mix data from outside Kigali Sim.</p>
   *
   * @param useKey The UseKey containing application and substance information
   * @return The GHG intensity value with volume normalized GHG in tCO2e per kg.
   */
  EngineNumber getEqualsGhgIntensityFor(UseKey useKey);

  /**
   * Retrieve the energy intensity for the current application and substance.
   *
   * @return The energy intensity value, typically in kwh / unit (yearly).
   */
  EngineNumber getEqualsEnergyIntensity();

  /**
   * Retrieve the energy intensity for the given UseKey.
   *
   * @param useKey The UseKey containing application and substance information for the application
   *     and substance pair for which energy intensity is to be returned.
   * @return The energy intensity value, typically in kwh / unit (yearly).
   */
  EngineNumber getEqualsEnergyIntensityFor(UseKey useKey);

  /**
   * Change a stream value by a delta amount.
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param useKey The key containing application and substance information
   */
  void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher, UseKey useKey);

  /**
   * Change a stream value by a delta amount with default scope.
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param yearMatcher Matcher to determine if the change applies to current year
   */
  void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher);

  /**
   * Cap a stream at a maximum value.
   *
   * @param stream The stream identifier to cap
   * @param amount The maximum value to cap at
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param displaceTarget Optional target for displaced amount
   * @param displacementType The type of displacement (EQUIVALENT, BY_VOLUME, or BY_UNITS)
   */
  void cap(String stream, EngineNumber amount, YearMatcher yearMatcher, String displaceTarget,
      org.kigalisim.lang.operation.CapOperation.LimitDisplacementType displacementType);

  /**
   * Set a minimum floor value for a stream.
   *
   * @param stream The stream identifier to set floor for
   * @param amount The minimum value to set as floor
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param displaceTarget Optional target for displaced amount
   * @param displacementType The type of displacement (EQUIVALENT, BY_VOLUME, or BY_UNITS)
   */
  void floor(String stream, EngineNumber amount, YearMatcher yearMatcher, String displaceTarget,
      org.kigalisim.lang.operation.FloorOperation.LimitDisplacementType displacementType);

  /**
   * Replace an amount from one substance with another.
   *
   * @param amountRaw The amount to replace
   * @param stream The stream identifier to modify
   * @param destinationSubstance The substance to replace with
   * @param yearMatcher Matcher to determine if the change applies to current year
   */
  void replace(EngineNumber amountRaw, String stream, String destinationSubstance,
      YearMatcher yearMatcher);

  /**
   * Get the results for all registered substances.
   *
   * @return List of results for each registered substance
   */
  List<EngineResult> getResults();

  /**
   * Gets whether recalc optimizations are enabled.
   *
   * <p>When true, certain redundant recalculation steps are skipped for
   * performance. When false, all recalc operations are performed for maximum accuracy
   * verification.</p>
   *
   * @return true if optimizations are enabled, false otherwise
   */
  boolean getOptimizeRecalcs();
}
