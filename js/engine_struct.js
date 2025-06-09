/**
 * Structures for describing simulation outputs.
 *
 * @license BSD-3-Clause
 */

/**
 * Result of an engine execution for a substance for an application and year.
 */
class EngineResult {
  /**
   * Constructor for creating an EngineResult instance.
   *
   * @param {string} application - The application associated with this engine
   *     result.
   * @param {string} substance - The substance associated with this engine
   *     result.
   * @param {number} year - The year for which the engine result is relevant.
   * @param {EngineNumber} manufactureValue - The value associated with
   *     manufacturing in volume like kg.
   * @param {EngineNumber} importValue - The value related to imports like in
   *     volume like kg.
   * @param {EngineNumber} recycleValue - The value denoting recycled
   *     materials in volume like kg.
   * @param {EngineNumber} domesticConsumptionValue - The domestic consumption
   *     value in tCO2e or equivalent.
   * @param {EngineNumber} importConsumptionValue - The import consumption
   *     value in tCO2e or equivalent.
   * @param {EngineNumber} recycleConsumptionValue - The recycle consumption
   *     value in tCO2e or equivalent.
   * @param {EngineNumber} populationValue - The population value in terms of
   *     equipment.
   * @param {EngineNumber} populationNew - The amount of new equipment added
   *     this year.
   * @param {EngineNumber} rechargeEmissions - The greenhouse gas emissions
   *     from recharge activities.
   * @param {EngineNumber} eolEmissions - The greenhouse gas emissions from
   *     end-of-life equipment.
   */
  constructor(
    application,
    substance,
    year,
    manufactureValue,
    importValue,
    recycleValue,
    domesticConsumptionValue,
    importConsumptionValue,
    recycleConsumptionValue,
    populationValue,
    populationNew,
    rechargeEmissions,
    eolEmissions,
    energyConsumption,
  ) {
    const self = this;
    self._application = application;
    self._substance = substance;
    self._year = year;
    self._manufactureValue = manufactureValue;
    self._importValue = importValue;
    self._recycleValue = recycleValue;
    self._domesticConsumptionValue = domesticConsumptionValue;
    self._importConsumptionValue = importConsumptionValue;
    self._recycleConsumptionValue = recycleConsumptionValue;
    self._populationValue = populationValue;
    self._populationNew = populationNew;
    self._rechargeEmissions = rechargeEmissions;
    self._eolEmissions = eolEmissions;
    self._energyConsumption = energyConsumption;
  }

  /**
   * Get the application.
   *
   * @returns {string} The application.
   */
  getApplication() {
    const self = this;
    return self._application;
  }

  /**
   * Get the substance.
   *
   * @returns {string} The substance.
   */
  getSubstance() {
    const self = this;
    return self._substance;
  }

  /**
   * Get the year the result is relevant to.
   *
   * @returns {number} The year.
   */
  getYear() {
    const self = this;
    return self._year;
  }

  /**
   * Get the manufacture value.
   *
   * @returns {EngineNumber} The manufacture value in volume like kg.
   */
  getManufacture() {
    const self = this;
    return self._manufactureValue;
  }

  /**
   * Get the import value.
   *
   * @returns {EngineNumber} The import value in volume like kg.
   */
  getImport() {
    const self = this;
    return self._importValue;
  }

  /**
   * Get the recycle value.
   *
   * @returns {EngineNumber} The recycle value in volume like kg.
   */
  getRecycle() {
    const self = this;
    return self._recycleValue;
  }

  /**
   * Get the total consumption without recycling.
   *
   * @returns {EngineNumber} The consumption value in tCO2e or similar.
   */
  getConsumptionNoRecycle() {
    const self = this;
    if (self._domesticConsumptionValue.getUnits() !== self._importConsumptionValue.getUnits()) {
      throw "Could not add incompatible units for consumption.";
    }

    return new EngineNumber(
      self._domesticConsumptionValue.getValue() + self._importConsumptionValue.getValue(),
      self._domesticConsumptionValue.getUnits(),
    );
  }

