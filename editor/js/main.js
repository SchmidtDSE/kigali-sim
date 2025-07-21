/**
 * Entrypoint into the tool.
 *
 * @license BSD, see LICENSE.md.
 */

import {CodeEditorPresenter} from "code_editor";
import {LocalStorageKeeper} from "local_storage_keeper";
import {EphemeralStorageKeeper} from "storage_keeper";
import {ReportDataWrapper} from "report_data";
import {ResultsPresenter} from "results";
import {UiEditorPresenter} from "ui_editor";
import {UiTranslatorCompiler} from "ui_translator";
import {UpdateUtil} from "updates";
import {WasmBackend, WasmLayer, BackendResult} from "wasm_backend";

const INCOGNITO_MESSAGE = [
  "This will clear your preferences and prior simulations upon closing this tab. Your preferences",
  "and actions during this session will also be deleted when you close the tab. Do you want to",
  "continue?",
].join(" ");
const CLEAR_ALL_DATA_MESSAGE = [
  "This will clear all saved preferences and the current model you are working on in the designer",
  "and editor. Do you want to continue?",
].join(" ");

/**
 * Manages tooltip display and user preferences for help tooltips.
 *
 * Handles initialization, user preference storage, and tooltip lifecycle
 * management using Tippy.js for accessible tooltip display.
 */
class TooltipPresenter {
  /**
   * Create a new tooltip presenter.
   *
   * @param {LocalStorageKeeper} storageKeeper - Storage manager for user preferences
   */
  constructor(storageKeeper) {
    const self = this;
    self._storageKeeper = storageKeeper;
    self._tooltipInstances = new Map();
    self._enabled = self._loadPreference();
    self._welcomeCheckbox = null;
    self._footerButton = null;
  }

  /**
   * Initialize the tooltip system.
   *
   * Sets up preference controls and creates tooltips if enabled.
   */
  initialize() {
    const self = this;
    self._setupPreferenceControls();
    if (self._enabled) {
      self._createTooltips();
    }
  }

  /**
   * Toggle tooltip visibility.
   *
   * @param {boolean} enabled - Whether tooltips should be shown
   */
  setEnabled(enabled) {
    const self = this;
    self._enabled = enabled;
    self._storageKeeper.setShowTooltips(enabled);

    if (enabled) {
      self._createTooltips();
    } else {
      self._destroyTooltips();
    }

    self._updateControls();
  }

  /**
   * Get current tooltip enabled state.
   *
   * @returns {boolean} Whether tooltips are currently enabled
   */
  isEnabled() {
    const self = this;
    return self._enabled;
  }

  /**
   * Load tooltip preference from storage.
   *
   * @returns {boolean} Tooltip preference (defaults to true)
   * @private
   */
  _loadPreference() {
    const self = this;
    const stored = self._storageKeeper.getShowTooltips();
    return stored !== null ? stored : true; // Default to enabled
  }

  /**
   * Setup preference control elements and event handlers.
   *
   * @private
   */
  _setupPreferenceControls() {
    const self = this;

    // Welcome screen checkbox
    self._welcomeCheckbox = document.getElementById("tooltip-preference-check");
    self._welcomeCheckbox.checked = self._enabled;
    self._welcomeCheckbox.addEventListener("change", function (event) {
      self.setEnabled(event.target.checked);
    });

    // Footer toggle button
    self._footerButton = document.getElementById("tooltip-toggle-button");
    self._updateControls();
    self._footerButton.addEventListener("click", function (event) {
      event.preventDefault();
      self.setEnabled(!self._enabled);
    });
  }

  /**
   * Update control elements to reflect current state.
   *
   * @private
   */
  _updateControls() {
    const self = this;

    self._welcomeCheckbox.checked = self._enabled;
    self._footerButton.textContent = self._enabled ?
      "Disable Help Tooltips" : "Enable Help Tooltips";
  }

  /**
   * Create tooltips for all target elements.
   *
   * @private
   */
  _createTooltips() {
    const self = this;

    // Initialize tooltips on all elements with data-tippy-content
    const elements = document.querySelectorAll("[data-tippy-content]");
    elements.forEach(function (element) {
      const instance = tippy(element);
      self._tooltipInstances.set(element, instance);
    });
  }

  /**
   * Destroy all tooltip instances.
   *
   * @private
   */
  _destroyTooltips() {
    const self = this;

    self._tooltipInstances.forEach(function (instance) {
      instance.destroy();
    });
    self._tooltipInstances.clear();
  }
}

