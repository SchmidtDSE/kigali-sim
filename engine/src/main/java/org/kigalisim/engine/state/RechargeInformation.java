/**
 * Immutable structure for recharge population and intensity information.
 *
 * <p>Represents recharge parameters with weighted-average intensity calculation
 * for accumulating multiple recharge operations within a single timestep.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Immutable structure holding recharge population and intensity information.
 *
 * <p>Provides methods to accumulate recharge parameters with weighted-average
 * intensity calculations. This allows multiple recharge operations to be combined while preserving
 * intensity information through weighted averaging.</p>
 */
public class RechargeInformation {

  private final EngineNumber population;
  private final EngineNumber intensity;

  /**
   * Create a new RechargeInformation instance.
   *
   * @param population The recharge population rate (in %)
   * @param intensity The recharge intensity (in kg / unit)
   */
  public RechargeInformation(EngineNumber population, EngineNumber intensity) {
    this.population = population;
    this.intensity = intensity;
  }

  /**
   * Get the recharge population rate.
   *
   * @return The population rate in %
   */
  public EngineNumber getPopulation() {
    return population;
  }

  /**
   * Get the recharge intensity.
   *
   * @return The intensity in kg / unit
   */
  public EngineNumber getIntensity() {
    return intensity;
  }

  /**
   * Add population and intensity to this recharge information with weighted-average intensity.
   *
   * <p>Multiple calls accumulate rates (addition) and intensities (weighted-average).
 * Population rates are added, intensities are weighted-averaged using absolute values for weights
 * to handle negative adjustments correctly.</p>
   *
   * <p>Weighted average formula: (|rate1| × intensity1 + |rate2| × intensity2) / (|rate1| + |rate2|)</p>
   *
   * @param newPopulation The recharge population rate to add
   * @param newIntensity The recharge intensity for this rate
   * @return A new RechargeInformation instance with accumulated values
   */
  public RechargeInformation add(EngineNumber newPopulation, EngineNumber newIntensity) {
    BigDecimal currentWeight = population.getValue().abs();
    BigDecimal addedWeight = newPopulation.getValue().abs();

    BigDecimal weightedIntensity = calculateWeightedIntensity(
        currentWeight,
        addedWeight,
        newIntensity
    );

    // Accumulate population rates (can be negative)
    BigDecimal accumulatedPopulation = population.getValue().add(newPopulation.getValue());

    return new RechargeInformation(
        new EngineNumber(accumulatedPopulation, "%"),
        new EngineNumber(weightedIntensity, "kg / unit")
    );
  }

  /**
   * Calculate the weighted-average intensity for recharge operations.
   *
   * <p>Use absolute values for weights to handle negative values correctly.
   * For the first recharge command or no population, just use new intensity directly.
   * For multiple recharge commands, compute weighted average: (w1*i1 + w2*i2) / (w1 + w2)</p>
   *
   * @param currentWeight The absolute value of the current population
   * @param addedWeight The absolute value of the new population
   * @param newIntensity The recharge intensity for this rate
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
          .divide(totalWeight, 10, java.math.RoundingMode.HALF_UP);
    }
  }
}
