import {Compiler} from "compiler";
import {ReportDataWrapper, FilterSet} from "report_data";

function loadRemote(path) {
  return fetch(path).then((response) => response.text());
}

function buildReportDataTests() {
  QUnit.module("ReportData", function () {
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
              const programResultWrapped = new ReportDataWrapper(programResult);
              checks.forEach((check) => {
                check(programResultWrapped, assert);
              });
            } catch (e) {
              assert.ok(false);
              console.log(e);
            }
          }

          done();
        });
      });
    };

    buildTest("runs the base script", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        assert.notDeepEqual(result, null);
      },
    ]);

    buildTest("gets the scenarios", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const years = result.getScenarios();
        assert.equal(years.size, 2);
        assert.ok(years.has("bau"));
        assert.ok(years.has("sim"));
      },
    ]);

    buildTest("gets all the years", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, null, null);
        const years = result.getYears(filterSet);
        assert.equal(years.size, 3);
        assert.ok(years.has(1));
        assert.ok(years.has(2));
        assert.ok(years.has(3));
      },
    ]);

    buildTest("gets years matching filter", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, "sim", null, null);
        const years = result.getYears(filterSet);
        assert.equal(years.size, 2);
        assert.ok(years.has(1));
        assert.ok(years.has(2));
      },
    ]);

    buildTest("gets all applications", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, null, null);
        const years = result.getApplications(filterSet);
        assert.equal(years.size, 2);
        assert.ok(years.has("appA"));
        assert.ok(years.has("appB"));
      },
    ]);

    buildTest("gets applications with filter", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, null, "subA");
        const years = result.getApplications(filterSet);
        assert.equal(years.size, 1);
        assert.ok(years.has("appA"));
      },
    ]);

    buildTest("gets all substances", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, null, null);
        const years = result.getSubstances(filterSet);
        assert.equal(years.size, 2);
        assert.ok(years.has("subA"));
        assert.ok(years.has("subB"));
      },
    ]);

    buildTest("gets substances matching filter", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(null, null, "appA", null);
        const years = result.getSubstances(filterSet);
        assert.equal(years.size, 1);
        assert.ok(years.has("subA"));
      },
    ]);

    buildTest("gets consumption", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(1, "bau", null, null);
        const totalConsumption = result.getConsumption(filterSet);
        assert.closeTo(totalConsumption.getValue(), 1500, 0.0001);
        assert.deepEqual(totalConsumption.getUnits(), "tCO2e");
      },
    ]);

    buildTest("gets sales", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(1, "bau", null, null);
        const totalSales = result.getSales(filterSet);
        assert.closeTo(totalSales.getValue(), 200000, 0.0001);
        assert.deepEqual(totalSales.getUnits(), "kg");
      },
    ]);

    buildTest("gets population", "/test/qta/multiple_with_policies.qta", [
      (result, assert) => {
        const filterSet = new FilterSet(1, "bau", null, null);
        const totalPopulation = result.getPopulation(filterSet);
        assert.closeTo(totalPopulation.getValue(), 200000, 0.0001);
        assert.deepEqual(totalPopulation.getUnits(), "units");
      },
    ]);
  });
}

export {buildReportDataTests};
