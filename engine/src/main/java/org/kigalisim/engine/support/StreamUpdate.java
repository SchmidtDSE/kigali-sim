package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Immutable class representing a stream update operation.
 * Contains all parameters needed to execute a stream update operation.
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

  public String getName() {
    return name;
  }

  public EngineNumber getValue() {
    return value;
  }

  public Optional<YearMatcher> getYearMatcher() {
    return yearMatcher;
  }

  public Optional<UseKey> getKey() {
    return key;
  }

  public boolean getPropagateChanges() {
    return propagateChanges;
  }

  public Optional<String> getUnitsToRecord() {
    return unitsToRecord;
  }

  public boolean getSubtractRecycling() {
    return subtractRecycling;
  }

  public boolean getForceUseFullRecharge() {
    return forceUseFullRecharge;
  }
}
