/**
 * Calculator for recharge volume operations.
 *
 * <p>This class encapsulates recharge volume calculation logic previously
 * found in SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.StreamKeeper;

/**
 * Calculator for recharge volume operations.
 */
public class RechargeVolumeCalculator {

  /**
   * Calculate the recharge volume for the given application and substance.
   *
   * @param scope The scope containing application and substance
   * @param stateGetter The state getter for unit conversions
   * @param streamKeeper The stream keeper for accessing recharge data
   * @param engine The engine for getting stream values
   * @return The recharge volume in kg
   */
  public static EngineNumber calculateRechargeVolume(Scope scope, ConverterStateGetter stateGetter,
      StreamKeeper streamKeeper, org.kigalisim.engine.Engine engine) {
    OverridingConverterStateGetter overridingStateGetter =
        new OverridingConverterStateGetter(stateGetter);
    UnitConverter unitConverter = new UnitConverter(overridingStateGetter);
    String application = scope.getApplication();
    String substance = scope.getSubstance();

    if (application == null || substance == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("calculating recharge volume", "");
    }

    // Get prior population for recharge calculation
    EngineNumber priorPopulationRaw = engine.getStream("priorEquipment");
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");

    // Get recharge population
    overridingStateGetter.setPopulation(engine.getStream("priorEquipment"));
    EngineNumber rechargePopRaw = streamKeeper.getRechargePopulation(scope);
    EngineNumber rechargePop = unitConverter.convert(rechargePopRaw, "units");
    overridingStateGetter.clearPopulation();

    // Switch to recharge population
    overridingStateGetter.setPopulation(rechargePop);

    // Get recharge amount
    EngineNumber rechargeIntensityRaw = streamKeeper.getRechargeIntensity(scope);
    EngineNumber rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    // Return to prior population
    overridingStateGetter.setPopulation(priorPopulation);

    return rechargeVolume;
  }
}
