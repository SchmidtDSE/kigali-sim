import {UiTranslatorCompiler} from "ui_translator";


function buildUiTranslatorReverseTests() {
  QUnit.module("UiTranslatorCompilerReverse", function() {
    QUnit.test("initializes", function(assert) {
      const compiler = new UiTranslatorCompiler();
      assert.ok(compiler !== undefined);
    });
  });
}


export {buildUiTranslatorReverseTests};
