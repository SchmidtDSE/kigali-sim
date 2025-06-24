/**
 * Unit tests for the FloorOperation class.
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
 * Tests for the FloorOperation class.
 */
public class FloorOperationTest {

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
   * Test that FloorOperation can be initialized with no during and no displacement.
   */
  @Test
  public void testInitializesNoDuringNoDisplacement() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    FloorOperation operation = new FloorOperation("manufacture", valueOperation);
    assertNotNull(operation, "FloorOperation should be constructable without during and displacement");
  }

  /**
   * Test that FloorOperation can be initialized with a during but no displacement.
   */
  @Test
  public void testInitializesWithDuringNoDisplacement() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    FloorOperation operation = new FloorOperation("manufacture", valueOperation, during);
    assertNotNull(operation, "FloorOperation should be constructable with during but no displacement");
  }

  /**
   * Test that FloorOperation can be initialized with displacement but no during.
   */
  @Test
  public void testInitializesWithDisplacementNoDuring() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    FloorOperation operation = new FloorOperation("manufacture", valueOperation, "import");
    assertNotNull(operation, "FloorOperation should be constructable with displacement but no during");
  }

  /**
   * Test that FloorOperation can be initialized with both during and displacement.
   */
  @Test
  public void testInitializesWithDuringAndDisplacement() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    FloorOperation operation = new FloorOperation("manufacture", valueOperation, "import", during);
    assertNotNull(operation, "FloorOperation should be constructable with both during and displacement");
  }

  /**
   * Test the execute method with no during and no displacement when flooring is needed.
   */
  @Test
  public void testExecuteNoDuringNoDisplacementWithFlooring() {
    // Set up a stream with a value lower than the floor
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(20), "kg"), Optional.empty());

    // Create a floor operation with a higher value
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    FloorOperation operation = new FloorOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the stream was floored in the engine
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Stream value should be floored to 42");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with no during and no displacement when no flooring is needed.
   */
  @Test
  public void testExecuteNoDuringNoDisplacementWithoutFlooring() {
    // Set up a stream with a value higher than the floor
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(100), "kg"), Optional.empty());

    // Create a floor operation with a lower value
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    FloorOperation operation = new FloorOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the stream was not changed
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(100), result.getValue(), "Stream value should remain 100");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with a during that applies.
   */
  @Test
  public void testExecuteWithDuringApplying() {
    // Set up a stream with a value lower than the floor
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(20), "kg"), Optional.empty());

    // Create a floor operation with a higher value and a during that applies to the current year
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2020), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    FloorOperation operation = new FloorOperation("manufacture", valueOperation, during);

    operation.execute(machine);

    // Verify the stream was floored
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Stream value should be floored to 42");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with a during that doesn't apply.
   */
  @Test
  public void testExecuteWithDuringNotApplying() {
    // Set up a stream with a value lower than the floor
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(20), "kg"), Optional.empty());

    // Create a floor operation with a higher value and a during that applies to a future year
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2021), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    FloorOperation operation = new FloorOperation("manufacture", valueOperation, during);

    operation.execute(machine);

    // Verify the stream was not floored
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(20), result.getValue(), "Stream value should remain 20");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with a complex value operation.
   */
  @Test
  public void testExecuteWithComplexValueOperation() {
    // Set up a stream with a value lower than the floor
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(20), "kg"), Optional.empty());

    // Create a floor operation with a complex value operation
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(30), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    Operation valueOperation = new AdditionOperation(left, right); // 30 + 12 = 42

    FloorOperation operation = new FloorOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the stream was floored
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Stream value should be floored to 42");
    assertEquals("kg", result.getUnits(), "Stream units should remain kg");
  }

  /**
   * Test the execute method with displacement.
   */
  @Test
  public void testExecuteWithDisplacement() {
    // Set up the source stream with a value lower than the floor
    engine.setStream("manufacture", new EngineNumber(BigDecimal.valueOf(20), "kg"), Optional.empty());

    // Set up the target stream for displacement with an initial value
    engine.setStream("import", new EngineNumber(BigDecimal.valueOf(50), "kg"), Optional.empty());

    // Create a floor operation with displacement
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    FloorOperation operation = new FloorOperation("manufacture", valueOperation, "import");

    operation.execute(machine);

    // Verify the source stream was floored
    EngineNumber sourceResult = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(42), sourceResult.getValue(), "Source stream should be floored to 42");
    assertEquals("kg", sourceResult.getUnits(), "Source stream units should remain kg");

    // Verify the displacement effect on the target stream
    // The floor operation adds 22 kg to the source stream, and the displacement
    // subtracts the same amount from the target stream
    EngineNumber targetResult = engine.getStream("import");
    assertEquals(BigDecimal.valueOf(28), targetResult.getValue(), "Target stream should be 50 - 22 = 28");
    assertEquals("kg", targetResult.getUnits(), "Target stream units should be kg");
  }
}
