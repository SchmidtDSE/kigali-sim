/**
 * Structures for unit conversion logic within the engine.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";

const CONVERT_ZERO_NOOP = true;
const ZERO_EMPTY_VOLUME_INTENSITY = false;

/**
 * Object simplifying conversion between units.
 */
class UnitConverter {
  /**
   * Create a new unit converter.
   *
   * @param stateGetter - Object allowing access to engine state as needed for unit conversion.
   */
  constructor(stateGetter) {
    const self = this;
    self._stateGetter = stateGetter;
  }

  /**
   * Convert a number to new units.
   *
   * @param source - The EngineNumber to convert.
   * @param destinationUnits - The units to which source should be converted.
   */
  convert(source, destinationUnits) {
    const self = this;

    if (source.getUnits() === destinationUnits) {
      return source;
    }

    if (CONVERT_ZERO_NOOP && source.getValue() == 0) {
      return new EngineNumber(0, destinationUnits);
    }

    const sourceUnitPieces = source.getUnits().split(" / ");
    const sourceHasDenominator = sourceUnitPieces.length > 1;
    const sourceDenominatorUnits = sourceHasDenominator ? sourceUnitPieces[1] : "";

    const destinationUnitPieces = destinationUnits.split(" / ");
    const destHasDenominator = destinationUnitPieces.length > 1;
    const destinationDenominatorUnits = destHasDenominator ? destinationUnitPieces[1] : "";

    const sourceNumeratorUnits = sourceUnitPieces[0];
    const destinationNumeratorUnits = destinationUnitPieces[0];
    const differentDenominator = destinationDenominatorUnits !== sourceDenominatorUnits;
    const sameDenominator = !differentDenominator;

    const numeratorStrategy = {
      "kg": (x) => self._toKg(x),
      "mt": (x) => self._toMt(x),
      "unit": (x) => self._toUnits(x),
      "units": (x) => self._toUnits(x),
      "tCO2e": (x) => self._toGhgConsumption(x),
      "kwh": (x) => self._toEnergyConsumption(x),
      "year": (x) => self._toYears(x),
      "years": (x) => self._toYears(x),
      "%": (x) => self._toPercent(x),
    }[destinationNumeratorUnits];

    const denominatorStrategy = {
      "kg": () => self.convert(self._stateGetter.getVolume(), "kg"),
      "mt": () => self.convert(self._stateGetter.getVolume(), "mt"),
      "unit": () => self.convert(self._stateGetter.getPopulation(), "unit"),
      "units": () => self.convert(self._stateGetter.getPopulation(), "units"),
      "tCO2e": () => self.convert(self._stateGetter.getGhgConsumption(), "tCO2e"),
      "kwh": () => self.convert(self._stateGetter.getEnergyConsumption(), "kwh"),
      "year": () => self.convert(self._stateGetter.getYearsElapsed(), "year"),
      "years": () => self.convert(self._stateGetter.getYearsElapsed(), "years"),
      "": () => new EngineNumber(1, ""),
    }[destinationDenominatorUnits];

    if (sourceHasDenominator && sameDenominator) {
      const sourceEffective = new EngineNumber(source.getValue(), sourceNumeratorUnits);
      const convertedNumerator = numeratorStrategy(sourceEffective);
      return new EngineNumber(convertedNumerator.getValue(), destinationUnits);
    } else {
      const numerator = numeratorStrategy(source);
      const denominator = denominatorStrategy(source);

      if (denominator.getValue() == 0) {
        const inferredFactor = self._inferScale(
          sourceDenominatorUnits,
          destinationDenominatorUnits,
        );
        if (inferredFactor !== undefined) {
          return new EngineNumber(numerator.getValue() / inferredFactor, destinationUnits);
        } else if (ZERO_EMPTY_VOLUME_INTENSITY) {
          return new EngineNumber(0, destinationUnits);
        } else {
          throw "Encountered unrecoverable NaN in conversion due to no volume.";
        }
      } else {
        return new EngineNumber(numerator.getValue() / denominator.getValue(), destinationUnits);
      }
    }
  }

  /**
   * Infer a scaling factor without population information.
   *
   * Infer the scale factor for converting between source and destination
   * units without population information.
   *
   * @param {string} source - The source unit type.
   * @param {string} destination - The destination unit type.
   * @returns {number} The scale factor for conversion or undefined if not
   *     found.
   */
  _inferScale(source, destination) {
    return {
      kg: {mt: 1000},
      mt: {kg: 1 / 1000},
      unit: {units: 1},
      units: {unit: 1},
      years: {year: 1},
      year: {years: 1},
    }[source][destination];
  }

  /**
   * Convert a number to kilograms.
   *
   * @private
   * @param target - The EngineNumber to convert.
   * @returns Target converted to kilograms.
   */
  _toKg(target) {
    const self = this;
    const asVolume = self._toVolume(target);
    const currentUnits = asVolume.getUnits();
    if (currentUnits === "mt") {
      return new EngineNumber(asVolume.getValue() * 1000, "kg");
    } else if (currentUnits === "kg") {
      return asVolume;
    } else {
      throw "Unexpected units " + currentUnits;
    }
  }

  /**
   * Convert a number to metric tons.
   *
   * @private
   * @param target - The EngineNumber to convert.
   * @returns Target converted to metric tons.
   */
  _toMt(target) {
    const self = this;
    const asVolume = self._toVolume(target);
    const currentUnits = asVolume.getUnits();
    if (currentUnits === "kg") {
      return new EngineNumber(asVolume.getValue() / 1000, "mt");
    } else if (currentUnits === "mt") {
      return asVolume;
    } else {
      throw "Unexpected units " + currentUnits;
    }
  }

