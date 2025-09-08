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
    this._name = name;
    this._gwp = gwp;
  }

  /**
   * Gets the substance name.
   *
   * @returns {string} The substance name.
   */
  getName() {
    return this._name;
  }

  /**
   * Gets the GWP value.
   *
   * @returns {number} The GWP value as a number.
   */
  getGwp() {
    return this._gwp;
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
    this._substanceMap = new Map();

    // Initialize the map with normalized keys
    for (const [name, gwp] of Object.entries(jsonData)) {
      const normalizedKey = this._getSubstanceKey(name);
      this._substanceMap.set(normalizedKey, new KnownSubstance(name, gwp));
    }
  }

  /**
   * Normalizes substance names for flexible matching.
   * Converts to lowercase and removes whitespace and punctuation including hyphens.
   *
   * @param {string} name - The substance name to normalize.
   * @returns {string} The normalized key for lookup.
   * @private
   */
  _getSubstanceKey(name) {
    return name.toLowerCase().replace(/[\s\-_\.,\(\)\[\]]/g, "");
  }

  /**
   * Gets a substance by name using flexible matching.
   *
   * @param {string} name - The substance name to look up.
   * @returns {KnownSubstance|null} The substance if found, null otherwise.
   */
  getSubstance(name) {
    if (!name || typeof name !== "string") {
      return null;
    }

    const normalizedKey = this._getSubstanceKey(name.trim());
    return this._substanceMap.get(normalizedKey) || null;
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
    this._lookupLink = lookupLink;
    this._substanceInput = substanceInput;
    this._ghgInput = ghgInput;
    this._ghgUnitsInput = ghgUnitsInput;
    this._jsonPath = jsonPath;
    this._libraryKeeper = null;

    // Set up click handler
    if (this._lookupLink) {
      this._lookupLink.addEventListener("click", (event) => {
        this._onLookupClick(event);
      });
    }
  }

  /**
   * Handles lookup link clicks.
   *
   * @param {Event} event - The click event.
   * @private
   */
  async _onLookupClick(event) {
    event.preventDefault();

    try {
      const substanceName = this._substanceInput ? this._substanceInput.value.trim() : "";

      if (!substanceName) {
        this._showAlert("Please enter a substance name before looking up GWP values.");
        return Promise.resolve();
      }

      // Load library if not already loaded
      if (!this._libraryKeeper) {
        await this._loadLibrary();
      }

      const substance = this._libraryKeeper.getSubstance(substanceName);

      if (substance) {
        // Update the GHG input with the found value
        if (this._ghgInput) {
          this._ghgInput.value = substance.getGwp().toString();
        }

        // Ensure units are set to kgCO2e / kg (which matches our data)
        if (this._ghgUnitsInput) {
          this._ghgUnitsInput.value = "kgCO2e / kg";
        }

        this._showAlert(
          `Found GWP value for ${substance.getName()}: ${substance.getGwp()} kgCO2e/kg. ` +
          "Please confirm this value is correct and current for your specific simulation.",
        );
      } else {
        this._showAlert(
          `No GWP value found for '${substanceName}'. Please enter the value manually ` +
          "or check the substance name spelling.",
        );
      }
    } catch (error) {
      console.error("Error during GWP lookup:", error);
      this._showAlert(
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
    const response = await fetch(this._jsonPath);
    if (!response.ok) {
      throw new Error(`Failed to load substance data: ${response.status}`);
    }

    const jsonData = await response.json();
    this._libraryKeeper = new SubstanceLibraryKeeper(jsonData);
  }

  /**
   * Shows an alert message to the user.
   *
   * @param {string} message - The message to display.
   * @private
   */
  _showAlert(message) {
    alert(message);
  }

  /**
   * Gets a substance by name (public interface for testing).
   *
   * @param {string} name - The substance name to look up.
   * @returns {KnownSubstance|null} The substance if found and library is loaded.
   */
  getSubstance(name) {
    return this._libraryKeeper ? this._libraryKeeper.getSubstance(name) : null;
  }
}

export {KnownSubstance, SubstanceLibraryKeeper, GwpLookupPresenter};
