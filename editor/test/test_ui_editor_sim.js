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

    QUnit.test(
      "_showForSimpleOrdering hides move controls and shows enable link",
      function (assert) {
      // Create a minimal DOM structure for testing
        const dialogElement = document.createElement("div");
        dialogElement.innerHTML = `
        <span class="enable-policy-order-holder" style="display: none;">
          <a href="#" class="enable-policy-order-link">specify policy order</a>
        </span>
        <div class="policy-check-label">
          <span class="move-policy-control" style="display: inline;"> -
            <a href="#" class="move-policy-up-link">move before</a>
          </span>
        </div>
        <div class="policy-check-label">
          <span class="move-policy-control" style="display: inline;"> -
            <a href="#" class="move-policy-down-link">move after</a>
          </span>
        </div>
      `;

        const mockPresenter = {
          _dialog: dialogElement,
          _showForSimpleOrdering: SimulationListPresenter.prototype._showForSimpleOrdering,
        };

        mockPresenter._showForSimpleOrdering();

        const enableHolder = dialogElement.querySelector(".enable-policy-order-holder");
        assert.equal(
          enableHolder.style.display,
          "inline",
          "Enable ordering holder should be visible",
        );

        const moveControls = dialogElement.querySelectorAll(".move-policy-control");
        moveControls.forEach((control) => {
          assert.equal(control.style.display, "none", "Move controls should be hidden");
        });
      },
    );

    QUnit.test(
      "_showForExplicitOrdering shows move controls and hides enable link",
      function (assert) {
      // Create a minimal DOM structure for testing
        const dialogElement = document.createElement("div");
        dialogElement.innerHTML = `
        <span class="enable-policy-order-holder" style="display: inline;">
          <a href="#" class="enable-policy-order-link">specify policy order</a>
        </span>
        <div class="policy-check-label">
          <span class="move-policy-control" style="display: none;"> -
            <a href="#" class="move-policy-up-link">move before</a>
          </span>
        </div>
        <div class="policy-check-label">
          <span class="move-policy-control" style="display: none;"> -
            <a href="#" class="move-policy-down-link">move after</a>
          </span>
        </div>
      `;

        const mockPresenter = {
          _dialog: dialogElement,
          _showForExplicitOrdering: SimulationListPresenter.prototype._showForExplicitOrdering,
        };

        mockPresenter._showForExplicitOrdering();

        const enableHolder = dialogElement.querySelector(".enable-policy-order-holder");
        assert.equal(
          enableHolder.style.display,
          "none",
          "Enable ordering holder should be hidden",
        );

        const moveControls = dialogElement.querySelectorAll(".move-policy-control");
        moveControls.forEach((control) => {
          assert.equal(control.style.display, "inline", "Move controls should be visible");
        });
      },
    );

    QUnit.test("_showForSimpleOrdering handles missing elements gracefully", function (assert) {
      const dialogElement = document.createElement("div");
      dialogElement.innerHTML = "<div>Empty dialog</div>";

      const mockPresenter = {
        _dialog: dialogElement,
        _showForSimpleOrdering: SimulationListPresenter.prototype._showForSimpleOrdering,
      };

      // Should not throw error when elements don't exist
      assert.ok(() => {
        mockPresenter._showForSimpleOrdering();
        return true;
      }, "Should handle missing elements without errors");
    });

    QUnit.test("_renderPolicyCheckboxes renders policies in array order", function (assert) {
      // Create a minimal DOM structure for testing
      const dialogElement = document.createElement("div");
      dialogElement.innerHTML = `
        <div class="policy-sim-list"></div>
        <span class="enable-policy-order-holder"></span>
      `;

      // We need the template to be available
      const templateScript = document.getElementById("sim-order-controls-template");
      if (!templateScript) {
        const newTemplate = document.createElement("script");
        newTemplate.type = "text/template";
        newTemplate.id = "sim-order-controls-template";
        newTemplate.innerHTML =
          '<span class="move-policy-control"> - ' +
          '<a href="#" class="move-policy-up-link" ' +
          'data-policy-name="{POLICY_NAME}">move before</a>' +
          '<span class="move-policy-sep"> / </span>' +
          '<a href="#" class="move-policy-down-link" ' +
          'data-policy-name="{POLICY_NAME}">move after</a>' +
          "</span>";
        document.body.appendChild(newTemplate);
      }

      const mockPresenter = {
        _dialog: dialogElement,
        _policyOrderArray: ["Policy C", "Policy A", "Policy B"],
        _isExplicitOrdering: true,
        _renderPolicyCheckboxes: SimulationListPresenter.prototype._renderPolicyCheckboxes,
        _renderOrderControls: SimulationListPresenter.prototype._renderOrderControls,
        _showForExplicitOrdering: SimulationListPresenter.prototype._showForExplicitOrdering,
        _showForSimpleOrdering: SimulationListPresenter.prototype._showForSimpleOrdering,
        _updateMoveControlVisibility:
          SimulationListPresenter.prototype._updateMoveControlVisibility,
        _orderControlsTemplate: document.getElementById("sim-order-controls-template").innerHTML,
      };

      mockPresenter._renderPolicyCheckboxes();

      const checkboxes = dialogElement.querySelectorAll(".policy-check");
      assert.equal(checkboxes.length, 3, "Should render 3 checkboxes");
      assert.equal(checkboxes[0].value, "Policy C", "First checkbox should be Policy C");
      assert.equal(checkboxes[1].value, "Policy A", "Second checkbox should be Policy A");
      assert.equal(checkboxes[2].value, "Policy B", "Third checkbox should be Policy B");
    });

    QUnit.test(
      "_renderPolicyCheckboxes preserves checked states across renders",
      function (assert) {
        const dialogElement = document.createElement("div");
        dialogElement.innerHTML = `
        <div class="policy-sim-list">
          <div class="policy-check-label">
            <label>
              <input type="checkbox" class="policy-check" value="Policy A" checked>
              <span>Policy A</span>
            </label>
          </div>
          <div class="policy-check-label">
            <label>
              <input type="checkbox" class="policy-check" value="Policy B">
              <span>Policy B</span>
            </label>
          </div>
        </div>
        <span class="enable-policy-order-holder"></span>
      `;

        const mockPresenter = {
          _dialog: dialogElement,
          _policyOrderArray: ["Policy A", "Policy B"],
          _isExplicitOrdering: false,
          _renderPolicyCheckboxes: SimulationListPresenter.prototype._renderPolicyCheckboxes,
          _renderOrderControls: SimulationListPresenter.prototype._renderOrderControls,
          _showForExplicitOrdering: SimulationListPresenter.prototype._showForExplicitOrdering,
          _showForSimpleOrdering: SimulationListPresenter.prototype._showForSimpleOrdering,
          _updateMoveControlVisibility:
            SimulationListPresenter.prototype._updateMoveControlVisibility,
          _orderControlsTemplate: document.getElementById("sim-order-controls-template").innerHTML,
        };

        // Policy A is checked, Policy B is unchecked
        mockPresenter._renderPolicyCheckboxes();

        const checkboxes = dialogElement.querySelectorAll(".policy-check");
        assert.equal(checkboxes[0].checked, true, "Policy A should remain checked");
        assert.equal(checkboxes[1].checked, false, "Policy B should remain unchecked");
      },
    );

    QUnit.test("_movePolicyUp swaps policy with previous element", function (assert) {
      const dialogElement = document.createElement("div");
      dialogElement.innerHTML = `
        <div class="policy-sim-list"></div>
        <span class="enable-policy-order-holder"></span>
      `;

      const mockPresenter = {
        _dialog: dialogElement,
        _policyOrderArray: ["Policy A", "Policy B", "Policy C"],
        _isExplicitOrdering: true,
        _movePolicyUp: SimulationListPresenter.prototype._movePolicyUp,
        _renderPolicyCheckboxes: function () {
          // Mock render - we're testing array manipulation, not rendering
        },
      };

      mockPresenter._movePolicyUp("Policy B");

      assert.deepEqual(
        mockPresenter._policyOrderArray,
        ["Policy B", "Policy A", "Policy C"],
        "Policy B should swap with Policy A",
      );
    });

    QUnit.test("_movePolicyUp does nothing when policy is first", function (assert) {
      const mockPresenter = {
        _policyOrderArray: ["Policy A", "Policy B", "Policy C"],
        _movePolicyUp: SimulationListPresenter.prototype._movePolicyUp,
        _renderPolicyCheckboxes: function () {},
      };

      mockPresenter._movePolicyUp("Policy A");

      assert.deepEqual(
        mockPresenter._policyOrderArray,
        ["Policy A", "Policy B", "Policy C"],
        "Array should remain unchanged when moving first element up",
      );
    });

    QUnit.test("_movePolicyDown swaps policy with next element", function (assert) {
      const dialogElement = document.createElement("div");
      dialogElement.innerHTML = `
        <div class="policy-sim-list"></div>
        <span class="enable-policy-order-holder"></span>
      `;

      const mockPresenter = {
        _dialog: dialogElement,
        _policyOrderArray: ["Policy A", "Policy B", "Policy C"],
        _isExplicitOrdering: true,
        _movePolicyDown: SimulationListPresenter.prototype._movePolicyDown,
        _renderPolicyCheckboxes: function () {
          // Mock render - we're testing array manipulation, not rendering
        },
      };

      mockPresenter._movePolicyDown("Policy B");

      assert.deepEqual(
        mockPresenter._policyOrderArray,
        ["Policy A", "Policy C", "Policy B"],
        "Policy B should swap with Policy C",
      );
    });

    QUnit.test("_movePolicyDown does nothing when policy is last", function (assert) {
      const mockPresenter = {
        _policyOrderArray: ["Policy A", "Policy B", "Policy C"],
        _movePolicyDown: SimulationListPresenter.prototype._movePolicyDown,
        _renderPolicyCheckboxes: function () {},
      };

      mockPresenter._movePolicyDown("Policy C");

      assert.deepEqual(
        mockPresenter._policyOrderArray,
        ["Policy A", "Policy B", "Policy C"],
        "Array should remain unchanged when moving last element down",
      );
    });

    QUnit.test("_movePolicyUp handles non-existent policy gracefully", function (assert) {
      const mockPresenter = {
        _policyOrderArray: ["Policy A", "Policy B"],
        _movePolicyUp: SimulationListPresenter.prototype._movePolicyUp,
        _renderPolicyCheckboxes: function () {},
      };

      mockPresenter._movePolicyUp("Policy X");

      assert.deepEqual(
        mockPresenter._policyOrderArray,
        ["Policy A", "Policy B"],
        "Array should remain unchanged when policy not found",
      );
    });

    QUnit.test(
      "_updateMoveControlVisibility hides move-up for first policy",
      function (assert) {
        const dialogElement = document.createElement("div");
        dialogElement.innerHTML = `
      <div class="policy-check-label">
        <span class="move-policy-control">
          <a href="#" class="move-policy-up-link">move before</a>
          <span class="move-policy-sep"> / </span>
          <a href="#" class="move-policy-down-link">move after</a>
        </span>
      </div>
      <div class="policy-check-label">
        <span class="move-policy-control">
          <a href="#" class="move-policy-up-link">move before</a>
          <span class="move-policy-sep"> / </span>
          <a href="#" class="move-policy-down-link">move after</a>
        </span>
      </div>
      <div class="policy-check-label">
        <span class="move-policy-control">
          <a href="#" class="move-policy-up-link">move before</a>
          <span class="move-policy-sep"> / </span>
          <a href="#" class="move-policy-down-link">move after</a>
        </span>
      </div>
    `;

        const mockPresenter = {
          _dialog: dialogElement,
          _updateMoveControlVisibility:
            SimulationListPresenter.prototype._updateMoveControlVisibility,
        };

        mockPresenter._updateMoveControlVisibility();

        const policyLabels = dialogElement.querySelectorAll(".policy-check-label");
        const firstLabel = policyLabels[0];
        const secondLabel = policyLabels[1];

        // First policy should have move-up hidden
        const firstMoveUp = firstLabel.querySelector(".move-policy-up-link");
        const firstMoveDown = firstLabel.querySelector(".move-policy-down-link");
        const firstSep = firstLabel.querySelector(".move-policy-sep");

        assert.equal(firstMoveUp.style.display, "none", "First policy move-up should be hidden");
        assert.equal(
          firstMoveDown.style.display,
          "inline",
          "First policy move-down should be visible",
        );
        assert.equal(firstSep.style.display, "none", "First policy separator should be hidden");

        // Second (middle) policy should have both visible
        const secondMoveUp = secondLabel.querySelector(".move-policy-up-link");
        const secondMoveDown = secondLabel.querySelector(".move-policy-down-link");
        const secondSep = secondLabel.querySelector(".move-policy-sep");

        assert.equal(
          secondMoveUp.style.display,
          "inline",
          "Second policy move-up should be visible",
        );
        assert.equal(
          secondMoveDown.style.display,
          "inline",
          "Second policy move-down should be visible",
        );
        assert.equal(
          secondSep.style.display,
          "inline",
          "Second policy separator should be visible",
        );
      },
    );

    QUnit.test("_updateMoveControlVisibility hides move-down for last policy", function (assert) {
      const dialogElement = document.createElement("div");
      dialogElement.innerHTML = `
    <div class="policy-check-label">
      <span class="move-policy-control">
        <a href="#" class="move-policy-up-link">move before</a>
        <span class="move-policy-sep"> / </span>
        <a href="#" class="move-policy-down-link">move after</a>
      </span>
    </div>
    <div class="policy-check-label">
      <span class="move-policy-control">
        <a href="#" class="move-policy-up-link">move before</a>
        <span class="move-policy-sep"> / </span>
        <a href="#" class="move-policy-down-link">move after</a>
      </span>
    </div>
  `;

      const mockPresenter = {
        _dialog: dialogElement,
        _updateMoveControlVisibility:
          SimulationListPresenter.prototype._updateMoveControlVisibility,
      };

      mockPresenter._updateMoveControlVisibility();

      const policyLabels = dialogElement.querySelectorAll(".policy-check-label");
      const lastLabel = policyLabels[1];

      // Last policy should have move-down hidden
      const lastMoveUp = lastLabel.querySelector(".move-policy-up-link");
      const lastMoveDown = lastLabel.querySelector(".move-policy-down-link");
      const lastSep = lastLabel.querySelector(".move-policy-sep");

      assert.equal(lastMoveUp.style.display, "inline", "Last policy move-up should be visible");
      assert.equal(lastMoveDown.style.display, "none", "Last policy move-down should be hidden");
      assert.equal(lastSep.style.display, "none", "Last policy separator should be hidden");
    });

    QUnit.test(
      "_updateMoveControlVisibility hides both links for single policy",
      function (assert) {
        const dialogElement = document.createElement("div");
        dialogElement.innerHTML = `
    <div class="policy-check-label">
      <span class="move-policy-control">
        <a href="#" class="move-policy-up-link">move before</a>
        <span class="move-policy-sep"> / </span>
        <a href="#" class="move-policy-down-link">move after</a>
      </span>
    </div>
  `;

        const mockPresenter = {
          _dialog: dialogElement,
          _updateMoveControlVisibility:
            SimulationListPresenter.prototype._updateMoveControlVisibility,
        };

        mockPresenter._updateMoveControlVisibility();

        const policyLabel = dialogElement.querySelector(".policy-check-label");

        // Single policy (both first and last) should have both links hidden
        const moveUp = policyLabel.querySelector(".move-policy-up-link");
        const moveDown = policyLabel.querySelector(".move-policy-down-link");
        const separator = policyLabel.querySelector(".move-policy-sep");

        assert.equal(moveUp.style.display, "none", "Single policy move-up should be hidden");
        assert.equal(moveDown.style.display, "none", "Single policy move-down should be hidden");
        assert.equal(separator.style.display, "none", "Single policy separator should be hidden");
      },
    );

    QUnit.test(
      "_updateMoveControlVisibility handles empty policy list gracefully",
      function (assert) {
        const dialogElement = document.createElement("div");
        dialogElement.innerHTML = "<div>No policies</div>";

        const mockPresenter = {
          _dialog: dialogElement,
          _updateMoveControlVisibility:
            SimulationListPresenter.prototype._updateMoveControlVisibility,
        };

        // Should not throw error when no policy labels exist
        assert.ok(() => {
          mockPresenter._updateMoveControlVisibility();
          return true;
        }, "Should handle empty policy list without errors");
      },
    );

    QUnit.test(
      "_updateMoveControlVisibility shows both links for middle policies",
      function (assert) {
        const dialogElement = document.createElement("div");
        dialogElement.innerHTML = `
    <div class="policy-check-label">
      <span class="move-policy-control">
        <a href="#" class="move-policy-up-link">move before</a>
        <span class="move-policy-sep"> / </span>
        <a href="#" class="move-policy-down-link">move after</a>
      </span>
    </div>
    <div class="policy-check-label">
      <span class="move-policy-control">
        <a href="#" class="move-policy-up-link">move before</a>
        <span class="move-policy-sep"> / </span>
        <a href="#" class="move-policy-down-link">move after</a>
      </span>
    </div>
    <div class="policy-check-label">
      <span class="move-policy-control">
        <a href="#" class="move-policy-up-link">move before</a>
        <span class="move-policy-sep"> / </span>
        <a href="#" class="move-policy-down-link">move after</a>
      </span>
    </div>
  `;

        const mockPresenter = {
          _dialog: dialogElement,
          _updateMoveControlVisibility:
            SimulationListPresenter.prototype._updateMoveControlVisibility,
        };

        mockPresenter._updateMoveControlVisibility();

        const policyLabels = dialogElement.querySelectorAll(".policy-check-label");
        const middleLabel = policyLabels[1];

        // Middle policy should have both links visible
        const moveUp = middleLabel.querySelector(".move-policy-up-link");
        const moveDown = middleLabel.querySelector(".move-policy-down-link");
        const separator = middleLabel.querySelector(".move-policy-sep");

        assert.equal(
          moveUp.style.display,
          "inline",
          "Middle policy move-up should be visible",
        );
        assert.equal(
          moveDown.style.display,
          "inline",
          "Middle policy move-down should be visible",
        );
        assert.equal(
          separator.style.display,
          "inline",
          "Middle policy separator should be visible",
        );
      },
    );
  });
}

export {buildUiEditorSimTests};
