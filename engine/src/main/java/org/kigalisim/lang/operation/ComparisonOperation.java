/**
 * Calculation which performs a comparison inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that compares two other calculations.
 *
 * <p>Calculation that resolves two calculations and then compares the results by using a
 * PushDownMachine within the QubecTalk runtime.</p>
 */
public class ComparisonOperation implements Operation {

  private final Operation left;
  private final Operation right;
  private final String operator;

  /**
   * Create a new ComparisonOperation.
   *
   * @param left The left operand of the comparison.
   * @param right The right operand of the comparison.
   * @param operator The comparison operator (==, !=, >, <, >=, <=).
   */
  public ComparisonOperation(Operation left, Operation right, String operator) {
    this.left = left;
    this.right = right;
    this.operator = operator;
  }

  @Override
  public void execute(PushDownMachine machine) {
    left.execute(machine);
    right.execute(machine);

    switch (operator) {
      case "==" -> machine.equals();
      case "!=" -> machine.notEquals();
      case ">" -> machine.greaterThan();
      case "<" -> machine.lessThan();
      case ">=" -> machine.greaterThanOrEqual();
      case "<=" -> machine.lessThanOrEqual();
      default -> throw new RuntimeException("Unknown comparison operator: " + operator);
    }
  }
}
