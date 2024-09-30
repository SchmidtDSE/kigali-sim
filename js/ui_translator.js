/**
 * Logic to interpret a plastics language script.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";
import {YearMatcher} from "engine_state";

const COMMAND_COMPATIBILITIES = {
  "change": "any",
  "retire": "any",
  "set": "any",
  "initial charge": "definition",
  "emit": "definition",
  "recycle": "policy",
  "cap": "policy",
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

  getApplications() {
    const self = this;
    return self._applications;
  }

  getPolicies() {
    const self = this;
    return self._policies;
  }

  getScenarios() {
    const self = this;
    return self._scenarios;
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

  getSubstances() {
    const self = this;
    return self._substances;
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
    self._initialCharge = null;
    self._cap = null;
    self._change = null;
    self._emit = null;
    self._recharge = null;
    self._recycle = null;
    self._replace = null;
    self._retire = null;
    self._setVal = null;
  }

  build(isCompatibleRaw) {
    const self = this;

    const commandsConsolidatedInterpreted = [
      self._initialCharge,
      self._cap,
      self._change,
      self._emit,
      self._recharge,
      self._recycle,
      self._replace,
      self._retire,
      self._setVal,
    ];
    const isCompatibleInterpreted = commandsConsolidatedInterpreted
      .filter((x) => x !== null)
      .map((x) => x.getIsCompatible())
      .reduce((a, b) => a && b);

    const isCompatible = isCompatibleRaw && isCompatibleInterpreted;

    return new Substance(
      self._name,
      self._initialCharge,
      self._cap,
      self._change,
      self._emit,
      self._recharge,
      self._recycle,
      self._replace,
      self._retire,
      self._setVal,
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
    const needsToMoveToMod = requiresMod && !self._isModification;
    const needsToMoveToDefinition = requiresDefinition && self._isModification;
    const incompatiblePlace = needsToMoveToMod || needsToMoveToDefinition;

    const strategy = {
      "change": (x) => self.setChange(x),
      "retire": (x) => self.setRetire(x),
      "set": (x) => self.setSetVal(x),
      "initial charge": (x) => self.setInitialCharge(x),
      "emit": (x) => self.setEmit(x),
      "recycle": (x) => self.setRecycle(x),
      "cap": (x) => self.setCap(x),
      "replace": (x) => self.setReplace(x),
    }[commandType];

    const effectiveCommand = incompatiblePlace ? self._makeInvalidPlacement() : command;
    strategy(effectiveCommand);
  }
  
  setName(newVal) {
    const self = this;
    self._name = newVal;
  }
  
  setInitialCharge(newVal) {
    const self = this;
    self._initialCharge = self._checkDuplicate(self._initialCharge, newVal);
  }
  
  setCap(newVal) {
    const self = this;
    self._cap = self._checkDuplicate(self._cap, newVal);
  }
  
  setChange(newVal) {
    const self = this;
    self._change = self._checkDuplicate(self._change, newVal);
  }
  
  setEmit(newVal) {
    const self = this;
    self._emit = self._checkDuplicate(self._emit, newVal);
  }
  
  setRecharge(newVal) {
    const self = this;
    self._recharge = self._checkDuplicate(self._recharge, newVal);
  }
  
  setRecycle(newVal) {
    const self = this;
    self._recycle = self._checkDuplicate(self._recycle, newVal);
  }
  
  setReplace(newVal) {
    const self = this;
    self._replace = self._checkDuplicate(self._replace, newVal);
  }
  
  setRetire(newVal) {
    const self = this;
    self._retire = self._checkDuplicate(self._retire, newVal);
  }
  
  setSetVal(newVal) {
    const self = this;
    self._setVal = self._checkDuplicate(self._setVal, newVal);
  }

  _checkDuplicate(originalVal, newVal) {
    if (originalVal === null) {
      return newVal;
    } else {
      return new IncompatibleCommand('duplicate');
    }
  }

  _makeInvalidPlacement() {
    const self = this;
    return new IncompatibleCommand("invalid placement");
  }
}


class Substance {
  constructor(name, charge, cap, change, emit, recharge, recycle, replace, retire, setVal, isMod,
    compat) {
    const self = this;
    self._name = name;
    self._initialCharge = charge;
    self._cap = cap;
    self._change = change;
    self._emit = emit;
    self._recharge = recharge;
    self._recycle = recycle;
    self._replace = replace;
    self._retire = retire;
    self._setVal = setVal;
    self._isModification = isMod;
    self._isCompatible = compat;
  }

  getName() {
    const self = this;
    return self._name;
  }

  getInitialCharge() {
    const self = this;
    return self._initialCharge;
  }

  getCap() {
    const self = this;
    return self._cap;
  }

  getChange() {
    const self = this;
    return self._change;
  }

  getEmit() {
    const self = this;
    return self._emit;
  }

  getRecharge() {
    const self = this;
    return self._recharge;
  }

  getRecycle() {
    const self = this;
    return self._recycle;
  }

  getReplace() {
    const self = this;
    return self._replace;
  }

  getRetire() {
    const self = this;
    return self._retire;
  }

  getSetVal() {
    const self = this;
    return self._setVal;
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
    addCode(prefix + " substance \"" + self.getName() + "\"", spaces);

    const addIfGiven = (code) => {
      if (code === null) {
        return;
      }
      addCode(code, spaces + 2);
    };

    addIfGiven(self._getInitialChargeCode());
    addIfGiven(self._getEmitCode());
    addIfGiven(self._getSetValCode());
    addIfGiven(self._getChangeCode());
    addIfGiven(self._getRetireCode());
    addIfGiven(self._getCapCode());
    addIfGiven(self._getRechargeCode());
    addIfGiven(self._getRecycleCode());
    addIfGiven(self._getReplaceCode());

    addCode("end substance", spaces);
    return finalizeCodePieces(baselinePieces);
  }

  _getInitialChargeCode() {
    const self = this;
    if (self._initialCharge === null) {
      return null;
    }

    const pieces = [
      "initial charge with",
      self._initialCharge.getValue().getValue(),
      self._initialCharge.getValue().getUnits(),
      "for",
      self._initialCharge.getTarget(),
    ];
    self._addDuration(pieces, self._initialCharge);

    return self._finalizeStatement(pieces);
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

  _getSetValCode() {
    const self = this;
    if (self._setVal === null) {
      return null;
    }

    const pieces = [
      "set",
      self._setVal.getTarget(),
      "to",
      self._setVal.getValue().getValue(),
      self._setVal.getValue().getUnits(),
    ];
    self._addDuration(pieces, self._setVal);

    return self._finalizeStatement(pieces);
  }

  _getChangeCode() {
    const self = this;
    if (self._change === null) {
      return null;
    }

    const pieces = [
      "change",
      self._change.getTarget(),
      "by",
      self._change.getValue().getValue(),
      self._change.getValue().getUnits(),
    ];
    self._addDuration(pieces, self._change);

    return self._finalizeStatement(pieces);
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

  _getCapCode() {
    const self = this;
    if (self._cap === null) {
      return null;
    }

    const pieces = [
      "cap",
      self._cap.getTarget(),
      "to",
      self._cap.getValue().getValue(),
      self._cap.getValue().getUnits(),
    ];
    self._addDuration(pieces, self._cap);

    return self._finalizeStatement(pieces);
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
    if (self._recycle === null) {
      return null;
    }

    const pieces = [
      "recover",
      self._recycle.getTarget().getValue(),
      self._recycle.getTarget().getUnits(),
      "with",
      self._recycle.getValue().getValue(),
      self._recycle.getValue().getUnits(),
      "reuse",
    ];
    self._addDuration(pieces, self._recycle);

    return self._finalizeStatement(pieces);
  }

  _getReplaceCode() {
    const self = this;
    if (self._replace === null) {
      return null;
    }

    const pieces = [
      "replace",
      self._replace.getVolume().getValue(),
      self._replace.getVolume().getUnits(),
      "of",
      self._replace.getSource(),
      "with",
      "\"" + self._replace.getDestination() + "\"",
    ];
    self._addDuration(pieces, self._replace);

    return self._finalizeStatement(pieces);
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

  buildDuring(minYearMaybe, maxYearMaybe) {
    const self = this;
    return (engine) => {
      const minYear = minYearMaybe === null ? null : minYearMaybe(engine);
      const maxYear = maxYearMaybe === null ? null : maxYearMaybe(engine);
      return new YearMatcher(minYear, maxYear);
    };
  }

  visitDuringSingleYear(ctx) {
    const self = this;
    const yearFuture = ctx.target.accept(self);
    return self.buildDuring(yearFuture, yearFuture);
  }

  visitDuringStart(ctx) {
    const self = this;
    const getStartYear = (engine) => engine.getStartYear();
    return self.buildDuring(getStartYear, getStartYear);
  }

  visitDuringRange(ctx) {
    const self = this;
    const lowerFuture = ctx.lower.accept(self);
    const upperFuture = ctx.upper.accept(self);
    return self.buildDuring(lowerFuture, upperFuture);
  }

  visitDuringWithMin(ctx) {
    const self = this;
    const lowerFuture = ctx.lower.accept(self);
    const upperFuture = (engine) => engine.getEndYear();
    return self.buildDuring(lowerFuture, upperFuture);
  }

  visitDuringWithMax(ctx) {
    const self = this;
    const lowerFuture = (engine) => engine.getStartYear();
    const upperFuture = ctx.upper.accept(self);
    return self.buildDuring(lowerFuture, upperFuture);
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

  visitCapAllYears(ctx) {
    const self = this;
    return self._buildOperation(ctx, "cap", null);
  }

  visitCapDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "cap", duration);
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
    return self._buildOperation(ctx, "set", null);
  }

  visitSetDuration(ctx) {
    const self = this;
    const duration = ctx.duration.accept(self);
    return self._buildOperation(ctx, "set", duration);
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
      return;
    }

    if (!stanzasByName.has("simulations")) {
      return;
    }

    const applications = stanzasByName.get("default").getApplications();

    const allStanzaNames = Array.of(...stanzasByName.keys());
    const policies = allStanzaNames
      .filter((x) => x !== "default")
      .filter((x) => x !== "about")
      .filter((x) => x !== "simulations")
      .map((x) => stanzasByName.get(x));

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
    return children.map((x) => x.getIsCompatible()).reduce((a, b) => a && b);
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

    const isCompatibleRaw = commands.map((x) => x.getIsCompatible()).reduce((a, b) => a && b);

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
