/**
 * Data structures for report and visualization functionality.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";

/**
 * Class representing aggregated result which can be visualized.
 */
class AggregatedResult {
  /**
   * Construct an AggregatedResult instance.
   *
   * @param {EngineNumber} manufactureValue - The value representing
   *     manufacturing.
   * @param {EngineNumber} importValue - The value representing imports.
   * @param {EngineNumber} recycleValue - The value of recycled goods.
   * @param {EngineNumber} domesticConsumptionValue - The value representing
   *     domestic consumption.
   * @param {EngineNumber} importConsumptionValue - The consumption value due
   *     to imports.
   * @param {EngineNumber} recycleConsumptionValue - The consumption value due
   *     to recycling.
   * @param {EngineNumber} populationValue - The value of the population amount.
   * @param {EngineNumber} populationNew - The value representing new equipment
   *     added in the current year.
   * @param {EngineNumber} rechargeEmissions - Emissions resulting from recharge
   *     activities.
   * @param {EngineNumber} eolEmissions - Emissions resulting from end-of-life
   *     equipment.
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
   * @returns {EngineNumber} The manufacture value with units.
   */
  getManufacture() {
    const self = this;
    return self._manufactureValue;
  }

  /**
   * Get the import (as opposed to manufacture) value.
   *
   * @returns {EngineNumber} The import value with units.
   */
  getImport() {
    const self = this;
    return self._importValue;
  }

  /**
   * Get the recycle sales.
   *
   * @returns {EngineNumber} The recycle sales with units.
   */
  getRecycle() {
    const self = this;
    return self._recycleValue;
  }

  /**
   * Get combined sales value (manufacture + import).
   *
   * @returns {EngineNumber} The combined sales value with units.
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
   * @returns {EngineNumber} The domestic consumption value.
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
   * @returns {EngineNumber} The recycle consumption with units.
   */
  getRecycleConsumption() {
    const self = this;
    return self._recycleConsumptionValue;
  }

