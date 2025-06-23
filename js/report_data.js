/**
 * Data structures for report and visualization functionality.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";
import {AggregatedResult, AttributeToExporterResult} from "engine_struct";

/**
 * Builder class for creating metric computation strategies.
 *
 * Builder which handles the construction of metric processing pipelines
 * including transformations and unit conversions.
 */
class MetricStrategyBuilder {
  /**
   * Create a new MetricStrategyBuilder instance.
   *
   * Create a new MetricStrategyBuilder instance, initalizing all strategy
   * components to null requiring them to be specified later.
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
   *
   * @param {string} metric - The metric name (e.g., 'sales', 'emissions').
   */
  setMetric(metric) {
    const self = this;
    self._metric = metric;
  }

  /**
   * Set the submetric name for the strategy.
   *
   * @param {string} submetric - The submetric name (e.g., 'all', 'import').
   */
  setSubmetric(submetric) {
    const self = this;
    self._submetric = submetric;
  }

  /**
   * Set the units for the strategy output.
   *
   * @param {string} units - The units specification (e.g., 'MtCO2e / yr').
   */
  setUnits(units) {
    const self = this;
    self._units = units;
  }

  /**
   * Set the core computation strategy.
   *
   * @param {Function} strategy - The function that implements the core metric
   *     computation.
   */
  setStrategy(strategy) {
    const self = this;
    self._strategy = strategy;
  }

  /**
   * Set the transformation to apply after the core strategy.
   *
   * @param {Function} transformation - The function that transforms the strategy
   *     output.
   */
  setTransformation(transformation) {
    const self = this;
    self._transformation = transformation;
  }

  /**
   * Add the configured strategy to the strategies collection.
   *
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
   * @param {Array<EngineResult>} innerData - The raw report data to wrap.
   */
  constructor(innerData) {
    const self = this;
    self._innerData = innerData;
    self._innerDataExporterAttributed = null;

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

        strategyBuilder.setStrategy((x) => self.getEnergyConsumption(x));

        const getKwhYr = (value) => {
          if (value.getUnits() !== "kwh") {
            throw "Unexpected energy units: " + value.getUnits();
          }
          return new EngineNumber(value.getValue(), "kwh / yr");
        };

        strategyBuilder.setUnits("kwh / yr");
        strategyBuilder.setTransformation(getKwhYr);
        strategyBuilder.add();

        strategyBuilder.setUnits("mwh / yr");
        strategyBuilder.setTransformation((value) => {
          const kwhValue = getKwhYr(value);
          return new EngineNumber(kwhValue.getValue() / 1000, "mwh");
        });
        strategyBuilder.add();

        strategyBuilder.setUnits("gwh / yr");
        strategyBuilder.setTransformation((value) => {
          const kwhValue = getKwhYr(value);
          return new EngineNumber(kwhValue.getValue() / 1000000, "gwh");
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
      strategyBuilder.setStrategy((x) => self.getGhgConsumption(x));
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
   * @param {FilterSet} filterSet - Filter set with attribution settings to
   *     apply.
   * @returns {Array<EngineResult>} The raw data.
   */
  getRawData(filterSet) {
    const self = this;
    if (filterSet.getAttributeImporter()) {
      return self._innerData;
    } else {
      if (self._innerDataExporterAttributed === null) {
        self._innerDataExporterAttributed = self._buildExporterAttributed(self._innerData);
      }
      return self._innerDataExporterAttributed;
    }
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
    if (filterSet.getScenario() === null) {
      return new Set(self.getRawData(filterSet).map((x) => x.getScenarioName()));
    } else {
      return new Set([filterSet.getScenario()]);
    }
  }

  /**
   * Get the name of the first scenario available without applying any filter.
   *
   * @param {FilterSet} filterSet - The filter set indicating preprocessing
   *    options even though a filter is not applied.
   * @returns {string|null} The first scenario name or null if no scenarios
   *    present.
   */
  getFirstScenario(filterSet) {
    const self = this;
    const scenarios = self.getScenarios(filterSet);
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
  getGhgConsumption(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getGhgConsumption();
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

    const stepWithSubapp = (target, filterVal, getter) => {
      if (filterVal === null) {
        return target;
      }

      const isMetaGroup = filterVal.endsWith(" - All");
      const withAllReplace = filterVal.replaceAll(" - All", " - ");

      return target.filter((record) => {
        const candidateVal = getter(record);

        if (isMetaGroup) {
          return candidateVal.startsWith(withAllReplace);
        } else {
          return candidateVal === filterVal;
        }
      });
    };

    const allRecords = self.getRawData(filterSet);

    const scenario = filterSet.getScenario();
    const withScenario = step(allRecords, scenario, (x) => x.getScenarioName());

    const year = filterSet.getYear();
    const withYear = step(withScenario, year, (x) => x.getYear());

    const app = filterSet.getApplication();
    const withApp = stepWithSubapp(withYear, app, (x) => x.getApplication());

    const sub = filterSet.getSubstance();
    const withSub = step(withApp, sub, (x) => x.getSubstance());

    return withSub;
  }

  /**
   * Decorate the inner data (EngineResult) to attribute to exporter.
   *
   * @private
   * @param {Array<EngineResult>} rawResults - The results with attribute
   *     to importer that should be decorated.
   * @returns {Array<AttributeToExporterResult>} Decorated version.
   */
  _buildExporterAttributed(rawResults) {
    const self = this;
    return rawResults.map((x) => new AttributeToExporterResult(x));
  }
}

export {ReportDataWrapper};
