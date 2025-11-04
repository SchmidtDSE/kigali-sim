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
import org.kigalisim.engine.state.SimulationState;
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

  /**
   * Retrieves current retirement-related state from the simulation.
   *
   * <p>This method retrieves and converts all current state values needed for retirement
 * calculations, including prior equipment, total equipment, and retired amounts. It also determines
 * the base population for retirement rate calculations, setting it on the first retirement
 * operation for this scope.</p>
   *
   * <p>Side effect: If this is the first retirement operation, sets the retirement base
   * population in the simulation state to the current prior equipment value.</p>
   *
   * @param scopeEffective The use key scope for retrieving state
   * @param simulationState The simulation state to query
   * @param unitConverter Unit converter for normalizing values to units
   * @return RetireBackgroundInfo containing all current state values
   */
  private RetireBackgroundInfo getCurrentState(UseKey scopeEffective,
      SimulationState simulationState, UnitConverter unitConverter) {
    EngineNumber currentPriorRaw = simulationState.getStream(scopeEffective, "priorEquipment");
    EngineNumber currentPrior = unitConverter.convert(currentPriorRaw, "units");

    EngineNumber currentEquipmentRaw = simulationState.getStream(scopeEffective, "equipment");
    EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");

    EngineNumber currentRetiredRaw = simulationState.getStream(scopeEffective, "retired");
    EngineNumber currentRetired = unitConverter.convert(currentRetiredRaw, "units");

    Optional<EngineNumber> basePopulationOpt =
        simulationState.getRetirementBasePopulation(scopeEffective);
    EngineNumber basePopulation;
    boolean firstRetire = !basePopulationOpt.isPresent();
    if (firstRetire) {
      basePopulation = currentPrior;
      simulationState.setRetirementBasePopulation(scopeEffective, basePopulation);
    } else {
      basePopulation = basePopulationOpt.get();
    }

    return new RetireBackgroundInfo(
        currentPrior,
        currentEquipment,
        currentRetired,
        basePopulation,
        firstRetire
    );
  }

  /**
   * Calculates the incremental retirement amount since the last retire command.
   *
   * <p>This method computes the delta (incremental) retirement by:</p>
   * <ol>
   *   <li>Converting the cumulative retirement rate to absolute units using base population</li>
   *   <li>Retrieving the previously applied cumulative amount</li>
   *   <li>Calculating delta as the difference between current cumulative and previously applied</li>
   * </ol>
   *
   * <p>The OverridingConverterStateGetter is used to temporarily set the population context
   * for converting percentage-based retirement rates to absolute unit amounts.</p>
   *
   * @param scopeEffective The use key scope for retrieving state
   * @param simulationState The simulation state to query
   * @param unitConverter Unit converter for converting retirement rate
   * @param stateGetter State getter with population override capability
   * @param backgroundInfo Current state information including base population
   * @return EngineNumber representing the incremental retirement amount in units
   */
  private EngineNumber calculateDelta(UseKey scopeEffective, SimulationState simulationState,
      UnitConverter unitConverter, OverridingConverterStateGetter stateGetter,
      RetireBackgroundInfo backgroundInfo) {
    // Convert cumulative retirement rate using base prior equipment
    EngineNumber cumulativeRateRaw = simulationState.getRetirementRate(scopeEffective);
    stateGetter.setPopulation(backgroundInfo.getBasePopulation());
    EngineNumber cumulativeAmount = unitConverter.convert(cumulativeRateRaw, "units");
    stateGetter.clearPopulation();

    // Calculate delta (incremental retirement since last retire command)
    Optional<EngineNumber> appliedAmountOpt =
        simulationState.getAppliedRetirementAmount(scopeEffective);
    EngineNumber appliedAmount = appliedAmountOpt.orElse(
        new EngineNumber(BigDecimal.ZERO, "units")
    );
    BigDecimal deltaValue = cumulativeAmount.getValue().subtract(appliedAmount.getValue());
    EngineNumber delta = new EngineNumber(deltaValue, "units");

    return delta;
  }

  /**
   * Calculates new stream values after applying retirement delta.
   *
   * <div>This method applies the retirement delta to compute new values for:
   * <ul>
   *   <li>Prior equipment: reduced by delta (equipment that was in service is now retired)</li>
   *   <li>Total equipment: reduced by delta (overall population decreases)</li>
   *   <li>Retired: increased by delta (cumulative retired equipment increases)</li>
   * </ul></div>
   *
   * <p>All calculations are performed in units.</p>
   *
   * @param delta The incremental retirement amount in units
   * @param backgroundInfo Current state information with existing stream values
   * @return PostRetireNewLevels with calculated new values for all affected streams
   */
  private PostRetireNewLevels calculateNewLevels(EngineNumber delta,
      RetireBackgroundInfo backgroundInfo) {
    // Apply delta to streams
    BigDecimal newPriorValue = backgroundInfo.getCurrentPrior().getValue()
        .subtract(delta.getValue());
    BigDecimal newEquipmentValue = backgroundInfo.getCurrentEquipment().getValue()
        .subtract(delta.getValue());
    BigDecimal newRetiredValue = backgroundInfo.getCurrentRetired().getValue()
        .add(delta.getValue());

    EngineNumber newPrior = new EngineNumber(newPriorValue, "units");
    EngineNumber newEquipment = new EngineNumber(newEquipmentValue, "units");
    EngineNumber newRetired = new EngineNumber(newRetiredValue, "units");

    return new PostRetireNewLevels(
        newPrior,
        newEquipment,
        newRetired
    );
  }

  /**
   * Updates equipment-related streams in the simulation state.
   *
   * <div>This method applies the calculated new values to the priorEquipment and equipment
   * streams. Both updates use:
   * <ul>
   *   <li>subtractRecycling = false: values already account for retirement</li>
   *   <li>invalidatePriorEquipment = false (for priorEquipment): prevent circular invalidation</li>
   * </ul></div>
   *
   * @param scopeEffective The use key scope for stream updates
   * @param simulationState The simulation state to update
   * @param newLevels The calculated new stream values to apply
   */
  private void updateEquipmentStreams(UseKey scopeEffective, SimulationState simulationState,
      PostRetireNewLevels newLevels) {
    SimulationStateUpdate priorEquipmentStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("priorEquipment")
        .setValue(newLevels.getNewPrior())
        .setSubtractRecycling(false)
        .setInvalidatePriorEquipment(false)
        .build();
    simulationState.update(
        priorEquipmentStream
    );

    SimulationStateUpdate equipmentStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("equipment")
        .setValue(newLevels.getNewEquipment())
        .setSubtractRecycling(false)
        .build();
    simulationState.update(
        equipmentStream
    );
  }

  /**
   * Updates the retired stream with the new cumulative retired value.
   *
   * <p>This method applies the calculated new retired value to the retired stream, which
 * tracks the cumulative total of all equipment retired over time (not just this year's retirement).
 * The update uses subtractRecycling = false since the value already represents the correct
 * cumulative retired amount.</p>
   *
   * @param scopeEffective The use key scope for stream update
   * @param simulationState The simulation state to update
   * @param newLevels The calculated new stream values including new retired
   */
  private void updateRetiredStream(UseKey scopeEffective, SimulationState simulationState,
      PostRetireNewLevels newLevels) {
    SimulationStateUpdate retiredStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("retired")
        .setValue(newLevels.getNewRetired())
        .setSubtractRecycling(false)
        .build();
    simulationState.update(
        retiredStream
    );
  }

  /**
   * Recalculates end-of-life greenhouse gas emissions after retirement changes.
   *
   * <p>This method delegates to EolEmissionsRecalcStrategy to recalculate EOL emissions
 * based on the updated retirement stream values. This is necessary because retirement operations
 * change the amount of equipment being retired, which affects emissions calculations.</p>
   *
   * @param target The engine instance executing the recalculation
   * @param scopeEffective The use key scope for EOL emissions recalculation
   * @param kit The recalculation kit with dependencies
   */
  private void updateGhgAccounting(Engine target, UseKey scopeEffective, RecalcKit kit) {
    EolEmissionsRecalcStrategy eolStrategy = new EolEmissionsRecalcStrategy(
        Optional.of(scopeEffective)
    );
    eolStrategy.execute(target, kit);
  }

  /**
   * Propagates retirement changes to dependent recalculation strategies.
   *
   * <div>This method triggers dependent recalculations to maintain simulation state consistency
   * after retirement operations:
   * <ul>
   *   <li>PopulationChangeRecalcStrategy: Updates equipment population deltas</li>
   *   <li>ConsumptionRecalcStrategy: Recalculates substance consumption based on new populations</li>
   * </ul></div>
   *
   * <p><strong>Important:</strong> SalesRecalcStrategy is intentionally NOT called here.
 * Retirement should not recalculate sales because sales were already calculated correctly before
 * retirement with the proper population context. Recalculating sales after retirement would
 * incorrectly use post-retirement populations.</p>
   *
   * @param target The engine instance executing the recalculation
   * @param kit The recalculation kit with dependencies
   */
  private void propagateChanges(Engine target, RecalcKit kit) {
    PopulationChangeRecalcStrategy populationStrategy = new PopulationChangeRecalcStrategy(
        Optional.empty(),
        Optional.empty()
    );
    populationStrategy.execute(target, kit);

    ConsumptionRecalcStrategy consumptionStrategy = new ConsumptionRecalcStrategy(
        Optional.empty()
    );
    consumptionStrategy.execute(target, kit);
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    // Setup dependencies and validate scope
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    UseKey scopeEffective = scope.orElse(target.getScope());
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    if (application == null || substance == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating population change", "");
    }

    SimulationState simulationState = kit.getStreamKeeper();

    // Get current state
    RetireBackgroundInfo backgroundInfo = getCurrentState(
        scopeEffective,
        simulationState,
        unitConverter
    );

    // Calculate incremental retirement delta
    EngineNumber delta = calculateDelta(
        scopeEffective,
        simulationState,
        unitConverter,
        stateGetter,
        backgroundInfo
    );

    // Calculate new stream values
    PostRetireNewLevels newLevels = calculateNewLevels(delta, backgroundInfo);

    // Update simulation state
    updateEquipmentStreams(
        scopeEffective,
        simulationState,
        newLevels
    );
    updateRetiredStream(
        scopeEffective,
        simulationState,
        newLevels
    );

    // Track cumulative applied amount for next delta calculation
    EngineNumber cumulativeRateRaw = simulationState.getRetirementRate(scopeEffective);
    stateGetter.setPopulation(backgroundInfo.getBasePopulation());
    EngineNumber cumulativeAmount = unitConverter.convert(cumulativeRateRaw, "units");
    stateGetter.clearPopulation();
    simulationState.setAppliedRetirementAmount(
        scopeEffective,
        cumulativeAmount
    );

    // Update dependent calculations
    updateGhgAccounting(target, scopeEffective, kit);
    propagateChanges(target, kit);
  }
}
