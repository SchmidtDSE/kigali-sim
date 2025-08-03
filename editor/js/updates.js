/**
 * Utility for checking and handling application updates in PWA environment.
 *
 * Utility for checking for updates and handling user notification either in
 * PWA or service. Implements offline-first approach where all network failures
 * are handled silently without showing any errors to the user.
 *
 * @license BSD, see LICENSE.md.
 */

/**
 * Utility class for handling application updates.
 *
 * Provides methods to check for available updates by comparing the current
 * application version (embedded in HTML) with the latest version available
 * on the server (version.txt). All network operations fail silently to
 * support offline usage.
 */
class UpdateUtil {
  /**
   * Check if an application update is available.
   *
   * Compares the current app version (from hidden input) with the server
   * version (from version.txt). Fails silently on any network errors to
   * support offline usage.
   *
   * @returns {Promise<boolean>} Promise that resolves to true if update
   *     is available, false otherwise. Always resolves (never rejects).
   */
  async checkForUpdates() {
    const self = this;
    try {
      // Get current version from hidden input in DOM
      const versionInput = document.getElementById("app-version");
      if (!versionInput) {
        // No version input found, assume no update needed
        return false;
      }

      const currentVersion = versionInput.value;
      if (!currentVersion || currentVersion === "EPOCH") {
        // Invalid current version (development mode), assume no update needed
        return false;
      }

      // Fetch latest version from server with cache busting
      const response = await fetch(`/version.txt?t=${Date.now()}`, {
        method: "GET",
        cache: "no-cache",
        headers: {
          "Cache-Control": "no-cache",
        },
      });

      if (!response.ok) {
        // Network error or file not found, fail silently
        return false;
      }

      const latestVersion = (await response.text()).trim();
      if (!latestVersion) {
        // Empty or invalid version file, assume no update
        return false;
      }

      // Compare versions (both should be epoch timestamps)
      const currentTimestamp = parseInt(currentVersion, 10);
      const latestTimestamp = parseInt(latestVersion, 10);

      if (isNaN(currentTimestamp) || isNaN(latestTimestamp)) {
        // Invalid version format, assume no update
        return false;
      }

      // Update available if server version is newer
      return latestTimestamp > currentTimestamp;
    } catch (error) {
      // Any error (network, parsing, etc.) - fail silently
      return false;
    }
  }

  /**
   * Show the update notice dialog to the user.
   *
   * Displays a modal dialog informing the user that an update is available
   * and offering options to reload now or continue with current version.
   *
   * @param {Function} [saveCallback] - Optional callback to explicitly save
   *     current state before reload
   * @param {boolean} [isUpToDate] - Whether the user is already up to date
   * @returns {Promise<string>} Promise that resolves to 'reload' if user
   *     chooses to reload, 'continue' if user chooses to continue, or 'save_failed'
   *     if save callback fails. Never rejects.
   */
  async showUpdateDialog(saveCallback = null, isUpToDate = false) {
    const self = this;
    return new Promise((resolve) => {
      const dialog = document.getElementById("update-notice-dialog");

      // Show/hide up-to-date message based on update status
      const upToDateMessage = dialog.querySelector("#up-to-date-message");
      if (upToDateMessage) {
        upToDateMessage.style.display = isUpToDate ? "block" : "none";
      }

      // Set up event handlers
      const reloadButton = dialog.querySelector(".reload-button");
      const continueButton = dialog.querySelector(".cancel-button");

      const handleReload = (event) => {
        event.preventDefault();

        // Show loading indicator and hide buttons
        const loadingIndicator = dialog.querySelector("#update-loading-indicator");
        const buttonPanel = dialog.querySelector(".dialog-buttons");
        loadingIndicator.style.display = "block";
        buttonPanel.style.display = "none";

        // Explicitly save current state before reloading if callback provided
        if (saveCallback && typeof saveCallback === "function") {
          try {
            saveCallback();
          } catch (error) {
            // Show alert if save fails and don't reload
            const message = "Failed to save your current work. " +
              "Please try again or save manually before updating.";
            alert(message);
            console.error("Save callback failed, aborting reload:", error);

            // Restore UI state
            loadingIndicator.style.display = "none";
            buttonPanel.style.display = "block";
            resolve("save_failed");
            return;
          }
        }

        // Perform hard refresh operations
        const promises = [];

        // Clear all browser caches if supported
        if ("caches" in window) {
          const cachesClearPromise = caches.keys().then((names) => {
            return Promise.all(names.map((name) => caches.delete(name)));
          });
          promises.push(cachesClearPromise);
        } else {
          promises.push(Promise.resolve());
        }

        // Update service worker if supported
        if ("serviceWorker" in navigator) {
          const serviceWorkerUpdatePromise = navigator.serviceWorker.getRegistrations()
            .then((registrations) => {
              return Promise.all(registrations.map((registration) => registration.update()));
            });
          promises.push(serviceWorkerUpdatePromise);
        } else {
          promises.push(Promise.resolve());
        }

        // Wait for both operations to complete, then reload after 1 second delay
        Promise.all(promises).then(() => {
          setTimeout(() => {
            window.location.reload();
            resolve("reload");
          }, 1000);
        }).catch((error) => {
          console.warn("Error during cache/service worker operations:", error);
          // Still reload even if cache operations fail
          setTimeout(() => {
            window.location.reload();
            resolve("reload");
          }, 1000);
        });
      };

      const handleContinue = (event) => {
        event.preventDefault();
        dialog.close();
        resolve("continue");
      };

      const handleClose = () => {
        resolve("continue");
      };

      // Add event listeners
      if (reloadButton) {
        reloadButton.addEventListener("click", handleReload);
      }
      if (continueButton) {
        continueButton.addEventListener("click", handleContinue);
      }
      dialog.addEventListener("close", handleClose);

      // Show the dialog
      dialog.showModal();
    });
  }
}

export {UpdateUtil};
