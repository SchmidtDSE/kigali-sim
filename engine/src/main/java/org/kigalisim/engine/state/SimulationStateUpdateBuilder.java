package org.kigalisim.engine.state;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.SalesStreamDistribution;

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
   * Sets the pre-computed stream value.
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
   * Determines if a stream name requires sales distribution logic.
   *
   * <p>Sales streams include: "sales", "domestic", "import", "export"</p>
   *
   * @param streamName the name of the stream
   * @return true if the stream requires sales distribution logic
   */
  private static boolean inferSalesDistributionRequired(String streamName) {
    if (streamName == null) {
      return false;
    }
    return "sales".equals(streamName)
        || "domestic".equals(streamName)
        || "import".equals(streamName)
        || "export".equals(streamName);
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
        inferSalesDistributionRequired(name)
    );
  }
}