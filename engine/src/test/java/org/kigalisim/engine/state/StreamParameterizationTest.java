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

  /**
   * Test last specified units getter and setter.
   */
  @Test
  public void testLastSpecifiedUnitsGetterAndSetter() {
    StreamParameterization parameterization = new StreamParameterization();

    // Test default value
    String defaultUnits = parameterization.getLastSpecifiedUnits();
    assertEquals("kg", defaultUnits, "Should have default units of kg");

    // Test setting and getting units
    parameterization.setLastSpecifiedUnits("kg");
    String retrieved = parameterization.getLastSpecifiedUnits();
    assertEquals("kg", retrieved, "Should retrieve set units");

    // Test setting different units
    parameterization.setLastSpecifiedUnits("units");
    String retrievedUnits = parameterization.getLastSpecifiedUnits();
    assertEquals("units", retrievedUnits, "Should retrieve updated units");
  }

  /**
   * Test that resetInternals preserves last specified units.
   */
  @Test
  public void testResetInternalsPreservesLastSpecifiedUnits() {
    StreamParameterization parameterization = new StreamParameterization();

    // Set units and verify
    parameterization.setLastSpecifiedUnits("units");
    assertEquals("units", parameterization.getLastSpecifiedUnits(),
                 "Should have set units");

    // Reset and verify units are preserved
    parameterization.resetInternals();
    assertEquals("units", parameterization.getLastSpecifiedUnits(),
                 "Should preserve units context across resets");
  }

  /**
   * Test that setLastSpecifiedUnits ignores percentage units.
   */
  @Test
  public void testSetLastSpecifiedUnitsIgnoresPercentageUnits() {
    StreamParameterization parameterization = new StreamParameterization();

    // Set initial non-percentage units
    parameterization.setLastSpecifiedUnits("kg");
    assertEquals("kg", parameterization.getLastSpecifiedUnits(),
                 "Should set initial units");

    // Try to set percentage unit - should be ignored
    parameterization.setLastSpecifiedUnits("%");
    assertEquals("kg", parameterization.getLastSpecifiedUnits(),
                 "Pure percentage units should be ignored");

    // Try to set unit containing percentage - should be ignored
    parameterization.setLastSpecifiedUnits("kg / %");
    assertEquals("kg", parameterization.getLastSpecifiedUnits(),
                 "Units containing percentage should be ignored");

    // Try another percentage format - should be ignored
    parameterization.setLastSpecifiedUnits("15%");
    assertEquals("kg", parameterization.getLastSpecifiedUnits(),
                 "Percentage values should be ignored");

    // Set valid non-percentage units - should work
    parameterization.setLastSpecifiedUnits("units");
    assertEquals("units", parameterization.getLastSpecifiedUnits(),
                 "Non-percentage units should still work");

    // Try percentage again - should be ignored, keeping "units"
    parameterization.setLastSpecifiedUnits("%");
    assertEquals("units", parameterization.getLastSpecifiedUnits(),
                 "Percentage should still be ignored after setting valid units");
  }

  /**
   * Test that setLastSpecifiedUnits handles null values properly.
   */
  @Test
  public void testSetLastSpecifiedUnitsHandlesNull() {
    StreamParameterization parameterization = new StreamParameterization();

    // Set initial units
    parameterization.setLastSpecifiedUnits("kg");
    assertEquals("kg", parameterization.getLastSpecifiedUnits(),
                 "Should set initial units");

    // Try null - should update to null (original behavior preserved)
    parameterization.setLastSpecifiedUnits(null);
    assertEquals(null, parameterization.getLastSpecifiedUnits(),
                 "Null units should update last specified units (original behavior)");

    // Set valid units again - should work
    parameterization.setLastSpecifiedUnits("mt");
    assertEquals("mt", parameterization.getLastSpecifiedUnits(),
                 "Valid units should still work after null");
  }

  /**
   * Test that setLastSpecifiedUnits handles various percentage formats.
   */
  @Test
  public void testSetLastSpecifiedUnitsHandlesVariousPercentageFormats() {
    StreamParameterization parameterization = new StreamParameterization();

    // Set initial units
    parameterization.setLastSpecifiedUnits("kg");
    assertEquals("kg", parameterization.getLastSpecifiedUnits(),
                 "Should set initial units");

    // Test various percentage formats that should be ignored
    String[] percentageFormats = {
      "%",
      "50%",
      "kg/%",
      "units / %",
      "% / year",
      "tCO2e / %",
      "% per unit",
      "percentage %"
    };

    for (String format : percentageFormats) {
      parameterization.setLastSpecifiedUnits(format);
      assertEquals("kg", parameterization.getLastSpecifiedUnits(),
                   "Format \"" + format + "\" should be ignored");
    }

    // Test valid formats that should NOT be ignored
    String[] validFormats = {
      "units",
      "mt",
      "kg / unit",
      "tCO2e / kg",
      "kwh / kg",
      "year",
      "years"
    };

    for (String format : validFormats) {
      parameterization.setLastSpecifiedUnits(format);
      assertEquals(format, parameterization.getLastSpecifiedUnits(),
                   "Format \"" + format + "\" should be accepted");
    }
  }
}
