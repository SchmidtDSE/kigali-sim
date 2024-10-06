/**
 * Logic to interpret a plastics language script.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";
import {YearMatcher} from "engine_state";

const COMMAND_COMPATIBILITIES = {
  "change": "any",
  "define var": "none",
  "retire": "any",
  "setVal": "any",
  "cap": "any",
  "floor": "any",
  "limit": "any",
  "initial charge": "definition",
  "emit": "definition",
  "recharge": "definition",
  "recycle": "policy",
  "replace": "policy",
};

const toolkit = QubecTalk.getToolkit();


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


function indent(pieces, spaces) {
  return pieces.map((piece) => indentSingle(piece, spaces));
}


function buildAddCode(target) {
  return (x, spaces) => {
    target.push(indentSingle(x, spaces));
  };
}


function finalizeCodePieces(target) {
  return target.join("\n");
}


class Program {
  constructor(applications, policies, scenarios, isCompatible) {
    const self = this;
    self._applications = applications;
    self._policies = policies;
    self._scenarios = scenarios;
    self._isCompatible = isCompatible;
  }

  getSubstances() {
    const self = this;
    return self.getApplications().map((x) => x.getSubstances()).flat();
  }

  insertSubstance(priorApplication, priorSubstanceName, substance) {
    const self = this;
    const application = self.getApplication(priorApplication);
    application.insertSubstance(priorSubstanceName, substance);
  }

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

  getApplications() {
    const self = this;
    return self._applications;
  }

  getApplication(name) {
    const self = this;
    const matching = self._applications.filter((x) => x.getName() === name);
    return matching.length == 0 ? null : matching[0];
  }

  addApplication(newApplication) {
    const self = this;
    self._applications.push(newApplication);
  }

  deleteApplication(name) {
    const self = this;
    self._applications = self._applications.filter((x) => x.getName() !== name);
    self._policies = self._policies.filter((x) => x.getApplications()[0].getName() !== name);
    self._removeUnknownPoliciesFromScenarios();
  }

  renameApplication(oldName, newName) {
    const self = this;
    const priorApplications = self._applications.filter((x) => x.getName() === oldName);
    priorApplications.forEach((x) => x.rename(newName));
  }

  getPolicies() {
    const self = this;
    return self._policies;
  }

  getPolicy(name) {
    const self = this;
    const matching = self._policies.filter((x) => x.getName() === name);
    return matching.length == 0 ? null : matching[0];
  }

  deletePolicy(name) {
    const self = this;
    self._policies = self._policies.filter((x) => x.getName() !== name);
    self._removeUnknownPoliciesFromScenarios();
  }

  insertPolicy(oldName, newPolicy) {
    const self = this;
    self.deletePolicy(oldName);
    self._policies.push(newPolicy);
  }

  getScenarios() {
    const self = this;
    return self._scenarios;
  }

  getScenario(name) {
    const self = this;
    const matching = self._scenarios.filter((x) => x.getName() === name);
    return matching.length == 0 ? null : matching[0];
  }

  deleteScenario(name) {
    const self = this;
    self._scenarios = self._scenarios.filter((x) => x.getName() !== name);
  }

  insertScenario(oldName, scenario) {
    const self = this;
    self.deleteScenario(oldName);
    self._scenarios.push(scenario);
  }

  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

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
      const policiesCode = self.getPolicies().map((x) => x.toCode(spaces)).join("\n\n\n\n");
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
}


class AboutStanza {
  getName() {
    const self = this;
    return "about";
  }

  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    addCode("start about", spaces);
    addCode("end about", spaces);

    return finalizeCodePieces(baselinePieces);
  }

  getIsCompatible() {
    const self = this;
    return false;
  }
}


class DefinitionalStanza {
  constructor(name, applications, isCompatible) {
    const self = this;
    self._name = name;
    self._applications = applications;
    self._isCompatible = isCompatible;
  }

  getName() {
    const self = this;
    return self._name;
  }

  getApplications() {
    const self = this;
    return self._applications;
  }

  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);
    const isDefault = self.getName() === "default";

    addCode("start " + (isDefault ? "default" : ("policy \"" + self.getName() + "\"")), spaces);
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


class SimulationScenario {
  constructor(name, policyNames, yearStart, yearEnd, isCompatible) {
    const self = this;
    self._name = name;
    self._policyNames = policyNames;
    self._yearStart = yearStart;
    self._yearEnd = yearEnd;
    self._isCompatible = isCompatible;
  }

  getName() {
    const self = this;
    return self._name;
  }

  getPolicyNames() {
    const self = this;
    return self._policyNames;
  }

  getYearStart() {
    const self = this;
    return self._yearStart;
  }

  getYearEnd() {
    const self = this;
    return self._yearEnd;
  }

  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    addCode("simulate \"" + self.getName() + "\"", spaces);

    if (self.getPolicyNames().length > 0) {
      self.getPolicyNames().forEach((x, i) => {
        const prefix = i == 0 ? "using" : "then";
        addCode(prefix + " \"" + x + "\"", spaces + 2);
      });
    }

    addCode("from years " + self.getYearStart() + " to " + self.getYearEnd(), spaces);
    return finalizeCodePieces(baselinePieces);
  }
}


class SimulationStanza {
  constructor(scenarios, isCompatible) {
    const self = this;
    self._scenarios = scenarios;
    self._isCompatible = isCompatible;
  }

  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  getScenarios() {
    const self = this;
    return self._scenarios;
  }

  getName() {
    const self = this;
    return "simulations";
  }

  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    addCode("start simulations", spaces);

    if (self.getScenarios().length > 0) {
      addCode("", spaces);
      const scenariosCode = self.getScenarios()
        .map((x) => x.toCode(2))
        .join("\n\n\n");
      addCode(scenariosCode, spaces);
      addCode("", spaces);
    }

    addCode("end simulations", spaces);
    return finalizeCodePieces(baselinePieces);
  }
}


class Application {
  constructor(name, substances, isModification, isCompatible) {
    const self = this;
    self._name = name;
    self._substances = substances;
    self._isModification = isModification;
    self._isCompatible = isCompatible;
  }

  getName() {
    const self = this;
    return self._name;
  }

  rename(newName) {
    const self = this;
    self._name = newName;
  }

  getSubstances() {
    const self = this;
    return self._substances;
  }

  insertSubstance(substanceName, newVersion) {
    const self = this;
    self.deleteSubstance(substanceName);
    self._substances.push(newVersion);
  }

  deleteSubstance(substanceName) {
    const self = this;
    self._substances = self._substances.filter((x) => x.getName() !== substanceName);
  }

  getSubstance(name) {
    const self = this;
    const matching = self._substances.filter((x) => x.getName() === name);
    return matching.length == 0 ? null : matching[0];
  }

  getIsModification() {
    const self = this;
    return self._isModification;
  }

  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    const prefix = self.getIsModification() ? "modify" : "define";
    addCode(prefix + " application \"" + self.getName() + "\"", spaces);

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


class SubstanceBuilder {
  constructor(name, isModification) {
    const self = this;
    self._name = name;
    self._isModification = isModification;
    self._initialCharges = [];
    self._limits = [];
    self._changes = [];
    self._emit = null;
    self._recharge = null;
    self._recycles = [];
    self._replaces = [];
    self._retire = null;
    self._setVals = [];
  }

  build(isCompatibleRaw) {
    const self = this;

    const commandsConsolidatedInterpreted = [
      self._initialCharges,
      self._limits,
      self._recycles,
      self._replaces,
      [
        self._emit,
        self._recharge,
        self._retire,
      ],
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
      self._emit,
      self._recharge,
      self._recycles,
      self._replaces,
      self._retire,
      self._setVals,
      self._isModification,
      isCompatible,
    );
  }

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
      "emit": (x) => self.setEmit(x),
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

  setName(newVal) {
    const self = this;
    self._name = newVal;
  }

  addInitialCharge(newVal) {
    const self = this;
    self._initialCharges.push(newVal);
  }

  addLimit(newVal) {
    const self = this;
    self._limits.push(newVal);
  }

  addChange(newVal) {
    const self = this;
    self._changes.push(newVal);
  }

  setEmit(newVal) {
    const self = this;
    self._emit = self._checkDuplicate(self._emit, newVal);
  }

  setRecharge(newVal) {
    const self = this;
    self._recharge = self._checkDuplicate(self._recharge, newVal);
  }

  addRecycle(newVal) {
    const self = this;
    self._recycles.push(newVal);
  }

  addReplace(newVal) {
    const self = this;
    self._replaces.push(newVal);
  }

  setRetire(newVal) {
    const self = this;
    self._retire = self._checkDuplicate(self._retire, newVal);
  }

  addSetVal(newVal) {
    const self = this;
    self._setVals.push(newVal);
  }

  _checkDuplicate(originalVal, newVal) {
    if (originalVal === null) {
      return newVal;
    } else {
      return new IncompatibleCommand("duplicate");
    }
  }

  _makeInvalidPlacement() {
    const self = this;
    return new IncompatibleCommand("invalid placement");
  }
}


class Substance {
  constructor(name, charges, limits, changes, emit, recharge, recycles, replaces, retire, setVals,
    isMod, compat) {
    const self = this;
    self._name = name;
    self._initialCharges = charges;
    self._limits = limits;
    self._changes = changes;
    self._emit = emit;
    self._recharge = recharge;
    self._recycles = recycles;
    self._replaces = replaces;
    self._retire = retire;
    self._setVals = setVals;
    self._isModification = isMod;
    self._isCompatible = compat;
  }

  getName() {
    const self = this;
    return self._name;
  }

  getInitialCharges() {
    const self = this;
    return self._initialCharges;
  }

  getInitialCharge(stream) {
    const self = this;
    const matching = self._initialCharges.filter((x) => x.getTarget() === stream);
    return matching.length == 0 ? null : matching[0];
  }

  getLimits() {
    const self = this;
    return self._limits;
  }

  getChanges() {
    const self = this;
    return self._changes;
  }

  getEmit() {
    const self = this;
    return self._emit;
  }

  getRecharge() {
    const self = this;
    return self._recharge;
  }

  getRecycles() {
    const self = this;
    return self._recycles;
  }

  getReplaces() {
    const self = this;
    return self._replaces;
  }

  getRetire() {
    const self = this;
    return self._retire;
  }

  getSetVals() {
    const self = this;
    return self._setVals;
  }

  getIsModification() {
    const self = this;
    return self._isModification;
  }

  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }

  toCode(spaces) {
    const self = this;

    const baselinePieces = [];
    const addCode = buildAddCode(baselinePieces);

    const prefix = self.getIsModification() ? "modify" : "uses";
    addCode(prefix + " substance \"" + self.getName() + "\"", spaces);

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
    addIfGiven(self._getEmitCode());
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

  _getEmitCode() {
    const self = this;
    if (self._emit === null) {
      return null;
    }

    const pieces = [
      "emit",
      self._emit.getValue().getValue(),
      self._emit.getValue().getUnits(),
    ];
    self._addDuration(pieces, self._emit);

    return self._finalizeStatement(pieces);
  }

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
        pieces.push("\"" + displacing + "\"");
      }

      self._addDuration(pieces, limit);
      return self._finalizeStatement(pieces);
    };

    return self._limits.map(buildLimit);
  }

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
        "\"" + replace.getDestination() + "\"",
      ];
      self._addDuration(pieces, replace);

      return self._finalizeStatement(pieces);
    };

    return self._replaces.map(buildReplace);
  }

  _addDuration(pieces, command) {
    const self = this;

    const duration = command.getDuration();
    if (duration === null) {
      return;
    }

    let startYear = duration.getStart();
    let endYear = duration.getEnd();
    if (startYear === null && endYear === null) {
      return;
    }

    if (startYear == endYear) {
      pieces.push("during year " + startYear);
      return;
    }

    if (startYear === null) {
      startYear = "beginning";
    }

    if (endYear === null) {
      endYear = "onwards";
    }

    pieces.push("during years " + startYear + " to " + endYear);
  }

  _finalizeStatement(pieces) {
    const self = this;
    return pieces.map((x) => x + "").join(" ");
  }
}


class Command {
  constructor(typeName, target, value, duration) {
    const self = this;
    self._typeName = typeName;
    self._target = target;
    self._value = value;
    self._duration = duration;
  }

  getTypeName() {
    const self = this;
    return self._typeName;
  }

  getTarget() {
    const self = this;
    return self._target;
  }

  getValue() {
    const self = this;
    return self._value;
  }

  getDuration() {
    const self = this;
    return self._duration;
  }

  getIsCompatible() {
    const self = this;
    return true;
  }
}


class LimitCommand {
  constructor(typeName, target, value, duration, displacing) {
    const self = this;
    self._typeName = typeName;
    self._target = target;
    self._value = value;
    self._duration = duration;
    self._displacing = displacing;
  }

  getTypeName() {
    const self = this;
    return self._typeName;
  }

  getTarget() {
    const self = this;
    return self._target;
  }

  getValue() {
    const self = this;
    return self._value;
  }

  getDuration() {
    const self = this;
    return self._duration;
  }

  getDisplacing() {
    const self = this;
    return self._displacing;
  }

  getIsCompatible() {
    const self = this;
    return true;
  }
}


class ReplaceCommand {
  constructor(volume, source, destination, duration) {
    const self = this;
    self._volume = volume;
    self._source = source;
    self._destination = destination;
    self._duration = duration;
  }

  getTypeName() {
    const self = this;
    return "replace";
  }

  getVolume() {
    const self = this;
    return self._volume;
  }

  getSource() {
    const self = this;
    return self._source;
  }

  getDestination() {
    const self = this;
    return self._destination;
  }

  getDuration() {
    const self = this;
    return self._duration;
  }

  getIsCompatible() {
    const self = this;
    return true;
  }
}


class IncompatibleCommand {
  constructor(typeName) {
    const self = this;
    self._typeName = typeName;
  }

  getTypeName() {
    const self = this;
    return self._typeName;
  }

  getIsCompatible() {
    const self = this;
    return false;
  }
}


/**
 * Visitor which compiles a QubecTalk program to JS objects describing the analysis.
 *
 * Visitor which attempts to compile a QubecTalk program to JS objects describing the anlaysis or
 * indication that the anlaysis cannot use the simplified JS object format.
 */
