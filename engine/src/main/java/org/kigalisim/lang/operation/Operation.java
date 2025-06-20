/**
 * Interface for a calculation which is resolved to an EngineNumber at QubecTalk runtime.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import org.kigalisim.lang.machine.PushDownMachine;


/**
 * Description of a calculation which can be resolved to an EngineNumber at QubecTalk runtime.
 */
public interface Operation {

  /**
   * Execute the calculation and leave the result at the top of the stack.
   *
   * @param machine The machine in which to execute the calculation if needed.
   */
  void execute(PushDownMachine machine);

}
