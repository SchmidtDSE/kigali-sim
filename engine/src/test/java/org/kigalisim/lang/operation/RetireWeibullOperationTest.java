/**
 * Unit tests for the RetireWeibullOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;
import org.kigalisim.lang.time.CalculatedTimePointFuture;
import org.kigalisim.lang.time.ParsedDuring;
import org.kigalisim.lang.time.TimePointFuture;

/**
 * Tests for the RetireWeibullOperation class.
 */
public class RetireWeibullOperationTest {

  private static final Scope SCOPE = new Scope("default", "app", "sub");

  /**
   * Test that executing the operation delegates to the engine's units retirement.
   */
  @Test
  public void testExecuteDelegatesToEngineRetire() {
    Engine engine = mock(Engine.class);
    SimulationState state = mock(SimulationState.class);
    when(engine.getStreamKeeper()).thenReturn(state);
    when(engine.getScope()).thenReturn(SCOPE);
    when(engine.getStartYear()).thenReturn(2020);
    when(engine.getYear()).thenReturn(2020);
    when(state.getRetireCalculatedThisStep(any())).thenReturn(false);
    when(engine.getStreamFor(SCOPE, "equipment")).thenReturn(new EngineNumber(BigDecimal.ZERO, "units"));
    when(engine.getStreamFor(SCOPE, "priorEquipment")).thenReturn(new EngineNumber(BigDecimal.ZERO, "units"));
    when(engine.getStream(any(), any(), any(), anyInt()))
        .thenReturn(new EngineNumber(BigDecimal.ZERO, "units"));

    PushDownMachine machine = new SingleThreadPushDownMachine(engine);
    RetireWeibullOperation operation =
        new RetireWeibullOperation(new BigDecimal("20"), false, false);

    operation.execute(machine);

    verify(engine).retire(any(EngineNumber.class), any(YearMatcher.class));
  }

  /**
   * Test that the operation skips execution when the during resolves outside the engine's range.
   */
  @Test
  public void testExecuteOutOfRangeSkips() {
    Engine engine = mock(Engine.class);
    SimulationState state = mock(SimulationState.class);
    when(engine.getStreamKeeper()).thenReturn(state);
    when(engine.getScope()).thenReturn(SCOPE);
    when(engine.getYear()).thenReturn(2020);

    PushDownMachine machine = new SingleThreadPushDownMachine(engine);
    Operation yearOperation = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2021), ""));
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    RetireWeibullOperation operation =
        new RetireWeibullOperation(new BigDecimal("20"), false, false, during);
    operation.execute(machine);

    verify(engine, never()).retire(any(), any());
  }

  /**
   * Test that a Weibull retire after a with-replacement retire raises the mixed-replacement error.
   */
  @Test
  public void testMixedReplacementRejected() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);
    engine.setStanza("default");
    engine.setApplication("app");
    engine.setSubstance("sub");
    PushDownMachine machine = new SingleThreadPushDownMachine(engine);

    engine.getStreamKeeper().setHasReplacementThisStep(engine.getScope(), true);
    engine.getStreamKeeper().setRetireCalculatedThisStep(engine.getScope(), true);

    RetireWeibullOperation operation =
        new RetireWeibullOperation(new BigDecimal("20"), false, false);

    assertThrows(RuntimeException.class, () -> operation.execute(machine));
  }

  /**
   * Test that a Weibull retire on a clean step records non-replacement and delegates.
   */
  @Test
  public void testCleanStepRecordsNonReplacement() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);
    engine.setStanza("default");
    engine.setApplication("app");
    engine.setSubstance("sub");
    PushDownMachine machine = new SingleThreadPushDownMachine(engine);

    RetireWeibullOperation operation =
        new RetireWeibullOperation(new BigDecimal("20"), false, false);
    operation.execute(machine);

    assertEquals(false, engine.getStreamKeeper().getHasReplacementThisStep(engine.getScope()),
        "Weibull retire should record a non-replacement step");
  }

  /**
   * Test the assuming-new pseudo-cohort amount on a real engine.
   */
  @Test
  public void testAssumingNewPseudoCohort() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 2);
    engine.setStanza("default");
    engine.setApplication("app");
    engine.setSubstance("sub");
    PushDownMachine machine = new SingleThreadPushDownMachine(engine);

    engine.executeStreamUpdate(new org.kigalisim.engine.recalc.StreamUpdateBuilder()
        .setName("priorEquipment")
        .setValue(new EngineNumber(new BigDecimal("1000"), "units"))
        .setYearMatcher(Optional.of(YearMatcher.unbounded()))
        .inferSubtractRecycling()
        .build());

    RetireWeibullOperation operation =
        new RetireWeibullOperation(new BigDecimal("20"), true, false);
    operation.execute(machine);

    BigDecimal retired = engine.getStreamKeeper().getStream(engine.getScope(), "retired").getValue();
    assertEquals(47.902, retired.doubleValue(), 0.001, "year-1 assuming-new retirement");
  }
}
