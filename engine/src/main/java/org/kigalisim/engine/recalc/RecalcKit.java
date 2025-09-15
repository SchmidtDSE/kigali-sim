/**
 * Container for dependencies needed by recalculation strategies.
 *
 * <p>This class provides all the dependencies that recalculation strategies need
 * to perform their operations, eliminating the need to cast Engine to SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.StateGetter;

/**
 * Container for dependencies needed by recalculation strategies.
 */
public class RecalcKit {

  private final SimulationState streamKeeper;
  private final UnitConverter unitConverter;
  private final StateGetter stateGetter;

  /**
   * Create a new RecalcKit.
   *
   * @param streamKeeper The stream keeper instance (required)
   * @param unitConverter The unit converter instance (required)
   * @param stateGetter The state getter instance (required)
   */
  public RecalcKit(SimulationState streamKeeper, UnitConverter unitConverter,
      StateGetter stateGetter) {
    this.streamKeeper = streamKeeper;
    this.unitConverter = unitConverter;
    this.stateGetter = stateGetter;
  }

  /**
   * Get the stream keeper.
   *
   * @return The stream keeper
   */
  public SimulationState getStreamKeeper() {
    return streamKeeper;
  }

  /**
   * Get the unit converter.
   *
   * @return The unit converter
   */
  public UnitConverter getUnitConverter() {
    return unitConverter;
  }

  /**
   * Get the state getter.
   *
   * @return The state getter
   */
  public StateGetter getStateGetter() {
    return stateGetter;
  }
}
