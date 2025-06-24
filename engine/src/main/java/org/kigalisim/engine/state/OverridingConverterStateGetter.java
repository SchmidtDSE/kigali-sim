/**
 * State getter that allows overriding values from an inner state getter.
 *
 * <p>This class wraps an inner StateGetter and allows individual values to be
 * overridden while delegating to the inner getter for non-overridden values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

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
    energyIntensity = Optional.empty();
    amortizedUnitVolume = Optional.empty();
    population = Optional.empty();
    yearsElapsed = Optional.empty();
    totalConsumption = Optional.empty();
    energyConsumption = Optional.empty();
    volume = Optional.empty();
    amortizedUnitConsumption = Optional.empty();
    populationChange = Optional.empty();
    substanceConsumption = Optional.empty();
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
    substanceConsumption = Optional.of(newValue);
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
    energyIntensity = Optional.of(newValue);
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
    amortizedUnitVolume = Optional.of(newValue);
  }

  /**
   * Clear the amortized unit volume override to use the inner getter's value.
   */
  public void clearAmortizedUnitVolume() {
    amortizedUnitVolume = Optional.empty();
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
    population = Optional.of(newValue);
  }

  /**
   * Clear the population override to use the inner getter's value.
   */
  public void clearPopulation() {
    population = Optional.empty();
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
    yearsElapsed = Optional.of(newValue);
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
    totalConsumption = Optional.of(newValue);
  }

  /**
   * Set the energy consumption equivalency.
   *
   * @param newValue The new energy consumption value
   */
  public void setEnergyConsumption(EngineNumber newValue) {
    energyConsumption = Optional.of(newValue);
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
    volume = Optional.of(newValue);
  }

  /**
   * Clear the volume override to use the inner getter's value.
   */
  public void clearVolume() {
    volume = Optional.empty();
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
    amortizedUnitConsumption = Optional.of(newValue);
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
    populationChange = Optional.of(newValue);
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
