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
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;
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
    EngineNumber domesticRaw = target.getStream("domestic", Optional.of(scopeEffective), Optional.empty());
    EngineNumber importRaw = target.getStream("import", Optional.of(scopeEffective), Optional.empty());
    EngineNumber priorRecycleRaw = target.getStream("recycle", Optional.of(scopeEffective), Optional.empty());

    EngineNumber domesticSalesConverted = unitConverter.convert(domesticRaw, "kg");
    EngineNumber importSalesConverted = unitConverter.convert(importRaw, "kg");

    BigDecimal domesticSalesKg = domesticSalesConverted.getValue();
    BigDecimal importSalesKg = importSalesConverted.getValue();
    BigDecimal totalNonRecycleKg = domesticSalesKg.add(importSalesKg);

    // Get distribution using centralized method
    SalesStreamDistribution distribution = streamKeeper.getDistribution(scopeEffective);

    BigDecimal percentDomestic = distribution.getPercentDomestic();
    BigDecimal percentImport = distribution.getPercentImport();

    // Recycle
    EngineNumber newRecycleValue = new EngineNumber(recycledDisplacedKg, "kg");
    streamKeeper.setStream(scopeEffective, "recycle", newRecycleValue);

    // Get implicit recharge to avoid double-counting
    EngineNumber implicitRechargeRaw = target.getStream("implicitRecharge", Optional.of(scopeEffective), Optional.empty());
    EngineNumber implicitRecharge = unitConverter.convert(implicitRechargeRaw, "kg");
    BigDecimal implicitRechargeKg = implicitRecharge.getValue();

    // Deal with implicit recharge and recycling
    // Total demand is recharge + new equipment needs
    BigDecimal totalDemand = kgForRecharge.add(kgForNew);

    // Subtract what we can fulfill from other sources:
    // - implicitRechargeKg: recharge that was already added when units were specified
    // - recycledDisplacedKg: material available from recycling
    BigDecimal requiredKgUnbound = totalDemand
        .subtract(implicitRechargeKg)
        .subtract(recycledDisplacedKg);

    boolean requiredKgNegative = requiredKgUnbound.compareTo(BigDecimal.ZERO) < 0;
    BigDecimal requiredKg = requiredKgNegative ? BigDecimal.ZERO : requiredKgUnbound;

    BigDecimal newDomesticKg = percentDomestic.multiply(requiredKg);
    BigDecimal newImportKg = percentImport.multiply(requiredKg);

    boolean hasUnitBasedSpecs = getHasUnitBasedSpecs(streamKeeper, scopeEffective, implicitRechargeKg);

    if (hasUnitBasedSpecs) {
      // Convert back to units to preserve user intent
      // This ensures that unit-based specifications are maintained through recycling operations
      // Need to set up the converter state for proper unit conversion
      stateGetter.setAmortizedUnitVolume(initialCharge);

      // Only set streams that have non-zero allocations (i.e., are enabled)
      if (percentDomestic.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber newDomesticUnits = unitConverter.convert(
            new EngineNumber(newDomesticKg, "kg"), "units");
        target.setStreamFor("domestic", newDomesticUnits, Optional.empty(),
            Optional.of(scopeEffective), false, Optional.empty());
      }

      if (percentImport.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber newImportUnits = unitConverter.convert(
            new EngineNumber(newImportKg, "kg"), "units");
        target.setStreamFor("import", newImportUnits, Optional.empty(),
            Optional.of(scopeEffective), false, Optional.empty());
      }

      // Clear the state after conversion
      stateGetter.clearAmortizedUnitVolume();
    } else {
      // Normal kg-based setting for non-unit specifications
      // Only set streams that have non-zero allocations (i.e., are enabled)
      if (percentDomestic.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber newDomestic = new EngineNumber(newDomesticKg, "kg");
        target.setStreamFor("domestic", newDomestic, Optional.empty(),
            Optional.of(scopeEffective), false, Optional.empty());
      }

      if (percentImport.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber newImport = new EngineNumber(newImportKg, "kg");
        target.setStreamFor("import", newImport, Optional.empty(),
            Optional.of(scopeEffective), false, Optional.empty());
      }
    }
  }

  /**
   * Determines if unit-based specifications should be preserved.
   *
   * @param streamKeeper the stream keeper to use for checking specifications
   * @param scopeEffective the scope to check
   * @param implicitRechargeKg the implicit recharge amount in kg
   * @return true if unit-based specifications should be preserved
   */
  private boolean getHasUnitBasedSpecs(StreamKeeper streamKeeper, UseKey scopeEffective, BigDecimal implicitRechargeKg) {
    // Check if we had unit-based specifications that need to be preserved
    boolean hasUnitBasedSpecs = streamKeeper.hasLastSpecifiedValue(scopeEffective, "sales")
        && streamKeeper.getLastSpecifiedValue(scopeEffective, "sales").hasEquipmentUnits();

    if (hasUnitBasedSpecs) {
      // Check if the current values indicate a unit-based operation
      // If implicit recharge is present, we know units were used in the current operation
      // TODO: Consider making this explicit rather than using implicit recharge as a heuristic
      boolean currentOperationIsUnitBased = implicitRechargeKg.compareTo(BigDecimal.ZERO) > 0;

      if (!currentOperationIsUnitBased) {
        // Current operation is kg-based (like displacement), don't preserve units
        hasUnitBasedSpecs = false;
      }
    }

    return hasUnitBasedSpecs;
  }
}
