package org.kigalisim.lang.operation;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.time.ParsedDuring;


/**
 * Operation that recharges equipment with a specified volume and intensity.
 *
 * <p>This operation calculates a recharge volume and intensity and applies it to the engine.
 * It can optionally be limited to a specific time period using a ParsedDuring object.</p>
 */
public class RechargeOperation implements Operation {

  private final Operation volumeOperation;
  private final Operation intensityOperation;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new RechargeOperation that applies to all years.
   *
   * @param volumeOperation The operation that calculates the recharge volume.
   * @param intensityOperation The operation that calculates the recharge intensity.
   */
  public RechargeOperation(Operation volumeOperation, Operation intensityOperation) {
    this.volumeOperation = volumeOperation;
    this.intensityOperation = intensityOperation;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new RechargeOperation that applies to a specific time period.
   *
   * @param volumeOperation The operation that calculates the recharge volume.
   * @param intensityOperation The operation that calculates the recharge intensity.
   * @param during The time period during which this operation applies.
   */
  public RechargeOperation(Operation volumeOperation, Operation intensityOperation, 
      ParsedDuring during) {
    this.volumeOperation = volumeOperation;
    this.intensityOperation = intensityOperation;
    duringMaybe = Optional.of(during);
  }

  @Override
  public void execute(PushDownMachine machine) {
    volumeOperation.execute(machine);
    EngineNumber volumeResult = machine.getResult();

    intensityOperation.execute(machine);
    EngineNumber intensityResult = machine.getResult();

    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();
    engine.recharge(volumeResult, intensityResult, yearMatcher);
  }
}
