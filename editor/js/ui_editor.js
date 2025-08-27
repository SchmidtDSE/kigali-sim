/**
 * Presenters and logic for the UI-based authoring experience.
 *
 * @license BSD, see LICENSE.md.
 */
import {EngineNumber} from "engine_number";
import {YearMatcher} from "year_matcher";

import {
  Application,
  Command,
  DefinitionalStanza,
  LimitCommand,
  Program,
  RechargeCommand,
  RecycleCommand,
  ReplaceCommand,
  SimulationScenario,
  SubstanceBuilder,
} from "ui_translator";

/**
 * Stream target selectors used throughout the application for updating dropdown states.
 * @constant {Array<string>}
 */
const STREAM_TARGET_SELECTORS = [
  ".set-target-input",
  ".change-target-input",
  ".limit-target-input",
  ".replace-target-input",
  ".displacing-input",
];

/**
 * Stream types that can be enabled/disabled based on substance configuration.
 * @constant {Array<string>}
 */
const ENABLEABLE_STREAMS = ["domestic", "import", "export"];

/**
 * Stream types that are always available regardless of substance configuration.
 * @constant {Array<string>}
 */
const ALWAYS_ON_STREAMS = ["sales", "equipment", "priorEquipment"];

/**
 * Updates the visibility of selector elements based on selected duration type.
 *
 * @param {HTMLElement} dateSelector - The date selector element to update.
 */
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

/**
 * Initializes a duration selector.
 *
 * @param {HTMLElement} newDiv - The new element to set up duration selector for.
 */
function setupDurationSelector(newDiv) {
  const dateSelectors = Array.of(...newDiv.querySelectorAll(".duration-subcomponent"));
  dateSelectors.forEach((dateSelector) => {
    dateSelector.addEventListener("change", (event) => {
      updateDurationSelector(dateSelector);
    });

    updateDurationSelector(dateSelector);
  });
}

/**
 * Build a function which sets up a list button with add/delete functionality.
 *
 * @param {Function} postCallback - Function to call after each list item UI is
 *     initialized or removed. If not given, will use a no-op.
 */
function buildSetupListButton(postCallback) {
  /**
   * Function which exeuctes on adding a new lits element.
   *
   * @param {HTMLElement} button - Button element to set up.
   * @param {HTMLElement} targetList - List element to add items to.
   * @param {string} templateId - ID of template to use for new items.
   * @param {Function} initUiCallback - Callback to invoke when item added.
   * @param {string} context - Context for stream detection ('consumption' or 'policy').
   */
  return (button, targetList, templateId, initUiCallback, context) => {
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
        postCallback();
      });

      initUiCallback(null, newDiv, context);

      setupDurationSelector(newDiv);

      if (postCallback !== undefined) {
        postCallback();
      }
    });
  };
}

/**
 * Sets a form field value with fallback to default.
 *
 * @param {HTMLElement} selection - Form field element.
 * @param {Object} source - Source object to get value from.
 * @param {*} defaultValue - Default value if source is null.
 * @param {Function} strategy - Function to extract value from source.
 */
function setFieldValue(selection, source, defaultValue, strategy) {
  const newValue = source === null ? null : strategy(source);
  const valueOrDefault = newValue === null ? defaultValue : newValue;
  selection.value = valueOrDefault;
}

/**
 * Gets raw value from a form field.
 *
 * @param {HTMLElement} selection - Form field to get value from.
 * @returns {string} The field's value.
 */
function getFieldValue(selection) {
  return selection.value;
}

/**
 * Gets sanitized value from a form field, removing quotes and commas.
 *
 * @param {HTMLElement} selection - Form field to get sanitized value from.
 * @returns {string} Sanitized field value.
 */
function getSanitizedFieldValue(selection) {
  const valueRaw = getFieldValue(selection);
  const clean = valueRaw.replaceAll('"', "").replaceAll(",", "");
  const trimmed = clean.trim();
  const guarded = trimmed === "" ? "Unnamed" : trimmed;
  return guarded;
}

/**
 * Sets up a list input with template-based items.
 *
 * @param {HTMLElement} listSelection - Container element for list.
 * @param {string} itemTemplate - HTML template for list items.
 * @param {Array} items - Array of items to populate list.
 * @param {Function} uiInit - Callback to initialize each item's UI.
 * @param {Function} removeCallback - Callback to invoke if item removed.
 */
function setListInput(listSelection, itemTemplate, items, uiInit, removeCallback) {
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
      removeCallback(item);
    });
  };
  items.forEach(addItem);
}

/**
 * Read the current items in a list.
 *
 * @param {HTMLElement} selection - The HTML element containing list items.
 * @param {Function} itemReadStrategy - A function to process each list item.
 * @returns {Array} An array of processed items returned by the strategy.
 */
function getListInput(selection, itemReadStrategy) {
  const dialogListItems = Array.of(...selection.querySelectorAll(".dialog-list-item"));
  return dialogListItems.map(itemReadStrategy);
}

/**
 * Sets a value/units pair for engine number inputs.
 *
 * @param {HTMLElement} valSelection - Value input element.
 * @param {HTMLElement} unitsSelection - Units select element.
 * @param {Object} source - Source object for values.
 * @param {EngineNumber} defaultValue - Default engine number.
 * @param {Function} strategy - Function to extract engine number from source.
 */
function setEngineNumberValue(valSelection, unitsSelection, source, defaultValue, strategy) {
  const newValue = source === null ? null : strategy(source);
  const valueOrDefault = newValue === null ? defaultValue : newValue;
  valSelection.value = valueOrDefault.getValue();
  unitsSelection.value = valueOrDefault.getUnits();
}

/**
 * Gets an engine number from value/units form fields.
 *
 * @param {HTMLElement} valSelection - Value input element.
 * @param {HTMLElement} unitsSelection - Units select element.
 * @returns {EngineNumber} Combined engine number object.
 */
function getEngineNumberValue(valSelection, unitsSelection) {
  const value = valSelection.value;
  const units = unitsSelection.value;
  return new EngineNumber(value, units);
}

/**
 * Sets the state of a duration selection UI widget.
 *
 * Set the duration for shown within a duration selection UI widget to match
 * that of a given command. If the command is null, it uses the default value.
 *
 * @param {HTMLElement} selection - The selection element containing duration-related inputs.
 * @param {Object} command - The command object from which the duration is extracted.
 * @param {YearMatcher} defaultVal - The default duration value if the command is null.
 * @param {boolean} initListeners - Flag indicating if new event listeners for
 *     element visibility should be added in response to changing duration type.
 */
function setDuring(selection, command, defaultVal, initListeners) {
  const effectiveVal = command === null ? defaultVal : command.getDuration();
  const durationTypeInput = selection.querySelector(".duration-type-input");

  const setElements = () => {
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
  };

  setElements();
  updateDurationSelector(selection);

  if (initListeners) {
    durationTypeInput.addEventListener("change", (event) => {
      updateDurationSelector(selection);
    });
  }
}

/**
 * Build a function which updates displays of command counts.
 *
 * @param dialog - Selection over the dialog in which the command count
 *     displays should be updated.
 * @returns Funciton which takes a string list selector and a string display
 *     selector. That function will put the count of commands found in the
 *     list selector into the display selector.
 */
function buildUpdateCount(dialog) {
  return (listSelector, displaySelector) => {
    const listSelection = dialog.querySelector(listSelector);
    const displaySelection = dialog.querySelector(displaySelector);

    const listItems = Array.of(...listSelection.querySelectorAll(".dialog-list-item"));
    const listCount = listItems.map((x) => 1).reduce((a, b) => a + b, 0);

    displaySelection.innerHTML = listCount;
  };
}

/**
 * Sets up event listeners for internal dialog links.
 *
 * This function adds click event listeners to dialog internal links that, when
 * clicked, will toggle the corresponding tab based on the link's href.
 *
 * @param {HTMLElement} root - The root element containing dialog.
 * @param {Object} tabs - Tabby object for managing tab toggling.
 */
function setupDialogInternalLinks(root, tabs) {
  const internalLinks = root.querySelectorAll(".dialog-internal-link");
  internalLinks.forEach((link) => {
    link.addEventListener("click", (event) => {
      event.preventDefault();
      const anchor = link.hash;
      tabs.toggle(anchor);
    });
  });
}

/**
 * Manages the UI for listing and editing applications.
 *
 * Manages the UI for listing and editing applications where these refer to
 * collections of substances based on use like commercial refrigeration.
 */
class ApplicationsListPresenter {
  /**
   * Creates a new ApplicationsListPresenter.
   *
   * @param {HTMLElement} root - Root DOM element for the applications list.
   * @param {Function} getCodeObj - Callback to get the current code object.
   * @param {Function} onCodeObjUpdate - Callback when code object is updated.
   */
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

