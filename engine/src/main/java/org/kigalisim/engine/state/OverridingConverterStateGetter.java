/**
 * Interface for overriding converter state getter.
 *
 * <p>This interface will be used for the future transition of converter state
 * management. It defines the contract for state getters that can override
 * values from an inner state getter.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;

/**
 * Interface for state getter that allows overriding values.
 *
 * <p>This interface defines methods for retrieving and overriding various
 * state values needed for unit conversions and calculations in the engine.</p>
 */
public interface OverridingConverterStateGetter {

  /**
   * Set the substance consumption value.
   *
   * @param newValue The new substance consumption value
   */
  void setSubstanceConsumption(EngineNumber newValue);

  /**
   * Get the substance consumption value.
   *
   * @return The substance consumption value
   */
  EngineNumber getSubstanceConsumption();

  /**
   * Set the energy intensity value.
   *
   * @param newValue The new energy intensity value
   */
  void setEnergyIntensity(EngineNumber newValue);

  /**
   * Get the energy intensity value.
   *
   * @return The energy intensity value
   */
  EngineNumber getEnergyIntensity();

  /**
   * Set the amortized unit volume value.
   *
   * @param newValue The new amortized unit volume value
   */
  void setAmortizedUnitVolume(EngineNumber newValue);

  /**
   * Get the amortized unit volume value.
   *
   * @return The amortized unit volume value
   */
  EngineNumber getAmortizedUnitVolume();

  /**
   * Set the population value.
   *
   * @param newValue The new population value
   */
  void setPopulation(EngineNumber newValue);

  /**
   * Get the population value.
   *
   * @return The population value
   */
  EngineNumber getPopulation();

  /**
   * Set the years elapsed value.
   *
   * @param newValue The new years elapsed value
   */
  void setYearsElapsed(EngineNumber newValue);

  /**
   * Get the years elapsed value.
   *
   * @return The years elapsed value
   */
  EngineNumber getYearsElapsed();

  /**
   * Get the GHG consumption value.
   *
   * @return The GHG consumption value
   */
  EngineNumber getGhgConsumption();

  /**
   * Get the energy consumption value.
   *
   * @return The energy consumption value
   */
  EngineNumber getEnergyConsumption();

  /**
   * Get the volume value.
   *
   * @return The volume value
   */
  EngineNumber getVolume();

  /**
   * Get the amortized unit consumption value.
   *
   * @return The amortized unit consumption value
   */
  EngineNumber getAmortizedUnitConsumption();

  /**
   * Set the population change value.
   *
   * @param newValue The new population change value
   */
  void setPopulationChange(EngineNumber newValue);

  /**
   * Get the population change value.
   *
   * @param unitConverter Converter for ensuring consistent units
   * @return The population change value
   */
  EngineNumber getPopulationChange(UnitConverter unitConverter);
}
