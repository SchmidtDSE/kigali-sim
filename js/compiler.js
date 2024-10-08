/**
 * Logic to interpret a plastics language script.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";
import {Engine} from "engine";
import {YearMatcher} from "engine_state";

const toolkit = QubecTalk.getToolkit();


/**
 * Visitor which compiles a QubecTalk program to JS lambdas.
 *
 * Visitor which compiles a QubecTalk program to QubecTalkProgram which contains JS lambdas and
 * simulation functions.
 */
class CompileVisitor extends toolkit.QubecTalkVisitor {
  visitNumber(ctx) {
    const self = this;

    const raw = ctx.getText();
    const signMultiplier = raw.includes("-") ? -1 : 1;
    const bodyRawText = ctx.getChild(ctx.getChildCount() - 1).getText();
    const bodyParsed = signMultiplier * parseFloat(bodyRawText);

    return (engine) => bodyParsed;
  }

  visitString(ctx) {
    const self = this;
    return (engine) => self._getStringWithoutQuotes(ctx.getText());
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
    const expressionFuture = ctx.getChild(0).accept(self);

    return (engine) => {
      const value = expressionFuture(engine);
      return new EngineNumber(value, unitString);
    };
  }

  visitSimpleExpression(ctx) {
    const self = this;
    return ctx.getChild(0).accept(self);
  }

  visitConditionExpression(ctx) {
    const self = this;

    const posExpression = ctx.pos.accept(self);
    const opFunc = {
      "==": (a, b) => a == b,
      "!=": (a, b) => a != b,
      "<": (a, b) => a < b,
      ">": (a, b) => a > b,
      ">=": (a, b) => a >= b,
      "<=": (a, b) => a <= b,
    }[ctx.op.text];
    const negExpression = ctx.neg.accept(self);

    return (engine) => {
      const result = opFunc(posExpression(engine), negExpression(engine));
      return result ? 1 : 0;
    };
  }

  visitConditionalExpression(ctx) {
    const self = this;

    const condition = ctx.cond.accept(self);
    const positive = ctx.pos.accept(self);
    const negative = ctx.neg.accept(self);

    return (state) => {
      if (condition(state) == 1) {
        return positive(state);
      } else {
        return negative(state);
      }
    };
  }

