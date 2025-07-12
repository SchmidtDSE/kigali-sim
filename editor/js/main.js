/**
 * Entrypoint into the tool.
 *
 * @license BSD, see LICENSE.md.
 */

import {CodeEditorPresenter} from "code_editor";
import {ReportDataWrapper} from "report_data";
import {ResultsPresenter} from "results";
import {UiEditorPresenter} from "ui_editor";
import {UiTranslatorCompiler} from "ui_translator";
import {UpdateUtil} from "updates";
import {WasmBackend, WasmLayer, BackendResult} from "wasm_backend";

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
const INTRODUCTION_PREFERENCE_KEY = "hideIntroduction";

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

    const source = localStorage.getItem("source");
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
    localStorage.setItem("source", code);

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
}

/**
 * Presenter for managing the introduction sequence.
 */
class IntroductionPresenter {
  constructor() {
    const self = this;
    self._loadingPanel = document.getElementById("loading");
    self._mainHolder = document.getElementById("main-holder");
  }

  /**
   * Initialize the introduction sequence.
   * @return {Promise} Promise that resolves when the user continues.
   */
  async initialize() {
    const self = this;
    const hideIntroduction = localStorage.getItem(INTRODUCTION_PREFERENCE_KEY) === "true";

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
      localStorage.setItem(INTRODUCTION_PREFERENCE_KEY, "true");
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
  const introPresenter = new IntroductionPresenter();

  const showApp = async () => {
    await introPresenter.initialize();
    introPresenter._showMainContent();
  };

  const onLoad = () => {
    const mainPresenter = new MainPresenter();
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

export {main};
