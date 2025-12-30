/**
 * Operation to force a change in units while maintaining value.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;


/**
 * Operation which maintains the same value but forces a change to units.
 *
 * <p>Operation which takes the top value of a push-down machine and forces it to have a
 * specific units value while maintaining the same numeric value.</p>
 */
public class RemoveUnitsOperation implements Operation {

  @Override
  public void execute(PushDownMachine machine) {
    machine.changeUnits("", true);
  }

}
