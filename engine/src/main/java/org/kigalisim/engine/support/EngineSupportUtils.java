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
import org.kigalisim.engine.state.Scope;
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
    STREAM_NAMES.add("virgin");
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
   * Check if a stream name represents a production metastream (sales or virgin).
   *
   * <p>Production metastreams are aggregate streams that represent production
   * consumption within a country. Sales includes domestic, import, and recycling;
   * virgin includes domestic and import only (excluding recycling).</p>
   *
   * @param name The stream name to check
   * @return true if the stream is a production metastream (sales or virgin)
   */
  public static boolean isProductionMetastream(String name) {
    return "sales".equals(name) || "virgin".equals(name);
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
    return "domestic".equals(name) || "import".equals(name) || "virgin".equals(name);
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

    OverridingConverterStateGetter overridingStateGetter =
        new OverridingConverterStateGetter(engine.getStateGetter());
    final UnitConverter unitConverter = new UnitConverter(overridingStateGetter);

    overridingStateGetter.setTotal(stream, currentValue);

    if (isSalesSubstream(stream) && initialCharge != null) {
      overridingStateGetter.setAmortizedUnitVolume(initialCharge);
    }

    SimulationState simulationState = engine.getStreamKeeper();
    Scope scope = engine.getScope();
    EngineNumber priorValue = simulationState.getStream(scope, stream, true);
    if (priorValue != null) {
      overridingStateGetter.setPriorVolume(priorValue);
    }

    return unitConverter;
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
    if (isProductionMetastream(streamName)) {
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

  /**
   * Records a sales-related stream's current value as lastSpecified, preserving unit-tracking
   * mode and excluding recharge/precharge kg riding on top.
   *
   * <p>Both cap/floor operations (see {@code LimitExecutor}) and displacement (see
   * {@code DisplaceExecutor}) need to record a freshly computed stream value as lastSpecified so
   * that future percentage-based operations and year-over-year growth use it as their basis.
   * Recording the raw stream value directly is unsafe for two reasons:</p>
   * <ul>
   *   <li>The raw value is always kg-denominated, so recording it verbatim would silently switch
   *       a unit-tracked stream (e.g. one set via "set sales to 1000 units") into kg-tracking
   *       mode, breaking subsequent unit-based carry-over.</li>
   *   <li>When tracked in equipment units, recharge (of priorEquipment) and precharge (of
   *       newEquipment) ride on top of the stream's raw kg value rather than being absorbed
   *       within it. Recording that inflated value would fold recharge into the baseline, which
   *       then compounds (recharge-on-recharge) in subsequent years.</li>
   * </ul>
   *
   * <p>This method backs out any such servicing kg (only when the existing lastSpecified value
   * is unit-tracked) and converts the result to the existing lastSpecified's units before
   * recording. For production metastreams ("sales"/"virgin"), the component streams
   * (domestic/import) are recorded the same way; for sales substreams (domestic/import), "sales"
   * is recorded the same way.</p>
   *
   * @param engine The engine to read/write stream state on
   * @param scope The scope (application/substance) of the stream
   * @param stream The stream identifier whose current value should be recorded as lastSpecified
   */
  public static void recordLastSpecifiedKeepingUnits(Engine engine, Scope scope, String stream) {
    SimulationState simulationState = engine.getStreamKeeper();
    EngineNumber valueToRecord = adjustRechargeForLastSpecified(
        engine, scope, stream, engine.getStream(stream));
    simulationState.setLastSpecifiedValue(scope, stream, valueToRecord);

    if (isProductionMetastream(stream)) {
      EngineNumber domesticToRecord = adjustRechargeForLastSpecified(
          engine, scope, "domestic", engine.getStream("domestic"));
      simulationState.setLastSpecifiedValue(scope, "domestic", domesticToRecord);

      EngineNumber importToRecord = adjustRechargeForLastSpecified(
          engine, scope, "import", engine.getStream("import"));
      simulationState.setLastSpecifiedValue(scope, "import", importToRecord);
    } else if (isSalesSubstream(stream)) {
      EngineNumber salesToRecord = adjustRechargeForLastSpecified(
          engine, scope, "sales", engine.getStream("sales"));
      simulationState.setLastSpecifiedValue(scope, "sales", salesToRecord);
    }
  }

  /**
   * Adjusts a single stream value for recording as lastSpecified.
   *
   * <p>See {@link #recordLastSpecifiedKeepingUnits} for the rationale. This removes any implied
   * recharge/precharge kg from {@code rawValue} (only when sales-related and the existing
   * lastSpecified is unit-tracked) and converts the result to the existing lastSpecified's units
   * (if any exist).</p>
   *
   * @param engine The engine to read stream/recharge state from
   * @param scope The scope (application/substance) of the stream
   * @param stream The stream identifier
   * @param rawValue The stream's current raw value
   * @return The value to record as lastSpecified
   */
  private static EngineNumber adjustRechargeForLastSpecified(Engine engine, Scope scope,
      String stream, EngineNumber rawValue) {
    SimulationState simulationState = engine.getStreamKeeper();
    EngineNumber priorLastSpecified = simulationState.getLastSpecifiedValue(scope, stream);
    EngineNumber withoutImpliedRecharge = removeImpliedRecharge(
        engine, scope, stream, rawValue, priorLastSpecified);

    if (priorLastSpecified == null) {
      return withoutImpliedRecharge;
    }
    UnitConverter unitConverter = createUnitConverterWithTotal(engine, stream);
    return unitConverter.convert(withoutImpliedRecharge, priorLastSpecified.getUnits());
  }

  /**
   * Removes recharge/precharge kg implied by a sales-related stream's value when it is
   * unit-tracked.
   *
   * @param engine The engine to read recharge state from
   * @param scope The scope (application/substance) of the stream
   * @param stream The stream identifier
   * @param value The stream's current value
   * @param priorLastSpecified The existing lastSpecified value being overwritten, or null
   * @return The value with implied servicing kg removed, or the value unchanged if not applicable
   */
  private static EngineNumber removeImpliedRecharge(Engine engine, Scope scope, String stream,
      EngineNumber value, EngineNumber priorLastSpecified) {
    boolean isSalesRelated = isProductionMetastream(stream) || isSalesSubstream(stream);
    boolean isUnitBased = priorLastSpecified != null && priorLastSpecified.hasEquipmentUnits();
    if (!isSalesRelated || !isUnitBased) {
      return value;
    }

    SimulationState simulationState = engine.getStreamKeeper();
    EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
        scope,
        engine.getStateGetter(),
        simulationState,
        engine
    );
    BigDecimal distributedRecharge = getDistributedRecharge(
        stream,
        rechargeVolume,
        scope,
        simulationState
    );
    if (distributedRecharge.compareTo(BigDecimal.ZERO) == 0) {
      return value;
    }

    UnitConverter unitConverter = createUnitConverterWithTotal(engine, stream);
    EngineNumber valueKg = unitConverter.convert(value, "kg");
    BigDecimal netKg = valueKg.getValue().subtract(distributedRecharge);
    if (netKg.compareTo(BigDecimal.ZERO) < 0) {
      netKg = BigDecimal.ZERO;
    }
    return new EngineNumber(netKg, "kg");
  }
}
