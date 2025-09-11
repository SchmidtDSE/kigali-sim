/**
 * Utilities for handling entity duplication and name conflict resolution.
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
  RechargeCommand,
  RecycleCommand,
  ReplaceCommand,
  SimulationScenario,
  Substance,
} from "ui_translator";

/**
 * Result of name conflict resolution.
 */
class NameConflictResolution {
  /**
   * Create a new NameConflictResolution.
   *
   * @param {string} originalName - The original name that was requested.
   * @param {string} resolvedName - The final name after conflict resolution.
   */
  constructor(originalName, resolvedName) {
    const self = this;
    self._originalName = originalName;
    self._resolvedName = resolvedName;
  }

  /**
   * Check if the name was changed during conflict resolution.
   *
   * @returns {boolean} True if the name was changed, false otherwise.
   */
  getNameChanged() {
    const self = this;
    return self._originalName !== self._resolvedName;
  }

  /**
   * Get the final resolved name.
   *
   * @returns {string} The resolved name.
   */
  getNewName() {
    const self = this;
    return self._resolvedName;
  }
}

/**
 * Resolves name conflicts by appending incrementing numbers until finding a unique name.
 *
 * @param {string} baseName - The initial desired name.
 * @param {Set<string>} existingNames - Set of existing names to avoid conflicts with.
 * @returns {NameConflictResolution} A resolution object with the result.
 */
function resolveNameConflict(baseName, existingNames) {
  if (!existingNames.has(baseName)) {
    return new NameConflictResolution(baseName, baseName);
  }

  let counter = 1;
  let candidate = `${baseName} (${counter})`;

  while (existingNames.has(candidate)) {
    counter++;
    candidate = `${baseName} (${counter})`;
  }

  return new NameConflictResolution(baseName, candidate);
}

/**
 * Resolves substance name conflicts with special handling for effective substance names.
 * This function handles the combination of substance and equipment model names.
 *
 * @param {string} baseName - The initial desired substance name.
 * @param {Set<string>} existingNames - Set of existing substance names to avoid conflicts with.
 * @returns {NameConflictResolution} A resolution object with the result.
 */
function resolveSubstanceNameConflict(baseName, existingNames) {
  return resolveNameConflict(baseName, existingNames);
}

/**
 * Presenter for managing entity duplication dialog functionality.
 */
class DuplicateEntityPresenter {
  /**
   * Creates a new DuplicateEntityPresenter.
   *
   * @param {Function} getCodeObj - Callback to get the current code object.
   * @param {Function} onCodeObjUpdate - Callback when code object is updated.
   */
  constructor(getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._dialog = document.getElementById("duplicate-entity-dialog");
    self._setupDialog();
  }

