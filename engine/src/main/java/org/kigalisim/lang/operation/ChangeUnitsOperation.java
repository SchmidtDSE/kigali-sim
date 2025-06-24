/**
 * Calculation which changes the units of an EngineNumber.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;


import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Description of a calculation which changes the units of an EngineNumber.
 *
 * <p>Description of a calculation which changes the units of an EngineNumber at the top of the
 * stack, requiring that it either is empty (no units assigned) or is the same as the desired
 * units.</p>
 */
public class ChangeUnitsOperation implements Operation {

  private final Operation operand;
  private final String units;

  /**
   * Create a new ChangeUnitsOperation.
   *
   * @param operand The operand of the calculation.
   * @param units The units to change to.
   */
  public ChangeUnitsOperation(Operation operand, String units) {
    this.operand = operand;
    this.units = units;
  }

  @Override
  public void execute(PushDownMachine machine) {
    operand.execute(machine);
    machine.changeUnits(units);
  }

}
