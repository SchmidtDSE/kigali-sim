import { Compiler } from "compiler";
import { ReportDataWrapper, FilterSet } from "report_data";


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
          assert.ok(compilerResult.getErrors().length == 0);

          const program = compilerResult.getProgram();
          assert.ok(compilerResult.getErrors().length == 0);

          if (compilerResult.getErrors().length > 0) {
            console.log(compilerResult.getErrors());
          } else {
            const programResult = program();
            const programResultWrapped = new ReportDataWrapper(programResult);
            checks.forEach((check) => {
              check(programResultWrapped, assert);
            });
          }
          
          done();
        });
      });
    };

    buildTest(
      "runs the base script",
      "/test/qta/multiple_with_policies.qta", [
        (result, assert) => {
          assert.ok(result !== null);
        },
      ],
    );

  });

}


export { buildReportDataTests };
