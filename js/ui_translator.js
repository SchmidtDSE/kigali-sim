/**
 * Logic to interpret and translate scripts.
 *
 * Provides classes and utilities for converting between text-based scripts
 * and object representations used within the UI-based editor.
 *
 * @license BSD, see LICENSE.md
 */

import {EngineNumber} from "engine_number";
import {YearMatcher} from "engine_state";

/**
 * Command compatibility mapping to compatibility modes:
 *
 * - "any": Compatible with both policy and definition contexts
 * - "none": Not compatible with simplified UI
 * - "definition": Only compatible with substance definitions.
 * - "policy": Only compatible with policy modifications.
 *
 * @type {Object.<string, string>}
 */
const COMMAND_COMPATIBILITIES = {
  "change": "any",
  "define var": "none",
  "retire": "any",
  "setVal": "any",
  "cap": "any",
  "floor": "any",
  "limit": "any",
  "initial charge": "definition",
  "equals": "definition",
  "recharge": "definition",
  "recycle": "policy",
  "replace": "policy",
};

const SUPPORTED_EQUALS_UNITS = ["tCO2e / unit", "tCO2e / kg", "tCO2e / mt"];

const toolkit = QubecTalk.getToolkit();

/**
 * Indent a single piece of text by the specified number of spaces.
 *
 * @param {string} piece - The text to indent
 * @param {number} spaces - Number of spaces to indent. Defaults to 0.
 * @returns {string} The indented text
 * @private
 */
function indentSingle(piece, spaces) {
  if (spaces === undefined) {
    spaces = 0;
  }

  let prefix = "";
  for (let i = 0; i < spaces; i++) {
    prefix += " ";
  }

  return prefix + piece;
}

/**
 * Indent an array of text pieces by the specified number of spaces.
 *
 * @param {string[]} pieces - Array of text pieces to indent
 * @param {number} spaces - Number of spaces to indent each piece
 * @returns {string[]} Array of indented text pieces
 */
function indent(pieces, spaces) {
  return pieces.map((piece) => indentSingle(piece, spaces));
}

/**
 * Create a function that adds indented code pieces to a target array.
 *
 * @param {string[]} target - Target array to add code pieces to
 * @returns {Function} Function that takes a code piece and spaces count
 */
function buildAddCode(target) {
  return (x, spaces) => {
    target.push(indentSingle(x, spaces));
  };
}

/**
 * Join code pieces into a single string with newlines.
 *
 * @param {string[]} target - Array of code pieces to join
 * @returns {string} Combined code string
 */
function finalizeCodePieces(target) {
  return target.join("\n");
}

/**
 * Representation of a QubecTalk program.
 *
 * A complete program containing optionally applications, policies, and
 * scenarios.
 */
class Program {
  /**
   * Create a new Program.
   *
   * @param {Application[]} applications - Array of application definitions.
   * @param {DefinitionalStanza[]} policies - Array of policy definitions.
   * @param {SimulationScenario[]} scenarios - Array of simulation scenarios.
   * @param {boolean} isCompatible - Whether program is compatible with UI editing.
   */
  constructor(applications, policies, scenarios, isCompatible) {
    const self = this;
    self._applications = applications;
    self._policies = policies;
    self._scenarios = scenarios;
    self._isCompatible = isCompatible && self._passesTempCompatiblityTests();
  }

  /**
   * Get all substances across all applications.
   *
   * @returns {Substance[]} Array of all substances.
   */
  getSubstances() {
    const self = this;
    return self
      .getApplications()
      .map((x) => x.getSubstances())
      .flat();
  }

  /**
   * Insert or updates a substance in an application.
   *
   * @param {string} priorApplication - Name of application to insert into.
   * @param {string} priorSubstanceName - Name of substance to replace. Pass
   *     null for new.
   * @param {Substance} substance - The substance to insert.
   */
  insertSubstance(priorApplication, priorSubstanceName, substance) {
    const self = this;
    const application = self.getApplication(priorApplication);
    application.insertSubstance(priorSubstanceName, substance);
  }

  /**
   * Delete a substance from an application.
   *
   * @param {string} applicationName - Name of application containing
   *     substance.
   * @param {string} substanceName - Name of substance to delete.
   */
  deleteSubstance(applicationName, substanceName) {
    const self = this;
    const application = self.getApplication(applicationName);
    application.deleteSubstance(substanceName);
    self._policies = self._policies.filter((x) => {
      const application = x.getApplications()[0];
      const substance = application.getSubstances()[0];
      const candidateName = substance.getName();
      return candidateName !== substanceName;
    });
    self._removeUnknownPoliciesFromScenarios();
  }

  /**
   * Get all applications.
   *
   * @returns {Application[]} Array of applications.
   */
  getApplications() {
    const self = this;
    return self._applications;
  }

  /**
   * Gets an application by name.
   *
   * @param {string} name - Name of application to find.
   * @returns {Application|null} The application or null if not found.
   */
  getApplication(name) {
    const self = this;
    const matching = self._applications.filter((x) => x.getName() === name);
    return matching.length == 0 ? null : matching[0];
  }

  /**
   * Add a new application.
   *
   * @param {Application} newApplication - Application to add.
   */
  addApplication(newApplication) {
    const self = this;
    self._applications.push(newApplication);
  }

  /**
   * Delete an application by name.
   *
   * @param {string} name - Name of application to delete.
   */
  deleteApplication(name) {
    const self = this;
    self._applications = self._applications.filter((x) => x.getName() !== name);
    self._policies = self._policies.filter((x) => x.getApplications()[0].getName() !== name);
    self._removeUnknownPoliciesFromScenarios();
  }

  /**
   * Rename an application.
   *
   * @param {string} oldName - Current name of application.
   * @param {string} newName - New name for application.
   */
  renameApplication(oldName, newName) {
    const self = this;
    const priorApplications = self._applications.filter((x) => x.getName() === oldName);
    priorApplications.forEach((x) => x.rename(newName));
  }

  /**
   * Get all policies.
   *
   * @returns {DefinitionalStanza[]} Array of policies.
   */
  getPolicies() {
    const self = this;
    return self._policies;
  }

  /**
   * Get a policy by name.
   *
   * @param {string} name - Name of policy to find.
   * @returns {DefinitionalStanza|null} The policy or null if not found.
   */
  getPolicy(name) {
    const self = this;
    const matching = self._policies.filter((x) => x.getName() === name);
    return matching.length == 0 ? null : matching[0];
  }

  /**
   * Delete a policy by name.
   *
   * @param {string} name - Name of policy to delete.
   * @param {boolean} [filterUnknown=true] - Whether to filter unknown policies.
   */
  deletePolicy(name, filterUnknown) {
    const self = this;

    if (filterUnknown === undefined) {
      filterUnknown = true;
    }

    self._policies = self._policies.filter((x) => x.getName() !== name);

    if (filterUnknown) {
      self._removeUnknownPoliciesFromScenarios();
    }
  }

  /**
   * Insert or update a policy.
   *
   * @param {string} oldName - Name of policy to replace, or null for new.
   * @param {DefinitionalStanza} newPolicy - Policy to insert.
   */
  insertPolicy(oldName, newPolicy) {
    const self = this;
    const nameChange = oldName !== newPolicy.getName();
    self.deletePolicy(oldName, nameChange);
    self._policies.push(newPolicy);
  }

  /**
   * Get all simulation scenarios.
   *
   * @returns {SimulationScenario[]} Array of scenarios.
   */
  getScenarios() {
    const self = this;
    return self._scenarios;
  }

  /**
   * Get a simulation scenario by name.
   *
   * @param {string} name - Name of scenario to find.
   * @returns {SimulationScenario|null} The scenario or null if not found.
   */
  getScenario(name) {
    const self = this;
    const matching = self._scenarios.filter((x) => x.getName() === name);
    return matching.length == 0 ? null : matching[0];
  }

  /**
   * Delete a simulation scenario by name.
   *
   * @param {string} name - Name of scenario to delete.
   */
  deleteScenario(name) {
    const self = this;
    self._scenarios = self._scenarios.filter((x) => x.getName() !== name);
  }

  /**
   * Insert or update a simulation scenario.
   *
   * @param {string} oldName - Name of scenario to replace, or null for new.
   * @param {SimulationScenario} scenario - Scenario to insert.
   */
  insertScenario(oldName, scenario) {
    const self = this;
    self.deleteScenario(oldName);
    self._scenarios.push(scenario);
  }

  /**
   * Gets whether program is compatible with UI editing.
   *
   * @returns {boolean} True if compatible, false otherwise.
   */
  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  /**
   * Generates the code representation of the program with the specified indentation.
   *
   * @param {number} spaces - Number of spaces to use for indenting the generated code.
   * @returns {string} The code representation of the program with specified indentation.
   */
  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    if (self.getApplications().length > 0) {
      const applicationsCode = self
        .getApplications()
        .map((x) => x.toCode(spaces + 2))
        .join("\n\n\n");

      addCode("start default", spaces);
      addCode("", spaces);
      addCode(applicationsCode, 0);
      addCode("", spaces);
      addCode("end default", spaces);
      addCode("", spaces);
      addCode("", spaces);
    }

