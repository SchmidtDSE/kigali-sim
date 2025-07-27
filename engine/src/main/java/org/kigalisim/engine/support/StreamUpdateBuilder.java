package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Builder for creating StreamUpdate instances.
 */
public final class StreamUpdateBuilder {
  private String name;
  private EngineNumber value;
  private Optional<YearMatcher> yearMatcher = Optional.empty();
  private Optional<UseKey> key = Optional.empty();
  private boolean propagateChanges = true;
  private Optional<String> unitsToRecord = Optional.empty();
  private boolean subtractRecycling = true;
  private boolean forceUseFullRecharge = false;

  /**
   * Sets the stream name.
   *
   * @param name the stream name
   * @return this builder
   */
  public StreamUpdateBuilder setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets the stream value.
   *
   * @param value the stream value
   * @return this builder
   */
  public StreamUpdateBuilder setValue(EngineNumber value) {
    this.value = value;
    return this;
  }

  /**
   * Sets the year matcher.
   *
   * @param yearMatcher the year matcher
   * @return this builder
   */
  public StreamUpdateBuilder setYearMatcher(Optional<YearMatcher> yearMatcher) {
    this.yearMatcher = yearMatcher;
    return this;
  }


  /**
   * Sets the use key.
   *
   * @param key the use key
   * @return this builder
   */
  public StreamUpdateBuilder setKey(UseKey key) {
    this.key = Optional.of(key);
    return this;
  }

  /**
   * Clears the use key.
   *
   * @return this builder
   */
  public StreamUpdateBuilder clearKey() {
    this.key = Optional.empty();
    return this;
  }

  /**
   * Sets whether changes should propagate.
   *
   * @param propagateChanges whether to propagate changes
   * @return this builder
   */
  public StreamUpdateBuilder setPropagateChanges(boolean propagateChanges) {
    this.propagateChanges = propagateChanges;
    return this;
  }


  /**
   * Sets the units to record.
   *
   * @param unitsToRecord the units to record
   * @return this builder
   */
  public StreamUpdateBuilder setUnitsToRecord(String unitsToRecord) {
    this.unitsToRecord = Optional.of(unitsToRecord);
    return this;
  }

  /**
   * Clears the units to record.
   *
   * @return this builder
   */
  public StreamUpdateBuilder clearUnitsToRecord() {
    this.unitsToRecord = Optional.empty();
    return this;
  }

  /**
   * Sets whether to subtract recycling.
   *
   * @param subtractRecycling whether to subtract recycling
   * @return this builder
   */
  public StreamUpdateBuilder setSubtractRecycling(boolean subtractRecycling) {
    this.subtractRecycling = subtractRecycling;
    return this;
  }

  /**
   * Sets whether to force use of full recharge.
   *
   * @param forceUseFullRecharge whether to force use of full recharge
   * @return this builder
   */
  public StreamUpdateBuilder setForceUseFullRecharge(boolean forceUseFullRecharge) {
    this.forceUseFullRecharge = forceUseFullRecharge;
    return this;
  }

  /**
   * Builds the StreamUpdate.
   *
   * @return the built StreamUpdate
   * @throws IllegalStateException if name or value is not set
   */
  public StreamUpdate build() {
    if (name == null || value == null) {
      throw new IllegalStateException("Name and value are required");
    }
    return new StreamUpdate(
        name,
        value,
        yearMatcher,
        key,
        propagateChanges,
        unitsToRecord,
        subtractRecycling,
        forceUseFullRecharge
    );
  }
}
