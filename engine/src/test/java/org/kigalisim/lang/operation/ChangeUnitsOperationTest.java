/**
 * Unit tests for the ChangeUnitsOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;

/**
 * Tests for the ChangeUnitsOperation class.
 */
public class ChangeUnitsOperationTest {

  private Engine engine;
  private PushDownMachine machine;

  /**
   * Set up a new engine and machine before each test.
   */
  @BeforeEach
  public void setUp() {
    engine = new SingleThreadEngine(2020, 2025);
    machine = new SingleThreadPushDownMachine(engine);
  }

  /**
   * Test that ChangeUnitsOperation can be initialized.
   */
  @Test
  public void testInitializes() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(42), ""));
    ChangeUnitsOperation operation = new ChangeUnitsOperation(operand, "kg");
    assertNotNull(operation, "ChangeUnitsOperation should be constructable");
  }

  /**
   * Test the execute method with empty units.
   */
  @Test
  public void testExecuteFromEmptyUnits() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(42), ""));
    ChangeUnitsOperation operation = new ChangeUnitsOperation(operand, "kg");

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Value should be preserved after changing units");
    assertEquals("kg", result.getUnits(), "Units should be changed correctly");
  }

  /**
   * Test the execute method with matching units.
   */
  @Test
  public void testExecuteWithMatchingUnits() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(42), "kg"));
    ChangeUnitsOperation operation = new ChangeUnitsOperation(operand, "kg");

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Value should be preserved after changing units");
    assertEquals("kg", result.getUnits(), "Units should remain the same");
  }

  /**
   * Test the execute method with nested operations.
   */
  @Test
  public void testExecuteWithNestedOperations() {
    Operation leftInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(30), ""));
    Operation rightInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(12), ""));
    Operation operand = new AdditionOperation(leftInner, rightInner); // 30 + 12 = 42

    ChangeUnitsOperation operation = new ChangeUnitsOperation(operand, "kg");

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Value should be calculated correctly");
    assertEquals("kg", result.getUnits(), "Units should be applied correctly after calculation");
  }

  /**
   * Test that execute throws an exception when units don't match.
   */
  @Test
  public void testExecuteWithUnitsMismatch() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(42), "liters"));
    ChangeUnitsOperation operation = new ChangeUnitsOperation(operand, "kg");

    assertThrows(RuntimeException.class, () -> operation.execute(machine),
        "execute should throw RuntimeException when units don't match");
  }
}
