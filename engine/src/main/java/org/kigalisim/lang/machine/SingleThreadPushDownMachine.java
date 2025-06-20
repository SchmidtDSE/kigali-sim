/**
 * Single threaded machine which can perform mathematical operations for QubecTalk.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.machine;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Stack;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;


/**
 * A push down automaton which can perform mathematical and logical operations for QubecTalk.
 *
 * <p>A push down automaton which can perform mathematical and logical operations for QubecTalk but
 * which may necessarily be thread safe.</p>
 */
public class SingleThreadPushDownMachine implements PushDownMachine {

  private final Engine engine;
  private final Stack<EngineNumber> stack;
  private Optional<String> expectedUnitsMaybe;

  /**
   * Create a new SingleThreadPushDownMachine.
   *
   * @param engine The engine in which this machine will run.
   */
  public SingleThreadPushDownMachine(Engine engine) {
    this.engine = engine;
    stack = new Stack<>();
    expectedUnitsMaybe = Optional.empty();
  }

  @Override
  public void push(EngineNumber value) {
    stack.push(value);
  }

  @Override
  public EngineNumber getResult() {
    if (stack.size() != 1) {
      throw new RuntimeException("Expected exactly one result on the stack.");
    }
    return pop();
  }

  @Override
  public void add() {
    EngineNumber right = pop();
    setExpectedUnits(right.getUnits());
    EngineNumber left = pop();
    BigDecimal resultValue = left.getValue().add(right.getValue());
    EngineNumber result = new EngineNumber(resultValue, getExpectedUnits());
    push(result);
    clearExpectedUnits();
  }

  @Override
  public void subtract() {
    EngineNumber right = pop();
    setExpectedUnits(right.getUnits());
    EngineNumber left = pop();
    BigDecimal resultValue = left.getValue().subtract(right.getValue());
    EngineNumber result = new EngineNumber(resultValue, getExpectedUnits());
    push(result);
    clearExpectedUnits();
  }

  @Override
  public void changeUnits(String units) {
    EngineNumber top = pop();
    boolean allowed = top.getUnits().isEmpty() || top.getUnits().equals(units);
    if (!allowed) {
      String message = String.format(
          "Unexpected units for top value. Anticipated empty or %s but got %s.",
          units,
          top.getUnits()
      );
      throw new RuntimeException(message);
    }
    EngineNumber topWithUnits = new EngineNumber(top.getValue(), units);
    push(topWithUnits);
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  /**
   * Pop the top value from the stack.
   *
   * @return The value popped from the stack.
   * @throws RuntimeException If the stack is empty or the value's units do not match the expected
   *     units.
   */
  private EngineNumber pop() {
    EngineNumber result = stack.pop();
    if (expectedUnitsMaybe.isPresent()) {
      String expectedUnits = expectedUnitsMaybe.get();
      if (!result.getUnits().equals(expectedUnits)) {
        String message = String.format(
            "Unexpected units for popped value. Anticipated %s but got %s.",
            expectedUnits,
            result.getUnits()
        );
        throw new RuntimeException(message);
      }
    }
    return result;
  }

  /**
   * Set the expected units for operands.
   *
   * @param units The expected units for the next operation.
   */
  private void setExpectedUnits(String units) {
    expectedUnitsMaybe = Optional.of(units);
  }

  /**
   * Clear the units expectation for the next operation.
   */
  private void clearExpectedUnits() {
    expectedUnitsMaybe = Optional.empty();
  }

  /**
   * Get the expected units for the next operation.
   *
   * @return The expected units for operands.
   */
  private String getExpectedUnits() {
    return expectedUnitsMaybe.orElseThrow();
  }
}