  /**
   * Get the total consumption.
   *
   * @returns {EngineNumber} The consumption value in tCO2e or similar.
   */
  getGhgConsumption() {
    const self = this;

    const noRecycleValue = self.getConsumptionNoRecycle();

    if (self._recycleConsumptionValue.getUnits() !== noRecycleValue.getUnits()) {
      throw "Could not add incompatible units for consumption.";
    }

    return new EngineNumber(
      self._recycleConsumptionValue.getValue() + noRecycleValue.getValue(),
      self._recycleConsumptionValue.getUnits(),
    );
  }

  /**
   * Get the domestic consumption value.
   *
   * @returns {EngineNumber} The domestic consumption value in tCO2e or
   *     similar.
   */
  getDomesticConsumption() {
    const self = this;
    return self._domesticConsumptionValue;
  }

  /**
   * Get the import consumption value.
   *
   * @returns {EngineNumber} The import consumption value.
   */
  getImportConsumption() {
    const self = this;
    return self._importConsumptionValue;
  }

  /**
   * Get the recycle consumption value.
   *
   * @returns {EngineNumber} The recycle consumption value.
   */
  getRecycleConsumption() {
    const self = this;
    return self._recycleConsumptionValue;
  }

  /**
   * Get the population value.
   *
   * @returns {EngineNumber} The population value.
   */
  getPopulation() {
    const self = this;
    return self._populationValue;
  }

  /**
   * Get the amount of new equipment added this year.
   *
   * @returns {EngineNumber} The amount of new equipment this year in units.
   */
  getPopulationNew() {
    const self = this;
    return self._populationNew;
  }

  /**
   * Get the greenhouse gas emissions from recharge activities.
   *
   * @returns {EngineNumber} The recharge emissions value with units.
   */
  getRechargeEmissions() {
    const self = this;
    return self._rechargeEmissions;
  }

  /**
   * Get the greenhouse gas emissions from end-of-life equipment.
   *
   * @returns {EngineNumber} The end-of-life emissions value with units.
   */
  getEolEmissions() {
    const self = this;
    return self._eolEmissions;
  }

  /**
   * Get the energy consumption value.
   *
   * @returns {EngineNumber} The energy consumption value with units.
   */
  getEnergyConsumption() {
    const self = this;
    return self._energyConsumption;
  }
}

/**
 * Description of trade activity within a result.
 *
 * As a supplement to an {EngineResult}, offers additional description of trade
 * activity to support different kinds of trade attributions.
 */
class ImportSummary {

  /**
   * Create a new summary of imports.
   *
   * @param {EngineValue} initialChargeValue - The volume of substance imported
   *     via initial charge on imported equipment (like kg).
   * @param {EngineValue} initialChargeConsumptionValue - The consumption
   *     associated with inital charge of imported equipment (like tCO2e).
   * @param {EngineValue} newPopulation - The number of new units imported.
   */
  constructor(initialChargeValue, initialChargeConsumptionValue, newPopulation) {
    const self = this;
    self._initialChargeValue = initialChargeValue;
    self._initialChargeConsumptionValue = initialChargeConsumptionValue;
    self._newPopulation = newPopulation;
  }
  
  /**
   * Get the volume of substance imported via initial charge on imported equipment.
   *
   * @returns {EngineValue} The initial charge value in volume units like kg.
   */
  getInitialChargeValue() {
    const self = this;
    return self._initialChargeValue;
  }
  
  /**
   * Get the consumption associated with initial charge of imported equipment.
   *
   * @returns {EngineValue} The initial charge consumption value in units like tCO2e.
   */
  getInitialChargeConsumptionValue() {
    const self = this;
    return self._initialChargeConsumptionValue;
  }
  
  /**
   * Get the number of new units imported.
   *
   * @returns {EngineValue} The new population value in units.
   */
  getNewPopulation() {
    const self = this;
    return self._newPopulation;
  }

}

/**
 * Class representing aggregated result which can be visualized.
 */
