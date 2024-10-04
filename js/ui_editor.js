import { EngineNumber } from "engine_number";

import { YearMatcher } from "engine_state";

import {
  Application,
  Program,
} from "ui_translator";


function updateDurationSelector(dateSelector) {
  const makeVisibilityCallback = (showStart, showEnd) => {
    return () => {
      const startElement = newDiv.querySelector(".duration-start");
      startElement.style.display = showStart ? "inline-block" : "none";

      const endElement = newDiv.querySelector(".duration-end");
      endElement.style.display = showEnd ? "inline-block" : "none";

      const toElement = newDiv.querySelector(".duration-to");
      const showTo = showStart && showEnd;
      toElement.style.display = showTo ? "inline-block" : "none";
    };
  };

  const strategies = {
    "in year": makeVisibilityCallback(true, false),
    "during all years": makeVisibilityCallback(false, false),
    "starting in year": makeVisibilityCallback(true, false),
    "ending in year": makeVisibilityCallback(false, true),
    "during years": makeVisibilityCallback(true, true),
  };

  const refreshVisibility = (dateSelector) => {
    const currentValue = dateSelector.querySelector(".duration-type-input").value;
    const strategy = strategies[currentValue];
    strategy();
  };

  updateDurationSelector(dateSelector);
}


function setupDurationSelector(newDiv) {
  const dateSelectors = Array.of(...newDiv.querySelectorAll(".duration-subcomponent"));
  dateSelectors.forEach((dateSelector) => {
    dateSelector.addEventListener("change", (event) => {
      refreshVisibility(dateSelector);
    });
    refreshVisibility(dateSelector);
  });
}


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

    setupDurationSelector(newDiv);
  });
}


function setFieldValue(selection, source, strategy, defaultValue) {
  const newValue = source === null ? null : strategy(source);
  const valueOrDefault = newValue === null ? defaultValue : newValue;
  selection.value = valueOrDefault;
}


function setEngineNumberValue(valSelection, unitsSelection, source, strategy, defaultValue) {
  const newValue = source === null ? null : strategy(source);
  const valueOrDefault = newValue === null ? defaultValue : newValue;
  valSelection.value = valueOrDefault.getValue();
  unitsSelection.value = valueOrDefault.getUnits();
}


