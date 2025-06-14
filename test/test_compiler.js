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

    buildTest("interprets a change command with real years", "/test/qta/real_years.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2026, 0, "test", "test");
        const manufacture = record.getManufacture();
        assert.closeTo(manufacture.getValue(), 110000, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2026, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 550, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("handles change command by adding kg", "/test/qta/change_add_kg.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const manufacture = record.getManufacture();
        assert.closeTo(manufacture.getValue(), 110, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
    ]);

    buildTest("handles change command by subtracting kg", "/test/qta/change_subtract_kg.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const manufacture = record.getManufacture();
        assert.closeTo(manufacture.getValue(), 90, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
    ]);

    buildTest("handles change command by adding units", "/test/qta/change_add_units.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const manufacture = record.getManufacture();
        assert.closeTo(manufacture.getValue(), 110, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
    ]);

    buildTest(
      "handles change command by subtracting units",
      "/test/qta/change_subtract_units.qta",
      [
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
          const manufacture = record.getManufacture();
          assert.closeTo(manufacture.getValue(), 90, 0.0001);
          assert.deepEqual(manufacture.getUnits(), "kg");
        },
      ],
    );

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

    buildTest("handles recharge on top when specifying by units", "/test/qta/recharge_on_top.qta", [
      (result, assert) => {
        // Test that recharge is added on top for units-based specifications
        // Should have 10000 (prior) + 1000 (manufacture) = 11000 units in year 1
        const record = getResult(result, "BAU", 1, 0, "App", "Sub1");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 11000, 0.0001);
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

    buildTest("handles recycling by kg", "/test/qta/recycle_by_kg.qta", [
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
        // 25 kg * 5 tCO2e/mt = 25 kg * 5 tCO2e/(1000 kg) = 0.125 tCO2e
        assert.closeTo(consumption.getValue(), 0.125, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("handles recycling by units", "/test/qta/recycle_by_units.qta", [
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
        // 1000 units * 2 kg/unit = 2000 kg, 2000 kg * 5 tCO2e/(1000 kg) = 10 tCO2e
        assert.closeTo(consumption.getValue(), 10, 0.0001);
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

    buildTest("evaluates logical operators", "/test/qta/logical_operators.qta", [
      (result, assert) => {
        // Test AND: 1 and 0 = false, so manufacture should be 30 (else branch)
        const recordYear1 = getResult(result, "business as usual", 1, 0, "test", "test");
        const consumptionYear1 = recordYear1.getGhgConsumption();
        assert.closeTo(consumptionYear1.getValue(), 30, 0.0001);
        assert.deepEqual(consumptionYear1.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test OR: 1 or 0 = true, so manufacture should be 50 (if branch)
        const recordYear2 = getResult(result, "business as usual", 2, 0, "test", "test");
        const consumptionYear2 = recordYear2.getGhgConsumption();
        assert.closeTo(consumptionYear2.getValue(), 50, 0.0001);
        assert.deepEqual(consumptionYear2.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test XOR: 1 xor 2 = false (both are truthy), so manufacture should be 40 (else branch)
        const recordYear3 = getResult(result, "business as usual", 3, 0, "test", "test");
        const consumptionYear3 = recordYear3.getGhgConsumption();
        assert.closeTo(consumptionYear3.getValue(), 40, 0.0001);
        assert.deepEqual(consumptionYear3.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test precedence with parentheses: (testA or testB) and testC = (1 or 0) and 2 =
        // 1 and 2 = true, so manufacture should be 70 (if branch)
        const recordYear4 = getResult(result, "business as usual", 4, 0, "test", "test");
        const consumptionYear4 = recordYear4.getGhgConsumption();
        assert.closeTo(consumptionYear4.getValue(), 70, 0.0001);
        assert.deepEqual(consumptionYear4.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test precedence without parentheses: testA or testB and testC =
        // testA or (testB and testC) = 1 or (0 and 2) = 1 or 0 = true,
        // so manufacture should be 80 (if branch)
        const recordYear5 = getResult(result, "business as usual", 5, 0, "test", "test");
        const consumptionYear5 = recordYear5.getGhgConsumption();
        assert.closeTo(consumptionYear5.getValue(), 80, 0.0001);
        assert.deepEqual(consumptionYear5.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test mixed comparison and logical: testA > 0 and testB == 0 =
        // 1 > 0 and 0 == 0 = true and true = true, so manufacture should be 90 (if branch)
        const recordYear6 = getResult(result, "business as usual", 6, 0, "test", "test");
        const consumptionYear6 = recordYear6.getGhgConsumption();
        assert.closeTo(consumptionYear6.getValue(), 90, 0.0001);
        assert.deepEqual(consumptionYear6.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test complex parentheses: (testA > 0 or testB > 0) and (testC == 2) =
        // (true or false) and (true) = true and true = true,
        // so manufacture should be 100 (if branch)
        const recordYear7 = getResult(result, "business as usual", 7, 0, "test", "test");
        const consumptionYear7 = recordYear7.getGhgConsumption();
        assert.closeTo(consumptionYear7.getValue(), 100, 0.0001);
        assert.deepEqual(consumptionYear7.getUnits(), "tCO2e");
      },
    ]);

    buildTest("evaluates simple AND operator", "/test/qta/simple_and.qta", [
      (result, assert) => {
        // Test AND: 1 and 0 = false, so manufacture should be 30 (else branch)
        const record = getResult(result, "business as usual", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 30, 0.0001);
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

    buildTest("handles replace by units", "/test/qta/replace_units.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "a");
        const consumption = record.getGhgConsumption();
        // Calculation: Original 50 mt - replaced 25 mt = 25 mt remaining × 10 tCO2e/mt = 250 tCO2e
        assert.closeTo(consumption.getValue(), 250, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "b");
        const consumption = record.getGhgConsumption();
        // Calculation: Original 50 mt + added 25 mt = 75 mt total × 5 tCO2e/mt = 375 tCO2e
        assert.closeTo(consumption.getValue(), 375, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("handles replace by kg", "/test/qta/replace_kg.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "a");
        const consumption = record.getGhgConsumption();
        // Calculation: Original 50 mt - replaced 25 mt = 25 mt remaining × 10 tCO2e/mt = 250 tCO2e
        assert.closeTo(consumption.getValue(), 250, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "b");
        const consumption = record.getGhgConsumption();
        // Calculation: Original 50 mt + added 25 mt = 75 mt total × 5 tCO2e/mt = 375 tCO2e
        assert.closeTo(consumption.getValue(), 375, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

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

    buildTest("checks order of operations in initialization", "/test/qta/order_check_volume.qta", [
      // Test Sub1 (A, B, C order) produces 1 MtCO2e across all years
      (result, assert) => {
        const record1 = getResult(result, "BAU", 1, 0, "App", "Sub1");
        const consumption1 = record1.getGhgConsumption();
        assert.closeTo(consumption1.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumption1.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record2 = getResult(result, "BAU", 2, 0, "App", "Sub1");
        const consumption2 = record2.getGhgConsumption();
        assert.closeTo(consumption2.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumption2.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record3 = getResult(result, "BAU", 3, 0, "App", "Sub1");
        const consumption3 = record3.getGhgConsumption();
        assert.closeTo(consumption3.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumption3.getUnits(), "tCO2e");
      },
      // Test Sub2 (A, C, B order) produces 1 MtCO2e across all years
      (result, assert) => {
        const record1 = getResult(result, "BAU", 1, 0, "App", "Sub2");
        const consumption1 = record1.getGhgConsumption();
        assert.closeTo(consumption1.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumption1.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record2 = getResult(result, "BAU", 2, 0, "App", "Sub2");
        const consumption2 = record2.getGhgConsumption();
        assert.closeTo(consumption2.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumption2.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record3 = getResult(result, "BAU", 3, 0, "App", "Sub2");
        const consumption3 = record3.getGhgConsumption();
        assert.closeTo(consumption3.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumption3.getUnits(), "tCO2e");
      },
      // Test Sub3 (C, A, B order) produces 1 MtCO2e across all years
      (result, assert) => {
        const record1 = getResult(result, "BAU", 1, 0, "App", "Sub3");
        const consumption1 = record1.getGhgConsumption();
        assert.closeTo(consumption1.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumption1.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record2 = getResult(result, "BAU", 2, 0, "App", "Sub3");
        const consumption2 = record2.getGhgConsumption();
        assert.closeTo(consumption2.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumption2.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record3 = getResult(result, "BAU", 3, 0, "App", "Sub3");
        const consumption3 = record3.getGhgConsumption();
        assert.closeTo(consumption3.getValue(), 1000000, 0.0001);
        assert.deepEqual(consumption3.getUnits(), "tCO2e");
      },
    ]);

    buildTest("runs case study", "/test/qta/case_study.qta", [
      (result, assert) => {
        // Test that the case study simulation completes successfully
        assert.ok(result.length > 0, "Case study should produce simulation results");
      },
      (result, assert) => {
        // Test that at least one stream for one substance/application pair is non-zero in 2030
        const record = getResult(result, "Business as Usual", 2030, 0,
          "Domestic Refrigeration", "HFC-134a");
        const consumption = record.getGhgConsumption();
        assert.ok(consumption.getValue() > 0,
          "Should have non-zero consumption for Domestic Refrigeration HFC-134a in 2030");
      },
    ]);

    buildTest("tests initialization by units", "/test/qta/init_units.qta", [
      // Test Sub1 (A, B, C order) - should have 1M units in year 1, 2M in year 2, 3M in year 3
      (result, assert) => {
        const record1 = getResult(result, "BAU", 1, 0, "App", "Sub1");
        const equipment1 = record1.getPopulation();
        assert.closeTo(equipment1.getValue(), 1000000, 0.0001);
        assert.deepEqual(equipment1.getUnits(), "units");
      },
      (result, assert) => {
        const record2 = getResult(result, "BAU", 2, 0, "App", "Sub1");
        const equipment2 = record2.getPopulation();
        assert.closeTo(equipment2.getValue(), 2000000, 0.0001);
        assert.deepEqual(equipment2.getUnits(), "units");
      },
      (result, assert) => {
        const record3 = getResult(result, "BAU", 3, 0, "App", "Sub1");
        const equipment3 = record3.getPopulation();
        assert.closeTo(equipment3.getValue(), 3000000, 0.0001);
        assert.deepEqual(equipment3.getUnits(), "units");
      },
    ]);

    buildTest("checks order of operations in initialization by units",
      "/test/qta/order_check_units.qta",
      [
        // Test Sub2 (A, C, B order) - should have same results as Sub1
        (result, assert) => {
          const record1 = getResult(result, "BAU", 1, 0, "App", "Sub2");
          const equipment1 = record1.getPopulation();
          assert.closeTo(equipment1.getValue(), 1000000, 0.0001);
          assert.deepEqual(equipment1.getUnits(), "units");
        },
        (result, assert) => {
          const record2 = getResult(result, "BAU", 2, 0, "App", "Sub2");
          const equipment2 = record2.getPopulation();
          assert.closeTo(equipment2.getValue(), 2000000, 0.0001);
          assert.deepEqual(equipment2.getUnits(), "units");
        },
        (result, assert) => {
          const record3 = getResult(result, "BAU", 3, 0, "App", "Sub2");
          const equipment3 = record3.getPopulation();
          assert.closeTo(equipment3.getValue(), 3000000, 0.0001);
          assert.deepEqual(equipment3.getUnits(), "units");
        },
        // Test Sub3 (C, A, B order) - should have same results as Sub1 and Sub2
        (result, assert) => {
          const record1 = getResult(result, "BAU", 1, 0, "App", "Sub3");
          const equipment1 = record1.getPopulation();
          assert.closeTo(equipment1.getValue(), 1000000, 0.0001);
          assert.deepEqual(equipment1.getUnits(), "units");
        },
        (result, assert) => {
          const record2 = getResult(result, "BAU", 2, 0, "App", "Sub3");
          const equipment2 = record2.getPopulation();
          assert.closeTo(equipment2.getValue(), 2000000, 0.0001);
          assert.deepEqual(equipment2.getUnits(), "units");
        },
        (result, assert) => {
          const record3 = getResult(result, "BAU", 3, 0, "App", "Sub3");
          const equipment3 = record3.getPopulation();
          assert.closeTo(equipment3.getValue(), 3000000, 0.0001);
          assert.deepEqual(equipment3.getUnits(), "units");
        },
      ],
    );

    buildTest("tests cap with units includes recharge on top", "/test/qta/cap_units.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const manufacture = record.getManufacture();

        // With recharge on top: 50 units * 2 kg/unit + (20 units * 10% * 1 kg/unit) = 102 kg
        // Since original value is 100 kg and cap should be 102 kg, no change expected
        assert.closeTo(manufacture.getValue(), 100, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
    ]);

    buildTest("tests cap with kg works without recharge addition", "/test/qta/cap_kg.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const manufacture = record.getManufacture();

        // Cap at 50 kg should reduce from 100 kg to 50 kg
        assert.closeTo(manufacture.getValue(), 50, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
    ]);

    buildTest("tests floor with units includes recharge on top", "/test/qta/floor_units.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const manufacture = record.getManufacture();

        // With recharge on top: 50 units * 2 kg/unit + (20 units * 10% * 1 kg/unit) = 102 kg
        // Since original value is 10 kg and floor should be 102 kg, should increase to 102 kg
        assert.closeTo(manufacture.getValue(), 102, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
    ]);

    buildTest("tests floor with kg works without recharge addition", "/test/qta/floor_kg.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const manufacture = record.getManufacture();

        // Floor at 50 kg should increase from 10 kg to 50 kg
        assert.closeTo(manufacture.getValue(), 50, 0.0001);
        assert.deepEqual(manufacture.getUnits(), "kg");
      },
    ]);
    
    buildTest(
      "tests ordering-sensitive emissions issue",
      "/test/qta/ordering_sensitive_emissions.qta", [
        (result, assert) => {
          // Test SubA (tCO2e equals comes before recharge)
          const recordA = getResult(result, BAU_NAME, 2035, 0, "test", "SubA");
          const emissionsA = recordA.getRechargeEmissions();
          assert.ok(emissionsA.getValue() > 0);
          assert.deepEqual(emissionsA.getUnits(), "tCO2e");
        },
        (result, assert) => {
          // Test SubB (recharge comes before tCO2e equals)
          const recordB = getResult(result, BAU_NAME, 2035, 0, "test", "SubB");
          const emissionsB = recordB.getRechargeEmissions();
          assert.ok(emissionsB.getValue() > 0);
          assert.deepEqual(emissionsB.getUnits(), "tCO2e");
        },
        (result, assert) => {
          // Both substances should have similar emissions since they are identical
          // except for ordering
          const recordA = getResult(result, BAU_NAME, 2035, 0, "test", "SubA");
          const recordB = getResult(result, BAU_NAME, 2035, 0, "test", "SubB");
          const emissionsA = recordA.getRechargeEmissions();
          const emissionsB = recordB.getRechargeEmissions();
          assert.closeTo(emissionsA.getValue(), emissionsB.getValue(), 0.001);
        },
      ]
    );
  });
}

export {buildCompilerTests};