  /**
   * Refreshes the applications list display.
   *
   * @param {Object} codeObj - Current code object.
   */
  refresh(codeObj) {
    const self = this;
    self._refreshList(codeObj);
  }

  /**
   * Updates the applications list UI with current data.
   *
   * @param {Object} codeObj - Current code object from which to extract
   *     applications.
   * @private
   */
  _refreshList(codeObj) {
    const self = this;
    const appNames = self._getAppNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li").data(appNames).enter().append("li");

    newItems.attr("aria-label", (x) => x);

    const buttonsPane = newItems.append("div").classed("list-buttons", true);

    newItems
      .append("div")
      .classed("list-label", true)
      .text((x) => x);

    buttonsPane
      .append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        self._showDialogFor(x);
      })
      .text("edit")
      .attr("aria-label", (x) => "edit " + x);

    buttonsPane.append("span").text(" | ");

    buttonsPane
      .append("a")
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

  /**
   * Sets up the dialog window for adding/editing applications.
   *
   * @private
   */
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

      const cleanName = (x) => x.replaceAll('"', "").replaceAll(",", "").trim();

      const nameInput = self._dialog.querySelector(".edit-application-name-input");
      const newNameUnguarded = cleanName(nameInput.value);

      const subnameInput = self._dialog.querySelector(".edit-application-subname-input");
      const newSubnameUnguarded = cleanName(subnameInput.value);
      const subnameEmpty = newSubnameUnguarded === "";

      const getEffectiveName = () => {
        if (subnameEmpty) {
          return newNameUnguarded;
        } else {
          return newNameUnguarded + " - " + newSubnameUnguarded;
        }
      };

      const effectiveName = getEffectiveName();
      const newName = effectiveName === "" ? "Unnamed" : effectiveName;

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

  /**
   * Shows the dialog for adding or editing an application.
   *
   * @param {string|null} name - Name of application to edit. Pass null if this
   *     is for a new application.
   * @private
   */
  _showDialogFor(name) {
    const self = this;
    self._editingName = name;

    if (name === null) {
      self._dialog.querySelector(".edit-application-name-input").value = "";
      self._dialog.querySelector(".edit-application-subname-input").value = "";
      self._dialog.querySelector(".action-title").innerHTML = "Add";
    } else {
      const nameComponents = name.split(" - ");
      const displayName = nameComponents[0];
      const subname = nameComponents.slice(1).join(" - ");
      self._dialog.querySelector(".edit-application-name-input").value = displayName;
      self._dialog.querySelector(".edit-application-subname-input").value = subname;
      self._dialog.querySelector(".action-title").innerHTML = "Edit";
    }

    self._dialog.showModal();
  }

  /**
   * Gets list of all application names.
   *
   * @returns {string[]} Array of application names.
   * @private
   */
  _getAppNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const applications = codeObj.getApplications();
    const appNames = applications.map((x) => x.getName());
    return appNames;
  }
}

/**
 * Manages the UI for displaying reminders about current substance and application.
 *
 * Displays the current substance and application being edited in the UI,
 * updating these reminders when selections change. Also provides links to
 * return to editing the base properties.
 */
class ReminderPresenter {
  /**
   * Creates a new ReminderPresenter.
   *
   * @param {HTMLElement} root - Root DOM element for the reminder UI.
   * @param {string} appInputSelector - CSS selector for application input element.
   * @param {string} substanceInputSelector - CSS selector for substance input element.
   * @param {string} baseTabSelector - CSS selector for base tab element.
   */
  constructor(root, appInputSelector, substanceInputSelector, baseTabSelector, tabs) {
    const self = this;

    self._root = root;
    self._appInputSelector = appInputSelector;
    self._substanceInputSelector = substanceInputSelector;
    self._baseTabSelector = baseTabSelector;
    self._tabs = tabs;

    self._setupEvents();
  }

  /**
   * Update the reminder UI elements with the current application and substance values.
   *
   * This function updates the innerHTML of reminder elements to display
   * the current values for application and substance. It ensures that changes
   * in the inputs are reflected in the corresponding reminder display fields.
   */
  update() {
    const self = this;

    const applicationInput = self._root.querySelector(self._appInputSelector);
    const substanceInput = self._root.querySelector(self._substanceInputSelector);

    const substanceDisplays = self._root.querySelectorAll(".reminder-substance");
    const appDisplays = self._root.querySelectorAll(".reminder-app");
    const embedSubstanceDisplays = self._root.querySelectorAll(".embed-substance-label");
    const embedAppDisplays = self._root.querySelectorAll(".embed-app-label");

    const updateSubstance = (display) => {
      display.innerHTML = "";
      const textNode = document.createTextNode(substanceInput.value);
      display.appendChild(textNode);
    };

    const updateApp = (display) => {
      display.innerHTML = "";
      const textNode = document.createTextNode(applicationInput.value);
      display.appendChild(textNode);
    };

    appDisplays.forEach(updateApp);
    embedAppDisplays.forEach(updateApp);

    substanceDisplays.forEach(updateSubstance);
    embedSubstanceDisplays.forEach(updateSubstance);
  }

  /**
   * Setup event listeners for the reminder UI.
   *
   * This method binds change events for the application and substance inputs that update their
   * respective displays within the reminder interface. It also binds click events to the edit
   * reminder links, managing the tab switch to the base tab selector when activated.
   *
   * @private
   */
  _setupEvents() {
    const self = this;

    const applicationInput = self._root.querySelector(self._appInputSelector);
    const substanceInput = self._root.querySelector(self._substanceInputSelector);

    substanceInput.addEventListener("change", () => self.update());
    applicationInput.addEventListener("change", () => self.update());
    self.update();

    const links = self._root.querySelectorAll(".edit-reminder-link");
    links.forEach((link) => {
      link.addEventListener("click", (event) => {
        event.preventDefault();
        self._tabs.toggle(self._baseTabSelector);
      });
    });
  }
}

/**
 * Manages the UI for listing and editing consumption logic.
 *
 * Manages the UI for listing and editing consumption logic where substances
 * are consumed for different applications.
 */
class ConsumptionListPresenter {
  /**
   * Creates a new ConsumptionListPresenter.
   *
   * @param {HTMLElement} root - The root DOM element for the consumption list.
   * @param {Function} getCodeObj - Callback to get the current code object.
   * @param {Function} onCodeObjUpdate - Callback when the code object is
   *     updated.
   */
  constructor(root, getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._root = root;
    self._dialog = self._root.querySelector(".dialog");
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._editingName = null;
    self._streamUpdater = new StreamSelectionAvailabilityUpdater(
      self._dialog,
      "consumption",
    );
    self._setupDialog();
    self.refresh();
  }

  /**
   * Visually and functionally enables the consumption list interface.
   */
  enable() {
    const self = this;
    self._root.classList.remove("inactive");
  }

  /**
   * Visually and functionally disables the consumption list interface.
   */
  disable() {
    const self = this;
    self._root.classList.add("inactive");
  }

  /**
   * Updates the consumption list display with new data.
   *
   * @param {Object} codeObj - Current code object to display.
   */
  refresh(codeObj) {
    const self = this;
    self._refreshList(codeObj);
  }