/**
 * Manages the running indicator and progress bar display.
 */
class RunningIndicatorPresenter {
  constructor() {
    const self = this;
    self._runningIndicator = document.getElementById("running-indicator");
    self._progressBar = document.getElementById("simulation-progress");
    self._resultsSection = document.getElementById("results");
  }

  /**
   * Show the running indicator with progress at 0%.
   */
  show() {
    const self = this;
    self.reset();
    self._resultsSection.style.display = "block";
    self._runningIndicator.style.display = "block";
  }

  /**
   * Hide the running indicator.
   */
  hide() {
    const self = this;
    self._runningIndicator.style.display = "none";
  }

  /**
   * Update the progress bar.
   * @param {number} percentage - Progress percentage (0-100)
   */
  updateProgress(percentage) {
    const self = this;
    self._progressBar.value = percentage;
  }

  /**
   * Reset progress to 0%.
   */
  reset() {
    const self = this;
    self.updateProgress(0);
  }
}

const HELP_TEXT = "Would you like our help in resolving this issue?";

const WHITESPACE_REGEX = new RegExp("^\\s*$");
const NEW_FILE_MSG = [
  "Starting a new file will clear your current work.",
  "Do you want to to continue?",
].join(" ");

/**
 * Presenter controlling the main simluation buttons.
 *
 * Presenter which controls the functionality of the script and run button panel
 * in the UI which allow for basic tool functionality (switching authoring modes
 * and running the simulation).
 */
class ButtonPanelPresenter {
  /**
   * Creates a new ButtonPanelPresenter instance.
   *
   * @param {HTMLElement} root - The root element containing the button panel.
   * @param {Function} onBuild - Callback function triggered when build/run is initiated.
   */
  constructor(root, onBuild) {
    const self = this;
    self._root = root;

    self._availableDisplay = self._root.querySelector("#available-panel");
    self._autorunDisplay = self._root.querySelector("#auto-run-panel");
    self._loadingDisplay = self._root.querySelector("#loading");
    self._runButton = self._root.querySelector("#run-button");

    self._onBuild = onBuild;
    self._runButton.addEventListener("click", (run) => {
      self._onBuild(run);
    });

    self.enable();
  }

  /**
   * Enables the button panel and shows available options.
   */
  enable() {
    const self = this;
    self._availableDisplay.style.display = "block";
    self._autorunDisplay.style.display = "block";
    self._loadingDisplay.style.display = "none";
  }

  /**
   * Disables the button panel and shows loading state.
   */
  disable() {
    const self = this;
    self._availableDisplay.style.display = "none";
    self._autorunDisplay.style.display = "none";
    self._loadingDisplay.style.display = "block";
  }

  /**
   * Hides script-related buttons.
   */
  hideScriptButtons() {
    const self = this;
    self._runButton.style.display = "none";
  }

  /**
   * Shows script-related buttons.
   */
  showScriptButtons() {
    const self = this;
    self._runButton.style.display = "inline-block";
  }
}

/**
 * Main presenter class that coordinates the application's functionality.
 */
class MainPresenter {
  /**
   * Creates a new MainPresenter instance and initializes the application.
   */
  constructor() {
    const self = this;

    self._hasCompilationErrors = false;

    // Initialize the running indicator presenter
    self._runningIndicatorPresenter = new RunningIndicatorPresenter();

    // Initialize the storage keeper based on save preferences checkbox
    const savePreferencesCheckbox = document.getElementById("save-preferences-checkbox");
    const shouldSave = savePreferencesCheckbox.checked;
    self._localStorageKeeper = shouldSave ? new LocalStorageKeeper() : new EphemeralStorageKeeper();
    // Create progress callback
    const progressCallback = (progress) => {
      const percentage = Math.round(progress * 100);
      self._runningIndicatorPresenter.updateProgress(percentage);
    };

    // Initialize the WASM backend for worker-based execution
    self._wasmLayer = new WasmLayer(progressCallback);
    self._wasmBackend = new WasmBackend(self._wasmLayer, progressCallback);

    self._codeEditorPresenter = new CodeEditorPresenter(
      document.getElementById("code-editor"),
      () => self._onCodeChange(),
      () => self._onAutoRefresh(),
    );
    self._buttonPanelPresenter = new ButtonPanelPresenter(
      document.getElementById("code-buttons-panel"),
      () => self._onBuild(true, false, false),
    );
    self._resultsPresenter = new ResultsPresenter(document.getElementById("results"));

    self._uiEditorPresenter = new UiEditorPresenter(
      false,
      document.getElementById("editor-tabs"),
      document.getElementById("ui-editor-pane"),
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
      () => self._codeEditorPresenter.forceUpdate(),
    );

    const source = self._localStorageKeeper.getSource();
    if (source) {
      self._codeEditorPresenter.setCode(source);
      const results = self._getCodeAsObj();
      if (results.getErrors().length > 0 || !results.getProgram().getIsCompatible()) {
        self._uiEditorPresenter.showCode();
      } else {
        self._uiEditorPresenter.forceCodeObj(results.getProgram());
      }
    }

    self._onCodeChange();
    self._setupFileButtons();
    self._setupStorageControls();

    // Initialize tooltip presenter for help tooltips
    self._tooltipPresenter = new TooltipPresenter(self._localStorageKeeper);
    self._tooltipPresenter.initialize();

    // Initialize update utility and check for updates (fails silently if offline)
    self._updateUtil = new UpdateUtil();
    self._checkForUpdates();
  }

