/**
 * Presenters and logic for the UI-based authoring experience.
 *
 * @license BSD, see LICENSE.md.
 */
import {Program} from "ui_translator_components";
import {MetaSerializer, MetaChangeApplier} from "meta_serialization";
import {DuplicateEntityPresenter} from "duplicate_util";
import {ApplicationsListPresenter} from "ui_editor_app";
import {SimulationListPresenter} from "ui_editor_sim";
import {
  ConsumptionListPresenter,
  PolicyListPresenter,
} from "ui_editor_action";

/**
 * Result container for active substance extraction with error reporting.
 */
class ActiveSubstancesResult {
  /**
   * Creates a new ActiveSubstancesResult.
   *
   * @param {SubstanceMetadata[]} substances - Array of substance metadata
   * @param {string|null} error - Error message or null if no error
   */
  constructor(substances, error = null) {
    this._substances = substances || [];
    this._error = error;
  }

  /**
   * Gets the array of substance metadata.
   *
   * @returns {SubstanceMetadata[]} Array of substance metadata
   */
  getSubstances() {
    return this._substances;
  }

  /**
   * Gets the error message if any.
   *
   * @returns {string|null} Error message or null if no error
   */
  getError() {
    return this._error;
  }

  /**
   * Checks if there was an error.
   *
   * @returns {boolean} True if there was an error
   */
  hasError() {
    return this._error !== null;
  }
}

/**
 * Manages substance table download/upload functionality.
 *
 * This presenter handles the substances table dialog which allows users to
 * download CSV files containing substance metadata and upload modifications.
 * Uses MetaSerializer to convert substances to/from CSV format.
 */
class SubstanceTablePresenter {
  /**
   * Creates a new SubstanceTablePresenter.
   *
   * @param {Function} getCodeObj - Callback to get the current code object.
   * @param {Function} onCodeObjUpdate - Callback when code object is updated.
   */
  constructor(getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._dialog = document.getElementById("substances-table-dialog");
    self._metaSerializer = new MetaSerializer();
    self._setupDialog();
  }

  /**
   * Refreshes the substance count and download data.
   *
   * @param {Object} codeObj - Current code object.
   */
  refresh(codeObj) {
    const self = this;
    const result = self._getActiveSubstances(codeObj);
    self._updateSubstanceCount(result.getSubstances().length);
    self._updateDownloadButton(result.getSubstances(), result.getError());
  }

