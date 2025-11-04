/**
 * Builder to help construct an EngineResult.
 *
 * <p>This class provides a builder pattern for constructing EngineResult instances,
 * ensuring all required fields are set before creating the result.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.serializer;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Builder pattern implementation for creating EngineResult objects.
 *
 * <p>This builder ensures that all required fields are provided before
 * constructing an EngineResult instance.</p>
 */
public class EngineResultBuilder {
  private Optional<String> application;
  private Optional<String> substance;
  private Optional<Integer> year;
  private Optional<String> scenarioName;
  private Optional<Integer> trialNumber;
  private Optional<EngineNumber> domesticValue;
  private Optional<EngineNumber> importValue;
  private Optional<EngineNumber> recycleValue;
  private Optional<EngineNumber> domesticConsumptionValue;
  private Optional<EngineNumber> importConsumptionValue;
  private Optional<EngineNumber> recycleConsumptionValue;
  private Optional<EngineNumber> populationValue;
  private Optional<EngineNumber> populationNew;
  private Optional<EngineNumber> rechargeEmissions;
  private Optional<EngineNumber> eolEmissions;
  private Optional<EngineNumber> initialChargeEmissions;
  private Optional<EngineNumber> energyConsumption;
  private Optional<EngineNumber> exportValue;
  private Optional<EngineNumber> exportConsumptionValue;
  private Optional<TradeSupplement> tradeSupplement;
  private Optional<EngineNumber> bankKg;
  private Optional<EngineNumber> bankTco2e;
  private Optional<EngineNumber> bankChangeKg;
  private Optional<EngineNumber> bankChangeTco2e;

  /**
   * Create builder without any values initialized.
   */
  public EngineResultBuilder() {
    application = Optional.empty();
    substance = Optional.empty();
    year = Optional.empty();
    scenarioName = Optional.empty();
    trialNumber = Optional.empty();
    domesticValue = Optional.empty();
    importValue = Optional.empty();
    recycleValue = Optional.empty();
    domesticConsumptionValue = Optional.empty();
    importConsumptionValue = Optional.empty();
    recycleConsumptionValue = Optional.empty();
    populationValue = Optional.empty();
    populationNew = Optional.empty();
    rechargeEmissions = Optional.empty();
    eolEmissions = Optional.empty();
    initialChargeEmissions = Optional.empty();
    energyConsumption = Optional.empty();
    exportValue = Optional.empty();
    exportConsumptionValue = Optional.empty();
    tradeSupplement = Optional.empty();
    bankKg = Optional.empty();
    bankTco2e = Optional.empty();
    bankChangeKg = Optional.empty();
    bankChangeTco2e = Optional.empty();
  }

  /**
   * Set the application for which a result is being given.
   *
   * @param application The application name like "commercialRefrigeration"
   * @return This builder for method chaining
   */
  public EngineResultBuilder setApplication(String application) {
    this.application = Optional.of(application);
    return this;
  }

  /**
   * Set the substance for which a result is being given.
   *
   * @param substance The substance name like "HFC-134a"
   * @return This builder for method chaining
   */
  public EngineResultBuilder setSubstance(String substance) {
    this.substance = Optional.of(substance);
    return this;
  }

  /**
   * Set the year for which a result is being given.
   *
   * @param year The year for which the result is relevant
   * @return This builder for method chaining
   */
  public EngineResultBuilder setYear(int year) {
    this.year = Optional.of(year);
    return this;
  }

  /**
   * Set the scenario name for which a result is being given.
   *
   * @param scenarioName The name of the scenario being run
   * @return This builder for method chaining
   */
  public EngineResultBuilder setScenarioName(String scenarioName) {
    this.scenarioName = Optional.of(scenarioName);
    return this;
  }

  /**
   * Set the trial number for which a result is being given.
   *
   * @param trialNumber The trial number of the current run
   * @return This builder for method chaining
   */
  public EngineResultBuilder setTrialNumber(int trialNumber) {
    this.trialNumber = Optional.of(trialNumber);
    return this;
  }

