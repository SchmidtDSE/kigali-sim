import {EngineNumber} from "engine_number";
import {ConverterStateGetter, OverridingConverterStateGetter} from "engine_unit_state";

function buildEngineUnitStateTests() {
  QUnit.module("ConverterStateGetter", function () {
    class MockEngine {
      constructor() {
        const self = this;
        self._streams = {};
        self._intensities = {};
        self._charges = {};
      }

      setStream(name, value) {
        const self = this;
        self._streams[name] = value;
      }

      getStream(name) {
        const self = this;
        return self._streams[name] || new EngineNumber(0, "kg");
      }

      setIntensity(name, value) {
        const self = this;
        self._intensities[name] = value;
      }

      getEqualsGhgIntensity() {
        const self = this;
        return self._intensities.ghg || new EngineNumber(1, "tCO2e / kg");
      }

      getEqualsEnergyIntensity() {
        const self = this;
        return self._intensities.energy || new EngineNumber(1, "kwh / kg");
      }

      setCharge(name, value) {
        const self = this;
        self._charges[name] = value;
      }

      getInitialCharge(name) {
        const self = this;
        return self._charges[name] || new EngineNumber(1, "kg / unit");
      }
    }

    QUnit.test("initializes with engine", function (assert) {
      const engine = new MockEngine();
      const stateGetter = new ConverterStateGetter(engine);
      assert.notDeepEqual(stateGetter, undefined);
    });

    QUnit.test("getSubstanceConsumption", function (assert) {
      const engine = new MockEngine();
      engine.setIntensity("ghg", new EngineNumber(2.5, "tCO2e / kg"));
      const stateGetter = new ConverterStateGetter(engine);

      const result = stateGetter.getSubstanceConsumption();

      assert.closeTo(result.getValue(), 2.5, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e / kg");
    });

    QUnit.test("getEnergyIntensity", function (assert) {
      const engine = new MockEngine();
      engine.setIntensity("energy", new EngineNumber(1.5, "kwh / kg"));
      const stateGetter = new ConverterStateGetter(engine);

      const result = stateGetter.getEnergyIntensity();

      assert.closeTo(result.getValue(), 1.5, 0.001);
      assert.deepEqual(result.getUnits(), "kwh / kg");
    });

    QUnit.test("getAmortizedUnitVolume", function (assert) {
      const engine = new MockEngine();
      engine.setCharge("sales", new EngineNumber(10, "kg / unit"));
      const stateGetter = new ConverterStateGetter(engine);

      const result = stateGetter.getAmortizedUnitVolume();

      assert.closeTo(result.getValue(), 10, 0.001);
      assert.deepEqual(result.getUnits(), "kg / unit");
    });

    QUnit.test("getPopulation", function (assert) {
      const engine = new MockEngine();
      engine.setStream("equipment", new EngineNumber(100, "units"));
      const stateGetter = new ConverterStateGetter(engine);

      const result = stateGetter.getPopulation();

      assert.closeTo(result.getValue(), 100, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("getYearsElapsed", function (assert) {
      const engine = new MockEngine();
      const stateGetter = new ConverterStateGetter(engine);

      const result = stateGetter.getYearsElapsed();

      assert.closeTo(result.getValue(), 1, 0.001);
      assert.deepEqual(result.getUnits(), "year");
    });

    QUnit.test("getGhgConsumption", function (assert) {
      const engine = new MockEngine();
      engine.setStream("consumption", new EngineNumber(50, "tCO2e"));
      const stateGetter = new ConverterStateGetter(engine);

      const result = stateGetter.getGhgConsumption();

      assert.closeTo(result.getValue(), 50, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e");
    });

    QUnit.test("getEnergyConsumption", function (assert) {
      const engine = new MockEngine();
      engine.setStream("energy", new EngineNumber(200, "kwh"));
      const stateGetter = new ConverterStateGetter(engine);

      const result = stateGetter.getEnergyConsumption();

      assert.closeTo(result.getValue(), 200, 0.001);
      assert.deepEqual(result.getUnits(), "kwh");
    });

    QUnit.test("getVolume", function (assert) {
      const engine = new MockEngine();
      engine.setStream("sales", new EngineNumber(75, "kg"));
      const stateGetter = new ConverterStateGetter(engine);

      const result = stateGetter.getVolume();

      assert.closeTo(result.getValue(), 75, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });
  });

  QUnit.module("OverridingConverterStateGetter", function () {
    class MockInnerStateGetter {
      constructor() {
        const self = this;
        self._values = {};
      }

      setValue(name, value) {
        const self = this;
        self._values[name] = value;
      }

      getSubstanceConsumption() {
        const self = this;
        return self._values.substanceConsumption || new EngineNumber(1, "tCO2e / kg");
      }

      getEnergyIntensity() {
        const self = this;
        return self._values.energyIntensity || new EngineNumber(1, "kwh / kg");
      }

      getAmortizedUnitVolume() {
        const self = this;
        return self._values.amortizedUnitVolume || new EngineNumber(1, "kg / unit");
      }

      getPopulation() {
        const self = this;
        return self._values.population || new EngineNumber(10, "units");
      }

      getYearsElapsed() {
        const self = this;
        return self._values.yearsElapsed || new EngineNumber(1, "year");
      }

      getGhgConsumption() {
        const self = this;
        return self._values.ghgConsumption || new EngineNumber(10, "tCO2e");
      }

      getEnergyConsumption() {
        const self = this;
        return self._values.energyConsumption || new EngineNumber(100, "kwh");
      }

      getVolume() {
        const self = this;
        return self._values.volume || new EngineNumber(50, "kg");
      }

      getAmortizedUnitConsumption() {
        const self = this;
        return self._values.amortizedUnitConsumption || new EngineNumber(1, "tCO2e / unit");
      }

      getPopulationChange() {
        const self = this;
        return self._values.populationChange || new EngineNumber(5, "units");
      }
    }

    QUnit.test("initializes with inner getter", function (assert) {
      const inner = new MockInnerStateGetter();
      const overriding = new OverridingConverterStateGetter(inner);
      assert.notDeepEqual(overriding, undefined);
    });

    QUnit.test("getSubstanceConsumption uses inner when not overridden", function (assert) {
      const inner = new MockInnerStateGetter();
      inner.setValue("substanceConsumption", new EngineNumber(3, "tCO2e / kg"));
      const overriding = new OverridingConverterStateGetter(inner);

      const result = overriding.getSubstanceConsumption();

      assert.closeTo(result.getValue(), 3, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e / kg");
    });

    QUnit.test("setSubstanceConsumption overrides inner value", function (assert) {
      const inner = new MockInnerStateGetter();
      inner.setValue("substanceConsumption", new EngineNumber(3, "tCO2e / kg"));
      const overriding = new OverridingConverterStateGetter(inner);

      overriding.setSubstanceConsumption(new EngineNumber(5, "tCO2e / kg"));
      const result = overriding.getSubstanceConsumption();

      assert.closeTo(result.getValue(), 5, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e / kg");
    });

    QUnit.test("getEnergyIntensity uses inner when not overridden", function (assert) {
      const inner = new MockInnerStateGetter();
      inner.setValue("energyIntensity", new EngineNumber(2, "kwh / kg"));
      const overriding = new OverridingConverterStateGetter(inner);

      const result = overriding.getEnergyIntensity();

      assert.closeTo(result.getValue(), 2, 0.001);
      assert.deepEqual(result.getUnits(), "kwh / kg");
    });

    QUnit.test("setEnergyIntensity overrides inner value", function (assert) {
      const inner = new MockInnerStateGetter();
      inner.setValue("energyIntensity", new EngineNumber(2, "kwh / kg"));
      const overriding = new OverridingConverterStateGetter(inner);

      overriding.setEnergyIntensity(new EngineNumber(4, "kwh / kg"));
      const result = overriding.getEnergyIntensity();

      assert.closeTo(result.getValue(), 4, 0.001);
      assert.deepEqual(result.getUnits(), "kwh / kg");
    });

    QUnit.test("getPopulation uses inner when not overridden", function (assert) {
      const inner = new MockInnerStateGetter();
      inner.setValue("population", new EngineNumber(20, "units"));
      const overriding = new OverridingConverterStateGetter(inner);

      const result = overriding.getPopulation();

      assert.closeTo(result.getValue(), 20, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("setPopulation overrides inner value", function (assert) {
      const inner = new MockInnerStateGetter();
      inner.setValue("population", new EngineNumber(20, "units"));
      const overriding = new OverridingConverterStateGetter(inner);

      overriding.setPopulation(new EngineNumber(30, "units"));
      const result = overriding.getPopulation();

      assert.closeTo(result.getValue(), 30, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("getVolume uses inner when not overridden", function (assert) {
      const inner = new MockInnerStateGetter();
      inner.setValue("volume", new EngineNumber(100, "kg"));
      const overriding = new OverridingConverterStateGetter(inner);

      const result = overriding.getVolume();

      assert.closeTo(result.getValue(), 100, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });

    QUnit.test("setVolume overrides inner value", function (assert) {
      const inner = new MockInnerStateGetter();
      inner.setValue("volume", new EngineNumber(100, "kg"));
      const overriding = new OverridingConverterStateGetter(inner);

      overriding.setVolume(new EngineNumber(150, "kg"));
      const result = overriding.getVolume();

      assert.closeTo(result.getValue(), 150, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });

    QUnit.test("setTotal with sales calls setVolume", function (assert) {
      const inner = new MockInnerStateGetter();
      const overriding = new OverridingConverterStateGetter(inner);

      overriding.setTotal("sales", new EngineNumber(200, "kg"));
      const result = overriding.getVolume();

      assert.closeTo(result.getValue(), 200, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });

    QUnit.test("setTotal with equipment calls setPopulation", function (assert) {
      const inner = new MockInnerStateGetter();
      const overriding = new OverridingConverterStateGetter(inner);

      overriding.setTotal("equipment", new EngineNumber(50, "units"));
      const result = overriding.getPopulation();

      assert.closeTo(result.getValue(), 50, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("setTotal with consumption calls setConsumption", function (assert) {
      const inner = new MockInnerStateGetter();
      const overriding = new OverridingConverterStateGetter(inner);

      overriding.setTotal("consumption", new EngineNumber(75, "tCO2e"));
      const result = overriding.getGhgConsumption();

      assert.closeTo(result.getValue(), 75, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e");
    });
  });
}

export {buildEngineUnitStateTests};
