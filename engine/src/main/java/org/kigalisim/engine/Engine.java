/**
 * Interface for simulation engine that underpins simulations regardless of interface.
 *
 * <p>This interface defines the contract for the Montreal Protocol simulation engine,
 * providing methods for managing engine lifecycle, scope operations, stream operations,
 * and various calculation functions. Implementations should handle the core simulation
 * mechanics including substance flows, equipment tracking, and emissions calculations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine;

import java.util.List;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Interface defining the contract for the Montreal Protocol simulation engine.
 *
 * <p>This interface provides methods for managing the engine lifecycle, setting simulation
 * parameters, manipulating substance streams, and retrieving results. It supports operations
 * across different scopes (stanza/application/substance) and time periods.</p>
 */
public interface Engine {

  /**
   * Get the starting year of the simulation.
   *
   * @return The start year
   */
  int getStartYear();

  /**
   * Get the ending year of the simulation.
   *
   * @return The end year
   */
  int getEndYear();

  /**
   * Get the scenario name.
   *
   * @return The name of the scenario being run
   */
  String getScenarioName();

  /**
   * Set the scenario name.
   *
   * @param scenarioName The name of the scenario being run
   */
  void setScenarioName(String scenarioName);

  /**
   * Get the trial number.
   *
   * @return The trial number of the current run
   */
  int getTrialNumber();

  /**
   * Set the trial number.
   *
   * @param trialNumber The trial number of the current run
   */
  void setTrialNumber(int trialNumber);

  /**
   * Set the stanza for the engine current scope.
   *
   * @param newStanza The new stanza name
   */
  void setStanza(String newStanza);

  /**
   * Set the application for the engine current scope.
   *
   * @param newApplication The new application name
   */
  void setApplication(String newApplication);

  /**
   * Set the substance for the engine current scope.
   *
   * @param newSubstance The new substance name
   * @param checkValid True if an error should be thrown if the app/substance is not previously
   *     registered or false if it should be registered if not found. Defaults to false.
   */
  void setSubstance(String newSubstance, Boolean checkValid);

  /**
   * Set the substance for the engine current scope with default validation behavior.
   *
   * @param newSubstance The new substance name
   */
  void setSubstance(String newSubstance);

  /**
   * Get the engine's current scope.
   *
   * @return Scope object
   */
  Scope getScope();

  /**
   * Increment the engine to simulate the next year.
   */
  void incrementYear();

  /**
   * Get the year that the engine is currently simulating.
   *
   * @return Current year simulating
   */
  int getYear();

  /**
   * Determine if the engine has reached its final year.
   *
   * @return True if reached the end year and false otherwise
   */
  boolean getIsDone();

  /**
   * Set the value of a stream.
   *
   * @param name The name of the stream to set
   * @param value The value to set for the stream
   * @param yearMatcher The year matcher object to determine if setting the stream applies to the
   *     current year, or empty. No-op if the year matcher is not satisfied.
   * @param key The scope in which the stream is being set. Uses default scope if empty.
   * @param propagateChanges Specifies if changes should propagate to other components.
   *     Defaults to true.
   * @param unitsToRecord Optional units to record instead of using value.getUnits().
   *     Used when the original user-specified units differ from the converted units being set.
   */
  void setStreamFor(String name, EngineNumber value, Optional<YearMatcher> yearMatcher, Optional<UseKey> key,
                    boolean propagateChanges, Optional<String> unitsToRecord);

  /**
   * Set the value of a stream with default parameters.
   *
   * @param name The name of the stream to set
   * @param value The value to set for the stream
   * @param yearMatcher The year matcher object or empty
   */
  void setStream(String name, EngineNumber value, Optional<YearMatcher> yearMatcher);

  /**
   * Enable a stream without setting its value.
   *
   * <p>This method marks a stream as enabled, allowing it to be included in distribution
   * calculations for operations like recharge, retire, and recover without having to
   * set an actual value to the stream.</p>
   *
   * @param name The name of the stream to enable
   * @param yearMatcher The year matcher object or empty
   */
  void enable(String name, Optional<YearMatcher> yearMatcher);

  /**
   * Get the stream value for a given application and substance key.
   *
   * @param name The name of the stream to retrieve
   * @param useKey The key containing application and substance information
   * @param conversion The conversion specification for units, or empty for no conversion
   * @return The value of the stream, possibly converted
   */
  EngineNumber getStream(String name, Optional<UseKey> useKey, Optional<String> conversion);

