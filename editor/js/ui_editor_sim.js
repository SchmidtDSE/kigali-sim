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
    self._orderControlsTemplate = document.getElementById("sim-order-controls-template").innerHTML;
    self._policyOrderArray = [];
    self._isExplicitOrdering = false;
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

    const enableOrderingLink = self._root.querySelector(".enable-policy-order-link");
    enableOrderingLink.addEventListener("click", (event) => {
      event.preventDefault();
      self._isExplicitOrdering = true;
      self._showForExplicitOrdering();
    });

    // Add click delegation for move up/down links
    self._dialog.addEventListener("click", (event) => {
      const target = event.target;

      // Handle move up (move before)
      if (target.classList.contains("move-policy-up-link")) {
        event.preventDefault();
        const policyName = target.getAttribute("data-policy-name");
        self._movePolicyUp(policyName);
        return;
      }

      // Handle move down (move after)
      if (target.classList.contains("move-policy-down-link")) {
        event.preventDefault();
        const policyName = target.getAttribute("data-policy-name");
        self._movePolicyDown(policyName);
        return;
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

    // Get all available policies
    const allPolicyNames = self
      ._getCodeObj()
      .getPolicies()
      .map((x) => x.getName());

    // Determine ordering mode and render order
    self._isExplicitOrdering = self._determineOrderingMode(policiesSelectedRaw, allPolicyNames);
    self._policyOrderArray = self._determinePolicyRenderOrder(
      policiesSelectedRaw,
      allPolicyNames,
      self._isExplicitOrdering,
    );

    // Render policy checkboxes
    self._renderPolicyCheckboxes();

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
   * Determines whether to use simple or explicit ordering mode.
   *
   * Simple ordering mode is used if any of these conditions are true:
   * - No policies are selected
   * - Only one policy is selected
   * - Selected policies are in ascending alphabetical order
   *
   * Otherwise, explicit ordering mode is used.
   *
   * @param {string[]} policiesSelected - Array of selected policy names in their current order.
   * @param {string[]} allPolicies - Array of all available policy names.
   * @returns {boolean} True if explicit ordering mode, false for simple ordering mode.
   * @private
   */
  _determineOrderingMode(policiesSelected, allPolicies) {
    const self = this;

    // Rule 1: No policies selected -> simple mode
    if (policiesSelected.length === 0) {
      return false;
    }

    // Rule 2: Only one policy selected -> simple mode
    if (policiesSelected.length === 1) {
      return false;
    }

    // Rule 3: Check if selected policies are in ascending alphabetical order
    const sortedSelected = [...policiesSelected].sort();
    const isAlphabetical = policiesSelected.every(
      (policy, index) => policy === sortedSelected[index],
    );

    if (isAlphabetical) {
      return false; // Simple mode
    }

    // None of the simple mode conditions met -> explicit mode
    return true;
  }

  /**
   * Determines the order in which policies should be rendered in the dialog.
   *
   * In simple mode: All policies sorted alphabetically.
   * In explicit mode: Checked policies in their original order, followed by
   * unchecked policies alphabetically.
   *
   * @param {string[]} policiesSelectedRaw - Array of selected policy names in
   *     their QTA code order.
   * @param {string[]} allPolicies - Array of all available policy names
   *     (unsorted).
   * @param {boolean} isExplicitMode - Whether explicit ordering mode is active.
   * @returns {string[]} Array of policy names in the order they should be
   *     rendered.
   * @private
   */
  _determinePolicyRenderOrder(policiesSelectedRaw, allPolicies, isExplicitMode) {
    const self = this;

    if (!isExplicitMode) {
      // Simple mode: All policies alphabetically
      return [...allPolicies].sort();
    }

    // Explicit mode: Selected policies in QTA order, then unselected alphabetically
    const selectedSet = new Set(policiesSelectedRaw);
    const unselectedPolicies = allPolicies.filter((policy) => !selectedSet.has(policy)).sort();

    return [...policiesSelectedRaw, ...unselectedPolicies];
  }

  /**
   * Updates UI visibility for simple ordering mode.
   *
   * In simple mode:
   * - Show the "specify policy order" link
   * - Hide all move before/after controls
   *
   * @private
   */
  _showForSimpleOrdering() {
    const self = this;

    // Show the enable ordering link holder
    const enableHolder = self._dialog.querySelector(".enable-policy-order-holder");
    if (enableHolder) {
      enableHolder.style.display = "inline";
    }

    // Hide all move policy controls
    const moveControls = self._dialog.querySelectorAll(".move-policy-control");
    moveControls.forEach((control) => {
      control.style.display = "none";
    });
  }

  /**
   * Updates UI visibility for explicit ordering mode.
   *
   * In explicit mode:
   * - Hide the "specify policy order" link
   * - Show all move before/after controls
   *
   * @private
   */
  _showForExplicitOrdering() {
    const self = this;

    // Hide the enable ordering link holder
    const enableHolder = self._dialog.querySelector(".enable-policy-order-holder");
    if (enableHolder) {
      enableHolder.style.display = "none";
    }

    // Show all move policy controls
    const moveControls = self._dialog.querySelectorAll(".move-policy-control");
    moveControls.forEach((control) => {
      control.style.display = "inline";
    });
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
    const policyNamesSelected = policiesChecked.map((x) => x.value);

    return new SimulationScenario(scenarioName, policyNamesSelected, start, end, true);
  }

  /**
   * Renders the ordering control HTML for a given policy.
   *
   * @param {string} policyName - The name of the policy.
   * @returns {string} HTML string for the ordering controls.
   * @private
   */
  _renderOrderControls(policyName) {
    const self = this;
    return self._orderControlsTemplate.replace(/{POLICY_NAME}/g, policyName);
  }

  /**
   * Renders the policy checkbox list in the dialog.
   *
   * This method generates the checkbox list based on the current _policyOrderArray
   * state. It preserves checkbox checked states across re-renders by querying
   * existing checkboxes before clearing the DOM.
   *
   * @private
   */
  _renderPolicyCheckboxes() {
    const self = this;

    // Capture current checked states before re-rendering
    const checkedStates = {};
    const existingCheckboxes = self._dialog.querySelectorAll(".policy-check");
    existingCheckboxes.forEach((checkbox) => {
      checkedStates[checkbox.value] = checkbox.checked;
    });

    // Render policy checkboxes in current order
    const newLabels = d3
      .select(self._dialog.querySelector(".policy-sim-list"))
      .html("")
      .selectAll(".policy-check-label")
      .data(self._policyOrderArray)
      .enter()
      .append("div")
      .classed("policy-check-label", true)
      .append("label");

    newLabels
      .append("input")
      .attr("type", "checkbox")
      .classed("policy-check", true)
      .attr("value", (x) => x)
      .property("checked", (x) => checkedStates[x] || false);

    newLabels.append("span").text((x) => x);

    newLabels.append("span").html((policyName) => self._renderOrderControls(policyName));

    // Apply visibility based on ordering mode
    if (self._isExplicitOrdering) {
      self._showForExplicitOrdering();
    } else {
      self._showForSimpleOrdering();
    }
  }

  /**
   * Moves a policy up (before the previous policy) in the ordering.
   *
   * Finds the policy in _policyOrderArray, swaps it with the previous element,
   * and re-renders the checkbox list to reflect the new order.
   *
   * @param {string} policyName - The name of the policy to move up.
   * @private
   */
  _movePolicyUp(policyName) {
    const self = this;

    const currentIndex = self._policyOrderArray.indexOf(policyName);

    // Cannot move up if not found or already at the top
    if (currentIndex <= 0) {
      return;
    }

    // Swap with previous element
    const temp = self._policyOrderArray[currentIndex - 1];
    self._policyOrderArray[currentIndex - 1] = self._policyOrderArray[currentIndex];
    self._policyOrderArray[currentIndex] = temp;

    // Re-render to reflect the new order
    self._renderPolicyCheckboxes();
  }

  /**
   * Moves a policy down (after the next policy) in the ordering.
   *
   * Finds the policy in _policyOrderArray, swaps it with the next element,
   * and re-renders the checkbox list to reflect the new order.
   *
   * @param {string} policyName - The name of the policy to move down.
   * @private
   */
  _movePolicyDown(policyName) {
    const self = this;

    const currentIndex = self._policyOrderArray.indexOf(policyName);

    // Cannot move down if not found or already at the bottom
    if (currentIndex === -1 || currentIndex >= self._policyOrderArray.length - 1) {
      return;
    }

    // Swap with next element
    const temp = self._policyOrderArray[currentIndex + 1];
    self._policyOrderArray[currentIndex + 1] = self._policyOrderArray[currentIndex];
    self._policyOrderArray[currentIndex] = temp;

    // Re-render to reflect the new order
    self._renderPolicyCheckboxes();
  }
}

export {SimulationListPresenter};
