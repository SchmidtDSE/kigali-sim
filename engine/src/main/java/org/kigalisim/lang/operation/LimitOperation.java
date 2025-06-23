/**
 * Operation to limit a value to a range.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.math.BigDecimal;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Operation to limit a value to a range.
 */
public class LimitOperation implements Operation {

  private final Operation operand;
  private final Operation lowerBound;
  private final Operation upperBound;

  /**
   * Create a new LimitOperation with both lower and upper bounds.
   *
   * @param operand The operand to limit.
   * @param lowerBound The lower bound.
   * @param upperBound The upper bound.
   */
  public LimitOperation(Operation operand, Operation lowerBound, Operation upperBound) {
    this.operand = operand;
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  /**
   * Create a new LimitOperation with only an upper bound.
   *
   * @param operand The operand to limit.
   * @param upperBound The upper bound.
   */
  public LimitOperation(Operation operand, Operation upperBound) {
    this.operand = operand;
    this.lowerBound = null;
    this.upperBound = upperBound;
  }

  /**
   * Create a new LimitOperation with only a lower bound.
   *
   * @param operand The operand to limit.
   * @param lowerBound The lower bound.
   * @param isLowerBound Flag to indicate this is a lower bound (to disambiguate from upper bound constructor).
   */
  public LimitOperation(Operation operand, Operation lowerBound, boolean isLowerBound) {
    this.operand = operand;
    this.lowerBound = lowerBound;
    this.upperBound = null;
  }

  @Override
  public void execute(PushDownMachine machine) {
    // Execute the operand
    operand.execute(machine);
    EngineNumber value = machine.getResult();
    
    // Get the bounds
    EngineNumber lower = null;
    if (lowerBound != null) {
      lowerBound.execute(machine);
      lower = machine.getResult();
    }
    
    EngineNumber upper = null;
    if (upperBound != null) {
      upperBound.execute(machine);
      upper = machine.getResult();
    }
    
    // Apply the limits
    BigDecimal result = value.getValue();
    
    if (lower != null && result.compareTo(lower.getValue()) < 0) {
      result = lower.getValue();
    }
    
    if (upper != null && result.compareTo(upper.getValue()) > 0) {
      result = upper.getValue();
    }
    
    // Push the result
    machine.push(new EngineNumber(result, value.getUnits()));
  }
}