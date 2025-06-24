/**
 * Calculation which performs a subtraction inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;


/**
 * Calculation that subtracts one calculation from another.
 *
 * <p>Calculation that resolves two calculations and then subtracts the right from the left by using
 * a PushDownMachine within the QubecTalk runtime.</p>
 */
public class SubtractionOperation implements Operation {

  private final Operation left;
  private final Operation right;

  /**
   * Create a new SubtractionOperation.
   *
   * @param left The left operand of the subtraction.
   * @param right The right operand of the subtraction.
   */
  public SubtractionOperation(Operation left, Operation right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public void execute(PushDownMachine machine) {
    left.execute(machine);
    right.execute(machine);
    machine.subtract();
  }
}
