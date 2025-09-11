import {WasmBackend, WasmLayer} from "wasm_backend";
import {ReportDataWrapper, MetricStrategyBuilder} from "report_data";
import {FilterSet} from "user_config";
import {EngineNumber} from "engine_number";

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
            const backendResult = await wasmBackend.execute(content);
            const programResult = backendResult.getParsedResults();
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

    QUnit.test("parses submetric domestic", (assert) => {
      const filterSet = new FilterSet(
        null,
        null,
        null,
        null,
        "sales:domestic:mt / yr",
        null,
        null,
        false,
      );
      assert.deepEqual(filterSet.getFullMetricName(), "sales:domestic:mt / yr");
      assert.deepEqual(filterSet.getMetric(), "sales");
      assert.deepEqual(filterSet.getSubMetric(), "domestic");
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

    QUnit.test("parses custom metric", (assert) => {
      const filterSet = new FilterSet(
        null,
        null,
        null,
        null,
        "emissions:custom:MtCO2e / yr",
        null,
        null,
        false,
      );
      assert.deepEqual(filterSet.getFullMetricName(), "emissions:custom:MtCO2e / yr");
      assert.deepEqual(filterSet.getMetric(), "emissions");
      assert.deepEqual(filterSet.getSubMetric(), "custom");
      assert.deepEqual(filterSet.getUnits(), "MtCO2e / yr");
      assert.ok(filterSet.isCustomMetric());
    });

    QUnit.test("handles custom definitions", (assert) => {
      const customDefs = {
        "emissions": ["recharge", "eol"],
        "sales": ["domestic", "import"],
      };
      const filterSet = new FilterSet(
        null,
        null,
        null,
        null,
        "emissions:custom:MtCO2e / yr",
        null,
        null,
        false,
        customDefs,
      );
      assert.deepEqual(filterSet.getCustomDefinition("emissions"), ["recharge", "eol"]);
      assert.deepEqual(filterSet.getCustomDefinition("sales"), ["domestic", "import"]);

      const updatedFilterSet = filterSet.getWithCustomDefinition("emissions", ["recharge"]);
      assert.deepEqual(updatedFilterSet.getCustomDefinition("emissions"), ["recharge"]);
      assert.deepEqual(updatedFilterSet.getCustomDefinition("sales"), ["domestic", "import"]);
    });

    QUnit.test("handles null metric strategy gracefully", (assert) => {
      // Create a mock ReportDataWrapper with empty data
      const emptyData = [];
      const wrapper = new ReportDataWrapper(emptyData);

      const filterSet = new FilterSet(
        null,
        null,
        null,
        null,
        "nonexistent:metric:units",
        null,
        null,
        false,
      );

      // Test that getMetric returns null for non-existent strategy
      const result = wrapper.getMetric(filterSet);
      assert.strictEqual(result, null, "getMetric should return null for invalid metric strategy");
    });

    QUnit.test("handles broken metric strategy gracefully", (assert) => {
      // Create a wrapper and manually break a strategy
      const emptyData = [];
      const wrapper = new ReportDataWrapper(emptyData);

      // Mock window.kigaliApp for testing
      const originalKigaliApp = window.kigaliApp;
      let resetCalled = false;
      window.kigaliApp = {
        resetVisualizationState: () => {
          resetCalled = true;
        },
      };

      // Break a metric strategy by setting it to null
      wrapper._metricStrategies["sales:domestic:mt / yr"] = null;

      const filterSet = new FilterSet(
        null,
        null,
        null,
        null,
        "sales:domestic:mt / yr",
        null,
        null,
        false,
      );

      // Test that getMetric handles broken strategy gracefully
      const result = wrapper.getMetric(filterSet);
      assert.strictEqual(result, null, "getMetric should return null for null strategy");
      assert.ok(resetCalled, "resetVisualizationState should be called when strategy is null");

      // Restore original kigaliApp
      window.kigaliApp = originalKigaliApp;
    });

    QUnit.test("handles metric strategy execution error gracefully", (assert) => {
      // Create a wrapper with a strategy that throws an error
      const emptyData = [];
      const wrapper = new ReportDataWrapper(emptyData);

      // Mock window.kigaliApp for testing
      const originalKigaliApp = window.kigaliApp;
      let resetCalled = false;
      window.kigaliApp = {
        resetVisualizationState: () => {
          resetCalled = true;
        },
      };

      // Create a strategy that throws a "not a function" error
      wrapper._metricStrategies["sales:domestic:mt / yr"] = () => {
        throw new Error("metricStrategy is not a function");
      };

      const filterSet = new FilterSet(
        null,
        null,
        null,
        null,
        "sales:domestic:mt / yr",
        null,
        null,
        false,
      );

      // Test that getMetric handles execution error gracefully
      const result = wrapper.getMetric(filterSet);
      assert.strictEqual(result, null, "getMetric should return null for strategy execution error");
      assert.ok(resetCalled, "resetVisualizationState should be called when strategy throws error");

      // Restore original kigaliApp
      window.kigaliApp = originalKigaliApp;
    });

    QUnit.test("re-throws non-function errors", (assert) => {
      // Create a wrapper with a strategy that throws a non-function error
      const emptyData = [];
      const wrapper = new ReportDataWrapper(emptyData);

      // Create a strategy that throws a different kind of error
      wrapper._metricStrategies["sales:domestic:mt / yr"] = () => {
        throw new Error("Some other validation error");
      };

      const filterSet = new FilterSet(
        null,
        null,
        null,
        null,
        "sales:domestic:mt / yr",
        null,
        null,
        false,
      );

      // Test that getMetric re-throws non-function errors
      assert.throws(() => {
        wrapper.getMetric(filterSet);
      }, /Some other validation error/, "Should re-throw non-function errors");
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
          "sales:domestic:kg / yr",
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
          "sales:domestic:kg / yr",
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
          "sales:domestic:kg / yr",
          null,
          null,
          true,
        );
        const totalSales = result.getMetric(filterSet);
        assert.closeTo(totalSales.getValue(), 180000, 0.0001);
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
            "sales:domestic:kg / yr",
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

    buildTest("gets domestic domestic by metric", "/examples/multiple_with_policies_split.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(
          1,
          "bau",
          null,
          null,
          "sales:domestic:kg / yr",
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
      "gets domestic domestic by metric with attribution",
      "/examples/multiple_with_policies_split.qta",
      [
        (result, assert) => {
          const filterSet = new FilterSet(
            1,
            "bau",
            null,
            null,
            "sales:domestic:kg / yr",
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

    buildTest("gets exports by metric", "/examples/multiple_with_policies_split.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(
          1,
          "bau",
          null,
          null,
          "sales:export:kg / yr",
          null,
          null,
          true,
        );
        const sales = result.getMetric(filterSet);
        assert.ok(sales.getValue() >= 0);
        assert.deepEqual(sales.getUnits(), "kg / yr");
      },
    ]);

    buildTest(
      "gets exports by metric with attribution",
      "/examples/multiple_with_policies_split.qta",
      [
        (result, assert) => {
          const filterSet = new FilterSet(
            1,
            "bau",
            null,
            null,
            "sales:export:kg / yr",
            null,
            null,
            false,
          );
          const sales = result.getMetric(filterSet);
          assert.ok(sales.getValue() >= 0);
          assert.deepEqual(sales.getUnits(), "kg / yr");
        },
      ],
    );

    buildTest("gets export emissions by metric", "/examples/multiple_with_policies_split.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(
          1,
          "bau",
          null,
          null,
          "emissions:export:tCO2e / yr",
          null,
          null,
          true,
        );
        const emissions = result.getMetric(filterSet);
        assert.ok(emissions.getValue() >= 0);
        assert.deepEqual(emissions.getUnits(), "tCO2e / yr");
      },
    ]);

    buildTest(
      "gets export emissions by metric with attribution",
      "/examples/multiple_with_policies_split.qta",
      [
        (result, assert) => {
          const filterSet = new FilterSet(
            1,
            "bau",
            null,
            null,
            "emissions:export:tCO2e / yr",
            null,
            null,
            false,
          );
          const emissions = result.getMetric(filterSet);
          assert.ok(emissions.getValue() >= 0);
          assert.deepEqual(emissions.getUnits(), "tCO2e / yr");
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

    QUnit.test("strategy builder supports dual year format registration", (assert) => {
      const builder = new MetricStrategyBuilder({});

      // Set up a test strategy with "/ yr" units
      builder.setMetric("sales");
      builder.setSubmetric("domestic");
      builder.setUnits("mt / yr");
      builder.setStrategy(() => new EngineNumber(100, "mt"));
      builder.setTransformation((val) => val);
      builder.add();

      const strategies = builder.build();

      // Both "/ yr" and "/ year" variants should exist
      assert.ok(strategies["sales:domestic:mt / yr"], "Original '/ yr' strategy should exist");
      assert.ok(strategies["sales:domestic:mt / year"], "Dual '/ year' strategy should exist");

      // Both should reference the same function
      assert.strictEqual(
        strategies["sales:domestic:mt / yr"],
        strategies["sales:domestic:mt / year"],
        "Both variants should reference the same strategy function",
      );
    });

    QUnit.test("strategy builder only creates dual registration for year units", (assert) => {
      const builder = new MetricStrategyBuilder({});

      // Set up a test strategy with non-year units
      builder.setMetric("sales");
      builder.setSubmetric("domestic");
      builder.setUnits("mt / unit");
      builder.setStrategy(() => new EngineNumber(100, "mt"));
      builder.setTransformation((val) => val);
      builder.add();

      const strategies = builder.build();

      // Only original strategy should exist, no dual registration
      assert.ok(strategies["sales:domestic:mt / unit"], "Original strategy should exist");
      assert.notOk(
        strategies["sales:domestic:mt / year"],
        "No dual registration for non-year units",
      );
    });

    QUnit.test("transformation functions handle both year formats", (assert) => {
      const builder = new MetricStrategyBuilder({});

      // Test emissions transformation with "/ year" format
      builder.setMetric("emissions");
      builder.setSubmetric("test");
      builder.setUnits("MtCO2e / yr");
      builder.setStrategy(() => new EngineNumber(1000000, "tCO2e / year"));
      builder.setTransformation((val) => {
        const normalizedUnits = val.getUnits().replace(" / year", "").replace(" / yr", "");
        if (normalizedUnits !== "tCO2e") {
          throw "Unexpected emissions source units: " + val.getUnits();
        }
        return new EngineNumber(val.getValue() / 1000000, "MtCO2e / yr");
      });
      builder.add();

      const strategies = builder.build();
      const filterSet = {};

      // Should not throw an error
      const result = strategies["emissions:test:MtCO2e / yr"](filterSet);
      assert.ok(result, "Strategy should execute successfully with / year format");
      assert.strictEqual(result.getValue(), 1, "Should convert correctly");
      assert.strictEqual(result.getUnits(), "MtCO2e / yr", "Should have correct output units");
    });

    QUnit.test("sales transformation functions handle both year formats", (assert) => {
      const builder = new MetricStrategyBuilder({});

      // Test sales transformation with "/ year" format
      builder.setMetric("sales");
      builder.setSubmetric("test");
      builder.setUnits("mt / yr");
      builder.setStrategy(() => new EngineNumber(2000, "kg / year"));
      builder.setTransformation((value) => {
        const normalizedUnits = value.getUnits().replace(" / year", "").replace(" / yr", "");
        if (normalizedUnits !== "kg") {
          throw "Unexpected sales units: " + value.getUnits();
        }
        return new EngineNumber(value.getValue() / 1000, "mt / yr");
      });
      builder.add();

      const strategies = builder.build();
      const filterSet = {};

      // Should not throw an error
      const result = strategies["sales:test:mt / yr"](filterSet);
      assert.ok(result, "Strategy should execute successfully with / year format");
      assert.strictEqual(result.getValue(), 2, "Should convert correctly");
      assert.strictEqual(result.getUnits(), "mt / yr", "Should have correct output units");
    });

    QUnit.test("custom emissions strategy works with MtCO2e / year units", (assert) => {
      const mockReportData = {
        getRechargeEmissions: () => new EngineNumber(500000, "tCO2e"),
        getEolEmissions: () => new EngineNumber(300000, "tCO2e"),
        getExportEmissions: () => new EngineNumber(200000, "tCO2e"),
      };

      const builder = new MetricStrategyBuilder({});
      builder.setMetric("emissions");
      builder.setSubmetric("custom");
      builder.setUnits("MtCO2e / yr");
      builder.setStrategy((filterSet) => {
        const customDef = filterSet.getCustomDefinition("emissions");
        if (!customDef || customDef.length === 0) return null;

        const emissionMethods = {
          "recharge": (x) => mockReportData.getRechargeEmissions(x),
          "eol": (x) => mockReportData.getEolEmissions(x),
          "export": (x) => mockReportData.getExportEmissions(x),
        };

        const results = customDef.map((submetric) => {
          const method = emissionMethods[submetric];
          return method ? method(filterSet) : null;
        }).filter((result) => result !== null);

        if (results.length === 0) return null;

        return results.reduce((a, b) => {
          if (!a) return b;
          if (!b) return a;
          if (a.getUnits() !== b.getUnits()) {
            throw new Error(
              `Cannot combine incompatible units: ${a.getUnits()} and ${b.getUnits()}`,
            );
          }
          return new EngineNumber(a.getValue() + b.getValue(), a.getUnits());
        });
      });
      builder.setTransformation((val) => {
        const normalizedUnits = val.getUnits().replace(" / year", "").replace(" / yr", "");
        if (normalizedUnits !== "tCO2e") {
          throw "Unexpected emissions source units: " + val.getUnits();
        }
        return new EngineNumber(val.getValue() / 1000000, "MtCO2e / yr");
      });
      builder.add();

      const strategies = builder.build();
      const mockFilterSet = {
        getCustomDefinition: (metric) => metric === "emissions" ? ["recharge", "eol"] : null,
      };

      const result = strategies["emissions:custom:MtCO2e / yr"](mockFilterSet);
      assert.ok(result, "Custom emissions strategy should execute successfully");
      assert.strictEqual(
        result.getValue(),
        0.8,
        "Should aggregate and convert correctly (800k tCO2e -> 0.8 MtCO2e)",
      );
      assert.strictEqual(result.getUnits(), "MtCO2e / yr", "Should have correct output units");
    });

    QUnit.test("custom sales strategy works with mt / year units", (assert) => {
      const mockReportData = {
        getDomestic: () => new EngineNumber(150000, "kg"),
        getImport: () => new EngineNumber(50000, "kg"),
        getExport: () => new EngineNumber(25000, "kg"),
        getRecycle: () => new EngineNumber(75000, "kg"),
      };

      const builder = new MetricStrategyBuilder({});
      builder.setMetric("sales");
      builder.setSubmetric("custom");
      builder.setUnits("mt / yr");
      builder.setStrategy((filterSet) => {
        const customDef = filterSet.getCustomDefinition("sales");
        if (!customDef || customDef.length === 0) return null;

        const salesMethods = {
          "domestic": (x) => mockReportData.getDomestic(x),
          "import": (x) => mockReportData.getImport(x),
          "export": (x) => mockReportData.getExport(x),
          "recycle": (x) => mockReportData.getRecycle(x),
        };

        const results = customDef.map((submetric) => {
          const method = salesMethods[submetric];
          return method ? method(filterSet) : null;
        }).filter((result) => result !== null);

        if (results.length === 0) return null;

        return results.reduce((a, b) => {
          if (!a) return b;
          if (!b) return a;
          if (a.getUnits() !== b.getUnits()) {
            throw new Error(
              `Cannot combine incompatible units: ${a.getUnits()} and ${b.getUnits()}`,
            );
          }
          return new EngineNumber(a.getValue() + b.getValue(), a.getUnits());
        });
      });
      builder.setTransformation((value) => {
        const normalizedUnits = value.getUnits().replace(" / year", "").replace(" / yr", "");
        if (normalizedUnits !== "kg") {
          throw "Unexpected sales units: " + value.getUnits();
        }
        return new EngineNumber(value.getValue() / 1000, "mt / yr");
      });
      builder.add();

      const strategies = builder.build();
      const mockFilterSet = {
        getCustomDefinition: (metric) => metric === "sales" ? ["domestic", "import"] : null,
      };

      const result = strategies["sales:custom:mt / yr"](mockFilterSet);
      assert.ok(result, "Custom sales strategy should execute successfully");
      assert.strictEqual(
        result.getValue(),
        200,
        "Should aggregate and convert correctly (200k kg -> 200 mt)",
      );
      assert.strictEqual(result.getUnits(), "mt / yr", "Should have correct output units");
    });

    QUnit.test("kgCO2e emissions conversion works correctly", (assert) => {
      const builder = new MetricStrategyBuilder({});
      // Test kgCO2e / yr conversion
      builder.setMetric("emissions");
      builder.setSubmetric("recharge");
      builder.setUnits("kgCO2e / yr");
      builder.setStrategy(() => new EngineNumber(1.5, "tCO2e"));
      builder.setTransformation((val) => {
        const normalizedUnits = val.getUnits().replace(" / year", "").replace(" / yr", "");
        if (normalizedUnits !== "tCO2e") {
          throw "Unexpected emissions source units: " + val.getUnits();
        }
        return new EngineNumber(val.getValue() * 1000, "kgCO2e / yr");
      });
      builder.add();

      const strategies = builder.build();
      const filterSet = {};

      const result = strategies["emissions:recharge:kgCO2e / yr"](filterSet);
      assert.ok(result, "kgCO2e strategy should execute successfully");
      assert.strictEqual(result.getValue(), 1500,
        "Should convert tCO2e to kgCO2e correctly (1.5 * 1000)");
      assert.strictEqual(result.getUnits(), "kgCO2e / yr", "Should have correct output units");
    });

    QUnit.test("tCO2e emissions conversion works correctly", (assert) => {
      const builder = new MetricStrategyBuilder({});
      // Test tCO2e / yr conversion (baseline emission units)
      builder.setMetric("emissions");
      builder.setSubmetric("recharge");
      builder.setUnits("tCO2e / yr");
      builder.setStrategy(() => new EngineNumber(1.5, "tCO2e"));
      builder.setTransformation((val) => {
        const normalizedUnits = val.getUnits().replace(" / year", "").replace(" / yr", "");
        if (normalizedUnits !== "tCO2e") {
          throw "Unexpected emissions source units: " + val.getUnits();
        }
        return new EngineNumber(val.getValue(), "tCO2e / yr");
      });
      builder.add();

      const strategies = builder.build();
      const filterSet = {};

      const result = strategies["emissions:recharge:tCO2e / yr"](filterSet);
      assert.ok(result, "tCO2e strategy should execute successfully");
      assert.strictEqual(result.getValue(), 1.5,
        "Should keep same value for tCO2e (baseline emission units)");
      assert.strictEqual(result.getUnits(), "tCO2e / yr", "Should have correct output units");
    });

    QUnit.test("MkgCO2e emissions conversion works correctly", (assert) => {
      const builder = new MetricStrategyBuilder({});
      // Test MkgCO2e / yr conversion (should be equivalent to tCO2e)
      builder.setMetric("emissions");
      builder.setSubmetric("recharge");
      builder.setUnits("MkgCO2e / yr");
      builder.setStrategy(() => new EngineNumber(1.5, "tCO2e"));
      builder.setTransformation((val) => {
        const normalizedUnits = val.getUnits().replace(" / year", "").replace(" / yr", "");
        if (normalizedUnits !== "tCO2e") {
          throw "Unexpected emissions source units: " + val.getUnits();
        }
        return new EngineNumber(val.getValue(), "MkgCO2e / yr");
      });
      builder.add();

      const strategies = builder.build();
      const filterSet = {};

      const result = strategies["emissions:recharge:MkgCO2e / yr"](filterSet);
      assert.ok(result, "MkgCO2e strategy should execute successfully");
      assert.strictEqual(result.getValue(), 1.5,
        "Should keep same value for MkgCO2e (equivalent to tCO2e)");
      assert.strictEqual(result.getUnits(), "MkgCO2e / yr", "Should have correct output units");
    });
  });
}

export {buildReportDataTests};