class TranslatorVisitor extends toolkit.QubecTalkVisitor {
  visitNumber(ctx) {
    const self = this;

    const raw = ctx.getText();
    const signMultiplier = raw.includes("-") ? -1 : 1;
    const bodyRawText = ctx.getChild(ctx.getChildCount() - 1).getText();
    const bodyParsed = signMultiplier * parseFloat(bodyRawText);

    return bodyParsed;
  }

  visitString(ctx) {
    const self = this;
    return self._getStringWithoutQuotes(ctx.getText());
  }

  visitUnitOrRatio(ctx) {
    const self = this;
    if (ctx.getChildCount() == 1) {
      return ctx.getChild(0).getText();
    } else {
      const numerator = ctx.getChild(0).getText();
      const denominator = ctx.getChild(2).getText();
      return numerator + " / " + denominator;
    }
  }

  visitUnitValue(ctx) {
    const self = this;

    const unitString = ctx.getChild(1).accept(self);
    const expressionContent = ctx.getChild(0).accept(self);

    return new EngineNumber(expressionContent, unitString);
  }

  visitSimpleExpression(ctx) {
    const self = this;
    return ctx.getChild(0).accept(self);
  }

  visitConditionExpression(ctx) {
    const self = this;

    const posExpression = ctx.pos.accept(self);
    const opFunc = ctx.op.text;
    const negExpression = ctx.neg.accept(self);

    return posExpression + " " + opFunc + " " + negExpression;
  }

