/**
 * Logic to serialize out snapshots of results from the engine.
 *
 * @license BSD-3-Clause
 */

import {EngineNumber, OverridingConverterStateGetter, UnitConverter} from "engine_number";
import {EngineResultBuilder} from "engine_struct";

/**
 * Decorator around an engine to serialize out results.
 */
class EngineResultSerializer {
  /**
   * Create a new decorator to produce engine result snapshots.
   *
   * @param {Engine} engine - The engine from which results will be serialized.
   * @param {ConverterStateGetter} stateGetter - State getter by which to
   *     access values at the current state that the engine is focusing on.
   *     This will not be modified, only read.
   */
  constructor(engine, stateGetter) {
    const self = this;
    self._engine = engine;
    self._stateGetter = stateGetter;
  }

  /**
   * Serialize the results for an application and substance in a given year.
   *
   * @param {string} application - The name of the application for which a
   *     result should be serialized like commerical refigeration.
   * @param {string} substance - The name of the substance like HFC-134a for
   *     which a result should be serialized.
   * @param {number} year - The year for which a result should be serialized.
   * @returns {EngineResult} - Snapshot of the result in the current engine
   *     state for the given application and substance.
   */
  getResult(application, substance, year) {
    const self = this;

    // Create builder
    const builder = new EngineResultBuilder();
    builder.setApplication(application);
    builder.setSubstance(substance);
    builder.setYear(year);

    // Prepare units
    const stateGetter = new OverridingConverterStateGetter(self._stateGetter);
    const unitConverter = new UnitConverter(stateGetter);

    // Get sales
    const manufactureRaw = self._engine.getStreamRaw(application, substance, "manufacture");
    const importRaw = self._engine.getStreamRaw(application, substance, "import");
    const recycleRaw = self._engine.getStreamRaw(application, substance, "recycle");

    const manufactureValue = unitConverter.convert(manufactureRaw, "kg");
    const importValue = unitConverter.convert(importRaw, "kg");
    const recycleValue = unitConverter.convert(recycleRaw, "kg");
    builder.setRecycleValue(recycleValue);

    // Get total energy consumption
    const energyConsumptionValue = self._engine.getStreamRaw(application, substance, "energy");
    builder.setEnergyConsumption(energyConsumptionValue);

    // Get emissions
    const populationValue = self._engine.getStreamRaw(application, substance, "equipment");
    builder.setPopulationValue(populationValue);

    const populationNew = self._engine.getStreamRaw(application, substance, "newEquipment");
    builder.setPopulationNew(populationNew);

    const rechargeEmissions = self._engine.getStreamRaw(
      application,
      substance,
      "rechargeEmissions",
    );
    const eolEmissions = self._engine.getStreamRaw(application, substance, "eolEmissions");
    builder.setEolEmissions(eolEmissions);

    // Get percent for offset
    const manufactureKg = manufactureValue.getValue();
    const importKg = importValue.getValue();
    const recycleKg = recycleValue.getValue();

    const nonRecycleSalesKg = manufactureKg + importKg;
    const noSales = nonRecycleSalesKg == 0;
    const percentManufacture = noSales ? 1 : manufactureKg / nonRecycleSalesKg;
    const percentImport = 1 - percentManufacture;

    // Offset sales
    const manufactureValueOffset = new EngineNumber(
      manufactureKg - recycleKg * percentManufacture,
      "kg",
    );
    builder.setManufactureValue(manufactureValueOffset);

    const importValueOffset = new EngineNumber(importKg - recycleKg * percentImport, "kg");
    builder.setImportValue(importValueOffset);

    // Get consumption
    const getConsumptionByVolume = () => {
      const consumptionRaw = self._engine.getGhgIntensity(application, substance);
      const endsKg = consumptionRaw.getUnits().endsWith("kg");
      const endsMt = consumptionRaw.getUnits().endsWith("mt");
      if (endsKg || endsMt) {
        return consumptionRaw;
      } else {
        return unitConverter.convert(consumptionRaw, "tCO2e / kg");
      }
    };
    const consumptionByVolume = getConsumptionByVolume();

    const getConsumptionForVolume = (volume) => {
      if (volume.getValue() == 0) {
        return new EngineNumber(0, "tCO2e");
      }

      stateGetter.setVolume(volume);
      return unitConverter.convert(consumptionByVolume, "tCO2e");
    };

    const domesticConsumptionValue = getConsumptionForVolume(manufactureValueOffset);
    builder.setDomesticConsumptionValue(domesticConsumptionValue);

    const importConsumptionValue = getConsumptionForVolume(importValueOffset);
    builder.setImportConsumptionValue(importConsumptionValue);

    const recycleConsumptionValue = getConsumptionForVolume(recycleValue);
    builder.setRecycleConsumptionValue(recycleConsumptionValue);

    // Offset recharge emissions
    stateGetter.setVolume(null);
    const rechargeEmissionsConvert = unitConverter.convert(rechargeEmissions, "tCO2e");
    const rechargeEmissionsOffset = new EngineNumber(
      rechargeEmissionsConvert.getValue() - recycleConsumptionValue.getValue(),
      "tCO2e",
    );
    builder.setRechargeEmissions(rechargeEmissionsOffset);

    // Will put build here instead of new EngineResult which is to be removed
    return builder.build();
  }
}

export {EngineResultSerializer};
