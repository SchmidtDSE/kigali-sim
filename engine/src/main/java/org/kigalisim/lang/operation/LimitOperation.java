/**
 * Operation to limit a value to a range.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Operation to limit a value to a range.
 */
public class LimitOperation implements Operation {

  private final Operation operand;
  private final Optional<Operation> lowerBound;
  private final Optional<Operation> upperBound;

  /**
   * Create a new LimitOperation with both lower and upper bounds.
   *
   * @param operand The operand to limit.
   * @param lowerBound The lower bound.
   * @param upperBound The upper bound.
   */
  public LimitOperation(Operation operand, Operation lowerBound, Operation upperBound) {
    this.operand = operand;
    this.lowerBound = Optional.ofNullable(lowerBound);
    this.upperBound = Optional.ofNullable(upperBound);
  }

  /**
   * Create a new LimitOperation with only an upper bound.
   *
   * @param operand The operand to limit.
   * @param upperBound The upper bound.
   */
  public LimitOperation(Operation operand, Operation upperBound) {
    this.operand = operand;
    this.lowerBound = Optional.empty();
    this.upperBound = Optional.ofNullable(upperBound);
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
    this.lowerBound = Optional.ofNullable(lowerBound);
    this.upperBound = Optional.empty();
  }

  @Override
  public void execute(PushDownMachine machine) {
    // Execute the operand
    operand.execute(machine);
    EngineNumber value = machine.getResult();

    // Get the bounds
    Optional<EngineNumber> lower = Optional.empty();
    if (lowerBound.isPresent()) {
      lowerBound.get().execute(machine);
      lower = Optional.of(machine.getResult());
    }

    Optional<EngineNumber> upper = Optional.empty();
    if (upperBound.isPresent()) {
      upperBound.get().execute(machine);
      upper = Optional.of(machine.getResult());
    }

    // Apply the limits
    BigDecimal result = value.getValue();

    if (lower.isPresent() && result.compareTo(lower.get().getValue()) < 0) {
      result = lower.get().getValue();
    }

    if (upper.isPresent() && result.compareTo(upper.get().getValue()) > 0) {
      result = upper.get().getValue();
    }

    // Push the result
    machine.push(new EngineNumber(result, value.getUnits()));
  }
}
