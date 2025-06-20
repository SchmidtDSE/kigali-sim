/**
 * Time point which resolves to a dynamic cap value.
 *
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.time;

import org.kigalisim.lang.machine.PushDownMachine;


/**
 * A future timepoint which resolves to a dynamic cap value.
 *
 * <p>A future timepoint which is determined at compile time, resolving to a dynamic cap value like
 * beginning or onwards.</p>
 */
public class DynamicCapFuture implements TimePointFuture {

  private final String value;

  /**
   * Create a new DynamicCapFuture.
   *
   *
   * @param value The dynamic cap value to which this should resolve.
   */
  public DynamicCapFuture(String value) {
    this.value = value;
  }

  @Override
  public TimePointRealized realize(PushDownMachine machine) {
    return new TimePointRealized(value);
  }

}
