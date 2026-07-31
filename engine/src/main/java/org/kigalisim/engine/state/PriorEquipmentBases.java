/**
 * Manages cumulative retirement and recharge base tracking for prior equipment.
 *
 * <p>Tracks population bases and applied amounts across multiple retire/recharge
 * commands within a single timestep to enable cumulative calculations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;

/**
 * State supporting cumulative retirement / recharge base tracking (prior equip).
 *
 * <p>Implemented by {@link MutablePriorEquipmentBases} for live, mutable tracking
 * during a simulation step and by {@link FrozenPriorEquipmentBases} for immutable
 * snapshots captured for prior-year lookups.</p>
 */
public interface PriorEquipmentBases {

  /**
   * Get the retirement base population.
   *
   * @return The base population, or empty if not yet captured this step
   */
  Optional<EngineNumber> getRetirementBasePopulation();

  /**
   * Set the retirement base population.
   *
   * @param value The base population value
   */
  void setRetirementBasePopulation(EngineNumber value);

  /**
   * Get the applied retirement amount.
   *
   * @return The total amount already retired this step
   */
  Optional<EngineNumber> getAppliedRetirementAmount();

  /**
   * Set the applied retirement amount.
   *
   * @param value The total amount retired this step
   */
  void setAppliedRetirementAmount(EngineNumber value);

  /**
   * Get whether replacement was used in this step's retire commands.
   *
   * @return true if with replacement, false if without replacement
   */
  boolean getHasReplacementThisStep();

  /**
   * Set whether replacement is used in this step's retire commands.
   *
   * @param value true for with replacement, false for without replacement
   */
  void setHasReplacementThisStep(boolean value);

  /**
   * Get whether retire has been calculated this step.
   *
   * @return true if retire was calculated, false otherwise
   */
  boolean getRetireCalculatedThisStep();

  /**
   * Set whether retire has been calculated this step.
   *
   * @param calculated true if retire was calculated, false otherwise
   */
  void setRetireCalculatedThisStep(boolean calculated);

  /**
   * Get the recharge base population.
   *
   * @return The base population, or empty if not yet captured this step
   */
  Optional<EngineNumber> getRechargeBasePopulation();

  /**
   * Set the recharge base population.
   *
   * @param value The base population value
   */
  void setRechargeBasePopulation(EngineNumber value);

  /**
   * Get the applied recharge amount.
   *
   * @return The total amount already recharged this step in kg
   */
  Optional<EngineNumber> getAppliedRechargeAmount();

  /**
   * Set the applied recharge amount.
   *
   * @param value The total amount recharged this step in kg
   */
  void setAppliedRechargeAmount(EngineNumber value);

  /**
   * Get the precharge base population.
   *
   * @return The base population, or empty if not yet captured this step
   */
  Optional<EngineNumber> getPrechargeBasePopulation();

  /**
   * Set the precharge base population.
   *
   * @param value The base population value
   */
  void setPrechargeBasePopulation(EngineNumber value);

  /**
   * Get the applied precharge amount.
   *
   * @return The total amount already precharged this step in kg
   */
  Optional<EngineNumber> getAppliedPrechargeAmount();

  /**
   * Set the applied precharge amount.
   *
   * @param value The total amount precharged this step in kg
   */
  void setAppliedPrechargeAmount(EngineNumber value);

  /**
   * Get whether recycling has been calculated this step.
   *
   * @return true if recycling was calculated, false otherwise
   */
  boolean getRecyclingCalculatedThisStep();

  /**
   * Set whether recycling has been calculated this step.
   *
   * @param calculated true if recycling was calculated, false otherwise
   */
  void setRecyclingCalculatedThisStep(boolean calculated);

  /**
   * Reset all tracking state at the beginning of a timestep.
   */
  void resetStateAtTimestep();

  /**
   * Get an immutable snapshot of this instance.
   *
   * <p>The snapshot shares immutable {@link EngineNumber}-typed values by reference
   * but does not share mutable containers, so later mutation of this instance does
   * not affect the snapshot.</p>
   *
   * @return An immutable snapshot backed by {@link FrozenPriorEquipmentBases}, or
   *     this same instance if it is already frozen
   */
  PriorEquipmentBases freeze();
}
