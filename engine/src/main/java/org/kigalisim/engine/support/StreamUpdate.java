package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Immutable class representing a stream update operation.
 * 
 * <p>Contains all parameters needed to execute a stream update operation in the engine,
 * including the stream name, value, timing constraints, scope, and various behavioral flags.</p>
 * 
 * @license BSD-3-Clause
 */
public final class StreamUpdate {
  private final String name;
  private final EngineNumber value;
  private final Optional<YearMatcher> yearMatcher;
  private final Optional<UseKey> key;
  private final boolean propagateChanges;
  private final Optional<String> unitsToRecord;
  private final boolean subtractRecycling;
  private final boolean forceUseFullRecharge;

  /**
   * Package-private constructor for creating a StreamUpdate instance.
   * 
   * @param name the name of the stream to update
   * @param value the value to set for the stream
   * @param yearMatcher optional year matcher to constrain when the update applies
   * @param key optional use key specifying the application/substance scope
   * @param propagateChanges whether this update should trigger recalculations
   * @param unitsToRecord optional units string to record for this operation
   * @param subtractRecycling whether recycling should be subtracted from the value
   * @param forceUseFullRecharge whether to force full recharge for sales substreams
   */
  StreamUpdate(String name, EngineNumber value, Optional<YearMatcher> yearMatcher,
               Optional<UseKey> key, boolean propagateChanges, Optional<String> unitsToRecord,
               boolean subtractRecycling, boolean forceUseFullRecharge) {
    this.name = name;
    this.value = value;
    this.yearMatcher = yearMatcher;
    this.key = key;
    this.propagateChanges = propagateChanges;
    this.unitsToRecord = unitsToRecord;
    this.subtractRecycling = subtractRecycling;
    this.forceUseFullRecharge = forceUseFullRecharge;
  }

  /**
   * Gets the name of the stream to update.
   * 
   * @return the stream name (e.g., "domestic", "import", "sales")
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the value to set for the stream.
   * 
   * @return the stream value with units
   */
  public EngineNumber getValue() {
    return value;
  }

  /**
   * Gets the optional year matcher constraining when this update applies.
   * 
   * @return optional year matcher, empty if update applies to all years
   */
  public Optional<YearMatcher> getYearMatcher() {
    return yearMatcher;
  }

  /**
   * Gets the optional use key specifying the application/substance scope.
   * 
   * @return optional use key, empty if using engine's current scope
   */
  public Optional<UseKey> getKey() {
    return key;
  }

  /**
   * Gets whether this update should trigger recalculations.
   * 
   * @return true if recalculations should be triggered, false otherwise
   */
  public boolean getPropagateChanges() {
    return propagateChanges;
  }

  /**
   * Gets the optional units string to record for this operation.
   * 
   * @return optional units string for tracking purposes
   */
  public Optional<String> getUnitsToRecord() {
    return unitsToRecord;
  }

  /**
   * Gets whether recycling should be subtracted from the value.
   * 
   * @return true if recycling should be subtracted, false otherwise
   */
  public boolean getSubtractRecycling() {
    return subtractRecycling;
  }

  /**
   * Gets whether to force full recharge for sales substreams.
   * 
   * @return true if full recharge should be used, false for proportional distribution
   */
  public boolean getForceUseFullRecharge() {
    return forceUseFullRecharge;
  }
}
