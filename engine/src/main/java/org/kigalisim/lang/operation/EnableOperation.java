package org.kigalisim.lang.operation;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.time.ParsedDuring;


/**
 * Operation that enables a stream in the engine without setting a value.
 *
 * <p>This operation marks a stream as enabled, allowing it to be used in distribution
 * calculations for operations like recharge, retire, and recover without having to
 * set an actual value to the stream.</p>
 */
public class EnableOperation implements Operation {

  private final String stream;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new EnableOperation that applies to all years.
   *
   * @param stream The name of the stream to enable.
   */
  public EnableOperation(String stream) {
    this.stream = stream;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new EnableOperation that applies to a specific time period.
   *
   * @param stream The name of the stream to enable.
   * @param during The time period during which this operation applies.
   */
  public EnableOperation(String stream, ParsedDuring during) {
    this.stream = stream;
    duringMaybe = Optional.of(during);
  }

  @Override
  public void execute(PushDownMachine machine) {
    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();
    engine.enable(stream, Optional.ofNullable(yearMatcher));
  }

}