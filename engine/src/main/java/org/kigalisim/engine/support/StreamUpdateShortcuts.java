/**
 * Utility class for stream update shortcut operations.
 *
 * <p>This class provides stream update operations that combine StreamUpdate
 * and RecalcOperation creation for common patterns like displacement and
 * replacement operations. These shortcuts handle the complexity of scope
 * management, unit conversion, and recalculation propagation.</p>
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
 * or with displacement context for correct GWP calculations. These shortcuts
 * streamline common operations in replace and displacement handling.</p>
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
   * tracking used for unit-based carry-over behavior. It is used when the change
   * should not influence subsequent relative changes.</p>
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
   * displacement context. It allows for consistent handling of negative stream values
   * across both methods.</p>
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param scope The scope in which to make the change
   * @param negativeAllowed If true, negative stream values are permitted
   */
  public void changeStreamWithoutReportingUnits(String stream, EngineNumber amount,
      Optional<YearMatcher> yearMatcher, Optional<UseKey> scope, boolean negativeAllowed) {
    if (!EngineSupportUtils.isInRange(yearMatcher.orElse(null), engine.getYear())) {
      return;
    }

    EngineNumber currentValue = engine.getStream(stream, scope, Optional.empty());
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());

    BigDecimal newAmountBound;
    if (!negativeAllowed && newAmount.compareTo(BigDecimal.ZERO) < 0) {
      System.err.println("WARNING: Negative stream value clamped to zero for stream " + stream);
      newAmountBound = BigDecimal.ZERO;
    } else {
      newAmountBound = newAmount;
    }

    EngineNumber outputWithUnits = new EngineNumber(newAmountBound, currentValue.getUnits());

    // Allow propagation but don't track units (since units tracking was handled by the caller)
    StreamUpdateBuilder builder = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(outputWithUnits);

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
   * properties (GWP, initial charge, energy intensity) to ensure correct emissions
   * calculations during displacement operations. The engine scope is temporarily
   * switched to the destination scope for accurate unit conversion and recalculation.</p>
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
   * properties (GWP, initial charge, energy intensity) to ensure correct emissions
   * calculations during displacement operations. The engine scope is temporarily
   * switched to the destination scope for accurate unit conversion and recalculation,
   * then restored to the original scope after the operation completes.</p>
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

    // Get current value and calculate new value (now using correct scope)
    EngineNumber currentValue = engine.getStream(stream);
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());

    BigDecimal newAmountBound;
    if (!negativeAllowed && newAmount.compareTo(BigDecimal.ZERO) < 0) {
      System.err.println("WARNING: Negative stream value clamped to zero for stream " + stream);
      newAmountBound = BigDecimal.ZERO;
    } else {
      newAmountBound = newAmount;
    }

    EngineNumber outputWithUnits = new EngineNumber(newAmountBound, currentValue.getUnits());

    // Set the stream value without triggering standard recalc to avoid double calculation
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(outputWithUnits)
        .setPropagateChanges(false)
        .build();

    engine.executeStreamUpdate(update);

    // Update lastSpecifiedValue for sales substreams since propagateChanges=false skips this
    if (EngineSupportUtils.getIsSalesStream(stream, false)) {
      UseKey destKey = new SimpleUseKey(destinationScope.getApplication(),
                                        destinationScope.getSubstance());
      engine.getStreamKeeper().setLastSpecifiedValue(destKey, stream, outputWithUnits);
    }

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
