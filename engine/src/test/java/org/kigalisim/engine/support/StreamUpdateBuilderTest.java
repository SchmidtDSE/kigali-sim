package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.SimpleUseKey;

/**
 * Tests for StreamUpdateBuilder class.
 */
public class StreamUpdateBuilderTest {

  @Test
  public void testBuilderRequiredFields() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(100), "kg");

    StreamUpdate update = new StreamUpdateBuilder()
        .setName("import")
        .setValue(value)
        .build();

    assertEquals("import", update.getName());
    assertEquals(value, update.getValue());
  }

  @Test
  public void testBuilderMissingName() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(100), "kg");

    StreamUpdateBuilder builder = new StreamUpdateBuilder()
        .setValue(value);

    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void testBuilderMissingValue() {
    StreamUpdateBuilder builder = new StreamUpdateBuilder()
        .setName("export");

    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void testBuilderAllFields() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(200), "units");
    SimpleUseKey key = new SimpleUseKey("test", "substance");

    StreamUpdate update = new StreamUpdateBuilder()
        .setName("equipment")
        .setValue(value)
        .setKey(Optional.of(key))
        .setPropagateChanges(false)
        .setSubtractRecycling(false)
        .setUnitsToRecord(Optional.of("units"))
        .build();

    assertEquals("equipment", update.getName());
    assertEquals(value, update.getValue());
    assertEquals(Optional.of(key), update.getKey());
    assertEquals(false, update.getPropagateChanges());
    assertEquals(false, update.getSubtractRecycling());
    assertEquals(Optional.of("units"), update.getUnitsToRecord());
  }

  @Test
  public void testBuilderChaining() {
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(300), "mt");

    StreamUpdateBuilder builder = new StreamUpdateBuilder();
    StreamUpdate update = builder
        .setName("manufacture")
        .setValue(value)
        .setPropagateChanges(true)
        .setSubtractRecycling(true)
        .build();

    assertEquals("manufacture", update.getName());
    assertEquals(value, update.getValue());
    assertEquals(true, update.getPropagateChanges());
    assertEquals(true, update.getSubtractRecycling());
  }
}
