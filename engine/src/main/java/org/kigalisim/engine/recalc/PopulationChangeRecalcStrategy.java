/**
 * Strategy for recalculating population changes.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcPopulationChange method of SingleThreadEngine.</p>
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
import org.kigalisim.engine.state.StateGetter;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.support.DivisionHelper;
import org.kigalisim.engine.support.ExceptionsGenerator;
import org.kigalisim.engine.support.RechargeVolumeCalculator;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;

/**
 * Strategy for recalculating population changes.
 */
public class PopulationChangeRecalcStrategy implements RecalcStrategy {

  private final Optional<UseKey> scope;
  private final Optional<Boolean> useExplicitRecharge;

  /**
   * Create a new PopulationChangeRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   * @param useExplicitRecharge Whether to use explicit recharge, empty to default to true
   */
  public PopulationChangeRecalcStrategy(Optional<UseKey> scope, Optional<Boolean> useExplicitRecharge) {
    this.scope = scope;
    this.useExplicitRecharge = useExplicitRecharge;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    StateGetter baseStateGetter = kit.getStateGetter();
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(baseStateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    UseKey scopeEffective = scope.orElse(target.getScope());
    boolean useExplicitRechargeEffective = useExplicitRecharge.orElse(true);
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    if (application == null || substance == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating population change", "");
    }

    // Get prior population
    EngineNumber priorPopulationRaw = target.getStream("priorEquipment", Optional.of(scopeEffective), Optional.empty());
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    // Get substance sales (includes domestic + import + recycle)
    EngineNumber substanceSalesRaw = target.getStream("sales", Optional.of(scopeEffective), Optional.empty());
    EngineNumber substanceSales = unitConverter.convert(substanceSalesRaw, "kg");

    // Get explicit recharge volume using the calculator
    EngineNumber explicitRechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
        scopeEffective,
        kit.getStateGetter(),
        kit.getStreamKeeper(),
        target
    );

    // Get existing implicit recharge stream (defaults to 0 kg if not set)
    EngineNumber implicitRechargeRaw = target.getStream("implicitRecharge", Optional.of(scopeEffective), Optional.empty());
    EngineNumber implicitRecharge = unitConverter.convert(implicitRechargeRaw, "kg");

    // Choose which recharge to use based on useExplicitRecharge flag
    BigDecimal rechargeKg;
    if (useExplicitRechargeEffective) {
      // Using explicit recharge calculation
      rechargeKg = explicitRechargeVolume.getValue();
    } else {
      // Using implicit recharge (set by setStreamFor when units are used)
      rechargeKg = implicitRecharge.getValue();
    }

    // Get total volume available for new units
    BigDecimal salesKg = substanceSales.getValue();
    // Always subtract recharge to get the net volume available for new equipment
    BigDecimal availableForNewUnitsKg = salesKg.subtract(rechargeKg);

    // Convert to unit delta
    EngineNumber initialChargeRaw = target.getInitialCharge("sales");
    EngineNumber initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");
    BigDecimal initialChargeKgUnit = initialCharge.getValue();
    BigDecimal deltaUnits = DivisionHelper.divideWithZero(
        availableForNewUnitsKg,
        initialChargeKgUnit
    );
    EngineNumber newUnitsMarginal = new EngineNumber(deltaUnits, "units");

    // Find new total
    BigDecimal priorPopulationUnits = priorPopulation.getValue();
    BigDecimal newUnits = priorPopulationUnits.add(deltaUnits);
    boolean newUnitsNegative = newUnits.compareTo(BigDecimal.ZERO) < 0;
    BigDecimal newUnitsAllowed = newUnitsNegative ? BigDecimal.ZERO : newUnits;
    EngineNumber newUnitsEffective = new EngineNumber(newUnitsAllowed, "units");

    // Save
    StreamUpdate equipmentUpdate = new StreamUpdateBuilder()
        .setName("equipment")
        .setValue(newUnitsEffective)
        .setKey(scopeEffective)
        .setPropagateChanges(false)
        .build();
    target.executeStreamUpdate(equipmentUpdate);

    StreamUpdate newEquipmentUpdate = new StreamUpdateBuilder()
        .setName("newEquipment")
        .setValue(newUnitsMarginal)
        .setKey(scopeEffective)
        .setPropagateChanges(false)
        .build();
    target.executeStreamUpdate(newEquipmentUpdate);

    // Recalc recharge emissions - need to create a new operation
    RechargeEmissionsRecalcStrategy rechargeStrategy = new RechargeEmissionsRecalcStrategy(
        Optional.of(scopeEffective)
    );
    rechargeStrategy.execute(target, kit);
  }
}
