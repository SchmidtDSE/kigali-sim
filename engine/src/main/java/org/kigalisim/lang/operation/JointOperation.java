/**
 * Operation which combines two other operations.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;


/**
 * Conjunction operation which performs one operation followed by another.
 */
public class JointOperation implements Operation {

  private final Operation inner;
  private final Operation outer;

  /**
   * Create a new joint operation which persoms one followed by another.
   *
   * @param inner The first operation to perform.
   * @param outer The second opeation to perform.
   */
  public JointOperation(Operation inner, Operation outer) {
    this.inner = inner;
    this.outer = outer;
  }

  @Override
  public void execute(PushDownMachine machine) {
    inner.execute(machine);
    outer.execute(machine);
  }

}
