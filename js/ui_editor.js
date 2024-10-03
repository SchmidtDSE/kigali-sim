import {
  Application,
  Program,
} from "ui_translator";


function setupListButton(button, targetList, templateId) {
  button.addEventListener("click", (event) => {
    event.preventDefault();

    const newDiv = document.createElement("div");
    newDiv.innerHTML = document.getElementById(templateId).innerHTML;
    newDiv.classList.add("dialog-list-item");
    targetList.appendChild(newDiv);

    const deleteLink = newDiv.querySelector(".delete-command-link");
    deleteLink.addEventListener("click", (event) => {
      event.preventDefault();
      newDiv.remove();
    });
  });
}


class ApplicationsListPresenter {
  constructor(root, getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._root = root;
    self._dialog = self._root.querySelector(".dialog");
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._editingName = null;
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
        self._showDialogFor(x);
      })
      .text("edit")
      .attr("aria-label", (x) => "edit " + x);

    buttonsPane.append("span").text(" | ");

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
      .text("delete")
      .attr("aria-label", (x) => "delete " + x);
  }

  _setupDialog() {
    const self = this;
    const addLink = self._root.querySelector(".add-link");
    addLink.addEventListener("click", (event) => {
      self._showDialogFor(null);
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

      if (self._editingName === null) {
        const application = new Application(newName, [], false, true);
        const codeObj = self._getCodeObj();
        codeObj.addApplication(application);
        self._onCodeObjUpdate(codeObj);
      } else {
        const codeObj = self._getCodeObj();
        codeObj.renameApplication(self._editingName, newName);
        self._onCodeObjUpdate(codeObj);
      }
    });
  }

  _showDialogFor(name) {
    const self = this;
    self._editingName = name;

    if (name === null) {
      self._dialog.querySelector(".edit-application-name-input").value = "";
      self._dialog.querySelector(".action-title").innerHTML = "Add";
    } else {
      self._dialog.querySelector(".edit-application-name-input").value = name;
      self._dialog.querySelector(".action-title").innerHTML = "Edit";
    }

    self._dialog.showModal();
  }

  _getAppNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const applications = codeObj.getApplications();
    const appNames = applications.map((x) => x.getName());
    return appNames;
  }
}


class ConsumptionListPresenter {
  constructor(root, getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._root = root;
    self._dialog = self._root.querySelector(".dialog");
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._editingName = null;
    self._setupDialog();
    self.refresh();
  }

  enable() {
    const self = this;
    self._root.classList.remove("inactive");
  }

  disable() {
    const self = this;
    self._root.classList.add("inactive");
  }

  refresh(codeObj) {
    const self = this;
    self._refreshList(codeObj);
  }

  _refreshList(codeObj) {
    const self = this;
    const consumptionNames = self._getConsumptionNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li")
      .data(consumptionNames)
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
        self._showDialogFor(x);
      })
      .text("edit")
      .attr("aria-label", (x) => "edit " + x);

    buttonsPane.append("span").text(" | ");

    buttonsPane.append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        const message = "Are you sure you want to delete " + x + "?";
        const isConfirmed = confirm(message);
        if (isConfirmed) {
          const codeObj = self._getCodeObj();
          codeObj.consumption(x);
          self._onCodeObjUpdate(codeObj);
        }
      })
      .text("delete")
      .attr("aria-label", (x) => "delete " + x);
  }

  _setupDialog() {
    const self = this;

    self._tabs = new Tabby("#" + self._dialog.querySelector(".tabs").id);

    const addLink = self._root.querySelector(".add-link");
    addLink.addEventListener("click", (event) => {
      self._showDialogFor(null);
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
    });

    const addLevelButton = self._root.querySelector(".add-start-button");
    const levelList = self._root.querySelector(".level-list");
    setupListButton(addLevelButton, levelList, "set-command-template");

    const addChangeButton = self._root.querySelector(".add-change-button");
    const changeList = self._root.querySelector(".change-list");
    setupListButton(addChangeButton, changeList, "change-command-template");

    const addLimitButton = self._root.querySelector(".add-limit-button");
    const limitList = self._root.querySelector(".limit-list");
    setupListButton(addLimitButton, limitList, "limit-command-template");
  }

  _showDialogFor(name) {
    const self = this;
    self._editingName = name;

    if (name === null) {
      self._dialog.querySelector(".action-title").innerHTML = "Add";
    } else {
      self._dialog.querySelector(".action-title").innerHTML = "Edit";
    }

    self._dialog.showModal();
  }

  _getConsumptionNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const applications = codeObj.getApplications();
    const consumptionsNested = applications.map((x) => {
      const appName = x.getName();
      const substances = x.getSubstances();
      return substances.map((substance) => {
        const substanceName = substance.getName();
        return "\"" + substanceName + "\" for \"" + appName + "\"";
      });
    });
    const consumptions = consumptionsNested.flat();
    return consumptions;
  }
}


class PolicyListPresenter {
  constructor(root, getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._root = root;
    self._dialog = self._root.querySelector(".dialog");
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._editingName = null;
    self._setupDialog();
    self.refresh();
  }

