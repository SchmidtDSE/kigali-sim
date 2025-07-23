/**
 * Strategy for recalculating recharge emissions.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcRechargeEmissions method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.support.RechargeVolumeCalculator;

/**
 * Strategy for recalculating recharge emissions.
 */
public class RechargeEmissionsRecalcStrategy implements RecalcStrategy {

  private final Optional<UseKey> scope;

  /**
   * Create a new RechargeEmissionsRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   */
  public RechargeEmissionsRecalcStrategy(Optional<UseKey> scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    UseKey scopeEffective = scope.orElse(target.getScope());
    EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
        scopeEffective,
        kit.getStateGetter(),
        kit.getStreamKeeper(),
        target
    );
    UnitConverter unitConverter = kit.getUnitConverter();
    EngineNumber rechargeGhg = unitConverter.convert(rechargeVolume, "tCO2e");
    target.setStreamFor("rechargeEmissions", rechargeGhg, Optional.empty(), Optional.of(scopeEffective), false, Optional.empty());
  }
}
