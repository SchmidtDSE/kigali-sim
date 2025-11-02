/**
 * Structures for describing simulation outputs.
 *
 * @license BSD-3-Clause
 */

import {EngineNumber, makeNumberUnambiguousString} from "engine_number";

/**
 * Result of an engine execution for a substance for an application and year.
 *
 * Part of a simulation result representing the values evaluated for a single
 * substance and application within a single year.
 */
class EngineResult {
  /**
   * Constructor for creating an EngineResult instance.
   *
   * @param {EngineResultBuilder} builder - A builder containing all values
   *     required to construct the result.
   */
  constructor(builder) {
    const self = this;
    self._application = builder.getApplication();
    self._substance = builder.getSubstance();
    self._year = builder.getYear();
    self._scenarioName = builder.getScenarioName();
    self._trialNumber = builder.getTrialNumber();
    self._domesticValue = builder.getDomesticValue();
    self._importValue = builder.getImportValue();
    self._exportValue = builder.getExportValue();
    self._recycleValue = builder.getRecycleValue();
    self._domesticConsumptionValue = builder.getDomesticConsumptionValue();
    self._importConsumptionValue = builder.getImportConsumptionValue();
    self._exportConsumptionValue = builder.getExportConsumptionValue();
    self._recycleConsumptionValue = builder.getRecycleConsumptionValue();
    self._populationValue = builder.getPopulationValue();
    self._populationNew = builder.getPopulationNew();
    self._rechargeEmissions = builder.getRechargeEmissions();
    self._eolEmissions = builder.getEolEmissions();
    self._initialChargeEmissions = builder.getInitialChargeEmissions();
    self._energyConsumption = builder.getEnergyConsumption();
    self._tradeSupplement = builder.getTradeSupplement();
    self._bankKg = builder.getBankKg();
    self._bankTco2e = builder.getBankTco2e();
    self._bankChangeKg = builder.getBankChangeKg();
    self._bankChangeTco2e = builder.getBankChangeTco2e();
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
   * Get the domestic value.
   *
   * @returns {EngineNumber} The domestic value in volume like kg.
   */
  getDomestic() {
    const self = this;
    return self._domesticValue;
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
   * Get the export value.
   *
   * @returns {EngineNumber} The export value in volume like kg.
   */
  getExport() {
    const self = this;
    return self._exportValue;
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
   * Get the export consumption value.
   *
   * @returns {EngineNumber} The export consumption value.
   */
  getExportConsumption() {
    const self = this;
    return self._exportConsumptionValue;
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
   * Get the greenhouse gas emissions from initial charge activities.
   *
   * This is an informational metric representing the GHG potential of substance
   * initially charged into equipment. Actual emissions occur later during recharge
   * (leakage between servicings) or at end-of-life disposal.
   *
   * @returns {EngineNumber} The initial charge emissions value with units.
   */
  getInitialChargeEmissions() {
    const self = this;
    return self._initialChargeEmissions;
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
   * Get the trade supplement information.
   *
   * @returns {TradeSupplement} The additional trade information needed for
   *     attribution.
   */
  getTradeSupplement() {
    const self = this;
    return self._tradeSupplement;
  }

  /**
   * Get the substance bank in kg.
   *
   * @returns {EngineNumber} The bank value in kg.
   */
  getBankKg() {
    const self = this;
    return self._bankKg;
  }

  /**
   * Get the substance bank in tCO2e.
   *
   * @returns {EngineNumber} The bank value in tCO2e.
   */
  getBankTco2e() {
    const self = this;
    return self._bankTco2e;
  }

  /**
   * Get the change in substance bank in kg.
   *
   * @returns {EngineNumber} The bank change value in kg.
   */
  getBankChangeKg() {
    const self = this;
    return self._bankChangeKg;
  }

  /**
   * Get the change in substance bank in tCO2e.
   *
   * @returns {EngineNumber} The bank change value in tCO2e.
   */
  getBankChangeTco2e() {
    const self = this;
    return self._bankChangeTco2e;
  }

  /**
   * Get the scenario name.
   *
   * @returns {string} The name of the scenario being run.
   */
  getScenarioName() {
    const self = this;
    return self._scenarioName;
  }

  /**
   * Get the trial number.
   *
   * @returns {number} The trial number of the current run.
   */
  getTrialNumber() {
    const self = this;
    return self._trialNumber;
  }
}

/**
 * Decorator which attributes initial charge to the exporter.
 *
 * Decorator which attributes initial charge to the exporter which is in
 * contrast to the default where initial chanrge is included for the importer
 * totals. Leaves other attributes including population and all domestic
 * calculations unchanged. This represents a single substance and application
 * in a single year with attribution added.
 */
class AttributeToExporterResult {
  /**
   * Create a new decorator around a raw result with importer attribution.
   *
   * @param {EngineResult} inner - The value to be decorated that will apply
   *     trade attribution to exporter at time of request.
   */
  constructor(inner) {
    const self = this;
    self._inner = inner;
  }

  /**
   * Get the application for which results are reported.
   *
   * @returns {string} The unchanged application from the decorated result.
   */
  getApplication() {
    const self = this;
    return self._inner.getApplication();
  }

  /**
   * Get the substance for which results are reported.
   *
   * @returns {string} The unchanged substance from the decorated result.
   */
  getSubstance() {
    const self = this;
    return self._inner.getSubstance();
  }

  /**
   * Get the year for which results are reported.
   *
   * @returns {number} The unchanged year from the decorated result.
   */
  getYear() {
    const self = this;
    return self._inner.getYear();
  }

  /**
   * Get the domestic volume.
   *
   * @returns {EngineNumber} The unchanged domestic volume in kg or similar
   *     from the decorated result.
   */
  getDomestic() {
    const self = this;
    return self._inner.getDomestic();
  }

  /**
   * Get the import volume associated with this result.
   *
   * @returns {EngineValue} The import volume in kg or similar from the
   *     decorated result but with initial charge attributed to exporter.
   */
  getImport() {
    const self = this;
    const totalImport = self._inner.getImport();
    const tradeSupplement = self._inner.getTradeSupplement();
    const importInitialCharge = tradeSupplement.getImportInitialChargeValue();

    const totalUnits = totalImport.getUnits();
    const initialChargeUnits = importInitialCharge.getUnits();
    if (totalUnits !== initialChargeUnits) {
      const mismatchDescription = "between " + totalUnits + " and " + initialChargeUnits;
      throw "Could not attribute trade due to units mismatch " + mismatchDescription;
    }

    const importInitialChargeValue = importInitialCharge.getValue();
    const effectiveInitialCharge = importInitialChargeValue > 0 ? importInitialChargeValue : 0;
    const innerNumber = totalImport.getValue() - effectiveInitialCharge;
    return new EngineNumber(innerNumber, totalUnits, makeNumberUnambiguousString(innerNumber));
  }

  /**
   * Get the recycle volume.
   *
   * @returns {EngineNumber} The unchanged recycle volume in kg or similar
   *     from the decorated result.
   */
  getRecycle() {
    const self = this;
    return self._inner.getRecycle();
  }

  /**
   * Get the total consumption without recycling.
   *
   * @returns {EngineNumber} The unchanged consumption value in tCO2e or similar
   *     from the decorated result, combining domestic and import consumption.
   */
  getConsumptionNoRecycle() {
    const self = this;
    return self._inner.getConsumptionNoRecycle();
  }

  /**
   * Get the total greenhouse gas consumption.
   *
   * @returns {EngineNumber} The total GHG consumption value in tCO2e or similar.
   */
  getGhgConsumption() {
    const self = this;
    return self._inner.getGhgConsumption();
  }

  /**
   * Get the domestic consumption value.
   *
   * @returns {EngineNumber} The domestic consumption value in tCO2e or similar.
   */
  getDomesticConsumption() {
    const self = this;
    return self._inner.getDomesticConsumption();
  }

  /**
   * Get the import consumption value with exporter attribution.
   *
   * @returns {EngineNumber} The import consumption value in tCO2e or similar,
   *     adjusted for exporter attribution by removing initial charge consumption.
   */
  getImportConsumption() {
    const self = this;
    const totalImport = self._inner.getImportConsumption();
    const tradeSupplement = self._inner.getTradeSupplement();
    const importInitialCharge = tradeSupplement.getImportInitialChargeConsumption();

    const totalUnits = totalImport.getUnits();
    const initialChargeUnits = importInitialCharge.getUnits();
    if (totalUnits !== initialChargeUnits) {
      const mismatchDescription = "between " + totalUnits + " and " + initialChargeUnits;
      throw "Could not attribute trade due to units mismatch " + mismatchDescription;
    }

    const importInitialChargeValue = importInitialCharge.getValue();
    const effectiveInitialCharge = importInitialChargeValue > 0 ? importInitialChargeValue : 0;
    const innerNumber = totalImport.getValue() - effectiveInitialCharge;
    return new EngineNumber(innerNumber, totalUnits, makeNumberUnambiguousString(innerNumber));
  }

  /**
   * Get the recycle consumption value.
   *
   * @returns {EngineNumber} The recycle consumption value in tCO2e or similar.
   */
  getRecycleConsumption() {
    const self = this;
    return self._inner.getRecycleConsumption();
  }

  /**
   * Get the population value.
   *
   * @returns {EngineNumber} The population value in terms of equipment units.
   */
  getPopulation() {
    const self = this;
    return self._inner.getPopulation();
  }

  /**
   * Get the amount of new equipment added this year.
   *
   * @returns {EngineNumber} The amount of new equipment added this year in units.
   */
  getPopulationNew() {
    const self = this;
    return self._inner.getPopulationNew();
  }

  /**
   * Get the greenhouse gas emissions from recharge activities.
   *
   * @returns {EngineNumber} The recharge emissions value in tCO2e or similar.
   */
  getRechargeEmissions() {
    const self = this;
    return self._inner.getRechargeEmissions();
  }

  /**
   * Get the greenhouse gas emissions from end-of-life equipment.
   *
   * @returns {EngineNumber} The end-of-life emissions value in tCO2e or similar.
   */
  getEolEmissions() {
    const self = this;
    return self._inner.getEolEmissions();
  }

  /**
   * Get the greenhouse gas emissions from initial charge activities.
   *
   * This is an informational metric representing the GHG potential of substance
   * initially charged into equipment. Actual emissions occur later during recharge
   * (leakage between servicings) or at end-of-life disposal.
   *
   * @returns {EngineNumber} The initial charge emissions value in tCO2e or similar.
   */
  getInitialChargeEmissions() {
    const self = this;
    return self._inner.getInitialChargeEmissions();
  }

  /**
   * Get the energy consumption value.
   *
   * @returns {EngineNumber} The energy consumption value with units.
   */
  getEnergyConsumption() {
    const self = this;
    return self._inner.getEnergyConsumption();
  }

  /**
   * Get the import supplement information.
   *
   * @returns {ImportSupplement} The additional import information needed for
   *     attribution from the decorated result.
   */
  getTradeSupplement() {
    const self = this;
    return self._inner.getTradeSupplement();
  }

  /**
   * Get the scenario name.
   *
   * @returns {string} The name of the scenario being run.
   */
  getScenarioName() {
    const self = this;
    return self._inner.getScenarioName();
  }

  /**
   * Get the trial number.
   *
   * @returns {number} The trial number of the current run.
   */
  getTrialNumber() {
    const self = this;
    return self._inner.getTrialNumber();
  }

  /**
   * Get the export volume associated with this result.
   *
   * @returns {EngineValue} The export volume in kg or similar from the
   *     decorated result but with initial charge attributed to exporter.
   */
  getExport() {
    const self = this;
    const totalExport = self._inner.getExport();
    const tradeSupplement = self._inner.getTradeSupplement();
    const exportInitialCharge = tradeSupplement.getExportInitialChargeValue();

    const totalUnits = totalExport.getUnits();
    const initialChargeUnits = exportInitialCharge.getUnits();
    if (totalUnits !== initialChargeUnits) {
      const mismatchDescription = "between " + totalUnits + " and " + initialChargeUnits;
      throw "Could not attribute trade due to units mismatch " + mismatchDescription;
    }

    const exportInitialChargeValue = exportInitialCharge.getValue();
    const effectiveInitialCharge = exportInitialChargeValue > 0 ? exportInitialChargeValue : 0;
    const innerNumber = totalExport.getValue() + effectiveInitialCharge;
    return new EngineNumber(innerNumber, totalUnits, makeNumberUnambiguousString(innerNumber));
  }

  /**
   * Get the export consumption value with exporter attribution.
   *
   * @returns {EngineNumber} The export consumption value in tCO2e or similar,
   *     adjusted for exporter attribution by adding initial charge consumption.
   */
  getExportConsumption() {
    const self = this;
    const totalExport = self._inner.getExportConsumption();
    const tradeSupplement = self._inner.getTradeSupplement();
    const exportInitialCharge = tradeSupplement.getExportInitialChargeConsumption();

    const totalUnits = totalExport.getUnits();
    const initialChargeUnits = exportInitialCharge.getUnits();
    if (totalUnits !== initialChargeUnits) {
      const mismatchDescription = "between " + totalUnits + " and " + initialChargeUnits;
      throw "Could not attribute trade due to units mismatch " + mismatchDescription;
    }

    const exportInitialChargeValue = exportInitialCharge.getValue();
    const effectiveInitialCharge = exportInitialChargeValue > 0 ? exportInitialChargeValue : 0;
    const innerNumber = totalExport.getValue() + effectiveInitialCharge;
    return new EngineNumber(innerNumber, totalUnits, makeNumberUnambiguousString(innerNumber));
  }

  /**
   * Get the substance bank in kg.
   *
   * @returns {EngineNumber} The bank value in kg.
   */
  getBankKg() {
    const self = this;
    return self._inner.getBankKg();
  }

  /**
   * Get the substance bank in tCO2e.
   *
   * @returns {EngineNumber} The bank value in tCO2e.
   */
  getBankTco2e() {
    const self = this;
    return self._inner.getBankTco2e();
  }

  /**
   * Get the change in substance bank in kg.
   *
   * @returns {EngineNumber} The bank change value in kg.
   */
  getBankChangeKg() {
    const self = this;
    return self._inner.getBankChangeKg();
  }

  /**
   * Get the change in substance bank in tCO2e.
   *
   * @returns {EngineNumber} The bank change value in tCO2e.
   */
  getBankChangeTco2e() {
    const self = this;
    return self._inner.getBankChangeTco2e();
  }
}

/**
 * Description of trade activity within a result.
 *
 * As a supplement to an {EngineResult}, offers additional description of trade
 * activity on new equipment (and their initial charge) to support different
 * kinds of trade attributions. This is not reported to the user but is
 * required for some internal calculations prior to aggregation operations. This
 * provides supplemental information for a single application and substance in
 * a single year.
 */
class TradeSupplement {
  /**
   * Create a new summary of trade activities.
   *
   * @param {EngineValue} importInitialChargeValue - The volume of substance imported
   *     via initial charge on imported equipment (like kg).
   * @param {EngineValue} importInitialChargeConsumption - The consumption
   *     associated with initial charge of imported equipment (like tCO2e).
   * @param {EngineValue} importPopulation - The number of new units imported.
   * @param {EngineValue} exportInitialChargeValue - The volume of substance exported
   *     via initial charge on exported equipment (like kg).
   * @param {EngineValue} exportInitialChargeConsumption - The consumption
   *     associated with initial charge of exported equipment (like tCO2e).
   */
  constructor(
    importInitialChargeValue,
    importInitialChargeConsumption,
    importPopulation,
    exportInitialChargeValue,
    exportInitialChargeConsumption,
  ) {
    const self = this;
    self._importInitialChargeValue = importInitialChargeValue;
    self._importInitialChargeConsumption = importInitialChargeConsumption;
    self._importPopulation = importPopulation;
    self._exportInitialChargeValue = exportInitialChargeValue;
    self._exportInitialChargeConsumption = exportInitialChargeConsumption;
  }

  /**
   * Get the volume of substance imported via initial charge on imported equipment.
   *
   * @returns {EngineValue} The import initial charge value in volume units like kg.
   */
  getImportInitialChargeValue() {
    const self = this;
    return self._importInitialChargeValue;
  }

  /**
   * Get the consumption associated with initial charge of imported equipment.
   *
   * @returns {EngineValue} The import initial charge consumption value in units like tCO2e.
   */
  getImportInitialChargeConsumption() {
    const self = this;
    return self._importInitialChargeConsumption;
  }

  /**
   * Get the number of new units imported.
   *
   * @returns {EngineValue} The import population value in units.
   */
  getImportPopulation() {
    const self = this;
    return self._importPopulation;
  }

  /**
   * Get the volume of substance exported via initial charge on exported equipment.
   *
   * @returns {EngineValue} The export initial charge value in volume units like kg.
   */
  getExportInitialChargeValue() {
    const self = this;
    return self._exportInitialChargeValue;
  }

  /**
   * Get the consumption associated with initial charge of exported equipment.
   *
   * @returns {EngineValue} The export initial charge consumption value in units like tCO2e.
   */
  getExportInitialChargeConsumption() {
    const self = this;
    return self._exportInitialChargeConsumption;
  }
}

// Alias for backward compatibility
const ImportSupplement = TradeSupplement;

/**
 * Builder to help construct an EngineResult.
 */
class EngineResultBuilder {
  /**
   * Create builder without any values initalized.
   */
  constructor() {
    const self = this;
    self._application = null;
    self._substance = null;
    self._year = null;
    self._scenarioName = null;
    self._trialNumber = null;
    self._domesticValue = null;
    self._importValue = null;
    self._exportValue = null;
    self._recycleValue = null;
    self._domesticConsumptionValue = null;
    self._importConsumptionValue = null;
    self._exportConsumptionValue = null;
    self._recycleConsumptionValue = null;
    self._populationValue = null;
    self._populationNew = null;
    self._rechargeEmissions = null;
    self._eolEmissions = null;
    self._initialChargeEmissions = null;
    self._energyConsumption = null;
    self._tradeSupplement = null;
    self._bankKg = null;
    self._bankTco2e = null;
    self._bankChangeKg = null;
    self._bankChangeTco2e = null;
  }

  /**
   * Set the application for which a result is being given.
   *
   * @param {string} application - The application to be associated with this
   *     engine result.
   */
  setApplication(application) {
    const self = this;
    self._application = application;
  }

  /**
   * Set the substance for which a result is being given.
   *
   * @param {string} substance - The substance to be associated with this
   *     engine result.
   */
  setSubstance(substance) {
    const self = this;
    self._substance = substance;
  }

  /**
   * Set the year for which a result is being given.
   *
   * @param {number} year - The year to be associated with this engine result.
   */
  setYear(year) {
    const self = this;
    self._year = year;
  }

  /**
   * Set the scenario name for which a result is being given.
   *
   * @param {string} scenarioName - The name of the scenario being run.
   */
  setScenarioName(scenarioName) {
    const self = this;
    self._scenarioName = scenarioName;
  }

  /**
   * Set the trial number for which a result is being given.
   *
   * @param {number} trialNumber - The trial number of the current run.
   */
  setTrialNumber(trialNumber) {
    const self = this;
    self._trialNumber = trialNumber;
  }

  /**
   * Set the domestic value.
   *
   * @param {EngineNumber} domesticValue - The value associated with
   *     domestic production in volume like kg.
   */
  setDomesticValue(domesticValue) {
    const self = this;
    self._domesticValue = domesticValue;
  }

  /**
   * Set the import value.
   *
   * @param {EngineNumber} importValue - The value related to imports like in
   *     volume like kg.
   */
  setImportValue(importValue) {
    const self = this;
    self._importValue = importValue;
  }

  /**
   * Set the export value.
   *
   * @param {EngineNumber} exportValue - The value related to exports like in
   *     volume like kg.
   */
  setExportValue(exportValue) {
    const self = this;
    self._exportValue = exportValue;
  }

  /**
   * Set the recycle value.
   *
   * @param {EngineNumber} recycleValue - The value denoting recycled
   *     materials in volume like kg.
   */
  setRecycleValue(recycleValue) {
    const self = this;
    self._recycleValue = recycleValue;
  }

  /**
   * Set the domestic consumption value.
   *
   * @param {EngineNumber} domesticConsumptionValue - The domestic consumption
   *     value in tCO2e or equivalent.
   */
  setDomesticConsumptionValue(domesticConsumptionValue) {
    const self = this;
    self._domesticConsumptionValue = domesticConsumptionValue;
  }

  /**
   * Set the import consumption value.
   *
   * @param {EngineNumber} importConsumptionValue - The import consumption
   *     value in tCO2e or equivalent.
   */
  setImportConsumptionValue(importConsumptionValue) {
    const self = this;
    self._importConsumptionValue = importConsumptionValue;
  }

  /**
   * Set the export consumption value.
   *
   * @param {EngineNumber} exportConsumptionValue - The export consumption
   *     value in tCO2e or equivalent.
   */
  setExportConsumptionValue(exportConsumptionValue) {
    const self = this;
    self._exportConsumptionValue = exportConsumptionValue;
  }

  /**
   * Set the recycle consumption value.
   *
   * @param {EngineNumber} recycleConsumptionValue - The recycle consumption
   *     value in tCO2e or equivalent.
   */
  setRecycleConsumptionValue(recycleConsumptionValue) {
    const self = this;
    self._recycleConsumptionValue = recycleConsumptionValue;
  }

  /**
   * Set the population value.
   *
   * @param {EngineNumber} populationValue - The population value in terms of
   *     equipment.
   */
  setPopulationValue(populationValue) {
    const self = this;
    self._populationValue = populationValue;
  }

  /**
   * Set the population new value.
   *
   * @param {EngineNumber} populationNew - The amount of new equipment added
   *     this year.
   */
  setPopulationNew(populationNew) {
    const self = this;
    self._populationNew = populationNew;
  }

  /**
   * Set the recharge emissions value.
   *
   * @param {EngineNumber} rechargeEmissions - The greenhouse gas emissions
   *     from recharge activities.
   */
  setRechargeEmissions(rechargeEmissions) {
    const self = this;
    self._rechargeEmissions = rechargeEmissions;
  }

  /**
   * Set the end-of-life emissions value.
   *
   * @param {EngineNumber} eolEmissions - The greenhouse gas emissions from
   *     end-of-life equipment.
   */
  setEolEmissions(eolEmissions) {
    const self = this;
    self._eolEmissions = eolEmissions;
  }

  /**
   * Set the initial charge emissions value.
   *
   * @param {EngineNumber} initialChargeEmissions - The greenhouse gas emissions from
   *     initial charge activities.
   */
  setInitialChargeEmissions(initialChargeEmissions) {
    const self = this;
    self._initialChargeEmissions = initialChargeEmissions;
  }

  /**
   * Set the energy consumption value.
   *
   * @param {EngineNumber} energyConsumption - The energy consumption value
   *     with units.
   */
  setEnergyConsumption(energyConsumption) {
    const self = this;
    self._energyConsumption = energyConsumption;
  }

  /**
   * Specify the supplemental trade information needed for attribution.
   *
   * @param {TradeSupplement} tradeSupplement - Supplemental trade
   *     information.
   */
  setTradeSupplement(tradeSupplement) {
    const self = this;
    self._tradeSupplement = tradeSupplement;
  }

  /**
   * Set the bank kg value.
   *
   * @param {EngineNumber} bankKg - The bank value in kg.
   */
  setBankKg(bankKg) {
    const self = this;
    self._bankKg = bankKg;
  }

  /**
   * Set the bank tCO2e value.
   *
   * @param {EngineNumber} bankTco2e - The bank value in tCO2e.
   */
  setBankTco2e(bankTco2e) {
    const self = this;
    self._bankTco2e = bankTco2e;
  }

  /**
   * Set the bank change kg value.
   *
   * @param {EngineNumber} bankChangeKg - The bank change value in kg.
   */
  setBankChangeKg(bankChangeKg) {
    const self = this;
    self._bankChangeKg = bankChangeKg;
  }

  /**
   * Set the bank change tCO2e value.
   *
   * @param {EngineNumber} bankChangeTco2e - The bank change value in tCO2e.
   */
  setBankChangeTco2e(bankChangeTco2e) {
    const self = this;
    self._bankChangeTco2e = bankChangeTco2e;
  }

  /**
   * Get the application for which a result is being built.
   *
   * @returns {string} The application, or null if not yet set.
   */
  getApplication() {
    const self = this;
    return self._application;
  }

  /**
   * Get the substance for which a result is being built.
   *
   * @returns {string} The substance, or null if not yet set.
   */
  getSubstance() {
    const self = this;
    return self._substance;
  }

  /**
   * Get the year for which a result is being built.
   *
   * @returns {number} The year, or null if not yet set.
   */
  getYear() {
    const self = this;
    return self._year;
  }

  /**
   * Get the scenario name for which a result is being built.
   *
   * @returns {string} The scenario name, or null if not yet set.
   */
  getScenarioName() {
    const self = this;
    return self._scenarioName;
  }

  /**
   * Get the trial number for which a result is being built.
   *
   * @returns {number} The trial number, or null if not yet set.
   */
  getTrialNumber() {
    const self = this;
    return self._trialNumber;
  }

  /**
   * Get the domestic value.
   *
   * @returns {EngineNumber} The domestic value, or null if not yet set.
   */
  getDomesticValue() {
    const self = this;
    return self._domesticValue;
  }

  /**
   * Get the import value.
   *
   * @returns {EngineNumber} The import value, or null if not yet set.
   */
  getImportValue() {
    const self = this;
    return self._importValue;
  }

  /**
   * Get the export value.
   *
   * @returns {EngineNumber} The export value, or null if not yet set.
   */
  getExportValue() {
    const self = this;
    return self._exportValue;
  }

  /**
   * Get the recycle value.
   *
   * @returns {EngineNumber} The recycle value, or null if not yet set.
   */
  getRecycleValue() {
    const self = this;
    return self._recycleValue;
  }

  /**
   * Get the domestic consumption value.
   *
   * @returns {EngineNumber} The domestic consumption value, or null if not yet set.
   */
  getDomesticConsumptionValue() {
    const self = this;
    return self._domesticConsumptionValue;
  }

  /**
   * Get the import consumption value.
   *
   * @returns {EngineNumber} The import consumption value, or null if not yet set.
   */
  getImportConsumptionValue() {
    const self = this;
    return self._importConsumptionValue;
  }

  /**
   * Get the export consumption value.
   *
   * @returns {EngineNumber} The export consumption value, or null if not yet set.
   */
  getExportConsumptionValue() {
    const self = this;
    return self._exportConsumptionValue;
  }

  /**
   * Get the recycle consumption value.
   *
   * @returns {EngineNumber} The recycle consumption value, or null if not yet set.
   */
  getRecycleConsumptionValue() {
    const self = this;
    return self._recycleConsumptionValue;
  }

  /**
   * Get the population value.
   *
   * @returns {EngineNumber} The population value, or null if not yet set.
   */
  getPopulationValue() {
    const self = this;
    return self._populationValue;
  }

  /**
   * Get the population new value.
   *
   * @returns {EngineNumber} The population new value, or null if not yet set.
   */
  getPopulationNew() {
    const self = this;
    return self._populationNew;
  }

  /**
   * Get the recharge emissions value.
   *
   * @returns {EngineNumber} The recharge emissions value, or null if not yet set.
   */
  getRechargeEmissions() {
    const self = this;
    return self._rechargeEmissions;
  }

  /**
   * Get the end-of-life emissions value.
   *
   * @returns {EngineNumber} The end-of-life emissions value, or null if not yet set.
   */
  getEolEmissions() {
    const self = this;
    return self._eolEmissions;
  }

  /**
   * Get the initial charge emissions value.
   *
   * @returns {EngineNumber} The initial charge emissions value, or null if not yet set.
   */
  getInitialChargeEmissions() {
    const self = this;
    return self._initialChargeEmissions;
  }

  /**
   * Get the energy consumption value.
   *
   * @returns {EngineNumber} The energy consumption value, or null if not yet set.
   */
  getEnergyConsumption() {
    const self = this;
    return self._energyConsumption;
  }

  /**
   * Get the supplemental trade information.
   *
   * @returns {TradeSupplement} The trade supplement, or null if not yet set.
   */
  getTradeSupplement() {
    const self = this;
    return self._tradeSupplement;
  }

  /**
   * Get the bank kg value.
   *
   * @returns {EngineNumber} The bank kg value, or null if not yet set.
   */
  getBankKg() {
    const self = this;
    return self._bankKg;
  }

  /**
   * Get the bank tCO2e value.
   *
   * @returns {EngineNumber} The bank tCO2e value, or null if not yet set.
   */
  getBankTco2e() {
    const self = this;
    return self._bankTco2e;
  }

  /**
   * Get the bank change kg value.
   *
   * @returns {EngineNumber} The bank change kg value, or null if not yet set.
   */
  getBankChangeKg() {
    const self = this;
    return self._bankChangeKg;
  }

  /**
   * Get the bank change tCO2e value.
   *
   * @returns {EngineNumber} The bank change tCO2e value, or null if not yet set.
   */
  getBankChangeTco2e() {
    const self = this;
    return self._bankChangeTco2e;
  }

  /**
   * Check that the builder is complete and create a new result.
   *
   * @returns {EngineResult} The result built from the values provided to this
   *     builder.
   */
  build() {
    const self = this;
    self._checkReadyToConstruct();
    return new EngineResult(self);
  }

  _checkReadyToConstruct() {
    const self = this;

    const checkValid = (value, name) => {
      if (value === null || value === undefined) {
        throw "Could not make engine result because " + name + " was not given.";
      }
    };

    checkValid(self._application, "application");
    checkValid(self._substance, "substance");
    checkValid(self._year, "year");
    checkValid(self._scenarioName, "scenarioName");
    checkValid(self._trialNumber, "trialNumber");
    checkValid(self._domesticValue, "domesticValue");
    checkValid(self._importValue, "importValue");
    checkValid(self._exportValue, "exportValue");
    checkValid(self._recycleValue, "recycleValue");
    checkValid(self._domesticConsumptionValue, "domesticConsumptionValue");
    checkValid(self._importConsumptionValue, "importConsumptionValue");
    checkValid(self._exportConsumptionValue, "exportConsumptionValue");
    checkValid(self._recycleConsumptionValue, "recycleConsumptionValue");
    checkValid(self._populationValue, "populationValue");
    checkValid(self._populationNew, "populationNew");
    checkValid(self._rechargeEmissions, "rechargeEmissions");
    checkValid(self._eolEmissions, "eolEmissions");
    checkValid(self._initialChargeEmissions, "initialChargeEmissions");
    checkValid(self._energyConsumption, "energyConsumption");
    checkValid(self._tradeSupplement, "tradeSupplement");
    checkValid(self._bankKg, "bankKg");
    checkValid(self._bankTco2e, "bankTco2e");
    checkValid(self._bankChangeKg, "bankChangeKg");
    checkValid(self._bankChangeTco2e, "bankChangeTco2e");
  }
}

/**
 * Statistics from or summary of a group of results.
 *
 * Result for a single group of results after aggregation like for all
 * consumption across all substances in an application.
 */
class AggregatedResult {
  /**
   * Construct an AggregatedResult instance from two objects compatible with EngineResult.
   *
   * Combines two objects that have accessor methods matching EngineResult by using additive
   * logic with unit standardization and conversion. If both objects are identical (same reference),
   * creates a copy without doubling values.
   *
   * @param {Object} first - The first result object with compatible accessor methods
   *     (getDomestic, getImport, etc.).
   * @param {Object} second - The second result object with compatible accessor methods,
   *     to be combined with the first result. If same as first, creates a wrapper without doubling.
   */
  constructor(first, second) {
    const self = this;

    // If both arguments are the same object, just copy the values without combining
    if (first === second) {
      self._domesticValue = first.getDomestic();
      self._importValue = first.getImport();
      self._recycleValue = first.getRecycle();
      self._exportValue = first.getExport();
      self._domesticConsumptionValue = first.getDomesticConsumption();
      self._importConsumptionValue = first.getImportConsumption();
      self._recycleConsumptionValue = first.getRecycleConsumption();
      self._exportConsumptionValue = first.getExportConsumption();
      self._populationValue = first.getPopulation();
      self._populationNew = first.getPopulationNew();
      self._rechargeEmissions = first.getRechargeEmissions();
      self._eolEmissions = first.getEolEmissions();
      self._initialChargeEmissions = first.getInitialChargeEmissions();
      self._energyConsumption = first.getEnergyConsumption();
      self._bankKg = first.getBankKg();
      self._bankTco2e = first.getBankTco2e();
      self._bankChangeKg = first.getBankChangeKg();
      self._bankChangeTco2e = first.getBankChangeTco2e();
      return;
    }

    // Combine the two objects using the same logic as the combine method
    self._domesticValue = self._combineUnitValue(first.getDomestic(), second.getDomestic());
    self._importValue = self._combineUnitValue(first.getImport(), second.getImport());
    self._recycleValue = self._combineUnitValue(first.getRecycle(), second.getRecycle());
    self._exportValue = self._combineUnitValue(first.getExport(), second.getExport());
    self._domesticConsumptionValue = self._combineUnitValue(
      first.getDomesticConsumption(),
      second.getDomesticConsumption(),
    );
    self._importConsumptionValue = self._combineUnitValue(
      first.getImportConsumption(),
      second.getImportConsumption(),
    );
    self._recycleConsumptionValue = self._combineUnitValue(
      first.getRecycleConsumption(),
      second.getRecycleConsumption(),
    );
    self._exportConsumptionValue = self._combineUnitValue(
      first.getExportConsumption(),
      second.getExportConsumption(),
    );
    self._populationValue = self._combineUnitValue(first.getPopulation(), second.getPopulation());
    self._populationNew = self._combineUnitValue(
      first.getPopulationNew(),
      second.getPopulationNew(),
    );
    self._rechargeEmissions = self._combineUnitValue(
      first.getRechargeEmissions(),
      second.getRechargeEmissions(),
    );
    self._eolEmissions = self._combineUnitValue(
      first.getEolEmissions(),
      second.getEolEmissions(),
    );
    self._initialChargeEmissions = self._combineUnitValue(
      first.getInitialChargeEmissions(),
      second.getInitialChargeEmissions(),
    );
    self._energyConsumption = self._combineUnitValue(
      first.getEnergyConsumption(),
      second.getEnergyConsumption(),
    );
    self._bankKg = self._combineUnitValue(first.getBankKg(), second.getBankKg());
    self._bankTco2e = self._combineUnitValue(first.getBankTco2e(), second.getBankTco2e());
    self._bankChangeKg = self._combineUnitValue(
      first.getBankChangeKg(),
      second.getBankChangeKg(),
    );
    self._bankChangeTco2e = self._combineUnitValue(
      first.getBankChangeTco2e(),
      second.getBankChangeTco2e(),
    );
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
   * Get the domestic (as opposed to import) of substance.
   *
   * @returns {EngineNumber} The domestic value with units like kg.
   */
  getDomestic() {
    const self = this;
    return self._domesticValue;
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
   * Get the export sales.
   *
   * @returns {EngineNumber} The export sales with units like kg.
   */
  getExport() {
    const self = this;
    return self._exportValue;
  }

  /**
   * Get combined sales value (manufacture + import).
   *
   * @returns {EngineNumber} The combined sales value with units like kg.
   */
  getSales() {
    const self = this;
    const domesticValue = self.getDomestic();
    const importValue = self.getImport();
    const recycleValue = self.getRecycle();
    const noRecycle = self._combineUnitValue(domesticValue, importValue);
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
   * Get the export consumption.
   *
   * @returns {EngineNumber} The export consumption with units like tCO2e.
   */
  getExportConsumption() {
    const self = this;
    return self._exportConsumptionValue;
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
   * Get the greenhouse gas emissions from initial charge activities.
   *
   * This is an informational metric representing the GHG potential of substance
   * initially charged into equipment. Actual emissions occur later during recharge
   * (leakage between servicings) or at end-of-life disposal.
   *
   * @returns {EngineNumber} The initial charge emissions value with units like
   *     tCO2e.
   */
  getInitialChargeEmissions() {
    const self = this;
    return self._initialChargeEmissions;
  }

  /**
   * Get the substance bank in kg.
   *
   * @returns {EngineNumber} The bank value in kg.
   */
  getBankKg() {
    const self = this;
    return self._bankKg;
  }

  /**
   * Get the substance bank in tCO2e.
   *
   * @returns {EngineNumber} The bank value in tCO2e.
   */
  getBankTco2e() {
    const self = this;
    return self._bankTco2e;
  }

  /**
   * Get the change in substance bank in kg.
   *
   * @returns {EngineNumber} The bank change value in kg.
   */
  getBankChangeKg() {
    const self = this;
    return self._bankChangeKg;
  }

  /**
   * Get the change in substance bank in tCO2e.
   *
   * @returns {EngineNumber} The bank change value in tCO2e.
   */
  getBankChangeTco2e() {
    const self = this;
    return self._bankChangeTco2e;
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
    const result = a.getValue() + b.getValue();
    return new EngineNumber(result, a.getUnits(), makeNumberUnambiguousString(result));
  }
}


export {
  AggregatedResult,
  AttributeToExporterResult,
  EngineResult,
  EngineResultBuilder,
  ImportSupplement,
  TradeSupplement,
};
