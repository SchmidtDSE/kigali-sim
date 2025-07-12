/**
 * Strategy for recharge recycling recalculation.
 *
 * <p>This strategy calculates recycling material from equipment being serviced/recharged.
 * It extends the abstract recycling strategy to provide recharge-specific volume calculation
 * based on recharge population and intensity.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.lang.operation.RecoverOperation.RecoverStage;

/**
 * Strategy for recharge recycling recalculation.
 *
 * <p>Calculates available recycling volume from equipment being serviced/recharged.</p>
 */
public class RechargeRecyclingRecalcStrategy extends AbstractRecyclingRecalcStrategy {

  /**
   * Create a new RechargeRecyclingRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   */
  public RechargeRecyclingRecalcStrategy(Optional<UseKey> scope) {
    super(scope, RecoverStage.RECHARGE);
  }

  /**
   * Calculate available volume for recharge recycling from equipment being serviced.
   *
   * @param target The engine on which to execute the recalculation
   * @param kit The RecalcKit containing required dependencies
   * @param scope The effective scope for calculations
   * @return The volume available for recycling from recharge operations
   */
  @Override
  protected EngineNumber calculateAvailableVolume(Engine target, RecalcKit kit, UseKey scope) {
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    StreamKeeper streamKeeper = kit.getStreamKeeper();

    try {
      // Get recharge population (similar to SalesRecalcStrategy logic)
      EngineNumber basePopulation = target.getStream("priorEquipment", Optional.of(scope), Optional.empty());
      stateGetter.setPopulation(basePopulation);
      EngineNumber rechargePopRaw = streamKeeper.getRechargePopulation(scope);
      EngineNumber rechargePop = unitConverter.convert(rechargePopRaw, "units");
      stateGetter.clearPopulation();

      // Switch into recharge population context
      stateGetter.setPopulation(rechargePop);

      // Get recharge intensity (volume per unit being recharged)
      EngineNumber rechargeIntensityRaw = streamKeeper.getRechargeIntensity(scope);
      EngineNumber rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

      // Clear population context
      stateGetter.clearPopulation();

      // The recharge volume represents the total substance volume being handled during recharge
      // This is the available volume for recycling during recharge operations
      return rechargeVolume;
    } catch (Exception e) {
      // If any error occurs in volume calculation, return 0 to avoid breaking the flow
      return new EngineNumber(BigDecimal.ZERO, "kg");
    }
  }
}
