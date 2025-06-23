/**
 * Calculation which performs a conditional operation inside a PushDownMachine.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.math.BigDecimal;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Calculation that performs a conditional operation (if-else) on three other calculations.
 *
 * <p>Calculation that resolves a condition calculation and then executes one of two other
 * calculations based on the result of the condition by using a PushDownMachine within the
 * QubecTalk runtime.</p>
 */
public class ConditionalOperation implements Operation {

  private final Operation condition;
  private final Operation trueCase;
  private final Operation falseCase;

  /**
   * Create a new ConditionalOperation.
   *
   * @param condition The condition to evaluate.
   * @param trueCase The operation to execute if the condition is true.
   * @param falseCase The operation to execute if the condition is false.
   */
  public ConditionalOperation(Operation condition, Operation trueCase, Operation falseCase) {
    this.condition = condition;
    this.trueCase = trueCase;
    this.falseCase = falseCase;
  }

  @Override
  public void execute(PushDownMachine machine) {
    condition.execute(machine);
    EngineNumber conditionResult = machine.getResult();
    
    boolean conditionBool = !conditionResult.getValue().equals(BigDecimal.ZERO);
    
    if (conditionBool) {
      trueCase.execute(machine);
    } else {
      falseCase.execute(machine);
    }
  }
}