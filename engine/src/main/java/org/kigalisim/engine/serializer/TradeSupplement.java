/**
 * Trade supplement data structure for engine results.
 *
 * <p>This class encapsulates supplemental trade information (import and export)
 * needed for performing user-configurable trade attribution options.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.serializer;

import org.kigalisim.engine.number.EngineNumber;

/**
 * Summary of trade (imports and exports) for attribution purposes.
 *
 * <p>This class contains information about imported and exported substances
 * including their volume, consumption impact, and associated equipment population.</p>
 */
public class TradeSupplement {
  private final EngineNumber importInitialChargeValue;
  private final EngineNumber importInitialChargeConsumption;
  private final EngineNumber importPopulation;
  private final EngineNumber exportInitialChargeValue;
  private final EngineNumber exportInitialChargeConsumption;

  /**
   * Create a new summary of trade (imports and exports).
   *
   * @param importInitialChargeValue The volume of substance imported
   *     via initial charge on imported equipment (like kg)
   * @param importInitialChargeConsumption The consumption associated with
   *     initial charge of imported equipment (like tCO2e)
   * @param importPopulation The number of new units imported
   * @param exportInitialChargeValue The volume of substance exported
   *     via initial charge on exported equipment (like kg)
   * @param exportInitialChargeConsumption The consumption associated with
   *     initial charge of exported equipment (like tCO2e)
   */
  public TradeSupplement(EngineNumber importInitialChargeValue,
                         EngineNumber importInitialChargeConsumption,
                         EngineNumber importPopulation,
                         EngineNumber exportInitialChargeValue,
                         EngineNumber exportInitialChargeConsumption) {
    this.importInitialChargeValue = importInitialChargeValue;
    this.importInitialChargeConsumption = importInitialChargeConsumption;
    this.importPopulation = importPopulation;
    this.exportInitialChargeValue = exportInitialChargeValue;
    this.exportInitialChargeConsumption = exportInitialChargeConsumption;
  }

  /**
   * Get the volume of substance imported via initial charge on imported equipment.
   *
   * @return The initial charge value in volume units like kg
   */
  public EngineNumber getImportInitialChargeValue() {
    return importInitialChargeValue;
  }

  /**
   * Get the consumption associated with initial charge of imported equipment.
   *
   * @return The initial charge consumption value in units like tCO2e
   */
  public EngineNumber getImportInitialChargeConsumption() {
    return importInitialChargeConsumption;
  }

  /**
   * Get the number of new units imported.
   *
   * @return The import population value in units
   */
  public EngineNumber getImportPopulation() {
    return importPopulation;
  }

  /**
   * Get the volume of substance exported via initial charge on exported equipment.
   *
   * @return The initial charge value in volume units like kg
   */
  public EngineNumber getExportInitialChargeValue() {
    return exportInitialChargeValue;
  }

  /**
   * Get the consumption associated with initial charge of exported equipment.
   *
   * @return The initial charge consumption value in units like tCO2e
   */
  public EngineNumber getExportInitialChargeConsumption() {
    return exportInitialChargeConsumption;
  }
}
