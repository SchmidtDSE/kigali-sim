/**
 * Unit tests for the PreCalculatedOperation class.
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
 * Tests for the PreCalculatedOperation class.
 */
public class PreCalculatedOperationTest {

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
   * Test that PreCalculatedOperation can be initialized.
   */
  @Test
  public void testInitializes() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(42), "kg");
    PreCalculatedOperation operation = new PreCalculatedOperation(value);
    assertNotNull(operation, "PreCalculatedOperation should be constructable");
  }

  /**
   * Test the execute method.
   */
  @Test
  public void testExecute() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(42), "kg");
    PreCalculatedOperation operation = new PreCalculatedOperation(value);

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(value.getValue(), result.getValue(), "Value should be preserved after execution");
    assertEquals(value.getUnits(), result.getUnits(), "Units should be preserved after execution");
  }

  /**
   * Test the execute method with zero value.
   */
  @Test
  public void testExecuteWithZero() {
    EngineNumber value = new EngineNumber(BigDecimal.ZERO, "kg");
    PreCalculatedOperation operation = new PreCalculatedOperation(value);

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.ZERO, result.getValue(), "Zero value should be preserved after execution");
    assertEquals("kg", result.getUnits(), "Units should be preserved after execution");
  }

  /**
   * Test the execute method with empty units.
   */
  @Test
  public void testExecuteWithEmptyUnits() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(42), "");
    PreCalculatedOperation operation = new PreCalculatedOperation(value);

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Value should be preserved after execution");
    assertEquals("", result.getUnits(), "Empty units should be preserved after execution");
  }

  /**
   * Test the execute method with negative value.
   */
  @Test
  public void testExecuteWithNegativeValue() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(-42), "kg");
    PreCalculatedOperation operation = new PreCalculatedOperation(value);

    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(-42), result.getValue(), "Negative value should be preserved after execution");
    assertEquals("kg", result.getUnits(), "Units should be preserved after execution");
  }
}
