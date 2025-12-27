/**
 * Compiler and visitor for translating QubecTalk scripts.
 *
 * Provides the main compilation infrastructure for converting text-based
 * QubecTalk scripts into object representations. Uses ANTLR visitor pattern
 * to traverse the parse tree.
 *
 * @license BSD, see LICENSE.md
 */

import {EngineNumber} from "engine_number";
import {YearMatcher, ParsedYear} from "duration";
import {parseUnitValue} from "meta_serialization";
import {NumberParseUtil} from "number_parse_util";
import {
  formatEngineNumber,
  indentSingle,
  indent,
  buildAddCode,
  finalizeCodePieces,
  preprocessEachYearSyntax,
} from "ui_translator_util";
import {
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
} from "ui_translator_components";

const toolkit = QubecTalk.getToolkit();

/**
 * Visitor compiling a QubecTalk program to JS objects describing the analysis.
 *
 * Visitor which attempts to compile a QubecTalk program to JS objects
 * describing the anlaysis or indication that the anlaysis cannot use the
 * simplified JS object format.
 */
class TranslatorVisitor extends toolkit.QubecTalkVisitor {
  /**
   * Create a new TranslatorVisitor with number parsing utilities.
   */
  constructor() {
    super();
    this.numberParser = new NumberParseUtil();
  }

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

    const result = self.numberParser.parseFlexibleNumber(bodyRawText);
    if (!result.isSuccess()) {
      throw new Error(`Failed to parse number in QubecTalk expression: ${result.getError()}`);
    }

    // Return an object with both the numeric value and original string
    // Use the full raw text which already includes the sign if present
    return {
      value: signMultiplier * result.getNumber(),
      originalString: raw,
    };
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
   * Visit a unit node and formats it by delegating to the appropriate child visitor.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The formatted unit string.
   */
  visitUnit(ctx) {
    const self = this;
    return ctx.getChild(0).accept(self);
  }

