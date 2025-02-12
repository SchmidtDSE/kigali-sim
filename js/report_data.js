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
   * Create a new aggregated result.
   *
   * @param {EngineNumber} manufactureValue - The manufacturing value.
   * @param {EngineNumber} importValue - The import value.
   * @param {EngineNumber} consumptionValue - The consumption value.
   * @param {EngineNumber} populationValue - The equipment population value.
   * @param {EngineNumber} populationNew - The new equipment added this year.
   */
  constructor(
    manufactureValue,
    importValue,
    consumptionValue,
    populationValue,
    populationNew,
    rechargeEmissions,
    eolEmissions,
  ) {
    const self = this;
    self._manufactureValue = manufactureValue;
    self._importValue = importValue;
    self._consumptionValue = consumptionValue;
    self._populationValue = populationValue;
    self._populationNew = populationNew;
    self._rechargeEmissions = rechargeEmissions;
    self._eolEmissions = eolEmissions;
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
   * Get combined sales value (manufacture + import).
   *
   * @returns {EngineNumber} The combined sales value with units.
   */
  getSales() {
    const self = this;
    const manufactureValue = self.getManufacture();
    const importValue = self.getImport();
    const sales = self._combineUnitValue(manufactureValue, importValue);
    return sales;
  }

  /**
   * Get the consumption of substance value.
   *
   * @returns {EngineNumber} The consumption value with units.
   */
  getConsumption() {
    const self = this;
    return self._consumptionValue;
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
   * Get the new equipment added in this yaer.
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
    const consumptionValue = self._combineUnitValue(self.getConsumption(), other.getConsumption());
    const populationValue = self._combineUnitValue(self.getPopulation(), other.getPopulation());
    const populationNew = self._combineUnitValue(self.getPopulationNew(), other.getPopulationNew());

    const rechargeEmissions = self._combineUnitValue(
      self.getRechargeEmissions(),
      other.getRechargeEmissions(),
    );
    const eolEmissions = self._combineUnitValue(self.getEolEmissions(), other.getEolEmissions());

    return new AggregatedResult(
      manufactureValue,
      importValue,
      consumptionValue,
      populationValue,
      populationNew,
      rechargeEmissions,
      eolEmissions,
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
    const metricStrategy = {
      "emissions": () => self.getTotalEmissions(filterSet),
      "emissions:recharge": () => self.getRechargeEmissions(filterSet),
      "emissions:eol": () => self.getEolEmissions(filterSet),
      "sales": () => self.getSales(filterSet),
      "sales:import": () => self.getImport(filterSet),
      "sales:manufacture": () => self.getManufacture(filterSet),
      "population": () => self.getPopulation(filterSet),
      "population:new": () => self.getPopulationNew(filterSet),
    }[metric];
    const value = metricStrategy();
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
   * @returns {EngineNumber|null} The consumption value, or null if no matching results.
   */
  getConsumption(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getConsumption();
  }

  /**
   * Get total emissions value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The total emissions value, or null if no matching results.
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
   * @returns {EngineNumber|null} The recharge emissions value, or null if no matching results.
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
   * @returns {EngineNumber|null} The end-of-life emissions value, or null if no matching results.
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
   * @returns {EngineNumber|null} The sales value, or null if no matching results.
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
   * @returns {EngineNumber|null} The imports component of sales, or null if no matching results.
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
   * @returns {EngineNumber|null} The domestic manufacture component of sales, or null if no matching results.
   */
  getManufacture(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getManufacture();
  }

  /**
   * Get population value matching a given filter set.
   *
   * @param {FilterSet} filterSet - The filter criteria to apply.
   * @returns {EngineNumber|null} The population value, or null if no matching results.
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
   * @returns {EngineNumber|null} The new equipment added, or null if no matching results.
   */
  getPopulationNew(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated === null ? null : aggregated.getPopulationNew();
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
          x.getConsumption(),
          x.getPopulation(),
          x.getPopulationNew(),
          x.getRechargeEmissions(),
          x.getEolEmissions(),
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
   * @returns {string|null} The application for which to filter like commerical
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