    if (self.getPolicies().length > 0) {
      const policiesCode = self
        .getPolicies()
        .map((x) => x.toCode(spaces))
        .join("\n\n\n\n");
      addCode(policiesCode, spaces);
      addCode("", spaces);
      addCode("", spaces);
    }

    if (self.getScenarios().length > 0) {
      addCode("start simulations", spaces);
      addCode("", spaces);
      const scenariosCode = self
        .getScenarios()
        .map((x) => x.toCode(2))
        .join("\n\n\n");
      addCode(scenariosCode, spaces);
      addCode("", spaces);
      addCode("end simulations", spaces);
    }

    return finalizeCodePieces(baselinePieces);
  }

  /**
   * Removing policies that are not compatible with the UI editor.
   *
   * Filters each scenario to include only the policies that are in the known
   * policies list that are compatible with the UI-based editor. It
   * subsequently updates each scenario with the filtered list of policies.
   *
   * @private
   */
  _removeUnknownPoliciesFromScenarios() {
    const self = this;
    const knownPolicies = new Set(self._policies.map((x) => x.getName()));
    self._scenarios = self._scenarios.map((scenario) => {
      if (!scenario.getIsCompatible()) {
        return scenario;
      }

      const name = scenario.getName();
      const start = scenario.getYearStart();
      const end = scenario.getYearEnd();

      const selectedPolicies = scenario.getPolicyNames();
      const allowedPolicies = selectedPolicies.filter((x) => knownPolicies.has(x));

      return new SimulationScenario(name, allowedPolicies, start, end, true);
    });
  }

  /**
   * Determine if the compatibility tests are passed.
   *
   * Evaluate the compatibility of applications and policies with specific
   * conditions that must be satisfied to pass the compatibility tests.
   *
   * @private
   * @returns {boolean} True if all temporary compatibility tests are passed or
   *     false otherwise.
   */
  _passesTempCompatiblityTests() {
    const self = this;

    const problematicApplications = self._applications.filter((application) => {
      const substances = application.getSubstances();
      const problematicSubstances = substances.filter((substance) => {
        const durationIsFullSpan = (duration) => {
          if (duration === null) {
            return true;
          }
          const durationFullSpan = duration.getStart() === null && duration.getEnd() === null;
          return durationFullSpan;
        };

        const getInitialChargeProblematic = () => {
          const initialCharges = substance.getInitialCharges();
          const uniqueTargets = new Set(initialCharges.map((x) => x.getTarget()));
          if (uniqueTargets.size != initialCharges.length) {
            return true;
          }
          const initialChargesWithDuration = initialCharges.filter((initialCharge) => {
            const duration = initialCharge.getDuration();
            return !durationIsFullSpan(duration);
          });
          if (initialChargesWithDuration.length > 0) {
            return true;
          }
        };

        const getEqualsProblematic = () => {
          const equals = substance.getEquals();
          if (equals === null) {
            return false;
          } else {
            const duration = equals.getDuration();

            if (!durationIsFullSpan(duration)) {
              return true;
            }

            const value = equals.getValue();
            const units = value.getUnits();
            if (!SUPPORTED_EQUALS_UNITS.includes(units)) {
              return true;
            }

            return false;
          }
        };

        return getInitialChargeProblematic() || getEqualsProblematic();
      });
      return problematicSubstances.length > 0;
    });

    const applicationsOk = problematicApplications.length == 0;

    const problematicPolicies = self._policies.filter((policy) => {
      const applications = policy.getApplications();
      if (applications.length != 1) {
        return true;
      }

      const application = applications[0];
      const substances = application.getSubstances();
      if (substances.length != 1) {
        return true;
      }

      return false;
    });

    const policiesOk = problematicPolicies.length == 0;

    return applicationsOk && policiesOk;
  }
}

/**
 * An "about" stanza in the QubecTalk script.
 */
class AboutStanza {
  /**
   * Gets the name of this stanza.
   * @returns {string} The stanza name "about".
   */
  getName() {
    const self = this;
    return "about";
  }

  /**
   * Generates the code representation of the about stanza.
   *
   * @param {number} spaces - Number of spaces for indentation.
   * @returns {string} Code representation of the stanza.
   */
  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    addCode("start about", spaces);
    addCode("end about", spaces);

    return finalizeCodePieces(baselinePieces);
  }

  /**
   * Checks compatibility of the about stanza with UI editing.
   *
   * @returns {boolean} False as about stanza is not compatible with UI.
   */
  getIsCompatible() {
    const self = this;
    return false;
  }
}

/**
 * Definitional stanza that can contain application and / or policies.
 */
class DefinitionalStanza {
  /**
   * Create a new DefinitionalStanza.
   *
   * @param {string} name - Name of the stanza.
   * @param {Application[]} applications - Array of applications.
   * @param {boolean} isCompatible - Whether stanza is UI-compatible.
   */
  constructor(name, applications, isCompatible) {
    const self = this;
    self._name = name;
    self._applications = applications;
    self._isCompatible = isCompatible;
  }

  /**
   * Get the name of this definitional stanza.
   *
   * @returns {string} The name of the stanza ("default" or policy name).
   */
  getName() {
    const self = this;
    return self._name;
  }

  /**
   * Get the applications defined in this stanza.
   *
   * @returns {Application[]} Array of applications defined in the stanza.
   */
  getApplications() {
    const self = this;
    return self._applications;
  }

  /**
   * Check if this stanza is compatible with UI editing.
   *
   * @returns {boolean} True if stanza can be edited in UI, false otherwise.
   */
  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  /**
   * Generate the code representation of this stanza.
   *
   * Generate the QubecTalk code representation of this definitional stanza,
   * including all its applications and appropriate indentation.
   *
   * @param {number} spaces - Number of spaces to use for indentation.
   * @returns {string} The code representation of the stanza.
   */
  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);
    const isDefault = self.getName() === "default";

    addCode("start " + (isDefault ? "default" : 'policy "' + self.getName() + '"'), spaces);
    addCode("", spaces);

    if (self.getApplications().length > 0) {
      const applicationsCode = self
        .getApplications()
        .map((x) => x.toCode(spaces + 2))
        .join("\n\n\n");
      addCode(applicationsCode, 0);
    }

    addCode("", spaces);
    addCode("end " + (isDefault ? "default" : "policy"), spaces);

    return finalizeCodePieces(baselinePieces);
  }
}

/**
 * Represent a simulation scenario that applies policies over a time period.
 */
class SimulationScenario {
  /**
   * Create a new SimulationScenario.
   *
   * @param {string} name - Name of the scenario.
   * @param {string[]} policyNames - Array of policy names to apply.
   * @param {number} yearStart - Start year of simulation.
   * @param {number} yearEnd - End year of simulation.
   * @param {boolean} isCompatible - Whether scenario is UI-compatible.
   */
  constructor(name, policyNames, yearStart, yearEnd, isCompatible) {
    const self = this;
    self._name = name;
    self._policyNames = policyNames;
    self._isCompatible = isCompatible;

    const yearStartRearrange = Math.min(yearStart, yearEnd);
    const yearEndRearrange = Math.max(yearStart, yearEnd);

    self._yearStart = yearStartRearrange;
    self._yearEnd = yearEndRearrange;
  }

  /**
   * Get the name of this simulation scenario.
   *
   * @returns {string} The scenario name.
   */
  getName() {
    const self = this;
    return self._name;
  }

  /**
   * Get names of policies included in this scenario.
   *
   * @returns {string[]} Array of policy names to apply.
   */
  getPolicyNames() {
    const self = this;
    return self._policyNames;
  }

  /**
   * Get the start year of the simulation.
   *
   * @returns {number} The year the simulation starts.
   */
  getYearStart() {
    const self = this;
    return self._yearStart;
  }

  /**
   * Get the end year of the simulation.
   *
   * @returns {number} The year the simulation ends.
   */
  getYearEnd() {
    const self = this;
    return self._yearEnd;
  }

  /**
   * Check if this scenario is compatible with UI editing.
   *
   * @returns {boolean} True if scenario can be edited in UI, false otherwise.
   */
  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  /**
   * Generate the code representation of this scenario.
   *
   * @param {number} spaces - Number of spaces to use for indentation.
   * @returns {string} The code representation of the simulation scenario.
   */
  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    addCode('simulate "' + self.getName() + '"', spaces);

    if (self.getPolicyNames().length > 0) {
      self.getPolicyNames().forEach((x, i) => {
        const prefix = i == 0 ? "using" : "then";
        addCode(prefix + ' "' + x + '"', spaces + 2);
      });
    }

    addCode("from years " + self.getYearStart() + " to " + self.getYearEnd(), spaces);
    return finalizeCodePieces(baselinePieces);
  }
}

