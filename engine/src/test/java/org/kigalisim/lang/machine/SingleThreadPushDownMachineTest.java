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
}