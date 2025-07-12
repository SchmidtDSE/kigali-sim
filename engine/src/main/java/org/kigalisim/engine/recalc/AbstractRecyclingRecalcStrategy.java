/**
 * Abstract strategy for stage-specific recycling recalculation.
 *
 * <p>This abstract class implements the template method pattern for recycling operations,
 * allowing concrete implementations to specify different volume calculation methods for
 * EOL and recharge recycling while sharing common recycling application logic.</p>
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
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.lang.operation.RecoverOperation.RecoverStage;

/**
 * Abstract strategy for stage-specific recycling recalculation.
 *
 * <p>Uses the template method pattern where execute() defines the overall algorithm
 * and subclasses provide stage-specific volume calculation implementations.</p>
 */
public abstract class AbstractRecyclingRecalcStrategy implements RecalcStrategy {

  protected final Optional<UseKey> scope;
  protected final RecoverStage stage;

  /**
   * Create a new AbstractRecyclingRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  public AbstractRecyclingRecalcStrategy(Optional<UseKey> scope, RecoverStage stage) {
    this.scope = scope;
    this.stage = stage;
  }

  /**
   * Template method implementation that defines the overall recycling algorithm.
   *
   * @param target The engine on which to execute the recalculation
   * @param kit The RecalcKit containing required dependencies
   */
  @Override
  public final void execute(Engine target, RecalcKit kit) {
    UseKey scopeEffective = scope.orElse(target.getScope());
    StreamKeeper streamKeeper = kit.getStreamKeeper();

    // Check if recycling is configured for this stage
    EngineNumber recoveryRate = streamKeeper.getRecoveryRate(scopeEffective, stage);
    if (recoveryRate.getValue().compareTo(BigDecimal.ZERO) == 0) {
      return; // No recycling for this stage
    }

    // Template method pattern - delegate volume calculation to subclasses
    EngineNumber availableVolume = calculateAvailableVolume(target, kit, scopeEffective);
    if (availableVolume.getValue().compareTo(BigDecimal.ZERO) <= 0) {
      return; // No volume available for recycling
    }

    // Get yield rate for this stage
    EngineNumber yieldRate = streamKeeper.getYieldRate(scopeEffective, stage);

    // Apply recycling using shared logic
    applyRecycling(target, kit, scopeEffective, availableVolume, recoveryRate, yieldRate);
  }

  /**
   * Abstract method for calculating available volume for recycling.
   * Subclasses must implement this to provide stage-specific volume calculations.
   *
   * @param target The engine on which to execute the recalculation
   * @param kit The RecalcKit containing required dependencies
   * @param scope The effective scope for calculations
   * @return The available volume for recycling at this stage
   */
  protected abstract EngineNumber calculateAvailableVolume(Engine target, RecalcKit kit, UseKey scope);

  /**
   * Shared recycling application logic that calculates and applies recycling material.
   *
   * @param target The engine on which to execute the recalculation
   * @param kit The RecalcKit containing required dependencies
   * @param scope The effective scope for calculations
   * @param availableVolume The volume available for recycling
   * @param recoveryRate The recovery rate for this stage
   * @param yieldRate The yield rate for this stage
   */
  protected void applyRecycling(Engine target, RecalcKit kit, UseKey scope,
                               EngineNumber availableVolume, EngineNumber recoveryRate, EngineNumber yieldRate) {
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    StreamKeeper streamKeeper = kit.getStreamKeeper();

    // Set volume context for rate calculations
    stateGetter.setVolume(availableVolume);

    // Calculate recovered volume
    EngineNumber recoveredVolumeRaw = recoveryRate;
    EngineNumber recoveredVolume = unitConverter.convert(recoveredVolumeRaw, "kg");

    // Calculate recycled volume from recovered volume
    stateGetter.setVolume(recoveredVolume);
    EngineNumber recycledVolumeRaw = yieldRate;
    EngineNumber recycledVolume = unitConverter.convert(recycledVolumeRaw, "kg");

    // Clear volume context
    stateGetter.clearVolume();

    // Get recycling displacement rate
    BigDecimal recycledKg = recycledVolume.getValue();
    EngineNumber displacementRateRaw = streamKeeper.getDisplacementRate(scope);
    EngineNumber displacementRate = unitConverter.convert(displacementRateRaw, "%");
    BigDecimal displacementRateRatio = displacementRate.getValue().divide(
        BigDecimal.valueOf(100),
        MathContext.DECIMAL128
    );
    BigDecimal recycledDisplacedKg = recycledKg.multiply(displacementRateRatio);

    // Set the recycled material in the recycle stream
    EngineNumber recycledAmount = new EngineNumber(recycledDisplacedKg, "kg");
    target.setStreamFor("recycle", recycledAmount, Optional.empty(), Optional.of(scope), false, Optional.empty());
  }
}
