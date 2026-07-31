/**
 * Tests for ui_editor_util.js functions.
 *
 * @license BSD, see LICENSE.md.
 */
import {
  validateNumericInput,
  validateSimulationDurationInput,
} from "ui_editor_util";

function buildUiEditorUtilTests() {
  QUnit.module("ui_editor_util", function () {
    QUnit.module("validateNumericInput", function () {
      QUnit.test("accepts simple valid numbers", function (assert) {
        assert.ok(validateNumericInput("123", false), "Plain integer should be valid");
        assert.ok(validateNumericInput("123.45", false), "Decimal should be valid");
        assert.ok(validateNumericInput("-123.45", false), "Negative decimal should be valid");
        assert.ok(validateNumericInput("+5", false), "Explicit positive should be valid");
        assert.ok(validateNumericInput("1,234.56", false), "UK thousands format should be valid");
      });

      QUnit.test("accepts valid year keywords for duration fields", function (assert) {
        assert.ok(
          validateNumericInput("onwards", true),
          "'onwards' should be valid for a duration field",
        );
        assert.ok(
          validateNumericInput("beginning", true),
          "'beginning' should be valid for a duration field",
        );
      });

      QUnit.test("rejects year keywords when not a duration field", function (assert) {
        assert.notOk(
          validateNumericInput("onwards", false),
          "'onwards' should be suspect when field is not a duration field",
        );
      });

      QUnit.test("rejects values with letters", function (assert) {
        assert.notOk(validateNumericInput("abc", false), "Letters should be suspect");
        assert.notOk(validateNumericInput("123abc", false), "Mixed alphanumeric should be suspect");
      });

      QUnit.test("rejects unsupported European number formats", function (assert) {
        assert.notOk(
          validateNumericInput("123.456,78", false),
          "European format should be suspect",
        );
      });

      QUnit.test("accepts likely intentional formulas", function (assert) {
        assert.ok(
          validateNumericInput("get value as percent", false),
          "Formula containing 'get ' and ' as ' should be treated as intentional",
        );
        assert.ok(
          validateNumericInput("get sales as kg for HFC-134a", false),
          "Realistic formula example should be treated as intentional",
        );
      });

      QUnit.test("does not treat partial formula keywords as a formula", function (assert) {
        assert.notOk(
          validateNumericInput("get value", false),
          "Missing ' as ' should still be treated as suspect",
        );
        assert.notOk(
          validateNumericInput("value as percent", false),
          "Missing 'get ' should still be treated as suspect",
        );
      });
    });

    QUnit.module("validateSimulationDurationInput", function () {
      QUnit.test("accepts reasonable durations", function (assert) {
        assert.ok(
          validateSimulationDurationInput("1", "10"),
          "Short duration should be valid",
        );
        assert.ok(
          validateSimulationDurationInput("2020", "3020"),
          "Duration of exactly 1000 years should be valid",
        );
      });

      QUnit.test("accepts non-simple-integer inputs without flagging", function (assert) {
        assert.ok(
          validateSimulationDurationInput("onwards", "10"),
          "Non-integer start should skip validation and be treated as valid",
        );
        assert.ok(
          validateSimulationDurationInput("1", "get end as year"),
          "Non-integer end should skip validation and be treated as valid",
        );
      });

      QUnit.test("rejects extremely long durations", function (assert) {
        assert.notOk(
          validateSimulationDurationInput("1", "2000"),
          "Duration over 1000 years should be suspect",
        );
      });
    });
  });
}

// Export for use in test runner
if (typeof window !== "undefined") {
  window.buildUiEditorUtilTests = buildUiEditorUtilTests;
}

export {buildUiEditorUtilTests};
