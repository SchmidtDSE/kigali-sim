package org.kigalisim.lang.operation;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.time.ParsedDuring;


/**
 * Operation that sets a consumption value in the engine.
 *
 * <p>This operation calculates a value and sets it as a consumption value in the engine.
 * It can optionally be limited to a specific time period using a ParsedDuring object.</p>
 */
public class EqualsOperation implements Operation {

  private final Operation valueOperation;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new EqualsOperation that applies to all years.
   *
   * @param valueOperation The operation that calculates the value to set.
   */
  public EqualsOperation(Operation valueOperation) {
    this.valueOperation = valueOperation;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new EqualsOperation that applies to a specific time period.
   *
   * @param valueOperation The operation that calculates the value to set.
   * @param during The time period during which this operation applies.
   */
  public EqualsOperation(Operation valueOperation, ParsedDuring during) {
    this.valueOperation = valueOperation;
    duringMaybe = Optional.of(during);
  }

  @Override
  public void execute(PushDownMachine machine) {
    valueOperation.execute(machine);
    EngineNumber result = machine.getResult();

    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();
    engine.equals(result, yearMatcher);
  }
}
