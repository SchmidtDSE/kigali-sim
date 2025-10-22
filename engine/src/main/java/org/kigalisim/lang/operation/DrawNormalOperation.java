/**
 * Calculation which draws a random sample from a normal distribution.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that samples from a normal distribution.
 *
 * <p>Calculation that resolves two calculations (mean and standard deviation) and then draws a
 * random sample from a normal distribution with those parameters by using a PushDownMachine within
 * the QubecTalk runtime.</p>
 */
public class DrawNormalOperation implements Operation {

  private final Operation mean;
  private final Operation std;

  /**
   * Create a new DrawNormalOperation.
   *
   * @param mean The mean of the normal distribution.
   * @param std The standard deviation of the normal distribution.
   */
  public DrawNormalOperation(Operation mean, Operation std) {
    this.mean = mean;
    this.std = std;
  }

  @Override
  public void execute(PushDownMachine machine) {
    mean.execute(machine);
    std.execute(machine);
    machine.drawNormal();
  }
}
