import {UiTranslatorCompiler} from "ui_translator";


function buildUiTranslatorTests() {
  QUnit.module("UiTranslatorCompiler", function() {
    QUnit.test("initializes", function(assert) {
      const compiler = new UiTranslatorCompiler();
      assert.ok(compiler !== undefined);
    });

    const loadRemote = (path) => {
      return fetch(path).then((response) => response.text());
    };

    const buildTest = (name, filepath, checks) => {
      QUnit.test(name, (assert) => {
        const done = assert.async();
        loadRemote(filepath).then((content) => {
          assert.ok(content.length > 0);

          let compilerResult = null;
          try {
            const compiler = new UiTranslatorCompiler();
            compilerResult = compiler.compile(content);
          } catch (e) {
            console.log(e);
            assert.ok(false);
          }

          assert.equal(compilerResult.getErrors().length, 0);

          const programResult = compilerResult.getProgram();
          assert.equal(compilerResult.getErrors().length, 0);

          if (compilerResult.getErrors().length > 0) {
            console.log(compilerResult.getErrors());
          } else {
            try {
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

    buildTest(
      "converts BAU single app substance",
      "/test/qta/ui/bau_single.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
        (result, assert) => {
          const applications = result.getApplications();
          assert.equal(applications.length, 1);

          const application = applications[0];
          assert.deepEqual(application.getName(), "app1");

          const substances = application.getSubstances();
          assert.equal(substances.length, 1);

          const substance = substances[0];
          assert.equal(substance.getName(), "sub1");
        },
        (result, assert) => {
          const applications = result.getApplications();
          const application = applications[0];

          const substances = application.getSubstances();
          const substance = substances[0];

          const consumption = substance.getEquals();
          const consumptionVolume = consumption.getValue();
          assert.deepEqual(consumptionVolume.getValue(), 5);
          assert.deepEqual(consumptionVolume.getUnits(), "tCO2e / mt");
        },
      ],
    );

    buildTest(
      "converts BAU multiple app substance",
      "/test/qta/ui/bau_multiple.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
        (result, assert) => {
          const applications = result.getApplications();
          assert.equal(applications.length, 2);

          const application = applications[0];
          assert.deepEqual(application.getName(), "app1");

          const applicationOther = applications[1];
          assert.deepEqual(applicationOther.getName(), "app2");

          const substances = application.getSubstances();
          assert.equal(substances.length, 2);

          const substance = substances[0];
          assert.equal(substance.getName(), "sub1a");
        },
      ],
    );

    buildTest(
      "converts single policy",
      "/test/qta/ui/policy_single.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
        (result, assert) => {
          const policies = result.getPolicies();
          assert.equal(policies.length, 1);

          const policy = policies[0];
          assert.deepEqual(policy.getName(), "policy1");

          const applications = policy.getApplications();
          assert.equal(applications.length, 1);

          const application = applications[0];
          assert.deepEqual(application.getName(), "app1");

          const substances = application.getSubstances();
          assert.equal(substances.length, 1);

          const substance = substances[0];
          assert.equal(substance.getName(), "sub1");
        },
      ],
    );

    buildTest(
      "converts multiple policies",
      "/test/qta/ui/policy_multiple.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
        (result, assert) => {
          const policies = result.getPolicies();
          assert.equal(policies.length, 2);

          const policy = policies[0];
          assert.deepEqual(policy.getName(), "policy1");

          const policyOther = policies[1];
          assert.deepEqual(policyOther.getName(), "policy2");

          const applications = policy.getApplications();
          assert.equal(applications.length, 1);

          const application = applications[0];
          assert.deepEqual(application.getName(), "app1");

          const substances = application.getSubstances();
          assert.equal(substances.length, 1);

          const substance = substances[0];
          assert.equal(substance.getName(), "sub1");
        },
      ],
    );

    buildTest(
      "includes only business as usual",
      "/test/qta/ui/bau_single.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
        (result, assert) => {
          const scenarios = result.getScenarios();
          assert.equal(scenarios.length, 1);

          const scenario = scenarios[0];
          assert.deepEqual(scenario.getName(), "business as usual");
        },
      ],
    );

    buildTest(
      "includes additional sim",
      "/test/qta/ui/sim.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
        (result, assert) => {
          const scenarios = result.getScenarios();
          assert.equal(scenarios.length, 2);

          const scenario = scenarios[0];
          assert.deepEqual(scenario.getName(), "business as usual");

          const scenarioOther = scenarios[1];
          assert.deepEqual(scenarioOther.getName(), "policy scenario");
        },
      ],
    );

    buildTest(
      "converts policy incompatible feature",
      "/test/qta/ui/incompatible_feature.qta", [
        (result, assert) => {
          assert.ok(!result.getIsCompatible());
        },
      ],
    );

    buildTest(
      "converts policy incompatible structure",
      "/test/qta/ui/incompatible_structure.qta", [
        (result, assert) => {
          assert.ok(!result.getIsCompatible());
        },
      ],
    );
  });
}


export {buildUiTranslatorTests};
