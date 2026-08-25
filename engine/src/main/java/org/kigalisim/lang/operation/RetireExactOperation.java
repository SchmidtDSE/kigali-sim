/**
 * Calculation which retires the sales cohort that turns a fixed age this year.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
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
 * {@code priorEquipment}, whose age this shortcut's sales-history lookup cannot see.</p>
 */
public class RetireExactOperation implements Operation {

  private final Operation amountOperation;

  private final boolean withReplacement;

  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new RetireExactOperation that applies to all years.
   *
   * @param amountOperation The operation that calculates the age-N cohort size to retire.
   * @param withReplacement Whether retired equipment should be replaced to maintain population.
   */
  public RetireExactOperation(Operation amountOperation, boolean withReplacement) {
    this.amountOperation = amountOperation;
    this.withReplacement = withReplacement;
    this.duringMaybe = Optional.empty();
  }

  /**
   * Create a new RetireExactOperation that applies to a specific time period.
   *
   * @param amountOperation The operation that calculates the age-N cohort size to retire.
   * @param withReplacement Whether retired equipment should be replaced to maintain population.
   * @param during The time period during which this operation applies.
   */
  public RetireExactOperation(Operation amountOperation, boolean withReplacement,
      ParsedDuring during) {
    this.amountOperation = amountOperation;
    this.withReplacement = withReplacement;
    this.duringMaybe = Optional.of(during);
  }

  /**
   * Execute the exact retire operation on the given push-down machine.
   *
   * <p>Evaluates the amount operation to get the size of the age-N cohort, builds a year
   * matcher from the optional during clause, and checks if this operation should execute in
   * the current year. If the year is in range, validates that retire commands are not mixed
   * with and without replacement in the same step, then retires the cohort, replacing it to
   * maintain population when {@code withReplacement} is set.</p>
   *
   * @param machine The push-down machine context for evaluating operations.
   */
  @Override
  public void execute(PushDownMachine machine) {
    amountOperation.execute(machine);
    final EngineNumber result = machine.getResult();

    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();

    if (!EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear())) {
      return;
    }

    EngineSupportUtils.ensureConsistentReplacement(engine, withReplacement);

    if (withReplacement) {
      EngineSupportUtils.retireWithReplacement(engine, result, yearMatcher);
    } else {
      engine.retire(result, yearMatcher);
    }
  }
}
