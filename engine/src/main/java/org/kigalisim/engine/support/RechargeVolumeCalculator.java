/**
 * Calculator for recharge volume operations.
 *
 * <p>This class encapsulates recharge volume calculation logic previously
 * found in SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.StateGetter;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;

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
  public static EngineNumber calculateRechargeVolume(UseKey scope, StateGetter stateGetter,
                                                     StreamKeeper streamKeeper, org.kigalisim.engine.Engine engine) {
    System.out.println("=== RECHARGE VOLUME CALCULATOR DEBUG ===");
    System.out.println("Scope: " + scope);
    
    OverridingConverterStateGetter overridingStateGetter =
        new OverridingConverterStateGetter(stateGetter);
    UnitConverter unitConverter = new UnitConverter(overridingStateGetter);
    Optional<String> application = Optional.ofNullable(scope.getApplication());
    Optional<String> substance = Optional.ofNullable(scope.getSubstance());

    System.out.println("Application: " + application + ", Substance: " + substance);

    if (!application.isPresent() || !substance.isPresent()) {
      ExceptionsGenerator.raiseNoAppOrSubstance("calculating recharge volume", "");
    }

    // Get prior population for recharge calculation
    EngineNumber priorPopulationRaw = engine.getStream("priorEquipment");
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    System.out.println("Prior population raw: " + priorPopulationRaw + ", converted: " + priorPopulation);

    // Get recharge population
    overridingStateGetter.setPopulation(engine.getStream("priorEquipment"));
    EngineNumber rechargePopRaw = streamKeeper.getRechargePopulation(scope);
    EngineNumber rechargePop = unitConverter.convert(rechargePopRaw, "units");
    System.out.println("Recharge population raw: " + rechargePopRaw + ", converted: " + rechargePop);
    overridingStateGetter.clearPopulation();

    // Switch to recharge population
    overridingStateGetter.setPopulation(rechargePop);
    System.out.println("Switched to recharge population: " + rechargePop);

    // Get recharge amount
    EngineNumber rechargeIntensityRaw = streamKeeper.getRechargeIntensity(scope);
    System.out.println("Recharge intensity raw: " + rechargeIntensityRaw);
    EngineNumber rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");
    System.out.println("Recharge volume calculated: " + rechargeVolume);

    // Return to prior population
    overridingStateGetter.setPopulation(priorPopulation);
    System.out.println("Returned to prior population: " + priorPopulation);
    System.out.println("=== END RECHARGE VOLUME CALCULATOR DEBUG ===\n");

    return rechargeVolume;
  }
}
