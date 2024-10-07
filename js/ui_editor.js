import {EngineNumber} from "engine_number";

import {YearMatcher} from "engine_state";

import {
  Application,
  Command,
  DefinitionalStanza,
  LimitCommand,
  Program,
  ReplaceCommand,
  SimulationScenario,
  SubstanceBuilder,
} from "ui_translator";


function updateDurationSelector(dateSelector) {
  const makeVisibilityCallback = (showStart, showEnd) => {
    return () => {
      const startElement = dateSelector.querySelector(".duration-start");
      startElement.style.display = showStart ? "inline-block" : "none";

      const endElement = dateSelector.querySelector(".duration-end");
      endElement.style.display = showEnd ? "inline-block" : "none";

      const toElement = dateSelector.querySelector(".duration-to");
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

  refreshVisibility(dateSelector);
}


function setupDurationSelector(newDiv) {
  const dateSelectors = Array.of(...newDiv.querySelectorAll(".duration-subcomponent"));
  dateSelectors.forEach((dateSelector) => {
    dateSelector.addEventListener("change", (event) => {
      updateDurationSelector(dateSelector);
    });
    updateDurationSelector(dateSelector);
  });
}


function setupListButton(button, targetList, templateId, initUiCallback) {
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

    initUiCallback(null, newDiv);

    setupDurationSelector(newDiv);
  });
}


function setFieldValue(selection, source, defaultValue, strategy) {
  const newValue = source === null ? null : strategy(source);
  const valueOrDefault = newValue === null ? defaultValue : newValue;
  selection.value = valueOrDefault;
}


function getFieldValue(selection) {
  return selection.value;
}


function getSanitizedFieldValue(selection) {
  const valueRaw = getFieldValue(selection);
  return valueRaw.replaceAll("\"", "").replaceAll(",", "");
}


function setListInput(listSelection, itemTemplate, items, uiInit) {
  listSelection.innerHTML = "";
  const addItem = (item) => {
    const newDiv = document.createElement("div");
    newDiv.innerHTML = itemTemplate;
    newDiv.classList.add("dialog-list-item");
    listSelection.appendChild(newDiv);
    uiInit(item, newDiv);

    const deleteLink = newDiv.querySelector(".delete-command-link");
    deleteLink.addEventListener("click", (event) => {
      event.preventDefault();
      newDiv.remove();
    });
  };
  items.forEach(addItem);
}


function getListInput(selection, itemReadStrategy) {
  const dialogListItems = Array.of(...selection.querySelectorAll(".dialog-list-item"));
  return dialogListItems.map(itemReadStrategy);
}


function setEngineNumberValue(valSelection, unitsSelection, source, defaultValue, strategy) {
  const newValue = source === null ? null : strategy(source);
  const valueOrDefault = newValue === null ? defaultValue : newValue;
  valSelection.value = valueOrDefault.getValue();
  unitsSelection.value = valueOrDefault.getUnits();
}


function getEngineNumberValue(valSelection, unitsSelection) {
  const value = valSelection.value;
  const units = unitsSelection.value;
  return new EngineNumber(value, units);
}


