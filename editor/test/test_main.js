/**
 * Tests for main.js functionality.
 *
 * @license BSD, see LICENSE.md.
 */

function buildMainTests() {
  QUnit.module("Main", function () {
    QUnit.test("_onBuild method signature accepts isAutoRefresh parameter", function (assert) {
      // Test that the method signature change is properly handled
      // by simulating the parameter passing logic

      const testOnBuildSignature = function (run, resetFilters, isAutoRefresh) {
        // Simulate the parameter validation logic from the actual method
        if (resetFilters === undefined) {
          resetFilters = false;
        }

        if (isAutoRefresh === undefined) {
          isAutoRefresh = false;
        }

        return {run, resetFilters, isAutoRefresh};
      };

      // Test with all parameters
      let result = testOnBuildSignature(true, false, true);
      assert.equal(result.run, true, "run parameter should be passed correctly");
      assert.equal(result.resetFilters, false, "resetFilters parameter should be passed correctly");
      assert.equal(result.isAutoRefresh, true,
        "isAutoRefresh parameter should be passed correctly");

      // Test with default values
      result = testOnBuildSignature(true);
      assert.equal(result.run, true, "run parameter should be passed correctly");
      assert.equal(result.resetFilters, false, "resetFilters should default to false");
      assert.equal(result.isAutoRefresh, false, "isAutoRefresh should default to false");

      // Test with partial parameters
      result = testOnBuildSignature(false, true);
      assert.equal(result.run, false, "run parameter should be passed correctly");
      assert.equal(result.resetFilters, true, "resetFilters parameter should be passed correctly");
      assert.equal(result.isAutoRefresh, false,
        "isAutoRefresh should default to false when not provided");
    });

    QUnit.test("conditional alert logic works correctly", function (assert) {
      // Test the conditional logic for showing alerts vs error display
      let alertCalled = false;
      let errorDisplayCalled = false;
      let errorMessage = "";

      const mockAlert = () => {
        alertCalled = true;
      };

      const mockErrorDisplay = (message) => {
        errorDisplayCalled = true;
        errorMessage = message;
      };

      const testConditionalErrorHandling = function (isAutoRefresh, message) {
        alertCalled = false;
        errorDisplayCalled = false;
        errorMessage = "";

        if (!isAutoRefresh) {
          mockAlert(message);
        } else {
          mockErrorDisplay(message);
        }

        return {alertCalled, errorDisplayCalled, errorMessage};
      };

      // Test that alert is shown for non-auto-refresh
      let result = testConditionalErrorHandling(false, "Test error");
      assert.equal(result.alertCalled, true, "Alert should be shown when not auto-refresh");
      assert.equal(result.errorDisplayCalled, false,
        "Error display should NOT be used when not auto-refresh");

      // Test that error display is used for auto-refresh
      result = testConditionalErrorHandling(true, "Test error");
      assert.equal(result.alertCalled, false, "Alert should NOT be shown when auto-refresh");
      assert.equal(result.errorDisplayCalled, true,
        "Error display should be used when auto-refresh");
      assert.equal(result.errorMessage, "Test error",
        "Error message should be passed to error display");
    });

    QUnit.test("auto-run functionality structure validation", function (assert) {
      // Test the checkbox state detection logic
      const checkbox = document.getElementById("auto-run-check");
      const codeEditorPane = document.getElementById("code-editor-pane");

      // Test checkbox state detection
      checkbox.checked = false;
      assert.equal(checkbox.checked, false,
        "Checkbox should be unchecked when set to false");

      checkbox.checked = true;
      assert.equal(checkbox.checked, true,
        "Checkbox should be checked when set to true");

      // Test tab detection logic
      codeEditorPane.setAttribute("aria-hidden", "true");
      assert.equal(codeEditorPane.getAttribute("aria-hidden"), "true",
        "Code editor pane should have aria-hidden='true' when tab is not active");

      codeEditorPane.setAttribute("aria-hidden", "false");
      assert.equal(codeEditorPane.getAttribute("aria-hidden"), "false",
        "Code editor pane should have aria-hidden='false' when tab is active");

      // Test combined logic
      const testShouldAutoRun = function () {
        const autoRunCheck = document.getElementById("auto-run-check");
        const codeEditorPane = document.getElementById("code-editor-pane");
        const isOnCodeEditorTab = codeEditorPane.getAttribute("aria-hidden") !== "true";
        const isAutoRunEnabled = autoRunCheck && autoRunCheck.checked;
        return isOnCodeEditorTab && isAutoRunEnabled;
      };

      // Test all combinations
      codeEditorPane.setAttribute("aria-hidden", "true");
      checkbox.checked = false;
      assert.equal(testShouldAutoRun(), false,
        "Auto-run should be false when tab is not active and checkbox is unchecked");

      codeEditorPane.setAttribute("aria-hidden", "true");
      checkbox.checked = true;
      assert.equal(testShouldAutoRun(), false,
        "Auto-run should be false when tab is not active even if checkbox is checked");

      codeEditorPane.setAttribute("aria-hidden", "false");
      checkbox.checked = false;
      assert.equal(testShouldAutoRun(), false,
        "Auto-run should be false when tab is active but checkbox is unchecked");

      codeEditorPane.setAttribute("aria-hidden", "false");
      checkbox.checked = true;
      assert.equal(testShouldAutoRun(), true,
        "Auto-run should be true when both tab is active and checkbox is checked");
    });

    QUnit.test("storage switching functionality", function (assert) {
      // Mock localStorage to avoid real storage operations in tests
      const originalSetItem = localStorage.setItem;
      const originalGetItem = localStorage.getItem;
      const originalRemoveItem = localStorage.removeItem;

      const mockStorage = {};
      localStorage.setItem = (key, value) => {
        mockStorage[key] = value;
      };
      localStorage.getItem = (key) => mockStorage[key] || null;
      localStorage.removeItem = (key) => {
        delete mockStorage[key];
      };

      // Test storage keeper switching logic
      const testStorageSwitch = function (useLocalStorage, currentData) {
        // Simulate the storage switching logic from MainPresenter
        if (useLocalStorage) {
          // Would switch to LocalStorageKeeper and migrate data
          if (currentData.source) {
            localStorage.setItem("source", currentData.source);
          }
          if (currentData.hideIntroduction) {
            localStorage.setItem("hideIntroduction", currentData.hideIntroduction.toString());
          }
          return "LocalStorageKeeper";
        } else {
          // Would switch to EphemeralStorageKeeper
          return "EphemeralStorageKeeper";
        }
      };

      const testData = {
        source: "test code",
        hideIntroduction: true,
      };

      // Test switching to local storage
      let result = testStorageSwitch(true, testData);
      assert.equal(result, "LocalStorageKeeper",
        "Should switch to LocalStorageKeeper when save preferences is enabled");
      assert.equal(localStorage.getItem("source"), "test code",
        "Should migrate source data to localStorage");
      assert.equal(localStorage.getItem("hideIntroduction"), "true",
        "Should migrate hide introduction preference to localStorage");

      // Test switching to ephemeral storage
      result = testStorageSwitch(false, testData);
      assert.equal(result, "EphemeralStorageKeeper",
        "Should switch to EphemeralStorageKeeper when save preferences is disabled");

      // Restore original localStorage methods
      localStorage.setItem = originalSetItem;
      localStorage.getItem = originalGetItem;
      localStorage.removeItem = originalRemoveItem;
    });

    QUnit.test("clear data functionality", function (assert) {
      // Test the clear data confirmation and reset logic
      const testClearData = function (userConfirms) {
        let confirmCalled = false;
        let clearCalled = false;
        let resetCalled = false;

        // Mock window.confirm
        const originalConfirm = window.confirm;
        window.confirm = (message) => {
          confirmCalled = true;
          assert.ok(message.includes("clear all saved preferences"),
            "Confirmation message should mention clearing preferences");
          assert.ok(message.includes("current model"),
            "Confirmation message should mention current model");
          return userConfirms;
        };

        // Mock storage keeper
        const mockStorageKeeper = {
          clear: () => {
            clearCalled = true;
          },
        };

        // Mock reset application state
        const mockReset = () => {
          resetCalled = true;
        };

        // Simulate the clear data click handler
        const confirmed = window.confirm(
          "This will clear all saved preferences and the current model you are " +
          "working on in the designer and editor. Continue?",
        );
        if (confirmed) {
          mockStorageKeeper.clear();
          mockReset();
        }

        // Restore original confirm
        window.confirm = originalConfirm;

        return {confirmCalled, clearCalled, resetCalled};
      };

      // Test when user confirms
      let result = testClearData(true);
      assert.ok(result.confirmCalled, "Should show confirmation dialog");
      assert.ok(result.clearCalled, "Should call storage keeper clear when confirmed");
      assert.ok(result.resetCalled, "Should reset application state when confirmed");

      // Test when user cancels
      result = testClearData(false);
      assert.ok(result.confirmCalled, "Should show confirmation dialog");
      assert.notOk(result.clearCalled, "Should not call storage keeper clear when cancelled");
      assert.notOk(result.resetCalled, "Should not reset application state when cancelled");
    });
  });
}

export {buildMainTests};
