/**
 * Tests for storage keeper classes.
 *
 * @license BSD-3-Clause
 */

import {StorageKeeper, LocalStorageKeeper, EphemeralStorageKeeper} from "storage_keeper";

function buildStorageKeeperTests() {
  QUnit.module("StorageKeeper Interface", function () {
    QUnit.test("interface methods throw not implemented errors", function (assert) {
      const storageKeeper = new StorageKeeper();

      assert.throws(
        () => storageKeeper.getSource(),
        /Not implemented/,
        "getSource throws not implemented error",
      );

      assert.throws(
        () => storageKeeper.setSource("test"),
        /Not implemented/,
        "setSource throws not implemented error",
      );

      assert.throws(
        () => storageKeeper.getHideIntroduction(),
        /Not implemented/,
        "getHideIntroduction throws not implemented error",
      );

      assert.throws(
        () => storageKeeper.setHideIntroduction(true),
        /Not implemented/,
        "setHideIntroduction throws not implemented error",
      );

      assert.throws(
        () => storageKeeper.clear(),
        /Not implemented/,
        "clear throws not implemented error",
      );
    });
  });

  QUnit.module("LocalStorageKeeper", function () {
    QUnit.test("getSource and setSource work correctly", function (assert) {
      const keeper = new LocalStorageKeeper();

      // Test setting and getting source code
      const testCode = `start default
  define application "refrigeration"
    uses substance "HFC-134a"
      enable manufacture
      initial charge with 0.15 kg / unit for manufacture
      set manufacture to 500 mt during year 1
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

    QUnit.test("extends StorageKeeper interface", function (assert) {
      const keeper = new LocalStorageKeeper();

      assert.ok(keeper instanceof StorageKeeper, "LocalStorageKeeper should extend StorageKeeper");
      assert.ok(typeof keeper.getSource === "function", "Should have getSource method");
      assert.ok(typeof keeper.setSource === "function", "Should have setSource method");
      assert.ok(typeof keeper.getHideIntroduction === "function",
        "Should have getHideIntroduction method");
      assert.ok(typeof keeper.setHideIntroduction === "function",
        "Should have setHideIntroduction method");
      assert.ok(typeof keeper.clear === "function", "Should have clear method");
    });
  });

  QUnit.module("EphemeralStorageKeeper", function () {
    QUnit.test("can store and retrieve source code", function (assert) {
      const keeper = new EphemeralStorageKeeper();

      assert.strictEqual(keeper.getSource(), null, "Initially returns null for source");

      keeper.setSource("test code");
      assert.strictEqual(keeper.getSource(), "test code", "Returns stored source code");

      keeper.setSource("updated code");
      assert.strictEqual(keeper.getSource(), "updated code", "Updates stored source code");
    });

    QUnit.test("can store and retrieve hide introduction preference", function (assert) {
      const keeper = new EphemeralStorageKeeper();

      assert.strictEqual(keeper.getHideIntroduction(), false,
        "Initially returns false for hide introduction");

      keeper.setHideIntroduction(true);
      assert.strictEqual(keeper.getHideIntroduction(), true, "Returns true when set to true");

      keeper.setHideIntroduction(false);
      assert.strictEqual(keeper.getHideIntroduction(), false, "Returns false when set to false");
    });

    QUnit.test("clear method removes all data", function (assert) {
      const keeper = new EphemeralStorageKeeper();

      keeper.setSource("test code");
      keeper.setHideIntroduction(true);

      assert.strictEqual(keeper.getSource(), "test code", "Source is set before clear");
      assert.strictEqual(keeper.getHideIntroduction(), true,
        "Hide introduction is set before clear");

      keeper.clear();

      assert.strictEqual(keeper.getSource(), null, "Source is null after clear");
      assert.strictEqual(keeper.getHideIntroduction(), false,
        "Hide introduction is false after clear");
    });

    QUnit.test("data does not persist across instances", function (assert) {
      const keeper1 = new EphemeralStorageKeeper();
      keeper1.setSource("test data");
      keeper1.setHideIntroduction(true);

      const keeper2 = new EphemeralStorageKeeper();

      assert.strictEqual(keeper2.getSource(), null, "New instance has no source data");
      assert.strictEqual(keeper2.getHideIntroduction(), false,
        "New instance has no hide introduction preference");
    });

    QUnit.test("handles empty and null values properly", function (assert) {
      const keeper = new EphemeralStorageKeeper();

      keeper.setSource("");
      assert.strictEqual(keeper.getSource(), "", "Can store empty string");

      keeper.setSource(null);
      assert.strictEqual(keeper.getSource(), null, "Can store null");

      keeper.setHideIntroduction(0);
      assert.strictEqual(keeper.getHideIntroduction(), false, "Converts falsy values to false");

      keeper.setHideIntroduction("true");
      assert.strictEqual(keeper.getHideIntroduction(), true, "Converts truthy values to true");
    });
  });
}

export {buildStorageKeeperTests};