  /**
   * Refreshes the consumption list display.
   *
   * @param {Object} codeObj - The current code object.
   * @private
   */
  _refreshList(codeObj) {
    const self = this;
    const consumptionNames = self._getConsumptionNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li").data(consumptionNames).enter().append("li");

    newItems.attr("aria-label", (x) => x);

    const buttonsPane = newItems.append("div").classed("list-buttons", true);

    newItems
      .append("div")
      .classed("list-label", true)
      .text((x) => x);

    buttonsPane
      .append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        self._showDialogFor(x);
      })
      .text("edit")
      .attr("aria-label", (x) => "edit " + x);

    buttonsPane.append("span").text(" | ");

    buttonsPane
      .append("a")
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

  /**
   * Sets up the dialog for adding/editing consumption records.
   *
   * Sets up the dialog for adding/editing consumption records, initializing
   * tabs and event handlers.
   *
   * @private
   */
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

    const updateHints = () => {
      const embedReminders = self._root.querySelectorAll(".embed-reminder");
      embedReminders.forEach((reminder) => {
        reminder.style.display = "none";
      });

      self._updateCounts();
    };
    const setupListButton = buildSetupListButton(updateHints);

    const addLevelButton = self._root.querySelector(".add-start-button");
    const levelList = self._root.querySelector(".level-list");
    setupListButton(
      addLevelButton,
      levelList,
      "set-command-template",
      (item, root, context) => {
        initSetCommandUi(item, root, self._getCodeObj(), context, self._streamUpdater);
      },
      "consumption",
    );

    const addChangeButton = self._root.querySelector(".add-change-button");
    const changeList = self._root.querySelector(".change-list");
    setupListButton(
      addChangeButton,
      changeList,
      "change-command-template",
      (item, root, context) => {
        initChangeCommandUi(item, root, self._getCodeObj(), context, self._streamUpdater);
      },
      "consumption",
    );

    const addLimitButton = self._root.querySelector(".add-limit-button");
    const limitList = self._root.querySelector(".limit-list");
    setupListButton(
      addLimitButton,
      limitList,
      "limit-command-template",
      (item, root, context) => {
        initLimitCommandUi(item, root, self._getCodeObj(), context, self._streamUpdater);
      },
      "consumption",
    );

    const addRechargeButton = self._root.querySelector(".add-recharge-button");
    const rechargeList = self._root.querySelector(".recharge-list");
    setupListButton(addRechargeButton, rechargeList, "recharge-command-template", (item, root) =>
      initRechargeCommandUi(item, root, self._getCodeObj()),
    );

    self._reminderPresenter = new ReminderPresenter(
      self._dialog,
      ".edit-consumption-application-input",
      ".edit-consumption-substance-input",
      "#consumption-general",
      self._tabs,
    );

    setupDialogInternalLinks(self._root, self._tabs);

    const enableImport = self._dialog.querySelector(".enable-import-checkbox");
    const enableDomestic = self._dialog.querySelector(".enable-domestic-checkbox");
    const enableExport = self._dialog.querySelector(".enable-export-checkbox");

    enableImport.addEventListener("change", () => self._updateSource());
    enableDomestic.addEventListener("change", () => self._updateSource());
    enableExport.addEventListener("change", () => self._updateSource());
  }

  /**
   * Shows the dialog for editing a consumption record.
   *
   * @param {string|null} name - Name of consumption to edit. Pass null for a
   *     new record.
   * @private
   */
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
        return {obj: null, application: ""};
      }
      const objIdentifierRegex = /\"([^\"]+)\" for \"([^\"]+)\"/;
      const match = name.match(objIdentifierRegex);
      const substance = match[1];
      const application = match[2];
      const substanceObj = codeObj.getApplication(application).getSubstance(substance);
      return {obj: substanceObj, application: application};
    };

    const objToShowInfo = getObjToShow();
    const objToShow = objToShowInfo["obj"];
    const applicationName = objToShowInfo["application"];

    const applicationNames = self
      ._getCodeObj()
      .getApplications()
      .map((x) => x.getName());

    const applicationSelect = self._dialog.querySelector(".application-select");
    d3.select(applicationSelect)
      .html("")
      .selectAll("option")
      .data(applicationNames)
      .enter()
      .append("option")
      .attr("value", (x) => x)
      .text((x) => x);

    // Split substance name to separate substance and equipment model
    const getSubstanceAndEquipment = (fullSubstanceName) => {
      if (!fullSubstanceName) {
        return {substance: "", equipment: ""};
      }
      const parts = fullSubstanceName.split(" - ");
      return {
        substance: parts[0] || "",
        equipment: parts.slice(1).join(" - ") || "",
      };
    };

    const substanceAndEquipment = objToShow ?
      getSubstanceAndEquipment(objToShow.getName()) :
      {substance: "", equipment: ""};

    setFieldValue(
      self._dialog.querySelector(".edit-consumption-substance-input"),
      objToShow,
      "",
      (x) => substanceAndEquipment.substance,
    );

    setFieldValue(
      self._dialog.querySelector(".edit-consumption-application-input"),
      objToShow,
      applicationNames[0],
      (x) => applicationName,
    );

    setFieldValue(
      self._dialog.querySelector(".edit-consumption-equipment-input"),
      objToShow,
      "",
      (x) => substanceAndEquipment.equipment,
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-ghg-input"),
      self._dialog.querySelector(".edit-consumption-ghg-units-input"),
      objToShow,
      new EngineNumber(1, "kgCO2e / kg"),
      (x) => (x.getEqualsGhg() ? x.getEqualsGhg().getValue() : null),
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-energy-input"),
      self._dialog.querySelector(".edit-consumption-energy-units-input"),
      objToShow,
      new EngineNumber(1, "kwh / unit"),
      (x) => (x.getEqualsKwh() ? x.getEqualsKwh().getValue() : null),
    );

    const getValueOrDefault = (target, fallback) => target === null ? fallback : target.getValue();

    const domesticFallback = new EngineNumber(1, "kg / unit");
    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-domestic-input"),
      self._dialog.querySelector(".initial-charge-domestic-units-input"),
      objToShow,
      domesticFallback,
      (x) => getValueOrDefault(x.getInitialCharge("domestic"), domesticFallback),
    );

    const importFallback = new EngineNumber(2, "kg / unit");
    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-import-input"),
      self._dialog.querySelector(".initial-charge-import-units-input"),
      objToShow,
      importFallback,
      (x) => getValueOrDefault(x.getInitialCharge("import"), importFallback),
    );

    const exportFallback = new EngineNumber(1, "kg / unit");
    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-export-input"),
      self._dialog.querySelector(".initial-charge-export-units-input"),
      objToShow,
      exportFallback,
      (x) => getValueOrDefault(x.getInitialCharge("export"), exportFallback),
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-retirement-input"),
      self._dialog.querySelector(".retirement-units-input"),
      objToShow,
      new EngineNumber(5, "% / year"),
      (x) => x.getRetire() ? x.getRetire().getValue() : null,
    );

    const removeCallback = () => self._updateCounts();

    setListInput(
      self._dialog.querySelector(".level-list"),
      document.getElementById("set-command-template").innerHTML,
      objToShow === null ? [] : objToShow.getSetVals(),
      (item, root) => initSetCommandUi(
        item, root, self._getCodeObj(), "consumption", self._streamUpdater,
      ),
      removeCallback,
    );

    setListInput(
      self._dialog.querySelector(".change-list"),
      document.getElementById("change-command-template").innerHTML,
      objToShow === null ? [] : objToShow.getChanges(),
      (item, root) => initChangeCommandUi(
        item, root, self._getCodeObj(), "consumption", self._streamUpdater,
      ),
      removeCallback,
    );

    setListInput(
      self._dialog.querySelector(".limit-list"),
      document.getElementById("limit-command-template").innerHTML,
      objToShow === null ? [] : objToShow.getLimits(),
      (item, root) => initLimitCommandUi(
        item,
        root,
        self._getCodeObj(),
        "consumption",
        self._streamUpdater,
      ),
      removeCallback,
    );

    setListInput(
      self._dialog.querySelector(".recharge-list"),
      document.getElementById("recharge-command-template").innerHTML,
      objToShow === null ? [] : objToShow.getRecharges(),
      (item, root) => initRechargeCommandUi(item, root, self._getCodeObj()),
      removeCallback,
    );

    // Set enable checkboxes based on existing substance data
    const enableImport = self._dialog.querySelector(".enable-import-checkbox");
    const enableDomestic = self._dialog.querySelector(".enable-domestic-checkbox");
    const enableExport = self._dialog.querySelector(".enable-export-checkbox");

    if (objToShow !== null) {
      // Check if the substance has enable commands
      const enableCommands = objToShow.getEnables();

      enableDomestic.checked = enableCommands.some((cmd) => cmd.getTarget() === "domestic");
      enableImport.checked = enableCommands.some((cmd) => cmd.getTarget() === "import");
      enableExport.checked = enableCommands.some((cmd) => cmd.getTarget() === "export");
    } else {
      // For new substances, default all enable checkboxes to unchecked
      enableDomestic.checked = false;
      enableImport.checked = false;
      enableExport.checked = false;
    }

    self._updateEnableCheckboxes();
    self._updateSource();

    self._dialog.showModal();
    self._reminderPresenter.update();
    self._updateCounts();
  }

  /**
   * Update the source inputs value and visibility.
   */
  _updateSource() {
    const self = this;

    const enableImport = self._dialog.querySelector(".enable-import-checkbox");
    const enableDomestic = self._dialog.querySelector(".enable-domestic-checkbox");
    const enableExport = self._dialog.querySelector(".enable-export-checkbox");

    const domesticInput = self._dialog.querySelector(
      ".edit-consumption-initial-charge-domestic-input",
    );
    const domesticInputOuter = self._dialog.querySelector(
      ".edit-consumption-initial-charge-domestic-input-outer",
    );
    const importInput = self._dialog.querySelector(".edit-consumption-initial-charge-import-input");
    const importInputOuter = self._dialog.querySelector(
      ".edit-consumption-initial-charge-import-input-outer",
    );
    const exportInput = self._dialog.querySelector(".edit-consumption-initial-charge-export-input");
    const exportInputOuter = self._dialog.querySelector(
      ".edit-consumption-initial-charge-export-input-outer",
    );

    // Show/hide fields based on checkbox states and set default values
    if (enableDomestic.checked) {
      domesticInputOuter.style.display = "block";
      // If enabling and current value is 0, set to 1 kg/unit
      if (domesticInput.value === "0" || domesticInput.value === "") {
        domesticInput.value = 1;
        const domesticUnitsInput = self._dialog.querySelector(
          ".initial-charge-domestic-units-input",
        );
        if (domesticUnitsInput) {
          domesticUnitsInput.value = "kg / unit";
        }
      }
    } else {
      domesticInputOuter.style.display = "none";
      domesticInput.value = 0;
    }

    if (enableImport.checked) {
      importInputOuter.style.display = "block";
      // If enabling and current value is 0, set to 1 kg/unit
      if (importInput.value === "0" || importInput.value === "") {
        importInput.value = 1;
        const importUnitsInput = self._dialog.querySelector(".initial-charge-import-units-input");
        if (importUnitsInput) {
          importUnitsInput.value = "kg / unit";
        }
      }
    } else {
      importInputOuter.style.display = "none";
      importInput.value = 0;
    }

    if (enableExport.checked) {
      exportInputOuter.style.display = "block";
      // If enabling and current value is 0, set to 1 kg/unit
      if (exportInput.value === "0" || exportInput.value === "") {
        exportInput.value = 1;
        const exportUnitsInput = self._dialog.querySelector(".initial-charge-export-units-input");
        if (exportUnitsInput) {
          exportUnitsInput.value = "kg / unit";
        }
      }
    } else {
      exportInputOuter.style.display = "none";
      exportInput.value = 0;
    }

    // Update stream target dropdowns in all open dialogs when checkbox states change
    const substanceInput = self._dialog.querySelector(".edit-consumption-substance-input");
    const currentSubstanceName = substanceInput.value;
    // Create a temporary substance object with current checkbox states to get enabled streams
    const tempEnabledStreams = [];
    if (enableDomestic.checked) {
      tempEnabledStreams.push("domestic");
    }
    if (enableImport.checked) {
      tempEnabledStreams.push("import");
    }
    if (enableExport.checked) {
      tempEnabledStreams.push("export");
    }

    // Update all open dialogs with the new stream states
    self._streamUpdater.updateAllStreamTargetDropdowns(tempEnabledStreams);
  }

  /**
   * Update the enable checkboxes based on current field values.
   */
  _updateEnableCheckboxes() {
    const self = this;

    const enableImport = self._dialog.querySelector(".enable-import-checkbox");
    const enableDomestic = self._dialog.querySelector(".enable-domestic-checkbox");
    const enableExport = self._dialog.querySelector(".enable-export-checkbox");

    const domesticInput = self._dialog.querySelector(
      ".edit-consumption-initial-charge-domestic-input",
    );
    const importInput = self._dialog.querySelector(".edit-consumption-initial-charge-import-input");
    const exportInput = self._dialog.querySelector(".edit-consumption-initial-charge-export-input");

    // For new substances, checkboxes should remain as explicitly set
    // (either unchecked by default or checked by user interaction)
    // Don't automatically check based on field values
  }

  /**
   * Gets list of all consuming substances.
   *
   * @returns {string[]} Array of substances.
   * @private
   */
  _getConsumptionNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const applications = codeObj.getApplications();
    const consumptionsNested = applications.map((x) => {
      const appName = x.getName();
      const substances = x.getSubstances();
      return substances.map((substance) => {
        const metadata = substance.getMeta(appName);
        return metadata.getKey();
      });
    });
    const consumptions = consumptionsNested.flat();
    return consumptions;
  }

  /**
   * Saves the current consumption data.
   *
   * @private
   */
  _save() {
    const self = this;
    const substance = self._parseObj();

    const codeObj = self._getCodeObj();

    if (self._editingName === null) {
      const applicationName = getFieldValue(
        self._dialog.querySelector(".edit-consumption-application-input"),
      );
      codeObj.insertSubstance(null, applicationName, null, substance);
    } else {
      const objIdentifierRegex = /\"([^\"]+)\" for \"([^\"]+)\"/;
      const match = self._editingName.match(objIdentifierRegex);
      const oldSubstanceName = match[1];
      const oldApplicationName = match[2];
      const newApplicationName = getFieldValue(
        self._dialog.querySelector(".edit-consumption-application-input"),
      );
      const newSubstanceName = substance.getName();

      // Check if we need to rename substance in policies
      const appNameSame = oldApplicationName === newApplicationName;
      const substanceNameChanged = oldSubstanceName !== newSubstanceName;

      if (appNameSame && substanceNameChanged) {
        // Substance name changed within same application - update policies
        codeObj.renameSubstanceInApplication(
          oldApplicationName,
          oldSubstanceName,
          newSubstanceName,
        );
        // Still need to update the substance definition itself
        codeObj.insertSubstance(
          oldApplicationName,
          newApplicationName,
          newSubstanceName,
          substance,
        );
      } else {
        // Application changed or no substance name change - use standard insert
        codeObj.insertSubstance(
          oldApplicationName,
          newApplicationName,
          oldSubstanceName,
          substance,
        );
      }
    }

    self._onCodeObjUpdate(codeObj);
  }

  /**
   * Parses the dialog form data into a substance object.
   *
   * @returns {Object} The parsed substance object.
   * @private
   */
  _parseObj() {
    const self = this;

    // Helper function to combine substance and equipment model
    const getEffectiveSubstanceName = () => {
      const baseSubstance = getSanitizedFieldValue(
        self._dialog.querySelector(".edit-consumption-substance-input"),
      );
      const equipmentModel = getFieldValue(
        self._dialog.querySelector(".edit-consumption-equipment-input"),
      );

      if (equipmentModel && equipmentModel.trim() !== "") {
        return baseSubstance + " - " + equipmentModel.trim();
      }
      return baseSubstance;
    };

    const substanceName = getEffectiveSubstanceName();

    const substanceBuilder = new SubstanceBuilder(substanceName, false);

    // Add enable commands based on checkbox states
    const enableImport = self._dialog.querySelector(".enable-import-checkbox");
    const enableDomestic = self._dialog.querySelector(".enable-domestic-checkbox");
    const enableExport = self._dialog.querySelector(".enable-export-checkbox");

    if (enableDomestic.checked) {
      substanceBuilder.addCommand(new Command("enable", "domestic", null, null));
    }
    if (enableImport.checked) {
      substanceBuilder.addCommand(new Command("enable", "import", null, null));
    }
    if (enableExport.checked) {
      substanceBuilder.addCommand(new Command("enable", "export", null, null));
    }

    const ghgValue = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-ghg-input"),
      self._dialog.querySelector(".edit-consumption-ghg-units-input"),
    );
    substanceBuilder.addCommand(new Command("equals", null, ghgValue, null));

    const energyValue = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-energy-input"),
      self._dialog.querySelector(".edit-consumption-energy-units-input"),
    );
    substanceBuilder.addCommand(new Command("equals", null, energyValue, null));

    const initialChargeDomestic = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-domestic-input"),
      self._dialog.querySelector(".initial-charge-domestic-units-input"),
    );
    const initialChargeDomesticCommand = new Command(
      "initial charge",
      "domestic",
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

    const initialChargeExport = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-export-input"),
      self._dialog.querySelector(".initial-charge-export-units-input"),
    );
    const initialChargeExportCommand = new Command(
      "initial charge",
      "export",
      initialChargeExport,
      null,
    );
    substanceBuilder.addCommand(initialChargeExportCommand);

    const retirement = getEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-retirement-input"),
      self._dialog.querySelector(".retirement-units-input"),
    );
    const retireCommand = new Command("retire", null, retirement, null);
    substanceBuilder.addCommand(retireCommand);


    const levels = getListInput(self._dialog.querySelector(".level-list"), readSetCommandUi);
    levels.forEach((x) => substanceBuilder.addCommand(x));

    const changes = getListInput(self._dialog.querySelector(".change-list"), readChangeCommandUi);
    changes.forEach((x) => substanceBuilder.addCommand(x));

    const limits = getListInput(self._dialog.querySelector(".limit-list"), readLimitCommandUi);
    limits.forEach((x) => substanceBuilder.addCommand(x));

    const recharges = getListInput(
      self._dialog.querySelector(".recharge-list"),
      readRechargeCommandUi,
    );
    recharges.forEach((x) => substanceBuilder.addCommand(x));

    return substanceBuilder.build(true);
  }

  /**
   * Updates the UI to display the count of commands in each list.
   *
   * @private
   */
  _updateCounts() {
    const self = this;

    const updateCount = buildUpdateCount(self._dialog);
    updateCount(".level-list", "#consumption-set-count");
    updateCount(".change-list", "#consumption-change-count");
    updateCount(".limit-list", "#consumption-limit-count");
    updateCount(".recharge-list", "#consumption-servicing-count");
  }
}

