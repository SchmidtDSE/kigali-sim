/**
 * Presenters for managing consumption and policy stanzas via the UI editor.
 *
 * Presenters for UI components which manage consumption and policy stanzas via
 * the UI editor. These are the locations where action commands are actually held.
 *
 * @license BSD, see LICENSE.md.
 */
import {ParsedYear, YearMatcher} from "duration";
import {EngineNumber} from "engine_number";
import {MetaSerializer, MetaChangeApplier} from "meta_serialization";
import {GwpLookupPresenter} from "known_substance";
import {NumberParseUtil} from "number_parse_util";
import {
  Application,
  Command,
  DefinitionalStanza,
  RetireCommand,
  SubstanceBuilder,
} from "ui_translator_components";
import {
  NameConflictResolution,
  resolveNameConflict,
  resolveSubstanceNameConflict,
  DuplicateEntityPresenter,
} from "duplicate_util";
import {ENABLEABLE_STREAMS} from "ui_editor_const";
import {ALWAYS_ON_STREAMS} from "ui_editor_const";
import {
  STREAM_TARGET_SELECTORS,
  updateDurationSelector,
  setupDurationSelector,
  buildSetupListButton,
  setFieldValue,
  getFieldValue,
  getSanitizedFieldValue,
  setListInput,
  getListInput,
  setEngineNumberValue,
  getEngineNumberValue,
  validateNumericInputs,
  setDuring,
  buildUpdateCount,
  setupDialogInternalLinks,
} from "ui_editor_util";
import {
  initSetCommandUi,
  readSetCommandUi,
  initChangeCommandUi,
  readChangeCommandUi,
  initLimitCommandUi,
  readLimitCommandUi,
  initRechargeCommandUi,
  readRechargeCommandUi,
  initRecycleCommandUi,
  readRecycleCommandUi,
  initReplaceCommandUi,
  readReplaceCommandUi,
  readDurationUi,
} from "ui_editor_strategy";

/**
 * Manages stream selection availability based on context.
 *
 * This class is responsible for updating the availability of stream options in
 * select dropdowns based on which streams are enabled in the current context.
 * It handles both consumption context (where streams are determined by checkboxes)
 * and policy context (where streams are determined by existing code).
 */
class StreamSelectionAvailabilityUpdater {
  /**
   * Creates a new StreamSelectionAvailabilityUpdater.
   *
   * @param {HTMLElement|Document} container - The DOM container element to
   *   search within for stream controls.
   * @param {string|null} context - The context for stream availability
   *   ('consumption' or 'policy').
   */
  constructor(container, context) {
    const self = this;
    self._container = container;
    self._context = context;
  }

  /**
   * Gets enabled streams for the current context.
   *
   * @param {Object} codeObj - The code object containing substances data.
   * @param {string} substanceName - The name of the substance to check.
   * @param {string} context - Optional context override. If not provided, uses
   *   the context provided at construction.
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
   *
   * @param {HTMLSelectElement} selectElement - The select element to check.
   * @returns {string} Value of the first enabled option, or 'sales' as fallback.
   */
  getFirstEnabledOption(selectElement) {
    const options = selectElement.querySelectorAll("option:not([disabled])");
    return options.length > 0 ? options[0].value : "sales";
  }

  /**
   * Updates stream option disabled states based on enabled streams.
   *
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
   *
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
   *
   * @param {Object} codeObj - The code object containing substances data.
   * @param {string} substanceName - The name of the substance whose streams changed.
   */
  refreshAllStreamTargetDropdowns(codeObj, substanceName) {
    const enabledStreams = this._getEnabledStreamsForSubstance(codeObj, substanceName);
    this.updateAllStreamTargetDropdowns(enabledStreams);
  }

  /**
   * Get enabled streams for a substance from code objects.
   *
   * @param {Object} codeObj - The code object containing substances data.
   * @param {string} substanceName - The name of the substance to check.
   * @returns {Array<string>} Array of enabled stream names.
   * @private
   */
  _getEnabledStreamsForSubstance(codeObj, substanceName) {
    const substances = codeObj.getSubstances();
    const substance = substances.find((s) => s.getName() === substanceName);

    if (!substance) {
      throw new Error(`Substance "${substanceName}" not found`);
    }

    const enableCommands = substance.getEnables();
    return enableCommands
      .map((cmd) => cmd.getTarget())
      .filter((x) => ENABLEABLE_STREAMS.includes(x));
  }

