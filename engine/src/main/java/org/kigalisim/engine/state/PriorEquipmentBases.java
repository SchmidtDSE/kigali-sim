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
    recyclingCalculatedThisStep = false;
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
    recyclingCalculatedThisStep = false;
  }

}
