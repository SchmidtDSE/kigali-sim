/**
 * Shared read-only helpers for SimulationState implementations.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Package-private static utilities shared by {@link MutableSimulationState} and
 * {@link FrozenSimulationState}.
 *
 * <p>Centralizes key-building, stream-name resolution, substance lookup, and the
 * stream-value read/aggregation logic (including the one-year-back lookback used by
 * {@code priorYear} lookups) so this non-trivial read behavior cannot drift between
 * the live, mutable implementation and the immutable snapshot implementation.</p>
 */
final class SimulationStateSupport {

  private SimulationStateSupport() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Generate a key for a UseKey.
   *
   * @param useKey The UseKey to generate a key for
   * @return The generated key
   */
  static String getKey(UseKey useKey) {
    return useKey.getKey();
  }

  /**
   * Generate a stream key for a UseKey and stream name.
   *
   * @param useKey The UseKey to generate a key for
   * @param name The stream name
   * @return The generated stream key
   */
  static String getKey(UseKey useKey, String name) {
    StringBuilder keyBuilder = new StringBuilder();
    keyBuilder.append(getKey(useKey));
    keyBuilder.append("\t");
    keyBuilder.append(name != null ? name : "-");
    return keyBuilder.toString();
  }

  /**
   * Verify that a stream name is valid.
   *
   * @param name The stream name to verify
   * @throws IllegalArgumentException If the stream name is not recognized
   */
  static void ensureStreamKnown(String name) {
    if (EngineConstants.getBaseUnits(name) == null) {
      throw new IllegalArgumentException("Unknown stream: " + name);
    }
  }

  /**
   * Indicate that a substance / application was not found.
   *
   * @param context the context in which the application-substance pair is unknown
   * @param application the name of the application being checked
   * @param substance the name of the substance being checked
   * @throws IllegalStateException always
   */
  static void throwSubstanceMissing(String context, String application, String substance) {
    StringBuilder message = new StringBuilder();
    message.append("Not a known application substance pair in ");
    message.append(context);
    message.append(": ");
    message.append(application);
    message.append(", ");
    message.append(substance);
    throw new IllegalStateException(message.toString());
  }

  /**
   * Get the stream name for an induction stage.
   *
   * @param stage The recovery stage
   * @return The corresponding stream name
   */
  static String getInductionStreamName(RecoveryStage stage) {
    return switch (stage) {
      case EOL -> "inductionEol";
      case RECHARGE -> "inductionRecharge";
    };
  }

  /**
   * Retrieve parameterization for a specific key.
   *
   * @param substances The substances map to look up within
   * @param useKey The key containing application and substance
   * @return The parameterization for the given key
   * @throws IllegalStateException If no substance is registered for the key
   */
  static StreamParameterization getParameterization(
      Map<String, StreamParameterization> substances, UseKey useKey) {
    String key = getKey(useKey);
    StreamParameterization result = substances.get(key);
    if (result == null) {
      throwSubstanceMissing("getParameterization", useKey.getApplication(), useKey.getSubstance());
    }
    return result;
  }

  /**
   * Get a stream value directly from the streams map, optionally preferring the prior year.
   *
   * <p>When {@code priorYear} is true and a prior state exists with this exact stream key
   * already present, that prior value is returned via the prior state's own public
   * {@link SimulationState#getStream(UseKey, String)}. Otherwise, this falls back to the
   * current instance's own value.</p>
   *
   * @param streams The streams map to look up within
   * @param priorState The prior simulation state, if any
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @param priorYear If true, prefer the prior year's value when present
   * @return The stream value
   * @throws IllegalStateException If no value is known for the key in either year
   */
  static EngineNumber getStreamDirect(Map<String, EngineNumber> streams,
      Optional<SimulationState> priorState, UseKey useKey, String name, boolean priorYear) {
    String key = getKey(useKey, name);
    if (priorYear && priorState.isPresent() && priorState.get().isKnownStream(useKey, name)) {
      return priorState.get().getStream(useKey, name);
    }

    EngineNumber result = streams.get(key);
    if (result == null) {
      throwSubstanceMissing("getStream", useKey.getApplication(), useKey.getSubstance());
    }
    return result;
  }

