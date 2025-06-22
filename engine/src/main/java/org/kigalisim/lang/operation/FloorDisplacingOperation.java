/**
 * Calculation which floors a stream value to a specified minimum and displaces the excess.
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
 * Operation that floors a stream value to a specified minimum and displaces the excess.
 *
 * <p>This operation calculates a value and floors a specified stream in the engine to that minimum.
 * The excess is displaced to another stream. It can optionally be limited to a specific time period
 * using a ParsedDuring object.</p>
 */
public class FloorDisplacingOperation implements Operation {

  private final String stream;
  private final Operation valueOperation;
  private final String displaceTarget;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new FloorDisplacingOperation that applies to all years.
   *
   * @param stream The name of the stream to floor.
   * @param valueOperation The operation that calculates the minimum value.
   * @param displaceTarget The name of the stream to displace excess to.
   */
  public FloorDisplacingOperation(String stream, Operation valueOperation, String displaceTarget) {
    this.stream = stream;
    this.valueOperation = valueOperation;
    this.displaceTarget = displaceTarget;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new FloorDisplacingOperation that applies to a specific time period.
   *
   * @param stream The name of the stream to floor.
   * @param valueOperation The operation that calculates the minimum value.
   * @param displaceTarget The name of the stream to displace excess to.
   * @param during The time period during which this operation applies.
   */
  public FloorDisplacingOperation(String stream, Operation valueOperation, 
                                 String displaceTarget, ParsedDuring during) {
    this.stream = stream;
    this.valueOperation = valueOperation;
    this.displaceTarget = displaceTarget;
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
    engine.floor(stream, result, yearMatcher, displaceTarget);
  }
}