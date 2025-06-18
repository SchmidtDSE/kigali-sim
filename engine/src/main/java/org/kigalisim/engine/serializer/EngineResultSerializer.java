/**
 * Logic to serialize out snapshots of results from the engine.
 *
 * <p>This class provides functionality to extract and serialize results from the
 * simulation engine for specific applications, substances, and years.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.serializer;

import java.math.BigDecimal;
import java.math.MathContext;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;

/**
 * Decorator around an engine to serialize out results.
 *
 * <p>This class wraps an Engine instance and provides methods to extract
 * structured results for specific simulation parameters.</p>
 */
public class EngineResultSerializer {
  private final Engine engine;
  private final ConverterStateGetter stateGetter;

  /**
   * Create a new decorator to produce engine result snapshots.
   *
   * @param engine The engine from which results will be serialized
   * @param stateGetter State getter by which to access values at the current state
   *     that the engine is focusing on. This will not be modified, only read.
   */
  public EngineResultSerializer(Engine engine, ConverterStateGetter stateGetter) {
    this.engine = engine;
    this.stateGetter = stateGetter;
  }

  /**
   * Serialize the results for an application and substance in a given year.
   *
   * @param application The name of the application for which a result should be
   *     serialized like commercial refrigeration
   * @param substance The name of the substance like HFC-134a for which a result
   *     should be serialized
   * @param year The year for which a result should be serialized
   * @return Snapshot of the result in the current engine state for the given
   *     application and substance
   */
  public EngineResult getResult(String application, String substance, int year) {
    // Create builder
    EngineResultBuilder builder = new EngineResultBuilder();
    builder.setApplication(application);
    builder.setSubstance(substance);
    builder.setYear(year);

    // Add values into builder
    parseMainBody(builder, application, substance);
    parseImportSupplement(builder, application, substance);

    return builder.build();
  }

  /**
   * Parse the attributes which are actually returned to the user.
   *
   * @param builder The builder into which parsed values should be registered
   * @param application The name of the application for which to get values
   *     like commercial refrigeration
   * @param substance The name of the substance for which to get values
   *     like HFC-134a
   */
  private void parseMainBody(EngineResultBuilder builder, String application, String substance) {
    // Prepare units
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);

    // Get sales
    EngineNumber manufactureRaw = engine.getStreamRaw(application, substance, "manufacture");
    EngineNumber importRaw = engine.getStreamRaw(application, substance, "import");
    EngineNumber recycleRaw = engine.getStreamRaw(application, substance, "recycle");

    EngineNumber manufactureValue = unitConverter.convert(manufactureRaw, "kg");
    EngineNumber importValue = unitConverter.convert(importRaw, "kg");
    EngineNumber recycleValue = unitConverter.convert(recycleRaw, "kg");
    builder.setRecycleValue(recycleValue);

    // Get total energy consumption
    EngineNumber energyConsumptionValue = engine.getStreamRaw(application, substance, "energy");
    builder.setEnergyConsumption(energyConsumptionValue);

    // Get emissions
    EngineNumber populationValue = engine.getStreamRaw(application, substance, "equipment");
    builder.setPopulationValue(populationValue);

    EngineNumber populationNew = engine.getStreamRaw(application, substance, "newEquipment");
    builder.setPopulationNew(populationNew);

    EngineNumber eolEmissions = engine.getStreamRaw(application, substance, "eolEmissions");
    builder.setEolEmissions(eolEmissions);

    // Get percent for offset
    BigDecimal manufactureKg = manufactureValue.getValue();
    BigDecimal importKg = importValue.getValue();
    BigDecimal recycleKg = recycleValue.getValue();

    BigDecimal nonRecycleSalesKg = manufactureKg.add(importKg);
    boolean noSales = nonRecycleSalesKg.compareTo(BigDecimal.ZERO) == 0;
    BigDecimal percentManufacture;
    if (noSales) {
      percentManufacture = BigDecimal.ONE;
    } else {
      percentManufacture = manufactureKg.divide(nonRecycleSalesKg, MathContext.DECIMAL128);
    }
    BigDecimal percentImport = BigDecimal.ONE.subtract(percentManufacture);

    // Offset sales
    EngineNumber manufactureValueOffset = new EngineNumber(
        manufactureKg.subtract(recycleKg.multiply(percentManufacture)), "kg");
    builder.setManufactureValue(manufactureValueOffset);

    EngineNumber importValueOffset = new EngineNumber(
        importKg.subtract(recycleKg.multiply(percentImport)), "kg");
    builder.setImportValue(importValueOffset);

    // Get consumption
    EngineNumber consumptionByVolume = getConsumptionByVolume(
        application, substance, unitConverter);

    EngineNumber domesticConsumptionValue = getConsumptionForVolume(
        manufactureValueOffset, consumptionByVolume, stateGetter, unitConverter);
    builder.setDomesticConsumptionValue(domesticConsumptionValue);

