/**
 * Calculator class for consumption stream calculations.
 *
 * <p>This class replaces the BiConsumer logic previously used in SingleThreadEngine
 * for calculating and saving consumption streams. It encapsulates the logic for
 * converting consumption values to appropriate units and ensuring values are within
 * valid ranges before saving them to the engine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;

/**
 * Calculator class for consumption stream calculations.
 *
 * <p>This class encapsulates the logic for calculating consumption streams,
 * including unit conversion, range validation, and saving results to the engine.
 * It provides a cleaner interface compared to the previous BiConsumer approach.</p>
 */
public class ConsumptionCalculator {

  private EngineNumber consumptionRaw;
  private String streamName;

  /**
   * Set the raw consumption value to be processed.
   *
   * @param consumptionRaw The raw consumption value
   */
  public void setConsumptionRaw(EngineNumber consumptionRaw) {
    this.consumptionRaw = consumptionRaw;
  }

  /**
   * Set the stream name for the calculation.
   *
   * @param streamName The name of the stream ("consumption" or "energy")
   */
  public void setStreamName(String streamName) {
    this.streamName = streamName;
  }

  /**
   * Execute the consumption calculation and save the result to the engine.
   *
   * <p>This method handles the complete calculation process including setting up
   * the state, performing unit conversion, ensuring values are in valid ranges,
   * and saving the result to the engine.</p>
   *
   * @param engine The engine instance to use for calculation context and to save the result to
   * @throws IllegalStateException if required properties are not set
   * @throws IllegalArgumentException if the engine is not a SingleThreadEngine
   */
  public void execute(Engine engine) {
    validateState();

    if (!(engine instanceof SingleThreadEngine)) {
      throw new IllegalArgumentException("ConsumptionCalculator requires a SingleThreadEngine");
    }

    SingleThreadEngine singleThreadEngine = (SingleThreadEngine) engine;

    // Get current scope
    Scope scopeEffective = singleThreadEngine.getScope();

    // Set up converters
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(singleThreadEngine.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);

    // Get sales for conversion context
    EngineNumber salesRaw = engine.getStream("sales", scopeEffective, null);
    EngineNumber sales = unitConverter.convert(salesRaw, "kg");

    // Determine consumption
    stateGetter.setVolume(sales);
    String targetUnits = streamName.equals("consumption") ? "tCO2e" : "kwh";
    EngineNumber consumption = unitConverter.convert(consumptionRaw, targetUnits);
    stateGetter.clearVolume();

    // Ensure in range
    boolean isNegative = consumption.getValue().compareTo(BigDecimal.ZERO) < 0;
    EngineNumber consumptionAllowed;
    if (isNegative) {
      consumptionAllowed = new EngineNumber(BigDecimal.ZERO, consumption.getUnits());
    } else {
      consumptionAllowed = consumption;
    }

    // Save
    engine.setStream(streamName, consumptionAllowed, null, scopeEffective, false, null);
  }

  /**
   * Validate that all required properties are set before execution.
   *
   * @throws IllegalStateException if any required property is null
   */
  private void validateState() {
    if (consumptionRaw == null) {
      throw new IllegalStateException("consumptionRaw must be set");
    }
    if (streamName == null) {
      throw new IllegalStateException("streamName must be set");
    }
  }
}
