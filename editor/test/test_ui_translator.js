import {UiTranslatorCompiler} from "ui_translator";

function buildUiTranslatorTests() {
  QUnit.module("UiTranslatorCompiler", function () {
    QUnit.test("initializes", function (assert) {
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

    buildTest("converts BAU single app substance", "/examples/ui/bau_single.qta", [
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

        const consumption = substance.getEqualsGhg();
        const consumptionVolume = consumption.getValue();
        assert.deepEqual(consumptionVolume.getValue(), 5);
        assert.deepEqual(consumptionVolume.getUnits(), "tCO2e / mt");
      },
    ]);

    buildTest(
      "converts BAU single app substance with energy",
      "/examples/ui/bau_single_energy.qta",
      [
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

          const consumption = substance.getEqualsKwh();
          const consumptionVolume = consumption.getValue();
          assert.deepEqual(consumptionVolume.getValue(), 5);
          assert.deepEqual(consumptionVolume.getUnits(), "kwh / mt");
        },
      ],
    );

    buildTest("converts BAU multiple app substance", "/examples/ui/bau_multiple.qta", [
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
    ]);

    buildTest("converts single policy", "/examples/ui/policy_single.qta", [
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
    ]);

    buildTest("converts multiple policies", "/examples/ui/policy_multiple.qta", [
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
    ]);

    buildTest("includes only business as usual", "/examples/ui/bau_single.qta", [
      (result, assert) => {
        assert.ok(result.getIsCompatible());
      },
      (result, assert) => {
        const scenarios = result.getScenarios();
        assert.equal(scenarios.length, 1);

        const scenario = scenarios[0];
        assert.deepEqual(scenario.getName(), "business as usual");
      },
    ]);

    buildTest("includes additional sim", "/examples/ui/sim.qta", [
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
    ]);

    buildTest("converts policy incompatible feature", "/examples/ui/incompatible_feature.qta", [
      (result, assert) => {
        assert.ok(!result.getIsCompatible());
      },
    ]);

    buildTest("converts policy incompatible structure", "/examples/ui/incompatible_structure.qta", [
      (result, assert) => {
        assert.ok(!result.getIsCompatible());
      },
    ]);

    buildTest("converts recover with displacement", "/examples/recover_displace_sales_kg.qta", [
      (result, assert) => {
        assert.ok(result.getIsCompatible());
      },
      (result, assert) => {
        const policies = result.getPolicies();
        assert.equal(policies.length, 1);

        const policy = policies[0];
        const applications = policy.getApplications();
        assert.equal(applications.length, 1);

        const application = applications[0];
        const substances = application.getSubstances();
        assert.equal(substances.length, 1);

        const substance = substances[0];
        const recycles = substance.getRecycles();
        assert.equal(recycles.length, 1);

        const recycle = recycles[0];
        assert.equal(recycle.getDisplacing(), "sales");
      },
    ]);

    buildTest("renames application successfully", "/examples/ui/rename_test.qta", [
      (result, assert) => {
        // Verify initial compilation succeeds
        assert.ok(result.getIsCompatible());
      },
      (result, assert) => {
        // Verify initial state
        const app = result.getApplication("original_app");
        assert.ok(app !== null);
        assert.equal(app.getName(), "original_app");
      },
      (result, assert) => {
        // Perform rename and verify results
        result.renameApplication("original_app", "renamed_app");
        const oldApp = result.getApplication("original_app");
        const newApp = result.getApplication("renamed_app");
        assert.ok(oldApp === null);
        assert.ok(newApp !== null);
        assert.equal(newApp.getName(), "renamed_app");
      },
      (result, assert) => {
        // Verify substance preservation and code generation
        const app = result.getApplication("renamed_app");
        const substances = app.getSubstances();
        assert.equal(substances.length, 1);
        assert.equal(substances[0].getName(), "test_substance");

        const code = result.toCode(0);
        assert.ok(code.includes('define application "renamed_app"'));
        assert.ok(!code.includes('define application "original_app"'));
      },
    ]);

    buildTest("renames application in policies successfully", "/examples/ui/policy_rename_test.qta", [
      (result, assert) => {
        // Verify initial compilation succeeds
        assert.ok(result.getIsCompatible());
      },
      (result, assert) => {
        // Verify initial state - application exists in both main and policy
        const mainApp = result.getApplication("original_app");
        const policies = result.getPolicies();
        const policyApp = policies[0].getApplications()[0];
        assert.ok(mainApp !== null);
        assert.equal(mainApp.getName(), "original_app");
        assert.equal(policyApp.getName(), "original_app");
      },
      (result, assert) => {
        // Perform rename and verify main application updated
        result.renameApplication("original_app", "renamed_app");
        const oldApp = result.getApplication("original_app");
        const newApp = result.getApplication("renamed_app");
        assert.ok(oldApp === null);
        assert.ok(newApp !== null);
        assert.equal(newApp.getName(), "renamed_app");
      },
      (result, assert) => {
        // Verify policy application was also updated and code generation works
        const policies = result.getPolicies();
        const policyApp = policies[0].getApplications()[0];
        assert.equal(policyApp.getName(), "renamed_app");
        
        const code = result.toCode(0);
        assert.ok(code.includes('define application "renamed_app"'));
        assert.ok(code.includes('modify application "renamed_app"'));
        assert.ok(!code.includes('application "original_app"'));
      },
    ]);
  });
}

export {buildUiTranslatorTests};
