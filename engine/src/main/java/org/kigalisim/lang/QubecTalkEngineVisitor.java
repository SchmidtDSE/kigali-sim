/**
 * Visitor for running QubecTalk code within the JVM.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.fragment.AboutStanzaFragment;
import org.kigalisim.lang.fragment.ApplicationFragment;
import org.kigalisim.lang.fragment.DuringFragment;
import org.kigalisim.lang.fragment.Fragment;
import org.kigalisim.lang.fragment.OperationFragment;
import org.kigalisim.lang.fragment.PolicyFragment;
import org.kigalisim.lang.fragment.ProgramFragment;
import org.kigalisim.lang.fragment.ScenarioFragment;
import org.kigalisim.lang.fragment.ScenariosFragment;
import org.kigalisim.lang.fragment.StringFragment;
import org.kigalisim.lang.fragment.SubstanceFragment;
import org.kigalisim.lang.fragment.UnitFragment;
import org.kigalisim.lang.localization.FlexibleNumberParseResult;
import org.kigalisim.lang.localization.NumberParseUtil;
import org.kigalisim.lang.operation.AdditionOperation;
import org.kigalisim.lang.operation.CapOperation;
import org.kigalisim.lang.operation.ChangeOperation;
import org.kigalisim.lang.operation.ChangeUnitsOperation;
import org.kigalisim.lang.operation.ConditionalOperation;
import org.kigalisim.lang.operation.DefineVariableOperation;
import org.kigalisim.lang.operation.DivisionOperation;
import org.kigalisim.lang.operation.DrawNormalOperation;
import org.kigalisim.lang.operation.DrawUniformOperation;
import org.kigalisim.lang.operation.EnableOperation;
import org.kigalisim.lang.operation.EqualityOperation;
import org.kigalisim.lang.operation.EqualsOperation;
import org.kigalisim.lang.operation.FloorOperation;
import org.kigalisim.lang.operation.GetStreamOperation;
import org.kigalisim.lang.operation.GetVariableOperation;
import org.kigalisim.lang.operation.InitialChargeOperation;
import org.kigalisim.lang.operation.JointOperation;
import org.kigalisim.lang.operation.LogicalOperation;
import org.kigalisim.lang.operation.MultiplicationOperation;
import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.operation.PowerOperation;
import org.kigalisim.lang.operation.PreCalculatedOperation;
import org.kigalisim.lang.operation.RechargeOperation;
import org.kigalisim.lang.operation.RecoverOperation;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;
import org.kigalisim.lang.operation.RemoveUnitsOperation;
import org.kigalisim.lang.operation.ReplaceOperation;
import org.kigalisim.lang.operation.RetireOperation;
import org.kigalisim.lang.operation.RetireWithReplacementOperation;
import org.kigalisim.lang.operation.SetOperation;
import org.kigalisim.lang.operation.SubtractionOperation;
import org.kigalisim.lang.program.ParsedApplication;
import org.kigalisim.lang.program.ParsedPolicy;
import org.kigalisim.lang.program.ParsedProgram;
import org.kigalisim.lang.program.ParsedScenario;
import org.kigalisim.lang.program.ParsedScenarios;
import org.kigalisim.lang.program.ParsedSubstance;
import org.kigalisim.lang.time.CalculatedTimePointFuture;
import org.kigalisim.lang.time.DynamicCapFuture;
import org.kigalisim.lang.time.ParsedDuring;
import org.kigalisim.lang.time.TimePointFuture;


/**
 * Visitor which interprets QubecTalk parsed code into Commands through Fragments.
 *
 * <p>Visitor which takes the parse tree of a QubecTalk program and converts it to a series of
 * Fragments which are used to build up Commands which actually execute operations against a Kigali
 * Sim Engine.</p>
 */
@SuppressWarnings("CheckReturnValue")
public class QubecTalkEngineVisitor extends QubecTalkBaseVisitor<Fragment> {

  /** Number parser for handling flexible thousands and decimal separators. */
  private final NumberParseUtil numberParser = new NumberParseUtil();

  /** Constant for "beginning" dynamic cap marker. */
  private static final String BEGINNING = "beginning";

  /** Constant for "onwards" dynamic cap marker. */
  private static final String ONWARDS = "onwards";

  /** Child index offset for substance body start. */
  private static final int SUBSTANCE_BODY_START = 3;

  /** Child index offset for substance body end. */
  private static final int SUBSTANCE_BODY_END = 2;

  /**
   * Constructs a new QubecTalkEngineVisitor.
   */
  public QubecTalkEngineVisitor() {
  }

