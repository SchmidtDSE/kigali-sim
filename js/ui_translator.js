/**
 * Logic to interpret a plastics language script.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";
import {YearMatcher} from "engine_state";

const toolkit = QubecTalk.getToolkit();


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
}


class AboutStanza {
  getName() {
    const self = this;
    return "about";
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
}


class Application {
  constructor(name, substances, isCompatible) {
    const self = this;
    self._name = name;
    self._substances = substances;
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

  getIsCompatible() {
    const self = this;
    return self._isCompatible;
  }
}


class Substance {
  constructor(name, charge, cap, change, emit, recharge, recycle, replace, retire, setVal, compat) {
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

  getIsCompatible() {
    const self = this;
    return self._isCompatible;
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

    return new Application(name, childrenParsed, isCompatible);
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

    const commandsByType = new Map();
    commands.forEach((command) => {
      const typeName = command.getTypeName();
      if (commandsByType.has(typeName)) {
        commandsByType.set(typeName, new IncompatibleCommand("repeated type " + typeName));
      } else {
        commandsByType.set(typeName, command);
      }
    });

    const getIfAvailable = (typeName, limitation) => {
      const isAvailable = commandsByType.has(typeName);
      if (!isAvailable) {
        return null;
      }

      if (isAvailable) {
        const requiresMod = limitation === "policy";
        const requiresDefinition = limitation === "definition";
        const needsToMoveToMod = requiresMod && !isModification;
        const needsToMoveToDefinition = requiresDefinition && isModification;
        if (needsToMoveToMod || needsToMoveToDefinition) {
          return new IncompatibleCommand(typeName);
        }
      }

      return commandsByType.get(typeName);
    };

    const charge = getIfAvailable("initial charge", "any");
    const change = getIfAvailable("change", "any");
    const recycle = getIfAvailable("recycle", "any");
    const retire = getIfAvailable("retire", "any");
    const setVal = getIfAvailable("set", "any");

    const emit = getIfAvailable("emit", "definition");
    const recharge = getIfAvailable("emit", "definition");

    const cap = getIfAvailable("cap", "policy");
    const replace = getIfAvailable("replace", "policy");

    const commandsConsolidatedRaw = Array.of(...commandsByType.values());
    const isCompatibleRaw = commandsConsolidatedRaw
      .map((x) => x.getIsCompatible())
      .reduce((a, b) => a && b);

    const commandsConsolidatedInterpreted = [
      charge,
      cap,
      change,
      emit,
      recharge,
      recycle,
      replace,
      retire,
      setVal,
    ];
    const isCompatibleInterpreted = commandsConsolidatedInterpreted
      .filter((x) => x !== null)
      .map((x) => x.getIsCompatible())
      .reduce((a, b) => a && b);

    const isCompatible = isCompatibleRaw && isCompatibleInterpreted;

    return new Substance(
      name,
      charge,
      cap,
      change,
      emit,
      recharge,
      recycle,
      replace,
      retire,
      setVal,
      isCompatible,
    );
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


export {UiTranslatorCompiler};
