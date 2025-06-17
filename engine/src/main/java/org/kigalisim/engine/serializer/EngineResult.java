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
  private final EngineNumber manufactureValue;
  private final EngineNumber importValue;
  private final EngineNumber recycleValue;
  private final EngineNumber domesticConsumptionValue;
  private final EngineNumber importConsumptionValue;
  private final EngineNumber recycleConsumptionValue;
  private final EngineNumber populationValue;
  private final EngineNumber populationNew;
  private final EngineNumber rechargeEmissions;
  private final EngineNumber eolEmissions;
  private final EngineNumber energyConsumption;
  private final ImportSupplement importSupplement;

  /**
   * Constructor for creating an EngineResult instance.
   *
   * @param application The application associated with this engine result
   * @param substance The substance associated with this engine result
   * @param year The year for which the engine result is relevant
   * @param manufactureValue The value associated with manufacturing in volume like kg
   * @param importValue The value related to imports like in volume like kg
   * @param recycleValue The value denoting recycled materials in volume like kg
   * @param domesticConsumptionValue The domestic consumption value in tCO2e or equivalent
   * @param importConsumptionValue The import consumption value in tCO2e or equivalent
   * @param recycleConsumptionValue The recycle consumption value in tCO2e or equivalent
   * @param populationValue The population value in terms of equipment
   * @param populationNew The amount of new equipment added this year
   * @param rechargeEmissions The greenhouse gas emissions from recharge activities
   * @param eolEmissions The greenhouse gas emissions from end-of-life equipment
   * @param energyConsumption The energy consumption value
   * @param importSupplement The supplemental import data needed for attribution
   */
  public EngineResult(String application, String substance, int year,
                     EngineNumber manufactureValue, EngineNumber importValue,
                     EngineNumber recycleValue, EngineNumber domesticConsumptionValue,
                     EngineNumber importConsumptionValue, EngineNumber recycleConsumptionValue,
                     EngineNumber populationValue, EngineNumber populationNew,
                     EngineNumber rechargeEmissions, EngineNumber eolEmissions,
                     EngineNumber energyConsumption, ImportSupplement importSupplement) {
    this.application = application;
    this.substance = substance;
    this.year = year;
    this.manufactureValue = manufactureValue;
    this.importValue = importValue;
    this.recycleValue = recycleValue;
    this.domesticConsumptionValue = domesticConsumptionValue;
    this.importConsumptionValue = importConsumptionValue;
    this.recycleConsumptionValue = recycleConsumptionValue;
    this.populationValue = populationValue;
    this.populationNew = populationNew;
    this.rechargeEmissions = rechargeEmissions;
    this.eolEmissions = eolEmissions;
    this.energyConsumption = energyConsumption;
    this.importSupplement = importSupplement;
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
   * Get the manufacture value.
   *
   * @return The manufacture value in volume like kg
   */
  public EngineNumber getManufacture() {
    return manufactureValue;
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
   * Get the total consumption without recycling.
   *
   * @return The consumption value in tCO2e or similar
   */
  public EngineNumber getConsumptionNoRecycle() {
    String domesticUnits = domesticConsumptionValue.getUnits();
    String importUnits = importConsumptionValue.getUnits();
    
    if (!domesticUnits.equals(importUnits)) {
      throw new IllegalStateException(
          "Could not add incompatible units for consumption.");
    }

    BigDecimal domesticConsumptionRaw = domesticConsumptionValue.getValue();
    BigDecimal importConsumptionRaw = importConsumptionValue.getValue();
    BigDecimal totalValue = domesticConsumptionRaw.add(importConsumptionRaw);
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
   * Get the energy consumption value.
   *
   * @return The energy consumption value with units
   */
  public EngineNumber getEnergyConsumption() {
    return energyConsumption;
  }

  /**
   * Get the import supplement data.
   *
   * @return The import supplement containing attribution data
   */
  public ImportSupplement getImportSupplement() {
    return importSupplement;
  }
}
