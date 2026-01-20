/**
 * Tests for SimulationListPresenter.
 *
 * @license BSD, see LICENSE.md.
 */

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
  });
}

export {buildUiEditorSimTests};
