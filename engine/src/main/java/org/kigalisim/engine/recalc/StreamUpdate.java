package org.kigalisim.engine.recalc;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Immutable class representing stream calculation instructions.
 *
 * <p>Contains parameters needed to execute a stream calculation operation in the engine,
 * including timing constraints, scope, and behavioral flags. This class provides the "instructions"
 * for how to calculate stream values, while SimulationStateUpdate contains the pre-computed
 * "results" ready for storage.</p>
 *
 * <p>Use StreamUpdate for operations that need calculation logic (set, change, cap, floor).
 * Use SimulationStateUpdate for setting pre-calculated values (recalc strategies, emissions).</p>
 *
 * @license BSD-3-Clause
 */
public final class StreamUpdate {
  private final String name;
  private final EngineNumber value;
  private final Optional<YearMatcher> yearMatcher;
  private final Optional<UseKey> key;
  private final boolean propagateChanges;
  private final Optional<String> unitsToRecord;
  private final boolean subtractRecycling;
  private final boolean forceUseFullRecharge;
  private final Optional<SalesStreamDistribution> distribution;
  private final boolean preserveImplicitRecharge;
  private final boolean preserveLastSpecified;

  /**
   * Package-private constructor for creating a StreamUpdate instance.
   *
   *
   * @param name the name of the stream to update
   * @param value the value to set for the stream
   * @param yearMatcher optional year matcher to constrain when the update applies
   * @param key optional use key specifying the application/substance scope
   * @param propagateChanges whether this update should trigger recalculations
   * @param unitsToRecord optional units string to record for this operation
   * @param subtractRecycling whether recycling should be subtracted from the value
   * @param forceUseFullRecharge whether to force full recharge for sales substreams
   * @param distribution optional pre-calculated distribution for sales streams
   * @param preserveImplicitRecharge whether to leave the implicitRecharge/implicitPrecharge
   *     streams untouched instead of updating or clearing them based on this update's units
   * @param preserveLastSpecified whether to skip the standard lastSpecifiedValue/composite
   *     ("sales") tracking update this update would otherwise trigger
   */
  StreamUpdate(String name, EngineNumber value, Optional<YearMatcher> yearMatcher,
      Optional<UseKey> key, boolean propagateChanges, Optional<String> unitsToRecord,
      boolean subtractRecycling, boolean forceUseFullRecharge,
      Optional<SalesStreamDistribution> distribution, boolean preserveImplicitRecharge,
      boolean preserveLastSpecified) {
    this.name = name;
    this.value = value;
    this.yearMatcher = yearMatcher;
    this.key = key;
    this.propagateChanges = propagateChanges;
    this.unitsToRecord = unitsToRecord;
    this.subtractRecycling = subtractRecycling;
    this.forceUseFullRecharge = forceUseFullRecharge;
    this.distribution = distribution;
    this.preserveImplicitRecharge = preserveImplicitRecharge;
    this.preserveLastSpecified = preserveLastSpecified;
  }

  /**
   * Gets the name of the stream to update.
   *
   *
   * @return the stream name (e.g., "domestic", "import", "sales")
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the value to set for the stream.
   *
   *
   * @return the stream value with units
   */
  public EngineNumber getValue() {
    return value;
  }

  /**
   * Gets the optional year matcher constraining when this update applies.
   *
   *
   * @return optional year matcher, empty if update applies to all years
   */
  public Optional<YearMatcher> getYearMatcher() {
    return yearMatcher;
  }

  /**
   * Gets the optional use key specifying the application/substance scope.
   *
   *
   * @return optional use key, empty if using engine's current scope
   */
  public Optional<UseKey> getKey() {
    return key;
  }

  /**
   * Gets whether this update should trigger recalculations.
   *
   *
   * @return true if recalculations should be triggered, false otherwise
   */
  public boolean getPropagateChanges() {
    return propagateChanges;
  }

  /**
   * Gets the optional units string to record for this operation.
   *
   *
   * @return optional units string for tracking purposes
   */
  public Optional<String> getUnitsToRecord() {
    return unitsToRecord;
  }

  /**
   * Gets whether recycling should be subtracted from the value.
   *
   *
   * @return true if recycling should be subtracted, false otherwise
   */
  public boolean getSubtractRecycling() {
    return subtractRecycling;
  }

  /**
   * Gets whether to force full recharge for sales substreams.
   *
   *
   * @return true if full recharge should be used, false for proportional distribution
   */
  public boolean getForceUseFullRecharge() {
    return forceUseFullRecharge;
  }

  /**
   * Gets the optional pre-calculated distribution for sales streams.
   *
   *
   * @return optional sales stream distribution, empty if none specified
   */
  public Optional<SalesStreamDistribution> getDistribution() {
    return distribution;
  }

  /**
   * Gets whether the implicitRecharge/implicitPrecharge streams should be left untouched.
   *
   * <p>Set by internal "shortcut" updates (see {@code StreamUpdateShortcuts}) that move kg
   * volume into or out of a stream without genuinely re-specifying it in volume or unit terms
   * (e.g. removing an amount for {@code replace}, or applying a displacement transfer). Such
   * updates don't change any recharge/precharge parameters, so the servicing kg already recorded
   * in implicitRecharge/implicitPrecharge remains accurate and should not be recomputed or
   * cleared based on this update's own units.</p>
   *
   * @return true if implicitRecharge/implicitPrecharge should be left as-is, false to update
   *     them normally based on whether this update's value carries equipment units
   */
  public boolean getPreserveImplicitRecharge() {
    return preserveImplicitRecharge;
  }

  /**
   * Gets whether the standard lastSpecifiedValue/composite ("sales") tracking update should be
   * skipped for this update.
   *
   * <p>Set by internal "shortcut" updates (see {@code StreamUpdateShortcuts
   * #changeStreamWithoutReportingUnits}) that already perform their own targeted
   * lastSpecifiedValue restoration for the specific stream being changed. Without this flag, the
   * standard tracking update in {@code StreamUpdateExecutor} would separately stamp
   * lastSpecifiedValue (and the "sales" composite when a substream changes) with this update's
   * raw kg value -- run before the shortcut's own restoration reaches the "sales" composite --
   * permanently flipping "sales" from unit-tracked to kg-tracked even though nothing about this
   * update genuinely re-specified it in volume terms.</p>
   *
   * @return true if the standard lastSpecifiedValue/composite tracking update should be skipped
   */
  public boolean getPreserveLastSpecified() {
    return preserveLastSpecified;
  }
}