  /**
   * Get the total consumption combining domestic and import.
   *
   * @returns {EngineNumber} The combined consumption value with units.
   */
  getConsumption() {
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
   * @returns {EngineNumber} The population value with units.
   */
  getPopulation() {
    const self = this;
    return self._populationValue;
  }

  /**
   * Get the new equipment added in this year.
   *
   * @returns {EngineNumber} The new equipment added in units.
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
   * Get the total greenhouse gas emissions combining recharge and end-of-life emissions.
   *
   * @returns {EngineNumber} The combined emissions value with units.
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

/**
 * Builder class for creating metric computation strategies.
 * Handles the construction of metric processing pipelines including
 * transformations and unit conversions.
 */
class MetricStrategyBuilder {
  /**
   * Create a new MetricStrategyBuilder instance.
   * Initializes all strategy components to null.
   */
  constructor() {
    const self = this;
    self._strategies = {};
    self._metric = null;
    self._submetric = null;
    self._units = null;
    self._strategy = null;
    self._transformation = null;
  }

  /**
   * Set the metric name for the strategy.
   * @param {string} metric - The metric name (e.g., 'sales', 'emissions').
   */
  setMetric(metric) {
    const self = this;
    self._metric = metric;
  }

  /**
   * Set the submetric name for the strategy.
   * @param {string} submetric - The submetric name (e.g., 'all', 'import').
   */
  setSubmetric(submetric) {
    const self = this;
    self._submetric = submetric;
  }

  /**
   * Set the units for the strategy output.
   * @param {string} units - The units specification (e.g., 'MtCO2e / yr').
   */
  setUnits(units) {
    const self = this;
    self._units = units;
  }

  /**
   * Set the core computation strategy.
   * @param {Function} strategy - The function that implements the core metric computation.
   */
  setStrategy(strategy) {
    const self = this;
    self._strategy = strategy;
  }

  /**
   * Set the transformation to apply after the core strategy.
   * @param {Function} transformation - The function that transforms the strategy output.
   */
  setTransformation(transformation) {
    const self = this;
    self._transformation = transformation;
  }

  /**
   * Add the configured strategy to the strategies collection.
   * Requires all components to be set (non-null).
   * @throws {string} If any required component is null.
   */
  add() {
    const self = this;
    self._requireCompleteDefinition();

    const fullNamePieces = [self._metric, self._submetric, self._units];
    const fullName = fullNamePieces.join(":");

    const innerStrategy = self._strategy;
    const innerTransformation = self._transformation;
    const execute = (filterSet) => {
      const result = innerStrategy(filterSet);

      if (result === null) {
        return null;
      }

      const transformed = innerTransformation(result);
      return transformed;
    };

    self._strategies[fullName] = execute;
  }

  /**
   * Build and return the complete strategies object.
   * @returns {Object} The strategies object containing all added strategies.
   */
  build() {
    const self = this;
    return self._strategies;
  }

  /**
   * Verify that all required components have been set.
   * @private
   * @throws {string} If any required component is null.
   */
  _requireCompleteDefinition() {
    const self = this;
    const pieces = [
      self._metric,
      self._submetric,
      self._units,
      self._strategy,
      self._transformation,
    ];
    const nullPieces = pieces.filter((x) => x === null);
    const numNullPieces = nullPieces.map((x) => 1).reduce((a, b) => a + b, 0);
    if (numNullPieces > 0) {
      throw "Encountered null values on MetricStrategyBuilder";
    }
  }
}

/**
 * Facade which simplifies access to engine outputs.
 *
 * Wrapper class for report data that provides filtering and aggregation
 * capabilities over simplified engine outputs.
 */
class ReportDataWrapper {
  /**
   * Create a new report data wrapper.
   *
   * @param {Object} innerData - The raw report data to wrap.
   */
  constructor(innerData) {
    const self = this;
    self._innerData = innerData;

    const strategyBuilder = new MetricStrategyBuilder();

    const addEmissionsConversion = (strategyBuilder) => {
      strategyBuilder.setUnits("tCO2e / yr");
      strategyBuilder.setTransformation((val) => {
        if (val.getUnits() !== "tCO2e") {
          throw "Unexpected emissions source units: " + val.getUnits();
        }

        return new EngineNumber(val.getValue(), "tCO2e / yr");
      });
      strategyBuilder.add();

      strategyBuilder.setUnits("ktCO2e / yr");
      strategyBuilder.setTransformation((val) => {
        if (val.getUnits() !== "tCO2e") {
          throw "Unexpected emissions source units: " + val.getUnits();
        }

        return new EngineNumber(val.getValue() / 1000, "ktCO2e / yr");
      });
      strategyBuilder.add();

      strategyBuilder.setUnits("MtCO2e / yr");
      strategyBuilder.setTransformation((val) => {
        if (val.getUnits() !== "tCO2e") {
          throw "Unexpected emissions source units: " + val.getUnits();
        }

        return new EngineNumber(val.getValue() / 1000000, "MtCO2e / yr");
      });
      strategyBuilder.add();
    };

    const addEmissionsStrategies = (strategyBuilder) => {
      strategyBuilder.setMetric("emissions");

      strategyBuilder.setSubmetric("all");
      strategyBuilder.setStrategy((x) => self.getTotalEmissions(x));
      addEmissionsConversion(strategyBuilder);

      strategyBuilder.setSubmetric("recharge");
      strategyBuilder.setStrategy((x) => self.getRechargeEmissions(x));
      addEmissionsConversion(strategyBuilder);

      strategyBuilder.setSubmetric("eol");
      strategyBuilder.setStrategy((x) => self.getEolEmissions(x));
      addEmissionsConversion(strategyBuilder);
    };

    const addSalesStrategies = (strategyBuilder) => {
      strategyBuilder.setMetric("sales");

      const makeForKgAndMt = (strategyBuilder) => {
        strategyBuilder.setUnits("mt / yr");
        strategyBuilder.setTransformation((value) => {
          if (value.getUnits() !== "kg") {
            throw "Unexpected sales units: " + value.getUnits();
          }

          return new EngineNumber(value.getValue() / 1000, "mt / yr");
        });
        strategyBuilder.add();

        strategyBuilder.setUnits("kg / yr");
        strategyBuilder.setTransformation((value) => {
          if (value.getUnits() !== "kg") {
            throw "Unexpected sales units: " + value.getUnits();
          }

          return new EngineNumber(value.getValue(), "kg / yr");
        });
        strategyBuilder.add();

        strategyBuilder.setUnits("kwh / yr");
        strategyBuilder.setStrategy((x) => self.getEnergyConsumption(x));
        strategyBuilder.setTransformation((value) => {
          if (value.getUnits() !== "kwh") {
            throw "Unexpected energy units: " + value.getUnits();
          }
          return new EngineNumber(value.getValue(), "kwh / yr");
        });
        strategyBuilder.add();
      };

      strategyBuilder.setSubmetric("all");
      strategyBuilder.setStrategy((x) => self.getSales(x));
      makeForKgAndMt(strategyBuilder);

      strategyBuilder.setSubmetric("import");
      strategyBuilder.setStrategy((x) => self.getImport(x));
      makeForKgAndMt(strategyBuilder);

      strategyBuilder.setSubmetric("manufacture");
      strategyBuilder.setStrategy((x) => self.getManufacture(x));
      makeForKgAndMt(strategyBuilder);

      strategyBuilder.setSubmetric("recycle");
      strategyBuilder.setStrategy((x) => self.getRecycle(x));
      makeForKgAndMt(strategyBuilder);
    };

    const addConsumptionStrategies = (strategyBuilder) => {
      strategyBuilder.setMetric("sales");

      strategyBuilder.setSubmetric("all");
      strategyBuilder.setStrategy((x) => self.getConsumption(x));
      addEmissionsConversion(strategyBuilder);

      strategyBuilder.setSubmetric("import");
      strategyBuilder.setStrategy((x) => self.getImportConsumption(x));
      addEmissionsConversion(strategyBuilder);

      strategyBuilder.setSubmetric("manufacture");
      strategyBuilder.setStrategy((x) => self.getDomesticConsumption(x));
      addEmissionsConversion(strategyBuilder);

      strategyBuilder.setSubmetric("recycle");
      strategyBuilder.setStrategy((x) => self.getRecycleConsumption(x));
      addEmissionsConversion(strategyBuilder);
    };

    const addPopulationStrategies = (strategyBuilder) => {
      const makeForThousandAndMillion = (strategyBuilder) => {
        strategyBuilder.setUnits("units");
        strategyBuilder.setTransformation((value) => {
          if (value.getUnits() !== "units") {
            throw "Unexpected population units: " + value.getUnits();
          }
          return value;
        });
        strategyBuilder.add();

        strategyBuilder.setUnits("thousand units");
        strategyBuilder.setTransformation((value) => {
          if (value.getUnits() !== "units") {
            throw "Unexpected population units: " + value.getUnits();
          }
          return new EngineNumber(value.getValue() / 1000, "thousands of units");
        });
        strategyBuilder.add();

        strategyBuilder.setUnits("million units");
        strategyBuilder.setTransformation((value) => {
          if (value.getUnits() !== "units") {
            throw "Unexpected population units: " + value.getUnits();
          }
          return new EngineNumber(value.getValue() / 1000000, "millions of units");
        });
        strategyBuilder.add();
      };

      strategyBuilder.setMetric("population");

      strategyBuilder.setSubmetric("all");
      strategyBuilder.setStrategy((x) => self.getPopulation(x));
      makeForThousandAndMillion(strategyBuilder);

      strategyBuilder.setSubmetric("new");
      strategyBuilder.setStrategy((x) => self.getPopulationNew(x));
      makeForThousandAndMillion(strategyBuilder);
    };

    addEmissionsStrategies(strategyBuilder);
    addSalesStrategies(strategyBuilder);
    addConsumptionStrategies(strategyBuilder);
    addPopulationStrategies(strategyBuilder);

    self._metricStrategies = strategyBuilder.build();
  }

  /**
   * Get the raw underlying data.
   *
   * @returns {Object} The raw data.
   */
  getRawData() {
    const self = this;
    return self._innerData;
  }

  /**
   * Get metric value based on filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber} The filtered metric value.
   */
  getMetric(filterSet) {
    const self = this;
    const metric = filterSet.getFullMetricName();
    const metricStrategy = self._metricStrategies[metric];
    const value = metricStrategy(filterSet);
    return value;
  }

  /**
   * Get dimension values based on a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {Set<*>} Set of dimension values.
   */
  getDimensionValues(filterSet) {
    const self = this;
    const dimension = filterSet.getDimension();
    const strategy = {
      simulations: () => self.getScenarios(filterSet),
      applications: () => self.getApplications(filterSet),
      substances: () => self.getSubstances(filterSet),
    }[dimension];
    const value = strategy();
    return value;
  }

  /**
   * Get scenarios matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {Set<string>} Set of scenario names.
   */
  getScenarios(filterSet) {
    const self = this;
    if (filterSet === undefined || filterSet.getScenario() === null) {
      return new Set(self._innerData.map((x) => x.getName()));
    } else {
      return new Set([filterSet.getScenario()]);
    }
  }

  /**
   * Get the name of the first scenario available  without applying any filter.
   *
   * @returns {string|null} The first scenario name or null if no scenarios
   *     present.
   */
  getFirstScenario() {
    const self = this;
    const scenarios = self.getScenarios(undefined);
    for (const scenario of scenarios) {
      return scenario;
    }
    return null;
  }

  /**
   * Get applications matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {Set<string>} Set of application names.
   */
  getApplications(filterSet) {
    const self = this;
    return self._getSetAfterFilter(filterSet, (x) => x.getApplication());
  }

  /**
   * Get substances matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {Set<string>} Set of substance names.
   */
  getSubstances(filterSet) {
    const self = this;
    return self._getSetAfterFilter(filterSet, (x) => x.getSubstance());
  }

  /**
   * Get years matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {Set<number>} Set of years.
   */
  getYears(filterSet) {
    const self = this;
    return self._getSetAfterFilter(filterSet, (x) => x.getYear());
  }

  /**
   * Get consumption value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The consumption value, or null if no matching
   *     results.
   */
  getConsumption(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getConsumption();
  }

  /**
   * Get the domestic consumption value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The domestic consumption value, or null if no
   *     matching results.
   */
  getDomesticConsumption(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getDomesticConsumption();
  }

  /**
   * Get the import consumption value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The import consumption value, or null if no
   *     matching results.
   */
  getImportConsumption(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getImportConsumption();
  }

  /**
   * Get the recycled consumption value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The recycled consumption value, or null if no
   *     matching results.
   */
  getRecycleConsumption(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getRecycleConsumption();
  }

  /**
   * Get total emissions value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The total emissions value, or null if no matching
   *     results.
   */
  getTotalEmissions(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getTotalEmissions();
  }

  /**
   * Get recharge emissions value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The recharge emissions value, or null if no
   *     matching results.
   */
  getRechargeEmissions(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getRechargeEmissions();
  }

  /**
   * Get end-of-life emissions value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The end-of-life emissions value, or null if
   *     no matching results.
   */
  getEolEmissions(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getEolEmissions();
  }

  /**
   * Get sales value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The sales value, or null if no matching
   *     results.
   */
  getSales(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getSales();
  }

  /**
   * Get sales from imports matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The imports component of sales, or null if no
   *     matching results.
   */
  getImport(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getImport();
  }

  /**
   * Get sales from domestic manufacture matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The domestic manufacture component of sales,
   *     or null if no matching results.
   */
  getManufacture(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getManufacture();
  }

  /**
   * Get the recycled sales value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The recycled sales value, or null if no
   *     matching results.
   */
  getRecycle(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getRecycle();
  }

  /**
   * Get population value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The population value, or null if no matching
   *     results.
   */
  getPopulation(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getPopulation();
  }

  /**
   * Get the amount of new equipment added.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The new equipment added, or null if no matching
   *     results.
   */
  getPopulationNew(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getPopulationNew();
  }

  /**
   * Get energy consumption value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The energy consumption value, or null if no
   *     matching results.
   */
  getEnergyConsumption(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getEnergyConsumption();
  }

  /**
   * Get flattened array of all results.
   *
   * @private
   * @returns {Array<*>} Flattened results array
   */
  _getFlatResults() {
    const self = this;
    return self._innerData.map((x) => x.getTrialResults()).flat();
  }

  /**
   * Get filtered set of values using a getter function.
   *
   * @private
   * @param {FilterSet} filterSet - The filter criteria.
   * @param {Function} getter - Function to get values from filtered results.
   * @returns {Set<*>} Set of filtered values.
   */
  _getSetAfterFilter(filterSet, getter) {
    const self = this;
    const afterFilter = self._applyFilterSet(filterSet);
    const values = afterFilter.map(getter);
    return new Set(values);
  }

  /**
   * Get aggregated result after applying filters.
   *
   * @private
   * @param {FilterSet} filterSet - The filter criteria.
   * @returns {AggregatedResult|null} Aggregated result or null if no matches.
   */
  _getAggregatedAfterFilter(filterSet) {
    const self = this;
    const afterFilter = self._applyFilterSet(filterSet);
    const preAggregated = afterFilter.map(
      (x) =>
        new AggregatedResult(
          x.getManufacture(),
          x.getImport(),
          x.getRecycle(),
          x.getDomesticConsumption(),
          x.getImportConsumption(),
          x.getRecycleConsumption(),
          x.getPopulation(),
          x.getPopulationNew(),
          x.getRechargeEmissions(),
          x.getEolEmissions(),
          x.getEnergyConsumption(),
        ),
    );

    if (preAggregated.length == 0) {
      return null;
    }

    const aggregated = preAggregated.reduce((a, b) => a.combine(b));

    return aggregated;
  }

  /**
   * Apply filter set to get matching results.
   *
   * @private
   * @param {FilterSet} filterSet - The filter criteria.
   * @returns {Array<*>} Array of matching results.
   */
  _applyFilterSet(filterSet) {
    const self = this;

    const step = (target, filterVal, getter) => {
      if (filterVal === null) {
        return target;
      }

      return target.filter((record) => {
        const candidateVal = getter(record);
        return candidateVal === filterVal;
      });
    };

    const allRecords = self._innerData;

    const scenario = filterSet.getScenario();
    const scenarios = step(allRecords, scenario, (x) => x.getName());

    const trials = scenarios.map((x) => x.getTrialResults());
    const trialsFlat = trials.flat(3);

    const year = filterSet.getYear();
    const withYear = step(trialsFlat, year, (x) => x.getYear());

    const app = filterSet.getApplication();
    const withApp = step(withYear, app, (x) => x.getApplication());

    const sub = filterSet.getSubstance();
    const withSub = step(withApp, sub, (x) => x.getSubstance());

    return withSub;
  }
}

/**
 * Filters to apply in creating a visualization.
 *
 * Class representing a set of filters to apply in identifying a subset of data
 * to visualize.
 */
class FilterSet {
  /**
   * Create a new filter set.
   *
   * @param {number|null} year - Year for which data will be filtered.
   * @param {string|null} scenario - Name of scenario for which to filter.
   * @param {string|null} application - Name of application for which to
   *     filter.
   * @param {string|null} substance - Name of substance which to filter.
   * @param {string|null} metric - Metric name for which to display. Note
   *     that this is the full metric names like sales or sales:import.
   * @param {string|null} dimension - Dimension type for which to filter.
   */
  constructor(year, scenario, application, substance, metric, dimension) {
    const self = this;
    self._year = year;
    self._scenario = scenario;
    self._application = application;
    self._substance = substance;
    self._metric = metric;
    self._dimension = dimension;
  }

  /**
   * Get a new filter set with updated dimension value.
   *
   * @param {*} value - The new dimension value (simulations, applications,
   *     substances).
   * @returns {FilterSet} New filter set with updated dimension.
   */
  getWithDimensionValue(value) {
    const self = this;
    const strategy = {
      simulations: (x) => self.getWithScenario(x),
      applications: (x) => self.getWithApplication(x),
      substances: (x) => self.getWithSubstance(x),
    }[self.getDimension()];
    return strategy(value);
  }

  /**
   * Get the year filter.
   *
   * @returns {number|null} The year for which to filter like 10.
   */
  getYear() {
    const self = this;
    return self._year;
  }

  /**
   * Get a new filter set with updated year.
   *
   * @param {number} newYear - The new year value.
   * @returns {FilterSet} New filter set with updated year.
   */
  getWithYear(newYear) {
    const self = this;
    return new FilterSet(
      newYear,
      self._scenario,
      self._application,
      self._substance,
      self._metric,
      self._dimension,
    );
  }

  /**
   * Get the scenario filter.
   *
   * @returns {string|null} The scenario for which to filter like Business as
   *     Usual.
   */
  getScenario() {
    const self = this;
    return self._scenario;
  }

  /**
   * Get a new filter set with updated scenario.
   *
   * @param {string} newScenario - The new scenario value.
   * @returns {FilterSet} New filter set with updated scenario.
   */
  getWithScenario(newScenario) {
    const self = this;
    return new FilterSet(
      self._year,
      newScenario,
      self._application,
      self._substance,
      self._metric,
      self._dimension,
    );
  }

  /**
   * Get the application filter.
   *
   * @returns {string|null} The application for which to filter like commercial
   *     refrigerant.
   */
  getApplication() {
    const self = this;
    return self._application;
  }

  /**
   * Get a new filter set with updated application.
   *
   * @param {string} newApplication - The new application value.
   * @returns {FilterSet} New filter set with updated application.
   */
  getWithApplication(newApplication) {
    const self = this;
    return new FilterSet(
      self._year,
      self._scenario,
      newApplication,
      self._substance,
      self._metric,
      self._dimension,
    );
  }

  /**
   * Get the substance filter.
   *
   * @returns {string|null} The substance for which to filter like HFC-134a.
   */
  getSubstance() {
    const self = this;
    return self._substance;
  }

  /**
   * Get a new filter set with updated substance.
   *
   * @param {string} newSubstance - The new substance value.
   * @returns {FilterSet} New filter set with updated substance.
   */
  getWithSubstance(newSubstance) {
    const self = this;
    return new FilterSet(
      self._year,
      self._scenario,
      self._application,
      newSubstance,
      self._metric,
      self._dimension,
    );
  }

  /**
   * Get the full name of the metric to display.
   *
   * @returns {string|null} The metric to display like sales. Note that this is
   *     the full name like sales:manufacture or sales:all.
   */
  getFullMetricName() {
    const self = this;
    return self._metric;
  }

  /**
   * Get the type of metric to display like sales.
   *
   * @returns {string|null} The metric family to display like sales.
   */
  getMetric() {
    const self = this;

    if (self._metric === null) {
      return null;
    }

    return self._metric.split(":")[0];
  }

  /**
   * Get the substream of the metric to dsiplay.
   *
   * @returns {string|null} The submetric to display like import or null if no
   *     submetric.
   */
  getSubMetric() {
    const self = this;

    if (self._metric === null) {
      return null;
    }

    const metricPieces = self._metric.split(":");
    return metricPieces.length < 2 ? null : metricPieces[1];
  }

  /**
   * Get the units of the metric to dsiplay.
   *
   * @returns {string|null} The units desired or null if not given.
   */
  getUnits() {
    const self = this;

    if (self._metric === null) {
      return null;
    }

    const metricPieces = self._metric.split(":");
    return metricPieces.length < 3 ? null : metricPieces[2];
  }

  /**
   * Get a new filter set with updated metric.
   *
   * @param {string} newMetric - The new metric value.
   * @returns {FilterSet} New filter set with updated metric.
   */
  getWithMetric(newMetric) {
    const self = this;
    return new FilterSet(
      self._year,
      self._scenario,
      self._application,
      self._substance,
      newMetric,
      self._dimension,
    );
  }

  /**
   * Get the dimension filter.
   *
   * @returns {string|null} The dimension for which to filter like simulations.
   */
  getDimension() {
    const self = this;
    return self._dimension;
  }

  /**
   * Get a new filter set with updated dimension.
   *
   * @param {string} newDimension - The new dimension value.
   * @returns {FilterSet} New filter set with updated dimension.
   */
  getWithDimension(newDimension) {
    const self = this;
    return new FilterSet(
      self._year,
      self._scenario,
      self._application,
      self._substance,
      self._metric,
      newDimension,
    );
  }

  /**
   * Check if there is a single scenario selected or available.
   *
   * @param {Set<string>} scenarios - Set of available scenarios.
   * @returns {boolean} True if single scenario.
   */
  hasSingleScenario(scenarios) {
    const self = this;
    const scenarioSelected = self._scenario !== null;
    const onlyOneScenario = scenarios.size == 1;
    return scenarioSelected || onlyOneScenario;
  }
}

export {ReportDataWrapper, FilterSet};
