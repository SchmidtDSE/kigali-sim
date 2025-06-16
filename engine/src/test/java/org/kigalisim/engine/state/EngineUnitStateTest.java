/**
 * Unit tests for engine unit state classes.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;

/**
 * Unit tests for the ConverterStateGetter and OverridingConverterStateGetter classes.
 */
public class EngineUnitStateTest {

  @Test
  public void testConverterStateGetterInitializesWithEngine() {
    Engine engine = mock(Engine.class);
    ConverterStateGetter stateGetter = new ConverterStateGetter(engine);
    assertNotNull(stateGetter);
  }

  @Test
  public void testConverterStateGetterGetSubstanceConsumption() {
    Engine engine = mock(Engine.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("2.5"), "tCO2e / kg");
    when(engine.getEqualsGhgIntensity()).thenReturn(expected);

    ConverterStateGetter stateGetter = new ConverterStateGetter(engine);
    EngineNumber result = stateGetter.getSubstanceConsumption();

    assertEquals(0, expected.getValue().compareTo(result.getValue()));
    assertEquals(expected.getUnits(), result.getUnits());
  }

  @Test
  public void testConverterStateGetterGetEnergyIntensity() {
    Engine engine = mock(Engine.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("1.5"), "kwh / kg");
    when(engine.getEqualsEnergyIntensity()).thenReturn(expected);

    ConverterStateGetter stateGetter = new ConverterStateGetter(engine);
    EngineNumber result = stateGetter.getEnergyIntensity();

    assertEquals(0, expected.getValue().compareTo(result.getValue()));
    assertEquals(expected.getUnits(), result.getUnits());
  }

  @Test
  public void testConverterStateGetterGetAmortizedUnitVolume() {
    Engine engine = mock(Engine.class);
    EngineNumber expected = new EngineNumber(BigDecimal.TEN, "kg / unit");
    when(engine.getInitialCharge("sales")).thenReturn(expected);

    ConverterStateGetter stateGetter = new ConverterStateGetter(engine);
    EngineNumber result = stateGetter.getAmortizedUnitVolume();

    assertEquals(0, expected.getValue().compareTo(result.getValue()));
    assertEquals(expected.getUnits(), result.getUnits());
  }

  @Test
  public void testConverterStateGetterGetPopulation() {
    Engine engine = mock(Engine.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("100"), "units");
    when(engine.getStream("equipment")).thenReturn(expected);

    ConverterStateGetter stateGetter = new ConverterStateGetter(engine);
    EngineNumber result = stateGetter.getPopulation();

    assertEquals(0, expected.getValue().compareTo(result.getValue()));
    assertEquals(expected.getUnits(), result.getUnits());
  }

  @Test
  public void testConverterStateGetterGetYearsElapsed() {
    Engine engine = mock(Engine.class);
    ConverterStateGetter stateGetter = new ConverterStateGetter(engine);
    EngineNumber result = stateGetter.getYearsElapsed();

    assertEquals(0, BigDecimal.ONE.compareTo(result.getValue()));
    assertEquals("year", result.getUnits());
  }

  @Test
  public void testConverterStateGetterGetGhgConsumption() {
    Engine engine = mock(Engine.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("50"), "tCO2e");
    when(engine.getStream("consumption")).thenReturn(expected);

    ConverterStateGetter stateGetter = new ConverterStateGetter(engine);
    EngineNumber result = stateGetter.getGhgConsumption();

    assertEquals(0, expected.getValue().compareTo(result.getValue()));
    assertEquals(expected.getUnits(), result.getUnits());
  }

  @Test
  public void testConverterStateGetterGetEnergyConsumption() {
    Engine engine = mock(Engine.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("200"), "kwh");
    when(engine.getStream("energy")).thenReturn(expected);

    ConverterStateGetter stateGetter = new ConverterStateGetter(engine);
    EngineNumber result = stateGetter.getEnergyConsumption();

    assertEquals(0, expected.getValue().compareTo(result.getValue()));
    assertEquals(expected.getUnits(), result.getUnits());
  }

