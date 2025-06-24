/**
 * Unit tests for the OperationFragment class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.operation.PreCalculatedOperation;

/**
 * Tests for the OperationFragment class.
 */
public class OperationFragmentTest {

  /**
   * Helper method to create a simple operation for testing.
   *
   * @return A simple operation that pushes a constant value
   */
  private Operation createTestOperation() {
    return new PreCalculatedOperation(new EngineNumber(42.0, "kg"));
  }

  /**
   * Test that OperationFragment can be initialized.
   */
  @Test
  public void testInitializes() {
    Operation operation = createTestOperation();
    OperationFragment fragment = new OperationFragment(operation);
    assertNotNull(fragment, "OperationFragment should be constructable");
  }

  /**
   * Test the getOperation method.
   */
  @Test
  public void testGetOperation() {
    Operation operation = createTestOperation();
    OperationFragment fragment = new OperationFragment(operation);
    assertEquals(operation, fragment.getOperation(),
        "getOperation should return the correct Operation");
  }

  /**
   * Test that getDuring throws RuntimeException.
   */
  @Test
  public void testGetDuringThrows() {
    Operation operation = createTestOperation();
    OperationFragment fragment = new OperationFragment(operation);
    assertThrows(RuntimeException.class, () -> fragment.getDuring(),
        "getDuring should throw RuntimeException");
  }

  /**
   * Test that getUnit throws RuntimeException.
   */
  @Test
  public void testGetUnitThrows() {
    Operation operation = createTestOperation();
    OperationFragment fragment = new OperationFragment(operation);
    assertThrows(RuntimeException.class, () -> fragment.getUnit(),
        "getUnit should throw RuntimeException");
  }
}
