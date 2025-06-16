/**
 * Interface for accessing engine state needed for unit conversions.
 *
 * <p>This interface provides access to various engine state values that are
 * required for unit conversion operations, such as substance consumption,
 * volume, population, and time-based metrics.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.number;

/**
 * Interface defining methods to access engine state for unit conversions.
 *
 * <p>Implementations of this interface provide access to current engine state
 * including consumption rates, volumes, populations, and other metrics needed
 * to convert between different unit types in the simulation.</p>
 */
public interface StateGetter {

  /**
   * Get the substance consumption rate.
   *
   * @return The substance consumption rate with units like "tCO2e / kg"
   */
  EngineNumber getSubstanceConsumption();

  /**
   * Get the energy intensity.
   *
   * @return The energy intensity with units like "kwh / kg"
   */
  EngineNumber getEnergyIntensity();

  /**
   * Get the amortized unit volume.
   *
   * @return The amortized unit volume with units like "kg / unit"
   */
  EngineNumber getAmortizedUnitVolume();

  /**
   * Get the current population.
   *
   * @return The population with units like "units"
   */
  EngineNumber getPopulation();

  /**
   * Get the years elapsed.
   *
   * @return The years elapsed with units like "years"
   */
  EngineNumber getYearsElapsed();

  /**
   * Get the total GHG consumption.
   *
   * @return The GHG consumption with units like "tCO2e"
   */
  EngineNumber getGhgConsumption();

  /**
   * Get the total energy consumption.
   *
   * @return The energy consumption with units like "kwh"
   */
  EngineNumber getEnergyConsumption();

  /**
   * Get the current volume.
   *
   * @return The volume with units like "kg" or "mt"
   */
  EngineNumber getVolume();

  /**
   * Get the amortized unit consumption.
   *
   * @return The amortized unit consumption with units like "tCO2e / unit"
   */
  EngineNumber getAmortizedUnitConsumption();

  /**
   * Get the population change.
   *
   * @return The population change with units like "units"
   */
  EngineNumber getPopulationChange();
}