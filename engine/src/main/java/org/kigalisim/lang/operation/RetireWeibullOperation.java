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

  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a Weibull retire operation that applies to all years.
   *
   * @param meanYears The mean equipment lifetime in years.
   * @param assumingNew Whether prior equipment should be treated as a pseudo-cohort of
   *     typical age (the {@code assuming new} modifier).
   */
  public RetireWeibullOperation(BigDecimal meanYears, boolean assumingNew) {
    this.meanYears = meanYears;
    this.assumingNew = assumingNew;
    this.duringMaybe = Optional.empty();
  }

  /**
   * Create a Weibull retire operation that applies to a specific time period.
   *
   * @param meanYears The mean equipment lifetime in years.
   * @param assumingNew Whether prior equipment should be treated as a pseudo-cohort of
   *     typical age (the {@code assuming new} modifier).
   * @param during The time period during which this operation applies.
   */
  public RetireWeibullOperation(BigDecimal meanYears, boolean assumingNew, ParsedDuring during) {
    this.meanYears = meanYears;
    this.assumingNew = assumingNew;
    this.duringMaybe = Optional.of(during);
  }

  /**
   * The mean equipment lifetime in years.
   *
   * @return the mean lifetime in years
   */
  public BigDecimal getMeanYears() {
    return meanYears;
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
   * current year is out of range, records this as a non-replacement retire, computes
   * the unit count from the survival history, and delegates the actual retirement to
   * the existing units-based retirement path.</p>
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

    EngineSupportUtils.ensureConsistentReplacement(engine, false);

    BigDecimal retireUnits = calculateRetireUnits(engine);
    engine.retire(new EngineNumber(retireUnits, "units"), yearMatcher);
  }

  /**
   * Compute the number of units to retire this year from the survival history.
   *
   * <p>For each age <i>a</i> up to the truncation age the cohort is the
   * {@code newEquipment} value from <i>a</i> years ago, weighted by S(a&minus;1)
   * (the cohort's survival heading into its <i>a</i>-th year) and then by the hazard
   * h(a). The retire amount is the current equipment population times the hazard
   * weight over the survival weight. With {@code assuming new}, prior equipment
   * contributes a single pseudo-cohort at a typical age. If there is no weight at
   * all, zero units are retired.</p>
   *
   * @param engine The engine to read stream and year state from.
   * @return the number of units to retire.
   */
  private BigDecimal calculateRetireUnits(Engine engine) {
    WeibullSurvival survival = WeibullSurvival.fromMean(meanYears);
    UseKey scope = engine.getScope();

    BigDecimal weightSum = BigDecimal.ZERO;
    BigDecimal retireWeight = BigDecimal.ZERO;

    for (int a = 1; a <= survival.getTruncationAge(); a++) {
      BigDecimal cohort = engine.getStream(
          "newEquipment", Optional.of(scope), Optional.of("units"), a).getValue();
      if (cohort.signum() == 0) {
        continue;
      }
      BigDecimal survivalPrior = survival.getSurvival(a - 1);
      BigDecimal weight = cohort.multiply(survivalPrior);
      weightSum = weightSum.add(weight);
      retireWeight = retireWeight.add(weight.multiply(survival.getHazard(a)));
    }

    if (assumingNew) {
      BigDecimal prior = engine.getStreamFor(scope, "priorEquipment").getValue();
      if (prior.signum() > 0) {
        int pseudoAge = Math.min(
            (engine.getYear() - engine.getStartYear()) + survival.getSyntheticCohortOffsetYears(),
            survival.getTruncationAge());
        BigDecimal survivalPrior = survival.getSurvival(pseudoAge - 1);
        BigDecimal weight = prior.multiply(survivalPrior);
        weightSum = weightSum.add(weight);
        retireWeight = retireWeight.add(weight.multiply(survival.getHazard(pseudoAge)));
      }
    }

    if (weightSum.signum() == 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal population = engine.getStreamFor(scope, "priorEquipment").getValue();
    return population.multiply(retireWeight)
        .divide(weightSum, MathContext.DECIMAL128);
  }
}
