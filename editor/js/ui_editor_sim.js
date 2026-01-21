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
    const newItems = itemList.selectAll("li")
      .data(simulationNames)
      .enter()
      .append("li");

    newItems.attr("aria-label", (x) => x);

    const buttonsPane = newItems.append("div").classed("list-buttons", true);

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

    self._dialog.addEventListener("click", (event) => {
      self._handleMoveLinkClick(event);
    });
  }

  /**
   * Handles click events for move up/down links within the policy list.
   *
   * Uses event delegation to handle clicks on move policy links. When a move
   * up link is clicked, moves the policy before the previous policy. When a
   * move down link is clicked, moves the policy after the next policy.
   *
   * @param {Event} event - The click event.
   * @private
   */
  _handleMoveLinkClick(event) {
    const self = this;
    const target = event.target;

    if (target.classList.contains("move-policy-up-link")) {
      event.preventDefault();
      const policyName = target.getAttribute("data-policy-name");
      self._movePolicyUp(policyName);
      return;
    }

    if (target.classList.contains("move-policy-down-link")) {
      event.preventDefault();
      const policyName = target.getAttribute("data-policy-name");
      self._movePolicyDown(policyName);
      return;
    }
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

    const allPolicyNames = self._getCodeObj()
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
   * Uses simple ordering mode if any of these rules are true:
   * 1. No policies are selected
   * 2. Only one policy is selected
   * 3. Selected policies are in ascending alphabetical order
   *
   * Otherwise, uses explicit ordering mode.
   *
   * @param {string[]} policiesSelected - Array of selected policy names in their current order.
   * @param {string[]} allPolicies - Array of all available policy names.
   * @returns {boolean} True if explicit ordering mode, false for simple ordering mode.
   * @private
   */
  _determineOrderingMode(policiesSelected, allPolicies) {
    const self = this;

    if (policiesSelected.length === 0) {
      return false;
    }

    if (policiesSelected.length === 1) {
      return false;
    }

    const sortedSelected = [...policiesSelected].sort();
    const isAlphabetical = policiesSelected.every(
      (policy, index) => policy === sortedSelected[index],
    );

    return !isAlphabetical;
  }

  /**
   * Determines the order in which policies should be rendered in the dialog.
   *
   * Simple mode: All policies sorted alphabetically.
   * Explicit mode: Checked policies in their original order, followed by
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

    const isSimpleMode = !isExplicitMode;
    if (isSimpleMode) {
      return [...allPolicies].sort();
    }

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

    const enableHolder = self._dialog.querySelector(".enable-policy-order-holder");
    if (enableHolder) {
      enableHolder.style.display = "inline";
    }

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

    const enableHolder = self._dialog.querySelector(".enable-policy-order-holder");
    if (enableHolder) {
      enableHolder.style.display = "none";
    }

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

    const policySimList = self._dialog.querySelector(".policy-sim-list");
    const newLabels = d3.select(policySimList)
      .html("")
      .selectAll(".policy-check-label")
      .data(self._policyOrderArray)
      .enter()
      .append("div")
      .classed("policy-check-label", true)
      .append("label");

    newLabels.append("input")
      .attr("type", "checkbox")
      .classed("policy-check", true)
      .attr("value", (x) => x)
      .property("checked", (x) => checkedStates[x] || false);

    newLabels.append("span").text((x) => x);

    newLabels.append("span").html((policyName) => self._renderOrderControls(policyName));

    if (self._isExplicitOrdering) {
      self._showForExplicitOrdering();
    } else {
      self._showForSimpleOrdering();
    }

    self._updateMoveControlVisibility();
  }

  /**
   * Moves a policy in the ordering by swapping with an adjacent element.
   *
   * @param {number} index - Current index of the policy in _policyOrderArray.
   * @param {boolean} advance - True to move forward (+1), false to move backward (-1).
   * @private
   */
  _movePolicy(index, advance) {
    const self = this;
    const targetIndex = advance ? index + 1 : index - 1;

    const temp = self._policyOrderArray[targetIndex];
    self._policyOrderArray[targetIndex] = self._policyOrderArray[index];
    self._policyOrderArray[index] = temp;

    self._renderPolicyCheckboxes();
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

    if (currentIndex <= 0) {
      return;
    }

    self._movePolicy(currentIndex, false);
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
    const outside = currentIndex === -1;
    const atEnd = currentIndex >= self._policyOrderArray.length - 1;

    if (outside || atEnd) {
      return;
    }

    self._movePolicy(currentIndex, true);
  }

  /**
   * Updates visibility of individual move up/down links based on position.
   *
   * Hides "move before" link for the first policy (cannot move up).
   * Hides "move after" link for the last policy (cannot move down).
   * Hides separator when either adjacent link is hidden for clean presentation.
   *
   * This method should be called after rendering the policy checkboxes.
   *
   * @private
   */
  _updateMoveControlVisibility() {
    const self = this;

    const policyLabels = self._dialog.querySelectorAll(".policy-check-label");
    const noPoliciesToUpdate = policyLabels.length === 0;

    if (noPoliciesToUpdate) {
      return;
    }

    policyLabels.forEach((label, index) => {
      const isFirst = index === 0;
      const isLast = index === policyLabels.length - 1;

      const moveUpLink = label.querySelector(".move-policy-up-link");
      const moveDownLink = label.querySelector(".move-policy-down-link");
      const separator = label.querySelector(".move-policy-sep");

      if (moveUpLink) {
        moveUpLink.style.display = isFirst ? "none" : "inline";
      }

      if (moveDownLink) {
        moveDownLink.style.display = isLast ? "none" : "inline";
      }

      if (separator) {
        separator.style.display = isFirst || isLast ? "none" : "inline";
      }
    });
  }
}

export {SimulationListPresenter};
