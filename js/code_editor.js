/**
 * Presenter class for handling code editor functionality.
 *
 * @license BSD
 */

class CodeEditorPresenter {
  /**
   * Creates a new CodeEditorPresenter instance
   *
   * @param {HTMLElement} root - The root element containing the editor.
   * @param {Function} onChange - Callback function triggered when editor
   *     content changes.
   */
  constructor(root, onChange) {
    const self = this;
    self._root = root;
    self._errorDisplay = self._root.querySelector(".error-display");
    self._editor = null;
    self._timeout = null;
    self._onChange = onChange;
    self._initEditor();
  }

  /**
   * Force a redraw of the editor.
   *
   * Force a redraw of the editor, pushing the request into the event queue.
   */
  forceUpdate() {
    const self = this;
    setTimeout(() => {
      self._editor.resize(true);
    }, 10);
  }

  /**
   * Gets the current code in the editor.
   *
   * @returns {string} The current editor content.
   */
  getCode() {
    const self = this;
    return self._editor.getValue();
  }

  /**
   * Sets new code content in the editor
   *
   * @param {string} code - The code to set in the editor.
   */
  setCode(code) {
    const self = this;
    self._editor.getSession().setValue(code, 1);
    self._onChange();
  }

  /**
   * Display an error message.
   *
   * @param {string} error - The error message to display.
   */
  showError(error) {
    const self = this;
    self._errorDisplay.innerHTML = "";

    const newTextNode = document.createTextNode(error);
    self._errorDisplay.appendChild(newTextNode);

    self._errorDisplay.style.display = "block";
  }

  /**
   * Hides the error display
   */
  hideError() {
    const self = this;
    self._errorDisplay.style.display = "none";
  }

  /**
   * Initializes the Ace editor.
   *
   * @private
   */
  _initEditor() {
    const self = this;

    const targetId = self._root.querySelector(".inner").id;
    self._editor = self._getAce().edit(targetId);
    self._editor.getSession().setUseWorker(false);

    self._editor.session.setOptions({
      tabSize: 2,
      useSoftTabs: true,
    });

    self._editor.setOption("printMarginColumn", 100);
    self._editor.setOption("enableKeyboardAccessibility", true);

    self._editor.setTheme("ace/theme/textmate");

    self._editor.getSession().on("change", () => {
      if (self._timeout !== null) {
        clearTimeout(self._timeout);
      }
      self._timeout = setTimeout(() => self._onChange(), 500);
    });

    ace.config.set("basePath", "/third_party");
    ace.config.loadModule("ace/ext/searchbox");
    // Set QubecTalk syntax highlighting mode
    self._editor.session.setMode("ace/mode/qubectalk");
  }

  /**
   * Gets the Ace editor instance.
   *
   * @private
   * @returns {Object} The Ace editor instance
   */
  _getAce() {
    const self = this;

    // eslint-disable-next-line no-undef
    return ace;
  }
}

export {CodeEditorPresenter};
