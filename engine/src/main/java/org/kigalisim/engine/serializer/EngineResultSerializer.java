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
import org.kigalisim.engine.state.UseKey;

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
   * @param useKey The UseKey containing application and substance information
   * @param year The year for which a result should be serialized
   * @return Snapshot of the result in the current engine state for the given
   *     application and substance
   */
  public EngineResult getResult(UseKey useKey, int year) {
    // Create builder
    EngineResultBuilder builder = new EngineResultBuilder();
    builder.setApplication(useKey.getApplication());
    builder.setSubstance(useKey.getSubstance());
    builder.setYear(year);
    builder.setScenarioName(engine.getScenarioName());
    builder.setTrialNumber(engine.getTrialNumber());

    // Add values into builder
    parseMainBody(builder, useKey);
    parseTradeSupplement(builder, useKey);

    return builder.build();
  }

  /**
   * Parse the attributes which are actually returned to the user.
   *
   * @param builder The builder into which parsed values should be registered
   * @param useKey The UseKey containing application and substance information
   */
  private void parseMainBody(EngineResultBuilder builder, UseKey useKey) {
    // Prepare units
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);

    // Get sales
    EngineNumber recycleRaw = engine.getStreamFor(useKey, "recycle");
    EngineNumber recycleValue = unitConverter.convert(recycleRaw, "kg");
    builder.setRecycleValue(recycleValue);

    // Get population metrics
    EngineNumber populationValue = engine.getStreamFor(useKey, "equipment");

    // Calculate energy consumption from equipment population
    EngineNumber energyIntensity = engine.getEqualsEnergyIntensityFor(useKey);
    boolean hasDataForEnergy = populationValue != null && energyIntensity != null;

    EngineNumber energyConsumptionValue;
    if (hasDataForEnergy) {
      // Set up UnitConverter with energy intensity for conversion
      OverridingConverterStateGetter energyStateGetter = new OverridingConverterStateGetter(stateGetter);
      energyStateGetter.setEnergyIntensity(energyIntensity);
      UnitConverter energyUnitConverter = new UnitConverter(energyStateGetter);

      // Convert population to energy consumption
      energyConsumptionValue = energyUnitConverter.convert(populationValue, "kwh");
    } else {
      energyConsumptionValue = new EngineNumber(BigDecimal.ZERO, "kwh");
    }
    builder.setEnergyConsumption(energyConsumptionValue);

    builder.setPopulationValue(populationValue);

    EngineNumber populationNew = engine.getStreamFor(useKey, "newEquipment");
    builder.setPopulationNew(populationNew);

    EngineNumber eolEmissions = engine.getStreamFor(useKey, "eolEmissions");
    builder.setEolEmissions(eolEmissions);

    // Calculate initial charge emissions
    EngineNumber initialChargeEmissions = calculateInitialChargeEmissions(useKey);
    builder.setInitialChargeEmissions(initialChargeEmissions);

    // Get sales for offset calculation
    EngineNumber manufactureRaw = engine.getStreamFor(useKey, "domestic");
    EngineNumber importRaw = engine.getStreamFor(useKey, "import");

    // Convert sales values for offset calculation
    EngineNumber manufactureValue = unitConverter.convert(manufactureRaw, "kg");
    EngineNumber importValue = unitConverter.convert(importRaw, "kg");

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

    // Use values directly - recycling already handled at stream level
    builder.setDomesticValue(manufactureValue);
    builder.setImportValue(importValue);

    // Get consumption
    EngineNumber consumptionByVolume = getConsumptionByVolume(
        useKey, unitConverter);

    EngineNumber domesticConsumptionValue = getConsumptionForVolume(
        manufactureValue, consumptionByVolume, stateGetter, unitConverter);
    builder.setDomesticConsumptionValue(domesticConsumptionValue);

    EngineNumber importConsumptionValue = getConsumptionForVolume(
        importValue, consumptionByVolume, stateGetter, unitConverter);
    builder.setImportConsumptionValue(importConsumptionValue);

    // Set export values (exports don't affect equipment population, just track volume and consumption)
    EngineNumber exportRaw = engine.getStreamFor(useKey, "export");
    EngineNumber exportValue;
    if (exportRaw == null) {
      exportValue = new EngineNumber(BigDecimal.ZERO, "kg");
    } else {
      exportValue = unitConverter.convert(exportRaw, "kg");
    }
    builder.setExportValue(exportValue);
    EngineNumber exportConsumptionValue = getConsumptionForVolume(
        exportValue, consumptionByVolume, stateGetter, unitConverter);
    builder.setExportConsumptionValue(exportConsumptionValue);

    EngineNumber recycleConsumptionValue = getConsumptionForVolume(
        recycleValue, consumptionByVolume, stateGetter, unitConverter);
    builder.setRecycleConsumptionValue(recycleConsumptionValue);

    // Offset recharge emissions
    EngineNumber rechargeEmissions = engine.getStreamFor(
        useKey,
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
   * @param useKey The UseKey containing application and substance information
   * @param unitConverter The unit converter to use
   * @return The consumption by volume engine number
   */
  private EngineNumber getConsumptionByVolume(UseKey useKey,
                                            UnitConverter unitConverter) {
    EngineNumber consumptionRaw = engine.getGhgIntensity(useKey);
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
    } else {
      stateGetter.setVolume(volume);
      return unitConverter.convert(consumptionByVolume, "tCO2e");
    }
  }

  /**
   * Parse information for the import supplement.
   *
   * <p>Parse information for the import supplement which is needed to perform
   * user-configurable import attribution options (are substances associated
   * with importer or exporter).</p>
   *
   * @param builder The builder into which parsed values should be registered
   * @param useKey The UseKey containing application and substance information
   */
  private void parseTradeSupplement(EngineResultBuilder builder,
                                    UseKey useKey) {
    // Prepare units
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    final UnitConverter unitConverter = new UnitConverter(stateGetter);

    EngineNumber ghgIntensity = engine.getEqualsGhgIntensityFor(useKey);
    stateGetter.setSubstanceConsumption(ghgIntensity);

    EngineNumber importInitialChargeUnit = engine.getRawInitialChargeFor(
        useKey, "import");
    stateGetter.setAmortizedUnitVolume(importInitialChargeUnit);

    // Check if this is a per-unit emissions factor
    if (isPerUnit(ghgIntensity)) {
      handlePerUnitTradeSupplement(builder);
      return;
    }

    // Determine import value without recharge
    EngineNumber totalImportValue = engine.getStreamFor(useKey, "import");
    EngineNumber totalDomesticValue = engine.getStreamFor(useKey, "domestic");
    EngineNumber totalRechargeEmissions = engine.getStreamFor(
        useKey, "rechargeEmissions");

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

    // Get recycling that satisfies recharge demand
    EngineNumber recycleRecharge = engine.getStreamFor(useKey, "recycleRecharge");
    BigDecimal recycleRechargeKg = unitConverter.convert(recycleRecharge, "kg").getValue();

    // Calculate virgin recharge demand (total recharge minus recycling)
    BigDecimal virginRechargeKg = totalRechargeKg.subtract(recycleRechargeKg);
    if (virginRechargeKg.compareTo(BigDecimal.ZERO) < 0) {
      virginRechargeKg = BigDecimal.ZERO;
    }

    BigDecimal importRechargeKg = proportionImport.multiply(virginRechargeKg);
    BigDecimal importForInitialChargeKg = totalImportValueKg.subtract(importRechargeKg);

    EngineNumber value = new EngineNumber(importForInitialChargeKg, "kg");

    // Determine consumption (tCO2e) and population (units)
    EngineNumber consumption = unitConverter.convert(value, "tCO2e");
    EngineNumber population = unitConverter.convert(value, "units");

    // Package trade supplement with placeholder export values (zero for now)
    EngineNumber zeroValue = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber zeroConsumption = new EngineNumber(BigDecimal.ZERO, "tCO2e");
    TradeSupplement tradeSupplement = new TradeSupplement(value, consumption, population, zeroValue, zeroConsumption);
    builder.setTradeSupplement(tradeSupplement);
  }

  /**
   * Get stream value converted to kg, returning zero if stream is null.
   *
   * @param useKey The UseKey containing application and substance information
   * @param streamName The name of the stream to retrieve
   * @param unitConverter The unit converter for calculations
   * @return The stream value in kg, or zero if null
   */
  private EngineNumber getInKg(UseKey useKey, String streamName, UnitConverter unitConverter) {
    EngineNumber value = engine.getStreamFor(useKey, streamName);
    if (value == null) {
      return new EngineNumber(BigDecimal.ZERO, "kg");
    } else {
      return unitConverter.convert(value, "kg");
    }
  }

  /**
   * Calculate initial charge emissions for a given use key.
   *
   * @param useKey The UseKey containing application and substance information
   * @return The initial charge emissions in tCO2e
   */
  private EngineNumber calculateInitialChargeEmissions(UseKey useKey) {
    // Create a properly-scoped unit converter for this specific UseKey
    OverridingConverterStateGetter scopedStateGetter = new OverridingConverterStateGetter(this.stateGetter);
    EngineNumber ghgIntensity = engine.getGhgIntensity(useKey);
    scopedStateGetter.setSubstanceConsumption(ghgIntensity);
    UnitConverter scopedConverter = new UnitConverter(scopedStateGetter);

    // Check if this is a per-unit emissions factor
    if (isPerUnit(ghgIntensity)) {
      // For per-unit emissions factors, calculate initial charge emissions differently
      // Initial charge emissions = new equipment population * per-unit emissions factor
      EngineNumber newPopulation = engine.getStreamFor(useKey, "newEquipment");
      if (newPopulation == null) {
        return new EngineNumber(BigDecimal.ZERO, "tCO2e");
      } else {
        // Convert to units if needed and multiply by per-unit factor
        EngineNumber populationInUnits = scopedConverter.convert(newPopulation, "units");
        OverridingConverterStateGetter emissionsStateGetter = new OverridingConverterStateGetter(this.stateGetter);
        emissionsStateGetter.setPopulation(populationInUnits);
        emissionsStateGetter.setSubstanceConsumption(ghgIntensity);
        UnitConverter emissionsConverter = new UnitConverter(emissionsStateGetter);
        return emissionsConverter.convert(ghgIntensity, "tCO2e");
      }
    } else {
      // Original logic for per-kg and per-mt emissions factors
      EngineNumber domesticKg = getInKg(useKey, "domestic", scopedConverter);
      EngineNumber importKg = getInKg(useKey, "import", scopedConverter);
      EngineNumber exportKg = getInKg(useKey, "export", scopedConverter);
      EngineNumber rechargeKg = getInKg(useKey, "rechargeEmissions", scopedConverter);

      // Calculate initial charge volume: (domestic + import + export) - recharge
      BigDecimal totalSales = domesticKg.getValue()
          .add(importKg.getValue())
          .add(exportKg.getValue());
      BigDecimal initialChargeVolume = totalSales.subtract(rechargeKg.getValue());

      // Ensure non-negative (if recharge > total sales, initial charge = 0)
      if (initialChargeVolume.compareTo(BigDecimal.ZERO) < 0) {
        initialChargeVolume = BigDecimal.ZERO;
      }

      // Convert to emissions using GHG intensity
      EngineNumber volumeNumber = new EngineNumber(initialChargeVolume, "kg");
      OverridingConverterStateGetter emissionsStateGetter = new OverridingConverterStateGetter(this.stateGetter);
      emissionsStateGetter.setVolume(volumeNumber);
      emissionsStateGetter.setSubstanceConsumption(ghgIntensity);
      UnitConverter emissionsConverter = new UnitConverter(emissionsStateGetter);

      return emissionsConverter.convert(ghgIntensity, "tCO2e");
    }
  }

  /**
   * Handle trade supplement calculation for per-unit emissions factors.
   *
   * <p>For per-unit emissions factors, trade supplement calculations don't apply
   * since the calculations are equipment-based rather than volume-based.</p>
   *
   * @param builder The builder into which parsed values should be registered
   */
  private void handlePerUnitTradeSupplement(EngineResultBuilder builder) {
    // Set all trade-related values to zero since calculations are equipment-based
    EngineNumber zeroValue = new EngineNumber(BigDecimal.ZERO, "kg");
    EngineNumber zeroConsumption = new EngineNumber(BigDecimal.ZERO, "tCO2e");
    EngineNumber zeroPopulation = new EngineNumber(BigDecimal.ZERO, "units");
    TradeSupplement tradeSupplement = new TradeSupplement(zeroValue, zeroConsumption, zeroPopulation, zeroValue, zeroConsumption);
    builder.setTradeSupplement(tradeSupplement);
  }

  /**
   * Check if the GHG intensity represents per-unit emissions factors.
   *
   * @param ghgIntensity The GHG intensity EngineNumber to check
   * @return True if the units are per-unit (unit or units), false otherwise
   */
  private boolean isPerUnit(EngineNumber ghgIntensity) {
    String normalizedUnits = ghgIntensity.getUnits().replaceAll("\\s+", "");
    String[] unitPieces = normalizedUnits.split("/");
    return unitPieces.length > 1 && ("unit".equals(unitPieces[1]) || "units".equals(unitPieces[1]));
  }
}