  /**
   * Visit a volume unit node and returns its text.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The volume unit string (kg, mt, etc.).
   */
  visitVolumeUnit(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a temporal unit node and returns its text.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The temporal unit string (year, years, etc.).
   */
  visitTemporalUnit(ctx) {
    const self = this;
    return ctx.getText();
  }

  /**
   * Visit a relative unit node and formats it with proper spacing.
   *
   * Handles percent variants: %, % prior year, % current year, % current.
   * ANTLR's getText() concatenates tokens without spaces, so we reconstruct
   * the proper format here.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The properly formatted relative unit string.
   */
  visitRelativeUnit(ctx) {
    const self = this;
    const childCount = ctx.getChildCount();

    if (childCount === 1) {
      // Just "%"
      return "%";
    } else if (childCount === 2) {
      // "% current"
      return "% current";
    } else if (childCount === 3) {
      // "% prior year" or "% current year"
      const secondToken = ctx.getChild(1).getText();
      return "% " + secondToken + " year";
    }

    // Fallback to getText() for unknown cases
    return ctx.getText();
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
      return ctx.getChild(0).accept(self);
    } else {
      const numerator = ctx.getChild(0).accept(self);
      const denominator = ctx.getChild(2).accept(self);

      // Always use "/" format for consistency
      return numerator + " / " + denominator;
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

    // Handle the case where expressionContent might be an object with value and originalString
    if (typeof expressionContent === "object" && expressionContent.value !== undefined) {
      return new EngineNumber(
        expressionContent.value,
        unitString,
        self._coerceToString(expressionContent),
      );
    } else {
      return new EngineNumber(expressionContent, unitString);
    }
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

    const posExpression = self._coerceToString(ctx.pos.accept(self));
    const opFunc = ctx.op.text;
    const negExpression = self._coerceToString(ctx.neg.accept(self));

    return posExpression + " " + opFunc + " " + negExpression;
  }

  /**
   * Visit a logical expression node and format it.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The formatted logical expression.
   */
  visitLogicalExpression(ctx) {
    const self = this;

    const leftExpression = self._coerceToString(ctx.left.accept(self));
    const opFunc = ctx.op.text;
    const rightExpression = self._coerceToString(ctx.right.accept(self));

    return leftExpression + " " + opFunc + " " + rightExpression;
  }

  /**
   * Visit a conditional expression node and format it.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The formatted conditional expression.
   */
  visitConditionalExpression(ctx) {
    const self = this;

    const condition = self._coerceToString(ctx.cond.accept(self));
    const positive = self._coerceToString(ctx.pos.accept(self));
    const negative = self._coerceToString(ctx.neg.accept(self));

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

    const priorExpression = self._coerceToString(ctx.getChild(0).accept(self));
    const afterExpression = self._coerceToString(ctx.getChild(2).accept(self));

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
    return "get " + ctx.getChild(1).getText();
  }

  /**
   * Visit an indirect stream access expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The indirect stream access text.
   */
  visitGetStreamIndirect(ctx) {
    const self = this;
    return "get " + ctx.getChild(1).getText() + " of " + ctx.getChild(3).getText();
  }

  /**
   * Visit a stream conversion expression node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The stream conversion text.
   */
  visitGetStreamConversion(ctx) {
    const self = this;
    return "get " + ctx.getChild(1).getText() + " as " + ctx.getChild(3).getText();
  }

  /**
   * Visit a substance/application units stream access node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {string} The stream access text.
   */
  visitGetStreamIndirectSubstanceAppUnits(ctx) {
    const self = this;
    const part1 = "get " + ctx.getChild(1).getText() + " of " + ctx.getChild(3).getText();
    const part2 = " as " + ctx.getChild(5).getText();
    return part1 + part2;
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
    return "(" + self._coerceToString(ctx.getChild(1).accept(self)) + ")";
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
   * @param {ParsedYear|null} minYear - Start year or null for unbounded
   * @param {ParsedYear|null} maxYear - End year or null for unbounded
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
    const yearObj = ctx.target.accept(self);
    const parsedYear = new ParsedYear(yearObj.value, yearObj.originalString);
    return self.buildDuring(parsedYear, parsedYear);
  }

  /**
   * Visit a start year duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {YearMatcher} Year matcher starting from engine start.
   */
  visitDuringStart(ctx) {
    const self = this;
    const startYear = new ParsedYear("beginning");
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
    const lowerObj = ctx.lower.accept(self);
    const upperObj = ctx.upper.accept(self);
    const parsedLower = new ParsedYear(lowerObj.value, lowerObj.originalString);
    const parsedUpper = new ParsedYear(upperObj.value, upperObj.originalString);
    return self.buildDuring(parsedLower, parsedUpper);
  }

  /**
   * Visit a minimum year duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {YearMatcher} Year matcher with min bound only.
   */
  visitDuringWithMin(ctx) {
    const self = this;
    const lowerObj = ctx.lower.accept(self);
    const parsedLower = new ParsedYear(lowerObj.value, lowerObj.originalString);
    return self.buildDuring(parsedLower, null);
  }

  /**
   * Visit a maximum year duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {YearMatcher} Year matcher with max bound only.
   */
  visitDuringWithMax(ctx) {
    const self = this;
    const upperObj = ctx.upper.accept(self);
    const parsedUpper = new ParsedYear(upperObj.value, upperObj.originalString);
    return self.buildDuring(null, parsedUpper);
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
   * @returns {RechargeCommand} New recharge command instance.
   */
  visitRechargeAllYears(ctx) {
    const self = this;
    const population = ctx.population.accept(self);
    const volume = ctx.volume.accept(self);
    return new RechargeCommand(population, volume, null);
  }

  /**
   * Visit a recharge command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RechargeCommand} New recharge command instance.
   */
  visitRechargeDuration(ctx) {
    const self = this;
    const population = ctx.population.accept(self);
    const volume = ctx.volume.accept(self);
    const duration = ctx.duration.accept(self);
    return new RechargeCommand(population, volume, duration);
  }

  /**
   * Visit a recover command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New recover command instance.
   */
  visitRecoverAllYears(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    return new RecycleCommand(volume, yieldVal, null, "recharge");
  }

  /**
   * Visit a recover command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} New recover command instance.
   */
  visitRecoverDuration(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const duration = ctx.duration.accept(self);
    return new RecycleCommand(volume, yieldVal, duration, "recharge");
  }


  /**
   * Visit a recover command with stage and all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with stage.
   */
  visitRecoverStageAllYears(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const stage = ctx.stage.text;
    return new RecycleCommand(volume, yieldVal, null, stage);
  }

  /**
   * Visit a recover command with stage and duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with stage and duration.
   */
  visitRecoverStageDuration(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const stage = ctx.stage.text;
    const duration = ctx.duration.accept(self);
    return new RecycleCommand(volume, yieldVal, duration, stage);
  }

  /**
   * Visit a recover command with explicit induction and all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with induction.
   */
  visitRecoverInductionAllYears(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const inductionVal = ctx.inductionVal.accept(self);
    return new RecycleCommand(volume, yieldVal, null, "recharge", inductionVal);
  }

  /**
   * Visit a recover command with explicit induction and duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with induction and duration.
   */
  visitRecoverInductionDuration(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const inductionVal = ctx.inductionVal.accept(self);
    const duration = ctx.duration.accept(self);
    return new RecycleCommand(volume, yieldVal, duration, "recharge", inductionVal);
  }

  /**
   * Visit a recover command with explicit induction, stage and all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with induction and stage.
   */
  visitRecoverInductionStageAllYears(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const inductionVal = ctx.inductionVal.accept(self);
    const stage = ctx.stage.text;
    return new RecycleCommand(volume, yieldVal, null, stage, inductionVal);
  }

  /**
   * Visit a recover command with explicit induction, stage and duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with induction, stage and duration.
   */
  visitRecoverInductionStageDuration(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const inductionVal = ctx.inductionVal.accept(self);
    const stage = ctx.stage.text;
    const duration = ctx.duration.accept(self);
    return new RecycleCommand(volume, yieldVal, duration, stage, inductionVal);
  }

  /**
   * Visit a recover command with default induction and all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with default induction.
   */
  visitRecoverDefaultInductionAllYears(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    return new RecycleCommand(volume, yieldVal, null, "recharge", "default");
  }

  /**
   * Visit a recover command with default induction and duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with default induction and duration.
   */
  visitRecoverDefaultInductionDuration(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const duration = ctx.duration.accept(self);
    return new RecycleCommand(volume, yieldVal, duration, "recharge", "default");
  }

  /**
   * Visit a recover command with default induction, stage and all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with default induction and stage.
   */
  visitRecoverDefaultInductionStageAllYears(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const stage = ctx.stage.text;
    return new RecycleCommand(volume, yieldVal, null, stage, "default");
  }

  /**
   * Visit a recover command with default induction, stage and duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RecycleCommand} New recycle command with default induction, stage and duration.
   */
  visitRecoverDefaultInductionStageDuration(ctx) {
    const self = this;
    const volume = ctx.volume.accept(self);
    const yieldVal = ctx.yieldVal.accept(self);
    const stage = ctx.stage.text;
    const duration = ctx.duration.accept(self);
    return new RecycleCommand(volume, yieldVal, duration, stage, "default");
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
   * @returns {RetireCommand} New retire command instance.
   */
  visitRetireAllYears(ctx) {
    const self = this;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const value = volumeFuture(ctx);

    // Check if "with replacement" is present in the parsed context
    const withReplacement = ctx.getText().toLowerCase().includes("withreplacement");

    // Create retire-specific command
    return new RetireCommand(value, null, withReplacement);
  }

  /**
   * Visit a retire command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {RetireCommand} New retire command instance.
   */
  visitRetireDuration(ctx) {
    const self = this;
    const volumeFuture = (ctx) => ctx.volume.accept(self);
    const value = volumeFuture(ctx);
    const duration = ctx.duration.accept(self);

    // Check if "with replacement" is present in the parsed context
    const withReplacement = ctx.getText().toLowerCase().includes("withreplacement");

    // Create retire-specific command with duration
    return new RetireCommand(value, duration, withReplacement);
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
   * Visit an enable command with all years duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} Enable command.
   */
  visitEnableAllYears(ctx) {
    const self = this;
    const target = ctx.target.getText();
    return new Command("enable", target, null, null);
  }

  /**
   * Visit an enable command with duration node.
   *
   * @param {Object} ctx - The parse tree node context.
   * @returns {Command} Enable command.
   */
  visitEnableDuration(ctx) {
    const self = this;
    const target = ctx.target.getText();
    const duration = ctx.duration.accept(self);
    return new Command("enable", target, null, duration);
  }

  /**
   * Visit an assume no command for all years.
   *
   * @param {Object} ctx - Parse tree context.
   * @returns {AssumeCommand} Assume command with mode, stream, and duration.
   */
  visitAssumeNoAllYears(ctx) {
    const self = this;
    const stream = ctx.target.getText();
    return new AssumeCommand("no", stream, null);
  }

  /**
   * Visit an assume no command with duration.
   *
   * @param {Object} ctx - Parse tree context.
   * @returns {AssumeCommand} Assume command with mode, stream, and duration.
   */
  visitAssumeNoDuration(ctx) {
    const self = this;
    const stream = ctx.target.getText();
    const duration = ctx.duration.accept(self);
    return new AssumeCommand("no", stream, duration);
  }

  /**
   * Visit an assume only recharge command for all years.
   *
   * @param {Object} ctx - Parse tree context.
   * @returns {AssumeCommand} Assume command with mode, stream, and duration.
   */
  visitAssumeOnlyRechargeAllYears(ctx) {
    const self = this;
    const stream = ctx.target.getText();
    return new AssumeCommand("only recharge", stream, null);
  }

  /**
   * Visit an assume only recharge command with duration.
   *
   * @param {Object} ctx - Parse tree context.
   * @returns {AssumeCommand} Assume command with mode, stream, and duration.
   */
  visitAssumeOnlyRechargeDuration(ctx) {
    const self = this;
    const stream = ctx.target.getText();
    const duration = ctx.duration.accept(self);
    return new AssumeCommand("only recharge", stream, duration);
  }

  /**
   * Visit an assume continued command for all years.
   *
   * @param {Object} ctx - Parse tree context.
   * @returns {AssumeCommand} Assume command with mode, stream, and duration.
   */
  visitAssumeContinuedAllYears(ctx) {
    const self = this;
    const stream = ctx.target.getText();
    return new AssumeCommand("continued", stream, null);
  }

  /**
   * Visit an assume continued command with duration.
   *
   * @param {Object} ctx - Parse tree context.
   * @returns {AssumeCommand} Assume command with mode, stream, and duration.
   */
  visitAssumeContinuedDuration(ctx) {
    const self = this;
    const stream = ctx.target.getText();
    const duration = ctx.duration.accept(self);
    return new AssumeCommand("continued", stream, duration);
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

    // Check assume compatibility before building
    const assumeCommands = commands
      .filter((x) => x && x.getTypeName && x.getTypeName() === "assume");

    if (assumeCommands.length > 1) {
      // Multiple assumes not supported in UI
      builder.setIsCompatible(false);
    } else if (assumeCommands.length === 1) {
      const assume = assumeCommands[0];

      // Check if compatible with UI: single assume, for sales, no duration
      if (assume.getStream() !== "sales" || assume.getDuration() !== null) {
        builder.setIsCompatible(false);
      }
    }

    const isCompatibleRaw = commands.map((x) => x.getIsCompatible()).reduce((a, b) => a && b, true);

    const substance = builder.build(isCompatibleRaw);

    return substance;
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

  /**
   * Check if we got a complex back and take it to a string.
   *
   * Some operations return a complex object but some operations throw away that information to
   * return to original string. This coerces to string.
   *
   * @param {Object} target - The value to coerce down to string.
   * @returns {String} Forced to string.
   */
  _coerceToString(target) {
    const self = this;
    if (target.originalString === undefined) {
      return target;
    } else {
      return target.originalString;
    }
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

    const preprocessedCode = preprocessEachYearSyntax(input);
    const chars = new toolkit.antlr4.InputStream(preprocessedCode);
    const lexer = new toolkit.QubecTalkLexer(chars);
    lexer.removeErrorListeners();
    lexer.addErrorListener({
      syntaxError: (recognizer, offendingSymbol, line, column, msg, err) => {
        const result = `(line ${line}, col ${column}): ${msg}`;
        errors.push(result);
      },
      reportAmbiguity: (recognizer, dfa, startIndex, stopIndex, exact, ambigAlts, configs) => {
        errors.push(`Ambiguity detected at position ${startIndex}-${stopIndex}`);
      },
      reportAttemptingFullContext: (recognizer, dfa, startIndex, stopIndex,
        conflictingAlts, configs) => {
        errors.push(`Attempting full context at position ${startIndex}-${stopIndex}`);
      },
      reportContextSensitivity: (recognizer, dfa, startIndex, stopIndex, prediction, configs) => {
        errors.push(`Context sensitivity at position ${startIndex}-${stopIndex}`);
      },
    });

    const tokens = new toolkit.antlr4.CommonTokenStream(lexer);
    const parser = new toolkit.QubecTalkParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener({
      syntaxError: (recognizer, offendingSymbol, line, column, msg, err) => {
        const result = `(line ${line}, col ${column}): ${msg}`;
        errors.push(result);
      },
      reportAmbiguity: (recognizer, dfa, startIndex, stopIndex, exact, ambigAlts, configs) => {
        // Performance warning about ambiguous grammar, not an error
      },
      reportAttemptingFullContext: (recognizer, dfa, startIndex, stopIndex,
        conflictingAlts, configs) => {
        // Performance warning, not an error
      },
      reportContextSensitivity: (recognizer, dfa, startIndex, stopIndex, prediction, configs) => {
        // Performance warning, not an error
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
  TranslatorVisitor,
  UiTranslatorCompiler,
};
