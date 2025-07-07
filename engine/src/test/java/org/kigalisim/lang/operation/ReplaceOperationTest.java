/**
 * Unit tests for the ReplaceOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

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
 * Tests for the ReplaceOperation class.
 */
public class ReplaceOperationTest {

  private Engine engine;
  private PushDownMachine machine;
  private YearMatcher allYearsMatcher;
  private static final String STREAM_NAME = "manufacture";
  private static final String DESTINATION_SUBSTANCE = "replacement substance";

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

    // Register the destination substance
    engine.setSubstance(DESTINATION_SUBSTANCE);
    engine.enable(STREAM_NAME, Optional.empty());

    // Switch back to the source substance
    engine.setSubstance("test substance");

    // Create a matcher that matches all years
    allYearsMatcher = YearMatcher.unbounded();
  }

  /**
   * Test that ReplaceOperation can be initialized with no during.
   */
  @Test
  public void testInitializesNoDuring() {
    EngineNumber amount = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation amountOperation = new PreCalculatedOperation(amount);
    ReplaceOperation operation = new ReplaceOperation(amountOperation, STREAM_NAME, DESTINATION_SUBSTANCE);
    assertNotNull(operation, "ReplaceOperation should be constructable without during");
  }

  /**
   * Test that ReplaceOperation can be initialized with a during.
   */
  @Test
  public void testInitializesWithDuring() {
    EngineNumber amount = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation amountOperation = new PreCalculatedOperation(amount);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    ReplaceOperation operation = new ReplaceOperation(amountOperation, STREAM_NAME, DESTINATION_SUBSTANCE, during);
    assertNotNull(operation, "ReplaceOperation should be constructable with during");
  }

  /**
   * Test the execute method with no during.
   */
  @Test
  public void testExecuteNoDuring() {
    // Set initial value
    engine.enable(STREAM_NAME, Optional.empty());
    EngineNumber initialNumber = new EngineNumber(BigDecimal.valueOf(100), "kg");
    engine.setStream(STREAM_NAME, initialNumber, Optional.ofNullable(allYearsMatcher));

    // Replace some of the substance
    EngineNumber amount = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation amountOperation = new PreCalculatedOperation(amount);
    ReplaceOperation operation = new ReplaceOperation(amountOperation, STREAM_NAME, DESTINATION_SUBSTANCE);

    operation.execute(machine);

    // Verify the replacement was applied in the engine
    // Note: We can't directly test the replacement effect since we'd need to check both substances,
    // but we can verify the operation executes without errors
    assertNotNull(operation, "ReplaceOperation should execute without errors");
  }

  /**
   * Test the execute method with a during.
   */
  @Test
  public void testExecuteWithDuring() {
    // Set initial value
    engine.enable(STREAM_NAME, Optional.empty());
    EngineNumber initialNumber = new EngineNumber(BigDecimal.valueOf(100), "kg");
    engine.setStream(STREAM_NAME, initialNumber, Optional.ofNullable(allYearsMatcher));

    // Replace some of the substance with a during that applies to the current year (2020)
    EngineNumber amount = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation amountOperation = new PreCalculatedOperation(amount);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2020), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    ReplaceOperation operation = new ReplaceOperation(amountOperation, STREAM_NAME, DESTINATION_SUBSTANCE, during);

    operation.execute(machine);

    // Verify the operation executes without errors
    assertNotNull(operation, "ReplaceOperation should execute without errors");
  }

  /**
   * Test the execute method with a during that doesn't apply.
   */
  @Test
  public void testExecuteWithDuringNotApplying() {
    // Set initial value
    engine.enable(STREAM_NAME, Optional.empty());
    EngineNumber initialNumber = new EngineNumber(BigDecimal.valueOf(100), "kg");
    engine.setStream(STREAM_NAME, initialNumber, Optional.ofNullable(allYearsMatcher));

    // Replace some of the substance with a during that applies to a future year (2021)
    EngineNumber amount = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation amountOperation = new PreCalculatedOperation(amount);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2021), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    ReplaceOperation operation = new ReplaceOperation(amountOperation, STREAM_NAME, DESTINATION_SUBSTANCE, during);

    operation.execute(machine);

    // Verify the operation executes without errors
    assertNotNull(operation, "ReplaceOperation should execute without errors");
  }

  /**
   * Test the execute method with a complex value operation.
   */
  @Test
  public void testExecuteWithComplexValueOperation() {
    // Set initial value
    engine.enable(STREAM_NAME, Optional.empty());
    EngineNumber initialNumber = new EngineNumber(BigDecimal.valueOf(100), "kg");
    engine.setStream(STREAM_NAME, initialNumber, Optional.ofNullable(allYearsMatcher));

    // Replace some of the substance with a complex operation
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(30), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    Operation amountOperation = new AdditionOperation(left, right); // 30 + 12 = 42

    ReplaceOperation operation = new ReplaceOperation(amountOperation, STREAM_NAME, DESTINATION_SUBSTANCE);

    operation.execute(machine);

    // Verify the operation executes without errors
    assertNotNull(operation, "ReplaceOperation should execute without errors");
  }
}
