/**
 * Calculation which performs a multiplication inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that multiplies two other calculations together.
 *
 * <p>Calculation that resolves two calculations and then multiplies the result by using a
 * PushDownMachine within the QubecTalk runtime.</p>
 */
public class MultiplicationOperation implements Operation {

  private final Operation left;
  private final Operation right;

  /**
   * Create a new MultiplicationOperation.
   *
   * @param left The left operand of the multiplication.
   * @param right The right operand of the multiplication.
   */
  public MultiplicationOperation(Operation left, Operation right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public void execute(PushDownMachine machine) {
    left.execute(machine);
    right.execute(machine);
    machine.multiply();
  }
}