  @Test
  public void testConverterStateGetterGetVolume() {
    Engine engine = mock(Engine.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("75"), "kg");
    when(engine.getStream("sales")).thenReturn(expected);

    ConverterStateGetter stateGetter = new ConverterStateGetter(engine);
    EngineNumber result = stateGetter.getVolume();

    assertEquals(0, expected.getValue().compareTo(result.getValue()));
    assertEquals(expected.getUnits(), result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterInitializesWithInnerGetter() {
    StateGetter inner = mock(StateGetter.class);
    OverridingConverterStateGetter overriding = 
        new OverridingConverterStateGetter(inner);
    assertNotNull(overriding);
  }

  @Test
  public void testOverridingGetSubstanceConsumptionUsesInnerWhenNotOverridden() {
    StateGetter inner = mock(StateGetter.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("3"), "tCO2e / kg");
    when(inner.getSubstanceConsumption()).thenReturn(expected);
    
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    EngineNumber result = overriding.getSubstanceConsumption();

    assertEquals(0, new BigDecimal("3").compareTo(result.getValue()));
    assertEquals("tCO2e / kg", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterSetSubstanceConsumptionOverridesInnerValue() {
    StateGetter inner = mock(StateGetter.class);
    EngineNumber innerValue = new EngineNumber(new BigDecimal("3"), "tCO2e / kg");
    when(inner.getSubstanceConsumption()).thenReturn(innerValue);
    
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    overriding.setSubstanceConsumption(new EngineNumber(new BigDecimal("5"), "tCO2e / kg"));
    EngineNumber result = overriding.getSubstanceConsumption();

    assertEquals(0, new BigDecimal("5").compareTo(result.getValue()));
    assertEquals("tCO2e / kg", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterGetEnergyIntensityUsesInnerWhenNotOverridden() {
    StateGetter inner = mock(StateGetter.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("2"), "kwh / kg");
    when(inner.getEnergyIntensity()).thenReturn(expected);
    
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    EngineNumber result = overriding.getEnergyIntensity();

    assertEquals(0, new BigDecimal("2").compareTo(result.getValue()));
    assertEquals("kwh / kg", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterSetEnergyIntensityOverridesInnerValue() {
    StateGetter inner = mock(StateGetter.class);
    EngineNumber innerValue = new EngineNumber(new BigDecimal("2"), "kwh / kg");
    when(inner.getEnergyIntensity()).thenReturn(innerValue);
    
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    overriding.setEnergyIntensity(new EngineNumber(new BigDecimal("4"), "kwh / kg"));
    EngineNumber result = overriding.getEnergyIntensity();

    assertEquals(0, new BigDecimal("4").compareTo(result.getValue()));
    assertEquals("kwh / kg", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterGetPopulationUsesInnerWhenNotOverridden() {
    StateGetter inner = mock(StateGetter.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("20"), "units");
    when(inner.getPopulation()).thenReturn(expected);
    
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    EngineNumber result = overriding.getPopulation();

    assertEquals(0, new BigDecimal("20").compareTo(result.getValue()));
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterSetPopulationOverridesInnerValue() {
    StateGetter inner = mock(StateGetter.class);
    EngineNumber innerValue = new EngineNumber(new BigDecimal("20"), "units");
    when(inner.getPopulation()).thenReturn(innerValue);
    
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    overriding.setPopulation(new EngineNumber(new BigDecimal("30"), "units"));
    EngineNumber result = overriding.getPopulation();

    assertEquals(0, new BigDecimal("30").compareTo(result.getValue()));
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterGetVolumeUsesInnerWhenNotOverridden() {
    StateGetter inner = mock(StateGetter.class);
    EngineNumber expected = new EngineNumber(new BigDecimal("100"), "kg");
    when(inner.getVolume()).thenReturn(expected);
    
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    EngineNumber result = overriding.getVolume();

    assertEquals(0, new BigDecimal("100").compareTo(result.getValue()));
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterSetVolumeOverridesInnerValue() {
    StateGetter inner = mock(StateGetter.class);
    EngineNumber innerValue = new EngineNumber(new BigDecimal("100"), "kg");
    when(inner.getVolume()).thenReturn(innerValue);
    
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    overriding.setVolume(new EngineNumber(new BigDecimal("150"), "kg"));
    EngineNumber result = overriding.getVolume();

    assertEquals(0, new BigDecimal("150").compareTo(result.getValue()));
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterSetTotalWithSalesCallsSetVolume() {
    StateGetter inner = mock(StateGetter.class);
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    overriding.setTotal("sales", new EngineNumber(new BigDecimal("200"), "kg"));
    EngineNumber result = overriding.getVolume();

    assertEquals(0, new BigDecimal("200").compareTo(result.getValue()));
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterSetTotalWithEquipmentCallsSetPopulation() {
    StateGetter inner = mock(StateGetter.class);
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    overriding.setTotal("equipment", new EngineNumber(new BigDecimal("50"), "units"));
    EngineNumber result = overriding.getPopulation();

    assertEquals(0, new BigDecimal("50").compareTo(result.getValue()));
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterSetTotalWithConsumptionCallsSetConsumption() {
    StateGetter inner = mock(StateGetter.class);
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    overriding.setTotal("consumption", new EngineNumber(new BigDecimal("75"), "tCO2e"));
    EngineNumber result = overriding.getGhgConsumption();

    assertEquals(0, new BigDecimal("75").compareTo(result.getValue()));
    assertEquals("tCO2e", result.getUnits());
  }

  @Test
  public void testOverridingConverterStateGetterSetTotalThrowsExceptionForUnrecognizedStreamName() {
    StateGetter inner = mock(StateGetter.class);
    OverridingConverterStateGetter overriding = new OverridingConverterStateGetter(inner);

    try {
      overriding.setTotal("unrecognized", new EngineNumber(new BigDecimal("100"), "kg"));
      fail("Expected IllegalArgumentException to be thrown");
    } catch (IllegalArgumentException e) {
      assertEquals("Unrecognized stream name: unrecognized", e.getMessage());
    }
  }
}