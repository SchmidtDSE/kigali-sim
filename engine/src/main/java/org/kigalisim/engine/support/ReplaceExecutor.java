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
 * GWP context for emissions calculations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimpleUseKey;
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

  /**
   * Creates a new ReplaceExecutor for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public ReplaceExecutor(Engine engine) {
    this.engine = engine;
    this.shortcuts = new StreamUpdateShortcuts(engine);
  }

  /**
   * Executes a substance replacement operation.
   *
   * <p>Replaces the specified amount of the current substance with the destination
 * substance in the given stream. The operation removes substance from the source and adds it to the
 * destination, using appropriate conversion rates and scope context.</p>
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

    // Validate not replacing with self
    if (currentSubstance.equals(destinationSubstance)) {
      ExceptionsGenerator.raiseSelfReplacement(currentSubstance);
    }

    // Update last specified values for both substances
    updateLastSpecified(currentScope, application, destinationSubstance, stream, amountRaw);

    // Resolve percentage to concrete amount
    EngineNumber effectiveAmount = getEffectiveAmount(currentScope, stream, amountRaw);

    // Apply replacement based on unit type
    if (effectiveAmount.hasEquipmentUnits()) {
      applyReplaceWithUnits(currentScope, stream, destinationSubstance, effectiveAmount);
    } else {
      applyReplaceWithVolume(currentScope, stream, destinationSubstance, effectiveAmount);
    }
  }

  /**
   * Updates last specified values for sales streams in both source and destination substances.
   *
   * <p>For sales streams (domestic, import, sales), this method tracks the user-specified
 * amount for both the current substance and the destination substance. This enables subsequent
 * change operations to correctly interpret percentage-based specifications and maintain proper
 * carry-over behavior.</p>
   *
   * @param currentScope The current scope containing the source substance
   * @param application The application name
   * @param destinationSubstance The destination substance name
   * @param stream The stream being modified
   * @param amountRaw The raw amount specified by the user
   */
  private void updateLastSpecified(Scope currentScope, String application,
      String destinationSubstance, String stream, EngineNumber amountRaw) {
    boolean isSalesStream = EngineSupportUtils.getIsSalesStream(stream, true);
    if (!isSalesStream) {
      return;
    }

    SimulationState simulationState = engine.getStreamKeeper();

    // Track for current substance
    simulationState.setLastSpecifiedValue(currentScope, stream, amountRaw);

    // Track for destination substance
    SimpleUseKey destKey = new SimpleUseKey(application, destinationSubstance);
    simulationState.setLastSpecifiedValue(destKey, stream, amountRaw);
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

  /**
   * Applies unit-based replacement using substance-specific initial charge rates.
   *
   * <p>For equipment units, this method converts the specified units to the source
 * substance's volume using the source's initial charge, removes that volume from the source, then
 * converts the same number of units to the destination substance's volume using the destination's
 * initial charge, and adds that volume to the destination.</p>
   *
   * <p>This approach ensures that the same number of equipment units is transferred,
 * but the actual substance volumes may differ based on each substance's initial charge
 * requirements.</p>
   *
   * @param currentScope The current scope containing the source substance
   * @param stream The stream being modified
   * @param destinationSubstance The destination substance name
   * @param effectiveAmount The amount in equipment units
   */
  private void applyReplaceWithUnits(Scope currentScope, String stream,
      String destinationSubstance, EngineNumber effectiveAmount) {
    // Convert to units using source substance's initial charge
    UnitConverter sourceUnitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine,
        stream
    );
    EngineNumber unitsToReplace = sourceUnitConverter.convert(effectiveAmount, "units");

    // Remove from source substance using source's initial charge
    EngineNumber sourceVolumeChange = sourceUnitConverter.convert(unitsToReplace, "kg");
    EngineNumber sourceAmountNegative = new EngineNumber(
        sourceVolumeChange.getValue().negate(),
        sourceVolumeChange.getUnits()
    );
    shortcuts.changeStreamWithoutReportingUnits(
        stream,
        sourceAmountNegative,
        Optional.empty(),
        Optional.empty()
    );

    // Add to destination substance using destination's initial charge
    Scope destinationScope = currentScope.getWithSubstance(destinationSubstance);
    Scope originalScope = engine.getScope();

    // Temporarily switch scope to get destination's initial charge
    engine.setStanza(destinationScope.getStanza());
    engine.setApplication(destinationScope.getApplication());
    engine.setSubstance(destinationScope.getSubstance());
    final EngineNumber destinationInitialCharge = engine.getInitialCharge("sales");
    engine.setStanza(originalScope.getStanza());
    engine.setApplication(originalScope.getApplication());
    engine.setSubstance(originalScope.getSubstance());
    ConverterStateGetter baseStateGetter = engine.getStateGetter();
    OverridingConverterStateGetter destinationStateGetter = new OverridingConverterStateGetter(
        baseStateGetter
    );
    destinationStateGetter.setAmortizedUnitVolume(destinationInitialCharge);
    UnitConverter destinationUnitConverter = new UnitConverter(destinationStateGetter);

    // Convert units to destination volume and apply
    EngineNumber destinationVolumeChange = destinationUnitConverter.convert(unitsToReplace, "kg");
    shortcuts.changeStreamWithDisplacementContext(
        stream,
        destinationVolumeChange,
        destinationScope
    );
  }

  /**
   * Applies volume-based replacement using the same substance volume for both substances.
   *
   * <p>For volume units (kg, mt), this method converts the amount to kg, removes that
 * volume from the source substance, and adds the same volume to the destination substance. Unlike
 * unit-based replacement, the actual substance volume is identical for both source and
 * destination.</p>
   *
   * @param currentScope The current scope containing the source substance
   * @param stream The stream being modified
   * @param destinationSubstance The destination substance name
   * @param effectiveAmount The amount in volume units (kg or mt)
   */
  private void applyReplaceWithVolume(Scope currentScope, String stream,
      String destinationSubstance, EngineNumber effectiveAmount) {
    // Convert to kg
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine,
        stream
    );
    EngineNumber amount = unitConverter.convert(effectiveAmount, "kg");

    // Remove from source substance
    EngineNumber amountNegative = new EngineNumber(amount.getValue().negate(), amount.getUnits());
    shortcuts.changeStreamWithoutReportingUnits(
        stream,
        amountNegative,
        Optional.empty(),
        Optional.empty()
    );

    // Add to destination substance
    Scope destinationScope = currentScope.getWithSubstance(destinationSubstance);
    shortcuts.changeStreamWithDisplacementContext(
        stream,
        amount,
        destinationScope
    );
  }
}
