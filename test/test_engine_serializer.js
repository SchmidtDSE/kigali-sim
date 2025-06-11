/*
 * QUnit tests for engine_serializer.js
 *
 * This file contains unit tests for the EngineResultSerializer and related logic.
 *
 * @file test_engine_serializer.js
 */

import {EngineResultSerializer} from "engine_serializer";
import {ConverterStateGetter} from "engine_unit_state";
import {EngineNumber} from "engine_number";
import {EngineResultBuilder} from "engine_struct";

export function buildEngineSerializerTests() {
  QUnit.module("EngineResultSerializer main body", function (hooks) {
    const manufacture = new EngineNumber(1600, "mt");
    const importMt = new EngineNumber(400, "mt");
    const recharge = new EngineNumber(1000, "mt");
    const valueToConsumption = new EngineNumber(500, "tCO2e / mt");
    const initialChargeImport = new EngineNumber(200, "kg / unit");
    const initialChargeDomestic = new EngineNumber(150, "kg / unit");
    const recycling = new EngineNumber(10, "mt");
    const energyIntensity = new EngineNumber(5, "kwh / kg");
    const priorEquipment = new EngineNumber(1000, "units");
    const eolEmissions = new EngineNumber(100, "tCO2e");

    const options = new Map();
    options.set("manufacture", manufacture);
    options.set("import", importMt);
    options.set("recycle", recycling);
    options.set("energy", energyIntensity);
    options.set("equipment", priorEquipment);
    options.set("newEquipment", priorEquipment);
    options.set("rechargeEmissions", recharge);
    options.set("eolEmissions", eolEmissions);
    options.set("ghgIntensity", valueToConsumption);

    const rawInitialChargeFor = {
      "import": initialChargeImport,
      "manufacture": initialChargeDomestic,
    };
    options.set("rawInitialChargeFor", rawInitialChargeFor);

    const engine = new MockEngine(options);
    const stateGetter = new ConverterStateGetter(engine);
    const serializer = new EngineResultSerializer(engine, stateGetter);

    const result = serializer.getResult("commercialRefrigeration", "HFC-134a", 1);

    QUnit.test("gets manufacture value", function (assert) {
      // Expected: 1600 mt - (10 mt * (1600/(1600+400))) = 1600 - 8 = 1592 mt = 1,592,000 kg
      assert.closeTo(result.getManufacture().getValue(), 1592000, 0.00001);
      assert.strictEqual(result.getManufacture().getUnits(), "kg");
    });

    QUnit.test("gets import value", function (assert) {
      // Expected: 400 mt - (10 mt * (400/(1600+400))) = 400 - 2 = 398 mt = 398,000 kg
      assert.closeTo(result.getImport().getValue(), 398000, 0.00001);
      assert.strictEqual(result.getImport().getUnits(), "kg");
    });

    QUnit.test("gets recycle value", function (assert) {
      // Expected: 10 mt = 10,000 kg
      assert.closeTo(result.getRecycle().getValue(), 10000, 0.00001);
      assert.strictEqual(result.getRecycle().getUnits(), "kg");
    });

    QUnit.test("gets domestic consumption", function (assert) {
      // Expected: 1,592,000 kg * 500 tCO2e / mt * (1mt/1,000kg) = 796 tCO2e
      assert.closeTo(result.getDomesticConsumption().getValue(), 796000, 0.00001);
      assert.strictEqual(result.getDomesticConsumption().getUnits(), "tCO2e");
    });

    QUnit.test("gets import consumption", function (assert) {
      // Expected: 398,000 kg * 500 tCO2e / mt * (1mt/1,000kg) = 199 tCO2e
      assert.closeTo(result.getImportConsumption().getValue(), 199000, 0.00001);
      assert.strictEqual(result.getImportConsumption().getUnits(), "tCO2e");
    });

    QUnit.test("gets recycle consumption", function (assert) {
      // Expected: 10,000 kg * 500 tCO2e / mt * (1mt/1,000kg) = 5 tCO2e
      assert.closeTo(result.getRecycleConsumption().getValue(), 5000, 0.00001);
      assert.strictEqual(result.getRecycleConsumption().getUnits(), "tCO2e");
    });

    QUnit.test("gets population value", function (assert) {
      // Expected: 1000 units (from prior equipment)
      assert.closeTo(result.getPopulation().getValue(), 1000, 0.00001);
    });

    QUnit.test("gets population new", function (assert) {
      // Expected: 1000 units (from new equipment)
      assert.closeTo(result.getPopulationNew().getValue(), 1000, 0.00001);
    });

    QUnit.test("gets recharge emissions", function (assert) {
      // Expected: 1000 mt converted to tCO2e (500,000) - 5000 tCO2e (recycle consumption)
      // = 495,000 tCO2e
      assert.closeTo(result.getRechargeEmissions().getValue(), 495000, 0.00001);
      assert.strictEqual(result.getRechargeEmissions().getUnits(), "tCO2e");
    });

    QUnit.test("gets end-of-life emissions", function (assert) {
      // Expected: 100 tCO2e
      assert.closeTo(result.getEolEmissions().getValue(), 100, 0.00001);
      assert.strictEqual(result.getEolEmissions().getUnits(), "tCO2e");
    });

    QUnit.test("gets energy consumption", function (assert) {
      // Expected: 5 kwh / kg
      assert.closeTo(result.getEnergyConsumption().getValue(), 5, 0.00001);
      assert.strictEqual(result.getEnergyConsumption().getUnits(), "kwh / kg");
    });
  });

  QUnit.module("EngineResultSerializer import supplement", function (hooks) {
    const manufacture = new EngineNumber(1600, "mt");
    const importMt = new EngineNumber(400, "mt");
    const recharge = new EngineNumber(1000, "mt");
    const valueToConsumption = new EngineNumber(500, "tCO2e / mt");
    const initialChargeImport = new EngineNumber(200, "kg / unit");

    const options = new Map();
    options.set("manufacture", manufacture);
    options.set("import", importMt);
    options.set("rechargeEmissions", recharge);
    options.set("equalsGhgIntensityFor", valueToConsumption);

    const rawInitialChargeFor = {
      "import": initialChargeImport,
    };
    options.set("rawInitialChargeFor", rawInitialChargeFor);

    const engine = new MockEngine(options);
    const stateGetter = new ConverterStateGetter(engine);

    const serializer = new EngineResultSerializer(engine, stateGetter);
    const result = serializer.getResult("commercialRefrigeration", "HFC-134a", 1);
    const importSupplement = result.getImportSupplement();

    QUnit.test("generate import supplement value", function (assert) {
      // Calculate: total import (400 mt = 400,000 kg) - import recharge portion
      // Import proportion: 400,000 / (1,600,000 + 400,000) = 0.2
      // Import recharge: 0.2 * 1,000,000 kg = 200,000 kg
      // Import for initial charge: 400,000 - 200,000 = 200,000 kg
      assert.closeTo(importSupplement.getInitialChargeValue().getValue(), 200000, 0.00001);
      assert.strictEqual(importSupplement.getInitialChargeValue().getUnits(), "kg");
    });

    QUnit.test("generate import supplement consumption", function (assert) {
      // 200,000 kg * 500 tCO2e/mt * (1 mt/1000 kg) = 100,000 tCO2e
      assert.closeTo(importSupplement.getInitialChargeConsumption().getValue(), 100000, 0.00001);
      assert.strictEqual(importSupplement.getInitialChargeConsumption().getUnits(), "tCO2e");
    });

    QUnit.test("generate import supplement units", function (assert) {
      // 200,000 kg / 200 kg/unit = 1,000 units
      assert.closeTo(importSupplement.getNewPopulation().getValue(), 1000, 0.00001);
      assert.strictEqual(importSupplement.getNewPopulation().getUnits(), "units");
    });
  });
}

