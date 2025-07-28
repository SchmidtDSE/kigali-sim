/**
 * Storage management classes for application data persistence.
 *
 * @license BSD-3-Clause
 */

/**
 * Interface for storage management classes.
 * Defines the contract for persisting and retrieving application data.
 */
class StorageKeeper {
  /**
   * Get the stored source code.
   * @returns {string|null} The source code or null if not set
   */
  getSource() {
    throw new Error("Not implemented");
  }

  /**
   * Store the source code.
   * @param {string} code The source code to store
   */
  setSource(code) {
    throw new Error("Not implemented");
  }

  /**
   * Get the hide introduction preference.
   * @returns {boolean} True if introduction should be hidden
   */
  getHideIntroduction() {
    throw new Error("Not implemented");
  }

  /**
   * Set the hide introduction preference.
   * @param {boolean} hide Whether to hide the introduction
   */
  setHideIntroduction(hide) {
    throw new Error("Not implemented");
  }

  /**
   * Clear all stored data.
   */
  clear() {
    throw new Error("Not implemented");
  }
}

/**
 * Facade to manage local storage.
 *
 * Facade to manage interactions with local storage for user preferences and
 * code.
 */
class LocalStorageKeeper extends StorageKeeper {
  static SOURCE_KEY = "source";
  static HIDE_INTRODUCTION_KEY = "hideIntroduction";

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
   * Clear all local storage data.
   */
  clear() {
    const self = this;
    localStorage.removeItem(LocalStorageKeeper.SOURCE_KEY);
    localStorage.removeItem(LocalStorageKeeper.HIDE_INTRODUCTION_KEY);
  }
}

/**
 * Ephemeral storage keeper that stores data in memory only.
 *
 * This storage keeper uses a Map to store data in memory and does not
 * persist data across browser sessions. Data is lost when the page is
 * reloaded or the browser is closed.
 */
class EphemeralStorageKeeper extends StorageKeeper {
  static SOURCE_KEY = "source";
  static HIDE_INTRODUCTION_KEY = "hideIntroduction";

  constructor() {
    super();
    this.storage = new Map();
  }

  /**
   * Get the user's source code from ephemeral storage.
   *
   * @returns {string|null} The source code or null if not found
   */
  getSource() {
    return this.storage.has(EphemeralStorageKeeper.SOURCE_KEY) ?
      this.storage.get(EphemeralStorageKeeper.SOURCE_KEY) : null;
  }

  /**
   * Save the user's source code to ephemeral storage.
   *
   * @param {string} code - The source code to save
   */
  setSource(code) {
    this.storage.set(EphemeralStorageKeeper.SOURCE_KEY, code);
  }

  /**
   * Get the user's introduction hiding preference from ephemeral storage.
   *
   * @returns {boolean} True if the introduction should be hidden, false otherwise
   */
  getHideIntroduction() {
    return this.storage.get(EphemeralStorageKeeper.HIDE_INTRODUCTION_KEY) === true;
  }

  /**
   * Set the user's introduction hiding preference in ephemeral storage.
   *
   * @param {boolean} hide - Whether to hide the introduction
   */
  setHideIntroduction(hide) {
    this.storage.set(EphemeralStorageKeeper.HIDE_INTRODUCTION_KEY, Boolean(hide));
  }

  /**
   * Clear all ephemeral storage data.
   */
  clear() {
    this.storage.clear();
  }
}

export {StorageKeeper, LocalStorageKeeper, EphemeralStorageKeeper};
