/**
 * Strategy for recalculating end-of-life emissions.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcEolEmissions method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.SimulationStateUpdate;
import org.kigalisim.engine.state.SimulationStateUpdateBuilder;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.support.ExceptionsGenerator;

/**
 * Strategy for recalculating end-of-life emissions.
 */
public class EolEmissionsRecalcStrategy implements RecalcStrategy {

  private final Optional<UseKey> scope;

  /**
   * Create a new EolEmissionsRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   */
  public EolEmissionsRecalcStrategy(Optional<UseKey> scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    // Setup
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    UseKey scopeEffective = scope.orElse(target.getScope());

    // Check allowed
    if (scopeEffective.getApplication() == null || scopeEffective.getSubstance() == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating EOL emissions change", "");
    }

    // Calculate the amount retired this year by using the retired stream delta
    // The retired stream is cumulative across all years
    // The priorRetired stream holds the retired count from the start of this year
    // So the delta gives us the amount retired during this year's operations
    SimulationState simulationState = kit.getStreamKeeper();

    // Get current retired amount (after RetireRecalcStrategy added this year's retirement)
    EngineNumber currentRetiredRaw = simulationState.getStream(scopeEffective, "retired");
    EngineNumber currentRetired = unitConverter.convert(currentRetiredRaw, "units");

    // Get priorRetired (the retired count at the start of this year)
    EngineNumber priorRetiredRaw = simulationState.getStream(scopeEffective, "priorRetired");
    EngineNumber priorRetired = unitConverter.convert(priorRetiredRaw, "units");

    // Calculate this year's retirement amount
    java.math.BigDecimal amountRetiredThisYear =
        currentRetired.getValue().subtract(priorRetired.getValue());
    EngineNumber amount = new EngineNumber(amountRetiredThisYear, "units");

    // Update GHG accounting
    EngineNumber eolGhg = unitConverter.convert(amount, "tCO2e");
    SimulationStateUpdate eolEmissionsStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("eolEmissions")
        .setValue(eolGhg)
        .setSubtractRecycling(false)
        .build();
    simulationState.update(eolEmissionsStream);
  }
}
