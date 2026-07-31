/**
 * Immutable snapshot implementation of prior equipment base tracking.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Immutable snapshot of {@link PriorEquipmentBases} state.
 *
 * <p>Holds only {@code final} fields populated at construction and throws
 * {@link UnsupportedOperationException} from every mutator, so a snapshot
 * captured for a prior-year lookup can never be silently corrupted.</p>
 */
public class FrozenPriorEquipmentBases implements PriorEquipmentBases {

  private static final String FROZEN_MESSAGE =
      "Cannot mutate a frozen PriorEquipmentBases snapshot.";

  private final Optional<EngineNumber> retirementBasePopulation;
  private final Optional<EngineNumber> appliedRetirementAmount;
  private final boolean hasReplacementThisStep;
  private final boolean retireCalculatedThisStep;
  private final Optional<EngineNumber> rechargeBasePopulation;
  private final Optional<EngineNumber> appliedRechargeAmount;
  private final Optional<EngineNumber> prechargeBasePopulation;
  private final Optional<EngineNumber> appliedPrechargeAmount;
  private final boolean recyclingCalculatedThisStep;

  /**
   * Create a new immutable snapshot with the specified field values.
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
  public FrozenPriorEquipmentBases(Optional<EngineNumber> retirementBasePopulation,
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

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRetirementBasePopulation() {
    return retirementBasePopulation;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRetirementBasePopulation(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRetirementAmount() {
    return appliedRetirementAmount;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setAppliedRetirementAmount(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getHasReplacementThisStep() {
    return hasReplacementThisStep;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setHasReplacementThisStep(boolean value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getRetireCalculatedThisStep() {
    return retireCalculatedThisStep;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRetireCalculatedThisStep(boolean calculated) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRechargeBasePopulation() {
    return rechargeBasePopulation;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRechargeBasePopulation(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRechargeAmount() {
    return appliedRechargeAmount;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setAppliedRechargeAmount(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getPrechargeBasePopulation() {
    return prechargeBasePopulation;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setPrechargeBasePopulation(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedPrechargeAmount() {
    return appliedPrechargeAmount;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setAppliedPrechargeAmount(EngineNumber value) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getRecyclingCalculatedThisStep() {
    return recyclingCalculatedThisStep;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void setRecyclingCalculatedThisStep(boolean calculated) {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException always, since this instance is frozen
   */
  @Override
  public void resetStateAtTimestep() {
    throw new UnsupportedOperationException(FROZEN_MESSAGE);
  }

  /**
   * {@inheritDoc}
   *
   * @return this same instance, since it is already frozen
   */
  @Override
  public PriorEquipmentBases freeze() {
    return this;
  }
}
