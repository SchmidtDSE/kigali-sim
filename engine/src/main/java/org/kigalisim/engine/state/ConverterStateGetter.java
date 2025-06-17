/**
 * Class providing state information needed for unit conversions.
 *
 * <p>Interfaces with the engine to retrieve information about current
 * substance consumption, volumes, populations, time elapsed, and other
 * metrics needed to convert between different unit types in the model.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;

/**
 * Class providing state information needed for unit conversions.
 *
 * <p>Interfaces with the engine to retrieve information about current
 * substance consumption, volumes, populations, time elapsed, and other
 * metrics needed to convert between different unit types in the model.</p>
 */
public class ConverterStateGetter implements StateGetter {

  private final Engine engine;

  /**
   * Create a new converter state getter.
   *
   * @param engine The engine instance to query for state information
   */
  public ConverterStateGetter(Engine engine) {
    this.engine = engine;
  }

  /**
   * Get the consumption ratio per unit volume of substance.
   *
   * @return The consumption per volume ratio in tCO2e/kg or tCO2e/mt
   * @throws RuntimeException If consumption or volume units are not as expected
   */
  @Override
  public EngineNumber getSubstanceConsumption() {
    return engine.getEqualsGhgIntensity();
  }

  /**
   * Get the energy consumption intensity per unit volume.
   *
   * @return The energy intensity as a ratio (e.g., kwh/mt or kwh/kg)
   * @throws RuntimeException If consumption or volume units are not as expected
   */
  @Override
  public EngineNumber getEnergyIntensity() {
    return engine.getEqualsEnergyIntensity();
  }

  /**
   * Get the amortized initial charge volume per unit for sales.
   *
   * @return The amortized initial charge volume in kg or mt per unit
   */
  @Override
  public EngineNumber getAmortizedUnitVolume() {
    return engine.getInitialCharge("sales");
  }

  /**
   * Get the current equipment population.
   *
   * @return The equipment count in units
   */
  @Override
  public EngineNumber getPopulation() {
    return engine.getStream("equipment");
  }

  /**
   * Get number of years in the simulation since the last step.
   *
   * @return The elapsed time in years since the last step
   */
  @Override
  public EngineNumber getYearsElapsed() {
    return new EngineNumber(BigDecimal.ONE, "year");
  }

  /**
   * Get the total ghg consumption for the current state.
   *
   * @return The consumption value in tCO2e
   */
  @Override
  public EngineNumber getGhgConsumption() {
    return engine.getStream("consumption");
  }

  /**
   * Get the total energy consumption for the current state.
   *
   * @return The consumption value in kwh
   */
  @Override
  public EngineNumber getEnergyConsumption() {
    return engine.getStream("energy");
  }

  /**
   * Get the total volume from sales for the current state.
   *
   * @return The volume in kg or mt
   */
  @Override
  public EngineNumber getVolume() {
    return engine.getStream("sales");
  }

  /**
   * Get the consumption ratio per unit of population.
   *
   * @return The consumption per unit ratio in tCO2e/unit
   * @throws RuntimeException If population or volume units are not as expected
   */
  @Override
  public EngineNumber getAmortizedUnitConsumption() {
    EngineNumber consumption = getGhgConsumption();
    EngineNumber population = getPopulation();

    BigDecimal ratioValue = consumption.getValue().divide(population.getValue());

    String populationUnits = population.getUnits();
    String consumptionUnits = consumption.getUnits();
    boolean populationUnitsExpected = "unit".equals(populationUnits)
        || "units".equals(populationUnits);
    boolean consumptionUnitsExpected = "tCO2e".equals(consumptionUnits);
    boolean unitsExpected = populationUnitsExpected && consumptionUnitsExpected;

    if (!unitsExpected) {
      throw new RuntimeException("Unexpected units for getAmortizedUnitConsumption.");
    }

    String ratioUnits = consumptionUnits + " / " + populationUnits;
    return new EngineNumber(ratioValue, ratioUnits);
  }

  /**
   * Calculate the change in population between prior and current equipment.
   *
   * @param unitConverter Converter for ensuring consistent units
   * @return The population change in units
   */
  @Override
  public EngineNumber getPopulationChange(UnitConverter unitConverter) {
    EngineNumber priorEquipmentRaw = engine.getStream("priorEquipment");
    EngineNumber newEquipmentRaw = engine.getStream("equipment");

    EngineNumber priorEquipment = unitConverter.convert(priorEquipmentRaw, "units");
    EngineNumber newEquipment = unitConverter.convert(newEquipmentRaw, "units");

    BigDecimal deltaValue = newEquipment.getValue().subtract(priorEquipment.getValue());
    return new EngineNumber(deltaValue, "units");
  }
}
