package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.UseKey;

/**
 * Immutable class representing a calculated stream value ready to be set.
 *
 * <p>This class encapsulates the result of stream calculations and contains all
 * parameters needed to update simulation state. Unlike StreamUpdate
 * which contains calculation instructions, SimulationStateUpdate contains pre-computed
 * results ready for storage.</p>
 *
 * <p>Use StreamUpdate for operations that need calculation logic (set, change, cap, floor).
 * Use SimulationStateUpdate for setting pre-calculated values (recalc strategies, emissions).</p>
 *
 * @license BSD-3-Clause
 */
public final class SimulationStateUpdate {
  private final UseKey useKey;
  private final String name;
  private final EngineNumber value;
  private final boolean subtractRecycling;
  private final Optional<SalesStreamDistribution> distribution;
  private final boolean salesDistributionRequired;

  /**
   * Package-private constructor for creating a SimulationStateUpdate instance.
   *
   * @param useKey the key containing application and substance
   * @param name the name of the stream to set
   * @param value the pre-calculated value to set for the stream
   * @param subtractRecycling whether recycling should be subtracted from the value
   * @param distribution optional pre-calculated distribution for sales streams
   * @param salesDistributionRequired whether this stream requires sales distribution logic
   */
  SimulationStateUpdate(UseKey useKey, String name, EngineNumber value,
                   boolean subtractRecycling, Optional<SalesStreamDistribution> distribution,
                   boolean salesDistributionRequired) {
    this.useKey = useKey;
    this.name = name;
    this.value = value;
    this.subtractRecycling = subtractRecycling;
    this.distribution = distribution;
    this.salesDistributionRequired = salesDistributionRequired;
  }

  /**
   * Gets the use key containing application and substance.
   *
   * @return the use key specifying the scope
   */
  public UseKey getUseKey() {
    return useKey;
  }

  /**
   * Gets the name of the stream to set.
   *
   * @return the stream name (e.g., "domestic", "import", "recycle")
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the pre-calculated value to set for the stream.
   *
   * @return the stream value with units
   */
  public EngineNumber getValue() {
    return value;
  }

  /**
   * Gets whether recycling should be subtracted from the value.
   *
   * @return true if recycling should be subtracted, false for direct setting
   */
  public boolean getSubtractRecycling() {
    return subtractRecycling;
  }

  /**
   * Gets the optional pre-calculated distribution for sales streams.
   *
   * @return optional sales stream distribution, empty if none specified
   */
  public Optional<SalesStreamDistribution> getDistribution() {
    return distribution;
  }

  /**
   * Gets whether this stream requires sales distribution logic.
   *
   * @return true if this is a sales stream that needs distribution, false for outcome streams
   */
  public boolean isSalesDistributionRequired() {
    return salesDistributionRequired;
  }
}