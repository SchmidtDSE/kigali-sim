/**
 * Unit tests for the GetStreamOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;

/**
 * Tests for the GetStreamOperation class.
 */
public class GetStreamOperationTest {

  private Engine engine;
  private PushDownMachine machine;

  /**
   * Set up a new engine and machine before each test.
   */
  @BeforeEach
  public void setUp() {
    engine = new SingleThreadEngine(2020, 2025);
    machine = new SingleThreadPushDownMachine(engine);

    // Set up the engine with a scope
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");
  }

  /**
   * Test that GetStreamOperation can be initialized with just a stream name.
   */
  @Test
  public void testInitializesWithStreamName() {
    GetStreamOperation operation = new GetStreamOperation("manufacture");
    assertNotNull(operation, "GetStreamOperation should be constructable with stream name");
  }

  /**
   * Test that GetStreamOperation can be initialized with a stream name and units.
   */
  @Test
  public void testInitializesWithStreamNameAndUnits() {
    GetStreamOperation operation = new GetStreamOperation("manufacture", "kg");
    assertNotNull(operation, "GetStreamOperation should be constructable with stream name and units");
  }

  /**
   * Test the execute method retrieves a stream value.
   */
  @Test
  public void testExecuteRetrievesStreamValue() {
    // Set a stream value in the engine
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    engine.setStream("manufacture", number, Optional.empty());

    // Create and execute the operation
    GetStreamOperation operation = new GetStreamOperation("manufacture");
    operation.execute(machine);

    // Verify the value was pushed onto the stack
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Stream value should be retrieved correctly");
    assertEquals("kg", result.getUnits(), "Stream units should be retrieved correctly");
  }

  /**
   * Test the execute method retrieves a stream value with unit conversion.
   */
  @Test
  public void testExecuteRetrievesStreamValueWithUnitConversion() {
    // Set a stream value in the engine
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(1), "kg");
    engine.setStream("manufacture", number, Optional.empty());

    // Create and execute the operation with unit conversion
    GetStreamOperation operation = new GetStreamOperation("manufacture", "mt");
    operation.execute(machine);

    // Verify the value was pushed onto the stack with converted units
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(0.001), result.getValue(), "Stream value should be converted correctly");
    assertEquals("mt", result.getUnits(), "Stream units should be converted correctly");
  }

  /**
   * Test the execute method with a non-existent stream throws an exception.
   */
  @Test
  public void testExecuteWithNonExistentStream() {
    // Create the operation with a stream that doesn't exist
    GetStreamOperation operation = new GetStreamOperation("non_existent_stream");

    // Verify that executing the operation throws an IllegalArgumentException
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> operation.execute(machine),
        "Should throw IllegalArgumentException for non-existent stream"
    );

    // Verify the exception message
    assertTrue(exception.getMessage().contains("Unknown stream: non_existent_stream"),
        "Exception message should indicate unknown stream");
  }
}
