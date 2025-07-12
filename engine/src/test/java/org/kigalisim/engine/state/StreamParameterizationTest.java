/**
 * Unit tests for the StreamParameterization class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for the StreamParameterization class.
 */
public class StreamParameterizationTest {

  /**
   * Test that StreamParameterization can be initialized.
   */
  @Test
  public void testInitializes() {
    StreamParameterization parameterization = new StreamParameterization();
    assertNotNull(parameterization, "StreamParameterization should be constructable");
  }

  /**
   * Test that resetInternals sets default values.
   */
  @Test
  public void testResetInternalsSetsDefaultValues() {
    StreamParameterization parameterization = new StreamParameterization();

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
    EngineNumber manufactureCharge = parameterization.getInitialCharge("manufacture");
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

    // Test displacement rate default
    EngineNumber displacementRate = parameterization.getDisplacementRate();
    assertEquals(new BigDecimal("100"), displacementRate.getValue(),
                 "Displacement rate should default to 100");
    assertEquals("%", displacementRate.getUnits(),
                 "Displacement rate should have correct units");
  }

  /**
   * Test GHG intensity getter and setter.
   */
  @Test
  public void testGhgIntensityGetterAndSetter() {
    StreamParameterization parameterization = new StreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("2.5"), "tCO2e / kg");

    parameterization.setGhgIntensity(newValue);
    EngineNumber retrieved = parameterization.getGhgIntensity();

