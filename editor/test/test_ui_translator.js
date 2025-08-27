import {UiTranslatorCompiler, SubstanceMetadata} from "ui_translator";

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

    buildTest("UI substance rename updates policies", "/examples/ui/policy_rename_test.qta", [
      (result, assert) => {
        // Verify initial compilation succeeds
        assert.ok(result.getIsCompatible(), "Initial compilation should succeed");

        // Verify policy rename method works by calling it directly
        result.renameSubstanceInApplication(
          "original_app",
          "test_substance",
          "renamed_test_substance",
        );

        const code = result.toCode(0);
        assert.ok(
          code.includes('modify substance "renamed_test_substance"'),
          "Policy should reference renamed substance after rename",
        );
        assert.ok(
          code.includes('uses substance "renamed_test_substance"'),
          "Main definition should use renamed substance",
        );
      },
    ]);

    // Tests for updateMetadata functionality
    buildTest("updateMetadata updates all substance metadata", "/examples/ui/bau_single.qta", [
      (result, assert) => {
        assert.ok(result.getIsCompatible());
        
        // Get the initial substance
        const application = result.getApplications()[0];
        const substance = application.getSubstances()[0];
        
        // Verify initial state
        assert.equal(substance.getName(), "sub1");
        
        // Create new metadata with all fields updated
        const newMetadata = new SubstanceMetadata(
          "UpdatedSub", // substance name
          "HighEfficiency", // equipment type
          "app1", // application name
          "2000 kgCO2e / kg", // ghg
          true, // hasDomestic
          true, // hasImport
          false, // hasExport
          "300 kwh / unit", // energy
          "0.2 kg / unit", // initialChargeDomestic
          "0.3 kg / unit", // initialChargeImport
          "", // initialChargeExport (empty)
          "8 % / year" // retirement
        );
        
        // Apply the update
        substance.updateMetadata(newMetadata, "app1");
        
        // Verify all fields were updated
        assert.equal(substance.getName(), "UpdatedSub - HighEfficiency");
        
        // Verify internal state was updated correctly
        const equalsGhg = substance.getEqualsGhg();
        assert.ok(equalsGhg !== null);
        assert.equal(equalsGhg.getValue().getValue(), 2000);
        assert.equal(equalsGhg.getValue().getUnits(), "kgCO2e / kg");
        
        const equalsKwh = substance.getEqualsKwh();
        assert.ok(equalsKwh !== null);
        assert.equal(equalsKwh.getValue().getValue(), 300);
        assert.equal(equalsKwh.getValue().getUnits(), "kwh / unit");
        
        // Verify enables are correct
        const enables = substance.getEnables();
        assert.equal(enables.length, 2);
        const enableTargets = enables.map(e => e.getTarget());
        assert.ok(enableTargets.includes("domestic"));
        assert.ok(enableTargets.includes("import"));
        assert.ok(!enableTargets.includes("export"));
        
        // Verify initial charges
        const charges = substance.getInitialCharges();
        assert.equal(charges.length, 2); // domestic and import, but not export
        
        const domesticCharge = charges.find(c => c.getTarget() === "domestic");
        assert.ok(domesticCharge !== null);
        assert.equal(domesticCharge.getValue().getValue(), 0.2);
        assert.equal(domesticCharge.getValue().getUnits(), "kg / unit");
        
        const importCharge = charges.find(c => c.getTarget() === "import");
        assert.ok(importCharge !== null);
        assert.equal(importCharge.getValue().getValue(), 0.3);
        assert.equal(importCharge.getValue().getUnits(), "kg / unit");
        
        // Verify retirement
        const retire = substance.getRetire();
        assert.ok(retire !== null);
        assert.equal(retire.getValue().getValue(), 8);
        assert.equal(retire.getValue().getUnits(), "% / year");
      }
    ]);

    buildTest("updateMetadata handles empty fields correctly", "/examples/ui/bau_single.qta", [
      (result, assert) => {
        assert.ok(result.getIsCompatible());
        
        // Get the initial substance
        const application = result.getApplications()[0];
        const substance = application.getSubstances()[0];
        
        // Create new metadata with some empty fields
        const newMetadata = new SubstanceMetadata(
          "MinimalSub", // substance name
          "", // no equipment type
          "app1", // application name
          "", // no ghg
          false, // no domestic
          false, // no import
          false, // no export
          "", // no energy
          "", // no initialChargeDomestic
          "", // no initialChargeImport
          "", // no initialChargeExport
          "" // no retirement
        );
        
        // Apply the update
        substance.updateMetadata(newMetadata, "app1");
        
        // Verify name is just the substance name without equipment
        assert.equal(substance.getName(), "MinimalSub");
        
        // Verify all optional commands are null/empty
        assert.equal(substance.getEqualsGhg(), null);
        assert.equal(substance.getEqualsKwh(), null);
        assert.equal(substance.getRetire(), null);
        assert.equal(substance.getEnables().length, 0);
        assert.equal(substance.getInitialCharges().length, 0);
      }
    ]);

    buildTest("updateMetadata error handling", "/examples/ui/bau_single.qta", [
      (result, assert) => {
        assert.ok(result.getIsCompatible());
        
        // Get the initial substance
        const application = result.getApplications()[0];
        const substance = application.getSubstances()[0];
        
        // Test invalid metadata object
        try {
          substance.updateMetadata(null, "app1");
          assert.ok(false, "Should have thrown error for null metadata");
        } catch (e) {
          assert.ok(e.message.includes("SubstanceMetadata instance"));
        }
        
        try {
          substance.updateMetadata({}, "app1");
          assert.ok(false, "Should have thrown error for invalid metadata");
        } catch (e) {
          assert.ok(e.message.includes("SubstanceMetadata instance"));
        }
        
        // Test invalid unit value format
        const badMetadata = new SubstanceMetadata(
          "TestSub",
          "",
          "app1",
          "invalid_unit_format", // malformed ghg value
          false,
          false,
          false,
          "",
          "",
          "",
          "",
          ""
        );
        
        try {
          substance.updateMetadata(badMetadata, "app1");
          assert.ok(false, "Should have thrown error for invalid unit format");
        } catch (e) {
          assert.ok(e.message.includes("Invalid unit value format"));
        }
      }
    ]);

    buildTest("rename method uses updateMetadata internally", "/examples/ui/bau_single.qta", [
      (result, assert) => {
        assert.ok(result.getIsCompatible());
        
        // Get the initial substance
        const application = result.getApplications()[0];
        const substance = application.getSubstances()[0];
        
        // Store original values
        const originalGhg = substance.getEqualsGhg();
        const originalEnergy = substance.getEqualsKwh();
        const originalEnables = substance.getEnables().slice();
        const originalCharges = substance.getInitialCharges().slice();
        const originalRetire = substance.getRetire();
        
        // Rename the substance
        substance.rename("NewName - Equipment");
        
        // Verify name was updated
        assert.equal(substance.getName(), "NewName - Equipment");
        
        // Verify all other properties remain unchanged (values should be equivalent)
        const newGhg = substance.getEqualsGhg();
        const newEnergy = substance.getEqualsKwh();
        const newEnables = substance.getEnables();
        const newCharges = substance.getInitialCharges();
        const newRetire = substance.getRetire();
        
        // Check GHG equality
        if (originalGhg && newGhg) {
          assert.equal(newGhg.getValue().getValue(), originalGhg.getValue().getValue());
          assert.equal(newGhg.getValue().getUnits(), originalGhg.getValue().getUnits());
        } else {
          assert.equal(newGhg, originalGhg);
        }
        
        // Check energy equality
        if (originalEnergy && newEnergy) {
          assert.equal(newEnergy.getValue().getValue(), originalEnergy.getValue().getValue());
          assert.equal(newEnergy.getValue().getUnits(), originalEnergy.getValue().getUnits());
        } else {
          assert.equal(newEnergy, originalEnergy);
        }
        
        // Check enables equality
        assert.equal(newEnables.length, originalEnables.length);
        newEnables.forEach((enable, index) => {
          assert.equal(enable.getTarget(), originalEnables[index].getTarget());
        });
        
        // Check charges equality  
        assert.equal(newCharges.length, originalCharges.length);
        newCharges.forEach((charge, index) => {
          assert.equal(charge.getTarget(), originalCharges[index].getTarget());
          assert.equal(charge.getValue().getValue(), originalCharges[index].getValue().getValue());
          assert.equal(charge.getValue().getUnits(), originalCharges[index].getValue().getUnits());
        });
        
        // Check retirement equality
        if (originalRetire && newRetire) {
          assert.equal(newRetire.getValue().getValue(), originalRetire.getValue().getValue());
          assert.equal(newRetire.getValue().getUnits(), originalRetire.getValue().getUnits());
        } else {
          assert.equal(newRetire, originalRetire);
        }
      }
    ]);

    buildTest("backward compatibility - existing rename behavior preserved", "/examples/ui/bau_single_energy.qta", [
      (result, assert) => {
        assert.ok(result.getIsCompatible());
        
        // Test renameSubstanceInApplication function still works
        result.renameSubstanceInApplication("app1", "sub1", "RenamedSubstance");
        
        // Verify the substance was renamed
        const application = result.getApplications()[0];
        const renamedSubstance = application.getSubstance("RenamedSubstance");
        assert.ok(renamedSubstance !== null);
        assert.equal(renamedSubstance.getName(), "RenamedSubstance");
        
        // Verify the old name is no longer accessible
        const oldSubstance = application.getSubstance("sub1");
        assert.equal(oldSubstance, null);
        
        // Verify code generation includes renamed substance
        const code = result.toCode(0);
        assert.ok(code.includes('uses substance "RenamedSubstance"'));
        assert.ok(!code.includes('uses substance "sub1"'));
      }
    ]);
  });
}

export {buildUiTranslatorTests};
