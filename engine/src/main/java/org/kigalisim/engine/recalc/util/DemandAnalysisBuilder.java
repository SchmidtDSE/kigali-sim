/**
 * Builder for demand analysis calculations in sales recalculation.
 *
 * <p>This class encapsulates the logic for determining total substance demand, whether unit-based
 * specifications should be preserved, and the required virgin material after accounting for
 * recycling and induction effects. It pulls this logic out of
 * {@link org.kigalisim.engine.recalc.SalesRecalcStrategy} to keep that strategy focused on
 * orchestration.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc.util;

import java.math.BigDecimal;
import java.math.MathContext;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.support.EngineSupportUtils;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Builder for demand analysis calculations in sales recalculation.
 */
public class DemandAnalysisBuilder extends ValidatedBuilder<DemandAnalysis> {

  private EngineNumber rechargeVolume;
  private EngineNumber prechargeVolume;
  private EngineNumber volumeForNew;
  private BigDecimal implicitRechargeKg;
  private BigDecimal implicitPrechargeKg;
  private BigDecimal eolRecycledKg;
  private BigDecimal rechargeRecycledKg;
  private SimulationState simulationState;
  private UseKey scopeEffective;

  /**
   * Create a new DemandAnalysisBuilder.
   */
  public DemandAnalysisBuilder() {
    super("DemandAnalysis");
  }

  /**
   * Set the recharge volume.
   *
   * @param rechargeVolume The total recharge volume in kg
   * @return This builder for fluent chaining
   */
  public DemandAnalysisBuilder setRechargeVolume(EngineNumber rechargeVolume) {
    this.rechargeVolume = rechargeVolume;
    return this;
  }

  /**
   * Set the precharge volume.
   *
   * @param prechargeVolume The total precharge volume in kg
   * @return This builder for fluent chaining
   */
  public DemandAnalysisBuilder setPrechargeVolume(EngineNumber prechargeVolume) {
    this.prechargeVolume = prechargeVolume;
    return this;
  }

  /**
   * Set the volume needed for new equipment.
   *
   * @param volumeForNew The substance volume needed for new equipment in kg
   * @return This builder for fluent chaining
   */
  public DemandAnalysisBuilder setVolumeForNew(EngineNumber volumeForNew) {
    this.volumeForNew = volumeForNew;
    return this;
  }

  /**
   * Set the implicit recharge amount.
   *
   * @param implicitRechargeKg The implicit recharge from unit-based specs in kg
   * @return This builder for fluent chaining
   */
  public DemandAnalysisBuilder setImplicitRechargeKg(BigDecimal implicitRechargeKg) {
    this.implicitRechargeKg = implicitRechargeKg;
    return this;
  }

  /**
   * Set the implicit precharge amount.
   *
   * @param implicitPrechargeKg The implicit precharge from unit-based specs in kg
   * @return This builder for fluent chaining
   */
  public DemandAnalysisBuilder setImplicitPrechargeKg(BigDecimal implicitPrechargeKg) {
    this.implicitPrechargeKg = implicitPrechargeKg;
    return this;
  }

  /**
   * Set the end-of-life recycled material amount.
   *
   * @param eolRecycledKg The EOL recycled material in kg
   * @return This builder for fluent chaining
   */
  public DemandAnalysisBuilder setEolRecycledKg(BigDecimal eolRecycledKg) {
    this.eolRecycledKg = eolRecycledKg;
    return this;
  }

  /**
   * Set the recharge-stage recycled material amount.
   *
   * @param rechargeRecycledKg The recharge-stage recycled material in kg
   * @return This builder for fluent chaining
   */
  public DemandAnalysisBuilder setRechargeRecycledKg(BigDecimal rechargeRecycledKg) {
    this.rechargeRecycledKg = rechargeRecycledKg;
    return this;
  }

  /**
   * Set the simulation state.
   *
   * @param simulationState The simulation state to query for induction rates
   * @return This builder for fluent chaining
   */
  public DemandAnalysisBuilder setSimulationState(SimulationState simulationState) {
    this.simulationState = simulationState;
    return this;
  }

  /**
   * Set the effective scope.
   *
   * @param scopeEffective The use key scope for induction rate queries
   * @return This builder for fluent chaining
   */
  public DemandAnalysisBuilder setScopeEffective(UseKey scopeEffective) {
    this.scopeEffective = scopeEffective;
    return this;
  }

  /**
   * Build the demand analysis.
   *
   * <p>This method orchestrates the calculation of total demand, determination of unit-based
   * specification preservation, and calculation of required virgin material.</p>
   *
   * @return A DemandAnalysis containing the computed demand, spec type, and virgin material
   */
  @Override
  protected DemandAnalysis buildInternal() {
    BigDecimal totalDemand = calculateTotalDemand();
    boolean hasUnitBasedSpecs = getHasUnitBasedSpecs();
    BigDecimal requiredVirginMaterial = calculateRequiredVirginMaterial(
        totalDemand,
        hasUnitBasedSpecs
    );
    return new DemandAnalysis(totalDemand, hasUnitBasedSpecs, requiredVirginMaterial);
  }

  /**
   * Validate that all fields required to build a DemandAnalysis have been set.
   *
   * <p>Fails fast with a clear message instead of letting a missing field surface later as a
   * {@link NullPointerException} deep inside demand calculation.</p>
   *
   * @throws IllegalStateException if a required field is missing
   */
  @Override
  protected void validate() {
    requireField(rechargeVolume, "rechargeVolume");
    requireField(prechargeVolume, "prechargeVolume");
    requireField(volumeForNew, "volumeForNew");
    requireField(implicitRechargeKg, "implicitRechargeKg");
    requireField(implicitPrechargeKg, "implicitPrechargeKg");
    requireField(eolRecycledKg, "eolRecycledKg");
    requireField(rechargeRecycledKg, "rechargeRecycledKg");
    requireField(simulationState, "simulationState");
    requireField(scopeEffective, "scopeEffective");
  }

