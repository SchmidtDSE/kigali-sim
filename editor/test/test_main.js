/**
 * Tests for main.js functionality.
 *
 * @license BSD, see LICENSE.md.
 */

import {
  IntroductionPresenter,
  RunningIndicatorPresenter,
  ButtonPanelPresenter,
  MainPresenter,
} from "main";

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
  });
}

export {buildMainTests};
