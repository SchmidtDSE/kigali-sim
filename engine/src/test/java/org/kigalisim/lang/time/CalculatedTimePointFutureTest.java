/**
 * Unit tests for the CalculatedTimePointFuture class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.operation.PreCalculatedOperation;

/**
 * Tests for the CalculatedTimePointFuture class.
 */
public class CalculatedTimePointFutureTest {

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
   * Test that CalculatedTimePointFuture can be initialized.
   */
  @Test
  public void testInitializes() {
    Operation operation = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2020), ""));
    CalculatedTimePointFuture future = new CalculatedTimePointFuture(operation);
    assertNotNull(future, "CalculatedTimePointFuture should be constructable");
  }

  /**
   * Test the realize method.
   */
  @Test
  public void testRealize() {
    Operation operation = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2020), ""));
    CalculatedTimePointFuture future = new CalculatedTimePointFuture(operation);

    TimePointRealized realized = future.realize(machine);

    assertNotNull(realized, "Realized time point should not be null");
    assertFalse(realized.isDynamicCap(), "Realized time point should not be a dynamic cap");
    assertEquals(BigDecimal.valueOf(2020), realized.getPointValue().getValue(),
        "Realized time point should have the correct value");
    assertEquals("", realized.getPointValue().getUnits(),
        "Realized time point should have empty units");
  }

  /**
   * Test that realize throws an exception when the operation result has units.
   */
  @Test
  public void testRealizeWithUnits() {
    Operation operation = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2020), "kg"));
    CalculatedTimePointFuture future = new CalculatedTimePointFuture(operation);

    assertThrows(IllegalStateException.class, () -> future.realize(machine),
        "realize should throw IllegalStateException when operation result has units");
  }
}
