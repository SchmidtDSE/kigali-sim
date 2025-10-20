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
 * <p>State mangaer tracking population bases and applied amounts across multiple
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
    this.retirementBasePopulation = Optional.empty();
    this.appliedRetirementAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "units"));
    this.hasReplacementThisStep = false;
    this.retireCalculatedThisStep = false;
    this.rechargeBasePopulation = Optional.empty();
    this.appliedRechargeAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "kg"));
    this.recyclingCalculatedThisStep = false;
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
    this.retirementBasePopulation = Optional.of(value);
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
    this.appliedRetirementAmount = Optional.of(value);
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
    this.hasReplacementThisStep = value;
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
    this.retireCalculatedThisStep = calculated;
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
    this.rechargeBasePopulation = Optional.of(value);
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
    this.appliedRechargeAmount = Optional.of(value);
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
    this.recyclingCalculatedThisStep = calculated;
  }

  /**
   * Reset all tracking state at the beginning of a timestep.
   */
  public void resetStateAtTimestep() {
    this.retirementBasePopulation = Optional.empty();
    this.appliedRetirementAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "units"));
    this.hasReplacementThisStep = false;
    this.retireCalculatedThisStep = false;
    this.rechargeBasePopulation = Optional.empty();
    this.appliedRechargeAmount = Optional.of(new EngineNumber(BigDecimal.ZERO, "kg"));
    this.recyclingCalculatedThisStep = false;
  }

  /**
   * Accumulate recharge parameters with weighted-average intensity.
   *
   * <p>Multiple calls accumulate rates (addition) and intensities (weighted-average).
   * Population rates are added, intensities are weighted-averaged using absolute values
   * for weights to handle negative adjustments correctly.</p>
   *
   * <p>Weighted average formula: (|rate1| × intensity1 + |rate2| × intensity2) / (|rate1| + |rate2|)</p>
   *
   * @param currentPopulation The current recharge population rate
   * @param currentIntensity The current recharge intensity
   * @param population The recharge population rate to add
   * @param intensity The recharge intensity for this rate
   * @return An array with [newPopulation, newIntensity]
   */
  public EngineNumber[] accumulateRecharge(EngineNumber currentPopulation, EngineNumber currentIntensity,
      EngineNumber population, EngineNumber intensity) {
    // Calculate weighted-average intensity BEFORE updating population
    // Use absolute values for weights to handle negative values correctly
    BigDecimal currentWeight = currentPopulation.getValue().abs();
    BigDecimal newWeight = population.getValue().abs();

    BigDecimal weightedIntensity;
    if (currentWeight.compareTo(BigDecimal.ZERO) == 0) {
      // First recharge command this step, just use new intensity directly
      weightedIntensity = intensity.getValue();
    } else {
      // Multiple recharge commands - compute weighted average: (w1*i1 + w2*i2) / (w1 + w2)
      BigDecimal totalWeight = currentWeight.add(newWeight);
      weightedIntensity = currentWeight.multiply(currentIntensity.getValue())
          .add(newWeight.multiply(intensity.getValue()))
          .divide(totalWeight, 10, java.math.RoundingMode.HALF_UP);
    }

    // Accumulate population rates (can be negative)
    BigDecimal newPopulation = currentPopulation.getValue().add(population.getValue());

    return new EngineNumber[] {
        new EngineNumber(newPopulation, "%"),
        new EngineNumber(weightedIntensity, "kg / unit")
    };
  }
}