class AggregatedResult {
  /**
   * Construct an AggregatedResult instance.
   *
   * @param {EngineNumber} manufactureValue - The value representing
   *     manufacturing in volume like kg.
   * @param {EngineNumber} importValue - The value representing imports in
   *     volume like kg.
   * @param {EngineNumber} recycleValue - The value of recycled goods in volume
   *     like kg.
   * @param {EngineNumber} domesticConsumptionValue - The value representing
   *     domestic consumption in tCO2e or similar.
   * @param {EngineNumber} importConsumptionValue - The consumption value due
   *     to imports in tCO2e or similar.
   * @param {EngineNumber} recycleConsumptionValue - The consumption value due
   *     to recycling in tCO2e or similar.
   * @param {EngineNumber} populationValue - The value of the population amount.
   * @param {EngineNumber} populationNew - The value representing new equipment
   *     added in the current year.
   * @param {EngineNumber} rechargeEmissions - Emissions resulting from recharge
   *     activities.
   * @param {EngineNumber} eolEmissions - Emissions resulting from end-of-life
   *     equipment.
   * @param {EngineNumber} energyConsumtion - Equivalent energy consumption for
   *     activity specified.
   */
  constructor(
    manufactureValue,
    importValue,
    recycleValue,
    domesticConsumptionValue,
    importConsumptionValue,
    recycleConsumptionValue,
    populationValue,
    populationNew,
    rechargeEmissions,
    eolEmissions,
    energyConsumption,
  ) {
    const self = this;
    self._manufactureValue = manufactureValue;
    self._importValue = importValue;
    self._recycleValue = recycleValue;
    self._domesticConsumptionValue = domesticConsumptionValue;
    self._importConsumptionValue = importConsumptionValue;
    self._recycleConsumptionValue = recycleConsumptionValue;
    self._populationValue = populationValue;
    self._populationNew = populationNew;
    self._rechargeEmissions = rechargeEmissions;
    self._eolEmissions = eolEmissions;
    self._energyConsumption = energyConsumption;
  }

  /**
   * Get the energy consumption value.
   *
   * @returns {EngineNumber} The energy consumption value with units.
   */
  getEnergyConsumption() {
    const self = this;
    return self._energyConsumption;
  }

  /**
   * Get the manufacture (as opposed to import) of substance.
   *
   * @returns {EngineNumber} The manufacture value with units like kg.
   */
  getManufacture() {
    const self = this;
    return self._manufactureValue;
  }

  /**
   * Get the import (as opposed to manufacture) value.
   *
   * @returns {EngineNumber} The import value with units like kg.
   */
  getImport() {
    const self = this;
    return self._importValue;
  }

  /**
   * Get the recycle sales.
   *
   * @returns {EngineNumber} The recycle sales with units like kg.
   */
  getRecycle() {
    const self = this;
    return self._recycleValue;
  }

  /**
   * Get combined sales value (manufacture + import).
   *
   * @returns {EngineNumber} The combined sales value with units like kg.
   */
  getSales() {
    const self = this;
    const manufactureValue = self.getManufacture();
    const importValue = self.getImport();
    const recycleValue = self.getRecycle();
    const noRecycle = self._combineUnitValue(manufactureValue, importValue);
    const sales = self._combineUnitValue(noRecycle, recycleValue);
    return sales;
  }

  /**
   * Get the domestic consumption value.
   *
   * @returns {EngineNumber} The domestic consumption value with units like
   *     tCO2e.
   */
  getDomesticConsumption() {
    const self = this;
    return self._domesticConsumptionValue;
  }

  /**
   * Get the import consumption value.
   *
   * @returns {EngineNumber} The import consumption value.
   */
  getImportConsumption() {
    const self = this;
    return self._importConsumptionValue;
  }

  /**
   * Get the recycle consumption.
   *
   * @returns {EngineNumber} The recycle consumption with units like tCO2e.
   */
  getRecycleConsumption() {
    const self = this;
    return self._recycleConsumptionValue;
  }

