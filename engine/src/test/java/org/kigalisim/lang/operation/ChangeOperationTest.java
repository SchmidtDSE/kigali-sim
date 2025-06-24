/**
 * Unit tests for the ChangeOperation class.
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
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;
import org.kigalisim.lang.time.CalculatedTimePointFuture;
import org.kigalisim.lang.time.ParsedDuring;
import org.kigalisim.lang.time.TimePointFuture;

/**
 * Tests for the ChangeOperation class.
 */
public class ChangeOperationTest {

  private Engine engine;
  private PushDownMachine machine;
  private YearMatcher allYearsMatcher;

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

    // Create a matcher that matches all years
    allYearsMatcher = YearMatcher.unbounded();
  }

  /**
   * Test that ChangeOperation can be initialized with no during.
   */
  @Test
  public void testInitializesNoDuring() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    ChangeOperation operation = new ChangeOperation("manufacture", valueOperation);
    assertNotNull(operation, "ChangeOperation should be constructable without during");
  }

  /**
   * Test that ChangeOperation can be initialized with a during.
   */
  @Test
  public void testInitializesWithDuring() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    ChangeOperation operation = new ChangeOperation("manufacture", valueOperation, during);
    assertNotNull(operation, "ChangeOperation should be constructable with during");
  }

  /**
   * Test the execute method with no during.
   */
  @Test
  public void testExecuteNoDuring() {
    // Set initial value
    EngineNumber initialNumber = new EngineNumber(BigDecimal.valueOf(100), "kg");
    engine.setStream("manufacture", initialNumber, Optional.ofNullable(allYearsMatcher));

    // Change the value
    EngineNumber changeNumber = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(changeNumber);
    ChangeOperation operation = new ChangeOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the stream was changed in the engine
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(142), result.getValue(), "Stream value should be changed correctly");
    assertEquals("kg", result.getUnits(), "Stream units should remain the same");
  }

  /**
   * Test the execute method with a during.
   */
  @Test
  public void testExecuteWithDuring() {
    // Set initial value
    EngineNumber initialNumber = new EngineNumber(BigDecimal.valueOf(100), "kg");
    engine.setStream("manufacture", initialNumber, Optional.ofNullable(allYearsMatcher));

    // Change the value with a during that applies to the current year (2020)
    EngineNumber changeNumber = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(changeNumber);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2020), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    ChangeOperation operation = new ChangeOperation("manufacture", valueOperation, during);

    operation.execute(machine);

    // Verify the stream was changed in the engine
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(142), result.getValue(), "Stream value should be changed correctly");
    assertEquals("kg", result.getUnits(), "Stream units should remain the same");
  }

  /**
   * Test the execute method with a during that doesn't apply.
   */
  @Test
  public void testExecuteWithDuringNotApplying() {
    // Set initial value
    EngineNumber initialNumber = new EngineNumber(BigDecimal.valueOf(100), "kg");
    engine.setStream("manufacture", initialNumber, Optional.ofNullable(allYearsMatcher));

    // Change the value with a during that applies to a future year (2021)
    EngineNumber changeNumber = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(changeNumber);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2021), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    ChangeOperation operation = new ChangeOperation("manufacture", valueOperation, during);

    operation.execute(machine);

    // Verify the stream was not changed in the engine (should still be 100)
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(100), result.getValue(),
        "Stream value should not be changed when during doesn't apply");
    assertEquals("kg", result.getUnits(), "Stream units should remain the same");
  }

  /**
   * Test the execute method with a complex value operation.
   */
  @Test
  public void testExecuteWithComplexValueOperation() {
    // Set initial value
    EngineNumber initialNumber = new EngineNumber(BigDecimal.valueOf(100), "kg");
    engine.setStream("manufacture", initialNumber, Optional.ofNullable(allYearsMatcher));

    // Change the value with a complex operation
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(30), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    Operation valueOperation = new AdditionOperation(left, right); // 30 + 12 = 42

    ChangeOperation operation = new ChangeOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the stream was changed in the engine
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(142), result.getValue(),
        "Stream value should be calculated and changed correctly");
    assertEquals("kg", result.getUnits(), "Stream units should remain the same");
  }

  /**
   * Test the execute method with eachyear units.
   */
  @Test
  public void testExecuteWithEachYearUnits() {
    // Set initial value
    EngineNumber initialNumber = new EngineNumber(BigDecimal.valueOf(100), "kg");
    engine.setStream("manufacture", initialNumber, Optional.ofNullable(allYearsMatcher));

    // Change the value with eachyear units
    EngineNumber changeNumber = new EngineNumber(BigDecimal.valueOf(42), "kgeachyear");
    Operation valueOperation = new PreCalculatedOperation(changeNumber);
    ChangeOperation operation = new ChangeOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the stream was changed in the engine and units were handled correctly
    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(142), result.getValue(), "Stream value should be changed correctly");
    assertEquals("kg", result.getUnits(), "Stream units should be extracted from eachyear units");
  }
}
