/**
 * Representation of a number with units within the engine.
 */
class EngineNumber {
  /**
   * Create a new number with units.
   *
   * @param value The numeric value (float, or int).
   * @param units The units to associate with this value like kg.
   */
  constructor(value, units) {
    const self = this;
    self._value = value;
    self._units = units;
  }

  /**
   * Get the value of this number.
   *
   * @returns Value as an integer or float.
   */
  getValue() {
    const self = this;
    return self._value;
  }

  /**
   * Get the units associated with this number.
   *
   * @returns The units as a string like "mt".
   */
  getUnits() {
    const self = this;
    return self._units;
  }
}

/**
 * Object simplifying conversion between units.
 */
class UnitConverter {
  /**
   * Create a new unit converter.
   *
   * @param stateGetter Object allowing access to engine state as needed for unit conversion.
   */
  constructor(stateGetter) {
    const self = this;
    self._stateGetter = stateGetter;
  }

  /**
   * Convert a number to new units.
   *
   * @param source The EngineNumber to convert.
   * @param destinationUnits The units to which source should be converted.
   */
  convert(source, destinationUnits) {
    const self = this;

    if (source.getUnits() === destinationUnits) {
      return source;
    }

    const destinationUnitPieces = destinationUnits.split(" / ");
    const destinationNumeratorUnits = destinationUnitPieces[0];

    const numeratorStrategy = {
      "kg": (x) => self._toKg(x),
      "mt": (x) => self._toMt(x),
      "unit": (x) => self._toUnits(x),
      "units": (x) => self._toUnits(x),
      "tCO2e": (x) => self._toEmissions(x),
      "year": (x) => self._toYears(x),
      "years": (x) => self._toYears(x),
      "%": (x) => self._toPercent(x),
    }[destinationNumeratorUnits];

    const destinationNumerator = numeratorStrategy(source);

    const hasDenominator = destinationUnitPieces.length > 1;
    const destinationDenominatorUnits = hasDenominator ?
      destinationUnitPieces[1] :
      "";
    if (hasDenominator) {
      const denominatorStrategy = {
        kg: () => self.convert(self._stateGetter.getVolume(), "kg"),
        mt: () => self.convert(self._stateGetter.getVolume(), "mt"),
        unit: () => self.convert(self._stateGetter.getPopulation(), "unit"),
        units: () => self.convert(self._stateGetter.getPopulation(), "units"),
        tCO2e: () => self.convert(self._stateGetter.getEmissions(), "tCO2e"),
        year: () => self.convert(self._stateGetter.getYearsElapsed(), "year"),
        years: () => self.convert(self._stateGetter.getYearsElapsed(), "years"),
      }[destinationDenominatorUnits];
      const destinationDenominator = denominatorStrategy();
      return new EngineNumber(
        destinationNumerator.getValue() / destinationDenominator.getValue(),
        destinationUnits,
      );
    } else {
      return destinationNumerator;
    }
  }

  /**
   * Convert a number to kilograms.
   *
   * @param target The EngineNumber to convert.
   * @returns Target converted to kilograms.
   */
  _toKg(target) {
    const self = this;
    const asVolume = self._toVolume(target);
    const currentUnits = asVolume.getUnits();
    if (currentUnits === "mt") {
      return new EngineNumber(asVolume.getValue() * 1000, "kg");
    } else {
      return asVolume;
    }
  }

  /**
   * Convert a number to metric tons.
   *
   * @param target The EngineNumber to convert.
   * @returns Target converted to metric tons.
   */
  _toMt(target) {
    const self = this;
    const asVolume = self._toVolume(target);
    const currentUnits = asVolume.getUnits();
    if (currentUnits === "kg") {
      return new EngineNumber(asVolume.getValue() / 1000, "mt");
    } else {
      return asVolume;
    }
  }

