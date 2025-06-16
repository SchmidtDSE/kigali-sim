/**
 * State getter that allows overriding values from an inner state getter.
 *
 * <p>This class wraps an inner StateGetter and allows individual values to be
 * overridden while delegating to the inner getter for non-overridden values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;

/**
 * State getter that allows overriding values from an inner state getter.
 *
 * <p>This class wraps an inner StateGetter and allows individual values to be
 * overridden while delegating to the inner getter for non-overridden values.</p>
 */
public class OverridingConverterStateGetter implements StateGetter {

  private final StateGetter innerGetter;
  private Optional<EngineNumber> energyIntensity;
  private Optional<EngineNumber> amortizedUnitVolume;
  private Optional<EngineNumber> population;
  private Optional<EngineNumber> yearsElapsed;
  private Optional<EngineNumber> totalConsumption;
  private Optional<EngineNumber> energyConsumption;
  private Optional<EngineNumber> volume;
  private Optional<EngineNumber> amortizedUnitConsumption;
  private Optional<EngineNumber> populationChange;
  private Optional<EngineNumber> substanceConsumption;

  /**
   * Create a new overriding converter state getter.
   *
   * @param innerGetter The base state getter to wrap
   */
  public OverridingConverterStateGetter(StateGetter innerGetter) {
    this.innerGetter = innerGetter;
    this.energyIntensity = Optional.empty();
    this.amortizedUnitVolume = Optional.empty();
    this.population = Optional.empty();
    this.yearsElapsed = Optional.empty();
    this.totalConsumption = Optional.empty();
    this.energyConsumption = Optional.empty();
    this.volume = Optional.empty();
    this.amortizedUnitConsumption = Optional.empty();
    this.populationChange = Optional.empty();
    this.substanceConsumption = Optional.empty();
  }

  /**
   * Set total values for different stream types.
   *
   * @param streamName The name of the stream (sales, manufacture, import, etc.)
   * @param value The value to set for the stream
   */
  public void setTotal(String streamName, EngineNumber value) {
    switch (streamName) {
      case "sales", "manufacture", "import" -> setVolume(value);
      case "equipment", "priorEquipment" -> setPopulation(value);
      case "consumption" -> setConsumption(value);
      default -> throw new IllegalArgumentException("Unrecognized stream name: " + streamName);
    }
  }

  /**
   * Set the substance consumption value.
   *
   * @param newValue The new substance consumption value
   */
  public void setSubstanceConsumption(EngineNumber newValue) {
    this.substanceConsumption = Optional.of(newValue);
  }

  /**
   * Get the substance consumption value.
   *
   * @return The substance consumption value
   */
  @Override
  public EngineNumber getSubstanceConsumption() {
    if (substanceConsumption.isPresent()) {
      return substanceConsumption.get();
    } else {
      return innerGetter.getSubstanceConsumption();
    }
  }

  /**
   * Set the energy intensity value.
   *
   * @param newValue The new energy intensity value
   */
  public void setEnergyIntensity(EngineNumber newValue) {
    this.energyIntensity = Optional.of(newValue);
  }

  /**
   * Get the energy intensity value.
   *
   * @return The energy intensity value
   */
  @Override
  public EngineNumber getEnergyIntensity() {
    if (energyIntensity.isPresent()) {
      return energyIntensity.get();
    } else {
      return innerGetter.getEnergyIntensity();
    }
  }

  /**
   * Set the amortized unit volume value.
   *
   * @param newValue The new amortized unit volume value
   */
  public void setAmortizedUnitVolume(EngineNumber newValue) {
    this.amortizedUnitVolume = Optional.of(newValue);
  }

  /**
   * Get the amortized unit volume value.
   *
   * @return The amortized unit volume value
   */
  @Override
  public EngineNumber getAmortizedUnitVolume() {
    if (amortizedUnitVolume.isPresent()) {
      return amortizedUnitVolume.get();
    } else {
      return innerGetter.getAmortizedUnitVolume();
    }
  }

  /**
   * Set the population value.
   *
   * @param newValue The new population value
   */
  public void setPopulation(EngineNumber newValue) {
    this.population = Optional.of(newValue);
  }

  /**
   * Get the population value.
   *
   * @return The population value
   */
  @Override
  public EngineNumber getPopulation() {
    if (population.isPresent()) {
      return population.get();
    } else {
      return innerGetter.getPopulation();
    }
  }

  /**
   * Set the years elapsed value.
   *
   * @param newValue The new years elapsed value
   */
  public void setYearsElapsed(EngineNumber newValue) {
    this.yearsElapsed = Optional.of(newValue);
  }

  /**
   * Get the years elapsed value.
   *
   * @return The years elapsed value
   */
  @Override
  public EngineNumber getYearsElapsed() {
    if (yearsElapsed.isPresent()) {
      return yearsElapsed.get();
    } else {
      return innerGetter.getYearsElapsed();
    }
  }

  /**
   * Set the consumption value.
   *
   * @param newValue The new consumption value
   */
  public void setConsumption(EngineNumber newValue) {
    this.totalConsumption = Optional.of(newValue);
  }

  /**
   * Set the energy consumption equivalency.
   *
   * @param newValue The new energy consumption value
   */
  public void setEnergyConsumption(EngineNumber newValue) {
    this.energyConsumption = Optional.of(newValue);
  }

  /**
   * Get the GHG consumption value.
   *
   * @return The GHG consumption value
   */
  @Override
  public EngineNumber getGhgConsumption() {
    if (totalConsumption.isPresent()) {
      return totalConsumption.get();
    } else {
      return innerGetter.getGhgConsumption();
    }
  }

  /**
   * Get the energy consumption value.
   *
   * @return The energy consumption value
   */
  @Override
  public EngineNumber getEnergyConsumption() {
    if (energyConsumption.isPresent()) {
      return energyConsumption.get();
    } else {
      return innerGetter.getEnergyConsumption();
    }
  }

  /**
   * Set the volume value.
   *
   * @param newValue The new volume value
   */
  public void setVolume(EngineNumber newValue) {
    this.volume = Optional.of(newValue);
  }

  /**
   * Get the volume value.
   *
   * @return The volume value
   */
  @Override
  public EngineNumber getVolume() {
    if (volume.isPresent()) {
      return volume.get();
    } else {
      return innerGetter.getVolume();
    }
  }

  /**
   * Set the amortized unit consumption value.
   *
   * @param newValue The new amortized unit consumption value
   */
  public void setAmortizedUnitConsumption(EngineNumber newValue) {
    this.amortizedUnitConsumption = Optional.of(newValue);
  }

  /**
   * Get the amortized unit consumption value.
   *
   * @return The amortized unit consumption value
   */
  @Override
  public EngineNumber getAmortizedUnitConsumption() {
    if (amortizedUnitConsumption.isPresent()) {
      return amortizedUnitConsumption.get();
    } else {
      return innerGetter.getAmortizedUnitConsumption();
    }
  }

  /**
   * Set the population change value.
   *
   * @param newValue The new population change value
   */
  public void setPopulationChange(EngineNumber newValue) {
    this.populationChange = Optional.of(newValue);
  }

  /**
   * Get the population change value.
   *
   * @param unitConverter Converter for ensuring consistent units
   * @return The population change value
   */
  @Override
  public EngineNumber getPopulationChange(UnitConverter unitConverter) {
    if (populationChange.isPresent()) {
      return populationChange.get();
    } else {
      return innerGetter.getPopulationChange(unitConverter);
    }
  }
}