/**
 * Simulations stanza that contains multiple simulation scenarios.
 */
class SimulationStanza {
  /**
   * Create a new SimulationStanza.
   *
   * @param {SimulationScenario[]} scenarios - Array of simulation scenarios.
   * @param {boolean} isCompatible - Whether stanza is compatible with UI editing.
   */
  constructor(scenarios, isCompatible) {
    const self = this;
    self._scenarios = scenarios;
    self._isCompatible = isCompatible;
  }

  /**
   * Check if this stanza is compatible with UI editing.
   *
   * @returns {boolean} True if stanza can be edited in UI, false otherwise.
   */
  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  /**
   * Get the simulation scenarios in this stanza.
   *
   * @returns {SimulationScenario[]} Array of simulation scenarios.
   */
  getScenarios() {
    const self = this;
    return self._scenarios;
  }

  /**
   * Get the name of this stanza.
   *
   * @returns {string} The string "simulations".
   */
  getName() {
    const self = this;
    return "simulations";
  }

  /**
   * Generate the code representation of this stanza.
   *
   * Generates the QubecTalk code representation of this simulations stanza,
   * including all its scenarios and appropriate indentation.
   *
   * @param {number} spaces - Number of spaces to use for indentation.
   * @returns {string} The code representation of the stanza.
   */
  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    addCode("start simulations", spaces);

    if (self.getScenarios().length > 0) {
      addCode("", spaces);
      const scenariosCode = self
        .getScenarios()
        .map((x) => x.toCode(2))
        .join("\n\n\n");
      addCode(scenariosCode, spaces);
      addCode("", spaces);
    }

    addCode("end simulations", spaces);
    return finalizeCodePieces(baselinePieces);
  }
}

/**
 * Represent an application that contains substances and their properties.
 */
class Application {
  /**
   * Create a new Application.
   *
   * @param {string} name - Name of the application.
   * @param {Substance[]} substances - Array of substances.
   * @param {boolean} isModification - Whether this modifies existing application.
   * @param {boolean} isCompatible - Whether application is UI-compatible.
   */
  constructor(name, substances, isModification, isCompatible) {
    const self = this;
    self._name = name;
    self._substances = substances;
    self._isModification = isModification;
    self._isCompatible = isCompatible;
  }

  /**
   * Get the name of this application.
   *
   * @returns {string} The application name.
   */
  getName() {
    const self = this;
    return self._name;
  }

  /**
   * Rename this application.
   *
   * @param {string} newName - The new name for the application.
   */
  rename(newName) {
    const self = this;
    self._name = newName;
  }

  /**
   * Get all substances defined in this application.
   *
   * @returns {Substance[]} Array of substances.
   */
  getSubstances() {
    const self = this;
    return self._substances;
  }

  /**
   * Insert or update a substance in this application.
   *
   * @param {string} substanceName - Name of substance to replace, or null for new.
   * @param {Substance} newVersion - The substance to insert.
   */
  insertSubstance(substanceName, newVersion) {
    const self = this;
    self.deleteSubstance(substanceName);
    self._substances.push(newVersion);
  }

  /**
   * Delete a substance from this application.
   *
   * @param {string} substanceName - Name of substance to delete.
   */
  deleteSubstance(substanceName) {
    const self = this;
    self._substances = self._substances.filter((x) => x.getName() !== substanceName);
  }

  /**
   * Get a specific substance by name.
   *
   * @param {string} name - Name of substance to find.
   * @returns {Substance|null} The substance or null if not found.
   */
  getSubstance(name) {
    const self = this;
    const matching = self._substances.filter((x) => x.getName() === name);
    return matching.length == 0 ? null : matching[0];
  }

  /**
   * Check if this application modifies an existing one.
   *
   * @returns {boolean} True if this modifies an existing application.
   */
  getIsModification() {
    const self = this;
    return self._isModification;
  }

  /**
   * Check if this application is compatible with UI editing.
   *
   * @returns {boolean} True if application can be edited in UI.
   */
  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  /**
   * Generate the code representation of this application.
   *
   * @param {number} spaces - Number of spaces to use for indentation.
   * @returns {string} The code representation of the application.
   */
  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    const prefix = self.getIsModification() ? "modify" : "define";
    addCode(prefix + ' application "' + self.getName() + '"', spaces);

    if (self.getSubstances().length > 0) {
      addCode("", spaces);
      const substancesCode = self
        .getSubstances()
        .map((x) => x.toCode(spaces + 2))
        .join("\n\n\n");
      addCode(substancesCode, 0);
      addCode("", spaces);
    }

    addCode("end application", spaces);
    return finalizeCodePieces(baselinePieces);
  }
}

/**
 * Build substances with their properties and commands.
 *
 * Provides a stateful interface for constructing Substances with various
 * commands and properties.
 */
class SubstanceBuilder {
  /**
   * Create a new SubstanceBuilder.
   *
   * @param {string} name - Name of the substance.
   * @param {boolean} isModification - Whether this modifies an existing substance.
   */
  constructor(name, isModification) {
    const self = this;
    self._name = name;
    self._isModification = isModification;
    self._initialCharges = [];
    self._limits = [];
    self._changes = [];
    self._equalsGhg = null;
    self._equalsKwh = null;
    self._recharge = null;
    self._recycles = [];
    self._replaces = [];
    self._retire = null;
    self._setVals = [];
  }

  /**
   * Build a new Substance from the current state.
   *
   * @param {boolean} isCompatibleRaw - Whether substance should be UI-compatible.
   * @returns {Substance} The constructed substance.
   */
  build(isCompatibleRaw) {
    const self = this;

    const commandsConsolidatedInterpreted = [
      self._initialCharges,
      self._limits,
      self._recycles,
      self._replaces,
      [self._equalsGhg, self._equalsKwh, self._recharge, self._retire],
      self._changes,
      self._setVals,
    ].flat();
    const isCompatibleInterpreted = commandsConsolidatedInterpreted
      .filter((x) => x !== null)
      .map((x) => x.getIsCompatible())
      .reduce((a, b) => a && b, true);

    const initialChargeTargets = self._initialCharges.map((x) => x.getTarget());
    const initialChargeTargetsUnique = new Set(initialChargeTargets);
    const initialChargesNonOverlap = initialChargeTargets.length == initialChargeTargetsUnique.size;

    const isCompatible = isCompatibleRaw && isCompatibleInterpreted && initialChargesNonOverlap;

    return new Substance(
      self._name,
      self._initialCharges,
      self._limits,
      self._changes,
      self._equals,
      self._recharge,
      self._recycles,
      self._replaces,
      self._retire,
      self._setVals,
      self._isModification,
      isCompatible,
    );
  }

  /**
   * Add a command to the substance being built.
   *
   * @param {Command} command - The command to add.
   * @returns {Command|IncompatibleCommand} The added command or incompatibility marker.
   */
  addCommand(command) {
    const self = this;

    const commandType = command.getTypeName();
    const compatibilityType = COMMAND_COMPATIBILITIES[commandType];
    if (compatibilityType === undefined) {
      throw "Unknown compatibility type for " + commandType;
    }

    const requiresMod = compatibilityType === "policy";
    const requiresDefinition = compatibilityType === "definition";
    const noCompat = compatibilityType === "none";

    const needsToMoveToMod = requiresMod && !self._isModification;
    const needsToMoveToDefinition = requiresDefinition && self._isModification;
    const incompatiblePlace = needsToMoveToMod || needsToMoveToDefinition || noCompat;

    const strategy = {
      "change": (x) => self.addChange(x),
      "retire": (x) => self.setRetire(x),
      "setVal": (x) => self.addSetVal(x),
      "initial charge": (x) => self.addInitialCharge(x),
      "recharge": (x) => self.setRecharge(x),
      "equals": (x) => {
        const units = x.getValue().getUnits();
        if (units.includes("kwh")) {
          return self.setEqualsKwh(x);
        } else {
          return self.setEqualsGhg(x);
        }
      },
      "recycle": (x) => self.addRecycle(x),
      "cap": (x) => self.addLimit(x),
      "floor": (x) => self.addLimit(x),
      "replace": (x) => self.addReplace(x),
    }[commandType];

    if (incompatiblePlace) {
      return self._makeInvalidPlacement();
    } else {
      return strategy(command);
    }
  }

  /**
   * Set the name of the substance.
   *
   * @param {string} newVal - New name for the substance.
   */
  setName(newVal) {
    const self = this;
    self._name = newVal;
  }

  /**
   * Add an initial charge command.
   *
   * @param {Command} newVal - Initial charge command to add.
   */
  addInitialCharge(newVal) {
    const self = this;
    self._initialCharges.push(newVal);
  }

  /**
   * Add a limit command.
   *
   * @param {LimitCommand} newVal - Limit command to add.
   */
  addLimit(newVal) {
    const self = this;
    self._limits.push(newVal);
  }

