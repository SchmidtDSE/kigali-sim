/**
 * Presenter for managing the simulations list in the UI editor.
 *
 * @license BSD, see LICENSE.md.
 */
import {ParsedYear, YearMatcher} from "duration";
import {SimulationScenario} from "ui_translator_components";
import {
  NameConflictResolution,
  resolveNameConflict,
  DuplicateEntityPresenter,
} from "duplicate_util";
import {
  updateDurationSelector,
  setupDurationSelector,
  buildSetupListButton,
  getFieldValue,
  getSanitizedFieldValue,
  setFieldValue,
  validateNumericInputs,
  validateSimulationDuration,
  setupDialogInternalLinks,
} from "ui_editor_util";

/**
 * Manages the UI for listing and editing simulations.
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
      event.preventDefault();
      if (self._save()) {
        self._dialog.close();
      }
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
      .map((x) => x.getName())
      .sort();
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
    return scenarios.map((x) => x.getName()).sort();
  }

  /**
   * Saves the current simulation data.
   *
   * @private
   */
  _save() {
    const self = this;

    // Validate numeric inputs and get user confirmation for potentially invalid values
    if (!validateNumericInputs(self._dialog, "simulation")) {
      return false; // User cancelled, stop save operation
    }

    // Validate simulation duration and warn about very long simulations
    if (!validateSimulationDuration(self._dialog)) {
      return false; // User cancelled, stop save operation
    }
    let scenario = self._parseObj();

    // Handle duplicate name resolution for new simulations
    if (self._editingName === null) {
      const baseName = scenario.getName();
      const priorNames = new Set(self._getSimulationNames());
      const resolution = resolveNameConflict(baseName, priorNames);

      // Update the input field if the name was changed
      if (resolution.getNameChanged()) {
        const nameInput = self._dialog.querySelector(".edit-simulation-name-input");
        nameInput.value = resolution.getNewName();

        // Need to re-parse with the updated name
        scenario = self._parseObj();
      }
    }

    const codeObj = self._getCodeObj();
    codeObj.insertScenario(self._editingName, scenario);
    self._onCodeObjUpdate(codeObj);
    return true;
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
    const policyNamesSelected = policiesChecked.map((x) => x.value).sort();

    return new SimulationScenario(scenarioName, policyNamesSelected, start, end, true);
  }
}

export {SimulationListPresenter};
