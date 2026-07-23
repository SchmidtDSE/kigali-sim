/**
 * Immutable description of servicing configuration.
 *
 * <p>This class describes whether a servicing population has been specified and, if so, whether
 * it is expressed as a percentage. It is used for precharge today but is agnostic to whether the
 * servicing is precharge or recharge.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc.util;

/**
 * Immutable description of servicing configuration.
 */
public class ServicingStatus {

  private final boolean servicingEnabled;
  private final boolean percentPopulation;

  /**
   * Create a new ServicingStatus.
   *
   * @param servicingEnabled True if a non-zero servicing population was specified
   * @param percentPopulation True if the servicing population is percentage-based
   */
  public ServicingStatus(boolean servicingEnabled, boolean percentPopulation) {
    this.servicingEnabled = servicingEnabled;
    this.percentPopulation = percentPopulation;
  }

  /**
   * Check if a non-zero servicing population was specified.
   *
   * @return True if servicing is present, false otherwise
   */
  public boolean isServicingEnabled() {
    return servicingEnabled;
  }

  /**
   * Check if the servicing population is percentage-based.
   *
   * @return True if the servicing uses percentage units, false otherwise
   */
  public boolean isPercentPopulation() {
    return percentPopulation;
  }
}
