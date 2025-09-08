/**
 * Tests for LocalStorageKeeper functionality.
 *
 * @license BSD, see LICENSE.md.
 */

import {LocalStorageKeeper} from "local_storage_keeper";

function buildLocalStorageKeeperTests() {
  QUnit.module("LocalStorageKeeper", function () {
    QUnit.test("getSource and setSource work correctly", function (assert) {
      const keeper = new LocalStorageKeeper();

      // Test setting and getting source code
      const testCode = `start default
  define application "refrigeration"
    uses substance "HFC-134a"
      enable domestic
      initial charge with 0.15 kg / unit for domestic
      set domestic to 500 mt during year 1
      equals 1430 kgCO2e / kg
    end substance
  end application
end default`;

      keeper.setSource(testCode);
      assert.equal(keeper.getSource(), testCode,
        "Should retrieve the same source code that was set");

      // Test with empty string
      keeper.setSource("");
      assert.equal(keeper.getSource(), "", "Should handle empty string correctly");

      // Test with null (clear)
      localStorage.removeItem(LocalStorageKeeper.SOURCE_KEY);
      assert.equal(keeper.getSource(), null, "Should return null when no source is stored");
    });

    QUnit.test("getHideIntroduction and setHideIntroduction work correctly", function (assert) {
      const keeper = new LocalStorageKeeper();

      // Test setting true
      keeper.setHideIntroduction(true);
      assert.equal(keeper.getHideIntroduction(), true, "Should return true when set to true");

      // Test setting false
      keeper.setHideIntroduction(false);
      assert.equal(keeper.getHideIntroduction(), false, "Should return false when set to false");

      // Test with no value (clear)
      localStorage.removeItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY);
      assert.equal(keeper.getHideIntroduction(), false,
        "Should return false when no value is stored");

      // Test string conversion
      localStorage.setItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY, "true");
      assert.equal(keeper.getHideIntroduction(), true,
        "Should convert string 'true' to boolean true");

      localStorage.setItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY, "false");
      assert.equal(keeper.getHideIntroduction(), false,
        "Should convert string 'false' to boolean false");
    });

    QUnit.test("clear method removes both keys", function (assert) {
      const keeper = new LocalStorageKeeper();

      // Set both values
      const qubecTalkCode = `start default
  define application "test"
    uses substance "test"
      enable manufacture
      set manufacture to 100 mt
    end substance
  end application
end default`;

      keeper.setSource(qubecTalkCode);
      keeper.setHideIntroduction(true);

      // Verify they are set
      assert.equal(keeper.getSource(), qubecTalkCode, "Source should be set before clear");
      assert.equal(keeper.getHideIntroduction(), true,
        "Hide introduction should be set before clear");

      // Clear and verify removal
      keeper.clear();
      assert.equal(keeper.getSource(), null, "Source should be null after clear");
      assert.equal(keeper.getHideIntroduction(), false,
        "Hide introduction should be false after clear");
    });

    QUnit.test("static keys are correctly defined", function (assert) {
      assert.equal(LocalStorageKeeper.SOURCE_KEY, "source", "SOURCE_KEY should be 'source'");
      assert.equal(LocalStorageKeeper.HIDE_INTRODUCTION_KEY, "hideIntroduction",
        "HIDE_INTRODUCTION_KEY should be 'hideIntroduction'");
    });

    QUnit.test("edge cases handled correctly", function (assert) {
      const keeper = new LocalStorageKeeper();

      // Test with special characters in source
      const specialCode = `start default
  define application "test app"
    uses substance "test-substance"
      enable manufacture
      # Comment with special chars: @#$%^&*()
      set manufacture to 50 mt during year 1
      equals 100 kgCO2e / kg
    end substance
  end application
end default`;

      keeper.setSource(specialCode);
      assert.equal(keeper.getSource(), specialCode,
        "Should handle special characters in source code");

      // Test boolean conversion edge cases
      keeper.setHideIntroduction(!!1); // truthy value converted to boolean first
      assert.equal(keeper.getHideIntroduction(), true, "Should convert truthy values to true");

      keeper.setHideIntroduction(!!0); // falsy value converted to boolean first
      assert.equal(keeper.getHideIntroduction(), false, "Should convert falsy values to false");

      // Test with null/undefined
      keeper.setHideIntroduction(null);
      assert.equal(keeper.getHideIntroduction(), false, "Should convert null to false");

      keeper.setHideIntroduction(undefined);
      assert.equal(keeper.getHideIntroduction(), false, "Should convert undefined to false");
    });
  });
}

export {buildLocalStorageKeeperTests};
