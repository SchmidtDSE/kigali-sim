/**
 * Utility class for executing stream update operations.
 *
 * <p>This class provides stream update logic for Engine implementations
 * to provide better separation of concerns and testability. It handles
 * stream combination tracking for unit-based carry-over operations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;

/**
 * Handles stream update operations for sales carry-over logic.
 *
 * <p>This class provides methods to handle sales stream combinations
 * when setting domestic or import streams with unit-based values.</p>
 */
public class StreamUpdateExecutor {

  private final Engine engine;

  /**
   * Creates a new StreamUpdateExecutor for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public StreamUpdateExecutor(Engine engine) {
    this.engine = engine;
  }

  /**
   * Handles stream combinations for unit preservation during carry-over operations.
   *
   * <p>When setting manufacture (domestic) or import streams with unit-based values,
   * this method combines them to create a unified sales intent. This ensures that
   * when both domestic and import are specified in units, the total sales value
   * correctly reflects the combined equipment count. This combination is critical
   * for maintaining accurate recharge calculations across carry-over years.</p>
   *
   * <p>Only processes unit-based specifications for domestic and import streams.
   * Volume-based (kg/mt) specifications are handled separately and do not require
   * combination logic.</p>
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream being set (must be "domestic" or "import")
   * @param value The value being set with equipment units
   */
  public void updateSalesCarryOver(UseKey useKey, String streamName, EngineNumber value) {
    // Only process unit-based values for combination tracking
    if (!value.hasEquipmentUnits()) {
      return;
    }

    // Only handle manufacture and import streams for combination
    if (!EngineSupportUtils.isSalesSubstream(streamName)) {
      return;
    }

    SimulationState simulationState = engine.getStreamKeeper();

    // When setting manufacture or import, combine with the other to create sales intent
    String otherStream = "domestic".equals(streamName) ? "import" : "domestic";
    EngineNumber otherValue = simulationState.getLastSpecifiedValue(useKey, otherStream);

    if (otherValue != null && otherValue.hasEquipmentUnits()) {
      // Both streams have unit-based values - combine them
      // Convert both to the same units (prefer the current stream's units)
      String targetUnits = value.getUnits();
      UnitConverter converter = EngineSupportUtils.createUnitConverterWithTotal(engine, streamName);
      EngineNumber otherConverted = converter.convert(otherValue, targetUnits);

      // Create combined sales value
      BigDecimal combinedValue = value.getValue().add(otherConverted.getValue());
      EngineNumber salesIntent = new EngineNumber(combinedValue, targetUnits);

      // Track the combined sales intent
      simulationState.setLastSpecifiedValue(useKey, "sales", salesIntent);
    } else {
      // Only one stream has units - use it as the sales intent
      simulationState.setLastSpecifiedValue(useKey, "sales", value);
    }
  }
}
