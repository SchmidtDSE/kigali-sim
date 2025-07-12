/**
 * Tests for SalesStreamDistributionBuilder class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for SalesStreamDistributionBuilder class.
 */
public class SalesStreamDistributionBuilderTest {

  @Test
  public void testProportionalSplitWithNonZeroValues() {
    // Test proportional split when both streams have non-zero values
    EngineNumber manufactureSales = new EngineNumber(new BigDecimal("60"), "kg");
    EngineNumber importSales = new EngineNumber(new BigDecimal("40"), "kg");
    EngineNumber exportSales = new EngineNumber(BigDecimal.ZERO, "kg");

    SalesStreamDistribution distribution = new SalesStreamDistributionBuilder()
        .setManufactureSales(manufactureSales)
        .setImportSales(importSales)
        .setExportSales(exportSales)
        .setManufactureEnabled(true)
        .setImportEnabled(true)
        .setExportEnabled(false)
        .setIncludeExports(false)
        .build();

    assertEquals(new BigDecimal("0.6"), distribution.getPercentManufacture(),
        "Manufacture percentage should be 0.6 (60/100)");
    assertEquals(new BigDecimal("0.4"), distribution.getPercentImport(),
        "Import percentage should be 0.4 (40/100)");
  }

  @Test
  public void testOnlyManufactureEnabled() {
    // Test 100% to manufacture when only manufacture was enabled
    EngineNumber manufactureSales = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber importSales = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber exportSales = new EngineNumber(BigDecimal.ZERO, "kg");

    SalesStreamDistribution distribution = new SalesStreamDistributionBuilder()
        .setManufactureSales(manufactureSales)
        .setImportSales(importSales)
        .setExportSales(exportSales)
        .setManufactureEnabled(true)
        .setImportEnabled(false)
        .setExportEnabled(false)
        .setIncludeExports(false)
        .build();

    assertEquals(BigDecimal.ONE, distribution.getPercentManufacture(),
        "Manufacture percentage should be 1.0 when only manufacture enabled");
    assertEquals(BigDecimal.ZERO, distribution.getPercentImport(),
        "Import percentage should be 0.0 when only manufacture enabled");
  }

  @Test
  public void testOnlyImportEnabled() {
    // Test 100% to import when only import was enabled
    EngineNumber manufactureSales = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber importSales = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber exportSales = new EngineNumber(BigDecimal.ZERO, "kg");

    SalesStreamDistribution distribution = new SalesStreamDistributionBuilder()
        .setManufactureSales(manufactureSales)
        .setImportSales(importSales)
        .setExportSales(exportSales)
        .setManufactureEnabled(false)
        .setImportEnabled(true)
        .setExportEnabled(false)
        .setIncludeExports(false)
        .build();

    assertEquals(BigDecimal.ZERO, distribution.getPercentManufacture(),
        "Manufacture percentage should be 0.0 when only import enabled");
    assertEquals(BigDecimal.ONE, distribution.getPercentImport(),
        "Import percentage should be 1.0 when only import enabled");
  }

  @Test
  public void testBothEnabledWithZeroValues() {
    // Test 50/50 fallback when both enabled but both have zero values
    EngineNumber manufactureSales = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber importSales = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber exportSales = new EngineNumber(BigDecimal.ZERO, "kg");

    SalesStreamDistribution distribution = new SalesStreamDistributionBuilder()
        .setManufactureSales(manufactureSales)
        .setImportSales(importSales)
        .setExportSales(exportSales)
        .setManufactureEnabled(true)
        .setImportEnabled(true)
        .setExportEnabled(false)
        .setIncludeExports(false)
        .build();

    assertEquals(new BigDecimal("0.5"), distribution.getPercentManufacture(),
        "Manufacture percentage should be 0.5 when both enabled with zero values");
    assertEquals(new BigDecimal("0.5"), distribution.getPercentImport(),
        "Import percentage should be 0.5 when both enabled with zero values");
  }

  @Test
  public void testNeitherEnabled() {
    // Test that exception is thrown when neither stream is enabled
    EngineNumber manufactureSales = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber importSales = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber exportSales = new EngineNumber(BigDecimal.ZERO, "kg");

    SalesStreamDistributionBuilder builder = new SalesStreamDistributionBuilder()
        .setManufactureSales(manufactureSales)
        .setImportSales(importSales)
        .setExportSales(exportSales)
        .setManufactureEnabled(false)
        .setImportEnabled(false)
        .setExportEnabled(false)
        .setIncludeExports(false);

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        builder::build,
        "Should throw exception when no streams are enabled");

    assertTrue(exception.getMessage().contains("no streams have been enabled"),
        "Exception message should mention no streams enabled");
  }

  @Test
  public void testProportionalSplitOverridesEnabledStatus() {
    // Test that proportional split takes precedence when both have non-zero values,
    // regardless of enabled status
    EngineNumber manufactureSales = new EngineNumber(new BigDecimal("30"), "kg");
    EngineNumber importSales = new EngineNumber(new BigDecimal("70"), "kg");

    // Even though only manufacture is enabled, proportional split should be used
    EngineNumber exportSales = new EngineNumber(BigDecimal.ZERO, "kg");
    SalesStreamDistribution distribution = new SalesStreamDistributionBuilder()
        .setManufactureSales(manufactureSales)
        .setImportSales(importSales)
        .setExportSales(exportSales)
        .setManufactureEnabled(true)
        .setImportEnabled(false)
        .setExportEnabled(false)
        .setIncludeExports(false)
        .build();

    assertEquals(new BigDecimal("0.3"), distribution.getPercentManufacture(),
        "Manufacture percentage should be 0.3 (30/100)");
    assertEquals(new BigDecimal("0.7"), distribution.getPercentImport(),
        "Import percentage should be 0.7 (70/100)");
  }

  @Test
  public void testUnequal100SplitPrecision() {
    // Test proportional split with values that result in repeating decimals
    EngineNumber manufactureSales = new EngineNumber(new BigDecimal("1"), "kg");
    EngineNumber importSales = new EngineNumber(new BigDecimal("2"), "kg");

    EngineNumber exportSales = new EngineNumber(BigDecimal.ZERO, "kg");
    SalesStreamDistribution distribution = new SalesStreamDistributionBuilder()
        .setManufactureSales(manufactureSales)
        .setImportSales(importSales)
        .setExportSales(exportSales)
        .setManufactureEnabled(true)
        .setImportEnabled(true)
        .setExportEnabled(false)
        .setIncludeExports(false)
        .build();

    // 1/3 and 2/3 split
    BigDecimal expectedManufacture = new BigDecimal("1").divide(new BigDecimal("3"),
        java.math.MathContext.DECIMAL128);
    BigDecimal expectedImport = new BigDecimal("2").divide(new BigDecimal("3"),
        java.math.MathContext.DECIMAL128);

    assertEquals(expectedManufacture, distribution.getPercentManufacture(),
        "Manufacture percentage should be 1/3");
    assertEquals(expectedImport, distribution.getPercentImport(),
        "Import percentage should be 2/3");
  }
}
