/**
 * Unit tests for the OperationStaticSemantics reported by operations.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for the OperationStaticSemantics reported by operations.
 */
public class OperationStaticSemanticsTest {

  private final Operation valueOperation = new PreCalculatedOperation(
      new EngineNumber(BigDecimal.ONE, "units")
  );

  /**
   * Test that a Weibull retire operation reports its type and assuming new flag.
   */
  @Test
  public void testRetireWeibull() {
    OperationStaticSemantics semantics = new RetireWeibullOperation(BigDecimal.TEN, true, false)
        .getStaticSemantics();
    assertEquals(OperationType.RETIRE_WEIBULL, semantics.getOperationType());
    assertEquals(Optional.of(true), semantics.getAssumePriorNew());
  }

  /**
   * Test that an exact retire operation reports its type and assuming new flag.
   */
  @Test
  public void testRetireExact() {
    OperationStaticSemantics semantics = new RetireExactOperation(valueOperation, 5, false, false)
        .getStaticSemantics();
    assertEquals(OperationType.RETIRE_EXACT, semantics.getOperationType());
    assertEquals(Optional.of(false), semantics.getAssumePriorNew());
  }

  /**
   * Test that a set operation reports its type and stream.
   */
  @Test
  public void testSet() {
    OperationStaticSemantics semantics = new SetOperation("priorEquipment", valueOperation)
        .getStaticSemantics();
    assertEquals(OperationType.SET, semantics.getOperationType());
    assertEquals(Set.of("priorEquipment"), semantics.getStreams());
    assertFalse(semantics.getAssumePriorNew().isPresent());
  }

  /**
   * Test that a change operation reports its type and stream.
   */
  @Test
  public void testChange() {
    OperationStaticSemantics semantics = new ChangeOperation("priorEquipment", valueOperation)
        .getStaticSemantics();
    assertEquals(OperationType.CHANGE, semantics.getOperationType());
    assertEquals(Set.of("priorEquipment"), semantics.getStreams());
  }

  /**
   * Test that a cap with displacement reports both the capped and displacement streams.
   */
  @Test
  public void testCapDisplacing() {
    OperationStaticSemantics semantics = new CapOperation("sales", valueOperation, "import")
        .getStaticSemantics();
    assertEquals(OperationType.CAP, semantics.getOperationType());
    assertEquals(Set.of("sales", "import"), semantics.getStreams());
  }

  /**
   * Test that a floor without displacement reports only its own stream.
   */
  @Test
  public void testFloorWithoutDisplacement() {
    OperationStaticSemantics semantics = new FloorOperation("sales", valueOperation)
        .getStaticSemantics();
    assertEquals(OperationType.FLOOR, semantics.getOperationType());
    assertEquals(Set.of("sales"), semantics.getStreams());
  }

  /**
   * Test that an equivalent-displacing floor reports both the capped and displacement streams.
   */
  @Test
  public void testFloorDisplacing() {
    OperationStaticSemantics semantics = new FloorDisplacingOperation(
        "sales",
        valueOperation,
        "import"
    ).getStaticSemantics();
    assertEquals(OperationType.FLOOR_DISPLACING, semantics.getOperationType());
    assertEquals(Set.of("sales", "import"), semantics.getStreams());
  }

  /**
   * Test that a get stream operation reports its stream.
   */
  @Test
  public void testGetStream() {
    OperationStaticSemantics semantics = new GetStreamOperation("sales").getStaticSemantics();
    assertEquals(OperationType.GET_STREAM, semantics.getOperationType());
    assertEquals(Set.of("sales"), semantics.getStreams());
  }

  /**
   * Test that a recharge operation reports its target as the stream.
   */
  @Test
  public void testRecharge() {
    OperationStaticSemantics semantics = new RechargeOperation(
        valueOperation,
        valueOperation,
        Optional.empty(),
        RechargeOperation.PRIOR
    ).getStaticSemantics();
    assertEquals(OperationType.RECHARGE, semantics.getOperationType());
    assertEquals(Set.of(RechargeOperation.PRIOR), semantics.getStreams());
  }

  /**
   * Test that an operation without streams reports only its type.
   */
  @Test
  public void testTypeOnly() {
    OperationStaticSemantics semantics = valueOperation.getStaticSemantics();
    assertEquals(OperationType.PRE_CALCULATED, semantics.getOperationType());
    assertTrue(semantics.getStreams().isEmpty());
    assertTrue(semantics.getAssumePriorNew().isEmpty());
  }

  /**
   * Test that the single stream constructor reports that stream.
   */
  @Test
  public void testSingleStreamConstructor() {
    OperationStaticSemantics semantics = new OperationStaticSemantics(OperationType.ENABLE, "sales");
    assertEquals(Set.of("sales"), semantics.getStreams());
  }

  /**
   * Test that the builder collapses repeated streams and accumulates distinct ones.
   */
  @Test
  public void testBuilderAddStream() {
    OperationStaticSemantics semantics = new OperationStaticSemanticsBuilder(OperationType.CAP)
        .addStream("sales")
        .addStream("sales")
        .addStream("import")
        .build();
    assertEquals(Set.of("sales", "import"), semantics.getStreams());
  }

  /**
   * Test that the returned streams cannot be modified by callers.
   */
  @Test
  public void testStreamsUnmodifiable() {
    OperationStaticSemantics semantics = new OperationStaticSemantics(OperationType.SET, "sales");
    assertThrows(UnsupportedOperationException.class, () -> semantics.getStreams().add("import"));
  }

  /**
   * Test that cap and floor operations leave a substance displacement target out of the streams.
   */
  @Test
  public void testSubstanceDisplacementTargetNotStream() {
    Set<String> expected = Set.of("sales");
    assertEquals(
        expected,
        new CapOperation("sales", valueOperation, "R-404A").getStaticSemantics().getStreams()
    );
    assertEquals(
        expected,
        new FloorOperation("sales", valueOperation, "R-404A").getStaticSemantics().getStreams()
    );
    assertEquals(
        expected,
        new CapDisplacingOperation("sales", valueOperation, "R-404A")
            .getStaticSemantics().getStreams()
    );
    assertEquals(
        expected,
        new FloorDisplacingOperation("sales", valueOperation, "R-404A")
            .getStaticSemantics().getStreams()
    );
  }

  /**
   * Test that the displacing cap operation reports a stream displacement target.
   */
  @Test
  public void testCapDisplacingOperationStreamTarget() {
    OperationStaticSemantics semantics = new CapDisplacingOperation(
        "sales",
        valueOperation,
        "import"
    ).getStaticSemantics();
    assertEquals(OperationType.CAP_DISPLACING, semantics.getOperationType());
    assertEquals(Set.of("sales", "import"), semantics.getStreams());
  }

  /**
   * Test that the builder adds a name only if it is a known stream when using addIfStream.
   */
  @Test
  public void testBuilderAddIfStream() {
    OperationStaticSemantics semantics = new OperationStaticSemanticsBuilder(OperationType.CAP)
        .addStream("sales")
        .addIfStream("import")
        .addIfStream("R-404A")
        .build();
    assertEquals(Set.of("sales", "import"), semantics.getStreams());
  }
}
