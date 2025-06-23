/**
 * Calculation which performs a logical operation inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that performs a logical operation on two other calculations.
 *
 * <p>Calculation that resolves two calculations and then performs a logical operation (AND, OR, XOR)
 * on the results by using a PushDownMachine within the QubecTalk runtime.</p>
 */
public class LogicalOperation implements Operation {

  private final Operation left;
  private final Operation right;
  private final String operator;

  /**
   * Create a new LogicalOperation.
   *
   * @param left The left operand of the logical operation.
   * @param right The right operand of the logical operation.
   * @param operator The logical operator (and, or, xor).
   */
  public LogicalOperation(Operation left, Operation right, String operator) {
    this.left = left;
    this.right = right;
    this.operator = operator;
  }

  @Override
  public void execute(PushDownMachine machine) {
    left.execute(machine);
    right.execute(machine);

    switch (operator) {
      case "and" -> machine.and();
      case "or" -> machine.or();
      case "xor" -> machine.xor();
      default -> throw new RuntimeException("Unknown logical operator: " + operator);
    }
  }
}
