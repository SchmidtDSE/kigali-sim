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
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.support.DivisionHelper;
import org.kigalisim.engine.support.ExceptionsGenerator;
import org.kigalisim.engine.support.RechargeVolumeCalculator;

/**
 * Strategy for recalculating population changes.
 */
public class PopulationChangeRecalcStrategy implements RecalcStrategy {

  private final Scope scope;
  private final Boolean subtractRecharge;

  /**
   * Create a new PopulationChangeRecalcStrategy.
   *
   * @param scope The scope to use for calculations, null to use engine's current scope
   * @param subtractRecharge Whether to subtract recharge, null to default to true
   */
  public PopulationChangeRecalcStrategy(Scope scope, Boolean subtractRecharge) {
    this.scope = scope;
    this.subtractRecharge = subtractRecharge;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    ConverterStateGetter baseStateGetter = kit.getStateGetter();
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(baseStateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    Scope scopeEffective = scope != null ? scope : target.getScope();
    boolean subtractRechargeEffective = subtractRecharge != null ? subtractRecharge : true;
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    if (application == null || substance == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating population change", "");
    }

    // Get prior population
    EngineNumber priorPopulationRaw = target.getStream("priorEquipment", scopeEffective, null);
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    // Get substance sales
    EngineNumber substanceSalesRaw = target.getStream("sales", scopeEffective, null);
    EngineNumber substanceSales = unitConverter.convert(substanceSalesRaw, "kg");

    // Get recharge volume using the calculator
    EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
        scopeEffective,
        kit.getStateGetter(),
        kit.getStreamKeeper(),
        target
    );

    // Get total volume available for new units
    BigDecimal salesKg = substanceSales.getValue();
    BigDecimal rechargeKg = subtractRechargeEffective ? rechargeVolume.getValue() : BigDecimal.ZERO;
    BigDecimal availableForNewUnitsKg = salesKg.subtract(rechargeKg);

    // Convert to unit delta
    EngineNumber initialChargeRaw = target.getInitialCharge("sales");
    EngineNumber initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");
    BigDecimal initialChargeKgUnit = initialCharge.getValue();
    BigDecimal deltaUnitsRaw = DivisionHelper.divideWithZero(
        availableForNewUnitsKg,
        initialChargeKgUnit
    );
    BigDecimal deltaUnits = deltaUnitsRaw;
    EngineNumber newUnitsMarginal = new EngineNumber(
        deltaUnits.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : deltaUnits,
        "units"
    );

    // Find new total
    BigDecimal priorPopulationUnits = priorPopulation.getValue();
    BigDecimal newUnits = priorPopulationUnits.add(deltaUnits);
    boolean newUnitsNegative = newUnits.compareTo(BigDecimal.ZERO) < 0;
    BigDecimal newUnitsAllowed = newUnitsNegative ? BigDecimal.ZERO : newUnits;
    EngineNumber newUnitsEffective = new EngineNumber(newUnitsAllowed, "units");

    // Save
    target.setStream("equipment", newUnitsEffective, null, scopeEffective, false, null);
    target.setStream("newEquipment", newUnitsMarginal, null, scopeEffective, false, null);

    // Recalc recharge emissions - need to create a new operation
    RechargeEmissionsRecalcStrategy rechargeStrategy = new RechargeEmissionsRecalcStrategy(
        scopeEffective
    );
    rechargeStrategy.execute(target, kit);
  }
}
