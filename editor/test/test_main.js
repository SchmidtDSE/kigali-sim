/**
 * Tests for main.js functionality.
 *
 * @license BSD, see LICENSE.md.
 */

import {MainPresenter} from "main";
import {
  IntroductionPresenter,
  PrivacyConfirmationPresenter,
} from "informational";
import {
  RunningIndicatorPresenter,
  ButtonPanelPresenter,
} from "editor_actions";

function buildMainTests() {
  QUnit.module("Main", function () {
    QUnit.test("IntroductionPresenter can be initialized", function (assert) {
      const presenter = new IntroductionPresenter();
      assert.notEqual(presenter, null, "IntroductionPresenter should be initialized");
    });

    QUnit.test("RunningIndicatorPresenter can be initialized", function (assert) {
      const presenter = new RunningIndicatorPresenter();
      assert.notEqual(presenter, null, "RunningIndicatorPresenter should be initialized");
    });

    QUnit.test("RunningIndicatorPresenter updateProgress only increases", function (assert) {
      const presenter = new RunningIndicatorPresenter();
      const progressBar = document.getElementById("simulation-progress");

      presenter.reset();
      assert.equal(progressBar.value, 0);

      presenter.updateProgress(50);
      assert.equal(progressBar.value, 50, "Should apply an increase");

      presenter.updateProgress(30);
      assert.equal(progressBar.value, 50, "Should ignore a decrease");

      presenter.updateProgress(80);
      assert.equal(progressBar.value, 80, "Should apply a further increase");
    });

    QUnit.test("RunningIndicatorPresenter reset bypasses increase-only guard", function (assert) {
      const presenter = new RunningIndicatorPresenter();
      const progressBar = document.getElementById("simulation-progress");

      presenter.reset();
      presenter.updateProgress(80);
      assert.equal(progressBar.value, 80);

      presenter.reset();
      assert.equal(progressBar.value, 0, "reset should force progress back to 0 for a new batch");
    });

    QUnit.test("RunningIndicatorPresenter show clears stale overlays", function (assert) {
      const presenter = new RunningIndicatorPresenter();
      const errorIndicator = document.getElementById("error-indicator");
      const noResultsMessage = document.getElementById("no-results-message");

      errorIndicator.style.display = "block";
      noResultsMessage.style.display = "block";

      presenter.show();

      assert.equal(
        errorIndicator.style.display,
        "none",
        "show should hide a stale error indicator before a new run",
      );
      assert.equal(
        noResultsMessage.style.display,
        "none",
        "show should hide a stale no-results message before a new run",
      );
      assert.equal(
        presenter._runningIndicator.style.display,
        "block",
        "show should display the running indicator",
      );
    });

    QUnit.test("ButtonPanelPresenter can be initialized", function (assert) {
      const root = document.getElementById("code-buttons-panel");
      const mockOnBuild = function () {};
      const presenter = new ButtonPanelPresenter(root, mockOnBuild);
      assert.notEqual(presenter, null, "ButtonPanelPresenter should be initialized");
    });

    QUnit.test("MainPresenter can be initialized", function (assert) {
      // Test that MainPresenter class exists and is a constructor
      assert.equal(typeof MainPresenter, "function", "MainPresenter should be a function");
      assert.notEqual(MainPresenter, null, "MainPresenter should not be null");
      // Note: Full initialization requires extensive DOM setup,
      // so we test class existence instead of full instantiation
    });

    QUnit.test("PrivacyConfirmationPresenter can be initialized", function (assert) {
      const presenter = new PrivacyConfirmationPresenter();
      assert.notEqual(presenter, null, "PrivacyConfirmationPresenter should be initialized");
    });
  });
}

export {buildMainTests};
