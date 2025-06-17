/**
 * Import supplement data structure for engine results.
 *
 * <p>This class encapsulates supplemental import information needed for performing
 * user-configurable import attribution options (are substances associated with
 * importer or exporter).</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.serializer;

import org.kigalisim.engine.number.EngineNumber;

/**
 * Summary of imports for attribution purposes.
 *
 * <p>This class contains information about imported substances including their
 * volume, consumption impact, and associated equipment population.</p>
 */
public class ImportSupplement {
  private final EngineNumber initialChargeValue;
  private final EngineNumber initialChargeConsumption;
  private final EngineNumber newPopulation;

  /**
   * Create a new summary of imports.
   *
   * @param initialChargeValue The volume of substance imported
   *     via initial charge on imported equipment (like kg)
   * @param initialChargeConsumption The consumption associated with
   *     initial charge of imported equipment (like tCO2e)
   * @param newPopulation The number of new units imported
   */
  public ImportSupplement(EngineNumber initialChargeValue,
                         EngineNumber initialChargeConsumption,
                         EngineNumber newPopulation) {
    this.initialChargeValue = initialChargeValue;
    this.initialChargeConsumption = initialChargeConsumption;
    this.newPopulation = newPopulation;
  }

  /**
   * Get the volume of substance imported via initial charge on imported equipment.
   *
   * @return The initial charge value in volume units like kg
   */
  public EngineNumber getInitialChargeValue() {
    return initialChargeValue;
  }

  /**
   * Get the consumption associated with initial charge of imported equipment.
   *
   * @return The initial charge consumption value in units like tCO2e
   */
  public EngineNumber getInitialChargeConsumption() {
    return initialChargeConsumption;
  }

  /**
   * Get the number of new units imported.
   *
   * @return The new population value in units
   */
  public EngineNumber getNewPopulation() {
    return newPopulation;
  }
}
