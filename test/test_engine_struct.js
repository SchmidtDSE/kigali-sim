/**
 * Tests for simulation result structures and builders.
 *
 * @license BSD-3-Clause
 */

import {
  EngineResult,
  AttributeToExporterResult,
  TradeSupplement,
  EngineResultBuilder,
  AggregatedResult,
} from "engine_struct";

import {EngineNumber} from "engine_number";

function buildEngineStructTests() {
  QUnit.module("EngineResult", function () {
    const makeExample = () => {
      const tradeSupplement = new TradeSupplement(
        new EngineNumber(5, "kg"),
        new EngineNumber(10, "tCO2e"),
        new EngineNumber(2, "units"),
        new EngineNumber(3, "kg"),
        new EngineNumber(6, "tCO2e"),
      );

      return new EngineResult(
        "test app",
        "test substance",
        2023,
        "test scenario",
        1,
        new EngineNumber(100, "kg"),
        new EngineNumber(50, "kg"),
        new EngineNumber(30, "kg"), // export value
        new EngineNumber(25, "kg"),
        new EngineNumber(200, "tCO2e"),
        new EngineNumber(100, "tCO2e"),
        new EngineNumber(60, "tCO2e"), // export consumption
        new EngineNumber(50, "tCO2e"),
        new EngineNumber(1000, "units"),
        new EngineNumber(100, "units"),
        new EngineNumber(300, "tCO2e"),
        new EngineNumber(150, "tCO2e"),
        new EngineNumber(500, "kWh"),
        tradeSupplement,
      );
    };

    QUnit.test("initializes", function (assert) {
      const result = makeExample();
      assert.notDeepEqual(result, undefined);
    });

    QUnit.test("getApplication", function (assert) {
      const result = makeExample();
      assert.deepEqual(result.getApplication(), "test app");
    });

    QUnit.test("getSubstance", function (assert) {
      const result = makeExample();
      assert.deepEqual(result.getSubstance(), "test substance");
    });

    QUnit.test("getYear", function (assert) {
      const result = makeExample();
      assert.equal(result.getYear(), 2023);
    });

    QUnit.test("getManufacture", function (assert) {
      const result = makeExample();
      const manufacture = result.getManufacture();
      assert.closeTo(manufacture.getValue(), 100, 0.0001);
      assert.deepEqual(manufacture.getUnits(), "kg");
    });

    QUnit.test("getImport", function (assert) {
      const result = makeExample();
      const importValue = result.getImport();
      assert.closeTo(importValue.getValue(), 50, 0.0001);
      assert.deepEqual(importValue.getUnits(), "kg");
    });

    QUnit.test("getRecycle", function (assert) {
      const result = makeExample();
      const recycle = result.getRecycle();
      assert.closeTo(recycle.getValue(), 25, 0.0001);
      assert.deepEqual(recycle.getUnits(), "kg");
    });

    QUnit.test("getConsumptionNoRecycle", function (assert) {
      const result = makeExample();
      const consumption = result.getConsumptionNoRecycle();
      assert.closeTo(consumption.getValue(), 300, 0.0001); // 200 + 100
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getGhgConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getGhgConsumption();
      assert.closeTo(consumption.getValue(), 350, 0.0001); // 200 + 100 + 50
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getDomesticConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getDomesticConsumption();
      assert.closeTo(consumption.getValue(), 200, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getImportConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getImportConsumption();
      assert.closeTo(consumption.getValue(), 100, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getRecycleConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getRecycleConsumption();
      assert.closeTo(consumption.getValue(), 50, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getPopulation", function (assert) {
      const result = makeExample();
      const population = result.getPopulation();
      assert.closeTo(population.getValue(), 1000, 0.0001);
      assert.deepEqual(population.getUnits(), "units");
    });

    QUnit.test("getPopulationNew", function (assert) {
      const result = makeExample();
      const populationNew = result.getPopulationNew();
      assert.closeTo(populationNew.getValue(), 100, 0.0001);
      assert.deepEqual(populationNew.getUnits(), "units");
    });

    QUnit.test("getRechargeEmissions", function (assert) {
      const result = makeExample();
      const emissions = result.getRechargeEmissions();
      assert.closeTo(emissions.getValue(), 300, 0.0001);
      assert.deepEqual(emissions.getUnits(), "tCO2e");
    });

    QUnit.test("getEolEmissions", function (assert) {
      const result = makeExample();
      const emissions = result.getEolEmissions();
      assert.closeTo(emissions.getValue(), 150, 0.0001);
      assert.deepEqual(emissions.getUnits(), "tCO2e");
    });

    QUnit.test("getEnergyConsumption", function (assert) {
      const result = makeExample();
      const energy = result.getEnergyConsumption();
      assert.closeTo(energy.getValue(), 500, 0.0001);
      assert.deepEqual(energy.getUnits(), "kWh");
    });

    QUnit.test("getImportSupplement", function (assert) {
      const result = makeExample();
      const supplement = result.getImportSupplement();
      assert.notDeepEqual(supplement, undefined);
      assert.deepEqual(supplement.constructor.name, "ImportSupplement");
    });

    QUnit.test("getScenarioName", function (assert) {
      const result = makeExample();
      assert.deepEqual(result.getScenarioName(), "test scenario");
    });

    QUnit.test("getTrialNumber", function (assert) {
      const result = makeExample();
      assert.equal(result.getTrialNumber(), 1);
    });
  });

  QUnit.module("AttributeToExporterResult", function () {
    const makeInnerResult = () => {
      const tradeSupplement = new TradeSupplement(
        new EngineNumber(5, "kg"),
        new EngineNumber(10, "tCO2e"),
        new EngineNumber(2, "units"),
        new EngineNumber(3, "kg"),
        new EngineNumber(6, "tCO2e"),
      );

      return new EngineResult(
        "test app",
        "test substance",
        2023,
        "test scenario",
        1,
        new EngineNumber(100, "kg"),
        new EngineNumber(50, "kg"),
        new EngineNumber(30, "kg"), // export value
        new EngineNumber(25, "kg"),
        new EngineNumber(200, "tCO2e"),
        new EngineNumber(100, "tCO2e"),
        new EngineNumber(60, "tCO2e"), // export consumption
        new EngineNumber(50, "tCO2e"),
        new EngineNumber(1000, "units"),
        new EngineNumber(100, "units"),
        new EngineNumber(300, "tCO2e"),
        new EngineNumber(150, "tCO2e"),
        new EngineNumber(500, "kWh"),
        tradeSupplement,
      );
    };

    const makeExample = () => {
      return new AttributeToExporterResult(makeInnerResult());
    };

    QUnit.test("initializes", function (assert) {
      const result = makeExample();
      assert.notDeepEqual(result, undefined);
    });

    QUnit.test("getApplication", function (assert) {
      const result = makeExample();
      assert.deepEqual(result.getApplication(), "test app");
    });

    QUnit.test("getSubstance", function (assert) {
      const result = makeExample();
      assert.deepEqual(result.getSubstance(), "test substance");
    });

    QUnit.test("getYear", function (assert) {
      const result = makeExample();
      assert.equal(result.getYear(), 2023);
    });

    QUnit.test("getManufacture", function (assert) {
      const result = makeExample();
      const manufacture = result.getManufacture();
      assert.closeTo(manufacture.getValue(), 100, 0.0001);
      assert.deepEqual(manufacture.getUnits(), "kg");
    });

    QUnit.test("getImport", function (assert) {
      const result = makeExample();
      const importValue = result.getImport();
      // Should be 50 - 5 = 45 due to exporter attribution
      assert.closeTo(importValue.getValue(), 45, 0.0001);
      assert.deepEqual(importValue.getUnits(), "kg");
    });

    QUnit.test("getImportConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getImportConsumption();
      // Should be 100 - 10 = 90 due to exporter attribution
      assert.closeTo(consumption.getValue(), 90, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getRecycle", function (assert) {
      const result = makeExample();
      const recycle = result.getRecycle();
      assert.closeTo(recycle.getValue(), 25, 0.0001);
      assert.deepEqual(recycle.getUnits(), "kg");
    });

    QUnit.test("getDomesticConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getDomesticConsumption();
      assert.closeTo(consumption.getValue(), 200, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });
  });

  QUnit.module("TradeSupplement", function () {
    const makeExample = () => {
      return new TradeSupplement(
        new EngineNumber(5, "kg"),
        new EngineNumber(10, "tCO2e"),
        new EngineNumber(2, "units"),
        new EngineNumber(3, "kg"),
        new EngineNumber(6, "tCO2e"),
      );
    };

    QUnit.test("initializes", function (assert) {
      const supplement = makeExample();
      assert.notDeepEqual(supplement, undefined);
    });

    QUnit.test("getImportInitialChargeValue", function (assert) {
      const supplement = makeExample();
      const value = supplement.getImportInitialChargeValue();
      assert.closeTo(value.getValue(), 5, 0.0001);
      assert.deepEqual(value.getUnits(), "kg");
    });

    QUnit.test("getImportInitialChargeConsumption", function (assert) {
      const supplement = makeExample();
      const consumption = supplement.getImportInitialChargeConsumption();
      assert.closeTo(consumption.getValue(), 10, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getImportPopulation", function (assert) {
      const supplement = makeExample();
      const population = supplement.getImportPopulation();
      assert.closeTo(population.getValue(), 2, 0.0001);
      assert.deepEqual(population.getUnits(), "units");
    });

    QUnit.test("getExportInitialChargeValue", function (assert) {
      const supplement = makeExample();
      const value = supplement.getExportInitialChargeValue();
      assert.closeTo(value.getValue(), 3, 0.0001);
      assert.deepEqual(value.getUnits(), "kg");
    });

    QUnit.test("getExportInitialChargeConsumption", function (assert) {
      const supplement = makeExample();
      const consumption = supplement.getExportInitialChargeConsumption();
      assert.closeTo(consumption.getValue(), 6, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    // Test legacy methods
    QUnit.test("getInitialChargeValue (legacy)", function (assert) {
      const supplement = makeExample();
      const value = supplement.getInitialChargeValue();
      assert.closeTo(value.getValue(), 5, 0.0001);
      assert.deepEqual(value.getUnits(), "kg");
    });

    QUnit.test("getInitialChargeConsumption (legacy)", function (assert) {
      const supplement = makeExample();
      const consumption = supplement.getInitialChargeConsumption();
      assert.closeTo(consumption.getValue(), 10, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getNewPopulation (legacy)", function (assert) {
      const supplement = makeExample();
      const population = supplement.getNewPopulation();
      assert.closeTo(population.getValue(), 2, 0.0001);
      assert.deepEqual(population.getUnits(), "units");
    });
  });

  QUnit.module("EngineResultBuilder", function () {
    const makeExampleBuilder = () => {
      const builder = new EngineResultBuilder();
      const tradeSupplement = new TradeSupplement(
        new EngineNumber(5, "kg"),
        new EngineNumber(10, "tCO2e"),
        new EngineNumber(2, "units"),
        new EngineNumber(3, "kg"),
        new EngineNumber(6, "tCO2e"),
      );

      builder.setApplication("test app");
      builder.setSubstance("test substance");
      builder.setYear(2023);
      builder.setScenarioName("test scenario");
      builder.setTrialNumber(1);
      builder.setManufactureValue(new EngineNumber(100, "kg"));
      builder.setImportValue(new EngineNumber(50, "kg"));
      builder.setExportValue(new EngineNumber(30, "kg"));
      builder.setRecycleValue(new EngineNumber(25, "kg"));
      builder.setDomesticConsumptionValue(new EngineNumber(200, "tCO2e"));
      builder.setImportConsumptionValue(new EngineNumber(100, "tCO2e"));
      builder.setExportConsumptionValue(new EngineNumber(60, "tCO2e"));
      builder.setRecycleConsumptionValue(new EngineNumber(50, "tCO2e"));
      builder.setPopulationValue(new EngineNumber(1000, "units"));
      builder.setPopulationNew(new EngineNumber(100, "units"));
      builder.setRechargeEmissions(new EngineNumber(300, "tCO2e"));
      builder.setEolEmissions(new EngineNumber(150, "tCO2e"));
      builder.setEnergyConsumption(new EngineNumber(500, "kWh"));
      builder.setTradeSupplement(tradeSupplement);

      return builder;
    };

    QUnit.test("initializes", function (assert) {
      const builder = new EngineResultBuilder();
      assert.notDeepEqual(builder, undefined);
    });

    QUnit.test("builds complete result", function (assert) {
      const builder = makeExampleBuilder();
      const result = builder.build();
      assert.notDeepEqual(result, undefined);
      assert.deepEqual(result.getApplication(), "test app");
      assert.deepEqual(result.getSubstance(), "test substance");
      assert.equal(result.getYear(), 2023);
      assert.deepEqual(result.getScenarioName(), "test scenario");
      assert.equal(result.getTrialNumber(), 1);
    });

    QUnit.test("fails on an empty result", function (assert) {
      const builder = new EngineResultBuilder();
      builder.setApplication("test app");
      // Missing required fields

      assert.throws(function () {
        builder.build();
      }, "Should throw when required fields are missing");
    });

    QUnit.test("fails on almost complete result", function (assert) {
      const builder = new EngineResultBuilder();
      const tradeSupplement = new TradeSupplement(
        new EngineNumber(5, "kg"),
        new EngineNumber(10, "tCO2e"),
        new EngineNumber(2, "units"),
        new EngineNumber(3, "kg"),
        new EngineNumber(6, "tCO2e"),
      );

      builder.setApplication("test app");
      builder.setSubstance("test substance");
      builder.setYear(2023);
      builder.setScenarioName("test scenario");
      builder.setTrialNumber(1);
      builder.setManufactureValue(new EngineNumber(100, "kg"));
      builder.setImportValue(new EngineNumber(50, "kg"));
      builder.setExportValue(new EngineNumber(30, "kg"));
      builder.setRecycleValue(new EngineNumber(25, "kg"));
      builder.setDomesticConsumptionValue(new EngineNumber(200, "tCO2e"));
      builder.setImportConsumptionValue(new EngineNumber(100, "tCO2e"));
      builder.setExportConsumptionValue(new EngineNumber(60, "tCO2e"));
      builder.setRecycleConsumptionValue(new EngineNumber(50, "tCO2e"));
      builder.setPopulationValue(new EngineNumber(1000, "units"));
      builder.setPopulationNew(new EngineNumber(100, "units"));
      builder.setRechargeEmissions(new EngineNumber(300, "tCO2e"));
      // Missing EOL emissions
      builder.setEnergyConsumption(new EngineNumber(500, "kWh"));
      builder.setTradeSupplement(tradeSupplement);

      assert.throws(function () {
        builder.build();
      }, "Should throw when EOL emissions are missing");
    });

    QUnit.test("fails when missing trade supplement", function (assert) {
      const builder = new EngineResultBuilder();

      builder.setApplication("test app");
      builder.setSubstance("test substance");
      builder.setYear(2023);
      builder.setScenarioName("test scenario");
      builder.setTrialNumber(1);
      builder.setManufactureValue(new EngineNumber(100, "kg"));
      builder.setImportValue(new EngineNumber(50, "kg"));
      builder.setExportValue(new EngineNumber(30, "kg"));
      builder.setRecycleValue(new EngineNumber(25, "kg"));
      builder.setDomesticConsumptionValue(new EngineNumber(200, "tCO2e"));
      builder.setImportConsumptionValue(new EngineNumber(100, "tCO2e"));
      builder.setExportConsumptionValue(new EngineNumber(60, "tCO2e"));
      builder.setRecycleConsumptionValue(new EngineNumber(50, "tCO2e"));
      builder.setPopulationValue(new EngineNumber(1000, "units"));
      builder.setPopulationNew(new EngineNumber(100, "units"));
      builder.setRechargeEmissions(new EngineNumber(300, "tCO2e"));
      builder.setEolEmissions(new EngineNumber(150, "tCO2e"));
      builder.setEnergyConsumption(new EngineNumber(500, "kWh"));
      // Missing trade supplement

      assert.throws(function () {
        builder.build();
      }, "Should throw when trade supplement is missing");
    });
  });

  QUnit.module("AggregatedResult", function () {
    const makeExample = () => {
      return new AggregatedResult(
        new EngineNumber(100, "kg"), // manufacture
        new EngineNumber(50, "kg"), // import
        new EngineNumber(25, "kg"), // recycle
        new EngineNumber(200, "tCO2e"), // domesticConsumption
        new EngineNumber(100, "tCO2e"), // importConsumption
        new EngineNumber(50, "tCO2e"), // recycleConsumption
        new EngineNumber(1000, "units"), // population
        new EngineNumber(100, "units"), // populationNew
        new EngineNumber(300, "tCO2e"), // rechargeEmissions
        new EngineNumber(150, "tCO2e"), // eolEmissions
        new EngineNumber(500, "kWh"), // energyConsumption
      );
    };

    QUnit.test("initializes", function (assert) {
      const result = makeExample();
      assert.notDeepEqual(result, undefined);
    });

    QUnit.test("getManufacture", function (assert) {
      const result = makeExample();
      const manufacture = result.getManufacture();
      assert.closeTo(manufacture.getValue(), 100, 0.0001);
      assert.deepEqual(manufacture.getUnits(), "kg");
    });

    QUnit.test("getImport", function (assert) {
      const result = makeExample();
      const importValue = result.getImport();
      assert.closeTo(importValue.getValue(), 50, 0.0001);
      assert.deepEqual(importValue.getUnits(), "kg");
    });

    QUnit.test("getRecycle", function (assert) {
      const result = makeExample();
      const recycle = result.getRecycle();
      assert.closeTo(recycle.getValue(), 25, 0.0001);
      assert.deepEqual(recycle.getUnits(), "kg");
    });

    QUnit.test("getSales", function (assert) {
      const result = makeExample();
      const sales = result.getSales();
      assert.closeTo(sales.getValue(), 175, 0.0001); // 100 + 50 + 25
      assert.deepEqual(sales.getUnits(), "kg");
    });

    QUnit.test("getDomesticConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getDomesticConsumption();
      assert.closeTo(consumption.getValue(), 200, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getImportConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getImportConsumption();
      assert.closeTo(consumption.getValue(), 100, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getRecycleConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getRecycleConsumption();
      assert.closeTo(consumption.getValue(), 50, 0.0001);
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getGhgConsumption", function (assert) {
      const result = makeExample();
      const consumption = result.getGhgConsumption();
      assert.closeTo(consumption.getValue(), 350, 0.0001); // 200 + 100 + 50
      assert.deepEqual(consumption.getUnits(), "tCO2e");
    });

    QUnit.test("getPopulation", function (assert) {
      const result = makeExample();
      const population = result.getPopulation();
      assert.closeTo(population.getValue(), 1000, 0.0001);
      assert.deepEqual(population.getUnits(), "units");
    });

    QUnit.test("getPopulationNew", function (assert) {
      const result = makeExample();
      const populationNew = result.getPopulationNew();
      assert.closeTo(populationNew.getValue(), 100, 0.0001);
      assert.deepEqual(populationNew.getUnits(), "units");
    });

    QUnit.test("getRechargeEmissions", function (assert) {
      const result = makeExample();
      const emissions = result.getRechargeEmissions();
      assert.closeTo(emissions.getValue(), 300, 0.0001);
      assert.deepEqual(emissions.getUnits(), "tCO2e");
    });

    QUnit.test("getEolEmissions", function (assert) {
      const result = makeExample();
      const emissions = result.getEolEmissions();
      assert.closeTo(emissions.getValue(), 150, 0.0001);
      assert.deepEqual(emissions.getUnits(), "tCO2e");
    });

    QUnit.test("getTotalEmissions", function (assert) {
      const result = makeExample();
      const emissions = result.getTotalEmissions();
      assert.closeTo(emissions.getValue(), 450, 0.0001); // 300 + 150
      assert.deepEqual(emissions.getUnits(), "tCO2e");
    });

    QUnit.test("getEnergyConsumption", function (assert) {
      const result = makeExample();
      const energy = result.getEnergyConsumption();
      assert.closeTo(energy.getValue(), 500, 0.0001);
      assert.deepEqual(energy.getUnits(), "kWh");
    });

    QUnit.test("combine", function (assert) {
      const result1 = makeExample();
      const result2 = new AggregatedResult(
        new EngineNumber(50, "kg"), // manufacture
        new EngineNumber(25, "kg"), // import
        new EngineNumber(10, "kg"), // recycle
        new EngineNumber(100, "tCO2e"), // domesticConsumption
        new EngineNumber(50, "tCO2e"), // importConsumption
        new EngineNumber(25, "tCO2e"), // recycleConsumption
        new EngineNumber(500, "units"), // population
        new EngineNumber(50, "units"), // populationNew
        new EngineNumber(150, "tCO2e"), // rechargeEmissions
        new EngineNumber(75, "tCO2e"), // eolEmissions
        new EngineNumber(250, "kWh"), // energyConsumption
      );

      const combined = result1.combine(result2);
      assert.closeTo(combined.getManufacture().getValue(), 150, 0.0001); // 100 + 50
      assert.closeTo(combined.getImport().getValue(), 75, 0.0001); // 50 + 25
      assert.closeTo(combined.getRecycle().getValue(), 35, 0.0001); // 25 + 10
    });
  });
}

export {buildEngineStructTests};