  /**
   * Sets up dialog event handlers.
   *
   * @private
   */
  _setupDialog() {
    const self = this;

    // Link to open dialog
    const tableLink = document.querySelector(".substances-table-link");
    tableLink.addEventListener("click", (event) => {
      event.preventDefault();
      self.refresh(self._getCodeObj()); // Refresh content
      self._dialog.showModal();
    });

    // Close button
    const closeButton = self._dialog.querySelector(".close-button");
    closeButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._dialog.close();
    });

    // Upload button - shows upload pane
    const uploadButton = self._dialog.querySelector(".upload-button");
    uploadButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._showUploadPane();
    });

    // Upload confirm button - processes uploaded CSV and closes dialog
    const uploadConfirmButton = self._dialog.querySelector(".upload-confirm-button");
    uploadConfirmButton.addEventListener("click", (event) => {
      event.preventDefault();
      const fileInput = document.getElementById("substances-upload-file");
      const file = fileInput.files[0];
      if (file) {
        self._processUploadedFile(file);
        // Clear input for next upload
        fileInput.value = "";
        self._dialog.close();
      }
      self._hideUploadPane();
    });

    // Upload cancel button - hides upload pane
    const uploadCancelButton = self._dialog.querySelector(".upload-cancel-button");
    uploadCancelButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._hideUploadPane();
    });
  }

  /**
   * Updates the substance count display.
   *
   * @param {number} count - Number of active substances
   * @private
   */
  _updateSubstanceCount(count) {
    const self = this;
    const countElement = self._dialog.querySelector(".substance-count");
    countElement.textContent = count.toString();
  }

  /**
   * Updates the download button with current CSV data.
   *
   * @param {SubstanceMetadata[]} substances - Array of substance metadata
   * @param {string|null} extractionError - Error from substance extraction
   * @private
   */
  _updateDownloadButton(substances, extractionError) {
    const self = this;
    const downloadButton = self._dialog.querySelector(".download-button");

    // Remove any existing click handler
    const newDownloadButton = downloadButton.cloneNode(true);
    downloadButton.parentNode.replaceChild(newDownloadButton, downloadButton);

    try {
      // Generate CSV data URI using MetaSerializer
      const csvUri = self._metaSerializer.renderMetaToCsvUri(substances);

      // Update download button
      newDownloadButton.href = csvUri;
      newDownloadButton.download = `substances_${new Date().toISOString().split("T")[0]}.csv`;
    } catch (error) {
      console.error("Failed to generate CSV for download:", error);

      // Fallback: show alert with error details when clicked
      newDownloadButton.href = "#";
      newDownloadButton.removeAttribute("download");
      newDownloadButton.addEventListener("click", (event) => {
        event.preventDefault();
        let errorMessage = "A download could not be generated.";
        if (extractionError) {
          errorMessage += ` Extraction error: ${extractionError}`;
        }
        if (error && error.message) {
          errorMessage += ` CSV generation error: ${error.message}`;
        }
        alert(errorMessage);
      });
    }
  }

  /**
   * Extracts all active substances from the current program.
   *
   * @param {Object} codeObj - Current code object
   * @returns {ActiveSubstancesResult} Result with substances array and error info
   * @private
   */
  _getActiveSubstances(codeObj) {
    const self = this;

    if (!codeObj) {
      return new ActiveSubstancesResult([], "Unknown internal error: no code object");
    }

    const substances = [];
    const errors = [];

    try {
      // Iterate through all applications
      const applications = codeObj.getApplications();

      for (const application of applications) {
        const appName = application.getName();
        const appSubstances = application.getSubstances();

        // Extract metadata from each substance in this application
        for (const substance of appSubstances) {
          try {
            const metadata = substance.getMeta(appName);
            substances.push(metadata);
          } catch (error) {
            const errorMsg = `Failed to extract metadata from substance ${substance.getName()} ` +
              `in ${appName}: ${error.message || error}`;
            console.warn(errorMsg);
            errors.push(errorMsg);
          }
        }
      }
    } catch (error) {
      const errorMsg = `Failed to extract substances from code object: ${error.message || error}`;
      console.error(errorMsg);
      return new ActiveSubstancesResult(substances, errorMsg);
    }

    const aggregatedError = errors.length > 0 ? errors.join("; ") : null;
    return new ActiveSubstancesResult(substances, aggregatedError);
  }

  /**
   * Processes an uploaded CSV file and applies substance updates.
   *
   * @param {File} file - The uploaded CSV file
   * @private
   */
  _processUploadedFile(file) {
    const self = this;
    const reader = new FileReader();

    reader.onload = (event) => {
      try {
        const csvContent = event.target.result;
        const parseResult = self._metaSerializer.deserializeMetaFromCsvString(csvContent);

        if (parseResult.hasErrors()) {
          if (parseResult.hasUserErrors()) {
            // Show user-friendly error messages for data issues
            self._showError("Please fix the following issues in your CSV:\n" +
              parseResult.getErrorSummary());
          }
          if (parseResult.hasSystemErrors()) {
            // Log system errors for debugging
            console.error("System errors occurred:", parseResult.getSystemErrors());
            self._showError("An unexpected error occurred while processing the CSV. " +
              "Please contact support.");
          }
          return;
        }

        if (parseResult.getUpdates().length > 0) {
          self._applyUpdatesToCodeObject(parseResult.getUpdates());
        } else {
          self._showError("No valid data found in the CSV file.");
        }
      } catch (error) {
        // Handle any remaining unexpected errors
        console.error("Unexpected error in CSV processing:", error);
        self._showError(`Failed to process CSV: ${error.message}`);
      }
    };

    reader.onerror = () => {
      self._showError("Failed to read file. Please try again.");
    };

    reader.readAsText(file, "UTF-8");
  }

  /**
   * Applies substance metadata updates to the code object.
   *
   * @param {SubstanceMetadataUpdate[]} updates - Array of substance metadata updates
   * @private
   */
  _applyUpdatesToCodeObject(updates) {
    const self = this;
    try {
      const codeObj = self._getCodeObj();
      const applier = new MetaChangeApplier(codeObj);
      applier.upsertMetadata(updates);

      // Propagate changes through system
      self._onCodeObjUpdate(codeObj);

      // Update dialog display
      self.refresh(codeObj);

      // Success feedback removed per feedback requirements
    } catch (error) {
      self._showError(`Failed to apply changes: ${error.message}`);
    }
  }

  /**
   * Shows an error message to the user.
   *
   * @param {string} message - Error message to display
   * @private
   */
  _showError(message) {
    alert(`Upload Error: ${message}`);
  }


  /**
   * Shows the upload pane and hides the dialog buttons.
   *
   * @private
   */
  _showUploadPane() {
    const self = this;
    const uploadPane = self._dialog.querySelector("#substances-upload-pane");
    const dialogButtons = self._dialog.querySelector("#substances-main-buttons");

    uploadPane.style.display = "block";
    dialogButtons.style.display = "none";
  }

  /**
   * Hides the upload pane and shows the dialog buttons.
   *
   * @private
   */
  _hideUploadPane() {
    const self = this;
    const uploadPane = self._dialog.querySelector("#substances-upload-pane");
    const dialogButtons = self._dialog.querySelector("#substances-main-buttons");

    uploadPane.style.display = "none";
    dialogButtons.style.display = "block";
  }
}