/**
 * Manages the UI for listing and editing policies.
 *
 * Manages the UI for listing and editing policies that define recycling,
 * replacement, level changes, limits, etc on substances.
 */
class PolicyListPresenter {
  /**
   * Creates a new PolicyListPresenter.
   *
   * @param {HTMLElement} root - The root DOM element for the policy list.
   * @param {Function} getCodeObj - Callback to get the current code object.
   * @param {Function} onCodeObjUpdate - Callback when the code object is updated.
   */
  constructor(root, getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._root = root;
    self._dialog = self._root.querySelector(".dialog");
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._editingName = null;
    self._streamUpdater = new StreamSelectionAvailabilityUpdater(
      self._dialog,
      "policy",
    );
    self._setupDialog();
    self.refresh();
  }

  /**
   * Visually and functionally enables the policy list interface.
   */
  enable() {
    const self = this;
    self._root.classList.remove("inactive");
  }

  /**
   * Visually and functionally sisables the policy list interface.
   */
  disable() {
    const self = this;
    self._root.classList.add("inactive");
  }

  /**
   * Updates the policy list to visualize new policies.
   *
   * @param {Object} codeObj - Current code object to display.
   */
  refresh(codeObj) {
    const self = this;
    self._refreshList(codeObj);
  }

  /**
   * Updates the policy list UI with current logic.
   *
   * @param {Object} codeObj - Current code object from which to extract policies.
   * @private
   */
  _refreshList(codeObj) {
    const self = this;
    const policyNames = self._getPolicyNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li").data(policyNames).enter().append("li");

    newItems.attr("aria-label", (x) => x);

    const buttonsPane = newItems.append("div").classed("list-buttons", true);

    newItems
      .append("div")
      .classed("list-label", true)
      .text((x) => x);

    buttonsPane
      .append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        self._showDialogFor(x);
      })
      .text("edit")
      .attr("aria-label", (x) => "edit " + x);

    buttonsPane.append("span").text(" | ");

    buttonsPane
      .append("a")
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

  /**
   * Sets up the dialog for adding/editing policies.
   *
   * Sets up the dialog for adding/editing policies, initializing tabs and
   * event handlers for recycling, replacement, level changes, limits, etc.
   *
   * @private
   */
  _setupDialog() {
    const self = this;

    self._tabs = new Tabby("#" + self._dialog.querySelector(".tabs").id);

    self._reminderPresenter = new ReminderPresenter(
      self._dialog,
      ".edit-policy-application-input",
      ".edit-policy-substance-input",
      "#policy-general",
      self._tabs,
    );

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

    const updateHints = () => {
      self._reminderPresenter.update();
      self._updateCounts();
    };
    const setupListButton = buildSetupListButton(updateHints);

    // Add event listener for substance selection changes to update stream target dropdowns
    const substanceInput = self._dialog.querySelector(".edit-policy-substance-input");
    substanceInput.addEventListener("change", () => {
      const selectedSubstance = substanceInput.value;
      const codeObj = self._getCodeObj();
      const enabledStreams = self._streamUpdater._getEnabledStreamsForSubstance(
        codeObj, selectedSubstance,
      );

      // Update all stream target dropdowns in the policy dialog
      self._streamUpdater.updateAllStreamTargetDropdowns(enabledStreams);
    });

    const addRecyclingButton = self._root.querySelector(".add-recycling-button");
    const recyclingList = self._root.querySelector(".recycling-list");
    setupListButton(
      addRecyclingButton,
      recyclingList,
      "recycle-command-template",
      (item, root, context) => {
        initRecycleCommandUi(item, root, self._getCodeObj(), context, self._streamUpdater);
      },
      "policy",
    );

    const addReplaceButton = self._root.querySelector(".add-replace-button");
    const replaceList = self._root.querySelector(".replace-list");
    setupListButton(
      addReplaceButton,
      replaceList,
      "replace-command-template",
      (item, root, context) => {
        initReplaceCommandUi(item, root, self._getCodeObj(), context, self._streamUpdater);
      },
      "policy",
    );

    const addLevelButton = self._root.querySelector(".add-level-button");
    const levelList = self._root.querySelector(".level-list");
    setupListButton(
      addLevelButton,
      levelList,
      "set-command-template",
      (item, root, context) => {
        initSetCommandUi(item, root, self._getCodeObj(), context, self._streamUpdater);
      },
      "policy",
    );

    const addChangeButton = self._root.querySelector(".add-change-button");
    const changeList = self._root.querySelector(".change-list");
    setupListButton(
      addChangeButton,
      changeList,
      "change-command-template",
      (item, root, context) => {
        initChangeCommandUi(item, root, self._getCodeObj(), context, self._streamUpdater);
      },
      "policy",
    );

    const addLimitButton = self._root.querySelector(".add-limit-button");
    const limitList = self._root.querySelector(".limit-list");
    setupListButton(
      addLimitButton,
      limitList,
      "limit-command-template",
      (item, root, context) => {
        initLimitCommandUi(item, root, self._getCodeObj(), context, self._streamUpdater);
      },
      "policy",
    );

    setupDialogInternalLinks(self._root, self._tabs);
  }

  /**
   * Shows the dialog for adding or editing a policy.
   *
   * @param {string|null} name - Name of policy to edit, or null for new policy.
   * @private
   */
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

    setFieldValue(self._dialog.querySelector(".edit-policy-name-input"), targetPolicy, "", (x) =>
      x.getName(),
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
    const substanceNamesDup = substances.map((x) => x.getName());
    const substanceNames = Array.of(...new Set(substanceNamesDup));
    const substanceSelect = d3.select(self._dialog.querySelector(".substances-select"));
    const substanceName = targetSubstance === null ? "" : targetSubstance.getName();
    substanceSelect.html("");
    substanceSelect
      .selectAll("option")
      .data(substanceNames)
      .enter()
      .append("option")
      .attr("value", (x) => x)
      .text((x) => x)
      .property("selected", (x) => x === substanceName);

    const removeCallback = () => self._updateCounts();

    setListInput(
      self._dialog.querySelector(".recycling-list"),
      document.getElementById("recycle-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getRecycles(),
      (item, root) => initRecycleCommandUi(
        item,
        root,
        self._getCodeObj(),
        "policy",
        self._streamUpdater,
      ),
      removeCallback,
    );

    setListInput(
      self._dialog.querySelector(".replace-list"),
      document.getElementById("replace-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getReplaces(),
      (item, root) => initReplaceCommandUi(
        item,
        root,
        self._getCodeObj(),
        "policy",
        self._streamUpdater,
      ),
      removeCallback,
    );

    setListInput(
      self._dialog.querySelector(".level-list"),
      document.getElementById("set-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getSetVals(),
      (item, root) => initSetCommandUi(
        item, root, self._getCodeObj(), "policy", self._streamUpdater,
      ),
      removeCallback,
    );

    setListInput(
      self._dialog.querySelector(".change-list"),
      document.getElementById("change-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getChanges(),
      (item, root) => initChangeCommandUi(
        item, root, self._getCodeObj(), "policy", self._streamUpdater,
      ),
      removeCallback,
    );

    setListInput(
      self._dialog.querySelector(".limit-list"),
      document.getElementById("limit-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getLimits(),
      (item, root) => initLimitCommandUi(
        item,
        root,
        self._getCodeObj(),
        "policy",
        self._streamUpdater,
      ),
      removeCallback,
    );

    self._dialog.showModal();
    self._reminderPresenter.update();
    self._updateCounts();

    // Initialize stream target dropdown states based on selected substance
    const substanceInput = self._dialog.querySelector(".edit-policy-substance-input");
    const selectedSubstance = substanceInput.value;
    if (selectedSubstance) {
      const enabledStreams = self._streamUpdater._getEnabledStreamsForSubstance(
        self._getCodeObj(), selectedSubstance,
      );

      // Update all stream target dropdowns in the policy dialog
      self._streamUpdater.updateAllStreamTargetDropdowns(enabledStreams);
    }
  }

  /**
   * Gets list of all policy names.
   *
   * @returns {string[]} Array of policy names.
   * @private
   */
  _getPolicyNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const policies = codeObj.getPolicies();
    return policies.map((x) => x.getName());
  }

  /**
   * Parses the dialog form data into a policy object.
   *
   * @returns {Object} The parsed policy object.
   * @private
   */
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

    const levels = getListInput(self._dialog.querySelector(".level-list"), readSetCommandUi);
    levels.forEach((command) => builder.addCommand(command));

    const changes = getListInput(self._dialog.querySelector(".change-list"), readChangeCommandUi);
    changes.forEach((command) => builder.addCommand(command));

    const limits = getListInput(self._dialog.querySelector(".limit-list"), readLimitCommandUi);
    limits.forEach((command) => builder.addCommand(command));

    const substance = builder.build(true);
    const application = new Application(applicationName, [substance], true, true);
    const policy = new DefinitionalStanza(policyName, [application], true, true);

    return policy;
  }

  /**
   * Saves the current policy data.
   *
   * @private
   */
  _save() {
    const self = this;
    const policy = self._parseObj();
    const codeObj = self._getCodeObj();
    codeObj.insertPolicy(self._editingName, policy);
    self._onCodeObjUpdate(codeObj);
  }

  /**
   * Updates the UI to display the count of commands in each list.
   *
   * @private
   */
  _updateCounts() {
    const self = this;
    const updateCount = buildUpdateCount(self._dialog);
    updateCount(".recycling-list", "#policy-recycle-count");
    updateCount(".replace-list", "#policy-replace-count");
    updateCount(".level-list", "#policy-set-count");
    updateCount(".change-list", "#policy-change-count");
    updateCount(".limit-list", "#policy-limit-count");
  }
}

/**
 * Manages the UI for listing and editing simulation situations.
 *
 * Manages the UI for listing and editing simulations. These are individual
 * situations which include their names, start and end years, and policies.
 */
class SimulationListPresenter {
  /**
   * Creates a new SimulationListPresenter.
   *
   * @param {HTMLElement} root - The root DOM element for the simulation list.
   * @param {Function} getCodeObj - Callback to get the current code object.
   * @param {Function} onCodeObjUpdate - Callback when the code object is updated.
   */
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

  /**
   * Visually and functionally enables the simulation list interface.
   */
  enable() {
    const self = this;
    self._root.classList.remove("inactive");
  }

  /**
   * Visually and functionally disables the simulation list interface.
   */
  disable() {
    const self = this;
    self._root.classList.add("inactive");
  }

  /**
   * Refreshes the simulation list display.
   *
   * @param {Object} codeObj - Current code object to display.
   */
  refresh(codeObj) {
    const self = this;
    self._refreshList(codeObj);
  }

  /**
   * Refreshes the simulation list display.
   *
   * @param {Object} codeObj - The current code object.
   * @private
   */
  _refreshList(codeObj) {
    const self = this;
    const simulationNames = self._getSimulationNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li").data(simulationNames).enter().append("li");

    newItems.attr("aria-label", (x) => x);

    const buttonsPane = newItems.append("div").classed("list-buttons", true);

    newItems
      .append("div")
      .classed("list-label", true)
      .text((x) => x);

    buttonsPane
      .append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        self._showDialogFor(x);
      })
      .text("edit")
      .attr("aria-label", (x) => "edit " + x);

    buttonsPane.append("span").text(" | ");

    buttonsPane
      .append("a")
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

  /**
   * Sets up the dialog for adding/editing simulations.
   *
   * Sets up the dialog for adding/editing simulation situations which are
   * named combinations of stackable scenarios (policies) on top the baseline.
   *
   * @private
   */
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

  /**
   * Shows the dialog for adding or editing a simulation.
   *
   * @param {string|null} name - Name of simulation to edit. Pass null for new
   *     simulation.
   * @private
   */
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

    setFieldValue(self._dialog.querySelector(".edit-simulation-name-input"), scenario, "", (x) =>
      x.getName(),
    );

    setFieldValue(self._dialog.querySelector(".edit-simulation-start-input"), scenario, 1, (x) =>
      x.getYearStart(),
    );

    setFieldValue(self._dialog.querySelector(".edit-simulation-end-input"), scenario, 10, (x) =>
      x.getYearEnd(),
    );

    const policyNames = self
      ._getCodeObj()
      .getPolicies()
      .map((x) => x.getName());
    const newLabels = d3
      .select(self._dialog.querySelector(".policy-sim-list"))
      .html("")
      .selectAll(".policy-check-label")
      .data(policyNames)
      .enter()
      .append("div")
      .classed("policy-check-label", true)
      .append("label");

    newLabels
      .append("input")
      .attr("type", "checkbox")
      .classed("policy-check", true)
      .attr("value", (x) => x)
      .property("checked", (x) => policiesSelected.has(x));

    newLabels.append("span").text((x) => x);

    self._dialog.showModal();
  }

  /**
   * Gets list of all simulation names.
   *
   * @returns {string[]} Array of simulation names.
   * @private
   */
  _getSimulationNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const scenarios = codeObj.getScenarios();
    return scenarios.map((x) => x.getName());
  }

  /**
   * Saves the current simulation data.
   *
   * @private
   */
  _save() {
    const self = this;
    const scenario = self._parseObj();
    const codeObj = self._getCodeObj();
    codeObj.insertScenario(self._editingName, scenario);
    self._onCodeObjUpdate(codeObj);
  }

  /**
   * Parses the dialog form data into a simulation scenario object.
   *
   * @returns {Object} The parsed simulation scenario object.
   * @private
   */
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

    return new SimulationScenario(scenarioName, policyNamesSelected, start, end, true);
  }
}

