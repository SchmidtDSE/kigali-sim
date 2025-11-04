/**
 * Result of an engine execution for a substance for an application and year.
 *
 * <p>Part of a simulation result representing the values evaluated for a single
 * substance and application within a single year.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.serializer;

import java.math.BigDecimal;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Engine execution result for a specific substance, application, and year.
 *
 * <p>This class encapsulates all the calculated values for a simulation result
 * including manufacturing, import, consumption, emissions, and equipment data.</p>
 */
public class EngineResult {
  private final String application;
  private final String substance;
  private final int year;
  private final String scenarioName;
  private final int trialNumber;
  private final EngineNumber domesticValue;
  private final EngineNumber importValue;
  private final EngineNumber recycleValue;
  private final EngineNumber domesticConsumptionValue;
  private final EngineNumber importConsumptionValue;
  private final EngineNumber recycleConsumptionValue;
  private final EngineNumber populationValue;
  private final EngineNumber populationNew;
  private final EngineNumber rechargeEmissions;
  private final EngineNumber eolEmissions;
  private final EngineNumber initialChargeEmissions;
  private final EngineNumber energyConsumption;
  private final EngineNumber exportValue;
  private final EngineNumber exportConsumptionValue;
  private final TradeSupplement tradeSupplement;
  private final EngineNumber bankKg;
  private final EngineNumber bankTco2e;
  private final EngineNumber bankChangeKg;
  private final EngineNumber bankChangeTco2e;

  /**
   * Constructor for creating an EngineResult instance from a builder.
   *
   * <p>This constructor accepts an EngineResultBuilder instance and extracts all
   * configured field values from it to initialize this result.</p>
   *
   * @param builder The EngineResultBuilder instance with all required fields configured
   */
  public EngineResult(EngineResultBuilder builder) {
    application = builder.getApplication();
    substance = builder.getSubstance();
    year = builder.getYear();
    scenarioName = builder.getScenarioName();
    trialNumber = builder.getTrialNumber();
    domesticValue = builder.getDomesticValue();
    importValue = builder.getImportValue();
    recycleValue = builder.getRecycleValue();
    domesticConsumptionValue = builder.getDomesticConsumptionValue();
    importConsumptionValue = builder.getImportConsumptionValue();
    recycleConsumptionValue = builder.getRecycleConsumptionValue();
    populationValue = builder.getPopulationValue();
    populationNew = builder.getPopulationNew();
    rechargeEmissions = builder.getRechargeEmissions();
    eolEmissions = builder.getEolEmissions();
    initialChargeEmissions = builder.getInitialChargeEmissions();
    energyConsumption = builder.getEnergyConsumption();
    exportValue = builder.getExportValue();
    exportConsumptionValue = builder.getExportConsumptionValue();
    tradeSupplement = builder.getTradeSupplement();
    bankKg = builder.getBankKg();
    bankTco2e = builder.getBankTco2e();
    bankChangeKg = builder.getBankChangeKg();
    bankChangeTco2e = builder.getBankChangeTco2e();
  }

  /**
   * Get the application.
   *
   * @return The application
   */
  public String getApplication() {
    return application;
  }

  /**
   * Get the substance.
   *
   * @return The substance
   */
  public String getSubstance() {
    return substance;
  }

  /**
   * Get the year the result is relevant to.
   *
   * @return The year
   */
  public int getYear() {
    return year;
  }

  /**
   * Get the domestic value.
   *
   * @return The domestic value in volume like kg
   */
  public EngineNumber getDomestic() {
    return domesticValue;
  }

  /**
   * Get the import value.
   *
   * @return The import value in volume like kg
   */
  public EngineNumber getImport() {
    return importValue;
  }

  /**
   * Get the recycle value.
   *
   * @return The recycle value in volume like kg
   */
  public EngineNumber getRecycle() {
    return recycleValue;
  }

  /**
   * Get the total consumption.
   *
   * <p>By default, excludes recycling consumption to measure virgin material consumption.</p>
   *
   * @return The virgin material consumption value in tCO2e or similar
   */
  public EngineNumber getConsumption() {
    return getConsumption(false);
  }

