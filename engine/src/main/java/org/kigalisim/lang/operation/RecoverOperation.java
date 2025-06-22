/**
 * Operation that recovers a percentage or amount of refrigerant and optionally displaces it.
 *
 * <p>This operation calculates a recovery amount, yield rate, and optional displacement amount
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
 * Operation that recovers a percentage or amount of refrigerant and optionally displaces it.
 *
 * <p>This operation calculates a recovery amount, yield rate, and optional displacement amount
 * and applies it to the engine. It can optionally be limited to a specific time period using
 * a ParsedDuring object.</p>
 */
public class RecoverOperation implements Operation {

  private final Operation volumeOperation;
  private final Operation yieldOperation;
  private final Optional<Operation> displacementOperation;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new RecoverOperation that applies to all years without displacement.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displacementOperation = Optional.empty();
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new RecoverOperation that applies to a specific time period without displacement.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param during The time period during which this operation applies.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, ParsedDuring during) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displacementOperation = Optional.empty();
    duringMaybe = Optional.of(during);
  }

  /**
   * Create a new RecoverOperation that applies to all years with displacement.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param displacementOperation The operation that calculates the displacement amount.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation,
                         Operation displacementOperation) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displacementOperation = Optional.of(displacementOperation);
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new RecoverOperation that applies to a specific time period with displacement.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param displacementOperation The operation that calculates the displacement amount.
   * @param during The time period during which this operation applies.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation,
                         Operation displacementOperation, ParsedDuring during) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displacementOperation = Optional.of(displacementOperation);
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

    // Execute the displacement operation to get the displacement amount (if provided)
    EngineNumber displacementAmount = null;
    if (displacementOperation.isPresent()) {
      displacementOperation.get().execute(machine);
      displacementAmount = machine.getResult();
    }

    // Build the year matcher
    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    // Call the recycle method on the engine
    Engine engine = machine.getEngine();
    engine.recycle(recoveryAmount, yieldRate, displacementAmount, yearMatcher);
  }
}