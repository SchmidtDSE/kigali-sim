/**
 * Unit tests for the StreamParameterization class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Tests for the StreamParameterization class.
 */
public class StreamParameterizationTest {

  /**
   * Test that StreamParameterization can be initialized.
   */
  @Test
  public void testInitializes() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    assertNotNull(parameterization, "StreamParameterization should be constructable");
  }

  /**
   * Test that resetInternals sets default values.
   */
  @Test
  public void testResetInternalsSetsDefaultValues() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Test GHG intensity default
    EngineNumber ghgIntensity = parameterization.getGhgIntensity();
    assertEquals(BigDecimal.ZERO, ghgIntensity.getValue(), "GHG intensity should default to 0");
    assertEquals("tCO2e / kg", ghgIntensity.getUnits(),
                 "GHG intensity should have correct units");

    // Test energy intensity default
    EngineNumber energyIntensity = parameterization.getEnergyIntensity();
    assertEquals(BigDecimal.ZERO, energyIntensity.getValue(),
                 "Energy intensity should default to 0");
    assertEquals("kwh / kg", energyIntensity.getUnits(),
                 "Energy intensity should have correct units");

    // Test initial charge defaults
    EngineNumber manufactureCharge = parameterization.getInitialCharge("domestic");
    assertEquals(BigDecimal.ONE, manufactureCharge.getValue(),
                 "Manufacture charge should default to 1");
    assertEquals("kg / unit", manufactureCharge.getUnits(),
                 "Manufacture charge should have correct units");

    EngineNumber importCharge = parameterization.getInitialCharge("import");
    assertEquals(BigDecimal.ONE, importCharge.getValue(),
                 "Import charge should default to 1");
    assertEquals("kg / unit", importCharge.getUnits(),
                 "Import charge should have correct units");

    // Test recharge population default
    EngineNumber rechargePopulation = parameterization.getRechargePopulation();
    assertEquals(BigDecimal.ZERO, rechargePopulation.getValue(),
                 "Recharge population should default to 0");
    assertEquals("%", rechargePopulation.getUnits(),
                 "Recharge population should have correct units");

    // Test recharge intensity default
    EngineNumber rechargeIntensity = parameterization.getRechargeIntensity();
    assertEquals(BigDecimal.ZERO, rechargeIntensity.getValue(),
                 "Recharge intensity should default to 0");
    assertEquals("kg / unit", rechargeIntensity.getUnits(),
                 "Recharge intensity should have correct units");

    // Test recovery rate default
    EngineNumber recoveryRate = parameterization.getRecoveryRate();
    assertEquals(BigDecimal.ZERO, recoveryRate.getValue(),
                 "Recovery rate should default to 0");
    assertEquals("%", recoveryRate.getUnits(), "Recovery rate should have correct units");

    // Test yield rate default
    EngineNumber yieldRate = parameterization.getYieldRate();
    assertEquals(BigDecimal.ZERO, yieldRate.getValue(), "Yield rate should default to 0");
    assertEquals("%", yieldRate.getUnits(), "Yield rate should have correct units");

    // Test retirement rate default
    EngineNumber retirementRate = parameterization.getRetirementRate();
    assertEquals(BigDecimal.ZERO, retirementRate.getValue(),
                 "Retirement rate should default to 0");
    assertEquals("%", retirementRate.getUnits(), "Retirement rate should have correct units");

  }

  /**
   * Test GHG intensity getter and setter.
   */
  @Test
  public void testGhgIntensityGetterAndSetter() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("2.5"), "kgCO2e / kg");

    parameterization.setGhgIntensity(newValue);
    EngineNumber retrieved = parameterization.getGhgIntensity();

    assertEquals(new BigDecimal("2.5"), retrieved.getValue(),
                 "Should retrieve set GHG intensity value");
    assertEquals("kgCO2e / kg", retrieved.getUnits(),
                 "Should retrieve correct GHG intensity units");
  }

  /**
   * Test energy intensity getter and setter.
   */
  @Test
  public void testEnergyIntensityGetterAndSetter() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("1.5"), "kwh / kg");

    parameterization.setEnergyIntensity(newValue);
    EngineNumber retrieved = parameterization.getEnergyIntensity();

    assertEquals(new BigDecimal("1.5"), retrieved.getValue(),
                 "Should retrieve set energy intensity value");
    assertEquals("kwh / kg", retrieved.getUnits(),
                 "Should retrieve correct energy intensity units");
  }

  /**
   * Test initial charge getter and setter for manufacture.
   */
  @Test
  public void testInitialChargeGetterAndSetterForManufacture() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("2.0"), "kg / unit");

    parameterization.setInitialCharge("domestic", newValue);
    EngineNumber retrieved = parameterization.getInitialCharge("domestic");

    assertEquals(new BigDecimal("2.0"), retrieved.getValue(),
                 "Should retrieve set initial charge value");
    assertEquals("kg / unit", retrieved.getUnits(),
                 "Should retrieve correct initial charge units");
  }

  /**
   * Test initial charge getter and setter for import.
   */
  @Test
  public void testInitialChargeGetterAndSetterForImport() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("1.8"), "kg / unit");

    parameterization.setInitialCharge("import", newValue);
    EngineNumber retrieved = parameterization.getInitialCharge("import");

    assertEquals(new BigDecimal("1.8"), retrieved.getValue(),
                 "Should retrieve set initial charge value");
    assertEquals("kg / unit", retrieved.getUnits(),
                 "Should retrieve correct initial charge units");
  }

  /**
   * Test that initial charge throws error for invalid stream.
   */
  @Test
  public void testInitialChargeThrowsErrorForInvalidStream() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(BigDecimal.ONE, "kg / unit");

    assertThrows(IllegalArgumentException.class, () -> {
      parameterization.setInitialCharge("invalid", newValue);
    }, "Should throw when setting initial charge for invalid stream");

    assertThrows(IllegalArgumentException.class, () -> {
      parameterization.getInitialCharge("invalid");
    }, "Should throw when getting initial charge for invalid stream");
  }

  /**
   * Test recharge population getter and setter.
   */
  @Test
  public void testRechargePopulationGetterAndSetter() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("15.5"), "%");

    parameterization.setRechargePopulation(newValue);
    EngineNumber retrieved = parameterization.getRechargePopulation();

    assertEquals(new BigDecimal("15.5"), retrieved.getValue(),
                 "Should retrieve set recharge population value");
    assertEquals("%", retrieved.getUnits(), "Should retrieve correct recharge population units");
  }

  /**
   * Test recharge intensity getter and setter.
   */
  @Test
  public void testRechargeIntensityGetterAndSetter() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("0.5"), "kg / unit");

    parameterization.setRechargeIntensity(newValue);
    EngineNumber retrieved = parameterization.getRechargeIntensity();

    assertEquals(new BigDecimal("0.5"), retrieved.getValue(),
                 "Should retrieve set recharge intensity value");
    assertEquals("kg / unit", retrieved.getUnits(),
                 "Should retrieve correct recharge intensity units");
  }

  /**
   * Test recovery rate getter and setter.
   */
  @Test
  public void testRecoveryRateGetterAndSetter() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("80.0"), "%");

    parameterization.setRecoveryRate(newValue);
    EngineNumber retrieved = parameterization.getRecoveryRate();

    assertEquals(new BigDecimal("80.0"), retrieved.getValue(),
                 "Should retrieve set recovery rate value");
    assertEquals("%", retrieved.getUnits(), "Should retrieve correct recovery rate units");
  }

  /**
   * Test yield rate getter and setter.
   */
  @Test
  public void testYieldRateGetterAndSetter() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("90.0"), "%");

    parameterization.setYieldRate(newValue);
    EngineNumber retrieved = parameterization.getYieldRate();

    assertEquals(new BigDecimal("90.0"), retrieved.getValue(),
                 "Should retrieve set yield rate value");
    assertEquals("%", retrieved.getUnits(), "Should retrieve correct yield rate units");
  }


  /**
   * Test retirement rate getter and setter.
   */
  @Test
  public void testRetirementRateGetterAndSetter() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("10.0"), "%");

    parameterization.setRetirementRate(newValue);
    EngineNumber retrieved = parameterization.getRetirementRate();

    assertEquals(new BigDecimal("10.0"), retrieved.getValue(),
                 "Should retrieve set retirement rate value");
    assertEquals("%", retrieved.getUnits(), "Should retrieve correct retirement rate units");
  }

  // Note: Tests for deprecated setLastSalesUnits and getLastSalesUnits methods have been removed.
  // The functionality is now tested through setLastSpecifiedValue and getLastSpecifiedValue tests.

  /**
   * Test setting and getting last specified value.
   */
  @Test
  public void testSetAndGetLastSpecifiedValue() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Test setting a value
    EngineNumber testValue = new EngineNumber(new BigDecimal("500"), "units");
    parameterization.setLastSpecifiedValue("import", testValue);

    // Test getting the value back
    EngineNumber retrieved = parameterization.getLastSpecifiedValue("import");
    assertNotNull(retrieved, "Retrieved value should not be null");
    assertEquals(new BigDecimal("500"), retrieved.getValue(), "Value should match");
    assertEquals("units", retrieved.getUnits(), "Units should match");

    // Test getting a non-existent value
    EngineNumber nonExistent = parameterization.getLastSpecifiedValue("sales");
    assertEquals(null, nonExistent, "Non-existent value should be null");
  }

  /**
   * Test setting and getting last specified value for virgin stream.
   */
  @Test
  public void testSetAndGetLastSpecifiedValueForVirgin() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Test setting a value for virgin stream
    EngineNumber testValue = new EngineNumber(new BigDecimal("500"), "units");
    parameterization.setLastSpecifiedValue("virgin", testValue);

    // Test getting the value back
    EngineNumber retrieved = parameterization.getLastSpecifiedValue("virgin");
    assertNotNull(retrieved, "Retrieved value should not be null");
    assertEquals(new BigDecimal("500"), retrieved.getValue(), "Value should match");
    assertEquals("units", retrieved.getUnits(), "Units should match");

    // Test getting a non-existent value
    EngineNumber nonExistent = parameterization.getLastSpecifiedValue("sales");
    assertEquals(null, nonExistent, "Non-existent value should be null");
  }

  /**
   * Test tracking of has last specified.
   */
  @Test
  public void testHasLastSpecifiedValue() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Initially should not have any values for sales
    assertFalse(parameterization.hasLastSpecifiedValue("sales"), "Should not have value initially");

    // Set a value
    EngineNumber testValue = new EngineNumber(new BigDecimal("800"), "units");
    parameterization.setLastSpecifiedValue("sales", testValue);

    // Now should have the value
    assertTrue(parameterization.hasLastSpecifiedValue("sales"), "Should have value after setting");
  }

  /**
   * Test tracking default has last specified is stale.
   */
  @Test
  public void testHasLastSpecifiedValueImplicit() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Initially should not have any values for sales
    assertFalse(parameterization.hasLastSpecifiedValue("sales"), "Should not have value initially");

    // Now should have the value
    assertTrue(parameterization.hasLastSpecifiedValue("import"), "Should not have value for different stream");
    assertFalse(parameterization.isSalesIntentFreshlySet(), "Default value should still be stale intent");
  }

  /**
   * Test that percentage units are ignored in setLastSpecifiedValue.
   */
  @Test
  public void testSetLastSpecifiedValueIgnoresPercentages() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Set initial value
    EngineNumber initialValue = new EngineNumber(new BigDecimal("100"), "kg");
    parameterization.setLastSpecifiedValue("sales", initialValue);

    // Try to set percentage value - should be ignored
    EngineNumber percentValue = new EngineNumber(new BigDecimal("50"), "%");
    parameterization.setLastSpecifiedValue("sales", percentValue);

    // Original value should still be there
    EngineNumber retrieved = parameterization.getLastSpecifiedValue("sales");
    assertEquals("kg", retrieved.getUnits(), "Units should still be kg, not %");
    assertEquals(new BigDecimal("100"), retrieved.getValue(),
                 "Value should be unchanged");
  }

  /**
   * Test salesIntentFreshlySet flag default value.
   */
  @Test
  public void testSalesIntentFreshlySetDefaultValue() {
    StreamParameterization parameterization = new MutableStreamParameterization();
    assertFalse(parameterization.isSalesIntentFreshlySet(), "Sales intent flag should default to false");
  }

  /**
   * Test salesIntentFreshlySet getter and setter.
   */
  @Test
  public void testSalesIntentFreshlySetGetterAndSetter() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Set to true
    parameterization.setSalesIntentFreshlySet(true);
    assertTrue(parameterization.isSalesIntentFreshlySet(), "Should return true after setting to true");

    // Set back to false
    parameterization.setSalesIntentFreshlySet(false);
    assertFalse(parameterization.isSalesIntentFreshlySet(), "Should return false after setting to false");
  }

  /**
   * Test that setLastSpecifiedValue sets salesIntentFreshlySet flag for sales streams.
   */
  @Test
  public void testSetLastSpecifiedValueSetsSalesIntentFlag() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Initially false
    assertFalse(parameterization.isSalesIntentFreshlySet(), "Flag should start false");

    // Set sales value - should set flag
    EngineNumber salesValue = new EngineNumber(new BigDecimal("100"), "units");
    parameterization.setLastSpecifiedValue("sales", salesValue);
    assertTrue(parameterization.isSalesIntentFreshlySet(), "Flag should be true after setting sales value");

    // Reset flag
    parameterization.setSalesIntentFreshlySet(false);

    // Set import value - should set flag
    EngineNumber importValue = new EngineNumber(new BigDecimal("50"), "units");
    parameterization.setLastSpecifiedValue("import", importValue);
    assertTrue(parameterization.isSalesIntentFreshlySet(), "Flag should be true after setting import value");

    // Reset flag
    parameterization.setSalesIntentFreshlySet(false);

    // Set manufacture value - should set flag
    EngineNumber manufactureValue = new EngineNumber(new BigDecimal("75"), "kg");
    parameterization.setLastSpecifiedValue("domestic", manufactureValue);
    assertTrue(parameterization.isSalesIntentFreshlySet(), "Flag should be true after setting manufacture value");
  }

  /**
   * Test that setLastSpecifiedValue does not set flag for non-sales streams.
   */
  @Test
  public void testSetLastSpecifiedValueDoesNotSetFlagForNonSalesStreams() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Set value for non-sales stream
    EngineNumber otherValue = new EngineNumber(new BigDecimal("200"), "kg");
    parameterization.setLastSpecifiedValue("consumption", otherValue);

    // Flag should remain false
    assertFalse(parameterization.isSalesIntentFreshlySet(), "Flag should remain false for non-sales streams");
  }

  /**
   * Test that percentage values don't affect sales intent flag.
   */
  @Test
  public void testPercentageValuesDontSetSalesIntentFlag() {
    StreamParameterization parameterization = new MutableStreamParameterization();

    // Try to set percentage value for sales stream
    EngineNumber percentValue = new EngineNumber(new BigDecimal("50"), "%");
    parameterization.setLastSpecifiedValue("sales", percentValue);

    // Flag should remain false since percentage values are ignored
    assertFalse(parameterization.isSalesIntentFreshlySet(), "Flag should remain false when percentage values are ignored");
  }


  /**
   * Test that freeze() creates an independent, immutable snapshot for StreamParameterization.
   */
  @Test
  public void testFreezeIndependent() {
    MutableStreamParameterization original = new MutableStreamParameterization();

    // Set some non-default values
    original.setGhgIntensity(new EngineNumber(new BigDecimal("2.5"), "kgCO2e / kg"));
    original.setEnergyIntensity(new EngineNumber(new BigDecimal("1.5"), "kwh / kg"));
    original.setInitialCharge("domestic", new EngineNumber(new BigDecimal("3.0"), "kg / unit"));
    original.setInitialCharge("import", new EngineNumber(new BigDecimal("2.0"), "kg / unit"));
    original.setLastSpecifiedValue("sales", new EngineNumber(new BigDecimal("100"), "units"));
    original.setLastSpecifiedValue("domestic", new EngineNumber(new BigDecimal("50"), "kg"));

    // Freeze
    StreamParameterization frozen = original.freeze();
    assertNotNull(frozen, "freeze() should return a non-null snapshot");

    // Mutate the original after freezing
    original.setGhgIntensity(new EngineNumber(new BigDecimal("9.9"), "kgCO2e / kg"));
    original.setEnergyIntensity(new EngineNumber(new BigDecimal("8.8"), "kwh / kg"));
    original.setInitialCharge("domestic", new EngineNumber(new BigDecimal("99.0"), "kg / unit"));
    original.setLastSpecifiedValue("sales", new EngineNumber(new BigDecimal("999"), "units"));
    original.setLastSpecifiedValue("domestic", new EngineNumber(new BigDecimal("888"), "kg"));

    // The frozen snapshot should retain the values as of the freeze() call
    assertEquals(new BigDecimal("2.5"), frozen.getGhgIntensity().getValue(),
                 "Frozen ghgIntensity should be unaffected by later mutation of the original");
    assertEquals("kgCO2e / kg", frozen.getGhgIntensity().getUnits(),
                 "Frozen ghgIntensity units should be unaffected by later mutation of the original");
    assertEquals(new BigDecimal("1.5"), frozen.getEnergyIntensity().getValue(),
                 "Frozen energyIntensity should be unaffected by later mutation of the original");
    assertEquals(new BigDecimal("3.0"), frozen.getInitialCharge("domestic").getValue(),
                 "Frozen initialCharge should be unaffected by later mutation of the original");
    assertEquals(new BigDecimal("2.0"), frozen.getInitialCharge("import").getValue(),
                 "Frozen import initialCharge should be unaffected by later mutation of the original");

    EngineNumber frozenSales = frozen.getLastSpecifiedValue("sales");
    assertNotNull(frozenSales, "Frozen lastSpecifiedValue should still exist");
    assertEquals(new BigDecimal("100"), frozenSales.getValue(),
                 "Frozen lastSpecifiedValue should be unaffected by later mutation of the original");

    EngineNumber frozenDomestic = frozen.getLastSpecifiedValue("domestic");
    assertNotNull(frozenDomestic, "Frozen lastSpecifiedValue for domestic should still exist");
    assertEquals(new BigDecimal("50"), frozenDomestic.getValue(),
                 "Frozen lastSpecifiedValue for domestic should be unaffected by later mutation");

    // Verify the original retains the mutated values
    assertEquals(new BigDecimal("9.9"), original.getGhgIntensity().getValue(),
                 "Original should have the mutated ghgIntensity");
    assertEquals(new BigDecimal("99.0"), original.getInitialCharge("domestic").getValue(),
                 "Original should have the mutated initialCharge");
    assertEquals(new BigDecimal("999"), original.getLastSpecifiedValue("sales").getValue(),
                 "Original should have the mutated lastSpecifiedValue");
  }

  /**
   * Test that freeze() on an already-frozen instance returns the same instance.
   */
  @Test
  public void testFreezeIsIdempotent() {
    StreamParameterization frozen = new MutableStreamParameterization().freeze();
    assertSame(frozen, frozen.freeze(), "Freezing an already-frozen instance should return itself");
  }

  /**
   * Test that freezing a StreamParameterization also freezes its nested priorEquipmentBases.
   */
  @Test
  public void testFreezeFreezesPriorEquipmentBases() {
    MutableStreamParameterization original = new MutableStreamParameterization();
    StreamParameterization frozen = original.freeze();

    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setRetirementBasePopulation(new EngineNumber(BigDecimal.TEN, "units")),
        "Nested priorEquipmentBases on a frozen StreamParameterization should also be frozen");
  }

  /**
   * Test that every mutator on a frozen instance throws UnsupportedOperationException.
   */
  @Test
  public void testFrozenMutatorsThrow() {
    StreamParameterization frozen = new MutableStreamParameterization().freeze();
    EngineNumber value = new EngineNumber(BigDecimal.ONE, "kg");

    assertThrows(UnsupportedOperationException.class, () -> frozen.setGhgIntensity(value));
    assertThrows(UnsupportedOperationException.class, () -> frozen.setEnergyIntensity(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setInitialCharge("domestic", value));
    assertThrows(UnsupportedOperationException.class, () -> frozen.setRechargePopulation(value));
    assertThrows(UnsupportedOperationException.class, () -> frozen.setRechargeIntensity(value));
    assertThrows(UnsupportedOperationException.class, () -> frozen.setPrechargePopulation(value));
    assertThrows(UnsupportedOperationException.class, () -> frozen.setPrechargeIntensity(value));
    assertThrows(UnsupportedOperationException.class, () -> frozen.setRecoveryRate(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setRecoveryRate(value, RecoveryStage.EOL));
    assertThrows(UnsupportedOperationException.class, () -> frozen.setYieldRate(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setYieldRate(value, RecoveryStage.EOL));
    assertThrows(UnsupportedOperationException.class, () -> frozen.setInductionRate(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setInductionRate(value, RecoveryStage.EOL));
    assertThrows(UnsupportedOperationException.class, () -> frozen.setRetirementRate(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setRetirementBasePopulation(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setAppliedRetirementAmount(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setHasReplacementThisStep(true));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setRetireCalculatedThisStep(true));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setRechargeBasePopulation(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setAppliedRechargeAmount(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setPrechargeBasePopulation(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setAppliedPrechargeAmount(value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setRecyclingCalculatedThisStep(true));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.accumulateRecharge(value, value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.accumulatePrecharge(value, value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setLastSpecifiedValue("domestic", value));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.markStreamAsEnabled("domestic"));
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.setSalesIntentFreshlySet(true));
    assertThrows(UnsupportedOperationException.class, () -> frozen.resetStateAtTimestep());
    assertThrows(UnsupportedOperationException.class,
        () -> frozen.clearLastSpecifiedValue("domestic"));
  }

}
