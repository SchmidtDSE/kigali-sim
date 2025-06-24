/**
 * Strategy for recalculating consumption.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcConsumption method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.support.ConsumptionCalculator;
import org.kigalisim.engine.support.ExceptionsGenerator;

/**
 * Strategy for recalculating consumption.
 */
public class ConsumptionRecalcStrategy implements RecalcStrategy {

  private final Optional<UseKey> scope;

  /**
   * Create a new ConsumptionRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   */
  public ConsumptionRecalcStrategy(Optional<UseKey> scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    UseKey scopeEffective = scope.orElse(target.getScope());

    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    if (application == null || substance == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating consumption", "");
    }

    // Update streams using ConsumptionCalculator
    ConsumptionCalculator calculator = new ConsumptionCalculator();

    // Get stream keeper from kit
    StreamKeeper streamKeeper = kit.getStreamKeeper();

    // Get GHG intensity and calculate consumption
    EngineNumber ghgIntensity = streamKeeper.getGhgIntensity(scopeEffective);
    calculator.setConsumptionRaw(ghgIntensity);
    calculator.setStreamName("consumption");
    calculator.execute(target);

    // Get energy intensity and calculate energy
    calculator = new ConsumptionCalculator();
    EngineNumber energyIntensity = streamKeeper.getEnergyIntensity(scopeEffective);
    calculator.setConsumptionRaw(energyIntensity);
    calculator.setStreamName("energy");
    calculator.execute(target);
  }
}