  /**
   * Determines if unit-based specifications should be preserved.
   *
   * <p>This method checks if we had unit-based specifications that need to be preserved by:</p>
   * <ol>
   *   <li>Checking if the context indicates unit-based sales specifications</li>
   *   <li>Verifying that the current operation is also unit-based by checking if implicit
   *   recharge or implicit precharge is present (indicating units were used in the current
   *   operation)</li>
   *   <li>If the current operation is kg-based (like displacement), unit specifications are
   *   not preserved</li>
   * </ol>
   *
   * @return true if unit-based specifications should be preserved
   */
  private boolean getHasUnitBasedSpecs() {
    boolean hasUnitBasedSpecs = EngineSupportUtils.hasUnitBasedSalesSpecifications(
        simulationState, scopeEffective);
    if (!hasUnitBasedSpecs) {
      return false;
    } else if (implicitRechargeKg.compareTo(BigDecimal.ZERO) > 0) {
      return true;
    } else if (implicitPrechargeKg.compareTo(BigDecimal.ZERO) > 0) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get the effective induction rate with appropriate defaults based on specification type.
   *
   * <p>If the induction rate was explicitly set, it is returned (converted from percentage to
   * ratio). Otherwise, a default behavior is applied based on the specification type: for unit-based
   * specs, the default is 0% induction (displacement behavior), while for non-unit specs, the
   * default is 100% induction (induced demand behavior).</p>
   *
   * @param stage the recovery stage (EOL or RECHARGE)
   * @param hasUnitBasedSpecs whether the specification is unit-based
   * @return the effective induction rate as a ratio (0.0 to 1.0)
   */
  private BigDecimal getEffectiveInductionRate(RecoveryStage stage, boolean hasUnitBasedSpecs) {
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
   * Calculates total substance demand from recharge and new equipment needs.
   *
   * <div>This method computes the baseline demand before accounting for:
   * <ul>
   *   <li>Implicit recharge (already included when units were specified)</li>
   *   <li>Implicit precharge (already included when units were specified)</li>
   *   <li>Recycling displacement and induction effects</li>
   * </ul></div>
   *
   * @return BigDecimal representing total demand minus implicit servicing in kg
   */
  private BigDecimal calculateTotalDemand() {
    BigDecimal totalDemand = rechargeVolume.getValue()
        .add(prechargeVolume.getValue())
        .add(volumeForNew.getValue());
    BigDecimal requiredKg = totalDemand.subtract(implicitRechargeKg)
        .subtract(implicitPrechargeKg);
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
   * @param hasUnitBasedSpecs Whether specifications are unit-based
   * @return BigDecimal representing required virgin material in kg
   */
  private BigDecimal calculateRequiredVirginMaterial(BigDecimal totalDemand,
      boolean hasUnitBasedSpecs) {
    if (hasUnitBasedSpecs) {
      return calculateRequiredVirginMaterialUnitsBased(totalDemand);
    } else {
      return calculateRequiredVirginMaterialVolumeBased(totalDemand);
    }
  }

  /**
   * Calculates required virgin material for unit-based specifications.
   *
   * <p>Starts with total demand (already excludes implicit recharge) and adds induced demand
   * based on induction rates (default 0% for displacement), maintaining unit-based specification
   * intent.</p>
   *
   * @param totalDemand Total demand before recycling effects
   * @return BigDecimal representing required virgin material in kg
   */
  private BigDecimal calculateRequiredVirginMaterialUnitsBased(BigDecimal totalDemand) {
    BigDecimal inductionRatioEol = getEffectiveInductionRate(RecoveryStage.EOL, true);
    BigDecimal inductionRatioRecharge = getEffectiveInductionRate(RecoveryStage.RECHARGE, true);

    BigDecimal eolInducedKg = eolRecycledKg.multiply(inductionRatioEol);
    BigDecimal rechargeInducedKg = rechargeRecycledKg.multiply(inductionRatioRecharge);

    BigDecimal inducedDemandKg = eolInducedKg.add(rechargeInducedKg);
    return totalDemand.add(inducedDemandKg);
  }

  /**
   * Calculates required virgin material for volume-based specifications.
   *
   * <p>Always subtracts the full recycled amount (baseline displacement), then adds back induced
   * demand based on induction rates (default 100%), and ensures a non-negative result.</p>
   *
   * @param totalDemand Total demand before recycling effects
   * @return BigDecimal representing required virgin material in kg
   */
  private BigDecimal calculateRequiredVirginMaterialVolumeBased(BigDecimal totalDemand) {
    BigDecimal inductionRatioEol = getEffectiveInductionRate(RecoveryStage.EOL, false);
    BigDecimal inductionRatioRecharge = getEffectiveInductionRate(RecoveryStage.RECHARGE, false);

    BigDecimal totalRecycledKg = eolRecycledKg.add(rechargeRecycledKg);
    BigDecimal totalRequiredKg = totalDemand.subtract(totalRecycledKg);

    BigDecimal eolInducedKg = eolRecycledKg.multiply(inductionRatioEol);
    BigDecimal rechargeInducedKg = rechargeRecycledKg.multiply(inductionRatioRecharge);
    BigDecimal totalInducedKg = eolInducedKg.add(rechargeInducedKg);
    totalRequiredKg = totalRequiredKg.add(totalInducedKg);

    return totalRequiredKg.max(BigDecimal.ZERO);
  }
}
