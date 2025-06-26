/**
 * Builder for creating sales stream distribution percentages.
 *
 * <p>This class implements the logic for determining the appropriate percentage
 * split between import and domestic manufacture based on which streams have been
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
 * split between import and domestic manufacture based on which streams have been
 * explicitly enabled and their current values.</p>
 */
public class SalesStreamDistributionBuilder {

  private Optional<EngineNumber> manufactureSales;
  private Optional<EngineNumber> importSales;
  private Optional<Boolean> manufactureEnabled;
  private Optional<Boolean> importEnabled;

  /**
   * Create builder without any values initialized.
   */
  public SalesStreamDistributionBuilder() {
    manufactureSales = Optional.empty();
    importSales = Optional.empty();
    manufactureEnabled = Optional.empty();
    importEnabled = Optional.empty();
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
   * Build a sales stream distribution based on provided values.
   *
   * <p>Distribution logic:
   * <ul>
   * <li>100% to import if only import was ever explicitly set to non-zero</li>
   * <li>100% to manufacture if only manufacture was ever explicitly set to non-zero</li>
   * <li>Current proportional split if both were enabled</li>
   * <li>50/50 split if neither was ever explicitly enabled (fallback)</li>
   * </ul>
   *
   * @return A SalesStreamDistribution with appropriate percentages
   * @throws IllegalStateException if any required field is missing
   */
  public SalesStreamDistribution build() {
    checkReadyToConstruct();
    
    BigDecimal manufactureSalesKg = manufactureSales.get().getValue();
    BigDecimal importSalesKg = importSales.get().getValue();
    BigDecimal totalSalesKg = manufactureSalesKg.add(importSalesKg);

    // If both streams have current sales, use proportional split
    if (totalSalesKg.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal percentManufacture = manufactureSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
      BigDecimal percentImport = importSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
      return new SalesStreamDistribution(percentManufacture, percentImport);
    }

    // When both are zero, use enabled status to determine allocation
    if (manufactureEnabled.get() && !importEnabled.get()) {
      return new SalesStreamDistribution(BigDecimal.ONE, BigDecimal.ZERO);
    } else if (importEnabled.get() && !manufactureEnabled.get()) {
      return new SalesStreamDistribution(BigDecimal.ZERO, BigDecimal.ONE);
    } else {
      // Both enabled or both disabled - use 50/50 split
      return new SalesStreamDistribution(
          new BigDecimal("0.5"),
          new BigDecimal("0.5")
      );
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
    checkValid(manufactureEnabled, "manufactureEnabled");
    checkValid(importEnabled, "importEnabled");
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

  /**
   * Build a sales stream distribution based on current values and enabled status.
   *
   * <p>Distribution logic:
   * <ul>
   * <li>100% to import if only import was ever explicitly set to non-zero</li>
   * <li>100% to manufacture if only manufacture was ever explicitly set to non-zero</li>
   * <li>Current proportional split if both were enabled</li>
   * <li>50/50 split if neither was ever explicitly enabled (fallback)</li>
   * </ul>
   *
   * @param manufactureSales Current manufacture sales value
   * @param importSales Current import sales value
   * @param manufactureEnabled true if manufacture stream has ever been enabled
   * @param importEnabled true if import stream has ever been enabled
   * @return A SalesStreamDistribution with appropriate percentages
   * @deprecated Use the builder pattern instead: new SalesStreamDistributionBuilder()
   *     .setManufactureSales(...).setImportSales(...).build()
   */
  @Deprecated
  public static SalesStreamDistribution buildDistribution(
      EngineNumber manufactureSales,
      EngineNumber importSales,
      boolean manufactureEnabled,
      boolean importEnabled
  ) {
    return new SalesStreamDistributionBuilder()
        .setManufactureSales(manufactureSales)
        .setImportSales(importSales)
        .setManufactureEnabled(manufactureEnabled)
        .setImportEnabled(importEnabled)
        .build();
  }
}