/**
 * Manages the UI editor interface.
 *
 * Central presenter which coordinates between code editing and visual editing
 * interfaces, managing tabs and content display with their respective
 * sub-presenters.
 */
class UiEditorPresenter {
  /**
   * Creates a new UiEditorPresenter.
   *
   * @param {boolean} startOnCode - Whether to start in code view.
   * @param {HTMLElement} tabRoot - Root element for editor tabs.
   * @param {HTMLElement} contentsRoot - Root element for editor contents.
   * @param {Function} getCodeAsObj - Callback to get current code object.
   * @param {Function} onCodeObjUpdate - Callback when code object updates.
   * @param {Function} onTabChange - Callback when active tab changes.
   */
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

  /**
   * Shows the code editor interface.
   */
  showCode() {
    const self = this;
    self._tabs.toggle("#code-editor-pane");
  }

  /**
   * Show all sections in the UI editor as enabled.
   */
  enableAllSections() {
    const self = this;
    self._consumptionList.enable();
    self._policyList.enable();
    self._simulationList.enable();
  }

  /**
   * Refreshes the UI with new code object data.
   *
   * @param {Object} codeObj - The new code object to display.
   */
  refresh(codeObj) {
    const self = this;

    if (codeObj === null) {
      self._initCodeObj();
    } else {
      self._codeObj = codeObj;
    }

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

  /**
   * Forces update with new code object.
   *
   * @param {Object} codeObj - The code object to force update with.
   */
  forceCodeObj(codeObj) {
    const self = this;
    self._onCodeObjUpdate(codeObj);
  }

  /**
   * Sets up event listeners for advanced editor links.
   *
   * @private
   */
  _setupAdvancedLinks() {
    const self = this;
    const links = Array.of(...self._contentsSelection.querySelectorAll(".advanced-editor-link"));
    links.forEach((link) =>
      link.addEventListener("click", (event) => {
        self._tabs.toggle("#code-editor-pane");
        event.preventDefault();
      }),
    );
  }

  /**
   * Gets the current code object.
   *
   * @returns {Object} The current code object.
   * @private
   */
  _getCodeAsObj() {
    const self = this;
    return self._codeObj;
  }

  /**
   * Initializes the code object from current state.
   *
   * @private
   */
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

  /**
   * Enables the basic panel interface.
   *
   * @private
   */
  _enableBasicPanel() {
    const self = this;
    self._contentsSelection.querySelector(".available-contents").style.display = "block";
    self._contentsSelection.querySelector(".not-available-contents").style.display = "none";
  }

  /**
   * Disables the basic panel interface.
   *
   * @private
   */
  _disableBasicPanel() {
    const self = this;
    self._contentsSelection.querySelector(".available-contents").style.display = "none";
    self._contentsSelection.querySelector(".not-available-contents").style.display = "block";
  }

  /**
   * Handles code object updates.
   *
   * @param {Object} codeObj - The updated code object.
   * @private
   */
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

/**
 * Class for managing stream selection availability updates in presenter contexts.
 * Encapsulates all logic for determining and updating stream option states.
 */
class StreamSelectionAvailabilityUpdater {
  constructor(container = document, context = null) {
    const self = this;
    self._container = container;
    self._context = context;
  }

  /**
   * Gets enabled streams for the current context.
   * @param {Object} codeObj - The code object containing substances data.
   * @param {string} substanceName - The name of the substance to check.
   * @param {string} context - Optional context override ('consumption' or 'policy').
   * @returns {Array<string>} Array of enabled stream names.
   */
  getEnabledStreamsForCurrentContext(codeObj, substanceName, context = null) {
    const actualContext = context || this._context;

    if (actualContext === "consumption") {
      return this._getCurrentEnabledStreamsFromCheckboxes();
    } else if (actualContext === "policy") {
      return this._getCurrentEnabledStreamsFromCode(codeObj, substanceName);
    }

    return [];
  }

  /**
   * Gets the first enabled option from a select element.
   * @param {HTMLSelectElement} selectElement - The select element to check.
   * @returns {string} Value of the first enabled option, or 'sales' as fallback.
   */
  getFirstEnabledOption(selectElement) {
    const options = selectElement.querySelectorAll("option:not([disabled])");
    return options.length > 0 ? options[0].value : "sales";
  }

  /**
   * Updates stream option disabled states based on enabled streams.
   * @param {HTMLSelectElement} selectElement - The select element to update.
   * @param {Array<string>} enabledStreams - Array of enabled stream names.
   */
  updateStreamOptionStates(selectElement, enabledStreams) {
    const options = selectElement.querySelectorAll("option");
    options.forEach((option) => {
      const value = option.value;

      if (ALWAYS_ON_STREAMS.includes(value)) {
        option.removeAttribute("disabled");
      } else if (ENABLEABLE_STREAMS.includes(value)) {
        if (enabledStreams.includes(value)) {
          option.removeAttribute("disabled");
        } else {
          option.setAttribute("disabled", "disabled");
        }
      }
    });
  }

  /**
   * Updates all stream target dropdowns within the container.
   * @param {Array<string>} enabledStreams - Array of enabled stream names.
   * @param {Function} filterFn - Optional filter function (deprecated).
   */
  updateAllStreamTargetDropdowns(enabledStreams, filterFn = null) {
    STREAM_TARGET_SELECTORS.forEach((selector) => {
      const dropdowns = this._container.querySelectorAll(selector);
      dropdowns.forEach((dropdown) => {
        this.updateStreamOptionStates(dropdown, enabledStreams);
      });
    });
  }

  /**
   * Refreshes all stream target dropdowns for a specific substance.
   * @param {Object} codeObj - The code object containing substances data.
   * @param {string} substanceName - The name of the substance whose streams changed.
   */
  refreshAllStreamTargetDropdowns(codeObj, substanceName) {
    const enabledStreams = this._getEnabledStreamsForSubstance(codeObj, substanceName);
    this.updateAllStreamTargetDropdowns(enabledStreams);
  }

  /**
   * Private method to get enabled streams for a substance from code objects.
   * @param {Object} codeObj - The code object containing substances data.
   * @param {string} substanceName - The name of the substance to check.
   * @returns {Array<string>} Array of enabled stream names.
   */
  _getEnabledStreamsForSubstance(codeObj, substanceName) {
    const substances = codeObj.getSubstances();
    const substance = substances.find((s) => s.getName() === substanceName);

    if (!substance) throw new Error(`Substance "${substanceName}" not found`);

    if (typeof substance.getEnables === "function") {
      const enableCommands = substance.getEnables();
      return enableCommands
        .map((cmd) => cmd.getTarget())
        .filter((x) => ENABLEABLE_STREAMS.includes(x));
    }

    return [];
  }

  /**
   * Private method to get currently enabled streams from checkboxes.
   * @returns {Array<string>} Array of enabled stream names.
   */
  _getCurrentEnabledStreamsFromCheckboxes() {
    const enabledStreams = [];
    const container = this._container;

    const enableDomestic = container.querySelector(".enable-domestic-checkbox");
    const enableImport = container.querySelector(".enable-import-checkbox");
    const enableExport = container.querySelector(".enable-export-checkbox");

    if (enableDomestic && enableDomestic.checked) {
      enabledStreams.push("domestic");
    }
    if (enableImport && enableImport.checked) {
      enabledStreams.push("import");
    }
    if (enableExport && enableExport.checked) {
      enabledStreams.push("export");
    }

    return enabledStreams;
  }

  /**
   * Private method to get currently enabled streams from code objects.
   * @param {Object} codeObj - The code object containing substances data.
   * @param {string} substanceName - The name of the substance to check.
   * @returns {Array<string>} Array of enabled stream names.
   */
  _getCurrentEnabledStreamsFromCode(codeObj, substanceName) {
    const policySubstanceInput = this._container.querySelector(".edit-policy-substance-input");
    if (!policySubstanceInput) return [];

    const firstName = policySubstanceInput.options[0].value;
    const policySubstanceNameCandidate = policySubstanceInput.value;
    const noneSelected = !policySubstanceNameCandidate &&
      policySubstanceInput.options &&
      policySubstanceInput.options.length > 0;

    const policySubstanceName = noneSelected ? firstName : policySubstanceNameCandidate;
    return this._getEnabledStreamsForSubstance(codeObj, policySubstanceName);
  }
}

/**
 * Initializes a set command UI element.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - Optional code object containing substances data.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 */
function initSetCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  // Update stream options based on enabled streams - use context-aware detection
  const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
  const targetSelect = root.querySelector(".set-target-input");

  streamUpdater.updateStreamOptionStates(targetSelect, enabledStreams);

  setFieldValue(root.querySelector(".set-target-input"), itemObj, "sales", (x) =>
    x.getTarget(),
  );
  setEngineNumberValue(
    root.querySelector(".set-amount-input"),
    root.querySelector(".set-units-input"),
    itemObj,
    new EngineNumber(1, "mt"),
    (x) => x.getValue(),
  );
  setDuring(root.querySelector(".duration-subcomponent"), itemObj, new YearMatcher(1, 1), true);
}

/**
 * Reads values from a set command UI element.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {Command} A new Command object with the UI values.
 */
function readSetCommandUi(root) {
  const target = getFieldValue(root.querySelector(".set-target-input"));
  const amount = getEngineNumberValue(
    root.querySelector(".set-amount-input"),
    root.querySelector(".set-units-input"),
  );
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));
  return new Command("setVal", target, amount, duration);
}

