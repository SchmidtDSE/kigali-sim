import {
  EngineNumber,
  UnitConverter,
  ConverterStateGetter,
  OverridingConverterStateGetter,
} from "engine_number";

import {
  Scope,
  StreamKeeper,
} from "engine_state";


class EngineResult {
  constructor(application, substance, year, manufactureValue, importValue, emissionsValue,
    populationValue) {
    const self = this;
    self._application = application;
    self._substance = substance;
    self._year = year;
    self._manufactureValue = manufactureValue;
    self._importValue = importValue;
    self._emissionsValue = emissionsValue;
    self._populationValue = populationValue;
  }

  getApplication() {
    const self = this;
    return self._application;
  }

  getSubstance() {
    const self = this;
    return self._substance;
  }

  getYear() {
    const self = this;
    return self._year;
  }

  getManufacture() {
    const self = this;
    return self._manufactureValue;
  }

  getImport() {
    const self = this;
    return self._importValue;
  }

  getEmissions() {
    const self = this;
    return self._emissionsValue;
  }

  getPopulation() {
    const self = this;
    return self._populationValue;
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
    self._startYear = startYear;
    self._endYear = endYear;
    self._currentYear = self._startYear;

    self._stateGetter = new ConverterStateGetter(self);
    self._unitConverter = new UnitConverter(self._stateGetter);
    self._streamKeeper = new StreamKeeper(self._unitConverter);
    self._scope = new Scope(null, null, null);
  }