  /**
   * Handles code change events and updates the UI accordingly.
   *
   * @private
   */
  _onCodeChange() {
    const self = this;
    const code = self._codeEditorPresenter.getCode();
    if (WHITESPACE_REGEX.test(code)) {
      self._buttonPanelPresenter.hideScriptButtons();
      self._uiEditorPresenter.refresh(null);
    } else {
      self._buttonPanelPresenter.showScriptButtons();
      self._onBuild(false, false, false);
    }
    self._localStorageKeeper.setSource(code);

    const encodedValue = encodeURI("data:text/qubectalk;charset=utf-8," + code);
    const saveButton = document.getElementById("save-file-button");
    saveButton.href = encodedValue;
  }

  /**
   * Handles automatic refresh after 3 seconds of no code changes.
   * Only runs the simulation if there are no compilation errors.
   *
   * @private
   */
  _onAutoRefresh() {
    const self = this;
    if (!self._hasCompilationErrors && self._shouldAutoRun()) {
      self._onBuild(true, false, true);
    }
  }

  /**
   * Checks if auto-run should be executed based on tab and checkbox state.
   *
   * @returns {boolean} True if auto-run should execute, false otherwise.
   * @private
   */
  _shouldAutoRun() {
    const self = this;
    return self._isOnCodeEditorTab() && self._isAutoRunEnabled();
  }

  /**
   * Checks if the user is currently on the code editor tab.
   *
   * @returns {boolean} True if on code editor tab, false otherwise.
   * @private
   */
  _isOnCodeEditorTab() {
    const codeEditorPane = document.getElementById("code-editor-pane");
    return codeEditorPane && codeEditorPane.getAttribute("hidden") !== "hidden";
  }

  /**
   * Checks if the auto-run checkbox is checked.
   *
   * @returns {boolean} True if auto-run is enabled, false otherwise.
   * @private
   */
  _isAutoRunEnabled() {
    const autoRunCheck = document.getElementById("auto-run-check");
    return autoRunCheck && autoRunCheck.checked;
  }

  /**
   * Shows a message indicating no results were produced.
   *
   * @private
   */
  _showNoResultsMessage() {
    const self = this;
    const resultsSection = document.getElementById("results");
    resultsSection.style.display = "block";

    // Show the pre-existing no-results message
    const noResultsMessage = document.getElementById("no-results-message");
    if (noResultsMessage) {
      noResultsMessage.style.display = "block";
    }
  }


  /**
   * Shows the error indicator overlay in the results section.
   * This displays an error message when simulations fail.
   *
   * @private
   */
  _showErrorIndicator() {
    const self = this;
    const resultsSection = document.getElementById("results");
    const errorIndicator = document.getElementById("error-indicator");
    const runningIndicator = document.getElementById("running-indicator");

    if (resultsSection && errorIndicator) {
      resultsSection.style.display = "block";
      if (runningIndicator) {
        runningIndicator.style.display = "none";
      }
      errorIndicator.style.display = "block";
    }
  }

  /**
   * Hides the error indicator overlay in the results section.
   *
   * @private
   */
  _hideErrorIndicator() {
    const self = this;
    const errorIndicator = document.getElementById("error-indicator");

    if (errorIndicator) {
      errorIndicator.style.display = "none";
    }
  }

