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

  /**
   * Get the user's saved source code from local storage.
   *
   * @returns {string|null} The saved source code or null if not found
   */
  getSource() {
    return localStorage.getItem(LocalStorageKeeper.SOURCE_KEY);
  }

  /**
   * Save the user's source code to local storage.
   *
   * @param {string} code - The source code to save
   */
  setSource(code) {
    localStorage.setItem(LocalStorageKeeper.SOURCE_KEY, code);
  }

  /**
   * Get the user's introduction hiding preference from local storage.
   *
   * @returns {boolean} True if the introduction should be hidden, false otherwise
   */
  getHideIntroduction() {
    return localStorage.getItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY) === "true";
  }

  /**
   * Set the user's introduction hiding preference in local storage.
   *
   * @param {boolean} hide - Whether to hide the introduction
   */
  setHideIntroduction(hide) {
    const value = Boolean(hide).toString();
    localStorage.setItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY, value);
  }

  /**
   * Clear all local storage data.
   */
  clear() {
    localStorage.removeItem(LocalStorageKeeper.SOURCE_KEY);
    localStorage.removeItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY);
  }
}

export {LocalStorageKeeper};
