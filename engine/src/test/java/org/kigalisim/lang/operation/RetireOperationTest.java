/**
 * Unit tests for the RetireOperation class.
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
 * Tests for the RetireOperation class.
 */
public class RetireOperationTest {

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
   * Test that RetireOperation can be initialized with no during.
   */
  @Test
  public void testInitializesNoDuring() {
    EngineNumber rate = new EngineNumber(BigDecimal.valueOf(0.1), "");
    Operation amountOperation = new PreCalculatedOperation(rate);
    RetireOperation operation = new RetireOperation(amountOperation);
    assertNotNull(operation, "RetireOperation should be constructable without during");
  }

  /**
   * Test that RetireOperation can be initialized with a during.
   */
  @Test
  public void testInitializesWithDuring() {
    EngineNumber rate = new EngineNumber(BigDecimal.valueOf(0.1), "");
    Operation amountOperation = new PreCalculatedOperation(rate);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    RetireOperation operation = new RetireOperation(amountOperation, during);
    assertNotNull(operation, "RetireOperation should be constructable with during");
  }

  /**
   * Test the execute method with no during.
   */
  @Test
  public void testExecuteNoDuring() {
    // Set up prior equipment population
    EngineNumber priorEquipment = new EngineNumber(BigDecimal.valueOf(1000), "units");
    engine.setStream("priorEquipment", priorEquipment, Optional.ofNullable(allYearsMatcher));

    // Set up retirement rate
    EngineNumber rate = new EngineNumber(BigDecimal.valueOf(10), "%");
    Operation amountOperation = new PreCalculatedOperation(rate);
    RetireOperation operation = new RetireOperation(amountOperation);

    operation.execute(machine);

    // Verify the retirement rate was set in the engine
    EngineNumber result = engine.getRetirementRate();
    assertEquals(BigDecimal.valueOf(10), result.getValue(), "Retirement rate should be set correctly");
    assertEquals("%", result.getUnits(), "Retirement rate units should be set correctly");
  }

  /**
   * Test the execute method with a during.
   */
  @Test
  public void testExecuteWithDuring() {
    // Set up prior equipment population
    EngineNumber priorEquipment = new EngineNumber(BigDecimal.valueOf(1000), "units");
    engine.setStream("priorEquipment", priorEquipment, Optional.ofNullable(allYearsMatcher));

    // Set up retirement rate with a during that applies to the current year (2020)
    EngineNumber rate = new EngineNumber(BigDecimal.valueOf(10), "%");
    Operation amountOperation = new PreCalculatedOperation(rate);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2020), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    RetireOperation operation = new RetireOperation(amountOperation, during);

    operation.execute(machine);

    // Verify the retirement rate was set in the engine
    EngineNumber result = engine.getRetirementRate();
    assertEquals(BigDecimal.valueOf(10), result.getValue(), "Retirement rate should be set correctly");
    assertEquals("%", result.getUnits(), "Retirement rate units should be set correctly");
  }

  /**
   * Test the execute method with a during that doesn't apply.
   */
  @Test
  public void testExecuteWithDuringNotApplying() {
    // Set up prior equipment population
    EngineNumber priorEquipment = new EngineNumber(BigDecimal.valueOf(1000), "units");
    engine.setStream("priorEquipment", priorEquipment, Optional.ofNullable(allYearsMatcher));

    // Set initial retirement rate
    EngineNumber initialRate = new EngineNumber(BigDecimal.valueOf(5), "%");
    engine.retire(initialRate, allYearsMatcher);

    // Set up retirement rate with a during that applies to a future year (2021)
    EngineNumber rate = new EngineNumber(BigDecimal.valueOf(10), "%");
    Operation amountOperation = new PreCalculatedOperation(rate);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2021), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    RetireOperation operation = new RetireOperation(amountOperation, during);

    operation.execute(machine);

    // Verify the retirement rate was not changed in the engine
    EngineNumber result = engine.getRetirementRate();
    assertEquals(BigDecimal.valueOf(5), result.getValue(),
        "Retirement rate should not be changed when during doesn't apply");
    assertEquals("%", result.getUnits(), "Retirement rate units should remain the same");
  }

  /**
   * Test the execute method with a complex value operation.
   */
  @Test
  public void testExecuteWithComplexValueOperation() {
    // Set up retirement rate with a complex operation
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(6), "%"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "%"));
    Operation amountOperation = new AdditionOperation(left, right); // 0.06 + 0.04 = 0.1

    RetireOperation operation = new RetireOperation(amountOperation);

    operation.execute(machine);

    // Verify the retirement rate was set in the engine
    EngineNumber result = engine.getRetirementRate();
    assertEquals(BigDecimal.valueOf(10), result.getValue(),
        "Retirement rate should be calculated and set correctly");
    assertEquals("%", result.getUnits(), "Retirement rate units should be set correctly");
  }
}
