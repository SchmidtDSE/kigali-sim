package org.kigalisim.lang.operation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Tests for the EqualityOperation class.
 */
public class EqualityOperationTest {

  /**
   * Test equals operation.
   */
  @Test
  public void testEqualsOperation() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EqualityOperation operation = new EqualityOperation(left, right, "==");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).equals();
  }

  /**
   * Test not equals operation.
   */
  @Test
  public void testNotEqualsOperation() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EqualityOperation operation = new EqualityOperation(left, right, "!=");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).notEquals();
  }

  /**
   * Test greater than operation.
   */
  @Test
  public void testGreaterThanOperation() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EqualityOperation operation = new EqualityOperation(left, right, ">");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).greaterThan();
  }

  /**
   * Test less than operation.
   */
  @Test
  public void testLessThanOperation() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EqualityOperation operation = new EqualityOperation(left, right, "<");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).lessThan();
  }

  /**
   * Test greater than or equal operation.
   */
  @Test
  public void testGreaterThanOrEqualOperation() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EqualityOperation operation = new EqualityOperation(left, right, ">=");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).greaterThanOrEqual();
  }

  /**
   * Test less than or equal operation.
   */
  @Test
  public void testLessThanOrEqualOperation() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EqualityOperation operation = new EqualityOperation(left, right, "<=");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).lessThanOrEqual();
  }
}
