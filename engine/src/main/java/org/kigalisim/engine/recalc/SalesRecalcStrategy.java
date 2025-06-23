/**
 * Strategy for recalculating sales.
 *
 * <p>This strategy encapsulates the logic previously found in the
 * recalcSales method of SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.math.BigDecimal;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.support.DivisionHelper;
import org.kigalisim.engine.support.ExceptionsGenerator;

/**
 * Strategy for recalculating sales.
 */
public class SalesRecalcStrategy implements RecalcStrategy {

  private final Scope scope;

  /**
   * Create a new SalesRecalcStrategy.
   *
   * @param scope The scope to use for calculations, null to use engine's current scope
   */
  public SalesRecalcStrategy(Scope scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    Scope scopeEffective = scope != null ? scope : target.getScope();

    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    if (application == null || substance == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating sales", "");
    }

    StreamKeeper streamKeeper = kit.getStreamKeeper();

    // Get recharge population
    EngineNumber basePopulation = target.getStream("priorEquipment", scopeEffective, null);
    stateGetter.setPopulation(basePopulation);
    EngineNumber rechargePopRaw = streamKeeper.getRechargePopulation(
        application,
        substance
    );
    EngineNumber rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.clearPopulation();

    // Switch into recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    EngineNumber rechargeIntensityRaw = streamKeeper.getRechargeIntensity(
        application,
        substance
    );
    EngineNumber rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    // Determine initial charge
    EngineNumber initialChargeRaw = target.getInitialCharge("sales");
    EngineNumber initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");

    // Get recycling volume
    stateGetter.setVolume(rechargeVolume);
    EngineNumber recoveryVolumeRaw = streamKeeper.getRecoveryRate(application, substance);
    EngineNumber recoveryVolume = unitConverter.convert(recoveryVolumeRaw, "kg");
    stateGetter.clearVolume();

    // Get recycling amount
    stateGetter.setVolume(recoveryVolume);
    EngineNumber recycledVolumeRaw = streamKeeper.getYieldRate(application, substance);
    EngineNumber recycledVolume = unitConverter.convert(recycledVolumeRaw, "kg");
    stateGetter.clearVolume();

    // Get recycling displaced
    BigDecimal recycledKg = recycledVolume.getValue();

    EngineNumber displacementRateRaw = streamKeeper.getDisplacementRate(
        application,
        substance
    );
    EngineNumber displacementRate = unitConverter.convert(displacementRateRaw, "%");
    BigDecimal displacementRateRatio = displacementRate.getValue().divide(BigDecimal.valueOf(100));
    final BigDecimal recycledDisplacedKg = recycledKg.multiply(displacementRateRatio);

    // Switch out of recharge population
    stateGetter.clearPopulation();

    // Determine needs for new equipment deployment
    stateGetter.setAmortizedUnitVolume(initialCharge);
    UnitConverter converter = kit.getUnitConverter();
    EngineNumber populationChangeRaw = stateGetter.getPopulationChange(converter);
    EngineNumber populationChange = unitConverter.convert(populationChangeRaw, "units");
    EngineNumber volumeForNew = unitConverter.convert(populationChange, "kg");

    // Get prior population
    EngineNumber priorPopulationRaw = target.getStream("priorEquipment", scopeEffective, null);
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    // Determine sales prior to recycling
    final BigDecimal kgForRecharge = rechargeVolume.getValue();
    final BigDecimal kgForNew = volumeForNew.getValue();

    // Return to original initial charge
    stateGetter.clearAmortizedUnitVolume();

    // Return original
    stateGetter.clearVolume();

    // Determine how much to offset domestic and imports
    EngineNumber manufactureRaw = target.getStream("manufacture", scopeEffective, null);
    EngineNumber importRaw = target.getStream("import", scopeEffective, null);
    EngineNumber priorRecycleRaw = target.getStream("recycle", scopeEffective, null);

    EngineNumber manufactureSalesConverted = unitConverter.convert(manufactureRaw, "kg");
    EngineNumber importSalesConverted = unitConverter.convert(importRaw, "kg");
    EngineNumber priorRecycleSalesConverted = unitConverter.convert(priorRecycleRaw, "kg");

    BigDecimal manufactureSalesKg = manufactureSalesConverted.getValue();
    BigDecimal importSalesKg = importSalesConverted.getValue();
    BigDecimal priorRecycleSalesKg = priorRecycleSalesConverted.getValue();
    BigDecimal totalNonRecycleKg = manufactureSalesKg.add(importSalesKg);

    // Get stream percentages for allocation
    BigDecimal percentManufacture;
    BigDecimal percentImport;

    if (totalNonRecycleKg.compareTo(BigDecimal.ZERO) == 0) {
      EngineNumber manufactureInitialCharge = target.getInitialCharge("manufacture");
      EngineNumber importInitialCharge = target.getInitialCharge("import");
      BigDecimal manufactureInitialChargeVal = manufactureInitialCharge.getValue();
      BigDecimal importInitialChargeVal = unitConverter
          .convert(importInitialCharge, manufactureInitialCharge.getUnits()).getValue();
      BigDecimal totalInitialChargeVal = manufactureInitialChargeVal.add(importInitialChargeVal);

      if (totalInitialChargeVal.compareTo(BigDecimal.ZERO) == 0) {
        percentManufacture = BigDecimal.ONE;
        percentImport = BigDecimal.ZERO;
      } else {
        percentManufacture = DivisionHelper.divideWithZero(
            manufactureInitialChargeVal, totalInitialChargeVal);
        percentImport = DivisionHelper.divideWithZero(
            importInitialChargeVal, totalInitialChargeVal);
      }
    } else {
      percentManufacture = DivisionHelper.divideWithZero(manufactureSalesKg, totalNonRecycleKg);
      percentImport = DivisionHelper.divideWithZero(importSalesKg, totalNonRecycleKg);
    }

    // Recycle
    EngineNumber newRecycleValue = new EngineNumber(recycledDisplacedKg, "kg");
    streamKeeper.setStream(application, substance, "recycle", newRecycleValue);

    // New values - preserve explicit values when demand is zero, recalculate when there's demand
    BigDecimal requiredKgUnbound = kgForRecharge.add(kgForNew);
    boolean requiredKgNegative = requiredKgUnbound.compareTo(BigDecimal.ZERO) < 0;
    BigDecimal requiredKg = requiredKgNegative ? BigDecimal.ZERO : requiredKgUnbound;

    BigDecimal newManufactureKg = percentManufacture.multiply(requiredKg);
    BigDecimal newImportKg = percentImport.multiply(requiredKg);
    EngineNumber newManufacture = new EngineNumber(newManufactureKg, "kg");
    EngineNumber newImport = new EngineNumber(newImportKg, "kg");

    // Call Engine.setStream with propagateChanges=false to match JavaScript behavior
    target.setStream("manufacture", newManufacture, null, scopeEffective, false, null);
    target.setStream("import", newImport, null, scopeEffective, false, null);
  }
}
