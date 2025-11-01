/**
 * Strategy for recalculating sales.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcSales method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.SimulationStateUpdate;
import org.kigalisim.engine.state.SimulationStateUpdateBuilder;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.support.EngineSupportUtils;
import org.kigalisim.engine.support.ExceptionsGenerator;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Strategy for recalculating sales.
 */
public class SalesRecalcStrategy implements RecalcStrategy {

  private final Optional<UseKey> scope;

  /**
   * Create a new SalesRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   */
  public SalesRecalcStrategy(Optional<UseKey> scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    UseKey scopeEffective = scope.orElse(target.getScope());

    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);

    if (scopeEffective.getApplication() == null || scopeEffective.getSubstance() == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating sales", "");
    }

    SimulationState simulationState = kit.getStreamKeeper();

    EngineNumber rechargeBase = initializeRechargeBase(scopeEffective, simulationState, unitConverter, target);
    EngineNumber rechargeVolume = calculateTotalRecharge(scopeEffective, simulationState, stateGetter, unitConverter, rechargeBase);

    EngineNumber initialChargeRaw = target.getInitialCharge("sales");
    EngineNumber initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");

    EngineNumber eolVolumeConverted = calculateEolRecyclingVolume(scopeEffective, simulationState, stateGetter, unitConverter, initialCharge);
    final BigDecimal eolRecycledKg = calculateRecyclingForStage(
        simulationState,
        stateGetter,
        unitConverter,
        scopeEffective,
        eolVolumeConverted,
        RecoveryStage.EOL
    );

    final BigDecimal rechargeRecycledKg = calculateRecyclingForStage(
        simulationState,
        stateGetter,
        unitConverter,
        scopeEffective,
        rechargeVolume,
        RecoveryStage.RECHARGE
    );

    stateGetter.clearPopulation();

    final EngineNumber volumeForNew = calculateNewEquipmentNeeds(stateGetter, unitConverter, kit.getUnitConverter(), initialCharge);

    EngineNumber priorPopulationRaw = target.getStream("priorEquipment", Optional.of(scopeEffective), Optional.empty());
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    stateGetter.clearAmortizedUnitVolume();
    stateGetter.clearVolume();

    SalesStreamDistribution distribution = simulationState.getDistribution(scopeEffective);

    updateRecyclingStreams(scopeEffective, simulationState, eolRecycledKg, rechargeRecycledKg);

    EngineNumber implicitRechargeRaw = target.getStream("implicitRecharge", Optional.of(scopeEffective), Optional.empty());
    EngineNumber implicitRecharge = unitConverter.convert(implicitRechargeRaw, "kg");
    BigDecimal implicitRechargeKg = implicitRecharge.getValue();

    BigDecimal requiredKg = calculateTotalDemand(rechargeVolume, volumeForNew, implicitRechargeKg);
    boolean hasUnitBasedSpecs = getHasUnitBasedSpecs(simulationState, scopeEffective, implicitRechargeKg);
    BigDecimal totalRequiredKg = calculateRequiredVirginMaterial(requiredKg, eolRecycledKg, rechargeRecycledKg, simulationState, scopeEffective, hasUnitBasedSpecs);

    BigDecimal newDomesticKg = distribution.getPercentDomestic().multiply(totalRequiredKg);
    BigDecimal newImportKg = distribution.getPercentImport().multiply(totalRequiredKg);

