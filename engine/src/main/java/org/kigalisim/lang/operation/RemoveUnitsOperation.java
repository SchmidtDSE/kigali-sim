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

  private static final OperationStaticSemantics STATIC_SEMANTICS = new OperationStaticSemantics(
      OperationType.REMOVE_UNITS
  );

  @Override
  public void execute(PushDownMachine machine) {
    machine.changeUnits("", true);
  }

  /** {@inheritDoc} */
  @Override
  public OperationStaticSemantics getStaticSemantics() {
    return STATIC_SEMANTICS;
  }
}
