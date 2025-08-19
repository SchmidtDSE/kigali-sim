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

    buildTest("renames application in policies successfully",
      "/examples/ui/policy_rename_test.qta", [
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

    buildTest("renames substance in application successfully",
      "/examples/ui/policy_rename_test.qta", [
        (result, assert) => {
          // Verify initial compilation succeeds
          assert.ok(result.getIsCompatible());
        },
        (result, assert) => {
          // Verify initial state
          const app = result.getApplication("original_app");
          const substance = app.getSubstance("test_substance");
          assert.ok(substance !== null);
          assert.equal(substance.getName(), "test_substance");

          const policies = result.getPolicies();
          const policyApp = policies[0].getApplications()[0];
          const policySubstance = policyApp.getSubstance("test_substance");
          assert.ok(policySubstance !== null);
        },
        (result, assert) => {
          // Perform rename and verify results
          result.renameSubstanceInApplication("original_app", "test_substance",
            "renamed_substance");

          const app = result.getApplication("original_app");
          const oldSubstance = app.getSubstance("test_substance");
          const newSubstance = app.getSubstance("renamed_substance");
          assert.ok(oldSubstance === null);
          assert.ok(newSubstance !== null);
          assert.equal(newSubstance.getName(), "renamed_substance");
        },
        (result, assert) => {
          // Verify policy was updated and code generation works
          const policies = result.getPolicies();
          const policyApp = policies[0].getApplications()[0];
          const policySubstance = policyApp.getSubstance("renamed_substance");
          assert.ok(policySubstance !== null);
          assert.equal(policySubstance.getName(), "renamed_substance");

          const code = result.toCode(0);
          assert.ok(code.includes('uses substance "renamed_substance"'));
          assert.ok(code.includes('modify substance "renamed_substance"'));
          assert.ok(!code.includes('substance "test_substance"'));
        },
      ]);

    buildTest("renames substance with equipment model successfully",
      "/examples/ui/substance_equipment_rename_test.qta", [
        (result, assert) => {
          // Verify initial compilation succeeds
          assert.ok(result.getIsCompatible());
        },
        (result, assert) => {
          // Verify initial state with equipment model
          const app = result.getApplication("test_app");
          const substance = app.getSubstance("HFC-134a - Refrigerator");
          assert.ok(substance !== null);
          assert.equal(substance.getName(), "HFC-134a - Refrigerator");

          const policies = result.getPolicies();
          const policyApp = policies[0].getApplications()[0];
          const policySubstance = policyApp.getSubstance("HFC-134a - Refrigerator");
          assert.ok(policySubstance !== null);
        },
        (result, assert) => {
          // Perform rename with equipment model and verify results
          result.renameSubstanceInApplication("test_app", "HFC-134a - Refrigerator",
            "R-134a - Refrigerator");

          const app = result.getApplication("test_app");
          const oldSubstance = app.getSubstance("HFC-134a - Refrigerator");
          const newSubstance = app.getSubstance("R-134a - Refrigerator");
          assert.ok(oldSubstance === null);
          assert.ok(newSubstance !== null);
          assert.equal(newSubstance.getName(), "R-134a - Refrigerator");
        },
        (result, assert) => {
          // Verify policy was updated and code generation works with equipment model
          const policies = result.getPolicies();
          const policyApp = policies[0].getApplications()[0];
          const policySubstance = policyApp.getSubstance("R-134a - Refrigerator");
          assert.ok(policySubstance !== null);
          assert.equal(policySubstance.getName(), "R-134a - Refrigerator");

          const code = result.toCode(0);
          assert.ok(code.includes('uses substance "R-134a - Refrigerator"'));
          assert.ok(code.includes('modify substance "R-134a - Refrigerator"'));
          assert.ok(!code.includes('substance "HFC-134a - Refrigerator"'));
        },
      ]);
  });

  buildTest("UI substance rename updates policies", "/examples/ui/policy_rename_test.qta", [
    (result, assert) => {
      // Verify initial compilation succeeds
      assert.ok(result.getIsCompatible(), "Initial compilation should succeed");
    },
    (result, assert) => {
      // Get the original substance and application
      const app = result.getApplication("original_app");
      const originalSubstance = app.getSubstance("test_substance");
      assert.ok(originalSubstance !== null, "Original substance should exist");

      // Create a new substance with different name but same essential properties
      const renamedSubstance = new SubstanceBuilder("renamed_test_substance", false);
      
      // Copy essential commands from original substance
      renamedSubstance.addCommand(new Command(
        "equals",
        null,
        new EngineNumber(1430, "tCO2e / mt"),
        null,
      ));
      renamedSubstance.addCommand(new Command(
        "initial charge",
        "domestic",
        new EngineNumber(0.12, "kg / unit"),
        null,
      ));
      renamedSubstance.addCommand(new Command(
        "set",
        "domestic",
        new EngineNumber(350, "mt"),
        null,
      ));
      
      const newSubstanceObj = renamedSubstance.build(true);

      // Simulate the UI save operation - this should trigger the rename logic
      result.insertSubstance(
        "original_app",
        "original_app",
        "test_substance",
        newSubstanceObj,
      );

      // Verify policy was updated to reference the new substance name
      const code = result.toCode(0);
      assert.ok(
        code.includes('modify substance "renamed_test_substance"'),
        "Policy should reference renamed substance",
      );
      assert.ok(
        !code.includes('modify substance "test_substance"'),
        "Policy should not reference old substance name",
      );

      // Verify the main substance definition was also updated
      assert.ok(
        code.includes('uses substance "renamed_test_substance"'),
        "Main definition should use new substance name",
      );
      assert.ok(
        !code.includes('uses substance "test_substance"'),
        "Main definition should not use old substance name",
      );
    },
  ]);
}

export {buildUiTranslatorTests};
