/**
 * Calculation which services (recharges or precharges) equipment with a specified volume and intensity.
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
 * Operation that services equipment with a specified volume and intensity.
 *
 * <p>This operation calculates a servicing volume and intensity and applies it to the engine.
 * It can optionally be limited to a specific time period using a ParsedDuring object. The target
 * parameter determines whether this operates on prior equipment (recharge) or new equipment
 * (precharge).</p>
 */
public class RechargeOperation implements Operation {

  /** Target stream name for servicing existing (prior) equipment, producing a recharge. */
  public static final String PRIOR = "priorEquipment";

  /** Target stream name for servicing new equipment before sale, producing a precharge. */
  public static final String NEW = "newEquipment";

  private final Operation volumeOperation;
  private final Operation intensityOperation;
  private final Optional<ParsedDuring> duringMaybe;
  private final String target;

  /**
   * Create a new RechargeOperation with explicit target and optional time period.
   *
   * @param volumeOperation The operation that calculates the servicing volume.
   * @param intensityOperation The operation that calculates the servicing intensity.
   * @param duringMaybe The optional time period during which this operation applies, or empty
   *     for all years.
   * @param target The target stream ({@link #PRIOR} for recharge, {@link #NEW} for precharge).
   */
  public RechargeOperation(Operation volumeOperation, Operation intensityOperation,
      Optional<ParsedDuring> duringMaybe, String target) {
    this.volumeOperation = volumeOperation;
    this.intensityOperation = intensityOperation;
    this.duringMaybe = duringMaybe;
    this.target = target;
  }

  /** {@inheritDoc} */
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
    engine.recharge(volumeResult, intensityResult, yearMatcher, target);
  }
}
