/**
 * Presenter classes for managing editor UI actions and indicators.
 *
 * @license BSD, see LICENSE.md.
 */

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
   *
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

/**
 * Presenter controlling the main simulation buttons.
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
    self._setupWelcomeScreen();
    self._setupFooterToggle();
  }

  /**
   * Setup welcome screen checkbox and event handler.
   *
   * @private
   */
  _setupWelcomeScreen() {
    const self = this;
    self._welcomeCheckbox = document.getElementById("tooltip-preference-check");
    self._welcomeCheckbox.checked = self._enabled;
    self._welcomeCheckbox.addEventListener("change", function (event) {
      self.setEnabled(event.target.checked);
    });
  }

  /**
   * Setup footer toggle button and event handler.
   *
   * @private
   */
  _setupFooterToggle() {
    const self = this;
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
    const footerText = self._enabled ? "Disable Tooltips" : "Enable Tooltips";
    self._footerButton.textContent = footerText;
  }

  /**
   * Create tooltips for all target elements.
   *
   * @private
   */
  _createTooltips() {
    const self = this;

    const elements = document.querySelectorAll("[data-tippy-content]");
    elements.forEach(function (element) {
      const instance = tippy(element, {
        appendTo: "parent",
      });
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

export {RunningIndicatorPresenter, ButtonPanelPresenter, TooltipPresenter};
