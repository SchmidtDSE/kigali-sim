import {CodeEditorPresenter} from "code_editor";
import {Compiler} from "compiler";
import {ReportDataWrapper} from "report_data";
import {ResultsPresenter} from "results";
import {UiEditorPresenter} from "ui_editor";
import {UiTranslatorCompiler} from "ui_translator";

const WHITESPACE_REGEX = new RegExp("^\\s*$");


class ButtonPanelPresenter {
  constructor(root, onBuild) {
    const self = this;
    self._root = root;

    self._availableDisplay = self._root.querySelector("#available-panel");
    self._loadingDisplay = self._root.querySelector("#loading");
    self._runButton = self._root.querySelector("#run-button");

    self._onBuild = onBuild;
    self._runButton.addEventListener("click", (run) => {
      self._onBuild(run);
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
      document.getElementById("code-editor"),
      () => self._onCodeChange(),
    );
    self._buttonPanelPresenter = new ButtonPanelPresenter(
      document.getElementById("code-buttons-panel"),
      () => self._onBuild(true),
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
  }

  _onCodeChange() {
    const self = this;
    const code = self._codeEditorPresenter.getCode();
    if (WHITESPACE_REGEX.test(code)) {
      self._buttonPanelPresenter.hideScriptButtons();
    } else {
      self._buttonPanelPresenter.showScriptButtons();
      self._onBuild(false);
    }
    localStorage.setItem("source", code);
    
    const encodedValue = encodeURI("data:text/qubectalk;charset=utf-8," + code);
    const saveButton = document.getElementById("save-file-button");
    saveButton.href = encodedValue;
  }

  _onBuild(run) {
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
          if (run) {
            const programResult = program();
            self._onResult(programResult);
          }
        } catch (e) {
          alert("" + e);
        }
      }
    };

    const executeSafe = () => {
      try {
        execute();
      } catch (e) {
        alert("" + e);
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

  _onResult(results) {
    const self = this;
    const resultsWrapped = new ReportDataWrapper(results);
    self._resultsPresenter.showResults(resultsWrapped);
  }

  _getCodeAsObj(overrideCode) {
    const self = this;
    const code = overrideCode === undefined ? self._codeEditorPresenter.getCode() : overrideCode;
    const compiler = new UiTranslatorCompiler();
    const result = compiler.compile(code);
    return result;
  }

  _onCodeObjUpdate(codeObj) {
    const self = this;
    const newCode = codeObj.toCode(0);
    self._codeEditorPresenter.setCode(newCode);

    if (self._uiEditorPresenter !== null) {
      self._uiEditorPresenter.refresh(codeObj);
    }

    if (codeObj.getScenarios() == 0) {
      return;
    }

    self._onBuild(true);
  }
}


function main() {
  const mainPresenter = new MainPresenter();
}


export {main};