  /**
   * Set the domestic value.
   *
   * @param domesticValue The value associated with domestic production in volume like kg
   * @return This builder for method chaining
   */
  public EngineResultBuilder setDomesticValue(EngineNumber domesticValue) {
    this.domesticValue = Optional.of(domesticValue);
    return this;
  }

  /**
   * Set the import value.
   *
   * @param importValue The value related to imports like in volume like kg
   * @return This builder for method chaining
   */
  public EngineResultBuilder setImportValue(EngineNumber importValue) {
    this.importValue = Optional.of(importValue);
    return this;
  }

  /**
   * Set the recycle value.
   *
   * @param recycleValue The value denoting recycled materials in volume like kg
   * @return This builder for method chaining
   */
  public EngineResultBuilder setRecycleValue(EngineNumber recycleValue) {
    this.recycleValue = Optional.of(recycleValue);
    return this;
  }

  /**
   * Set the domestic consumption value.
   *
   * @param domesticConsumptionValue The domestic consumption value in tCO2e or equivalent
   * @return This builder for method chaining
   */
  public EngineResultBuilder setDomesticConsumptionValue(EngineNumber domesticConsumptionValue) {
    this.domesticConsumptionValue = Optional.of(domesticConsumptionValue);
    return this;
  }

  /**
   * Set the import consumption value.
   *
   * @param importConsumptionValue The import consumption value in tCO2e or equivalent
   * @return This builder for method chaining
   */
  public EngineResultBuilder setImportConsumptionValue(EngineNumber importConsumptionValue) {
    this.importConsumptionValue = Optional.of(importConsumptionValue);
    return this;
  }

  /**
   * Set the recycle consumption value.
   *
   * @param recycleConsumptionValue The recycle consumption value in tCO2e
   *     or equivalent
   * @return This builder for method chaining
   */
  public EngineResultBuilder setRecycleConsumptionValue(EngineNumber recycleConsumptionValue) {
    this.recycleConsumptionValue = Optional.of(recycleConsumptionValue);
    return this;
  }

  /**
   * Set the population value.
   *
   * @param populationValue The population value in terms of equipment
   * @return This builder for method chaining
   */
  public EngineResultBuilder setPopulationValue(EngineNumber populationValue) {
    this.populationValue = Optional.of(populationValue);
    return this;
  }

  /**
   * Set the population new value.
   *
   * @param populationNew The amount of new equipment added this year
   * @return This builder for method chaining
   */
  public EngineResultBuilder setPopulationNew(EngineNumber populationNew) {
    this.populationNew = Optional.of(populationNew);
    return this;
  }

  /**
   * Set the recharge emissions value.
   *
   * @param rechargeEmissions The greenhouse gas emissions from recharge activities
   * @return This builder for method chaining
   */
  public EngineResultBuilder setRechargeEmissions(EngineNumber rechargeEmissions) {
    this.rechargeEmissions = Optional.of(rechargeEmissions);
    return this;
  }

  /**
   * Set the end-of-life emissions value.
   *
   * @param eolEmissions The greenhouse gas emissions from end-of-life equipment
   * @return This builder for method chaining
   */
  public EngineResultBuilder setEolEmissions(EngineNumber eolEmissions) {
    this.eolEmissions = Optional.of(eolEmissions);
    return this;
  }

  /**
   * Set the initial charge emissions value.
   *
   * <p>This is an informational metric representing the GHG potential of substance
   * initially charged into equipment. Actual emissions occur later during recharge (leakage between
   * servicings) or at end-of-life disposal.</p>
   *
   * @param initialChargeEmissions The greenhouse gas emissions from initial charge activities
   * @return This builder for method chaining
   */
  public EngineResultBuilder setInitialChargeEmissions(EngineNumber initialChargeEmissions) {
    this.initialChargeEmissions = Optional.of(initialChargeEmissions);
    return this;
  }

  /**
   * Set the energy consumption value.
   *
   * @param energyConsumption The energy consumption value
   * @return This builder for method chaining
   */
  public EngineResultBuilder setEnergyConsumption(EngineNumber energyConsumption) {
    this.energyConsumption = Optional.ofNullable(energyConsumption);
    return this;
  }

