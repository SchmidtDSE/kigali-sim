import {UiTranslaterCompiler} from "ui_translater";


function buildUiTranslaterTests() {
  QUnit.module("UiTranslaterCompiler", function() {
    QUnit.test("initializes", function(assert) {
      const compiler = new UiTranslaterCompiler();
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

    buildTest(
      "converts BAU single app substance",
      "/test/qta/ui/bau_single.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
      ],
    );

    buildTest(
      "converts BAU multiple app substance",
      "/test/qta/ui/bau_multiple.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
      ],
    );

    buildTest(
      "converts policy single app substance",
      "/test/qta/ui/bau_multiple.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
      ],
    );

    buildTest(
      "converts policy multiple app substance",
      "/test/qta/ui/bau_multiple.qta", [
        (result, assert) => {
          assert.ok(result.getIsCompatible());
        },
      ],
    );

    buildTest(
      "converts policy incompatible feature",
      "/test/qta/ui/bau_multiple.qta", [
        (result, assert) => {
          assert.ok(!result.getIsCompatible());
        },
      ],
    );

    buildTest(
      "converts policy incompatible structure",
      "/test/qta/ui/bau_multiple.qta", [
        (result, assert) => {
          assert.ok(!result.getIsCompatible());
        },
      ],
    );
  });
}