  /**
   * Get currently enabled streams from checkboxes.
   *
   * This method is only called in consumption context where the enable
   * checkboxes are guaranteed to be present in the dialog structure.
   *
   * @returns {Array<string>} Array of enabled stream names.
   * @private
   */
  _getCurrentEnabledStreamsFromCheckboxes() {
    const enabledStreams = [];
    const container = this._container;

    const enableDomestic = container.querySelector(".enable-domestic-checkbox");
    const enableImport = container.querySelector(".enable-import-checkbox");
    const enableExport = container.querySelector(".enable-export-checkbox");

    if (enableDomestic.checked) {
      enabledStreams.push("domestic");
    }
    if (enableImport.checked) {
      enabledStreams.push("import");
    }
    if (enableExport.checked) {
      enabledStreams.push("export");
    }

    return enabledStreams;
  }

  /**
   * Get currently enabled streams from code objects.
   *
   * @param {Object} codeObj - The code object containing substances data.
   * @param {string} substanceName - The name of the substance to check.
   * @returns {Array<string>} Array of enabled stream names.
   * @private
   */
  _getCurrentEnabledStreamsFromCode(codeObj, substanceName) {
    const policySubstanceInput = this._container.querySelector(".edit-policy-substance-input");
    if (!policySubstanceInput) {
      return [];
    }

    const firstName = policySubstanceInput.options[0].value;
    const policySubstanceNameCandidate = policySubstanceInput.value;
    const noneSelected = this._hasNoSubstanceSelected(policySubstanceInput);

    const policySubstanceName = noneSelected ? firstName : policySubstanceNameCandidate;
    return this._getEnabledStreamsForSubstance(codeObj, policySubstanceName);
  }

