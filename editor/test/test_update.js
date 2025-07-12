/**
 * Tests for updates.js functionality.
 *
 * @license BSD, see LICENSE.md.
 */

function buildUpdateTests() {
  QUnit.module("UpdateUtil", function () {
    QUnit.test("checkForUpdates returns false when no version input exists",
      async function (assert) {
      // Remove any existing version input
        const existingInput = document.getElementById("app-version");
        if (existingInput) {
          existingInput.remove();
        }

        const {UpdateUtil} = await import("../js/updates.js");
        const updateUtil = new UpdateUtil();
        const result = await updateUtil.checkForUpdates();

        assert.false(result, "Should return false when no version input exists");
      });

    QUnit.test("checkForUpdates returns false when version input has EPOCH value",
      async function (assert) {
      // Create version input with EPOCH value (development mode)
        const versionInput = document.createElement("input");
        versionInput.type = "hidden";
        versionInput.id = "app-version";
        versionInput.value = "EPOCH";
        document.body.appendChild(versionInput);

        const {UpdateUtil} = await import("../js/updates.js");
        const updateUtil = new UpdateUtil();
        const result = await updateUtil.checkForUpdates();

        assert.false(result, "Should return false when version is EPOCH (development mode)");

        // Cleanup
        versionInput.remove();
      });

    QUnit.test("checkForUpdates returns false when version input is empty",
      async function (assert) {
      // Create version input with empty value
        const versionInput = document.createElement("input");
        versionInput.type = "hidden";
        versionInput.id = "app-version";
        versionInput.value = "";
        document.body.appendChild(versionInput);

        const {UpdateUtil} = await import("../js/updates.js");
        const updateUtil = new UpdateUtil();
        const result = await updateUtil.checkForUpdates();

        assert.false(result, "Should return false when version is empty");

        // Cleanup
        versionInput.remove();
      });

    QUnit.test("checkForUpdates handles network errors silently", async function (assert) {
      // Create version input with valid timestamp
      const versionInput = document.createElement("input");
      versionInput.type = "hidden";
      versionInput.id = "app-version";
      versionInput.value = "1752320637"; // Valid timestamp
      document.body.appendChild(versionInput);

      // Mock fetch to throw network error
      const originalFetch = window.fetch;
      window.fetch = () => Promise.reject(new Error("Network error"));

      const {UpdateUtil} = await import("../js/updates.js");
      const updateUtil = new UpdateUtil();
      const result = await updateUtil.checkForUpdates();

      assert.false(result, "Should return false on network error");

      // Cleanup
      window.fetch = originalFetch;
      versionInput.remove();
    });

    QUnit.test("checkForUpdates returns false when fetch returns non-ok response",
      async function (assert) {
      // Create version input with valid timestamp
        const versionInput = document.createElement("input");
        versionInput.type = "hidden";
        versionInput.id = "app-version";
        versionInput.value = "1752320637"; // Valid timestamp
        document.body.appendChild(versionInput);

        // Mock fetch to return 404
        const originalFetch = window.fetch;
        window.fetch = () => Promise.resolve({
          ok: false,
          status: 404,
        });

        const {UpdateUtil} = await import("../js/updates.js");
        const updateUtil = new UpdateUtil();
        const result = await updateUtil.checkForUpdates();

        assert.false(result, "Should return false when version.txt not found");

        // Cleanup
        window.fetch = originalFetch;
        versionInput.remove();
      });

    QUnit.test("checkForUpdates returns false when versions are equal", async function (assert) {
      const timestamp = "1752320637";

      // Create version input with timestamp
      const versionInput = document.createElement("input");
      versionInput.type = "hidden";
      versionInput.id = "app-version";
      versionInput.value = timestamp;
      document.body.appendChild(versionInput);

      // Mock fetch to return same timestamp
      const originalFetch = window.fetch;
      window.fetch = () => Promise.resolve({
        ok: true,
        text: () => Promise.resolve(timestamp),
      });

      const {UpdateUtil} = await import("../js/updates.js");
      const updateUtil = new UpdateUtil();
      const result = await updateUtil.checkForUpdates();

      assert.false(result, "Should return false when versions are equal");

      // Cleanup
      window.fetch = originalFetch;
      versionInput.remove();
    });

    QUnit.test("checkForUpdates returns true when server version is newer",
      async function (assert) {
        const currentTimestamp = "1752320637";
        const newerTimestamp = "1752320640";

        // Create version input with older timestamp
        const versionInput = document.createElement("input");
        versionInput.type = "hidden";
        versionInput.id = "app-version";
        versionInput.value = currentTimestamp;
        document.body.appendChild(versionInput);

        // Mock fetch to return newer timestamp
        const originalFetch = window.fetch;
        window.fetch = () => Promise.resolve({
          ok: true,
          text: () => Promise.resolve(newerTimestamp),
        });

        const {UpdateUtil} = await import("../js/updates.js");
        const updateUtil = new UpdateUtil();
        const result = await updateUtil.checkForUpdates();

        assert.true(result, "Should return true when server version is newer");

        // Cleanup
        window.fetch = originalFetch;
        versionInput.remove();
      });

    QUnit.test("checkForUpdates returns false when server version is older",
      async function (assert) {
        const currentTimestamp = "1752320640";
        const olderTimestamp = "1752320637";

        // Create version input with newer timestamp
        const versionInput = document.createElement("input");
        versionInput.type = "hidden";
        versionInput.id = "app-version";
        versionInput.value = currentTimestamp;
        document.body.appendChild(versionInput);

        // Mock fetch to return older timestamp
        const originalFetch = window.fetch;
        window.fetch = () => Promise.resolve({
          ok: true,
          text: () => Promise.resolve(olderTimestamp),
        });

        const {UpdateUtil} = await import("../js/updates.js");
        const updateUtil = new UpdateUtil();
        const result = await updateUtil.checkForUpdates();

        assert.false(result, "Should return false when server version is older");

        // Cleanup
        window.fetch = originalFetch;
        versionInput.remove();
      });

    QUnit.test("checkForUpdates handles invalid version formats", async function (assert) {
      // Create version input with invalid format
      const versionInput = document.createElement("input");
      versionInput.type = "hidden";
      versionInput.id = "app-version";
      versionInput.value = "invalid";
      document.body.appendChild(versionInput);

      // Mock fetch to return valid timestamp
      const originalFetch = window.fetch;
      window.fetch = () => Promise.resolve({
        ok: true,
        text: () => Promise.resolve("1752320637"),
      });

      const {UpdateUtil} = await import("../js/updates.js");
      const updateUtil = new UpdateUtil();
      const result = await updateUtil.checkForUpdates();

      assert.false(result, "Should return false when current version format is invalid");

      // Cleanup
      window.fetch = originalFetch;
      versionInput.remove();
    });
  });
}

export {buildUpdateTests};
