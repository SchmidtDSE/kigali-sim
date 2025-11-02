/**
 * Strategy for recalculating end-of-life emissions.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcEolEmissions method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.math.BigDecimal;
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

  /**
   * Recalculate end-of-life emissions based on the retirement delta.
   *
   * <p>This method calculates the amount of equipment retired during the current year using
   * the delta between the cumulative retired stream and the priorRetired stream (which holds
   * the retired count from the start of this year). The difference gives us the amount retired
   * during this year's operations. This amount is then converted to GHG emissions and updated
   * in the simulation state.</p>
   *
   * @param target The engine instance being operated on
   * @param kit The recalculation kit providing access to simulation state and utilities
   */
  @Override
  public void execute(Engine target, RecalcKit kit) {
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    UseKey scopeEffective = scope.orElse(target.getScope());

    if (scopeEffective.getApplication() == null || scopeEffective.getSubstance() == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating EOL emissions change", "");
    }

    SimulationState simulationState = kit.getStreamKeeper();

    EngineNumber currentRetiredRaw = simulationState.getStream(scopeEffective, "retired");
    EngineNumber currentRetired = unitConverter.convert(currentRetiredRaw, "units");

    EngineNumber priorRetiredRaw = simulationState.getStream(scopeEffective, "priorRetired");
    EngineNumber priorRetired = unitConverter.convert(priorRetiredRaw, "units");

    BigDecimal amountRetiredThisYear = currentRetired.getValue()
        .subtract(priorRetired.getValue());
    EngineNumber amount = new EngineNumber(amountRetiredThisYear, "units");

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
