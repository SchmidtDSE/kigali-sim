/**
 * Classes for handling substance lookup functionality.
 *
 * @license BSD, see LICENSE.md.
 */

/**
 * Represents a known substance with its name and GWP value.
 */
class KnownSubstance {
  /**
   * Creates a new KnownSubstance.
   *
   * @param {string} name - The substance name.
   * @param {number} gwp - The GWP value as a number.
   */
  constructor(name, gwp) {
    const self = this;
    self._name = name;
    self._gwp = gwp;
  }

  /**
   * Gets the substance name.
   *
   * @returns {string} The substance name.
   */
  getName() {
    const self = this;
    return self._name;
  }

  /**
   * Gets the GWP value.
   *
   * @returns {number} The GWP value as a number.
   */
  getGwp() {
    const self = this;
    return self._gwp;
  }
}

/**
 * Manages a library of known substances with flexible name matching.
 */
class SubstanceLibraryKeeper {
  /**
   * Creates a new SubstanceLibraryKeeper.
   *
   * @param {Object} jsonData - JSON object mapping substance names to GWP values.
   */
  constructor(jsonData) {
    const self = this;
    self._substanceMap = new Map();

    // Initialize the map with normalized keys
    for (const [name, gwp] of Object.entries(jsonData)) {
      const normalizedKey = self._getSubstanceKey(name);
      self._substanceMap.set(normalizedKey, new KnownSubstance(name, gwp));
    }
  }

  /**
   * Gets a substance by name using flexible matching.
   *
   * @param {string} name - The substance name to look up.
   * @returns {KnownSubstance|null} The substance if found, null otherwise.
   */
  getSubstance(name) {
    const self = this;
    if (!name) {
      return null;
    }

    const normalizedKey = self._getSubstanceKey(name.trim());
    return self._substanceMap.get(normalizedKey) || null;
  }

  /**
   * Normalizes substance names for flexible matching.
   * Converts to lowercase and removes whitespace and punctuation including hyphens and colons.
   *
   * @param {string} name - The substance name to normalize.
   * @returns {string} The normalized key for lookup.
   * @private
   */
  _getSubstanceKey(name) {
    const self = this;
    return name.toLowerCase().replace(/[\s\-_\.,:\(\)\[\]]/g, "");
  }
}

/**
 * Presenter for GWP lookup functionality in the UI.
 */
class GwpLookupPresenter {
  /**
   * Creates a new GwpLookupPresenter.
   *
   * @param {HTMLElement} lookupLink - The lookup link element.
   * @param {HTMLElement} substanceInput - The substance name input element.
   * @param {HTMLElement} ghgInput - The GHG value input element.
   * @param {HTMLElement} ghgUnitsInput - The GHG units select element.
   * @param {string} jsonPath - Path to the JSON data file.
   */
  constructor(lookupLink, substanceInput, ghgInput, ghgUnitsInput, jsonPath) {
    const self = this;
    self._lookupLink = lookupLink;
    self._substanceInput = substanceInput;
    self._ghgInput = ghgInput;
    self._ghgUnitsInput = ghgUnitsInput;
    self._jsonPath = jsonPath;
    self._libraryKeeper = null;

    // Set up click handler
    self._lookupLink.addEventListener("click", (event) => {
      self._onLookupClick(event);
    });
  }

  /**
   * Gets a substance by name (public interface for testing).
   *
   * @param {string} name - The substance name to look up.
   * @returns {KnownSubstance|null} The substance if found and library is loaded.
   */
  getSubstance(name) {
    const self = this;
    return self._libraryKeeper ? self._libraryKeeper.getSubstance(name) : null;
  }

  /**
   * Handles lookup link clicks.
   *
   * @param {Event} event - The click event.
   * @private
   */
  async _onLookupClick(event) {
    const self = this;
    event.preventDefault();

    try {
      const substanceName = self._substanceInput.value.trim();

      if (!substanceName) {
        self._showAlert("Please enter a substance name before looking up GWP values.");
        return Promise.resolve();
      }

      // Load library if not already loaded
      if (!self._libraryKeeper) {
        await self._loadLibrary();
      }

      const substance = self._libraryKeeper.getSubstance(substanceName);

      if (substance) {
        // Update the GHG input with the found value
        const gwpValue = substance.getGwp();
        self._ghgInput.value = isNaN(gwpValue) ? "" : gwpValue.toString();

        // Ensure units are set to kgCO2e / kg (which matches our data)
        self._ghgUnitsInput.value = "kgCO2e / kg";

        self._showAlert(
          `Found GWP value for ${substance.getName()}: ${substance.getGwp()} kgCO2e/kg. ` +
          "Please confirm this value is correct and current for your specific simulation.",
        );
      } else {
        self._showAlert(
          `No GWP value found for '${substanceName}'. Please enter the value manually ` +
          "or check the substance name spelling.",
        );
      }
    } catch (error) {
      console.error("Error during GWP lookup:", error);
      self._showAlert(
        "Error loading substance database. Please try again or enter the value manually.",
      );
    }
  }

  /**
   * Loads the substance library from JSON data.
   *
   * @returns {Promise<void>} Promise that resolves when library is loaded.
   * @private
   */
  async _loadLibrary() {
    const self = this;
    const response = await fetch(self._jsonPath);
    if (!response.ok) {
      throw new Error(`Failed to load substance data: ${response.status}`);
    }

    const jsonData = await response.json();
    self._libraryKeeper = new SubstanceLibraryKeeper(jsonData);
  }

  /**
   * Shows an alert message to the user.
   *
   * @param {string} message - The message to display.
   * @private
   */
  _showAlert(message) {
    const self = this;
    alert(message);
  }
}

export {KnownSubstance, SubstanceLibraryKeeper, GwpLookupPresenter};
