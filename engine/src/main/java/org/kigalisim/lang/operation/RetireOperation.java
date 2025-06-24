package org.kigalisim.lang.operation;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.YearMatcher;
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

  @Override
  public void execute(PushDownMachine machine) {
    amountOperation.execute(machine);
    EngineNumber result = machine.getResult();

    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();
    engine.retire(result, yearMatcher);
  }
}
