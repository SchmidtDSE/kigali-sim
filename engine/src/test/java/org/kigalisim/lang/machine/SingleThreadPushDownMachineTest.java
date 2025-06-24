/**
 * Unit tests for the SingleThreadPushDownMachine class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for the SingleThreadPushDownMachine class.
 */
public class SingleThreadPushDownMachineTest {

  private Engine engine;
  private SingleThreadPushDownMachine machine;

  /**
   * Set up a new engine and machine before each test.
   */
  @BeforeEach
  public void setUp() {
    engine = new SingleThreadEngine(2020, 2025);
    machine = new SingleThreadPushDownMachine(engine);
  }

  /**
   * Test that SingleThreadPushDownMachine can be initialized.
   */
  @Test
  public void testInitializes() {
    assertNotNull(machine, "SingleThreadPushDownMachine should be constructable");
  }

  /**
   * Test the getEngine method.
   */
  @Test
  public void testGetEngine() {
    assertEquals(engine, machine.getEngine(), "getEngine should return the correct engine");
  }

  /**
   * Test pushing a value and getting the result.
   */
  @Test
  public void testPushAndGetResult() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(42), "kg");
    machine.push(value);
    EngineNumber result = machine.getResult();
    assertEquals(value.getValue(), result.getValue(), "Result value should match pushed value");
    assertEquals(value.getUnits(), result.getUnits(), "Result units should match pushed units");
  }

  /**
   * Test that getResult throws an exception when the stack is empty.
   */
  @Test
  public void testGetResultEmptyStack() {
    assertThrows(RuntimeException.class, () -> machine.getResult(),
        "getResult should throw RuntimeException when stack is empty");
  }

  /**
   * Test that getResult throws an exception when the stack has more than one value.
   */
  @Test
  public void testGetResultMultipleValues() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(2), "kg"));
    assertThrows(RuntimeException.class, () -> machine.getResult(),
        "getResult should throw RuntimeException when stack has more than one value");
  }

  /**
   * Test the add operation.
   */
  @Test
  public void testAdd() {
    machine.push(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    machine.add();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(7), result.getValue(), "Addition should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after addition");
  }

  /**
   * Test that add throws an exception when units don't match.
   */
  @Test
  public void testAddUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(4), "liters"));
    assertThrows(RuntimeException.class, () -> machine.add(),
        "add should throw RuntimeException when units don't match");
  }

  /**
   * Test the subtract operation.
   */
  @Test
  public void testSubtract() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    machine.subtract();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(6), result.getValue(), "Subtraction should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after subtraction");
  }

  /**
   * Test that subtract throws an exception when units don't match.
   */
  @Test
  public void testSubtractUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(4), "liters"));
    assertThrows(RuntimeException.class, () -> machine.subtract(),
        "subtract should throw RuntimeException when units don't match");
  }

  /**
   * Test the changeUnits operation with empty units.
   */
  @Test
  public void testChangeUnitsFromEmpty() {
    machine.push(new EngineNumber(BigDecimal.valueOf(42), ""));
    machine.changeUnits("kg");
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Value should be preserved after changing units");
    assertEquals("kg", result.getUnits(), "Units should be changed correctly");
  }

  /**
   * Test the changeUnits operation with matching units.
   */
  @Test
  public void testChangeUnitsSameUnits() {
    machine.push(new EngineNumber(BigDecimal.valueOf(42), "kg"));
    machine.changeUnits("kg");
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Value should be preserved after changing units");
    assertEquals("kg", result.getUnits(), "Units should remain the same");
  }

  /**
   * Test that changeUnits throws an exception when units don't match.
   */
  @Test
  public void testChangeUnitsUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(42), "kg"));
    assertThrows(RuntimeException.class, () -> machine.changeUnits("liters"),
        "changeUnits should throw RuntimeException when units don't match");
  }

  /**
   * Test the multiply operation.
   */
  @Test
  public void testMultiply() {
    machine.push(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    machine.multiply();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(12), result.getValue(), "Multiplication should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after multiplication");
  }

  /**
   * Test that multiply throws an exception when units don't match.
   */
  @Test
  public void testMultiplyUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(4), "liters"));
    assertThrows(RuntimeException.class, () -> machine.multiply(),
        "multiply should throw RuntimeException when units don't match");
  }

  /**
   * Test the divide operation.
   */
  @Test
  public void testDivide() {
    machine.push(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    machine.divide();
    EngineNumber result = machine.getResult();
    assertEquals(3, result.getValue().doubleValue(), 0.0001, "Division should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after division");
  }

  /**
   * Test that divide throws an exception when units don't match.
   */
  @Test
  public void testDivideUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(4), "liters"));
    assertThrows(RuntimeException.class, () -> machine.divide(),
        "divide should throw RuntimeException when units don't match");
  }

  /**
   * Test that divide throws an exception when dividing by zero.
   */
  @Test
  public void testDivideByZero() {
    machine.push(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    assertThrows(ArithmeticException.class, () -> machine.divide(),
        "divide should throw ArithmeticException when dividing by zero");
  }

  /**
   * Test the and operation with both operands true.
   */
  @Test
  public void testAndBothTrue() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.and();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "AND of true and true should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after AND operation");
  }

  /**
   * Test the and operation with one operand true and one false.
   */
  @Test
  public void testAndOneTrue() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    machine.and();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "AND of true and false should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after AND operation");
  }

  /**
   * Test the and operation with both operands false.
   */
  @Test
  public void testAndBothFalse() {
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    machine.and();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "AND of false and false should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after AND operation");
  }

  /**
   * Test that and throws an exception when units don't match.
   */
  @Test
  public void testAndUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "liters"));
    assertThrows(RuntimeException.class, () -> machine.and(),
        "and should throw RuntimeException when units don't match");
  }

  /**
   * Test the or operation with both operands true.
   */
  @Test
  public void testOrBothTrue() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.or();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "OR of true and true should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after OR operation");
  }

  /**
   * Test the or operation with one operand true and one false.
   */
  @Test
  public void testOrOneTrue() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    machine.or();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "OR of true and false should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after OR operation");
  }

  /**
   * Test the or operation with both operands false.
   */
  @Test
  public void testOrBothFalse() {
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    machine.or();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "OR of false and false should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after OR operation");
  }

  /**
   * Test that or throws an exception when units don't match.
   */
  @Test
  public void testOrUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "liters"));
    assertThrows(RuntimeException.class, () -> machine.or(),
        "or should throw RuntimeException when units don't match");
  }

  /**
   * Test the xor operation with both operands true.
   */
  @Test
  public void testXorBothTrue() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.xor();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "XOR of true and true should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after XOR operation");
  }

  /**
   * Test the xor operation with one operand true and one false.
   */
  @Test
  public void testXorOneTrue() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    machine.xor();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "XOR of true and false should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after XOR operation");
  }

  /**
   * Test the xor operation with both operands false.
   */
  @Test
  public void testXorBothFalse() {
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    machine.xor();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "XOR of false and false should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after XOR operation");
  }

  /**
   * Test that xor throws an exception when units don't match.
   */
  @Test
  public void testXorUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(1), "liters"));
    assertThrows(RuntimeException.class, () -> machine.xor(),
        "xor should throw RuntimeException when units don't match");
  }

  /**
   * Test the equals operation with equal values.
   */
  @Test
  public void testEqualsEqual() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.equals();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "EQUALS of equal values should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after EQUALS operation");
  }

  /**
   * Test the equals operation with unequal values.
   */
  @Test
  public void testEqualsUnequal() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.equals();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "EQUALS of unequal values should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after EQUALS operation");
  }

  /**
   * Test that equals throws an exception when units don't match.
   */
  @Test
  public void testEqualsUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "liters"));
    assertThrows(RuntimeException.class, () -> machine.equals(),
        "equals should throw RuntimeException when units don't match");
  }

  /**
   * Test the notEquals operation with equal values.
   */
  @Test
  public void testNotEqualsEqual() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.notEquals();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "NOT_EQUALS of equal values should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after NOT_EQUALS operation");
  }

  /**
   * Test the notEquals operation with unequal values.
   */
  @Test
  public void testNotEqualsUnequal() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.notEquals();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "NOT_EQUALS of unequal values should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after NOT_EQUALS operation");
  }

  /**
   * Test that notEquals throws an exception when units don't match.
   */
  @Test
  public void testNotEqualsUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "liters"));
    assertThrows(RuntimeException.class, () -> machine.notEquals(),
        "notEquals should throw RuntimeException when units don't match");
  }

  /**
   * Test the greaterThan operation with greater value.
   */
  @Test
  public void testGreaterThanGreater() {
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.greaterThan();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "GREATER_THAN with greater value should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after GREATER_THAN operation");
  }

  /**
   * Test the greaterThan operation with equal value.
   */
  @Test
  public void testGreaterThanEqual() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.greaterThan();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "GREATER_THAN with equal value should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after GREATER_THAN operation");
  }

  /**
   * Test the greaterThan operation with lesser value.
   */
  @Test
  public void testGreaterThanLesser() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.greaterThan();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "GREATER_THAN with lesser value should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after GREATER_THAN operation");
  }

  /**
   * Test that greaterThan throws an exception when units don't match.
   */
  @Test
  public void testGreaterThanUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "liters"));
    assertThrows(RuntimeException.class, () -> machine.greaterThan(),
        "greaterThan should throw RuntimeException when units don't match");
  }

  /**
   * Test the lessThan operation with lesser value.
   */
  @Test
  public void testLessThanLesser() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.lessThan();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "LESS_THAN with lesser value should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after LESS_THAN operation");
  }

  /**
   * Test the lessThan operation with equal value.
   */
  @Test
  public void testLessThanEqual() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.lessThan();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "LESS_THAN with equal value should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after LESS_THAN operation");
  }

  /**
   * Test the lessThan operation with greater value.
   */
  @Test
  public void testLessThanGreater() {
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.lessThan();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "LESS_THAN with greater value should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after LESS_THAN operation");
  }

  /**
   * Test that lessThan throws an exception when units don't match.
   */
  @Test
  public void testLessThanUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "liters"));
    assertThrows(RuntimeException.class, () -> machine.lessThan(),
        "lessThan should throw RuntimeException when units don't match");
  }

  /**
   * Test the greaterThanOrEqual operation with greater value.
   */
  @Test
  public void testGreaterThanOrEqualGreater() {
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.greaterThanOrEqual();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "GREATER_THAN_OR_EQUAL with greater value should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after GREATER_THAN_OR_EQUAL operation");
  }

  /**
   * Test the greaterThanOrEqual operation with equal value.
   */
  @Test
  public void testGreaterThanOrEqualEqual() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.greaterThanOrEqual();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "GREATER_THAN_OR_EQUAL with equal value should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after GREATER_THAN_OR_EQUAL operation");
  }

  /**
   * Test the greaterThanOrEqual operation with lesser value.
   */
  @Test
  public void testGreaterThanOrEqualLesser() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.greaterThanOrEqual();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "GREATER_THAN_OR_EQUAL with lesser value should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after GREATER_THAN_OR_EQUAL operation");
  }

  /**
   * Test that greaterThanOrEqual throws an exception when units don't match.
   */
  @Test
  public void testGreaterThanOrEqualUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "liters"));
    assertThrows(RuntimeException.class, () -> machine.greaterThanOrEqual(),
        "greaterThanOrEqual should throw RuntimeException when units don't match");
  }

  /**
   * Test the lessThanOrEqual operation with lesser value.
   */
  @Test
  public void testLessThanOrEqualLesser() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.lessThanOrEqual();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "LESS_THAN_OR_EQUAL with lesser value should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after LESS_THAN_OR_EQUAL operation");
  }

  /**
   * Test the lessThanOrEqual operation with equal value.
   */
  @Test
  public void testLessThanOrEqualEqual() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.lessThanOrEqual();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ONE, result.getValue(), "LESS_THAN_OR_EQUAL with equal value should be true");
    assertEquals("kg", result.getUnits(), "Units should be preserved after LESS_THAN_OR_EQUAL operation");
  }

  /**
   * Test the lessThanOrEqual operation with greater value.
   */
  @Test
  public void testLessThanOrEqualGreater() {
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.lessThanOrEqual();
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "LESS_THAN_OR_EQUAL with greater value should be false");
    assertEquals("kg", result.getUnits(), "Units should be preserved after LESS_THAN_OR_EQUAL operation");
  }

  /**
   * Test that lessThanOrEqual throws an exception when units don't match.
   */
  @Test
  public void testLessThanOrEqualUnitsMismatch() {
    machine.push(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    machine.push(new EngineNumber(BigDecimal.valueOf(20), "liters"));
    assertThrows(RuntimeException.class, () -> machine.lessThanOrEqual(),
        "lessThanOrEqual should throw RuntimeException when units don't match");
  }
}
