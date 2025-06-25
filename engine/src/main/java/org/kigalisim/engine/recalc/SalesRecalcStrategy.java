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
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.support.DivisionHelper;
import org.kigalisim.engine.support.ExceptionsGenerator;

/**
 * Strategy for recalculating sales.
 */
public class SalesRecalcStrategy implements RecalcStrategy {

  private final Optional<UseKey> scope;

  /**
   * Create a new SalesRecalcStrategy.
   *
   * @param scope The scope to use for calculations, empty to use engine's current scope
   */
  public SalesRecalcStrategy(Optional<UseKey> scope) {
    this.scope = scope;
  }

  @Override
  public void execute(Engine target, RecalcKit kit) {
    UseKey scopeEffective = scope.orElse(target.getScope());

    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(kit.getStateGetter());
    UnitConverter unitConverter = new UnitConverter(stateGetter);

    if (scopeEffective.getApplication() == null || scopeEffective.getSubstance() == null) {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating sales", "");
    }

    StreamKeeper streamKeeper = kit.getStreamKeeper();

    // Get recharge population
    EngineNumber basePopulation = target.getStream("priorEquipment", Optional.of(scopeEffective), Optional.empty());
    stateGetter.setPopulation(basePopulation);
    EngineNumber rechargePopRaw = streamKeeper.getRechargePopulation(scopeEffective);
    EngineNumber rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.clearPopulation();

    // Switch into recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    EngineNumber rechargeIntensityRaw = streamKeeper.getRechargeIntensity(scopeEffective);
    EngineNumber rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    // Determine initial charge
    EngineNumber initialChargeRaw = target.getInitialCharge("sales");
    EngineNumber initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");

    // Get recycling volume
    stateGetter.setVolume(rechargeVolume);
    EngineNumber recoveryVolumeRaw = streamKeeper.getRecoveryRate(scopeEffective);
    EngineNumber recoveryVolume = unitConverter.convert(recoveryVolumeRaw, "kg");
    stateGetter.clearVolume();

    // Get recycling amount
    stateGetter.setVolume(recoveryVolume);
    EngineNumber recycledVolumeRaw = streamKeeper.getYieldRate(scopeEffective);
    EngineNumber recycledVolume = unitConverter.convert(recycledVolumeRaw, "kg");
    stateGetter.clearVolume();

    // Get recycling displaced
    BigDecimal recycledKg = recycledVolume.getValue();

    EngineNumber displacementRateRaw = streamKeeper.getDisplacementRate(scopeEffective);
    EngineNumber displacementRate = unitConverter.convert(displacementRateRaw, "%");
    BigDecimal displacementRateRatio = displacementRate.getValue().divide(
        BigDecimal.valueOf(100),
        MathContext.DECIMAL128
    );
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
    EngineNumber priorPopulationRaw = target.getStream("priorEquipment", Optional.of(scopeEffective), Optional.empty());
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
    EngineNumber manufactureRaw = target.getStream("manufacture", Optional.of(scopeEffective), Optional.empty());
    EngineNumber importRaw = target.getStream("import", Optional.of(scopeEffective), Optional.empty());
    EngineNumber priorRecycleRaw = target.getStream("recycle", Optional.of(scopeEffective), Optional.empty());

    EngineNumber manufactureSalesConverted = unitConverter.convert(manufactureRaw, "kg");
    EngineNumber importSalesConverted = unitConverter.convert(importRaw, "kg");

    BigDecimal manufactureSalesKg = manufactureSalesConverted.getValue();
    BigDecimal importSalesKg = importSalesConverted.getValue();
    BigDecimal totalNonRecycleKg = manufactureSalesKg.add(importSalesKg);

    // Get stream enabled status
    boolean manufactureEnabled = streamKeeper.hasStreamBeenEnabled(scopeEffective, "manufacture");
    boolean importEnabled = streamKeeper.hasStreamBeenEnabled(scopeEffective, "import");

    // Build distribution using the new builder
    SalesStreamDistribution distribution = SalesStreamDistributionBuilder.buildDistribution(
        manufactureSalesConverted,
        importSalesConverted,
        manufactureEnabled,
        importEnabled
    );

    BigDecimal percentManufacture = distribution.getPercentManufacture();
    BigDecimal percentImport = distribution.getPercentImport();

    // Recycle
    EngineNumber newRecycleValue = new EngineNumber(recycledDisplacedKg, "kg");
    streamKeeper.setStream(scopeEffective, "recycle", newRecycleValue);

    // New values - preserve explicit values when demand is zero, recalculate when there's demand
    BigDecimal requiredKgUnbound = kgForRecharge.add(kgForNew);
    boolean requiredKgNegative = requiredKgUnbound.compareTo(BigDecimal.ZERO) < 0;
    BigDecimal requiredKg = requiredKgNegative ? BigDecimal.ZERO : requiredKgUnbound;

    BigDecimal newManufactureKg = percentManufacture.multiply(requiredKg);
    BigDecimal newImportKg = percentImport.multiply(requiredKg);
    EngineNumber newManufacture = new EngineNumber(newManufactureKg, "kg");
    EngineNumber newImport = new EngineNumber(newImportKg, "kg");

    // Call Engine.setStream with propagateChanges=false to match JavaScript behavior
    target.setStreamFor("manufacture", newManufacture, Optional.empty(), Optional.of(scopeEffective), false, Optional.empty());
    target.setStreamFor("import", newImport, Optional.empty(), Optional.of(scopeEffective), false, Optional.empty());
  }
}
