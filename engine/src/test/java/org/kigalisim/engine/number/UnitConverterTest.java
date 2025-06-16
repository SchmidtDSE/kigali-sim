/**
 * Unit tests for the UnitConverter class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.number;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests for the UnitConverter class.
 */
public class UnitConverterTest {

  /**
   * Helper method to create a mockito-based StateGetter for testing.
   *
   * @return Mock StateGetter with lenient stubbing
   */
  private StateGetter createMockStateGetter() {
    StateGetter mock = mock(StateGetter.class);
    // Use lenient stubbing for common default values
    lenient().when(mock.getSubstanceConsumption()).thenReturn(new EngineNumber(0, "tCO2e / kg"));
    lenient().when(mock.getEnergyIntensity()).thenReturn(new EngineNumber(0, "kwh / kg"));
    lenient().when(mock.getAmortizedUnitVolume()).thenReturn(new EngineNumber(0, "kg / unit"));
    lenient().when(mock.getPopulation()).thenReturn(new EngineNumber(0, "units"));
    lenient().when(mock.getYearsElapsed()).thenReturn(new EngineNumber(0, "years"));
    lenient().when(mock.getGhgConsumption()).thenReturn(new EngineNumber(0, "tCO2e"));
    lenient().when(mock.getVolume()).thenReturn(new EngineNumber(0, "kg"));
    lenient().when(mock.getAmortizedUnitConsumption())
        .thenReturn(new EngineNumber(0, "tCO2e / unit"));
    lenient().when(mock.getPopulationChange()).thenReturn(new EngineNumber(0, "units"));
    lenient().when(mock.getEnergyConsumption()).thenReturn(new EngineNumber(0, "kwh"));
    return mock;
  }

  /**
   * Helper method to convert units using the UnitConverter.
   *
   * @param source The source EngineNumber
   * @param destination The destination units
   * @param stateGetter The state getter for conversion context
   * @return The converted EngineNumber
   */
  private EngineNumber convertUnits(EngineNumber source, String destination,
      StateGetter stateGetter) {
    UnitConverter converter = new UnitConverter(stateGetter);
    return converter.convert(source, destination);
  }

  /**
   * Helper method to assert values are close within tolerance.
   *
   * @param expected The expected value
   * @param actual The actual value
   * @param tolerance The tolerance for comparison
   */
  private void assertCloseTo(double expected, BigDecimal actual, double tolerance) {
    double actualDouble = actual.doubleValue();
    assertEquals(expected, actualDouble, tolerance);
  }

  @Test
  public void testVolumeToVolume() {
    EngineNumber result = convertUnits(new EngineNumber(1, "mt"), "kg", createMockStateGetter());

    assertCloseTo(1000, result.getValue(), 0.001);
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testConsumptionToVolume() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getSubstanceConsumption())
        .thenReturn(new EngineNumber(5, "tCO2e / mt"));

    EngineNumber result = convertUnits(new EngineNumber(20, "tCO2e"), "mt", mockStateGetter);