  /**
   * Add a change command.
   *
   * @param {Command} newVal - Change command to add.
   */
  addChange(newVal) {
    const self = this;
    self._changes.push(newVal);
  }

  /**
   * Set the equals command.
   *
   * @param {Command} newVal - Equals command to set.
   * @returns {Command|IncompatibleCommand} The command or incompatibility marker.
   */
  setEqualsGhg(newVal) {
    const self = this;
    self._equalsGhg = self._checkDuplicate(self._equalsGhg, newVal);
  }

  /**
   * Set the energy consumption equals command.
   *
   * @param {Command} newVal - Energy equals command to set.
   * @returns {Command|IncompatibleCommand} The command or incompatibility marker.
   */
  setEqualsKwh(newVal) {
    const self = this;
    self._equalsKwh = self._checkDuplicate(self._equalsKwh, newVal);
  }

  /**
   * Set the recharge command.
   *
   * @param {Command} newVal - Recharge command to set.
   * @returns {Command|IncompatibleCommand} The command or incompatibility marker. */
  setRecharge(newVal) {
    const self = this;
    self._recharge = self._checkDuplicate(self._recharge, newVal);
  }

  /**
   * Add a recycle command.
   *
   * @param {Command} newVal - Recycle command to add.
   */
  addRecycle(newVal) {
    const self = this;
    self._recycles.push(newVal);
  }

  /**
   * Add a replace command.
   *
   * @param {ReplaceCommand} newVal - Replace command to add.
   */
  addReplace(newVal) {
    const self = this;
    self._replaces.push(newVal);
  }

  /**
   * Set the retire command.
   *
   * @param {Command} newVal - Retire command to set.
   * @returns {Command|IncompatibleCommand} The command or incompatibility marker.
   */
  setRetire(newVal) {
    const self = this;
    self._retire = self._checkDuplicate(self._retire, newVal);
  }

  /**
   * Add a set value command.
   *
   * @param {Command} newVal - Set value command to add.
   */
  addSetVal(newVal) {
    const self = this;
    self._setVals.push(newVal);
  }

  /**
   * Check for duplicate single-value commands.
   *
   * @param {Command|null} originalVal - Existing command if any.
   * @param {Command} newVal - New command to check.
   * @returns {Command|IncompatibleCommand} The command or incompatibility marker.
   * @private
   */
  _checkDuplicate(originalVal, newVal) {
    if (originalVal === null) {
      return newVal;
    } else {
      return new IncompatibleCommand("duplicate");
    }
  }

  /**
   * Create an incompatible command for invalid placement.
   *
   * @returns {IncompatibleCommand} An incompatibility marker.
   * @private
   */
  _makeInvalidPlacement() {
    const self = this;
    return new IncompatibleCommand("invalid placement");
  }
}

/**
 * A substance with various commands dictating its behavior.
 */
class Substance {
  /**
   * Create a new Substance.
   *
   * @param {string} name - Name of the substance.
   * @param {Command[]} charges - Initial charge commands.
   * @param {LimitCommand[]} limits - Limit commands.
   * @param {Command[]} changes - Change commands.
   * @param {Command} equals - Equals command.
   * @param {Command} recharge - Recharge command.
   * @param {Command[]} recycles - Recycle commands.
   * @param {ReplaceCommand[]} replaces - Replace commands.
   * @param {Command} retire - Retire command.
   * @param {Command[]} setVals - Set value commands.
   * @param {boolean} isMod - Whether this modifies existing substance.
   * @param {boolean} compat - Whether substance is UI-compatible.
   */
  constructor(
    name,
    charges,
    limits,
    changes,
    equalsGhg,
    equalsKwh,
    recharge,
    recycles,
    replaces,
    retire,
    setVals,
    isMod,
    compat,
  ) {
    const self = this;
    self._name = name;
    self._initialCharges = charges;
    self._limits = limits;
    self._changes = changes;
    self._equalsGhg = equalsGhg;
    self._equalsKwh = equalsKwh;
    self._recharge = recharge;
    self._recycles = recycles;
    self._replaces = replaces;
    self._retire = retire;
    self._setVals = setVals;
    self._isModification = isMod;
    self._isCompatible = compat;
  }

  /**
   * Get the name of this substance.
   *
   * @returns {string} The substance name like HFC-134a.
   */
  getName() {
    const self = this;
    return self._name;
  }

  /**
   * Get all initial charge commands for this substance.
   *
   * @returns {Command[]} Array of initial charge commands.
   */
  getInitialCharges() {
    const self = this;
    return self._initialCharges;
  }

  /**
   * Get the initial charge command for a specific stream.
   *
   * @param {string} stream - The stream to get initial charge for.
   * @returns {Command|null} The initial charge command or null if not found.
   */
  getInitialCharge(stream) {
    const self = this;
    const matching = self._initialCharges.filter((x) => x.getTarget() === stream);
    return matching.length == 0 ? null : matching[0];
  }

  /**
   * Get all limit commands for this substance.
   *
   * @returns {LimitCommand[]} Array of limit commands.
   */
  getLimits() {
    const self = this;
    return self._limits;
  }

  /**
   * Get all change commands for this substance.
   *
   * @returns {Command[]} Array of change commands.
   */
  getChanges() {
    const self = this;
    return self._changes;
  }

  /**
   * Get the GHG equals command for this substance.
   *
   * @returns {Command|null} The GHG equals command or null if not set.
   */
  getEqualsGhg() {
    const self = this;
    return self._equalsGhg;
  }

  /**
   * Get the energy consumption equals command for this substance.
   *
   * @returns {Command|null} The energy equals command or null if not set.
   */
  getEqualsKwh() {
    const self = this;
    return self._equalsKwh;
  }

  /**
   * Get the recharge command for this substance.
   *
   * @returns {Command|null} The recharge command or null if not set.
   */
  getRecharge() {
    const self = this;
    return self._recharge;
  }

  /**
   * Get all recycle commands for this substance.
   *
   * @returns {Command[]} Array of recycle commands.
   */
  getRecycles() {
    const self = this;
    return self._recycles;
  }

  /**
   * Get all replace commands for this substance.
   *
   * @returns {ReplaceCommand[]} Array of replace commands.
   */
  getReplaces() {
    const self = this;
    return self._replaces;
  }

  /**
   * Get the retire command for this substance.
   *
   * @returns {Command|null} The retire command or null if not set.
   */
  getRetire() {
    const self = this;
    return self._retire;
  }

  /**
   * Get all set value commands for this substance.
   *
   * @returns {Command[]} Array of set value commands.
   */
  getSetVals() {
    const self = this;
    return self._setVals;
  }

  /**
   * Check if this substance modifies an existing one.
   *
   * @returns {boolean} True if this modifies an existing substance.
   */
  getIsModification() {
    const self = this;
    return self._isModification;
  }

  /**
   * Check if this substance is compatible with UI editing.
   *
   * @returns {boolean} True if substance can be edited in UI.
   */
  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  /**
   * Generate the code representation of the substance.
   *
   * Translate the substance's properties and commands into their code
   * representation based on the number of spaces specified for the indentation.
   *
   * @param {number} spaces - Number of spaces to use for indentation.
   * @returns {string} The code representation of the substance.
   */
  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    const prefix = self.getIsModification() ? "modify" : "uses";
    addCode(prefix + ' substance "' + self.getName() + '"', spaces);

    const addIfGiven = (code) => {
      if (code === null) {
        return;
      }
      addCode(code, spaces + 2);
    };

    const addAllIfGiven = (codeLines) => {
      if (codeLines === null) {
        return;
      }
      codeLines.forEach(addIfGiven);
    };

    addAllIfGiven(self._getInitialChargesCode());
    addIfGiven(self._getEqualsCode(self._equalsGhg));
    addIfGiven(self._getEqualsCode(self._equalsKwh));
    addAllIfGiven(self._getSetValsCode());
    addAllIfGiven(self._getChangesCode());
    addIfGiven(self._getRetireCode());
    addAllIfGiven(self._getLimitCode());
    addIfGiven(self._getRechargeCode());
    addAllIfGiven(self._getRecycleCode());
    addAllIfGiven(self._getReplaceCode());

