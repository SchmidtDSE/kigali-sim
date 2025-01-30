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
      "tCO2e": (x) => self._toConsumption(x),
      "year": (x) => self._toYears(x),
      "years": (x) => self._toYears(x),
      "%": (x) => self._toPercent(x),
    }[destinationNumeratorUnits];

    const destinationNumerator = numeratorStrategy(source);

    const hasDenominator = destinationUnitPieces.length > 1;
    const destinationDenominatorUnits = hasDenominator ? destinationUnitPieces[1] : "";
    if (hasDenominator) {
      const denominatorStrategy = {
        kg: () => self.convert(self._stateGetter.getVolume(), "kg"),
        mt: () => self.convert(self._stateGetter.getVolume(), "mt"),
        unit: () => self.convert(self._stateGetter.getPopulation(), "unit"),
        units: () => self.convert(self._stateGetter.getPopulation(), "units"),
        tCO2e: () => self.convert(self._stateGetter.getConsumption(), "tCO2e"),
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
      const conversion = self._stateGetter.getSubstanceConsumption();
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
      const conversion = self._stateGetter.getAmortizedUnitConsumption();
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
   * Convert a number to consumption as tCO2e.
   *
   * @param target The EngineNumber to convert.
   * @returns Target converted to consumption as tCO2e.
   */
  _toConsumption(target) {
    const self = this;

    target = self._normalize(target);
    const currentUnits = target.getUnits();

    if (currentUnits === "tCO2e") {
      return target;
    } else if (currentUnits === "kg" || currentUnits === "mt") {
      const conversion = self._stateGetter.getSubstanceConsumption();
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
      const total = self._stateGetter.getConsumption();
      const newValue = total.getValue() * asRatio;
      return new EngineNumber(newValue, "tCO2e");
    } else {
      throw "Unable to convert to consumption: " + currentUnits;
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
      const perYearConsumptionValue = self._stateGetter.getConsumption().getValue();
      const newYears = target.getValue() / perYearConsumptionValue;
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
      const perYearPopulation = self._stateGetter.getPopulationChange(self).getValue();
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
        return self._stateGetter.getConsumption();
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
    const percentValue = (target.getValue() / total.getValue()) * 100;
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
    target = self._normConsumption(target);
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
   * Convert a number where a units ratio has consumption in the denominator to a non-ratio units.
   *
   * @param target The value to normalize by consumption.
   * @returns Target without consumption in its units denominator.
   */
  _normConsumption(target) {
    const self = this;
    const currentUnits = target.getUnits();

    if (!currentUnits.endsWith(" / tCO2e")) {
      return target;
    }

    const originalValue = target.getValue();
    const newUnits = currentUnits.split(" / ")[0];
    const totalConsumption = self._stateGetter.getConsumption();
    const totalConsumptionValue = totalConsumption.getValue();
    const newValue = originalValue * totalConsumptionValue;

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

  getSubstanceConsumption() {
    const self = this;
    const consumption = self.getConsumption();
    const volume = self.getVolume();
    const ratioValue = consumption.getValue() / volume.getValue();

    const consumptionUnits = consumption.getUnits();
    const volumeUnits = volume.getUnits();
    const consumptionUnitsExpected = consumptionUnits === "tCO2e";
    const volumeUnitsExpected = volumeUnits === "mt" || volumeUnits === "kg";
    const unitsExpected = consumptionUnitsExpected && volumeUnitsExpected;
    if (!unitsExpected) {
      throw "Unexpected units for getSubstanceConsumption.";
    }

    const ratioUnits = consumptionUnits + " / " + volumeUnits;
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
    const firstYear = self._engine.getStartYear() == self._engine.getYear();
    return new EngineNumber(firstYear ? 0 : 1, "year");
  }

  getConsumption() {
    const self = this;
    return self._engine.getStream("consumption");
  }

  getVolume() {
    const self = this;
    const sales = self._engine.getStream("sales");
    return sales;
  }

  getAmortizedUnitConsumption() {
    const self = this;
    const consumption = self.getConsumption();
    const population = self.getPopulation();
    const ratioValue = consumption.getValue() / population.getValue();

    const populationUnits = population.getUnits();
    const volumeUnits = volume.getUnits();
    const populationUnitsExpected = populationUnits === "unit" || populationUnits === "units";
    const volumeUnitsExpected = volumeUnits === "mt" || volumeUnits === "kg";
    const unitsExpected = populationUnitsExpected && volumeUnitsExpected;
    if (!unitsExpected) {
      throw "Unexpected units for getAmortizedUnitConsumption.";
    }

    const ratioUnits = consumptionUnits + " / " + populationUnits;
    return new EngineNumber(ratioValue, ratioUnits);
  }

  getPopulationChange(unitConverter) {
    const self = this;

    const priorEquipmentRaw = self._engine.getStream("priorEquipment");
    const newEquipmentRaw = self._engine.getStream("equipment");

    const priorEquipment = unitConverter.convert(priorEquipmentRaw, "units").getValue();
    const newEquipment = unitConverter.convert(newEquipmentRaw, "units").getValue();

    const deltaValue = newEquipment - priorEquipment;
    return new EngineNumber(deltaValue, "units");
  }
}

class OverridingConverterStateGetter {
  constructor(innerGetter) {
    const self = this;
    self._innerGetter = innerGetter;
    self._substanceConsumption = null;
    self._amortizedUnitVolume = null;
    self._population = null;
    self._yearsElapsed = null;
    self._totalConsumption = null;
    self._volume = null;
    self._amortizedUnitConsumption = null;
    self._populationChange = null;
  }

  setTotal(streamName, value) {
    const self = this;
    const strategy = {
      sales: (x) => self.setVolume(x),
      manufacture: (x) => self.setVolume(x),
      import: (x) => self.setVolume(x),
      equipment: (x) => self.setPopulation(x),
      priorEquipment: (x) => self.setPopulation(x),
      consumption: (x) => self.setConsumption(x),
    }[streamName];
    strategy(value);
  }

  setSubstanceConsumption(newValue) {
    const self = this;
    self._substanceConsumption = newValue;
  }

  getSubstanceConsumption() {
    const self = this;
    if (self._substanceConsumption === null) {
      return self._innerGetter.getSubstanceConsumption();
    } else {
      return self._substanceConsumption;
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

  setConsumption(newValue) {
    const self = this;
    self._totalConsumption = newValue;
  }

  getConsumption() {
    const self = this;
    if (self._totalConsumption === null) {
      return self._innerGetter.getConsumption();
    } else {
      return self._totalConsumption;
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

  setAmortizedUnitConsumption(newValue) {
    const self = this;
    self._amortizedUnitConsumption = newValue;
  }

  getAmortizedUnitConsumption() {
    const self = this;
    if (self._amortizedUnitConsumption === null) {
      return self._innerGetter.getAmortizedUnitConsumption();
    } else {
      return self._amortizedUnitConsumption;
    }
  }

  setPopulationChange(newValue) {
    const self = this;
    self._populationChange = newValue;
  }

  getPopulationChange(unitConverter) {
    const self = this;
    if (self._populationChange === null) {
      return self._innerGetter.getPopulationChange(unitConverter);
    } else {
      return self._populationChange;
    }
  }
}

export {EngineNumber, UnitConverter, ConverterStateGetter, OverridingConverterStateGetter};
