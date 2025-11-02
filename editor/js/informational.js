/**
 * Presenter classes for managing informational dialogs and sequences.
 *
 * @license BSD, see LICENSE.md.
 */

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
   *
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
   *
   * @param {Function} resolve - Callback to resolve the promise when user continues.
   * @private
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
 * Presenter for managing the privacy confirmation checkbox and dialog.
 */
class PrivacyConfirmationPresenter {
  constructor() {
    const self = this;
    self._checkbox = document.getElementById("privacy-confirmation-check");
    self._dialog = document.getElementById("privacy-confirmation-dialog");
    self._closeButton = self._dialog.querySelector(".close-button");
    self._privacyTermsButton = document.getElementById("privacy-terms-button");
    self._privacyDetailsButton = document.getElementById("privacy-details-button");

    self._setupEventListeners();
    self._setupPrivacyButton(self._privacyTermsButton);
    self._setupPrivacyButton(self._privacyDetailsButton);
  }

  /**
   * Set up event listeners for checkbox and dialog interactions.
   *
   * @private
   */
  _setupEventListeners() {
    const self = this;

    self._checkbox.addEventListener("change", (event) => {
      if (!event.target.checked) {
        self._showDialog();
      }
    });

    self._closeButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._hideDialog();
    });

    self._dialog.addEventListener("close", () => {
      self._onDialogClose();
    });
  }

  /**
   * Set up event listener for a privacy button.
   *
   * @param {HTMLElement} button - The button element to set up event listeners for
   * @private
   */
  _setupPrivacyButton(button) {
    const self = this;

    button.addEventListener("click", (event) => {
      event.preventDefault();
      self._showDialog();
    });
  }

  /**
   * Show the privacy confirmation dialog.
   *
   * @private
   */
  _showDialog() {
    const self = this;
    self._dialog.showModal();
  }

  /**
   * Hide the privacy confirmation dialog.
   *
   * @private
   */
  _hideDialog() {
    const self = this;
    self._dialog.close();
  }

  /**
   * Handle dialog close event - re-check the checkbox.
   *
   * @private
   */
  _onDialogClose() {
    const self = this;
    self._checkbox.checked = true;
  }
}

/**
 * Presenter for managing the AI assistant dialog.
 */
class AIAssistantPresenter {
  constructor() {
    const self = this;
    self._dialog = document.getElementById("ai-assistant-dialog");
    self._closeButton = self._dialog.querySelector(".close-button");
    self._aiButton = document.getElementById("ai-assistant-button");
    self._setupEventListeners();
  }

  /**
   * Set up event listeners for AI button and dialog interactions.
   *
   * @private
   */
  _setupEventListeners() {
    const self = this;

    self._aiButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._showDialog();
    });

    self._closeButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._hideDialog();
    });

    self._dialog.addEventListener("close", () => {
      self._onDialogClose();
    });
  }

  /**
   * Show the AI assistant dialog.
   *
   * @private
   */
  _showDialog() {
    const self = this;
    self._dialog.showModal();
  }

  /**
   * Hide the AI assistant dialog.
   *
   * @private
   */
  _hideDialog() {
    const self = this;
    self._dialog.close();
  }

  /**
   * Handle dialog close event.
   *
   * @private
   */
  _onDialogClose() {
  }
}

/**
 * Presenter for managing the AI designer dialog.
 */
class AIDesignerPresenter {
  constructor() {
    const self = this;
    self._dialog = document.getElementById("ai-designer-dialog");
    self._closeButton = self._dialog?.querySelector(".close-button");
    self._aiButton = document.getElementById("ai-designer-button");

    self._setupEventListeners();
  }

  /**
   * Set up event listeners for AI button and dialog interactions.
   *
   * @private
   */
  _setupEventListeners() {
    const self = this;

    self._aiButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._showDialog();
    });

    self._closeButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._hideDialog();
    });

    self._dialog.addEventListener("close", () => {
      self._onDialogClose();
    });
  }

  /**
   * Show the AI designer dialog.
   *
   * @private
   */
  _showDialog() {
    const self = this;
    self._dialog.showModal();
  }

  /**
   * Hide the AI designer dialog.
   *
   * @private
   */
  _hideDialog() {
    const self = this;
    self._dialog.close();
  }

  /**
   * Handle dialog close event.
   *
   * @private
   */
  _onDialogClose() {
  }
}

export {
  IntroductionPresenter,
  PrivacyConfirmationPresenter,
  AIAssistantPresenter,
  AIDesignerPresenter,
};
