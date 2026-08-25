/**
 * Calculation which retires equipment according to a Weibull survival curve.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.engine.support.EngineSupportUtils;
import org.kigalisim.engine.support.WeibullSurvival;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.time.ParsedDuring;

/**
 * Operation that retires equipment according to a Weibull survival curve (shape 2,
 * the Rayleigh case) driven by a mean lifetime in years.
 *
 * <p>Each year the operation computes a unit count from the Weibull survival curve
 * and the recorded sales history (the cohort size at age <i>a</i> is the
 * {@code newEquipment} stream from <i>a</i> years ago), then retires that count
 * through the existing units-based retirement path. This composes with the existing
 * engine retirement machinery rather than duplicating it.</p>
 */
public class RetireWeibullOperation implements Operation {

  private final BigDecimal meanYears;

  private final boolean assumingNew;

  private final boolean withReplacement;

  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a Weibull retire operation that applies to all years.
   *
   * @param meanYears The mean equipment lifetime in years.
   * @param assumingNew Whether prior equipment should be treated as a pseudo-cohort of
   *     typical age (the {@code assuming new} modifier).
   * @param withReplacement Whether retired equipment should be replaced to maintain
   *     population.
   */
  public RetireWeibullOperation(BigDecimal meanYears, boolean assumingNew, boolean withReplacement) {
    this.meanYears = meanYears;
    this.assumingNew = assumingNew;
    this.withReplacement = withReplacement;
    this.duringMaybe = Optional.empty();
  }

  /**
   * Create a Weibull retire operation that applies to a specific time period.
   *
   * @param meanYears The mean equipment lifetime in years.
   * @param assumingNew Whether prior equipment should be treated as a pseudo-cohort of
   *     typical age (the {@code assuming new} modifier).
   * @param withReplacement Whether retired equipment should be replaced to maintain
   *     population.
   * @param during The time period during which this operation applies.
   */
  public RetireWeibullOperation(BigDecimal meanYears, boolean assumingNew, boolean withReplacement,
      ParsedDuring during) {
    this.meanYears = meanYears;
    this.assumingNew = assumingNew;
    this.withReplacement = withReplacement;
    this.duringMaybe = Optional.of(during);
  }

  /**
   * Whether prior equipment is treated as a pseudo-cohort of typical age.
   *
   * @return true if the {@code assuming new} modifier is set
   */
  public boolean getAssumingNew() {
    return assumingNew;
  }

  /**
   * Execute the Weibull retire operation on the given push-down machine.
   *
   * <p>Builds a year matcher from the optional during clause, skips execution if the
   * current year is out of range, computes the unit count from the survival history, and
   * delegates the actual retirement to the existing units-based retirement path. With
   * {@code with replacement}, measures the equipment population before and after
   * retirement and increases sales by the actual reduction to maintain population,
   * mirroring {@link RetireWithReplacementOperation}.</p>
   *
   * @param machine The push-down machine context for evaluating operations.
   */
  @Override
  public void execute(PushDownMachine machine) {
    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();

    if (!EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear())) {
      return;
    }

    EngineSupportUtils.ensureConsistentReplacement(engine, withReplacement);

    BigDecimal retireUnits = calculateRetireUnits(engine);
    EngineNumber retireAmount = new EngineNumber(retireUnits, "units");

