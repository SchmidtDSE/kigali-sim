/**
 * Simulation engine that underpins simulations regardless of interface.
 *
 * @license BSD, see LICENSE.md.
 */

import {
  EngineNumber,
  UnitConverter,
  ConverterStateGetter,
  OverridingConverterStateGetter,
} from "engine_number";

import {Scope, StreamKeeper} from "engine_state";

const STREAM_NAMES = new Set([
  "priorEquipment",
  "equipment",
  "export",
  "import",
  "manufacture",
  "sales",
]);

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
   *     manufacturing.
   * @param {EngineNumber} importValue - The value related to imports.
   * @param {EngineNumber} recycleValue - The value denoting recycled
   *     materials.
   * @param {EngineNumber} domesticConsumptionValue - The domestic consumption
   *     value.
   * @param {EngineNumber} importConsumptionValue - The import consumption
   *     value.
   * @param {EngineNumber} recycleConsumptionValue - The recycle consumption
   *     value.
   * @param {EngineNumber} populationValue - The population value.
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
   * @returns {EngineNumber} The manufacture value.
   */
  getManufacture() {
    const self = this;
    return self._manufactureValue;
  }

  /**
   * Get the import value.
   *
   * @returns {EngineNumber} The import value.
   */
  getImport() {
    const self = this;
    return self._importValue;
  }

  /**
   * Get the recycle value.
   *
   * @returns {EngineNumber} The recycle value.
   */
  getRecycle() {
    const self = this;
    return self._recycleValue;
  }

  /**
   * Get the total consumption without recycling.
   *
   * @returns {EngineNumber} The consumption value.
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
   * @returns {EngineNumber} The consumption value.
   */
  getConsumption() {
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
}

/**
 * Facade which runs engine mechanics.
 */
class Engine {
  /**
   * Create a new engine running from
   */
  constructor(startYear, endYear) {
    const self = this;

    const startYearRearrange = Math.min(startYear, endYear);
    const endYearRearrange = Math.max(startYear, endYear);

    self._startYear = startYearRearrange;
    self._endYear = endYearRearrange;

    self._currentYear = self._startYear;

    self._stateGetter = new ConverterStateGetter(self);
    self._unitConverter = new UnitConverter(self._stateGetter);
    self._streamKeeper = new StreamKeeper(self._unitConverter);
    self._scope = new Scope(null, null, null);
  }

  /**
   * Get the starting year of the simulation.
   *
   * @returns {number} The start year.
   */
  getStartYear() {
    const self = this;
    return self._startYear;
  }

  /**
   * Get the ending year of the simulation.
   *
   * @returns {number} The start year.
   */
  getEndYear() {
    const self = this;
    return self._endYear;
  }

  /**
   * Set the stanza for the engine current scope.
   *
   * @param newStanza The new stanza name.
   */
  setStanza(newStanza) {
    const self = this;
    self._scope = self._scope.getWithStanza(newStanza);
  }

  /**
   * Set the application for the engine current scope.
   *
   * @param newApplication The new application name.
   */
  setApplication(newApplication) {
    const self = this;
    self._scope = self._scope.getWithApplication(newApplication);
  }

  /**
   * Set the substance for the engine current scope.
   *
   * @param newSubstance The new application name.
   * @param checkValid True if an error should be thrown if the app / substance is not previously
   *    registered or false if it should be registered if not found. Defaults to false.
   */
  setSubstance(newSubstance, checkValid) {
    const self = this;
    self._scope = self._scope.getWithSubstance(newSubstance);

    if (checkValid === undefined) {
      checkValid = false;
    }

    const application = self._scope.getApplication();

    if (checkValid) {
      const knownSubstance = self._streamKeeper.hasSubstance(application, newSubstance);
      if (!knownSubstance) {
        throw "Tried accessing unknown app / substance pair: " + application + ", " + newSubstance;
      }
    } else {
      self._streamKeeper.ensureSubstance(application, newSubstance);
    }
  }

  /**
   * Get the engine's current scope.
   *
   * @return Scope object.
   */
  getScope() {
    const self = this;
    return self._scope;
  }

  /**
   * Increment the engine to simulate the next year.
   */
  incrementYear() {
    const self = this;

    if (self.getIsDone()) {
      throw "Already completed.";
    }

    self._currentYear += 1;
    self._streamKeeper.incrementYear();
  }

