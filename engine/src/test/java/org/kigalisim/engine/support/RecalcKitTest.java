/**
 * Tests for RecalcKit.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.StreamKeeper;

/**
 * Tests for RecalcKit.
 */
public class RecalcKitTest {

  @Test
  public void testConstructorWithAllValues() {
    StreamKeeper streamKeeper = mock(StreamKeeper.class);
    UnitConverter unitConverter = mock(UnitConverter.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);

    RecalcKit kit = new RecalcKit(
        streamKeeper,
        unitConverter,
        stateGetter
    );

    assertNotNull(kit.getStreamKeeper());
    assertNotNull(kit.getUnitConverter());
    assertNotNull(kit.getStateGetter());
    assertEquals(streamKeeper, kit.getStreamKeeper());
    assertEquals(unitConverter, kit.getUnitConverter());
    assertEquals(stateGetter, kit.getStateGetter());
  }
}