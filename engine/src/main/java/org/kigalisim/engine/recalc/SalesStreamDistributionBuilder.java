/**
 * Builder for creating sales stream distribution percentages.
 *
 * <p>This class implements the logic for determining the appropriate percentage
 * split between import, domestic, and export based on which streams have been
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
 * split between import, domestic, and export based on which streams have been
 * explicitly enabled and their current values.</p>
 */
public class SalesStreamDistributionBuilder {

  private Optional<EngineNumber> domesticSales;
  private Optional<EngineNumber> importSales;
  private Optional<EngineNumber> exportSales;
  private Optional<Boolean> domesticEnabled;
  private Optional<Boolean> importEnabled;
  private Optional<Boolean> exportEnabled;
  private Optional<Boolean> includeExports;

  /**
   * Create builder without any values initialized.
   */
  public SalesStreamDistributionBuilder() {
    domesticSales = Optional.empty();
    importSales = Optional.empty();
    exportSales = Optional.empty();
    domesticEnabled = Optional.empty();
    importEnabled = Optional.empty();
    exportEnabled = Optional.empty();
    includeExports = Optional.empty();
  }

  /**
   * Set the domestic sales value.
   *
   * @param domesticSales Current domestic sales value
   * @return This builder for method chaining
   */
  public SalesStreamDistributionBuilder setDomesticSales(EngineNumber domesticSales) {
    this.domesticSales = Optional.of(domesticSales);
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
   * Set whether domestic stream is enabled.
   *
   * @param domesticEnabled true if domestic stream has ever been enabled
   * @return This builder for method chaining
   */
  public SalesStreamDistributionBuilder setDomesticEnabled(boolean domesticEnabled) {
    this.domesticEnabled = Optional.of(domesticEnabled);
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
   * <li>If exports are excluded: 100% split between import and domestic only</li>
   * <li>If exports are included: proportional split between import, domestic, and export</li>
   * <li>Proportional split based on current values if streams have sales</li>
   * <li>Equal split among enabled streams if no current sales</li>
   * </ul>
   *
   * @return A SalesStreamDistribution with appropriate percentages
   * @throws IllegalStateException if any required field is missing
   */
  public SalesStreamDistribution build() {
    checkReadyToConstruct();

    BigDecimal domesticSalesKg = domesticSales.get().getValue();
    BigDecimal importSalesKg = importSales.get().getValue();
    BigDecimal exportSalesKg = exportSales.get().getValue();

    boolean includeExportsFlag = includeExports.get();

    if (!includeExportsFlag) {
      // Legacy behavior: only import and domestic, export is always 0%
      BigDecimal totalSalesKg = domesticSalesKg.add(importSalesKg);

      if (totalSalesKg.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal percentDomestic = domesticSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        BigDecimal percentImport = importSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        return new SalesStreamDistribution(percentDomestic, percentImport, BigDecimal.ZERO);
      }

      // When both are zero, use enabled status to determine allocation
      if (domesticEnabled.get() && !importEnabled.get()) {
        return new SalesStreamDistribution(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
      } else if (importEnabled.get() && !domesticEnabled.get()) {
        return new SalesStreamDistribution(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO);
      } else if (domesticEnabled.get() && importEnabled.get()) {
        // Both enabled - use 50/50 split
        return new SalesStreamDistribution(
            new BigDecimal("0.5"),
            new BigDecimal("0.5"),
            BigDecimal.ZERO
        );
      } else {
        // Both disabled - this is an error condition
        throw new IllegalStateException(
            "Cannot calculate sales distribution: no streams have been enabled. "
            + "Use 'set import' or 'set domestic' statements to enable streams before operations like 'recharge' that require sales recalculation."
        );
      }
    } else {
      // Include exports in distribution
      BigDecimal totalSalesKg = domesticSalesKg.add(importSalesKg).add(exportSalesKg);

      if (totalSalesKg.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal percentDomestic = domesticSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        BigDecimal percentImport = importSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        BigDecimal percentExport = exportSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
        return new SalesStreamDistribution(percentDomestic, percentImport, percentExport);
      }

      // When all are zero, use enabled status to determine allocation
      int enabledCount = 0;
      if (domesticEnabled.get()) {
        enabledCount++;
      }
      if (importEnabled.get()) {
        enabledCount++;
      }
      if (exportEnabled.get()) {
        enabledCount++;
      }

      if (enabledCount == 0) {
        // None enabled - this is an error condition
        throw new IllegalStateException(
            "Cannot calculate sales distribution: no streams have been enabled. "
            + "Use 'set import', 'set domestic', or 'set export' statements to enable streams before operations like 'recharge' that require sales recalculation."
        );
      } else {
        BigDecimal equalShare = BigDecimal.ONE.divide(BigDecimal.valueOf(enabledCount), MathContext.DECIMAL128);
        return new SalesStreamDistribution(
            domesticEnabled.get() ? equalShare : BigDecimal.ZERO,
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
    checkValid(domesticSales, "domesticSales");
    checkValid(importSales, "importSales");
    checkValid(exportSales, "exportSales");
    checkValid(domesticEnabled, "domesticEnabled");
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
