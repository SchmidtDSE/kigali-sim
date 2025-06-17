/**
 * Unit tests for the StreamKeeper class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;

/**
 * Tests for the StreamKeeper class.
 */
public class StreamKeeperTest {

  /**
   * Create a mock StreamKeeper for testing.
   *
   * @return A new StreamKeeper with mock dependencies
   */
  private StreamKeeper createMockKeeper() {
    StateGetter stateGetter = mock(StateGetter.class);
    when(stateGetter.getSubstanceConsumption())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "tCO2e / kg"));
    when(stateGetter.getEnergyIntensity())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "kwh / kg"));
    when(stateGetter.getAmortizedUnitVolume())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "kg / unit"));
    when(stateGetter.getPopulation())
        .thenReturn(new EngineNumber(new BigDecimal("100"), "units"));
    when(stateGetter.getYearsElapsed())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "year"));
    when(stateGetter.getGhgConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("50"), "tCO2e"));
    when(stateGetter.getEnergyConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("100"), "kwh"));
    when(stateGetter.getVolume())
        .thenReturn(new EngineNumber(new BigDecimal("200"), "kg"));
    when(stateGetter.getAmortizedUnitConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("0.5"), "tCO2e / unit"));
    when(stateGetter.getPopulationChange(any(UnitConverter.class)))
        .thenReturn(new EngineNumber(new BigDecimal("10"), "units"));

    final UnitConverter unitConverter = new UnitConverter(stateGetter);

    // Create a mock OverridingConverterStateGetter for StreamKeeper
    OverridingConverterStateGetter mockOverridingStateGetter =
        mock(OverridingConverterStateGetter.class);
    when(mockOverridingStateGetter.getSubstanceConsumption())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "tCO2e / kg"));
    when(mockOverridingStateGetter.getEnergyIntensity())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "kwh / kg"));
    when(mockOverridingStateGetter.getAmortizedUnitVolume())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "kg / unit"));
    when(mockOverridingStateGetter.getPopulation())
        .thenReturn(new EngineNumber(new BigDecimal("100"), "units"));
    when(mockOverridingStateGetter.getYearsElapsed())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "year"));
    when(mockOverridingStateGetter.getGhgConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("50"), "tCO2e"));
    when(mockOverridingStateGetter.getEnergyConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("100"), "kwh"));
    when(mockOverridingStateGetter.getVolume())
        .thenReturn(new EngineNumber(new BigDecimal("200"), "kg"));
    when(mockOverridingStateGetter.getAmortizedUnitConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("0.5"), "tCO2e / unit"));
    when(mockOverridingStateGetter.getPopulationChange(any(UnitConverter.class)))
        .thenReturn(new EngineNumber(new BigDecimal("10"), "units"));

    return new StreamKeeper(mockOverridingStateGetter, unitConverter);
  }

  /**
   * Test that StreamKeeper can be initialized.
   */
  @Test
  public void testInitializes() {
    StreamKeeper keeper = createMockKeeper();
    assertNotNull(keeper, "StreamKeeper should be constructable");
  }

  /**
   * Test that hasSubstance returns false for unknown substance.
   */
  @Test
  public void testHasSubstanceReturnsFalseForUnknownSubstance() {
    StreamKeeper keeper = createMockKeeper();
    assertFalse(keeper.hasSubstance("test app", "test substance"),
                "Should return false for unknown substance");
  }

  /**
   * Test that ensureSubstance creates new substance.
   */
  @Test
  public void testEnsureSubstanceCreatesNewSubstance() {
    StreamKeeper keeper = createMockKeeper();

    keeper.ensureSubstance("test app", "test substance");

    assertTrue(keeper.hasSubstance("test app", "test substance"),
               "Should return true after ensuring substance");
  }

  /**
   * Test that ensureSubstance creates default streams.
   */
  @Test
  public void testEnsureSubstanceCreatesDefaultStreams() {
    StreamKeeper keeper = createMockKeeper();

    keeper.ensureSubstance("test app", "test substance");

    // Test that default streams exist with zero values
    EngineNumber manufacture = keeper.getStream("test app", "test substance", "manufacture");
    assertEquals(BigDecimal.ZERO, manufacture.getValue(),
                 "Manufacture should default to 0");
    assertEquals("kg", manufacture.getUnits(), "Manufacture should have kg units");

    EngineNumber importValue = keeper.getStream("test app", "test substance", "import");
    assertEquals(BigDecimal.ZERO, importValue.getValue(), "Import should default to 0");
    assertEquals("kg", importValue.getUnits(), "Import should have kg units");

    EngineNumber recycle = keeper.getStream("test app", "test substance", "recycle");
    assertEquals(BigDecimal.ZERO, recycle.getValue(), "Recycle should default to 0");
    assertEquals("kg", recycle.getUnits(), "Recycle should have kg units");

    EngineNumber consumption = keeper.getStream("test app", "test substance", "consumption");
    assertEquals(BigDecimal.ZERO, consumption.getValue(), "Consumption should default to 0");
    assertEquals("tCO2e", consumption.getUnits(), "Consumption should have tCO2e units");

    EngineNumber equipment = keeper.getStream("test app", "test substance", "equipment");
    assertEquals(BigDecimal.ZERO, equipment.getValue(), "Equipment should default to 0");
    assertEquals("units", equipment.getUnits(), "Equipment should have units");
  }

  /**
   * Test that setStream and getStream work for simple streams.
   */
  @Test
  public void testSetStreamAndGetStreamWorkForSimpleStreams() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    EngineNumber newValue = new EngineNumber(new BigDecimal("100"), "kg");
    keeper.setStream("test app", "test substance", "manufacture", newValue);

    EngineNumber retrieved = keeper.getStream("test app", "test substance", "manufacture");
    assertEquals(new BigDecimal("100"), retrieved.getValue(),
                 "Should retrieve set value");
    assertEquals("kg", retrieved.getUnits(), "Should retrieve correct units");
  }

  /**
   * Test that sales stream returns sum of manufacture and import.
   */
  @Test
  public void testSalesStreamReturnsSumOfManufactureAndImport() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    keeper.setStream("test app", "test substance", "manufacture",
                     new EngineNumber(new BigDecimal("50"), "kg"));
    keeper.setStream("test app", "test substance", "import",
                     new EngineNumber(new BigDecimal("30"), "kg"));

    EngineNumber sales = keeper.getStream("test app", "test substance", "sales");
    assertEquals(new BigDecimal("80"), sales.getValue(),
                 "Sales should be sum of manufacture and import");
    assertEquals("kg", sales.getUnits(), "Sales should have kg units");
  }

  /**
   * Test that GHG intensity getter and setter delegate to parameterization.
   */
  @Test
  public void testGhgIntensityGetterAndSetterDelegateToParameterization() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    EngineNumber newValue = new EngineNumber(new BigDecimal("2.5"), "tCO2e / kg");
    keeper.setGhgIntensity("test app", "test substance", newValue);

    EngineNumber retrieved = keeper.getGhgIntensity("test app", "test substance");
    assertEquals(new BigDecimal("2.5"), retrieved.getValue(),
                 "Should retrieve set GHG intensity");
    assertEquals("tCO2e / kg", retrieved.getUnits(),
                 "Should retrieve correct GHG intensity units");
  }

  /**
   * Test that energy intensity getter and setter delegate to parameterization.
   */
  @Test
  public void testEnergyIntensityGetterAndSetterDelegateToParameterization() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    EngineNumber newValue = new EngineNumber(new BigDecimal("1.5"), "kwh / kg");
    keeper.setEnergyIntensity("test app", "test substance", newValue);

    EngineNumber retrieved = keeper.getEnergyIntensity("test app", "test substance");
    assertEquals(new BigDecimal("1.5"), retrieved.getValue(),
                 "Should retrieve set energy intensity");
    assertEquals("kwh / kg", retrieved.getUnits(),
                 "Should retrieve correct energy intensity units");
  }

  /**
   * Test that initial charge getter and setter delegate to parameterization.
   */
  @Test
  public void testInitialChargeGetterAndSetterDelegateToParameterization() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    EngineNumber newValue = new EngineNumber(new BigDecimal("2.0"), "kg / unit");
    keeper.setInitialCharge("test app", "test substance", "manufacture", newValue);

    EngineNumber retrieved = keeper.getInitialCharge("test app", "test substance", "manufacture");
    assertEquals(new BigDecimal("2.0"), retrieved.getValue(),
                 "Should retrieve set initial charge");
    assertEquals("kg / unit", retrieved.getUnits(),
                 "Should retrieve correct initial charge units");
  }

  /**
   * Test that incrementYear moves equipment to priorEquipment.
   */
  @Test
  public void testIncrementYearMovesEquipmentToPriorEquipment() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    // Set equipment value
    keeper.setStream("test app", "test substance", "equipment",
                     new EngineNumber(new BigDecimal("150"), "units"));

    // Increment year
    keeper.incrementYear();

    // Check that equipment was moved to priorEquipment
    EngineNumber priorEquipment = keeper.getStream("test app", "test substance",
                                                   "priorEquipment");
    assertEquals(new BigDecimal("150"), priorEquipment.getValue(),
                 "Prior equipment should equal previous equipment value");
    assertEquals("units", priorEquipment.getUnits(),
                 "Prior equipment should have correct units");
  }

  /**
   * Test that error is thrown for unknown substance in setStream.
   */
  @Test
  public void testThrowsErrorForUnknownSubstanceInSetStream() {
    StreamKeeper keeper = createMockKeeper();

    assertThrows(IllegalStateException.class, () -> {
      keeper.setStream("unknown app", "unknown substance", "manufacture",
                       new EngineNumber(new BigDecimal("100"), "kg"));
    }, "Should throw for unknown substance in setStream");
  }

  /**
   * Test that error is thrown for unknown substance in getStream.
   */
  @Test
  public void testThrowsErrorForUnknownSubstanceInGetStream() {
    StreamKeeper keeper = createMockKeeper();

    assertThrows(IllegalStateException.class, () -> {
      keeper.getStream("unknown app", "unknown substance", "manufacture");
    }, "Should throw for unknown substance in getStream");
  }

  /**
   * Test that error is thrown for unknown stream.
   */
  @Test
  public void testThrowsErrorForUnknownStream() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    assertThrows(IllegalArgumentException.class, () -> {
      keeper.setStream("test app", "test substance", "unknown_stream",
                       new EngineNumber(new BigDecimal("100"), "kg"));
    }, "Should throw for unknown stream in setStream");

    assertThrows(IllegalArgumentException.class, () -> {
      keeper.getStream("test app", "test substance", "unknown_stream");
    }, "Should throw for unknown stream in getStream");
  }

  /**
   * Test that getRegisteredSubstances returns substance list.
   */
  @Test
  public void testGetRegisteredSubstancesReturnsSubstanceList() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("app1", "substance1");
    keeper.ensureSubstance("app2", "substance2");

    List<SubstanceInApplicationId> substances = keeper.getRegisteredSubstances();
    assertEquals(2, substances.size(), "Should return correct number of substances");

    SubstanceInApplicationId substance1 = substances.stream()
        .filter(s -> "app1".equals(s.getApplication()) && "substance1".equals(s.getSubstance()))
        .findFirst()
        .orElse(null);
    SubstanceInApplicationId substance2 = substances.stream()
        .filter(s -> "app2".equals(s.getApplication()) && "substance2".equals(s.getSubstance()))
        .findFirst()
        .orElse(null);

    assertNotNull(substance1, "Should find first substance");
    assertNotNull(substance2, "Should find second substance");
  }

  /**
   * Test last specified units getter and setter delegate to parameterization.
   */
  @Test
  public void testLastSpecifiedUnitsGetterAndSetterDelegateToParameterization() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    // Test default value
    String defaultUnits = keeper.getLastSpecifiedUnits("test app", "test substance");
    assertEquals("kg", defaultUnits, "Should have default units of kg");

    // Test setting and getting
    keeper.setLastSpecifiedUnits("test app", "test substance", "units");
    String retrieved = keeper.getLastSpecifiedUnits("test app", "test substance");
    assertEquals("units", retrieved, "Should retrieve set units");
  }

  /**
   * Test setStream automatically tracks last specified units.
   */
  @Test
  public void testSetStreamAutomaticallyTracksLastSpecifiedUnits() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    // Test default value
    String defaultUnits = keeper.getLastSpecifiedUnits("test app", "test substance");
    assertEquals("kg", defaultUnits, "Should have default units of kg");

    // Test setting units directly via StreamKeeper methods
    keeper.setLastSpecifiedUnits("test app", "test substance", "kg");
    String unitsAfterKg = keeper.getLastSpecifiedUnits("test app", "test substance");
    assertEquals("kg", unitsAfterKg, "Should have kg units after setting");

    // Test setting different units
    keeper.setLastSpecifiedUnits("test app", "test substance", "units");
    String unitsAfterUnits = keeper.getLastSpecifiedUnits("test app", "test substance");
    assertEquals("units", unitsAfterUnits, "Should have units after setting");
  }

  /**
   * Test setStream with units for sales components (manufacture/import).
   */
  @Test
  public void testSetStreamForSalesWithUnits() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    // Set initial charge of 2 kg/unit for manufacture stream
    keeper.setInitialCharge("test app", "test substance", "manufacture",
                           new EngineNumber(new BigDecimal("2.0"), "kg / unit"));

    // Set manufacture to 10 units - this should trigger setStreamForSalesWithUnits
    keeper.setStream("test app", "test substance", "manufacture",
                    new EngineNumber(new BigDecimal("10"), "units"));

    // Get the stream value back - should be converted to kg (10 units * 2 kg/unit = 20 kg)
    EngineNumber result = keeper.getStream("test app", "test substance", "manufacture");

    // The result should be in kg and the value should be 20
    assertEquals("kg", result.getUnits(), "Should convert units to kg");
    assertEquals(0, new BigDecimal("20.0").compareTo(result.getValue()),
                "Should multiply units by initial charge: 10 units * 2 kg/unit = 20 kg");
  }

  /**
   * Test setStreamForSalesWithUnits throws exception with zero initial charge.
   */
  @Test
  public void testSetStreamForSalesWithUnitsZeroInitialCharge() {
    StreamKeeper keeper = createMockKeeper();
    keeper.ensureSubstance("test app", "test substance");

    // Set initial charge to zero
    keeper.setInitialCharge("test app", "test substance", "manufacture",
                           new EngineNumber(BigDecimal.ZERO, "kg / unit"));

    // Attempting to set units should throw an exception
    assertThrows(RuntimeException.class, () -> {
      keeper.setStream("test app", "test substance", "manufacture",
                      new EngineNumber(new BigDecimal("10"), "units"));
    }, "Should throw exception when initial charge is zero");
  }
}
