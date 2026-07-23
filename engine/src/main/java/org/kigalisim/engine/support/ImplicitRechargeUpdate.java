/**
 * Immutable result of implicit recharge/precharge calculation.
 *
 * <p>Contains the adjusted value to set for a stream after applying implicit
 * recharge (if applicable) and optional state updates for the implicitRecharge and
 * implicitPrecharge streams.</p>
 *
 * <p>When sales streams are set with equipment units, implicit recharge is
 * calculated and added to the specified value to service the existing equipment population. Similarly,
 * implicit precharge is calculated and added for new equipment. This class encapsulates the adjusted
 * value and the recharge/precharge state updates.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.SimulationStateUpdate;

/**
 * Immutable result of implicit recharge/precharge calculation.
 *
 * <p>This class encapsulates the result of calculating and applying implicit
 * recharge and precharge to a stream value. For sales streams with equipment units, it contains
 * both the adjusted value (original value plus recharge and precharge) and state updates for the
 * implicitRecharge and implicitPrecharge streams. For other streams, it contains the original value
 * unchanged with empty state updates.</p>
 */
public final class ImplicitRechargeUpdate {
  private final EngineNumber valueToSet;
  private final Optional<SimulationStateUpdate> implicitRechargeStateUpdate;
  private final Optional<SimulationStateUpdate> implicitPrechargeStateUpdate;

  /**
   * Creates a new ImplicitRechargeUpdate with the specified values.
   *
   * @param valueToSet The adjusted value to set for the stream (with recharge added if applicable)
   * @param implicitRechargeStateUpdate Optional state update for the implicitRecharge stream
   */
  public ImplicitRechargeUpdate(EngineNumber valueToSet,
      Optional<SimulationStateUpdate> implicitRechargeStateUpdate) {
    this(valueToSet, implicitRechargeStateUpdate, Optional.empty());
  }

  /**
   * Creates a new ImplicitRechargeUpdate with recharge and precharge state updates.
   *
   * @param valueToSet The adjusted value to set for the stream (with recharge and precharge added)
   * @param implicitRechargeStateUpdate Optional state update for the implicitRecharge stream
   * @param implicitPrechargeStateUpdate Optional state update for the implicitPrecharge stream
   */
  public ImplicitRechargeUpdate(EngineNumber valueToSet,
      Optional<SimulationStateUpdate> implicitRechargeStateUpdate,
      Optional<SimulationStateUpdate> implicitPrechargeStateUpdate) {
    this.valueToSet = valueToSet;
    this.implicitRechargeStateUpdate = implicitRechargeStateUpdate;
    this.implicitPrechargeStateUpdate = implicitPrechargeStateUpdate;
  }

  /**
   * Gets the adjusted value to set for the stream.
   *
   * <p>For sales streams with equipment units, this is the original value plus
   * the calculated recharge and precharge volumes. For other streams, this is the original value
   * unchanged.</p>
   *
   * @return The value to set for the stream
   */
  public EngineNumber getValueToSet() {
    return valueToSet;
  }

  /**
   * Gets the optional state update for the implicitRecharge stream.
   *
   * @return Optional state update for the implicitRecharge stream
   */
  public Optional<SimulationStateUpdate> getImplicitRechargeStateUpdate() {
    return implicitRechargeStateUpdate;
  }

  /**
   * Gets the optional state update for the implicitPrecharge stream.
   *
   * @return Optional state update for the implicitPrecharge stream
   */
  public Optional<SimulationStateUpdate> getImplicitPrechargeStateUpdate() {
    return implicitPrechargeStateUpdate;
  }
}
