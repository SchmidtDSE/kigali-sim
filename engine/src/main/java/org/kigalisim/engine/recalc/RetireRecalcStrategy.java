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
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.SimulationStateUpdate;
import org.kigalisim.engine.state.SimulationStateUpdateBuilder;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.support.ExceptionsGenerator;

/**
 * Strategy for recalculating retirement.
 */
public class RetireRecalcStrategy implements RecalcStrategy {

  private final Optional<UseKey> scope;

  /**
   * Create a new RetireRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   */
  public RetireRecalcStrategy(Optional<UseKey> scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    // Setup
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    UseKey scopeEffective = scope.orElse(target.getScope());
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    // Check allowed
    if (application == null || substance == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating population change", "");
    }

    // Get SimulationState from kit
    var simulationState = kit.getStreamKeeper();

    // Get current state
    EngineNumber currentPriorRaw = simulationState.getStream(
        scopeEffective,
        "priorEquipment"
    );
    EngineNumber currentPrior = unitConverter.convert(currentPriorRaw, "units");

    EngineNumber currentEquipmentRaw = simulationState.getStream(
        scopeEffective,
        "equipment"
    );
    final EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");

    // Capture base population on first retire command this year
    EngineNumber basePopulation = simulationState.getRetirementBasePopulation(scopeEffective);
    if (basePopulation == null) {
      basePopulation = currentPrior;
      simulationState.setRetirementBasePopulation(scopeEffective, basePopulation);
    }

    // Convert cumulative retirement rate using BASE population (not current)
    EngineNumber cumulativeRateRaw = simulationState.getRetirementRate(scopeEffective);
    stateGetter.setPopulation(basePopulation);
    EngineNumber cumulativeAmount = unitConverter.convert(cumulativeRateRaw, "units");
    stateGetter.clearPopulation();

    // Calculate delta (incremental retirement since last retire command)
    EngineNumber appliedAmount = simulationState.getAppliedRetirementAmount(scopeEffective);
    BigDecimal deltaValue = cumulativeAmount.getValue().subtract(appliedAmount.getValue());
    EngineNumber delta = new EngineNumber(deltaValue, "units");

    // Apply ONLY delta to streams (not cumulative amount)
    BigDecimal newPriorValue = currentPrior.getValue().subtract(delta.getValue());
    BigDecimal newEquipmentValue = currentEquipment.getValue().subtract(delta.getValue());

    EngineNumber currentRetiredRaw = simulationState.getStream(scopeEffective, "retired");
    EngineNumber currentRetired = unitConverter.convert(currentRetiredRaw, "units");
    BigDecimal newRetiredValue = currentRetired.getValue().add(delta.getValue());

    EngineNumber newPrior = new EngineNumber(newPriorValue, "units");
    EngineNumber newEquipment = new EngineNumber(newEquipmentValue, "units");
    EngineNumber newRetired = new EngineNumber(newRetiredValue, "units");

    // Update equipment streams
    SimulationStateUpdate priorEquipmentStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("priorEquipment")
        .setValue(newPrior)
        .setSubtractRecycling(false)
        .setFromRetireRecalc(true)  // Exempt from invalidation
        .build();
    simulationState.update(priorEquipmentStream);

    SimulationStateUpdate equipmentStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("equipment")
        .setValue(newEquipment)
        .setSubtractRecycling(false)
        .build();
    simulationState.update(equipmentStream);

    // Update retired stream with the amount retired this year
    SimulationStateUpdate retiredStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("retired")
        .setValue(newRetired)
        .setSubtractRecycling(false)
        .build();
    simulationState.update(retiredStream);

    // Track cumulative applied amount for next delta calculation
    simulationState.setAppliedRetirementAmount(scopeEffective, cumulativeAmount);

    // Update GHG accounting
    EolEmissionsRecalcStrategy eolStrategy = new EolEmissionsRecalcStrategy(Optional.of(scopeEffective));
    eolStrategy.execute(target, kit);

    // Propagate
    PopulationChangeRecalcStrategy populationStrategy =
        new PopulationChangeRecalcStrategy(Optional.empty(), Optional.empty());
    populationStrategy.execute(target, kit);
    // Note: SalesRecalcStrategy removed - retirement should not recalculate sales
    // Sales were already calculated correctly before retirement with proper population context
    ConsumptionRecalcStrategy consumptionStrategy = new ConsumptionRecalcStrategy(Optional.empty());
    consumptionStrategy.execute(target, kit);
  }
}