/**
 * Initializes a change command UI element.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - Optional code object containing substances data.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 */
function initChangeCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  // Update stream options based on enabled streams - use context-aware detection
  const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
  const targetSelect = root.querySelector(".change-target-input");

  streamUpdater.updateStreamOptionStates(targetSelect, enabledStreams);
  setFieldValue(root.querySelector(".change-target-input"), itemObj, "sales", (x) =>
    x.getTarget(),
  );
  setFieldValue(root.querySelector(".change-sign-input"), itemObj, "+", (x) =>
    x.getValue() < 0 ? "-" : "+",
  );
  setFieldValue(root.querySelector(".change-amount-input"), itemObj, 5, (x) => {
    if (x.getValue() === null || x.getValue().getValue() === null) {
      return 5;
    }
    const valueSigned = x.getValue().getValue();
    const valueUnsigned = Math.abs(valueSigned);
    return valueUnsigned;
  });
  setFieldValue(root.querySelector(".change-units-input"), itemObj, "% / year", (x) => {
    if (x.getValue() === null) {
      return "% / year";
    }
    return x.getValue().getUnits();
  });
  setDuring(root.querySelector(".duration-subcomponent"), itemObj, new YearMatcher(2, 10), true);
}

/**
 * Reads values from a change command UI element.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {Command} A new Command object with the UI values.
 */
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

