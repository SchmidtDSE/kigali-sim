/**
 * Operation that recovers a percentage or amount of refrigerant.
 *
 * <p>This operation calculates a recovery amount and yield rate
 * and applies it to the engine. It can optionally be limited to a specific time period using
 * a ParsedDuring object.</p>
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
 * Operation that recovers a percentage or amount of refrigerant.
 *
 * <p>This operation calculates a recovery amount and yield rate
 * and applies it to the engine. It can optionally be limited to a specific time period using
 * a ParsedDuring object.</p>
 */
public class RecoverOperation implements Operation {

  private final Operation volumeOperation;
  private final Operation yieldOperation;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new RecoverOperation that applies to all years.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new RecoverOperation that applies to a specific time period.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param during The time period during which this operation applies.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, ParsedDuring during) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    duringMaybe = Optional.of(during);
  }

  @Override
  public void execute(PushDownMachine machine) {
    // Execute the volume operation to get the recovery amount
    volumeOperation.execute(machine);
    EngineNumber recoveryAmount = machine.getResult();

    // Execute the yield operation to get the yield rate
    yieldOperation.execute(machine);
    EngineNumber yieldRate = machine.getResult();

    // Build the year matcher
    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    // Call the recycle method on the engine
    Engine engine = machine.getEngine();
    engine.recycle(recoveryAmount, yieldRate, yearMatcher);
  }
}
