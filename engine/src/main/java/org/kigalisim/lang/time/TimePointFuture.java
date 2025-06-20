/**
 * Description of a time point which can be resolved at QubecTalk runtime.
 *
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.time;

import org.kigalisim.lang.machine.PushDownMachine;


/**
 * Time point which can be resolved at QubecTalk runtime.
 *
 * <p>Time point which can be calculated or "realized" during the execution of the QubecTalk
 * runtime, using a machine for calculations if needed.</p>
 */
public interface TimePointFuture {

  /**
   * Realize the time point using the given machine.
   *
   * @param machine Machine to use for calculations if needed.
   * @return The realized time point.
   */
  TimePointRealized realize(PushDownMachine machine);

}
