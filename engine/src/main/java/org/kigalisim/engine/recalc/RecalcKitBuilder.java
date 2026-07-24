/**
 * Builder for creating RecalcKit instances.
 *
 * <p>This builder provides a fluent interface for constructing RecalcKit instances
 * with default empty Optional values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.util.Optional;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.StateGetter;
import org.kigalisim.engine.support.ValidatedBuilder;

/**
 * Builder for creating RecalcKit instances.
 */
public class RecalcKitBuilder extends ValidatedBuilder<RecalcKit> {

  private Optional<SimulationState> simulationState;
  private Optional<UnitConverter> unitConverter;
  private Optional<StateGetter> stateGetter;

  /**
   * Create a new RecalcKitBuilder.
   */
  public RecalcKitBuilder() {
    super("RecalcKit");
    this.simulationState = Optional.empty();
    this.unitConverter = Optional.empty();
    this.stateGetter = Optional.empty();
  }

  /**
   * Set the simulation state.
   *
   * @param simulationState The simulation state to set
   * @return This builder for chaining
   */
  public RecalcKitBuilder setStreamKeeper(SimulationState simulationState) {
    this.simulationState = Optional.of(simulationState);
    return this;
  }

  /**
   * Set the unit converter.
   *
   * @param unitConverter The unit converter to set
   * @return This builder for chaining
   */
  public RecalcKitBuilder setUnitConverter(UnitConverter unitConverter) {
    this.unitConverter = Optional.of(unitConverter);
    return this;
  }

  /**
   * Set the state getter.
   *
   * @param stateGetter The state getter to set
   * @return This builder for chaining
   */
  public RecalcKitBuilder setStateGetter(StateGetter stateGetter) {
    this.stateGetter = Optional.of(stateGetter);
    return this;
  }

  /**
   * Build the RecalcKit.
   *
   * @return A new RecalcKit instance
   */
  @Override
  protected RecalcKit buildInternal() {
    return new RecalcKit(simulationState.get(), unitConverter.get(), stateGetter.get());
  }

  /**
   * Check that all required fields are set before construction.
   *
   * @throws IllegalStateException if any required field is missing
   */
  @Override
  protected void validate() {
    requireField(simulationState, "simulationState");
    requireField(unitConverter, "unitConverter");
    requireField(stateGetter, "stateGetter");
  }
}