function setDuring(selection, command, defaultVal) {
  const effectiveVal = obj === null ? defaultVal : command.getDuration();
  const durationTypeInput = selection.querySelector(".duration-type-input");
  
  if (effectiveVal === null) {
    durationTypeInput.value = "during all years";
    return;
  }

  const durationStartInput = selection.querySelector(".duration-start");
  const durationEndInput = selection.querySelector(".duration-end");
  const durationStart = effectiveVal.getStart();
  const noStart = durationStart === null;
  const durationEnd = effectiveVal.getEnd();
  const noEnd = durationEnd === null;

  if (noStart && noEnd) {
    durationTypeInput.value = "during all years";
  } else if (noStart) {
    durationTypeInput.value = "ending in year";
    durationEndInput.value = durationEnd;
  } else if (noEnd) {
    durationTypeInput.value = "starting in year";
    durationStartInput.value = durationStart;
  } else if (durationStart == durationEnd) {
    durationTypeInput.value = "in year";
    durationStartInput.value = durationStart;
  } else {
    durationTypeInput.value = "during years";
    durationStartInput.value = durationStart;
    durationEndInput.value = durationEnd;
  }

  updateDurationSelector(selection);
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

    const getObjToShow = () => {
      if (name === null) {
        return {"obj": null, "application": ""};
      }
      const objIdentifierRegex = /\"([^\"]+)\" for \"([^\"]+)\"/g;
      const match = name.match(objIdentifierRegex);
      const substance = match[0];
      const application = match[1];
      const codeObj = self._getCodeObj.get();
      const substanceObj = codeObj.getApplication(application).getSubstance(substance);
      return {"obj": substanceObj, "application": application};
    };

    const objToShowInfo = getObjToShow();
    const objToShow = objToShowInfo["obj"];
    const applicationName = objToShowInfo["application"];

    const substances = codeObj.getSubstances();
    const substanceNames = substances.map((x) => x.getName());
    const substanceSelect = d3.select(self._dialog.querySelector(".substances-select"));
    substanceSelect.html("");
    substanceSelect.selectAll("option")
      .data(substanceNames)
      .enter()
      .append("option")
      .attr("value", (x) => x)
      .text((x) => x);

    setFieldValue(
      self._dialog.querySelector(".edit-consumption-substance-input"),
      objToShow,
      "",
      (x) => x.getName(),
    );

    setFieldValue(
      self._dialog.querySelector(".edit-consumption-application-input"),
      objToShow,
      "",
      (x) => applicationName,
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-domestic-input"),
      self._dialog.querySelector(".initial-charge-domestic-units-input"),
      objToShow,
      new EngineNumber(1, "kg / unit"),
      (x) => x.getInitialCharge("domestic").getValue(),
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-import-input"),
      self._dialog.querySelector(".initial-charge-import-units-input"),
      objToShow,
      new EngineNumber(2, "kg / unit"),
      (x) => x.getInitialCharge("import").getValue(),
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-retirement-input"),
      self._dialog.querySelector(".retirement-units-input"),
      objToShow,
      new EngineNumber(5, "% / year"),
      (x) => x.getRetire().getValue(),
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-recharge-population"),
      self._dialog.querySelector(".recharge-population-units-input"),
      objToShow,
      new EngineNumber(5, "% / year"),
      (x) => x.getRecharge().getTarget(),
    );
    
    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-recharge-volume"),
      self._dialog.querySelector(".recharge-volume-units-input"),
      objToShow,
      new EngineNumber(1, "kg / unit"),
      (x) => x.getRecharge().getValue(),
    );

    setListInput(
      self._dialog.querySelector(".level-list"),
      self._dialog.querySelector(".set-command-template").innerHTML,
      objToShow.getSetVals(),
      (itemObj, root) => {
        setFieldValue(
          root.querySelector(".set-target-input"),
          itemObj,
          (x) => x.getTarget(),
          "import"
        );
        setEngineNumberValue(
          root.querySelector(".set-amount-input"),
          root.querySelector(".set-units-input"),
          itemObj,
          new EngineNumber(1, "mt"),
          (x) => x.getValue(),
        );
        setDuring(
          root.querySelector(".duration-subcomponent"),
          rootObj,
          new YearMatcher(1, 1),
        );
      },
    );

    setListInput(
      self._dialog.querySelector(".change-list"),
      self._dialog.querySelector(".change-command-template").innerHTML,
      objToShow.getSetVals(),
      (itemObj, root) => {
        setFieldValue(
          root.querySelector(".change-target-input"),
          itemObj,
          (x) => x.getTarget(),
          "import"
        );
        setFieldValue(
          root.querySelector(".change-sign-input"),
          itemObj,
          (x) => x.getValue() < 0 ? "-" : "+",
          "+",
        );
        setFieldValue(
          root.querySelector(".change-amount-input"),
          itemObj,
          (x) => {
            if (x.getValue() === null || x.getValue().getValue() === null) {
              return 5;  // Default
            }
            const valueSigned = x.getValue().getValue();
            const valueUnsigned = Math.abs(valueSigned);
            return valueUnsigned;
          },
          5,
        );
        setFieldValue(
          root.querySelector(".change-units-input"),
          itemObj,
          (x) => {
            if (x.getValue() === null) {
              return "% / year";  // Default
            }
            return x.getValue().getUnits();
          },
          "% / year"
        );
        setDuring(
          root.querySelector(".duration-subcomponent"),
          rootObj,
          new YearMatcher(1, 1),
        );
      },
    );

    setListInput(
      self._dialog.querySelector(".limit-list"),
      self._dialog.querySelector(".limit-command-template").innerHTML,
      objToShow.getSetVals(),
      (itemObj, root) => {
        setFieldValue(
          root.querySelector(".limit-type-input"),
          itemObj,
          (x) => x.getType(),
          "cap"
        );
        setFieldValue(
          root.querySelector(".limit-target-input"),
          itemObj,
          (x) => x.getTarget(),
          "sales"
        );
        setEngineNumberValue(
          root.querySelector(".limit-amount-input"),
          root.querySelector(".limit-units-input"),
          itemObj,
          new EngineNumber(1, "mt"),
          (x) => x.getValue(),
        );
        setFieldValue(
          root.querySelector(".displacing-input"),
          itemObj,
          (x) => x.getDisplacing() === null ? "" : x.getDisplacing(),
          ""
        );
        setDuring(
          root.querySelector(".duration-subcomponent"),
          rootObj,
          new YearMatcher(1, 1),
        );
      },
    );

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
