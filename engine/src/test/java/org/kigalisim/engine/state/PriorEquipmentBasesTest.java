/**
 * Unit tests for PriorEquipmentBases, MutablePriorEquipmentBases, and
 * FrozenPriorEquipmentBases.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for PriorEquipmentBases, MutablePriorEquipmentBases, and
 * FrozenPriorEquipmentBases.
 */
public class PriorEquipmentBasesTest {

  /**
   * Test that MutablePriorEquipmentBases can be initialized with default values.
   */
  @Test
  public void testInitializes() {
    MutablePriorEquipmentBases bases = new MutablePriorEquipmentBases();
    assertNotNull(bases, "MutablePriorEquipmentBases should be constructable");
    assertTrue(bases.getRetirementBasePopulation().isEmpty(),
        "Retirement base population should start empty");
    assertEquals(
        BigDecimal.ZERO,
        bases.getAppliedRetirementAmount().get().getValue(),
        "Applied retirement amount should start at zero"
    );
    assertFalse(bases.getHasReplacementThisStep(), "Should not have replacement initially");
    assertFalse(bases.getRecyclingCalculatedThisStep(), "Recycling should not be calculated yet");
  }

  /**
   * Test that freeze() creates an independent snapshot for MutablePriorEquipmentBases.
   */
  @Test
  public void testFreezeCreatesIndependentSnapshot() {
    MutablePriorEquipmentBases original = new MutablePriorEquipmentBases();
    original.setRetirementBasePopulation(new EngineNumber(BigDecimal.valueOf(100), "units"));
    original.setHasReplacementThisStep(true);

    PriorEquipmentBases frozen = original.freeze();

    assertEquals(
        BigDecimal.valueOf(100),
        frozen.getRetirementBasePopulation().get().getValue(),
        "Frozen snapshot should carry over retirement base population"
    );
    assertTrue(frozen.getHasReplacementThisStep(), "Frozen snapshot should carry over replacement flag");

    // Mutating the original after freezing should not affect the snapshot
    original.setRetirementBasePopulation(new EngineNumber(BigDecimal.valueOf(999), "units"));
    original.setHasReplacementThisStep(false);

    assertEquals(
        BigDecimal.valueOf(100),
        frozen.getRetirementBasePopulation().get().getValue(),
        "Frozen snapshot should not be affected by later mutation of the original"
    );
    assertTrue(frozen.getHasReplacementThisStep(),
        "Frozen snapshot should not be affected by later mutation of the original");
  }

  /**
   * Test that freeze() on a frozen instance returns the same instance.
   */
  @Test
  public void testFreezeIsIdempotent() {
    MutablePriorEquipmentBases original = new MutablePriorEquipmentBases();
    PriorEquipmentBases frozen = original.freeze();
    assertSame(frozen, frozen.freeze(), "Freezing an already-frozen instance should return itself");
  }

  /**
   * Test that every mutator on a frozen instance throws UnsupportedOperationException.
   */
  @Test
  public void testFrozenMutatorsThrow() {
    PriorEquipmentBases frozen = new MutablePriorEquipmentBases().freeze();
    EngineNumber value = new EngineNumber(BigDecimal.ONE, "units");

    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.setRetirementBasePopulation(value)
    );
    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.setAppliedRetirementAmount(value)
    );
    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.setHasReplacementThisStep(true)
    );
    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.setRetireCalculatedThisStep(true)
    );
    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.setRechargeBasePopulation(value)
    );
    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.setAppliedRechargeAmount(value)
    );
    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.setPrechargeBasePopulation(value)
    );
    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.setAppliedPrechargeAmount(value)
    );
    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.setRecyclingCalculatedThisStep(true)
    );
    assertThrows(
        UnsupportedOperationException.class,
        () -> frozen.resetStateAtTimestep()
    );
  }

  /**
   * Test that resetStateAtTimestep on a mutable instance restores default values.
   */
  @Test
  public void testResetStateAtTimestep() {
    MutablePriorEquipmentBases bases = new MutablePriorEquipmentBases();
    bases.setRetirementBasePopulation(new EngineNumber(BigDecimal.TEN, "units"));
    bases.setHasReplacementThisStep(true);
    bases.setRecyclingCalculatedThisStep(true);

    bases.resetStateAtTimestep();

    assertTrue(bases.getRetirementBasePopulation().isEmpty(),
        "Retirement base population should be reset to empty");
    assertFalse(bases.getHasReplacementThisStep(), "Replacement flag should be reset");
    assertFalse(bases.getRecyclingCalculatedThisStep(), "Recycling flag should be reset");
  }
}
