/**
 * Immutable result of implicit recharge calculation.
 *
 * <p>Contains the adjusted value to set for a stream after applying implicit
 * recharge (if applicable) and an optional state update for the implicitRecharge stream itself.</p>
 *
 * <p>When sales streams are set with equipment units, implicit recharge is
 * calculated and added to the specified value to service the existing equipment population. This
 * class encapsulates both the adjusted value and the recharge state update.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.SimulationStateUpdate;

/**
 * Immutable result of implicit recharge calculation.
 *
 * <p>This class encapsulates the result of calculating and applying implicit
 * recharge to a stream value. For sales streams with equipment units, it contains both the adjusted
 * value (original value plus recharge) and a state update for the implicitRecharge stream. For
 * other streams, it contains the original value unchanged with an empty state update.</p>
 */
public final class ImplicitRechargeUpdate {
  private final EngineNumber valueToSet;
  private final Optional<SimulationStateUpdate> implicitRechargeStateUpdate;

  /**
   * Creates a new ImplicitRechargeUpdate with the specified values.
   *
   * @param valueToSet The adjusted value to set for the stream (with recharge added if applicable)
   * @param implicitRechargeStateUpdate Optional state update for the implicitRecharge stream
   */
  public ImplicitRechargeUpdate(EngineNumber valueToSet,
      Optional<SimulationStateUpdate> implicitRechargeStateUpdate) {
    this.valueToSet = valueToSet;
    this.implicitRechargeStateUpdate = implicitRechargeStateUpdate;
  }

  /**
   * Gets the adjusted value to set for the stream.
   *
   * <p>For sales streams with equipment units, this is the original value plus
   * the calculated recharge volume. For other streams, this is the original value unchanged.</p>
   *
   * @return The value to set for the stream
   */
  public EngineNumber getValueToSet() {
    return valueToSet;
  }

  /**
   * Gets the optional state update for the implicitRecharge stream.
   *
   * <p>When implicit recharge is active (sales stream with equipment units), this
   * contains a state update to record the recharge volume in the implicitRecharge stream. When
   * implicit recharge is not active, this is empty.</p>
   *
   * @return Optional state update for the implicitRecharge stream
   */
  public Optional<SimulationStateUpdate> getImplicitRechargeStateUpdate() {
    return implicitRechargeStateUpdate;
  }
}
