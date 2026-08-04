/**
 * Tests for ui_editor_strategy.js functions.
 *
 * @license BSD, see LICENSE.md.
 */
import {
  validateInductionInput,
  readDurationUi,
  initSetCommandUi,
  readSetCommandUi,
  initChangeCommandUi,
  readChangeCommandUi,
  initLimitCommandUi,
  readLimitCommandUi,
} from "ui_editor_strategy";
import {EngineNumber} from "engine_number";
import {ParsedYear} from "duration";
import {Command, LimitCommand} from "ui_translator_components";

function buildUiEditorStrategyTests() {
  QUnit.module("ui_editor_strategy", function (hooks) {
    QUnit.module("validateInductionInput", function () {
      QUnit.test("accepts valid numeric values", function (assert) {
        const result0 = validateInductionInput("0");
        assert.ok(result0 instanceof EngineNumber, "0 should return EngineNumber");
        assert.equal(result0.getValue(), 0, "0 should have value 0");

        const result50 = validateInductionInput("50");
        assert.ok(result50 instanceof EngineNumber, "50 should return EngineNumber");
        assert.equal(result50.getValue(), 50, "50 should have value 50");

        const result100 = validateInductionInput("100");
        assert.ok(result100 instanceof EngineNumber, "100 should return EngineNumber");
        assert.equal(result100.getValue(), 100, "100 should have value 100");
      });

      QUnit.test("preserves original string in EngineNumber", function (assert) {
        const result = validateInductionInput("50.5");
        assert.ok(result instanceof EngineNumber, "Should return EngineNumber");
        assert.equal(result.getValue(), 50.5, "Should have correct numeric value");
        assert.equal(result.getOriginalString(), "50.5", "Should preserve original string");
      });

      QUnit.test("accepts 'default' keyword", function (assert) {
        const result = validateInductionInput("default");
        assert.equal(result, "default", "Should return 'default' string");
      });

      QUnit.test("rejects empty string with descriptive error", function (assert) {
        assert.throws(
          () => validateInductionInput(""),
          /Induction rate is required/,
          "Empty string should throw error about required field",
        );
      });

      QUnit.test("rejects negative values", function (assert) {
        assert.throws(
          () => validateInductionInput("-10"),
          /out of range.*0-100/,
          "Negative value should throw range error",
        );
      });

      QUnit.test("rejects values over 100", function (assert) {
        assert.throws(
          () => validateInductionInput("150"),
          /out of range.*0-100/,
          "Value over 100 should throw range error",
        );
      });

      QUnit.test("rejects non-numeric strings", function (assert) {
        assert.throws(
          () => validateInductionInput("abc"),
          /Invalid induction rate.*Must be a number/,
          "Non-numeric string should throw invalid format error",
        );
      });

      QUnit.test("rejects NaN values", function (assert) {
        assert.throws(
          () => validateInductionInput("NaN"),
          /Invalid induction rate.*Must be a number/,
          "NaN string should throw invalid format error",
        );
      });

      QUnit.test("handles whitespace in numeric values", function (assert) {
        const result = validateInductionInput("  50  ");
        assert.ok(result instanceof EngineNumber, "Should handle whitespace");
        assert.equal(result.getValue(), 50, "Should parse numeric value correctly");
        assert.equal(result.getOriginalString(), "50", "Should trim whitespace");
      });

      QUnit.test("accepts boundary values", function (assert) {
        const result0 = validateInductionInput("0");
        assert.equal(result0.getValue(), 0, "0 is valid boundary");

        const result100 = validateInductionInput("100");
        assert.equal(result100.getValue(), 100, "100 is valid boundary");
      });

      QUnit.test("accepts decimal values within range", function (assert) {
        const result = validateInductionInput("33.33");
        assert.ok(result instanceof EngineNumber, "Should accept decimal values");
        assert.equal(result.getValue(), 33.33, "Should preserve decimal precision");
      });
    });

    QUnit.module("readDurationUi", function () {
      QUnit.test("reads 'in year' type (both use duration-start)", function (assert) {
        const root = document.createElement("div");
        root.innerHTML = `
          <select class="duration-type-input">
            <option value="in year" selected>in year</option>
          </select>
          <input class="duration-start" value="5" />
        `;

        const result = readDurationUi(root);
        assert.ok(result, "Should return YearMatcher object");
        assert.ok(result.getStart() instanceof ParsedYear, "Start should be ParsedYear");
        assert.ok(result.getEnd() instanceof ParsedYear, "End should be ParsedYear");
        assert.equal(result.getStart().getNumericYear(), 5, "Start should be 5");
        assert.equal(result.getEnd().getNumericYear(), 5, "End should also be 5 for 'in year'");
      });

      QUnit.test("reads 'during all years' type (both null)", function (assert) {
        const root = document.createElement("div");
        root.innerHTML = `
          <select class="duration-type-input">
            <option value="during all years" selected>during all years</option>
          </select>
        `;

        const result = readDurationUi(root);
        assert.ok(result, "Should return YearMatcher object");
        assert.equal(result.getStart(), null, "Start should be null for 'during all years'");
        assert.equal(result.getEnd(), null, "End should be null for 'during all years'");
      });

      QUnit.test("reads 'starting in year' type (min only)", function (assert) {
        const root = document.createElement("div");
        root.innerHTML = `
          <select class="duration-type-input">
            <option value="starting in year" selected>starting in year</option>
          </select>
          <input class="duration-start" value="3" />
        `;

        const result = readDurationUi(root);
        assert.ok(result, "Should return YearMatcher object");
        assert.ok(result.getStart() instanceof ParsedYear, "Start should be ParsedYear");
        assert.equal(result.getEnd(), null, "End should be null for 'starting in year'");
        assert.equal(result.getStart().getNumericYear(), 3, "Start should be 3");
      });

      QUnit.test("reads 'ending in year' type (max only)", function (assert) {
        const root = document.createElement("div");
        root.innerHTML = `
          <select class="duration-type-input">
            <option value="ending in year" selected>ending in year</option>
          </select>
          <input class="duration-end" value="10" />
        `;

        const result = readDurationUi(root);
        assert.ok(result, "Should return YearMatcher object");
        assert.equal(result.getStart(), null, "Start should be null for 'ending in year'");
        assert.ok(result.getEnd() instanceof ParsedYear, "End should be ParsedYear");
        assert.equal(result.getEnd().getNumericYear(), 10, "End should be 10");
      });

      QUnit.test("reads 'during years' type (both min and max different)", function (assert) {
        const root = document.createElement("div");
        root.innerHTML = `
          <select class="duration-type-input">
            <option value="during years" selected>during years</option>
          </select>
          <input class="duration-start" value="2" />
          <input class="duration-end" value="8" />
        `;

        const result = readDurationUi(root);
        assert.ok(result, "Should return YearMatcher object");
        assert.ok(result.getStart() instanceof ParsedYear, "Start should be ParsedYear");
        assert.ok(result.getEnd() instanceof ParsedYear, "End should be ParsedYear");
        assert.equal(result.getStart().getNumericYear(), 2, "Start should be 2");
        assert.equal(result.getEnd().getNumericYear(), 8, "End should be 8");
      });

      QUnit.test("creates ParsedYear objects correctly", function (assert) {
        const root = document.createElement("div");
        root.innerHTML = `
          <select class="duration-type-input">
            <option value="during years" selected>during years</option>
          </select>
          <input class="duration-start" value="1" />
          <input class="duration-end" value="10" />
        `;

        const result = readDurationUi(root);
        const start = result.getStart();
        const end = result.getEnd();

        assert.ok(start instanceof ParsedYear, "Start should be instance of ParsedYear");
        assert.ok(end instanceof ParsedYear, "End should be instance of ParsedYear");
        assert.equal(
          typeof start.getNumericYear(),
          "number",
          "ParsedYear.getNumericYear() should return number",
        );
        assert.equal(
          typeof end.getNumericYear(),
          "number",
          "ParsedYear.getNumericYear() should return number",
        );
      });

      QUnit.test("handles empty duration inputs", function (assert) {
        const root = document.createElement("div");
        root.innerHTML = `
          <select class="duration-type-input">
            <option value="in year" selected>in year</option>
          </select>
          <input class="duration-start" value="" />
        `;

        // Should handle empty duration-start input - will get empty string value
        const result = readDurationUi(root);
        assert.ok(result, "Should return YearMatcher even with empty inputs");
      });

      QUnit.test("handles string year values", function (assert) {
        const root = document.createElement("div");
        root.innerHTML = `
          <select class="duration-type-input">
            <option value="during years" selected>during years</option>
          </select>
          <input class="duration-start" value="2025" />
          <input class="duration-end" value="2030" />
        `;

        const result = readDurationUi(root);
        assert.equal(result.getStart().getNumericYear(), 2025, "Should handle large year values");
        assert.equal(result.getEnd().getNumericYear(), 2030, "Should handle large year values");
      });
    });

    QUnit.module("initChangeCommandUi", function () {
      const noopStreamUpdater = {
        getEnabledStreamsForCurrentContext: () => [],
        updateStreamOptionStates: () => {},
      };

      function buildChangeCommandRoot() {
        const template = document.getElementById("change-command-fixture");
        const root = document.createElement("div");
        root.appendChild(template.content.cloneNode(true));
        return root;
      }

      QUnit.test("sets sign dropdown to '-' for a negative change value", function (assert) {
        const root = buildChangeCommandRoot();
        const command = new Command("change", "sales", new EngineNumber(-5, "% / year"), null);

        initChangeCommandUi(command, root, null, "consumption", noopStreamUpdater);

        assert.equal(
          root.querySelector(".change-sign-input").value,
          "-",
          "Sign dropdown should reflect the negative value",
        );
        assert.equal(
          root.querySelector(".change-amount-input").value,
          "5",
          "Amount should be shown unsigned",
        );
      });

      QUnit.test("sets sign dropdown to '+' for a positive change value", function (assert) {
        const root = buildChangeCommandRoot();
        const command = new Command("change", "sales", new EngineNumber(5, "% / year"), null);

        initChangeCommandUi(command, root, null, "consumption", noopStreamUpdater);

        assert.equal(
          root.querySelector(".change-sign-input").value,
          "+",
          "Sign dropdown should reflect the positive value",
        );
      });

      QUnit.test("round trips a negative change value through init and read", function (assert) {
        const root = buildChangeCommandRoot();
        const command = new Command("change", "sales", new EngineNumber(-5, "% / year"), null);

        initChangeCommandUi(command, root, null, "consumption", noopStreamUpdater);
        const reread = readChangeCommandUi(root);

        assert.equal(
          reread.getValue().getValue(),
          -5,
          "Re-reading the UI should preserve the negative sign",
        );
      });

      QUnit.test("round trips a newEquipment target through init and read", function (assert) {
        const root = buildChangeCommandRoot();
        const command = new Command("change", "newEquipment", new EngineNumber(10, "%"), null);

        initChangeCommandUi(command, root, null, "consumption", noopStreamUpdater);

        assert.equal(
          root.querySelector(".change-target-input").value,
          "newEquipment",
          "Target select should match the newEquipment option, not fall back to empty",
        );

        const reread = readChangeCommandUi(root);
        assert.equal(
          reread.getTarget(),
          "newEquipment",
          "Re-reading the UI should preserve the newEquipment target",
        );
      });
    });

    QUnit.module("initSetCommandUi", function () {
      const noopStreamUpdater = {
        getEnabledStreamsForCurrentContext: () => [],
        updateStreamOptionStates: () => {},
      };

      function buildSetCommandRoot() {
        const template = document.getElementById("set-command-fixture");
        const root = document.createElement("div");
        root.appendChild(template.content.cloneNode(true));
        return root;
      }

      QUnit.test("round trips a newEquipment target through init and read", function (assert) {
        const root = buildSetCommandRoot();
        const command = new Command("setVal", "newEquipment", new EngineNumber(50, "units"), null);

        initSetCommandUi(command, root, null, "consumption", noopStreamUpdater);

        assert.equal(
          root.querySelector(".set-target-input").value,
          "newEquipment",
          "Target select should match the newEquipment option, not fall back to empty",
        );

        const reread = readSetCommandUi(root);
        assert.equal(
          reread.getTarget(),
          "newEquipment",
          "Re-reading the UI should preserve the newEquipment target",
        );
      });
    });

    QUnit.module("initLimitCommandUi", function () {
      const noopStreamUpdater = {
        getEnabledStreamsForCurrentContext: () => [],
        updateStreamOptionStates: () => {},
      };
      const noopCodeObj = {getSubstances: () => []};

      function buildLimitCommandRoot() {
        const template = document.getElementById("limit-command-fixture");
        const root = document.createElement("div");
        root.appendChild(template.content.cloneNode(true));
        return root;
      }

      QUnit.test("round trips a newEquipment target through init and read", function (assert) {
        const root = buildLimitCommandRoot();
        const command = new LimitCommand(
          "cap", "newEquipment", new EngineNumber(200, "units"), null, null, "",
        );

        initLimitCommandUi(command, root, noopCodeObj, "consumption", noopStreamUpdater);

        assert.equal(
          root.querySelector(".limit-target-input").value,
          "newEquipment",
          "Target select should match the newEquipment option, not fall back to empty",
        );

        const reread = readLimitCommandUi(root);
        assert.equal(
          reread.getTarget(),
          "newEquipment",
          "Re-reading the UI should preserve the newEquipment target",
        );
      });

      QUnit.test(
        "round trips a newEquipment displacing target through init and read",
        function (assert) {
          const root = buildLimitCommandRoot();
          const command = new LimitCommand(
            "cap", "sales", new EngineNumber(200, "units"), null, "newEquipment", "",
          );

          initLimitCommandUi(command, root, noopCodeObj, "consumption", noopStreamUpdater);

          assert.equal(
            root.querySelector(".displacing-target-input").value,
            "newEquipment",
            "Displacing target select should match the newEquipment option, not fall back to empty",
          );

          const reread = readLimitCommandUi(root);
          assert.equal(
            reread.getDisplacing(),
            "newEquipment",
            "Re-reading the UI should preserve the newEquipment displacing target",
          );
        },
      );
    });

    QUnit.module("index.html target dropdown exclusions", function () {
      QUnit.test(
        "replace-command-template's replace-target-input has no newEquipment option",
        function (assert) {
          const done = assert.async();
          fetch("/index.html")
            .then((response) => response.text())
            .then((content) => {
              const templateMatch = content.match(
                /<script type="text\/template" id="replace-command-template">([\s\S]*?)<\/script>/,
              );
              assert.ok(templateMatch !== null, "replace-command-template should exist");

              const templateHtml = templateMatch[1];
              const targetSelectMatch = templateHtml.match(
                /<select class="replace-target-input"[\s\S]*?<\/select>/,
              );
              assert.ok(targetSelectMatch !== null, "replace-target-input select should exist");

              assert.equal(
                targetSelectMatch[0].indexOf("newEquipment"),
                -1,
                "replace-target-input should NOT offer newEquipment (replace is not " +
                  "implemented for newEquipment; see Component 7)",
              );

              done();
            });
        },
      );
    });
  });
}

export {buildUiEditorStrategyTests};
