/**
 * Calculation which draws a random sample from a uniform distribution.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that samples from a uniform distribution.
 *
 * <p>Calculation that resolves two calculations (low and high bounds) and then draws a random
 * sample from a uniform distribution within those bounds by using a PushDownMachine within the
 * QubecTalk runtime.</p>
 */
public class DrawUniformOperation implements Operation {

  private final Operation low;
  private final Operation high;

  /**
   * Create a new DrawUniformOperation.
   *
   * @param low The lower bound of the uniform distribution.
   * @param high The upper bound of the uniform distribution.
   */
  public DrawUniformOperation(Operation low, Operation high) {
    this.low = low;
    this.high = high;
  }

  /** {@inheritDoc} */
  @Override
  public void execute(PushDownMachine machine) {
    low.execute(machine);
    high.execute(machine);
    machine.drawUniform();
  }
}
