/**
 * Calculation which changes a stream value by a specified amount.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.time.ParsedDuring;

/**
 * Operation that changes a stream value by a specified amount.
 *
 * <p>This operation calculates a value and changes a specified stream in the engine by that amount.
 * It can optionally be limited to a specific time period using a ParsedDuring object.</p>
 */
public class ChangeOperation implements Operation {

  private final String stream;
  private final Operation valueOperation;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new ChangeOperation that applies to all years.
   *
   * @param stream The name of the stream to change.
   * @param valueOperation The operation that calculates the amount to change by.
   */
  public ChangeOperation(String stream, Operation valueOperation) {
    this.stream = stream;
    this.valueOperation = valueOperation;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new ChangeOperation that applies to a specific time period.
   *
   * @param stream The name of the stream to change.
   * @param valueOperation The operation that calculates the amount to change by.
   * @param during The time period during which this operation applies.
   */
  public ChangeOperation(String stream, Operation valueOperation, ParsedDuring during) {
    this.stream = stream;
    this.valueOperation = valueOperation;
    duringMaybe = Optional.of(during);
  }

  @Override
  public void execute(PushDownMachine machine) {
    valueOperation.execute(machine);
    EngineNumber result = machine.getResult();

    // Handle special "each year" units by extracting the actual unit
    String units = result.getUnits();
    if (units != null && units.endsWith("eachyear")) {
      String actualUnit = units.replace("eachyear", "");
      result = new EngineNumber(result.getValue(), actualUnit);
    }

    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();
    engine.changeStream(stream, result, yearMatcher);
  }
}
