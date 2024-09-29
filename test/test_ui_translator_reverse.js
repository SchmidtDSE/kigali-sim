import {UiTranslatorCompiler} from "ui_translator";


function buildUiTranslatorReverseTests() {
  QUnit.module("UiTranslatorCompilerReverse", function() {
    QUnit.test("initializes", function(assert) {
      const compiler = new UiTranslatorCompiler();
      assert.ok(compiler !== undefined);
    });

    const loadRemote = (path) => {
      return fetch(path).then((response) => response.text());
    };

    const buildTest = (name, filepath) => {
      QUnit.test(name, (assert) => {
        const done = assert.async();
        loadRemote(filepath).then((codeOriginal) => {
          assert.ok(codeOriginal.length > 0);

          let compilerResult = null;
          try {
            const compiler = new UiTranslatorCompiler();
            compilerResult = compiler.compile(codeOriginal);
          } catch (e) {
            console.log(e);
            assert.ok(false);
          }

          assert.equal(compilerResult.getErrors().length, 0);

          const programResult = compilerResult.getProgram();
          assert.equal(compilerResult.getErrors().length, 0);

          if (compilerResult.getErrors().length > 0) {
            console.log(compilerResult.getErrors());
            assert.ok(false);
            return;
          }

          const codeOutput = programResult.toCode();
          
          const codeOriginalNoWhitespace = codeOriginal.replaceAll(/\w/g, "");
          const codeOutputNoWhitespace = codeOutput.replaceAll(/\w/g, "");

          assert.deepEqual(codeOriginalNoWhitespace, codeOutputNoWhitespace);
          done();
        });
      });
    };

    /*buildTest(
      "converts BAU single app substance",
      "/test/qta/ui/bau_single.qta",
    );

    buildTest(
      "converts BAU multiple app substance",
      "/test/qta/ui/bau_multiple.qta",
    );

    buildTest(
      "converts single policy",
      "/test/qta/ui/policy_single.qta",
    );

    buildTest(
      "converts multiple policies",
      "/test/qta/ui/policy_multiple.qta",
    );

    buildTest(
      "includes only business as usual",
      "/test/qta/ui/bau_single.qta",
    );

    buildTest(
      "includes additional sim",
      "/test/qta/ui/sim.qta",
    );*/
  });
}


export {buildUiTranslatorReverseTests};
