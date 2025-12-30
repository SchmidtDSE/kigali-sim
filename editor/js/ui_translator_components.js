/**
 * Component classes for UI translation.
 *
 * Contains the main data model classes for representing programs, applications,
 * scenarios, substances, and commands in the QubecTalk UI editor.
 *
 * @license BSD, see LICENSE.md
 */

import {EngineNumber} from "engine_number";
import {YearMatcher, ParsedYear} from "duration";
import {parseUnitValue} from "meta_serialization";
import {
  formatEngineNumber,
  indentSingle,
  indent,
  buildAddCode,
  finalizeCodePieces,
} from "ui_translator_util";
import {COMMAND_COMPATIBILITIES, SUPPORTED_EQUALS_UNITS} from "ui_editor_const";
import {
  AssumeCommand,
  Command,
  IncompatibleCommand,
  LimitCommand,
  RechargeCommand,
  RecycleCommand,
  ReplaceCommand,
  RetireCommand,
} from "ui_translator_commands";

const toolkit = QubecTalk.getToolkit();

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
    return self.getApplications()
      .map((x) => x.getSubstances())
      .flat();
  }

  /**
   * Insert or updates a substance in an application.
   *
   * @param {string} priorApplication - Name of application to remove from or
   *     null if no prior.
   * @param {string} newApplication - Name of application to insert into.
   * @param {string} priorSubstanceName - Name of substance to replace. Pass
   *     null for new.
   * @param {Substance} substance - The substance to insert.
   */
  insertSubstance(priorApplication, newApplication, priorSubstanceName, substance) {
    const self = this;

    if (priorApplication !== null) {
      const priorAppObj = self.getApplication(priorApplication);
      priorAppObj.deleteSubstance(priorSubstanceName);
    }

    const newAppObj = self.getApplication(newApplication);
    newAppObj.insertSubstance(null, substance);
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

    // Update applications within policies
    self._policies.forEach((policy) => {
      const policyApplications = policy.getApplications().filter((x) => x.getName() === oldName);
      policyApplications.forEach((x) => x.rename(newName));
    });
  }

  /**
   * Rename a substance within a specific application.
   *
   * @param {string} applicationName - Name of the application containing the substance.
   * @param {string} oldSubstanceName - Current name of the substance.
   * @param {string} newSubstanceName - New name for the substance.
   */
  renameSubstanceInApplication(applicationName, oldSubstanceName, newSubstanceName) {
    const self = this;

    // Update substances in main applications
    const targetApplications = self._applications.filter(
      (x) => x.getName() === applicationName,
    );
    targetApplications.forEach((app) => {
      const substances = app.getSubstances().filter((x) => x.getName() === oldSubstanceName);
      substances.forEach((x) => x.rename(newSubstanceName));
    });

    // Update substances within policies for the same application
    self._policies.forEach((policy) => {
      const policyApplications = policy.getApplications().filter(
        (x) => x.getName() === applicationName,
      );
      policyApplications.forEach((app) => {
        const substances = app.getSubstances().filter((x) => x.getName() === oldSubstanceName);
        substances.forEach((x) => x.rename(newSubstanceName));
      });
    });
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
   * Get the names of all simulation scenarios.
   *
   * Convenience method to extract just the scenario names without
   * needing to access full SimulationScenario objects. Filters out
   * IncompatibleCommand objects (e.g., from "across X trials" syntax).
   *
   * @returns {string[]} Array of scenario names in order they appear.
   */
  getScenarioNames() {
    const self = this;
    return self._scenarios
      .filter((scenario) => scenario.getIsCompatible())
      .map((scenario) => scenario.getName());
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
      const applicationsCode = self.getApplications()
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
      const policiesCode = self.getPolicies()
        .map((x) => x.toCode(spaces))
        .join("\n\n\n\n");
      addCode(policiesCode, spaces);
      addCode("", spaces);
      addCode("", spaces);
    }

    if (self.getScenarios().length > 0) {
      addCode("start simulations", spaces);
      addCode("", spaces);
      const scenariosCode = self.getScenarios()
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

        const getEqualsProblematic = (equals) => {
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

        const ghgProblematic = getEqualsProblematic(substance.getEqualsGhg());
        const kwhProblematic = getEqualsProblematic(substance.getEqualsKwh());
        const equalsProblematic = ghgProblematic || kwhProblematic;

        return getInitialChargeProblematic() || equalsProblematic;
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
      const applicationsCode = self.getApplications()
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
      const substancesCode = self.getSubstances()
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
    self._recharges = [];
    self._recycles = [];
    self._replaces = [];
    self._retire = null;
    self._setVals = [];
    self._enables = [];
    self._assumeMode = null;
    self._isCompatibleOverride = null;
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
      self._enables,
      self._initialCharges,
      self._limits,
      self._recycles,
      self._replaces,
      [self._equalsGhg, self._equalsKwh, self._retire],
      self._recharges,
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

    const isCompatibleComputed =
      isCompatibleRaw && isCompatibleInterpreted && initialChargesNonOverlap;
    const isCompatible =
      self._isCompatibleOverride !== null ?
        self._isCompatibleOverride :
        isCompatibleComputed;

    return new Substance(
      self._name,
      self._initialCharges,
      self._limits,
      self._changes,
      self._equalsGhg,
      self._equalsKwh,
      self._recharges,
      self._recycles,
      self._replaces,
      self._retire,
      self._setVals,
      self._enables,
      self._isModification,
      isCompatible,
      self._assumeMode,
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
      "recharge": (x) => self.addRecharge(x),
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
      "enable": (x) => self.addEnable(x),
      "assume": (x) => self.setAssumeMode(x.getMode()),
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
   * Add a recharge command.
   *
   * @param {Command} newVal - Recharge command to add.
   */
  addRecharge(newVal) {
    const self = this;
    self._recharges.push(newVal);
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
   * Set the sales assumption mode for this substance.
   *
   * @param {string|null} mode - The assumption mode: "continued", "only recharge", "no", or null.
   */
  setAssumeMode(mode) {
    const self = this;
    self._assumeMode = mode;
  }

  /**
   * Override the compatibility flag for this substance.
   *
   * @param {boolean} isCompatible - Whether the substance is UI-compatible.
   */
  setIsCompatible(isCompatible) {
    const self = this;
    self._isCompatibleOverride = isCompatible;
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
   * Add an enable command to this substance.
   *
   * @param {Command} enable - The enable command to add.
   */
  addEnable(enable) {
    const self = this;
    self._enables.push(enable);
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
 * Substance configuration including consumption patterns and lifecycle.
 *
 * Represents a single substance (e.g., HFC-134a, R-410A) used within an application.
 * Contains GWP values, initial charges, consumption/sales streams (domestic, import, export),
 * retirement rates, recharge schedules, and sales assumptions for bank tracking.
 *
 * Sales assumptions control carryover behavior:
 * - "continued": Sales continue from previous year (default, no-op)
 * - "only recharge": New sales limited to servicing existing equipment
 * - "no": All sales reset to zero
 *
 * UI editor supports max 1 assume command for sales stream without duration.
 * Advanced editor supports multiple assumes, durations, and other streams (domestic, import, bank).
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
   * @param {Command[]} recharges - Recharge commands.
   * @param {Command[]} recycles - Recycle commands.
   * @param {ReplaceCommand[]} replaces - Replace commands.
   * @param {Command} retire - Retire command.
   * @param {Command[]} setVals - Set value commands.
   * @param {Command[]} enables - Enable commands.
   * @param {boolean} isMod - Whether this modifies existing substance.
   * @param {boolean} compat - Whether substance is UI-compatible.
   * @param {string|null} assumeMode - Sales assumption mode.
   */
  constructor(
    name,
    charges,
    limits,
    changes,
    equalsGhg,
    equalsKwh,
    recharges,
    recycles,
    replaces,
    retire,
    setVals,
    enables,
    isMod,
    compat,
    assumeMode,
  ) {
    const self = this;
    self._name = name;
    self._initialCharges = charges;
    self._limits = limits;
    self._changes = changes;
    self._equalsGhg = equalsGhg;
    self._equalsKwh = equalsKwh;
    self._recharges = recharges;
    self._recycles = recycles;
    self._replaces = replaces;
    self._retire = retire;
    self._setVals = setVals;
    self._enables = enables;
    self._isModification = isMod;
    self._isCompatible = compat;
    self._assumeMode = assumeMode;
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
   * Rename this substance.
   *
   * @param {string} newName - New name for the substance.
   */
  rename(newName) {
    const self = this;
    // Simply update the name directly for rename operation
    self._name = newName;
  }

  /**
   * Update all metadata for this substance using a SubstanceMetadata object.
   *
   * @param {SubstanceMetadata} newMetadata - New metadata to apply to this substance.
   * @param {string} applicationName - Name of the application this substance belongs to.
   */
  updateMetadata(newMetadata, applicationName) {
    const self = this;

    // Validate input
    if (!newMetadata || !(newMetadata instanceof SubstanceMetadata)) {
      throw new Error("newMetadata must be a SubstanceMetadata instance");
    }

    // Use imported parseUnitValue function from meta_serialization module

    // Update name
    const fullName = newMetadata.getName();
    self._name = fullName;

    // Update GHG equals command
    const ghgValue = parseUnitValue(newMetadata.getGhg(), true);
    if (ghgValue) {
      self._equalsGhg = new Command("equals", null, ghgValue, null);
    } else {
      self._equalsGhg = null;
    }

    // Update energy equals command
    const energyValue = parseUnitValue(newMetadata.getEnergy(), true);
    if (energyValue) {
      self._equalsKwh = new Command("equals", null, energyValue, null);
    } else {
      self._equalsKwh = null;
    }

    // Update enabled streams
    self._enables = [];
    if (newMetadata.getHasDomestic()) {
      self._enables.push(new Command("enable", "domestic", null, null));
    }
    if (newMetadata.getHasImport()) {
      self._enables.push(new Command("enable", "import", null, null));
    }
    if (newMetadata.getHasExport()) {
      self._enables.push(new Command("enable", "export", null, null));
    }

    // Update initial charges
    self._initialCharges = [];

    const domesticCharge = parseUnitValue(newMetadata.getInitialChargeDomestic(), true);
    if (domesticCharge) {
      const cmd = new Command("initial charge", "domestic", domesticCharge, null);
      self._initialCharges.push(cmd);
    }

    const importCharge = parseUnitValue(newMetadata.getInitialChargeImport(), true);
    if (importCharge) {
      const cmd = new Command("initial charge", "import", importCharge, null);
      self._initialCharges.push(cmd);
    }

    const exportCharge = parseUnitValue(newMetadata.getInitialChargeExport(), true);
    if (exportCharge) {
      const cmd = new Command("initial charge", "export", exportCharge, null);
      self._initialCharges.push(cmd);
    }

    // Update retirement command (Component 5: includes withReplacement)
    const retirementValue = parseUnitValue(newMetadata.getRetirement(), true);
    if (retirementValue) {
      // Parse withReplacement flag from metadata (stored as "true"/"false" string)
      const retirementWithReplacementStr = newMetadata.getRetirementWithReplacement();
      const withReplacement = retirementWithReplacementStr === "true";

      self._retire = new RetireCommand(retirementValue, null, withReplacement);
    } else {
      self._retire = null;
    }

    // Update assumeMode from metadata defaultSales (Component 2)
    // Note: metadata.getDefaultSales() returns normalized internal value
    const defaultSales = newMetadata.getDefaultSales();
    if (defaultSales && defaultSales.trim()) {
      const assumeMode = defaultSales.trim();
      // Set the assumeMode directly; empty string means use default (null)
      if (assumeMode === "continued" || assumeMode === "") {
        self._assumeMode = null; // null and "continued" are equivalent
      } else {
        self._assumeMode = assumeMode;
      }
    } else {
      // Empty or missing defaultSales means use default (null)
      self._assumeMode = null;
    }
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
   * Get all recharge commands for this substance.
   *
   * @returns {Command[]} Array of recharge commands.
   */
  getRecharges() {
    const self = this;
    return self._recharges;
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
   * Get all enable commands for this substance.
   *
   * @returns {Command[]} Array of enable commands.
   */
  getEnables() {
    const self = this;
    return self._enables;
  }

  /**
   * Get the sales assumption mode for this substance.
   *
   * @returns {string|null} The assumption mode: "continued", "only recharge",
   *   "no", or null for default.
   */
  getAssumeMode() {
    const self = this;
    return self._assumeMode;
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
   * Get metadata representation of this substance for CSV export/import.
   *
   * This method extracts metadata from the current substance structure and
   * creates a SubstanceMetadata instance. The application name is provided
   * as a parameter since it's not stored within the substance itself.
   *
   * @param {string} applicationName - Name of the application this substance belongs to.
   * @returns {SubstanceMetadata} Metadata representation of the substance.
   */
  getMeta(applicationName) {
    const self = this;

    // Assert that applicationName is provided and non-empty
    if (!applicationName || applicationName.trim() === "") {
      throw new Error("applicationName must be provided and non-empty");
    }

    // Extract substance name and equipment type
    const fullName = self._name;
    let substance = fullName;
    let equipment = "";

    // Check if name contains equipment type (format: "substance - equipment")
    const dashIndex = fullName.indexOf(" - ");
    if (dashIndex > 0) {
      substance = fullName.substring(0, dashIndex);
      equipment = fullName.substring(dashIndex + 3);
    }

    // Extract GHG value
    let ghg = "";
    if (self._equalsGhg) {
      const ghgValue = self._equalsGhg.getValue();
      ghg = formatEngineNumber(ghgValue);
    }

    // Extract energy value
    let energy = "";
    if (self._equalsKwh) {
      const energyValue = self._equalsKwh.getValue();
      energy = formatEngineNumber(energyValue);
    }

    // Extract enabled streams
    let hasDomestic = false;
    let hasImport = false;
    let hasExport = false;

    self._enables.forEach((enable) => {
      const target = enable.getTarget();
      if (target === "domestic") {
        hasDomestic = true;
      } else if (target === "import") {
        hasImport = true;
      } else if (target === "export") {
        hasExport = true;
      }
    });

    // Extract initial charges
    let initialChargeDomestic = "";
    let initialChargeImport = "";
    let initialChargeExport = "";

    self._initialCharges.forEach((charge) => {
      const target = charge.getTarget();
      const chargeValue = charge.getValue();
      const chargeString = formatEngineNumber(chargeValue);

      if (target === "domestic") {
        initialChargeDomestic = chargeString;
      } else if (target === "import") {
        initialChargeImport = chargeString;
      } else if (target === "export") {
        initialChargeExport = chargeString;
      }
    });

    // Extract retirement rate and withReplacement flag
    let retirement = "";
    let retirementWithReplacement = "";
    if (self._retire) {
      const retireValue = self._retire.getValue();
      retirement = formatEngineNumber(retireValue);
      // Export withReplacement as "true" or "false" string
      const withReplacement = self._retire.getWithReplacement();
      retirementWithReplacement = withReplacement ? "true" : "false";
    }

    // Map assumeMode to user-facing defaultSales value
    const assumeMode = self.getAssumeMode();
    let defaultSales = "";
    if (assumeMode === null || assumeMode === "continued") {
      defaultSales = "continue from prior year";
    } else if (assumeMode === "only recharge") {
      defaultSales = "only servicing";
    } else if (assumeMode === "no") {
      defaultSales = "none";
    } else {
      // Default to "continue from prior year" for any unexpected values
      defaultSales = "continue from prior year";
    }

    return new SubstanceMetadata(
      substance,
      equipment,
      applicationName,
      ghg,
      hasDomestic,
      hasImport,
      hasExport,
      energy,
      initialChargeDomestic,
      initialChargeImport,
      initialChargeExport,
      retirement,
      retirementWithReplacement,
      defaultSales,
    );
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

    addAllIfGiven(self._getEnablesCode());
    addIfGiven(self._getAssumeCode());
    addAllIfGiven(self._getInitialChargesCode());
    addIfGiven(self._getEqualsCode(self._equalsGhg));
    addIfGiven(self._getEqualsCode(self._equalsKwh));
    addAllIfGiven(self._getSetValsCode());
    addAllIfGiven(self._getChangesCode());
    addIfGiven(self._getRetireCode());
    addAllIfGiven(self._getLimitCode());
    addAllIfGiven(self._getRechargeCode());
    addAllIfGiven(self._getRecycleCode());
    addAllIfGiven(self._getReplaceCode());

    addCode("end substance", spaces);
    return finalizeCodePieces(baselinePieces);
  }

  /**
   * Generate code for enable commands.
   *
   * @returns {string[]|null} Array of code strings or null if no enables.
   * @private
   */
  _getEnablesCode() {
    const self = this;
    if (self._enables.length == 0) {
      return null;
    }

    const buildEnable = (enable) => {
      const pieces = [
        "enable",
        enable.getTarget(),
      ];
      self._addDuration(pieces, enable);
      return self._finalizeStatement(pieces);
    };

    return self._enables.map(buildEnable);
  }

  /**
   * Generate code for assume command.
   *
   * @returns {string|null} Code string or null if no assume or if assume is "continued" (no-op).
   * @private
   */
  _getAssumeCode() {
    const self = this;

    if (self._assumeMode === null || self._assumeMode === "continued") {
      return null;
    } else if (self._assumeMode === "no") {
      return "assume no sales";
    } else if (self._assumeMode === "only recharge") {
      return "assume only recharge sales";
    } else {
      throw new Error(
        "Invalid assume mode: " + self._assumeMode + ". " +
        "Expected null, 'continued', 'no', or 'only recharge'.",
      );
    }
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
      const engineNumber = initialCharge.getValue();
      const pieces = [
        "initial charge with",
        formatEngineNumber(engineNumber),
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

    const engineNumber = equalsCommand.getValue();
    const pieces = [
      "equals",
      formatEngineNumber(engineNumber),
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
      const engineNumber = setVal.getValue();
      const pieces = [
        "set",
        setVal.getTarget(),
        "to",
        formatEngineNumber(engineNumber),
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
      const engineNumber = change.getValue();
      const pieces = [
        "change",
        change.getTarget(),
        "by",
        formatEngineNumber(engineNumber),
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

    const engineNumber = self._retire.getValue();
    const pieces = [
      "retire",
      formatEngineNumber(engineNumber),
    ];

    // Add "with replacement" if the flag is set (must come before duration)
    if (self._retire.getWithReplacement()) {
      pieces.push("with replacement");
    }

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
      const engineNumber = limit.getValue();
      const pieces = [
        limit.getTypeName(),
        limit.getTarget(),
        "to",
        formatEngineNumber(engineNumber),
      ];

      const displacing = limit.getDisplacing();
      if (displacing !== null && displacing !== undefined) {
        const displacingType = limit.getDisplacingType();

        // Build the displacement clause with proper spacing
        if (displacingType === "by volume") {
          pieces.push("displacing by volume");
        } else if (displacingType === "by units") {
          pieces.push("displacing by units");
        } else {
          pieces.push("displacing");
        }

        pieces.push('"' + displacing + '"');
      }

      self._addDuration(pieces, limit);
      return self._finalizeStatement(pieces);
    };

    return self._limits.map(buildLimit);
  }

  /**
   * Generate code for recharge commands.
   *
   * @returns {string[]|null} Array of code strings or null if no recharge commands.
   * @private
   */
  _getRechargeCode() {
    const self = this;
    if (self._recharges.length == 0) {
      return null;
    }

    return self._recharges.map((recharge) => {
      if (recharge.buildCommand) {
        // New RechargeCommand objects with buildCommand method
        return self._finalizeStatement([recharge.buildCommand()]);
      } else {
        // Legacy Command objects
        const targetEngineNumber = recharge.getTarget();
        const valueEngineNumber = recharge.getValue();
        const pieces = [
          "recharge",
          formatEngineNumber(targetEngineNumber),
          "with",
          formatEngineNumber(valueEngineNumber),
        ];
        self._addDuration(pieces, recharge);
        return self._finalizeStatement(pieces);
      }
    });
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
      const targetEngineNumber = recycle.getTarget();
      const valueEngineNumber = recycle.getValue();
      const pieces = [
        "recover",
        formatEngineNumber(targetEngineNumber),
        "with",
        formatEngineNumber(valueEngineNumber),
        "reuse",
      ];

      // Add induction clause if specified
      const induction = recycle.getInduction ? recycle.getInduction() : null;
      if (induction !== null) {
        pieces.push("with");
        if (induction === "default") {
          pieces.push("default");
        } else if (induction instanceof EngineNumber) {
          pieces.push(formatEngineNumber(induction));
        } else {
          // Handle string numbers or other formats
          pieces.push(induction.toString());
          pieces.push("%");
        }
        pieces.push("induction");
      }

      const stage = recycle.getStage ? recycle.getStage() : "recharge";
      if (stage !== "recharge") {
        pieces.push("at");
        pieces.push(stage);
      }

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
        formatEngineNumber(replace.getVolume()),
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

    if (startYear && endYear && startYear.equals(endYear)) {
      pieces.push("during year " + startYear.getYearStr());
      return;
    }

    const processUnbounded = () => {
      const noStart = startYear === null;
      const startYearRealized = noStart ? "beginning" : startYear.getYearStr();

      const noEnd = endYear === null;
      const endYearRealized = noEnd ? "onwards" : endYear.getYearStr();

      pieces.push("during years " + startYearRealized + " to " + endYearRealized);
    };

    const processBounded = () => {
      const startYearValue = startYear.getNumericYear();
      const endYearValue = endYear.getNumericYear();
      const startYearRearrange = Math.min(startYearValue, endYearValue);
      const endYearRearrange = Math.max(startYearValue, endYearValue);
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
 * Metadata container for substance properties that can be exported to CSV.
 *
 * This class centralizes all CSV-mappable attributes for a substance, providing
 * a clean interface for metadata manipulation and export/import operations.
 */
class SubstanceMetadata {
  /**
   * Create a new SubstanceMetadata instance.
   *
   * @param {string} substance - Name of the substance without equipment type
   * @param {string} equipment - Optional equipment type (added with " - " separator)
   * @param {string} application - Name of the application that consumes this substance
   * @param {string} ghg - GWP conversion value (e.g., "1430 kgCO2e / kg")
   * @param {boolean} hasDomestic - Whether domestic stream is enabled
   * @param {boolean} hasImport - Whether import stream is enabled
   * @param {boolean} hasExport - Whether export stream is enabled
   * @param {string} energy - Annual energy consumption (e.g., "500 kwh / unit")
   * @param {string} initialChargeDomestic - Initial charge for domestic stream
   * @param {string} initialChargeImport - Initial charge for import stream
   * @param {string} initialChargeExport - Initial charge for export stream
   * @param {string} retirement - Retirement rate (e.g., "10% / year")
   * @param {string} retirementWithReplacement - String "true" or "false" for replacement flag
   * @param {string} defaultSales - Sales assumption mode (user-facing value)
   */
  constructor(
    substance,
    equipment,
    application,
    ghg,
    hasDomestic,
    hasImport,
    hasExport,
    energy,
    initialChargeDomestic,
    initialChargeImport,
    initialChargeExport,
    retirement,
    retirementWithReplacement,
    defaultSales,
  ) {
    const self = this;
    self._substance = substance || "";
    self._equipment = equipment || "";
    self._application = application || "";
    self._ghg = ghg || "";
    self._hasDomestic = hasDomestic || false;
    self._hasImport = hasImport || false;
    self._hasExport = hasExport || false;
    self._energy = energy || "";
    self._initialChargeDomestic = initialChargeDomestic || "";
    self._initialChargeImport = initialChargeImport || "";
    self._initialChargeExport = initialChargeExport || "";
    self._retirement = retirement || "";
    self._retirementWithReplacement = retirementWithReplacement || "";
    self._defaultSales = defaultSales || "";
  }

  /**
   * Get the substance name (without equipment type).
   *
   * @returns {string} The substance name.
   */
  getSubstance() {
    const self = this;
    return self._substance;
  }

  /**
   * Get the equipment type.
   *
   * @returns {string} The equipment type.
   */
  getEquipment() {
    const self = this;
    return self._equipment;
  }

  /**
   * Get the effective name combining substance and equipment type.
   *
   * Handles concatenation logic similar to getEffectiveName in ui_editor.js.
   * If equipment type is empty, returns just the substance name.
   * Otherwise, returns "substance - equipment".
   *
   * @returns {string} The effective name for the substance.
   */
  getName() {
    const self = this;
    if (!self._equipment || self._equipment.trim() === "") {
      return self._substance;
    } else {
      return self._substance + " - " + self._equipment;
    }
  }

  /**
   * Get the CSV key format used by ConsumptionListPresenter.
   *
   * Returns the format: "substanceName" for "applicationName"
   *
   * @returns {string} The key format for CSV operations.
   */
  getKey() {
    const self = this;
    return '"' + self.getName() + '" for "' + self._application + '"';
  }

  /**
   * Get the application name.
   *
   * @returns {string} The application name.
   */
  getApplication() {
    const self = this;
    return self._application;
  }

  /**
   * Get the GHG equivalency value.
   *
   * @returns {string} The GHG value.
   */
  getGhg() {
    const self = this;
    return self._ghg;
  }

  /**
   * Check if domestic stream is enabled.
   *
   * @returns {boolean} True if domestic stream is enabled.
   */
  getHasDomestic() {
    const self = this;
    return self._hasDomestic;
  }

  /**
   * Check if import stream is enabled.
   *
   * @returns {boolean} True if import stream is enabled.
   */
  getHasImport() {
    const self = this;
    return self._hasImport;
  }

  /**
   * Check if export stream is enabled.
   *
   * @returns {boolean} True if export stream is enabled.
   */
  getHasExport() {
    const self = this;
    return self._hasExport;
  }

  /**
   * Get the energy consumption value.
   *
   * @returns {string} The energy consumption value.
   */
  getEnergy() {
    const self = this;
    return self._energy;
  }

  /**
   * Get the initial charge for domestic stream.
   *
   * @returns {string} The initial charge value.
   */
  getInitialChargeDomestic() {
    const self = this;
    return self._initialChargeDomestic;
  }

  /**
   * Get the initial charge for import stream.
   *
   * @returns {string} The initial charge value.
   */
  getInitialChargeImport() {
    const self = this;
    return self._initialChargeImport;
  }

  /**
   * Get the initial charge for export stream.
   *
   * @returns {string} The initial charge value.
   */
  getInitialChargeExport() {
    const self = this;
    return self._initialChargeExport;
  }

  /**
   * Get the retirement rate.
   *
   * @returns {string} The retirement rate.
   */
  getRetirement() {
    const self = this;
    return self._retirement;
  }

  /**
   * Get the retirement with replacement flag.
   *
   * @returns {string} String "true" or "false", or empty string if not set.
   */
  getRetirementWithReplacement() {
    const self = this;
    return self._retirementWithReplacement;
  }

  /**
   * Get the default sales assumption mode.
   *
   * @returns {string} The default sales mode (user-facing value).
   */
  getDefaultSales() {
    const self = this;
    return self._defaultSales;
  }
}

/**
 * Builder for constructing SubstanceMetadata instances with fluent interface.
 *
 * Provides method chaining for easy construction of metadata objects.
 */
class SubstanceMetadataBuilder {
  /**
   * Create a new SubstanceMetadataBuilder.
   */
  constructor() {
    const self = this;
    self._substance = null;
    self._equipment = null;
    self._application = null;
    self._ghg = null;
    self._hasDomestic = false;
    self._hasImport = false;
    self._hasExport = false;
    self._energy = null;
    self._initialChargeDomestic = null;
    self._initialChargeImport = null;
    self._initialChargeExport = null;
    self._retirement = null;
    self._retirementWithReplacement = null;
    self._defaultSales = null;
  }

  /**
   * Set the substance name.
   *
   * @param {string} substance - The substance name.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setSubstance(substance) {
    const self = this;
    self._substance = substance;
    return self;
  }

  /**
   * Set the equipment type.
   *
   * @param {string} equipment - The equipment type.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setEquipment(equipment) {
    const self = this;
    self._equipment = equipment;
    return self;
  }

  /**
   * Set the application name.
   *
   * @param {string} application - The application name.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setApplication(application) {
    const self = this;
    self._application = application;
    return self;
  }

  /**
   * Set the GHG equivalency value.
   *
   * @param {string} ghg - The GHG value.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setGhg(ghg) {
    const self = this;
    self._ghg = ghg;
    return self;
  }

  /**
   * Set whether domestic stream is enabled.
   *
   * @param {boolean} hasDomestic - True if domestic stream is enabled.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setHasDomestic(hasDomestic) {
    const self = this;
    self._hasDomestic = hasDomestic || false;
    return self;
  }

  /**
   * Set whether import stream is enabled.
   *
   * @param {boolean} hasImport - True if import stream is enabled.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setHasImport(hasImport) {
    const self = this;
    self._hasImport = hasImport || false;
    return self;
  }

  /**
   * Set whether export stream is enabled.
   *
   * @param {boolean} hasExport - True if export stream is enabled.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setHasExport(hasExport) {
    const self = this;
    self._hasExport = hasExport || false;
    return self;
  }

  /**
   * Set the energy consumption value.
   *
   * @param {string} energy - The energy consumption value.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setEnergy(energy) {
    const self = this;
    self._energy = energy;
    return self;
  }

  /**
   * Set the initial charge for domestic stream.
   *
   * @param {string} initialChargeDomestic - The initial charge value.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setInitialChargeDomestic(initialChargeDomestic) {
    const self = this;
    self._initialChargeDomestic = initialChargeDomestic;
    return self;
  }

  /**
   * Set the initial charge for import stream.
   *
   * @param {string} initialChargeImport - The initial charge value.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setInitialChargeImport(initialChargeImport) {
    const self = this;
    self._initialChargeImport = initialChargeImport;
    return self;
  }

  /**
   * Set the initial charge for export stream.
   *
   * @param {string} initialChargeExport - The initial charge value.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setInitialChargeExport(initialChargeExport) {
    const self = this;
    self._initialChargeExport = initialChargeExport;
    return self;
  }

  /**
   * Set the retirement rate.
   *
   * @param {string} retirement - The retirement rate.
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setRetirement(retirement) {
    const self = this;
    self._retirement = retirement;
    return self;
  }

  /**
   * Set the retirement with replacement flag.
   *
   * @param {string} retirementWithReplacement - String "true" or "false".
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setRetirementWithReplacement(retirementWithReplacement) {
    const self = this;
    self._retirementWithReplacement = retirementWithReplacement;
    return self;
  }

  /**
   * Set the default sales assumption mode.
   *
   * @param {string} defaultSales - The default sales mode (user-facing value).
   * @returns {SubstanceMetadataBuilder} This builder instance for method chaining.
   */
  setDefaultSales(defaultSales) {
    const self = this;
    self._defaultSales = defaultSales;
    return self;
  }

  /**
   * Build a SubstanceMetadata instance from current builder state.
   *
   * @returns {SubstanceMetadata} The constructed metadata instance.
   * @throws {Error} If required fields are null or empty.
   */
  build() {
    const self = this;

    // Validate required fields are non-null
    if (self._substance === null || self._substance === undefined) {
      throw new Error("Substance name is required");
    }
    if (self._application === null || self._application === undefined) {
      throw new Error("Application name is required");
    }

    // Convert null values to empty strings for optional fields to maintain compatibility
    return new SubstanceMetadata(
      self._substance,
      self._equipment || "",
      self._application,
      self._ghg || "",
      self._hasDomestic,
      self._hasImport,
      self._hasExport,
      self._energy || "",
      self._initialChargeDomestic || "",
      self._initialChargeImport || "",
      self._initialChargeExport || "",
      self._retirement || "",
      self._retirementWithReplacement || "",
      self._defaultSales || "",
    );
  }
}

export {
  AboutStanza,
  Application,
  AssumeCommand,
  Command,
  DefinitionalStanza,
  IncompatibleCommand,
  LimitCommand,
  Program,
  RechargeCommand,
  RecycleCommand,
  ReplaceCommand,
  RetireCommand,
  SimulationScenario,
  SimulationStanza,
  Substance,
  SubstanceBuilder,
  SubstanceMetadata,
  SubstanceMetadataBuilder,
};