    addCode("end substance", spaces);
    return finalizeCodePieces(baselinePieces);
  }

  /**
   * Generate code for initial charge commands.
   *
   * @returns {string[]|null} Array of code strings or null if no charges.
   * @private
   */
  _getInitialChargesCode() {
    const self = this;
    if (self._initialCharges === null) {
      return null;
    }

    const buildInitialCharge = (initialCharge) => {
      const pieces = [
        "initial charge with",
        initialCharge.getValue().getValue(),
        initialCharge.getValue().getUnits(),
        "for",
        initialCharge.getTarget(),
      ];
      self._addDuration(pieces, initialCharge);
      return self._finalizeStatement(pieces);
    };

    return self._initialCharges.map(buildInitialCharge);
  }

  /**
   * Generate code for the equals command.
   *
   * @returns {string|null} Code string or null if no equals command.
   * @private
   */
  _getEqualsCode(equalsCommand) {
    const self = this;
    if (equalsCommand === null) {
      return null;
    }

    const pieces = [
      "equals",
      equalsCommand.getValue().getValue(),
      equalsCommand.getValue().getUnits(),
    ];
    self._addDuration(pieces, equalsCommand);

    return self._finalizeStatement(pieces);
  }

  /**
   * Generate code for set value commands.
   *
   * @returns {string[]|null} Array of code strings or null if no set values.
   * @private
   */
  _getSetValsCode() {
    const self = this;
    if (self._setVals.length == 0) {
      return null;
    }

    const buildSetVal = (setVal) => {
      const pieces = [
        "set",
        setVal.getTarget(),
        "to",
        setVal.getValue().getValue(),
        setVal.getValue().getUnits(),
      ];
      self._addDuration(pieces, setVal);
      return self._finalizeStatement(pieces);
    };

    return self._setVals.map(buildSetVal);
  }

  /**
   * Generate code for change commands.
   *
   * @returns {string[]|null} Array of code strings or null if no changes.
   * @private
   */
  _getChangesCode() {
    const self = this;
    if (self._change === null) {
      return null;
    }

    const buildChange = (change) => {
      const pieces = [
        "change",
        change.getTarget(),
        "by",
        change.getValue().getValue(),
        change.getValue().getUnits(),
      ];
      self._addDuration(pieces, change);
      return self._finalizeStatement(pieces);
    };

    return self._changes.map(buildChange);
  }

  /**
   * Generate code for the retire command.
   *
   * @returns {string|null} Code string or null if no retire command.
   * @private
   */
  _getRetireCode() {
    const self = this;
    if (self._retire === null) {
      return null;
    }

    const pieces = [
      "retire",
      self._retire.getValue().getValue(),
      self._retire.getValue().getUnits(),
    ];
    self._addDuration(pieces, self._retire);

    return self._finalizeStatement(pieces);
  }

  /**
   * Generate code for limit commands.
   *
   * @returns {string[]|null} Array of code strings or null if no limits.
   * @private
   */
  _getLimitCode() {
    const self = this;
    if (self._limits === null || self._limits.length == 0) {
      return null;
    }

    const buildLimit = (limit) => {
      const pieces = [
        limit.getTypeName(),
        limit.getTarget(),
        "to",
        limit.getValue().getValue(),
        limit.getValue().getUnits(),
      ];

      const displacing = limit.getDisplacing();
      if (displacing !== null && displacing !== undefined) {
        pieces.push("displacing");
        pieces.push('"' + displacing + '"');
      }

      self._addDuration(pieces, limit);
      return self._finalizeStatement(pieces);
    };

    return self._limits.map(buildLimit);
  }

  /**
   * Generate code for the recharge command.
   *
   * @returns {string|null} Code string or null if no recharge command.
   * @private
   */
  _getRechargeCode() {
    const self = this;
    if (self._recharge === null) {
      return null;
    }

    const pieces = [
      "recharge",
      self._recharge.getTarget().getValue(),
      self._recharge.getTarget().getUnits(),
      "with",
      self._recharge.getValue().getValue(),
      self._recharge.getValue().getUnits(),
    ];
    self._addDuration(pieces, self._recharge);

    return self._finalizeStatement(pieces);
  }

  /**
   * Generate code for recycle commands.
   *
   * @returns {string[]|null} Array of code strings or null if no recycles.
   * @private
   */
  _getRecycleCode() {
    const self = this;
    if (self._recycles === null) {
      return null;
    }

    const buildRecycle = (recycle) => {
      const pieces = [
        "recover",
        recycle.getTarget().getValue(),
        recycle.getTarget().getUnits(),
        "with",
        recycle.getValue().getValue(),
        recycle.getValue().getUnits(),
        "reuse",
      ];
      self._addDuration(pieces, recycle);

      return self._finalizeStatement(pieces);
    };

    return self._recycles.map(buildRecycle);
  }

  /**
   * Generate code for replace commands.
   *
   * @returns {string[]|null} Array of code strings or null if no replaces.
   * @private
   */
  _getReplaceCode() {
    const self = this;
    if (self._replaces === null) {
      return null;
    }

    const buildReplace = (replace) => {
      const pieces = [
        "replace",
        replace.getVolume().getValue(),
        replace.getVolume().getUnits(),
        "of",
        replace.getSource(),
        "with",
        '"' + replace.getDestination() + '"',
      ];
      self._addDuration(pieces, replace);

      return self._finalizeStatement(pieces);
    };

    return self._replaces.map(buildReplace);
  }

  /**
   * Adds duration information to code pieces array.
   *
   * @param {string[]} pieces - Array of code pieces to append to.
   * @param {Command} command - Command containing duration info.
   * @private
   */
  _addDuration(pieces, command) {
    const self = this;

    const duration = command.getDuration();
    if (duration === null) {
      return;
    }

    const startYear = duration.getStart();
    const endYear = duration.getEnd();
    if (startYear === null && endYear === null) {
      return;
    }

    if (startYear == endYear) {
      pieces.push("during year " + startYear);
      return;
    }

    const processUnbounded = () => {
      const noStart = startYear === null;
      const startYearRealized = noStart ? "beginning" : startYear;

      const noEnd = endYear === null;
      const endYearRealized = noEnd ? "onwards" : endYear;

      pieces.push("during years " + startYearRealized + " to " + endYearRealized);
    };

    const processBounded = () => {
      const startYearRearrange = Math.min(startYear, endYear);
      const endYearRearrange = Math.max(startYear, endYear);
      pieces.push("during years " + startYearRearrange + " to " + endYearRearrange);
    };

    if (startYear === null || endYear === null) {
      processUnbounded();
    } else {
      processBounded();
    }
  }

  /**
   * Join code pieces into a single statement.
   *
   * @param {string[]} pieces - Array of code pieces to join.
   * @returns {string} The combined code statement.
   * @private
   */
  _finalizeStatement(pieces) {
    const self = this;
    return pieces.map((x) => x + "").join(" ");
  }
}

/**
 * Command with type, target, value and duration.
 *
 * Command such as a set command with a specified type, target, value and
 * duration.
 */
class Command {
  /**
   * Create a new Command.
   *
   * @param {string} typeName - Type of the command.
   * @param {string} target - Target of the command.
   * @param {EngineNumber} value - Value for the command.
   * @param {YearMatcher} duration - Duration for the command.
   */
  constructor(typeName, target, value, duration) {
    const self = this;
    self._typeName = typeName;
    self._target = target;
    self._value = value;
    self._duration = duration;
  }

  /**
   * Get the type name of this command.
   *
   * @returns {string} The command type name (e.g. "change", "retire", "setVal", etc).
   */
  getTypeName() {
    const self = this;
    return self._typeName;
  }

  /**
   * Get the target of this command.
   *
   * @returns {string} The target name (e.g. "manufacture", "import", etc).
   */
  getTarget() {
    const self = this;
    return self._target;
  }

  /**
   * Get the value associated with this command.
   *
   * @returns {EngineNumber} The command's value with units.
   */
  getValue() {
    const self = this;
    return self._value;
  }

  /**
   * Get the duration for which this command applies.
   *
   * @returns {YearMatcher} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Check if this command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as basic commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Limit command with displacement capability.
 */
class LimitCommand {
  /**
   * Create a new LimitCommand.
   *
   * @param {string} typeName - Type of limit (cap/floor).
   * @param {string} target - Target of the limit.
   * @param {EngineNumber} value - Limit value.
   * @param {YearMatcher} duration - Duration of limit.
   * @param {string} displacing - Substance being displaced.
   */
  constructor(typeName, target, value, duration, displacing) {
    const self = this;
    self._typeName = typeName;
    self._target = target;
    self._value = value;
    self._duration = duration;
    self._displacing = displacing;
  }

  /**
   * Get the type name of this limit command.
   *
   * @returns {string} The command type ("cap" or "floor").
   */
  getTypeName() {
    const self = this;
    return self._typeName;
  }

  /**
   * Get the target of this limit command.
   *
   * @returns {string} The target name (e.g. "manufacture", "import", etc).
   */
  getTarget() {
    const self = this;
    return self._target;
  }

  /**
   * Get the value associated with this limit.
   *
   * @returns {EngineNumber} The limit value with units.
   */
  getValue() {
    const self = this;
    return self._value;
  }

  /**
   * Get the duration for which this limit applies.
   *
   * @returns {YearMatcher} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Get the substance being displaced by this limit.
   *
   * @returns {string|null} Name of substance being displaced, or null if none.
   */
  getDisplacing() {
    const self = this;
    return self._displacing;
  }

