/**
 * A future timepoint which resolves to a calculated value.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.time;

import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.operation.Operation;


/**
 * A future timepoint which resolves to a calculated value.
 *
 * <p>A future timepoint which resolves to a value calculated through a PushDownMachine and expects
 * that value to not yet have units.</p>
 */
public class CalculatedTimePointFuture implements TimePointFuture {

  private final Operation operation;

  /**
   * Create a new CalculatedTimePointFuture.
   *
   * @param operation The calculation to perform to resolve this future.
   */
  public CalculatedTimePointFuture(Operation operation) {
    this.operation = operation;
  }

  @Override
  public TimePointRealized realize(PushDownMachine machine) {
    operation.execute(machine);
    EngineNumber result = machine.getResult();
    if (!result.getUnits().isEmpty()) {
      throw new IllegalStateException(
          "Calculated result for a timepoint already had units: " + result.getUnits()
      );
    }
    return new TimePointRealized(result);
  }

}
