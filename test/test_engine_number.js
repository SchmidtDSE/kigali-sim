import {EngineNumber, UnitConverter} from "engine_number";

function buildEngineNumberTests() {
  QUnit.module("EngineNumber", function () {
    const makeExample = () => {
      return new EngineNumber(1.23, "kg");
    };

    QUnit.test("initializes", function (assert) {
      const number = makeExample();
      assert.notDeepEqual(number, undefined);
    });

    QUnit.test("getValue", function (assert) {
      const number = makeExample();
      assert.closeTo(number.getValue(), 1.23, 0.0001);
    });

    QUnit.test("getUnits", function (assert) {
      const number = makeExample();
      assert.deepEqual(number.getUnits(), "kg");
    });
  });

  QUnit.module("convertUnits", function () {
    class MockConverterStateGetter {
      constructor() {
        const self = this;
        self._substanceConsumption = null;
        self._amortizedUnitVolume = null;
        self._population = null;
        self._yearsElapsed = null;
        self._totalConsumption = null;
        self._volume = null;
        self._amortizedUnitConsumption = null;
        self._populationChange = null;
      }

      setSubstanceConsumption(newValue) {
        const self = this;
        self._substanceConsumption = newValue;
      }

      getSubstanceConsumption() {
        const self = this;
        return self._substanceConsumption;
      }

      setAmortizedUnitVolume(newValue) {
        const self = this;
        self._amortizedUnitVolume = newValue;
      }

      getAmortizedUnitVolume() {
        const self = this;
        return self._amortizedUnitVolume;
      }

      setPopulation(newValue) {
        const self = this;
        self._population = newValue;
      }

      getPopulation() {
        const self = this;
        return self._population;
      }

      setYearsElapsed(newValue) {
        const self = this;
        self._yearsElapsed = newValue;
      }

      getYearsElapsed() {
        const self = this;
        return self._yearsElapsed;
      }

      setConsumption(newValue) {
        const self = this;
        self._totalConsumption = newValue;
      }

      getConsumption() {
        const self = this;
        return self._totalConsumption;
      }

      setVolume(newValue) {
        const self = this;
        self._volume = newValue;
      }

      getVolume() {
        const self = this;
        return self._volume;
      }

      setAmortizedUnitConsumption(newValue) {
        const self = this;
        self._amortizedUnitConsumption = newValue;
      }

      getAmortizedUnitConsumption() {
        const self = this;
        return self._amortizedUnitConsumption;
      }

      setPopulationChange(newValue) {
        const self = this;
        self._populationChange = newValue;
      }

      getPopulationChange() {
        const self = this;
        return self._populationChange;
      }
    }

    function convertUnits(source, destination, stateGetter) {
      const converter = new UnitConverter(stateGetter);
      return converter.convert(source, destination);
    }

    QUnit.test("volume to volume", function (assert) {
      const result = convertUnits(new EngineNumber(1, "mt"), "kg", new MockConverterStateGetter());

      assert.closeTo(result.getValue(), 1000, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });

    QUnit.test("consumption to volume", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setSubstanceConsumption(new EngineNumber(5, "tCO2e / mt"));

      const result = convertUnits(new EngineNumber(20, "tCO2e"), "mt", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 4, 0.001);
      assert.deepEqual(result.getUnits(), "mt");
    });

    QUnit.test("units to volume", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setAmortizedUnitVolume(new EngineNumber(10, "kg / unit"));

      const result = convertUnits(new EngineNumber(20, "units"), "kg", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 200, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });

    QUnit.test("volume per pop to volume", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setPopulation(new EngineNumber(10, "units"));

      const result = convertUnits(
        new EngineNumber(20, "kg / unit"),
        "kg",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 200, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });

    QUnit.test("volume per pop to volume per pop", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setPopulation(new EngineNumber(10, "units"));

      const result = convertUnits(
        new EngineNumber(20, "kg / unit"),
        "mt / unit",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 20 / 1000, 0.001);
      assert.deepEqual(result.getUnits(), "mt / unit");
    });

    QUnit.test("volume per time to volume", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setYearsElapsed(new EngineNumber(2, "years"));

      const result = convertUnits(
        new EngineNumber(20, "kg / year"),
        "kg",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 40, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });

    QUnit.test("volume per consumption to volume", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setConsumption(new EngineNumber(10, "tCO2e"));

      const result = convertUnits(
        new EngineNumber(20, "kg / tCO2e"),
        "kg",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 200, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });

    QUnit.test("percent to volume", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setVolume(new EngineNumber(20, "kg"));

      const result = convertUnits(new EngineNumber(10, "%"), "kg", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 2, 0.001);
      assert.deepEqual(result.getUnits(), "kg");
    });

    QUnit.test("pop to pop", function (assert) {
      const result = convertUnits(
        new EngineNumber(5, "unit"),
        "units",
        new MockConverterStateGetter(),
      );

      assert.closeTo(result.getValue(), 5, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("volume to pop", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setAmortizedUnitVolume(new EngineNumber(10, "kg / unit"));

      const result = convertUnits(new EngineNumber(20, "kg"), "units", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 2, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("consumption to pop", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setAmortizedUnitConsumption(new EngineNumber(50, "tCO2e / unit"));

      const result = convertUnits(
        new EngineNumber(200, "tCO2e"),
        "units",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 4, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("pop per time to pop", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setYearsElapsed(new EngineNumber(2, "years"));

      const result = convertUnits(
        new EngineNumber(20, "units / year"),
        "units",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 40, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("pop per volume to pop", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setVolume(new EngineNumber(10, "kg"));

      const result = convertUnits(
        new EngineNumber(2, "units / kg"),
        "units",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 20, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("pop per consumption to pop", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setConsumption(new EngineNumber(5, "tCO2e"));

      const result = convertUnits(
        new EngineNumber(2, "units / tCO2e"),
        "units",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 10, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("percent to pop", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setPopulation(new EngineNumber(20, "units"));

      const result = convertUnits(new EngineNumber(10, "%"), "units", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 2, 0.001);
      assert.deepEqual(result.getUnits(), "units");
    });

    QUnit.test("consumption to consumption", function (assert) {
      const result = convertUnits(
        new EngineNumber(5, "tCO2e"),
        "tCO2e",
        new MockConverterStateGetter(),
      );

      assert.closeTo(result.getValue(), 5, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e");
    });

    QUnit.test("volume to consumption", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setSubstanceConsumption(new EngineNumber(5, "tCO2e / kg"));

      const result = convertUnits(new EngineNumber(10, "kg"), "tCO2e", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 50, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e");
    });

    QUnit.test("pop to consumption", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setAmortizedUnitVolume(new EngineNumber(0.1, "tCO2e / unit"));

      const result = convertUnits(new EngineNumber(20, "units"), "tCO2e", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 2, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e");
    });

    QUnit.test("consumption per time to consumption", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setYearsElapsed(new EngineNumber(2, "years"));

      const result = convertUnits(
        new EngineNumber(20, "tCO2e / year"),
        "tCO2e",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 40, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e");
    });

    QUnit.test("consumption per volume to consumption", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setVolume(new EngineNumber(5, "kg"));

      const result = convertUnits(
        new EngineNumber(2, "tCO2e / kg"),
        "tCO2e",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 10, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e");
    });

    QUnit.test("consumption per pop to consumption", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setPopulation(new EngineNumber(20, "units"));

      const result = convertUnits(
        new EngineNumber(10, "tCO2e / unit"),
        "tCO2e",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 200, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e");
    });

    QUnit.test("percent to consumption", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setConsumption(new EngineNumber(10, "tCO2e"));

      const result = convertUnits(new EngineNumber(10, "%"), "tCO2e", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 1, 0.001);
      assert.deepEqual(result.getUnits(), "tCO2e");
    });

    QUnit.test("years to years", function (assert) {
      const result = convertUnits(
        new EngineNumber(5, "year"),
        "years",
        new MockConverterStateGetter(),
      );

      assert.closeTo(result.getValue(), 5, 0.001);
      assert.deepEqual(result.getUnits(), "years");
    });

    QUnit.test("consumption to years", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setConsumption(new EngineNumber(5, "tCO2e"));

      const result = convertUnits(new EngineNumber(10, "tCO2e"), "years", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 2, 0.001);
      assert.deepEqual(result.getUnits(), "years");
    });

    QUnit.test("volume to years", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setVolume(new EngineNumber(5, "kg"));

      const result = convertUnits(new EngineNumber(10, "kg"), "years", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 2, 0.001);
      assert.deepEqual(result.getUnits(), "years");
    });

    QUnit.test("pop to years", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setPopulationChange(new EngineNumber(2, "units"));

      const result = convertUnits(new EngineNumber(20, "units"), "years", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 10, 0.001);
      assert.deepEqual(result.getUnits(), "years");
    });

    QUnit.test("percent to years", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setYearsElapsed(new EngineNumber(2, "years"));

      const result = convertUnits(new EngineNumber(10, "%"), "years", mockConverterStateGetter);

      assert.closeTo(result.getValue(), 0.2, 0.001);
      assert.deepEqual(result.getUnits(), "years");
    });

    QUnit.test("normalize by population", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setPopulation(new EngineNumber(2, "units"));

      const result = convertUnits(
        new EngineNumber(20, "kg"),
        "kg / unit",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 10, 0.001);
      assert.deepEqual(result.getUnits(), "kg / unit");
    });

    QUnit.test("normalize by volume", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setVolume(new EngineNumber(2, "kg"));

      const result = convertUnits(
        new EngineNumber(10, "units"),
        "unit / kg",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 5, 0.001);
      assert.deepEqual(result.getUnits(), "unit / kg");
    });

    QUnit.test("normalize by consumption", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setConsumption(new EngineNumber(2, "tCO2e"));

      const result = convertUnits(
        new EngineNumber(10, "units"),
        "unit / tCO2e",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 5, 0.001);
      assert.deepEqual(result.getUnits(), "unit / tCO2e");
    });

    QUnit.test("normalize by time", function (assert) {
      const mockConverterStateGetter = new MockConverterStateGetter();
      mockConverterStateGetter.setYearsElapsed(new EngineNumber(2, "years"));

      const result = convertUnits(
        new EngineNumber(10, "units"),
        "unit / year",
        mockConverterStateGetter,
      );

      assert.closeTo(result.getValue(), 5, 0.001);
      assert.deepEqual(result.getUnits(), "unit / year");
    });
  });
}

export {buildEngineNumberTests};
