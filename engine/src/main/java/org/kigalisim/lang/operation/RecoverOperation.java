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
  private final Optional<ParsedDuring> duringMaybe;
  private final RecoveryStage stage;
  private final Optional<Operation> inductionOperation;

  /**
   * Create a new RecoverOperation that applies to all years.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.duringMaybe = Optional.empty();
    this.stage = RecoveryStage.RECHARGE;
    this.inductionOperation = Optional.empty();
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
    this.duringMaybe = Optional.empty();
    this.stage = stage;
    this.inductionOperation = Optional.empty();
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
    this.duringMaybe = Optional.of(during);
    this.stage = RecoveryStage.RECHARGE;
    this.inductionOperation = Optional.empty();
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
    this.duringMaybe = Optional.of(during);
    this.stage = stage;
    this.inductionOperation = Optional.empty();
  }

  /**
   * Create a new RecoverOperation that applies to all years with induction.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param inductionOperation The operation that calculates the induction rate (Optional.empty() for default).
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, Optional<Operation> inductionOperation) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.duringMaybe = Optional.empty();
    this.stage = RecoveryStage.RECHARGE;
    this.inductionOperation = inductionOperation;
  }

  /**
   * Create a new RecoverOperation that applies to all years with stage and induction.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param stage The recovery stage (EOL or RECHARGE).
   * @param inductionOperation The operation that calculates the induction rate (Optional.empty() for default).
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, RecoveryStage stage, Optional<Operation> inductionOperation) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.duringMaybe = Optional.empty();
    this.stage = stage;
    this.inductionOperation = inductionOperation;
  }

  /**
   * Create a new RecoverOperation that applies to a specific time period with induction.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param during The time period during which this operation applies.
   * @param inductionOperation The operation that calculates the induction rate (Optional.empty() for default).
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, ParsedDuring during, Optional<Operation> inductionOperation) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.duringMaybe = Optional.of(during);
    this.stage = RecoveryStage.RECHARGE;
    this.inductionOperation = inductionOperation;
  }

  /**
   * Create a new RecoverOperation that applies to a specific time period with stage and induction.
   *
   * @param volumeOperation The operation that calculates the recovery amount.
   * @param yieldOperation The operation that calculates the yield rate.
   * @param during The time period during which this operation applies.
   * @param stage The recovery stage (EOL or RECHARGE).
   * @param inductionOperation The operation that calculates the induction rate (Optional.empty() for default).
   */
  public RecoverOperation(Operation volumeOperation, Operation yieldOperation, ParsedDuring during, RecoveryStage stage, Optional<Operation> inductionOperation) {
    this.volumeOperation = volumeOperation;
    this.yieldOperation = yieldOperation;
    this.duringMaybe = Optional.of(during);
    this.stage = stage;
    this.inductionOperation = inductionOperation;
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
    final EngineNumber recoveryAmount = machine.getResult();

    // Execute the yield operation to get the yield rate
    yieldOperation.execute(machine);
    final EngineNumber yieldRate = machine.getResult();

    // Execute the induction operation if present
    Optional<Double> inductionRate = Optional.empty();
    if (inductionOperation.isPresent()) {
      inductionOperation.get().execute(machine);
      EngineNumber inductionValue = machine.getResult();
      // Validate that the induction value is a percentage (0-100%)
      double induction = inductionValue.getValue().doubleValue();
      if (induction < 0 || induction > 100) {
        throw new IllegalArgumentException("Induction rate must be between 0% and 100%, got: " + induction + "%");
      }
      inductionRate = Optional.of(induction / 100.0); // Convert percentage to decimal
    }

    // Build the year matcher
    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    // Get the engine and set the induction rate
    Engine engine = machine.getEngine();
    engine.setInductionRate(inductionRate, stage);

    // Call the recycle method on the engine
    engine.recycle(recoveryAmount, yieldRate, yearMatcher, stage);
  }
}
