/**
 * Calculates servicing (precharge/recharge) offsets for population changes.
 *
 * <p>This class encapsulates the logic for determining how precharge and recharge volumes affect
 * the calculation of new equipment units. It handles three cases: percentage-based precharge with
 * volume-based sales (circular case), explicit absolute precharge with volume-based sales, and
 * units-based tracking.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc.util;

import java.math.BigDecimal;
import java.math.MathContext;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.StateGetter;
import org.kigalisim.engine.support.DivisionHelper;

/**
 * Calculates servicing (precharge/recharge) offsets for population changes.
 */
public class ServicingOffsetter {

  /**
   * Calculate the servicing offset for a population change.
   *
   * <p>This method determines the precharge and recharge offsets and the resulting unit delta
   * based on the tracking mode and precharge configuration. It dispatches to the appropriate
   * branch: percentage precharge with volume-based sales (circular), explicit precharge with
   * volume-based sales, or units-based tracking.</p>
   *
   * @param salesKg The total substance sales volume in kilograms
   * @param rechargeKg The recharge volume in kilograms
   * @param initialChargeKgUnit The initial charge per unit in kg/unit
   * @param prechargePopRaw The raw precharge population value, or null if unspecified
   * @param prechargeIntensityRaw The raw precharge intensity value
   * @param useExplicitRechargeEffective Whether explicit (volume-based) recharge is in use
   * @param implicitPrechargeKg The implicit precharge volume in kilograms
   * @param stateGetter The base state getter for unit conversions
   * @return A ServicingOffset with deltaUnits, prechargeKg, and rechargeKg
   */
  public ServicingOffset offset(BigDecimal salesKg, BigDecimal rechargeKg,
      BigDecimal initialChargeKgUnit, EngineNumber prechargePopRaw,
      EngineNumber prechargeIntensityRaw, boolean useExplicitRechargeEffective,
      BigDecimal implicitPrechargeKg, StateGetter stateGetter) {

    PrechargeInfo prechargeInfo = describePrecharge(prechargePopRaw);
    boolean hasPrecharge = prechargeInfo.getHasPrecharge();
    boolean isPercentPrecharge = prechargeInfo.getIsPercentPrecharge();
    boolean isCircularCase = isPercentPrecharge && useExplicitRechargeEffective;

    if (isCircularCase) {
      return offsetVolumeSalesPercentPrecharge(salesKg, rechargeKg, initialChargeKgUnit,
          prechargePopRaw, prechargeIntensityRaw);
    } else if (hasPrecharge && useExplicitRechargeEffective) {
      return offsetVolumeSalesExplicitPrecharge(salesKg, rechargeKg, initialChargeKgUnit,
          prechargePopRaw, prechargeIntensityRaw, stateGetter);
    } else {
      return offsetUnitSales(salesKg, rechargeKg, initialChargeKgUnit, implicitPrechargeKg);
    }
  }

  /**
   * Describe the precharge configuration from a raw precharge population value.
   *
   * @param prechargePopRaw The raw precharge population value, or null if unspecified
   * @return A PrechargeInfo describing the precharge configuration
   */
  private PrechargeInfo describePrecharge(EngineNumber prechargePopRaw) {
    if (prechargePopRaw == null) {
      return new PrechargeInfo(false, false);
    } else {
      boolean hasPrecharge = prechargePopRaw.getValue().compareTo(BigDecimal.ZERO) != 0;
      if (!hasPrecharge) {
        return new PrechargeInfo(false, false);
      } else {
        String units = prechargePopRaw.getUnits();
        boolean isPercent = units != null && units.contains("%");
        return new PrechargeInfo(true, isPercent);
      }
    }
  }

