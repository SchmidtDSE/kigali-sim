import {WasmBackend, WasmLayer} from "wasm_backend";
import {AttributeToExporterResult} from "engine_struct";

function loadRemote(path) {
  return fetch(path).then((response) => response.text());
}

function buildIntegrationTests() {
  QUnit.module("Integration", function () {
    QUnit.test("gets toolkit", function (assert) {
      const toolkit = QubecTalk.getToolkit();
      assert.notDeepEqual(toolkit, undefined);
      assert.notDeepEqual(toolkit["antlr4"], undefined);
      assert.notDeepEqual(toolkit["QubecTalkLexer"], undefined);
      assert.notDeepEqual(toolkit["QubecTalkParser"], undefined);
      assert.notDeepEqual(toolkit["QubecTalkListener"], undefined);
      assert.notDeepEqual(toolkit["QubecTalkVisitor"], undefined);
    });


    // Shared WASM backend instances to avoid re-initialization overhead
    const wasmLayer = new WasmLayer();
    const wasmBackend = new WasmBackend(wasmLayer);

    const buildTest = (name, filepath, checks) => {
      QUnit.test(name, (assert) => {
        const done = assert.async();
        loadRemote(filepath).then(async (content) => {
          assert.ok(content.length > 0);

          try {
            const backendResult = await wasmBackend.execute(content);
            const programResult = backendResult.getParsedResults();
            checks.forEach((check) => {
              check(programResult, assert);
            });
          } catch (e) {
            console.log(e);
            assert.ok(false, "Execution failed: " + e.message);
          }

          done();
        });
      });
    };

    const getResult = (results, scenario, year, trialIndex, application, substance) => {
      const filtered = results
        .filter((x) => x.getScenarioName() === scenario)
        .filter((x) => x.getYear() == year)
        .filter((x) => x.getApplication() === application)
        .filter((x) => x.getSubstance() === substance);

      // For simple cases, just return the first match regardless of trial number
      // since most tests use single trial simulations
      return filtered[0];
    };

    const BAU_NAME = "business as usual";

    buildTest("tests kwh units", "/examples/basic_kwh.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const energyConsumption = record.getEnergyConsumption();
        assert.closeTo(energyConsumption.getValue(), 500, 0.0001);
        assert.deepEqual(energyConsumption.getUnits(), "kwh");
      },
    ]);

    buildTest("tests kwh units", "/examples/basic_kwh_units.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const energyConsumption = record.getEnergyConsumption();
        assert.closeTo(energyConsumption.getValue(), 500, 0.0001);
        assert.deepEqual(energyConsumption.getUnits(), "kwh");
      },
    ]);

    buildTest("runs a basic script", "/examples/basic.qta", [
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
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 100000, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
    ]);

    buildTest("runs a domestic set by units", "/examples/basic_set_manufacture_units.qta", [
      (result, assert) => {
        const firstRecord = getResult(result, BAU_NAME, 2025, 0, "Test", "HFC-134a");
        const firstEquipment = firstRecord.getPopulation();

        const secondRecord = getResult(result, BAU_NAME, 2035, 0, "Test", "HFC-134a");
        const secondEquipment = secondRecord.getPopulation();

        assert.deepEqual(firstEquipment.getUnits(), secondEquipment.getUnits());
        assert.ok(secondEquipment.getValue() > firstEquipment.getValue());
      },
    ]);

    buildTest("tests minimal interpreter example", "/examples/minimal_interpreter.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "testApp", "testSubstance");
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 100000, 0.0001, "Domestic should be 100000 kg");
        assert.deepEqual(domestic.getUnits(), "kg", "Domestic units should be kg");
      },
    ]);

    buildTest("interprets starting units", "/examples/basic_units.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 1, 0.0001);
        assert.deepEqual(equipment.getUnits(), "units");
      },
    ]);

    buildTest("interprets starting units with conversion", "/examples/basic_units_convert.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 1000, 0.0001);
        assert.deepEqual(equipment.getUnits(), "units");
      },
    ]);

    buildTest("runs a basic script with special float", "/examples/basic_special_float.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const equipment = record.getPopulation();
        assert.closeTo(equipment.getValue(), 200000, 0.0001);
        assert.deepEqual(equipment.getUnits(), "units");
      },
    ]);

    buildTest("interprets a change command", "/examples/change.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 110000, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 550, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("interprets a change command with real years", "/examples/real_years.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2026, 0, "test", "test");
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 110000, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2026, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 550, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("handles change command by adding kg", "/examples/change_add_kg.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 110, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
    ]);

    buildTest("handles change command by subtracting kg", "/examples/change_subtract_kg.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 90, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
    ]);

    buildTest("handles change command by adding units", "/examples/change_add_units.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 110, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
    ]);

    buildTest(
      "handles change command by subtracting units",
      "/examples/change_subtract_units.qta",
      [
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
          const domestic = record.getDomestic();
          assert.closeTo(domestic.getValue(), 90, 0.0001);
          assert.deepEqual(domestic.getUnits(), "kg");
        },
      ],
    );

    buildTest("interprets a retire command", "/examples/retire.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 100000, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
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

    buildTest("interprets a retire command with prior population", "/examples/retire_prior.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const population = record.getPopulation();
        assert.closeTo(population.getValue(), 190000, 0.0001);
        assert.deepEqual(population.getUnits(), "units");
      },
    ]);

    buildTest("interprets multiple retire commands", "/examples/retire_multiple.qta", [
      (result, assert) => {
        const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
        const population = record.getPopulation();
        assert.ok(population.getValue() < 190000);
        assert.deepEqual(population.getUnits(), "units");
      },
    ]);

    buildTest("interprets a recharge command", "/examples/recharge.qta", [
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

    buildTest(
      "handles recharge on top when specifying by units",
      "/examples/recharge_on_top.qta",
      [
        (result, assert) => {
        // Test that recharge is added on top for units-based specifications
        // Should have 10000 (prior) + 1000 (domestic) = 11000 units in year 1
          const record = getResult(result, "BAU", 1, 0, "App", "Sub1");
          const equipment = record.getPopulation();
          assert.closeTo(equipment.getValue(), 11000, 0.0001);
          assert.deepEqual(equipment.getUnits(), "units");
        },
      ]);

    buildTest("handles multiple consumption", "/examples/multiple.qta", [
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

    buildTest("handles policies", "/examples/policies.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 250, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("handles recycling", "/examples/recycling.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 500, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, "result", 2, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        // With recycling but no displacement, virgin material stays the same
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

    buildTest("handles recycling by kg", "/examples/recycle_by_kg.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 500, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, "result", 2, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        // With recycling but no displacement, virgin material stays the same
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

    buildTest("handles recycling by units", "/examples/recycle_by_units.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 500, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        const record = getResult(result, "result", 2, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        // With recycling but no displacement, virgin material stays the same
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

    buildTest("handles replace", "/examples/replace.qta", [
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

    buildTest("combines policies", "/examples/combination.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 125, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("evaluates conditionals", "/examples/conditional.qta", [
      (result, assert) => {
        const record = getResult(result, "business as usual", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 250, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("evaluates logical operators", "/examples/logical_operators.qta", [
      (result, assert) => {
        // Test AND: 1 and 0 = false, so domestic should be 30 (else branch)
        const recordYear1 = getResult(result, "business as usual", 1, 0, "test", "test");
        const consumptionYear1 = recordYear1.getGhgConsumption();
        assert.closeTo(consumptionYear1.getValue(), 30, 0.0001);
        assert.deepEqual(consumptionYear1.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test OR: 1 or 0 = true, so domestic should be 50 (if branch)
        const recordYear2 = getResult(result, "business as usual", 2, 0, "test", "test");
        const consumptionYear2 = recordYear2.getGhgConsumption();
        assert.closeTo(consumptionYear2.getValue(), 50, 0.0001);
        assert.deepEqual(consumptionYear2.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test XOR: 1 xor 2 = false (both are truthy), so domestic should be 40 (else branch)
        const recordYear3 = getResult(result, "business as usual", 3, 0, "test", "test");
        const consumptionYear3 = recordYear3.getGhgConsumption();
        assert.closeTo(consumptionYear3.getValue(), 40, 0.0001);
        assert.deepEqual(consumptionYear3.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test precedence with parentheses: (testA or testB) and testC = (1 or 0) and 2 =
        // 1 and 2 = true, so domestic should be 70 (if branch)
        const recordYear4 = getResult(result, "business as usual", 4, 0, "test", "test");
        const consumptionYear4 = recordYear4.getGhgConsumption();
        assert.closeTo(consumptionYear4.getValue(), 70, 0.0001);
        assert.deepEqual(consumptionYear4.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test precedence without parentheses: testA or testB and testC =
        // testA or (testB and testC) = 1 or (0 and 2) = 1 or 0 = true,
        // so domestic should be 80 (if branch)
        const recordYear5 = getResult(result, "business as usual", 5, 0, "test", "test");
        const consumptionYear5 = recordYear5.getGhgConsumption();
        assert.closeTo(consumptionYear5.getValue(), 80, 0.0001);
        assert.deepEqual(consumptionYear5.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test mixed comparison and logical: testA > 0 and testB == 0 =
        // 1 > 0 and 0 == 0 = true and true = true, so domestic should be 90 (if branch)
        const recordYear6 = getResult(result, "business as usual", 6, 0, "test", "test");
        const consumptionYear6 = recordYear6.getGhgConsumption();
        assert.closeTo(consumptionYear6.getValue(), 90, 0.0001);
        assert.deepEqual(consumptionYear6.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test complex parentheses: (testA > 0 or testB > 0) and (testC == 2) =
        // (true or false) and (true) = true and true = true,
        // so domestic should be 100 (if branch)
        const recordYear7 = getResult(result, "business as usual", 7, 0, "test", "test");
        const consumptionYear7 = recordYear7.getGhgConsumption();
        assert.closeTo(consumptionYear7.getValue(), 100, 0.0001);
        assert.deepEqual(consumptionYear7.getUnits(), "tCO2e");
      },
    ]);

    buildTest("evaluates simple AND operator", "/examples/simple_and.qta", [
      (result, assert) => {
        // Test AND: 1 and 0 = false, so domestic should be 30 (else branch)
        const record = getResult(result, "business as usual", 1, 0, "test", "test");
        const consumption = record.getGhgConsumption();
        assert.closeTo(consumption.getValue(), 30, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("verifies substance replacement over time", "/examples/basic_replace.qta", [
      (result, assert) => {
        // Check year 1 consumption
        const recordAYear1 = getResult(result, "Sim", 1, 0, "Test", "Sub A");
        const consumptionAYear1 = recordAYear1.getGhgConsumption();
        assert.closeTo(consumptionAYear1.getValue(), 10000, 0.0001);
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
        assert.closeTo(consumptionBYear10.getValue(), 1000, 0.0001);
        assert.deepEqual(consumptionBYear10.getUnits(), "tCO2e");
      },
    ]);

    buildTest("verifies simple substance replacement", "/examples/basic_replace_simple.qta", [
      (result, assert) => {
        // Check year 1 - no replacement yet
        const recordAYear1 = getResult(result, "Sim", 1, 0, "Test", "Sub A");
        const consumptionAYear1 = recordAYear1.getGhgConsumption();
        assert.closeTo(consumptionAYear1.getValue(), 10000000, 0.0001);
        assert.deepEqual(consumptionAYear1.getUnits(), "tCO2e");

        const recordBYear1 = getResult(result, "Sim", 1, 0, "Test", "Sub B");
        const consumptionBYear1 = recordBYear1.getGhgConsumption();
        assert.closeTo(consumptionBYear1.getValue(), 0, 0.0001);
        assert.deepEqual(consumptionBYear1.getUnits(), "tCO2e");

        // Check year 10 - replacement should result in complete shift from A to B
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
      "/examples/basic_replace_units.qta",
      [
        (result, assert) => {
          // Check year 1 - no replacement yet
          const recordAYear1 = getResult(result, "Sim", 1, 0, "Test", "Sub A");
          const consumptionAYear1 = recordAYear1.getGhgConsumption();
          assert.closeTo(consumptionAYear1.getValue(), 10000000, 0.0001);
          assert.deepEqual(consumptionAYear1.getUnits(), "tCO2e");

          const recordBYear1 = getResult(result, "Sim", 1, 0, "Test", "Sub B");
          const consumptionBYear1 = recordBYear1.getGhgConsumption();
          assert.closeTo(consumptionBYear1.getValue(), 0, 0.0001);
          assert.deepEqual(consumptionBYear1.getUnits(), "tCO2e");

          // Check year 10 - replacement active for years 5-10 (6 years total)
          // Sub A: Original 100 mt, replaced 6 × (1000 units × 10 kg/unit) = 60 mt
          // Remaining: 40 mt × 100 tCO2e/mt = 4,000,000 tCO2e
          const recordAYear10 = getResult(result, "Sim", 10, 0, "Test", "Sub A");
          const consumptionAYear10 = recordAYear10.getGhgConsumption();
          assert.closeTo(consumptionAYear10.getValue(), 4000000, 0.0001);
          assert.deepEqual(consumptionAYear10.getUnits(), "tCO2e");

          // Sub B: Added 6 × (1000 units × 20 kg/unit) = 120 mt
          // Total: 120 mt × 10 tCO2e/mt = 1,200,000 tCO2e
          const recordBYear10 = getResult(result, "Sim", 10, 0, "Test", "Sub B");
          const consumptionBYear10 = recordBYear10.getGhgConsumption();
          assert.closeTo(consumptionBYear10.getValue(), 1200000, 0.0001);
          assert.deepEqual(consumptionBYear10.getUnits(), "tCO2e");
        },
      ],
    );

    buildTest("handles replace by units", "/examples/replace_units.qta", [
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

    buildTest("handles replace by kg", "/examples/replace_kg.qta", [
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

    buildTest("runs trials", "/examples/trials.qta", [
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

    buildTest("cold starts with equipment", "/examples/cold_start_equipment.qta", [
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

    buildTest(
      "checks order of operations in initialization",
      "/examples/order_check_volume.qta",
      [
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

    buildTest("runs case study", "/examples/case_study_test.qta", [
      (result, assert) => {
        // Test that the case study simulation completes successfully
        assert.ok(result.length > 0, "Case study should produce simulation results");
      },
      (result, assert) => {
        // Test that at least one stream for one substance/application pair is non-zero in 2030
        const record = getResult(result, "BAU Uncertainty", 2030, 0,
          "Domestic Refrigeration", "HFC-134a");
        const consumption = record.getGhgConsumption();
        assert.ok(consumption.getValue() > 0,
          "Should have non-zero consumption for Domestic Refrigeration HFC-134a in 2030");
      },
    ]);

    buildTest("tests initialization by units", "/examples/init_units.qta", [
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
      "/examples/order_check_units.qta",
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

    buildTest("tests cap with units includes recharge on top", "/examples/cap_units.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const domestic = record.getDomestic();

        // With recharge on top: 50 units * 2 kg/unit + (20 units * 10% * 1 kg/unit) = 102 kg
        // Since original value is 100 kg and cap should be 102 kg, no change expected
        assert.closeTo(domestic.getValue(), 100, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
    ]);

    buildTest("tests cap with kg works without recharge addition", "/examples/cap_kg.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const domestic = record.getDomestic();

        // Cap at 50 kg should reduce from 100 kg to 50 kg
        assert.closeTo(domestic.getValue(), 50, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
    ]);

    buildTest("tests cap with units displacement", "/examples/cap_displace_units.qta", [
      (result, assert) => {
        // Check that sub_a domestic was capped
        const recordSubA = getResult(result, "result", 1, 0, "test", "sub_a");
        const domesticSubA = recordSubA.getDomestic();

        // Cap is 5 units, with proportional recharge: 5 units * 10 kg/unit +
        // (20 kg recharge * 66.67% domestic distribution) = 50 + 13.33 = 63.33 kg
        // Original was 100 kg, so should be capped to 63.33 kg
        assert.closeTo(domesticSubA.getValue(), 63.333333333333336, 0.0001);
        assert.deepEqual(domesticSubA.getUnits(), "kg");

        // Check displacement to sub_b
        const recordSubB = getResult(result, "result", 1, 0, "test", "sub_b");
        const domesticSubB = recordSubB.getDomestic();

        // With unit-based displacement: 36.67 kg reduction in sub_a =
        // 36.67 kg / 10 kg/unit = 3.67 units
        // 3.67 units displaced to sub_b = 3.67 units * 20 kg/unit = 73.33 kg
        // Original sub_b: 200 kg, Final sub_b: 200 kg + 73.33 kg = 273.33 kg
        assert.closeTo(domesticSubB.getValue(), 273.3333333333333, 0.0001);
        assert.deepEqual(domesticSubB.getUnits(), "kg");
      },
    ]);

    buildTest(
      "tests unit-to-unit displacement conversion",
      "/examples/cap_displace_unit_conversion.qta",
      [
        (result, assert) => {
        // Check that sub_a domestic was capped
          const recordSubA = getResult(result, "result", 1, 0, "test", "sub_a");
          const domesticSubA = recordSubA.getDomestic();

          // Cap is 5 units, with recharge: 5 units * 10 kg/unit +
          // (20 units * 10% * 10 kg/unit) = 50 + 20 = 70 kg
          // Original was 30 units * 10 kg/unit = 300 kg, so should be capped to 70 kg
          // Reduction: 300 - 70 = 230 kg
          assert.closeTo(domesticSubA.getValue(), 70, 0.0001);
          assert.deepEqual(domesticSubA.getUnits(), "kg");

          // Check displacement to sub_b
          const recordSubB = getResult(result, "result", 1, 0, "test", "sub_b");
          const domesticSubB = recordSubB.getDomestic();

          // With the fix, displacement should be unit-based:
          // 230 kg reduction in sub_a = 230 kg / 10 kg/unit = 23 units
          // 23 units displaced to sub_b = 23 units * 20 kg/unit = 460 kg
          // Original sub_b: 10 units * 20 kg/unit = 200 kg
          // Final sub_b: 200 kg + 460 kg = 660 kg
          assert.closeTo(domesticSubB.getValue(), 660, 0.0001);
          assert.deepEqual(domesticSubB.getUnits(), "kg");
        },
      ]);

    buildTest("tests floor with units includes recharge on top", "/examples/floor_units.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const domestic = record.getDomestic();

        // With recharge on top: 50 units * 2 kg/unit + (20 units * 10% * 1 kg/unit) = 102 kg
        // Since original value is 10 kg and floor should be 102 kg, should increase to 102 kg
        assert.closeTo(domestic.getValue(), 102, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
    ]);

    buildTest("tests floor with kg works without recharge addition", "/examples/floor_kg.qta", [
      (result, assert) => {
        const record = getResult(result, "result", 1, 0, "test", "test");
        const domestic = record.getDomestic();

        // Floor at 50 kg should increase from 10 kg to 50 kg
        assert.closeTo(domestic.getValue(), 50, 0.0001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
    ]);

    buildTest(
      "tests ordering-sensitive recharge emissions issue",
      "/examples/ordering_sensitive_emissions.qta", [
        (result, assert) => {
          // Test SubA (tCO2e equals comes before recharge)
          const recordA = getResult(result, BAU_NAME, 2035, 0, "test", "SubA");
          const emissionsA = recordA.getRechargeEmissions();
          assert.ok(emissionsA.getValue() > 0, "A > 0");
          assert.deepEqual(emissionsA.getUnits(), "tCO2e");
        },
        (result, assert) => {
          // Test SubB (recharge comes before tCO2e equals)
          const recordB = getResult(result, BAU_NAME, 2035, 0, "test", "SubB");
          const emissionsB = recordB.getRechargeEmissions();
          assert.ok(emissionsB.getValue() > 0, "B > 0");
          assert.deepEqual(emissionsB.getUnits(), "tCO2e");
        },
        (result, assert) => {
          // Test SubC (recharge comes before tCO2e equals)
          const recordC = getResult(result, BAU_NAME, 2035, 0, "test", "SubC");
          const emissionsC = recordC.getRechargeEmissions();
          assert.ok(emissionsC.getValue() > 0, "C > 0");
          assert.deepEqual(emissionsC.getUnits(), "tCO2e");
        },
        (result, assert) => {
          // Test SubD (recharge comes before tCO2e equals)
          const recordD = getResult(result, BAU_NAME, 2035, 0, "test", "SubD");
          const emissionsD = recordD.getRechargeEmissions();
          assert.ok(emissionsD.getValue() > 0, "D > 0");
          assert.deepEqual(emissionsD.getUnits(), "tCO2e");
        },
        (result, assert) => {
          const recordA = getResult(result, BAU_NAME, 2035, 0, "test", "SubA");
          const recordB = getResult(result, BAU_NAME, 2035, 0, "test", "SubB");
          const recordC = getResult(result, BAU_NAME, 2035, 0, "test", "SubC");
          const recordD = getResult(result, BAU_NAME, 2035, 0, "test", "SubD");
          const emissionsA = recordA.getRechargeEmissions();
          const emissionsB = recordB.getRechargeEmissions();
          const emissionsC = recordC.getRechargeEmissions();
          const emissionsD = recordD.getRechargeEmissions();
          assert.closeTo(emissionsA.getValue(), emissionsB.getValue(), 0.001, "A = B");
          assert.closeTo(emissionsB.getValue(), emissionsC.getValue(), 0.001, "B = C");
          assert.closeTo(emissionsC.getValue(), emissionsD.getValue(), 0.001, "C = D");
        },
      ],
    );

    buildTest(
      "tests ordering-sensitive eol emissions issue",
      "/examples/ordering_sensitive_emissions.qta", [
        (result, assert) => {
          // Test SubA (tCO2e equals comes before recharge)
          const recordA = getResult(result, BAU_NAME, 2035, 0, "test", "SubA");
          const emissionsA = recordA.getEolEmissions();
          assert.ok(emissionsA.getValue() > 0, "A > 0");
          assert.deepEqual(emissionsA.getUnits(), "tCO2e");
        },
        (result, assert) => {
          // Test SubB (recharge comes before tCO2e equals)
          const recordB = getResult(result, BAU_NAME, 2035, 0, "test", "SubB");
          const emissionsB = recordB.getEolEmissions();
          assert.ok(emissionsB.getValue() > 0, "B > 0");
          assert.deepEqual(emissionsB.getUnits(), "tCO2e");
        },
        (result, assert) => {
          // Test SubC (recharge comes before tCO2e equals)
          const recordB = getResult(result, BAU_NAME, 2035, 0, "test", "SubC");
          const emissionsB = recordB.getEolEmissions();
          assert.ok(emissionsB.getValue() > 0, "C > 0");
          assert.deepEqual(emissionsB.getUnits(), "tCO2e");
        },
        (result, assert) => {
          // Test SubD (recharge comes before tCO2e equals)
          const recordB = getResult(result, BAU_NAME, 2035, 0, "test", "SubD");
          const emissionsB = recordB.getEolEmissions();
          assert.ok(emissionsB.getValue() > 0, "D > 0");
          assert.deepEqual(emissionsB.getUnits(), "tCO2e");
        },
        (result, assert) => {
          const recordA = getResult(result, BAU_NAME, 2035, 0, "test", "SubA");
          const recordB = getResult(result, BAU_NAME, 2035, 0, "test", "SubB");
          const recordC = getResult(result, BAU_NAME, 2035, 0, "test", "SubC");
          const recordD = getResult(result, BAU_NAME, 2035, 0, "test", "SubD");
          const emissionsA = recordA.getEolEmissions();
          const emissionsB = recordB.getEolEmissions();
          const emissionsC = recordC.getEolEmissions();
          const emissionsD = recordD.getEolEmissions();
          assert.closeTo(emissionsA.getValue(), emissionsB.getValue(), 0.001, "A = B");
          assert.closeTo(emissionsB.getValue(), emissionsC.getValue(), 0.001, "B = C");
          assert.closeTo(emissionsC.getValue(), emissionsD.getValue(), 0.001, "C = D");
        },
      ],
    );

    buildTest("tests cap displace bug with kg", "/examples/cap_displace_bug_kg.qta", [
      (result, assert) => {
        // Check HFC-134a new equipment is zero in 2030
        const recordHFC2030 = getResult(result, "Equipment Import Ban", 2030, 0,
          "Commercial Refrigeration", "HFC-134a");
        const newEquipmentHFC2030 = recordHFC2030.getPopulationNew();

        // Should be zero new equipment
        assert.closeTo(newEquipmentHFC2030.getValue(), 0, 0.0001);
      },
      (result, assert) => {
        // Check HFC-134a consumption is zero in 2030
        const recordHFC2030 = getResult(result, "Equipment Import Ban", 2030, 0,
          "Commercial Refrigeration", "HFC-134a");
        const domesticConsumption = recordHFC2030.getDomesticConsumption();
        const importConsumption = recordHFC2030.getImportConsumption();
        const totalConsumption = domesticConsumption.getValue() + importConsumption.getValue();

        // Should be zero consumption
        assert.closeTo(totalConsumption, 0, 0.0001);
      },
      (result, assert) => {
        // Check R-404A new equipment is not zero in year 2035
        const recordR404A2035 = getResult(result, "Equipment Import Ban", 2035, 0,
          "Commercial Refrigeration", "R-404A");
        const newEquipmentR404A2035 = recordR404A2035.getPopulationNew();

        // Should have new equipment due to displacement
        assert.ok(newEquipmentR404A2035.getValue() > 0);
      },
    ]);

    buildTest("tests cap displace bug with units", "/examples/cap_displace_bug_units.qta", [
      (result, assert) => {
        // Check HFC-134a new equipment is zero in 2030
        const recordHFC2030 = getResult(result, "Equipment Import Ban", 2030, 0,
          "Commercial Refrigeration", "HFC-134a");
        const newEquipmentHFC2030 = recordHFC2030.getPopulationNew();

        // Should be zero new equipment
        assert.closeTo(newEquipmentHFC2030.getValue(), 0, 0.0001);
      },
      (result, assert) => {
        // Check R-404A new equipment is not zero in year 2035
        const recordR404A2035 = getResult(result, "Equipment Import Ban", 2035, 0,
          "Commercial Refrigeration", "R-404A");
        const newEquipmentR404A2035 = recordR404A2035.getPopulationNew();

        // Should have new equipment due to displacement
        assert.ok(newEquipmentR404A2035.getValue() > 0);
      },
    ]);

    buildTest("tests cap displace bug with recharge and units",
      "/examples/cap_displace_with_recharge_units.qta", [
        (result, assert) => {
        // Check HFC-134a new equipment is zero in 2030
          const recordHFC2030 = getResult(result, "Equipment Import Ban", 2030, 0,
            "Commercial Refrigeration", "HFC-134a");
          const newEquipmentHFC2030 = recordHFC2030.getPopulationNew();

          // Should be zero new equipment
          assert.closeTo(newEquipmentHFC2030.getValue(), 0, 0.0001);
        },
        (result, assert) => {
        // Check HFC-134a consumption is NOT zero in 2030 due to recharge
          const recordHFC2030 = getResult(result, "Equipment Import Ban", 2030, 0,
            "Commercial Refrigeration", "HFC-134a");
          const domesticConsumption = recordHFC2030.getDomesticConsumption();
          const importConsumption = recordHFC2030.getImportConsumption();
          const totalConsumption = domesticConsumption.getValue() + importConsumption.getValue();

          // Should have consumption due to recharge
          assert.ok(totalConsumption > 0,
            "Total consumption for HFC-134a should be greater than zero in 2030 due to recharge");
        },
        (result, assert) => {
        // Check R-404A new equipment is not zero in year 2035
          const recordR404A2035 = getResult(result, "Equipment Import Ban", 2035, 0,
            "Commercial Refrigeration", "R-404A");
          const newEquipmentR404A2035 = recordR404A2035.getPopulationNew();

          // Should have new equipment due to displacement
          assert.ok(newEquipmentR404A2035.getValue() > 0);
        },
      ]);
    buildTest("runs minimal interpreter example", "/examples/minimal_interpreter.qta", [
      (result, assert) => {
        // Check year 1
        const record1 = getResult(result, BAU_NAME, 1, 0, "testApp", "testSubstance");
        const domestic1 = record1.getDomestic();
        assert.closeTo(domestic1.getValue(), 100000, 0.0001); // 100 mt = 100000 kg
        assert.deepEqual(domestic1.getUnits(), "kg");

        // Check year 2
        const record2 = getResult(result, BAU_NAME, 2, 0, "testApp", "testSubstance");
        const domestic2 = record2.getDomestic();
        assert.closeTo(domestic2.getValue(), 100000, 0.0001);
        assert.deepEqual(domestic2.getUnits(), "kg");

        // Check year 3
        const record3 = getResult(result, BAU_NAME, 3, 0, "testApp", "testSubstance");
        const domestic3 = record3.getDomestic();
        assert.closeTo(domestic3.getValue(), 100000, 0.0001);
        assert.deepEqual(domestic3.getUnits(), "kg");
      },
    ]);

    buildTest(
      "tests cap displace units magnitude",
      "/examples/cap_displace_units_magnitude.qta",
      [
        (result, assert) => {
        // Check 2025 new equipment - R-404A should have 200 units, HFC-134a should have 400 units
          const recordR404A2025 = getResult(result, "Import Ban", 2025, 0, "Test", "R-404A");
          const newEquipmentR404A2025 = recordR404A2025.getPopulationNew();
          assert.closeTo(newEquipmentR404A2025.getValue(), 200, 0.0001);
          assert.deepEqual(newEquipmentR404A2025.getUnits(), "units");

          const recordHFC2025 = getResult(result, "Import Ban", 2025, 0, "Test", "HFC-134a");
          const newEquipmentHFC2025 = recordHFC2025.getPopulationNew();
          assert.closeTo(newEquipmentHFC2025.getValue(), 400, 0.0001);
          assert.deepEqual(newEquipmentHFC2025.getUnits(), "units");
        },
        (result, assert) => {
        // Check 2029 new equipment - HFC-134a should have 0 units (capped),
        // R-404A should have 600 units (displaced)
          const recordHFC2029 = getResult(result, "Import Ban", 2029, 0, "Test", "HFC-134a");
          const newEquipmentHFC2029 = recordHFC2029.getPopulationNew();
          assert.closeTo(newEquipmentHFC2029.getValue(), 0, 0.0001);
          assert.deepEqual(newEquipmentHFC2029.getUnits(), "units");

          const recordR404A2029 = getResult(result, "Import Ban", 2029, 0, "Test", "R-404A");
          const newEquipmentR404A2029 = recordR404A2029.getPopulationNew();
          assert.closeTo(newEquipmentR404A2029.getValue(), 600, 0.0001);
          assert.deepEqual(newEquipmentR404A2029.getUnits(), "units");
        },
        (result, assert) => {
        // Check 2030 new equipment - same as 2029 (HFC-134a should have 0 units,
        // R-404A should have 600 units)
          const recordHFC2030 = getResult(result, "Import Ban", 2030, 0, "Test", "HFC-134a");
          const newEquipmentHFC2030 = recordHFC2030.getPopulationNew();
          assert.closeTo(newEquipmentHFC2030.getValue(), 0, 0.0001);
          assert.deepEqual(newEquipmentHFC2030.getUnits(), "units");

          const recordR404A2030 = getResult(result, "Import Ban", 2030, 0, "Test", "R-404A");
          const newEquipmentR404A2030 = recordR404A2030.getPopulationNew();
          assert.closeTo(newEquipmentR404A2030.getValue(), 600, 0.0001);
          assert.deepEqual(newEquipmentR404A2030.getUnits(), "units");
        },
      ]);

    buildTest(
      "tests recover with sales displacement",
      "/examples/recover_displace_sales_kg.qta",
      [
        (result, assert) => {
        // Check that sales were displaced (reduced) for the same substance
          const recordSubA = getResult(result, "result", 1, 0, "test", "sub_a");
          const domesticSubA = recordSubA.getDomestic();
          const importSubA = recordSubA.getImport();

          // Calculate total sales (domestic + import)
          const totalSales = domesticSubA.getValue() + importSubA.getValue();

          // Original sales: 150 kg (100 domestic + 50 import)
          // After 20 kg displacement with sales targeting (still allowed):
          // Actual result with current calculation precision
          assert.closeTo(totalSales, 129.34959349593495, 0.0001);
        },
      ]);

    // Test that substance displacement now throws an exception
    QUnit.test("tests recover with substance displacement", (assert) => {
      const done = assert.async();
      loadRemote("/examples/recover_displace_substance.qta").then(async (content) => {
        assert.ok(content.length > 0);

        try {
          const backendResult = await wasmBackend.execute(content);
          // If we get here, the execution didn't throw an exception as expected
          assert.ok(false, "Expected UnsupportedOperationException for substance displacement");
        } catch (e) {
          // Check that the error message contains our expected displacement blocking message
          const expectedMessage = "Displacement target 'sub_b' is not currently " +
            "supported in recycling operations";
          assert.ok(e.message.includes(expectedMessage),
            "Should throw exception for substance displacement: " + e.message);
        }

        done();
      });
    });

    buildTest("tests imports with recharge attribution", "/examples/import_recharge_test.qta", [
      (result, assert) => {
        // Year 2: Test default behavior (importer attribution)
        // Actual result shows 100 kg import (includes all import activity)
        const record2 = getResult(result, "BAU", 2, 0, "ImportRechargeTest", "TestSubstance");
        const import2 = record2.getImport();
        assert.closeTo(import2.getValue(), 100, 0.0001);
        assert.deepEqual(import2.getUnits(), "kg");
      },
      (result, assert) => {
        // Year 2: Test exporter attribution using AttributeToExporterResult
        // With exporter attribution, initial charge should be subtracted from imports
        const record2 = getResult(result, "BAU", 2, 0, "ImportRechargeTest", "TestSubstance");
        const exporterAttributed2 = new AttributeToExporterResult(record2);
        const exporterImport2 = exporterAttributed2.getImport();

        // Should be less than the full 100 kg due to initial charge attribution to exporter
        assert.ok(exporterImport2.getValue() > 0, "Import should be greater than 0");
        assert.ok(exporterImport2.getValue() < 100,
          "Import should be less than 100 kg due to exporter attribution");
        assert.deepEqual(exporterImport2.getUnits(), "kg");
      },
    ]);

    buildTest("tests basic export functionality", "/examples/basic_exporter.qta", [
      (result, assert) => {
        // Test that the basic exporter simulation completes successfully
        assert.ok(result.length > 0, "Basic exporter should produce simulation results");
      },
      (result, assert) => {
        // Test that export stream shows the expected value
        const record = getResult(
          result,
          "exporter scenario",
          1,
          0,
          "commercial refrigeration",
          "HFC-134a",
        );
        const exportValue = record.getExport();
        assert.closeTo(exportValue.getValue(), 200000, 0.0001); // 200 mt = 200000 kg
        assert.deepEqual(exportValue.getUnits(), "kg");
      },
      (result, assert) => {
        // Test that export consumption is calculated correctly
        const record = getResult(
          result,
          "exporter scenario",
          1,
          0,
          "commercial refrigeration",
          "HFC-134a",
        );
        const exportConsumption = record.getExportConsumption();
        // Export consumption: 200 mt * 500 tCO2e/mt = 100,000 tCO2e
        assert.closeTo(exportConsumption.getValue(), 100000, 0.1);
        assert.deepEqual(exportConsumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test that TradeSupplement export fields are populated
        const record = getResult(
          result,
          "exporter scenario",
          1,
          0,
          "commercial refrigeration",
          "HFC-134a",
        );
        const tradeSupplement = record.getTradeSupplement();
        // Test that TradeSupplement export fields are populated (like Java test)
        const exportInitialChargeValue = tradeSupplement.getExportInitialChargeValue();
        assert.ok(exportInitialChargeValue, "Export initial charge value should not be null");
        const exportInitialChargeConsumption = tradeSupplement.getExportInitialChargeConsumption();
        assert.ok(
          exportInitialChargeConsumption,
          "Export initial charge consumption should not be null",
        );
      },
    ]);

    // kgCO2e integration tests
    buildTest("validates kgCO2e grammar parsing", "/examples/kgco2e_validation.qta", [
      (result, assert) => {
        // Test that kgCO2e units parse successfully without execution errors
        const record = getResult(result, "grammar_validation_test", 1, 0,
          "kgco2e_test", "test_substance");

        // Basic validation that the simulation produced results
        assert.ok(record, "Should have simulation result for kgCO2e grammar test");
        // Validate domestic calculation
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 100, 0.001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
      (result, assert) => {
        // Test GHG consumption calculation
        const record = getResult(result, "grammar_validation_test", 1, 0,
          "kgco2e_test", "test_substance");
        const consumption = record.getGhgConsumption();
        // 100 kg * 1430 kgCO2e/kg = 143,000 kgCO2e = 143 tCO2e
        assert.closeTo(consumption.getValue(), 143, 0.1);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("validates kgCO2e units end-to-end", "/examples/kgco2e_engine_test.qta", [
      (result, assert) => {
        // Test year 1 - initial setup with kgCO2e units
        const record = getResult(result, "kgco2e_test", 1, 0,
          "kgco2e_engine_test", "test_substance_kgco2e");
        const consumption = record.getGhgConsumption();
        // Validate GHG consumption calculation with kgCO2e input
        // 100 kg domestic * 1430 kgCO2e/kg = 143,000 kgCO2e = 143 tCO2e (internal)
        assert.closeTo(consumption.getValue(), 143, 0.1);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test year 1 - domestic consumption validation
        const record = getResult(result, "kgco2e_test", 1, 0,
          "kgco2e_engine_test", "test_substance_kgco2e");
        const domestic = record.getDomestic();
        // Validate domestic volume is correctly processed
        assert.closeTo(domestic.getValue(), 100, 0.001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
      (result, assert) => {
        // Test year 2 - validate recharge calculations with kgCO2e
        const record = getResult(result, "kgco2e_test", 2, 0,
          "kgco2e_engine_test", "test_substance_kgco2e");
        const rechargeEmissions = record.getRechargeEmissions();
        // Validate recharge emissions are calculated correctly
        // With 10% recharge: additional emissions should be non-zero
        assert.ok(rechargeEmissions.getValue() > 0, "Recharge emissions should be greater than 0");
        assert.deepEqual(rechargeEmissions.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test year 3 - validate multi-year consistency
        const record = getResult(result, "kgco2e_test", 3, 0,
          "kgco2e_engine_test", "test_substance_kgco2e");
        const consumption = record.getGhgConsumption();
        // Consumption should remain consistent with kgCO2e calculations
        assert.closeTo(consumption.getValue(), 143, 0.1);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test population calculation consistency
        const record = getResult(result, "kgco2e_test", 1, 0,
          "kgco2e_engine_test", "test_substance_kgco2e");
        const equipment = record.getPopulation();
        // Validate equipment population calculation is unaffected by kgCO2e units
        assert.ok(equipment.getValue() > 0, "Equipment population should be greater than 0");
        assert.deepEqual(equipment.getUnits(), "units");
      },
    ]);

    buildTest("validates kgCO2e to tCO2e conversion accuracy", "/examples/kgco2e_engine_test.qta", [
      (result, assert) => {
        // Test conversion accuracy: 1430 kgCO2e/kg should equal 1.43 tCO2e/kg internally
        const record = getResult(result, "kgco2e_test", 1, 0,
          "kgco2e_engine_test", "test_substance_kgco2e");
        const consumption = record.getGhgConsumption();
        // Precise validation of conversion calculation
        // 100 kg * 1430 kgCO2e/kg = 143,000 kgCO2e = 143.0 tCO2e exactly
        assert.closeTo(consumption.getValue(), 143.0, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    // tCO2e integration tests
    buildTest("validates tCO2e grammar parsing", "/examples/tco2e_validation.qta", [
      (result, assert) => {
        // Test that tCO2e units parse successfully without execution errors
        const record = getResult(result, "grammar_validation_test", 1, 0,
          "tco2e_test", "test_substance_tco2e");

        // Basic validation that the simulation produced results
        assert.ok(record, "Should have simulation result for tCO2e grammar test");
        // Validate domestic calculation
        const domestic = record.getDomestic();
        assert.closeTo(domestic.getValue(), 100, 0.001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
      (result, assert) => {
        // Test GHG consumption calculation
        const record = getResult(result, "grammar_validation_test", 1, 0,
          "tco2e_test", "test_substance_tco2e");
        const consumption = record.getGhgConsumption();
        // 100 kg * 1.43 tCO2e/kg = 143.0 tCO2e
        assert.closeTo(consumption.getValue(), 143, 0.1);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("validates tCO2e units end-to-end", "/examples/tco2e_engine_test.qta", [
      (result, assert) => {
        // Test year 1 - initial setup with tCO2e units
        const record = getResult(result, "tco2e_test", 1, 0,
          "tco2e_engine_test", "test_substance_tco2e");
        const consumption = record.getGhgConsumption();
        // Validate GHG consumption calculation with tCO2e input
        // 100 kg domestic * 1.43 tCO2e/kg = 143.0 tCO2e (internal)
        assert.closeTo(consumption.getValue(), 143, 0.1);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test year 1 - domestic consumption validation
        const record = getResult(result, "tco2e_test", 1, 0,
          "tco2e_engine_test", "test_substance_tco2e");
        const domestic = record.getDomestic();
        // Validate domestic volume is correctly processed
        assert.closeTo(domestic.getValue(), 100, 0.001);
        assert.deepEqual(domestic.getUnits(), "kg");
      },
      (result, assert) => {
        // Test year 2 - validate recharge calculations with tCO2e
        const record = getResult(result, "tco2e_test", 2, 0,
          "tco2e_engine_test", "test_substance_tco2e");
        const rechargeEmissions = record.getRechargeEmissions();
        // Validate recharge emissions are calculated correctly
        // With 10% recharge: additional emissions should be non-zero
        assert.ok(rechargeEmissions.getValue() > 0, "Recharge emissions should be greater than 0");
        assert.deepEqual(rechargeEmissions.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test year 3 - validate multi-year consistency
        const record = getResult(result, "tco2e_test", 3, 0,
          "tco2e_engine_test", "test_substance_tco2e");
        const consumption = record.getGhgConsumption();
        // Consumption should remain consistent with tCO2e calculations
        assert.closeTo(consumption.getValue(), 143, 0.1);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
      (result, assert) => {
        // Test population calculation consistency
        const record = getResult(result, "tco2e_test", 1, 0,
          "tco2e_engine_test", "test_substance_tco2e");
        const equipment = record.getPopulation();
        // Validate equipment population calculation is unaffected by tCO2e units
        assert.ok(equipment.getValue() > 0, "Equipment population should be greater than 0");
        assert.deepEqual(equipment.getUnits(), "units");
      },
    ]);

    buildTest("validates tCO2e conversion accuracy", "/examples/tco2e_engine_test.qta", [
      (result, assert) => {
        // Test that tCO2e values are processed directly without conversion
        const record = getResult(result, "tco2e_test", 1, 0,
          "tco2e_engine_test", "test_substance_tco2e");
        const consumption = record.getGhgConsumption();
        // Precise validation of tCO2e calculation
        // 100 kg * 1.43 tCO2e/kg = 143.0 tCO2e exactly
        assert.closeTo(consumption.getValue(), 143.0, 0.0001);
        assert.deepEqual(consumption.getUnits(), "tCO2e");
      },
    ]);
  });
}

export {buildIntegrationTests};
