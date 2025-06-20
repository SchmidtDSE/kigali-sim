/**
 * Calculation which performs an addition inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that adds two other calculations together.
 *
 * <p>Calculation that resolves two calculations and then adds the result by using a
 * PushDownMachine within the QubecTalk runtime.</p>
 */
public class AdditionOperation implements Operation {

  private final Operation left;
  private final Operation right;

  /**
   * Create a new AdditionOperation.
   *
   * @param left The left operand of the addition.
   * @param right The right operand of the addition.
   */
  public AdditionOperation(Operation left, Operation right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public void execute(PushDownMachine machine) {
    left.execute(machine);
    right.execute(machine);
    machine.add();
  }
}
