/**
 * Structures to describe user configuration of visualizations and outputs.
 *
 * @license BSD-3-Clause
 */

/**
 * Filters and baseline to apply in creating a visualization.
 *
 * Class representing a set of filters to apply in identifying a subset of data
 * to visualize as well as an optional offsetting baseline.
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
   * @param {string|null} baseline - Baseline scenario for comparison.
   * @param {boolean|null} attributeImporter - Whether to attribute imported
   *     equipment initial charge to importer (true) or exporter (false).
   * @param {Object|null} customDefinitions - Custom metric definitions object
   *     with keys 'emissions' and 'sales' mapping to arrays of submetrics.
   */
  constructor(
    year,
    scenario,
    application,
    substance,
    metric,
    dimension,
    baseline,
    attributeImporter,
    customDefinitions,
  ) {
    const self = this;
    self._year = year;
    self._scenario = scenario;
    self._application = application;
    self._substance = substance;
    self._metric = metric;
    self._dimension = dimension;
    self._baseline = baseline;
    self._attributeImporter = attributeImporter;
    self._customDefinitions = customDefinitions || {
      'emissions': null,
      'sales': null
    };
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
      self._baseline,
      self._attributeImporter,
      self._customDefinitions,
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
      self._baseline,
      self._attributeImporter,
      self._customDefinitions,
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
      self._baseline,
      self._attributeImporter,
      self._customDefinitions,
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
      self._baseline,
      self._attributeImporter,
      self._customDefinitions,
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
      self._baseline,
      self._attributeImporter,
      self._customDefinitions,
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
      self._baseline,
      self._attributeImporter,
      self._customDefinitions,
    );
  }

  /**
   * Get the baseline scenario.
   *
   * @returns {string|null} The baseline scenario name for comparison.
   */
  getBaseline() {
    const self = this;
    return self._baseline;
  }

  /**
   * Get a new filter set with updated baseline.
   *
   * @param {string} newBaseline - The new baseline scenario value.
   * @returns {FilterSet} New filter set with updated baseline.
   */
  getWithBaseline(newBaseline) {
    const self = this;
    return new FilterSet(
      self._year,
      self._scenario,
      self._application,
      self._substance,
      self._metric,
      self._dimension,
      newBaseline,
      self._attributeImporter,
      self._customDefinitions,
    );
  }

  /**
   * Get the attribute importer setting.
   *
   * @returns {boolean|null} Whether to attribute imported equipment initial
   *     charge to importer (true) or exporter (false), or null if not set.
   */
  getAttributeImporter() {
    const self = this;
    return self._attributeImporter;
  }

  /**
   * Get a new filter set with updated attribute importer setting.
   *
   * @param {boolean} newAttributeImporter - The new attribute importer value.
   * @returns {FilterSet} New filter set with updated attribute importer.
   */
  getWithAttributeImporter(newAttributeImporter) {
    const self = this;
    return new FilterSet(
      self._year,
      self._scenario,
      self._application,
      self._substance,
      self._metric,
      self._dimension,
      self._baseline,
      newAttributeImporter,
      self._customDefinitions,
    );
  }

  /**
   * Get custom metric definition for a specific metric family.
   *
   * @param {string} metricFamily - The metric family ('emissions' or 'sales').
   * @returns {Array<string>|null} Array of selected submetrics or null if none.
   */
  getCustomDefinition(metricFamily) {
    const self = this;
    return self._customDefinitions[metricFamily];
  }

  /**
   * Get a new filter set with updated custom definition for a metric family.
   *
   * @param {string} metricFamily - The metric family ('emissions' or 'sales').
   * @param {Array<string>|null} definition - Array of submetrics or null.
   * @returns {FilterSet} New filter set with updated custom definition.
   */
  getWithCustomDefinition(metricFamily, definition) {
    const self = this;
    const newCustomDefinitions = {...self._customDefinitions};
    newCustomDefinitions[metricFamily] = definition;
    
    return new FilterSet(
      self._year,
      self._scenario,
      self._application,
      self._substance,
      self._metric,
      self._dimension,
      self._baseline,
      self._attributeImporter,
      newCustomDefinitions,
    );
  }

  /**
   * Check if the current metric is a custom metric.
   *
   * @returns {boolean} True if current metric uses custom submetric.
   */
  isCustomMetric() {
    const self = this;
    if (self._metric === null) {
      return false;
    }
    const submetric = self.getSubMetric();
    return submetric === 'custom';
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

export {FilterSet};
