/**
 * Immutable container for post-retirement stream values.
 *
 * <p>This class encapsulates the calculated new values for equipment streams after
 * retirement delta has been applied. All values are in units.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import org.kigalisim.engine.number.EngineNumber;

/**
 * Immutable container for post-retirement stream values.
 */
public class PostRetireNewLevels {

  private final EngineNumber newPrior;
  private final EngineNumber newEquipment;
  private final EngineNumber newRetired;

  /**
   * Create a new PostRetireNewLevels.
   *
   * @param newPrior New prior equipment value in units after retirement
   * @param newEquipment New total equipment value in units after retirement
   * @param newRetired New cumulative retired value in units after retirement
   */
  public PostRetireNewLevels(EngineNumber newPrior, EngineNumber newEquipment,
      EngineNumber newRetired) {
    this.newPrior = newPrior;
    this.newEquipment = newEquipment;
    this.newRetired = newRetired;
  }

  /**
   * Get the new prior equipment value.
   *
   * @return New prior equipment in units
   */
  public EngineNumber getNewPrior() {
    return newPrior;
  }

  /**
   * Get the new total equipment value.
   *
   * @return New total equipment in units
   */
  public EngineNumber getNewEquipment() {
    return newEquipment;
  }

  /**
   * Get the new retired value.
   *
   * @return New cumulative retired equipment in units
   */
  public EngineNumber getNewRetired() {
    return newRetired;
  }
}
