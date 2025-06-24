/**
 * Strategy for recalculating end-of-life emissions.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcEolEmissions method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.support.ExceptionsGenerator;

/**
 * Strategy for recalculating end-of-life emissions.
 */
public class EolEmissionsRecalcStrategy implements RecalcStrategy {

  private final Scope scope;

  /**
   * Create a new EolEmissionsRecalcStrategy.
   *
   * @param scope The scope to use for calculations, null to use engine's current scope
   */
  public EolEmissionsRecalcStrategy(Scope scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    // Setup
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    Scope scopeEffective = scope != null ? scope : target.getScope();

    // Check allowed
    if (scopeEffective.getApplication() == null || scopeEffective.getSubstance() == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating EOL emissions change", "");
    }

    // Calculate change
    EngineNumber currentPriorRaw = target.getStreamRaw(scopeEffective, "priorEquipment");
    EngineNumber currentPrior = unitConverter.convert(currentPriorRaw, "units");

    stateGetter.setPopulation(currentPrior);
    StreamKeeper streamKeeper = kit.getStreamKeeper();
    EngineNumber amountRaw = streamKeeper.getRetirementRate(scopeEffective);
    EngineNumber amount = unitConverter.convert(amountRaw, "units");
    stateGetter.clearPopulation();

    // Update GHG accounting
    EngineNumber eolGhg = unitConverter.convert(amount, "tCO2e");
    streamKeeper.setStream(scopeEffective, "eolEmissions", eolGhg);
  }
}