    assertCloseTo(4, result.getValue(), 0.001);
    assertEquals("mt", result.getUnits());
  }

  @Test
  public void testUnitsToVolume() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getAmortizedUnitVolume())
        .thenReturn(new EngineNumber(10, "kg / unit"));

    EngineNumber result = convertUnits(new EngineNumber(20, "units"), "kg", mockStateGetter);

    assertCloseTo(200, result.getValue(), 0.001);
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testVolumePerPopToVolume() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getPopulation()).thenReturn(new EngineNumber(10, "units"));

    EngineNumber result = convertUnits(new EngineNumber(20, "kg / unit"), "kg", mockStateGetter);

    assertCloseTo(200, result.getValue(), 0.001);
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testVolumePerPopToVolumePerPop() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getPopulation()).thenReturn(new EngineNumber(10, "units"));

    EngineNumber result = convertUnits(
        new EngineNumber(20, "kg / unit"), "mt / unit", mockStateGetter);

    assertCloseTo(20.0 / 1000, result.getValue(), 0.001);
    assertEquals("mt / unit", result.getUnits());
  }

  @Test
  public void testVolumePerTimeToVolume() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getYearsElapsed()).thenReturn(new EngineNumber(2, "years"));

    EngineNumber result = convertUnits(new EngineNumber(20, "kg / year"), "kg", mockStateGetter);

    assertCloseTo(40, result.getValue(), 0.001);
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testVolumePerConsumptionToVolume() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getGhgConsumption()).thenReturn(new EngineNumber(10, "tCO2e"));

    EngineNumber result = convertUnits(new EngineNumber(20, "kg / tCO2e"), "kg", mockStateGetter);

    assertCloseTo(200, result.getValue(), 0.001);
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testVolumePerConsumptionToVolumeUsingEnergyConsumption() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getEnergyConsumption()).thenReturn(new EngineNumber(10, "MJ"));

    EngineNumber result = convertUnits(new EngineNumber(20, "kg / kwh"), "kg", mockStateGetter);

    assertCloseTo(200, result.getValue(), 0.001);
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testPercentToVolume() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getVolume()).thenReturn(new EngineNumber(20, "kg"));

    EngineNumber result = convertUnits(new EngineNumber(10, "%"), "kg", mockStateGetter);

    assertCloseTo(2, result.getValue(), 0.001);
    assertEquals("kg", result.getUnits());
  }

  @Test
  public void testPopToPop() {
    EngineNumber result = convertUnits(
        new EngineNumber(5, "unit"), "units", createMockStateGetter());

    assertCloseTo(5, result.getValue(), 0.001);
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testVolumeToPop() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getAmortizedUnitVolume())
        .thenReturn(new EngineNumber(10, "kg / unit"));

    EngineNumber result = convertUnits(new EngineNumber(20, "kg"), "units", mockStateGetter);

    assertCloseTo(2, result.getValue(), 0.001);
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testConsumptionToPop() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getAmortizedUnitConsumption())
        .thenReturn(new EngineNumber(50, "tCO2e / unit"));

    EngineNumber result = convertUnits(new EngineNumber(200, "tCO2e"), "units", mockStateGetter);

    assertCloseTo(4, result.getValue(), 0.001);
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testPopPerTimeToPop() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getYearsElapsed()).thenReturn(new EngineNumber(2, "years"));

    EngineNumber result = convertUnits(
        new EngineNumber(20, "units / year"), "units", mockStateGetter);

    assertCloseTo(40, result.getValue(), 0.001);
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testPopPerVolumeToPop() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getVolume()).thenReturn(new EngineNumber(10, "kg"));

    EngineNumber result = convertUnits(
        new EngineNumber(2, "units / kg"), "units", mockStateGetter);

    assertCloseTo(20, result.getValue(), 0.001);
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testPopPerConsumptionToPop() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getGhgConsumption()).thenReturn(new EngineNumber(5, "tCO2e"));

    EngineNumber result = convertUnits(
        new EngineNumber(2, "units / tCO2e"), "units", mockStateGetter);

    assertCloseTo(10, result.getValue(), 0.001);
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testPopPerConsumptionToPopUsingEnergyConsumption() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getEnergyConsumption()).thenReturn(new EngineNumber(5, "kwh"));

    EngineNumber result = convertUnits(
        new EngineNumber(2, "units / kwh"), "units", mockStateGetter);

    assertCloseTo(10, result.getValue(), 0.001);
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testPercentToPop() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getPopulation()).thenReturn(new EngineNumber(20, "units"));

    EngineNumber result = convertUnits(new EngineNumber(10, "%"), "units", mockStateGetter);

    assertCloseTo(2, result.getValue(), 0.001);
    assertEquals("units", result.getUnits());
  }

  @Test
  public void testConsumptionToConsumptionGhg() {
    EngineNumber result = convertUnits(
        new EngineNumber(5, "tCO2e"), "tCO2e", createMockStateGetter());

    assertCloseTo(5, result.getValue(), 0.001);
    assertEquals("tCO2e", result.getUnits());
  }

  @Test
  public void testVolumeToConsumptionForGhg() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getSubstanceConsumption())
        .thenReturn(new EngineNumber(5, "tCO2e / kg"));

    EngineNumber result = convertUnits(new EngineNumber(10, "kg"), "tCO2e", mockStateGetter);

    assertCloseTo(50, result.getValue(), 0.001);
    assertEquals("tCO2e", result.getUnits());
  }

  @Test
  public void testPopToConsumptionForGhg() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getSubstanceConsumption())
        .thenReturn(new EngineNumber(0.1, "tCO2e / unit"));

    EngineNumber result = convertUnits(new EngineNumber(20, "units"), "tCO2e", mockStateGetter);

    assertCloseTo(2, result.getValue(), 0.001);
    assertEquals("tCO2e", result.getUnits());
  }

  @Test
  public void testConsumptionPerTimeToConsumptionForGhg() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getYearsElapsed()).thenReturn(new EngineNumber(2, "years"));

    EngineNumber result = convertUnits(
        new EngineNumber(20, "tCO2e / year"), "tCO2e", mockStateGetter);

    assertCloseTo(40, result.getValue(), 0.001);
    assertEquals("tCO2e", result.getUnits());
  }

  @Test
  public void testConsumptionPerVolumeToConsumptionForGhg() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getVolume()).thenReturn(new EngineNumber(5, "kg"));

    EngineNumber result = convertUnits(
        new EngineNumber(2, "tCO2e / kg"), "tCO2e", mockStateGetter);

    assertCloseTo(10, result.getValue(), 0.001);
    assertEquals("tCO2e", result.getUnits());
  }

  @Test
  public void testConsumptionPerPopToConsumptionForGhg() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getPopulation()).thenReturn(new EngineNumber(20, "units"));

    EngineNumber result = convertUnits(
        new EngineNumber(10, "tCO2e / unit"), "tCO2e", mockStateGetter);

    assertCloseTo(200, result.getValue(), 0.001);
    assertEquals("tCO2e", result.getUnits());
  }

  @Test
  public void testPercentToConsumptionForGhg() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getGhgConsumption()).thenReturn(new EngineNumber(10, "tCO2e"));

    EngineNumber result = convertUnits(new EngineNumber(10, "%"), "tCO2e", mockStateGetter);

    assertCloseTo(1, result.getValue(), 0.001);
    assertEquals("tCO2e", result.getUnits());
  }

  @Test
  public void testPercentToConsumptionUsingEnergyConsumption() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getEnergyConsumption()).thenReturn(new EngineNumber(10, "kwh"));

    EngineNumber result = convertUnits(new EngineNumber(10, "%"), "kwh", mockStateGetter);

    assertCloseTo(1, result.getValue(), 0.001);
    assertEquals("kwh", result.getUnits());
  }

  @Test
  public void testConsumptionToConsumptionEnergy() {
    EngineNumber result = convertUnits(new EngineNumber(5, "kwh"), "kwh", createMockStateGetter());

    assertCloseTo(5, result.getValue(), 0.001);
    assertEquals("kwh", result.getUnits());
  }

  @Test
  public void testVolumeToConsumptionForEnergy() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getEnergyIntensity())
        .thenReturn(new EngineNumber(5, "kwh / kg"));

    EngineNumber result = convertUnits(new EngineNumber(10, "kg"), "kwh", mockStateGetter);

    assertCloseTo(50, result.getValue(), 0.001);
    assertEquals("kwh", result.getUnits());
  }

  @Test
  public void testPopToConsumptionForEnergy() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getEnergyIntensity())
        .thenReturn(new EngineNumber(0.1, "kwh / unit"));

    EngineNumber result = convertUnits(new EngineNumber(20, "units"), "kwh", mockStateGetter);

    assertCloseTo(2, result.getValue(), 0.001);
    assertEquals("kwh", result.getUnits());
  }

  @Test
  public void testConsumptionPerTimeToConsumptionForEnergy() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getYearsElapsed()).thenReturn(new EngineNumber(2, "years"));

    EngineNumber result = convertUnits(new EngineNumber(20, "kwh / year"), "kwh", mockStateGetter);

    assertCloseTo(40, result.getValue(), 0.001);
    assertEquals("kwh", result.getUnits());
  }

  @Test
  public void testConsumptionPerVolumeToConsumptionForEnergy() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getVolume()).thenReturn(new EngineNumber(5, "kg"));

    EngineNumber result = convertUnits(new EngineNumber(2, "kwh / kg"), "kwh", mockStateGetter);

    assertCloseTo(10, result.getValue(), 0.001);
    assertEquals("kwh", result.getUnits());
  }

  @Test
  public void testYearsToYears() {
    EngineNumber result = convertUnits(
        new EngineNumber(5, "year"), "years", createMockStateGetter());

    assertCloseTo(5, result.getValue(), 0.001);
    assertEquals("years", result.getUnits());
  }

  @Test
  public void testConsumptionToYears() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getGhgConsumption()).thenReturn(new EngineNumber(5, "tCO2e"));

    EngineNumber result = convertUnits(new EngineNumber(10, "tCO2e"), "years", mockStateGetter);

    assertCloseTo(2, result.getValue(), 0.001);
    assertEquals("years", result.getUnits());
  }

  @Test
  public void testConsumptionToYearsUsingEnergyConsumption() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getEnergyConsumption()).thenReturn(new EngineNumber(5, "kwh"));

    EngineNumber result = convertUnits(new EngineNumber(10, "kwh"), "years", mockStateGetter);

    assertCloseTo(2, result.getValue(), 0.001);
    assertEquals("years", result.getUnits());
  }

  @Test
  public void testVolumeToYears() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getVolume()).thenReturn(new EngineNumber(5, "kg"));

    EngineNumber result = convertUnits(new EngineNumber(10, "kg"), "years", mockStateGetter);

    assertCloseTo(2, result.getValue(), 0.001);
    assertEquals("years", result.getUnits());
  }

  @Test
  public void testPopToYears() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getPopulationChange())
        .thenReturn(new EngineNumber(2, "units"));

    EngineNumber result = convertUnits(new EngineNumber(20, "units"), "years", mockStateGetter);

    assertCloseTo(10, result.getValue(), 0.001);
    assertEquals("years", result.getUnits());
  }

  @Test
  public void testPercentToYears() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getYearsElapsed()).thenReturn(new EngineNumber(2, "years"));

    EngineNumber result = convertUnits(new EngineNumber(10, "%"), "years", mockStateGetter);

    assertCloseTo(0.2, result.getValue(), 0.001);
    assertEquals("years", result.getUnits());
  }

  @Test
  public void testNormalizeByPopulation() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getPopulation()).thenReturn(new EngineNumber(2, "units"));

    EngineNumber result = convertUnits(new EngineNumber(20, "kg"), "kg / unit", mockStateGetter);

    assertCloseTo(10, result.getValue(), 0.001);
    assertEquals("kg / unit", result.getUnits());
  }

  @Test
  public void testNormalizeByVolume() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getVolume()).thenReturn(new EngineNumber(2, "kg"));

    EngineNumber result = convertUnits(new EngineNumber(10, "units"), "unit / kg", mockStateGetter);

    assertCloseTo(5, result.getValue(), 0.001);
    assertEquals("unit / kg", result.getUnits());
  }

  @Test
  public void testNormalizeByGhgConsumption() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getGhgConsumption()).thenReturn(new EngineNumber(2, "tCO2e"));

    EngineNumber result = convertUnits(
        new EngineNumber(10, "units"), "unit / tCO2e", mockStateGetter);

    assertCloseTo(5, result.getValue(), 0.001);
    assertEquals("unit / tCO2e", result.getUnits());
  }

  @Test
  public void testNormalizeByTime() {
    StateGetter mockStateGetter = createMockStateGetter();
    lenient().when(mockStateGetter.getYearsElapsed()).thenReturn(new EngineNumber(2, "years"));

    EngineNumber result = convertUnits(
        new EngineNumber(10, "units"), "unit / year", mockStateGetter);

    assertCloseTo(5, result.getValue(), 0.001);
    assertEquals("unit / year", result.getUnits());
  }
}