  buildAirthmeticExpression(ctx, op) {
    const self = this;

    const priorExpression = ctx.getChild(0).accept(self);
    const opFunc = {
      "+": (a, b) => a + b,
      "-": (a, b) => a - b,
      "*": (a, b) => a * b,
      "/": (a, b) => a / b,
      "^": (a, b) => Math.pow(a, b),
    }[op];
    const afterExpression = ctx.getChild(2).accept(self);

    return (engine) => {
      return opFunc(priorExpression(engine), afterExpression(engine));
    };
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

  buildStreamGetExpression(target, rescopeFuture, conversionMaybe) {
    const self = this;

    return (engine) => {
      const rescope = rescopeFuture === null ? null : rescopeFuture(engine);
      return engine.getStream(target, rescope, conversionMaybe).getValue();
    };
  }

  visitGetStream(ctx) {
    const self = this;
    return self.buildStreamGetExpression(ctx.target.getText(), null, null);
  }

  visitGetStreamIndirect(ctx) {
    const self = this;
    return self.buildStreamGetExpression(ctx.target.getText(), ctx.rescope.accept(self), null);
  }

  visitGetStreamConversion(ctx) {
    const self = this;
    return self.buildStreamGetExpression(ctx.target.getText(), null, ctx.conversion.accept(self));
  }

  visitGetStreamIndirectSubstanceAppUnits(ctx) {
    const self = this;
    return self.buildStreamGetExpression(
      ctx.target.getText(),
      ctx.rescope.accept(self),
      ctx.conversion.accept(self),
    );
  }

  visitLimitMinExpression(ctx) {
    const self = this;

    const operandFuture = ctx.operand.accept(self);
    const minimumFuture = ctx.limit.accept(self);

    return (engine) => {
      const operand = operandFuture(engine);
      const minimum = minimumFuture(engine);
      return operand < minimum ? minimum : operand;
    };
  }

  visitLimitMaxExpression(ctx) {
    const self = this;

    const operandFuture = ctx.operand.accept(self);
    const maximumFuture = ctx.limit.accept(self);

    return (engine) => {
      const operand = operandFuture(engine);
      const maximum = maximumFuture(engine);
      return operand > maximum ? maximum : operand;
    };
  }

  visitLimitBoundExpression(ctx) {
    const self = this;

    const operandFuture = ctx.operand.accept(self);
    const minimumFuture = ctx.lower.accept(self);
    const maximumFuture = ctx.upper.accept(self);

    return (engine) => {
      const operand = operandFuture(engine);
      const minimum = minimumFuture(engine);
      const maximum = maximumFuture(engine);
      if (operand < minimum) {
        return minimum;
      } else if (operand > maximum) {
        return maximum;
      } else {
        return operand;
      }
    };
  }

  visitParenExpression(ctx) {
    const self = this;
    return ctx.getChild(1).accept(self);
  }

  visitDrawNormalExpression(ctx) {
    const self = this;
    const meanFuture = ctx.mean.accept(self);
    const stdFuture = ctx.std.accept(self);

    return (engine) => {
      const meanValue = meanFuture(engine);
      const stdValue = stdFuture(engine);
      const generator = d3.randomNormal(meanValue, stdValue);
      return generator();
    };
  }

  visitDrawUniformExpression(ctx) {
    const self = this;
    const lowFuture = ctx.low.accept(self);
    const highFuture = ctx.high.accept(self);

    return (engine) => {
      const lowValue = lowFuture(engine);
      const highValue = highFuture(engine);
      const generator = d3.randomUniform(lowValue, highValue);
      return generator();
    };
  }

  visitSimpleIdentifier(ctx) {
    const self = this;
    const identifier = ctx.getChild(0).getText();

    return (engine) => {
      return engine.getVariable(identifier);
    };
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
    return {name: "about"};
  }

  visitDefaultStanza(ctx) {
    const self = this;
    const numApplications = ctx.getChildCount() - 4;

    const appChildren = [];
    for (let i = 0; i < numApplications; i++) {
      appChildren.push(ctx.getChild(i + 2));
    }

    const appCommands = appChildren.map((x) => x.accept(self));
    const execute = (engine) => {
      engine.setStanza("default");
      appCommands.forEach((command) => command(engine));
    };
    return {name: "default", executable: execute};
  }

  visitPolicyStanza(ctx) {
    const self = this;
    const policyName = self._getStringWithoutQuotes(ctx.name.getText());
    const numApplications = ctx.getChildCount() - 5;

    const appChildren = [];
    for (let i = 0; i < numApplications; i++) {
      appChildren.push(ctx.getChild(i + 3));
    }

    const appCommands = appChildren.map((x) => x.accept(self));
    const execute = (engine) => {
      engine.setStanza("policy " + policyName);
      appCommands.forEach((command) => command(engine));
    };
    return {name: "policy " + policyName, executable: execute};
  }

  visitSimulationsStanza(ctx) {
    const self = this;
    const numApplications = ctx.getChildCount() - 4;

    const appChildren = [];
    for (let i = 0; i < numApplications; i++) {
      appChildren.push(ctx.getChild(i + 2));
    }

    const simulations = appChildren.map((x) => x.accept(self));
    return {
      "name": "simulations",
      "executable": [(engine) => engine.setStanza("simulations")],
      "simulations": simulations,
    };
  }

  buildDef(ctx, scopeSetter) {
    const self = this;
    const name = self._getStringWithoutQuotes(ctx.name.getText());
    const numChildren = ctx.getChildCount() - 5;

    const children = [];
    for (let i = 0; i < numChildren; i++) {
      children.push(ctx.getChild(i + 3));
    }

    const commands = children.map((x) => x.accept(self));
    const execute = (engine) => {
      scopeSetter(engine, name);
      commands.forEach((command) => command(engine));
    };
    return execute;
  }

  visitApplicationDef(ctx) {
    const self = this;
    return self.buildDef(ctx, (engine, name) => engine.setApplication(name));
  }

  visitSubstanceDef(ctx) {
    const self = this;
    return self.buildDef(ctx, (engine, name) => engine.setSubstance(name, false));
  }

  visitApplicationMod(ctx) {
    const self = this;
    return self.buildDef(ctx, (engine, name) => engine.setApplication(name));
  }

  visitSubstanceMod(ctx) {
    const self = this;
    return self.buildDef(ctx, (engine, name) => engine.setSubstance(name, true));
  }

  buildStreamMod(callback, ctx, durationFuture) {
    const self = this;
    const streamName = ctx.target.getText();
    const valueFuture = ctx.value.accept(self);

    return (engine) => {
      const value = valueFuture(engine);
      const duration = durationFuture(engine);
      callback(engine, streamName, value, duration);
    };
  }

  buildCap(ctx, durationFuture, displace) {
    const self = this;
    const capType = ctx.getChild(0).getText();
    const strategy = {
      "cap": (engine, stream, value, duration) => engine.cap(stream, value, duration, displace),
      "floor": (engine, stream, value, duration) => engine.floor(stream, value, duration, displace),
    }[capType];
    return self.buildStreamMod(strategy, ctx, durationFuture);
  }

  visitLimitCommandAllYears(ctx) {
    const self = this;
    return self.buildCap(ctx, (engine) => null, null);
  }

  visitLimitCommandDisplacingAllYears(ctx) {
    const self = this;
    const displaceTarget = self._getStringWithoutQuotes(ctx.getChild(5).getText());
    return self.buildCap(ctx, (engine) => null, displaceTarget);
  }

  visitLimitCommandDuration(ctx) {
    const self = this;
    const durationFuture = ctx.duration.accept(self);
    return self.buildCap(ctx, durationFuture);
  }

  visitLimitCommandDisplacingDuration(ctx) {
    const self = this;
    const durationFuture = ctx.duration.accept(self);
    const displaceTarget = self._getStringWithoutQuotes(ctx.getChild(5).getText());
    return self.buildCap(ctx, durationFuture, displaceTarget);
  }

  buildChange(ctx, durationFuture) {
    const self = this;
    return self.buildStreamMod(
      (engine, stream, value, duration) => engine.changeStream(stream, value, duration),
      ctx,
      durationFuture,
    );
  }

  visitChangeAllYears(ctx) {
    const self = this;
    return self.buildChange(ctx, (engine) => null);
  }

  visitChangeDuration(ctx) {
    const self = this;
    const durationFuture = ctx.duration.accept(self);
    return self.buildChange(ctx, durationFuture);
  }

  visitDefineVarStatement(ctx) {
    const self = this;
    const identifier = ctx.target.getText();
    const valueFuture = ctx.value.accept(self);

    return (engine) => {
      const value = valueFuture(engine);
      engine.defineVariable(identifier);
      engine.setVariable(identifier, value);
    };
  }

  visitInitialChargeAllYears(ctx) {
    const self = this;
    const stream = ctx.target.getText();
    const valueFuture = ctx.value.accept(self);

    return (engine) => {
      const value = valueFuture(engine);
      return engine.setInitialCharge(value, stream, null);
    };
  }

  visitInitialChargeDuration(ctx) {
    const self = this;
    const stream = ctx.target.getText();
    const valueFuture = ctx.value.accept(self);
    const durationFuture = ctx.duration.accept(self);

    return (engine) => {
      const value = valueFuture(engine);
      const duration = durationFuture(engine);
      return engine.setInitialCharge(value, stream, duration);
    };
  }

  visitRechargeAllYears(ctx) {
    const self = this;
    const populationFuture = ctx.population.accept(self);
    const volumeFuture = ctx.volume.accept(self);

    return (engine) => {
      const population = populationFuture(engine);
      const volume = volumeFuture(engine);
      return engine.recharge(population, volume, null);
    };
  }

  visitRechargeDuration(ctx) {
    const self = this;
    const populationFuture = ctx.population.accept(self);
    const volumeFuture = ctx.volume.accept(self);
    const durationFuture = ctx.duration.accept(self);

    return (engine) => {
      const population = populationFuture(engine);
      const volume = volumeFuture(engine);
      const duration = durationFuture(engine);
      return engine.recharge(population, volume, duration);
    };
  }

  buildRecover(ctx, displacementFuture, durationFuture) {
    const self = this;
    const volumeFuture = ctx.volume.accept(self);
    const yieldFuture = ctx.yieldVal.accept(self);

    return (engine) => {
      const volume = volumeFuture(engine);
      const yieldValue = yieldFuture(engine);
      const displacement = displacementFuture(engine);
      const duration = durationFuture(engine);
      engine.recycle(volume, yieldValue, displacement, duration);
    };
  }

  visitRecoverAllYears(ctx) {
    const self = this;
    return self.buildRecover(ctx, (engine) => null, (engine) => null);
  }

  visitRecoverDuration(ctx) {
    const self = this;
    const durationFuture = ctx.duration.accept(self);
    return self.buildRecover(ctx, (engine) => null, durationFuture);
  }

  visitRecoverDisplacementAllYears(ctx) {
    const self = this;
    const displacementFuture = ctx.displacement.accept(self);
    return self.buildRecover(ctx, displacementFuture, (engine) => null);
  }

  visitRecoverDisplacementDuration(ctx) {
    const self = this;
    const displacementFuture = ctx.displacement.accept(self);
    const durationFuture = ctx.duration.accept(self);
    return self.buildRecover(ctx, displacementFuture, durationFuture);
  }

  visitReplaceAllYears(ctx) {
    const self = this;
    const volumeFuture = ctx.volume.accept(self);
    const stream = ctx.target.getText();
    const destination = self._getStringWithoutQuotes(ctx.destination.getText());

    return (engine) => {
      const volume = volumeFuture(engine);
      engine.replace(volume, stream, destination, null);
    };
  }

  visitReplaceDuration(ctx) {
    const self = this;
    const volumeFuture = ctx.volume.accept(self);
    const durationFuture = ctx.duration.accept(self);
    const stream = ctx.target.getText();
    const destination = self._getStringWithoutQuotes(ctx.destination.getText());

    return (engine) => {
      const volume = volumeFuture(engine);
      const duration = durationFuture(engine);
      engine.replace(volume, stream, destination, duration);
    };
  }

  visitRetireAllYears(ctx) {
    const self = this;
    const volumeFuture = ctx.volume.accept(self);

    return (engine) => {
      const volume = volumeFuture(engine);
      return engine.retire(volume, null);
    };
  }

  visitRetireDuration(ctx) {
    const self = this;
    const volumeFuture = ctx.volume.accept(self);
    const durationFuture = ctx.duration.accept(self);

    return (engine) => {
      const volume = volumeFuture(engine);
      const duration = durationFuture(engine);
      return engine.retire(volume, duration);
    };
  }

  visitSetAllYears(ctx) {
    const self = this;
    const target = ctx.target.getText();
    const valueFuture = ctx.value.accept(self);

    return (engine) => {
      const value = valueFuture(engine);
      engine.setStream(target, value, null);
    };
  }

  visitSetDuration(ctx) {
    const self = this;
    const target = ctx.target.getText();
    const valueFuture = ctx.value.accept(self);
    const durationFuture = ctx.duration.accept(self);

    return (engine) => {
      const value = valueFuture(engine);
      const duration = durationFuture(engine);
      engine.setStream(target, value, duration);
    };
  }

  visitEqualsAllYears(ctx) {
    const self = this;
    const valueFuture = ctx.value.accept(self);

    return (engine) => {
      const value = valueFuture(engine);
      engine.equals(value);
    };
  }

  visitEqualsDuration(ctx) {
    const self = this;
    const valueFuture = ctx.value.accept(self);
    const durationFuture = ctx.duration.accept(self);

    return (engine) => {
      const value = valueFuture(engine);
      const duration = durationFuture(engine);
      engine.equals(value, duration);
    };
  }

  buildSimulate(ctx, stanzas, futureNumTrials) {
    const self = this;
    const name = self._getStringWithoutQuotes(ctx.name.getText());
    const startFuture = ctx.start.accept(self);
    const endFuture = ctx.end.accept(self);

    return (engine) => {
      const start = startFuture(engine);
      const end = endFuture(engine);
      const numTrials = futureNumTrials(engine);
      return {name: name, stanzas: stanzas, trials: numTrials, start: start, end: end};
    };
  }

  visitBaseSimulation(ctx) {
    const self = this;
    return self.buildSimulate(ctx, ["default"], (x) => 1);
  }

  visitPolicySim(ctx) {
    const self = this;
    const numPolicies = Math.ceil((ctx.getChildCount() - 8) / 2);

    const policies = ["default"];
    for (let i = 0; i < numPolicies; i++) {
      const rawName = ctx.getChild(i * 2 + 3).getText();
      const nameNoQuotes = self._getStringWithoutQuotes(rawName);
      const nameComplete = "policy " + nameNoQuotes;
      policies.push(nameComplete);
    }

    return self.buildSimulate(ctx, policies, (x) => 1);
  }

  visitBaseSimulationTrials(ctx) {
    const self = this;
    const futureNumTrials = ctx.trials.accept(self);
    return self.buildSimulate(ctx, ["default"], futureNumTrials);
  }

  visitPolicySimTrials(ctx) {
    const self = this;
    const numPolicies = Math.ceil((ctx.getChildCount() - 11) / 2);
    const futureNumTrials = ctx.trials.accept(self);

    const policies = [];
    for (let i = 0; i < numPolicies; i++) {
      policies.push(ctx.getChild(i * 2 + 3).accept(self));
    }

    return self.buildSimulate(ctx, policies, futureNumTrials);
  }

  visitProgram(ctx) {
    const self = this;

    const stanzasByName = new Map();
    const numStanzas = ctx.getChildCount();

    for (let i = 0; i < numStanzas; i++) {
      const newStanza = ctx.getChild(i).accept(self);
      stanzasByName.set(newStanza.name, newStanza);
    }

    const execute = () => {
      if (!stanzasByName.has("simulations")) {
        throw "Need at least one simulation scenario.";
      }

      const simulationsStanza = stanzasByName.get("simulations");
      const simulationExecutables = simulationsStanza["executable"];
      const simulationFutures = simulationsStanza["simulations"];

      const bootstrapEngine = new Engine(1, 1);
      simulationExecutables.forEach((x) => x(bootstrapEngine));
      const simulations = simulationFutures.map((simulationFuture) => {
        return simulationFuture(bootstrapEngine);
      });
      const results = simulations.map((simulation) => {
        const runSimulation = () => {
          const engine = new Engine(simulation.start, simulation.end);

          const runYear = () => {
            const stanzas = simulation.stanzas;
            stanzas.forEach((stanzaName) => {
              if (!stanzasByName.get(stanzaName)) {
                throw "Could not find " + stanzaName;
              }
              const stanzaDetails = stanzasByName.get(stanzaName);
              const stanzaExecutable = stanzaDetails.executable;
              stanzaExecutable(engine);
            });
            return engine.getResults();
          };

          const yearResults = [];
          while (!engine.getIsDone()) {
            yearResults.push(runYear());
            engine.incrementYear();
          }

          return yearResults;
        };

        const trialResults = [];
        for (let i = 0; i < simulation.trials; i++) {
          trialResults.push(runSimulation());
        }

        return new SimulationResult(simulation.name, trialResults);
      });
      return results;
    };

    return execute;
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
    if (target.startsWith("\"") && target.endsWith("\"")) {
      return target.substring(1, target.length - 1);
    } else {
      return target;
    }
  }
}


class SimulationResult {
  constructor(name, trialResults) {
    const self = this;
    self._name = name;
    self._trialResults = trialResults;
  }

  getName() {
    const self = this;
    return self._name;
  }

  getTrialResults() {
    const self = this;
    return self._trialResults;
  }
}


/**
 * Structure contianing the result of attempting to compile a QubecTalk script.
 */
class CompileResult {
  /**
   * Create a new record of a compilation attempt.
   *
   * @param program The compiled program as a lambda if successful or null if unsuccessful.
   * @param errors Any errors enountered or empty list if no errors.
   */
  constructor(program, errors) {
    const self = this;
    self._program = program;
    self._errors = errors;
  }

  /**
   * Get the program as a lambda.
   *
   * @returns The compiled program as a lambda or null if compilation failed.
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


class Compiler {
  compile(input) {
    const self = this;

    if (input.replaceAll("\n", "").replaceAll(" ", "") === "") {
      return new CompileResult(null, []);
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
      return new CompileResult(null, errors);
    }

    const program = programUncompiled.accept(new CompileVisitor());
    if (errors.length > 0) {
      return new CompileResult(null, errors);
    }

    return new CompileResult(program, errors);
  }
}


export {Compiler};
