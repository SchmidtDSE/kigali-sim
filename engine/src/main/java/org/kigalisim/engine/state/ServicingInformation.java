/**
 * Immutable structure for servicing (recharge/precharge) population and intensity information.
 *
 * <p>Represents servicing parameters with weighted-average intensity calculation
 * for accumulating multiple servicing operations within a single timestep.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import java.math.MathContext;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Immutable structure holding servicing (recharge/precharge) population and intensity information.
 *
 * <p>Provides methods to accumulate servicing parameters with weighted-average
 * intensity calculations. This allows multiple servicing operations to be combined while preserving
 * intensity information through weighted averaging.</p>
 */
public class ServicingInformation {

  private final EngineNumber population;
  private final EngineNumber intensity;

  /**
   * Create a new ServicingInformation instance.
   *
   * @param population The servicing population rate (in %)
   * @param intensity The servicing intensity (in kg / unit)
   */
  public ServicingInformation(EngineNumber population, EngineNumber intensity) {
    this.population = population;
    this.intensity = intensity;
  }

  /**
   * Get the servicing population rate.
   *
   * @return The population rate in %
   */
  public EngineNumber getPopulation() {
    return population;
  }

  /**
   * Get the servicing intensity.
   *
   * @return The intensity in kg / unit
   */
  public EngineNumber getIntensity() {
    return intensity;
  }

  /**
   * Add population and intensity to this servicing information with weighted-average intensity.
   *
   * <p>Multiple calls accumulate rates (addition) and intensities (weighted-average).
   * Population rates are added, intensities are weighted-averaged using absolute values for weights
   * to handle negative adjustments correctly.</p>
   *
   * <p>Weighted average formula: (|rate1| × intensity1 + |rate2| × intensity2) / (|rate1| + |rate2|)</p>
   *
   * @param newPopulation The servicing population rate to add
   * @param newIntensity The servicing intensity for this rate
   * @return A new ServicingInformation instance with accumulated values
   */
  public ServicingInformation add(EngineNumber newPopulation, EngineNumber newIntensity) {
    boolean priorZero = population.getValue().equals(BigDecimal.ZERO);
    if (priorZero) {
      return new ServicingInformation(newPopulation, newIntensity);
    }

    boolean differentPopulationUnits = !population.getUnits().equals(newPopulation.getUnits());
    if (differentPopulationUnits) {
      throw new RuntimeException("Cannot mix units for servicing.");
    }

    boolean differentIntensityUnits = !intensity.getUnits().equals(newIntensity.getUnits());
    if (differentIntensityUnits) {
      throw new RuntimeException("Cannot mix units for servicing.");
    }

    BigDecimal currentWeight = population.getValue().abs();
    BigDecimal addedWeight = newPopulation.getValue().abs();

    BigDecimal weightedIntensity = calculateWeightedIntensity(
        currentWeight,
        addedWeight,
        newIntensity
    );

    // Accumulate population rates (can be negative)
    BigDecimal accumulatedPopulation = population.getValue().add(newPopulation.getValue());

    return new ServicingInformation(
        new EngineNumber(accumulatedPopulation, newPopulation.getUnits()),
        new EngineNumber(weightedIntensity, newIntensity.getUnits())
    );
  }

  /**
   * Calculate the weighted-average intensity for servicing operations.
   *
   * <p>Use absolute values for weights to handle negative values correctly.
   * For the first servicing command or no population, just use new intensity directly.
   * For multiple servicing commands, compute weighted average: (w1*i1 + w2*i2) / (w1 + w2)</p>
   *
   * @param currentWeight The absolute value of the current population
   * @param addedWeight The absolute value of the new population
   * @param newIntensity The servicing intensity for this rate
   * @return The calculated weighted-average intensity
   */
  private BigDecimal calculateWeightedIntensity(
      BigDecimal currentWeight,
      BigDecimal addedWeight,
      EngineNumber newIntensity
  ) {
    boolean noPriorPopulation = currentWeight.compareTo(BigDecimal.ZERO) == 0;

    if (noPriorPopulation) {
      return newIntensity.getValue();
    } else {
      BigDecimal totalWeight = currentWeight.add(addedWeight);
      return currentWeight.multiply(intensity.getValue())
          .add(addedWeight.multiply(newIntensity.getValue()))
          .divide(totalWeight, MathContext.DECIMAL128);
    }
  }
}
