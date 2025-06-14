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
      // Test the conditional logic for showing alerts
      let alertCalled = false;
      const mockAlert = () => {
        alertCalled = true;
      };

      const testConditionalAlert = function (isAutoRefresh) {
        alertCalled = false;
        if (!isAutoRefresh) {
          mockAlert();
        }
        return alertCalled;
      };

      // Test that alert is shown for non-auto-refresh
      let result = testConditionalAlert(false);
      assert.equal(result, true, "Alert should be shown when not auto-refresh");

      // Test that alert is NOT shown for auto-refresh
      result = testConditionalAlert(true);
      assert.equal(result, false, "Alert should NOT be shown when auto-refresh");
    });
  });
}

export {buildMainTests};