function setDuring(selection, command, defaultVal) {
  const effectiveVal = command === null ? defaultVal : command.getDuration();
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
      const newName = nameInput.value.replaceAll("\"", "").replaceAll(",", "");

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
          const objIdentifierRegex = /\"([^\"]+)\" for \"([^\"]+)\"/;
          const match = x.match(objIdentifierRegex);
          const substance = match[1];
          const application = match[2];
          codeObj.deleteSubstance(application, substance);
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
      self._save();
      self._dialog.close();
      event.preventDefault();
    });

    const addLevelButton = self._root.querySelector(".add-start-button");
    const levelList = self._root.querySelector(".level-list");
    setupListButton(addLevelButton, levelList, "set-command-template", initSetCommandUi);

    const addChangeButton = self._root.querySelector(".add-change-button");
    const changeList = self._root.querySelector(".change-list");
    setupListButton(addChangeButton, changeList, "change-command-template", initChangeCommandUi);

    const addLimitButton = self._root.querySelector(".add-limit-button");
    const limitList = self._root.querySelector(".limit-list");
    setupListButton(
      addLimitButton,
      limitList,
      "limit-command-template",
      (item, root) => initLimitCommandUi(item, root, self._getCodeObj()),
    );
  }

  _showDialogFor(name) {
    const self = this;
    self._editingName = name;

    self._tabs.toggle("#consumption-general");

    if (name === null) {
      self._dialog.querySelector(".action-title").innerHTML = "Add";
    } else {
      self._dialog.querySelector(".action-title").innerHTML = "Edit";
    }

    const codeObj = self._getCodeObj();

    const getObjToShow = () => {
      if (name === null) {
        return {"obj": null, "application": ""};
      }
      const objIdentifierRegex = /\"([^\"]+)\" for \"([^\"]+)\"/;
      const match = name.match(objIdentifierRegex);
      const substance = match[1];
      const application = match[2];
      const substanceObj = codeObj.getApplication(application).getSubstance(substance);
      return {"obj": substanceObj, "application": application};
    };

    const objToShowInfo = getObjToShow();
    const objToShow = objToShowInfo["obj"];
    const applicationName = objToShowInfo["application"];

    const applicationNames = self._getCodeObj().getApplications().map((x) => x.getName());
    const applicationSelect = self._dialog.querySelector(".application-select");
    d3.select(applicationSelect)
      .html("")
      .selectAll("option")
      .data(applicationNames)
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
      applicationNames[0],
      (x) => applicationName,
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-emissions-input"),
      self._dialog.querySelector(".edit-consumption-emissions-units-input"),
      objToShow,
      new EngineNumber(1, "tCO2e / kg"),
      (x) => x.getEmit().getValue(),
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-domestic-input"),
      self._dialog.querySelector(".initial-charge-domestic-units-input"),
      objToShow,
      new EngineNumber(1, "kg / unit"),
      (x) => x.getInitialCharge("manufacture").getValue(),
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
      self._dialog.querySelector(".edit-consumption-recharge-population-input"),
      self._dialog.querySelector(".recharge-population-units-input"),
      objToShow,
      new EngineNumber(5, "% / year"),
      (x) => x.getRecharge().getTarget(),
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-recharge-volume-input"),
      self._dialog.querySelector(".recharge-volume-units-input"),
      objToShow,
      new EngineNumber(1, "kg / unit"),
      (x) => x.getRecharge().getValue(),
    );

    setListInput(
      self._dialog.querySelector(".level-list"),
      document.getElementById("set-command-template").innerHTML,
      objToShow === null ? [] : objToShow.getSetVals(),
      initSetCommandUi,
    );

    setListInput(
      self._dialog.querySelector(".change-list"),
      document.getElementById("change-command-template").innerHTML,
      objToShow === null ? [] : objToShow.getChanges(),
      initChangeCommandUi,
    );

    setListInput(
      self._dialog.querySelector(".limit-list"),
      document.getElementById("limit-command-template").innerHTML,
      objToShow === null ? [] : objToShow.getLimits(),
      (item, root) => initLimitCommandUi(item, root, self._getCodeObj()),
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

  _save() {
    const self = this;
    const substance = self._parseObj();

    const codeObj = self._getCodeObj();

    if (self._editingName === null) {
      const applicationName = getFieldValue(
        self._dialog.querySelector(".edit-consumption-application-input"),
      );
      codeObj.insertSubstance(applicationName, null, substance);
    } else {
      const objIdentifierRegex = /\"([^\"]+)\" for \"([^\"]+)\"/;
      const match = self._editingName.match(objIdentifierRegex);
      const substanceName = match[1];
      const applicationName = match[2];
      codeObj.insertSubstance(applicationName, substanceName, substance);
    }

    self._onCodeObjUpdate(codeObj);
  }

  _parseObj() {
    const self = this;

    const substanceName = getSanitizedFieldValue(
      self._dialog.querySelector(".edit-consumption-substance-input"),
    );

    const substanceBuilder = new SubstanceBuilder(substanceName, false);

    const emissionsValue = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-emissions-input"),
      self._dialog.querySelector(".edit-consumption-emissions-units-input"),
    );
    substanceBuilder.addCommand(new Command("emit", null, emissionsValue, null));

    const initialChargeDomestic = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-domestic-input"),
      self._dialog.querySelector(".initial-charge-domestic-units-input"),
    );
    const initialChargeDomesticCommand = new Command(
      "initial charge",
      "manufacture",
      initialChargeDomestic,
      null,
    );
    substanceBuilder.addCommand(initialChargeDomesticCommand);

    const initialChargeImport = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-import-input"),
      self._dialog.querySelector(".initial-charge-import-units-input"),
    );
    const initialChargeImportCommand = new Command(
      "initial charge",
      "import",
      initialChargeImport,
      null,
    );
    substanceBuilder.addCommand(initialChargeImportCommand);

    const retirement = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-retirement-input"),
      self._dialog.querySelector(".retirement-units-input"),
    );
    const retireCommand = new Command(
      "retire",
      null,
      retirement,
      null,
    );
    substanceBuilder.addCommand(retireCommand);

    const rechargePopulation = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-recharge-population-input"),
      self._dialog.querySelector(".recharge-population-units-input"),
    );

    const rechargeVolume = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-recharge-volume-input"),
      self._dialog.querySelector(".recharge-volume-units-input"),
    );

    const rechargeCommand = new Command(
      "recharge",
      rechargePopulation,
      rechargeVolume,
      null,
    );
    substanceBuilder.addCommand(rechargeCommand);

    const levels = getListInput(
      self._dialog.querySelector(".level-list"),
      readSetCommandUi,
    );
    levels.forEach((x) => substanceBuilder.addCommand(x));

    const changes = getListInput(
      self._dialog.querySelector(".change-list"),
      readChangeCommandUi,
    );
    changes.forEach((x) => substanceBuilder.addCommand(x));

    const limits = getListInput(
      self._dialog.querySelector(".limit-list"),
      readLimitCommandUi,
    );
    limits.forEach((x) => substanceBuilder.addCommand(x));

    return substanceBuilder.build(true);
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
    const policyNames = self._getPolicyNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li")
      .data(policyNames)
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
          codeObj.deletePolicy(x);
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
      self._save();
      self._dialog.close();
      event.preventDefault();
    });

    const addRecyclingButton = self._root.querySelector(".add-recycling-button");
    const recyclingList = self._root.querySelector(".recycling-list");
    setupListButton(
      addRecyclingButton,
      recyclingList,
      "recycle-command-template",
      initRecycleCommandUi,
    );

    const addReplaceButton = self._root.querySelector(".add-replace-button");
    const replaceList = self._root.querySelector(".replace-list");
    setupListButton(
      addReplaceButton,
      replaceList,
      "replace-command-template",
      (item, root) => initReplaceCommandUi(item, root, self._getCodeObj()),
    );

    const addLevelButton = self._root.querySelector(".add-level-button");
    const levelList = self._root.querySelector(".level-list");
    setupListButton(
      addLevelButton,
      levelList,
      "set-command-template",
      initSetCommandUi,
    );

    const addChangeButton = self._root.querySelector(".add-change-button");
    const changeList = self._root.querySelector(".change-list");
    setupListButton(
      addChangeButton,
      changeList,
      "change-command-template",
      initChangeCommandUi,
    );

    const addLimitButton = self._root.querySelector(".add-limit-button");
    const limitList = self._root.querySelector(".limit-list");
    setupListButton(
      addLimitButton,
      limitList,
      "limit-command-template",
      (item, root) => initLimitCommandUi(item, root, self._getCodeObj()),
    );
  }

  _showDialogFor(name) {
    const self = this;
    self._editingName = name;
    const codeObj = self._getCodeObj();

    self._tabs.toggle("#policy-general");

    const isArrayEmpty = (x) => x === null || x.length == 0;

    const targetPolicy = name === null ? null : codeObj.getPolicy(name);
    const targetApplications = targetPolicy === null ? null : targetPolicy.getApplications();
    const targetApplication = isArrayEmpty(targetApplications) ? null : targetApplications[0];
    const targetSubstances = targetApplication === null ? null : targetApplication.getSubstances();
    const targetSubstance = isArrayEmpty(targetSubstances) ? null : targetSubstances[0];

    if (name === null) {
      self._dialog.querySelector(".action-title").innerHTML = "Add";
    } else {
      self._dialog.querySelector(".action-title").innerHTML = "Edit";
    }

    setFieldValue(
      self._dialog.querySelector(".edit-policy-name-input"),
      targetPolicy,
      "",
      (x) => x.getName(),
    );

    const applicationNames = codeObj.getApplications().map((x) => x.getName());
    const applicationSelect = self._dialog.querySelector(".application-select");
    const targetAppName = targetApplication === null ? "" : targetApplication.getName();
    d3.select(applicationSelect)
      .html("")
      .selectAll("option")
      .data(applicationNames)
      .enter()
      .append("option")
      .attr("value", (x) => x)
      .text((x) => x)
      .property("selected", (x) => x === targetAppName);

    const substances = codeObj.getSubstances();
    const substanceNames = substances.map((x) => x.getName());
    const substanceSelect = d3.select(self._dialog.querySelector(".substances-select"));
    const substanceName = targetSubstance === null ? "" : targetSubstance.getName();
    substanceSelect.html("");
    substanceSelect.selectAll("option")
      .data(substanceNames)
      .enter()
      .append("option")
      .attr("value", (x) => x)
      .text((x) => x)
      .property("selected", (x) => x === substanceName);

    setListInput(
      self._dialog.querySelector(".recycling-list"),
      document.getElementById("recycle-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getRecycles(),
      initRecycleCommandUi,
    );

    setListInput(
      self._dialog.querySelector(".replace-list"),
      document.getElementById("replace-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getReplaces(),
      (item, root) => initReplaceCommandUi(item, root, self._getCodeObj()),
    );

    setListInput(
      self._dialog.querySelector(".level-list"),
      document.getElementById("set-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getSetVals(),
      initSetCommandUi,
    );

    setListInput(
      self._dialog.querySelector(".change-list"),
      document.getElementById("change-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getChanges(),
      initChangeCommandUi,
    );

    setListInput(
      self._dialog.querySelector(".limit-list"),
      document.getElementById("limit-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getLimits(),
      (item, root) => initLimitCommandUi(item, root, self._getCodeObj()),
    );

    self._dialog.showModal();
  }

  _getPolicyNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const policies = codeObj.getPolicies();
    return policies.map((x) => x.getName());
  }

  _parseObj() {
    const self = this;

    const policyName = getSanitizedFieldValue(
      self._dialog.querySelector(".edit-policy-name-input"),
    );
    const applicationName = getFieldValue(
      self._dialog.querySelector(".edit-policy-application-input"),
    );

    const substanceName = getFieldValue(self._dialog.querySelector(".edit-policy-substance-input"));
    const builder = new SubstanceBuilder(substanceName, true);

    const recycles = getListInput(
      self._dialog.querySelector(".recycling-list"),
      readRecycleCommandUi,
    );
    recycles.forEach((command) => builder.addCommand(command));

    const replaces = getListInput(
      self._dialog.querySelector(".replace-list"),
      readReplaceCommandUi,
    );
    replaces.forEach((command) => builder.addCommand(command));

    const levels = getListInput(
      self._dialog.querySelector(".level-list"),
      readSetCommandUi,
    );
    levels.forEach((command) => builder.addCommand(command));

    const changes = getListInput(
      self._dialog.querySelector(".change-list"),
      readChangeCommandUi,
    );
    changes.forEach((command) => builder.addCommand(command));

    const limits = getListInput(
      self._dialog.querySelector(".limit-list"),
      readLimitCommandUi,
    );
    limits.forEach((command) => builder.addCommand(command));

    const substance = builder.build(true);
    const application = new Application(applicationName, [substance], true, true);
    const policy = new DefinitionalStanza(policyName, [application], true, true);

    return policy;
  }

  _save() {
    const self = this;
    const policy = self._parseObj();
    const codeObj = self._getCodeObj();
    codeObj.insertPolicy(self._editingName, policy);
    self._onCodeObjUpdate(codeObj);
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
          codeObj.deleteScenario(x);
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
      self._save();
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

    const scenario = name === null ? null : self._getCodeObj().getScenario(name);

    const policiesSelectedRaw = scenario === null ? [] : scenario.getPolicyNames();
    const policiesSelected = new Set(policiesSelectedRaw);

    setFieldValue(
      self._dialog.querySelector(".edit-simulation-name-input"),
      scenario,
      "",
      (x) => x.getName(),
    );

    setFieldValue(
      self._dialog.querySelector(".edit-simulation-start-input"),
      scenario,
      1,
      (x) => x.getYearStart(),
    );

    setFieldValue(
      self._dialog.querySelector(".edit-simulation-end-input"),
      scenario,
      10,
      (x) => x.getYearEnd(),
    );

    const policyNames = self._getCodeObj().getPolicies().map((x) => x.getName());
    const newLabels = d3.select(self._dialog.querySelector(".policy-sim-list"))
      .html("")
      .selectAll(".policy-check-label")
      .data(policyNames)
      .enter()
      .append("label")
      .classed("policy-check-label", true);

    newLabels.append("input")
      .attr("type", "checkbox")
      .classed("policy-check", true)
      .attr("value", (x) => x)
      .property("checked", (x) => policiesSelected.has(x));

    newLabels.append("span")
      .text((x) => x);

    self._dialog.showModal();
  }

  _getSimulationNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const scenarios = codeObj.getScenarios();
    return scenarios.map((x) => x.getName());
  }

  _save() {
    const self = this;
    const scenario = self._parseObj();
    const codeObj = self._getCodeObj();
    codeObj.insertScenario(self._editingName, scenario);
    self._onCodeObjUpdate(codeObj);
  }

  _parseObj() {
    const self = this;

    const scenarioName = getSanitizedFieldValue(
      self._dialog.querySelector(".edit-simulation-name-input"),
    );
    const start = getFieldValue(self._dialog.querySelector(".edit-simulation-start-input"));
    const end = getFieldValue(self._dialog.querySelector(".edit-simulation-end-input"));

    const policyChecks = Array.of(...self._dialog.querySelectorAll(".policy-check"));
    const policiesChecked = policyChecks.filter((x) => x.checked);
    const policyNamesSelected = policiesChecked.map((x) => x.value);

    return new SimulationScenario(
      scenarioName,
      policyNamesSelected,
      start,
      end,
      true,
    );
  }
}


class UiEditorPresenter {
  constructor(startOnCode, tabRoot, contentsRoot, getCodeAsObj, onCodeObjUpdate, onTabChange) {
    const self = this;

    self._contentsSelection = contentsRoot;
    self._getCodeAsObjInner = getCodeAsObj;
    self._onCodeObjUpdateInner = onCodeObjUpdate;
    self._codeObj = null;
    self._initCodeObj();

    self._tabs = new Tabby("#" + tabRoot.id);
    tabRoot.addEventListener("tabby", () => onTabChange());

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

    if (startOnCode) {
      self._tabs.toggle("#code-editor-pane");
    }
  }

  showCode() {
    const self = this;
    self._tabs.toggle("#code-editor-pane");
  }

  refresh(codeObj) {
    const self = this;
    self._codeObj = codeObj;

    if (self._codeObj.getIsCompatible()) {
      self._applicationsList.refresh(codeObj);
      self._consumptionList.refresh(codeObj);
      self._policyList.refresh(codeObj);
      self._simulationList.refresh(codeObj);
      self._enableBasicPanel();
    } else {
      self._disableBasicPanel();
    }
  }

  forceCodeObj(codeObj) {
    const self = this;
    self._onCodeObjUpdate(codeObj);
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

    if (self._codeObj.getSubstances().length > 0) {
      self._policyList.enable();
      self._simulationList.enable();
    } else {
      self._policyList.disable();
      self._simulationList.disable();
    }

    self._onCodeObjUpdateInner(codeObj);
  }
}


function initSetCommandUi(itemObj, root) {
  setFieldValue(
    root.querySelector(".set-target-input"),
    itemObj,
    "manufacture",
    (x) => x.getTarget(),
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
    itemObj,
    new YearMatcher(1, 1),
  );
}


function readSetCommandUi(root) {
  const target = getFieldValue(root.querySelector(".set-target-input"));
  const amount = getEngineNumberValue(
    root.querySelector(".set-amount-input"),
    root.querySelector(".set-units-input"),
  );
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));
  return new Command("setVal", target, amount, duration);
}


