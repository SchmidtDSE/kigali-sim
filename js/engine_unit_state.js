/**
 * Structures for handling state information needed for unit conversions.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";

/**
 * Class providing state information needed for unit conversions.
 *
 * Interfaces with the engine to retrieve information about current
 * substance consumption, volumes, populations, time elapsed, and other
 * metrics needed to convert between different unit types in the model.
 */
class ConverterStateGetter {
  /**
   * Create a new converter state getter.
   *
   * @param {Engine} engine - The engine instance to query for state information.
   */
  constructor(engine) {
    const self = this;
    self._engine = engine;
  }

  /**
   * Get the consumption ratio per unit volume of substance.
   *
   * @returns {EngineNumber} The consumption per volume ratio in tCO2e/kg or tCO2e/mt.
   * @throws {string} If consumption or volume units are not as expected.
   */
  getSubstanceConsumption() {
    const self = this;
    return self._engine.getEqualsGhgIntensity();
  }

  /**
   * Get the energy consumption intensity per unit volume.
   *
   * @returns {EngineNumber} The energy intensity as a ratio (e.g., kwh/mt or kwh/kg).
   * @throws {string} If the consumption or volume units are not as expected.
   */
  getEnergyIntensity() {
    const self = this;
    return self._engine.getEqualsEnergyIntensity();
  }

  /**
   * Get the charge volume per unit for sales.
   *
   * @returns {EngineNumber} The charge volume in kg or mt per unit.
   */
  getAmortizedUnitVolume() {
    const self = this;
    return self._engine.getInitialCharge("sales");
  }

  /**
   * Get the current equipment population.
   *
   * @returns {EngineNumber} The equipment count in units.
   */
  getPopulation() {
    const self = this;
    return self._engine.getStream("equipment");
  }

  /**
   * Get number of yearsin the simulation since the last step.
   *
   * @returns {EngineNumber} The elapsed time in years since the last step.
   */
  getYearsElapsed() {
    const self = this;
    return new EngineNumber(1, "year");
  }

  /**
   * Get the total ghg consumption for the current state.
   *
   * @returns {EngineNumber} The consumption value in tCO2e.
   */
  getGhgConsumption() {
    const self = this;
    return self._engine.getStream("consumption");
  }

  /**
   * Get the total energy consumption for the current state.
   *
   * @returns {EngineNumber} The consumption value in kwh.
   */
  getEnergyConsumption() {
    const self = this;
    return self._engine.getStream("energy");
  }

  /**
   * Get the total volume from sales for the current state.
   *
   * @returns {EngineNumber} The volume in kg or mt.
   */
  getVolume() {
    const self = this;
    const sales = self._engine.getStream("sales");
    return sales;
  }

  /**
   * Get the consumption ratio per unit of population.
   *
   * @returns {EngineNumber} The consumption per unit ratio in tCO2e/unit.
   * @throws {string} If population or volume units are not as expected.
   */
  getAmortizedUnitConsumption() {
    const self = this;
    const consumption = self.getGhgConsumption();
    const population = self.getPopulation();
    const ratioValue = consumption.getValue() / population.getValue();

    const populationUnits = population.getUnits();
    const consumptionUnits = consumption.getUnits();
    const populationUnitsExpected = populationUnits === "unit" || populationUnits === "units";
    const consumptionUnitsExpected = consumptionUnits === "tCO2e";
    const unitsExpected = populationUnitsExpected && consumptionUnitsExpected;
    if (!unitsExpected) {
      throw "Unexpected units for getAmortizedUnitConsumption.";
    }

    const ratioUnits = consumptionUnits + " / " + populationUnits;
    return new EngineNumber(ratioValue, ratioUnits);
  }

  /**
   * Calculate the change in population between prior and current equipment.
   *
   * @param {UnitConverter} unitConverter - Converter for ensuring consistent units.
   * @returns {EngineNumber} The population change in units.
   */
  getPopulationChange(unitConverter) {
    const self = this;

    const priorEquipmentRaw = self._engine.getStream("priorEquipment");
    const newEquipmentRaw = self._engine.getStream("equipment");

    const priorEquipment = unitConverter.convert(priorEquipmentRaw, "units").getValue();
    const newEquipment = unitConverter.convert(newEquipmentRaw, "units").getValue();

    const deltaValue = newEquipment - priorEquipment;
    return new EngineNumber(deltaValue, "units");
  }
}

/**
 * State getter that allows overriding values from an inner state getter.
 */
class OverridingConverterStateGetter {
  /**
   * Create a new overriding converter state getter.
   *
   * @param {ConverterStateGetter} innerGetter - The base state getter to wrap.
   */
  constructor(innerGetter) {
    const self = this;
    self._innerGetter = innerGetter;
    self._energyIntensity = null;
    self._amortizedUnitVolume = null;
    self._population = null;
    self._yearsElapsed = null;
    self._totalConsumption = null;
    self._energyConsumption = null;
    self._volume = null;
    self._amortizedUnitConsumption = null;
    self._populationChange = null;
    self._substanceConsumption = null;
  }

  /**
   * Set total values for different stream types.
   *
   * @param {string} streamName - The name of the stream (sales, manufacture,
   *     import, etc.).
   * @param {EngineNumber} value - The value to set for the stream.
   */
  setTotal(streamName, value) {
    const self = this;
    const strategy = {
      sales: (x) => self.setVolume(x),
      manufacture: (x) => self.setVolume(x),
      import: (x) => self.setVolume(x),
      equipment: (x) => self.setPopulation(x),
      priorEquipment: (x) => self.setPopulation(x),
      consumption: (x) => self.setConsumption(x),
    }[streamName];
    strategy(value);
  }

