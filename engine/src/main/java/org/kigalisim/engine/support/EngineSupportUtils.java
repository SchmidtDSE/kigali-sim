/**
 * Utility methods for engine support operations.
 *
 * <p>This class provides static utility methods that can be used by various
 * engine support classes without requiring access to private engine methods.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Static utility methods for engine operations.
 */
public final class EngineSupportUtils {

  /**
   * Set of valid stream names in the simulation engine.
   * Includes equipment streams, sales streams, and trade streams.
   */
  public static final Set<String> STREAM_NAMES = new HashSet<>();

  /**
   * The stream used for recycled material recovery operations.
   */
  public static final String RECYCLE_RECOVER_STREAM = "sales";

  static {
    STREAM_NAMES.add("priorEquipment");
    STREAM_NAMES.add("equipment");
    STREAM_NAMES.add("export");
    STREAM_NAMES.add("import");
    STREAM_NAMES.add("domestic");
    STREAM_NAMES.add("sales");
  }

  private EngineSupportUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Check if a year matcher is in range for the given current year.
   *
   * @param yearMatcher The year matcher to check (can be null)
   * @param currentYear The current year to check against
   * @return True if in range or no matcher provided
   */
  public static boolean getIsInRange(YearMatcher yearMatcher, int currentYear) {
    return yearMatcher == null || yearMatcher.getInRange(currentYear);
  }

  /**
   * Check if a year matcher is in range for the given current year.
   *
   * @param yearMatcher The optional year matcher to check
   * @param currentYear The current year to check against
   * @return True if in range or no matcher provided
   */
  public static boolean getIsInRange(Optional<YearMatcher> yearMatcher, int currentYear) {
    return getIsInRange(yearMatcher.orElse(null), currentYear);
  }

  /**
   * Check if a stream name represents a sales substream (domestic or import).
   *
   * <p>Sales substreams are the component streams that make up the overall sales stream,
   * representing domestic manufacturing and imported products.</p>
   *
   * @param name The stream name to check
   * @return true if the stream is domestic or import
   */
  public static boolean isSalesSubstream(String name) {
    return "domestic".equals(name) || "import".equals(name);
  }

  /**
   * Check if a stream is a sales-related stream.
   *
   * <p>A sales-related stream includes the core sales stream and its substreams (domestic and
   * import). Optionally, the export stream can be included depending on the context of the operation
   * being performed.</p>
   *
   * <p>This is the centralized implementation for stream classification logic and should be used
   * throughout the engine instead of local implementations to ensure consistent behavior.</p>
   *
   * @param stream The stream name to check
   * @param includeExports Whether to include export stream as a sales stream
   * @return true if the stream is a sales-related stream (sales, domestic, or import); or export
   *     if includeExports is true
   */
  public static boolean getIsSalesStream(String stream, boolean includeExports) {
    boolean isCoreStream = "sales".equals(stream) || isSalesSubstream(stream);
    return isCoreStream || (includeExports && "export".equals(stream));
  }

  /**
   * Creates a unit converter with total values initialized.
   *
   * @param stateGetter The converter state getter from the engine
   * @param stream The stream identifier to create converter for
   * @param currentValue The current stream value
   * @param initialCharge The initial charge (for sales substreams only, can be null)
   * @return A configured unit converter instance
   */
  public static UnitConverter createUnitConverterWithTotal(ConverterStateGetter stateGetter,
      String stream, EngineNumber currentValue, EngineNumber initialCharge) {
    OverridingConverterStateGetter overridingStateGetter =
        new OverridingConverterStateGetter(stateGetter);
    UnitConverter unitConverter = new UnitConverter(overridingStateGetter);

    overridingStateGetter.setTotal(stream, currentValue);

    if (isSalesSubstream(stream) && initialCharge != null) {
      overridingStateGetter.setAmortizedUnitVolume(initialCharge);
    }

    return unitConverter;
  }

  /**
   * Creates a unit converter with total values initialized (convenience method).
   *
   * @param engine The Engine instance to get state from
   * @param stream The stream identifier to create converter for
   * @return A configured unit converter instance
   */
  public static UnitConverter createUnitConverterWithTotal(Engine engine, String stream) {
    EngineNumber currentValue = engine.getStream(stream);
    EngineNumber initialCharge = null;

    if (isSalesSubstream(stream)) {
      initialCharge = engine.getInitialCharge(stream);
    }

    return createUnitConverterWithTotal(engine.getStateGetter(), stream, currentValue, initialCharge);
  }

  /**
   * Check if sales streams were specified in equipment units for the given scope.
   * When streams are specified in units, certain operations need different handling (e.g., retirement
   * affects recharge calculations, carry-over logic differs).
   *
   * @param simulationState the SimulationState instance to query
   * @param scope the scope to check
   * @return true if sales streams were specified in units
   */
  public static boolean hasUnitBasedSalesSpecifications(SimulationState simulationState, UseKey scope) {
    if (!simulationState.hasLastSpecifiedValue(scope, "sales")) {
      return false;
    }
    EngineNumber lastSpecifiedValue = simulationState.getLastSpecifiedValue(scope, "sales");
    return lastSpecifiedValue != null && lastSpecifiedValue.hasEquipmentUnits();
  }

  /**
   * Ensures a value is positive, clamping to zero if negative.
   *
   * <p>This method checks if a value would be negative and returns zero if so, otherwise
   * returns the value unchanged. It's used to enforce constraints that prevent negative stream values
   * when the operation being performed doesn't allow them.</p>
   *
   * @param value The value to check
   * @return The value if positive, or zero if negative
   */
  public static BigDecimal ensurePositive(BigDecimal value) {
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      System.err.println("WARNING: Negative stream value clamped to zero");
      return BigDecimal.ZERO;
    }
    return value;
  }

  /**
   * Gets the distributed recharge amount for a specific stream.
   *
   * <p>This method distributes total recharge volume across sales streams based on
   * their current distribution percentages. The sales stream receives 100% of the recharge (to be
   * distributed internally), sales substreams (domestic/import) receive their proportional share, and
   * other streams receive zero.</p>
   *
   * @param streamName The name of the stream
   * @param totalRecharge The total recharge amount
   * @param useKey The use key containing application and substance
   * @param simulationState The simulation state to query for distribution
   * @return The distributed recharge amount based on stream percentages
   * @throws IllegalArgumentException if streamName is a sales substream but not domestic or import
   */
  public static BigDecimal getDistributedRecharge(String streamName, EngineNumber totalRecharge,
      UseKey useKey, SimulationState simulationState) {
    if ("sales".equals(streamName)) {
      // Sales stream gets 100% - setStreamForSales will distribute it
      return totalRecharge.getValue();
    }

    if (isSalesSubstream(streamName)) {
      SalesStreamDistribution distribution = simulationState.getDistribution(useKey);
      BigDecimal percentage = switch (streamName) {
        case "domestic" -> distribution.getPercentDomestic();
        case "import" -> distribution.getPercentImport();
        default -> throw new IllegalArgumentException("Unknown sales substream: " + streamName);
      };
      return totalRecharge.getValue().multiply(percentage);
    }

    // Export and other streams get no recharge
    return BigDecimal.ZERO;
  }
}
