/**
 * Utility class for stream update shortcut operations.
 *
 * <p>This class provides stream update operations that combine StreamUpdate
 * and RecalcOperation creation for common patterns like displacement and replacement operations.
 * These shortcuts handle the complexity of scope management, unit conversion, and recalculation
 * propagation.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.RecalcKit;
import org.kigalisim.engine.recalc.RecalcKitBuilder;
import org.kigalisim.engine.recalc.RecalcOperation;
import org.kigalisim.engine.recalc.RecalcOperationBuilder;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimpleUseKey;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Handles stream update shortcut operations for displacement and replacement.
 *
 * <p>This class provides methods to update streams without reporting units
 * or with displacement context for correct GWP calculations. These shortcuts streamline common
 * operations in replace and displacement handling.</p>
 */
public class StreamUpdateShortcuts {

  private static final boolean OPTIMIZE_RECALCS = true;
  private final Engine engine;

  /**
   * Creates a new StreamUpdateShortcuts for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public StreamUpdateShortcuts(Engine engine) {
    this.engine = engine;
  }

  /**
   * Change a stream value without reporting units to the last units tracking system.
   *
   * <p>This method updates a stream value without affecting the lastSpecifiedValue
   * tracking used for unit-based carry-over behavior. It is used when the change should not influence
   * subsequent relative changes.</p>
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param scope The scope in which to make the change
   */
  public void changeStreamWithoutReportingUnits(String stream, EngineNumber amount,
      Optional<YearMatcher> yearMatcher, Optional<UseKey> scope) {
    changeStreamWithoutReportingUnits(stream, amount, yearMatcher, scope, false);
  }

  /**
   * Change a stream value without reporting units to the last units tracking system.
   *
   * <p>This method is similar to changeStreamWithDisplacementContext but without the
   * displacement context. It allows for consistent handling of negative stream values across both
   * methods.</p>
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param scope The scope in which to make the change
   * @param negativeAllowed If true, negative stream values are permitted
   */
  public void changeStreamWithoutReportingUnits(String stream, EngineNumber amount,
      Optional<YearMatcher> yearMatcher, Optional<UseKey> scope, boolean negativeAllowed) {
    boolean isInRange = EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear());
    if (!isInRange) {
      return;
    }

