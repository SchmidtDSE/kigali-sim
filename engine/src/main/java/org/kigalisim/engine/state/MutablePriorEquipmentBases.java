/**
 * Mutable, live implementation of prior equipment base tracking.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Live, mutable state manager tracking population bases and applied amounts across
 * multiple retire/recharge commands within a timestep to enable cumulative calculations.
 */
public class MutablePriorEquipmentBases implements PriorEquipmentBases {

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
   * Create a new MutablePriorEquipmentBases instance with default values.
   */
  public MutablePriorEquipmentBases() {
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

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRetirementBasePopulation() {
    return retirementBasePopulation;
  }

  /** {@inheritDoc} */
  @Override
  public void setRetirementBasePopulation(EngineNumber value) {
    retirementBasePopulation = Optional.of(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRetirementAmount() {
    return appliedRetirementAmount;
  }

  /** {@inheritDoc} */
  @Override
  public void setAppliedRetirementAmount(EngineNumber value) {
    appliedRetirementAmount = Optional.of(value);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getHasReplacementThisStep() {
    return hasReplacementThisStep;
  }

  /** {@inheritDoc} */
  @Override
  public void setHasReplacementThisStep(boolean value) {
    hasReplacementThisStep = value;
  }

  /** {@inheritDoc} */
  @Override
  public boolean getRetireCalculatedThisStep() {
    return retireCalculatedThisStep;
  }

  /** {@inheritDoc} */
  @Override
  public void setRetireCalculatedThisStep(boolean calculated) {
    retireCalculatedThisStep = calculated;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRechargeBasePopulation() {
    return rechargeBasePopulation;
  }

  /** {@inheritDoc} */
  @Override
  public void setRechargeBasePopulation(EngineNumber value) {
    rechargeBasePopulation = Optional.of(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRechargeAmount() {
    return appliedRechargeAmount;
  }

  /** {@inheritDoc} */
  @Override
  public void setAppliedRechargeAmount(EngineNumber value) {
    appliedRechargeAmount = Optional.of(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getPrechargeBasePopulation() {
    return prechargeBasePopulation;
  }

  /** {@inheritDoc} */
  @Override
  public void setPrechargeBasePopulation(EngineNumber value) {
    prechargeBasePopulation = Optional.of(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedPrechargeAmount() {
    return appliedPrechargeAmount;
  }

  /** {@inheritDoc} */
  @Override
  public void setAppliedPrechargeAmount(EngineNumber value) {
    appliedPrechargeAmount = Optional.of(value);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getRecyclingCalculatedThisStep() {
    return recyclingCalculatedThisStep;
  }

  /** {@inheritDoc} */
  @Override
  public void setRecyclingCalculatedThisStep(boolean calculated) {
    recyclingCalculatedThisStep = calculated;
  }

  /** {@inheritDoc} */
  @Override
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
   * Create an immutable snapshot of this instance.
   *
   * <p>{@link EngineNumber} and {@link Optional} are both immutable, so the
   * Optionals can be shared directly between this instance and the snapshot
   * without risk of cross-contamination from later mutations of this instance.</p>
   *
   * @return An immutable {@link FrozenPriorEquipmentBases} snapshot
   */
  @Override
  public PriorEquipmentBases freeze() {
    return new FrozenPriorEquipmentBases(
        retirementBasePopulation,
        appliedRetirementAmount,
        hasReplacementThisStep,
        retireCalculatedThisStep,
        rechargeBasePopulation,
        appliedRechargeAmount,
        prechargeBasePopulation,
        appliedPrechargeAmount,
        recyclingCalculatedThisStep);
  }
}
