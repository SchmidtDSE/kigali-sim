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

  /**
   * Enum representing the stage of recovery.
   */
  public enum RecoveryStage {
    EOL,
    RECHARGE
  }

  private final Operation volumeOperation;
  private final Operation yieldOperation;
  private final Optional<String> displaceTarget;
  private final Optional<ParsedDuring> duringMaybe;
  private final RecoveryStage stage;

  /**
   * Create a new RecoverOperation that applies to all years.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displaceTarget = Optional.empty();
    this.duringMaybe = Optional.empty();
    this.stage = RecoveryStage.RECHARGE;
  }

  /**
   * Create a new RecoverOperation that applies to all years with stage.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param stage The recovery stage (EOL or RECHARGE).
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, RecoveryStage stage) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displaceTarget = Optional.empty();
    this.duringMaybe = Optional.empty();
    this.stage = stage;
  }

  /**
   * Create a new RecoverOperation that applies to all years with displacement.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param displaceTarget The name of the stream or substance to displace.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, String displaceTarget) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displaceTarget = Optional.ofNullable(displaceTarget);
    this.duringMaybe = Optional.empty();
    this.stage = RecoveryStage.RECHARGE;
  }

  /**
   * Create a new RecoverOperation that applies to all years with displacement and stage.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param displaceTarget The name of the stream or substance to displace.
   * @param stage The recovery stage (EOL or RECHARGE).
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, String displaceTarget, RecoveryStage stage) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displaceTarget = Optional.ofNullable(displaceTarget);
    this.duringMaybe = Optional.empty();
    this.stage = stage;
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
    this.displaceTarget = Optional.empty();
    this.duringMaybe = Optional.of(during);
    this.stage = RecoveryStage.RECHARGE;
  }

  /**
   * Create a new RecoverOperation that applies to a specific time period with stage.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param during The time period during which this operation applies.
   * @param stage The recovery stage (EOL or RECHARGE).
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, ParsedDuring during, RecoveryStage stage) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displaceTarget = Optional.empty();
    this.duringMaybe = Optional.of(during);
    this.stage = stage;
  }

  /**
   * Create a new RecoverOperation that applies to a specific time period with displacement.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param displaceTarget The name of the stream or substance to displace.
   * @param during The time period during which this operation applies.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation,
                         String displaceTarget, ParsedDuring during) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displaceTarget = Optional.ofNullable(displaceTarget);
    this.duringMaybe = Optional.of(during);
    this.stage = RecoveryStage.RECHARGE;
  }

  /**
   * Create a new RecoverOperation that applies to a specific time period with displacement and stage.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param displaceTarget The name of the stream or substance to displace.
   * @param during The time period during which this operation applies.
   * @param stage The recovery stage (EOL or RECHARGE).
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation,
                         String displaceTarget, ParsedDuring during, RecoveryStage stage) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.displaceTarget = Optional.ofNullable(displaceTarget);
    this.duringMaybe = Optional.of(during);
    this.stage = stage;
  }

  /**
   * Get the recovery stage.
   *
   * @return The recovery stage (EOL or RECHARGE).
   */
  public RecoveryStage getStage() {
    return stage;
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

    // Call the appropriate recycle method on the engine
    Engine engine = machine.getEngine();
    if (displaceTarget.isPresent()) {
      engine.recycle(recoveryAmount, yieldRate, yearMatcher, displaceTarget.get(), stage);
    } else {
      engine.recycle(recoveryAmount, yieldRate, yearMatcher, stage);
    }
  }
}
