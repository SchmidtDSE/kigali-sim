package org.kigalisim.lang.operation;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.time.ParsedDuring;


/**
 * Operation that sets a value to a stream in the engine.
 *
 * <p>This operation calculates a value and sets it to a specified stream in the engine.
 * It can optionally be limited to a specific time period using a ParsedDuring object.</p>
 */
public class SetOperation implements Operation {

  private final String stream;
  private final Operation valueOperation;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new SetOperation that applies to all years.
   *
   * @param stream The name of the stream to set.
   * @param valueOperation The operation that calculates the value to set.
   */
  public SetOperation(String stream, Operation valueOperation) {
    this.stream = stream;
    this.valueOperation = valueOperation;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new SetOperation that applies to a specific time period.
   *
   * @param stream The name of the stream to set.
   * @param valueOperation The operation that calculates the value to set.
   * @param during The time period during which this operation applies.
   */
  public SetOperation(String stream, Operation valueOperation, ParsedDuring during) {
    this.stream = stream;
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
    engine.setStream(stream, result, yearMatcher);
  }

}