  /**
   * Convert a number to a volume units.
   *
   * @param target The EngineNumber to convert.
   * @returns Target converted to kilograms or metric tons.
   */
  _toVolume(target) {
    const self = this;

    target = self._normalize(target);
    const currentUnits = target.getUnits();

    if (currentUnits === "mt" || currentUnits === "kg") {
      return target;
    } else if (currentUnits === "tCO2e") {
      const originalValue = target.getValue();
      const conversion = self._stateGetter.getSubstanceEmissions();
      const conversionValue = conversion.getValue();
      const newUnits = conversion.getUnits().split(" / ")[1];
      const newValue = originalValue / conversionValue;
      return new EngineNumber(newValue, newUnits);
    } else if (currentUnits === "unit" || currentUnits === "units") {
      const originalValue = target.getValue();
      const conversion = self._stateGetter.getAmortizedUnitVolume();
      const conversionValue = conversion.getValue();
      const newUnits = conversion.getUnits().split(" / ")[0];
      const newValue = originalValue * conversionValue;
      return new EngineNumber(newValue, newUnits);
    } else if (currentUnits === "%") {
      const originalValue = target.getValue();
      const asRatio = originalValue / 100;
      const total = self._stateGetter.getVolume();
      const newUnits = total.getUnits();
      const newValue = total.getValue() * asRatio;
      return new EngineNumber(newValue, newUnits);
    } else {
      throw "Unable to convert to volume: " + currentUnits;
    }
  }

  /**
   * Convert a number to units (population).
   *
   * @param target The EngineNumber to convert.
   * @returns Target converted to units (population).
   */
  _toUnits(target) {
    const self = this;

    target = self._normalize(target);
    const currentUnits = target.getUnits();

    if (currentUnits === "units") {
      return target;
    } else if (currentUnits === "unit") {
      return new EngineNumber(target.getValue(), "units");
    } else if (currentUnits === "kg" || currentUnits === "mt") {
      const conversion = self._stateGetter.getAmortizedUnitVolume();
      const conversionValue = conversion.getValue();
      const conversionUnitPieces = conversion.getUnits().split(" / ");
      const expectedUnits = conversionUnitPieces[0];
      const newUnits = conversionUnitPieces[1];
      const targetConverted = self.convert(target, expectedUnits);
      const originalValue = targetConverted.getValue();
      const newValue = originalValue / conversionValue;
      return new EngineNumber(newValue, "units");
    } else if (currentUnits === "tCO2e") {
      const originalValue = target.getValue();
      const conversion = self._stateGetter.getAmortizedUnitEmissions();
      const conversionValue = conversion.getValue();
      const newValue = originalValue / conversionValue;
      return new EngineNumber(newValue, "units");
    } else if (currentUnits === "%") {
      const originalValue = target.getValue();
      const asRatio = originalValue / 100;
      const total = self._stateGetter.getPopulation();
      const newValue = total.getValue() * asRatio;
      return new EngineNumber(newValue, "units");
    } else {
      throw "Unable to convert to pouplation: " + currentUnits;
    }
  }

  /**
   * Convert a number to emissions as tCO2e.
   *
   * @param target The EngineNumber to convert.
   * @returns Target converted to emissions as tCO2e.
   */
  _toEmissions(target) {
    const self = this;

    target = self._normalize(target);
    const currentUnits = target.getUnits();

    if (currentUnits === "tCO2e") {
      return target;
    } else if (currentUnits === "kg" || currentUnits === "mt") {
      const conversion = self._stateGetter.getSubstanceEmissions();
      const conversionValue = conversion.getValue();
      const conversionUnitPieces = conversion.getUnits().split(" / ");
      const newUnits = conversionUnitPieces[0];
      const expectedUnits = conversionUnitPieces[1];
      const targetConverted = self.convert(target, expectedUnits);
      const originalValue = targetConverted.getValue();
      const newValue = originalValue * conversionValue;
      return new EngineNumber(newValue, newUnits);
    } else if (currentUnits === "unit" || currentUnits === "units") {
      const originalValue = target.getValue();
      const conversion = self._stateGetter.getAmortizedUnitVolume();
      const conversionValue = conversion.getValue();
      const newValue = originalValue * conversionValue;
      return new EngineNumber(newValue, "tCO2e");
    } else if (currentUnits === "%") {
      const originalValue = target.getValue();
      const asRatio = originalValue / 100;
      const total = self._stateGetter.getEmissions();
      const newValue = total.getValue() * asRatio;
      return new EngineNumber(newValue, "tCO2e");
    } else {
      throw "Unable to convert to emissions: " + currentUnits;
    }
  }

