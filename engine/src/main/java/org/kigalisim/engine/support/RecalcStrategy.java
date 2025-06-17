/**
 * Strategy interface for recalculation operations.
 *
 * <p>This interface defines the contract for recalculation strategies that can be
 * executed on an Engine instance. Each strategy encapsulates a specific type of
 * recalculation logic that was previously implemented as private methods in
 * SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import org.kigalisim.engine.Engine;

/**
 * Strategy interface for recalculation operations.
 */
public interface RecalcStrategy {

  /**
   * Execute the recalculation strategy on the given engine.
   *
   * @param target The engine on which to execute the recalculation
   */
  void execute(Engine target);
}
