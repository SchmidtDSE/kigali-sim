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
import org.kigalisim.engine.number.EngineNumber;
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
import org.kigalisim.lang.operation.AdditionOperation;
import org.kigalisim.lang.operation.CapOperation;
import org.kigalisim.lang.operation.ChangeOperation;
import org.kigalisim.lang.operation.ChangeUnitsOperation;
import org.kigalisim.lang.operation.ConditionalOperation;
import org.kigalisim.lang.operation.DefineVariableOperation;
import org.kigalisim.lang.operation.DivisionOperation;
import org.kigalisim.lang.operation.EqualityOperation;
import org.kigalisim.lang.operation.EqualsOperation;
import org.kigalisim.lang.operation.FloorOperation;
import org.kigalisim.lang.operation.GetVariableOperation;
import org.kigalisim.lang.operation.InitialChargeOperation;
import org.kigalisim.lang.operation.LogicalOperation;
import org.kigalisim.lang.operation.MultiplicationOperation;
import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.operation.PreCalculatedOperation;
import org.kigalisim.lang.operation.RechargeOperation;
import org.kigalisim.lang.operation.RecoverOperation;
import org.kigalisim.lang.operation.ReplaceOperation;
import org.kigalisim.lang.operation.RetireOperation;
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
 * Fragments which are used to build up Commands which actually execute operations against a
 * Kigali Sim Engine.</p>
 */
@SuppressWarnings("CheckReturnValue")
public class QubecTalkEngineVisitor extends QubecTalkBaseVisitor<Fragment> {

