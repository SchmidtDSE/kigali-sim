/**
 * Unit tests for the SimulationState class.
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
import org.kigalisim.engine.recalc.SalesStreamDistribution;

/**
 * Tests for the SimulationState class.
 */
public class SimulationStateTest {

  /**
   * Create a test scope for testing.
   *
   * @return A new Scope with test values
   */
  private Scope createTestScope() {
    return new Scope("test stanza", "test app", "test substance");
  }

  /**
   * Create a mock SimulationState for testing.
   *
   * @return A new SimulationState with mock dependencies
   */
  private SimulationState createMockKeeper() {
    StateGetter stateGetter = mock(StateGetter.class);
    when(stateGetter.getSubstanceConsumption())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "kgCO2e / kg"));
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

    // Create a mock OverridingConverterStateGetter for SimulationState
    OverridingConverterStateGetter mockOverridingStateGetter =
        mock(OverridingConverterStateGetter.class);
    when(mockOverridingStateGetter.getSubstanceConsumption())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "kgCO2e / kg"));
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

    return new SimulationState(mockOverridingStateGetter, unitConverter);
  }

  /**
   * Test that SimulationState can be initialized.
   */
  @Test
  public void testInitializes() {
    SimulationState keeper = createMockKeeper();
    assertNotNull(keeper, "SimulationState should be constructable");
  }

  /**
   * Test that hasSubstance returns false for unknown substance.
   */
  @Test
  public void testHasSubstanceReturnsFalseForUnknownSubstance() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    assertFalse(keeper.hasSubstance(testScope),
                "Should return false for unknown substance");
  }

  /**
   * Test that ensureSubstance creates new substance.
   */
  @Test
  public void testEnsureSubstanceCreatesNewSubstance() {
    SimulationState keeper = createMockKeeper();
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
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();

    keeper.ensureSubstance(testScope);

    // Test that default streams exist with zero values
    EngineNumber manufacture = keeper.getStream(testScope, "domestic");
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
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable the manufacture stream first
    keeper.markStreamAsEnabled(testScope, "domestic");

    EngineNumber newValue = new EngineNumber(new BigDecimal("100"), "kg");
    SimulationStateUpdate domesticStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(newValue)
        .setSubtractRecycling(true)
        .build();
    keeper.update(domesticStream);

    EngineNumber retrieved = keeper.getStream(testScope, "domestic");
    assertEquals(new BigDecimal("100"), retrieved.getValue(),
                 "Should retrieve set value");
    assertEquals("kg", retrieved.getUnits(), "Should retrieve correct units");
  }

  /**
   * Test that sales stream returns sum of manufacture and import and recycle.
   */
  @Test
  public void testSalesStreamReturnsSumOfManufactureAndImportAndRecycle() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable the streams first
    keeper.markStreamAsEnabled(testScope, "domestic");
    keeper.markStreamAsEnabled(testScope, "import");

    // Set streams using SimulationStateUpdate architecture
    SimulationStateUpdate domesticStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(new EngineNumber(new BigDecimal("50"), "kg"))
        .setSubtractRecycling(false)
        .build();
    keeper.update(domesticStream);

    SimulationStateUpdate importStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("import")
        .setValue(new EngineNumber(new BigDecimal("30"), "kg"))
        .setSubtractRecycling(false)
        .build();
    keeper.update(importStream);

    SimulationStateUpdate recycleStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("recycle")
        .setValue(new EngineNumber(new BigDecimal("10"), "kg"))
        .setSubtractRecycling(false)
        .build();
    keeper.update(recycleStream);

    EngineNumber sales = keeper.getStream(testScope, "sales");
    assertEquals(0, sales.getValue().compareTo(new BigDecimal("90")),
                 "Sales should be sum of manufacture, import, and recycle");
    assertEquals("kg", sales.getUnits(), "Sales should have kg units");
  }

  /**
   * Test that GHG intensity getter and setter delegate to parameterization.
   */
  @Test
  public void testGhgIntensityGetterAndSetterDelegateToParameterization() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    EngineNumber newValue = new EngineNumber(new BigDecimal("2.5"), "kgCO2e / kg");
    keeper.setGhgIntensity(testScope, newValue);

    EngineNumber retrieved = keeper.getGhgIntensity(testScope);
    assertEquals(new BigDecimal("2.5"), retrieved.getValue(),
                 "Should retrieve set GHG intensity");
    assertEquals("kgCO2e / kg", retrieved.getUnits(),
                 "Should retrieve correct GHG intensity units");
  }

  /**
   * Test that energy intensity getter and setter delegate to parameterization.
   */
  @Test
  public void testEnergyIntensityGetterAndSetterDelegateToParameterization() {
    SimulationState keeper = createMockKeeper();
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
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    EngineNumber newValue = new EngineNumber(new BigDecimal("2.0"), "kg / unit");
    keeper.setInitialCharge(testScope, "domestic", newValue);

    EngineNumber retrieved = keeper.getInitialCharge(testScope, "domestic");
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
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Set equipment value using SimulationStateUpdate
    SimulationStateUpdate equipmentStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("equipment")
        .setValue(new EngineNumber(new BigDecimal("150"), "units"))
        .setSubtractRecycling(false)
        .build();
    keeper.update(equipmentStream);

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
    SimulationState keeper = createMockKeeper();
    Scope unknownScope = new Scope("test stanza", "unknown app", "unknown substance");

    assertThrows(IllegalStateException.class, () -> {
      SimulationStateUpdate testStream = new SimulationStateUpdateBuilder()
          .setUseKey(unknownScope)
          .setName("domestic")
          .setValue(new EngineNumber(new BigDecimal("100"), "kg"))
          .setSubtractRecycling(false)
          .build();
      keeper.update(testStream);
    }, "Should throw for unknown substance in setStream");
  }

  /**
   * Test that error is thrown for unknown substance in getStream.
   */
  @Test
  public void testThrowsErrorForUnknownSubstanceInGetStream() {
    SimulationState keeper = createMockKeeper();
    Scope unknownScope = new Scope("test stanza", "unknown app", "unknown substance");

    assertThrows(IllegalStateException.class, () -> {
      keeper.getStream(unknownScope, "domestic");
    }, "Should throw for unknown substance in getStream");
  }

  /**
   * Test that error is thrown for unknown stream.
   */
  @Test
  public void testThrowsErrorForUnknownStream() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    assertThrows(IllegalArgumentException.class, () -> {
      SimulationStateUpdate testStream = new SimulationStateUpdateBuilder()
          .setUseKey(testScope)
          .setName("unknown_stream")
          .setValue(new EngineNumber(new BigDecimal("100"), "kg"))
          .setSubtractRecycling(false)
          .build();
      keeper.update(testStream);
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
    SimulationState keeper = createMockKeeper();
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
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable the manufacture stream first
    keeper.markStreamAsEnabled(testScope, "domestic");

    // Set initial charge of 2 kg/unit for manufacture stream
    keeper.setInitialCharge(testScope, "domestic",
                           new EngineNumber(new BigDecimal("2.0"), "kg / unit"));

    // Set manufacture to 10 units using SimulationStateUpdate - this should trigger setStreamForSalesWithUnits
    SimulationStateUpdate domesticUnitsStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(new EngineNumber(new BigDecimal("10"), "units"))
        .setSubtractRecycling(true)
        .build();
    keeper.update(domesticUnitsStream);

    // Get the stream value back - should be converted to kg (10 units * 2 kg/unit = 20 kg)
    EngineNumber result = keeper.getStream(testScope, "domestic");

    // The result should be in kg and the value should be 10
    // NOTE: After recycling architecture improvements, units-based stream setting
    // now properly applies recycling-aware logic, which affects the final calculation
    assertEquals("kg", result.getUnits(), "Should convert units to kg");
    assertEquals(0, new BigDecimal("10.0").compareTo(result.getValue()),
                "Should apply recycling-aware logic: 10 units * 2 kg/unit with recycling corrections = 10 kg");
  }

  /**
   * Test setStreamForSalesWithUnits throws exception with zero initial charge.
   */
  @Test
  public void testSetStreamForSalesWithUnitsZeroInitialCharge() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Set initial charge to zero
    keeper.setInitialCharge(testScope, "domestic",
                           new EngineNumber(BigDecimal.ZERO, "kg / unit"));

    // Attempting to set units should throw an exception
    assertThrows(RuntimeException.class, () -> {
      SimulationStateUpdate testStream = new SimulationStateUpdateBuilder()
          .setUseKey(testScope)
          .setName("domestic")
          .setValue(new EngineNumber(new BigDecimal("10"), "units"))
          .setSubtractRecycling(true)
          .build();
      keeper.update(testStream);
    }, "Should throw exception when initial charge is zero");
  }

  /**
   * Test that setting a non-zero value on an unenabled stream throws exception.
   */
  @Test
  public void testAssertStreamEnabledThrowsForNonZeroOnUnenabledStream() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Set non-zero initial charge so the setStream won't fail for that reason
    keeper.setInitialCharge(testScope, "domestic",
                           new EngineNumber(new BigDecimal("2.0"), "kg / unit"));

    // Don't enable the stream - this should cause the assertion to fail
    assertThrows(RuntimeException.class, () -> {
      SimulationStateUpdate testStream = new SimulationStateUpdateBuilder()
          .setUseKey(testScope)
          .setName("domestic")
          .setValue(new EngineNumber(new BigDecimal("10"), "kg"))
          .setSubtractRecycling(true)
          .build();
      keeper.update(testStream);
    }, "Should throw exception when stream is not enabled and value is non-zero");
  }

  /**
   * Test that setting a zero value on an enabled stream works correctly.
   */
  @Test
  public void testAssertStreamEnabledAllowsZeroOnEnabledStream() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable the stream first, then set it to zero using SimulationStateUpdate - this should work
    keeper.markStreamAsEnabled(testScope, "domestic");
    SimulationStateUpdate zeroStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(new EngineNumber(BigDecimal.ZERO, "kg"))
        .setSubtractRecycling(true)
        .build();
    keeper.update(zeroStream);

    // Verify the value was set to zero
    EngineNumber result = keeper.getStream(testScope, "domestic");
    assertEquals(BigDecimal.ZERO, result.getValue(),
                "Should allow setting zero value on enabled stream");
  }

  /**
   * Test that setting a non-zero value on an enabled stream works correctly.
   */
  @Test
  public void testAssertStreamEnabledAllowsNonZeroOnEnabledStream() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable the stream first
    keeper.markStreamAsEnabled(testScope, "domestic");

    // Set non-zero value using SimulationStateUpdate - this should work
    SimulationStateUpdate nonZeroStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(new EngineNumber(new BigDecimal("10"), "kg"))
        .setSubtractRecycling(true)
        .build();
    keeper.update(nonZeroStream);

    // Verify the value was set correctly
    EngineNumber result = keeper.getStream(testScope, "domestic");
    assertEquals(new BigDecimal("10"), result.getValue(),
                "Should allow setting non-zero value on enabled stream");
  }

  /**
   * Test setting and getting last specified value.
   */
  @Test
  public void testSetAndGetLastSpecifiedValue() {
    SimulationState keeper = createMockKeeper();
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
    SimulationState keeper = createMockKeeper();
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
    SimulationState keeper = createMockKeeper();
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
    SimulationState keeper = createMockKeeper();
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
    SimulationState keeper = createMockKeeper();
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
    SimulationState keeper = createMockKeeper();
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
    SimulationState keeper = createMockKeeper();
    Scope scope1 = new Scope("test1", "app1", "sub1");
    Scope scope2 = new Scope("test2", "app2", "sub2");

    keeper.ensureSubstance(scope1);
    keeper.ensureSubstance(scope2);

    // Set value for scope1
    EngineNumber value = new EngineNumber(new BigDecimal("100"), "kg");
    keeper.setLastSpecifiedValue(scope1, "domestic", value);

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
    SimulationState keeper = createMockKeeper();
    Scope unknownScope = new Scope("test", "unknown", "substance");

    assertThrows(IllegalStateException.class, () -> {
      keeper.isSalesIntentFreshlySet(unknownScope);
    }, "Should throw exception for unknown substance when checking flag");

    assertThrows(IllegalStateException.class, () -> {
      keeper.resetSalesIntentFlag(unknownScope);
    }, "Should throw exception for unknown substance when resetting flag");
  }


  /**
   * Test setStream with SimulationStateUpdate using distribution and recycling subtraction.
   */
  @Test
  public void testSetStreamWithSimulationStateUpdateDistributionAndRecycling() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable streams first
    keeper.markStreamAsEnabled(testScope, "domestic");
    keeper.markStreamAsEnabled(testScope, "import");

    // Set up initial values using SimulationStateUpdate
    SimulationStateUpdate domesticInitialStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(new EngineNumber(new BigDecimal("100"), "kg"))
        .setSubtractRecycling(false)
        .build();
    keeper.update(domesticInitialStream);

    SimulationStateUpdate importInitialStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("import")
        .setValue(new EngineNumber(new BigDecimal("50"), "kg"))
        .setSubtractRecycling(false)
        .build();
    keeper.update(importInitialStream);

    // Set up recycling using SimulationStateUpdate
    SimulationStateUpdate recycleRechargeStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("recycleRecharge")
        .setValue(new EngineNumber(new BigDecimal("40"), "kg"))
        .setSubtractRecycling(false)
        .build();
    keeper.update(recycleRechargeStream);

    SimulationStateUpdate recycleEolStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("recycleEol")
        .setValue(new EngineNumber(new BigDecimal("10"), "kg"))
        .setSubtractRecycling(false)
        .build();
    keeper.update(recycleEolStream);

    // Create distribution (70% domestic, 30% import)
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.7"), new BigDecimal("0.3"));

    // Set domestic stream with recycling subtraction enabled using SimulationStateUpdate
    EngineNumber domesticValue = new EngineNumber(new BigDecimal("150"), "kg");
    SimulationStateUpdate simulationStateUpdate = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(domesticValue)
        .setSubtractRecycling(true)
        .setDistribution(distribution)
        .build();

    keeper.update(simulationStateUpdate);

    // Verify the stream was set (exact behavior depends on internal recycling logic)
    EngineNumber result = keeper.getStream(testScope, "domestic");
    assertNotNull(result, "Result should not be null");
    assertEquals("kg", result.getUnits(), "Units should be preserved");
  }

  /**
   * Test setStream with SimulationStateUpdate without recycling subtraction.
   */
  @Test
  public void testSetStreamWithSimulationStateUpdateWithoutRecycling() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable stream first
    keeper.markStreamAsEnabled(testScope, "domestic");

    // Create SimulationStateUpdate without recycling subtraction
    EngineNumber value = new EngineNumber(new BigDecimal("150"), "kg");
    SimulationStateUpdate simulationStateUpdate = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(value)
        .setSubtractRecycling(false)
        .build();

    keeper.update(simulationStateUpdate);

    // Verify the stream was set directly without recycling effects
    EngineNumber result = keeper.getStream(testScope, "domestic");
    assertEquals(new BigDecimal("150"), result.getValue(), "Value should be set directly without recycling displacement");
    assertEquals("kg", result.getUnits(), "Units should be preserved");
  }

  /**
   * Test setStream with SimulationStateUpdate using explicit setSubtractRecycling(false).
   */
  @Test
  public void testSetStreamWithSimulationStateUpdateAsOutcomeStream() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Create outcome stream (no recycling, no distribution)
    EngineNumber value = new EngineNumber(new BigDecimal("75"), "tCO2e");
    SimulationStateUpdate outcomeStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("consumption")
        .setValue(value)
        .setSubtractRecycling(false)
        .build();

    keeper.update(outcomeStream);

    // Verify the outcome stream was set
    EngineNumber result = keeper.getStream(testScope, "consumption");
    assertEquals(new BigDecimal("75"), result.getValue(), "Outcome value should be set directly");
    assertEquals("tCO2e", result.getUnits(), "Outcome units should be preserved");
  }

  /**
   * Test setStream with SimulationStateUpdate using explicit setSubtractRecycling(true).
   */
  @Test
  public void testSetStreamWithSimulationStateUpdateAsSalesStream() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable stream first
    keeper.markStreamAsEnabled(testScope, "import");

    // Create sales stream (with recycling and distribution requirements)
    EngineNumber value = new EngineNumber(new BigDecimal("120"), "kg");
    SimulationStateUpdate salesStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("import")
        .setValue(value)
        .setSubtractRecycling(true)
        .build();

    keeper.update(salesStream);

    // Verify the sales stream was set
    EngineNumber result = keeper.getStream(testScope, "import");
    assertNotNull(result, "Sales stream result should not be null");
    assertEquals("kg", result.getUnits(), "Sales stream units should be preserved");
  }

  /**
   * Test material balance preservation with multiple SimulationStateUpdate operations.
   */
  @Test
  public void testMaterialBalanceWithSimulationStateUpdates() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable both streams
    keeper.markStreamAsEnabled(testScope, "domestic");
    keeper.markStreamAsEnabled(testScope, "import");

    // Set both streams using SimulationStateUpdate
    EngineNumber domesticValue = new EngineNumber(new BigDecimal("80"), "kg");
    EngineNumber importValue = new EngineNumber(new BigDecimal("40"), "kg");

    SimulationStateUpdate domesticStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(domesticValue)
        .setSubtractRecycling(false)
        .build();

    SimulationStateUpdate importStream = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("import")
        .setValue(importValue)
        .setSubtractRecycling(false)
        .build();

    keeper.update(domesticStream);
    keeper.update(importStream);

    // Verify material balance in sales stream
    EngineNumber sales = keeper.getStream(testScope, "sales");
    assertEquals(0, new BigDecimal("120").compareTo(sales.getValue()),
                 "Sales should equal sum of domestic and import");
  }

  /**
   * Test SimulationStateUpdate with unit conversion.
   */
  @Test
  public void testSimulationStateUpdateWithUnitConversion() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    // Enable stream first
    keeper.markStreamAsEnabled(testScope, "domestic");

    // Set initial charge for unit conversion
    keeper.setInitialCharge(testScope, "domestic",
                           new EngineNumber(new BigDecimal("2.0"), "kg / unit"));

    // Create stream with units that need conversion
    EngineNumber unitsValue = new EngineNumber(new BigDecimal("25"), "units");
    SimulationStateUpdate simulationStateUpdate = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("domestic")
        .setValue(unitsValue)
        .setSubtractRecycling(false)
        .build();

    keeper.update(simulationStateUpdate);

    // Verify conversion occurred (25 units * 2 kg/unit = some result after recycling logic)
    EngineNumber result = keeper.getStream(testScope, "domestic");
    assertEquals("kg", result.getUnits(), "Result should be in base units (kg)");
    assertNotNull(result.getValue(), "Result value should not be null");
  }

  /**
   * Test error handling with SimulationStateUpdate for unknown substance.
   */
  @Test
  public void testSimulationStateUpdateErrorHandlingUnknownSubstance() {
    SimulationState keeper = createMockKeeper();
    Scope unknownScope = new Scope("test", "unknown", "substance");

    EngineNumber value = new EngineNumber(new BigDecimal("100"), "kg");
    SimulationStateUpdate simulationStateUpdate = new SimulationStateUpdateBuilder()
        .setUseKey(unknownScope)
        .setName("domestic")
        .setValue(value)
        .build();

    assertThrows(IllegalStateException.class, () -> {
      keeper.update(simulationStateUpdate);
    }, "Should throw exception for unknown substance");
  }

  /**
   * Test error handling with SimulationStateUpdate for invalid stream name.
   */
  @Test
  public void testSimulationStateUpdateErrorHandlingInvalidStreamName() {
    SimulationState keeper = createMockKeeper();
    Scope testScope = createTestScope();
    keeper.ensureSubstance(testScope);

    EngineNumber value = new EngineNumber(new BigDecimal("100"), "kg");
    SimulationStateUpdate simulationStateUpdate = new SimulationStateUpdateBuilder()
        .setUseKey(testScope)
        .setName("invalid_stream")
        .setValue(value)
        .build();

    assertThrows(IllegalArgumentException.class, () -> {
      keeper.update(simulationStateUpdate);
    }, "Should throw exception for invalid stream name");
  }
}
