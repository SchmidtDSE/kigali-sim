/**
 * Description of a machine which can perform mathematical operations for QubecTalk.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.machine;

import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;


/**
 * A push down automaton which can perform mathematical and logical operations for QubecTalk.
 *
 * <p>A stack-based machine which can perform mathematical calculations as part of the QubecTalk
 * runtime. It assumes that units provided are appropriately converted prior to pushing. It also can
 * perform logical operations like ternary operations.</p>
 */
public interface PushDownMachine {

  /**
   * Push a number onto the machine's stack.
   *
   * @param value The value to push.
   */
  void push(EngineNumber value);

  /**
   * Get the result of the machine's calculations.
   *
   * @return The result of the calculations.
   * @throws RuntimeException if the machine does not have exactly one result waiting at the top
   *     of its stack.
   */
  EngineNumber getResult();

  /**
   * Add the two numbers on top of the stack.
   *
   * <p>Add the two numbers on top of the stack, pushing the result of the calculation to the top
   * of the stack.</p>
   */
  void add();

  /**
   * Subtract the two numbers on top of the stack.
   *
   * <p>Subtract the two numbers on top of the stack, pushing the result of the calculation to the
   * top of the stack. It assumes that the left operand was pushed prior to the right operand such
   * that the right operand is on the top of the stack and the left operand is right below.</p>
   */
  void subtract();

  /**
   * Multiply the two numbers on top of the stack.
   *
   * <p>Multiply the two numbers on top of the stack, pushing the result of the calculation to the
   * top of the stack. It assumes that the left operand was pushed prior to the right operand such
   * that the right operand is on the top of the stack and the left operand is right below.</p>
   */
  void multiply();

  /**
   * Divide the two numbers on top of the stack.
   *
   * <p>Divide the two numbers on top of the stack, pushing the result of the calculation to the
   * top of the stack. It assumes that the left operand was pushed prior to the right operand such
   * that the right operand is on the top of the stack and the left operand is right below.</p>
   *
   * @throws ArithmeticException If the right operand (divisor) is zero.
   */
  void divide();

  /**
   * Change the units of the number at the top of the stack.
   *
   * @param units The new units for the number at the top of the stack.
   * @throws RuntimeException If the stack is empty or the top value already has units which do not
   *     match the desired units.
   */
  void changeUnits(String units);

  /**
   * Get the engine in which this machine is running.
   *
   * @return The engine in which this machine is running.
   */
  Engine getEngine();

  /**
   * Perform a logical AND operation on the two numbers on top of the stack.
   *
   * <p>Perform a logical AND operation on the two numbers on top of the stack, pushing the result
   * of the calculation to the top of the stack. It assumes that the left operand was pushed prior
   * to the right operand such that the right operand is on the top of the stack and the left
   * operand is right below. Non-zero values are treated as true, zero values as false.</p>
   */
  void and();

  /**
   * Perform a logical OR operation on the two numbers on top of the stack.
   *
   * <p>Perform a logical OR operation on the two numbers on top of the stack, pushing the result
   * of the calculation to the top of the stack. It assumes that the left operand was pushed prior
   * to the right operand such that the right operand is on the top of the stack and the left
   * operand is right below. Non-zero values are treated as true, zero values as false.</p>
   */
  void or();

  /**
   * Perform a logical XOR operation on the two numbers on top of the stack.
   *
   * <p>Perform a logical XOR operation on the two numbers on top of the stack, pushing the result
   * of the calculation to the top of the stack. It assumes that the left operand was pushed prior
   * to the right operand such that the right operand is on the top of the stack and the left
   * operand is right below. Non-zero values are treated as true, zero values as false.</p>
   */
  void xor();

  /**
   * Perform an equality comparison on the two numbers on top of the stack.
   *
   * <p>Perform an equality comparison on the two numbers on top of the stack, pushing the result
   * of the calculation to the top of the stack. It assumes that the left operand was pushed prior
   * to the right operand such that the right operand is on the top of the stack and the left
   * operand is right below.</p>
   */
  void equals();

  /**
   * Perform a not-equals comparison on the two numbers on top of the stack.
   *
   * <p>Perform a not-equals comparison on the two numbers on top of the stack, pushing the result
   * of the calculation to the top of the stack. It assumes that the left operand was pushed prior
   * to the right operand such that the right operand is on the top of the stack and the left
   * operand is right below.</p>
   */
  void notEquals();

  /**
   * Perform a greater-than comparison on the two numbers on top of the stack.
   *
   * <p>Perform a greater-than comparison on the two numbers on top of the stack, pushing the result
   * of the calculation to the top of the stack. It assumes that the left operand was pushed prior
   * to the right operand such that the right operand is on the top of the stack and the left
   * operand is right below.</p>
   */
  void greaterThan();

  /**
   * Perform a less-than comparison on the two numbers on top of the stack.
   *
   * <p>Perform a less-than comparison on the two numbers on top of the stack, pushing the result
   * of the calculation to the top of the stack. It assumes that the left operand was pushed prior
   * to the right operand such that the right operand is on the top of the stack and the left
   * operand is right below.</p>
   */
  void lessThan();

  /**
   * Perform a greater-than-or-equal comparison on the two numbers on top of the stack.
   *
   * <p>Perform a greater-than-or-equal comparison on the two numbers on top of the stack, pushing
   * the result of the calculation to the top of the stack. It assumes that the left operand was
   * pushed prior to the right operand such that the right operand is on the top of the stack and
   * the left operand is right below.</p>
   */
  void greaterThanOrEqual();

  /**
   * Perform a less-than-or-equal comparison on the two numbers on top of the stack.
   *
   * <p>Perform a less-than-or-equal comparison on the two numbers on top of the stack, pushing the
   * result of the calculation to the top of the stack. It assumes that the left operand was pushed
   * prior to the right operand such that the right operand is on the top of the stack and the left
   * operand is right below.</p>
   */
  void lessThanOrEqual();
}