  /**
   * {@inheritDoc}
   */
  @Override public Fragment visitNumber(QubecTalkParser.NumberContext ctx) {
    String rawText = ctx.getText();
    BigDecimal numberRaw = new BigDecimal(rawText);
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
    return new UnitFragment(unit.replaceAll(" each ", " / "));
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
    Operation calculation;
    if (operatorStr.equals("+")) {
      calculation = new AdditionOperation(left, right);
    } else if (operatorStr.equals("-")) {
      calculation = new SubtractionOperation(left, right);
    } else {
      throw new RuntimeException("Unknown addition operation");
    }
    return new OperationFragment(calculation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitPowExpression(QubecTalkParser.PowExpressionContext ctx) {
    return visitChildren(ctx);
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
    return visitChildren(ctx);
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
    return visitChildren(ctx);
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
    Operation calculation;
    if (operatorStr.equals("*")) {
      calculation = new MultiplicationOperation(left, right);
    } else if (operatorStr.equals("/")) {
      calculation = new DivisionOperation(left, right);
    } else {
      throw new RuntimeException("Unknown multiplication operation");
    }
    return new OperationFragment(calculation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDrawNormalExpression(QubecTalkParser.DrawNormalExpressionContext ctx) {
    return visitChildren(ctx);
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
    return visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDrawUniformExpression(QubecTalkParser.DrawUniformExpressionContext ctx) {
    return visitChildren(ctx);
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
    return visitChildren(ctx);
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
    return new StringFragment(ctx.getText().replaceAll("\"", ""));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitIdentifierAsVar(QubecTalkParser.IdentifierAsVarContext ctx) {
    return visitChildren(ctx);
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
    TimePointFuture start = new DynamicCapFuture("beginning");
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
    TimePointFuture start = new DynamicCapFuture("beginning");
    TimePointFuture end = new DynamicCapFuture("onwards");
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.of(end));
    return new DuringFragment(during);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitDuringWithMax(QubecTalkParser.DuringWithMaxContext ctx) {
    TimePointFuture start = new DynamicCapFuture("beginning");
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
    TimePointFuture end = new DynamicCapFuture("onwards");
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.of(end));
    return new DuringFragment(during);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitAboutStanza(QubecTalkParser.AboutStanzaContext ctx) {
    return visitChildren(ctx);
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
   * {@inheritDoc}
   */
  @Override
  public Fragment visitSubstanceDef(QubecTalkParser.SubstanceDefContext ctx) {
    String name = visit(ctx.name).getString();
    List<Operation> operations = new ArrayList<>();

    // Process all children in order (after the substance name and before END_SUBSTANCE_)
    // This ensures we process both substanceStatement and globalStatement in the correct order
    // Note that getChild is needed here because two different statement types are present.
    for (int i = 3; i < ctx.getChildCount() - 2; i++) {
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
   * {@inheritDoc}
   */
  @Override
  public Fragment visitSubstanceMod(QubecTalkParser.SubstanceModContext ctx) {
    String name = visit(ctx.name).getString();
    List<Operation> operations = new ArrayList<>();

    // Process all children in order (after the substance name and before END_SUBSTANCE_)
    // This ensures we process both substanceStatement and globalStatement in the correct order
    for (int i = 3; i < ctx.getChildCount() - 2; i++) {
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
    String stream = ctx.target.getText();
    Operation valueOperation = visit(ctx.value).getOperation();
    Operation operation;

    // Check if this is a cap or floor operation
    if (ctx.getText().startsWith("cap")) {
      operation = new CapOperation(stream, valueOperation);
    } else {
      operation = new FloorOperation(stream, valueOperation);
    }

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitCommandDisplacingAllYears(
      QubecTalkParser.LimitCommandDisplacingAllYearsContext ctx) {
    String stream = ctx.target.getText();
    Operation valueOperation = visit(ctx.value).getOperation();
    String displaceTarget = ctx.getChild(5).accept(this).getString();

    Operation operation;

    // Check if this is a cap or floor operation
    if (ctx.getText().startsWith("cap")) {
      operation = new CapOperation(stream, valueOperation, displaceTarget);
    } else {
      operation = new FloorOperation(stream, valueOperation, displaceTarget);
    }

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitCommandDuration(QubecTalkParser.LimitCommandDurationContext ctx) {
    String stream = ctx.target.getText();
    Operation valueOperation = visit(ctx.value).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation;

    // Check if this is a cap or floor operation
    if (ctx.getText().startsWith("cap")) {
      operation = new CapOperation(stream, valueOperation, during);
    } else {
      operation = new FloorOperation(stream, valueOperation, during);
    }

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitLimitCommandDisplacingDuration(
      QubecTalkParser.LimitCommandDisplacingDurationContext ctx) {
    String stream = ctx.target.getText();
    Operation valueOperation = visit(ctx.value).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    String displaceTarget = ctx.getChild(5).accept(this).getString();

    Operation operation;

    // Check if this is a cap or floor operation
    if (ctx.getText().startsWith("cap")) {
      operation = new CapOperation(stream, valueOperation, displaceTarget, during);
    } else {
      operation = new FloorOperation(stream, valueOperation, displaceTarget, during);
    }

    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitChangeAllYears(QubecTalkParser.ChangeAllYearsContext ctx) {
    String stream = ctx.target.getText();
    Operation valueOperation = visit(ctx.value).getOperation();
    Operation operation = new ChangeOperation(stream, valueOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitChangeDuration(QubecTalkParser.ChangeDurationContext ctx) {
    String stream = ctx.target.getText();
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
    String stream = ctx.target.getText();
    Operation operation = new InitialChargeOperation(stream, valueOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitInitialChargeDuration(QubecTalkParser.InitialChargeDurationContext ctx) {
    return visitChildren(ctx);
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
  public Fragment visitReplaceAllYears(QubecTalkParser.ReplaceAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    String stream = ctx.target.getText();
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
    String stream = ctx.target.getText();
    String destinationSubstance = visit(ctx.destination).getString();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new ReplaceOperation(volumeOperation, stream, destinationSubstance, during);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRetireAllYears(QubecTalkParser.RetireAllYearsContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    Operation operation = new RetireOperation(volumeOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitRetireDuration(QubecTalkParser.RetireDurationContext ctx) {
    Operation volumeOperation = visit(ctx.volume).getOperation();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new RetireOperation(volumeOperation, during);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitSetAllYears(QubecTalkParser.SetAllYearsContext ctx) {
    Operation valueOperation = visit(ctx.value).getOperation();
    String stream = ctx.target.getText();
    Operation operation = new SetOperation(stream, valueOperation);
    return new OperationFragment(operation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitSetDuration(QubecTalkParser.SetDurationContext ctx) {
    Operation valueOperation = visit(ctx.value).getOperation();
    String stream = ctx.target.getText();
    ParsedDuring during = visit(ctx.duration).getDuring();
    Operation operation = new SetOperation(stream, valueOperation, during);
    return new OperationFragment(operation);
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
   * {@inheritDoc}
   */
  @Override
  public Fragment visitPolicySim(QubecTalkParser.PolicySimContext ctx) {
    // Get the scenario name
    String name = visit(ctx.name).getString();

    // Get start and end years
    int startYear = Integer.parseInt(ctx.start.getText());
    int endYear = Integer.parseInt(ctx.end.getText());

    // Get the policies
    List<String> policies = new ArrayList<>();
    for (int i = 0; i < ctx.string().size() - 1; i++) {
      policies.add(visit(ctx.string(i + 1)).getString());
    }

    // Create a scenario with the policies
    ParsedScenario scenario = new ParsedScenario(name, policies, startYear, endYear, 1);
    return new ScenarioFragment(scenario);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitBaseSimulationTrials(QubecTalkParser.BaseSimulationTrialsContext ctx) {
    // Get the scenario name
    String name = visit(ctx.name).getString();

    // Get start and end years
    int startYear = Integer.parseInt(ctx.start.getText());
    int endYear = Integer.parseInt(ctx.end.getText());

    // Get the number of trials
    int trials = Integer.parseInt(ctx.trials.getText());

    // Create a scenario with no policies
    ParsedScenario scenario = new ParsedScenario(name, new ArrayList<>(), startYear, endYear, trials);
    return new ScenarioFragment(scenario);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Fragment visitPolicySimTrials(QubecTalkParser.PolicySimTrialsContext ctx) {
    // Get the scenario name
    String name = visit(ctx.name).getString();

    // Get start and end years
    int startYear = Integer.parseInt(ctx.start.getText());
    int endYear = Integer.parseInt(ctx.end.getText());

    // Get the number of trials
    int trials = Integer.parseInt(ctx.trials.getText());

    // Get the policies
    List<String> policies = new ArrayList<>();
    for (int i = 0; i < ctx.string().size() - 1; i++) {
      policies.add(visit(ctx.string(i + 1)).getString());
    }

    // Create a scenario with the policies
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
      } else {
        policies.add(stanzaFragment.getPolicy());
      }
    }

    ParsedProgram program = new ParsedProgram(policies, scenarios);
    return new ProgramFragment(program);
  }
}
