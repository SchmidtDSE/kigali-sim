/**
 * Unit tests for the InitialChargeOperation class.
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
 * Tests for the InitialChargeOperation class.
 */
public class InitialChargeOperationTest {

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
   * Test that InitialChargeOperation can be initialized with no during.
   */
  @Test
  public void testInitializesNoDuring() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(5), "kg / unit");
    Operation valueOperation = new PreCalculatedOperation(number);
    InitialChargeOperation operation = new InitialChargeOperation("manufacture", valueOperation);
    assertNotNull(operation, "InitialChargeOperation should be constructable without during");
  }

  /**
   * Test that InitialChargeOperation can be initialized with a during.
   */
  @Test
  public void testInitializesWithDuring() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(5), "kg / unit");
    Operation valueOperation = new PreCalculatedOperation(number);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    InitialChargeOperation operation = new InitialChargeOperation("manufacture", valueOperation, during);
    assertNotNull(operation, "InitialChargeOperation should be constructable with during");
  }

  /**
   * Test the execute method with no during.
   */
  @Test
  public void testExecuteNoDuring() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(5), "kg / unit");
    Operation valueOperation = new PreCalculatedOperation(number);
    InitialChargeOperation operation = new InitialChargeOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the initial charge was set in the engine
    EngineNumber result = engine.getInitialCharge("manufacture");
    assertEquals(BigDecimal.valueOf(5), result.getValue(), "Initial charge should be set correctly");
    assertEquals("kg / unit", result.getUnits(), "Initial charge units should be set correctly");
  }

  /**
   * Test the execute method with a during.
   */
  @Test
  public void testExecuteWithDuring() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(5), "kg / unit");
    Operation valueOperation = new PreCalculatedOperation(number);

    // Create a during that applies to the current year (2020)
    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2020), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    InitialChargeOperation operation = new InitialChargeOperation("manufacture", valueOperation, during);

    operation.execute(machine);

    // Verify the initial charge was set in the engine
    EngineNumber result = engine.getInitialCharge("manufacture");
    assertEquals(BigDecimal.valueOf(5), result.getValue(), "Initial charge should be set correctly");
    assertEquals("kg / unit", result.getUnits(), "Initial charge units should be set correctly");
  }

  /**
   * Test the execute method with a during that doesn't apply.
   */
  @Test
  public void testExecuteWithDuringNotApplying() {
    // Setup the original value
    machine.getEngine().setInitialCharge(
        new EngineNumber(BigDecimal.ZERO, "kg / unit"),
        "manufacture",
        new YearMatcher(Optional.empty(), Optional.empty())
    );

    // Setup a noop
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(5), "kg / unit");
    Operation valueOperation = new PreCalculatedOperation(number);

    // Create a during that applies to a future year (2021)
    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2021), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    InitialChargeOperation operation = new InitialChargeOperation("manufacture", valueOperation, during);

    operation.execute(machine);

    // Verify the initial charge was not set in the engine (should be 0)
    EngineNumber result = engine.getInitialCharge("manufacture");
    assertEquals(BigDecimal.ZERO, result.getValue(),
        "Initial charge should not be set when during doesn't apply");
  }

  /**
   * Test the execute method with a complex value operation.
   */
  @Test
  public void testExecuteWithComplexValueOperation() {
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(3), "kg / unit"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2), "kg / unit"));
    Operation valueOperation = new AdditionOperation(left, right); // 3 + 2 = 5

    InitialChargeOperation operation = new InitialChargeOperation("manufacture", valueOperation);

    operation.execute(machine);

    // Verify the initial charge was set in the engine
    EngineNumber result = engine.getInitialCharge("manufacture");
    assertEquals(BigDecimal.valueOf(5), result.getValue(),
        "Initial charge should be calculated and set correctly");
    assertEquals("kg / unit", result.getUnits(), "Initial charge units should be set correctly");
  }
}
