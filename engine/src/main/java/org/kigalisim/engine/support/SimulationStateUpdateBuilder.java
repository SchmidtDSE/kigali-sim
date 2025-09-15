package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.UseKey;

/**
 * Builder for creating SimulationStateUpdate instances.
 *
 * <p>Provides a fluent interface for constructing SimulationStateUpdate objects
 * with validation and sensible defaults.</p>
 */
public final class SimulationStateUpdateBuilder {
  private UseKey useKey;
  private String name;
  private EngineNumber value;
  private boolean subtractRecycling = true;  // Default to recycling logic for backward compatibility
  private Optional<SalesStreamDistribution> distribution = Optional.empty();
  private boolean salesDistributionRequired = false;  // Default for outcome streams

  /**
   * Creates a new SimulationStateUpdateBuilder with default values.
   */
  public SimulationStateUpdateBuilder() {
    // Default values are set in field declarations
  }

  /**
   * Sets the use key containing application and substance.
   *
   * @param useKey the use key
   * @return this builder
   */
  public SimulationStateUpdateBuilder setUseKey(UseKey useKey) {
    this.useKey = useKey;
    return this;
  }

  /**
   * Sets the stream name.
   *
   * @param name the stream name
   * @return this builder
   */
  public SimulationStateUpdateBuilder setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets the pre-calculated stream value.
   *
   * @param value the stream value
   * @return this builder
   */
  public SimulationStateUpdateBuilder setValue(EngineNumber value) {
    this.value = value;
    return this;
  }

  /**
   * Sets whether to subtract recycling from the value.
   *
   * @param subtractRecycling whether to subtract recycling
   * @return this builder
   */
  public SimulationStateUpdateBuilder setSubtractRecycling(boolean subtractRecycling) {
    this.subtractRecycling = subtractRecycling;
    return this;
  }

  /**
   * Sets the pre-calculated distribution for sales streams.
   *
   * @param distribution the sales stream distribution
   * @return this builder
   */
  public SimulationStateUpdateBuilder setDistribution(SalesStreamDistribution distribution) {
    this.distribution = Optional.ofNullable(distribution);
    return this;
  }

  /**
   * Clears the distribution.
   *
   * @return this builder
   */
  public SimulationStateUpdateBuilder clearDistribution() {
    this.distribution = Optional.empty();
    return this;
  }

  /**
   * Sets whether this stream requires sales distribution logic.
   *
   * @param salesDistributionRequired whether sales distribution is required
   * @return this builder
   */
  public SimulationStateUpdateBuilder setSalesDistributionRequired(boolean salesDistributionRequired) {
    this.salesDistributionRequired = salesDistributionRequired;
    return this;
  }

  /**
   * Convenience method to configure this as an outcome stream.
   *
   * <p>Outcome streams don't require sales distribution logic and typically
   * don't use recycling subtraction.</p>
   *
   * @return this builder
   */
  public SimulationStateUpdateBuilder asOutcomeStream() {
    this.subtractRecycling = false;
    this.salesDistributionRequired = false;
    this.distribution = Optional.empty();
    return this;
  }

  /**
   * Convenience method to configure this as a sales stream.
   *
   * <p>Sales streams require distribution logic and typically use recycling subtraction.</p>
   *
   * @return this builder
   */
  public SimulationStateUpdateBuilder asSalesStream() {
    this.subtractRecycling = true;
    this.salesDistributionRequired = true;
    return this;
  }

  /**
   * Builds the SimulationStateUpdate.
   *
   * @return the built SimulationStateUpdate
   * @throws IllegalStateException if required fields are not set
   */
  public SimulationStateUpdate build() {
    if (useKey == null) {
      throw new IllegalStateException("UseKey is required");
    }
    if (name == null) {
      throw new IllegalStateException("Name is required");
    }
    if (value == null) {
      throw new IllegalStateException("Value is required");
    }

    return new SimulationStateUpdate(
        useKey,
        name,
        value,
        subtractRecycling,
        distribution,
        salesDistributionRequired
    );
  }
}