  /**
   * Set up dialog event handlers and dynamic behavior.
   * @private
   */
  _setupDialog() {
    const self = this;

    // Link to open dialog
    const duplicateLink = document.querySelector(".duplicate-entity-link");
    duplicateLink.addEventListener("click", (event) => {
      event.preventDefault();
      self._refreshEntityDropdown();
      self._dialog.showModal();
    });

    // Entity type change handler - update source entity dropdown and equipment field visibility
    const entityTypeInput = self._dialog.querySelector(".duplicate-entity-type-input");
    entityTypeInput.addEventListener("change", () => {
      self._refreshEntityDropdown();
      self._updateNewNameSuggestion();
      self._toggleEquipmentModelField();
    });

    // Source entity change handler - suggest new name
    const sourceEntityInput = self._dialog.querySelector(".duplicate-source-entity-input");
    sourceEntityInput.addEventListener("change", () => {
      self._updateNewNameSuggestion();
    });

    // Equipment model change handler - update name suggestion
    const equipmentModelInput = self._dialog.querySelector(".duplicate-equipment-model-input");
    equipmentModelInput.addEventListener("input", () => {
      self._updateNewNameSuggestion();
    });

    // Save button handler
    const saveButton = self._dialog.querySelector(".save-button");
    saveButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._duplicateEntity();
    });

    // Cancel button handler
    const cancelButton = self._dialog.querySelector(".cancel-button");
    cancelButton.addEventListener("click", (event) => {
      event.preventDefault();
      self._dialog.close();
    });
  }

  /**
   * Refresh the source entity dropdown based on selected entity type.
   * @private
   */
  _refreshEntityDropdown() {
    const self = this;
    const entityType = self._dialog.querySelector(".duplicate-entity-type-input").value;
    const sourceDropdown = self._dialog.querySelector(".duplicate-source-entity-input");
    const codeObj = self._getCodeObj();

    // Clear existing options
    sourceDropdown.innerHTML = "";

    const entityMappers = {
      application: () => codeObj.getApplications().map((app) => ({
        name: app.getName(),
        value: app.getName(),
      })),
      policy: () => codeObj.getPolicies().map((policy) => ({
        name: policy.getName(),
        value: policy.getName(),
      })),
      simulation: () => codeObj.getScenarios().map((scenario) => ({
        name: scenario.getName(),
        value: scenario.getName(),
      })),
      substance: () => codeObj.getSubstances().map((substance) => ({
        name: substance.getName(),
        value: substance.getName(),
        application: self._findSubstanceApplication(codeObj, substance.getName()),
      })),
    };

    const mapper = entityMappers[entityType];
    const entities = mapper ? mapper() : [];

    // Add options to dropdown
    if (entities.length === 0) {
      const option = document.createElement("option");
      option.value = "";
      option.textContent = `No ${entityType}s available`;
      option.disabled = true;
      sourceDropdown.appendChild(option);
    } else {
      entities.forEach((entity) => {
        const option = document.createElement("option");
        option.value = entity.value;
        option.textContent = entity.name;
        sourceDropdown.appendChild(option);
      });
    }
  }

  /**
   * Update the new name suggestion based on selected source entity.
   * @private
   */
  _updateNewNameSuggestion() {
    const self = this;
    const entityType = self._dialog.querySelector(".duplicate-entity-type-input").value;
    const sourceEntity = self._dialog.querySelector(".duplicate-source-entity-input").value;
    const newNameInput = self._dialog.querySelector(".duplicate-new-name-input");
    const equipmentModel = self._dialog.querySelector(".duplicate-equipment-model-input").value;

    if (sourceEntity && sourceEntity !== "") {
      if (entityType === "substance" && equipmentModel && equipmentModel.trim() !== "") {
        // Generate compound name suggestion
        newNameInput.value = `${sourceEntity} - ${equipmentModel.trim()}`;
      } else {
        newNameInput.value = `${sourceEntity} Copy`;
      }
    }
  }

  /**
   * Execute the entity duplication operation.
   * @private
   */
  _duplicateEntity() {
    const self = this;
    const entityType = self._dialog.querySelector(".duplicate-entity-type-input").value;
    const sourceEntityName = self._dialog.querySelector(".duplicate-source-entity-input").value;
    const newName = self._dialog.querySelector(".duplicate-new-name-input").value.trim();

    // Validation
    if (!sourceEntityName) {
      alert("Please select a source entity to duplicate.");
      return;
    }

    if (!newName) {
      alert("Please enter a name for the new entity.");
      return;
    }

    const codeObj = self._getCodeObj();

    try {
      const duplicators = {
        application: () => self._duplicateApplication(codeObj, sourceEntityName, newName),
        policy: () => self._duplicatePolicy(codeObj, sourceEntityName, newName),
        simulation: () => self._duplicateSimulation(codeObj, sourceEntityName, newName),
        substance: () => self._duplicateSubstance(codeObj, sourceEntityName, newName),
      };

      const duplicator = duplicators[entityType];
      if (!duplicator) {
        throw new Error(`Unknown entity type: ${entityType}`);
      }
      duplicator();

      // Run validation checks that might show confirmation dialogs
      // If user cancels any confirmation, keep dialog open
      if (!self._validateBeforeUpdate(entityType)) {
        return; // User cancelled validation, keep dialog open
      }

      self._onCodeObjUpdate(codeObj);
      self._dialog.close();
    } catch (error) {
      console.error("Error duplicating entity:", error);
      alert(`Error duplicating ${entityType}: ${error.message}`);
    }
  }

  /**
   * Validate the duplicate dialog before updating code object.
   * Runs validation checks that may show confirmation dialogs.
   * @param {string} entityType - The type of entity being duplicated
   * @returns {boolean} True if validation passes or user confirms, false if user cancels
   * @private
   */
  _validateBeforeUpdate(entityType) {
    const self = this;

    // Validate numeric inputs and get user confirmation for potentially invalid values
    if (!validateNumericInputs(self._dialog, entityType)) {
      return false; // User cancelled numeric input validation
    }

    // For simulations, also check duration
    if (entityType === "simulation") {
      if (!validateSimulationDuration(self._dialog)) {
        return false; // User cancelled simulation duration validation
      }
    }

    return true;
  }

  /**
   * Duplicate an application with deep copy of all substances and commands.
   * @param {Program} codeObj - The program object to modify
   * @param {string} sourceAppName - Name of source application
   * @param {string} newName - Name for the duplicated application
   * @private
   */
  _duplicateApplication(codeObj, sourceAppName, newName) {
    const self = this;
    const sourceApp = codeObj.getApplication(sourceAppName);

    if (!sourceApp) {
      throw new Error(`Application "${sourceAppName}" not found`);
    }

    // Deep copy substances with all their commands
    const duplicatedSubstances = sourceApp.getSubstances().map((substance) => {
      return self._deepCopySubstance(substance);
    });

    // Create new application with copied substances
    const newApplication = new Application(
      newName,
      duplicatedSubstances,
      sourceApp._isModification,
      sourceApp._isCompatible,
    );

    codeObj.addApplication(newApplication);
  }

  /**
   * Duplicate a policy with deep copy of all applications and commands.
   * @param {Program} codeObj - The program object to modify
   * @param {string} sourcePolicyName - Name of source policy
   * @param {string} newName - Name for the duplicated policy
   * @private
   */
  _duplicatePolicy(codeObj, sourcePolicyName, newName) {
    const self = this;
    const sourcePolicy = codeObj.getPolicy(sourcePolicyName);

    if (!sourcePolicy) {
      throw new Error(`Policy "${sourcePolicyName}" not found`);
    }

    // Deep copy applications within the policy
    const duplicatedApplications = sourcePolicy.getApplications().map((app) => {
      const duplicatedSubstances = app.getSubstances().map((substance) => {
        return self._deepCopySubstance(substance);
      });

      return new Application(
        app.getName(), // Keep same application name (policy context)
        duplicatedSubstances,
        app._isModification,
        app._isCompatible,
      );
    });

    // Create new policy stanza
    const newPolicy = new DefinitionalStanza(
      newName,
      duplicatedApplications,
      sourcePolicy._isCompatible,
    );

    codeObj.insertPolicy(null, newPolicy);
  }

  /**
   * Duplicate a simulation scenario.
   * @param {Program} codeObj - The program object to modify
   * @param {string} sourceSimName - Name of source simulation
   * @param {string} newName - Name for the duplicated simulation
   * @private
   */
  _duplicateSimulation(codeObj, sourceSimName, newName) {
    const sourceSimulation = codeObj.getScenario(sourceSimName);

    if (!sourceSimulation) {
      throw new Error(`Simulation "${sourceSimName}" not found`);
    }

    // Create new simulation scenario with same parameters
    const newSimulation = new SimulationScenario(
      newName,
      [...sourceSimulation.getPolicyNames()], // Copy policy array
      sourceSimulation.getYearStart(),
      sourceSimulation.getYearEnd(),
      sourceSimulation._isCompatible,
    );

    codeObj.insertScenario(null, newSimulation);
  }

  /**
   * Show/hide equipment model field based on entity type selection.
   * @private
   */
  _toggleEquipmentModelField() {
    const self = this;
    const entityType = self._dialog.querySelector(".duplicate-entity-type-input").value;
    const equipmentSection = self._dialog.querySelector(".equipment-model-section");

    if (entityType === "substance") {
      equipmentSection.style.display = "block";
    } else {
      equipmentSection.style.display = "none";
      // Clear equipment model when not substance
      self._dialog.querySelector(".duplicate-equipment-model-input").value = "";
    }
  }

  /**
   * Find which application contains a specific substance.
   * @param {Program} codeObj - The program object
   * @param {string} substanceName - Name of substance to find
   * @returns {string} Application name containing the substance
   * @private
   */
  _findSubstanceApplication(codeObj, substanceName) {
    const applications = codeObj.getApplications();
    for (const app of applications) {
      if (app.getSubstances().some((sub) => sub.getName() === substanceName)) {
        return app.getName();
      }
    }
    return null;
  }

  /**
   * Duplicate a substance with optional equipment model for compound naming.
   * @param {Program} codeObj - The program object to modify
   * @param {string} sourceSubstanceName - Name of source substance
   * @param {string} newName - Name for the duplicated substance (may be compound)
   * @private
   */
  _duplicateSubstance(codeObj, sourceSubstanceName, newName) {
    const self = this;

    // Find the source substance across all applications
    let sourceSubstance = null;
    let sourceApplication = null;

    for (const app of codeObj.getApplications()) {
      const substance = app.getSubstances().find((sub) => sub.getName() === sourceSubstanceName);
      if (substance) {
        sourceSubstance = substance;
        sourceApplication = app;
        break;
      }
    }

    if (!sourceSubstance || !sourceApplication) {
      throw new Error(`Substance "${sourceSubstanceName}" not found`);
    }

    // Check for duplicate names
    const existingSubstances = codeObj.getSubstances();
    if (existingSubstances.some((sub) => sub.getName() === newName)) {
      throw new Error(`Substance "${newName}" already exists`);
    }

    // Deep copy the substance with new name
    const duplicatedSubstance = self._deepCopySubstance(sourceSubstance);
    duplicatedSubstance._name = newName;

    // Add to the same application as the source substance
    sourceApplication.insertSubstance(null, duplicatedSubstance);
  }

  /**
   * Deep copy a substance with all its commands and properties.
   * @param {Substance} sourceSubstance - The substance to copy
   * @returns {Substance} Deep copied substance
   * @private
   */
  _deepCopySubstance(sourceSubstance) {
    const self = this;

    // Deep copy all command arrays
    const copiedCharges = sourceSubstance.getInitialCharges().map((cmd) =>
      self._deepCopyCommand(cmd),
    );
    const copiedLimits = sourceSubstance.getLimits().map((cmd) =>
      self._deepCopyLimitCommand(cmd),
    );
    const copiedChanges = sourceSubstance.getChanges().map((cmd) =>
      self._deepCopyCommand(cmd),
    );
    const copiedEqualsGhg = sourceSubstance.getEqualsGhg() ?
      self._deepCopyCommand(sourceSubstance.getEqualsGhg()) : null;
    const copiedEqualsKwh = sourceSubstance.getEqualsKwh() ?
      self._deepCopyCommand(sourceSubstance.getEqualsKwh()) : null;
    const copiedRecharges = sourceSubstance.getRecharges().map((cmd) => {
      return self._deepCopyRechargeCommand(cmd);
    });
    const copiedRecycles = sourceSubstance.getRecycles().map((cmd) => {
      return self._deepCopyRecycleCommand(cmd);
    });
    const copiedReplaces = sourceSubstance.getReplaces().map((cmd) => {
      return self._deepCopyReplaceCommand(cmd);
    });
    const copiedRetire = sourceSubstance.getRetire() ?
      self._deepCopyCommand(sourceSubstance.getRetire()) : null;
    const copiedSetVals = sourceSubstance.getSetVals().map((cmd) =>
      self._deepCopyCommand(cmd),
    );
    const copiedEnables = sourceSubstance.getEnables().map((cmd) =>
      self._deepCopyCommand(cmd),
    );

    // Create new substance with copied commands (matching constructor parameter order)
    return new Substance(
      sourceSubstance.getName(),
      copiedCharges,
      copiedLimits,
      copiedChanges,
      copiedEqualsGhg,
      copiedEqualsKwh,
      copiedRecharges,
      copiedRecycles,
      copiedReplaces,
      copiedRetire,
      copiedSetVals,
      copiedEnables,
      sourceSubstance._isModification,
      sourceSubstance._isCompatible,
    );
  }

  /**
   * Deep copy a basic command.
   * @param {Command} sourceCommand - The command to copy
   * @returns {Command} Deep copied command
   * @private
   */
  _deepCopyCommand(sourceCommand) {
    const self = this;
    const value = sourceCommand.getValue();
    const engineNumber = value ? new EngineNumber(value.getValue(), value.getUnits()) : null;
    const duration = sourceCommand.getDuration();
    const yearMatcher = duration ? self._deepCopyYearMatcher(duration) : null;

    return new Command(
      sourceCommand.getTypeName(),
      sourceCommand.getTarget(),
      engineNumber,
      yearMatcher,
    );
  }

  /**
   * Deep copy a limit command.
   * @param {LimitCommand} sourceLimitCommand - The limit command to copy
   * @returns {LimitCommand} Deep copied limit command
   * @private
   */
  _deepCopyLimitCommand(sourceLimitCommand) {
    const self = this;
    const value = sourceLimitCommand.getValue();
    const engineNumber = value ? new EngineNumber(value.getValue(), value.getUnits()) : null;
    const duration = sourceLimitCommand.getDuration();
    const yearMatcher = duration ? self._deepCopyYearMatcher(duration) : null;

    return new LimitCommand(
      sourceLimitCommand.getTypeName(),
      sourceLimitCommand.getTarget(),
      engineNumber,
      yearMatcher,
      sourceLimitCommand.getDisplacing(),
    );
  }

  /**
   * Deep copy a recharge command.
   * @param {RechargeCommand} sourceRechargeCommand - The recharge command to copy
   * @returns {RechargeCommand} Deep copied recharge command
   * @private
   */
  _deepCopyRechargeCommand(sourceRechargeCommand) {
    const self = this;
    const value = sourceRechargeCommand.getValue();
    const engineNumber = value ? new EngineNumber(value.getValue(), value.getUnits()) : null;
    const duration = sourceRechargeCommand.getDuration();
    const yearMatcher = duration ? self._deepCopyYearMatcher(duration) : null;

    return new RechargeCommand(
      sourceRechargeCommand.getPopulation(),
      sourceRechargeCommand.getPopulationUnits(),
      engineNumber,
      yearMatcher,
    );
  }

  /**
   * Deep copy a recycle command.
   * @param {RecycleCommand} sourceRecycleCommand - The recycle command to copy
   * @returns {RecycleCommand} Deep copied recycle command
   * @private
   */
  _deepCopyRecycleCommand(sourceRecycleCommand) {
    const self = this;
    const target = sourceRecycleCommand.getTarget();
    const targetNumber = target ? new EngineNumber(target.getValue(), target.getUnits()) : null;
    const value = sourceRecycleCommand.getValue();
    const valueNumber = value ? new EngineNumber(value.getValue(), value.getUnits()) : null;
    const duration = sourceRecycleCommand.getDuration();
    const yearMatcher = duration ? self._deepCopyYearMatcher(duration) : null;

    return new RecycleCommand(
      targetNumber,
      valueNumber,
      yearMatcher,
      sourceRecycleCommand.getDisplacing(),
    );
  }

  /**
   * Deep copy a replace command.
   * @param {ReplaceCommand} sourceReplaceCommand - The replace command to copy
   * @returns {ReplaceCommand} Deep copied replace command
   * @private
   */
  _deepCopyReplaceCommand(sourceReplaceCommand) {
    const self = this;
    const volume = sourceReplaceCommand.getVolume();
    const volumeNumber = volume ? new EngineNumber(volume.getValue(), volume.getUnits()) : null;
    const duration = sourceReplaceCommand.getDuration();
    const yearMatcher = duration ? self._deepCopyYearMatcher(duration) : null;

    return new ReplaceCommand(
      volumeNumber,
      sourceReplaceCommand.getSource(),
      sourceReplaceCommand.getDestination(),
      yearMatcher,
    );
  }

  /**
   * Deep copy a YearMatcher duration object.
   * @param {YearMatcher} sourceYearMatcher - The year matcher to copy
   * @returns {YearMatcher} Deep copied year matcher
   * @private
   */
  _deepCopyYearMatcher(sourceYearMatcher) {
    // YearMatcher constructor takes start, end, and duration type parameters
    return new YearMatcher(
      sourceYearMatcher.getStart(),
      sourceYearMatcher.getEnd(),
      sourceYearMatcher.getDurationType(),
    );
  }
}

export {
  NameConflictResolution,
  resolveNameConflict,
  resolveSubstanceNameConflict,
  DuplicateEntityPresenter,
};
