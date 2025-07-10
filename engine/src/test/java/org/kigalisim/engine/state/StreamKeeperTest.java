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
   * Create a test scope for testing.
   *
   * @return A new Scope with test values
   */
  private Scope createTestScope() {
    return new Scope("test stanza", "test app", "test substance");
  }

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
    Scope testScope = createTestScope();
    assertFalse(keeper.hasSubstance(testScope),
                "Should return false for unknown substance");
  }

  /**
   * Test that ensureSubstance creates new substance.
   */
  @Test
  public void testEnsureSubstanceCreatesNewSubstance() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();

    keeper.ensureSubstance(testScope);

    assertTrue(keeper.hasSubstance(testScope),
               "Should return true after ensuring substance");
  }

  /**
   * Test that ensureSubstance creates default streams.
   */
  @Test
  public void testEnsureSubstanceCreatesDefaultStreams() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();

    keeper.ensureSubstance(testScope);

    // Test that default streams exist with zero values
    EngineNumber manufacture = keeper.getStream(testScope, "manufacture");
    assertEquals(BigDecimal.ZERO, manufacture.getValue(),
                 "Manufacture should default to 0");
    assertEquals("kg", manufacture.getUnits(), "Manufacture should have kg units");

    EngineNumber importValue = keeper.getStream(testScope, "import");
    assertEquals(BigDecimal.ZERO, importValue.getValue(), "Import should default to 0");
    assertEquals("kg", importValue.getUnits(), "Import should have kg units");

    EngineNumber recycle = keeper.getStream(testScope, "recycle");
    assertEquals(BigDecimal.ZERO, recycle.getValue(), "Recycle should default to 0");
    assertEquals("kg", recycle.getUnits(), "Recycle should have kg units");

    EngineNumber consumption = keeper.getStream(testScope, "consumption");
    assertEquals(BigDecimal.ZERO, consumption.getValue(), "Consumption should default to 0");
    assertEquals("tCO2e", consumption.getUnits(), "Consumption should have tCO2e units");

    EngineNumber equipment = keeper.getStream(testScope, "equipment");
    assertEquals(BigDecimal.ZERO, equipment.getValue(), "Equipment should default to 0");
    assertEquals("units", equipment.getUnits(), "Equipment should have units");
  }

  /**
   * Test that setStream and getStream work for simple streams.
   */
  @Test
  public void testSetStreamAndGetStreamWorkForSimpleStreams() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable the manufacture stream first
    keeper.markStreamAsEnabled(testScope, "manufacture");

    EngineNumber newValue = new EngineNumber(new BigDecimal("100"), "kg");
    keeper.setStream(testScope, "manufacture", newValue);

    EngineNumber retrieved = keeper.getStream(testScope, "manufacture");
    assertEquals(new BigDecimal("100"), retrieved.getValue(),
                 "Should retrieve set value");
    assertEquals("kg", retrieved.getUnits(), "Should retrieve correct units");
  }

  /**
   * Test that sales stream returns sum of manufacture and import and recycle.
   */
  @Test
  public void testSalesStreamReturnsSumOfManufactureAndImportAndRecycle() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable the streams first
    keeper.markStreamAsEnabled(testScope, "manufacture");
    keeper.markStreamAsEnabled(testScope, "import");

    keeper.setStream(testScope, "manufacture",
                     new EngineNumber(new BigDecimal("50"), "kg"));
    keeper.setStream(testScope, "import",
                     new EngineNumber(new BigDecimal("30"), "kg"));
    keeper.setStream(testScope, "recycle",
                     new EngineNumber(new BigDecimal("10"), "kg"));

    EngineNumber sales = keeper.getStream(testScope, "sales");
    assertEquals(new BigDecimal("90"), sales.getValue(),
                 "Sales should be sum of manufacture, import, and recycle");
    assertEquals("kg", sales.getUnits(), "Sales should have kg units");
  }

  /**
   * Test that GHG intensity getter and setter delegate to parameterization.
   */
  @Test
  public void testGhgIntensityGetterAndSetterDelegateToParameterization() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    EngineNumber newValue = new EngineNumber(new BigDecimal("2.5"), "tCO2e / kg");
    keeper.setGhgIntensity(testScope, newValue);

    EngineNumber retrieved = keeper.getGhgIntensity(testScope);
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
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    EngineNumber newValue = new EngineNumber(new BigDecimal("1.5"), "kwh / kg");
    keeper.setEnergyIntensity(testScope, newValue);

    EngineNumber retrieved = keeper.getEnergyIntensity(testScope);
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
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    EngineNumber newValue = new EngineNumber(new BigDecimal("2.0"), "kg / unit");
    keeper.setInitialCharge(testScope, "manufacture", newValue);

    EngineNumber retrieved = keeper.getInitialCharge(testScope, "manufacture");
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
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Set equipment value
    keeper.setStream(testScope, "equipment",
                     new EngineNumber(new BigDecimal("150"), "units"));

    // Increment year
    keeper.incrementYear();

    // Check that equipment was moved to priorEquipment
    EngineNumber priorEquipment = keeper.getStream(testScope, "priorEquipment");
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
    Scope unknownScope = new Scope("test stanza", "unknown app", "unknown substance");

    assertThrows(IllegalStateException.class, () -> {
      keeper.setStream(unknownScope, "manufacture",
                       new EngineNumber(new BigDecimal("100"), "kg"));
    }, "Should throw for unknown substance in setStream");
  }

  /**
   * Test that error is thrown for unknown substance in getStream.
   */
  @Test
  public void testThrowsErrorForUnknownSubstanceInGetStream() {
    StreamKeeper keeper = createMockKeeper();
    Scope unknownScope = new Scope("test stanza", "unknown app", "unknown substance");

    assertThrows(IllegalStateException.class, () -> {
      keeper.getStream(unknownScope, "manufacture");
    }, "Should throw for unknown substance in getStream");
  }

  /**
   * Test that error is thrown for unknown stream.
   */
  @Test
  public void testThrowsErrorForUnknownStream() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    assertThrows(IllegalArgumentException.class, () -> {
      keeper.setStream(testScope, "unknown_stream",
                       new EngineNumber(new BigDecimal("100"), "kg"));
    }, "Should throw for unknown stream in setStream");

    assertThrows(IllegalArgumentException.class, () -> {
      keeper.getStream(testScope, "unknown_stream");
    }, "Should throw for unknown stream in getStream");
  }

  /**
   * Test that getRegisteredSubstances returns substance list.
   */
  @Test
  public void testGetRegisteredSubstancesReturnsSubstanceList() {
    StreamKeeper keeper = createMockKeeper();
    Scope scope1 = new Scope("test stanza", "app1", "substance1");
    Scope scope2 = new Scope("test stanza", "app2", "substance2");
    keeper.ensureSubstance(scope1);
    keeper.ensureSubstance(scope2);

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

  // Note: Tests for deprecated setLastSalesUnits and getLastSalesUnits methods have been removed.
  // The functionality is now tested through setLastSpecifiedValue and getLastSpecifiedValue tests.

  /**
   * Test setStream with units for sales components (manufacture/import).
   */
  @Test
  public void testSetStreamForSalesWithUnits() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable the manufacture stream first
    keeper.markStreamAsEnabled(testScope, "manufacture");

    // Set initial charge of 2 kg/unit for manufacture stream
    keeper.setInitialCharge(testScope, "manufacture",
                           new EngineNumber(new BigDecimal("2.0"), "kg / unit"));

    // Set manufacture to 10 units - this should trigger setStreamForSalesWithUnits
    keeper.setStream(testScope, "manufacture",
                    new EngineNumber(new BigDecimal("10"), "units"));

    // Get the stream value back - should be converted to kg (10 units * 2 kg/unit = 20 kg)
    EngineNumber result = keeper.getStream(testScope, "manufacture");

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
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Set initial charge to zero
    keeper.setInitialCharge(testScope, "manufacture",
                           new EngineNumber(BigDecimal.ZERO, "kg / unit"));

    // Attempting to set units should throw an exception
    assertThrows(RuntimeException.class, () -> {
      keeper.setStream(testScope, "manufacture",
                      new EngineNumber(new BigDecimal("10"), "units"));
    }, "Should throw exception when initial charge is zero");
  }

  /**
   * Test that setting a non-zero value on an unenabled stream throws exception.
   */
  @Test
  public void testAssertStreamEnabledThrowsForNonZeroOnUnenabledStream() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Set non-zero initial charge so the setStream won't fail for that reason
    keeper.setInitialCharge(testScope, "manufacture",
                           new EngineNumber(new BigDecimal("2.0"), "kg / unit"));

    // Don't enable the stream - this should cause the assertion to fail
    assertThrows(RuntimeException.class, () -> {
      keeper.setStream(testScope, "manufacture",
                      new EngineNumber(new BigDecimal("10"), "kg"));
    }, "Should throw exception when stream is not enabled and value is non-zero");
  }

  /**
   * Test that setting a zero value on an unenabled stream does not throw exception.
   */
  @Test
  public void testAssertStreamEnabledAllowsZeroOnUnenabledStream() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Don't enable the stream but try to set it to zero - this should work
    keeper.setStream(testScope, "manufacture",
                    new EngineNumber(BigDecimal.ZERO, "kg"));

    // Verify the value was set to zero
    EngineNumber result = keeper.getStream(testScope, "manufacture");
    assertEquals(BigDecimal.ZERO, result.getValue(),
                "Should allow setting zero value on unenabled stream");
  }

  /**
   * Test that setting a non-zero value on an enabled stream works correctly.
   */
  @Test
  public void testAssertStreamEnabledAllowsNonZeroOnEnabledStream() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable the stream first
    keeper.markStreamAsEnabled(testScope, "manufacture");

    // Set non-zero value - this should work
    keeper.setStream(testScope, "manufacture",
                    new EngineNumber(new BigDecimal("10"), "kg"));

    // Verify the value was set correctly
    EngineNumber result = keeper.getStream(testScope, "manufacture");
    assertEquals(new BigDecimal("10"), result.getValue(),
                "Should allow setting non-zero value on enabled stream");
  }

  /**
   * Test setting and getting last specified value.
   */
  @Test
  public void testSetAndGetLastSpecifiedValue() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Test setting a value with units
    EngineNumber testValue = new EngineNumber(new BigDecimal("800"), "units");
    keeper.setLastSpecifiedValue(testScope, "sales", testValue);

    // Test getting the value back
    EngineNumber retrieved = keeper.getLastSpecifiedValue(testScope, "sales");
    assertNotNull(retrieved, "Retrieved value should not be null");
    assertEquals(new BigDecimal("800"), retrieved.getValue(), "Value should match");
    assertEquals("units", retrieved.getUnits(), "Units should match");
  }

  /**
   * Test hasLastSpecifiedValue method.
   */
  @Test
  public void testHasLastSpecifiedValue() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Initially should not have a value
    assertFalse(keeper.hasLastSpecifiedValue(testScope, "sales"),
                "Should not have value initially");

    // Set a value
    EngineNumber testValue = new EngineNumber(new BigDecimal("500"), "units");
    keeper.setLastSpecifiedValue(testScope, "import", testValue);

    // Now should have the value
    assertTrue(keeper.hasLastSpecifiedValue(testScope, "import"),
               "Should have value after setting");
    assertFalse(keeper.hasLastSpecifiedValue(testScope, "sales"),
                "Should not have value for different stream");
  }

  /**
   * Test that percentage units are ignored in setLastSpecifiedValue.
   */
  @Test
  public void testSetLastSpecifiedValueIgnoresPercentages() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Set initial value
    EngineNumber initialValue = new EngineNumber(new BigDecimal("100"), "kg");
    keeper.setLastSpecifiedValue(testScope, "sales", initialValue);

    // Try to set percentage value - should be ignored
    EngineNumber percentValue = new EngineNumber(new BigDecimal("50"), "%");
    keeper.setLastSpecifiedValue(testScope, "sales", percentValue);

    // Original value should still be there
    EngineNumber retrieved = keeper.getLastSpecifiedValue(testScope, "sales");
    assertEquals("kg", retrieved.getUnits(), "Units should still be kg, not %");
    assertEquals(new BigDecimal("100"), retrieved.getValue(), 
                 "Value should be unchanged");
  }


  /**
   * Test setLastSpecifiedValue with null stream name throws appropriate exception.
   */
  @Test
  public void testSetLastSpecifiedValueWithUnknownSubstance() {
    StreamKeeper keeper = createMockKeeper();
    Scope unknownScope = new Scope("test", "unknown", "substance");
    
    EngineNumber testValue = new EngineNumber(new BigDecimal("100"), "kg");
    
    assertThrows(IllegalStateException.class, () -> {
      keeper.setLastSpecifiedValue(unknownScope, "sales", testValue);
    }, "Should throw exception for unknown substance");
  }

  /**
   * Test isSalesIntentFreshlySet method.
   */
  @Test
  public void testIsSalesIntentFreshlySet() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Initially should be false
    assertFalse(keeper.isSalesIntentFreshlySet(testScope),
                "Sales intent flag should start false");

    // Set a sales value - should set the flag
    EngineNumber salesValue = new EngineNumber(new BigDecimal("100"), "units");
    keeper.setLastSpecifiedValue(testScope, "sales", salesValue);
    
    assertTrue(keeper.isSalesIntentFreshlySet(testScope),
               "Sales intent flag should be true after setting sales value");
  }

  /**
   * Test resetSalesIntentFlag method.
   */
  @Test
  public void testResetSalesIntentFlag() {
    StreamKeeper keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Set a value to trigger the flag
    EngineNumber importValue = new EngineNumber(new BigDecimal("50"), "units");
    keeper.setLastSpecifiedValue(testScope, "import", importValue);
    
    assertTrue(keeper.isSalesIntentFreshlySet(testScope),
               "Flag should be true after setting import value");

    // Reset the flag
    keeper.resetSalesIntentFlag(testScope);
    
    assertFalse(keeper.isSalesIntentFreshlySet(testScope),
                "Flag should be false after reset");
  }

  /**
   * Test that sales intent flag is independent per scope.
   */
  @Test
  public void testSalesIntentFlagIndependentPerScope() {
    StreamKeeper keeper = createMockKeeper();
    Scope scope1 = new Scope("test1", "app1", "sub1");
    Scope scope2 = new Scope("test2", "app2", "sub2");
    
    keeper.ensureSubstance(scope1);
    keeper.ensureSubstance(scope2);

    // Set value for scope1
    EngineNumber value = new EngineNumber(new BigDecimal("100"), "kg");
    keeper.setLastSpecifiedValue(scope1, "manufacture", value);
    
    // Check flags
    assertTrue(keeper.isSalesIntentFreshlySet(scope1),
               "Scope1 flag should be true");
    assertFalse(keeper.isSalesIntentFreshlySet(scope2),
                "Scope2 flag should remain false");
    
    // Reset scope1 and set scope2
    keeper.resetSalesIntentFlag(scope1);
    keeper.setLastSpecifiedValue(scope2, "sales", value);
    
    // Check flags again
    assertFalse(keeper.isSalesIntentFreshlySet(scope1),
                "Scope1 flag should be false after reset");
    assertTrue(keeper.isSalesIntentFreshlySet(scope2),
               "Scope2 flag should be true after setting");
  }

  /**
   * Test sales intent flag with unknown substance.
   */
  @Test
  public void testSalesIntentFlagWithUnknownSubstance() {
    StreamKeeper keeper = createMockKeeper();
    Scope unknownScope = new Scope("test", "unknown", "substance");
    
    assertThrows(IllegalStateException.class, () -> {
      keeper.isSalesIntentFreshlySet(unknownScope);
    }, "Should throw exception for unknown substance when checking flag");
    
    assertThrows(IllegalStateException.class, () -> {
      keeper.resetSalesIntentFlag(unknownScope);
    }, "Should throw exception for unknown substance when resetting flag");
  }
}