    assertEquals(new BigDecimal("2.5"), retrieved.getValue(),
                 "Should retrieve set GHG intensity value");
    assertEquals("tCO2e / kg", retrieved.getUnits(),
                 "Should retrieve correct GHG intensity units");
  }

  /**
   * Test energy intensity getter and setter.
   */
  @Test
  public void testEnergyIntensityGetterAndSetter() {
    StreamParameterization parameterization = new StreamParameterization();
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
    StreamParameterization parameterization = new StreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("2.0"), "kg / unit");

    parameterization.setInitialCharge("manufacture", newValue);
    EngineNumber retrieved = parameterization.getInitialCharge("manufacture");

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
    StreamParameterization parameterization = new StreamParameterization();
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
    StreamParameterization parameterization = new StreamParameterization();
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
    StreamParameterization parameterization = new StreamParameterization();
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
    StreamParameterization parameterization = new StreamParameterization();
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
    StreamParameterization parameterization = new StreamParameterization();
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
    StreamParameterization parameterization = new StreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("90.0"), "%");

    parameterization.setYieldRate(newValue);
    EngineNumber retrieved = parameterization.getYieldRate();

    assertEquals(new BigDecimal("90.0"), retrieved.getValue(),
                 "Should retrieve set yield rate value");
    assertEquals("%", retrieved.getUnits(), "Should retrieve correct yield rate units");
  }

  /**
   * Test displacement rate getter and setter.
   */
  @Test
  public void testDisplacementRateGetterAndSetter() {
    StreamParameterization parameterization = new StreamParameterization();
    EngineNumber newValue = new EngineNumber(new BigDecimal("75.0"), "%");

    parameterization.setDisplacementRate(newValue);
    EngineNumber retrieved = parameterization.getDisplacementRate();

    assertEquals(new BigDecimal("75.0"), retrieved.getValue(),
                 "Should retrieve set displacement rate value");
    assertEquals("%", retrieved.getUnits(), "Should retrieve correct displacement rate units");
  }

  /**
   * Test retirement rate getter and setter.
   */
  @Test
  public void testRetirementRateGetterAndSetter() {
    StreamParameterization parameterization = new StreamParameterization();
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
    StreamParameterization parameterization = new StreamParameterization();

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
   * Test hasLastSpecifiedValue method.
   */
  @Test
  public void testHasLastSpecifiedValue() {
    StreamParameterization parameterization = new StreamParameterization();

    // Initially should not have any values
    assertEquals(false, parameterization.hasLastSpecifiedValue("sales"),
                 "Should not have value initially");

    // Set a value
    EngineNumber testValue = new EngineNumber(new BigDecimal("800"), "units");
    parameterization.setLastSpecifiedValue("sales", testValue);

    // Now should have the value
    assertEquals(true, parameterization.hasLastSpecifiedValue("sales"),
                 "Should have value after setting");
    assertEquals(false, parameterization.hasLastSpecifiedValue("import"),
                 "Should not have value for different stream");
  }

  /**
   * Test that percentage units are ignored in setLastSpecifiedValue.
   */
  @Test
  public void testSetLastSpecifiedValueIgnoresPercentages() {
    StreamParameterization parameterization = new StreamParameterization();

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
    StreamParameterization parameterization = new StreamParameterization();
    assertEquals(false, parameterization.isSalesIntentFreshlySet(),
                 "Sales intent flag should default to false");
  }

  /**
   * Test salesIntentFreshlySet getter and setter.
   */
  @Test
  public void testSalesIntentFreshlySetGetterAndSetter() {
    StreamParameterization parameterization = new StreamParameterization();

    // Set to true
    parameterization.setSalesIntentFreshlySet(true);
    assertEquals(true, parameterization.isSalesIntentFreshlySet(),
                 "Should return true after setting to true");

    // Set back to false
    parameterization.setSalesIntentFreshlySet(false);
    assertEquals(false, parameterization.isSalesIntentFreshlySet(),
                 "Should return false after setting to false");
  }

  /**
   * Test that setLastSpecifiedValue sets salesIntentFreshlySet flag for sales streams.
   */
  @Test
  public void testSetLastSpecifiedValueSetsSalesIntentFlag() {
    StreamParameterization parameterization = new StreamParameterization();

    // Initially false
    assertEquals(false, parameterization.isSalesIntentFreshlySet(),
                 "Flag should start false");

    // Set sales value - should set flag
    EngineNumber salesValue = new EngineNumber(new BigDecimal("100"), "units");
    parameterization.setLastSpecifiedValue("sales", salesValue);
    assertEquals(true, parameterization.isSalesIntentFreshlySet(),
                 "Flag should be true after setting sales value");

    // Reset flag
    parameterization.setSalesIntentFreshlySet(false);

    // Set import value - should set flag
    EngineNumber importValue = new EngineNumber(new BigDecimal("50"), "units");
    parameterization.setLastSpecifiedValue("import", importValue);
    assertEquals(true, parameterization.isSalesIntentFreshlySet(),
                 "Flag should be true after setting import value");

    // Reset flag
    parameterization.setSalesIntentFreshlySet(false);

    // Set manufacture value - should set flag
    EngineNumber manufactureValue = new EngineNumber(new BigDecimal("75"), "kg");
    parameterization.setLastSpecifiedValue("manufacture", manufactureValue);
    assertEquals(true, parameterization.isSalesIntentFreshlySet(),
                 "Flag should be true after setting manufacture value");
  }

  /**
   * Test that setLastSpecifiedValue does not set flag for non-sales streams.
   */
  @Test
  public void testSetLastSpecifiedValueDoesNotSetFlagForNonSalesStreams() {
    StreamParameterization parameterization = new StreamParameterization();

    // Set value for non-sales stream
    EngineNumber otherValue = new EngineNumber(new BigDecimal("200"), "kg");
    parameterization.setLastSpecifiedValue("consumption", otherValue);

    // Flag should remain false
    assertEquals(false, parameterization.isSalesIntentFreshlySet(),
                 "Flag should remain false for non-sales streams");
  }

  /**
   * Test that percentage values don't affect sales intent flag.
   */
  @Test
  public void testPercentageValuesDontSetSalesIntentFlag() {
    StreamParameterization parameterization = new StreamParameterization();

    // Try to set percentage value for sales stream
    EngineNumber percentValue = new EngineNumber(new BigDecimal("50"), "%");
    parameterization.setLastSpecifiedValue("sales", percentValue);

    // Flag should remain false since percentage values are ignored
    assertEquals(false, parameterization.isSalesIntentFreshlySet(),
                 "Flag should remain false when percentage values are ignored");
  }

}
