/**
 * Immutable result of a servicing offset calculation.
 *
 * <p>This class encapsulates the outputs of {@link ServicingOffsetter#offset}, including the
 * computed unit delta, precharge kilograms, and recharge kilograms.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc.util;

import java.math.BigDecimal;

/**
 * Immutable result of a servicing offset calculation.
 */
public class ServicingOffset {

  private final BigDecimal deltaUnits;
  private final BigDecimal prechargeKg;
  private final BigDecimal rechargeKg;

  /**
   * Create a new ServicingOffset.
   *
   * @param deltaUnits The change in equipment population in units
   * @param prechargeKg The precharge substance volume in kilograms
   * @param rechargeKg The recharge substance volume in kilograms
   */
  public ServicingOffset(BigDecimal deltaUnits, BigDecimal prechargeKg, BigDecimal rechargeKg) {
    this.deltaUnits = deltaUnits;
    this.prechargeKg = prechargeKg;
    this.rechargeKg = rechargeKg;
  }

  /**
   * Get the change in equipment population.
   *
   * @return The unit delta as a BigDecimal
   */
  public BigDecimal getDeltaUnits() {
    return deltaUnits;
  }

  /**
   * Get the precharge volume.
   *
   * @return The precharge volume in kilograms
   */
  public BigDecimal getPrechargeKg() {
    return prechargeKg;
  }

  /**
   * Get the recharge volume.
   *
   * @return The recharge volume in kilograms
   */
  public BigDecimal getRechargeKg() {
    return rechargeKg;
  }
}
