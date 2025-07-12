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
      // Get current prior population (available for retirement)
      EngineNumber priorPopulationRaw = streamKeeper.getStream(scope, "priorEquipment");
      if (priorPopulationRaw == null) {
        return new EngineNumber(BigDecimal.ZERO, "kg");
      }
      EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");

      // Get retirement rate
      EngineNumber retirementRateRaw = streamKeeper.getRetirementRate(scope);

      // Calculate retiring units using population context
      stateGetter.setPopulation(priorPopulation);
      EngineNumber retiringUnits = unitConverter.convert(retirementRateRaw, "units");
      stateGetter.clearPopulation();

      // Convert retiring units to volume using initial charge
      EngineNumber initialChargeRaw = target.getRawInitialChargeFor(scope, "import");
      EngineNumber initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");

      // Calculate total volume from retiring equipment
      BigDecimal retiringVolume = retiringUnits.getValue().multiply(initialCharge.getValue());

      return new EngineNumber(retiringVolume, "kg");
    } catch (Exception e) {
      // If any error occurs in volume calculation, return 0 to avoid breaking the flow
      return new EngineNumber(BigDecimal.ZERO, "kg");
    }
  }
}