  /**
   * Set the substance consumption value.
   *
   * @param {EngineNumber} newValue - The new substance consumption value.
   */
  setSubstanceConsumption(newValue) {
    const self = this;
    self._substanceConsumption = newValue;
  }

  /**
   * Get the substance consumption value.
   *
   * @returns {EngineNumber} The substance consumption value.
   */
  getSubstanceConsumption() {
    const self = this;
    if (self._substanceConsumption === null) {
      return self._innerGetter.getSubstanceConsumption();
    } else {
      return self._substanceConsumption;
    }
  }

  /**
   * Set the energy intensity.
   *
   * @param {EngineNumber} newValue - The new energy intensity value.
   */
  setEnergyIntensity(newValue) {
    const self = this;
    self._energyIntensity = newValue;
  }

  /**
   * Get the energy intensity.
   *
   * @returns {EngineNumber} The energy intensity value with units.
   */
  getEnergyIntensity() {
    const self = this;
    if (self._energyIntensity === null) {
      return self._innerGetter.getEnergyIntensity();
    } else {
      return self._energyIntensity;
    }
  }

  /**
   * Set the amortized unit volume.
   *
   * @param {EngineNumber} newValue - The new amortized unit volume.
   */
  setAmortizedUnitVolume(newValue) {
    const self = this;
    self._amortizedUnitVolume = newValue;
  }

  /**
   * Get the amortized unit volume.
   *
   * @returns {EngineNumber} The amortized unit volume.
   */
  getAmortizedUnitVolume() {
    const self = this;
    if (self._amortizedUnitVolume === null) {
      return self._innerGetter.getAmortizedUnitVolume();
    } else {
      return self._amortizedUnitVolume;
    }
  }

  /**
   * Set the population value.
   *
   * @param {EngineNumber} newValue - The new population value.
   */
  setPopulation(newValue) {
    const self = this;
    self._population = newValue;
  }

  /**
   * Get the population value.
   *
   * @returns {EngineNumber} The population value.
   */
  getPopulation() {
    const self = this;
    if (self._population === null) {
      return self._innerGetter.getPopulation();
    } else {
      return self._population;
    }
  }

  /**
   * Set the number of years elapsed.
   *
   * @param {EngineNumber} newValue - The new years elapsed value.
   */
  setYearsElapsed(newValue) {
    const self = this;
    self._yearsElapsed = newValue;
  }

  /**
   * Get the number of years elapsed.
   *
   * @returns {EngineNumber} The years elapsed value.
   */
  getYearsElapsed() {
    const self = this;
    if (self._yearsElapsed === null) {
      return self._innerGetter.getYearsElapsed();
    } else {
      return self._yearsElapsed;
    }
  }

  /**
   * Set the consumption value.
   *
   * @param {EngineNumber} newValue - The new consumption value.
   */
  setConsumption(newValue) {
    const self = this;
    self._totalConsumption = newValue;
  }

  /**
   * Set the energy consumption equivalency.
   *
   * @param {EngineNumber} newValue - The new energy consumption value.
   */
  setEnergyConsumption(newValue) {
    const self = this;
    self._energyConsumption = newValue;
  }

  /**
   * Get the consumption value.
   *
   * @returns {EngineNumber} The consumption value.
   */
  getGhgConsumption() {
    const self = this;
    if (self._totalConsumption === null) {
      return self._innerGetter.getGhgConsumption();
    } else {
      return self._totalConsumption;
    }
  }

  /**
   * Get the energy consumption equivalency.
   *
   * @returns {EngineNumber} The energy consumption value with units.
   */
  getEnergyConsumption() {
    const self = this;
    if (self._energyConsumption === null) {
      return self._innerGetter.getEnergyConsumption();
    } else {
      return self._energyConsumption;
    }
  }

  /**
   * Set the volume value.
   *
   * @param {EngineNumber} newValue - The new volume value.
   */
  setVolume(newValue) {
    const self = this;
    self._volume = newValue;
  }

  /**
   * Get the volume value.
   *
   * @returns {EngineNumber} The volume value.
   */
  getVolume() {
    const self = this;
    if (self._volume === null) {
      return self._innerGetter.getVolume();
    } else {
      return self._volume;
    }
  }

  /**
   * Set the amortized unit consumption.
   *
   * @param {EngineNumber} newValue - The new amortized unit consumption value.
   */
  setAmortizedUnitConsumption(newValue) {
    const self = this;
    self._amortizedUnitConsumption = newValue;
  }

  /**
   * Get the amortized unit consumption.
   *
   * @returns {EngineNumber} The amortized unit consumption value.
   */
  getAmortizedUnitConsumption() {
    const self = this;
    if (self._amortizedUnitConsumption === null) {
      return self._innerGetter.getAmortizedUnitConsumption();
    } else {
      return self._amortizedUnitConsumption;
    }
  }

  /**
   * Set the population change value.
   *
   * Set the population change value, in other words the change between prior
   * and new equipment.
   *
   * @param {EngineNumber} newValue - The new population change value.
   */
  setPopulationChange(newValue) {
    const self = this;
    self._populationChange = newValue;
  }

  /**
   * Get the population change value.
   *
   * Get the population change value, in other words the change between prior
   * and new equipment.
   *
   * @param {UnitConverter} unitConverter - Converter for ensuring consistent
   *     units.
   * @returns {EngineNumber} The population change value.
   */
  getPopulationChange(unitConverter) {
    const self = this;
    if (self._populationChange === null) {
      return self._innerGetter.getPopulationChange(unitConverter);
    } else {
      return self._populationChange;
    }
  }
}

export {ConverterStateGetter, OverridingConverterStateGetter};