  /**
   * Determines if a select element has no value selected but has options available.
   *
   * This occurs in the initial dialog state before user interaction, when the
   * select element has options but no value has been explicitly chosen. In such
   * cases, we need to default to the first option for stream availability calculation.
   *
   * @param {HTMLSelectElement} selectElement - The select element to check.
   * @returns {boolean} True if no selection with available options, false otherwise.
   * @private
   */
  _hasNoSubstanceSelected(selectElement) {
    return !selectElement.value && selectElement.options && selectElement.options.length > 0;
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
   * @param {Object} tabs - Tabby tabs instance.
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

    /**
     * Updates a display element with the current substance value.
     *
     * @param {HTMLElement} display - The display element to update.
     */
    const updateSubstance = (display) => {
      display.innerHTML = "";
      const textNode = document.createTextNode(substanceInput.value);
      display.appendChild(textNode);
    };

    /**
     * Updates a display element with the current application value.
     *
     * @param {HTMLElement} display - The display element to update.
     */
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
        self._handleDeleteConsumption(x);
      })
      .text("delete")
      .attr("aria-label", (x) => "delete " + x);
  }

  /**
   * Handles deletion of a consumption record.
   *
   * Prompts the user for confirmation before deleting the specified consumption.
   * Parses the consumption name to extract substance and application, then
   * removes it from the code object.
   *
   * @param {string} consumptionName - The full name of the consumption to delete.
   * @private
   */
  _handleDeleteConsumption(consumptionName) {
    const self = this;
    const message = "Are you sure you want to delete " + consumptionName + "?";
    const isConfirmed = confirm(message);
    if (!isConfirmed) {
      return;
    }

    const codeObj = self._getCodeObj();
    const objIdentifierRegex = /\"([^\"]+)\" for \"([^\"]+)\"/;
    const match = consumptionName.match(objIdentifierRegex);
    const substance = match[1];
    const application = match[2];
    codeObj.deleteSubstance(application, substance);
    self._onCodeObjUpdate(codeObj);
  }

  /**
   * Sets up the dialog for adding/editing consumption records.
   *
   * Orchestrates initialization of all dialog components including tabs,
   * buttons, command lists, reminders, GWP lookup, and stream checkboxes.
   *
   * @private
   */
  _setupDialog() {
    const self = this;

    self._tabs = new Tabby("#" + self._dialog.querySelector(".tabs").id);

    self._setupDialogButtons();
    self._setupDialogCommandLists();
    self._setupDialogReminders();
    self._setupDialogGwpLookup();
    self._setupDialogStreamCheckboxes();
  }

  /**
   * Sets up dialog buttons for add, close, and save actions.
   *
   * Initializes event handlers for the add link, close button, and save button
   * within the consumption dialog.
   *
   * @private
   */
  _setupDialogButtons() {
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
      event.preventDefault();
      if (self._save()) {
        self._dialog.close();
      }
    });
  }

  /**
   * Sets up command list buttons for set, change, limit, and recharge.
   *
   * Initializes all command list buttons with their respective templates and
   * initialization functions. Configures the update callback to hide reminders
   * and refresh counts.
   *
   * @private
   */
  _setupDialogCommandLists() {
    const self = this;

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
  }

  /**
   * Sets up reminder presenter and internal navigation links.
   *
   * Initializes the ReminderPresenter to display current application and
   * substance context, and configures internal dialog navigation links.
   *
   * @private
   */
  _setupDialogReminders() {
    const self = this;

    self._reminderPresenter = new ReminderPresenter(
      self._dialog,
      ".edit-consumption-application-input",
      ".edit-consumption-substance-input",
      "#consumption-general",
      self._tabs,
    );

    setupDialogInternalLinks(self._root, self._tabs);
  }

  /**
   * Sets up GWP lookup functionality.
   *
   * Initializes the GwpLookupPresenter to enable substance GWP value lookup
   * from the known_gwp.json database via the lookup link.
   *
   * @private
   */
  _setupDialogGwpLookup() {
    const self = this;

    const lookupLink = self._root.querySelector("#lookup-gwp");
    const substanceInput = self._dialog.querySelector(".edit-consumption-substance-input");
    const ghgInput = self._dialog.querySelector(".edit-consumption-ghg-input");
    const ghgUnitsInput = self._dialog.querySelector(".edit-consumption-ghg-units-input");
    const gwpPathInput = document.getElementById("known-gwp-path");
    const jsonPath = gwpPathInput ? gwpPathInput.value : "json/known_gwp.json";

    self._gwpLookupPresenter = new GwpLookupPresenter(
      lookupLink,
      substanceInput,
      ghgInput,
      ghgUnitsInput,
      jsonPath,
    );
  }

  /**
   * Sets up enable stream checkbox listeners.
   *
   * Attaches change event listeners to the domestic, import, and export enable
   * checkboxes to trigger visibility and validation updates.
   *
   * @private
   */
  _setupDialogStreamCheckboxes() {
    const self = this;

    const enableImport = self._dialog.querySelector(".enable-import-checkbox");
    const enableDomestic = self._dialog.querySelector(".enable-domestic-checkbox");
    const enableExport = self._dialog.querySelector(".enable-export-checkbox");

    enableImport.addEventListener("change", () => self._updateSource());
    enableDomestic.addEventListener("change", () => self._updateSource());
    enableExport.addEventListener("change", () => self._updateSource());
  }

  /**
   * Parses a full substance name into substance and equipment parts.
   *
   * Splits a compound substance name on the " - " separator to extract the
   * base substance name and equipment model. Returns empty strings if the
   * input is null or undefined.
   *
   * @param {string} fullSubstanceName - The full substance name potentially
   *   containing equipment model (e.g., "R-410A - Model X").
   * @returns {{substance: string, equipment: string}} Object with parsed parts.
   * @private
   */
  _getSubstanceAndEquipment(fullSubstanceName) {
    if (!fullSubstanceName) {
      return {substance: "", equipment: ""};
    }
    const parts = fullSubstanceName.split(" - ");
    return {
      substance: parts[0] || "",
      equipment: parts.slice(1).join(" - ") || "",
    };
  }

  /**
   * Gets effective substance and equipment from object or returns defaults.
   *
   * When an existing object is provided, parses its name to extract substance
   * and equipment parts. Returns empty strings for both if object is null.
   *
   * @param {Object|null} objToShow - The substance object to extract from.
   * @returns {{substance: string, equipment: string}} Parsed substance and equipment.
   * @private
   */
  _getEffectiveSubstanceAndEquipment(objToShow) {
    if (objToShow) {
      return this._getSubstanceAndEquipment(objToShow.getName());
    } else {
      return {substance: "", equipment: ""};
    }
  }

  /**
   * Gets value from target or returns fallback if target is null.
   *
   * Null-safe accessor that calls getValue() on the target object if available,
   * otherwise returns the provided fallback value.
   *
   * @param {Object|null} target - The target object with getValue() method.
   * @param {*} fallback - The fallback value to return if target is null.
   * @returns {*} The target's value or the fallback.
   * @private
   */
  _getValueOrDefault(target, fallback) {
    return target === null ? fallback : target.getValue();
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

    const applicationNames = codeObj.getApplications().map((x) => x.getName());

    const applicationSelect = self._dialog.querySelector(".application-select");
    d3.select(applicationSelect)
      .html("")
      .selectAll("option")
      .data(applicationNames)
      .enter()
      .append("option")
      .attr("value", (x) => x)
      .text((x) => x);

    const substanceAndEquipment = self._getEffectiveSubstanceAndEquipment(objToShow);

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

    const domesticFallback = new EngineNumber(1, "kg / unit");
    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-domestic-input"),
      self._dialog.querySelector(".initial-charge-domestic-units-input"),
      objToShow,
      domesticFallback,
      (x) => self._getValueOrDefault(x.getInitialCharge("domestic"), domesticFallback),
    );

    const importFallback = new EngineNumber(2, "kg / unit");
    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-import-input"),
      self._dialog.querySelector(".initial-charge-import-units-input"),
      objToShow,
      importFallback,
      (x) => self._getValueOrDefault(x.getInitialCharge("import"), importFallback),
    );

    const exportFallback = new EngineNumber(1, "kg / unit");
    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-initial-charge-export-input"),
      self._dialog.querySelector(".initial-charge-export-units-input"),
      objToShow,
      exportFallback,
      (x) => self._getValueOrDefault(x.getInitialCharge("export"), exportFallback),
    );

    setEngineNumberValue(
      self._dialog.querySelector(".edit-consumption-retirement-input"),
      self._dialog.querySelector(".retirement-units-input"),
      objToShow,
      new EngineNumber(5, "% / year"),
      (x) => x.getRetire() ? x.getRetire().getValue() : null,
    );

    /**
     * Sets up retirement UI controls including input and checkbox.
     *
     * Initializes the retirement reduces equipment checkbox based on whether
     * the substance has a retire command with the withReplacement flag.
     * The checkbox logic is inverted: checked means normal retirement that
     * reduces equipment population, unchecked means retirement with replacement
     * that maintains equipment population.
     */
    const setupRetirementUI = () => {
      const retirementReducesCheckbox = self._dialog.querySelector(
        ".retirement-reduces-equipment-checkbox",
      );

      if (objToShow !== null && objToShow.getRetire()) {
        const withReplacement = objToShow.getRetire().getWithReplacement();
        retirementReducesCheckbox.checked = !withReplacement;
      } else {
        retirementReducesCheckbox.checked = true;
      }
    };
    setupRetirementUI();

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

    /**
     * Sets up enable checkboxes based on substance's enable commands.
     *
     * Initializes the domestic, import, and export enable checkboxes.
     * For existing substances, checks if the substance has enable commands
     * for each stream. For new substances, defaults all checkboxes to unchecked
     * since the enable command must be explicitly added when a stream is used.
     */
    const setupEnableCheckboxes = () => {
      const enableImport = self._dialog.querySelector(".enable-import-checkbox");
      const enableDomestic = self._dialog.querySelector(".enable-domestic-checkbox");
      const enableExport = self._dialog.querySelector(".enable-export-checkbox");

      if (objToShow !== null) {
        const enableCommands = objToShow.getEnables();
        enableDomestic.checked = enableCommands.some((cmd) => cmd.getTarget() === "domestic");
        enableImport.checked = enableCommands.some((cmd) => cmd.getTarget() === "import");
        enableExport.checked = enableCommands.some((cmd) => cmd.getTarget() === "export");
      } else {
        enableDomestic.checked = false;
        enableImport.checked = false;
        enableExport.checked = false;
      }
    };
    setupEnableCheckboxes();

    /**
     * Sets up the sales assumption dropdown.
     *
     * Initializes the sales assumption dropdown with the current assume mode.
     * Defaults to "continued" for new substances or when no assume mode is set.
     * The assume mode controls whether sales continue from previous year,
     * are zeroed out, or only cover recharge needs.
     */
    const setupSalesAssumption = () => {
      const salesAssumptionInput = self._dialog.querySelector(".sales-assumption-input");
      if (objToShow === null) {
        salesAssumptionInput.value = "continued";
      } else {
        const assumeMode = objToShow.getAssumeMode();
        salesAssumptionInput.value = assumeMode === null ? "continued" : assumeMode;
      }
    };
    setupSalesAssumption();

    self._updateSource();

    self._dialog.showModal();
    self._reminderPresenter.update();
    self._updateCounts();
  }

  /**
   * Update the source inputs value and visibility.
   *
   * Shows or hides initial charge inputs based on enable checkbox states.
   * When a stream is enabled, shows its input and sets default value if needed.
   * When disabled, hides the input and sets value to 0. Updates stream target
   * dropdowns to reflect current enabled streams.
   *
   * @private
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

    /**
     * Updates visibility and value for a stream input based on checkbox state.
     *
     * When checkbox is checked, shows the input field and sets default value
     * (1 kg/unit) if current value is 0 or empty. When unchecked, hides the
     * input and sets value to 0.
     *
     * @param {HTMLInputElement} checkbox - The enable checkbox for this stream.
     * @param {HTMLInputElement} input - The value input element.
     * @param {HTMLElement} inputOuter - The container element for visibility.
     * @param {string} unitsInputSelector - CSS selector for the units dropdown.
     * @param {string} unitsValue - Default units value to set.
     */
    const updateStreamInput = (checkbox, input, inputOuter, unitsInputSelector, unitsValue) => {
      if (checkbox.checked) {
        inputOuter.style.display = "block";
        if (input.value === "0" || input.value === "") {
          input.value = 1;
          const unitsInput = self._dialog.querySelector(unitsInputSelector);
          if (unitsInput) {
            unitsInput.value = unitsValue;
          }
        }
      } else {
        inputOuter.style.display = "none";
        input.value = 0;
      }
    };

    updateStreamInput(
      enableDomestic,
      domesticInput,
      domesticInputOuter,
      ".initial-charge-domestic-units-input",
      "kg / unit",
    );
    updateStreamInput(
      enableImport,
      importInput,
      importInputOuter,
      ".initial-charge-import-units-input",
      "kg / unit",
    );
    updateStreamInput(
      enableExport,
      exportInput,
      exportInputOuter,
      ".initial-charge-export-units-input",
      "kg / unit",
    );

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

    self._streamUpdater.updateAllStreamTargetDropdowns(tempEnabledStreams);
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
    return consumptions.sort();
  }

  /**
   * Saves the current consumption data.
   *
   * Validates inputs and delegates to either _saveNew or _saveUpdate based on
   * whether a new substance is being created or an existing one is being modified.
   *
   * @returns {boolean} True if save succeeded, false if user cancelled.
   * @private
   */
  _save() {
    const self = this;

    if (!validateNumericInputs(self._dialog, "substance")) {
      return false;
    }
    const substance = self._parseObj();

    const codeObj = self._getCodeObj();
    const newSubstance = self._editingName === null;

    if (newSubstance) {
      return self._saveNew(substance, codeObj);
    } else {
      return self._saveUpdate(substance, codeObj);
    }
  }

  /**
   * Saves a new consumption substance.
   *
   * Handles name conflict resolution for new substances. If a duplicate name
   * is detected, appends a numeric suffix and updates the UI accordingly.
   * Delegates to specialized handlers based on conflict status.
   *
   * @param {Object} substance - The parsed substance object.
   * @param {Object} codeObj - The code object to update.
   * @returns {boolean} True indicating save succeeded.
   * @private
   */
  _saveNew(substance, codeObj) {
    const self = this;
    const applicationName = getFieldValue(
      self._dialog.querySelector(".edit-consumption-application-input"),
    );

    const baseName = substance.getName();
    const priorNames = new Set(self._getConsumptionNames());
    const fullBaseName = `"${baseName}" for "${applicationName}"`;
    const resolution = resolveSubstanceNameConflict(fullBaseName, priorNames);

    if (resolution.getNameChanged()) {
      return self._saveNewConflict(substance, applicationName, resolution);
    } else {
      return self._saveNewNoConflict(substance, applicationName, codeObj);
    }
  }

  /**
   * Saves a new substance after resolving name conflict.
   *
   * Updates the substance input field with the resolved name and re-parses
   * the form data before inserting into the code object.
   *
   * @param {Object} substance - The original parsed substance object.
   * @param {string} applicationName - The application name.
   * @param {Object} resolution - The name conflict resolution result.
   * @returns {boolean} True indicating save succeeded.
   * @private
   */
  _saveNewConflict(substance, applicationName, resolution) {
    const self = this;
    const resolvedFullName = resolution.getNewName();
    const fullNameMatch = resolvedFullName.match(/^"([^"]+)" for "[^"]+"(\s*\([^)]+\))?$/);
    if (fullNameMatch) {
      const baseSubstanceName = fullNameMatch[1];
      const suffix = fullNameMatch[2] || "";
      const resolvedSubstanceName = baseSubstanceName + suffix;

      const substanceInput = self._dialog.querySelector(".edit-consumption-substance-input");
      const equipmentInput = self._dialog.querySelector(".edit-consumption-equipment-input");

      const equipmentModel = getFieldValue(equipmentInput);
      if (equipmentModel && equipmentModel.trim() !== "") {
        const baseResolved = resolvedSubstanceName.replace(` - ${equipmentModel.trim()}`, "");
        substanceInput.value = baseResolved;
      } else {
        substanceInput.value = resolvedSubstanceName;
      }

      substance = self._parseObj();
    }

    const codeObj = self._getCodeObj();
    codeObj.insertSubstance(null, applicationName, null, substance);
    self._onCodeObjUpdate(codeObj);
    return true;
  }

  /**
   * Saves a new substance without name conflicts.
   *
   * Directly inserts the substance into the code object without modification.
   *
   * @param {Object} substance - The parsed substance object.
   * @param {string} applicationName - The application name.
   * @param {Object} codeObj - The code object to update.
   * @returns {boolean} True indicating save succeeded.
   * @private
   */
  _saveNewNoConflict(substance, applicationName, codeObj) {
    const self = this;
    codeObj.insertSubstance(null, applicationName, null, substance);
    self._onCodeObjUpdate(codeObj);
    return true;
  }

  /**
   * Updates an existing consumption substance.
   *
   * Handles renaming of substances within the same application, ensuring
   * that policy references are updated accordingly. Uses standard insertion
   * logic for application changes or when the substance name remains unchanged.
   *
   * @param {Object} substance - The parsed substance object.
   * @param {Object} codeObj - The code object to update.
   * @returns {boolean} True indicating save succeeded.
   * @private
   */
  _saveUpdate(substance, codeObj) {
    const self = this;
    const objIdentifierRegex = /\"([^\"]+)\" for \"([^\"]+)\"/;
    const match = self._editingName.match(objIdentifierRegex);
    const oldSubstanceName = match[1];
    const oldApplicationName = match[2];
    const newApplicationName = getFieldValue(
      self._dialog.querySelector(".edit-consumption-application-input"),
    );
    const newSubstanceName = substance.getName();

    const appNameSame = oldApplicationName === newApplicationName;
    const substanceNameChanged = oldSubstanceName !== newSubstanceName;

    if (appNameSame && substanceNameChanged) {
      codeObj.renameSubstanceInApplication(
        oldApplicationName,
        oldSubstanceName,
        newSubstanceName,
      );
      codeObj.insertSubstance(
        oldApplicationName,
        newApplicationName,
        newSubstanceName,
        substance,
      );
    } else {
      codeObj.insertSubstance(
        oldApplicationName,
        newApplicationName,
        oldSubstanceName,
        substance,
      );
    }

    self._onCodeObjUpdate(codeObj);
    return true;
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

    /**
     * Adds an enable command for a stream if its checkbox is checked.
     *
     * @param {string} streamName - The stream name (domestic, import, export).
     * @param {HTMLInputElement} checkbox - The enable checkbox element.
     */
    const addEnableCommand = (streamName, checkbox) => {
      if (checkbox.checked) {
        substanceBuilder.addCommand(new Command("enable", streamName, null, null));
      }
    };

    const enableImport = self._dialog.querySelector(".enable-import-checkbox");
    const enableDomestic = self._dialog.querySelector(".enable-domestic-checkbox");
    const enableExport = self._dialog.querySelector(".enable-export-checkbox");

    addEnableCommand("domestic", enableDomestic);
    addEnableCommand("import", enableImport);
    addEnableCommand("export", enableExport);

    const assumeModeDropdown = self._dialog.querySelector(".sales-assumption-input");
    const assumeMode = assumeModeDropdown.value;

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

    // Get checkbox state - inverted logic (checked = reduces, unchecked = with replacement)
    const retirementReducesCheckbox = self._dialog.querySelector(
      ".retirement-reduces-equipment-checkbox",
    );
    const withReplacement = !retirementReducesCheckbox.checked;

    // Create retire command with withReplacement flag
    const retireCommand = new RetireCommand(retirement, null, withReplacement);
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

    substanceBuilder.setAssumeMode(assumeMode);

    const substance = substanceBuilder.build(true);

    return substance;
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
   * Creates a listener function for substance input changes.
   *
   * Returns a function that updates stream target dropdowns when the substance
   * selection changes. The listener retrieves enabled streams for the selected
   * substance and updates all stream target dropdowns in the policy dialog to
   * reflect which streams are available.
   *
   * @param {HTMLSelectElement} substanceInput - The substance selection input element.
   * @returns {Function} Event listener function for the change event.
   * @private
   */
  _createSubstanceInputListener(substanceInput) {
    const self = this;
    return () => {
      const selectedSubstance = substanceInput.value;
      const codeObj = self._getCodeObj();
      const enabledStreams = self._streamUpdater._getEnabledStreamsForSubstance(
        codeObj,
        selectedSubstance,
      );

      self._streamUpdater.updateAllStreamTargetDropdowns(enabledStreams);
    };
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
      event.preventDefault();
      if (self._save()) {
        self._dialog.close();
      }
    });

    const updateHints = () => {
      self._reminderPresenter.update();
      self._updateCounts();
    };
    const setupListButton = buildSetupListButton(updateHints);

    const substanceInput = self._dialog.querySelector(".edit-policy-substance-input");
    substanceInput.addEventListener("change", self._createSubstanceInputListener(substanceInput));

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

    const addRechargeButton = self._root.querySelector(".add-recharge-button");
    const rechargeList = self._root.querySelector(".recharge-list");
    setupListButton(
      addRechargeButton,
      rechargeList,
      "recharge-command-template",
      (item, root, context) => {
        initRechargeCommandUi(item, root, self._getCodeObj());
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

    setListInput(
      self._dialog.querySelector(".recharge-list"),
      document.getElementById("recharge-command-template").innerHTML,
      targetSubstance === null ? [] : targetSubstance.getRecharges(),
      (item, root) => initRechargeCommandUi(item, root, self._getCodeObj()),
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
    return policies.map((x) => x.getName()).sort();
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

    const recharges = getListInput(
      self._dialog.querySelector(".recharge-list"),
      readRechargeCommandUi,
    );
    recharges.forEach((command) => builder.addCommand(command));

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

    if (!validateNumericInputs(self._dialog, "policy")) {
      return false;
    }
    let policy = self._parseObj();

    const newPolicy = self._editingName === null;
    if (newPolicy) {
      const baseName = policy.getName();
      const priorNames = new Set(self._getPolicyNames());
      const resolution = resolveNameConflict(baseName, priorNames);

      if (resolution.getNameChanged()) {
        const nameInput = self._dialog.querySelector(".edit-policy-name-input");
        nameInput.value = resolution.getNewName();

        policy = self._parseObj();
      }
    }

    const codeObj = self._getCodeObj();
    codeObj.insertPolicy(self._editingName, policy);
    self._onCodeObjUpdate(codeObj);
    return true;
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
    updateCount(".recharge-list", "#policy-servicing-count");
  }
}

export {
  StreamSelectionAvailabilityUpdater,
  ReminderPresenter,
  ConsumptionListPresenter,
  PolicyListPresenter,
};
