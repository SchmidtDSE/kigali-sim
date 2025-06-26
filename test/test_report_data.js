import {WasmBackend, WasmLayer} from "wasm_backend";
import {ReportDataWrapper} from "report_data";
import {FilterSet} from "user_config";

function loadRemote(path) {
  return fetch(path).then((response) => response.text());
}

function buildReportDataTests() {
  QUnit.module("ReportData", function () {
    // Shared WASM backend instances to avoid re-initialization overhead
    const wasmLayer = new WasmLayer();
    const wasmBackend = new WasmBackend(wasmLayer);

    const buildTest = (name, filepath, checks) => {
      QUnit.test(name, (assert) => {
        const done = assert.async();
        loadRemote(filepath).then(async (content) => {
          assert.ok(content.length > 0);

          try {
            // Execute using WASM backend instead of old JS engine
            const programResult = await wasmBackend.execute(content);
            const programResultWrapped = new ReportDataWrapper(programResult);
            checks.forEach((check) => {
              check(programResultWrapped, assert);
            });
          } catch (e) {
            console.log(e);
            assert.ok(false, "Execution failed: " + e.message);
          }

          done();
        });
      });
    };

    QUnit.test("parses submetric all", (assert) => {
      const filterSet = new FilterSet(
        null,
        null,
        null,
        null,
        "sales:all:mt / yr",
        null,
        null,
        false,
      );
      assert.deepEqual(filterSet.getFullMetricName(), "sales:all:mt / yr");
      assert.deepEqual(filterSet.getMetric(), "sales");
      assert.deepEqual(filterSet.getSubMetric(), "all");
      assert.deepEqual(filterSet.getUnits(), "mt / yr");
    });

    QUnit.test("parses submetric import", (assert) => {
      const filterSet = new FilterSet(
        null,
        null,
        null,
        null,
        "sales:import:mt / yr",
        null,
        null,
        false,
      );
      assert.deepEqual(filterSet.getFullMetricName(), "sales:import:mt / yr");
      assert.deepEqual(filterSet.getMetric(), "sales");
      assert.deepEqual(filterSet.getSubMetric(), "import");
      assert.deepEqual(filterSet.getUnits(), "mt / yr");
    });

    buildTest("runs the base script", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        assert.notDeepEqual(result, null);
      },
    ]);

    buildTest("gets the scenarios", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(
          null,
          null,
          null,
          null,
          "sales:import:mt / yr",
          null,
          null,
          false,
        );
        const years = result.getScenarios(filterSet);
        assert.equal(years.size, 2);
        assert.ok(years.has("bau"));
        assert.ok(years.has("sim"));
      },
    ]);

    buildTest("gets all the years", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, null, null, null, null, null, false);
        const years = result.getYears(filterSet);
        assert.equal(years.size, 3);
        assert.ok(years.has(1));
        assert.ok(years.has(2));
        assert.ok(years.has(3));
      },
    ]);

    buildTest("gets years matching filter", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, "sim", null, null, null, null, null, false);
        const years = result.getYears(filterSet);
        assert.equal(years.size, 2);
        assert.ok(years.has(1));
        assert.ok(years.has(2));
      },
    ]);

    buildTest("gets all applications", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, null, null, null, null, null, false);
        const years = result.getApplications(filterSet);
        assert.equal(years.size, 2);
        assert.ok(years.has("appA"));
        assert.ok(years.has("appB"));
      },
    ]);

    buildTest("gets applications with filter", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, null, "subA", null, null, null, false);
        const years = result.getApplications(filterSet);
        assert.equal(years.size, 1);
        assert.ok(years.has("appA"));
      },
    ]);

    buildTest("gets all substances", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, null, null, null, null, null, false);
        const years = result.getSubstances(filterSet);
        assert.equal(years.size, 2);
        assert.ok(years.has("subA"));
        assert.ok(years.has("subB"));
      },
    ]);

    buildTest("gets substances matching filter", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, "appA", null, null, null, null, false);
        const years = result.getSubstances(filterSet);
        assert.equal(years.size, 1);
        assert.ok(years.has("subA"));
      },
    ]);

    buildTest("gets consumption", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(1, "bau", null, null, null, null, null, true);
        const totalConsumption = result.getGhgConsumption(filterSet);
        assert.closeTo(totalConsumption.getValue(), 1500, 0.0001);
        assert.deepEqual(totalConsumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("gets consumption with attribution", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(1, "bau", null, null, null, null, null, false);
        const totalConsumption = result.getGhgConsumption(filterSet);
        assert.closeTo(totalConsumption.getValue(), 1500, 0.0001);
        assert.deepEqual(totalConsumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("gets sales", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(1, "bau", null, null, null, null, null, true);
        const totalSales = result.getSales(filterSet);
        assert.closeTo(totalSales.getValue(), 200000, 0.0001);
        assert.deepEqual(totalSales.getUnits(), "kg");
      },
    ]);

    buildTest("gets sales with attribution", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(1, "bau", null, null, null, null, null, false);
        const totalSales = result.getSales(filterSet);
        assert.closeTo(totalSales.getValue(), 200000, 0.0001);
        assert.deepEqual(totalSales.getUnits(), "kg");
      },
    ]);

    buildTest("gets sales by metric", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(
          1,
          "bau",
          null,
          null,
          "sales:all:kg / yr",
          null,
          null,
          true,
        );
        const totalSales = result.getMetric(filterSet);
        assert.closeTo(totalSales.getValue(), 200000, 0.0001);
        assert.deepEqual(totalSales.getUnits(), "kg / yr");
      },
    ]);

    buildTest("gets sales by metric with attribution", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(
          1,
          "bau",
          null,
          null,
          "sales:all:kg / yr",
          null,
          null,
          false,
        );
        const totalSales = result.getMetric(filterSet);
        assert.closeTo(totalSales.getValue(), 200000, 0.0001);
        assert.deepEqual(totalSales.getUnits(), "kg / yr");
      },
    ]);

    buildTest("gets sales by metric split", "/examples/multiple_with_policies_split.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(
          1,
          "bau",
          null,
          null,
          "sales:all:kg / yr",
          null,
          null,
          true,
        );
        const totalSales = result.getMetric(filterSet);
        assert.closeTo(totalSales.getValue(), 200000, 0.0001);
        assert.deepEqual(totalSales.getUnits(), "kg / yr");
      },
    ]);

    buildTest(
      "gets sales by metric split with attribution",
      "/examples/multiple_with_policies_split.qta",
      [
        (result, assert) => {
          const filterSet = new FilterSet(
            1,
            "bau",
            null,
            null,
            "sales:all:kg / yr",
            null,
            null,
            false,
          );
          const totalSales = result.getMetric(filterSet);
          assert.ok(totalSales.getValue() < 200000);
          assert.deepEqual(totalSales.getUnits(), "kg / yr");
        },
      ],
    );

    buildTest("gets imports by metric", "/examples/multiple_with_policies_split.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(
          1,
          "bau",
          null,
          null,
          "sales:import:kg / yr",
          null,
          null,
          true,
        );
        const sales = result.getMetric(filterSet);
        assert.closeTo(sales.getValue(), 200000 * 0.1, 0.0001);
        assert.deepEqual(sales.getUnits(), "kg / yr");
      },
    ]);

    buildTest(
      "gets imports by metric with attribution",
      "/examples/multiple_with_policies_split.qta",
      [
        (result, assert) => {
          const filterSet = new FilterSet(
            1,
            "bau",
            null,
            null,
            "sales:import:kg / yr",
            null,
            null,
            false,
          );
          const sales = result.getMetric(filterSet);
          assert.ok(sales.getValue() < 200000 * 0.1);
          assert.deepEqual(sales.getUnits(), "kg / yr");
        },
      ],
    );

    buildTest("gets domestic manfacture by metric", "/examples/multiple_with_policies_split.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(
          1,
          "bau",
          null,
          null,
          "sales:manufacture:kg / yr",
          null,
          null,
          true,
        );
        const sales = result.getMetric(filterSet);
        assert.closeTo(sales.getValue(), 200000 * 0.9, 0.0001);
        assert.deepEqual(sales.getUnits(), "kg / yr");
      },
    ]);

    buildTest(
      "gets domestic manfacture by metric with attribution",
      "/examples/multiple_with_policies_split.qta",
      [
        (result, assert) => {
          const filterSet = new FilterSet(
            1,
            "bau",
            null,
            null,
            "sales:manufacture:kg / yr",
            null,
            null,
            false,
          );
          const sales = result.getMetric(filterSet);
          assert.closeTo(sales.getValue(), 200000 * 0.9, 0.0001);
          assert.deepEqual(sales.getUnits(), "kg / yr");
        },
      ],
    );

    QUnit.test("tests getWithBaseline", (assert) => {
      const filterSet = new FilterSet(
        1,
        "sim",
        "appA",
        "subA",
        "emissions:all:MtCO2e / yr",
        "simulations",
        null,
        false,
      );
      assert.deepEqual(filterSet.getBaseline(), null);

      const newFilterSet = filterSet.getWithBaseline("bau");
      assert.deepEqual(newFilterSet.getBaseline(), "bau");

      // Ensure other properties remain the same
      assert.deepEqual(newFilterSet.getYear(), 1);
      assert.deepEqual(newFilterSet.getScenario(), "sim");
      assert.deepEqual(newFilterSet.getApplication(), "appA");
      assert.deepEqual(newFilterSet.getSubstance(), "subA");
      assert.deepEqual(newFilterSet.getFullMetricName(), "emissions:all:MtCO2e / yr");
      assert.deepEqual(newFilterSet.getDimension(), "simulations");
    });

    buildTest("gets population", "/examples/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(1, "bau", null, null, null, null, null, false);
        const totalPopulation = result.getPopulation(filterSet);
        assert.closeTo(totalPopulation.getValue(), 200000, 0.0001);
        assert.deepEqual(totalPopulation.getUnits(), "units");
      },
    ]);
  });
}

export {buildReportDataTests};
