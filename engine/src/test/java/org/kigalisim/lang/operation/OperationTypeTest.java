/**
 * Unit tests for the OperationType reported by operations.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for the OperationType reported by operations.
 */
public class OperationTypeTest {

  private final Operation valueOperation = new PreCalculatedOperation(
      new EngineNumber(BigDecimal.ONE, "units")
  );

  /**
   * Test that a Weibull retire operation reports RETIRE_WEIBULL.
   */
  @Test
  public void testRetireWeibullType() {
    Operation operation = new RetireWeibullOperation(BigDecimal.TEN, false, false);
    assertEquals(OperationType.RETIRE_WEIBULL, operation.getOperationType());
  }

  /**
   * Test that an exact retire operation reports RETIRE_EXACT.
   */
  @Test
  public void testRetireExactType() {
    Operation operation = new RetireExactOperation(valueOperation, 5, false, false);
    assertEquals(OperationType.RETIRE_EXACT, operation.getOperationType());
  }

  /**
   * Test that a set operation reports SET.
   */
  @Test
  public void testSetType() {
    Operation operation = new SetOperation("priorEquipment", valueOperation);
    assertEquals(OperationType.SET, operation.getOperationType());
  }

  /**
   * Test that a change operation reports CHANGE.
   */
  @Test
  public void testChangeType() {
    Operation operation = new ChangeOperation("priorEquipment", valueOperation);
    assertEquals(OperationType.CHANGE, operation.getOperationType());
  }

  /**
   * Test that operations without a specific type report OTHER.
   */
  @Test
  public void testDefaultType() {
    assertEquals(OperationType.OTHER, valueOperation.getOperationType());
  }
}
