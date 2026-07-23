/**
 * Builder that calculates servicing (precharge/recharge) offsets for population changes.
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
 * Builder that calculates servicing (precharge/recharge) offsets for population changes.
 */
public class ServicingOffsetBuilder {

  private BigDecimal salesKg;
  private BigDecimal rechargeKg;
  private BigDecimal initialChargeKgUnit;
  private EngineNumber prechargePopRaw;
  private EngineNumber prechargeIntensityRaw;
  private boolean useExplicitRechargeEffective;
  private BigDecimal implicitPrechargeKg;
  private StateGetter stateGetter;

  /**
   * Set the total substance sales volume in kilograms.
   *
   * @param salesKg The total substance sales volume in kilograms
   * @return This builder for chaining
   */
  public ServicingOffsetBuilder setSalesKg(BigDecimal salesKg) {
    this.salesKg = salesKg;
    return this;
  }

  /**
   * Set the recharge volume in kilograms.
   *
   * @param rechargeKg The recharge volume in kilograms
   * @return This builder for chaining
   */
  public ServicingOffsetBuilder setRechargeKg(BigDecimal rechargeKg) {
    this.rechargeKg = rechargeKg;
    return this;
  }

  /**
   * Set the initial charge per unit in kg/unit.
   *
   * @param initialChargeKgUnit The initial charge per unit in kg/unit
   * @return This builder for chaining
   */
  public ServicingOffsetBuilder setInitialChargeKgUnit(BigDecimal initialChargeKgUnit) {
    this.initialChargeKgUnit = initialChargeKgUnit;
    return this;
  }

  /**
   * Set the raw precharge population value.
   *
   * @param prechargePopRaw The raw precharge population value, or null if unspecified
   * @return This builder for chaining
   */
  public ServicingOffsetBuilder setPrechargePopRaw(EngineNumber prechargePopRaw) {
    this.prechargePopRaw = prechargePopRaw;
    return this;
  }

  /**
   * Set the raw precharge intensity value.
   *
   * @param prechargeIntensityRaw The raw precharge intensity value
   * @return This builder for chaining
   */
  public ServicingOffsetBuilder setPrechargeIntensityRaw(EngineNumber prechargeIntensityRaw) {
    this.prechargeIntensityRaw = prechargeIntensityRaw;
    return this;
  }

  /**
   * Set whether explicit (volume-based) recharge is in use.
   *
   * @param useExplicitRechargeEffective Whether explicit (volume-based) recharge is in use
   * @return This builder for chaining
   */
  public ServicingOffsetBuilder setUseExplicitRechargeEffective(
      boolean useExplicitRechargeEffective) {
    this.useExplicitRechargeEffective = useExplicitRechargeEffective;
    return this;
  }

  /**
   * Set the implicit precharge volume in kilograms.
   *
   * @param implicitPrechargeKg The implicit precharge volume in kilograms
   * @return This builder for chaining
   */
  public ServicingOffsetBuilder setImplicitPrechargeKg(BigDecimal implicitPrechargeKg) {
    this.implicitPrechargeKg = implicitPrechargeKg;
    return this;
  }

  /**
   * Set the base state getter for unit conversions.
   *
   * @param stateGetter The base state getter for unit conversions
   * @return This builder for chaining
   */
  public ServicingOffsetBuilder setStateGetter(StateGetter stateGetter) {
    this.stateGetter = stateGetter;
    return this;
  }

  /**
   * Build the servicing offset for a population change.
   *
   * <p>This method determines the precharge and recharge offsets and the resulting unit delta
   * based on the tracking mode and precharge configuration. It dispatches to the appropriate
   * branch: percentage precharge with volume-based sales (circular), explicit precharge with
   * volume-based sales, or units-based tracking.</p>
   *
   * @return A ServicingOffset with deltaUnits, prechargeKg, and rechargeKg
   */
  public ServicingOffset build() {
    ServicingStatus servicingStatus = describeServicing();
    boolean isServicingEnabled = servicingStatus.isServicingEnabled();
    boolean isPercentPopulation = servicingStatus.isPercentPopulation();
    boolean isCircularCase = isPercentPopulation && useExplicitRechargeEffective;

    if (isCircularCase) {
      return offsetVolumeSalesPercentPrecharge();
    } else if (isServicingEnabled && useExplicitRechargeEffective) {
      return offsetVolumeSalesExplicitPrecharge();
    } else {
      return offsetUnitSales();
    }
  }

  /**
   * Describe the servicing configuration from the raw precharge population value.
   *
   * @return A ServicingStatus describing the precharge configuration
   */
  private ServicingStatus describeServicing() {
    if (prechargePopRaw == null) {
      return new ServicingStatus(false, false);
    } else {
      boolean hasPrecharge = prechargePopRaw.getValue().compareTo(BigDecimal.ZERO) != 0;
      if (!hasPrecharge) {
        return new ServicingStatus(false, false);
      } else {
        String units = prechargePopRaw.getUnits();
        boolean isPercent = units != null && units.contains("%");
        return new ServicingStatus(true, isPercent);
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
   * @return A ServicingOffset with the computed deltaUnits and prechargeKg
   */
  private ServicingOffset offsetVolumeSalesPercentPrecharge() {
    BigDecimal prechargeRatio = prechargePopRaw.getValue()
        .divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
    BigDecimal prechargeIntensityKg = prechargeIntensityRaw.getValue();
    BigDecimal denominator = initialChargeKgUnit
        .add(prechargeRatio.multiply(prechargeIntensityKg));
    BigDecimal deltaUnits = DivisionHelper.divideWithZero(
        salesKg.subtract(rechargeKg),
        denominator
    );
    BigDecimal prechargeKg = deltaUnits.multiply(prechargeRatio).multiply(prechargeIntensityKg);
    return new ServicingOffset(deltaUnits, prechargeKg, rechargeKg);
  }

  /**
   * Handle explicit precharge for kg-based tracking with absolute (units) precharge population.
   *
   * @return A ServicingOffset with the computed deltaUnits and prechargeKg
   */
  private ServicingOffset offsetVolumeSalesExplicitPrecharge() {
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
        availableForNewUnitsKg,
        initialChargeKgUnit
    );
    return new ServicingOffset(deltaUnits, prechargeKg, rechargeKg);
  }

  /**
   * Handle units-based tracking using implicit precharge (already added on top).
   *
   * @return A ServicingOffset with the computed deltaUnits and prechargeKg
   */
  private ServicingOffset offsetUnitSales() {
    BigDecimal prechargeKg = implicitPrechargeKg;
    BigDecimal availableForNewUnitsKg = salesKg.subtract(rechargeKg).subtract(prechargeKg);
    BigDecimal deltaUnits = DivisionHelper.divideWithZero(
        availableForNewUnitsKg,
        initialChargeKgUnit
    );
    return new ServicingOffset(deltaUnits, prechargeKg, rechargeKg);
  }
}