  /**
   * Set the export value.
   *
   * @param exportValue The export value in volume like kg
   * @return This builder for method chaining
   */
  public EngineResultBuilder setExportValue(EngineNumber exportValue) {
    this.exportValue = Optional.of(exportValue);
    return this;
  }

  /**
   * Set the export consumption value.
   *
   * @param exportConsumptionValue The export consumption value in tCO2e or equivalent
   * @return This builder for method chaining
   */
  public EngineResultBuilder setExportConsumptionValue(EngineNumber exportConsumptionValue) {
    this.exportConsumptionValue = Optional.of(exportConsumptionValue);
    return this;
  }

  /**
   * Set the trade supplement data.
   *
   * @param tradeSupplement Supplemental trade information
   * @return This builder for method chaining
   */
  public EngineResultBuilder setTradeSupplement(TradeSupplement tradeSupplement) {
    this.tradeSupplement = Optional.of(tradeSupplement);
    return this;
  }

  /**
   * Set the bank kg value.
   *
   * @param bankKg The total substance volume in equipment bank in kg
   * @return This builder for method chaining
   */
  public EngineResultBuilder setBankKg(EngineNumber bankKg) {
    this.bankKg = Optional.of(bankKg);
    return this;
  }

  /**
   * Set the bank tCO2e value.
   *
   * @param bankTco2e The total GHG potential of substance in equipment bank in tCO2e
   * @return This builder for method chaining
   */
  public EngineResultBuilder setBankTco2e(EngineNumber bankTco2e) {
    this.bankTco2e = Optional.of(bankTco2e);
    return this;
  }

  /**
   * Set the bank change kg value.
   *
   * @param bankChangeKg The change in substance bank from previous year in kg
   * @return This builder for method chaining
   */
  public EngineResultBuilder setBankChangeKg(EngineNumber bankChangeKg) {
    this.bankChangeKg = Optional.of(bankChangeKg);
    return this;
  }

  /**
   * Set the bank change tCO2e value.
   *
   * @param bankChangeTco2e The change in GHG potential of substance bank from previous year in tCO2e
   * @return This builder for method chaining
   */
  public EngineResultBuilder setBankChangeTco2e(EngineNumber bankChangeTco2e) {
    this.bankChangeTco2e = Optional.of(bankChangeTco2e);
    return this;
  }

  /**
   * Get the application value.
   *
   * @return The application value
   */
  public String getApplication() {
    return application.get();
  }

  /**
   * Get the substance value.
   *
   * @return The substance value
   */
  public String getSubstance() {
    return substance.get();
  }

  /**
   * Get the year value.
   *
   * @return The year value
   */
  public int getYear() {
    return year.get();
  }

  /**
   * Get the scenario name value.
   *
   * @return The scenario name value
   */
  public String getScenarioName() {
    return scenarioName.get();
  }

  /**
   * Get the trial number value.
   *
   * @return The trial number value
   */
  public int getTrialNumber() {
    return trialNumber.get();
  }

  /**
   * Get the domestic value.
   *
   * @return The domestic value
   */
  public EngineNumber getDomesticValue() {
    return domesticValue.get();
  }

  /**
   * Get the import value.
   *
   * @return The import value
   */
  public EngineNumber getImportValue() {
    return importValue.get();
  }

  /**
   * Get the recycle value.
   *
   * @return The recycle value
   */
  public EngineNumber getRecycleValue() {
    return recycleValue.get();
  }

  /**
   * Get the domestic consumption value.
   *
   * @return The domestic consumption value
   */
  public EngineNumber getDomesticConsumptionValue() {
    return domesticConsumptionValue.get();
  }

  /**
   * Get the import consumption value.
   *
   * @return The import consumption value
   */
  public EngineNumber getImportConsumptionValue() {
    return importConsumptionValue.get();
  }

  /**
   * Get the recycle consumption value.
   *
   * @return The recycle consumption value
   */
  public EngineNumber getRecycleConsumptionValue() {
    return recycleConsumptionValue.get();
  }

  /**
   * Get the population value.
   *
   * @return The population value
   */
  public EngineNumber getPopulationValue() {
    return populationValue.get();
  }

  /**
   * Get the population new value.
   *
   * @return The population new value
   */
  public EngineNumber getPopulationNew() {
    return populationNew.get();
  }