  /**
   * Handles build/run events and compiles/executes the code.
   *
   * @param {boolean} run - Flag indicating if to execute the code after
   *     compilation.
   * @param {boolean} resetFilters - Flag indicating if to reset the results
   *     UI filter values. Defaults to false if not given.
   * @param {boolean} isAutoRefresh - Flag indicating if this is triggered by
   *     auto-refresh. Defaults to false if not given.
   * @private
   */
  _onBuild(run, resetFilters, isAutoRefresh) {
    const self = this;
    self._buttonPanelPresenter.disable();

    if (resetFilters === undefined) {
      resetFilters = false;
    }

    if (isAutoRefresh === undefined) {
      isAutoRefresh = false;
    }

    const execute = async () => {
      const code = self._codeEditorPresenter.getCode();

      // First, validate syntax using the UI translator compiler (for UI feedback)
      const compiler = new UiTranslatorCompiler();
      const validationResult = compiler.compile(code);

      const compileErrors = validationResult.getErrors();
      const hasErrors = compileErrors.length > 0;
      self._hasCompilationErrors = hasErrors;

      if (hasErrors) {
        self._codeEditorPresenter.showError(compileErrors[0]);
        self._buttonPanelPresenter.enable();
        return;
      } else {
        self._codeEditorPresenter.hideError();
      }

      if (run) {
        // Show the running indicator when simulation starts
        self._runningIndicatorPresenter.show();

        try {
          // Execute using the WASM backend worker
          const programResult = await self._wasmBackend.execute(code);

          // Hide the running indicator when execution completes
          self._runningIndicatorPresenter.hide();

          if (programResult.getParsedResults().length === 0) {
            self._showNoResultsMessage();
          } else {
            if (resetFilters) {
              self._resultsPresenter.resetFilter();
            }
            self._onResult(programResult);
          }

          // Clear any previous runtime errors when execution succeeds during auto-refresh
          if (isAutoRefresh) {
            self._codeEditorPresenter.hideError();
          }
        } catch (e) {
          // Show error indicator on simulation failure
          self._showErrorIndicator();

          console.log(e);
          const message = "Execution error: " + e.message;
          if (!isAutoRefresh) {
            alertWithHelpOption(message);
          } else {
            self._codeEditorPresenter.showError(message);
          }
          captureSentryMessage(message, "error");
        }
      }
    };

    const executeSafe = async () => {
      try {
        await execute();
      } catch (e) {
        const message = "Execute error: " + e;
        if (!isAutoRefresh) {
          alertWithHelpOption(message);
        } else {
          self._codeEditorPresenter.showError(message);
        }
        captureSentryMessage(message, "error");
      }
      self._buttonPanelPresenter.enable();
    };

    setTimeout(executeSafe, 50);

    const codeObjResults = self._getCodeAsObj();
    if (codeObjResults.getErrors() == 0) {
      const codeObj = codeObjResults.getProgram();

      if (self._uiEditorPresenter !== null) {
        self._uiEditorPresenter.refresh(codeObj);
      }
    }
  }

  /**
   * Handles program execution results and displays them.
   *
   * @param {BackendResult} backendResult - The backend result containing CSV and parsed data.
   * @private
   */
  _onResult(backendResult) {
    const self = this;

    // Hide any existing no-results message and error indicator
    const noResultsMessage = document.getElementById("no-results-message");
    if (noResultsMessage) {
      noResultsMessage.style.display = "none";
    }
    self._hideErrorIndicator();

    const resultsWrapped = new ReportDataWrapper(backendResult.getParsedResults());
    self._resultsPresenter.showResults(resultsWrapped, backendResult);
  }

  /**
   * Gets the code as an object representation.
   *
   * @param {string} [overrideCode] - Optional code to use instead of editor content.
   * @returns {Object} The compiled code object.
   * @private
   */
  _getCodeAsObj(overrideCode) {
    const self = this;
    const code = overrideCode === undefined ? self._codeEditorPresenter.getCode() : overrideCode;
    const compiler = new UiTranslatorCompiler();
    const result = compiler.compile(code);
    return result;
  }

  /**
   * Handles code object updates and refreshes the UI.
   *
   * @param {Object} codeObj - The updated code object.
   * @private
   */
  _onCodeObjUpdate(codeObj) {
    const self = this;
    const newCode = codeObj.toCode(0);
    self._codeEditorPresenter.setCode(newCode);

    if (self._uiEditorPresenter !== null) {
      self._uiEditorPresenter.refresh(codeObj);
    }

    if (codeObj.getScenarios() == 0) {
      self._resultsPresenter.hide();
      return;
    }

    self._onBuild(true, false, false);
  }