  /**
   * Convert a number to years.
   *
   * @param target The EngineNumber to convert.
   * @returns Target converted to years.
   */
  _toYears(target) {
    const self = this;

    target = self._normalize(target);
    const currentUnits = target.getUnits();

    if (currentUnits === "years") {
      return target;
    } else if (currentUnits === "year") {
      return new EngineNumber(target.getValue(), "years");
    } else if (currentUnits === "tCO2e") {
      const perYearEmissionsValue = self._stateGetter.getEmissions().getValue();
      const newYears = target.getValue() / perYearEmissionsValue;
      return new EngineNumber(newYears, "years");
    } else if (currentUnits === "kg" || currentUnits === "mt") {
      const perYearVolume = self._stateGetter.getVolume();
      const perYearVolumeUnits = perYearVolume.getUnits();
      const perYearVolumeValue = perYearVolume.getValue();
      const volumeConverted = self.convert(target, perYearVolumeUnits);
      const volumeConvertedValue = volumeConverted.getValue();
      const newYears = volumeConvertedValue / perYearVolumeValue;
      return new EngineNumber(newYears, "years");
    } else if (currentUnits === "unit" || currentUnits === "units") {
      const perYearPopulation = self._stateGetter
        .getPopulationChange()
        .getValue();
      const newYears = target.getValue() / perYearPopulation;
      return new EngineNumber(newYears, "years");
    } else if (currentUnits === "%") {
      const originalValue = target.getValue();
      const asRatio = originalValue / 100;
      const total = self._stateGetter.getYearsElapsed();
      const newValue = total.getValue() * asRatio;
      return new EngineNumber(newValue, "years");
    } else {
      throw "Unable to convert to years: " + currentUnits;
    }
  }

  _toPercent(target) {
    const self = this;

    target = self._normalize(target);
    const currentUnits = target.getUnits();

    const getTotal = () => {
      if (currentUnits === "years" || currentUnits === "year") {
        return self._stateGetter.getYearsElapsed();
      } else if (currentUnits === "tCO2e") {
        return self._stateGetter.getEmissions();
      } else if (currentUnits === "kg" || currentUnits === "mt") {
        const volume = self._stateGetter.getVolume();
        return self.convert(volume, currentUnits);
      } else if (currentUnits === "unit" || currentUnits === "units") {
        return self._stateGetter.getPopulation();
      } else {
        throw "Unable to convert to %: " + currentUnits;
      }
    };

    const total = getTotal();
    const percentValue = target.getValue() / total.getValue() * 100;
    return new EngineNumber(percentValue, "%");
  }

  /**
   * Normalize to non-ratio units if possible.
   *
   * @param target The number to convert from a units with ratio to single type units.
   * @returns Number after conversion to non-ratio units or target unchanged if it does not have a
   *    ratio units or could not be normalized.
   */
  _normalize(target) {
    const self = this;
    target = self._normUnits(target);
    target = self._normTime(target);
    target = self._normEmissions(target);
    target = self._normVolume(target);
    return target;
  }

  /**
   * Convert a number where a units ratio has population in the denominator to a non-ratio units.
   *
   * @param target The value to normalize by population.
   * @returns Target without population in its units denominator.
   */
  _normUnits(target) {
    const self = this;
    const currentUnits = target.getUnits();

    const divUnit = currentUnits.endsWith("/ unit");
    const divUnits = currentUnits.endsWith("/ units");
    const isPerUnit = divUnit || divUnits;

    if (!isPerUnit) {
      return target;
    }

    const originalValue = target.getValue();
    const newUnits = currentUnits.split(" / ")[0];
    const population = self._stateGetter.getPopulation();
    const populationValue = population.getValue();
    const newValue = originalValue * populationValue;

    return new EngineNumber(newValue, newUnits);
  }