  /**
   * {@inheritDoc}
   */
  @Override public Fragment visitNumber(QubecTalkParser.NumberContext ctx) {
    String rawText = ctx.getText();
    FlexibleNumberParseResult parseResult = numberParser.parseFlexibleNumber(rawText);
    if (parseResult.isError()) {
      throw new RuntimeException("Failed to parse number in QubecTalk expression: " + parseResult.getError().get());
    }
    BigDecimal numberRaw = parseResult.getParsedNumber().get();
    EngineNumber number = new EngineNumber(numberRaw, "");
    Operation calculation = new PreCalculatedOperation(number);
    return new OperationFragment(calculation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitString(QubecTalkParser.StringContext ctx) {
    String text = ctx.getText().replaceAll("\"", "");
    return new StringFragment(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override public Fragment visitUnitValue(QubecTalkParser.UnitValueContext ctx) {
    Operation futureCalculation = visit(ctx.expression()).getOperation();
    String unit = visit(ctx.unitOrRatio()).getUnit();
    Operation calculation = new ChangeUnitsOperation(futureCalculation, unit);
    return new OperationFragment(calculation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitUnitOrRatio(QubecTalkParser.UnitOrRatioContext ctx) {
    String unit = ctx.getText();

    unit = unit.replaceAll(" each ", " / ");

    return new UnitFragment(unit);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitConditionExpression(QubecTalkParser.ConditionExpressionContext ctx) {
    Operation left = visit(ctx.pos).getOperation();
    Operation right = visit(ctx.neg).getOperation();

    String operatorStr = ctx.op.getText();
    Operation operation = new EqualityOperation(left, right, operatorStr);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitAdditionExpression(QubecTalkParser.AdditionExpressionContext ctx) {
    Operation left = visit(ctx.expression(0)).getOperation();
    Operation right = visit(ctx.expression(1)).getOperation();

    String operatorStr = ctx.op.getText();
    Operation calculation = switch (operatorStr) {
      case "+" -> new AdditionOperation(left, right);
      case "-" -> new SubtractionOperation(left, right);
      default -> throw new RuntimeException("Unknown addition operation");
    };
    return new OperationFragment(calculation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitPowExpression(QubecTalkParser.PowExpressionContext ctx) {
    Operation left = visit(ctx.expression(0)).getOperation();
    Operation right = visit(ctx.expression(1)).getOperation();
    return new OperationFragment(new PowerOperation(left, right));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitConditionalExpression(QubecTalkParser.ConditionalExpressionContext ctx) {
    Operation condition = visit(ctx.cond).getOperation();
    Operation trueCase = visit(ctx.pos).getOperation();
    Operation falseCase = visit(ctx.neg).getOperation();

    Operation operation = new ConditionalOperation(condition, trueCase, falseCase);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitGetStreamConversion(QubecTalkParser.GetStreamConversionContext ctx) {
    String streamName = visit(ctx.target).getString();

    UnitFragment unitFragment = (UnitFragment) visit(ctx.conversion);
    String unitConversion = unitFragment.getUnit();

    Operation operation = new JointOperation(
        new GetStreamOperation(streamName, unitConversion),
        new RemoveUnitsOperation()
    );

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitMinExpression(QubecTalkParser.LimitMinExpressionContext ctx) {
    return visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitGetStreamIndirectConversion(
      QubecTalkParser.GetStreamIndirectConversionContext ctx) {
    String streamName = visit(ctx.target).getString();

    String targetSubstance = visit(ctx.rescope).getString();

    UnitFragment unitFragment = (UnitFragment) visit(ctx.conversion);
    String unitConversion = unitFragment.getUnit();

    Operation operation = new JointOperation(
        new GetStreamOperation(streamName, targetSubstance, unitConversion),
        new RemoveUnitsOperation()
    );

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitMaxExpression(QubecTalkParser.LimitMaxExpressionContext ctx) {
    return visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitMultiplyExpression(QubecTalkParser.MultiplyExpressionContext ctx) {
    Operation left = visit(ctx.expression(0)).getOperation();
    Operation right = visit(ctx.expression(1)).getOperation();

    String operatorStr = ctx.op.getText();
    Operation calculation = switch (operatorStr) {
      case "*" -> new MultiplicationOperation(left, right);
      case "/" -> new DivisionOperation(left, right);
      default -> throw new RuntimeException("Unknown multiplication operation");
    };
    return new OperationFragment(calculation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDrawNormalExpression(QubecTalkParser.DrawNormalExpressionContext ctx) {
    Fragment meanFragment = visit(ctx.mean);
    Operation mean = meanFragment.getOperation();

    Fragment stdFragment = visit(ctx.std);
    Operation std = stdFragment.getOperation();

    Operation operation = new DrawNormalOperation(mean, std);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLogicalExpression(QubecTalkParser.LogicalExpressionContext ctx) {
    Fragment leftFragment = visit(ctx.left);
    Operation left = leftFragment.getOperation();

    Fragment rightFragment = visit(ctx.right);
    Operation right = rightFragment.getOperation();

    String operatorStr = ctx.op.getText();
    Operation operation = new LogicalOperation(left, right, operatorStr);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitGetStreamIndirect(QubecTalkParser.GetStreamIndirectContext ctx) {
    String streamName = visit(ctx.target).getString();

    String targetSubstance = visit(ctx.rescope).getString();

    Operation operation = new JointOperation(
        new GetStreamOperation(streamName, targetSubstance, null),
        new RemoveUnitsOperation()
    );

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDrawUniformExpression(QubecTalkParser.DrawUniformExpressionContext ctx) {
    Fragment lowFragment = visit(ctx.low);
    Operation low = lowFragment.getOperation();

    Fragment highFragment = visit(ctx.high);
    Operation high = highFragment.getOperation();

    Operation operation = new DrawUniformOperation(low, high);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitSimpleIdentifier(QubecTalkParser.SimpleIdentifierContext ctx) {
    String identifier = ctx.getChild(0).getText();
    Operation operation = new GetVariableOperation(identifier);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitGetStream(QubecTalkParser.GetStreamContext ctx) {
    String streamName = visit(ctx.target).getString();

    Operation operation = new JointOperation(
        new GetStreamOperation(streamName),
        new RemoveUnitsOperation()
    );

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitBoundExpression(QubecTalkParser.LimitBoundExpressionContext ctx) {
    return visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitStream(QubecTalkParser.StreamContext ctx) {
    return new StringFragment(applyStreamSugar(ctx.getText().replaceAll("\"", "")));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitIdentifierAsVar(QubecTalkParser.IdentifierAsVarContext ctx) {
    String identifier = ctx.getChild(0).getText();
    Operation operation = new GetVariableOperation(identifier);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDuringRange(QubecTalkParser.DuringRangeContext ctx) {
    TimePointFuture start = new CalculatedTimePointFuture(visit(ctx.expression(0)).getOperation());
    TimePointFuture end = new CalculatedTimePointFuture(visit(ctx.expression(1)).getOperation());
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.of(end));
    return new DuringFragment(during);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDuringStart(QubecTalkParser.DuringStartContext ctx) {
    TimePointFuture start = new DynamicCapFuture(BEGINNING);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.of(start));
    return new DuringFragment(during);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDuringSingleYear(QubecTalkParser.DuringSingleYearContext ctx) {
    TimePointFuture point = new CalculatedTimePointFuture(visit(ctx.expression()).getOperation());
    ParsedDuring during = new ParsedDuring(Optional.of(point), Optional.of(point));
    return new DuringFragment(during);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDuringAll(QubecTalkParser.DuringAllContext ctx) {
    TimePointFuture start = new DynamicCapFuture(BEGINNING);
    TimePointFuture end = new DynamicCapFuture(ONWARDS);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.of(end));
    return new DuringFragment(during);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDuringWithMax(QubecTalkParser.DuringWithMaxContext ctx) {
    TimePointFuture start = new DynamicCapFuture(BEGINNING);
    TimePointFuture end = new CalculatedTimePointFuture(visit(ctx.expression()).getOperation());
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.of(end));
    return new DuringFragment(during);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDuringWithMin(QubecTalkParser.DuringWithMinContext ctx) {
    TimePointFuture start = new CalculatedTimePointFuture(visit(ctx.expression()).getOperation());
    TimePointFuture end = new DynamicCapFuture(ONWARDS);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.of(end));
    return new DuringFragment(during);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDefaultStanza(QubecTalkParser.DefaultStanzaContext ctx) {
    List<ParsedApplication> applications = new ArrayList<>();

    for (QubecTalkParser.ApplicationDefContext appCtx : ctx.applicationDef()) {
      Fragment appFragment = visit(appCtx);
      applications.add(appFragment.getApplication());
    }

    ParsedPolicy policy = new ParsedPolicy("default", applications);
    return new PolicyFragment(policy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitAboutStanza(QubecTalkParser.AboutStanzaContext ctx) {
    return new AboutStanzaFragment();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitSimulationsStanza(QubecTalkParser.SimulationsStanzaContext ctx) {
    List<ParsedScenario> scenarios = new ArrayList<>();

    // Process each simulation
    for (int i = 2; i < ctx.getChildCount() - 2; i++) {
      if (ctx.getChild(i) instanceof QubecTalkParser.SimulateContext) {
        QubecTalkParser.SimulateContext simCtx = (QubecTalkParser.SimulateContext) ctx.getChild(i);
        Fragment simFragment = visit(simCtx);
        scenarios.add(simFragment.getScenario());
      }
    }

    ParsedScenarios parsedScenarios = new ParsedScenarios(scenarios);
    return new ScenariosFragment(parsedScenarios);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitPolicyStanza(QubecTalkParser.PolicyStanzaContext ctx) {
    List<ParsedApplication> applications = new ArrayList<>();

    // Get the policy name
    String policyName = visit(ctx.name).getString();

    for (QubecTalkParser.ApplicationModContext appCtx : ctx.applicationMod()) {
      Fragment appFragment = visit(appCtx);
      applications.add(appFragment.getApplication());
    }

    ParsedPolicy policy = new ParsedPolicy(policyName, applications);
    return new PolicyFragment(policy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitApplicationDef(QubecTalkParser.ApplicationDefContext ctx) {
    String name = visit(ctx.name).getString();
    List<ParsedSubstance> substances = new ArrayList<>();

    for (QubecTalkParser.SubstanceDefContext subCtx : ctx.substanceDef()) {
      Fragment substanceFragment = visit(subCtx);
      substances.add(substanceFragment.getSubstance());
    }

    ParsedApplication application = new ParsedApplication(name, substances);
    return new ApplicationFragment(application);
  }

  /**
   * Processes a substance definition by extracting its name and operations.
   *
   * <p>Iterates through all children in order, processing both substanceStatement and
   * globalStatement types in the correct sequence. Uses getChild since multiple statement types are
   * present in a single substance definition.</p>
   */
  @Override
  public Fragment visitSubstanceDef(QubecTalkParser.SubstanceDefContext ctx) {
    String name = visit(ctx.name).getString();
    List<Operation> operations = new ArrayList<>();

    for (int i = SUBSTANCE_BODY_START; i < ctx.getChildCount() - SUBSTANCE_BODY_END; i++) {
      Fragment statementFragment = visit(ctx.getChild(i));
      if (statementFragment != null) {
        try {
          Operation operation = statementFragment.getOperation();
          if (operation != null) {
            operations.add(operation);
          }
        } catch (RuntimeException e) {
          // Ignore fragments that don't have operations
        }
      }
    }

    ParsedSubstance substance = new ParsedSubstance(name, operations);
    return new SubstanceFragment(substance);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitApplicationMod(QubecTalkParser.ApplicationModContext ctx) {
    String name = visit(ctx.name).getString();
    List<ParsedSubstance> substances = new ArrayList<>();

    for (QubecTalkParser.SubstanceModContext subCtx : ctx.substanceMod()) {
      Fragment substanceFragment = visit(subCtx);
      substances.add(substanceFragment.getSubstance());
    }

    ParsedApplication application = new ParsedApplication(name, substances);
    return new ApplicationFragment(application);
  }

  /**
   * Processes a substance modification by extracting its name and operations.
   *
   * <p>Iterates through all children in order, processing both substanceStatement and
   * globalStatement types in the correct sequence. Uses getChild since multiple statement types are
   * present in a single substance modification.</p>
   */
  @Override
  public Fragment visitSubstanceMod(QubecTalkParser.SubstanceModContext ctx) {
    String name = visit(ctx.name).getString();
    List<Operation> operations = new ArrayList<>();

    for (int i = SUBSTANCE_BODY_START; i < ctx.getChildCount() - SUBSTANCE_BODY_END; i++) {
      Fragment statementFragment = visit(ctx.getChild(i));
      if (statementFragment != null) {
        try {
          Operation operation = statementFragment.getOperation();
          if (operation != null) {
            operations.add(operation);
          }
        } catch (RuntimeException e) {
          // Ignore fragments that don't have operations
        }
      }
    }

    ParsedSubstance substance = new ParsedSubstance(name, operations);
    return new SubstanceFragment(substance);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitCommandAllYears(QubecTalkParser.LimitCommandAllYearsContext ctx) {
    String stream = applyStreamSugar(ctx.target.getText());
    Operation valueOperation = visit(ctx.value).getOperation();
    Operation operation;

    if (ctx.getText().startsWith("cap")) {
      operation = new CapOperation(stream, valueOperation);
    } else if (ctx.getText().startsWith("floor")) {
      operation = new FloorOperation(stream, valueOperation);
    } else {
      throw new RuntimeException("Unknown limit operation: expected 'cap' or 'floor'");
    }

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitCommandDisplacingAllYears(
      QubecTalkParser.LimitCommandDisplacingAllYearsContext ctx) {
    String stream = applyStreamSugar(ctx.target.getText());
    Operation valueOperation = visit(ctx.value).getOperation();
    String displaceTarget = ctx.getChild(5).accept(this).getString();

    Operation operation;

    if (ctx.getText().startsWith("cap")) {
      operation = new CapOperation(stream, valueOperation, displaceTarget);
    } else if (ctx.getText().startsWith("floor")) {
      operation = new FloorOperation(stream, valueOperation, displaceTarget);
    } else {
      throw new RuntimeException("Unknown limit operation: expected 'cap' or 'floor'");
    }

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitCommandDuration(QubecTalkParser.LimitCommandDurationContext ctx) {
    String stream = applyStreamSugar(ctx.target.getText());
    Operation valueOperation = visit(ctx.value).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation;

    if (ctx.getText().startsWith("cap")) {
      operation = new CapOperation(stream, valueOperation, during);
    } else if (ctx.getText().startsWith("floor")) {
      operation = new FloorOperation(stream, valueOperation, during);
    } else {
      throw new RuntimeException("Unknown limit operation: expected 'cap' or 'floor'");
    }

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitCommandDisplacingDuration(
      QubecTalkParser.LimitCommandDisplacingDurationContext ctx) {
    String stream = applyStreamSugar(ctx.target.getText());
    Operation valueOperation = visit(ctx.value).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    String displaceTarget = ctx.getChild(5).accept(this).getString();

    Operation operation;

    if (ctx.getText().startsWith("cap")) {
      operation = new CapOperation(stream, valueOperation, displaceTarget, during);
    } else if (ctx.getText().startsWith("floor")) {
      operation = new FloorOperation(stream, valueOperation, displaceTarget, during);
    } else {
      throw new RuntimeException("Unknown limit operation: expected 'cap' or 'floor'");
    }

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitChangeAllYears(QubecTalkParser.ChangeAllYearsContext ctx) {
    String stream = applyStreamSugar(ctx.target.getText());
    Operation valueOperation = visit(ctx.value).getOperation();
    Operation operation = new ChangeOperation(stream, valueOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitChangeDuration(QubecTalkParser.ChangeDurationContext ctx) {
    String stream = applyStreamSugar(ctx.target.getText());
    Operation valueOperation = visit(ctx.value).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new ChangeOperation(stream, valueOperation, during);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDefineVarStatement(QubecTalkParser.DefineVarStatementContext ctx) {
    String identifier = ctx.target.getText();
    Operation valueOperation = visit(ctx.value).getOperation();
    Operation operation = new DefineVariableOperation(identifier, valueOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitEqualsAllYears(QubecTalkParser.EqualsAllYearsContext ctx) {
    Operation valueOperation = visit(ctx.value).getOperation();
    Operation operation = new EqualsOperation(valueOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitEqualsDuration(QubecTalkParser.EqualsDurationContext ctx) {
    return visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitInitialChargeAllYears(QubecTalkParser.InitialChargeAllYearsContext ctx) {
    Operation valueOperation = visit(ctx.value).getOperation();
    String stream = applyStreamSugar(ctx.target.getText());

    String unitString = ctx.value.unitOrRatio().getText();
    validateInitialChargeUnits(unitString, stream);

    Operation operation = new InitialChargeOperation(stream, valueOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitInitialChargeDuration(QubecTalkParser.InitialChargeDurationContext ctx) {
    Operation valueOperation = visit(ctx.value).getOperation();
    String stream = applyStreamSugar(ctx.target.getText());
    ParsedDuring during = visit(ctx.duration).getDuring();

    String unitString = ctx.value.unitOrRatio().getText();
    validateInitialChargeUnits(unitString, stream);

    Operation operation = new InitialChargeOperation(stream, valueOperation, during);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRechargeAllYears(QubecTalkParser.RechargeAllYearsContext ctx) {
    Operation populationOperation = visit(ctx.population).getOperation();
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation operation = new RechargeOperation(populationOperation, volumeOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRechargeDuration(QubecTalkParser.RechargeDurationContext ctx) {
    Operation populationOperation = visit(ctx.population).getOperation();
    Operation volumeOperation = visit(ctx.volume).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new RechargeOperation(populationOperation, volumeOperation, during);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverAllYears(QubecTalkParser.RecoverAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverDuration(QubecTalkParser.RecoverDurationContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, during);
    return new OperationFragment(operation);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverStageAllYears(QubecTalkParser.RecoverStageAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    RecoveryStage stage = parseRecoveryStage(ctx.stage);
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, stage);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverStageDuration(QubecTalkParser.RecoverStageDurationContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    RecoveryStage stage = parseRecoveryStage(ctx.stage);
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, during, stage);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverInductionAllYears(QubecTalkParser.RecoverInductionAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    Operation inductionOperation = visit(ctx.inductionVal).getOperation();
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, Optional.of(inductionOperation));
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverInductionDuration(QubecTalkParser.RecoverInductionDurationContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    Operation inductionOperation = visit(ctx.inductionVal).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, during, Optional.of(inductionOperation));
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverInductionStageAllYears(QubecTalkParser.RecoverInductionStageAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    Operation inductionOperation = visit(ctx.inductionVal).getOperation();
    RecoveryStage stage = parseRecoveryStage(ctx.stage);
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, stage, Optional.of(inductionOperation));
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverInductionStageDuration(QubecTalkParser.RecoverInductionStageDurationContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    Operation inductionOperation = visit(ctx.inductionVal).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    RecoveryStage stage = parseRecoveryStage(ctx.stage);
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, during, stage, Optional.of(inductionOperation));
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverDefaultInductionAllYears(QubecTalkParser.RecoverDefaultInductionAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, Optional.empty());
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverDefaultInductionDuration(QubecTalkParser.RecoverDefaultInductionDurationContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, during, Optional.empty());
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverDefaultInductionStageAllYears(QubecTalkParser.RecoverDefaultInductionStageAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    RecoveryStage stage = parseRecoveryStage(ctx.stage);
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, stage, Optional.empty());
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRecoverDefaultInductionStageDuration(QubecTalkParser.RecoverDefaultInductionStageDurationContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation yieldOperation = visit(ctx.yieldVal).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    RecoveryStage stage = parseRecoveryStage(ctx.stage);
    Operation operation = new RecoverOperation(volumeOperation, yieldOperation, during, stage, Optional.empty());
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitReplaceAllYears(QubecTalkParser.ReplaceAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    String stream = applyStreamSugar(ctx.target.getText());
    String destinationSubstance = visit(ctx.destination).getString();
    Operation operation = new ReplaceOperation(volumeOperation, stream, destinationSubstance);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitReplaceDuration(QubecTalkParser.ReplaceDurationContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    String stream = applyStreamSugar(ctx.target.getText());
    String destinationSubstance = visit(ctx.destination).getString();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new ReplaceOperation(volumeOperation, stream, destinationSubstance, during);
    return new OperationFragment(operation);
  }


  /**
   * Processes retire operation applied to all years, with or without replacement.
   *
   * <p>Checks for the presence of "with replacement" clause and creates the appropriate
   * operation type.</p>
   */
  @Override
  public Fragment visitRetireAllYears(QubecTalkParser.RetireAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    boolean withReplacement = hasWithReplacement(ctx);

    Operation operation = withReplacement
        ? new RetireWithReplacementOperation(volumeOperation)
        : new RetireOperation(volumeOperation);

    return new OperationFragment(operation);
  }

  /**
   * Processes retire operation for a specified duration, with or without replacement.
   *
   * <p>Checks for the presence of "with replacement" clause and creates the appropriate
   * operation type with timing information.</p>
   */
  @Override
  public Fragment visitRetireDuration(QubecTalkParser.RetireDurationContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    boolean withReplacement = hasWithReplacement(ctx);

    Operation operation = withReplacement
        ? new RetireWithReplacementOperation(volumeOperation, during)
        : new RetireOperation(volumeOperation, during);

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitSetAllYears(QubecTalkParser.SetAllYearsContext ctx) {
    Operation valueOperation = visit(ctx.value).getOperation();
    String stream = applyStreamSugar(ctx.target.getText());
    Operation operation = new SetOperation(stream, valueOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitSetDuration(QubecTalkParser.SetDurationContext ctx) {
    Operation valueOperation = visit(ctx.value).getOperation();
    String stream = applyStreamSugar(ctx.target.getText());
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new SetOperation(stream, valueOperation, during);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitEnableAllYears(QubecTalkParser.EnableAllYearsContext ctx) {
    String stream = applyStreamSugar(ctx.target.getText());
    Operation operation = new EnableOperation(stream);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitEnableDuration(QubecTalkParser.EnableDurationContext ctx) {
    String stream = applyStreamSugar(ctx.target.getText());
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new EnableOperation(stream, during);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitAssumeNoAllYears(QubecTalkParser.AssumeNoAllYearsContext ctx) {
    return processAssumeStatement("no", ctx.target, Optional.empty());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitAssumeNoDuration(QubecTalkParser.AssumeNoDurationContext ctx) {
    ParsedDuring during = visit(ctx.duration).getDuring();
    return processAssumeStatement("no", ctx.target, Optional.of(during));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitAssumeOnlyRechargeAllYears(QubecTalkParser.AssumeOnlyRechargeAllYearsContext ctx) {
    return processAssumeStatement("onlyrecharge", ctx.target, Optional.empty());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitAssumeOnlyRechargeDuration(QubecTalkParser.AssumeOnlyRechargeDurationContext ctx) {
    ParsedDuring during = visit(ctx.duration).getDuring();
    return processAssumeStatement("onlyrecharge", ctx.target, Optional.of(during));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitAssumeContinuedAllYears(QubecTalkParser.AssumeContinuedAllYearsContext ctx) {
    return processAssumeStatement("continued", ctx.target, Optional.empty());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitAssumeContinuedDuration(QubecTalkParser.AssumeContinuedDurationContext ctx) {
    ParsedDuring during = visit(ctx.duration).getDuring();
    return processAssumeStatement("continued", ctx.target, Optional.of(during));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitBaseSimulation(QubecTalkParser.BaseSimulationContext ctx) {
    // Get the scenario name
    String name = visit(ctx.name).getString();

    // Get start and end years
    int startYear = Integer.parseInt(ctx.start.getText());
    int endYear = Integer.parseInt(ctx.end.getText());

    // Create a scenario with no policies
    ParsedScenario scenario = new ParsedScenario(name, new ArrayList<>(), startYear, endYear, 1);
    return new ScenarioFragment(scenario);
  }

  /**
   * Processes a simulation scenario with specified policies and time bounds.
   *
   * <p>Extracts the scenario name, start/end years, and the list of policies to apply.</p>
   */
  @Override
  public Fragment visitPolicySim(QubecTalkParser.PolicySimContext ctx) {
    String name = visit(ctx.name).getString();
    int startYear = Integer.parseInt(ctx.start.getText());
    int endYear = Integer.parseInt(ctx.end.getText());

    List<String> policies = new ArrayList<>();
    for (int i = 0; i < ctx.string().size() - 1; i++) {
      policies.add(visit(ctx.string(i + 1)).getString());
    }

    ParsedScenario scenario = new ParsedScenario(name, policies, startYear, endYear, 1);
    return new ScenarioFragment(scenario);
  }

  /**
   * Processes a base simulation scenario with multiple trials but no policies.
   *
   * <p>Extracts the scenario name, start/end years, and trial count for stochastic runs.</p>
   */
  @Override
  public Fragment visitBaseSimulationTrials(QubecTalkParser.BaseSimulationTrialsContext ctx) {
    String name = visit(ctx.name).getString();
    int startYear = Integer.parseInt(ctx.start.getText());
    int endYear = Integer.parseInt(ctx.end.getText());
    int trials = Integer.parseInt(ctx.trials.getText());

    ParsedScenario scenario = new ParsedScenario(name, new ArrayList<>(), startYear, endYear, trials);
    return new ScenarioFragment(scenario);
  }

  /**
   * Processes a policy simulation scenario with multiple trials and specified policies.
   *
   * <p>Extracts the scenario name, start/end years, trial count, and the list of policies.</p>
   */
  @Override
  public Fragment visitPolicySimTrials(QubecTalkParser.PolicySimTrialsContext ctx) {
    String name = visit(ctx.name).getString();
    int startYear = Integer.parseInt(ctx.start.getText());
    int endYear = Integer.parseInt(ctx.end.getText());
    int trials = Integer.parseInt(ctx.trials.getText());

    List<String> policies = new ArrayList<>();
    for (int i = 0; i < ctx.string().size() - 1; i++) {
      policies.add(visit(ctx.string(i + 1)).getString());
    }

    ParsedScenario scenario = new ParsedScenario(name, policies, startYear, endYear, trials);
    return new ScenarioFragment(scenario);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitGlobalStatement(QubecTalkParser.GlobalStatementContext ctx) {
    return visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitSubstanceStatement(QubecTalkParser.SubstanceStatementContext ctx) {
    return visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitParenExpression(QubecTalkParser.ParenExpressionContext ctx) {
    return visit(ctx.getChild(1));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitProgram(QubecTalkParser.ProgramContext ctx) {
    List<ParsedPolicy> policies = new ArrayList<>();
    List<ParsedScenario> scenarios = new ArrayList<>();

    for (QubecTalkParser.StanzaContext stanzaCtx : ctx.stanza()) {
      Fragment stanzaFragment = visit(stanzaCtx);

      if (stanzaFragment.getIsStanzaScenarios()) {
        ParsedScenarios parsedScenarios = stanzaFragment.getScenarios();
        for (String scenarioName : parsedScenarios.getScenarios()) {
          scenarios.add(parsedScenarios.getScenario(scenarioName));
        }
      } else if (stanzaFragment.getIsStanzaPolicyOrDefault()) {
        policies.add(stanzaFragment.getPolicy());
      }
    }

    ParsedProgram program = new ParsedProgram(policies, scenarios);
    return new ProgramFragment(program);
  }

  /**
   * Parse recovery stage from context token.
   */
  private RecoveryStage parseRecoveryStage(Token stageToken) {
    String stageText = stageToken.getText();
    return switch (stageText) {
      case "eol" -> RecoveryStage.EOL;
      case "recharge" -> RecoveryStage.RECHARGE;
      default -> throw new IllegalArgumentException("Invalid recovery stage: " + stageText);
    };
  }

  /**
   * Check if a parse tree context contains the "with replacement" clause.
   *
   * <p>This method checks for the presence of "withreplacement" (case-insensitive,
   * no spaces) in the parse tree text. The grammar combines WITH_ and REPLACEMENT_ tokens, so they
   * appear concatenated in the text representation.</p>
   *
   * @param ctx The parse tree context to check
   * @return true if the context contains "with replacement", false otherwise
   */
  private boolean hasWithReplacement(org.antlr.v4.runtime.ParserRuleContext ctx) {
    return ctx.getText().toLowerCase().contains("withreplacement");
  }

  /**
   * Validates that initial charge units end with "unit" or "units".
   *
   * @param unitString The unit string to validate (e.g., "kg / unit", "kg / units", "kg")
   * @param stream The stream name for error reporting (e.g., "domestic", "import")
   * @throws RuntimeException if units don't end with "unit" or "units"
   */
  private void validateInitialChargeUnits(String unitString, String stream) {
    String normalized = unitString.trim().toLowerCase();

    if (!normalized.endsWith("unit") && !normalized.endsWith("units")) {
      throw new RuntimeException(
          String.format(
              "Initial charge for %s stream must be specified per unit (e.g., 'kg / unit' or 'kg / units'), but found '%s'. "
                  + "Equipment-based calculations require initial charges to be rates per unit.",
              stream,
              unitString
          )
      );
    }
  }

  /**
   * Apply syntactic sugar transformations to stream names.
   *
   * <p>This method transforms stream name aliases to their canonical form.
   * Currently supports:
   * <ul>
   *   <li>"bank" -> "equipment"</li>
   *   <li>"priorBank" -> "priorEquipment"</li>
   * </ul>
   *
   * @param streamName The stream name to transform
   * @return The canonical stream name
   */
  private String applyStreamSugar(String streamName) {
    return switch (streamName) {
      case "bank" -> "equipment";
      case "priorBank" -> "priorEquipment";
      default -> streamName;
    };
  }

  /**
   * Create a SetOperation with optional duration.
   *
   * @param stream The target stream name
   * @param valueOperation The operation producing the value to set
   * @param duringMaybe Optional time period for the operation
   * @return SetOperation with or without duration based on duringMaybe
   */
  private Operation makeSet(String stream, Operation valueOperation, Optional<ParsedDuring> duringMaybe) {
    if (duringMaybe.isPresent()) {
      return new SetOperation(stream, valueOperation, duringMaybe.get());
    } else {
      return new SetOperation(stream, valueOperation);
    }
  }

  /**
   * Process assume statement and transform to SetOperation.
   *
   * @param modeText The assume mode ("no", "onlyrecharge", or "continued")
   * @param targetContext The parse context containing the target stream
   * @param duringMaybe Optional time period for the command
   * @return OperationFragment containing the transformed SetOperation, or null for no-op
   */
  private Fragment processAssumeStatement(
      String modeText,
      QubecTalkParser.StreamContext targetContext,
      Optional<ParsedDuring> duringMaybe) {

    String stream = applyStreamSugar(targetContext.getText());

    return switch (modeText) {
      case "no" -> {
        EngineNumber zeroKg = new EngineNumber(java.math.BigDecimal.ZERO, "kg");
        Operation valueOperation = new PreCalculatedOperation(zeroKg);
        yield new OperationFragment(makeSet(stream, valueOperation, duringMaybe));
      }
      case "onlyrecharge" -> {
        EngineNumber zeroUnits = new EngineNumber(java.math.BigDecimal.ZERO, "units");
        Operation valueOperation = new PreCalculatedOperation(zeroUnits);
        yield new OperationFragment(makeSet(stream, valueOperation, duringMaybe));
      }
      case "continued" -> new OperationFragment(null);
      default -> throw new RuntimeException("Unknown assume mode: " + modeText);
    };
  }
}