  visitConditionalExpression(ctx) {
    const self = this;

    const condition = ctx.cond.accept(self);
    const positive = ctx.pos.accept(self);
    const negative = ctx.neg.accept(self);

    return positive + " if " + condition + " else " + negative + " endif";
  }

  buildAirthmeticExpression(ctx, op) {
    const self = this;

    const priorExpression = ctx.getChild(0).accept(self);
    const afterExpression = ctx.getChild(2).accept(self);

    return priorExpression + " " + op + " " + afterExpression;
  }

  visitAdditionExpression(ctx) {
    const self = this;
    return self.buildAirthmeticExpression(ctx, ctx.op.text);
  }

  visitMultiplyExpression(ctx) {
    const self = this;
    return self.buildAirthmeticExpression(ctx, ctx.op.text);
  }

  visitPowExpression(ctx) {
    const self = this;
    return self.buildAirthmeticExpression(ctx, "^");
  }

  visitGetStream(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitGetStreamIndirect(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitGetStreamConversion(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitGetStreamIndirectSubstanceAppUnits(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitLimitMinExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitLimitMaxExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitLimitBoundExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitParenExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitDrawNormalExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitDrawUniformExpression(ctx) {
    const self = this;
    return ctx.getText();
  }

  visitSimpleIdentifier(ctx) {
    const self = this;
    const identifier = ctx.getChild(0).getText();
    return identifier;
  }

  buildDuring(minYear, maxYear) {
    const self = this;
    return new YearMatcher(minYear, maxYear);
  }

  visitDuringSingleYear(ctx) {
    const self = this;
    const year = ctx.target.accept(self);
    return self.buildDuring(year, year);
  }

  visitDuringStart(ctx) {
    const self = this;
    const startYear = engine.getStartYear();
    return self.buildDuring(startYear, startYear);
  }

  visitDuringRange(ctx) {
    const self = this;
    const lower = ctx.lower.accept(self);
    const upper = ctx.upper.accept(self);
    return self.buildDuring(lower, upper);
  }

  visitDuringWithMin(ctx) {
    const self = this;
    const lower = ctx.lower.accept(self);
    const upper = (engine) => engine.getEndYear();
    return self.buildDuring(lower, upper);
  }

  visitDuringWithMax(ctx) {
    const self = this;
    const lower = (engine) => engine.getStartYear();
    const upper = ctx.upper.accept(self);
    return self.buildDuring(lower, upper);
  }

  visitDuringAll(ctx) {
    const self = this;
    return (engine) => null;
  }

  visitAboutStanza(ctx) {
    const self = this;
    return new AboutStanza();
  }

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

  visitApplicationDef(ctx) {
    const self = this;
    return self._parseApplication(ctx, false);
  }

  visitSubstanceDef(ctx) {
    const self = this;
    return self._parseSubstance(ctx, false);
  }

  visitApplicationMod(ctx) {
    const self = this;
    return self._parseApplication(ctx, true);
  }

  visitSubstanceMod(ctx) {
    const self = this;
    return self._parseSubstance(ctx, true);
  }

  visitLimitCommandAllYears(ctx) {
    const self = this;
    return self._buildLimit(ctx, null, null);
  }

  visitLimitCommandDisplacingAllYears(ctx) {
    const self = this;
    const displaceTarget = self._getStringWithoutQuotes(ctx.getChild(5).getText());
    return self._buildLimit(ctx, null, displaceTarget);
  }

  visitLimitCommandDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildLimit(ctx, duration, null);
  }

  visitLimitCommandDisplacingDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    const displaceTarget = self._getStringWithoutQuotes(ctx.getChild(5).getText());
    return self._buildLimit(ctx, duration, displaceTarget);
  }

  visitChangeAllYears(ctx) {
    const self = this;
    return self._buildOperation(ctx, "change", null);
  }

  visitChangeDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "change", duration);
  }