  /**
   * Get the value of a specific stream, dispatching to aggregate-stream logic as needed.
   *
   * @param self The SimulationState instance on whose behalf this lookup runs, used to
   *     recurse into component streams (e.g. domestic / import) through the public API
   * @param streams The streams map to look up within for direct (non-aggregate) streams
   * @param priorState The prior simulation state, if any
   * @param unitConverter Converter used to normalize component values to kg before summing
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @param priorYear If true, returns prior year value if available, returns current year if no
   *     prior year exists.
   * @return The stream value
   */
  static EngineNumber getStream(SimulationState self, Map<String, EngineNumber> streams,
      Optional<SimulationState> priorState, UnitConverter unitConverter,
      UseKey useKey, String name, boolean priorYear) {
    ensureStreamKnown(name);
    return switch (name) {
      case "sales" -> getStreamSales(self, unitConverter, useKey, priorYear);
      case "virgin" -> getStreamVirgin(self, unitConverter, useKey, priorYear);
      case "recycle" -> getStreamRecycle(self, unitConverter, useKey, priorYear);
      case "induction" -> self.getTotalInductionStream(useKey, priorYear);
      default -> getStreamDirect(streams, priorState, useKey, name, priorYear);
    };
  }

  /**
   * Get the sales stream value by summing domestic, import, and recycle streams.
   *
   * @param self The SimulationState instance on whose behalf this lookup runs
   * @param unitConverter Converter used to normalize component values to kg before summing
   * @param useKey The key containing application and substance
   * @param priorYear If true, returns prior year value if available, returns current year if no
   *     prior year exists.
   * @return The total sales value in kg
   */
  private static EngineNumber getStreamSales(SimulationState self, UnitConverter unitConverter,
      UseKey useKey, boolean priorYear) {
    EngineNumber domesticAmountRaw = self.getStream(useKey, "domestic", priorYear);
    EngineNumber importAmountRaw = self.getStream(useKey, "import", priorYear);
    EngineNumber recycleAmountRaw = self.getStream(useKey, "recycle", priorYear);

    EngineNumber domesticAmount = unitConverter.convert(domesticAmountRaw, "kg");
    EngineNumber importAmount = unitConverter.convert(importAmountRaw, "kg");
    EngineNumber recycleAmount = unitConverter.convert(recycleAmountRaw, "kg");

    BigDecimal newTotal = domesticAmount.getValue()
        .add(importAmount.getValue())
        .add(recycleAmount.getValue());

    return new EngineNumber(newTotal, "kg");
  }

  /**
   * Get the virgin stream value by summing domestic and import streams (excluding recycling).
   *
   * @param self The SimulationState instance on whose behalf this lookup runs
   * @param unitConverter Converter used to normalize component values to kg before summing
   * @param useKey The key containing application and substance
   * @param priorYear If true, returns prior year value if available, returns current year if no
   *     prior year exists.
   * @return The total virgin value in kg
   */
  private static EngineNumber getStreamVirgin(SimulationState self, UnitConverter unitConverter,
      UseKey useKey, boolean priorYear) {
    EngineNumber domesticAmountRaw = self.getStream(useKey, "domestic", priorYear);
    EngineNumber importAmountRaw = self.getStream(useKey, "import", priorYear);

    EngineNumber domesticAmount = unitConverter.convert(domesticAmountRaw, "kg");
    EngineNumber importAmount = unitConverter.convert(importAmountRaw, "kg");

    BigDecimal newTotal = domesticAmount.getValue().add(importAmount.getValue());

    return new EngineNumber(newTotal, "kg");
  }

  /**
   * Get the recycle stream value by summing recycleRecharge and recycleEol streams.
   *
   * @param self The SimulationState instance on whose behalf this lookup runs
   * @param unitConverter Converter used to normalize component values to kg before summing
   * @param useKey The key containing application and substance
   * @param priorYear If true, returns prior year value if available, returns current year if no
   *     prior year exists.
   * @return The total recycle value in kg
   */
  private static EngineNumber getStreamRecycle(SimulationState self, UnitConverter unitConverter,
      UseKey useKey, boolean priorYear) {
    EngineNumber recycleRechargeAmountRaw = self.getStream(useKey, "recycleRecharge", priorYear);
    EngineNumber recycleEolAmountRaw = self.getStream(useKey, "recycleEol", priorYear);

    EngineNumber recycleRechargeAmount = unitConverter.convert(recycleRechargeAmountRaw, "kg");
    EngineNumber recycleEolAmount = unitConverter.convert(recycleEolAmountRaw, "kg");

    BigDecimal newTotal = recycleRechargeAmount.getValue().add(recycleEolAmount.getValue());

    return new EngineNumber(newTotal, "kg");
  }
}
