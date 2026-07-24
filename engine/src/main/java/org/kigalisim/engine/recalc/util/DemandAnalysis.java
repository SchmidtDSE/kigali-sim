/**
 * Immutable result of a demand analysis calculation.
 *
 * <p>This class encapsulates the outputs of {@link DemandAnalysisBuilder#build}, including the
 * total substance demand, whether unit-based specifications should be preserved, and the required
 * virgin material after accounting for recycling and induction effects.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc.util;

import java.math.BigDecimal;

/**
 * Immutable result of a demand analysis calculation.
 */
public class DemandAnalysis {

  private final BigDecimal totalDemand;
  private final boolean hadUnitBasedSpecs;
  private final BigDecimal requiredVirginMaterial;

  /**
   * Create a new DemandAnalysis.
   *
   * @param totalDemand The total substance demand minus implicit servicing in kg
   * @param hadUnitBasedSpecs True if unit-based specifications should be preserved
   * @param requiredVirginMaterial The required virgin material in kg after recycling effects
   */
  public DemandAnalysis(BigDecimal totalDemand, boolean hadUnitBasedSpecs,
      BigDecimal requiredVirginMaterial) {
    this.totalDemand = totalDemand;
    this.hadUnitBasedSpecs = hadUnitBasedSpecs;
    this.requiredVirginMaterial = requiredVirginMaterial;
  }

  /**
   * Get the total substance demand.
   *
   * @return The total demand minus implicit servicing in kg
   */
  public BigDecimal getTotalDemand() {
    return totalDemand;
  }

  /**
   * Check if unit-based specifications should be preserved.
   *
   * @return True if unit-based specifications should be preserved, false otherwise
   */
  public boolean getHadUnitBasedSpecs() {
    return hadUnitBasedSpecs;
  }

  /**
   * Get the required virgin material.
   *
   * @return The required virgin material in kg after accounting for recycling and induction
   */
  public BigDecimal getRequiredVirginMaterial() {
    return requiredVirginMaterial;
  }
}