    EngineNumber currentValue = engine.getStream(stream, scope, Optional.empty());
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());

    BigDecimal newAmountBound = negativeAllowed ? newAmount : EngineSupportUtils.ensurePositive(newAmount);

    EngineNumber outputWithUnits = new EngineNumber(newAmountBound, currentValue.getUnits());

    // Allow propagation but don't track units (since units tracking was handled by the caller)
    // Also set subtractRecycling=false to avoid negative value clamping in setStreamSalesSubstream
    StreamUpdateBuilder builder = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(outputWithUnits)
        .setSubtractRecycling(false);

    if (scope.isPresent()) {
      builder.setKey(scope.get());
    }

    StreamUpdate update = builder.build();
    engine.executeStreamUpdate(update);
  }

  /**
   * Change a stream value with proper displacement context for correct GWP calculations.
   *
   * <p>This method creates a custom recalc kit that uses the destination substance's
   * properties (GWP, initial charge, energy intensity) to ensure correct emissions calculations
   * during displacement operations. The engine scope is temporarily switched to the destination scope
   * for accurate unit conversion and recalculation.</p>
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param destinationScope The scope for the destination substance
   */
  public void changeStreamWithDisplacementContext(String stream, EngineNumber amount,
      Scope destinationScope) {
    changeStreamWithDisplacementContext(stream, amount, destinationScope, false);
  }

  /**
   * Change a stream value with proper displacement context for correct GWP calculations.
   *
   * <p>This method creates a custom recalc kit that uses the destination substance's
   * properties (GWP, initial charge, energy intensity) to ensure correct emissions calculations
   * during displacement operations. The engine scope is temporarily switched to the destination scope
   * for accurate unit conversion and recalculation, then restored to the original scope after the
   * operation completes.</p>
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param destinationScope The scope for the destination substance
   * @param negativeAllowed If true, negative stream values are permitted
   */
  public void changeStreamWithDisplacementContext(String stream, EngineNumber amount,
      Scope destinationScope, boolean negativeAllowed) {
    // Store original scope
    final Scope originalScope = engine.getScope();

    // Temporarily switch engine scope to destination substance
    engine.setStanza(destinationScope.getStanza());
    engine.setApplication(destinationScope.getApplication());
    engine.setSubstance(destinationScope.getSubstance());

    // Get current value and calculate new value
    EngineNumber outputWithUnits = applyDelta(stream, amount, negativeAllowed);

    // Set the stream value without triggering standard recalc to avoid double calculation
    // Also set subtractRecycling=false to avoid negative value clamping in setStreamSalesSubstream
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(outputWithUnits)
        .setPropagateChanges(false)
        .setSubtractRecycling(false)
        .build();

    engine.executeStreamUpdate(update);

    // Update lastSpecifiedValue for sales substreams since propagateChanges=false skips this
    forceSetLastSpecifiedValue(stream, outputWithUnits, destinationScope);

    // Only recalculate for streams that affect equipment populations
    if (!EngineSupportUtils.getIsSalesStream(stream, false)) {
      // Restore original scope
      engine.setStanza(originalScope.getStanza());
      engine.setApplication(originalScope.getApplication());
      engine.setSubstance(originalScope.getSubstance());
      return;
    }

    // Create standard recalc operation - engine scope is now correctly set to destination
    boolean useImplicitRecharge = false; // Displacement operations don't add recharge

    RecalcOperationBuilder builder = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit()) // Use standard recalc kit - scope is correct now
        .setUseExplicitRecharge(!useImplicitRecharge)
        .recalcPopulationChange()
        .thenPropagateToConsumption();

    if (!engine.getOptimizeRecalcs()) {
      builder = builder.thenPropagateToSales();
    }

    RecalcOperation operation = builder.build();
    operation.execute(engine);

    // Restore original scope
    engine.setStanza(originalScope.getStanza());
    engine.setApplication(originalScope.getApplication());
    engine.setSubstance(originalScope.getSubstance());
  }

  /**
   * Calculates the delta and applies it to the current stream value, clamping if necessary.
   *
   * <p>Gets the current stream value using the engine's current scope, converts the delta
   * to the appropriate units, and calculates the new value. The result is clamped to zero if negative
   * values are not allowed.</p>
   *
   * @param stream The stream identifier
   * @param amount The amount to change by
   * @param negativeAllowed If true, negative stream values are permitted
   * @return The new EngineNumber value with appropriate units
   */
  private EngineNumber applyDelta(String stream, EngineNumber amount, boolean negativeAllowed) {
    EngineNumber currentValue = engine.getStream(stream);
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());

    BigDecimal newAmountBound = negativeAllowed ? newAmount : EngineSupportUtils.ensurePositive(newAmount);

    return new EngineNumber(newAmountBound, currentValue.getUnits());
  }

  /**
   * Updates the lastSpecifiedValue for a sales substream.
   *
   * <p>This method ensures that the lastSpecifiedValue is properly tracked for sales
   * substreams when propagateChanges=false skips the normal tracking mechanism. This is necessary for
   * correct unit-based carry-over behavior in subsequent operations.</p>
   *
   * @param stream The stream identifier
   * @param value The EngineNumber value to set as the last specified value
   * @param destinationScope The scope for the destination substance
   */
  private void forceSetLastSpecifiedValue(String stream, EngineNumber value,
      Scope destinationScope) {
    if (EngineSupportUtils.getIsSalesStream(stream, false)) {
      UseKey destKey = new SimpleUseKey(destinationScope.getApplication(),
                                        destinationScope.getSubstance());
      engine.getStreamKeeper().setLastSpecifiedValue(destKey, stream, value);
    }
  }

  /**
   * Create a RecalcKit with this engine's dependencies.
   *
   * @return A RecalcKit containing this engine's simulationState, unitConverter, and stateGetter
   */
  private RecalcKit createRecalcKit() {
    return new RecalcKitBuilder()
        .setStreamKeeper(engine.getStreamKeeper())
        .setUnitConverter(engine.getUnitConverter())
        .setStateGetter(engine.getStateGetter())
        .build();
  }
}