/**
 * Initializes a limit command UI widget.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - The code object containing available substances.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 */
function initLimitCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  const substances = codeObj.getSubstances();
  const substanceNamesDup = substances.map((x) => x.getName());
  const substanceNames = Array.of(...new Set(substanceNamesDup));
  const substanceSelect = d3.select(root.querySelector(".substances-select"));
  substanceSelect.html("");
  substanceSelect
    .selectAll("option")
    .data(substanceNames)
    .enter()
    .append("option")
    .attr("value", (x) => x)
    .text((x) => x);

  setFieldValue(root.querySelector(".limit-type-input"), itemObj, "cap", (x) => x.getTypeName());
  setFieldValue(root.querySelector(".limit-target-input"), itemObj, "sales", (x) => x.getTarget());
  setEngineNumberValue(
    root.querySelector(".limit-amount-input"),
    root.querySelector(".limit-units-input"),
    itemObj,
    new EngineNumber(1, "mt"),
    (x) => x.getValue(),
  );
  setFieldValue(root.querySelector(".displacing-input"), itemObj, "", (x) =>
    x && x.getDisplacing ? (x.getDisplacing() === null ? "" : x.getDisplacing()) : "",
  );
  setDuring(root.querySelector(".duration-subcomponent"), itemObj, new YearMatcher(2, 10), true);

  // Add event listener to update options when substance changes
  const substanceSelectElement = root.querySelector(".substances-select");

  const updateLimitTargetOptions = () => {
    const limitTargetSelect = root.querySelector(".limit-target-input");
    const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
    streamUpdater.updateStreamOptionStates(limitTargetSelect, enabledStreams);
  };

  const updateDisplacingOptions = () => {
    const displacingSelect = root.querySelector(".displacing-input");
    if (displacingSelect) {
      const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(
        codeObj, null, context,
      );
      streamUpdater.updateStreamOptionStates(displacingSelect, enabledStreams);
    }
  };

  substanceSelectElement.addEventListener("change", updateLimitTargetOptions);
  substanceSelectElement.addEventListener("change", updateDisplacingOptions);

  // Initial update of stream options
  updateLimitTargetOptions();
  updateDisplacingOptions();
}

