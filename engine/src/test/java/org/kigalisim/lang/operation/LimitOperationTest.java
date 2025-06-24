/**
 * Unit tests for the LimitOperation class.
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
 * Tests for the LimitOperation class.
 */
public class LimitOperationTest {

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
   * Test that LimitOperation can be initialized with both lower and upper bounds.
   */
  @Test
  public void testInitializesWithBothBounds() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(50), "kg"));
    Operation lowerBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    Operation upperBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(100), "kg"));

    LimitOperation operation = new LimitOperation(operand, lowerBound, upperBound);
    assertNotNull(operation, "LimitOperation should be constructable with both bounds");
  }

  /**
   * Test that LimitOperation can be initialized with only an upper bound.
   */
  @Test
  public void testInitializesWithUpperBound() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(50), "kg"));
    Operation upperBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(100), "kg"));

    LimitOperation operation = new LimitOperation(operand, upperBound);
    assertNotNull(operation, "LimitOperation should be constructable with only upper bound");
  }

  /**
   * Test that LimitOperation can be initialized with only a lower bound.
   */
  @Test
  public void testInitializesWithLowerBound() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(50), "kg"));
    Operation lowerBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(0), "kg"));

    LimitOperation operation = new LimitOperation(operand, lowerBound, true);
    assertNotNull(operation, "LimitOperation should be constructable with only lower bound");
  }

  /**
   * Test the execute method with a value within bounds.
   */
  @Test
  public void testExecuteWithValueWithinBounds() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(50), "kg"));
    Operation lowerBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    Operation upperBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(100), "kg"));

    LimitOperation operation = new LimitOperation(operand, lowerBound, upperBound);
    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(50), result.getValue(), "Value within bounds should remain unchanged");
    assertEquals("kg", result.getUnits(), "Units should be preserved");
  }

  /**
   * Test the execute method with a value below the lower bound.
   */
  @Test
  public void testExecuteWithValueBelowLowerBound() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(-10), "kg"));
    Operation lowerBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    Operation upperBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(100), "kg"));

    LimitOperation operation = new LimitOperation(operand, lowerBound, upperBound);
    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(0), result.getValue(), "Value below lower bound should be limited to lower bound");
    assertEquals("kg", result.getUnits(), "Units should be preserved");
  }

  /**
   * Test the execute method with a value above the upper bound.
   */
  @Test
  public void testExecuteWithValueAboveUpperBound() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(150), "kg"));
    Operation lowerBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(0), "kg"));
    Operation upperBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(100), "kg"));

    LimitOperation operation = new LimitOperation(operand, lowerBound, upperBound);
    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(100), result.getValue(), "Value above upper bound should be limited to upper bound");
    assertEquals("kg", result.getUnits(), "Units should be preserved");
  }

  /**
   * Test the execute method with only a lower bound.
   */
  @Test
  public void testExecuteWithOnlyLowerBound() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(-10), "kg"));
    Operation lowerBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(0), "kg"));

    LimitOperation operation = new LimitOperation(operand, lowerBound, true);
    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(0), result.getValue(), "Value below lower bound should be limited to lower bound");
    assertEquals("kg", result.getUnits(), "Units should be preserved");
  }

  /**
   * Test the execute method with only an upper bound.
   */
  @Test
  public void testExecuteWithOnlyUpperBound() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(150), "kg"));
    Operation upperBound = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(100), "kg"));

    LimitOperation operation = new LimitOperation(operand, upperBound);
    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(100), result.getValue(), "Value above upper bound should be limited to upper bound");
    assertEquals("kg", result.getUnits(), "Units should be preserved");
  }

  /**
   * Test the execute method with complex operations for bounds.
   */
  @Test
  public void testExecuteWithComplexBoundOperations() {
    Operation operand = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(50), "kg"));

    // Lower bound: 10 + 15 = 25
    Operation lowerLeft = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(10), "kg"));
    Operation lowerRight = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(15), "kg"));
    Operation lowerBound = new AdditionOperation(lowerLeft, lowerRight);

    // Upper bound: 100 - 20 = 80
    Operation upperLeft = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(100), "kg"));
    Operation upperRight = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(20), "kg"));
    Operation upperBound = new SubtractionOperation(upperLeft, upperRight);

    LimitOperation operation = new LimitOperation(operand, lowerBound, upperBound);
    operation.execute(machine);

    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(50), result.getValue(), "Value within complex bounds should remain unchanged");
    assertEquals("kg", result.getUnits(), "Units should be preserved");
  }
}