  enable() {
    const self = this;
    self._root.classList.remove("inactive");
  }

  disable() {
    const self = this;
    self._root.classList.add("inactive");
  }

  refresh(codeObj) {
    const self = this;
    self._refreshList(codeObj);
  }

  _refreshList(codeObj) {
    const self = this;
    const consumptionNames = self._getPolicyNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li")
      .data(consumptionNames)
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
        self._showDialogFor(x);
      })
      .text("edit")
      .attr("aria-label", (x) => "edit " + x);

    buttonsPane.append("span").text(" | ");

    buttonsPane.append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        const message = "Are you sure you want to delete " + x + "?";
        const isConfirmed = confirm(message);
        if (isConfirmed) {
          const codeObj = self._getCodeObj();
          codeObj.consumption(x);
          self._onCodeObjUpdate(codeObj);
        }
      })
      .text("delete")
      .attr("aria-label", (x) => "delete " + x);
  }

  _setupDialog() {
    const self = this;

    self._tabs = new Tabby("#" + self._dialog.querySelector(".tabs").id);

    const addLink = self._root.querySelector(".add-link");
    addLink.addEventListener("click", (event) => {
      self._showDialogFor(null);
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
    });

    const addRecyclingButton = self._root.querySelector(".add-recycling-button");
    const recyclingList = self._root.querySelector(".recycling-list");
    setupListButton(addRecyclingButton, recyclingList, "recycle-command-template");

    const addReplaceButton = self._root.querySelector(".add-replace-button");
    const replaceList = self._root.querySelector(".replace-list");
    setupListButton(addReplaceButton, replaceList, "replace-command-template");

    const addLevelButton = self._root.querySelector(".add-level-button");
    const levelList = self._root.querySelector(".level-list");
    setupListButton(addLevelButton, levelList, "set-command-template");

    const addChangeButton = self._root.querySelector(".add-change-button");
    const changeList = self._root.querySelector(".change-list");
    setupListButton(addChangeButton, changeList, "change-command-template");

    const addLimitButton = self._root.querySelector(".add-limit-button");
    const limitList = self._root.querySelector(".limit-list");
    setupListButton(addLimitButton, limitList, "limit-command-template");
  }

  _showDialogFor(name) {
    const self = this;
    self._editingName = name;

    if (name === null) {
      self._dialog.querySelector(".action-title").innerHTML = "Add";
    } else {
      self._dialog.querySelector(".action-title").innerHTML = "Edit";
    }

    self._dialog.showModal();
  }

  _getPolicyNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const policies = codeObj.getPolicies();
    return policies.map((x) => x.getName());
  }
}


class SimulationListPresenter {
  constructor(root, getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._root = root;
    self._dialog = self._root.querySelector(".dialog");
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._editingName = null;
    self._setupDialog();
    self.refresh();
  }

  enable() {
    const self = this;
    self._root.classList.remove("inactive");
  }

  disable() {
    const self = this;
    self._root.classList.add("inactive");
  }

  refresh(codeObj) {
    const self = this;
    self._refreshList(codeObj);
  }

  _refreshList(codeObj) {
    const self = this;
    const simulationNames = self._getSimulationNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li")
      .data(simulationNames)
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
        self._showDialogFor(x);
      })
      .text("edit")
      .attr("aria-label", (x) => "edit " + x);

    buttonsPane.append("span").text(" | ");

    buttonsPane.append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        const message = "Are you sure you want to delete " + x + "?";
        const isConfirmed = confirm(message);
        if (isConfirmed) {
          const codeObj = self._getCodeObj();
          codeObj.consumption(x);
          self._onCodeObjUpdate(codeObj);
        }
      })
      .text("delete")
      .attr("aria-label", (x) => "delete " + x);
  }

  _setupDialog() {
    const self = this;

    console.log("here");

    const addLink = self._root.querySelector(".add-link");
    addLink.addEventListener("click", (event) => {
      self._showDialogFor(null);
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
    });
  }

  _showDialogFor(name) {
    const self = this;
    self._editingName = name;

    if (name === null) {
      self._dialog.querySelector(".action-title").innerHTML = "Add";
    } else {
      self._dialog.querySelector(".action-title").innerHTML = "Edit";
    }

    self._dialog.showModal();
  }

  _getSimulationNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const scenarios = codeObj.getScenarios();
    return scenarios.map((x) => x.getName());
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

    const consumptionEditor = self._contentsSelection.querySelector(".consumption");
    self._consumptionList = new ConsumptionListPresenter(
      consumptionEditor,
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
    );

    const policyEditor = self._contentsSelection.querySelector(".policies");
    self._policyList = new PolicyListPresenter(
      policyEditor,
      () => self._getCodeAsObj(),
      (codeObj) => self._onCodeObjUpdate(codeObj),
    );

    const simulationEditor = self._contentsSelection.querySelector(".simulations");
    self._simulationList = new SimulationListPresenter(
      simulationEditor,
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

    if (self._codeObj.getApplications().length > 0) {
      self._consumptionList.enable();
    } else {
      self._consumptionList.disable();
    }

    self._onCodeObjUpdateInner(codeObj);
  }
}


export {UiEditorPresenter};