  /**
   * Get the year that the engine is currently simulating.
   *
   * @returns Current year simulating.
   */
  getYear() {
    const self = this;
    return self._currentYear;
  }

  /**
   * Determine if the engine has reached its final year.
   *
   * @returns True if reached the end year and false otherwise.
   */
  getIsDone() {
    const self = this;
    return self._currentYear > self._endYear;
  }

  /**
   * Set the value of a stream.
   *
   * @param {string} name - The name of the stream to set.
   * @param {EngineNumber} value - The value to set for the stream.
   * @param {Object|null} yearMatcher - The year matcher object to determine if
   *     setting the stream applies to the current year, or null. No-op if the
   *     year matcher is not satisfied.
   * @param {Scope} scope - The scope in which the stream is being set. Uses
   *     default scope if not provided.
   * @param {boolean} propagateChanges - Specifies if changes should propagate
   *     to other components. Defaults to true.
   */
  setStream(name, value, yearMatcher, scope, propagateChanges) {
    const self = this;

    const noYearMatcher = yearMatcher === undefined || yearMatcher === null;
    const inRange = noYearMatcher || yearMatcher.getInRange(self._currentYear);
    if (!inRange) {
      return;
    }

    const useDefaultScope = scope === undefined || scope === null;
    const scopeEffective = useDefaultScope ? self._scope : scope;
    const application = scopeEffective.getApplication();
    const substance = scopeEffective.getSubstance();

    if (application === null || substance === null) {
      throw "Tried setting stream without application and substance specified.";
    }

    self._streamKeeper.setStream(application, substance, name, value);

    if (propagateChanges === undefined || propagateChanges === null) {
      propagateChanges = true;
    }

    if (!propagateChanges) {
      return;
    }

    if (name === "sales" || name === "manufacture" || name === "import") {
      self._recalcPopulationChange(scopeEffective);
      self._recalcConsumption(scopeEffective);
    } else if (name === "consumption") {
      self._recalcSales(scopeEffective);
      self._recalcPopulationChange(scopeEffective);
    } else if (name === "equipment") {
      self._recalcSales(scopeEffective);
      self._recalcConsumption(scopeEffective);
    } else if (name === "priorEquipment") {
      self._recalcRetire(scopeEffective);
    }
  }

  /**
   * Get the stream value for a given application and substance scope.
   *
   * @param {string} name - The name of the stream to retrieve.
   * @param {Scope} scope - The scope within which the stream exists. Uses
   *     default scope if not provided.
   * @param {Object|null} conversion - The conversion specification for units,
   *     or null for no conversion.
   * @returns {EngineNumber} The value of the stream, possibly converted.
   */
  getStream(name, scope, conversion) {
    const self = this;
    const scopeEffective = scope === undefined || scope === null ? self._scope : scope;
    const application = scopeEffective.getApplication();
    const substance = scopeEffective.getSubstance();
    const value = self._streamKeeper.getStream(application, substance, name);

    if (conversion === undefined || conversion === null) {
      return value;
    } else {
      return self._unitConverter.convert(value, conversion);
    }
  }

  /**
   * Create a user-defined variable in the current scope.
   *
   * @param {string} name - The name of the variable to define.
   * @throws {Error} When trying to define protected variables 'yearsElapsed'
   *     or 'yearAbsolute'.
   */
  defineVariable(name) {
    const self = this;
    if (name === "yearsElapsed" || name === "yearAbsolute") {
      throw "Cannot override yearsElapsed or yearAbsolute.";
    }
    self._scope.defineVariable(name);
  }

  /**
   * Get the value of a user-defined variable in the current scope.
   *
   * @param {string} name - The name of the variable to retrieve.
   * @returns {*} The value of the variable, or special values for
   *     'yearsElapsed' and 'yearAbsolute'.
   */
  getVariable(name) {
    const self = this;
    if (name === "yearsElapsed") {
      return self._currentYear - self._startYear;
    } else if (name === "yearAbsolute") {
      return self._currentYear;
    } else {
      return self._scope.getVariable(name);
    }
  }