  /**
   * Get the recharge emissions value.
   *
   * @return The recharge emissions value
   */
  public EngineNumber getRechargeEmissions() {
    return rechargeEmissions.get();
  }

  /**
   * Get the end-of-life emissions value.
   *
   * @return The end-of-life emissions value
   */
  public EngineNumber getEolEmissions() {
    return eolEmissions.get();
  }

  /**
   * Get the initial charge emissions value.
   *
   * @return The initial charge emissions value
   */
  public EngineNumber getInitialChargeEmissions() {
    return initialChargeEmissions.get();
  }

  /**
   * Get the energy consumption value.
   *
   * @return The energy consumption value
   */
  public EngineNumber getEnergyConsumption() {
    return energyConsumption.get();
  }

  /**
   * Get the export value.
   *
   * @return The export value
   */
  public EngineNumber getExportValue() {
    return exportValue.get();
  }

  /**
   * Get the export consumption value.
   *
   * @return The export consumption value
   */
  public EngineNumber getExportConsumptionValue() {
    return exportConsumptionValue.get();
  }

  /**
   * Get the trade supplement data.
   *
   * @return The trade supplement data
   */
  public TradeSupplement getTradeSupplement() {
    return tradeSupplement.get();
  }

  /**
   * Get the bank kg value.
   *
   * @return The bank kg value
   */
  public EngineNumber getBankKg() {
    return bankKg.get();
  }

  /**
   * Get the bank tCO2e value.
   *
   * @return The bank tCO2e value
   */
  public EngineNumber getBankTco2e() {
    return bankTco2e.get();
  }

  /**
   * Get the bank change kg value.
   *
   * @return The bank change kg value
   */
  public EngineNumber getBankChangeKg() {
    return bankChangeKg.get();
  }

  /**
   * Get the bank change tCO2e value.
   *
   * @return The bank change tCO2e value
   */
  public EngineNumber getBankChangeTco2e() {
    return bankChangeTco2e.get();
  }

  /**
   * Check that the builder is complete and create a new result.
   *
   * @return The result built from the values provided to this builder
   * @throws IllegalStateException if any required field is missing
   */
  public EngineResult build() {
    checkReadyToConstruct();
    return new EngineResult(this);
  }

  /**
   * Check that all required fields are set before construction.
   *
   * @throws IllegalStateException if any required field is missing
   */
  private void checkReadyToConstruct() {
    checkValid(application, "application");
    checkValid(substance, "substance");
    checkValid(year, "year");
    checkValid(scenarioName, "scenarioName");
    checkValid(trialNumber, "trialNumber");
    checkValid(domesticValue, "domesticValue");
    checkValid(importValue, "importValue");
    checkValid(recycleValue, "recycleValue");
    checkValid(domesticConsumptionValue, "domesticConsumptionValue");
    checkValid(importConsumptionValue, "importConsumptionValue");
    checkValid(recycleConsumptionValue, "recycleConsumptionValue");
    checkValid(populationValue, "populationValue");
    checkValid(populationNew, "populationNew");
    checkValid(rechargeEmissions, "rechargeEmissions");
    checkValid(eolEmissions, "eolEmissions");
    checkValid(initialChargeEmissions, "initialChargeEmissions");
    checkValid(energyConsumption, "energyConsumption");
    checkValid(exportValue, "exportValue");
    checkValid(exportConsumptionValue, "exportConsumptionValue");
    checkValid(tradeSupplement, "tradeSupplement");
    checkValid(bankKg, "bankKg");
    checkValid(bankTco2e, "bankTco2e");
    checkValid(bankChangeKg, "bankChangeKg");
    checkValid(bankChangeTco2e, "bankChangeTco2e");
  }

  /**
   * Check if a value is valid (not empty).
   *
   * @param value The optional value to check
   * @param name The name of the field for error reporting
   * @throws IllegalStateException if the value is empty
   */
  private void checkValid(Optional<?> value, String name) {
    if (value.isEmpty()) {
      throw new IllegalStateException(
          "Could not make engine result because " + name + " was not given.");
    }
  }
}