  visitDefineVarStatement(ctx) {
    const self = this;
    return new IncompatibleCommand("define var");
  }

  visitInitialChargeAllYears(ctx) {
    const self = this;
    return self._buildOperation(ctx, "initial charge", null);
  }

  visitInitialChargeDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "initial charge", duration);
  }

  visitRechargeAllYears(ctx) {
    const self = this;
    const populationFuture = (ctx) => ctx.population.accept(self);
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    return self._buildOperation(ctx, "recharge", null, populationFuture, volumeFuture);
  }

  visitRechargeDuration(ctx) {
    const self = this;
    const populationFuture = (ctx) => ctx.population.accept(self);
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "recharge", duration, populationFuture, volumeFuture);
  }

  visitRecoverAllYears(ctx) {
    const self = this;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const yieldFuture = (ctx) => ctx.yieldVal.accept(self);
    return self._buildOperation(ctx, "recycle", null, volumeFuture, yieldFuture);
  }

  visitRecoverDuration(ctx) {
    const self = this;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const yieldFuture = (ctx) => ctx.yieldVal.accept(self);
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "recycle", duration, volumeFuture, yieldFuture);
  }

  visitRecoverDisplacementAllYears(ctx) {
    const self = this;
    return new IncompatibleCommand("recover with displace");
  }

  visitRecoverDisplacementDuration(ctx) {
    const self = this;
    return new IncompatibleCommand("recover with displace");
  }

  visitReplaceAllYears(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const source = ctx.target.getText();
    const destination = self._getStringWithoutQuotes(ctx.destination.getText());
    return new ReplaceCommand(volume, source, destination, null);
  }

  visitReplaceDuration(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const duration = ctx.duration.accept(self);
    const source = ctx.target.getText();
    const destination = self._getStringWithoutQuotes(ctx.destination.getText());
    return new ReplaceCommand(volume, source, destination, duration);
  }

  visitRetireAllYears(ctx) {
    const self = this;
    const targetFuture = (ctx) => null;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    return self._buildOperation(ctx, "retire", null, targetFuture, volumeFuture);
  }

  visitRetireDuration(ctx) {
    const self = this;
    const targetFuture = (ctx) => null;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "retire", duration, targetFuture, volumeFuture);
  }

  visitSetAllYears(ctx) {
    const self = this;
    return self._buildOperation(ctx, "setVal", null);
  }

  visitSetDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "setVal", duration);
  }

  visitEmitAllYears(ctx) {
    const self = this;
    const targetFuture = (ctx) => null;
    return self._buildOperation(ctx, "emit", null, targetFuture);
  }

  visitEmitDuration(ctx) {
    const self = this;
    const targetFuture = (ctx) => null;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "emit", duration, targetFuture);
  }

  visitBaseSimulation(ctx) {
    const self = this;
    const name = self._getStringWithoutQuotes(ctx.name.getText());
    const yearStart = ctx.start.getText();
    const yearEnd = ctx.end.getText();
    return new SimulationScenario(name, [], yearStart, yearEnd, true);
  }

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

  visitBaseSimulationTrials(ctx) {
    const self = this;
    return new IncompatibleCommand("simulate with trials");
  }

  visitPolicySimTrials(ctx) {
    const self = this;
    return new IncompatibleCommand("simulate with trials");
  }

  visitProgram(ctx) {
    const self = this;

    const stanzasByName = new Map();
    const numStanzas = ctx.getChildCount();

    for (let i = 0; i < numStanzas; i++) {
      const newStanza = ctx.getChild(i).accept(self);
      stanzasByName.set(newStanza.getName(), newStanza);
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

  visitGlobalStatement(ctx) {
    const self = this;
    return ctx.getChild(0).accept(self);
  }

  visitSubstanceStatement(ctx) {
    const self = this;
    return ctx.getChild(0).accept(self);
  }

  _getStringWithoutQuotes(target) {
    const self = this;
    return target.substring(1, target.length - 1);
  }

  _getChildrenCompatible(children) {
    const self = this;
    return children.map((x) => x.getIsCompatible()).reduce((a, b) => a && b, true);
  }

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
 * Structure contianing the result of attempting to translate from QubecTalk script.
 */
class TranslationResult {
  /**
   * Create a new record of a translation attempt.
   *
   * @param program The translated program as a lambda if successful or null if unsuccessful.
   * @param errors Any errors enountered or empty list if no errors.
   */
  constructor(program, errors) {
    const self = this;
    self._program = program;
    self._errors = errors;
  }

  /**
   * Get the program as an oject.
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


class UiTranslatorCompiler {
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
