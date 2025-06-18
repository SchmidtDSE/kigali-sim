/**
 * Strategy for recalculating retirement.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcRetire method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;

/**
 * Strategy for recalculating retirement.
 */
public class RetireRecalcStrategy implements RecalcStrategy {

  private Scope scope;

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
    // Move the logic from SingleThreadEngine.recalcRetire
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
    EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");

    stateGetter.setPopulation(currentPrior);
    EngineNumber amountRaw = streamKeeper.getRetirementRate(application, substance);
    EngineNumber amount = unitConverter.convert(amountRaw, "units");
    stateGetter.clearPopulation();

    // Calculate new values
    BigDecimal newPriorValue = currentPrior.getValue().subtract(amount.getValue());
    BigDecimal newEquipmentValue = currentEquipment.getValue().subtract(amount.getValue());

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
