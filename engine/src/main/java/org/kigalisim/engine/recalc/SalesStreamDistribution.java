/**
 * Represents the percentage distribution between manufacture, import, and export streams for sales.
 *
 * <p>This class encapsulates the percentage split logic for distributing sales
 * between domestic manufacture, import, and export streams based on which streams have
 * been enabled and their current values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.math.BigDecimal;

/**
 * Represents the percentage distribution between manufacture, import, and export streams for sales.
 *
 * <p>This class encapsulates the percentage split logic for distributing sales
 * between domestic manufacture, import, and export streams based on which streams have
 * been enabled and their current values.</p>
 */
public class SalesStreamDistribution {

  private final BigDecimal percentManufacture;
  private final BigDecimal percentImport;
  private final BigDecimal percentExport;

  /**
   * Create a new sales stream distribution.
   *
   * @param percentManufacture The percentage of sales attributed to manufacture (0.0 to 1.0)
   * @param percentImport The percentage of sales attributed to import (0.0 to 1.0)
   * @param percentExport The percentage of sales attributed to export (0.0 to 1.0)
   */
  public SalesStreamDistribution(BigDecimal percentManufacture, BigDecimal percentImport, BigDecimal percentExport) {
    this.percentManufacture = percentManufacture;
    this.percentImport = percentImport;
    this.percentExport = percentExport;
  }

  /**
   * Create a new sales stream distribution without exports (legacy constructor).
   *
   * @param percentManufacture The percentage of sales attributed to manufacture (0.0 to 1.0)
   * @param percentImport The percentage of sales attributed to import (0.0 to 1.0)
   */
  public SalesStreamDistribution(BigDecimal percentManufacture, BigDecimal percentImport) {
    this(percentManufacture, percentImport, BigDecimal.ZERO);
  }

  /**
   * Get the percentage of sales attributed to manufacture.
   *
   * @return The manufacture percentage (0.0 to 1.0)
   */
  public BigDecimal getPercentManufacture() {
    return percentManufacture;
  }

  /**
   * Get the percentage of sales attributed to import.
   *
   * @return The import percentage (0.0 to 1.0)
   */
  public BigDecimal getPercentImport() {
    return percentImport;
  }

  /**
   * Get the percentage of sales attributed to export.
   *
   * @return The export percentage (0.0 to 1.0)
   */
  public BigDecimal getPercentExport() {
    return percentExport;
  }
}