  /**
   * Convert a number where a units ratio has time in the denominator to a non-ratio units.
   *
   * @param target The value to normalize by time.
   * @returns Target without time in its units denominator.
   */
  _normTime(target) {
    const self = this;
    const currentUnits = target.getUnits();

    if (!currentUnits.endsWith(" / year")) {
      return target;
    }

    const originalValue = target.getValue();
    const newUnits = currentUnits.split(" / ")[0];
    const years = self._stateGetter.getYearsElapsed();
    const yearsValue = years.getValue();
    const newValue = originalValue * yearsValue;

    return new EngineNumber(newValue, newUnits);
  }

  /**
   * Convert a number where a units ratio has emissions in the denominator to a non-ratio units.
   *
   * @param target The value to normalize by emissions.
   * @returns Target without emissions in its units denominator.
   */
  _normEmissions(target) {
    const self = this;
    const currentUnits = target.getUnits();

    if (!currentUnits.endsWith(" / tCO2e")) {
      return target;
    }

    const originalValue = target.getValue();
    const newUnits = currentUnits.split(" / ")[0];
    const totalEmissions = self._stateGetter.getEmissions();
    const totalEmissionsValue = totalEmissions.getValue();
    const newValue = originalValue * totalEmissionsValue;

    return new EngineNumber(newValue, newUnits);
  }

  /**
   * Convert a number where a units ratio has volume in the denominator to a non-ratio units.
   *
   * @param target The value to normalize by volume.
   * @returns Target without volume in its units denominator.
   */
  _normVolume(target) {
    const self = this;

    const targetUnits = target.getUnits();

    const divKg = targetUnits.endsWith(" / kg");
    const divMt = targetUnits.endsWith(" / mt");
    const needsNorm = divKg || divMt;
    if (!needsNorm) {
      return target;
    }

    const targetUnitPieces = targetUnits.split(" / ");
    const newUnits = targetUnitPieces[0];
    const expectedUnits = targetUnitPieces[1];

    const volume = self._stateGetter.getVolume();
    const volumeConverted = self.convert(volume, expectedUnits);
    const conversionValue = volumeConverted.getValue();

    const originalValue = target.getValue();
    const newValue = originalValue * conversionValue;

    return new EngineNumber(newValue, newUnits);
  }
}

class ConverterStateGetter {
  constructor(engine) {
    const self = this;
    self._engine = engine;
  }

  getSubstanceEmissions() {
    const self = this;
    const emissions = self.getEmissions();
    const volume = self.getVolume();
    const ratioValue = emissions.getValue() / volume.getValue();

    const emissionsUnits = emissions.getUnits();
    const volumeUnits = volume.getUnits();
    const emissionsUnitsExpected = emissionsUnits === "tCO2e";
    const volumeUnitsExpected = volumeUnits === "mt" || volumeUnits === "kg";
    const unitsExpected = emissionsUnitsExpected && volumeUnitsExpected;
    if (!unitsExpected) {
      throw "Unexpected units for getSubstanceEmissions.";
    }

    const ratioUnits = emissionsUnits + " / " + volumeUnits;
    return new EngineNumber(ratioValue, ratioUnits);
  }

  getAmortizedUnitVolume() {
    const self = this;
    return self._engine.getInitialCharge("sales");
  }

  getPopulation() {
    const self = this;
    return self._engine.getStream("equipment");
  }

  getYearsElapsed() {
    const self = this;
    return new EngineNumber(1, "year");
  }

  getEmissions() {
    const self = this;
    return self._engine.getStream("emissions");
  }

  getVolume() {
    const self = this;
    const sales = self._engine.getStream("sales"); ;
    return sales;
  }

