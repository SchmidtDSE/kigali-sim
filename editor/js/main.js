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
import {RunningIndicatorPresenter, ButtonPanelPresenter, TooltipPresenter} from "editor_actions";
import {
  IntroductionPresenter,
  PrivacyConfirmationPresenter,
  AIAssistantPresenter,
  AIDesignerPresenter,
} from "informational";

const HELP_TEXT = "Would you like our help in resolving this issue?";
const WHITESPACE_REGEX = new RegExp("^\\s*$");
const NEW_FILE_MSG = [
  "Starting a new file will clear your current work.",
  "Do you want to to continue?",
].join(" ");

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
 * Main presenter class that coordinates the application's functionality.
 */
class MainPresenter {
  /**
   * Creates a new MainPresenter instance and initializes the application.
   */
  constructor() {
    const self = this;

    self._hasCompilationErrors = false;

    self._runningIndicatorPresenter = new RunningIndicatorPresenter();

    self._initStorageKeeper();
    self._initWasmBackend();
    self._initCodeEditor();

    self._runInitialSource(self._localStorageKeeper.getSource());

    self._onCodeChange();
    self._setupFileButtons();
    self._setupStorageControls();
    self._setupForceUpdateButton();

    self._tooltipPresenter = new TooltipPresenter(self._localStorageKeeper);
    self._tooltipPresenter.initialize();

    self._updateUtil = new UpdateUtil();
    self._checkForUpdates();

    self._setupGlobalErrorRecovery();
  }

  /**
   * Initialize the storage keeper based on save preferences checkbox.
   *
   * @private
   */
  _initStorageKeeper() {
    const self = this;
    const savePreferencesCheckbox = document.getElementById("save-preferences-checkbox");
    const shouldSave = savePreferencesCheckbox.checked;
    self._localStorageKeeper = shouldSave ? new LocalStorageKeeper() : new EphemeralStorageKeeper();
  }

  /**
   * Initialize the WASM backend for worker-based execution.
   *
   * @private
   */
  _initWasmBackend() {
    const self = this;
    const progressCallback = (progress) => {
      const percentage = Math.round(progress * 100);
      self._runningIndicatorPresenter.updateProgress(percentage);
    };

    self._wasmLayer = new WasmLayer(progressCallback);
    self._wasmBackend = new WasmBackend(self._wasmLayer, progressCallback);
  }

  /**
   * Initialize the code editor and related UI components.
   *
   * @private
   */
  _initCodeEditor() {
    const self = this;

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
  }

  /**
   * Run the initial source code if provided.
   *
   * @param {string} source - The initial source code to run
   * @private
   */
  _runInitialSource(source) {
    const self = this;
    if (source) {
      self._codeEditorPresenter.setCode(source);
      const results = self._getCodeAsObj();
      if (results.getErrors().length > 0 || !results.getProgram().getIsCompatible()) {
        self._uiEditorPresenter.showCode();
      } else {
        self._uiEditorPresenter.forceCodeObj(results.getProgram());
      }
    }
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

    const encodedValue = "data:text/qubectalk;charset=utf-8," + encodeURIComponent(code);
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
        self._runningIndicatorPresenter.show();

        try {
          const programResult = await self._wasmBackend.execute(code);

          self._runningIndicatorPresenter.hide();

          if (programResult.getParsedResults().length === 0) {
            self._showNoResultsMessage();
          } else {
            if (resetFilters) {
              self._resultsPresenter.resetFilter();
            }
            self._onResult(programResult);
          }

          if (isAutoRefresh) {
            self._codeEditorPresenter.hideError();
          }
        } catch (e) {
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
        // Wait for service worker to cache new files before offering reload.
        // Also avoids access issue with disorientation.
        setTimeout(() => {
          // Pass a save callback to ensure current work is saved before reload
          self._updateUtil.showUpdateDialog(() => {
            const currentCode = self._codeEditorPresenter.getCode();
            self._localStorageKeeper.setSource(currentCode);
          });
        }, 500);
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

      // Only run simulation if code is not empty or whitespace
      if (!WHITESPACE_REGEX.test(code)) {
        self._onBuild(true, resetFilters, false);
      }
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
    saveButton.href = "data:text/qubectalk;charset=utf-8," + encodeURIComponent("");
  }

  /**
   * Sets up the force update button event handler.
   * @private
   */
  _setupForceUpdateButton() {
    const self = this;
    const forceUpdateButton = document.getElementById("force-update-button");

    forceUpdateButton.addEventListener("click", async (event) => {
      event.preventDefault();

      try {
        const updateAvailable = await self._updateUtil.checkForUpdates();

        // Pass save callback and up-to-date status
        await self._updateUtil.showUpdateDialog(
          () => {
            const currentCode = self._codeEditorPresenter.getCode();
            self._localStorageKeeper.setSource(currentCode);
          },
          !updateAvailable,
        );
      } catch (error) {
        // Show dialog anyway on error, assuming not up to date
        console.log("Error checking for updates:", error);
        await self._updateUtil.showUpdateDialog(
          () => {
            const currentCode = self._codeEditorPresenter.getCode();
            self._localStorageKeeper.setSource(currentCode);
          },
          false,
        );
      }
    });
  }

  /**
   * Sets up global error recovery mechanism for visualization errors.
   * This is a backstop and not expected during normal operation.
   * @private
   */
  _setupGlobalErrorRecovery() {
    const self = this;

    // Create global recovery function accessible from anywhere in the app
    if (!window.kigaliApp) {
      window.kigaliApp = {};
    }

    window.kigaliApp.resetVisualizationState = () => {
      try {
        console.info("Resetting visualization state due to metric strategy error");

        self._showUserNotification(
          "Visualization settings have been reset to resolve a display issue. " +
          "Your simulation data is safe.",
          "info",
        );

        if (self._resultsPresenter) {
          self._resultsPresenter.resetFilter();
        }
      } catch (error) {
        console.error("Error during visualization state reset:", error);
        alert("Visualization has been reset due to a display issue. Your data is safe.");
      }
    };
  }

  /**
   * Show user-friendly notification message.
   * @param {string} message - The message to display to the user
   * @param {string} type - The type of notification ('info', 'warning', 'error')
   * @private
   */
  _showUserNotification(message, type = "info") {
    const self = this;

    if (type === "error" && self._isOnCodeEditorTab()) {
      self._codeEditorPresenter.showError(message);
      return;
    }

    console.log(`[${type.toUpperCase()}] ${message}`);

    if (type === "error" || type === "warning") {
      alert(message);
    }
  }
}

/**
 * Main entry point for the application.
 */
function main() {
  const onLoad = () => {
    const mainPresenter = new MainPresenter();
    const privacyPresenter = new PrivacyConfirmationPresenter();
    const aiAssistantPresenter = new AIAssistantPresenter();
    const aiDesignerPresenter = new AIDesignerPresenter();
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

export {main, MainPresenter};
