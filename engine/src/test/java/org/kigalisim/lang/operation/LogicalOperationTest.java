package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.mockito.ArgumentCaptor;

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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("1"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("1"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "and");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 1 (true)
    assertEquals(BigDecimal.ONE, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("0"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("1"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "and");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 0 (false)
    assertEquals(BigDecimal.ZERO, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("1"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("0"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "and");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 0 (false)
    assertEquals(BigDecimal.ZERO, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("1"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("1"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "or");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 1 (true)
    assertEquals(BigDecimal.ONE, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("1"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("0"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "or");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 1 (true)
    assertEquals(BigDecimal.ONE, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("0"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("1"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "or");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 1 (true)
    assertEquals(BigDecimal.ONE, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("0"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("0"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "or");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 0 (false)
    assertEquals(BigDecimal.ZERO, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("1"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("1"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "xor");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 0 (false)
    assertEquals(BigDecimal.ZERO, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("1"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("0"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "xor");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 1 (true)
    assertEquals(BigDecimal.ONE, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("0"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("1"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "xor");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 1 (true)
    assertEquals(BigDecimal.ONE, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("0"), "");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("0"), "");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    LogicalOperation operation = new LogicalOperation(left, right, "xor");

    // Execute
    operation.execute(machine);

    // Verify
    verify(left).execute(machine);
    verify(right).execute(machine);

    // Capture the argument passed to push
    ArgumentCaptor<EngineNumber> captor = ArgumentCaptor.forClass(EngineNumber.class);
    verify(machine).push(captor.capture());

    // Verify the value is 0 (false)
    assertEquals(BigDecimal.ZERO, captor.getValue().getValue());
    assertEquals("", captor.getValue().getUnits());
  }
}
