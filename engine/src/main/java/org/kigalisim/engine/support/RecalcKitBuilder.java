/**
 * Builder for creating RecalcKit instances.
 *
 * <p>This builder provides a fluent interface for constructing RecalcKit instances
 * with default empty Optional values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.StreamKeeper;

/**
 * Builder for creating RecalcKit instances.
 */
public class RecalcKitBuilder {

  private Optional<StreamKeeper> streamKeeper;
  private Optional<UnitConverter> unitConverter;
  private Optional<ConverterStateGetter> stateGetter;

  /**
   * Create a new RecalcKitBuilder.
   */
  public RecalcKitBuilder() {
    this.streamKeeper = Optional.empty();
    this.unitConverter = Optional.empty();
    this.stateGetter = Optional.empty();
  }

  /**
   * Set the stream keeper.
   *
   * @param streamKeeper The stream keeper to set
   * @return This builder for chaining
   */
  public RecalcKitBuilder setStreamKeeper(StreamKeeper streamKeeper) {
    this.streamKeeper = Optional.of(streamKeeper);
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
  public RecalcKitBuilder setStateGetter(ConverterStateGetter stateGetter) {
    this.stateGetter = Optional.of(stateGetter);
    return this;
  }

  /**
   * Build the RecalcKit.
   *
   * @return A new RecalcKit instance
   * @throws IllegalStateException if any required field is missing
   */
  public RecalcKit build() {
    if (streamKeeper.isEmpty()) {
      throw new IllegalStateException("StreamKeeper is required");
    }
    if (unitConverter.isEmpty()) {
      throw new IllegalStateException("UnitConverter is required");
    }
    if (stateGetter.isEmpty()) {
      throw new IllegalStateException("ConverterStateGetter is required");
    }
    return new RecalcKit(streamKeeper.get(), unitConverter.get(), stateGetter.get());
  }
}