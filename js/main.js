import {CodeEditorPresenter} from "code_editor";
import {Compiler} from "compiler";
import {ReportDataWrapper} from "report_data";
import {ResultsPresenter} from "results";

const WHITESPACE_REGEX = new RegExp("^\\s*$");


class ButtonPanelPresenter {
  constructor(targetId, onRun) {
    const self = this;
    self._root = document.getElementById(targetId);

    self._availableDisplay = self._root.querySelector("#available-panel");
    self._loadingDisplay = self._root.querySelector("#loading");
    self._runButton = self._root.querySelector("#run-button");

    self._onRun = onRun;
    self._runButton.addEventListener("click", () => {
      self._onRun();
    });

    self.enable();
  }

  enable() {
    const self = this;
    self._availableDisplay.style.display = "block";
    self._loadingDisplay.style.display = "none";
  }

  disable() {
    const self = this;
    self._availableDisplay.style.display = "none";
    self._loadingDisplay.style.display = "block";
  }

  hideScriptButtons() {
    const self = this;
    self._runButton.style.display = "none";
  }

  showScriptButtons() {
    const self = this;
    self._runButton.style.display = "inline-block";
  }
}


class MainPresenter {
  constructor() {
    const self = this;
    self._codeEditorPresenter = new CodeEditorPresenter(
      "code-editor",
      () => self._onCodeChange(),
    );
    self._buttonPanelPresenter = new ButtonPanelPresenter(
      "buttons-panel",
      () => self._onRun(),
    );
    self._resultsPresenter = new ResultsPresenter("results");
    self._onCodeChange();
  }

  _onCodeChange() {
    const self = this;
    const code = self._codeEditorPresenter.getCode();
    if (WHITESPACE_REGEX.test(code)) {
      self._buttonPanelPresenter.hideScriptButtons();
    } else {
      self._buttonPanelPresenter.showScriptButtons();
    }
  }

  _onRun() {
    const self = this;
    self._buttonPanelPresenter.disable();

    const execute = () => {
      const compiler = new Compiler();
      const code = self._codeEditorPresenter.getCode();
      const result = compiler.compile(code);

      const compileErrors = result.getErrors();
      const hasErrors = compileErrors.length > 0;
      if (hasErrors) {
        self._codeEditorPresenter.showError(compileErrors[0]);
        self._buttonPanelPresenter.enable();
        return;
      } else {
        self._codeEditorPresenter.hideError();
      }

      const program = result.getProgram();
      if (result.getErrors().length > 0) {
        alert(result.getErrors()[0]);
        self._buttonPanelPresenter.enable();
        return;
      }

      if (result.getErrors().length > 0) {
        alert(result.getErrors()[0]);
        self._buttonPanelPresenter.enable();
        return;
      } else if (program !== null) {
        try {
          const programResult = program();
          self._onResult(programResult);
        } catch (e) {
          alert("" + e);
        }
      }

      self._buttonPanelPresenter.enable();
    };

    setTimeout(execute, 250);
  }

  _onResult(results) {
    const self = this;
    const resultsWrapped = new ReportDataWrapper(results);
    self._resultsPresenter.showResults(resultsWrapped);
  }
}


function main() {
  const mainPresenter = new MainPresenter();
}


export {main};