  /**
   * Check if this limit command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as limit commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Represent a command to replace one substance with another.
 */
class ReplaceCommand {
  /**
   * Create a new ReplaceCommand.
   *
   * @param {EngineNumber} volume - Volume to replace.
   * @param {string} source - Source substance.
   * @param {string} destination - Destination substance.
   * @param {YearMatcher} duration - Duration of replacement.
   */
  constructor(volume, source, destination, duration) {
    const self = this;
    self._volume = volume;
    self._source = source;
    self._destination = destination;
    self._duration = duration;
  }

  /**
   * Get the type name of this replace command.
   *
   * @returns {string} Always returns "replace".
   */
  getTypeName() {
    const self = this;
    return "replace";
  }

  /**
   * Get the volume to be replaced.
   *
   * @returns {EngineNumber} The volume with units.
   */
  getVolume() {
    const self = this;
    return self._volume;
  }

  /**
   * Get the source substance to replace from.
   *
   * @returns {string} Name of source substance.
   */
  getSource() {
    const self = this;
    return self._source;
  }

  /**
   * Get the destination substance to replace with.
   *
   * @returns {string} Name of destination substance.
   */
  getDestination() {
    const self = this;
    return self._destination;
  }

  /**
   * Get the duration for which this replacement applies.
   *
   * @returns {YearMatcher} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Check if this replace command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as replace commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Command that is not compatible with the UI editor.
 */
class IncompatibleCommand {
  /**
   * Create a new IncompatibleCommand.
   *
   * @param {string} typeName - Type of incompatible command.
   */
  constructor(typeName) {
    const self = this;
    self._typeName = typeName;
  }

  /**
   * Get the type name of this incompatible command.
   *
   * @returns {string} The type name of the command.
   */
  getTypeName() {
    const self = this;
    return self._typeName;
  }

  /**
   * Check compatibility with UI.
   *
   * @returns {boolean} Always returns false as it is incompatible.
   */
  getIsCompatible() {
    const self = this;
    return false;
  }
}

/**
 * Visitor compiling a QubecTalk program to JS objects describing the analysis.
 *
 * Visitor which attempts to compile a QubecTalk program to JS objects
 * describing the anlaysis or indication that the anlaysis cannot use the
 * simplified JS object format.
 */
class TranslatorVisitor extends toolkit.QubecTalkVisitor {
  /**
   * Visit a number node and converts it to a numeric value.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {number} The parsed number value, accounting for sign.
   */
  visitNumber(ctx) {
    const self = this;

    const raw = ctx.getText();
    const signMultiplier = raw.includes("-") ? -1 : 1;
    const bodyRawText = ctx.getChild(ctx.getChildCount() - 1).getText();
    const bodyParsed = signMultiplier * parseFloat(bodyRawText);

    return bodyParsed;
  }

  /**
   * Visit a string node and removes surrounding quotes.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The string value without quotes.
   */
  visitString(ctx) {
    const self = this;
    return self._getStringWithoutQuotes(ctx.getText());
  }

  /**
   * Visit a unit or ratio node and formats it appropriately.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The formatted unit or ratio string.
   */
  visitUnitOrRatio(ctx) {
    const self = this;
    if (ctx.getChildCount() == 1) {
      return ctx.getChild(0).getText();
    } else {
      const numerator = ctx.getChild(0).getText();
      const denominator = ctx.getChild(2).getText();

      if (denominator.startsWith("year")) {
        return numerator + " each " + denominator;
      } else {
        return numerator + " / " + denominator;
      }
    }
  }

  /**
   * Visit a unit value node and creates an EngineNumber.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {EngineNumber} The value with its associated units.
   */
  visitUnitValue(ctx) {
    const self = this;

    const unitString = ctx.getChild(1).accept(self);
    const expressionContent = ctx.getChild(0).accept(self);

    return new EngineNumber(expressionContent, unitString);
  }

  /**
   * Visit a simple expression node and processes its single child.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {*} The result of visiting the child expression.
   */
  visitSimpleExpression(ctx) {
    const self = this;
    return ctx.getChild(0).accept(self);
  }

  /**
   * Visit a condition expression node and format it.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The formatted condition expression.
   */
  visitConditionExpression(ctx) {
    const self = this;

    const posExpression = ctx.pos.accept(self);
    const opFunc = ctx.op.text;
    const negExpression = ctx.neg.accept(self);

    return posExpression + " " + opFunc + " " + negExpression;
  }

  /**
   * Visit a conditional expression node and format it.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The formatted conditional expression.
   */
  visitConditionalExpression(ctx) {
    const self = this;

    const condition = ctx.cond.accept(self);
    const positive = ctx.pos.accept(self);
    const negative = ctx.neg.accept(self);

    return positive + " if " + condition + " else " + negative + " endif";
  }

  /**
   * Build an arithmetic expression.
   *
   * @param {Object} ctx - The parse tree node context.
   * @param {string} op - The operator to use.
   * @returns {string} The formatted arithmetic expression.
   */
  buildAirthmeticExpression(ctx, op) {
    const self = this;

    const priorExpression = ctx.getChild(0).accept(self);
    const afterExpression = ctx.getChild(2).accept(self);

    return priorExpression + " " + op + " " + afterExpression;
  }

  /**
   * Visit an addition expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The formatted addition expression.
   */
  visitAdditionExpression(ctx) {
    const self = this;
    return self.buildAirthmeticExpression(ctx, ctx.op.text);
  }

  /**
   * Visit a multiplication expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The formatted multiplication expression.
   */
  visitMultiplyExpression(ctx) {
    const self = this;
    return self.buildAirthmeticExpression(ctx, ctx.op.text);
  }

  /**
   * Visit a power expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The formatted power expression.
   */
  visitPowExpression(ctx) {
    const self = this;
    return self.buildAirthmeticExpression(ctx, "^");
  }

