/**
 * Strategy for recalculating recharge (servicing) emissions.
 *
 * <p>This strategy encapsulates the logic for calculating emissions from both
 * recharge (servicing prior equipment) and precharge (servicing new equipment),
 * combining them into the single rechargeEmissions stream.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.support.PrechargeVolumeCalculator;
import org.kigalisim.engine.support.RechargeVolumeCalculator;

/**
 * Strategy for recalculating recharge emissions (including precharge).
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
    UnitConverter unitConverter = kit.getUnitConverter();

    // Calculate recharge volume (from prior equipment)
    EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
        scopeEffective,
        kit.getStateGetter(),
        kit.getStreamKeeper(),
        target
    );
    EngineNumber rechargeGhg = unitConverter.convert(rechargeVolume, "tCO2e");

    // Calculate precharge volume (from new equipment)
    EngineNumber prechargeVolume = PrechargeVolumeCalculator.calculatePrechargeVolume(
        scopeEffective,
        kit.getStateGetter(),
        kit.getStreamKeeper(),
        target
    );
    EngineNumber prechargeGhg = unitConverter.convert(prechargeVolume, "tCO2e");

    // Combine both into rechargeEmissions
    BigDecimal totalEmissions = rechargeGhg.getValue().add(prechargeGhg.getValue());
    EngineNumber totalGhg = new EngineNumber(totalEmissions, "tCO2e");

    StreamUpdate update = new StreamUpdateBuilder()
        .setName("rechargeEmissions")
        .setValue(totalGhg)
        .setKey(scopeEffective)
        .setPropagateChanges(false)
        .build();
    target.executeStreamUpdate(update);
  }
}
