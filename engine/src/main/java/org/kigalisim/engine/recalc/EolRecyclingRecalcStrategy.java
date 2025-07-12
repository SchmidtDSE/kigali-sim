/**
 * Strategy for EOL (End of Life) recycling recalculation.
 *
 * <p>This strategy calculates recycling material from equipment reaching end of life.
 * It extends the abstract recycling strategy to provide EOL-specific volume calculation
 * based on retiring equipment population.</p>
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
 * Strategy for EOL (End of Life) recycling recalculation.
 *
 * <p>Calculates available recycling volume from equipment retiring at end of life.</p>
 */
public class EolRecyclingRecalcStrategy extends AbstractRecyclingRecalcStrategy {

  /**
   * Create a new EolRecyclingRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   */
  public EolRecyclingRecalcStrategy(Optional<UseKey> scope) {
    super(scope, RecoverStage.EOL);
  }

  /**
   * Calculate available volume for EOL recycling from retiring equipment.
   *
   * @param target The engine on which to execute the recalculation
   * @param kit The RecalcKit containing required dependencies
   * @param scope The effective scope for calculations
   * @return The volume available for recycling from retiring equipment
   */
  @Override
  protected EngineNumber calculateAvailableVolume(Engine target, RecalcKit kit, UseKey scope) {
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    StreamKeeper streamKeeper = kit.getStreamKeeper();

    try {
      // Get prior equipment population (similar to recharge pattern)
      EngineNumber priorPopulationRaw = target.getStream("priorEquipment", Optional.of(scope), Optional.empty());
      EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
      
      // Set population context for retirement rate calculation
      stateGetter.setPopulation(priorPopulation);
      
      // Get retirement rate and calculate retiring units
      EngineNumber retirementRateRaw = streamKeeper.getRetirementRate(scope);
      EngineNumber retiringUnits = unitConverter.convert(retirementRateRaw, "units");
      
      // Clear population context
      stateGetter.clearPopulation();
      
      // Set retiring units as population for substance volume calculation
      stateGetter.setPopulation(retiringUnits);
      
      // Get initial charge to calculate substance volume per unit
      EngineNumber initialChargeRaw = target.getInitialCharge("sales");
      EngineNumber substanceVolumePerUnit = unitConverter.convert(initialChargeRaw, "kg");
      
      // Clear population context
      stateGetter.clearPopulation();
      
      
      // The retiring equipment volume is calculated similar to recharge recycling:
      // recharge: rechargePopulation × rechargeIntensity
      // EOL: retiringUnits × substanceVolumePerUnit (which is total substance in retiring equipment)
      return substanceVolumePerUnit;
    } catch (Exception e) {
      // If any error occurs in volume calculation, return 0 to avoid breaking the flow
      return new EngineNumber(BigDecimal.ZERO, "kg");
    }
  }
}