// DuplicateEntityPresenter has been moved to duplicate_util.js

/**
 * Manages the UI editor interface.
 *
 * Central presenter which coordinates between code editing and visual editing
 * interfaces, managing tabs and content display with their respective
 * sub-presenters.
 */
class UiEditorPresenter {
  /**
   * Creates a new UiEditorPresenter.
   *
   * @param {boolean} startOnCode - Whether to start in code view.
   * @param {HTMLElement} tabRoot - Root element for editor tabs.
   * @param {HTMLElement} contentsRoot - Root element for editor contents.
   * @param {Function} getCodeAsObj - Callback to get current code object.
   * @param {Function} onCodeObjUpdate - Callback when code object updates.
   * @param {Function} onTabChange - Callback when active tab changes.
   */
  constructor(startOnCode, tabRoot, contentsRoot, getCodeAsObj, onCodeObjUpdate, onTabChange) {
    const self = this;

    self._contentsSelection = contentsRoot;
    self._getCodeAsObjInner = getCodeAsObj;
    self._onCodeObjUpdateInner = onCodeObjUpdate;
    self._codeObj = null;
    self._initCodeObj();

    self._tabs = new Tabby("#" + tabRoot.id);
    tabRoot.addEventListener("tabby", () => onTabChange());

    const appEditor = self._contentsSelection.querySelector(".applications");
    self._applicationsList = new ApplicationsListPresenter(
      appEditor,
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
    );

    const consumptionEditor = self._contentsSelection.querySelector(".consumption");
    self._consumptionList = new ConsumptionListPresenter(
      consumptionEditor,
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
    );

    const policyEditor = self._contentsSelection.querySelector(".policies");
    self._policyList = new PolicyListPresenter(
      policyEditor,
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
    );

    const simulationEditor = self._contentsSelection.querySelector(".simulations");
    self._simulationList = new SimulationListPresenter(
      simulationEditor,
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
    );

    self._substanceTable = new SubstanceTablePresenter(
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
    );

    self._duplicateEntityPresenter = new DuplicateEntityPresenter(
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
    );

    self._setupAdvancedLinks();

    if (startOnCode) {
      self._tabs.toggle("#code-editor-pane");
    }
  }

