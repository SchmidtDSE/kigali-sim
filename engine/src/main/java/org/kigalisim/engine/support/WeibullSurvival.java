/**
 * Weibull retirement survival mathematics (shape k = 2, the Rayleigh case).
 *
 * <p>For a mean equipment lifetime &mu; and shape 2, the scale parameter is
 * &lambda; = &mu; / &Gamma;(1 + 1/k) = &mu; &times; 2/&radic;&pi; (the characteristic
 * life), and the survival function is S(a) = exp(&minus;(a/&lambda;)<sup>2</sup>) where
 * <i>a</i> is equipment age in years. The conditional probability that a unit
 * surviving to age <i>a</i>&minus;1 retires during its <i>a</i>-th year is
 * h(a) = 1 &minus; S(a)/S(a&minus;1).</p>
 *
 * <p>This class is the single source of truth for the Weibull retirement
 * mathematics and is deliberately free of engine wiring so it can be tested as a
 * pure function.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Weibull survival mathematics for the shape-2 (Rayleigh) retirement model.
 */
public final class WeibullSurvival {

  private static final double ONE_OVER_MEAN_TO_SCALE = 2.0 / Math.sqrt(Math.PI);

  private static final double LN_1000 = Math.log(1000.0);

  private static final double LN_2 = Math.log(2.0);

  private final BigDecimal meanYears;

  private final double characteristicLife;

  private final int truncationAge;

  private final int syntheticCohortOffsetYears;

  private WeibullSurvival(BigDecimal meanYears) {
    this.meanYears = meanYears;
    double mean = meanYears.doubleValue();
    this.characteristicLife = mean * ONE_OVER_MEAN_TO_SCALE;
    this.truncationAge = (int) Math.ceil(characteristicLife * Math.sqrt(LN_1000));
    this.syntheticCohortOffsetYears = new BigDecimal(2.0 * mean / Math.PI)
        .setScale(0, RoundingMode.HALF_UP)
        .intValueExact();
  }

  /**
   * Create survival mathematics for a positive mean lifetime in years.
   *
   * @param meanYears the mean equipment lifetime in years, strictly positive
   * @return the corresponding Weibull survival mathematics
   * @throws IllegalArgumentException if {@code meanYears} is not positive
   */
  public static WeibullSurvival fromMean(BigDecimal meanYears) {
    if (meanYears == null || meanYears.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Weibull retirement requires a positive mean lifetime in years");
    }
    return new WeibullSurvival(meanYears);
  }

  /**
   * The mean lifetime in years supplied at construction.
   *
   * @return the mean lifetime in years
   */
  public BigDecimal getMeanYears() {
    return meanYears;
  }

  /**
   * The characteristic life &lambda; = &mu; &times; 2/&radic;&pi;, the age by
   * which 63.2% of units have failed.
   *
   * @return the characteristic life in years
   */
  public BigDecimal getCharacteristicLife() {
    return toBigDecimal(characteristicLife);
  }

  /**
   * The median life &lambda;&radic;(ln 2) in years.
   *
   * @return the median life in years
   */
  public BigDecimal getMedianLife() {
    return toBigDecimal(characteristicLife * Math.sqrt(LN_2));
  }

  /**
   * The first age at which cumulative retirement reaches 99.9% (S(a) &le; 0.001),
   * i.e. ceil(&lambda;&middot;&radic;(ln 1000)). The hazard at and beyond this age
   * is 1.0 so the residual tail is swept up.
   *
   * @return the truncation age in years
   */
  public int getTruncationAge() {
    return truncationAge;
  }

  /**
   * The survival probability S(a) = exp(&minus;(a/&lambda;)<sup>2</sup>) at age
   * <i>a</i>, where S(0) = 1.
   *
   * @param age the equipment age in years
   * @return the survival probability at that age
   */
  public BigDecimal getSurvival(int age) {
    double ratio = age / characteristicLife;
    return toBigDecimal(Math.exp(-(ratio * ratio)));
  }

  /**
   * The conditional probability h(a) that a unit surviving to age <i>a</i>&minus;1
   * retires during its <i>a</i>-th year. At and beyond the truncation age this is
   * 1.0 by the sweep rule.
   *
   * @param age the equipment age in years, starting at 1
   * @return the discrete annual hazard at that age
   */
  public BigDecimal getHazard(int age) {
    if (age >= truncationAge) {
      return BigDecimal.ONE;
    }
    double priorSurvival = getSurvival(age - 1).doubleValue();
    double hazard = 1.0 - getSurvival(age).doubleValue() / priorSurvival;
    return toBigDecimal(hazard);
  }

  /**
   * The steady-state mean fleet age for shape 2, round(2&mu;/&pi;) years, used to
   * place the synthetic prior-equipment cohort of the {@code assuming new}
   * modifier.
   *
   * @return the synthetic cohort offset in years
   */
  public int getSyntheticCohortOffsetYears() {
    return syntheticCohortOffsetYears;
  }

  private static BigDecimal toBigDecimal(double value) {
    return BigDecimal.valueOf(value);
  }
}
