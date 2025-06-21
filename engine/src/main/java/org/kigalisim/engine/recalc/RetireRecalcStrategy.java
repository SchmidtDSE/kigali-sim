/**
 * Strategy for recalculating retirement.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcRetire method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.math.BigDecimal;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.support.ExceptionsGenerator;

/**
 * Strategy for recalculating retirement.
 */
public class RetireRecalcStrategy implements RecalcStrategy {

  private final Scope scope;

  /**
   * Create a new RetireRecalcStrategy.
   *
   * @param scope The scope to use for calculations, null to use engine's current scope
   */
  public RetireRecalcStrategy(Scope scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    // Setup
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    Scope scopeEffective = scope != null ? scope : target.getScope();
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    // Check allowed
    if (application == null || substance == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating population change", "");
    }

    // Get StreamKeeper from kit
    var streamKeeper = kit.getStreamKeeper();

    // Calculate change
    EngineNumber currentPriorRaw = streamKeeper.getStream(
        application,
        substance,
        "priorEquipment"
    );
    EngineNumber currentPrior = unitConverter.convert(currentPriorRaw, "units");

    EngineNumber currentEquipmentRaw = streamKeeper.getStream(
        application,
        substance,
        "equipment"
    );
    final EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");

    stateGetter.setPopulation(currentPrior);
    EngineNumber amountRaw = streamKeeper.getRetirementRate(application, substance);
    EngineNumber amount = unitConverter.convert(amountRaw, "units");
    stateGetter.clearPopulation();

    // Skip retirement calculation if we're in year 1 (to preserve the initial manufacture value)
    if (target.getYear() == 1) {
      return;
    }

    // Calculate new values
    // Convert percentage to decimal (divide by 100)
    BigDecimal retirementRate = amount.getValue().divide(new BigDecimal("100"), 10, BigDecimal.ROUND_HALF_UP);
    BigDecimal retirementAmount = currentPrior.getValue().multiply(retirementRate);
    BigDecimal newPriorValue = currentPrior.getValue().subtract(retirementAmount);
    BigDecimal newEquipmentValue = currentEquipment.getValue().subtract(retirementAmount);

    EngineNumber newPrior = new EngineNumber(newPriorValue, "units");
    EngineNumber newEquipment = new EngineNumber(newEquipmentValue, "units");

    // Update equipment streams
    streamKeeper.setStream(application, substance, "priorEquipment", newPrior);
    streamKeeper.setStream(application, substance, "equipment", newEquipment);

    // Update GHG accounting
    EolEmissionsRecalcStrategy eolStrategy = new EolEmissionsRecalcStrategy(scopeEffective);
    eolStrategy.execute(target, kit);

    // Propagate
    PopulationChangeRecalcStrategy populationStrategy =
        new PopulationChangeRecalcStrategy(null, null);
    populationStrategy.execute(target, kit);
    SalesRecalcStrategy salesStrategy = new SalesRecalcStrategy(null);
    salesStrategy.execute(target, kit);
    ConsumptionRecalcStrategy consumptionStrategy = new ConsumptionRecalcStrategy(null);
    consumptionStrategy.execute(target, kit);
  }
}
