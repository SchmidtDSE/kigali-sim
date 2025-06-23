package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.mockito.ArgumentCaptor;

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

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("10"), "kg");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("10"), "kg");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    ComparisonOperation operation = new ComparisonOperation(left, right, "==");

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
   * Test not equals comparison.
   */
  @Test
  public void testNotEqualsComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("10"), "kg");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("20"), "kg");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    ComparisonOperation operation = new ComparisonOperation(left, right, "!=");

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
   * Test greater than comparison.
   */
  @Test
  public void testGreaterThanComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("20"), "kg");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("10"), "kg");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    ComparisonOperation operation = new ComparisonOperation(left, right, ">");

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
   * Test less than comparison.
   */
  @Test
  public void testLessThanComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("10"), "kg");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("20"), "kg");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    ComparisonOperation operation = new ComparisonOperation(left, right, "<");

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
   * Test greater than or equal comparison.
   */
  @Test
  public void testGreaterThanOrEqualComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("10"), "kg");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("10"), "kg");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    ComparisonOperation operation = new ComparisonOperation(left, right, ">=");

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
   * Test less than or equal comparison.
   */
  @Test
  public void testLessThanOrEqualComparison() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("10"), "kg");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("10"), "kg");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    ComparisonOperation operation = new ComparisonOperation(left, right, "<=");

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
   * Test false comparison result.
   */
  @Test
  public void testFalseComparisonResult() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation left = mock(Operation.class);
    Operation right = mock(Operation.class);

    EngineNumber leftNumber = new EngineNumber(new BigDecimal("10"), "kg");
    EngineNumber rightNumber = new EngineNumber(new BigDecimal("20"), "kg");

    when(machine.getResult()).thenReturn(leftNumber, rightNumber);

    ComparisonOperation operation = new ComparisonOperation(left, right, "==");

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
