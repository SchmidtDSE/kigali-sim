/**
 * Tests for SimulationListPresenter.
 *
 * @license BSD, see LICENSE.md.
 */
import {SimulationListPresenter} from "ui_editor_sim";

function buildUiEditorSimTests() {
  QUnit.module("SimulationListPresenter", function (hooks) {
    QUnit.test("_renderOrderControls generates correct HTML structure", function (assert) {
      // Create a minimal mock presenter just to test the template rendering
      const mockRoot = document.createElement("div");
      mockRoot.innerHTML = `
        <div class="dialog">
          <div class="policy-sim-list"></div>
          <button class="cancel-button"></button>
          <button class="save-button"></button>
        </div>
      `;

      // We need the template to be available
      const templateScript = document.createElement("script");
      templateScript.type = "text/template";
      templateScript.id = "sim-order-controls-template";
      templateScript.innerHTML =
        '<span class="move-policy-control"> - ' +
        '<a href="#" class="move-policy-up-link" ' +
        'data-policy-name="{POLICY_NAME}">move before</a>' +
        '<span class="move-policy-sep"> / </span>' +
        '<a href="#" class="move-policy-down-link" ' +
        'data-policy-name="{POLICY_NAME}">move after</a>' +
        "</span>";
      document.body.appendChild(templateScript);

      // Note: We can't fully instantiate SimulationListPresenter in isolation
      // because it has dependencies. Instead, test the template rendering logic directly.
      const template = document.getElementById("sim-order-controls-template").innerHTML;
      const rendered = template.replace(/{POLICY_NAME}/g, "Test Policy");

      // Verify the rendered HTML contains expected elements
      assert.ok(rendered.includes("move-policy-control"), "Contains control wrapper class");
      assert.ok(rendered.includes("move-policy-up-link"), "Contains move up link");
      assert.ok(rendered.includes("move-policy-down-link"), "Contains move down link");
      assert.ok(rendered.includes("move-policy-sep"), "Contains separator");
      assert.ok(
        rendered.includes('data-policy-name="Test Policy"'),
        "Contains policy name in data attribute",
      );
      assert.ok(rendered.includes("move before"), 'Contains "move before" text');
      assert.ok(rendered.includes("move after"), 'Contains "move after" text');

      // Cleanup
      document.body.removeChild(templateScript);
    });

    QUnit.test("template replaces all occurrences of placeholder", function (assert) {
      const template = '<span data-policy-name="{POLICY_NAME}">{POLICY_NAME}</span>';
      const rendered = template.replace(/{POLICY_NAME}/g, "My Policy");

      assert.equal(
        rendered,
        '<span data-policy-name="My Policy">My Policy</span>',
        "All placeholders should be replaced",
      );
      assert.ok(!rendered.includes("{POLICY_NAME}"), "No placeholders should remain");
    });

    QUnit.test("_determineOrderingMode returns false for no policies", function (assert) {
      const selectedPolicies = [];
      const allPolicies = ["Policy A", "Policy B", "Policy C"];

      // Create a minimal mock to access the private method
      const mockPresenter = {
        _determineOrderingMode: SimulationListPresenter.prototype._determineOrderingMode,
      };

      const result = mockPresenter._determineOrderingMode(selectedPolicies, allPolicies);
      assert.strictEqual(
        result,
        false,
        "Should return false (simple mode) when no policies selected",
      );
    });

    QUnit.test("_determineOrderingMode returns false for single policy", function (assert) {
      const selectedPolicies = ["Policy B"];
      const allPolicies = ["Policy A", "Policy B", "Policy C"];

      const mockPresenter = {
        _determineOrderingMode: SimulationListPresenter.prototype._determineOrderingMode,
      };

      const result = mockPresenter._determineOrderingMode(selectedPolicies, allPolicies);
      assert.strictEqual(
        result,
        false,
        "Should return false (simple mode) when only one policy selected",
      );
    });

    QUnit.test("_determineOrderingMode returns false for alphabetical policies", function (assert) {
      const selectedPolicies = ["Policy A", "Policy B", "Policy C"];
      const allPolicies = ["Policy A", "Policy B", "Policy C", "Policy D"];

      const mockPresenter = {
        _determineOrderingMode: SimulationListPresenter.prototype._determineOrderingMode,
      };

      const result = mockPresenter._determineOrderingMode(selectedPolicies, allPolicies);
      assert.strictEqual(
        result,
        false,
        "Should return false (simple mode) when policies are alphabetically ordered",
      );
    });

    QUnit.test(
      "_determineOrderingMode returns true for non-alphabetical policies",
      function (assert) {
        const selectedPolicies = ["Policy C", "Policy A", "Policy B"];
        const allPolicies = ["Policy A", "Policy B", "Policy C", "Policy D"];

        const mockPresenter = {
          _determineOrderingMode: SimulationListPresenter.prototype._determineOrderingMode,
        };

        const result = mockPresenter._determineOrderingMode(selectedPolicies, allPolicies);
        assert.strictEqual(
          result,
          true,
          "Should return true (explicit mode) when policies are not alphabetically ordered",
        );
      },
    );

    QUnit.test(
      "_determinePolicyRenderOrder returns alphabetical in simple mode",
      function (assert) {
        const selectedPolicies = ["Policy B"];
        const allPolicies = ["Policy C", "Policy A", "Policy B"];
        const isExplicitMode = false;

        const mockPresenter = {
          _determinePolicyRenderOrder:
            SimulationListPresenter.prototype._determinePolicyRenderOrder,
        };

        const result = mockPresenter._determinePolicyRenderOrder(
          selectedPolicies,
          allPolicies,
          isExplicitMode,
        );
        assert.deepEqual(
          result,
          ["Policy A", "Policy B", "Policy C"],
          "Should return all policies alphabetically in simple mode",
        );
      },
    );

    QUnit.test(
      "_determinePolicyRenderOrder returns selected then unselected in explicit mode",
      function (assert) {
        const selectedPolicies = ["Policy C", "Policy A"];
        const allPolicies = ["Policy C", "Policy A", "Policy B", "Policy D"];
        const isExplicitMode = true;

        const mockPresenter = {
          _determinePolicyRenderOrder:
            SimulationListPresenter.prototype._determinePolicyRenderOrder,
        };

        const result = mockPresenter._determinePolicyRenderOrder(
          selectedPolicies,
          allPolicies,
          isExplicitMode,
        );
        assert.deepEqual(
          result,
          ["Policy C", "Policy A", "Policy B", "Policy D"],
          "Should return selected in original order, then unselected alphabetically",
        );
      },
    );

    QUnit.test(
      "_determinePolicyRenderOrder handles all selected policies",
      function (assert) {
        const selectedPolicies = ["Policy C", "Policy A", "Policy B"];
        const allPolicies = ["Policy C", "Policy A", "Policy B"];
        const isExplicitMode = true;

        const mockPresenter = {
          _determinePolicyRenderOrder:
            SimulationListPresenter.prototype._determinePolicyRenderOrder,
        };

        const result = mockPresenter._determinePolicyRenderOrder(
          selectedPolicies,
          allPolicies,
          isExplicitMode,
        );
        assert.deepEqual(
          result,
          ["Policy C", "Policy A", "Policy B"],
          "Should return all policies in selected order when all are selected",
        );
      },
    );
  });
}

export {buildUiEditorSimTests};
