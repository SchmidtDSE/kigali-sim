import { Compiler } from "compiler";


function loadRemote(path) {
  return fetch(path).then((response) => response.text());
}

function buildCompilerTests() {
  QUnit.module("Compiler", function () {
    QUnit.test("gets toolkit", function (assert) {
      const toolkit = QubecTalk.getToolkit();
      assert.ok(toolkit !== undefined);
      assert.ok(toolkit["antlr4"] !== undefined);
      assert.ok(toolkit["QubecTalkLexer"] !== undefined);
      assert.ok(toolkit["QubecTalkParser"] !== undefined);
      assert.ok(toolkit["QubecTalkListener"] !== undefined);
      assert.ok(toolkit["QubecTalkVisitor"] !== undefined);
    });

    QUnit.test("initializes a compiler", (assert) => {
      const compiler = new Compiler();
      assert.ok(compiler !== undefined);
    });

    const buildTest = (name, filepath, checks) => {
      QUnit.test(name, (assert) => {
        const done = assert.async();
        loadRemote(filepath).then((content) => {
          assert.ok(content.length > 0);
          
          const compiler = new Compiler();
          const compilerResult = compiler.compile(content);
          assert.ok(compilerResult.getErrors().length == 0);

          const program = compilerResult.getProgram();
          assert.ok(compilerResult.getErrors().length == 0);

          if (compilerResult.getErrors().length > 0) {
            console.log(compilerResult.getErrors());
          } else {
            const programResult = program();
            checks.forEach((check) => {
              check(programResult, assert);
            });
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
      return trialFlat.filter((x) => x.getYear() == year)
        .filter((x) => x.getApplication() === application)
        .filter((x) => x.getSubstance() === substance)[0];
    };

    const BAU_NAME = "business as usual";

    buildTest(
      "runs a basic script",
      "/test/qta/basic.qta", [
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
          const equipment = record.getPopulation();
          assert.ok(Math.abs(equipment.getValue() - 20000) < 0.0001);
          assert.ok(equipment.getUnits() === "units");
        },
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 500) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
          const manufacture = record.getManufacture();
          assert.ok(Math.abs(manufacture.getValue() - 100000) < 0.0001);
          assert.ok(manufacture.getUnits() === "kg");
        },
      ],
    );

    buildTest(
      "interprets a change command",
      "/test/qta/change.qta", [
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
          const manufacture = record.getManufacture();
          assert.ok(Math.abs(manufacture.getValue() - 110000) < 0.0001);
          assert.ok(manufacture.getUnits() === "kg");
        },
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 550) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
      ],
    );

    buildTest(
      "interprets a retire command",
      "/test/qta/retire.qta", [
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
          const manufacture = record.getManufacture();
          assert.ok(Math.abs(manufacture.getValue() - 100000) < 0.0001);
          assert.ok(manufacture.getUnits() === "kg");
        },
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 450) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
          const population = record.getPopulation();
          assert.ok(Math.abs(population.getValue() - 90000) < 0.0001);
          assert.ok(emissions.getUnits() === "units");
        },
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
          const population = record.getPopulation();
          assert.ok(Math.abs(population.getValue() - 171000) < 0.0001);
          assert.ok(emissions.getUnits() === "units");
        },
      ],
    );

    buildTest(
      "interprets a retire command",
      "/test/qta/recharge.qta", [
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 1, 0, "test", "test");
          const equipment = record.getPopulation();
          assert.ok(Math.abs(equipment.getValue() - 100000) < 0.0001);
          assert.ok(equipment.getUnits() === "units");
        },
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 2, 0, "test", "test");
          const equipment = record.getPopulation();
          assert.ok(Math.abs(equipment.getValue() - 190000) < 0.0001);
          assert.ok(equipment.getUnits() === "units");
        },
      ],
    );

    buildTest(
      "handles multiple emissions",
      "/test/qta/multiple.qta", [
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 1, 0, "test", "a");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 500) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
        (result, assert) => {
          const record = getResult(result, BAU_NAME, 1, 0, "test", "b");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 1000) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
      ],
    );

    buildTest(
      "handles policies",
      "/test/qta/policies.qta", [
        (result, assert) => {
          const record = getResult(result, "result", 1, 0, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 250) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
      ],
    );

    buildTest(
      "handles recycling",
      "/test/qta/recycling.qta", [
        (result, assert) => {
          const record = getResult(result, "result", 1, 0, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 500) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
        (result, assert) => {
          const record = getResult(result, "result", 2, 0, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 437.5) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
      ],
    );

    buildTest(
      "handles replace",
      "/test/qta/replace.qta", [
        (result, assert) => {
          const record = getResult(result, "result", 1, 0, "test", "a");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 250) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
        (result, assert) => {
          const record = getResult(result, "result", 1, 0, "test", "b");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 375) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
      ],
    );

    buildTest(
      "combines policies",
      "/test/qta/combination.qta", [
        (result, assert) => {
          const record = getResult(result, "result", 1, 0, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 125) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
      ],
    );

    buildTest(
      "evaluates conditionals",
      "/test/qta/conditional.qta", [
        (result, assert) => {
          const record = getResult(result, "business as usual", 1, 0, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(Math.abs(emissions.getValue() - 250) < 0.0001);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
      ],
    );

    buildTest(
      "runs trials",
      "/test/qta/trials.qta", [
        (result, assert) => {
          const record = getResult(result, "business as usual", 1, 0, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(emissions.getValue() >= 300);
          assert.ok(emissions.getValue() <= 700);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
        (result, assert) => {
          const record = getResult(result, "business as usual", 1, 1, "test", "test");
          const emissions = record.getEmissions();
          assert.ok(emissions.getValue() >= 300);
          assert.ok(emissions.getValue() <= 700);
          assert.ok(emissions.getUnits() === "tCO2e");
        },
      ],
    );

    buildTest(
      "runs reference",
      "/test/qta/reference.qta",
      [],
    );
  });
}

export { buildCompilerTests };