  /**
   * Convert a number to a volume units.
   *
   * @private
   * @param target - The EngineNumber to convert.
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
   * @private
   * @param target - The EngineNumber to convert.
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
   * @private
   * @param target - The EngineNumber to convert.
   * @returns Target converted to consumption as tCO2e.
   */
  _toGhgConsumption(target) {
    const self = this;

    target = self._normalize(target);
    const currentUnits = target.getUnits();

    const currentVolume = currentUnits === "kg" || currentUnits === "mt";
    const currentPop = currentUnits === "unit" || currentUnits === "units";
    const currentInfer = currentVolume || currentPop;

    if (currentUnits === "tCO2e") {
      return target;
    } else if (currentInfer) {
      const conversion = self._stateGetter.getSubstanceConsumption();
      const conversionValue = conversion.getValue();
      const conversionUnitPieces = conversion.getUnits().split(" / ");
      const newUnits = conversionUnitPieces[0];
      const expectedUnits = conversionUnitPieces[1];
      const targetConverted = self.convert(target, expectedUnits);
      const originalValue = targetConverted.getValue();
      const newValue = originalValue * conversionValue;
      if (newUnits !== "tCO2e") {
        throw "Unexpected units " + newUnits;
      }
      return new EngineNumber(newValue, newUnits);
    } else if (currentUnits === "%") {
      const originalValue = target.getValue();
      const asRatio = originalValue / 100;
      const total = self._stateGetter.getGhgConsumption();
      const newValue = total.getValue() * asRatio;
      return new EngineNumber(newValue, "tCO2e");
    } else {
      throw "Unable to convert to consumption: " + currentUnits;
    }
  }

  /**
   * Convert a number to energy consumption as kwh.
   *
   * @private
   * @param target - The EngineNumber to convert.
   * @returns Target converted to energy consumption as kwh.
   */
  _toEnergyConsumption(target) {
    const self = this;

    target = self._normalize(target);
    const currentUnits = target.getUnits();

    const currentVolume = currentUnits === "kg" || currentUnits === "mt";
    const currentPop = currentUnits === "unit" || currentUnits === "units";
    const currentInfer = currentVolume || currentPop;

    if (currentUnits === "kwh") {
      return target;
    } else if (currentInfer) {
      const conversion = self._stateGetter.getEnergyIntensity();
      const conversionValue = conversion.getValue();
      const conversionUnitPieces = conversion.getUnits().split(" / ");
      const newUnits = conversionUnitPieces[0];
      const expectedUnits = conversionUnitPieces[1];
      const targetConverted = self.convert(target, expectedUnits);
      const originalValue = targetConverted.getValue();
      const newValue = originalValue * conversionValue;
      if (newUnits !== "kwh") {
        throw "Unexpected units " + newUnits;
      }
      return new EngineNumber(newValue, newUnits);
    } else if (currentUnits === "%") {
      const originalValue = target.getValue();
      const asRatio = originalValue / 100;
      const total = self._stateGetter.getEnergyConsumption();
      const newValue = total.getValue() * asRatio;
      return new EngineNumber(newValue, "kwh");
    } else {
      throw "Unable to convert to energy consumption: " + currentUnits;
    }
  }

  /**
   * Convert a number to years.
   *
   * @private
   * @param target - The EngineNumber to convert.
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
      const perYearConsumptionValue = self._stateGetter.getGhgConsumption().getValue();
      const newYears = target.getValue() / perYearConsumptionValue;
      return new EngineNumber(newYears, "years");
    } else if (currentUnits === "kwh") {
      const perYearConsumptionValue = self._stateGetter.getEnergyConsumption().getValue();
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

  /**
   * Convert a number to percentage.
   *
   * @private
   * @param target - The EngineNumber to convert.
   * @returns Target converted to percentage.
   */
  _toPercent(target) {
    const self = this;

    target = self._normalize(target);
    const currentUnits = target.getUnits();

    const getTotal = () => {
      if (currentUnits === "years" || currentUnits === "year") {
        return self._stateGetter.getYearsElapsed();
      } else if (currentUnits === "tCO2e") {
        return self._stateGetter.getGhgConsumption();
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
   * @param target - The number to convert from a units with ratio to single type
   *     units.
   * @returns Number after conversion to non-ratio units or target unchanged if
   *     it does not have a ratio units or could not be normalized.
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
   * Convert a number where a units ratio has population in the denominator to
   * a non-ratio units.
   *
   * @private
   * @param target - The value to normalize by population.
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
   * @private
   * @param target - The value to normalize by time.
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
   * Convert a number where a units ratio has consumption in the denominator to
   * a non-ratio units.
   *
   * @private
   * @param target - The value to normalize by consumption.
   * @returns Target without consumption in its units denominator.
   */
  _normConsumption(target) {
    const self = this;
    const currentUnits = target.getUnits();

    const isCo2 = currentUnits.endsWith(" / tCO2e");
    const isKwh = currentUnits.endsWith(" / kwh");
    if (!isCo2 && !isKwh) {
      return target;
    }

    const getTargetConsumption = () => {
      if (isCo2) {
        return self._stateGetter.getGhgConsumption();
      } else {
        return self._stateGetter.getEnergyConsumption();
      }
    };

    const originalValue = target.getValue();
    const newUnits = currentUnits.split(" / ")[0];
    const totalConsumption = getTargetConsumption();
    const totalConsumptionValue = totalConsumption.getValue();
    const newValue = originalValue * totalConsumptionValue;

    return new EngineNumber(newValue, newUnits);
  }

  /**
   * Convert a number where a units ratio has volume in the denominator to a
   * non-ratio units.
   *
   * @private
   * @param target - The value to normalize by volume.
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

export {UnitConverter};