function initChangeCommandUi(itemObj, root) {
  setFieldValue(
    root.querySelector(".change-target-input"),
    itemObj,
    "manufacture",
    (x) => x.getTarget(),
  );
  setFieldValue(
    root.querySelector(".change-sign-input"),
    itemObj,
    "+",
    (x) => x.getValue() < 0 ? "-" : "+",
  );
  setFieldValue(
    root.querySelector(".change-amount-input"),
    itemObj,
    5,
    (x) => {
      if (x.getValue() === null || x.getValue().getValue() === null) {
        return 5; // Default
      }
      const valueSigned = x.getValue().getValue();
      const valueUnsigned = Math.abs(valueSigned);
      return valueUnsigned;
    },
  );
  setFieldValue(
    root.querySelector(".change-units-input"),
    itemObj,
    "% / year",
    (x) => {
      if (x.getValue() === null) {
        return "% / year"; // Default
      }
      return x.getValue().getUnits();
    },
  );
  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(2, 10),
  );
}


function readChangeCommandUi(root) {
  const target = getFieldValue(root.querySelector(".change-target-input"));
  const invert = getFieldValue(root.querySelector(".change-sign-input")) === "-";
  const amountRaw = parseFloat(getFieldValue(root.querySelector(".change-amount-input")));
  const amount = amountRaw * (invert ? -1 : 1);
  const units = getFieldValue(root.querySelector(".change-units-input"));
  const amountWithUnits = new EngineNumber(amount, units);
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));
  return new Command("change", target, amountWithUnits, duration);
}


