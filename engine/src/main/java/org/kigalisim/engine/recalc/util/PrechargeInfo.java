/**
 * Immutable description of precharge configuration.
 *
 * <p>This class describes whether a precharge population has been specified and, if so, whether it
 * is expressed as a percentage.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc.util;

/**
 * Immutable description of precharge configuration.
 */
public class PrechargeInfo {

  private final boolean hasPrecharge;
  private final boolean isPercentPrecharge;

  /**
   * Create a new PrechargeInfo.
   *
   * @param hasPrecharge True if a non-zero precharge population was specified
   * @param isPercentPrecharge True if the precharge population is percentage-based
   */
  public PrechargeInfo(boolean hasPrecharge, boolean isPercentPrecharge) {
    this.hasPrecharge = hasPrecharge;
    this.isPercentPrecharge = isPercentPrecharge;
  }

  /**
   * Check if a non-zero precharge population was specified.
   *
   * @return True if precharge is present, false otherwise
   */
  public boolean getHasPrecharge() {
    return hasPrecharge;
  }

  /**
   * Check if the precharge population is percentage-based.
   *
   * @return True if the precharge uses percentage units, false otherwise
   */
  public boolean getIsPercentPrecharge() {
    return isPercentPrecharge;
  }
}
