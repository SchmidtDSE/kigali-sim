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
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
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

    SimulationState streamKeeper = kit.getStreamKeeper();

    // Get recharge population
    EngineNumber basePopulation = target.getStream("priorEquipment", Optional.of(scopeEffective), Optional.empty());
    stateGetter.setPopulation(basePopulation);
    EngineNumber rechargePopRaw = streamKeeper.getRechargePopulation(scopeEffective);
    EngineNumber rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.clearPopulation();

    // Switch into recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    EngineNumber rechargeIntensityRaw = streamKeeper.getRechargeIntensity(scopeEffective);
    EngineNumber rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    // Determine initial charge
    EngineNumber initialChargeRaw = target.getInitialCharge("sales");
    EngineNumber initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");

    // Calculate EOL recycling (from actual retired equipment)
    EngineNumber retiredPopulationRaw = streamKeeper.getStream(scopeEffective, "retired");
    EngineNumber retiredPopulation = unitConverter.convert(retiredPopulationRaw, "units");

    // Calculate EOL volume from retired equipment
    stateGetter.setPopulation(retiredPopulation);
    EngineNumber eolVolumeRaw = streamKeeper.getStream(scopeEffective, "retired");
    EngineNumber eolVolume = unitConverter.convert(eolVolumeRaw, "kg");
    // Convert from units to kg using initial charge
    BigDecimal eolVolumeKg = retiredPopulation.getValue().multiply(initialCharge.getValue());
    EngineNumber eolVolumeConverted = new EngineNumber(eolVolumeKg, "kg");
    stateGetter.clearPopulation();

    BigDecimal eolRecycledKg = calculateRecyclingForStage(
        streamKeeper, stateGetter, unitConverter, scopeEffective,
        eolVolumeConverted, RecoveryStage.EOL);

    // Calculate recharge recycling (from recharge population)
    BigDecimal rechargeRecycledKg = calculateRecyclingForStage(
        streamKeeper, stateGetter, unitConverter, scopeEffective,
        rechargeVolume, RecoveryStage.RECHARGE);

    // Recycling does not apply cross-substance displacement
    // Only within-substance displacement (recycled material displaces virgin material of same substance)
    final BigDecimal recycledKg = eolRecycledKg.add(rechargeRecycledKg);

    // Switch out of recharge population
    stateGetter.clearPopulation();

    // Determine needs for new equipment deployment
    stateGetter.setAmortizedUnitVolume(initialCharge);
    UnitConverter converter = kit.getUnitConverter();
    EngineNumber populationChangeRaw = stateGetter.getPopulationChange(converter);
    EngineNumber populationChange = unitConverter.convert(populationChangeRaw, "units");
    EngineNumber volumeForNew = unitConverter.convert(populationChange, "kg");

    // Get prior population
    EngineNumber priorPopulationRaw = target.getStream("priorEquipment", Optional.of(scopeEffective), Optional.empty());
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    // Determine sales prior to recycling
    final BigDecimal kgForRecharge = rechargeVolume.getValue();
    final BigDecimal kgForNew = volumeForNew.getValue();

    // Return to original initial charge
    stateGetter.clearAmortizedUnitVolume();

    // Return original
    stateGetter.clearVolume();

    // Get distribution using centralized method
    SalesStreamDistribution distribution = streamKeeper.getDistribution(scopeEffective);

    final BigDecimal percentDomestic = distribution.getPercentDomestic();
    final BigDecimal percentImport = distribution.getPercentImport();

    // Set individual recycling streams
    EngineNumber newRecycleEolValue = new EngineNumber(eolRecycledKg, "kg");
    EngineNumber newRecycleRechargeValue = new EngineNumber(rechargeRecycledKg, "kg");
    SimulationStateUpdate recycleEolStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("recycleEol")
        .setValue(newRecycleEolValue)
        .setSubtractRecycling(false)
        .build();
    streamKeeper.update(recycleEolStream);

    SimulationStateUpdate recycleRechargeStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("recycleRecharge")
        .setValue(newRecycleRechargeValue)
        .setSubtractRecycling(false)
        .build();
    streamKeeper.update(recycleRechargeStream);

    // Also set total recycle stream for backward compatibility
    EngineNumber newRecycleValue = new EngineNumber(recycledKg, "kg");
    SimulationStateUpdate recycleStream = new SimulationStateUpdateBuilder()
        .setUseKey(scopeEffective)
        .setName("recycle")
        .setValue(newRecycleValue)
        .setSubtractRecycling(false)
        .build();
    streamKeeper.update(recycleStream);

    // Get implicit recharge to avoid double-counting
    EngineNumber implicitRechargeRaw = target.getStream("implicitRecharge", Optional.of(scopeEffective), Optional.empty());
    EngineNumber implicitRecharge = unitConverter.convert(implicitRechargeRaw, "kg");
    BigDecimal implicitRechargeKg = implicitRecharge.getValue();

    // Determine specification type early
    boolean hasUnitBasedSpecsEarly = getHasUnitBasedSpecs(streamKeeper, scopeEffective, implicitRechargeKg);

    // Deal with implicit recharge and recycling
    // Total demand is recharge + new equipment needs
    BigDecimal totalDemand = kgForRecharge.add(kgForNew);

    // Subtract what we can fulfill from other sources:
    // - implicitRechargeKg: recharge that was already added when units were specified
    BigDecimal requiredKg = totalDemand
        .subtract(implicitRechargeKg);

    // Calculate induced demand based on induction rate using unified logic for all specifications
    BigDecimal inducedDemandKg = BigDecimal.ZERO;
    boolean hasUnitBasedSpecs = getHasUnitBasedSpecs(streamKeeper, scopeEffective, implicitRechargeKg);

    if (hasUnitBasedSpecs) {
      // Get effective induction rate (default is 0% for displacement behavior in unit-based specs)
      BigDecimal inductionRatioEol = getEffectiveInductionRate(streamKeeper, scopeEffective, RecoveryStage.EOL, hasUnitBasedSpecs);
      BigDecimal inductionRatioRecharge = getEffectiveInductionRate(streamKeeper, scopeEffective, RecoveryStage.RECHARGE, hasUnitBasedSpecs);

      // Calculate induced demand for each stage
      BigDecimal eolInducedKg = eolRecycledKg.multiply(inductionRatioEol);
      BigDecimal rechargeInducedKg = rechargeRecycledKg.multiply(inductionRatioRecharge);

      // Total induced demand
      inducedDemandKg = eolInducedKg.add(rechargeInducedKg);
    }

    // Apply different logic for unit-based vs non-unit specifications
    BigDecimal totalRequiredKg;

    if (hasUnitBasedSpecs) {
      // Unit-based: Add induced demand to required kg
      totalRequiredKg = requiredKg.add(inducedDemandKg);
    } else {
      // Non-unit based: Apply both displacement and induction effects
      // When induction = 0%: full displacement (subtract all recycling), no induction
      // When induction = 100%: no displacement, full induction (add all recycling)

      // Get effective induction rates for non-units specifications
      BigDecimal inductionRatioEol = getEffectiveInductionRate(streamKeeper, scopeEffective, RecoveryStage.EOL, hasUnitBasedSpecs);
      BigDecimal inductionRatioRecharge = getEffectiveInductionRate(streamKeeper, scopeEffective, RecoveryStage.RECHARGE, hasUnitBasedSpecs);

      // Step 1: Always subtract baseline recycling displacement (recycling always displaces virgin material)
      BigDecimal totalRecycledKg = eolRecycledKg.add(rechargeRecycledKg);
      totalRequiredKg = requiredKg.subtract(totalRecycledKg);

      // Step 2: Add back induced demand based on induction rates
      BigDecimal eolInducedKg = eolRecycledKg.multiply(inductionRatioEol);
      BigDecimal rechargeInducedKg = rechargeRecycledKg.multiply(inductionRatioRecharge);
      BigDecimal totalInducedKg = eolInducedKg.add(rechargeInducedKg);
      totalRequiredKg = totalRequiredKg.add(totalInducedKg);

      // Ensure sales don't go negative
      totalRequiredKg = totalRequiredKg.max(BigDecimal.ZERO);
    }

    BigDecimal newDomesticKg = percentDomestic.multiply(totalRequiredKg);
    BigDecimal newImportKg = percentImport.multiply(totalRequiredKg);


    if (hasUnitBasedSpecs) {
      // Convert back to units to preserve user intent
      // This ensures that unit-based specifications are maintained through recycling operations
      // Need to set up the converter state for proper unit conversion
      stateGetter.setAmortizedUnitVolume(initialCharge);

      // Only set streams that have non-zero allocations (i.e., are enabled)
      if (percentDomestic.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber newDomesticUnits = unitConverter.convert(
            new EngineNumber(newDomesticKg, "kg"), "units");
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

      if (percentImport.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber newImportUnits = unitConverter.convert(
            new EngineNumber(newImportKg, "kg"), "units");
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

      // Clear the state after conversion
      stateGetter.clearAmortizedUnitVolume();
    } else {
      // Normal kg-based setting for non-unit specifications
      // Only set streams that have non-zero allocations (i.e., are enabled)
      if (percentDomestic.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber newDomestic = new EngineNumber(newDomesticKg, "kg");
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

      if (percentImport.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber newImport = new EngineNumber(newImportKg, "kg");
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

  /**
   * Determines if unit-based specifications should be preserved.
   *
   * @param streamKeeper the stream keeper to use for checking specifications
   * @param scopeEffective the scope to check
   * @param implicitRechargeKg the implicit recharge amount in kg
   * @return true if unit-based specifications should be preserved
   */
  private boolean getHasUnitBasedSpecs(SimulationState streamKeeper, UseKey scopeEffective, BigDecimal implicitRechargeKg) {
    // Check if we had unit-based specifications that need to be preserved
    boolean hasUnitBasedSpecs = EngineSupportUtils.hasUnitBasedSalesSpecifications(streamKeeper, scopeEffective);

    if (hasUnitBasedSpecs) {
      // Check if the current values indicate a unit-based operation
      // If implicit recharge is present, we know units were used in the current operation
      // TODO: Consider making this explicit rather than using implicit recharge as a heuristic
      boolean currentOperationIsUnitBased = implicitRechargeKg.compareTo(BigDecimal.ZERO) > 0;

      if (!currentOperationIsUnitBased) {
        // Current operation is kg-based (like displacement), don't preserve units
        hasUnitBasedSpecs = false;
      }
    }

    return hasUnitBasedSpecs;
  }

  /**
   * Calculate recycling for a specific stage (EOL or RECHARGE).
   *
   * @param streamKeeper the stream keeper to use for getting rates
   * @param stateGetter the state getter for volume calculations
   * @param unitConverter the unit converter to use
   * @param scopeEffective the scope to calculate for
   * @param baseVolume the base volume to use for calculations
   * @param stage the recovery stage (EOL or RECHARGE)
   * @return the recycled amount in kg for this stage
   */
  private BigDecimal calculateRecyclingForStage(
      SimulationState streamKeeper,
      OverridingConverterStateGetter stateGetter,
      UnitConverter unitConverter,
      UseKey scopeEffective,
      EngineNumber baseVolume,
      RecoveryStage stage) {

    // Get recycling volume (recovery rate) for this stage
    stateGetter.setVolume(baseVolume);
    EngineNumber recoveryVolumeRaw = streamKeeper.getRecoveryRate(scopeEffective, stage);
    EngineNumber recoveryVolume = unitConverter.convert(recoveryVolumeRaw, "kg");
    stateGetter.clearVolume();

    // Get recycling amount (yield rate) for this stage
    stateGetter.setVolume(recoveryVolume);
    EngineNumber recycledVolumeRaw = streamKeeper.getYieldRate(scopeEffective, stage);
    EngineNumber recycledVolume = unitConverter.convert(recycledVolumeRaw, "kg");
    stateGetter.clearVolume();

    return recycledVolume.getValue();
  }

  /**
   * Get the effective induction rate with appropriate defaults based on specification type.
   *
   * @param streamKeeper the stream keeper to get induction rate from
   * @param scopeEffective the scope to get induction rate for
   * @param stage the recovery stage (EOL or RECHARGE)
   * @param hasUnitBasedSpecs whether the specification is unit-based
   * @return the effective induction rate as a ratio (0.0 to 1.0)
   */
  private BigDecimal getEffectiveInductionRate(SimulationState streamKeeper, UseKey scopeEffective, RecoveryStage stage, boolean hasUnitBasedSpecs) {
    EngineNumber inductionRate = streamKeeper.getInductionRate(scopeEffective, stage);

    // Check if induction rate was explicitly set (not null)
    boolean wasExplicitlySet = inductionRate != null;

    if (wasExplicitlySet) {
      // Use the explicitly set value
      return inductionRate.getValue().divide(BigDecimal.valueOf(100), java.math.MathContext.DECIMAL128);
    } else {
      // Apply default behavior based on specification type
      if (hasUnitBasedSpecs) {
        // Units-based specs: default is 0% induction (displacement behavior)
        return BigDecimal.ZERO;
      } else {
        // Non-units specs: default is 100% induction (induced demand behavior)
        return BigDecimal.ONE;
      }
    }
  }

}
