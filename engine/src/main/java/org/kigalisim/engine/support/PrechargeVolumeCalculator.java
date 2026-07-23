/**
 * Calculator for precharge volume operations.
 *
 * <p>This class encapsulates precharge volume calculation logic, operating on
 * new equipment instead of prior equipment.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.StateGetter;
import org.kigalisim.engine.state.UseKey;

/**
 * Calculator for precharge volume operations.
 *
 * <p>Calculates the volume of substance needed for precharge (servicing of new
 * equipment before sale), as opposed to recharge (servicing of existing equipment).</p>
 */
public class PrechargeVolumeCalculator {

  /**
   * Calculate the precharge volume for the given application and substance.
   *
   * @param scope The scope containing application and substance
   * @param stateGetter The state getter for unit conversions
   * @param simulationState The simulation state for accessing precharge data
   * @param engine The engine for getting stream values
   * @return The precharge volume in kg
   */
  public static EngineNumber calculatePrechargeVolume(UseKey scope,
      StateGetter stateGetter,
      SimulationState simulationState,
      Engine engine) {
    OverridingConverterStateGetter overridingStateGetter =
        new OverridingConverterStateGetter(stateGetter);
    UnitConverter unitConverter = new UnitConverter(overridingStateGetter);
    Optional<String> application = Optional.ofNullable(scope.getApplication());
    Optional<String> substance = Optional.ofNullable(scope.getSubstance());

    if (!application.isPresent() || !substance.isPresent()) {
      ExceptionsGenerator.raiseNoAppOrSubstance("calculating precharge volume", "");
    }

    // Get new equipment population for precharge calculation
    EngineNumber newPopulationRaw = engine.getStream("newEquipment");
    EngineNumber newPopulation = unitConverter.convert(newPopulationRaw, "units");

    // Get precharge population
    overridingStateGetter.setPopulation(engine.getStream("newEquipment"));
    EngineNumber prechargePopRaw = simulationState.getPrechargePopulation(scope);
    EngineNumber prechargePop = unitConverter.convert(prechargePopRaw, "units");
    overridingStateGetter.clearPopulation();

    // Switch to precharge population
    overridingStateGetter.setPopulation(prechargePop);

    // Get precharge amount
    EngineNumber prechargeIntensityRaw = simulationState.getPrechargeIntensity(scope);
    EngineNumber prechargeVolume = unitConverter.convert(prechargeIntensityRaw, "kg");

    // Return to new equipment population
    overridingStateGetter.setPopulation(newPopulation);
    return prechargeVolume;
  }
}