  /**
   * Checks for application updates and shows dialog if available.
   *
   * This method fails silently on all errors to support offline usage.
   * Only checks for updates in WASM builds, not during engine development.
   *
   * @private
   */
  async _checkForUpdates() {
    const self = this;
    try {
      const updateAvailable = await self._updateUtil.checkForUpdates();
      if (updateAvailable) {
        await self._updateUtil.showUpdateDialog();
      }
    } catch (error) {
      // Fail silently - likely due to offline
      console.debug("Update check did not respond, possibly offline:", error);
    }
  }

  /**
   * Sets up file-related button handlers.
   *
   * @private
   */
  _setupFileButtons() {
    const self = this;

    const loadFileDialog = document.getElementById("load-file-dialog");

    const setCode = (code, resetFilters) => {
      self._codeEditorPresenter.setCode(code);
      self._onCodeChange();
      self._onBuild(true, resetFilters, false);
    };

    const newFileDialog = document.getElementById("new-file-button");
    newFileDialog.addEventListener("click", (event) => {
      event.preventDefault();
      if (confirm(NEW_FILE_MSG)) {
        setCode("");
      }
    });

    const loadFileButton = document.getElementById("load-file-button");
    loadFileButton.addEventListener("click", (event) => {
      event.preventDefault();
      loadFileDialog.showModal();
    });

    const cancelButton = loadFileDialog.querySelector(".cancel-button");
    cancelButton.addEventListener("click", (event) => {
      event.preventDefault();
      loadFileDialog.close();
    });

    const loadButton = loadFileDialog.querySelector(".load-button");
    loadButton.addEventListener("click", (event) => {
      event.preventDefault();

      const file = loadFileDialog.querySelector(".upload-file").files[0];
      if (file) {
        const reader = new FileReader();
        reader.readAsText(file, "UTF-8");
        reader.onload = (event) => {
          const newCode = event.target.result;
          setCode(newCode, true);
          self._uiEditorPresenter.enableAllSections();
          loadFileDialog.close();
        };
      }
    });
  }

  /**
   * Sets up storage control event handlers for checkbox and clear button.
   * @private
   */
  _setupStorageControls() {
    const self = this;

    // Set up the save preferences checkbox
    const savePreferencesCheckbox = document.getElementById("save-preferences-checkbox");
    savePreferencesCheckbox.addEventListener("change", (event) => {
      self._handleSavePreferencesChange(event.target.checked);
    });

    // Set up the clear data button
    const clearDataButton = document.getElementById("clear-data-button");
    clearDataButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._handleClearDataClick();
    });
  }

  /**
   * Handles changes to the save preferences checkbox.
   * @param {boolean} savePreferences - Whether to save preferences
   * @private
   */
  _handleSavePreferencesChange(savePreferences) {
    const self = this;

    // Get current code before switching storage keepers
    const currentCode = self._codeEditorPresenter.getCode();

    // Switch to appropriate storage keeper
    if (savePreferences) {
      self._localStorageKeeper = new LocalStorageKeeper();
      // Save current code to new storage keeper
      if (currentCode) {
        self._localStorageKeeper.setSource(currentCode);
      }
    } else {
      // Ask for confirmation before clearing data
      const confirmed = window.confirm(INCOGNITO_MESSAGE);
      if (confirmed) {
        // Clear the current storage keeper before switching
        self._localStorageKeeper.clear();
        self._localStorageKeeper = new EphemeralStorageKeeper();
      } else {
        // Re-check the checkbox if user cancels
        const savePreferencesCheckbox = document.getElementById("save-preferences-checkbox");
        savePreferencesCheckbox.checked = true;
      }
    }
  }

  /**
   * Handles the clear data button click with confirmation.
   * @private
   */
  _handleClearDataClick() {
    const self = this;

    const confirmed = window.confirm(CLEAR_ALL_DATA_MESSAGE);

    if (confirmed) {
      self._localStorageKeeper.clear();
      self._resetApplicationState();
      window.location.reload();
    }
  }

  /**
   * Resets application state after clearing data.
   * @private
   */
  _resetApplicationState() {
    const self = this;

    // Clear the code editor
    self._codeEditorPresenter.setCode("");

    // Reset UI editor
    self._uiEditorPresenter.refresh(null);

    // Clear any results
    self._resultsPresenter.clear();

    // Hide results section
    const resultsSection = document.getElementById("results");
    resultsSection.style.display = "none";

    // Update file save button
    const saveButton = document.getElementById("save-file-button");
    saveButton.href = "data:text/qubectalk;charset=utf-8,";
  }
}

