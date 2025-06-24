/**
 * Unit tests for the SubtractionOperation class.
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
 * Tests for the SubtractionOperation class.
 */
public class SubtractionOperationTest {

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
   * Test that SubtractionOperation can be initialized.
   */
  @Test
  public void testInitializes() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    SubtractionOperation operation = new SubtractionOperation(left, right);
    assertNotNull(operation, "SubtractionOperation should be constructable");
  }

  /**
   * Test the execute method.
   */
  @Test
  public void testExecute() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    SubtractionOperation operation = new SubtractionOperation(left, right);

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(6), result.getValue(), "Subtraction should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after subtraction");
  }

  /**
   * Test the execute method with nested operations.
   */
  @Test
  public void testExecuteNested() {
    Operation leftInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(15), "kg"));
    Operation rightInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(5), "kg"));
    Operation left = new SubtractionOperation(leftInner, rightInner); // 15 - 5 = 10

    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    SubtractionOperation operation = new SubtractionOperation(left, right); // (15 - 5) - 4 = 6

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(6), result.getValue(), "Nested subtraction should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after nested subtraction");
  }

  /**
   * Test the execute method with mixed operations.
   */
  @Test
  public void testExecuteMixed() {
    Operation leftInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(5), "kg"));
    Operation rightInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    Operation left = new AdditionOperation(leftInner, rightInner); // 5 + 3 = 8

    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2), "kg"));
    SubtractionOperation operation = new SubtractionOperation(left, right); // (5 + 3) - 2 = 6

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(6), result.getValue(), "Mixed operations should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after mixed operations");
  }
}
