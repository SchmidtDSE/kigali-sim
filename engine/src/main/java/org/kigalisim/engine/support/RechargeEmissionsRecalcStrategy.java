/**
 * Strategy for recalculating recharge emissions.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcRechargeEmissions method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.Scope;

/**
 * Strategy for recalculating recharge emissions.
 */
public class RechargeEmissionsRecalcStrategy implements RecalcStrategy {

  private final Scope scope;

  /**
   * Create a new RechargeEmissionsRecalcStrategy.
   *
   * @param scope The scope to use for calculations, null to use engine's current scope
   */
  public RechargeEmissionsRecalcStrategy(Scope scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    Scope scopeEffective = scope != null ? scope : target.getScope();
    EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
        scopeEffective, 
        kit.getStateGetter(), 
        kit.getStreamKeeper(),
        target
    );
    UnitConverter unitConverter = kit.getUnitConverter();
    EngineNumber rechargeGhg = unitConverter.convert(rechargeVolume, "tCO2e");
    target.setStream("rechargeEmissions", rechargeGhg, null, scopeEffective, false, null);
  }
}
