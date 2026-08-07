/**
 * Executor for substance replacement operations.
 *
 * <p>This class handles replacing a specified amount of one substance with another
 * substance in a given stream. It supports both equipment-unit-based and volume-based replacement
 * modes, handles percentage resolution, and manages proper scope context for multi-substance
 * operations.</p>
 *
 * <p>Replacement operations involve removing substance from the source and adding it
 * to the destination, using appropriate initial charge rates for unit-based operations and proper
 * GWP context for emissions calculations. Replace always displaces to a different substance
 * (never a stream), so the destination-side transfer and its lastSpecified bookkeeping are
 * delegated to {@link DisplaceExecutor#applyChangeToSubstance}, the same logic cap/floor/recover
 * use for their "displacing" clause, rather than reimplementing it here.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Handles substance replacement operations.
 *
 * <p>This class provides execution logic for replacing one substance with another
 * in a given stream, supporting both unit-based and volume-based replacement modes.</p>
 */
public class ReplaceExecutor {
  private final Engine engine;
  private final StreamUpdateShortcuts shortcuts;
  private final DisplaceExecutor displaceExecutor;

  /**
   * Creates a new ReplaceExecutor for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public ReplaceExecutor(Engine engine) {
    this.engine = engine;
    this.shortcuts = new StreamUpdateShortcuts(engine);
    this.displaceExecutor = new DisplaceExecutor(engine);
  }

  /**
   * Executes a substance replacement operation.
   *
   * <p>Replaces the specified amount of the current substance with the destination
   * substance in the given stream. The amount is removed from the source stream here, then the
   * same change (in kg) is handed to {@link DisplaceExecutor#applyChangeToSubstance} to apply to
   * the destination substance, using appropriate conversion rates and scope context.</p>
   *
   * <p>For percentage-based amounts, the last specified value is used to determine
   * whether to use unit-based or volume-based replacement logic. Unit-based replacement uses initial
   * charge rates specific to each substance, while volume-based replacement uses the same volume for
   * both source and destination.</p>
   *
   * @param amountRaw The amount to replace (can be units, volume, or percentage)
   * @param stream The stream identifier to modify (e.g., "domestic", "import", "sales")
   * @param destinationSubstance The substance to replace with
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @throws IllegalArgumentException if attempting to replace substance with itself
   * @throws IllegalStateException if no application or substance is in current scope
   */
  public void execute(EngineNumber amountRaw, String stream, String destinationSubstance,
      YearMatcher yearMatcher) {
    boolean isInRange = EngineSupportUtils.getIsInRange(yearMatcher, engine.getStreamKeeper().getCurrentYear());
    if (!isInRange) {
      return;
    }

    // Validate scope and substance
    Scope currentScope = engine.getScope();
    String application = currentScope.getApplication();
    String currentSubstance = currentScope.getSubstance();
    if (application == null || currentSubstance == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("setting stream", " specified");
    }

    // A cross-substance replace could in principle be given meaning (mirroring how cap/floor
    // displacement moves a newEquipment change to another substance's sales), but ReplaceExecutor
    // has no case for newEquipment (or equipment/priorEquipment) below, and implementing that
    // mirror is out of scope here. Reject explicitly rather than silently writing to a stream that
    // gets clobbered by the next population recalc.
    if ("newEquipment".equals(stream)) {
      ExceptionsGenerator.raiseNewEquipmentReplace();
    }

    // Validate not replacing with self
    if (currentSubstance.equals(destinationSubstance)) {
      ExceptionsGenerator.raiseSelfReplacement(currentSubstance);
    }

    // Resolve percentage to concrete amount
    EngineNumber effectiveAmount = getEffectiveAmount(currentScope, stream, amountRaw);
    boolean useUnits = effectiveAmount.hasEquipmentUnits();

    // Remove the effective amount from the source substance. This is expressed in kg regardless
    // of whether the amount is unit- or volume-based: the source's own initial charge (via
    // createUnitConverterWithTotal) handles the unit conversion either way, so a single
    // conversion path covers both cases.
    UnitConverter sourceUnitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine,
        stream
    );
    EngineNumber sourceVolumeChange = sourceUnitConverter.convert(effectiveAmount, "kg");
    BigDecimal changeAmount = sourceVolumeChange.getValue().negate();

    shortcuts.changeStreamWithoutReportingUnits(
        stream,
        new EngineNumber(changeAmount, "kg"),
        Optional.empty(),
        Optional.empty()
    );
    EngineSupportUtils.recordLastSpecifiedKeepingUnits(engine, currentScope, stream);

    // Add the same change to the destination substance, reusing DisplaceExecutor's
    // destination-side transfer logic (mirrors cap/floor/recover's "displacing" clause).
    displaceExecutor.applyChangeToSubstance(stream, changeAmount, destinationSubstance, useUnits);
  }

  /**
   * Resolves percentage-based amounts to concrete units or volumes.
   *
   * <p>If the amount is specified as a percentage, this method looks up the last
   * specified value to determine the base amount and unit type. The percentage is then applied to
   * that base value. If no last specified value exists, the current stream value is used as the
   * base.</p>
   *
   * <p>For non-percentage amounts, returns the amount unchanged.</p>
   *
   * @param scope The current scope for value lookup
   * @param stream The stream being modified
   * @param amountRaw The raw amount (may be percentage)
   * @return The effective amount in concrete units (units, kg, or mt)
   */
  private EngineNumber getEffectiveAmount(Scope scope, String stream, EngineNumber amountRaw) {
    boolean isPercent = amountRaw.getUnits().equals("%");
    if (!isPercent) {
      return amountRaw;
    }

    SimulationState simulationState = engine.getStreamKeeper();
    EngineNumber lastSpecified = simulationState.getLastSpecifiedValue(scope, stream);

    boolean hasPrior = lastSpecified != null;
    if (hasPrior) {
      BigDecimal percentageValue = lastSpecified.getValue()
          .multiply(amountRaw.getValue())
          .divide(new BigDecimal("100"));
      return new EngineNumber(percentageValue, lastSpecified.getUnits());
    } else {
      // Use current value units to determine if unit-based logic should apply
      EngineNumber currentValue = engine.getStream(stream);
      BigDecimal percentageValue = currentValue.getValue()
          .multiply(amountRaw.getValue())
          .divide(new BigDecimal("100"));
      return new EngineNumber(percentageValue, currentValue.getUnits());
    }
  }
}