  /**
   * Get the total consumption combining domestic and import.
   *
   * @returns {EngineNumber} The combined consumption value with units like
   *     tCO2e.
   */
  getGhgConsumption() {
    const self = this;
    const noRecycle = self._combineUnitValue(
      self.getDomesticConsumption(),
      self.getImportConsumption(),
    );
    return self._combineUnitValue(noRecycle, self.getRecycleConsumption());
  }

  /**
   * Get the population (amount of equipment) value.
   *
   * @returns {EngineNumber} The population value with units like tCO2e.
   */
  getPopulation() {
    const self = this;
    return self._populationValue;
  }

  /**
   * Get the new equipment added in this year.
   *
   * @returns {EngineNumber} The new equipment added in units like tCO2e.
   */
  getPopulationNew() {
    const self = this;
    return self._populationNew;
  }

  /**
   * Get the greenhouse gas emissions from recharge activities.
   *
   * @returns {EngineNumber} The recharge emissions value with units like
   *     tCO2e.
   */
  getRechargeEmissions() {
    const self = this;
    return self._rechargeEmissions;
  }

  /**
   * Get the greenhouse gas emissions from end-of-life equipment.
   *
   * @returns {EngineNumber} The end-of-life emissions value with units like
   *     tCO2e.
   */
  getEolEmissions() {
    const self = this;
    return self._eolEmissions;
  }

  /**
   * Get the total greenhouse gas emissions combining recharge and end-of-life emissions.
   *
   * @returns {EngineNumber} The combined emissions value with units like tCO2e.
   */
  getTotalEmissions() {
    const self = this;
    return self._combineUnitValue(self.getRechargeEmissions(), self.getEolEmissions());
  }

  /**
   * Combine this result with another result.
   *
   * Combine this result with another result in an additive way with unit
   * standardization and conversion.
   *
   * @param {AggregatedResult} other - The other result to combine with.
   * @returns {AggregatedResult} A new combined result.
   */
  combine(other) {
    const self = this;

    const manufactureValue = self._combineUnitValue(self.getManufacture(), other.getManufacture());
    const importValue = self._combineUnitValue(self.getImport(), other.getImport());
    const recycleValue = self._combineUnitValue(self.getRecycle(), other.getRecycle());
    const domesticConsumptionValue = self._combineUnitValue(
      self.getDomesticConsumption(),
      other.getDomesticConsumption(),
    );
    const importConsumptionValue = self._combineUnitValue(
      self.getImportConsumption(),
      other.getImportConsumption(),
    );
    const recycleConsumptionValue = self._combineUnitValue(
      self.getRecycleConsumption(),
      other.getRecycleConsumption(),
    );
    const populationValue = self._combineUnitValue(self.getPopulation(), other.getPopulation());
    const populationNew = self._combineUnitValue(self.getPopulationNew(), other.getPopulationNew());

    const rechargeEmissions = self._combineUnitValue(
      self.getRechargeEmissions(),
      other.getRechargeEmissions(),
    );
    const eolEmissions = self._combineUnitValue(self.getEolEmissions(), other.getEolEmissions());
    const energyConsumption = self._combineUnitValue(
      self.getEnergyConsumption(),
      other.getEnergyConsumption(),
    );

    return new AggregatedResult(
      manufactureValue,
      importValue,
      recycleValue,
      domesticConsumptionValue,
      importConsumptionValue,
      recycleConsumptionValue,
      populationValue,
      populationNew,
      rechargeEmissions,
      eolEmissions,
      energyConsumption,
    );
  }

  /**
   * Combine two unit values with the same units.
   *
   * @private
   * @param {EngineNumber} a - First value.
   * @param {EngineNumber} b - Second value.
   * @returns {EngineNumber} Combined value.
   * @throws {string} If units don't match.
   */
  _combineUnitValue(a, b) {
    const self = this;
    if (a.getUnits() !== b.getUnits()) {
      throw "Encountered different units during aggregation.";
    }
    return new EngineNumber(a.getValue() + b.getValue(), a.getUnits());
  }
}