    if (withReplacement) {
      retireWithReplacement(engine, retireAmount, yearMatcher);
    } else {
      engine.retire(retireAmount, yearMatcher);
    }
  }

  /**
   * Retire the given amount and replace it by increasing sales.
   *
   * <p>Measures equipment before and after retirement to determine the actual
   * reduction, then increases sales by that amount in whichever units sales were last
   * specified, maintaining the equipment population while simulating turnover.</p>
   *
   * @param engine The engine to read and update stream state on.
   * @param retireAmount The number of units to retire.
   * @param yearMatcher The year matcher for this operation.
   */
  private void retireWithReplacement(Engine engine, EngineNumber retireAmount, YearMatcher yearMatcher) {
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, "sales");
    EngineNumber equipmentBefore = unitConverter.convert(engine.getStream("equipment"), "units");

    engine.retire(retireAmount, yearMatcher);

    EngineNumber equipmentAfter = unitConverter.convert(engine.getStream("equipment"), "units");
    BigDecimal actualReduction = equipmentBefore.getValue().subtract(equipmentAfter.getValue());

    if (actualReduction.compareTo(BigDecimal.ZERO) > 0) {
      String targetUnits = determineTargetUnits(engine);
      EngineNumber replacementAmount = unitConverter.convert(
          new EngineNumber(actualReduction, "units"),
          targetUnits
      );
      engine.changeStream("sales", replacementAmount, yearMatcher);
    }
  }

  /**
   * Determine the target units for replacement based on how sales were last specified.
   *
   * @param engine The current simulation engine.
   * @return The target units for replacement ("units" or "kg").
   */
  private String determineTargetUnits(Engine engine) {
    SimulationState simulationState = engine.getStreamKeeper();
    UseKey scope = engine.getScope();
    EngineNumber lastSalesValue = simulationState.getLastSpecifiedValue(scope, "sales");

    if (lastSalesValue != null && lastSalesValue.hasEquipmentUnits()) {
      return "units";
    } else {
      return "kg";
    }
  }

  /**
   * Compute the number of units to retire this year from the survival history.
   *
   * <p>For each age <i>a</i> up to the truncation age, or the years elapsed so far if
   * that is smaller, the cohort is the
   * {@code newEquipment} value from <i>a</i> years ago, weighted by S(a&minus;1)
   * (the cohort's survival heading into its <i>a</i>-th year) and then by the hazard
   * h(a). The retire amount is the current equipment population times the hazard
   * weight over the survival weight. With {@code assuming new}, any prior equipment
   * the sales history does not account for contributes a single pseudo-cohort at a
   * typical age; equipment already covered by a tracked cohort is left to that
   * cohort so it is not counted twice. If there is no weight at all, zero units are
   * retired.</p>
   *
   * @param engine The engine to read stream and year state from.
   * @return the number of units to retire.
   */
  private BigDecimal calculateRetireUnits(Engine engine) {
    WeibullSurvival survival = WeibullSurvival.fromMean(meanYears);
    UseKey scope = engine.getScope();
    BigDecimal population = engine.getStreamFor(scope, "priorEquipment").getValue();
    int yearsElapsed = engine.getYear() - engine.getStartYear();

    BigDecimal weightSum = BigDecimal.ZERO;
    BigDecimal retireWeight = BigDecimal.ZERO;

    // Ages beyond the elapsed years have no recorded history and always read zero, so
    // stop there rather than walking the whole truncation window every year.
    int oldestTrackedAge = Math.min(survival.getTruncationAge(), yearsElapsed);
    for (int a = 1; a <= oldestTrackedAge; a++) {
      BigDecimal cohort = engine.getStream(
          "newEquipment",
          Optional.of(scope),
          Optional.of("units"),
          a
      ).getValue();
      if (cohort.signum() == 0) {
        continue;
      }
      BigDecimal survivalPrior = survival.getSurvival(a - 1);
      BigDecimal weight = cohort.multiply(survivalPrior);
      weightSum = weightSum.add(weight);
      retireWeight = retireWeight.add(weight.multiply(survival.getHazard(a)));
    }

    if (assumingNew) {
      // The loop's weights are the tracked cohorts' surviving units, so whatever prior
      // equipment they do not explain is the manually entered stock of unknown age.
      BigDecimal untracked = population.subtract(weightSum).max(BigDecimal.ZERO);
      if (untracked.signum() > 0) {
        int effectiveYears = yearsElapsed + survival.getSyntheticCohortOffsetYears();
        int pseudoAge = Math.min(effectiveYears, survival.getTruncationAge());
        weightSum = weightSum.add(untracked);
        retireWeight = retireWeight.add(untracked.multiply(survival.getHazard(pseudoAge)));
      }
    }

    if (weightSum.signum() == 0) {
      return BigDecimal.ZERO;
    }
    return population.multiply(retireWeight)
        .divide(weightSum, MathContext.DECIMAL128);
  }
}