  /**
   * Shows the code editor interface.
   */
  showCode() {
    const self = this;
    self._tabs.toggle("#code-editor-pane");
  }

  /**
   * Show all sections in the UI editor as enabled.
   */
  enableAllSections() {
    const self = this;
    self._consumptionList.enable();
    self._policyList.enable();
    self._simulationList.enable();
  }

  /**
   * Refreshes the UI with new code object data.
   *
   * @param {Object} codeObj - The new code object to display.
   */
  refresh(codeObj) {
    const self = this;

    if (codeObj === null) {
      self._initCodeObj();
    } else {
      self._codeObj = codeObj;
    }

    if (self._codeObj.getIsCompatible()) {
      self._applicationsList.refresh(codeObj);
      self._consumptionList.refresh(codeObj);
      self._policyList.refresh(codeObj);
      self._simulationList.refresh(codeObj);
      self._substanceTable.refresh(codeObj);
      self._enableBasicPanel();
    } else {
      self._disableBasicPanel();
    }
  }

  /**
   * Forces update with new code object.
   *
   * @param {Object} codeObj - The code object to force update with.
   */
  forceCodeObj(codeObj) {
    const self = this;
    self._onCodeObjUpdate(codeObj);
  }

  /**
   * Sets up event listeners for advanced editor links.
   *
   * @private
   */
  _setupAdvancedLinks() {
    const self = this;
    const links = Array.of(...self._contentsSelection.querySelectorAll(".advanced-editor-link"));
    links.forEach((link) =>
      link.addEventListener("click", (event) => {
        self._tabs.toggle("#code-editor-pane");
        event.preventDefault();
      }),
    );
  }

  /**
   * Gets the current code object.
   *
   * @returns {Object} The current code object.
   * @private
   */
  _getCodeAsObj() {
    const self = this;
    return self._codeObj;
  }

  /**
   * Initializes the code object from current state.
   *
   * @private
   */
  _initCodeObj() {
    const self = this;
    const result = self._getCodeAsObjInner();
    const hasErrors = result.getErrors().length > 0;
    if (hasErrors) {
      self._disableBasicPanel();
      self._codeObj = new Program([], [], [], true);
    } else if (result.getProgram() === null) {
      self._enableBasicPanel();
      const codeObj = result.getProgram();
      if (codeObj === null) {
        self._codeObj = new Program([], [], [], true);
      } else {
        self._codeObj = codeObj;
      }
    }
  }

  /**
   * Enables the basic panel interface.
   *
   * @private
   */
  _enableBasicPanel() {
    const self = this;
    self._contentsSelection.querySelector(".available-contents").style.display = "block";
    self._contentsSelection.querySelector(".not-available-contents").style.display = "none";
  }

  /**
   * Disables the basic panel interface.
   *
   * @private
   */
  _disableBasicPanel() {
    const self = this;
    self._contentsSelection.querySelector(".available-contents").style.display = "none";
    self._contentsSelection.querySelector(".not-available-contents").style.display = "block";
  }

  /**
   * Handles code object updates.
   *
   * @param {Object} codeObj - The updated code object.
   * @private
   */
  _onCodeObjUpdate(codeObj) {
    const self = this;
    self._codeObj = codeObj;

    if (self._codeObj.getApplications().length > 0) {
      self._consumptionList.enable();
    } else {
      self._consumptionList.disable();
    }

    if (self._codeObj.getSubstances().length > 0) {
      self._policyList.enable();
      self._simulationList.enable();
    } else {
      self._policyList.disable();
      self._simulationList.disable();
    }

    self._onCodeObjUpdateInner(codeObj);
  }
}


export {UiEditorPresenter};