  /**
   * Get the total consumption with optional recycling inclusion.
   *
   * @param includeRecycling If true, includes recycling consumption in the total
   * @return The consumption value in tCO2e or similar
   */
  public EngineNumber getConsumption(boolean includeRecycling) {
    String domesticUnits = domesticConsumptionValue.getUnits();
    String importUnits = importConsumptionValue.getUnits();

    if (!domesticUnits.equals(importUnits)) {
      throw new IllegalStateException(
          "Could not add incompatible units for consumption."
      );
    }

    BigDecimal domesticConsumptionRaw = domesticConsumptionValue.getValue();
    BigDecimal importConsumptionRaw = importConsumptionValue.getValue();
    BigDecimal totalValue = domesticConsumptionRaw.add(importConsumptionRaw);

    if (includeRecycling) {
      String recycleUnits = recycleConsumptionValue.getUnits();
      if (!domesticUnits.equals(recycleUnits)) {
        throw new IllegalStateException(
            "Could not add incompatible units for total consumption with recycling."
        );
      }
      totalValue = totalValue.add(recycleConsumptionValue.getValue());
    }

    return new EngineNumber(totalValue, domesticUnits);
  }

  /**
   * Get the domestic consumption value.
   *
   * @return The domestic consumption value in tCO2e or equivalent
   */
  public EngineNumber getDomesticConsumption() {
    return domesticConsumptionValue;
  }

  /**
   * Get the import consumption value.
   *
   * @return The import consumption value in tCO2e or equivalent
   */
  public EngineNumber getImportConsumption() {
    return importConsumptionValue;
  }

  /**
   * Get the recycle consumption value.
   *
   * @return The recycle consumption value in tCO2e or equivalent
   */
  public EngineNumber getRecycleConsumption() {
    return recycleConsumptionValue;
  }

  /**
   * Get the population value.
   *
   * @return The population value
   */
  public EngineNumber getPopulation() {
    return populationValue;
  }

  /**
   * Get the amount of new equipment added this year.
   *
   * @return The amount of new equipment this year in units
   */
  public EngineNumber getPopulationNew() {
    return populationNew;
  }

  /**
   * Get the greenhouse gas emissions from recharge activities.
   *
   * @return The recharge emissions value with units
   */
  public EngineNumber getRechargeEmissions() {
    return rechargeEmissions;
  }

  /**
   * Get the greenhouse gas emissions from end-of-life equipment.
   *
   * @return The end-of-life emissions value with units
   */
  public EngineNumber getEolEmissions() {
    return eolEmissions;
  }

  /**
   * Get the greenhouse gas emissions from initial charge activities.
   *
   * <p>This is an informational metric representing the GHG potential of substance
 * initially charged into equipment. Actual emissions occur later during recharge (leakage between
 * servicings) or at end-of-life disposal.</p>
   *
   * @return The initial charge emissions value with units
   */
  public EngineNumber getInitialChargeEmissions() {
    return initialChargeEmissions;
  }

  /**
   * Get the energy consumption value.
   *
   * @return The energy consumption value with units
   */
  public EngineNumber getEnergyConsumption() {
    return energyConsumption;
  }


  /**
   * Get the export value.
   *
   * @return The export value in volume like kg
   */
  public EngineNumber getExport() {
    return exportValue;
  }

  /**
   * Get the export consumption value.
   *
   * @return The export consumption value in tCO2e or equivalent
   */
  public EngineNumber getExportConsumption() {
    return exportConsumptionValue;
  }

  /**
   * Get the trade supplement data.
   *
   * @return The trade supplement containing attribution data
   */
  public TradeSupplement getTradeSupplement() {
    return tradeSupplement;
  }

  /**
   * Get the scenario name.
   *
   * @return The name of the scenario being run
   */
  public String getScenarioName() {
    return scenarioName;
  }

  /**
   * Get the trial number.
   *
   * @return The trial number of the current run
   */
  public int getTrialNumber() {
    return trialNumber;
  }

  /**
   * Get the bank kg value.
   *
   * @return The total substance volume in equipment bank in kg
   */
  public EngineNumber getBankKg() {
    return bankKg;
  }

  /**
   * Get the bank tCO2e value.
   *
   * @return The total GHG potential of substance in equipment bank in tCO2e
   */
  public EngineNumber getBankTco2e() {
    return bankTco2e;
  }

  /**
   * Get the bank change kg value.
   *
   * @return The change in substance bank from previous year in kg
   */
  public EngineNumber getBankChangeKg() {
    return bankChangeKg;
  }

  /**
   * Get the bank change tCO2e value.
   *
   * @return The change in GHG potential of substance bank from previous year in tCO2e
   */
  public EngineNumber getBankChangeTco2e() {
    return bankChangeTco2e;
  }
}
