/**
 * Test for the WeibullSurvival retirement mathematics.
 *
 * <p>The expected values in this class are the numeric oracles of the Weibull
 * retirement specification, recomputed in double precision.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests for WeibullSurvival.
 */
public class WeibullSurvivalTest {

  @Test
  public void testScaleFromMean() {
    assertEquals(5.641896, WeibullSurvival.fromMean(new BigDecimal("5"))
        .getCharacteristicLife().doubleValue(), 1e-4);
    assertEquals(22.567583, WeibullSurvival.fromMean(new BigDecimal("20"))
        .getCharacteristicLife().doubleValue(), 1e-4);
  }

  @Test
  public void testSurvivalValues() {
    WeibullSurvival fiveYear = WeibullSurvival.fromMean(new BigDecimal("5"));
    assertEquals(1.0, fiveYear.getSurvival(0).doubleValue(), 1e-4);
    assertEquals(0.455938, fiveYear.getSurvival(5).doubleValue(), 1e-4);
    assertEquals(0.043214, fiveYear.getSurvival(10).doubleValue(), 1e-4);
    assertEquals(0.000852, fiveYear.getSurvival(15).doubleValue(), 1e-4);
  }

  @Test
  public void testHazardMatchesWorkedTable() {
    WeibullSurvival fiveYear = WeibullSurvival.fromMean(new BigDecimal("5"));
    double[] expected = {0.0309, 0.0899, 0.1454, 0.1974, 0.2463, 0.2922,
        0.3353, 0.3758, 0.4138, 0.4495, 0.4830};
    for (int a = 1; a <= 11; a++) {
      assertEquals(expected[a - 1], fiveYear.getHazard(a).doubleValue(), 0.001,
          "hazard at age " + a);
    }
  }

  @Test
  public void testHazardTwentyYearMean() {
    WeibullSurvival twentyYear = WeibullSurvival.fromMean(new BigDecimal("20"));
    assertEquals(0.00196, twentyYear.getHazard(1).doubleValue(), 1e-4);
    assertEquals(0.03662, twentyYear.getHazard(10).doubleValue(), 1e-4);
    assertEquals(0.04790, twentyYear.getHazard(13).doubleValue(), 1e-4);
    assertEquals(0.07372, twentyYear.getHazard(20).doubleValue(), 1e-4);
  }

  @Test
  public void testTruncationAge() {
    assertEquals(15, WeibullSurvival.fromMean(new BigDecimal("5")).getTruncationAge());
    assertEquals(60, WeibullSurvival.fromMean(new BigDecimal("20")).getTruncationAge());
    assertEquals(1.0, WeibullSurvival.fromMean(new BigDecimal("5")).getHazard(15).doubleValue(), 1e-9);
    assertEquals(1.0, WeibullSurvival.fromMean(new BigDecimal("5")).getHazard(16).doubleValue(), 1e-9);
  }

  @Test
  public void testSyntheticCohortOffset() {
    assertEquals(3, WeibullSurvival.fromMean(new BigDecimal("5")).getSyntheticCohortOffsetYears());
    assertEquals(13, WeibullSurvival.fromMean(new BigDecimal("20")).getSyntheticCohortOffsetYears());
  }

  @Test
  public void testMedianLife() {
    assertEquals(4.697, WeibullSurvival.fromMean(new BigDecimal("5")).getMedianLife().doubleValue(), 1e-3);
    assertEquals(18.789, WeibullSurvival.fromMean(new BigDecimal("20")).getMedianLife().doubleValue(), 1e-3);
  }

  @Test
  public void testMeanMustBePositive() {
    assertThrows(IllegalArgumentException.class, () -> WeibullSurvival.fromMean(BigDecimal.ZERO));
    assertThrows(IllegalArgumentException.class, () -> WeibullSurvival.fromMean(new BigDecimal("-1")));
  }
}
