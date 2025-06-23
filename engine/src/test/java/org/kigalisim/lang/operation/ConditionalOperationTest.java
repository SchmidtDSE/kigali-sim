package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.mockito.ArgumentCaptor;

/**
 * Tests for the ConditionalOperation class.
 */
public class ConditionalOperationTest {

  /**
   * Test conditional operation with true condition.
   */
  @Test
  public void testConditionalWithTrueCondition() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation condition = mock(Operation.class);
    Operation trueCase = mock(Operation.class);
    Operation falseCase = mock(Operation.class);

    EngineNumber conditionResult = new EngineNumber(new BigDecimal("1"), "");
    EngineNumber trueCaseResult = new EngineNumber(new BigDecimal("42"), "kg");

    when(machine.getResult()).thenReturn(conditionResult, trueCaseResult);

    ConditionalOperation operation = new ConditionalOperation(condition, trueCase, falseCase);

    // Execute
    operation.execute(machine);

    // Verify
    verify(condition).execute(machine);
    verify(trueCase).execute(machine);
    verifyNoInteractions(falseCase);
  }

  /**
   * Test conditional operation with false condition.
   */
  @Test
  public void testConditionalWithFalseCondition() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation condition = mock(Operation.class);
    Operation trueCase = mock(Operation.class);
    Operation falseCase = mock(Operation.class);

    EngineNumber conditionResult = new EngineNumber(new BigDecimal("0"), "");
    EngineNumber falseCaseResult = new EngineNumber(new BigDecimal("24"), "kg");

    when(machine.getResult()).thenReturn(conditionResult, falseCaseResult);

    ConditionalOperation operation = new ConditionalOperation(condition, trueCase, falseCase);

    // Execute
    operation.execute(machine);

    // Verify
    verify(condition).execute(machine);
    verify(falseCase).execute(machine);
    verifyNoInteractions(trueCase);
  }

  /**
   * Test conditional operation with non-zero condition treated as true.
   */
  @Test
  public void testConditionalWithNonZeroCondition() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation condition = mock(Operation.class);
    Operation trueCase = mock(Operation.class);
    Operation falseCase = mock(Operation.class);

    EngineNumber conditionResult = new EngineNumber(new BigDecimal("5"), "");
    EngineNumber trueCaseResult = new EngineNumber(new BigDecimal("42"), "kg");

    when(machine.getResult()).thenReturn(conditionResult, trueCaseResult);

    ConditionalOperation operation = new ConditionalOperation(condition, trueCase, falseCase);

    // Execute
    operation.execute(machine);

    // Verify
    verify(condition).execute(machine);
    verify(trueCase).execute(machine);
    verifyNoInteractions(falseCase);
  }

  /**
   * Test conditional operation with negative condition treated as true.
   */
  @Test
  public void testConditionalWithNegativeCondition() {
    // Setup
    PushDownMachine machine = mock(PushDownMachine.class);
    Operation condition = mock(Operation.class);
    Operation trueCase = mock(Operation.class);
    Operation falseCase = mock(Operation.class);

    EngineNumber conditionResult = new EngineNumber(new BigDecimal("-5"), "");
    EngineNumber trueCaseResult = new EngineNumber(new BigDecimal("42"), "kg");

    when(machine.getResult()).thenReturn(conditionResult, trueCaseResult);

    ConditionalOperation operation = new ConditionalOperation(condition, trueCase, falseCase);

    // Execute
    operation.execute(machine);

    // Verify
    verify(condition).execute(machine);
    verify(trueCase).execute(machine);
    verifyNoInteractions(falseCase);
  }
}
