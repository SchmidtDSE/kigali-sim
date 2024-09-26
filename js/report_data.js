import {EngineNumber} from "engine_number";


class AggregatedResult {
  constructor(manufactureValue, importValue, emissionsValue, populationValue) {
    const self = this;
    self._manufactureValue = manufactureValue;
    self._importValue = importValue;
    self._emissionsValue = emissionsValue;
    self._populationValue = populationValue;
  }

  getManufacture() {
    const self = this;
    return self._manufactureValue;
  }

  getImport() {
    const self = this;
    return self._importValue;
  }

  getSales() {
    const self = this;
    const manufactureValue = self.getManufacture();
    const importValue = self.getImport();
    const sales = self._combineUnitValue(manufactureValue, importValue);
    return sales;
  }

  getEmissions() {
    const self = this;
    return self._emissionsValue;
  }

  getPopulation() {
    const self = this;
    return self._populationValue;
  }

  combine(other) {
    const self = this;

    const manufactureValue = self._combineUnitValue(self.getManufacture(), other.getManufacture());
    const importValue = self._combineUnitValue(self.getImport(), other.getImport());
    const emissionsValue = self._combineUnitValue(self.getEmissions(), other.getEmissions());
    const populationValue = self._combineUnitValue(self.getPopulation(), other.getPopulation());

    return new AggregatedResult(
      manufactureValue,
      importValue,
      emissionsValue,
      populationValue,
    );
  }

  _combineUnitValue(a, b) {
    const self = this;
    if (a.getUnits() !== b.getUnits()) {
      throw "Encountered different units during aggregation.";
    }
    return new EngineNumber(a.getValue() + b.getValue(), a.getUnits());
  }
}


class ReportDataWrapper {
  constructor(innerData) {
    const self = this;
    self._innerData = innerData;
  }

  getScenarios() {
    const self = this;
    return new Set(self._innerData.map((x) => x.getName()));
  }

  getApplications(filterSet) {
    const self = this;
    return self._getSetAfterFilter(filterSet, (x) => x.getApplication());
  }

  getSubstances(filterSet) {
    const self = this;
    return self._getSetAfterFilter(filterSet, (x) => x.getSubstance());
  }

  getYears(filterSet) {
    const self = this;
    return self._getSetAfterFilter(filterSet, (x) => x.getYear());
  }

  getEmissions(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated.getEmissions();
  }

  getSales(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated.getSales();
  }

  getPopulation(filterSet) {
    const self = this;
    const aggregated = self._getAggregatedAfterFilter(filterSet);
    return aggregated.getPopulation();
  }

  _getFlatResults() {
    const self = this;
    return self._innerData.map((x) => x.getTrialResults()).flat();
  }

  _getSetAfterFilter(filterSet, getter) {
    const self = this;
    const afterFilter = self._applyFilterSet(filterSet);
    const values = afterFilter.map(getter);
    return new Set(values);
  }

  _getAggregatedAfterFilter(filterSet) {
    const self = this;
    const afterFilter = self._applyFilterSet(filterSet);
    const preAggregated = afterFilter.map((x) => new AggregatedResult(
      x.getManufacture(),
      x.getImport(),
      x.getEmissions(),
      x.getPopulation(),
    ));

    if (preAggregated.length == 0) {
      return null;
    }

    const aggregated = preAggregated.reduce((a, b) => a.combine(b));

    return aggregated;
  }

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


class FilterSet {
  constructor(year, scenario, application, substance, metric, dimension) {
    const self = this;
    self._year = year;
    self._scenario = scenario;
    self._application = application;
    self._substance = substance;
    self._metric = metric;
    self._dimension = dimension;
  }

  getYear() {
    const self = this;
    return self._year;
  }

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

  getScenario() {
    const self = this;
    return self._scenario;
  }

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

  getApplication() {
    const self = this;
    return self._application;
  }

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

  getSubstance() {
    const self = this;
    return self._substance;
  }

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

  getMetric() {
    const self = this;
    return self._metric;
  }

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

  getDimension() {
    const self = this;
    return self._dimension;
  }

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
  
}


export {ReportDataWrapper, FilterSet};
