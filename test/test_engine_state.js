import {YearMatcher, Scope, StreamParameterization, StreamKeeper} from "engine_state";
import {EngineNumber} from "engine_number";
import {UnitConverter} from "engine_unit";

function buildEngineStateTests() {
  QUnit.module("YearMatcher", function () {
    QUnit.test("initializes", function (assert) {
      const matcher = new YearMatcher(null, null);
      assert.notDeepEqual(matcher, undefined);
    });

    QUnit.test("matches any", function (assert) {
      const matcher = new YearMatcher(null, null);
      assert.ok(matcher.getInRange(1));
    });

    QUnit.test("matches after", function (assert) {
      const matcher = new YearMatcher(2, null);
      assert.ok(!matcher.getInRange(1));
      assert.ok(matcher.getInRange(2));
      assert.ok(matcher.getInRange(3));
    });

    QUnit.test("matches before", function (assert) {
      const matcher = new YearMatcher(null, 2);
      assert.ok(matcher.getInRange(1));
      assert.ok(matcher.getInRange(2));
      assert.ok(!matcher.getInRange(3));
    });

    QUnit.test("matches within", function (assert) {
      const matcher = new YearMatcher(2, 3);
      assert.ok(!matcher.getInRange(1));
      assert.ok(matcher.getInRange(2));
      assert.ok(matcher.getInRange(3));
      assert.ok(!matcher.getInRange(4));
    });

    QUnit.test("matches within reverse", function (assert) {
      const matcher = new YearMatcher(3, 2);
      assert.ok(!matcher.getInRange(1));
      assert.ok(matcher.getInRange(2));
      assert.ok(matcher.getInRange(3));
      assert.ok(!matcher.getInRange(4));
    });
  });

  QUnit.module("Scope", function () {
    QUnit.test("initializes", function (assert) {
      const scope = new Scope("default", "test app", "test substance");
      assert.notDeepEqual(scope, undefined);
    });

    QUnit.test("getters", function (assert) {
      const scope = new Scope("default", "test app", "test substance");
      assert.deepEqual(scope.getStanza(), "default");
      assert.deepEqual(scope.getApplication(), "test app");
      assert.deepEqual(scope.getSubstance(), "test substance");
    });

    QUnit.test("changes substance", function (assert) {
      const scopeOld = new Scope("default", "test app", "test substance");
      const scopeNew = scopeOld.getWithSubstance("test substance 2");
      assert.deepEqual(scopeNew.getStanza(), "default");
      assert.deepEqual(scopeNew.getApplication(), "test app");
      assert.deepEqual(scopeNew.getSubstance(), "test substance 2");
    });

    QUnit.test("changes application", function (assert) {
      const scopeOld = new Scope("default", "test app", "test substance");
      const scopeNew = scopeOld.getWithApplication("test app 2");
      assert.deepEqual(scopeNew.getStanza(), "default");
      assert.deepEqual(scopeNew.getApplication(), "test app 2");
      assert.deepEqual(scopeNew.getSubstance(), null);
    });

    QUnit.test("changes stanza", function (assert) {
      const scopeOld = new Scope("default", "test app", "test substance");
      const scopeNew = scopeOld.getWithStanza('policy "test policy"');
      assert.deepEqual(scopeNew.getStanza(), 'policy "test policy"');
      assert.deepEqual(scopeNew.getApplication(), null);
      assert.deepEqual(scopeNew.getSubstance(), null);
    });

    QUnit.test("writes and reads var", function (assert) {
      const scope = new Scope("default", "test app", "test substance");
      scope.defineVariable("testVar");
      scope.setVariable("testVar", 123);
      assert.equal(scope.getVariable("testVar"), 123);
    });

    QUnit.test("reads upwards in scope", function (assert) {
      const oldScope = new Scope("default", "test app", null);
      oldScope.defineVariable("testVar");
      oldScope.setVariable("testVar", 123);
      assert.equal(oldScope.getVariable("testVar"), 123);

      const newScope = oldScope.getWithSubstance("test substance 2");
      assert.equal(newScope.getVariable("testVar"), 123);

      newScope.setVariable("testVar", 124);
      assert.equal(newScope.getVariable("testVar"), 124);
    });

    QUnit.test("shadows a variable", function (assert) {
      const oldScope = new Scope("default", "test app", null);
      oldScope.defineVariable("testVar");
      oldScope.setVariable("testVar", 123);
      assert.equal(oldScope.getVariable("testVar"), 123);

      const newScope = oldScope.getWithSubstance("test substance 2");
      newScope.defineVariable("testVar");
      newScope.setVariable("testVar", 124);
      assert.equal(newScope.getVariable("testVar"), 124);

      const restoredScope = newScope.getWithSubstance("test substance 3");
      assert.equal(restoredScope.getVariable("testVar"), 123);
    });

    QUnit.test("edits scopes above", function (assert) {
      const oldScope = new Scope("default", "test app", null);
      oldScope.defineVariable("testVar");
      oldScope.setVariable("testVar", 123);
      assert.equal(oldScope.getVariable("testVar"), 123);

      const tempScope = oldScope.getWithSubstance("test substance 2");
      tempScope.setVariable("testVar", 124);

      const newScope = tempScope.getWithSubstance("test substance 3");
      assert.equal(newScope.getVariable("testVar"), 124);
    });
  });

  QUnit.module("StreamParameterization", function () {
    QUnit.test("initializes", function (assert) {
      const parameterization = new StreamParameterization();
      assert.notDeepEqual(parameterization, undefined);
    });

    QUnit.test("resetInternals sets default values", function (assert) {
      const parameterization = new StreamParameterization();

      // Test GHG intensity default
      const ghgIntensity = parameterization.getGhgIntensity();
      assert.equal(ghgIntensity.getValue(), 0);
      assert.equal(ghgIntensity.getUnits(), "tCO2e / kg");

      // Test energy intensity default
      const energyIntensity = parameterization.getEnergyIntensity();
      assert.equal(energyIntensity.getValue(), 0);
      assert.equal(energyIntensity.getUnits(), "kwh / kg");

      // Test initial charge defaults
      const manufactureCharge = parameterization.getInitialCharge("manufacture");
      assert.equal(manufactureCharge.getValue(), 1);
      assert.equal(manufactureCharge.getUnits(), "kg / unit");

      const importCharge = parameterization.getInitialCharge("import");
      assert.equal(importCharge.getValue(), 1);
      assert.equal(importCharge.getUnits(), "kg / unit");

      // Test recharge population default
      const rechargePopulation = parameterization.getRechargePopulation();
      assert.equal(rechargePopulation.getValue(), 0);
      assert.equal(rechargePopulation.getUnits(), "%");

      // Test recharge intensity default
      const rechargeIntensity = parameterization.getRechargeIntensity();
      assert.equal(rechargeIntensity.getValue(), 0);
      assert.equal(rechargeIntensity.getUnits(), "kg / unit");

      // Test recovery rate default
      const recoveryRate = parameterization.getRecoveryRate();
      assert.equal(recoveryRate.getValue(), 0);
      assert.equal(recoveryRate.getUnits(), "%");

      // Test yield rate default
      const yieldRate = parameterization.getYieldRate();
      assert.equal(yieldRate.getValue(), 0);
      assert.equal(yieldRate.getUnits(), "%");

      // Test retirement rate default
      const retirementRate = parameterization.getRetirementRate();
      assert.equal(retirementRate.getValue(), 0);
      assert.equal(retirementRate.getUnits(), "%");

      // Test displacement rate default
      const displacementRate = parameterization.getDisplacementRate();
      assert.equal(displacementRate.getValue(), 100);
      assert.equal(displacementRate.getUnits(), "%");
    });

    QUnit.test("GHG intensity getter and setter", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(2.5, "tCO2e / kg");

      parameterization.setGhgIntensity(newValue);
      const retrieved = parameterization.getGhgIntensity();

      assert.equal(retrieved.getValue(), 2.5);
      assert.equal(retrieved.getUnits(), "tCO2e / kg");
    });

    QUnit.test("energy intensity getter and setter", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(1.5, "kwh / kg");

      parameterization.setEnergyIntensity(newValue);
      const retrieved = parameterization.getEnergyIntensity();

      assert.equal(retrieved.getValue(), 1.5);
      assert.equal(retrieved.getUnits(), "kwh / kg");
    });

    QUnit.test("initial charge getter and setter for manufacture", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(2.0, "kg / unit");

      parameterization.setInitialCharge("manufacture", newValue);
      const retrieved = parameterization.getInitialCharge("manufacture");

      assert.equal(retrieved.getValue(), 2.0);
      assert.equal(retrieved.getUnits(), "kg / unit");
    });

    QUnit.test("initial charge getter and setter for import", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(1.8, "kg / unit");

      parameterization.setInitialCharge("import", newValue);
      const retrieved = parameterization.getInitialCharge("import");

      assert.equal(retrieved.getValue(), 1.8);
      assert.equal(retrieved.getUnits(), "kg / unit");
    });

    QUnit.test("initial charge throws error for invalid stream", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(1.0, "kg / unit");

      assert.throws(function () {
        parameterization.setInitialCharge("invalid", newValue);
      }, "Must address a sales substream.");

      assert.throws(function () {
        parameterization.getInitialCharge("invalid");
      }, "Must address a sales substream.");
    });

    QUnit.test("recharge population getter and setter", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(15.5, "%");

      parameterization.setRechargePopulation(newValue);
      const retrieved = parameterization.getRechargePopulation();

      assert.equal(retrieved.getValue(), 15.5);
      assert.equal(retrieved.getUnits(), "%");
    });

    QUnit.test("recharge intensity getter and setter", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(0.5, "kg / unit");

      parameterization.setRechargeIntensity(newValue);
      const retrieved = parameterization.getRechargeIntensity();

      assert.equal(retrieved.getValue(), 0.5);
      assert.equal(retrieved.getUnits(), "kg / unit");
    });

    QUnit.test("recovery rate getter and setter", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(80.0, "%");

      parameterization.setRecoveryRate(newValue);
      const retrieved = parameterization.getRecoveryRate();

      assert.equal(retrieved.getValue(), 80.0);
      assert.equal(retrieved.getUnits(), "%");
    });

    QUnit.test("yield rate getter and setter", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(90.0, "%");

      parameterization.setYieldRate(newValue);
      const retrieved = parameterization.getYieldRate();

      assert.equal(retrieved.getValue(), 90.0);
      assert.equal(retrieved.getUnits(), "%");
    });

    QUnit.test("displacement rate getter and setter", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(75.0, "%");

      parameterization.setDisplacementRate(newValue);
      const retrieved = parameterization.getDisplacementRate();

      assert.equal(retrieved.getValue(), 75.0);
      assert.equal(retrieved.getUnits(), "%");
    });

    QUnit.test("retirement rate getter and setter", function (assert) {
      const parameterization = new StreamParameterization();
      const newValue = new EngineNumber(10.0, "%");

      parameterization.setRetirementRate(newValue);
      const retrieved = parameterization.getRetirementRate();

      assert.equal(retrieved.getValue(), 10.0);
      assert.equal(retrieved.getUnits(), "%");
    });

    QUnit.test("last specified units getter and setter", function (assert) {
      const parameterization = new StreamParameterization();

      // Test default value
      const defaultUnits = parameterization.getLastSpecifiedUnits();
      assert.equal(defaultUnits, "kg");

      // Test setting and getting units
      parameterization.setLastSpecifiedUnits("kg");
      const retrieved = parameterization.getLastSpecifiedUnits();
      assert.equal(retrieved, "kg");

      // Test setting different units
      parameterization.setLastSpecifiedUnits("units");
      const retrievedUnits = parameterization.getLastSpecifiedUnits();
      assert.equal(retrievedUnits, "units");
    });

    QUnit.test("resetInternals resets last specified units", function (assert) {
      const parameterization = new StreamParameterization();

      // Set units and verify
      parameterization.setLastSpecifiedUnits("kg");
      assert.equal(parameterization.getLastSpecifiedUnits(), "kg");

      // Reset and verify default
      parameterization.resetInternals();
      assert.equal(parameterization.getLastSpecifiedUnits(), "kg");
    });

    QUnit.test("setLastSpecifiedUnits ignores percentage units", function (assert) {
      const parameterization = new StreamParameterization();

      // Set initial non-percentage units
      parameterization.setLastSpecifiedUnits("kg");
      assert.equal(parameterization.getLastSpecifiedUnits(), "kg");

      // Try to set percentage unit - should be ignored
      parameterization.setLastSpecifiedUnits("%");
      assert.equal(parameterization.getLastSpecifiedUnits(), "kg",
        "Pure percentage units should be ignored");

      // Try to set unit containing percentage - should be ignored
      parameterization.setLastSpecifiedUnits("kg / %");
      assert.equal(parameterization.getLastSpecifiedUnits(), "kg",
        "Units containing percentage should be ignored");

      // Try another percentage format - should be ignored
      parameterization.setLastSpecifiedUnits("15%");
      assert.equal(parameterization.getLastSpecifiedUnits(), "kg",
        "Percentage values should be ignored");

      // Set valid non-percentage units - should work
      parameterization.setLastSpecifiedUnits("units");
      assert.equal(parameterization.getLastSpecifiedUnits(), "units",
        "Non-percentage units should still work");

      // Try percentage again - should be ignored, keeping "units"
      parameterization.setLastSpecifiedUnits("%");
      assert.equal(parameterization.getLastSpecifiedUnits(), "units",
        "Percentage should still be ignored after setting valid units");
    });

    QUnit.test("setLastSpecifiedUnits handles null and undefined", function (assert) {
      const parameterization = new StreamParameterization();

      // Set initial units
      parameterization.setLastSpecifiedUnits("kg");
      assert.equal(parameterization.getLastSpecifiedUnits(), "kg");

      // Try null - should update to null (original behavior preserved)
      parameterization.setLastSpecifiedUnits(null);
      assert.equal(parameterization.getLastSpecifiedUnits(), null,
        "Null units should update last specified units (original behavior)");

      // Reset to test undefined
      parameterization.setLastSpecifiedUnits("kg");
      assert.equal(parameterization.getLastSpecifiedUnits(), "kg");

      // Try undefined - should update to undefined (original behavior preserved)
      parameterization.setLastSpecifiedUnits(undefined);
      assert.equal(parameterization.getLastSpecifiedUnits(), undefined,
        "Undefined units should update last specified units (original behavior)");

      // Set valid units again - should work
      parameterization.setLastSpecifiedUnits("mt");
      assert.equal(parameterization.getLastSpecifiedUnits(), "mt",
        "Valid units should still work after null/undefined");
    });

    QUnit.test("setLastSpecifiedUnits handles various percentage formats", function (assert) {
      const parameterization = new StreamParameterization();

      // Set initial units
      parameterization.setLastSpecifiedUnits("kg");
      assert.equal(parameterization.getLastSpecifiedUnits(), "kg");

      // Test various percentage formats that should be ignored
      const percentageFormats = [
        "%",
        "50%",
        "kg/%",
        "units / %",
        "% / year",
        "tCO2e / %",
        "% per unit",
        "percentage %",
      ];

      percentageFormats.forEach((format) => {
        parameterization.setLastSpecifiedUnits(format);
        assert.equal(parameterization.getLastSpecifiedUnits(), "kg",
          `Format "${format}" should be ignored`);
      });

      // Test valid formats that should NOT be ignored
      const validFormats = [
        "units",
        "mt",
        "kg / unit",
        "tCO2e / kg",
        "kwh / kg",
        "year",
        "years",
      ];

      validFormats.forEach((format) => {
        parameterization.setLastSpecifiedUnits(format);
        assert.equal(parameterization.getLastSpecifiedUnits(), format,
          `Format "${format}" should be accepted`);
      });
    });
  });

  QUnit.module("StreamKeeper", function () {
    // Mock state getter
    class MockConverterStateGetter {
      constructor() {
        const self = this;
        self._substanceConsumption = new EngineNumber(1, "tCO2e / kg");
        self._energyIntensity = new EngineNumber(1, "kwh / kg");
        self._amortizedUnitVolume = new EngineNumber(1, "kg / unit");
        self._population = new EngineNumber(100, "units");
        self._yearsElapsed = new EngineNumber(1, "year");
        self._totalGhgConsumption = new EngineNumber(50, "tCO2e");
        self._totalEnergyConsumption = new EngineNumber(100, "kwh");
        self._volume = new EngineNumber(200, "kg");
        self._amortizedUnitConsumption = new EngineNumber(0.5, "tCO2e / unit");
        self._populationChange = new EngineNumber(10, "units");
      }

      getSubstanceConsumption() {
        const self = this;
        return self._substanceConsumption;
      }

      getEnergyIntensity() {
        const self = this;
        return self._energyIntensity;
      }

      getAmortizedUnitVolume() {
        const self = this;
        return self._amortizedUnitVolume;
      }

      getPopulation() {
        const self = this;
        return self._population;
      }

      setPopulation(newValue) {
        const self = this;
        self._population = newValue;
      }

      getYearsElapsed() {
        const self = this;
        return self._yearsElapsed;
      }

      getGhgConsumption() {
        const self = this;
        return self._totalGhgConsumption;
      }

      getEnergyConsumption() {
        const self = this;
        return self._totalEnergyConsumption;
      }

      getVolume() {
        const self = this;
        return self._volume;
      }

      getAmortizedUnitConsumption() {
        const self = this;
        return self._amortizedUnitConsumption;
      }

      getPopulationChange(unitConverter) {
        const self = this;
        return self._populationChange;
      }
    }

    const createMockKeeper = () => {
      const stateGetter = new MockConverterStateGetter();
      const unitConverter = new UnitConverter(stateGetter);
      return new StreamKeeper(stateGetter, unitConverter);
    };

    QUnit.test("initializes", function (assert) {
      const keeper = createMockKeeper();
      assert.notDeepEqual(keeper, undefined);
    });

    QUnit.test("hasSubstance returns false for unknown substance", function (assert) {
      const keeper = createMockKeeper();
      assert.ok(!keeper.hasSubstance("test app", "test substance"));
    });

    QUnit.test("ensureSubstance creates new substance", function (assert) {
      const keeper = createMockKeeper();

      keeper.ensureSubstance("test app", "test substance");

      assert.ok(keeper.hasSubstance("test app", "test substance"));
    });

    QUnit.test("ensureSubstance creates default streams", function (assert) {
      const keeper = createMockKeeper();

      keeper.ensureSubstance("test app", "test substance");

      // Test that default streams exist with zero values
      const manufacture = keeper.getStream("test app", "test substance", "manufacture");
      assert.equal(manufacture.getValue(), 0);
      assert.equal(manufacture.getUnits(), "kg");

      const importValue = keeper.getStream("test app", "test substance", "import");
      assert.equal(importValue.getValue(), 0);
      assert.equal(importValue.getUnits(), "kg");

      const recycle = keeper.getStream("test app", "test substance", "recycle");
      assert.equal(recycle.getValue(), 0);
      assert.equal(recycle.getUnits(), "kg");

      const consumption = keeper.getStream("test app", "test substance", "consumption");
      assert.equal(consumption.getValue(), 0);
      assert.equal(consumption.getUnits(), "tCO2e");

      const equipment = keeper.getStream("test app", "test substance", "equipment");
      assert.equal(equipment.getValue(), 0);
      assert.equal(equipment.getUnits(), "units");
    });

    QUnit.test("setStream and getStream work for simple streams", function (assert) {
      const keeper = createMockKeeper();
      keeper.ensureSubstance("test app", "test substance");

      const newValue = new EngineNumber(100, "kg");
      keeper.setStream("test app", "test substance", "manufacture", newValue);

      const retrieved = keeper.getStream("test app", "test substance", "manufacture");
      assert.equal(retrieved.getValue(), 100);
      assert.equal(retrieved.getUnits(), "kg");
    });

    QUnit.test("sales stream returns sum of manufacture and import", function (assert) {
      const keeper = createMockKeeper();
      keeper.ensureSubstance("test app", "test substance");

      keeper.setStream("test app", "test substance", "manufacture", new EngineNumber(50, "kg"));
      keeper.setStream("test app", "test substance", "import", new EngineNumber(30, "kg"));

      const sales = keeper.getStream("test app", "test substance", "sales");
      assert.equal(sales.getValue(), 80);
      assert.equal(sales.getUnits(), "kg");
    });

    QUnit.test("ghg intensity getter and setter delegate to parameterization", function (assert) {
      const keeper = createMockKeeper();
      keeper.ensureSubstance("test app", "test substance");

      const newValue = new EngineNumber(2.5, "tCO2e / kg");
      keeper.setGhgIntensity("test app", "test substance", newValue);

      const retrieved = keeper.getGhgIntensity("test app", "test substance");
      assert.equal(retrieved.getValue(), 2.5);
      assert.equal(retrieved.getUnits(), "tCO2e / kg");
    });

    QUnit.test("energy intensity getter and setter delegate to parameterization",
      function (assert) {
        const keeper = createMockKeeper();
        keeper.ensureSubstance("test app", "test substance");

        const newValue = new EngineNumber(1.5, "kwh / kg");
        keeper.setEnergyIntensity("test app", "test substance", newValue);

        const retrieved = keeper.getEnergyIntensity("test app", "test substance");
        assert.equal(retrieved.getValue(), 1.5);
        assert.equal(retrieved.getUnits(), "kwh / kg");
      });

    QUnit.test("initial charge getter and setter delegate to parameterization", function (assert) {
      const keeper = createMockKeeper();
      keeper.ensureSubstance("test app", "test substance");

      const newValue = new EngineNumber(2.0, "kg / unit");
      keeper.setInitialCharge("test app", "test substance", "manufacture", newValue);

      const retrieved = keeper.getInitialCharge("test app", "test substance", "manufacture");
      assert.equal(retrieved.getValue(), 2.0);
      assert.equal(retrieved.getUnits(), "kg / unit");
    });

    QUnit.test("incrementYear moves equipment to priorEquipment", function (assert) {
      const keeper = createMockKeeper();
      keeper.ensureSubstance("test app", "test substance");

      // Set equipment value
      keeper.setStream("test app", "test substance", "equipment", new EngineNumber(150, "units"));

      // Increment year
      keeper.incrementYear();

      // Check that equipment was moved to priorEquipment
      const priorEquipment = keeper.getStream("test app", "test substance", "priorEquipment");
      assert.equal(priorEquipment.getValue(), 150);
      assert.equal(priorEquipment.getUnits(), "units");
    });

    QUnit.test("throws error for unknown substance in setStream", function (assert) {
      const keeper = createMockKeeper();

      assert.throws(function () {
        keeper.setStream("unknown app", "unknown substance", "manufacture",
          new EngineNumber(100, "kg"));
      }, /Not a known application substance pair/);
    });

    QUnit.test("throws error for unknown substance in getStream", function (assert) {
      const keeper = createMockKeeper();

      assert.throws(function () {
        keeper.getStream("unknown app", "unknown substance", "manufacture");
      }, /Not a known application substance pair/);
    });

    QUnit.test("throws error for unknown stream", function (assert) {
      const keeper = createMockKeeper();
      keeper.ensureSubstance("test app", "test substance");

      assert.throws(function () {
        keeper.setStream("test app", "test substance", "unknown_stream",
          new EngineNumber(100, "kg"));
      }, /Unknown stream/);

      assert.throws(function () {
        keeper.getStream("test app", "test substance", "unknown_stream");
      }, /Unknown stream/);
    });

    QUnit.test("getRegisteredSubstances returns substance list", function (assert) {
      const keeper = createMockKeeper();
      keeper.ensureSubstance("app1", "substance1");
      keeper.ensureSubstance("app2", "substance2");

      const substances = keeper.getRegisteredSubstances();
      assert.equal(substances.length, 2);

      const substance1 = substances.find((s) => s.getApplication() === "app1" &&
        s.getSubstance() === "substance1");
      const substance2 = substances.find((s) => s.getApplication() === "app2" &&
        s.getSubstance() === "substance2");

      assert.notDeepEqual(substance1, undefined);
      assert.notDeepEqual(substance2, undefined);
    });

    QUnit.test("last specified units getter and setter delegate to parameterization",
      function (assert) {
        const keeper = createMockKeeper();
        keeper.ensureSubstance("test app", "test substance");

        // Test default value
        const defaultUnits = keeper.getLastSpecifiedUnits("test app", "test substance");
        assert.equal(defaultUnits, "kg");

        // Test setting and getting
        keeper.setLastSpecifiedUnits("test app", "test substance", "kg");
        const retrieved = keeper.getLastSpecifiedUnits("test app", "test substance");
        assert.equal(retrieved, "kg");
      });

    QUnit.test("setStream automatically tracks last specified units", function (assert) {
      const keeper = createMockKeeper();
      keeper.ensureSubstance("test app", "test substance");

      // Test default value
      const defaultUnits = keeper.getLastSpecifiedUnits("test app", "test substance");
      assert.equal(defaultUnits, "kg");

      // Test setting units directly via StreamKeeper methods
      keeper.setLastSpecifiedUnits("test app", "test substance", "kg");
      const unitsAfterKg = keeper.getLastSpecifiedUnits("test app", "test substance");
      assert.equal(unitsAfterKg, "kg");

      // Test setting different units
      keeper.setLastSpecifiedUnits("test app", "test substance", "units");
      const unitsAfterUnits = keeper.getLastSpecifiedUnits("test app", "test substance");
      assert.equal(unitsAfterUnits, "units");
    });

    QUnit.test("StreamKeeper ignores percentage units in setLastSpecifiedUnits", function (assert) {
      const keeper = createMockKeeper();
      keeper.ensureSubstance("test app", "test substance");

      // Set initial units
      keeper.setLastSpecifiedUnits("test app", "test substance", "kg");
      assert.equal(keeper.getLastSpecifiedUnits("test app", "test substance"), "kg");

      // Try to set percentage units - should be ignored
      keeper.setLastSpecifiedUnits("test app", "test substance", "%");
      assert.equal(keeper.getLastSpecifiedUnits("test app", "test substance"), "kg",
        "Percentage units should be ignored by StreamKeeper");

      keeper.setLastSpecifiedUnits("test app", "test substance", "50%");
      assert.equal(keeper.getLastSpecifiedUnits("test app", "test substance"), "kg",
        "Percentage values should be ignored by StreamKeeper");

      keeper.setLastSpecifiedUnits("test app", "test substance", "kg / %");
      assert.equal(keeper.getLastSpecifiedUnits("test app", "test substance"), "kg",
        "Units containing percentages should be ignored by StreamKeeper");

      // Set valid units - should work
      keeper.setLastSpecifiedUnits("test app", "test substance", "units");
      assert.equal(keeper.getLastSpecifiedUnits("test app", "test substance"), "units",
        "Valid units should still work through StreamKeeper");
    });
  });
}

export {buildEngineStateTests};