function initLimitCommandUi(itemObj, root, codeObj) {
  const substances = codeObj.getSubstances();
  const substanceNames = substances.map((x) => x.getName());
  const substanceSelect = d3.select(root.querySelector(".substances-select"));
  substanceSelect.html("");
  substanceSelect.selectAll("option")
    .data(substanceNames)
    .enter()
    .append("option")
    .attr("value", (x) => x)
    .text((x) => x);

  setFieldValue(
    root.querySelector(".limit-type-input"),
    itemObj,
    "cap",
    (x) => x.getTypeName(),
  );
  setFieldValue(
    root.querySelector(".limit-target-input"),
    itemObj,
    "sales",
    (x) => x.getTarget(),
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
    "",
    (x) => x.getDisplacing() === null ? "" : x.getDisplacing(),
  );
  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(2, 10),
  );
}


function readLimitCommandUi(root) {
  const limitType = getFieldValue(root.querySelector(".limit-type-input"));
  const target = getFieldValue(root.querySelector(".limit-target-input"));
  const amount = getEngineNumberValue(
    root.querySelector(".limit-amount-input"),
    root.querySelector(".limit-units-input"),
  );
  const displacingRaw = getFieldValue(root.querySelector(".displacing-input"));
  const displacing = displacingRaw === "" ? null : displacingRaw;
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));
  return new LimitCommand(limitType, target, amount, duration, displacing);
}


