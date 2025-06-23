/**
 * Calculation which performs a comparison inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.math.BigDecimal;
import org.kigalisim.engine.number.EngineNumber;
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
    EngineNumber leftResult = machine.getResult();
    
    right.execute(machine);
    EngineNumber rightResult = machine.getResult();
    
    boolean result = false;
    
    switch (operator) {
      case "==":
        result = leftResult.getValue().compareTo(rightResult.getValue()) == 0;
        break;
      case "!=":
        result = leftResult.getValue().compareTo(rightResult.getValue()) != 0;
        break;
      case ">":
        result = leftResult.getValue().compareTo(rightResult.getValue()) > 0;
        break;
      case "<":
        result = leftResult.getValue().compareTo(rightResult.getValue()) < 0;
        break;
      case ">=":
        result = leftResult.getValue().compareTo(rightResult.getValue()) >= 0;
        break;
      case "<=":
        result = leftResult.getValue().compareTo(rightResult.getValue()) <= 0;
        break;
      default:
        throw new RuntimeException("Unknown comparison operator: " + operator);
    }
    
    // Push the result as a 1 (true) or 0 (false) with no units
    machine.push(new EngineNumber(result ? BigDecimal.ONE : BigDecimal.ZERO, ""));
  }
}