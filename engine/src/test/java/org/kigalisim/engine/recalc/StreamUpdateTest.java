package org.kigalisim.engine.recalc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.SimpleUseKey;

/**
 * Tests for StreamUpdate class.
 */
public class StreamUpdateTest {

  @Test
  public void testStreamUpdateGetters() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(100), "kg");
    SimpleUseKey key = new SimpleUseKey("app", "sub");

    StreamUpdate update = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(value)
        .setKey(key)
        .setPropagateChanges(false)
        .setSubtractRecycling(false)
        .setUnitsToRecord("kg")
        .build();

    assertEquals("domestic", update.getName());
    assertEquals(value, update.getValue());
    assertEquals(Optional.of(key), update.getKey());
    assertFalse(update.getPropagateChanges());
    assertFalse(update.getSubtractRecycling());
    assertEquals(Optional.of("kg"), update.getUnitsToRecord());
    assertTrue(update.getYearMatcher().isEmpty());
  }

  @Test
  public void testStreamUpdateDefaults() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(50), "mt");

    StreamUpdate update = new StreamUpdateBuilder()
        .setName("sales")
        .setValue(value)
        .build();

    assertEquals("sales", update.getName());
    assertEquals(value, update.getValue());
    assertTrue(update.getKey().isEmpty());
    assertTrue(update.getPropagateChanges());
    assertTrue(update.getSubtractRecycling());
    assertTrue(update.getUnitsToRecord().isEmpty());
    assertTrue(update.getYearMatcher().isEmpty());
  }
}