  /**
   * Set the value of a variable in the current scope.
   *
   * @param {string} name - The name of the variable to set.
   * @param {*} value - The value to assign to the variable.
   * @throws {Error} When trying to set protected variables
   *     'yearsElapsed' or 'yearAbsolute'.
   */
  setVariable(name, value) {
    const self = this;
    if (name === "yearsElapsed" || name === "yearAbsolute") {
      throw "Cannot set yearsElapsed or yearAbsolute.";
    }
    self._scope.setVariable(name, value);
  }

  /**
   * Get the initial charge value for a given stream.
   *
   * @param {string} stream - The stream identifier to get the initial charge
   *     for.
   * @returns {EngineNumber} The initial charge value for the stream.
   */
  getInitialCharge(stream) {
    const self = this;

    if (stream === "sales") {
      const manufactureRaw = self.getStream("manufacture");
      const manufactureValue = self._unitConverter.convert(manufactureRaw, "kg");

      const importRaw = self.getStream("import");
      const importValue = self._unitConverter.convert(importRaw, "kg");

      const determineTotal = () => {
        const manufactureRaw = manufactureValue.getValue();
        const importRaw = importValue.getValue();

        if (!isFinite(manufactureRaw)) {
          return importRaw;
        } else if (!isFinite(importRaw)) {
          return manufactureRaw;
        } else {
          return manufactureRaw + importRaw;
        }
      };

      const total = determineTotal();
      const emptyStreams = total == 0;

      const manufactureInitialChargeRaw = self.getInitialCharge("manufacture");
      const manufactureInitialChargeUnbounded = self._unitConverter.convert(
        manufactureInitialChargeRaw,
        "kg / unit",
      );
      const importInitialChargeRaw = self.getInitialCharge("import");
      const importInitialChargeUnbounded = self._unitConverter.convert(
        importInitialChargeRaw,
        "kg / unit",
      );

      const getBounded = (target, alternative) => {
        if (target.getValue() == 0) {
          return alternative;
        } else {
          return target;
        }
      };

      const manufactureInitialCharge = getBounded(
        manufactureInitialChargeUnbounded,
        importInitialChargeUnbounded,
      );

      const importInitialCharge = getBounded(
        importInitialChargeUnbounded,
        manufactureInitialChargeUnbounded,
      );

      const manufactureKg = emptyStreams ? 1 : manufactureValue.getValue();
      const importKg = emptyStreams ? 1 : importValue.getValue();
      const manufactureKgUnit = manufactureInitialCharge.getValue();
      const importKgUnit = importInitialCharge.getValue();
      const manufactureUnits = manufactureKgUnit == 0 ? 0 : manufactureKg / manufactureKgUnit;
      const importUnits = importKgUnit == 0 ? 0 : importKg / importKgUnit;
      const newSumWeighted = manufactureKgUnit * manufactureUnits + importKgUnit * importUnits;
      const newSumWeight = manufactureUnits + importUnits;
      const pooledKgUnit = newSumWeighted / newSumWeight;
      return new EngineNumber(pooledKgUnit, "kg / unit");
    } else {
      const application = self._scope.getApplication();
      const substance = self._scope.getSubstance();
      return self._streamKeeper.getInitialCharge(application, substance, stream);
    }
  }

  /**
   * Set the initial charge for a stream.
   *
   * @param {EngineNumber} value - The initial charge value to set.
   * @param {string} stream - The stream identifier to set the initial charge
   *     for.
   * @param {Object} yearMatcher - Matcher to determine if the change applies
   *     to current year.
   */
  setInitialCharge(value, stream, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    if (stream === "sales") {
      self.setInitialCharge(value, "manufacture");
      self.setInitialCharge(value, "import");
    } else {
      const application = self._scope.getApplication();
      const substance = self._scope.getSubstance();
      self._streamKeeper.setInitialCharge(application, substance, stream, value);
    }

    self._recalcPopulationChange();
  }

  /**
   * Get the recharge volume for the current application and substance.
   *
   * @returns {EngineNumber} The recharge volume value.
   */
  getRechargeVolume() {
    const self = this;
    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    return self._streamKeeper.getRechargeVolume(application, substance);
  }

  /**
   * Get the recharge intensity for the current application and substance.
   *
   * @returns {EngineNumber} The recharge intensity value.
   */
  getRechargeIntensity() {
    const self = this;
    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    return self._streamKeeper.getRechargeIntensity(application, substance);
  }

