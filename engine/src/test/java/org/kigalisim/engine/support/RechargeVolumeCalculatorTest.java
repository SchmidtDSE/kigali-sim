/**
 * Tests for RechargeVolumeCalculator.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.StreamKeeper;

/**
 * Tests for RechargeVolumeCalculator.
 */
public class RechargeVolumeCalculatorTest {

  @Test
  public void testCalculateRechargeVolume() {
    // Setup mocks
    Scope scope = mock(Scope.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);
    StreamKeeper streamKeeper = mock(StreamKeeper.class);
    org.kigalisim.engine.Engine engine = mock(org.kigalisim.engine.Engine.class);

    when(scope.getApplication()).thenReturn("testApp");
    when(scope.getSubstance()).thenReturn("testSubstance");

    EngineNumber priorEquipment = new EngineNumber(new BigDecimal("5.0"), "units");
    EngineNumber rechargePopulation = new EngineNumber(new BigDecimal("2.0"), "units");
    EngineNumber rechargeIntensity = new EngineNumber(new BigDecimal("10.0"), "kg");

    when(engine.getStream("priorEquipment")).thenReturn(priorEquipment);
    when(streamKeeper.getRechargePopulation("testApp", "testSubstance"))
        .thenReturn(rechargePopulation);
    when(streamKeeper.getRechargeIntensity("testApp", "testSubstance"))
        .thenReturn(rechargeIntensity);

    // Call the method
    EngineNumber result = RechargeVolumeCalculator.calculateRechargeVolume(
        scope, stateGetter, streamKeeper, engine);

    // Verify the result - should be the converted recharge intensity
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testCalculateRechargeVolumeZero() {
    // Setup mocks
    Scope scope = mock(Scope.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);
    StreamKeeper streamKeeper = mock(StreamKeeper.class);
    org.kigalisim.engine.Engine engine = mock(org.kigalisim.engine.Engine.class);

    when(scope.getApplication()).thenReturn("testApp");
    when(scope.getSubstance()).thenReturn("testSubstance");

    EngineNumber zeroPriorEquipment = new EngineNumber(BigDecimal.ZERO, "units");
    EngineNumber zeroRechargePopulation = new EngineNumber(BigDecimal.ZERO, "units");
    EngineNumber zeroRechargeIntensity = new EngineNumber(BigDecimal.ZERO, "kg");

    when(engine.getStream("priorEquipment")).thenReturn(zeroPriorEquipment);
    when(streamKeeper.getRechargePopulation("testApp", "testSubstance"))
        .thenReturn(zeroRechargePopulation);
    when(streamKeeper.getRechargeIntensity("testApp", "testSubstance"))
        .thenReturn(zeroRechargeIntensity);

    // Call the method
    EngineNumber result = RechargeVolumeCalculator.calculateRechargeVolume(
        scope, stateGetter, streamKeeper, engine);

    // Verify the result
    assertEquals(BigDecimal.ZERO, result.getValue());
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testCalculateRechargeVolumeWithNullScope() {
    // Setup mocks
    Scope scope = mock(Scope.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);
    StreamKeeper streamKeeper = mock(StreamKeeper.class);
    org.kigalisim.engine.Engine engine = mock(org.kigalisim.engine.Engine.class);

    // Return null for application or substance to trigger exception
    when(scope.getApplication()).thenReturn(null);
    when(scope.getSubstance()).thenReturn("testSubstance");

    // Should throw RuntimeException from ExceptionsGenerator
    assertThrows(RuntimeException.class, () -> {
      RechargeVolumeCalculator.calculateRechargeVolume(scope, stateGetter, streamKeeper, engine);
    });
  }
}