  /**
   * Visit a stream access expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The stream access text.
   */
  visitGetStream(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit an indirect stream access expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The indirect stream access text.
   */
  visitGetStreamIndirect(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a stream conversion expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The stream conversion text.
   */
  visitGetStreamConversion(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a substance/application units stream access node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The stream access text.
   */
  visitGetStreamIndirectSubstanceAppUnits(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a minimum limit expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The minimum limit expression text.
   */
  visitLimitMinExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a maximum limit expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The maximum limit expression text.
   */
  visitLimitMaxExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a bounded limit expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The bounded limit expression text.
   */
  visitLimitBoundExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a parenthesized expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The parenthesized expression text.
   */
  visitParenExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a normal distribution expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The normal distribution expression text.
   */
  visitDrawNormalExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a uniform distribution expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The uniform distribution expression text.
   */
  visitDrawUniformExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a simple identifier node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The identifier text.
   */
  visitSimpleIdentifier(ctx) {
    const self = this;
    const identifier = ctx.getChild(0).getText();
    return identifier;
  }

  /**
   * Build a YearMatcher for a duration.
   *
   * @param {number|null} minYear - Start year or null for unbounded
   * @param {number|null} maxYear - End year or null for unbounded
   * @returns {YearMatcher} The year matcher object
   */
  buildDuring(minYear, maxYear) {
    const self = this;
    return new YearMatcher(minYear, maxYear);
  }

  /**
   * Visit a single year duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {YearMatcher} Year matcher for single year.
   */
  visitDuringSingleYear(ctx) {
    const self = this;
    const year = ctx.target.accept(self);
    return self.buildDuring(year, year);
  }

  /**
   * Visit a start year duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {YearMatcher} Year matcher starting from engine start.
   */
  visitDuringStart(ctx) {
    const self = this;
    const startYear = engine.getStartYear();
    return self.buildDuring(startYear, startYear);
  }

  /**
   * Visit a year range duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {YearMatcher} Year matcher for range.
   */
  visitDuringRange(ctx) {
    const self = this;
    const lower = ctx.lower.accept(self);
    const upper = ctx.upper.accept(self);
    return self.buildDuring(lower, upper);
  }

  /**
   * Visit a minimum year duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {YearMatcher} Year matcher with min bound only.
   */
  visitDuringWithMin(ctx) {
    const self = this;
    const lower = ctx.lower.accept(self);
    const upper = null;
    return self.buildDuring(lower, upper);
  }

  /**
   * Visit a maximum year duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {YearMatcher} Year matcher with max bound only.
   */
  visitDuringWithMax(ctx) {
    const self = this;
    const lower = null;
    const upper = ctx.upper.accept(self);
    return self.buildDuring(lower, upper);
  }

  /**
   * Visit an "all years" duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Function} Function that returns null for unbounded.
   */
  visitDuringAll(ctx) {
    const self = this;
    return (engine) => null;
  }

  /**
   * Visit an about stanza node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {AboutStanza} New about stanza instance.
   */
  visitAboutStanza(ctx) {
    const self = this;
    return new AboutStanza();
  }

  /**
   * Visit a default stanza node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {DefinitionalStanza} New default stanza instance.
   */
  visitDefaultStanza(ctx) {
    const self = this;
    const numApplications = ctx.getChildCount() - 4;

    const appChildren = [];
    for (let i = 0; i < numApplications; i++) {
      appChildren.push(ctx.getChild(i + 2));
    }

    const applications = appChildren.map((x) => x.accept(self));
    const isCompatible = self._getChildrenCompatible(applications);
    return new DefinitionalStanza("default", applications, isCompatible);
  }

  /**
   * Visit a policy stanza node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {DefinitionalStanza} New policy stanza instance.
   */
  visitPolicyStanza(ctx) {
    const self = this;
    const policyName = self._getStringWithoutQuotes(ctx.name.getText());
    const numApplications = ctx.getChildCount() - 5;

    const appChildren = [];
    for (let i = 0; i < numApplications; i++) {
      appChildren.push(ctx.getChild(i + 3));
    }

    const applications = appChildren.map((x) => x.accept(self));
    const isCompatible = self._getChildrenCompatible(applications);
    return new DefinitionalStanza(policyName, applications, isCompatible);
  }

  /**
   * Visit a simulations stanza node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {SimulationStanza} New simulations stanza instance.
   */
  visitSimulationsStanza(ctx) {
    const self = this;
    const numApplications = ctx.getChildCount() - 4;

    const children = [];
    for (let i = 0; i < numApplications; i++) {
      children.push(ctx.getChild(i + 2));
    }

    const scenarios = children.map((x) => x.accept(self));
    const isCompatible = self._getChildrenCompatible(scenarios);
    return new SimulationStanza(scenarios, isCompatible);
  }

  /**
   * Visit an application definition node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @param {boolean} isModification - Whether this is a modification.
   * @returns {Application} New application instance.
   */
  visitApplicationDef(ctx) {
    const self = this;
    return self._parseApplication(ctx, false);
  }

  /**
   * Visit a substance definition node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @param {boolean} isModification - Whether this is a modification.
   * @returns {Substance} New substance instance.
   */
  visitSubstanceDef(ctx) {
    const self = this;
    return self._parseSubstance(ctx, false);
  }

  /**
   * Visit an application modification node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @param {boolean} isModification - Whether this is a modification.
   * @returns {Application} New application instance.
   */
  visitApplicationMod(ctx) {
    const self = this;
    return self._parseApplication(ctx, true);
  }

  /**
   * Visit a substance modification node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @param {boolean} isModification - Whether this is a modification.
   * @returns {Substance} New substance instance.
   */
  visitSubstanceMod(ctx) {
    const self = this;
    return self._parseSubstance(ctx, true);
  }

  /**
   * Visit a limit command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {LimitCommand} New limit command instance.
   */
  visitLimitCommandAllYears(ctx) {
    const self = this;
    return self._buildLimit(ctx, null, null);
  }

  /**
   * Visit a limit command with displacement and all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {LimitCommand} New limit command instance.
   */
  visitLimitCommandDisplacingAllYears(ctx) {
    const self = this;
    const displaceTarget = self._getStringWithoutQuotes(ctx.getChild(5).getText());
    return self._buildLimit(ctx, null, displaceTarget);
  }

  /**
   * Visit a limit command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {LimitCommand} New limit command instance.
   */
  visitLimitCommandDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildLimit(ctx, duration, null);
  }

  /**
   * Visit a limit command with displacement and duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {LimitCommand} New limit command instance.
   */
  visitLimitCommandDisplacingDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    const displaceTarget = self._getStringWithoutQuotes(ctx.getChild(5).getText());
    return self._buildLimit(ctx, duration, displaceTarget);
  }

  /**
   * Visit a change command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New change command instance.
   */
  visitChangeAllYears(ctx) {
    const self = this;
    return self._buildOperation(ctx, "change", null);
  }

  /**
   * Visit a change command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New change command instance.
   */
  visitChangeDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "change", duration);
  }

  /**
   * Visit a define var statement (user-defined variable) node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {IncompatibleCommand} Incompatibility marker for define var.
   */
  visitDefineVarStatement(ctx) {
    const self = this;
    return new IncompatibleCommand("define var");
  }

  /**
   * Visit an initial charge command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New initial charge command instance.
   */
  visitInitialChargeAllYears(ctx) {
    const self = this;
    return self._buildOperation(ctx, "initial charge", null);
  }

  /**
   * Visit an initial charge command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New initial charge command instance.
   */
  visitInitialChargeDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "initial charge", duration);
  }

  /**
   * Visit a recharge command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New recharge command instance.
   */
  visitRechargeAllYears(ctx) {
    const self = this;
    const populationFuture = (ctx) => ctx.population.accept(self);
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    return self._buildOperation(ctx, "recharge", null, populationFuture, volumeFuture);
  }

  /**
   * Visit a recharge command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New recharge command instance.
   */
  visitRechargeDuration(ctx) {
    const self = this;
    const populationFuture = (ctx) => ctx.population.accept(self);
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "recharge", duration, populationFuture, volumeFuture);
  }

  /**
   * Visit a recover command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New recover command instance.
   */
  visitRecoverAllYears(ctx) {
    const self = this;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const yieldFuture = (ctx) => ctx.yieldVal.accept(self);
    return self._buildOperation(ctx, "recycle", null, volumeFuture, yieldFuture);
  }