// Mock Engine for use with ConverterStateGetter in tests
class MockEngine {
  constructor(options) {
    this.options = options;
  }
  getStream(name, scope, conversion) {
    // Only use "name" for mock, ignore scope/conversion
    return this.options.has(name) ? this.options.get(name) : new EngineNumber(0, "kg");
  }
  getStreamRaw(application, substance, stream) {
    // Only use "stream" for mock, ignore application/substance
    return this.options.has(stream) ? this.options.get(stream) : new EngineNumber(0, "kg");
  }
  getGhgIntensity(application, substance) {
    // Only use "ghgIntensity" for mock
    if (this.options.has("ghgIntensity")) {
      return this.options.get("ghgIntensity");
    } else {
      return new EngineNumber(0, "tCO2e / kg");
    }
  }
  getEqualsGhgIntensity() {
    // Return the ghgIntensity for the current scope
    if (this.options.has("ghgIntensity")) {
      return this.options.get("ghgIntensity");
    } else {
      return new EngineNumber(0, "tCO2e / kg");
    }
  }
  getEqualsGhgIntensityFor(application, substance) {
    // Only use "equalsGhgIntensityFor" for mock
    if (this.options.has("equalsGhgIntensityFor")) {
      return this.options.get("equalsGhgIntensityFor");
    } else {
      return new EngineNumber(0, "tCO2e / kg");
    }
  }
  getRawInitialChargeFor(application, substance, stream) {
    // Only use "rawInitialChargeFor" for mock
    if (this.options.has("rawInitialChargeFor")) {
      const rawInitialChargeFor = this.options.get("rawInitialChargeFor");
      if (rawInitialChargeFor && rawInitialChargeFor[stream] !== undefined) {
        return rawInitialChargeFor[stream];
      }
    }
    return new EngineNumber(0, "kg / unit");
  }
}
