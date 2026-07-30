/**
 * Manages cumulative retirement and recharge base tracking for prior equipment.
 *
 * <p>Tracks population bases and applied amounts across multiple retire/recharge
 * commands within a single timestep to enable cumulative calculations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;

/**
 * State supporting cumulative retirement / recharge base tracking (prior equip).
 *
 * <p>State manager tracking population bases and applied amounts across multiple
 * retire/recharge commands within a timestep to enable cumulative calculations.</p>
 */
public class PriorEquipmentBases {

  private Optional<EngineNumber> retirementBasePopulation;
  private Optional<EngineNumber> appliedRetirementAmount;
  private boolean hasReplacementThisStep;
  private boolean retireCalculatedThisStep;
  private Optional<EngineNumber> rechargeBasePopulation;
  private Optional<EngineNumber> appliedRechargeAmount;
  private Optional<EngineNumber> prechargeBasePopulation;
  private Optional<EngineNumber> appliedPrechargeAmount;
  private boolean recyclingCalculatedThisStep;

  /**
   * Create a new PriorEquipmentBases instance with default values.
   */
  public PriorEquipmentBases() {
    retirementBasePopulation = Optional.empty();
    appliedRetirementAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "units"));
    hasReplacementThisStep = false;
    retireCalculatedThisStep = false;
    rechargeBasePopulation = Optional.empty();
    appliedRechargeAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "kg"));
    prechargeBasePopulation = Optional.empty();
    appliedPrechargeAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "kg"));
    recyclingCalculatedThisStep = false;
  }

  /**
   * Create a new PriorEquipmentBases instance with the specified field values.
   *
   * <p>This is used by {@link #deepCopy()} to construct a copy without relying on
   * the default initialization or direct field mutation after construction.</p>
   *
   * @param retirementBasePopulation The retirement base population, or empty if not captured
   * @param appliedRetirementAmount The total amount already retired this step
   * @param hasReplacementThisStep Whether replacement was used in this step's retire commands
   * @param retireCalculatedThisStep Whether retire has been calculated this step
   * @param rechargeBasePopulation The recharge base population, or empty if not captured
   * @param appliedRechargeAmount The total amount already recharged this step in kg
   * @param prechargeBasePopulation The precharge base population, or empty if not captured
   * @param appliedPrechargeAmount The total amount already precharged this step in kg
   * @param recyclingCalculatedThisStep Whether recycling has been calculated this step
   */
  private PriorEquipmentBases(Optional<EngineNumber> retirementBasePopulation,
      Optional<EngineNumber> appliedRetirementAmount, boolean hasReplacementThisStep,
      boolean retireCalculatedThisStep, Optional<EngineNumber> rechargeBasePopulation,
      Optional<EngineNumber> appliedRechargeAmount, Optional<EngineNumber> prechargeBasePopulation,
      Optional<EngineNumber> appliedPrechargeAmount, boolean recyclingCalculatedThisStep) {
    this.retirementBasePopulation = retirementBasePopulation;
    this.appliedRetirementAmount = appliedRetirementAmount;
    this.hasReplacementThisStep = hasReplacementThisStep;
    this.retireCalculatedThisStep = retireCalculatedThisStep;
    this.rechargeBasePopulation = rechargeBasePopulation;
    this.appliedRechargeAmount = appliedRechargeAmount;
    this.prechargeBasePopulation = prechargeBasePopulation;
    this.appliedPrechargeAmount = appliedPrechargeAmount;
    this.recyclingCalculatedThisStep = recyclingCalculatedThisStep;
  }

  /**
   * Get the retirement base population.
   *
   * @return The base population, or empty if not yet captured this step
   */
  public Optional<EngineNumber> getRetirementBasePopulation() {
    return retirementBasePopulation;
  }

  /**
   * Set the retirement base population.
   *
   * @param value The base population value
   */
  public void setRetirementBasePopulation(EngineNumber value) {
    retirementBasePopulation = Optional.of(value);
  }

  /**
   * Get the applied retirement amount.
   *
   * @return The total amount already retired this step
   */
  public Optional<EngineNumber> getAppliedRetirementAmount() {
    return appliedRetirementAmount;
  }

  /**
   * Set the applied retirement amount.
   *
   * @param value The total amount retired this step
   */
  public void setAppliedRetirementAmount(EngineNumber value) {
    appliedRetirementAmount = Optional.of(value);
  }

  /**
   * Get whether replacement was used in this step's retire commands.
   *
   * @return true if with replacement, false if without replacement
   */
  public boolean getHasReplacementThisStep() {
    return hasReplacementThisStep;
  }

  /**
   * Set whether replacement is used in this step's retire commands.
   *
   * @param value true for with replacement, false for without replacement
   */
  public void setHasReplacementThisStep(boolean value) {
    hasReplacementThisStep = value;
  }

  /**
   * Get whether retire has been calculated this step.
   *
   * @return true if retire was calculated, false otherwise
   */
  public boolean getRetireCalculatedThisStep() {
    return retireCalculatedThisStep;
  }

  /**
   * Set whether retire has been calculated this step.
   *
   * @param calculated true if retire was calculated, false otherwise
   */
  public void setRetireCalculatedThisStep(boolean calculated) {
    retireCalculatedThisStep = calculated;
  }

  /**
   * Get the recharge base population.
   *
   * @return The base population, or empty if not yet captured this step
   */
  public Optional<EngineNumber> getRechargeBasePopulation() {
    return rechargeBasePopulation;
  }

  /**
   * Set the recharge base population.
   *
   * @param value The base population value
   */
  public void setRechargeBasePopulation(EngineNumber value) {
    rechargeBasePopulation = Optional.of(value);
  }

  /**
   * Get the applied recharge amount.
   *
   * @return The total amount already recharged this step in kg
   */
  public Optional<EngineNumber> getAppliedRechargeAmount() {
    return appliedRechargeAmount;
  }

  /**
   * Set the applied recharge amount.
   *
   * @param value The total amount recharged this step in kg
   */
  public void setAppliedRechargeAmount(EngineNumber value) {
    appliedRechargeAmount = Optional.of(value);
  }

  /**
   * Get the precharge base population.
   *
   * @return The base population, or empty if not yet captured this step
   */
  public Optional<EngineNumber> getPrechargeBasePopulation() {
    return prechargeBasePopulation;
  }

  /**
   * Set the precharge base population.
   *
   * @param value The base population value
   */
  public void setPrechargeBasePopulation(EngineNumber value) {
    prechargeBasePopulation = Optional.of(value);
  }

  /**
   * Get the applied precharge amount.
   *
   * @return The total amount already precharged this step in kg
   */
  public Optional<EngineNumber> getAppliedPrechargeAmount() {
    return appliedPrechargeAmount;
  }

  /**
   * Set the applied precharge amount.
   *
   * @param value The total amount precharged this step in kg
   */
  public void setAppliedPrechargeAmount(EngineNumber value) {
    appliedPrechargeAmount = Optional.of(value);
  }

  /**
   * Get whether recycling has been calculated this step.
   *
   * @return true if recycling was calculated, false otherwise
   */
  public boolean getRecyclingCalculatedThisStep() {
    return recyclingCalculatedThisStep;
  }

  /**
   * Set whether recycling has been calculated this step.
   *
   * @param calculated true if recycling was calculated, false otherwise
   */
  public void setRecyclingCalculatedThisStep(boolean calculated) {
    recyclingCalculatedThisStep = calculated;
  }

  /**
   * Reset all tracking state at the beginning of a timestep.
   */
  public void resetStateAtTimestep() {
    retirementBasePopulation = Optional.empty();
    appliedRetirementAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "units"));
    hasReplacementThisStep = false;
    retireCalculatedThisStep = false;
    rechargeBasePopulation = Optional.empty();
    appliedRechargeAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "kg"));
    prechargeBasePopulation = Optional.empty();
    appliedPrechargeAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "kg"));
    recyclingCalculatedThisStep = false;
  }


  /**
   * Create a deep copy of this prior equipment bases instance.
   *
   * <p>EngineNumber instances are immutable and Optional is immutable, so the
   * Optionals can be shared directly between this instance and the copy without
   * risk of cross-contamination from later mutations.</p>
   *
   * @return A deep copy of this PriorEquipmentBases instance
   */
  public PriorEquipmentBases deepCopy() {
    return new PriorEquipmentBases(
        retirementBasePopulation,
        appliedRetirementAmount,
        hasReplacementThisStep,
        retireCalculatedThisStep,
        rechargeBasePopulation,
        appliedRechargeAmount,
        prechargeBasePopulation,
        appliedPrechargeAmount,
        recyclingCalculatedThisStep
    );
  }
}
