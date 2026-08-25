/**
 * Calculation which retires a percentage of equipment each year.
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
 * Operation that retires a percentage of equipment each year.
 *
 * <p>This operation calculates a retirement rate and applies it to the engine.
 * It can optionally be limited to a specific time period using a ParsedDuring object.</p>
 */
public class RetireOperation implements Operation {

  private final Operation amountOperation;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new RetireOperation that applies to all years.
   *
   * @param amountOperation The operation that calculates the retirement rate.
   */
  public RetireOperation(Operation amountOperation) {
    this.amountOperation = amountOperation;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new RetireOperation that applies to a specific time period.
   *
   * @param amountOperation The operation that calculates the retirement rate.
   * @param during The time period during which this operation applies.
   */
  public RetireOperation(Operation amountOperation, ParsedDuring during) {
    this.amountOperation = amountOperation;
    duringMaybe = Optional.of(during);
  }

  /**
   * Execute the retire operation on the given push-down machine.
   *
   * <div>
   * Evaluates the amount operation to get the retirement rate, builds a year matcher from the
   * optional during clause, and checks if this operation should execute in the current year.
   * If the year is in range, validates that retire commands are not mixed with and without
   * replacement in the same step, then applies the retirement to the engine.
   * </div>
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

    // Check if this operation should execute in the current year
    if (!EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear())) {
      return;
    }

    handleMixedReplacement(engine);

    engine.retire(result, yearMatcher);
  }

  /**
   * Ensure retire commands are not mixed with and without replacement in the same step.
   *
   * <p>This non-replacement retire records a non-replacement step and rejects a mismatch
   * with any prior replacement retire in the same step.</p>
   *
   * @param engine The engine containing the current simulation state.
   */
  private void handleMixedReplacement(Engine engine) {
    EngineSupportUtils.ensureConsistentReplacement(engine, false);
  }
}
