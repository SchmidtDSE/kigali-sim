package org.kigalisim.lang.operation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Tests for the LogicalOperation class.
 */
public class LogicalOperationTest {

  /**
   * Test AND operation with both operands true.
   */
  @Test
  public void testAndBothTrue() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "and");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).and();
  }

  /**
   * Test AND operation with left operand false.
   */
  @Test
  public void testAndLeftFalse() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "and");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).and();
  }

  /**
   * Test AND operation with right operand false.
   */
  @Test
  public void testAndRightFalse() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "and");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).and();
  }

  /**
   * Test OR operation with both operands true.
   */
  @Test
  public void testOrBothTrue() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "or");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).or();
  }

  /**
   * Test OR operation with left operand true.
   */
  @Test
  public void testOrLeftTrue() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "or");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).or();
  }

  /**
   * Test OR operation with right operand true.
   */
  @Test
  public void testOrRightTrue() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "or");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).or();
  }

  /**
   * Test OR operation with both operands false.
   */
  @Test
  public void testOrBothFalse() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "or");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).or();
  }

  /**
   * Test XOR operation with both operands true.
   */
  @Test
  public void testXorBothTrue() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "xor");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).xor();
  }

  /**
   * Test XOR operation with left operand true.
   */
  @Test
  public void testXorLeftTrue() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "xor");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).xor();
  }

  /**
   * Test XOR operation with right operand true.
   */
  @Test
  public void testXorRightTrue() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "xor");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).xor();
  }

  /**
   * Test XOR operation with both operands false.
   */
  @Test
  public void testXorBothFalse() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    LogicalOperation operation = new LogicalOperation(left, right, "xor");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);
    verify(machine).xor();
  }
}
