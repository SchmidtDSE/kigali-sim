/**
 * Tests for RecalcKitBuilder.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.StreamKeeper;

/**
 * Tests for RecalcKitBuilder.
 */
public class RecalcKitBuilderTest {

  @Test
  public void testDefaultConstructorThrowsOnBuild() {
    RecalcKitBuilder builder = new RecalcKitBuilder();

    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void testBuilderWithAllValues() {
    StreamKeeper streamKeeper = mock(StreamKeeper.class);
    UnitConverter unitConverter = mock(UnitConverter.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);

    RecalcKitBuilder builder = new RecalcKitBuilder();
    RecalcKit kit = builder
        .setStreamKeeper(streamKeeper)
        .setUnitConverter(unitConverter)
        .setStateGetter(stateGetter)
        .build();

    assertNotNull(kit.getStreamKeeper());
    assertNotNull(kit.getUnitConverter());
    assertNotNull(kit.getStateGetter());
    assertEquals(streamKeeper, kit.getStreamKeeper());
    assertEquals(unitConverter, kit.getUnitConverter());
    assertEquals(stateGetter, kit.getStateGetter());
  }

  @Test
  public void testBuilderWithMissingStreamKeeper() {
    UnitConverter unitConverter = mock(UnitConverter.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);

    RecalcKitBuilder builder = new RecalcKitBuilder();
    builder.setUnitConverter(unitConverter)
        .setStateGetter(stateGetter);

    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void testBuilderWithMissingUnitConverter() {
    StreamKeeper streamKeeper = mock(StreamKeeper.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);

    RecalcKitBuilder builder = new RecalcKitBuilder();
    builder.setStreamKeeper(streamKeeper)
        .setStateGetter(stateGetter);

    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void testBuilderWithMissingStateGetter() {
    StreamKeeper streamKeeper = mock(StreamKeeper.class);
    UnitConverter unitConverter = mock(UnitConverter.class);

    RecalcKitBuilder builder = new RecalcKitBuilder();
    builder.setStreamKeeper(streamKeeper)
        .setUnitConverter(unitConverter);

    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void testBuilderChaining() {
    StreamKeeper streamKeeper = mock(StreamKeeper.class);
    UnitConverter unitConverter = mock(UnitConverter.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);

    RecalcKitBuilder builder = new RecalcKitBuilder();
    RecalcKit kit = builder
        .setStreamKeeper(streamKeeper)
        .setUnitConverter(unitConverter)
        .setStateGetter(stateGetter)
        .build();

    assertEquals(streamKeeper, kit.getStreamKeeper());
    assertEquals(unitConverter, kit.getUnitConverter());
    assertEquals(stateGetter, kit.getStateGetter());
  }
}
