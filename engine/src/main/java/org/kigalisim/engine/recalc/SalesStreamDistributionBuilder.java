/**
 * Builder for creating sales stream distribution percentages.
 *
 * <p>This class implements the logic for determining the appropriate percentage
 * split between import, domestic manufacture, and export based on which streams have been
 * explicitly enabled and their current values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Builder for creating sales stream distribution percentages.
 *
 * <p>This class implements the logic for determining the appropriate percentage
 * split between import, domestic manufacture, and export based on which streams have been
 * explicitly enabled and their current values.</p>
 */
public class SalesStreamDistributionBuilder {

  private Optional<EngineNumber> manufactureSales;
  private Optional<EngineNumber> importSales;
  private Optional<EngineNumber> exportSales;
  private Optional<Boolean> manufactureEnabled;
  private Optional<Boolean> importEnabled;
  private Optional<Boolean> exportEnabled;
  private Optional<Boolean> includeExports;

  /**
   * Create builder without any values initialized.
   */
  public SalesStreamDistributionBuilder() {
    manufactureSales = Optional.empty();
    importSales = Optional.empty();
    exportSales = Optional.empty();
    manufactureEnabled = Optional.empty();
    importEnabled = Optional.empty();
    exportEnabled = Optional.empty();
    includeExports = Optional.empty();
  }

  /**
   * Set the manufacture sales value.
   *
   * @param manufactureSales Current manufacture sales value
   * @return This builder for method chaining
   */
  public SalesStreamDistributionBuilder setManufactureSales(EngineNumber manufactureSales) {
    this.manufactureSales = Optional.of(manufactureSales);
    return this;
  }

  /**
   * Set the import sales value.
   *
   * @param importSales Current import sales value
   * @return This builder for method chaining
   */
  public SalesStreamDistributionBuilder setImportSales(EngineNumber importSales) {
    this.importSales = Optional.of(importSales);
    return this;
  }

  /**
   * Set whether manufacture stream is enabled.
   *
   * @param manufactureEnabled true if manufacture stream has ever been enabled
   * @return This builder for method chaining
   */
  public SalesStreamDistributionBuilder setManufactureEnabled(boolean manufactureEnabled) {
    this.manufactureEnabled = Optional.of(manufactureEnabled);
    return this;
  }

  /**
   * Set whether import stream is enabled.
   *
   * @param importEnabled true if import stream has ever been enabled
   * @return This builder for method chaining
   */
  public SalesStreamDistributionBuilder setImportEnabled(boolean importEnabled) {
    this.importEnabled = Optional.of(importEnabled);
    return this;
  }

  /**
   * Set the export sales value.
   *
   * @param exportSales Current export sales value
   * @return This builder for method chaining
   */
  public SalesStreamDistributionBuilder setExportSales(EngineNumber exportSales) {
    this.exportSales = Optional.of(exportSales);
    return this;
  }

  /**
   * Set whether export stream is enabled.
   *
   * @param exportEnabled true if export stream has ever been enabled
   * @return This builder for method chaining
   */
  public SalesStreamDistributionBuilder setExportEnabled(boolean exportEnabled) {
    this.exportEnabled = Optional.of(exportEnabled);
    return this;
  }

  /**
   * Set whether exports should be included in the distribution.
   *
   * @param includeExports true if exports should be included in distribution calculations
   * @return This builder for method chaining
   */
  public SalesStreamDistributionBuilder setIncludeExports(boolean includeExports) {
    this.includeExports = Optional.of(includeExports);
    return this;
  }

  /**
   * Build a sales stream distribution based on provided values.
   *
   * <p>Distribution logic:
   * <ul>
   * <li>If exports are excluded: 100% split between import and manufacture only</li>
   * <li>If exports are included: proportional split between import, manufacture, and export</li>
   * <li>Proportional split based on current values if streams have sales</li>
   * <li>Equal split among enabled streams if no current sales</li>
   * </ul>
   *
   * @return A SalesStreamDistribution with appropriate percentages
   * @throws IllegalStateException if any required field is missing
   */
  public SalesStreamDistribution build() {
    checkReadyToConstruct();
    
    BigDecimal manufactureSalesKg = manufactureSales.get().getValue();
    BigDecimal importSalesKg = importSales.get().getValue();
    BigDecimal exportSalesKg = exportSales.get().getValue();
    
    boolean includeExportsFlag = includeExports.get();
    
    if (!includeExportsFlag) {
      // Legacy behavior: only import and manufacture, export is always 0%
      BigDecimal totalSalesKg = manufactureSalesKg.add(importSalesKg);
      
      if (totalSalesKg.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal percentManufacture = manufactureSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        BigDecimal percentImport = importSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        return new SalesStreamDistribution(percentManufacture, percentImport, BigDecimal.ZERO);
      }
      
      // When both are zero, use enabled status to determine allocation
      if (manufactureEnabled.get() && !importEnabled.get()) {
        return new SalesStreamDistribution(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
      } else if (importEnabled.get() && !manufactureEnabled.get()) {
        return new SalesStreamDistribution(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO);
      } else {
        // Both enabled or both disabled - use 50/50 split
        return new SalesStreamDistribution(
            new BigDecimal("0.5"),
            new BigDecimal("0.5"),
            BigDecimal.ZERO
        );
      }
    } else {
      // Include exports in distribution
      BigDecimal totalSalesKg = manufactureSalesKg.add(importSalesKg).add(exportSalesKg);
      
      if (totalSalesKg.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal percentManufacture = manufactureSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        BigDecimal percentImport = importSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        BigDecimal percentExport = exportSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        return new SalesStreamDistribution(percentManufacture, percentImport, percentExport);
      }
      
      // When all are zero, use enabled status to determine allocation
      int enabledCount = 0;
      if (manufactureEnabled.get()) {
        enabledCount++;
      }
      if (importEnabled.get()) {
        enabledCount++;
      }
      if (exportEnabled.get()) {
        enabledCount++;
      }
      
      if (enabledCount == 0) {
        // None enabled - equal split among all three
        BigDecimal third = new BigDecimal("0.333333333333333");
        return new SalesStreamDistribution(third, third, third);
      } else {
        BigDecimal equalShare = BigDecimal.ONE.divide(BigDecimal.valueOf(enabledCount), MathContext.DECIMAL128);
        return new SalesStreamDistribution(
            manufactureEnabled.get() ? equalShare : BigDecimal.ZERO,
            importEnabled.get() ? equalShare : BigDecimal.ZERO,
            exportEnabled.get() ? equalShare : BigDecimal.ZERO
        );
      }
    }
  }

  /**
   * Check that all required fields are set before construction.
   *
   * @throws IllegalStateException if any required field is missing
   */
  private void checkReadyToConstruct() {
    checkValid(manufactureSales, "manufactureSales");
    checkValid(importSales, "importSales");
    checkValid(exportSales, "exportSales");
    checkValid(manufactureEnabled, "manufactureEnabled");
    checkValid(importEnabled, "importEnabled");
    checkValid(exportEnabled, "exportEnabled");
    checkValid(includeExports, "includeExports");
  }

  /**
   * Check if a value is valid (not empty).
   *
   * @param value The optional value to check
   * @param name The name of the field for error reporting
   * @throws IllegalStateException if the value is empty
   */
  private void checkValid(Optional<?> value, String name) {
    if (value.isEmpty()) {
      throw new IllegalStateException(
          "Could not make sales stream distribution because " + name + " was not given.");
    }
  }

}