function initRecycleCommandUi(itemObj, root) {
  setEngineNumberValue(
    root.querySelector(".recycle-amount-input"),
    root.querySelector(".recycle-units-input"),
    itemObj,
    new EngineNumber(10, "%"),
    (x) => x.getTarget(),
  );
  setEngineNumberValue(
    root.querySelector(".recycle-reuse-amount-input"),
    root.querySelector(".recycle-reuse-units-input"),
    itemObj,
    new EngineNumber(10, "%"),
    (x) => x.getValue(),
  );
  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(2, 10),
  );
}


function readRecycleCommandUi(root) {
  const collection = getEngineNumberValue(
    root.querySelector(".recycle-amount-input"),
    root.querySelector(".recycle-units-input"),
  );
  const reuse = getEngineNumberValue(
    root.querySelector(".recycle-reuse-amount-input"),
    root.querySelector(".recycle-reuse-units-input"),
  );
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));
  return new Command("recycle", collection, reuse, duration);
}


function initReplaceCommandUi(itemObj, root, codeObj) {
  const substances = codeObj.getSubstances();
  const substanceNames = substances.map((x) => x.getName());
  const substanceSelect = d3.select(root.querySelector(".substances-select"));
  substanceSelect.html("");
  substanceSelect.selectAll("option")
    .data(substanceNames)
    .enter()
    .append("option")
    .attr("value", (x) => x)
    .text((x) => x);

  setEngineNumberValue(
    root.querySelector(".replace-amount-input"),
    root.querySelector(".replace-units-input"),
    itemObj,
    new EngineNumber(10, "%"),
    (x) => x.getVolume(),
  );

  setFieldValue(
    root.querySelector(".replace-target-input"),
    itemObj,
    "sales",
    (x) => x.getSource(),
  );

  setFieldValue(
    root.querySelector(".replace-replacement-input"),
    itemObj,
    substanceNames[0],
    (x) => x.getDestination(),
  );

  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(2, 10),
  );
}


function readReplaceCommandUi(root) {
  const target = getFieldValue(root.querySelector(".replace-target-input"));
  const amount = getEngineNumberValue(
    root.querySelector(".replace-amount-input"),
    root.querySelector(".replace-units-input"),
  );
  const replacement = getFieldValue(".replace-replacement-input");
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));

  return new ReplaceCommand(amount, target, replacement, duration);
}


function readDurationUi(root) {
  const durationType = getFieldValue(root.querySelector(".duration-type-input"));
  const targets = {
    "in year": {"min": "duration-start", "max": "duration-start"},
    "during all years": {"min": null, "max": null},
    "starting in year": {"min": "duration-start", "max": null},
    "ending in year": {"min": null, "max": "duration-end"},
    "during years": {"min": "duration-start", "max": "duration-end"},
  }[durationType];
  const getYearValue = (x) => x === null ? null : root.querySelector("." + x).value;
  const minYear = getYearValue(targets["min"]);
  const maxYear = getYearValue(targets["max"]);
  return new YearMatcher(minYear, maxYear);
}


export {UiEditorPresenter};
