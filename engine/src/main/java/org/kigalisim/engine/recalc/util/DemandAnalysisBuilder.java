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
public class DemandAnalysisBuilder {

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
  public DemandAnalysis build() {
    BigDecimal totalDemand = calculateTotalDemand(rechargeVolume, prechargeVolume, volumeForNew,
        implicitRechargeKg, implicitPrechargeKg);
    boolean hasUnitBasedSpecs = getHasUnitBasedSpecs(simulationState, scopeEffective,
        implicitRechargeKg, implicitPrechargeKg);
    BigDecimal requiredVirginMaterial = calculateRequiredVirginMaterial(totalDemand,
        eolRecycledKg, rechargeRecycledKg, simulationState, scopeEffective, hasUnitBasedSpecs);
    return new DemandAnalysis(totalDemand, hasUnitBasedSpecs, requiredVirginMaterial);
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
   * @param simulationState the simulation state to use for checking specifications
   * @param scopeEffective the scope to check
   * @param implicitRechargeKg the implicit recharge amount in kg
   * @param implicitPrechargeKg the implicit precharge amount in kg
   * @return true if unit-based specifications should be preserved
   */
  private boolean getHasUnitBasedSpecs(SimulationState simulationState, UseKey scopeEffective,
      BigDecimal implicitRechargeKg, BigDecimal implicitPrechargeKg) {
    boolean hasUnitBasedSpecs = EngineSupportUtils.hasUnitBasedSalesSpecifications(
        simulationState, scopeEffective);
    if (!hasUnitBasedSpecs) {
      return false;
    }
    if (implicitRechargeKg.compareTo(BigDecimal.ZERO) > 0) {
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
   * Calculates total substance demand from recharge and new equipment needs.
   *
   * <div>This method computes the baseline demand before accounting for:
   * <ul>
   *   <li>Implicit recharge (already included when units were specified)</li>
   *   <li>Implicit precharge (already included when units were specified)</li>
   *   <li>Recycling displacement and induction effects</li>
   * </ul></div>
   *
   * @param rechargeVolume Total recharge volume in kg
   * @param prechargeVolume Total precharge volume in kg
   * @param newEquipmentVolume Volume needed for new equipment in kg
   * @param implicitRechargeKg Implicit recharge from unit-based specs in kg
   * @param implicitPrechargeKg Implicit precharge from unit-based specs in kg
   * @return BigDecimal representing total demand minus implicit servicing in kg
   */
  private BigDecimal calculateTotalDemand(EngineNumber rechargeVolume,
      EngineNumber prechargeVolume, EngineNumber newEquipmentVolume,
      BigDecimal implicitRechargeKg, BigDecimal implicitPrechargeKg) {
    BigDecimal totalDemand = rechargeVolume.getValue()
        .add(prechargeVolume.getValue())
        .add(newEquipmentVolume.getValue());
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
      BigDecimal inductionRatioEol = getEffectiveInductionRate(simulationState, scopeEffective,
          RecoveryStage.EOL, hasUnitBasedSpecs);
      BigDecimal inductionRatioRecharge = getEffectiveInductionRate(simulationState, scopeEffective,
          RecoveryStage.RECHARGE, hasUnitBasedSpecs);

      BigDecimal eolInducedKg = eolRecycledKg.multiply(inductionRatioEol);
      BigDecimal rechargeInducedKg = rechargeRecycledKg.multiply(inductionRatioRecharge);

      BigDecimal inducedDemandKg = eolInducedKg.add(rechargeInducedKg);
      totalRequiredKg = totalDemand.add(inducedDemandKg);
    } else {
      BigDecimal inductionRatioEol = getEffectiveInductionRate(simulationState, scopeEffective,
          RecoveryStage.EOL, hasUnitBasedSpecs);
      BigDecimal inductionRatioRecharge = getEffectiveInductionRate(simulationState, scopeEffective,
          RecoveryStage.RECHARGE, hasUnitBasedSpecs);

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
}