  /**
   * Set recharge parameters for the current application and substance.
   *
   * @param {EngineNumber} volume - The recharge volume to set.
   * @param {EngineNumber} intensity - The recharge intensity to set.
   * @param {Object} yearMatcher - Matcher to determine if the change applies
   *     to current year.
   */
  recharge(volume, intensity, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    // Setup
    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();

    // Check allowed
    if (application === null || substance === null) {
      throw "Tried recalculating population change without application and substance.";
    }

    // Set values
    self._streamKeeper.setRechargePopulation(application, substance, volume);
    self._streamKeeper.setRechargeIntensity(application, substance, intensity);

    // Recalculate
    self._recalcPopulationChange();
    self._recalcSales();
    self._recalcConsumption();
  }

  /**
   * Set retirement rate for the current application and substance.
   *
   * @param {EngineNumber} amount - The retirement rate to set.
   * @param {Object} yearMatcher - Matcher to determine if the change applies
   *     to current year.
   */
  retire(amount, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    self._streamKeeper.setRetirementRate(application, substance, amount);
    self._recalcRetire();
  }

  /**
   * Get the retirement rate for the current application and substance.
   *
   * @returns {EngineNumber} The retirement rate value.
   */
  getRetirementRate() {
    const self = this;
    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    return self._streamKeeper.getRetirementRate(application, substance);
  }

  /**
   * Set recycling parameters for the current application and substance.
   *
   * @param {EngineNumber} recoveryWithUnits - The recovery rate.
   * @param {EngineNumber} yieldWithUnits - The yield rate.
   * @param {EngineNumber} displaceLevel - The displacement level.
   * @param {Object} yearMatcher - Matcher to determine if the change applies
   *     to current year.
   */
  recycle(recoveryWithUnits, yieldWithUnits, displaceLevel, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    self._streamKeeper.setRecoveryRate(application, substance, recoveryWithUnits);
    self._streamKeeper.setYieldRate(application, substance, yieldWithUnits);

    if (displaceLevel !== null && displaceLevel !== undefined) {
      self._streamKeeper.setDisplacementRate(application, substance, displaceLevel);
    }

    self._recalcSales();
    self._recalcPopulationChange();
    self._recalcConsumption();
  }

  /**
   * Set GHG equivalency for the current application and substance.
   *
   * @param {EngineNumber} amount - The GHG intensity value to set.
   * @param {Object} yearMatcher - Matcher to determine if the change applies
   *     to current year.
   */
  equals(amount, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    self._streamKeeper.setGhgIntensity(application, substance, amount);

    self._recalcConsumption();
  }

  /**
   * Change a stream value by a delta amount.
   *
   * @param {string} stream - The stream identifier to modify.
   * @param {EngineNumber} amount - The amount to change the stream by.
   * @param {Object} yearMatcher - Matcher to determine if the change applies
   *     to current year.
   * @param {Scope} scope - The scope in which to make the change.
   */
  changeStream(stream, amount, yearMatcher, scope) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const currentValue = self.getStream(stream, scope);
    const unitConverter = self._createUnitConverterWithTotal(stream);

