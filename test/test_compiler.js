import {Compiler} from "compiler";

function loadRemote(path) {
  return fetch(path).then((response) => response.text());
}

function buildCompilerTests() {
  QUnit.module("Compiler", function () {
    QUnit.test("gets toolkit", function (assert) {
      const toolkit = QubecTalk.getToolkit();
      assert.notDeepEqual(toolkit, undefined);
      assert.notDeepEqual(toolkit["antlr4"], undefined);
      assert.notDeepEqual(toolkit["QubecTalkLexer"], undefined);
      assert.notDeepEqual(toolkit["QubecTalkParser"], undefined);
      assert.notDeepEqual(toolkit["QubecTalkListener"], undefined);
      assert.notDeepEqual(toolkit["QubecTalkVisitor"], undefined);
    });

    QUnit.test("initializes a compiler", (assert) => {
      const compiler = new Compiler();
      assert.notDeepEqual(compiler, undefined);
    });

    const buildTest = (name, filepath, checks) => {
      QUnit.test(name, (assert) => {
        const done = assert.async();
        loadRemote(filepath).then((content) => {
          assert.ok(content.length > 0);

          const compiler = new Compiler();
          const compilerResult = compiler.compile(content);
          assert.equal(compilerResult.getErrors().length, 0);

          const program = compilerResult.getProgram();
          assert.equal(compilerResult.getErrors().length, 0);

          if (compilerResult.getErrors().length > 0) {
            console.log(compilerResult.getErrors());
          } else {
            try {
              const programResult = program();
              checks.forEach((check) => {
                check(programResult, assert);
              });
            } catch (e) {
              console.log(e);
              assert.ok(false);
            }
          }

          done();
        });
      });
    };

    const getResult = (results, scenario, year, trialIndex, application, substance) => {
      const result = results.filter((x) => x.getName() === scenario)[0];
      const trials = result.getTrialResults();
      const trial = trials[trialIndex];
      const trialFlat = trial.flat();
      return trialFlat
        .filter((x) => x.getYear() == year)
        .filter((x) => x.getApplication() === application)
        .filter((x) => x.getSubstance() === substance)[0];
    };

    const BAU_NAME = "business as usual";

    buildTest("tests kwh units", "/test/qta/basic_kwh.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const energyConsumption = record.getEnergyConsumption();
        assert.closeTo(energyConsumption.getValue(), 500, 0.0001);
        assert.deepEqual(energyConsumption.getUnits(), "kwh");
      },
    ]);

    buildTest("tests kwh units", "/test/qta/basic_kwh_units.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const energyConsumption = record.getEnergyConsumption();
        assert.closeTo(energyConsumption.getValue(), 500, 0.0001);
        assert.deepEqual(energyConsumption.getUnits(), "kwh");
      },
    ]);

    buildTest("runs a basic script", "/test/qta/basic.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 20000, 0.0001);
        assert.deepEqual(equipment.getUnits(), "units");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 500, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const manufacture = record.getManufacture();
        assert.closeTo(manufacture.getValue(), 100000, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
    ]);

    buildTest("interprets starting units", "/test/qta/basic_units.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 1, 0.0001);
        assert.deepEqual(equipment.getUnits(), "units");
      },
    ]);

    buildTest("interprets starting units with conversion", "/test/qta/basic_units_convert.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 1000, 0.0001);
        assert.deepEqual(equipment.getUnits(), "units");
      },
    ]);

    buildTest("runs a basic script with special float", "/test/qta/basic_special_float.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 200000, 0.0001);
        assert.deepEqual(equipment.getUnits(), "units");
      },
    ]);

    buildTest("interprets a change command", "/test/qta/change.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const manufacture = record.getManufacture();
        assert.closeTo(manufacture.getValue(), 110000, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 550, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("interprets a retire command", "/test/qta/retire.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const manufacture = record.getManufacture();
        assert.closeTo(manufacture.getValue(), 100000, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 500, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const population = record.getPopulation();
        assert.closeTo(population.getValue(), 100000, 0.0001);
        assert.deepEqual(population.getUnits(), "units");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const population = record.getPopulation();
        assert.closeTo(population.getValue(), 190000, 0.0001);
        assert.deepEqual(population.getUnits(), "units");
      },
    ]);

    buildTest("interprets a retire command with prior population", "/test/qta/retire_prior.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const population = record.getPopulation();
        assert.closeTo(population.getValue(), 190000, 0.0001);
        assert.deepEqual(population.getUnits(), "units");
      },
    ]);

    buildTest("interprets multiple retire commands", "/test/qta/retire_multiple.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const population = record.getPopulation();
        assert.ok(population.getValue() < 190000);
        assert.deepEqual(population.getUnits(), "units");
      },
    ]);

    buildTest("interprets a recharge command", "/test/qta/recharge.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 100000, 0.0001);
        assert.deepEqual(equipment.getUnits(), "units");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 190000, 0.0001);
        assert.deepEqual(equipment.getUnits(), "units");
      },
    ]);

    buildTest("handles multiple consumption", "/test/qta/multiple.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "a");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 500, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "b");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 1000, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("handles policies", "/test/qta/policies.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 250, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("handles recycling", "/test/qta/recycling.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 500, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, "result", 2, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 500, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, "result", 2, 0, "test", "test");
        const consumption = record.getRecycleConsumption();
        assert.closeTo(consumption.getValue(), 500 - 437.5, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("handles replace", "/test/qta/replace.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "a");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 250, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "b");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 375, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("combines policies", "/test/qta/combination.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 125, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("evaluates conditionals", "/test/qta/conditional.qta", [
      (result, assert) => {
        const record = getResult(result, "business as usual", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 250, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("verifies substance replacement over time", "/test/qta/basic_replace.qta", [
      (result, assert) => {
        // Check year 1 consumption
        const recordAYear1 = getResult(result, "Sim", 1, 0, "Test", "Sub A");
        const consumptionAYear1 = recordAYear1.getGhgConsumption();
        assert.closeTo(consumptionAYear1.getValue(), 10000000, 0.0001);
        assert.deepEqual(consumptionAYear1.getUnits(), "tCO2e");

        const recordBYear1 = getResult(result, "Sim", 1, 0, "Test", "Sub B");
        const consumptionBYear1 = recordBYear1.getGhgConsumption();
        assert.closeTo(consumptionBYear1.getValue(), 0, 0.0001);
        assert.deepEqual(consumptionBYear1.getUnits(), "tCO2e");

        // Check year 10 consumption
        const recordAYear10 = getResult(result, "Sim", 10, 0, "Test", "Sub A");
        const consumptionAYear10 = recordAYear10.getGhgConsumption();
        assert.closeTo(consumptionAYear10.getValue(), 0, 0.0001);
        assert.deepEqual(consumptionAYear10.getUnits(), "tCO2e");

        const recordBYear10 = getResult(result, "Sim", 10, 0, "Test", "Sub B");
        const consumptionBYear10 = recordBYear10.getGhgConsumption();
        assert.closeTo(consumptionBYear10.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumptionBYear10.getUnits(), "tCO2e");
      },
    ]);

    buildTest(
      "verifies substance replacement over time units",
      "/test/qta/basic_replace_units.qta",
      [
        (result, assert) => {
          // Check year 1 consumption
          const recordAYear1 = getResult(result, "Sim", 1, 0, "Test", "Sub A");
          const consumptionAYear1 = recordAYear1.getGhgConsumption();
          assert.closeTo(consumptionAYear1.getValue(), 10000000, 0.0001);
          assert.deepEqual(consumptionAYear1.getUnits(), "tCO2e");

          const recordBYear1 = getResult(result, "Sim", 1, 0, "Test", "Sub B");
          const consumptionBYear1 = recordBYear1.getGhgConsumption();
          assert.closeTo(consumptionBYear1.getValue(), 0, 0.0001);
          assert.deepEqual(consumptionBYear1.getUnits(), "tCO2e");

          // Check year 10 consumption
          const recordAYear10 = getResult(result, "Sim", 10, 0, "Test", "Sub A");
          const consumptionAYear10 = recordAYear10.getGhgConsumption();
          assert.closeTo(consumptionAYear10.getValue(), 0, 0.0001);
          assert.deepEqual(consumptionAYear10.getUnits(), "tCO2e");

          const recordBYear10 = getResult(result, "Sim", 10, 0, "Test", "Sub B");
          const consumptionBYear10 = recordBYear10.getGhgConsumption();
          assert.closeTo(consumptionBYear10.getValue(), 1000000, 0.0001);
          assert.deepEqual(consumptionBYear10.getUnits(), "tCO2e");
        },
      ],
    );

    buildTest("runs trials", "/test/qta/trials.qta", [
      (result, assert) => {
        const record = getResult(result, "business as usual", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.ok(consumption.getValue() >= 300);
        assert.ok(consumption.getValue() <= 700);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, "business as usual", 1, 1, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.ok(consumption.getValue() >= 300);
        assert.ok(consumption.getValue() <= 700);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("runs reference", "/test/qta/reference.qta", []);

    buildTest("cold starts with equipment", "/test/qta/cold_start_equipment.qta", [
      (result, assert) => {
        const record = getResult(result, "BAU", 1, 0, "Test", "Sub");
        const consumption = record.getGhgConsumption();
        assert.ok(consumption.getValue() > 0);
      },
      (result, assert) => {
        const record = getResult(result, "BAU", 10, 0, "Test", "Sub");
        const consumption = record.getGhgConsumption();
        assert.ok(consumption.getValue() > 0);
      },
    ]);
  });
}

export {buildCompilerTests};
