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
import org.kigalisim.engine.number.EngineNumber;

/**
 * Builder for creating sales stream distribution percentages.
 *
 * <p>This class implements the logic for determining the appropriate percentage
 * split between import and domestic manufacture based on which streams have been
 * explicitly enabled and their current values.</p>
 */
public class SalesStreamDistributionBuilder {

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
   */
  public static SalesStreamDistribution buildDistribution(
      EngineNumber manufactureSales,
      EngineNumber importSales,
      boolean manufactureEnabled,
      boolean importEnabled
  ) {
    BigDecimal manufactureSalesKg = manufactureSales.getValue();
    BigDecimal importSalesKg = importSales.getValue();
    BigDecimal totalSalesKg = manufactureSalesKg.add(importSalesKg);

    // If both streams have current sales, use proportional split
    if (totalSalesKg.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal percentManufacture = manufactureSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
      BigDecimal percentImport = importSalesKg.divide(totalSalesKg, MathContext.DECIMAL128);
      return new SalesStreamDistribution(percentManufacture, percentImport);
    }

    // When both are zero, use enabled status to determine allocation
    if (manufactureEnabled && !importEnabled) {
      return new SalesStreamDistribution(BigDecimal.ONE, BigDecimal.ZERO);
    } else if (importEnabled && !manufactureEnabled) {
      return new SalesStreamDistribution(BigDecimal.ZERO, BigDecimal.ONE);
    } else {
      // Both enabled or both disabled - use 50/50 split
      return new SalesStreamDistribution(
          new BigDecimal("0.5"),
          new BigDecimal("0.5")
      );
    }
  }
}