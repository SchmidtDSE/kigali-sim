/**
 * Structure holding an EngineNumber calculated prior to typical runtime calculation.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;


/**
 * Description of a calculation that has already been resolved to an EngineNumber.
 */
public class PreCalculatedOperation implements Operation {

  private final EngineNumber result;

  /**
   * Create a new PreCalculatedOperation.
   *
   * @param result The precomputed result of the calculation.
   */
  public PreCalculatedOperation(EngineNumber result) {
    this.result = result;
  }

  @Override
  public void execute(PushDownMachine engine) {
    engine.push(result);
  }
}