/**
 * Presenter for managing the privacy confirmation checkbox and dialog.
 */
class PrivacyConfirmationPresenter {
  constructor() {
    const self = this;
    self._checkbox = document.getElementById("privacy-confirmation-check");
    self._dialog = document.getElementById("privacy-confirmation-dialog");
    self._closeButton = self._dialog.querySelector(".close-button");

    self._setupEventListeners();
  }

  /**
   * Set up event listeners for checkbox and dialog interactions.
   */
  _setupEventListeners() {
    const self = this;

    // Listen for checkbox changes
    self._checkbox.addEventListener("change", (event) => {
      if (!event.target.checked) {
        self._showDialog();
      }
    });

    // Listen for dialog close button
    self._closeButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._hideDialog();
    });

    // Listen for dialog close via ESC key or backdrop click
    self._dialog.addEventListener("close", () => {
      self._onDialogClose();
    });
  }

  /**
   * Show the privacy confirmation dialog.
   */
  _showDialog() {
    const self = this;
    self._dialog.showModal();
  }

  /**
   * Hide the privacy confirmation dialog.
   */
  _hideDialog() {
    const self = this;
    self._dialog.close();
  }

  /**
   * Handle dialog close event - re-check the checkbox.
   */
  _onDialogClose() {
    const self = this;
    self._checkbox.checked = true;
  }
}

/**
 * Presenter for managing the introduction sequence.
 */
class IntroductionPresenter {
  constructor(localStorageKeeper) {
    const self = this;
    self._localStorageKeeper = localStorageKeeper;
    self._loadingPanel = document.getElementById("loading");
    self._mainHolder = document.getElementById("main-holder");
  }

  /**
   * Initialize the introduction sequence.
   * @return {Promise} Promise that resolves when the user continues.
   */
  async initialize() {
    const self = this;
    const hideIntroduction = self._localStorageKeeper.getHideIntroduction();

    if (hideIntroduction) {
      return Promise.resolve();
    }

    return new Promise((resolve) => {
      self._setupIntroductionUI(resolve);
    });
  }

  /**
   * Set up the introduction UI with buttons.
   * @param {Function} resolve - Callback to resolve the promise when user continues.
   */
  _setupIntroductionUI(resolve) {
    const self = this;
    const loadingIndicator = document.getElementById("initial-loading-indicator");
    const buttonPanel = document.getElementById("continue-buttons-panel");
    const continueButton = document.getElementById("continue-button");
    const dontShowAgainButton = document.getElementById("continue-no-show-button");

    continueButton.onclick = (e) => {
      e.preventDefault();
      loadingIndicator.style.display = "block";
      buttonPanel.style.display = "none";
      resolve();
    };

    dontShowAgainButton.onclick = (e) => {
      e.preventDefault();
      self._localStorageKeeper.setHideIntroduction(true);
      loadingIndicator.style.display = "block";
      buttonPanel.style.display = "none";
      resolve();
    };

    loadingIndicator.style.display = "none";
    buttonPanel.style.display = "block";
  }

  /**
   * Show the main application content.
   */
  _showMainContent() {
    const self = this;
    self._loadingPanel.style.display = "none";
    self._mainHolder.style.display = "block";
  }
}

/**
 * Main entry point for the application.
 */
function main() {
  const onLoad = () => {
    const mainPresenter = new MainPresenter();
    const privacyPresenter = new PrivacyConfirmationPresenter();
    const introPresenter = new IntroductionPresenter(mainPresenter._localStorageKeeper);

    const showApp = async () => {
      await introPresenter.initialize();
      introPresenter._showMainContent();
    };
    setTimeout(showApp, 500);
  };

  setTimeout(onLoad, 500);
}

/**
 * Show the user an alert and offer help.
 *
 * Show the user an alert to the user with the given message as a confirm
 * dialog. HELP_TEXT will be added to the end. If the user says OK, then
 * they will be redirected to /guide/get_help.html
 *
 * @param message {string} - The message to display.
 */
function alertWithHelpOption(message) {
  if (confirm(message + " " + HELP_TEXT)) {
    window.location.href = "/guide/get_help.html";
  }
}

/**
 * Send a report of an issue to Sentry if enabled.
 */
function captureSentryMessage(message, level) {
  console.log("Sentry message not sent.", message, level);
}

export {
  main,
  IntroductionPresenter,
  RunningIndicatorPresenter,
  ButtonPanelPresenter,
  MainPresenter,
  PrivacyConfirmationPresenter,
};