  getStartYear() {
    const self = this;
    return self._startYear;
  }

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
   */
  setSubstance(newSubstance) {
    const self = this;
    self._scope = self._scope.getWithSubstance(newSubstance);
    self._streamKeeper.ensureSubstance(
      self._scope.getApplication(),
      newSubstance,
    );
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
      self._recalcEmissions(scopeEffective);
    } else if (name === "emissions") {
      self._recalcSales(scopeEffective);
      self._recalcPopulationChange(scopeEffective);
    } else if (name === "equipment") {
      self._recalcSales(scopeEffective);
      self._recalcEmissions(scopeEffective);
    } else if (name === "priorEqipment") {
      self._recalcPopulationChange(scopeEffective);
    }
  }

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

  defineVariable(name) {
    const self = this;
    if (name === "yearsElapsed" || name === "yearAbsolute") {
      throw "Cannot override yearsElapsed or yearAbsolute.";
    }
    self._scope.defineVariable(name);
  }

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

  setVariable(name, value) {
    const self = this;
    if (name === "yearsElapsed" || name === "yearAbsolute") {
      throw "Cannot set yearsElapsed or yearAbsolute.";
    }
    self._scope.setVariable(name, value);
  }

  getInitialCharge(stream) {
    const self = this;

    if (stream === "sales") {
      const manufactureRaw = self.getStream("manufacture");
      const manufactureValue = self._unitConverter.convert(manufactureRaw, "kg");

      const importRaw = self.getStream("import");
      const importValue = self._unitConverter.convert(importRaw, "kg");

      const total = manufactureValue.getValue() + importValue.getValue();
      const emptyStreams = total == 0;

      const manufactureInitialChargeRaw = self.getInitialCharge("manufacture");
      const manufactureInitialCharge = self._unitConverter.convert(
        manufactureInitialChargeRaw,
        "kg / unit",
      );
      const importInitialChargeRaw = self.getInitialCharge("import");
      const importInitialCharge = self._unitConverter.convert(
        importInitialChargeRaw,
        "kg / unit",
      );

      const manufactureKg = emptyStreams ? 1 : manufactureValue.getValue();
      const importKg = emptyStreams ? 1 : importValue.getValue();
      const manufactureKgUnit = manufactureInitialCharge.getValue();
      const importKgUnit = importInitialCharge.getValue();
      const manufactureUnits = manufactureKgUnit == 0 ? 0 : manufactureKg / manufactureKgUnit;
      const importUnits = importKgUnit == 0 ? 0 : importKg / importKgUnit;
      const newSumWeighted = (manufactureKgUnit * manufactureUnits + importKgUnit * importUnits);
      const newSumWeight = manufactureUnits + importUnits;
      const pooledKgUnit = newSumWeighted / newSumWeight;
      return new EngineNumber(pooledKgUnit, "kg / unit");
    } else {
      const application = self._scope.getApplication();
      const substance = self._scope.getSubstance();
      return self._streamKeeper.getInitialCharge(application, substance, stream);
    }
  }

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

  getRechargeVolume() {
    const self = this;
    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    return self._streamKeeper.getRechargeVolume(application, substance);
  }

  getRechargeIntensity() {
    const self = this;
    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    return self._streamKeeper.getRechargeIntensity(application, substance);
  }

  recharge(volume, intensity, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    self._streamKeeper.setRechargePopulation(application, substance, volume);
    self._streamKeeper.setRechargeIntensity(application, substance, intensity);

    self._recalcPopulationChange();
    self._recalcSales();
    self._recalcEmissions();
  }

  retire(amount, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const unitConverter = self._createUnitConverterWithTotal("priorEquipment");
    const equipmentToRetire = unitConverter.convert(amount, "units");
    const retireAsDelta = new EngineNumber(
      equipmentToRetire.getValue() * -1,
      equipmentToRetire.getUnits(),
    );

    self.changeStream("retirement", equipmentToRetire);
    self.changeStream("equipment", retireAsDelta);
  }

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
    self._recalcEmissions();
  }

  emit(amount, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const application = self._scope.getApplication();
    const substance = self._scope.getSubstance();
    self._streamKeeper.setGhgIntensity(application, substance, amount);

    self._recalcEmissions();
  }

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

  cap(stream, amount, yearMatcher) {
    const self = this;

    if (!self._getIsInRange(yearMatcher)) {
      return;
    }

    const currentValue = self.getStream(stream);
    const unitConverter = self._createUnitConverterWithTotal(stream);

    const convertedMax = unitConverter.convert(amount, currentValue.getUnits());
    const newAmount = Math.min(currentValue.getValue(), convertedMax.getValue());
    const outputWithUnits = new EngineNumber(newAmount, currentValue.getUnits());
    self.setStream(stream, outputWithUnits);
  }

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

  getResults() {
    const self = this;

    const substances = self._streamKeeper.getRegisteredSubstances();

    return substances.map((substanceId) => {
      const application = substanceId.getApplication();
      const substance = substanceId.getSubstance();
      const year = self._currentYear;
      const manufactureValue = self._streamKeeper.getStream(application, substance, "manufacture");
      const importValue = self._streamKeeper.getStream(application, substance, "import");
      const emissionsValue = self._streamKeeper.getStream(application, substance, "emissions");
      const populationValue = self._streamKeeper.getStream(application, substance, "equipment");

      return new EngineResult(
        application,
        substance,
        year,
        manufactureValue,
        importValue,
        emissionsValue,
        populationValue,
      );
    });
  }

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

  _getIsInRange(yearMatcher) {
    const self = this;
    const noYearMatcher = yearMatcher === undefined || yearMatcher === null;
    const inRange = noYearMatcher || yearMatcher.getInRange(self._currentYear);
    return inRange;
  }

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

    // Get prior popoulation
    const priorPopulationRaw = self.getStream("priorEquipment", scopeEffective);
    const priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    // Get retirement from prior population.
    const retirementRaw = self._streamKeeper.getRetirementRate(
      application,
      substance,
    );
    const retiredPopulation = unitConverter.convert(retirementRaw, "units");
    const retireUnits = retiredPopulation.getValue();

    // Get substance sales
    const substanceSalesRaw = self.getStream("sales", scopeEffective);
    const substanceSales = unitConverter.convert(substanceSalesRaw, "kg");

    // Get recharge population
    stateGetter.setPopulation(self.getStream("priorEquipment", scopeEffective));
    const rechargePopRaw = self._streamKeeper.getRechargePopulation(
      application,
      substance,
    );
    const rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.setPopulation(null);

    // Switch to recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    const rechargeIntensityRaw = self._streamKeeper.getRechargeIntensity(
      application,
      substance,
    );
    const rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    // Get recycling volume
    stateGetter.setVolume(rechargeVolume);
    const recoveryVolumeRaw = self._streamKeeper.getRecoveryRate(
      application,
      substance,
    );
    const recoveryVolume = unitConverter.convert(recoveryVolumeRaw, "kg");
    stateGetter.setVolume(null);

    // Get recycling amount
    stateGetter.setVolume(recoveryVolume);
    const recycledVolumeRaw = self._streamKeeper.getYieldRate(
      application,
      substance,
    );
    const recycledVolume = unitConverter.convert(recycledVolumeRaw, "kg");
    stateGetter.setVolume(null);

    // Return to prior population
    stateGetter.setPopulation(priorPopulation);

    // Get total volume available for new units
    const salesKg = substanceSales.getValue();
    const recycledKg = recycledVolume.getValue();
    const rechargeKg = rechargeVolume.getValue();

    const displacementRateRaw = self._streamKeeper.getDisplacementRate(
      application,
      substance,
    );
    const displacementRate = unitConverter.convert(displacementRateRaw, "%");
    const displacementRateRatio = 1 - displacementRate.getValue() / 100;
    const recycledNonDisplaced = recycledKg * displacementRateRatio;

    const availableForNewUnitsKg = salesKg + recycledNonDisplaced - rechargeKg;

    // Convert to unit delta
    const initialChargeRaw = self.getInitialCharge("sales");
    const initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");
    const initialChargeKgUnit = initialCharge.getValue();
    const deltaUnits = availableForNewUnitsKg / initialChargeKgUnit;

    // Find new total
    const priorPopulationUnits = priorPopulation.getValue();
    const newUnits = priorPopulationUnits + deltaUnits - retireUnits;
    const newUnitsAllowed = newUnits < 0 ? 0 : newUnits;
    const newVolume = new EngineNumber(newUnitsAllowed, "units");

    // Save
    self.setStream("equipment", newVolume, null, scopeEffective, false);
  }

  _recalcEmissions(scope) {
    const self = this;

    const scopeEffective = scope === null || scope === undefined ? self._scope : scope;

    const stateGetter = new OverridingConverterStateGetter(self._stateGetter);
    const unitConverter = new UnitConverter(stateGetter);
    const application = scopeEffective.getApplication();
    const substance = scopeEffective.getSubstance();

    if (application === null || substance === null) {
      throw "Tried recalculating emissions without application and substance.";
    }

    // Determine percent domestic manufacturing
    const manufacturingRaw = self.getStream("manufacture", scopeEffective);
    const manufacturing = unitConverter.convert(manufacturingRaw, "kg");

    // Determine emissions
    stateGetter.setVolume(manufacturing);
    const emissionsRaw = self._streamKeeper.getGhgIntensity(
      application,
      substance,
    );
    const emissions = unitConverter.convert(emissionsRaw, "tCO2e");
    stateGetter.setVolume(null);

    // Ensure in range
    const isNegative = emissions.getValue() < 0;
    const emissionsAllowed = isNegative ? new EngineNumber(0, "tCO2e") : emissions;

    // Save
    self.setStream("emissions", emissionsAllowed, null, scopeEffective, false);
  }

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
    stateGetter.setPopulation(self.getStream("priorEquipment", scopeEffective));
    const rechargePopRaw = self._streamKeeper.getRechargePopulation(
      application,
      substance,
    );
    const rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.setPopulation(null);

    // Switch into recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    const rechargeIntensityRaw = self._streamKeeper.getRechargeIntensity(
      application,
      substance,
    );
    const rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    // Determine initial charge
    const initialChargeRaw = self.getInitialCharge("sales");
    const initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");

    // Get recycling volume
    stateGetter.setVolume(rechargeVolume);
    const recoveryVolumeRaw = self._streamKeeper.getRecoveryRate(
      application,
      substance,
    );
    const recoveryVolume = unitConverter.convert(recoveryVolumeRaw, "kg");
    stateGetter.setVolume(null);

    // Get recycling amount
    stateGetter.setVolume(recoveryVolume);
    const recycledVolumeRaw = self._streamKeeper.getYieldRate(
      application,
      substance,
    );
    const recycledVolume = unitConverter.convert(recycledVolumeRaw, "kg");
    stateGetter.setVolume(null);

    // Get recycling displaced
    const recycledKg = recycledVolume.getValue();

    const displacementRateRaw = self._streamKeeper.getDisplacementRate(
      application,
      substance,
    );
    const displacementRate = unitConverter.convert(displacementRateRaw, "%");
    const displacementRateRatio = displacementRate.getValue() / 100;
    const recycledDisplacedKg = recycledKg * displacementRateRatio;

    // Switch out of recharge population
    stateGetter.setPopulation(null);

    // Determine needs for new equipment deployment.
    stateGetter.setAmortizedUnitVolume(initialCharge);
    const populationChangeRaw = stateGetter.getPopulationChange();
    const retirementRaw = self._streamKeeper.getStream(application, substance, "retirement");
    const populationChange = unitConverter.convert(populationChangeRaw, "units");
    const retirement = unitConverter.convert(retirementRaw, "units");
    const populationChangeOffset = new EngineNumber(
      populationChange.getValue() + retirement.getValue(),
      "units",
    );
    const volumeForNew = unitConverter.convert(populationChangeOffset, "kg");
    stateGetter.setAmortizedUnitVolume(null);

    // Determine sales prior to recycling
    const kgForRecharge = rechargeVolume.getValue();
    const kgForNew = volumeForNew.getValue();
    const kgNoRecycling = kgForRecharge + kgForNew;
    const volumeNoRecycling = new EngineNumber(kgNoRecycling, "kg");

    // Assume this volume for unit conversion
    stateGetter.setVolume(volumeNoRecycling);

    // Return original
    stateGetter.setVolume(null);

    // Determine sales after recycling
    const kgWithRecycling = kgNoRecycling - recycledDisplacedKg;
    const isNegative = kgWithRecycling < 0;
    const kgWithRecyclingAllowed = isNegative ? 0 : kgWithRecycling;
    const totalSales = new EngineNumber(kgWithRecyclingAllowed, "kg");

    // Update import and domestic sales proportionally.
    self.setStream("sales", totalSales, null, scopeEffective, false);
  }
}

export {Engine};
