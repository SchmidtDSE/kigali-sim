/**
 * Logic to manage local storage (in-browser storage).
 *
 * @license BSD-3-Clause
 */


/**
 * Facade to manage local storage.
 *
 * Facade to manage interactions with local storage for user preferences and
 * code.
 */
class LocalStorageKeeper {
  static SOURCE_KEY = "source";
  static HIDE_INTRODUCTION_KEY = "hideIntroduction";
  static SHOW_TOOLTIPS_KEY = "showTooltips";

  /**
   * Get the user's saved source code from local storage.
   *
   * @returns {string|null} The saved source code or null if not found
   */
  getSource() {
    const self = this;
    return localStorage.getItem(LocalStorageKeeper.SOURCE_KEY);
  }

  /**
   * Save the user's source code to local storage.
   *
   * @param {string} code - The source code to save
   */
  setSource(code) {
    const self = this;
    localStorage.setItem(LocalStorageKeeper.SOURCE_KEY, code);
  }

  /**
   * Get the user's introduction hiding preference from local storage.
   *
   * @returns {boolean} True if the introduction should be hidden, false otherwise
   */
  getHideIntroduction() {
    const self = this;
    return localStorage.getItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY) === "true";
  }

  /**
   * Set the user's introduction hiding preference in local storage.
   *
   * @param {boolean} hide - Whether to hide the introduction
   */
  setHideIntroduction(hide) {
    const self = this;
    const value = Boolean(hide).toString();
    localStorage.setItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY, value);
  }

  /**
   * Get the user's tooltip display preference from local storage.
   *
   * @returns {boolean|null} True if tooltips should be shown, null if not set
   */
  getShowTooltips() {
    const self = this;
    const stored = localStorage.getItem(LocalStorageKeeper.SHOW_TOOLTIPS_KEY);
    return stored === null ? null : stored === "true";
  }

  /**
   * Set the user's tooltip display preference in local storage.
   *
   * @param {boolean} show - Whether to show tooltips
   */
  setShowTooltips(show) {
    const self = this;
    const value = Boolean(show).toString();
    localStorage.setItem(LocalStorageKeeper.SHOW_TOOLTIPS_KEY, value);
  }

  /**
   * Clear all local storage data.
   */
  clear() {
    const self = this;
    localStorage.removeItem(LocalStorageKeeper.SOURCE_KEY);
    localStorage.removeItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY);
    localStorage.removeItem(LocalStorageKeeper.SHOW_TOOLTIPS_KEY);
  }
}

export {LocalStorageKeeper};
