class CodeEditorPresenter {
  constructor(root, onChange) {
    const self = this;
    self._root = root;
    self._errorDisplay = self._root.querySelector(".error-display");
    self._editor = null;
    self._timeout = null;
    self._onChange = onChange;
    self._initEditor();
  }

  forceUpdate() {
    const self = this;
    setTimeout(() => {
      self._editor.resize(true);
    }, 10);
  }

  getCode() {
    const self = this;
    return self._editor.getValue();
  }

  setCode(code) {
    const self = this;
    self._editor.getSession().setValue(code, 1);
    self._onChange();
  }

  showError(error) {
    const self = this;
    self._errorDisplay.innerHTML = "";

    const newTextNode = document.createTextNode(error);
    self._errorDisplay.appendChild(newTextNode);

    self._errorDisplay.style.display = "block";
  }

  hideError() {
    const self = this;
    self._errorDisplay.style.display = "none";
  }

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
  }

  _getAce() {
    const self = this;

    // eslint-disable-next-line no-undef
    return ace;
  }
}

export {CodeEditorPresenter};
