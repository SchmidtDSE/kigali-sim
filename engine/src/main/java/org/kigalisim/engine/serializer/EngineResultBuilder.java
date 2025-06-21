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
  private Optional<EngineNumber> manufactureValue;
  private Optional<EngineNumber> importValue;
  private Optional<EngineNumber> recycleValue;
  private Optional<EngineNumber> domesticConsumptionValue;
  private Optional<EngineNumber> importConsumptionValue;
  private Optional<EngineNumber> recycleConsumptionValue;
  private Optional<EngineNumber> populationValue;
  private Optional<EngineNumber> populationNew;
  private Optional<EngineNumber> rechargeEmissions;
  private Optional<EngineNumber> eolEmissions;
  private Optional<EngineNumber> energyConsumption;
  private Optional<ImportSupplement> importSupplement;

  /**
   * Create builder without any values initialized.
   */
  public EngineResultBuilder() {
    application = Optional.empty();
    substance = Optional.empty();
    year = Optional.empty();
    manufactureValue = Optional.empty();
    importValue = Optional.empty();
    recycleValue = Optional.empty();
    domesticConsumptionValue = Optional.empty();
    importConsumptionValue = Optional.empty();
    recycleConsumptionValue = Optional.empty();
    populationValue = Optional.empty();
    populationNew = Optional.empty();
    rechargeEmissions = Optional.empty();
    eolEmissions = Optional.empty();
    energyConsumption = Optional.empty();
    importSupplement = Optional.empty();
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
   * Set the manufacture value.
   *
   * @param manufactureValue The value associated with manufacturing in volume like kg
   * @return This builder for method chaining
   */
  public EngineResultBuilder setManufactureValue(EngineNumber manufactureValue) {
    this.manufactureValue = Optional.of(manufactureValue);
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
   * Set the import supplement data.
   *
   * @param importSupplement Supplemental import information
   * @return This builder for method chaining
   */
  public EngineResultBuilder setImportSupplement(ImportSupplement importSupplement) {
    this.importSupplement = Optional.of(importSupplement);
    return this;
  }

  /**
   * Check that the builder is complete and create a new result.
   *
   * @return The result built from the values provided to this builder
   * @throws IllegalStateException if any required field is missing
   */
  public EngineResult build() {
    checkReadyToConstruct();
    return new EngineResult(
        application.get(),
        substance.get(),
        year.get(),
        manufactureValue.get(),
        importValue.get(),
        recycleValue.get(),
        domesticConsumptionValue.get(),
        importConsumptionValue.get(),
        recycleConsumptionValue.get(),
        populationValue.get(),
        populationNew.get(),
        rechargeEmissions.get(),
        eolEmissions.get(),
        energyConsumption.get(),
        importSupplement.get()
    );
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
    checkValid(manufactureValue, "manufactureValue");
    checkValid(importValue, "importValue");
    checkValid(recycleValue, "recycleValue");
    checkValid(domesticConsumptionValue, "domesticConsumptionValue");
    checkValid(importConsumptionValue, "importConsumptionValue");
    checkValid(recycleConsumptionValue, "recycleConsumptionValue");
    checkValid(populationValue, "populationValue");
    checkValid(populationNew, "populationNew");
    checkValid(rechargeEmissions, "rechargeEmissions");
    checkValid(eolEmissions, "eolEmissions");
    checkValid(energyConsumption, "energyConsumption");
    checkValid(importSupplement, "importSupplement");
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
