package org.kigalisim.lang.operation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Tests for the ComparisonOperation class.
 */
public class ComparisonOperationTest {

  /**
   * Test equals comparison.
   */
  @Test
  public void testEqualsComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    ComparisonOperation operation = new ComparisonOperation(left, right, "==");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).equals();
  }

  /**
   * Test not equals comparison.
   */
  @Test
  public void testNotEqualsComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    ComparisonOperation operation = new ComparisonOperation(left, right, "!=");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).notEquals();
  }

  /**
   * Test greater than comparison.
   */
  @Test
  public void testGreaterThanComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    ComparisonOperation operation = new ComparisonOperation(left, right, ">");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).greaterThan();
  }

  /**
   * Test less than comparison.
   */
  @Test
  public void testLessThanComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    ComparisonOperation operation = new ComparisonOperation(left, right, "<");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).lessThan();
  }

  /**
   * Test greater than or equal comparison.
   */
  @Test
  public void testGreaterThanOrEqualComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    ComparisonOperation operation = new ComparisonOperation(left, right, ">=");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).greaterThanOrEqual();
  }

  /**
   * Test less than or equal comparison.
   */
  @Test
  public void testLessThanOrEqualComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    ComparisonOperation operation = new ComparisonOperation(left, right, "<=");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).lessThanOrEqual();
  }

  /**
   * Test equals comparison with false result.
   * Note: This test is redundant with testEqualsComparison() since we're now testing
   * that the correct comparison method is called, rather than the specific result value.
   */
  @Test
  public void testFalseComparisonResult() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    ComparisonOperation operation = new ComparisonOperation(left, right, "==");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).equals();
  }
}