  /**
   * Get the stream value with default scope and no conversion.
   *
   * @param name The name of the stream to retrieve
   * @return The value of the stream
   */
  EngineNumber getStream(String name);

  /**
   * Get the stream value without any conversion.
   *
   * @param useKey The application and substance name for which the stream should be returned.
   * @param stream The name of the stream to get like recycle
   * @return The value of the given combination without conversion
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
   * @return The value of the variable, or special values for 'yearsElapsed' and 'yearAbsolute'
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
   * @param stream The stream identifier to get the initial charge for
   * @return The initial charge value for the stream
   */
  EngineNumber getInitialCharge(String stream);

  /**
   * Get the initial charge for a specific application and substance.
   *
   * @param key Application and substance for which initial charge is requested
   * @param stream The stream in which the initial charge is requested and must be realized
   * @return The initial charge for the stream in the given application and substance
   */
  EngineNumber getRawInitialChargeFor(UseKey key, String stream);

  /**
   * Set the initial charge for a stream.
   *
   * @param value The initial charge value to set
   * @param stream The stream identifier to set the initial charge for
   * @param yearMatcher Matcher to determine if the change applies to current year
   */
  void setInitialCharge(EngineNumber value, String stream, YearMatcher yearMatcher);

  /**
   * Get the recharge volume for the current application and substance.
   *
   * @return The recharge volume value
   */
  EngineNumber getRechargeVolume();

  /**
   * Get the recharge intensity for the current application and substance.
   *
   * @return The recharge intensity value
   */
  EngineNumber getRechargeIntensity();

  /**
   * Set recharge parameters for the current application and substance.
   *
   * @param volume The recharge volume to set
   * @param intensity The recharge intensity to set
   * @param yearMatcher Matcher to determine if the change applies to current year
   */
  void recharge(EngineNumber volume, EngineNumber intensity, YearMatcher yearMatcher);

  /**
   * Set retirement rate for the current application and substance.
   *
   * @param amount The retirement rate to set
   * @param yearMatcher Matcher to determine if the change applies to current year
   */
  void retire(EngineNumber amount, YearMatcher yearMatcher);

  /**
   * Get the retirement rate for the current application and substance.
   *
   * @return The retirement rate value
   */
  EngineNumber getRetirementRate();

  /**
   * Set recycling parameters for the current application and substance.
   *
   * @param recoveryWithUnits The recovery rate
   * @param yieldWithUnits The yield rate
   * @param yearMatcher Matcher to determine if the change applies to current year
   */
  void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
      YearMatcher yearMatcher);

  /**
   * Recycle or recover a substance with displacement to another stream.
   *
   * @param recoveryWithUnits The recovery rate
   * @param yieldWithUnits The yield rate
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param displacementTarget The stream or substance to displace (reduce)
   */
  void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
      YearMatcher yearMatcher, String displacementTarget);

  /**
   * Set GHG equivalency for the current application and substance.
   *
   * @param amount The GHG intensity value to set
   * @param yearMatcher Matcher to determine if the change applies to current year
   */
  void equals(EngineNumber amount, YearMatcher yearMatcher);

  /**
   * Get the GHG intensity associated with a substance.
   *
   * @param useKey The UseKey containing application and substance information
   * @return The GHG intensity value associated with the given combination
   */
  EngineNumber getGhgIntensity(UseKey useKey);

  /**
   * Retrieve the tCO2e intensity for the current application and substance.
   *
   * @return The GHG intensity value with volume normalized GHG
   */
  EngineNumber getEqualsGhgIntensity();

  /**
   * Retrieve the tCO2e intensity for the given UseKey.
   *
   * @param useKey The UseKey containing application and substance information
   * @return The GHG intensity value with volume normalized GHG
   */
  EngineNumber getEqualsGhgIntensityFor(UseKey useKey);

  /**
   * Retrieve the energy intensity for the current application and substance.
   *
   * @return The energy intensity value with volume normalized energy
   */
  EngineNumber getEqualsEnergyIntensity();

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
   */
  void cap(String stream, EngineNumber amount, YearMatcher yearMatcher, String displaceTarget);

  /**
   * Set a minimum floor value for a stream.
   *
   * @param stream The stream identifier to set floor for
   * @param amount The minimum value to set as floor
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param displaceTarget Optional target for displaced amount
   */
  void floor(String stream, EngineNumber amount, YearMatcher yearMatcher, String displaceTarget);

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
}
