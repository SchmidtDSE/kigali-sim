/**
 * Unit tests for the DivisionOperation class.
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
 * Tests for the DivisionOperation class.
 */
public class DivisionOperationTest {

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
   * Test that DivisionOperation can be initialized.
   */
  @Test
  public void testInitializes() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    DivisionOperation operation = new DivisionOperation(left, right);
    assertNotNull(operation, "DivisionOperation should be constructable");
  }

  /**
   * Test the execute method.
   */
  @Test
  public void testExecute() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    DivisionOperation operation = new DivisionOperation(left, right);

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(3), result.getValue(), "Division should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after division");
  }

  /**
   * Test the execute method with nested operations.
   */
  @Test
  public void testExecuteNested() {
    Operation leftInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(24), "kg"));
    Operation rightInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2), "kg"));
    Operation left = new DivisionOperation(leftInner, rightInner); // 24 / 2 = 12

    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    DivisionOperation operation = new DivisionOperation(left, right); // (24 / 2) / 4 = 3

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(3), result.getValue(), "Nested division should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after nested division");
  }

  /**
   * Test that division by zero throws an exception.
   */
  @Test
  public void testDivideByZero() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    DivisionOperation operation = new DivisionOperation(left, right);

    assertThrows(ArithmeticException.class, () -> operation.execute(machine),
        "Division by zero should throw ArithmeticException");
  }
}