    updateSalesStreams(target, scopeEffective, newDomesticKg, newImportKg, distribution, hasUnitBasedSpecs, initialCharge, stateGetter, unitConverter);
  }

  /**
   * Determines if unit-based specifications should be preserved.
   *
   * <p>This method checks if we had unit-based specifications that need to be preserved by:</p>
   * <ol>
   *   <li>Checking if the context indicates unit-based sales specifications</li>
   *   <li>Verifying that the current operation is also unit-based by checking if implicit
   *   recharge is present (indicating units were used in the current operation)</li>
   *   <li>If the current operation is kg-based (like displacement), unit specifications are
   *   not preserved</li>
   * </ol>
   *
   * @param simulationState the simulation state to use for checking specifications
   * @param scopeEffective the scope to check
   * @param implicitRechargeKg the implicit recharge amount in kg
   * @return true if unit-based specifications should be preserved
   */
  private boolean getHasUnitBasedSpecs(SimulationState simulationState, UseKey scopeEffective, BigDecimal implicitRechargeKg) {
    boolean hasUnitBasedSpecs = EngineSupportUtils.hasUnitBasedSalesSpecifications(simulationState, scopeEffective);
    return hasUnitBasedSpecs
        ? implicitRechargeKg.compareTo(BigDecimal.ZERO) > 0
        : false;
  }

  /**
   * Calculate recycling for a specific stage (EOL or RECHARGE).
   *
   * <p>This method calculates the recycled amount in kg for a given recovery stage through two
   * steps: first, it determines the recycling volume (recovery rate) for this stage, then it
   * calculates the recycling amount (yield rate) for this stage.</p>
   *
   * @param simulationState the simulation state to use for getting rates
   * @param stateGetter the state getter for volume calculations
   * @param unitConverter the unit converter to use
   * @param scopeEffective the scope to calculate for
   * @param baseVolume the base volume to use for calculations
   * @param stage the recovery stage (EOL or RECHARGE)
   * @return the recycled amount in kg for this stage
   */
  private BigDecimal calculateRecyclingForStage(SimulationState simulationState,
      OverridingConverterStateGetter stateGetter, UnitConverter unitConverter,
      UseKey scopeEffective, EngineNumber baseVolume, RecoveryStage stage) {

    stateGetter.setVolume(baseVolume);
    EngineNumber recoveryVolumeRaw = simulationState.getRecoveryRate(scopeEffective, stage);
    EngineNumber recoveryVolume = unitConverter.convert(recoveryVolumeRaw, "kg");
    stateGetter.clearVolume();

    stateGetter.setVolume(recoveryVolume);
    EngineNumber recycledVolumeRaw = simulationState.getYieldRate(scopeEffective, stage);
    EngineNumber recycledVolume = unitConverter.convert(recycledVolumeRaw, "kg");
    stateGetter.clearVolume();

    return recycledVolume.getValue();
  }

  /**
   * Get the effective induction rate with appropriate defaults based on specification type.
   *
   * <p>If the induction rate was explicitly set, it is returned (converted from percentage to
   * ratio). Otherwise, a default behavior is applied based on the specification type: for
   * unit-based specs, the default is 0% induction (displacement behavior), while for non-unit
   * specs, the default is 100% induction (induced demand behavior).</p>
   *
   * @param simulationState the simulation state to get induction rate from
   * @param scopeEffective the scope to get induction rate for
   * @param stage the recovery stage (EOL or RECHARGE)
   * @param hasUnitBasedSpecs whether the specification is unit-based
   * @return the effective induction rate as a ratio (0.0 to 1.0)
   */
  private BigDecimal getEffectiveInductionRate(SimulationState simulationState,
      UseKey scopeEffective, RecoveryStage stage, boolean hasUnitBasedSpecs) {
    EngineNumber inductionRate = simulationState.getInductionRate(scopeEffective, stage);

    boolean wasExplicitlySet = inductionRate != null;

    if (wasExplicitlySet) {
      return inductionRate.getValue().divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
    } else {
      if (hasUnitBasedSpecs) {
        return BigDecimal.ZERO;
      } else {
        return BigDecimal.ONE;
      }
    }
  }

  /**
   * Initializes the recharge base population for this scope if not already set.
   *
   * <p>On the first recharge operation for a given scope, this method captures the current
   * prior equipment value as the base population for all future recharge calculations. This
   * ensures that recharge percentages are consistently applied against the same baseline
   * throughout the simulation.</p>
   *
   * <p>Side effect: If this is the first recharge, sets the recharge base population in
   * the simulation state to the current priorEquipment value.</p>
   *
   * @param scopeEffective The use key scope for this calculation
   * @param simulationState The simulation state to query/update
   * @param unitConverter Unit converter for normalizing values to units
   * @param target The engine instance for stream access
   * @return EngineNumber representing the recharge base population in units
   */
  private EngineNumber initializeRechargeBase(UseKey scopeEffective,
      SimulationState simulationState, UnitConverter unitConverter, Engine target) {
    Optional<EngineNumber> rechargeBaseOpt = simulationState.getRechargeBasePopulation(scopeEffective);
    EngineNumber rechargeBase;
    boolean firstRecharge = !rechargeBaseOpt.isPresent();
    if (firstRecharge) {
      EngineNumber currentPriorRaw = target.getStream("priorEquipment", Optional.of(scopeEffective), Optional.empty());
      EngineNumber currentPrior = unitConverter.convert(currentPriorRaw, "units");
      simulationState.setRechargeBasePopulation(scopeEffective, currentPrior);
      rechargeBase = currentPrior;
    } else {
      rechargeBase = rechargeBaseOpt.get();
    }
    return rechargeBase;
  }

  /**
   * Calculates the total recharge volume based on recharge base population.
   *
   * <p>This method performs a two-step calculation:</p>
   * <ol>
   *   <li>Converts the recharge population percentage to absolute units using the base population</li>
   *   <li>Calculates the weighted-average recharge intensity (kg) for those units</li>
   * </ol>
   *
   * <p>The calculation uses OverridingConverterStateGetter to temporarily set population
   * context for accurate percentage-to-absolute conversions.</p>
   *
   * @param scopeEffective The use key scope for this calculation
   * @param simulationState The simulation state to query
   * @param stateGetter State getter with population override capability
   * @param unitConverter Unit converter for converting values
   * @param rechargeBase The base population for recharge calculations
   * @return EngineNumber representing total recharge volume in kg
   */
  private EngineNumber calculateTotalRecharge(UseKey scopeEffective,
      SimulationState simulationState, OverridingConverterStateGetter stateGetter,
      UnitConverter unitConverter, EngineNumber rechargeBase) {
    stateGetter.setPopulation(rechargeBase);
    EngineNumber rechargePopRaw = simulationState.getRechargePopulation(scopeEffective);
    EngineNumber totalRechargeUnits = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.clearPopulation();

    stateGetter.setPopulation(totalRechargeUnits);
    EngineNumber rechargeIntensityRaw = simulationState.getRechargeIntensity(scopeEffective);
    EngineNumber totalRechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    return totalRechargeVolume;
  }

  /**
   * Calculates end-of-life recycling volume from retired equipment.
   *
   * <p>This method determines the total substance volume available for recycling at
   * equipment end-of-life by multiplying retired equipment units by the initial charge
   * per unit. The result is passed to {@link #calculateRecyclingForStage} to apply
   * recovery and yield rates.</p>
   *
   * @param scopeEffective The use key scope for this calculation
   * @param simulationState The simulation state to query
   * @param stateGetter State getter with population override capability
   * @param unitConverter Unit converter for converting values
   * @param initialCharge Initial charge per unit in kg/unit
   * @return EngineNumber representing total EOL volume available for recycling in kg
   */
  private EngineNumber calculateEolRecyclingVolume(UseKey scopeEffective,
      SimulationState simulationState, OverridingConverterStateGetter stateGetter,
      UnitConverter unitConverter, EngineNumber initialCharge) {
    EngineNumber retiredPopulationRaw = simulationState.getStream(scopeEffective, "retired");
    EngineNumber retiredPopulation = unitConverter.convert(retiredPopulationRaw, "units");

    stateGetter.setPopulation(retiredPopulation);
    EngineNumber eolVolumeRaw = simulationState.getStream(scopeEffective, "retired");
    EngineNumber eolVolume = unitConverter.convert(eolVolumeRaw, "kg");
    BigDecimal eolVolumeKg = retiredPopulation.getValue().multiply(initialCharge.getValue());
    EngineNumber eolVolumeConverted = new EngineNumber(eolVolumeKg, "kg");
    stateGetter.clearPopulation();

    return eolVolumeConverted;
  }

  /**
   * Calculates the substance volume needed for new equipment deployment.
   *
   * <p>This method determines how much substance is required for new equipment by:</p>
   * <ol>
   *   <li>Getting the population change (delta in equipment units)</li>
   *   <li>Converting population change to substance volume using initial charge</li>
   * </ol>
   *
   * <p>Uses the amortized unit volume approach to ensure proper conversion from units
   * to kg based on the initial charge per unit.</p>
   *
   * @param stateGetter State getter with override capability for unit conversions
   * @param unitConverter Unit converter for converting population to volume
   * @param converter Additional converter from the RecalcKit
   * @param initialCharge Initial charge per unit in kg/unit
   * @return EngineNumber representing volume needed for new equipment in kg
   */
  private EngineNumber calculateNewEquipmentNeeds(OverridingConverterStateGetter stateGetter,
      UnitConverter unitConverter, UnitConverter converter, EngineNumber initialCharge) {
    stateGetter.setAmortizedUnitVolume(initialCharge);
    EngineNumber populationChangeRaw = stateGetter.getPopulationChange(converter);
    EngineNumber populationChange = unitConverter.convert(populationChangeRaw, "units");
    EngineNumber volumeForNew = unitConverter.convert(populationChange, "kg");

    return volumeForNew;
  }

  /**
   * Updates recycling streams with calculated EOL and recharge recycling values.
   *
   * <div>This method updates three streams:
   * <ul>
   *   <li>recycleEol: Recycled material from equipment retirement</li>
   *   <li>recycleRecharge: Recycled material from recharge/servicing operations</li>
   *   <li>recycle: Total recycled material (for backward compatibility)</li>
   * </ul></div>
   *
   * <p>All streams are updated with subtractRecycling = false since these values
   * represent the recycled amounts themselves, not values that need recycling applied.</p>
   *
   * @param scopeEffective The use key scope for stream updates
   * @param simulationState The simulation state to update
   * @param eolRecycledKg EOL recycling amount in kg
   * @param rechargeRecycledKg Recharge-stage recycling amount in kg
   */
  private void updateRecyclingStreams(UseKey scopeEffective, SimulationState simulationState,
      BigDecimal eolRecycledKg, BigDecimal rechargeRecycledKg) {
    EngineNumber newRecycleEolValue = new EngineNumber(eolRecycledKg, "kg");
    EngineNumber newRecycleRechargeValue = new EngineNumber(rechargeRecycledKg, "kg");
    SimulationStateUpdate recycleEolStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("recycleEol")
        .setValue(newRecycleEolValue)
        .setSubtractRecycling(false)
        .build();
    simulationState.update(recycleEolStream);

    SimulationStateUpdate recycleRechargeStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("recycleRecharge")
        .setValue(newRecycleRechargeValue)
        .setSubtractRecycling(false)
        .build();
    simulationState.update(recycleRechargeStream);

    BigDecimal recycledKg = eolRecycledKg.add(rechargeRecycledKg);
    EngineNumber newRecycleValue = new EngineNumber(recycledKg, "kg");
    SimulationStateUpdate recycleStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("recycle")
        .setValue(newRecycleValue)
        .setSubtractRecycling(false)
        .build();
    simulationState.update(recycleStream);
  }

  /**
   * Calculates total substance demand from recharge and new equipment needs.
   *
   * <div>This method computes the baseline demand before accounting for:
   * <ul>
   *   <li>Implicit recharge (already included when units were specified)</li>
   *   <li>Recycling displacement and induction effects</li>
   * </ul></div>
   *
   * @param rechargeVolume Total recharge volume in kg
   * @param newEquipmentVolume Volume needed for new equipment in kg
   * @param implicitRechargeKg Implicit recharge from unit-based specs in kg
   * @return BigDecimal representing total demand minus implicit recharge in kg
   */
  private BigDecimal calculateTotalDemand(EngineNumber rechargeVolume,
      EngineNumber newEquipmentVolume, BigDecimal implicitRechargeKg) {
    BigDecimal totalDemand = rechargeVolume.getValue().add(newEquipmentVolume.getValue());
    BigDecimal requiredKg = totalDemand.subtract(implicitRechargeKg);
    return requiredKg;
  }

  /**
   * Calculates required virgin material after applying recycling and induction effects.
   *
   * <p>This method applies different logic based on specification type:</p>
   *
   * <div><strong>Unit-based specifications:</strong>
   * <ul>
   *   <li>Start with total demand (already excludes implicit recharge)</li>
   *   <li>Add induced demand based on induction rates (default 0% for displacement)</li>
   *   <li>Result maintains unit-based specification intent</li>
   * </ul></div>
   *
   * <div><strong>Volume-based specifications:</strong>
   * <ul>
   *   <li>Always subtract full recycled amount (baseline displacement)</li>
   *   <li>Add back induced demand based on induction rates (default 100%)</li>
   *   <li>Ensure non-negative result</li>
   * </ul></div>
   *
   * <div>Induction rates control how recycled material affects total supply:
   * <ul>
   *   <li>0% induction: Full displacement (recycling reduces virgin demand)</li>
   *   <li>100% induction: Full induced demand (recycling adds to total supply)</li>
   *   <li>Partial: Proportional combination of both effects</li>
   * </ul></div>
   *
   * @param totalDemand Total demand before recycling effects
   * @param eolRecycledKg EOL recycled material in kg
   * @param rechargeRecycledKg Recharge-stage recycled material in kg
   * @param simulationState Simulation state for induction rate queries
   * @param scopeEffective Scope for induction rate queries
   * @param hasUnitBasedSpecs Whether specifications are unit-based
   * @return BigDecimal representing required virgin material in kg
   */
  private BigDecimal calculateRequiredVirginMaterial(BigDecimal totalDemand,
      BigDecimal eolRecycledKg, BigDecimal rechargeRecycledKg,
      SimulationState simulationState, UseKey scopeEffective, boolean hasUnitBasedSpecs) {
    BigDecimal totalRequiredKg;

    if (hasUnitBasedSpecs) {
      BigDecimal inductionRatioEol = getEffectiveInductionRate(simulationState, scopeEffective, RecoveryStage.EOL, hasUnitBasedSpecs);
      BigDecimal inductionRatioRecharge = getEffectiveInductionRate(simulationState, scopeEffective, RecoveryStage.RECHARGE, hasUnitBasedSpecs);

      BigDecimal eolInducedKg = eolRecycledKg.multiply(inductionRatioEol);
      BigDecimal rechargeInducedKg = rechargeRecycledKg.multiply(inductionRatioRecharge);

      BigDecimal inducedDemandKg = eolInducedKg.add(rechargeInducedKg);
      totalRequiredKg = totalDemand.add(inducedDemandKg);
    } else {
      BigDecimal inductionRatioEol = getEffectiveInductionRate(simulationState, scopeEffective, RecoveryStage.EOL, hasUnitBasedSpecs);
      BigDecimal inductionRatioRecharge = getEffectiveInductionRate(simulationState, scopeEffective, RecoveryStage.RECHARGE, hasUnitBasedSpecs);

      BigDecimal totalRecycledKg = eolRecycledKg.add(rechargeRecycledKg);
      totalRequiredKg = totalDemand.subtract(totalRecycledKg);

      BigDecimal eolInducedKg = eolRecycledKg.multiply(inductionRatioEol);
      BigDecimal rechargeInducedKg = rechargeRecycledKg.multiply(inductionRatioRecharge);
      BigDecimal totalInducedKg = eolInducedKg.add(rechargeInducedKg);
      totalRequiredKg = totalRequiredKg.add(totalInducedKg);

      totalRequiredKg = totalRequiredKg.max(BigDecimal.ZERO);
    }

    return totalRequiredKg;
  }

  /**
   * Updates sales streams (domestic and import) based on specification type.
   *
   * <p>This method applies different update strategies based on specification type:</p>
   *
   * <div><strong>Unit-based specifications:</strong>
   * <ul>
   *   <li>Convert kg values back to units using initial charge</li>
   *   <li>Update streams with subtractRecycling = true to preserve user intent</li>
   *   <li>Ensures unit-based specifications are maintained through recycling</li>
   * </ul></div>
   *
   * <div><strong>Volume-based specifications:</strong>
   * <ul>
   *   <li>Update streams directly in kg</li>
   *   <li>Update streams with subtractRecycling = false</li>
   * </ul></div>
   *
   * <p>Only streams with non-zero allocations (enabled streams) are updated.</p>
   *
   * @param target Engine instance for executing stream updates
   * @param scopeEffective Scope for stream updates
   * @param domesticKg Domestic sales amount in kg
   * @param importKg Import sales amount in kg
   * @param distribution Sales stream distribution (domestic/import percentages)
   * @param hasUnitBasedSpecs Whether specifications are unit-based
   * @param initialCharge Initial charge per unit for unit conversion
   * @param stateGetter State getter for unit conversion context
   * @param unitConverter Unit converter for kg-to-units conversion
   */
  private void updateSalesStreams(Engine target, UseKey scopeEffective,
      BigDecimal domesticKg, BigDecimal importKg, SalesStreamDistribution distribution,
      boolean hasUnitBasedSpecs, EngineNumber initialCharge,
      OverridingConverterStateGetter stateGetter, UnitConverter unitConverter) {
    boolean hasDomestic = distribution.getPercentDomestic().compareTo(BigDecimal.ZERO) > 0;
    boolean hasImports = distribution.getPercentImport().compareTo(BigDecimal.ZERO) > 0;

    if (hasUnitBasedSpecs) {
      stateGetter.setAmortizedUnitVolume(initialCharge);

      if (hasDomestic) {
        EngineNumber newDomesticUnits = unitConverter.convert(
            new EngineNumber(domesticKg, "kg"), "units");
        StreamUpdate domesticUpdate = new StreamUpdateBuilder()
            .setName("domestic")
            .setValue(newDomesticUnits)
            .setKey(scopeEffective)
            .setPropagateChanges(false)
            .setSubtractRecycling(hasUnitBasedSpecs)
            .setDistribution(distribution)
            .build();
        target.executeStreamUpdate(domesticUpdate);
      }

      if (hasImports) {
        EngineNumber newImportUnits = unitConverter.convert(
            new EngineNumber(importKg, "kg"), "units");
        StreamUpdate importUpdate = new StreamUpdateBuilder()
            .setName("import")
            .setValue(newImportUnits)
            .setKey(scopeEffective)
            .setPropagateChanges(false)
            .setSubtractRecycling(hasUnitBasedSpecs)
            .setDistribution(distribution)
            .build();
        target.executeStreamUpdate(importUpdate);
      }

      stateGetter.clearAmortizedUnitVolume();
    } else {
      if (hasDomestic) {
        EngineNumber newDomestic = new EngineNumber(domesticKg, "kg");
        StreamUpdate domesticUpdate = new StreamUpdateBuilder()
            .setName("domestic")
            .setValue(newDomestic)
            .setKey(scopeEffective)
            .setPropagateChanges(false)
            .setSubtractRecycling(hasUnitBasedSpecs)
            .setDistribution(distribution)
            .build();
        target.executeStreamUpdate(domesticUpdate);
      }

      if (hasImports) {
        EngineNumber newImport = new EngineNumber(importKg, "kg");
        StreamUpdate importUpdate = new StreamUpdateBuilder()
            .setName("import")
            .setValue(newImport)
            .setKey(scopeEffective)
            .setPropagateChanges(false)
            .setSubtractRecycling(hasUnitBasedSpecs)
            .setDistribution(distribution)
            .build();
        target.executeStreamUpdate(importUpdate);
      }
    }
  }

}