    const convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    const newAmount = currentValue.getValue() + convertedDelta.getValue();
    const outputWithUnits = new EngineNumber(newAmount, currentValue.getUnits());
    self.setStream(stream, outputWithUnits, null, scope);
  }

  /**
   * Cap a stream at a maximum value.
   *
   * @param {string} stream - The stream identifier to cap.
   * @param {EngineNumber} amount - The maximum value to cap at.
   * @param {Object} yearMatcher - Matcher to determine if the change applies
   *     to current year.
   * @param {string} displaceTarget - Optional target for displaced amount.
   */
  cap(stream, amount, yearMatcher, displaceTarget) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const unitConverter = self._createUnitConverterWithTotal(stream);

    const currentValueRaw = self.getStream(stream);
    const currentValue = unitConverter.convert(currentValueRaw, "kg");

    const convertedMax = unitConverter.convert(amount, "kg");
    const changeAmountRaw = convertedMax.getValue() - currentValue.getValue();
    const changeAmount = Math.min(changeAmountRaw, 0);

    const changeWithUnits = new EngineNumber(changeAmount, "kg");
    self.changeStream(stream, changeWithUnits);

    if (displaceTarget !== null && displaceTarget !== undefined) {
      const displaceChange = new EngineNumber(changeAmount * -1, "kg");
      const isStream = STREAM_NAMES.has(displaceTarget);

      if (isStream) {
        self.changeStream(displaceTarget, displaceChange);
      } else {
        const destinationScope = self._scope.getWithSubstance(displaceTarget);
        self.changeStream(stream, displaceChange, null, destinationScope);
      }
    }
  }

  /**
   * Set a minimum floor value for a stream.
   *
   * @param {string} stream - The stream identifier to set floor for.
   * @param {EngineNumber} amount - The minimum value to set as floor.
   * @param {Object} yearMatcher - Matcher to determine if the change applies
   *     to current year.
   * @param {string} displaceTarget - Optional target for displaced amount.
   */
  floor(stream, amount, yearMatcher, displaceTarget) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const unitConverter = self._createUnitConverterWithTotal(stream);

    const currentValueRaw = self.getStream(stream);
    const currentValue = unitConverter.convert(currentValueRaw, "kg");

    const convertedMin = unitConverter.convert(amount, "kg");
    const changeAmountRaw = convertedMin.getValue() - currentValue.getValue();
    const changeAmount = Math.max(changeAmountRaw, 0);

    const changeWithUnits = new EngineNumber(changeAmount, "kg");
    self.changeStream(stream, changeWithUnits);

    if (displaceTarget !== null && displaceTarget !== undefined) {
      const displaceChange = new EngineNumber(changeAmount * -1, "kg");
      const isStream = STREAM_NAMES.has(displaceTarget);

      if (isStream) {
        self.changeStream(displaceTarget, displaceChange);
      } else {
        const destinationScope = self._scope.getWithSubstance(displaceTarget);
        self.changeStream(stream, displaceChange, null, destinationScope);
      }
    }
  }

  /**
   * Replace an amount from one substance with another.
   *
   * @param {EngineNumber} amountRaw - The amount to replace.
   * @param {string} stream - The stream identifier to modify.
   * @param {string} destinationSubstance - The substance to replace with.
   * @param {Object} yearMatcher - Matcher to determine if the change applies
   *     to current year.
   */
  replace(amountRaw, stream, destinationSubstance, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const unitConverter = self._createUnitConverterWithTotal(stream);
    const amount = unitConverter.convert(amountRaw, "kg");

    const amountNegative = new EngineNumber(-1 * amount.getValue(), amount.getUnits());
    self.changeStream(stream, amountNegative);

    const destinationScope = self._scope.getWithSubstance(destinationSubstance);
    self.changeStream(stream, amount, null, destinationScope);
  }

  /**
   * Get the results for all registered substances.
   *
   * @returns {EngineResult[]} Array of results for each registered substance.
   */
  getResults() {
    const self = this;

    const substances = self._streamKeeper.getRegisteredSubstances();

    return substances.map((substanceId) => {
      // Get meta
      const application = substanceId.getApplication();
      const substance = substanceId.getSubstance();
      const year = self._currentYear;

      // Prepare units
      const stateGetter = new OverridingConverterStateGetter(self._stateGetter);
      const unitConverter = new UnitConverter(stateGetter);

      // Get sales
      const manufactureRaw = self._streamKeeper.getStream(application, substance, "manufacture");
      const importRaw = self._streamKeeper.getStream(application, substance, "import");
      const recycleRaw = self._streamKeeper.getStream(application, substance, "recycle");

      const manufactureValue = unitConverter.convert(manufactureRaw, "kg");
      const importValue = unitConverter.convert(importRaw, "kg");
      const recycleValue = unitConverter.convert(recycleRaw, "kg");

      // Get emissions
      const populationValue = self._streamKeeper.getStream(application, substance, "equipment");
      const populationNew = self._streamKeeper.getStream(application, substance, "newEquipment");
      const rechargeEmissions = self._streamKeeper.getStream(
        application,
        substance,
        "rechargeEmissions",
      );
      const eolEmissions = self._streamKeeper.getStream(application, substance, "eolEmissions");

      // Get percent for offset
      const manufactureKg = manufactureValue.getValue();
      const importKg = importValue.getValue();
      const recycleKg = recycleValue.getValue();

      const nonRecycleSalesKg = manufactureKg + importKg;
      const percentManufacture = manufactureKg / nonRecycleSalesKg;
      const percentImport = 1 - percentManufacture;

      // Offset sales
      const manufactureValueOffset = new EngineNumber(
        manufactureKg - recycleKg * percentManufacture,
        "kg",
      );

      const importValueOffset = new EngineNumber(importKg - recycleKg * percentImport, "kg");

      // Get consumption
      const consumptionRaw = self._streamKeeper.getGhgIntensity(application, substance);

      stateGetter.setVolume(manufactureValueOffset);
      const domesticConsumptionValue = unitConverter.convert(consumptionRaw, "tCO2e");

      stateGetter.setVolume(importValueOffset);
      const importConsumptionValue = unitConverter.convert(consumptionRaw, "tCO2e");

      stateGetter.setVolume(recycleValue);
      const recycleConsumptionValue = unitConverter.convert(consumptionRaw, "tCO2e");

      // Offset recharge emissions
      stateGetter.setVolume(null);
      const rechargeEmissionsConvert = unitConverter.convert(rechargeEmissions, "tCO2e");
      const rechargeEmissionsOffset = new EngineNumber(
        rechargeEmissionsConvert.getValue() - recycleConsumptionValue.getValue(),
        "tCO2e",
      );

      // Package
      return new EngineResult(
        application,
        substance,
        year,
        manufactureValueOffset,
        importValueOffset,
        recycleValue,
        domesticConsumptionValue,
        importConsumptionValue,
        recycleConsumptionValue,
        populationValue,
        populationNew,
        rechargeEmissionsOffset,
        eolEmissions,
      );
    });
  }

  /**
   * Creates a unit converter with total values initialized.
   *
   * @param {string} stream - The stream identifier to create converter for.
   * @returns {UnitConverter} A configured unit converter instance.
   * @private
   */
  _createUnitConverterWithTotal(stream) {
    const self = this;

    const stateGetter = new OverridingConverterStateGetter(self._stateGetter);
    const unitConverter = new UnitConverter(stateGetter);

    const currentValue = self.getStream(stream);
    stateGetter.setTotal(stream, currentValue);

    const isSalesSubstream = stream === "manufacture" || stream === "import";
    if (isSalesSubstream) {
      stateGetter.setAmortizedUnitVolume(self.getInitialCharge(stream));
    }

    return unitConverter;
  }

  /**
   * Determines if a year matcher applies to current year.
   *
   * @param {Object|null} yearMatcher - The year matcher to check.
   * @returns {boolean} True if in range or no matcher provided.
   * @private
   */
  _getIsInRange(yearMatcher) {
    const self = this;
    const noYearMatcher = yearMatcher === undefined || yearMatcher === null;
    const inRange = noYearMatcher || yearMatcher.getInRange(self._currentYear);
    return inRange;
  }

  /**
   * Recalculates population changes based on current state.
   *
   * @param {Scope|null} scope - The scope to recalculate for.
   * @private
   */
  _recalcPopulationChange(scope) {
    const self = this;

    const stateGetter = new OverridingConverterStateGetter(self._stateGetter);
    const unitConverter = new UnitConverter(stateGetter);
    const scopeEffective = scope === null || scope === undefined ? self._scope : scope;
    const application = scopeEffective.getApplication();
    const substance = scopeEffective.getSubstance();

    if (application === null || substance === null) {
      throw "Tried recalculating population change without application and substance.";
    }

    // Get prior population
    const priorPopulationRaw = self.getStream("priorEquipment", scopeEffective);
    const priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    // Get substance sales
    const substanceSalesRaw = self.getStream("sales", scopeEffective);
    const substanceSales = unitConverter.convert(substanceSalesRaw, "kg");

    // Get recharge population
    stateGetter.setPopulation(self.getStream("priorEquipment", scopeEffective));
    const rechargePopRaw = self._streamKeeper.getRechargePopulation(application, substance);
    const rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.setPopulation(null);

    // Switch to recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    const rechargeIntensityRaw = self._streamKeeper.getRechargeIntensity(application, substance);
    const rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");
    const rechargeGhg = unitConverter.convert(rechargeVolume, "tCO2e");

    // Return to prior population
    stateGetter.setPopulation(priorPopulation);

    // Get total volume available for new units
    const salesKg = substanceSales.getValue();
    const rechargeKg = rechargeVolume.getValue();
    const availableForNewUnitsKg = salesKg - rechargeKg;

    // Convert to unit delta
    const initialChargeRaw = self.getInitialCharge("sales");
    const initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");
    const initialChargeKgUnit = initialCharge.getValue();
    const deltaUnits = availableForNewUnitsKg / initialChargeKgUnit;
    const newVolume = new EngineNumber(deltaUnits, "units");

    // Find new total
    const priorPopulationUnits = priorPopulation.getValue();
    const newUnits = priorPopulationUnits + deltaUnits;
    const newUnitsAllowed = newUnits < 0 ? 0 : newUnits;
    const newVolumeTotal = new EngineNumber(newUnitsAllowed, "units");

    // Save
    self.setStream("equipment", newVolumeTotal, null, scopeEffective, false);
    self.setStream("newEquipment", newVolume, null, scopeEffective, false);
    self.setStream("rechargeEmissions", rechargeGhg, null, scopeEffective, false);
  }

  /**
   * Recalculates consumption values based on current state.
   *
   * @param {Scope|null} scope - The scope to recalculate for.
   * @private
   */
  _recalcConsumption(scope) {
    const self = this;

    const scopeEffective = scope === null || scope === undefined ? self._scope : scope;

    const stateGetter = new OverridingConverterStateGetter(self._stateGetter);
    const unitConverter = new UnitConverter(stateGetter);
    const application = scopeEffective.getApplication();
    const substance = scopeEffective.getSubstance();

    if (application === null || substance === null) {
      throw "Tried recalculating consumption without application and substance.";
    }

    // Determine sales
    const salesRaw = self.getStream("sales", scopeEffective);
    const sales = unitConverter.convert(salesRaw, "kg");

    // Determine consumption
    stateGetter.setVolume(sales);
    const consumptionRaw = self._streamKeeper.getGhgIntensity(application, substance);
    const consumption = unitConverter.convert(consumptionRaw, "tCO2e");
    stateGetter.setVolume(null);

    // Ensure in range
    const isNegative = consumption.getValue() < 0;
    const consumptionAllowed = isNegative ? new EngineNumber(0, "tCO2e") : consumption;

    // Save
    self.setStream("consumption", consumptionAllowed, null, scopeEffective, false);
  }

  /**
   * Recalculates sales values based on current state.
   *
   * @param {Scope|null} scope - The scope to recalculate for.
   * @private
   */
  _recalcSales(scope) {
    const self = this;

    const scopeEffective = scope === null || scope === undefined ? self._scope : scope;

    const stateGetter = new OverridingConverterStateGetter(self._stateGetter);
    const unitConverter = new UnitConverter(stateGetter);
    const application = scopeEffective.getApplication();
    const substance = scopeEffective.getSubstance();

    if (application === null || substance === null) {
      throw "Tried recalculating sales without application and substance.";
    }

    // Get recharge population
    const basePopulation = self.getStream("priorEquipment", scopeEffective);
    stateGetter.setPopulation(basePopulation);
    const rechargePopRaw = self._streamKeeper.getRechargePopulation(application, substance);
    const rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.setPopulation(null);

    // Switch into recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    const rechargeIntensityRaw = self._streamKeeper.getRechargeIntensity(application, substance);
    const rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    // Determine initial charge
    const initialChargeRaw = self.getInitialCharge("sales");
    const initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");

    // Get recycling volume
    stateGetter.setVolume(rechargeVolume);
    const recoveryVolumeRaw = self._streamKeeper.getRecoveryRate(application, substance);
    const recoveryVolume = unitConverter.convert(recoveryVolumeRaw, "kg");
    stateGetter.setVolume(null);

    // Get recycling amount
    stateGetter.setVolume(recoveryVolume);
    const recycledVolumeRaw = self._streamKeeper.getYieldRate(application, substance);
    const recycledVolume = unitConverter.convert(recycledVolumeRaw, "kg");
    stateGetter.setVolume(null);

    // Get recycling displaced
    const recycledKg = recycledVolume.getValue();

    const displacementRateRaw = self._streamKeeper.getDisplacementRate(application, substance);
    const displacementRate = unitConverter.convert(displacementRateRaw, "%");
    const displacementRateRatio = displacementRate.getValue() / 100;
    const recycledDisplacedKg = recycledKg * displacementRateRatio;

    // Switch out of recharge population
    stateGetter.setPopulation(null);

    // Determine needs for new equipment deployment.
    stateGetter.setAmortizedUnitVolume(initialCharge);
    const populationChangeRaw = stateGetter.getPopulationChange(self._unitConverter);
    const populationChange = unitConverter.convert(populationChangeRaw, "units");
    const volumeForNew = unitConverter.convert(populationChange, "kg");

    // Get prior population
    const priorPopulationRaw = self.getStream("priorEquipment", scopeEffective);
    const priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    // Determine sales prior to recycling
    const kgForRecharge = rechargeVolume.getValue();
    const kgForNew = volumeForNew.getValue();

    // Return to original initial charge
    stateGetter.setAmortizedUnitVolume(null);

    // Return original
    stateGetter.setVolume(null);

    // Determine how much to offset domestic and imports
    const manufactureRaw = self.getStream("manufacture", scopeEffective);
    const importRaw = self.getStream("import", scopeEffective);
    const priorRecycleRaw = self.getStream("recycle", scopeEffective);

    const manufactureSalesConverted = unitConverter.convert(manufactureRaw, "kg");
    const importSalesConverted = unitConverter.convert(importRaw, "kg");
    const priorRecycleSalesConverted = unitConverter.convert(priorRecycleRaw, "kg");

    const manufactureSalesKg = manufactureSalesConverted.getValue();
    const importSalesKg = importSalesConverted.getValue();
    const priorRecycleSalesKg = priorRecycleSalesConverted.getValue();
    const totalNonRecycleKg = manufactureSalesKg + importSalesKg;

    const percentManufacture = manufactureSalesKg / totalNonRecycleKg;
    const percentImport = importSalesKg / totalNonRecycleKg;

    // Offset
    const newRecycleValue = new EngineNumber(recycledDisplacedKg, "kg");
    self._streamKeeper.setStream(application, substance, "recycle", newRecycleValue, false);
  }

  /**
   * Recalculates retirement values based on current state.
   *
   * @param {Scope|null} scope - The scope to recalculate for.
   * @private
   */
  _recalcRetire(scope) {
    const self = this;

    // Setup
    const stateGetter = new OverridingConverterStateGetter(self._stateGetter);
    const unitConverter = new UnitConverter(stateGetter);
    const scopeEffective = scope === null || scope === undefined ? self._scope : scope;
    const application = scopeEffective.getApplication();
    const substance = scopeEffective.getSubstance();

    // Check allowed
    if (application === null || substance === null) {
      throw "Tried recalculating population change without application and substance.";
    }

    // Calculate change
    const currentPriorRaw = self._streamKeeper.getStream(application, substance, "priorEquipment");
    const currentPrior = unitConverter.convert(currentPriorRaw, "units");

    const currentEquipmentRaw = self._streamKeeper.getStream(application, substance, "equipment");
    const currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");

    stateGetter.setPopulation(currentPrior);
    const amountRaw = self._streamKeeper.getRetirementRate(application, substance);
    const amount = unitConverter.convert(amountRaw, "units");
    stateGetter.setPopulation(null);

    const newPrior = new EngineNumber(currentPrior.getValue() - amount.getValue(), "units");
    const newEquipment = new EngineNumber(currentEquipment.getValue() - amount.getValue(), "units");

    // Update equipement streams
    self._streamKeeper.setStream(application, substance, "priorEquipment", newPrior);
    self._streamKeeper.setStream(application, substance, "equipment", newEquipment);

    // Update GHG accounting
    const eolGhg = unitConverter.convert(amount, "tCO2e");
    self._streamKeeper.setStream(application, substance, "eolEmissions", eolGhg);

    // Propagate
    self._recalcPopulationChange();
    self._recalcSales();
    self._recalcConsumption();
  }
}

export {Engine};