/**
 * Reads values from a limit command UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {LimitCommand} A new LimitCommand object with the UI values.
 */
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

/**
 * Initializes a recharge command UI widget.
 *
 * @param {Object} itemObj - The recharge command object or null for new commands.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - The code object for context.
 */
function initRechargeCommandUi(itemObj, root, codeObj) {
  // Handle both RechargeCommand objects and generic Command objects
  const populationGetter = (x) => {
    if (x.getPopulation) {
      return x.getPopulation();
    } else {
      return x.getTarget().getValue();
    }
  };
  const populationUnitsGetter = (x) => {
    if (x.getPopulationUnits) {
      return x.getPopulationUnits();
    } else {
      return x.getTarget().getUnits();
    }
  };
  const volumeGetter = (x) => {
    if (x.getVolume) {
      return x.getVolume();
    } else {
      return x.getValue().getValue();
    }
  };
  const volumeUnitsGetter = (x) => {
    if (x.getVolumeUnits) {
      return x.getVolumeUnits();
    } else {
      return x.getValue().getUnits();
    }
  };

  setFieldValue(
    root.querySelector(".recharge-population-input"),
    itemObj,
    "5",
    populationGetter,
  );
  setFieldValue(
    root.querySelector(".recharge-population-units-input"),
    itemObj,
    "%",
    populationUnitsGetter,
  );
  setFieldValue(
    root.querySelector(".recharge-volume-input"),
    itemObj,
    "0.85",
    volumeGetter,
  );
  setFieldValue(
    root.querySelector(".recharge-volume-units-input"),
    itemObj,
    "kg / unit",
    volumeUnitsGetter,
  );

  // Set up duration using standard pattern
  setDuring(root.querySelector(".duration-subcomponent"), itemObj, new YearMatcher(1, 1), true);
}

/**
 * Reads values from a recharge command UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {RechargeCommand} A new RechargeCommand object with the UI values.
 */
function readRechargeCommandUi(root) {
  const population = getFieldValue(root.querySelector(".recharge-population-input"));
  const populationUnits = getFieldValue(
    root.querySelector(".recharge-population-units-input"),
  );
  const volume = getFieldValue(root.querySelector(".recharge-volume-input"));
  const volumeUnits = getFieldValue(
    root.querySelector(".recharge-volume-units-input"),
  );

  // Read duration using standard pattern
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));

  // Convert YearMatcher duration to RechargeCommand parameters
  const durationTypeInput = root.querySelector(".duration-type-input");
  const durationType = durationTypeInput.value;
  const yearStart = duration.getStart();
  const yearEnd = duration.getEnd();

  return new RechargeCommand(
    population,
    populationUnits,
    volume,
    volumeUnits,
    durationType,
    yearStart,
    yearEnd,
  );
}

/**
 * Initializes a recycle command UI widget.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - Optional code object containing substances data.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 */
function initRecycleCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  // Update stream options based on enabled streams - use context-aware detection
  const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
  const displacingSelect = root.querySelector(".displacing-input");

  streamUpdater.updateStreamOptionStates(displacingSelect, enabledStreams);
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
  setFieldValue(root.querySelector(".displacing-input"), itemObj, "", (x) =>
    x && x.getDisplacing ? (x.getDisplacing() === null ? "" : x.getDisplacing()) : "",
  );
  setFieldValue(root.querySelector(".recycle-stage-input"), itemObj, "recharge", (x) =>
    x && x.getStage ? x.getStage() : "recharge",
  );
  setDuring(root.querySelector(".duration-subcomponent"), itemObj, new YearMatcher(2, 10), true);
}

/**
 * Reads values from a recycle command UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {Command} A new Command object with the UI values.
 */
function readRecycleCommandUi(root) {
  const collection = getEngineNumberValue(
    root.querySelector(".recycle-amount-input"),
    root.querySelector(".recycle-units-input"),
  );
  const reuse = getEngineNumberValue(
    root.querySelector(".recycle-reuse-amount-input"),
    root.querySelector(".recycle-reuse-units-input"),
  );
  const displacingRaw = getFieldValue(root.querySelector(".displacing-input"));
  const displacing = displacingRaw === "" ? null : displacingRaw;
  const stage = getFieldValue(root.querySelector(".recycle-stage-input"));
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));
  return new RecycleCommand(collection, reuse, duration, displacing, stage);
}

/**
 * Initializes a replace command UI widget.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - The code object containing available substances.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 */
function initReplaceCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  const substances = codeObj.getSubstances();
  const substanceNamesDup = substances.map((x) => x.getName());
  const substanceNames = Array.of(...new Set(substanceNamesDup));
  const substanceSelect = d3.select(root.querySelector(".substances-select"));
  substanceSelect.html("");
  substanceSelect
    .selectAll("option")
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

  // Add event listener to update options when substance changes
  const substanceSelectElement = root.querySelector(".substances-select");
  const updateReplaceTargetOptions = () => {
    const replaceTargetSelect = root.querySelector(".replace-target-input");
    const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
    streamUpdater.updateStreamOptionStates(replaceTargetSelect, enabledStreams);
  };
  const updateDisplacingOptions = () => {
    const displacingSelect = root.querySelector(".displacing-input");
    if (displacingSelect) {
      const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(
        codeObj, null, context,
      );
      streamUpdater.updateStreamOptionStates(displacingSelect, enabledStreams);
    }
  };
  substanceSelectElement.addEventListener("change", updateReplaceTargetOptions);
  substanceSelectElement.addEventListener("change", updateDisplacingOptions);

  setFieldValue(root.querySelector(".replace-target-input"), itemObj, "sales", (x) =>
    x.getSource(),
  );

  setFieldValue(root.querySelector(".replace-replacement-input"), itemObj, substanceNames[0], (x) =>
    x.getDestination(),
  );

  setDuring(root.querySelector(".duration-subcomponent"), itemObj, new YearMatcher(2, 10), true);

  // Initial update of stream options
  updateReplaceTargetOptions();
  updateDisplacingOptions();
}

/**
 * Reads values from a replace command UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {ReplaceCommand} A new ReplaceCommand object with the UI values.
 */
function readReplaceCommandUi(root) {
  const target = getFieldValue(root.querySelector(".replace-target-input"));
  const amount = getEngineNumberValue(
    root.querySelector(".replace-amount-input"),
    root.querySelector(".replace-units-input"),
  );
  const replacement = getFieldValue(root.querySelector(".replace-replacement-input"));
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));

  return new ReplaceCommand(amount, target, replacement, duration);
}

/**
 * Reads duration values from a duration UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {YearMatcher} A new YearMatcher object with the UI values.
 */
function readDurationUi(root) {
  const durationType = getFieldValue(root.querySelector(".duration-type-input"));
  const targets = {
    "in year": {min: "duration-start", max: "duration-start"},
    "during all years": {min: null, max: null},
    "starting in year": {min: "duration-start", max: null},
    "ending in year": {min: null, max: "duration-end"},
    "during years": {min: "duration-start", max: "duration-end"},
  }[durationType];
  const getYearValue = (x) => (x === null ? null : root.querySelector("." + x).value);
  const minYear = getYearValue(targets["min"]);
  const maxYear = getYearValue(targets["max"]);
  return new YearMatcher(minYear, maxYear);
}

export {UiEditorPresenter};
