/**
 * Unit tests for the DynamicCapFuture class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;

/**
 * Tests for the DynamicCapFuture class.
 */
public class DynamicCapFutureTest {

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
   * Test that DynamicCapFuture can be initialized.
   */
  @Test
  public void testInitializes() {
    DynamicCapFuture future = new DynamicCapFuture("beginning");
    assertNotNull(future, "DynamicCapFuture should be constructable");
  }

  /**
   * Test the realize method with "beginning" value.
   */
  @Test
  public void testRealizeBeginning() {
    DynamicCapFuture future = new DynamicCapFuture("beginning");

    TimePointRealized realized = future.realize(machine);

    assertNotNull(realized, "Realized time point should not be null");
    assertTrue(realized.isDynamicCap(), "Realized time point should be a dynamic cap");
    assertEquals("beginning", realized.getDynamicCap(),
        "Realized time point should have the correct dynamic cap value");
  }

  /**
   * Test the realize method with "onwards" value.
   */
  @Test
  public void testRealizeOnwards() {
    DynamicCapFuture future = new DynamicCapFuture("onwards");

    TimePointRealized realized = future.realize(machine);

    assertNotNull(realized, "Realized time point should not be null");
    assertTrue(realized.isDynamicCap(), "Realized time point should be a dynamic cap");
    assertEquals("onwards", realized.getDynamicCap(),
        "Realized time point should have the correct dynamic cap value");
  }

  /**
   * Test the realize method with a custom value.
   */
  @Test
  public void testRealizeCustomValue() {
    String customValue = "custom-cap-value";
    DynamicCapFuture future = new DynamicCapFuture(customValue);

    TimePointRealized realized = future.realize(machine);

    assertNotNull(realized, "Realized time point should not be null");
    assertTrue(realized.isDynamicCap(), "Realized time point should be a dynamic cap");
    assertEquals(customValue, realized.getDynamicCap(),
        "Realized time point should have the correct dynamic cap value");
  }
}
