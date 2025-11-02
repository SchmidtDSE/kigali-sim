/**
 * Immutable container for retirement background state information.
 *
 * <p>This class encapsulates the current state values needed for retirement calculations,
 * including current equipment populations, retired amounts, and the base population used
 * for retirement rate calculations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import org.kigalisim.engine.number.EngineNumber;

/**
 * Immutable container for retirement background state information.
 */
public class RetireBackgroundInfo {

  private final EngineNumber currentPrior;
  private final EngineNumber currentEquipment;
  private final EngineNumber currentRetired;
  private final EngineNumber basePopulation;
  private final boolean firstRetire;

  /**
   * Create a new RetireBackgroundInfo.
   *
   * @param currentPrior Current prior equipment value in units
   * @param currentEquipment Current total equipment value in units
   * @param currentRetired Current retired value in units
   * @param basePopulation Base population for retirement rate calculations
   * @param firstRetire Whether this is the first retirement operation for this scope
   */
  public RetireBackgroundInfo(EngineNumber currentPrior, EngineNumber currentEquipment,
      EngineNumber currentRetired, EngineNumber basePopulation, boolean firstRetire) {
    this.currentPrior = currentPrior;
    this.currentEquipment = currentEquipment;
    this.currentRetired = currentRetired;
    this.basePopulation = basePopulation;
    this.firstRetire = firstRetire;
  }

  /**
   * Get the current prior equipment value.
   *
   * @return Current prior equipment in units
   */
  public EngineNumber getCurrentPrior() {
    return currentPrior;
  }

  /**
   * Get the current total equipment value.
   *
   * @return Current total equipment in units
   */
  public EngineNumber getCurrentEquipment() {
    return currentEquipment;
  }

  /**
   * Get the current retired value.
   *
   * @return Current cumulative retired equipment in units
   */
  public EngineNumber getCurrentRetired() {
    return currentRetired;
  }

  /**
   * Get the base population for retirement calculations.
   *
   * @return Base population used for converting retirement rates to absolute amounts
   */
  public EngineNumber getBasePopulation() {
    return basePopulation;
  }

  /**
   * Check if this is the first retirement operation.
   *
   * @return true if this is the first retire operation for this scope, false otherwise
   */
  public boolean isFirstRetire() {
    return firstRetire;
  }
}