  getAmortizedUnitEmissions() {
    const self = this;
    const emissions = self.getEmissions();
    const population = self.getPopulation();
    const ratioValue = emissions.getValue() / population.getValue();

    const populationUnits = population.getUnits();
    const volumeUnits = volume.getUnits();
    const populationUnitsExpected = populationUnits === "unit" || populationUnits === "units";
    const volumeUnitsExpected = volumeUnits === "mt" || volumeUnits === "kg";
    const unitsExpected = populationUnitsExpected && volumeUnitsExpected;
    if (!unitsExpected) {
      throw "Unexpected units for getAmortizedUnitEmissions.";
    }

    const ratioUnits = emissionsUnits + " / " + populationUnits;
    return new EngineNumber(ratioValue, ratioUnits);
  }

  getPopulationChange() {
    const self = this;
    const priorEquipment = self._engine.getStream("priorEquipment");
    const newEquipment = self._engine.getStream("equipment");
    const deltaValue = newEquipment.getValue() - priorEquipment.getValue();
    return new EngineNumber(deltaValue, "units");
  }
}

class OverridingConverterStateGetter {
  constructor(innerGetter) {
    const self = this;
    self._innerGetter = innerGetter;
    self._substanceEmissions = null;
    self._amortizedUnitVolume = null;
    self._population = null;
    self._yearsElapsed = null;
    self._totalEmissions = null;
    self._volume = null;
    self._amortizedUnitEmissions = null;
    self._populationChange = null;
  }

  setTotal(streamName, value) {
    const self = this;
    const strategy = {
      "sales": (x) => self.setVolume(x),
      "manufacture": (x) => self.setVolume(x),
      "import": (x) => self.setVolume(x),
      "equipment": (x) => self.setPopulation(x),
      "priorEquipment": (x) => self.setPopulation(x),
      "emissions": (x) => self.setEmissions(x),
    }[streamName];
    strategy(value);
  }

  setSubstanceEmissions(newValue) {
    const self = this;
    self._substanceEmissions = newValue;
  }

  getSubstanceEmissions() {
    const self = this;
    if (self._substanceEmissions === null) {
      return self._innerGetter.getSubstanceEmissions();
    } else {
      return self._substanceEmissions;
    }
  }

  setAmortizedUnitVolume(newValue) {
    const self = this;
    self._amortizedUnitVolume = newValue;
  }

  getAmortizedUnitVolume() {
    const self = this;
    if (self._amortizedUnitVolume === null) {
      return self._innerGetter.getAmortizedUnitVolume();
    } else {
      return self._amortizedUnitVolume;
    }
  }

  setPopulation(newValue) {
    const self = this;
    self._population = newValue;
  }

  getPopulation() {
    const self = this;
    if (self._population === null) {
      return self._innerGetter.getPopulation();
    } else {
      return self._population;
    }
  }

  setYearsElapsed(newValue) {
    const self = this;
    self._yearsElapsed = newValue;
  }

  getYearsElapsed() {
    const self = this;
    if (self._yearsElapsed === null) {
      return self._innerGetter.getYearsElapsed();
    } else {
      return self._yearsElapsed;
    }
  }

  setEmissions(newValue) {
    const self = this;
    self._totalEmissions = newValue;
  }

  getEmissions() {
    const self = this;
    if (self._totalEmissions === null) {
      return self._innerGetter.getEmissions();
    } else {
      return self._totalEmissions;
    }
  }

  setVolume(newValue) {
    const self = this;
    self._volume = newValue;
  }

  getVolume() {
    const self = this;
    if (self._volume === null) {
      return self._innerGetter.getVolume();
    } else {
      return self._volume;
    }
  }

  setAmortizedUnitEmissions(newValue) {
    const self = this;
    self._amortizedUnitEmissions = newValue;
  }

  getAmortizedUnitEmissions() {
    const self = this;
    if (self._amortizedUnitEmissions === null) {
      return self._innerGetter.getAmortizedUnitEmissions();
    } else {
      return self._amortizedUnitEmissions;
    }
  }

  setPopulationChange(newValue) {
    const self = this;
    self._populationChange = newValue;
  }

  getPopulationChange() {
    const self = this;
    if (self._populationChange === null) {
      return self._innerGetter.getPopulationChange();
    } else {
      return self._populationChange;
    }
  }
}

export {
  EngineNumber,
  UnitConverter,
  ConverterStateGetter,
  OverridingConverterStateGetter,
};
