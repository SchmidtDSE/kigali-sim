/**
 * Calculation which retires the sales cohort that turns a fixed age this year.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.engine.support.EngineSupportUtils;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.time.ParsedDuring;

/**
 * Operation backing the {@code retire N year old exact} shortcut.
 *
 * <p>Delegates the actual retirement to the existing units-based retirement path once the
 * age-N cohort's size is computed by {@code amountOperation}, optionally replacing retired
 * equipment to maintain population the same way {@link RetireWithReplacementOperation} and
 * {@link RetireWeibullOperation} do. Kept as its own operation type rather than reusing
 * {@link RetireOperation} / {@link RetireWithReplacementOperation} directly so that
 * {@code ProgramValidator} can flag it when combined with a directly-set
 * {@code priorEquipment}, whose age this shortcut's sales-history lookup cannot see, unless
 * {@code assumingNew} is set.</p>
 */
public class RetireExactOperation implements Operation {

  private final Operation amountOperation;

  private final int ageYears;

  private final boolean assumingNew;

  private final boolean withReplacement;

  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new RetireExactOperation that applies to all years.
   *
   * @param amountOperation The operation that calculates the age-N cohort size to retire.
   * @param ageYears The exact age in years at which the cohort retires.
   * @param assumingNew Whether prior equipment should be treated as a pseudo-cohort assumed
   *     to have entered service when the simulation began (the {@code assuming new} modifier).
   * @param withReplacement Whether retired equipment should be replaced to maintain population.
   */
  public RetireExactOperation(Operation amountOperation, int ageYears, boolean assumingNew,
      boolean withReplacement) {
    this.amountOperation = amountOperation;
    this.ageYears = ageYears;
    this.assumingNew = assumingNew;
    this.withReplacement = withReplacement;
    this.duringMaybe = Optional.empty();
  }

  /**
   * Create a new RetireExactOperation that applies to a specific time period.
   *
   * @param amountOperation The operation that calculates the age-N cohort size to retire.
   * @param ageYears The exact age in years at which the cohort retires.
   * @param assumingNew Whether prior equipment should be treated as a pseudo-cohort assumed
   *     to have entered service when the simulation began (the {@code assuming new} modifier).
   * @param withReplacement Whether retired equipment should be replaced to maintain population.
   * @param during The time period during which this operation applies.
   */
  public RetireExactOperation(Operation amountOperation, int ageYears, boolean assumingNew,
      boolean withReplacement, ParsedDuring during) {
    this.amountOperation = amountOperation;
    this.ageYears = ageYears;
    this.assumingNew = assumingNew;
    this.withReplacement = withReplacement;
    this.duringMaybe = Optional.of(during);
  }

  /**
   * Whether prior equipment is treated as a pseudo-cohort assumed to have entered service
   * when the simulation began.
   *
   * @return true if the {@code assuming new} modifier is set
   */
  public boolean getAssumingNew() {
    return assumingNew;
  }

  /**
   * Execute the exact retire operation on the given push-down machine.
   *
   * <p>Evaluates the amount operation to get the size of the age-N cohort, builds a year
   * matcher from the optional during clause, and checks if this operation should execute in
   * the current year. If the year is in range, validates that retire commands are not mixed
   * with and without replacement in the same step. With {@code assuming new}, any
   * priorEquipment not explained by tracked sales cohorts is added to the retirement amount
   * in the one year the simulation reaches the given age (as if that stock had entered
   * service when the simulation began). The cohort is then retired, replacing it to maintain
   * population when {@code withReplacement} is set.</p>
   *
   * @param machine The push-down machine context for evaluating operations.
   */
  @Override
  public void execute(PushDownMachine machine) {
    amountOperation.execute(machine);
    EngineNumber trackedResult = machine.getResult();

    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();

    if (!EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear())) {
      return;
    }

    EngineNumber result = trackedResult;
    if (assumingNew) {
      BigDecimal untracked = calculateAssumingNewUntracked(engine);
      if (untracked.signum() > 0) {
        result = new EngineNumber(trackedResult.getValue().add(untracked), trackedResult.getUnits());
      }
    }

    EngineSupportUtils.ensureConsistentReplacement(engine, withReplacement);

    if (withReplacement) {
      EngineSupportUtils.retireWithReplacement(engine, result, yearMatcher);
    } else {
      engine.retire(result, yearMatcher);
    }
  }

  /**
   * Compute the untracked priorEquipment to fold into this year's retirement.
   *
   * <p>Treats any priorEquipment not explained by tracked sales cohorts as a single
   * pseudo-cohort assumed to have entered service when the simulation began, so it turns
   * {@code ageYears} old (and retires in full) only in the one year the simulation reaches
   * that age. In every other year this contributes nothing.</p>
   *
   * @param engine The engine to read stream and year state from.
   * @return the untracked units to retire this year, or zero if this is not that year or
   *     sales history already explains the full priorEquipment population.
   */
  private BigDecimal calculateAssumingNewUntracked(Engine engine) {
    int yearsElapsed = engine.getYear() - engine.getStartYear();
    if (yearsElapsed != ageYears) {
      return BigDecimal.ZERO;
    }

    UseKey scope = engine.getScope();
    BigDecimal population = engine.getStreamFor(scope, "priorEquipment").getValue();

    BigDecimal tracked = BigDecimal.ZERO;
    for (int a = 1; a <= yearsElapsed; a++) {
      tracked = tracked.add(engine.getStream(
          "newEquipment",
          Optional.of(scope),
          Optional.of("units"),
          a
      ).getValue());
    }

    return population.subtract(tracked).max(BigDecimal.ZERO);
  }
}