    EngineNumber importConsumptionValue = getConsumptionForVolume(
        importValueOffset, consumptionByVolume, stateGetter, unitConverter);
    builder.setImportConsumptionValue(importConsumptionValue);

    EngineNumber recycleConsumptionValue = getConsumptionForVolume(
        recycleValue, consumptionByVolume, stateGetter, unitConverter);
    builder.setRecycleConsumptionValue(recycleConsumptionValue);

    // Offset recharge emissions
    EngineNumber rechargeEmissions = engine.getStreamRaw(
        application,
        substance,
        "rechargeEmissions"
    );
    OverridingConverterStateGetter clearStateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter clearUnitConverter = new UnitConverter(clearStateGetter);
    EngineNumber rechargeEmissionsConvert = clearUnitConverter.convert(rechargeEmissions, "tCO2e");
    EngineNumber rechargeEmissionsOffset = new EngineNumber(
        rechargeEmissionsConvert.getValue().subtract(recycleConsumptionValue.getValue()), "tCO2e");
    builder.setRechargeEmissions(rechargeEmissionsOffset);
  }

  /**
   * Get consumption calculation strategy based on volume.
   *
   * @param application The application name
   * @param substance The substance name
   * @param unitConverter The unit converter to use
   * @return The consumption by volume engine number
   */
  private EngineNumber getConsumptionByVolume(String application, String substance,
                                            UnitConverter unitConverter) {
    EngineNumber consumptionRaw = engine.getGhgIntensity(application, substance);
    String units = consumptionRaw.getUnits();
    if (units.endsWith("kg") || units.endsWith("mt")) {
      return consumptionRaw;
    } else {
      return unitConverter.convert(consumptionRaw, "tCO2e / kg");
    }
  }

  /**
   * Get consumption for a specific volume.
   *
   * @param volume The volume to calculate consumption for
   * @param consumptionByVolume The consumption rate per volume
   * @param stateGetter The state getter for unit conversion context
   * @param unitConverter The unit converter to use
   * @return The consumption engine number
   */
  private EngineNumber getConsumptionForVolume(EngineNumber volume,
                                             EngineNumber consumptionByVolume,
                                             OverridingConverterStateGetter stateGetter,
                                             UnitConverter unitConverter) {
    if (volume.getValue().compareTo(BigDecimal.ZERO) == 0) {
      return new EngineNumber(BigDecimal.ZERO, "tCO2e");
    }

    stateGetter.setVolume(volume);
    return unitConverter.convert(consumptionByVolume, "tCO2e");
  }

  /**
   * Parse information for the import supplement.
   *
   * <p>Parse information for the import supplement which is needed to perform
   * user-configurable import attribution options (are substances associated
   * with importer or exporter).</p>
   *
   * @param builder The builder into which parsed values should be registered
   * @param application The name of the application for which to get values
   *     like commercial refrigeration
   * @param substance The name of the substance for which to get values
   *     like HFC-134a
   */
  private void parseImportSupplement(EngineResultBuilder builder,
                                    String application, String substance) {
    // Prepare units
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);

    EngineNumber ghgIntensity = engine.getEqualsGhgIntensityFor(application, substance);
    stateGetter.setSubstanceConsumption(ghgIntensity);

    EngineNumber importInitialChargeUnit = engine.getRawInitialChargeFor(
        application, substance, "import");
    stateGetter.setAmortizedUnitVolume(importInitialChargeUnit);

    // Determine import value without recharge
    EngineNumber totalImportValue = engine.getStreamRaw(application, substance, "import");
    EngineNumber totalDomesticValue = engine.getStreamRaw(application, substance, "manufacture");
    EngineNumber totalRechargeEmissions = engine.getStreamRaw(
        application, substance, "rechargeEmissions");

    BigDecimal totalImportValueKg = unitConverter.convert(totalImportValue, "kg").getValue();
    BigDecimal totalDomesticValueKg = unitConverter.convert(totalDomesticValue, "kg").getValue();
    BigDecimal totalKg = totalImportValueKg.add(totalDomesticValueKg);

    BigDecimal proportionImport;
    if (totalKg.compareTo(BigDecimal.ZERO) == 0) {
      proportionImport = BigDecimal.ZERO;
    } else {
      proportionImport = totalImportValueKg.divide(totalKg, MathContext.DECIMAL128);
    }
    BigDecimal totalRechargeKg = unitConverter.convert(totalRechargeEmissions, "kg").getValue();

    BigDecimal importRechargeKg = proportionImport.multiply(totalRechargeKg);
    BigDecimal importForInitialChargeKg = totalImportValueKg.subtract(importRechargeKg);

    EngineNumber value = new EngineNumber(importForInitialChargeKg, "kg");

    // Determine consumption (tCO2e) and population (units)
    EngineNumber consumption = unitConverter.convert(value, "tCO2e");
    EngineNumber population = unitConverter.convert(value, "units");

    // Package
    ImportSupplement importSupplement = new ImportSupplement(value, consumption, population);
    builder.setImportSupplement(importSupplement);
  }
}
