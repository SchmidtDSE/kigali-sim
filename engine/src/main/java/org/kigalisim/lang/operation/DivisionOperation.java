/**
 * Calculation which performs a division inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that divides two other calculations.
 *
 * <p>Calculation that resolves two calculations and then divides the result by using a
 * PushDownMachine within the QubecTalk runtime.</p>
 */
public class DivisionOperation implements Operation {

  private final Operation left;
  private final Operation right;

  /**
   * Create a new DivisionOperation.
   *
   * @param left The left operand of the division (dividend).
   * @param right The right operand of the division (divisor).
   */
  public DivisionOperation(Operation left, Operation right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public void execute(PushDownMachine machine) {
    left.execute(machine);
    right.execute(machine);
    machine.divide();
  }
}