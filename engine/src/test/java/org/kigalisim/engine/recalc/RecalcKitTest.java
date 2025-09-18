/**
 * Tests for RecalcKit.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.SimulationState;

/**
 * Tests for RecalcKit.
 */
public class RecalcKitTest {

  @Test
  public void testConstructorWithAllValues() {
    SimulationState simulationState = mock(SimulationState.class);
    UnitConverter unitConverter = mock(UnitConverter.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);

    RecalcKit kit = new RecalcKit(
        simulationState,
        unitConverter,
        stateGetter
    );

    assertNotNull(kit.getStreamKeeper());
    assertNotNull(kit.getUnitConverter());
    assertNotNull(kit.getStateGetter());
    assertEquals(simulationState, kit.getStreamKeeper());
    assertEquals(unitConverter, kit.getUnitConverter());
    assertEquals(stateGetter, kit.getStateGetter());
  }
}
