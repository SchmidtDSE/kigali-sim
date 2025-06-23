/**
 * Calculation which performs an equality comparison inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that performs an equality comparison on two other calculations.
 *
 * <p>Calculation that resolves two calculations and then performs an equality comparison
 * (==, !=, >, <, >=, <=) on the results by using a PushDownMachine within the QubecTalk runtime.</p>
 */
public class EqualityOperation implements Operation {

  private final Operation left;
  private final Operation right;
  private final String operator;

  /**
   * Create a new EqualityOperation.
   *
   * @param left The left operand of the equality comparison.
   * @param right The right operand of the equality comparison.
   * @param operator The equality operator (==, !=, >, <, >=, <=).
   */
  public EqualityOperation(Operation left, Operation right, String operator) {
    this.left = left;
    this.right = right;
    this.operator = operator;
  }

  @Override
  public void execute(PushDownMachine machine) {
    left.execute(machine);
    right.execute(machine);

    switch (operator) {
      case "==":
        machine.equals();
        break;
      case "!=":
        machine.notEquals();
        break;
      case ">":
        machine.greaterThan();
        break;
      case "<":
        machine.lessThan();
        break;
      case ">=":
        machine.greaterThanOrEqual();
        break;
      case "<=":
        machine.lessThanOrEqual();
        break;
      default:
        throw new RuntimeException("Unknown equality operator: " + operator);
    }
  }
}
