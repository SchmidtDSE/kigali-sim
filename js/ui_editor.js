import {
  Application,
  Program,
} from "ui_translator";


class ApplicationsListPresenter {
  constructor(root, getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._root = root;
    self._dialog = self._root.querySelector(".dialog");
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._setupDialog();
    self.refresh();
  }

  refresh(codeObj) {
    const self = this;
    self._refreshList(codeObj);
  }

  _refreshList(codeObj) {
    const self = this;
    const appNames = self._getAppNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li")
      .data(appNames)
      .enter()
      .append("li");
    
    newItems.attr("aria-label", (x) => x);
    
    const buttonsPane = newItems.append("div")
      .classed("list-buttons", true);

    newItems.append("div")
      .classed("list-label", true)
      .text((x) => x);
    
    buttonsPane.append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        const message = "Are you sure you want to delete " + x + "?";
        const isConfirmed = confirm(message);
        if (isConfirmed) {
          const codeObj = self._getCodeObj();
          codeObj.deleteApplication(x);
          self._onCodeObjUpdate(codeObj);
        }
      })
      .text("delete");
  }

  _setupDialog() {
    const self = this;
    const addLink = self._root.querySelector(".add-link");
    addLink.addEventListener("click", (event) => {
      self._dialog.showModal();
      event.preventDefault();
    });

    const closeButton = self._root.querySelector(".cancel-button");
    closeButton.addEventListener("click", (event) => {
      self._dialog.close();
      event.preventDefault();
    });

    const saveButton = self._root.querySelector(".save-button");
    saveButton.addEventListener("click", (event) => {
      self._dialog.close();
      event.preventDefault();

      const nameInput = self._dialog.querySelector(".edit-application-name-input");
      const newName = nameInput.value;

      const priorNames = new Set(self._getAppNames());
      const nameIsDuplicate = priorNames.has(newName);
      if (nameIsDuplicate) {
        alert("Whoops! An application by that name already exists.");
        return;
      }

      nameInput.value = "";

      const application = new Application(newName, [], false, true);
      const codeObj = self._getCodeObj();
      codeObj.addApplication(application);
      self._onCodeObjUpdate(codeObj);
    });
  }

  _getAppNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const applications = codeObj.getApplications();
    const appNames = applications.map((x) => x.getName());
    return appNames;
  }
}


class UiEditorPresenter {
  constructor(tabRoot, contentsRoot, getCodeAsObj, onCodeObjUpdate) {
    const self = this;

    self._contentsSelection = contentsRoot;
    self._getCodeAsObjInner = getCodeAsObj;
    self._onCodeObjUpdateInner = onCodeObjUpdate;
    self._codeObj = null;
    self._initCodeObj();

    self._tabs = new Tabby("#" + tabRoot.id);
    const appEditor = self._contentsSelection.querySelector(".applications");
    self._applicationsList = new ApplicationsListPresenter(
      appEditor,
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
    );
    self._setupAdvancedLinks();
  }

  refresh(codeObj) {
    const self = this;
    self._codeObj = codeObj;
    self._applicationsList.refresh(codeObj);
  }

  _setupAdvancedLinks() {
    const self = this;
    const links = Array.of(...self._contentsSelection.querySelectorAll(".advanced-editor-link"));
    links.forEach((link) => link.addEventListener("click", (event) => {
      self._tabs.toggle("#code-editor-pane");
      event.preventDefault();
    }));
  }

  _getCodeAsObj() {
    const self = this;
    return self._codeObj;
  }

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

  _enableBasicPanel() {
    const self = this;
    self._contentsSelection.querySelector(".available-contents").style.display = "block";
    self._contentsSelection.querySelector(".not-available-contents").style.display = "none";
  }

  _disableBasicPanel() {
    const self = this;
    self._contentsSelection.querySelector(".available-contents").style.display = "none";
    self._contentsSelection.querySelector(".not-available-contents").style.display = "block";
  }

  _onCodeObjUpdate(codeObj) {
    const self = this;
    self._codeObj = codeObj;
    self._onCodeObjUpdateInner(codeObj);
  }
}


export {UiEditorPresenter};