  /**
   * Handle the circular case with percentage-based precharge and volume-based sales.
   *
   * <p>Uses the analytical solution for the circular dependency where changing sales changes new
   * equipment, which changes the precharge population, which changes new equipment again:</p>
   *
   * <pre>e_new = V_sales / (c_initial + e_precharge% * c_precharge)</pre>
   *
   * @param salesKg The total substance sales volume in kilograms
   * @param rechargeKg The recharge volume in kilograms
   * @param initialChargeKgUnit The initial charge per unit in kg/unit
   * @param prechargePopRaw The raw precharge population as a percentage
   * @param prechargeIntensityRaw The raw precharge intensity in kg/unit
   * @return A ServicingOffset with the computed deltaUnits and prechargeKg
   */
  private ServicingOffset offsetVolumeSalesPercentPrecharge(BigDecimal salesKg,
      BigDecimal rechargeKg, BigDecimal initialChargeKgUnit, EngineNumber prechargePopRaw,
      EngineNumber prechargeIntensityRaw) {
    BigDecimal prechargeRatio = prechargePopRaw.getValue()
        .divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
    BigDecimal prechargeIntensityKg = prechargeIntensityRaw.getValue();
    BigDecimal denominator = initialChargeKgUnit
        .add(prechargeRatio.multiply(prechargeIntensityKg));
    BigDecimal deltaUnits = DivisionHelper.divideWithZero(
        salesKg.subtract(rechargeKg), denominator);
    BigDecimal prechargeKg = deltaUnits.multiply(prechargeRatio).multiply(prechargeIntensityKg);
    return new ServicingOffset(deltaUnits, prechargeKg, rechargeKg);
  }

  /**
   * Handle explicit precharge for kg-based tracking with absolute (units) precharge population.
   *
   * @param salesKg The total substance sales volume in kilograms
   * @param rechargeKg The recharge volume in kilograms
   * @param initialChargeKgUnit The initial charge per unit in kg/unit
   * @param prechargePopRaw The raw precharge population in absolute units
   * @param prechargeIntensityRaw The raw precharge intensity in kg/unit
   * @param stateGetter The base state getter for unit conversions
   * @return A ServicingOffset with the computed deltaUnits and prechargeKg
   */
  private ServicingOffset offsetVolumeSalesExplicitPrecharge(BigDecimal salesKg,
      BigDecimal rechargeKg, BigDecimal initialChargeKgUnit, EngineNumber prechargePopRaw,
      EngineNumber prechargeIntensityRaw, StateGetter stateGetter) {
    OverridingConverterStateGetter prechargeStateGetter =
        new OverridingConverterStateGetter(stateGetter);
    UnitConverter prechargeConverter = new UnitConverter(prechargeStateGetter);
    EngineNumber prechargePop = prechargeConverter.convert(prechargePopRaw, "units");
    prechargeStateGetter.setPopulation(prechargePop);
    EngineNumber prechargeVolume = prechargeConverter.convert(prechargeIntensityRaw, "kg");
    prechargeStateGetter.clearPopulation();
    BigDecimal prechargeKg = prechargeVolume.getValue();
    BigDecimal availableForNewUnitsKg = salesKg.subtract(rechargeKg).subtract(prechargeKg);
    BigDecimal deltaUnits = DivisionHelper.divideWithZero(
        availableForNewUnitsKg, initialChargeKgUnit);
    return new ServicingOffset(deltaUnits, prechargeKg, rechargeKg);
  }

  /**
   * Handle units-based tracking using implicit precharge (already added on top).
   *
   * @param salesKg The total substance sales volume in kilograms
   * @param rechargeKg The recharge volume in kilograms
   * @param initialChargeKgUnit The initial charge per unit in kg/unit
   * @param implicitPrechargeKg The implicit precharge volume in kilograms
   * @return A ServicingOffset with the computed deltaUnits and prechargeKg
   */
  private ServicingOffset offsetUnitSales(BigDecimal salesKg, BigDecimal rechargeKg,
      BigDecimal initialChargeKgUnit, BigDecimal implicitPrechargeKg) {
    BigDecimal prechargeKg = implicitPrechargeKg;
    BigDecimal availableForNewUnitsKg = salesKg.subtract(rechargeKg).subtract(prechargeKg);
    BigDecimal deltaUnits = DivisionHelper.divideWithZero(
        availableForNewUnitsKg, initialChargeKgUnit);
    return new ServicingOffset(deltaUnits, prechargeKg, rechargeKg);
  }
}