  /**
   * Visit a recover command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New recover command instance.
   */
  visitRecoverDuration(ctx) {
    const self = this;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const yieldFuture = (ctx) => ctx.yieldVal.accept(self);
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "recycle", duration, volumeFuture, yieldFuture);
  }

  /**
   * Visit a recover command with displacement and all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {IncompatibleCommand} Incompatibility marker for recover with displace.
   */
  visitRecoverDisplacementAllYears(ctx) {
    const self = this;
    return new IncompatibleCommand("recover with displace");
  }

  /**
   * Visit a recover command with displacement and duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {IncompatibleCommand} Incompatibility marker for recover with displace.
   */
  visitRecoverDisplacementDuration(ctx) {
    const self = this;
    return new IncompatibleCommand("recover with displace");
  }

  /**
   * Visit a replace command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {ReplaceCommand} New replace command instance.
   */
  visitReplaceAllYears(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const source = ctx.target.getText();
    const destination = self._getStringWithoutQuotes(ctx.destination.getText());
    return new ReplaceCommand(volume, source, destination, null);
  }

  /**
   * Visit a replace command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {ReplaceCommand} New replace command instance.
   */
  visitReplaceDuration(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const duration = ctx.duration.accept(self);
    const source = ctx.target.getText();
    const destination = self._getStringWithoutQuotes(ctx.destination.getText());
    return new ReplaceCommand(volume, source, destination, duration);
  }

  /**
   * Visit a retire command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New retire command instance.
   */
  visitRetireAllYears(ctx) {
    const self = this;
    const targetFuture = (ctx) => null;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    return self._buildOperation(ctx, "retire", null, targetFuture, volumeFuture);
  }

  /**
   * Visit a retire command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New retire command instance.
   */
  visitRetireDuration(ctx) {
    const self = this;
    const targetFuture = (ctx) => null;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "retire", duration, targetFuture, volumeFuture);
  }

  /**
   * Visit a set command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New set command instance.
   */
  visitSetAllYears(ctx) {
    const self = this;
    return self._buildOperation(ctx, "setVal", null);
  }

  /**
   * Visit a set command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New set command instance.
   */
  visitSetDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "setVal", duration);
  }

  /**
   * Visit an equals command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New equals command instance.
   */
  visitEqualsAllYears(ctx) {
    const self = this;
    const targetFuture = (ctx) => null;
    return self._buildOperation(ctx, "equals", null, targetFuture);
  }

  /**
   * Visit an equals command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New equals command instance.
   */
  visitEqualsDuration(ctx) {
    const self = this;
    const targetFuture = (ctx) => null;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "equals", duration, targetFuture);
  }

  /**
   * Visit a base simulation node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {SimulationScenario} New simulation scenario instance.
   */
  visitBaseSimulation(ctx) {
    const self = this;
    const name = self._getStringWithoutQuotes(ctx.name.getText());
    const yearStart = ctx.start.getText();
    const yearEnd = ctx.end.getText();
    return new SimulationScenario(name, [], yearStart, yearEnd, true);
  }

  /**
   * Visit a policy simulation node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {SimulationScenario} New simulation scenario instance.
   */
  visitPolicySim(ctx) {
    const self = this;
    const name = self._getStringWithoutQuotes(ctx.name.getText());
    const numPolicies = Math.ceil((ctx.getChildCount() - 8) / 2);
    const yearStart = ctx.start.getText();
    const yearEnd = ctx.end.getText();

    const policies = [];
    for (let i = 0; i < numPolicies; i++) {
      const rawName = ctx.getChild(i * 2 + 3).getText();
      const nameNoQuotes = self._getStringWithoutQuotes(rawName);
      policies.push(nameNoQuotes);
    }

    return new SimulationScenario(name, policies, yearStart, yearEnd, true);
  }

  /**
   * Visit a base simulation with trials node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {IncompatibleCommand} Incompatibility marker for simulate with trials.
   */
  visitBaseSimulationTrials(ctx) {
    const self = this;
    return new IncompatibleCommand("simulate with trials");
  }

  /**
   * Visit a policy simulation with trials node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {IncompatibleCommand} Incompatibility marker for simulate with trials.
   */
  visitPolicySimTrials(ctx) {
    const self = this;
    return new IncompatibleCommand("simulate with trials");
  }

  /**
   * Visit a program node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Program} New program instance.
   */
  visitProgram(ctx) {
    const self = this;

    const stanzasByName = new Map();
    const numStanzas = ctx.getChildCount();

    for (let i = 0; i < numStanzas; i++) {
      const newStanza = ctx.getChild(i).accept(self);
      if (newStanza !== undefined) {
        stanzasByName.set(newStanza.getName(), newStanza);
      }
    }

    if (!stanzasByName.has("default")) {
      return new Program([], [], [], true);
    }

    const applications = stanzasByName.get("default").getApplications();

    const allStanzaNames = Array.of(...stanzasByName.keys());
    const policies = allStanzaNames
      .filter((x) => x !== "default")
      .filter((x) => x !== "about")
      .filter((x) => x !== "simulations")
      .map((x) => stanzasByName.get(x));

    if (!stanzasByName.has("simulations")) {
      return new Program(applications, policies, [], true);
    }

    const scenarios = stanzasByName.get("simulations").getScenarios();

    const stanzas = Array.of(...stanzasByName.values());

    const isCompatible = self._getChildrenCompatible(stanzas);

    return new Program(applications, policies, scenarios, isCompatible);
  }

  /**
   * Visit a global statement node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {*} The result of visiting the child node.
   */
  visitGlobalStatement(ctx) {
    const self = this;
    return ctx.getChild(0).accept(self);
  }

  /**
   * Visit a substance statement node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {*} The result of visiting the child node.
   */
  visitSubstanceStatement(ctx) {
    const self = this;
    return ctx.getChild(0).accept(self);
  }

  /**
   * Extract string value from a quoted string node, removing quotes.
   *
   * @param {string} target - The quoted string.
   * @returns {string} The string without quotes.
   * @private
   */
  _getStringWithoutQuotes(target) {
    const self = this;
    return target.substring(1, target.length - 1);
  }

  /**
   * Check compatibility of children nodes.
   *
   * @param {Array} children - Array of nodes to check.
   * @returns {boolean} True if all children are compatible, false otherwise.
   * @private
   */
  _getChildrenCompatible(children) {
    const self = this;
    return children.map((x) => x.getIsCompatible()).reduce((a, b) => a && b, true);
  }

  /**
   * Parse an application node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @param {boolean} isModification - Whether this is a modification.
   * @returns {Application} New application instance.
   * @private
   */
  _parseApplication(ctx, isModification) {
    const self = this;
    const name = self._getStringWithoutQuotes(ctx.name.getText());
    const numApplications = ctx.getChildCount() - 5;

    const children = [];
    for (let i = 0; i < numApplications; i++) {
      children.push(ctx.getChild(i + 3));
    }

    const childrenParsed = children.map((x) => x.accept(self));
    const isCompatible = self._getChildrenCompatible(childrenParsed);

    return new Application(name, childrenParsed, isModification, isCompatible);
  }

  /**
   * Parse a substance node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @param {boolean} isModification - Whether this is a modification.
   * @returns {Substance} New substance instance.
   * @private
   */
  _parseSubstance(ctx, isModification) {
    const self = this;
    const name = self._getStringWithoutQuotes(ctx.name.getText());
    const numChildren = ctx.getChildCount() - 5;

    const children = [];
    for (let i = 0; i < numChildren; i++) {
      children.push(ctx.getChild(i + 3));
    }

    const commands = children.map((x) => {
      return x.accept(self);
    });

    const builder = new SubstanceBuilder(name, isModification);

    commands.forEach((x) => {
      builder.addCommand(x);
    });

    const isCompatibleRaw = commands.map((x) => x.getIsCompatible()).reduce((a, b) => a && b, true);

    return builder.build(isCompatibleRaw);
  }

  /**
   * Build an operation command.
   *
   * @param {Object} ctx - The parse tree node context.
   * @param {string} typeName - Type name of the command.
   * @param {YearMatcher} duration - Duration of the command.
   * @param {Function} targetGetter - Function to get the target.
   * @param {Function} valueGetter - Function to get the value.
   * @returns {Command} New command instance.
   * @private
   */
  _buildOperation(ctx, typeName, duration, targetGetter, valueGetter) {
    const self = this;
    if (targetGetter === undefined || targetGetter === null) {
      targetGetter = (ctx) => ctx.target.getText();
    }
    const target = targetGetter(ctx);

    if (valueGetter === undefined || valueGetter === null) {
      valueGetter = (ctx) => ctx.value.accept(self);
    }
    const value = valueGetter(ctx);

    return new Command(typeName, target, value, duration);
  }

  /**
   * Build a limit command.
   *
   * @param {Object} ctx - The parse tree node context.
   * @param {YearMatcher} duration - Duration of the command.
   * @param {string} displaceTarget - Displacing target.
   * @param {Function} targetGetter - Function to get the target.
   * @param {Function} valueGetter - Function to get the value.
   * @returns {LimitCommand} New limit command instance.
   * @private
   */
  _buildLimit(ctx, duration, displaceTarget, targetGetter, valueGetter) {
    const self = this;
    const capType = ctx.getChild(0).getText();

    if (targetGetter === undefined || targetGetter === null) {
      targetGetter = (ctx) => ctx.target.getText();
    }
    const target = targetGetter(ctx);

    if (valueGetter === undefined || valueGetter === null) {
      valueGetter = (ctx) => ctx.value.accept(self);
    }
    const value = valueGetter(ctx);

    return new LimitCommand(capType, target, value, duration, displaceTarget);
  }
}

/**
 * Result of translating from QubecTalk script to UI editor objects.
 */
class TranslationResult {
  /**
   * Create a new record of a translation attempt.
   *
   * @param program The translated program as a lambda if successful or null if
   *     unsuccessful.
   * @param errors Any errors encountered or empty list if no errors.
   */
  constructor(program, errors) {
    const self = this;
    self._program = program;
    self._errors = errors;
  }

  /**
   * Get the program as an object.
   *
   * @returns The compiled program as an object or null if translation failed.
   */
  getProgram() {
    const self = this;
    return self._program;
  }

  /**
   * Get errors encountered in compiling the QubecTalk script.
   *
   * @returns Errors or empty list if no errors.
   */
  getErrors() {
    const self = this;
    return self._errors;
  }
}

/**
 * Compiler that translates QubecTalk code into object representation.
 *
 * Facade which parses QubecTalk scripts and converts them into objects which
 * represent the program structure for UI editor-compatiable objects. Detects
 * and reports syntax errors.
 */
class UiTranslatorCompiler {
  /**
   * Compiles QubecTalk code into an object representation.
   *
   * Parses the input code using ANTLR and translates it into objects
   * representing the program structure. Reports any syntax errors encountered.
   *
   * @param {string} input - The QubecTalk code to compile.
   * @returns {TranslationResult} Result containing either the compiled program
   *     object or any encountered errors.
   */
  compile(input) {
    const self = this;

    if (input.replaceAll("\n", "").replaceAll(" ", "") === "") {
      return new TranslationResult(null, []);
    }

    const errors = [];

    const chars = new toolkit.antlr4.InputStream(input);
    const lexer = new toolkit.QubecTalkLexer(chars);
    lexer.removeErrorListeners();
    lexer.addErrorListener({
      syntaxError: (recognizer, offendingSymbol, line, column, msg, err) => {
        const result = `(line ${line}, col ${column}): ${msg}`;
        errors.push(result);
      },
    });

    const tokens = new toolkit.antlr4.CommonTokenStream(lexer);
    const parser = new toolkit.QubecTalkParser(tokens);

    // TODO: Leftover from base.
    parser.buildParsePlastics = true;
    parser.removeErrorListeners();
    parser.addErrorListener({
      syntaxError: (recognizer, offendingSymbol, line, column, msg, err) => {
        const result = `(line ${line}, col ${column}): ${msg}`;
        errors.push(result);
      },
    });

    const programUncompiled = parser.program();

    if (errors.length > 0) {
      return new TranslationResult(null, errors);
    }

    const program = programUncompiled.accept(new TranslatorVisitor());
    if (errors.length > 0) {
      return new TranslationResult(null, errors);
    }

    return new TranslationResult(program, errors);
  }
}

export {
  AboutStanza,
  Application,
  Command,
  DefinitionalStanza,
  LimitCommand,
  Program,
  ReplaceCommand,
  SimulationScenario,
  SimulationStanza,
  Substance,
  SubstanceBuilder,
  UiTranslatorCompiler,
  buildAddCode,
  finalizeCodePieces,
  indent,
};
