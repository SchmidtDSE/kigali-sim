/**
 * Calculation which performs an addition inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that raises one number to the power of another.
 */
public class PowerOperation implements Operation {

  private final Operation left;
  private final Operation right;

  /**
   * Create a new PowerOperation.
   *
   * @param left The left operand of the power operation (base).
   * @param right The right operand of the power (power).
   */
  public PowerOperation(Operation left, Operation right) {
    this.left = left;
    this.right = right;
  }

  /** {@inheritDoc} */
  @Override
  public void execute(PushDownMachine machine) {
    left.execute(machine);
    right.execute(machine);
    machine.power();
  }
}
