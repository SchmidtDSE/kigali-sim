/**
 * Immutable component holding a servicing (recharge or precharge) volume and its optional state update.
 *
 * <p>This small value class is produced by {@link ImplicitRechargeUpdateBuilder} when breaking
 * apart the recharge and precharge portions of an implicit servicing calculation. It pairs the
 * BigDecimal amount that should be added to the stream value with the optional
 * {@link SimulationStateUpdate} that records the servicing volume in the corresponding
 * implicit stream.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.state.SimulationStateUpdate;

/**
 * Immutable component holding a servicing volume and its optional state update.
 *
 * <p>Instances are created by {@link ImplicitRechargeUpdateBuilder#getForRecharge} and
 * {@link ImplicitRechargeUpdateBuilder#getForPrecharge} to encapsulate one half (either the
 * recharge or precharge half) of an implicit servicing calculation.</p>
 */
public final class ServicingUpdateComponent {

  private final BigDecimal value;
  private final Optional<SimulationStateUpdate> update;

  /**
   * Creates a new ServicingUpdateComponent.
   *
   * @param value The BigDecimal amount to add to the stream value for this servicing portion
   * @param update The optional state update recording this servicing volume in the implicit
   *     stream, or empty if no state update is needed
   */
  public ServicingUpdateComponent(BigDecimal value, Optional<SimulationStateUpdate> update) {
    this.value = value;
    this.update = update;
  }

  /**
   * Gets the amount that should be added to the stream value for this servicing portion.
   *
   * @return The servicing volume to add as a BigDecimal
   */
  public BigDecimal getValue() {
    return value;
  }

  /**
   * Gets the optional state update recording this servicing volume in the implicit stream.
   *
   * @return Optional state update for the corresponding implicit stream
   */
  public Optional<SimulationStateUpdate> getUpdate() {
    return update;
  }
}
