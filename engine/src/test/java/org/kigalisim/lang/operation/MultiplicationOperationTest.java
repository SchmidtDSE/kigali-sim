/**
 * Unit tests for the MultiplicationOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;

/**
 * Tests for the MultiplicationOperation class.
 */
public class MultiplicationOperationTest {

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
   * Test that MultiplicationOperation can be initialized.
   */
  @Test
  public void testInitializes() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    MultiplicationOperation operation = new MultiplicationOperation(left, right);
    assertNotNull(operation, "MultiplicationOperation should be constructable");
  }

  /**
   * Test the execute method.
   */
  @Test
  public void testExecute() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    MultiplicationOperation operation = new MultiplicationOperation(left, right);

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(12), result.getValue(), "Multiplication should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after multiplication");
  }

  /**
   * Test the execute method with nested operations.
   */
  @Test
  public void testExecuteNested() {
    Operation leftInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2), "kg"));
    Operation rightInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    Operation left = new MultiplicationOperation(leftInner, rightInner); // 2 * 3 = 6

    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    MultiplicationOperation operation = new MultiplicationOperation(left, right); // (2 * 3) * 4 = 24

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(24), result.getValue(), "Nested multiplication should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after nested multiplication");
  }
}
