/**
 * Represents the percentage distribution between manufacture and import streams for sales.
 *
 * <p>This class encapsulates the percentage split logic for distributing sales
 * between domestic manufacture and import streams based on which streams have
 * been enabled and their current values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.math.BigDecimal;

/**
 * Represents the percentage distribution between manufacture and import streams for sales.
 *
 * <p>This class encapsulates the percentage split logic for distributing sales
 * between domestic manufacture and import streams based on which streams have
 * been enabled and their current values.</p>
 */
public class SalesStreamDistribution {

  private final BigDecimal percentManufacture;
  private final BigDecimal percentImport;

  /**
   * Create a new sales stream distribution.
   *
   * @param percentManufacture The percentage of sales attributed to manufacture (0.0 to 1.0)
   * @param percentImport The percentage of sales attributed to import (0.0 to 1.0)
   */
  public SalesStreamDistribution(BigDecimal percentManufacture, BigDecimal percentImport) {
    this.percentManufacture = percentManufacture;
    this.percentImport = percentImport;
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
}