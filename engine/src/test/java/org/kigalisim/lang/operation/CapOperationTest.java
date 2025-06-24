/**
 * Unit tests for the CapOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;
import org.kigalisim.lang.time.CalculatedTimePointFuture;
import org.kigalisim.lang.time.ParsedDuring;
import org.kigalisim.lang.time.TimePointFuture;

/**
 * Tests for the CapOperation class.
 */
public class CapOperationTest {

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
   * Test that CapOperation can be initialized with no during and no displacement.
   */
  @Test
  public void testInitializesNoDuringNoDisplacement() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    CapOperation operation = new CapOperation("manufacture", valueOperation);
    assertNotNull(operation, "CapOperation should be constructable without during and displacement");
  }

  /**
   * Test that CapOperation can be initialized with a during but no displacement.
   */
  @Test
  public void testInitializesWithDuringNoDisplacement() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    CapOperation operation = new CapOperation("manufacture", valueOperation, during);
    assertNotNull(operation, "CapOperation should be constructable with during but no displacement");
  }

  /**
   * Test that CapOperation can be initialized with displacement but no during.
   */
  @Test
  public void testInitializesWithDisplacementNoDuring() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    CapOperation operation = new CapOperation("manufacture", valueOperation, "other_stream");
    assertNotNull(operation, "CapOperation should be constructable with displacement but no during");
  }

  /**
   * Test that CapOperation can be initialized with both during and displacement.
   */
  @Test
  public void testInitializesWithDuringAndDisplacement() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    CapOperation operation = new CapOperation("manufacture", valueOperation, "other_stream", during);
    assertNotNull(operation, "CapOperation should be constructable with both during and displacement");
  }

  /**
   * Test the execute method with no during and no displacement when capping is needed.
   */
  @Test
  public void testExecuteNoDuringNoDisplacementWithCapping() {
    // Set up a stream with a value higher than the cap
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(100), "kg"), Optional.empty());

    // Create a cap operation with a lower value
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    CapOperation operation = new CapOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the stream was capped in the engine
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Stream value should be capped to 42");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with no during and no displacement when no capping is needed.
   */
  @Test
  public void testExecuteNoDuringNoDisplacementWithoutCapping() {
    // Set up a stream with a value lower than the cap
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(20), "kg"), Optional.empty());

    // Create a cap operation with a higher value
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    CapOperation operation = new CapOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the stream was not changed
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(20), result.getValue(), "Stream value should remain 20");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with a during that applies.
   */
  @Test
  public void testExecuteWithDuringApplying() {
    // Set up a stream with a value higher than the cap
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(100), "kg"), Optional.empty());

    // Create a cap operation with a lower value and a during that applies to the current year
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2020), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    CapOperation operation = new CapOperation("manufacture", valueOperation, during);

    operation.execute(machine);

    // Verify the stream was capped
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Stream value should be capped to 42");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with a during that doesn't apply.
   */
  @Test
  public void testExecuteWithDuringNotApplying() {
    // Set up a stream with a value higher than the cap
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(100), "kg"), Optional.empty());

    // Create a cap operation with a lower value and a during that applies to a future year
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2021), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    CapOperation operation = new CapOperation("manufacture", valueOperation, during);

    operation.execute(machine);

    // Verify the stream was not capped
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(100), result.getValue(), "Stream value should remain 100");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with a complex value operation.
   */
  @Test
  public void testExecuteWithComplexValueOperation() {
    // Set up a stream with a value higher than the cap
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(100), "kg"), Optional.empty());

    // Create a cap operation with a complex value operation
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(30), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    Operation valueOperation = new AdditionOperation(left, right); // 30 + 12 = 42

    CapOperation operation = new CapOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the stream was capped
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Stream value should be capped to 42");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with displacement.
   */
  @Test
  public void testExecuteWithDisplacement() {
    // Set up the source stream with a value higher than the cap
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(100), "kg"), Optional.empty());

    // Set up the target stream for displacement
    engine.setStream("import", new EngineNumber(BigDecimal.valueOf(0), "kg"), Optional.empty());

    // Create a cap operation with displacement
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    CapOperation operation = new CapOperation("manufacture", valueOperation, "import");

    operation.execute(machine);

    // Verify the source stream was capped
    EngineNumber sourceResult = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(42), sourceResult.getValue(), "Source stream should be capped to 42");
    assertEquals("kg", sourceResult.getUnits(), "Source stream units should remain kg");

    // Verify the excess was displaced to the target stream
    EngineNumber targetResult = engine.getStream("import");
    assertEquals(BigDecimal.valueOf(58), targetResult.getValue(), "Target stream should receive the excess 58");
    assertEquals("kg", targetResult.getUnits(), "Target stream units should be kg");
  }
}
