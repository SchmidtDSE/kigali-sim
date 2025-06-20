/**
 * Unit tests for the AdditionOperation class.
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
 * Tests for the AdditionOperation class.
 */
public class AdditionOperationTest {

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
   * Test that AdditionOperation can be initialized.
   */
  @Test
  public void testInitializes() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    AdditionOperation operation = new AdditionOperation(left, right);
    assertNotNull(operation, "AdditionOperation should be constructable");
  }

  /**
   * Test the execute method.
   */
  @Test
  public void testExecute() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(3), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    AdditionOperation operation = new AdditionOperation(left, right);
    
    operation.execute(machine);
    
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(7), result.getValue(), "Addition should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after addition");
  }

  /**
   * Test the execute method with nested operations.
   */
  @Test
  public void testExecuteNested() {
    Operation leftInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(1), "kg"));
    Operation rightInner = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2), "kg"));
    Operation left = new AdditionOperation(leftInner, rightInner); // 1 + 2 = 3
    
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "kg"));
    AdditionOperation operation = new AdditionOperation(left, right); // (1 + 2) + 4 = 7
    
    operation.execute(machine);
    
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(7), result.getValue(), "Nested addition should work correctly");
    assertEquals("kg", result.getUnits(), "Units should be preserved after nested addition");
  }
}