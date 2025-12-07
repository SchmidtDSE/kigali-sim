/**
 * Unit tests for the RechargeInformation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for the RechargeInformation class.
 */
public class RechargeInformationTest {

  /**
   * Test that RechargeInformation can be initialized with valid parameters.
   */
  @Test
  public void testInitializes() {
    EngineNumber population = new EngineNumber(BigDecimal.TEN, "%");
    EngineNumber intensity = new EngineNumber(BigDecimal.ONE, "kg / unit");

    RechargeInformation info = new RechargeInformation(population, intensity);

    assertNotNull(info, "RechargeInformation should be constructable");
    assertEquals(population.getValue(), info.getPopulation().getValue(),
        "Population should match");
    assertEquals(intensity.getValue(), info.getIntensity().getValue(),
        "Intensity should match");
  }

  /**
   * Test that RechargeInformation is immutable by returning the same objects.
   */
  @Test
  public void testGettersReturnCorrectValues() {
    BigDecimal popValue = new BigDecimal("15.5");
    BigDecimal intensValue = new BigDecimal("2.25");

    EngineNumber population = new EngineNumber(popValue, "%");
    EngineNumber intensity = new EngineNumber(intensValue, "kg / unit");

    RechargeInformation info = new RechargeInformation(population, intensity);

    assertEquals(popValue, info.getPopulation().getValue(),
        "getPopulation should return the initial population value");
    assertEquals(intensValue, info.getIntensity().getValue(),
        "getIntensity should return the initial intensity value");
    assertEquals("%", info.getPopulation().getUnits(),
        "Population units should be %");
    assertEquals("kg / unit", info.getIntensity().getUnits(),
        "Intensity units should be kg / unit");
  }

  /**
   * Test adding to an initial recharge (where currentPopulation is zero).
   *
   * <p>When starting with zero population, the resulting intensity should be exactly
   * the new intensity with no averaging.</p>
   */
  @Test
  public void testAddToZeroPopulation() {
    // Start with zero population
    EngineNumber initialPopulation = new EngineNumber(BigDecimal.ZERO, "%");
    EngineNumber initialIntensity = new EngineNumber(BigDecimal.ZERO, "kg / unit");
    RechargeInformation info = new RechargeInformation(initialPopulation, initialIntensity);

    // Add new population and intensity
    EngineNumber newPopulation = new EngineNumber(BigDecimal.TEN, "%");
    EngineNumber newIntensity = new EngineNumber(new BigDecimal("2.5"), "kg / unit");

    RechargeInformation result = info.add(newPopulation, newIntensity);

    assertEquals(new BigDecimal("10"), result.getPopulation().getValue(),
        "Population should be 10 after adding 10 to 0");
    assertEquals(new BigDecimal("2.5"), result.getIntensity().getValue(),
        "Intensity should be 2.5 (new intensity) when starting from 0");
  }

  /**
   * Test adding to an existing recharge with weighted-average intensity calculation.
   *
   * <p>Example: (10 * 2.0 + 20 * 3.0) / (10 + 20) = (20 + 60) / 30 = 80 / 30 = 2.666...</p>
   */
  @Test
  public void testAddToExistingPopulationWeightedAverage() {
    // Start with existing population and intensity
    EngineNumber initialPopulation = new EngineNumber(BigDecimal.TEN, "%");
    EngineNumber initialIntensity = new EngineNumber(new BigDecimal("2.0"), "kg / unit");
    RechargeInformation info = new RechargeInformation(initialPopulation, initialIntensity);

    // Add new population and intensity
    EngineNumber newPopulation = new EngineNumber(BigDecimal.valueOf(20), "%");
    EngineNumber newIntensity = new EngineNumber(new BigDecimal("3.0"), "kg / unit");

    RechargeInformation result = info.add(newPopulation, newIntensity);

    assertEquals(new BigDecimal("30"), result.getPopulation().getValue(),
        "Population should be 30 (10 + 20)");

    // Expected intensity: (10 * 2.0 + 20 * 3.0) / (10 + 20) = 80 / 30 = 2.6666...
    BigDecimal expectedIntensity = new BigDecimal("2.6666666667");
    BigDecimal difference = result.getIntensity().getValue().subtract(expectedIntensity).abs();
    assertEquals(true, difference.compareTo(new BigDecimal("0.000001")) < 0,
        "Intensity should be weighted average: (10*2.0 + 20*3.0)/(10+20)");
  }

  /**
   * Test weighted average with identical populations and different intensities.
   */
  @Test
  public void testWeightedAverageEqualWeights() {
    // Start with 5% at intensity 1.0
    EngineNumber initialPopulation = new EngineNumber(BigDecimal.valueOf(5), "%");
    EngineNumber initialIntensity = new EngineNumber(BigDecimal.ONE, "kg / unit");
    RechargeInformation info = new RechargeInformation(initialPopulation, initialIntensity);

    // Add 5% at intensity 3.0
    EngineNumber newPopulation = new EngineNumber(BigDecimal.valueOf(5), "%");
    EngineNumber newIntensity = new EngineNumber(new BigDecimal("3.0"), "kg / unit");

    RechargeInformation result = info.add(newPopulation, newIntensity);

    assertEquals(new BigDecimal("10"), result.getPopulation().getValue(),
        "Population should be 10");

    // Expected: (5 * 1.0 + 5 * 3.0) / 10 = 20 / 10 = 2.0
    BigDecimal expectedIntensity = new BigDecimal("2.0");
    BigDecimal difference = result.getIntensity().getValue().subtract(expectedIntensity).abs();
    assertEquals(true, difference.compareTo(new BigDecimal("0.000001")) < 0,
        "Intensity should be 2.0 (average of 1.0 and 3.0)");
  }

  /**
   * Test adding negative population adjustments.
   *
   * <p>Negative population values represent reductions. The weights for intensity
   * averaging should use absolute values to handle this correctly.</p>
   */
  @Test
  public void testAddNegativePopulation() {
    // Start with 20% at intensity 2.0
    EngineNumber initialPopulation = new EngineNumber(BigDecimal.valueOf(20), "%");
    EngineNumber initialIntensity = new EngineNumber(new BigDecimal("2.0"), "kg / unit");
    RechargeInformation info = new RechargeInformation(initialPopulation, initialIntensity);

    // Add negative 10% at intensity 4.0 (reduction with different intensity)
    EngineNumber negativePopulation = new EngineNumber(BigDecimal.valueOf(-10), "%");
    EngineNumber newIntensity = new EngineNumber(new BigDecimal("4.0"), "kg / unit");

    RechargeInformation result = info.add(negativePopulation, newIntensity);

    assertEquals(new BigDecimal("10"), result.getPopulation().getValue(),
        "Population should be 10 (20 - 10)");

    // Expected: (|20| * 2.0 + |-10| * 4.0) / (|20| + |-10|) = (40 + 40) / 30 = 80 / 30
    // = 2.6666...
    BigDecimal expectedIntensity = new BigDecimal("2.6666666667");
    BigDecimal difference = result.getIntensity().getValue().subtract(expectedIntensity).abs();
    assertEquals(true, difference.compareTo(new BigDecimal("0.000001")) < 0,
        "Intensity should use absolute weights: (20*2.0 + 10*4.0)/(20+10)");
  }

  /**
   * Test edge case with zero new population being added.
   */
  @Test
  public void testAddZeroPopulation() {
    // Start with 10% at intensity 2.5
    EngineNumber initialPopulation = new EngineNumber(BigDecimal.TEN, "%");
    EngineNumber initialIntensity = new EngineNumber(new BigDecimal("2.5"), "kg / unit");
    RechargeInformation info = new RechargeInformation(initialPopulation, initialIntensity);

    // Add zero population
    EngineNumber zeroPopulation = new EngineNumber(BigDecimal.ZERO, "%");
    EngineNumber newIntensity = new EngineNumber(new BigDecimal("3.5"), "kg / unit");

    RechargeInformation result = info.add(zeroPopulation, newIntensity);

    assertEquals(new BigDecimal("10"), result.getPopulation().getValue(),
        "Population should remain 10");

    // Expected: (10 * 2.5 + 0 * 3.5) / 10 = 25 / 10 = 2.5
    BigDecimal expectedIntensity = new BigDecimal("2.5");
    BigDecimal difference = result.getIntensity().getValue().subtract(expectedIntensity).abs();
    assertEquals(true, difference.compareTo(new BigDecimal("0.000001")) < 0,
        "Intensity should remain 2.5 (zero addition doesn't change average)");
  }

  /**
   * Test multiple sequential additions.
   */
  @Test
  public void testMultipleSequentialAdditions() {
    // Start with 10% at intensity 1.0
    EngineNumber pop1 = new EngineNumber(BigDecimal.TEN, "%");
    EngineNumber int1 = new EngineNumber(BigDecimal.ONE, "kg / unit");
    RechargeInformation result = new RechargeInformation(pop1, int1);

    // First addition: add 10% at 2.0
    EngineNumber pop2 = new EngineNumber(BigDecimal.TEN, "%");
    EngineNumber int2 = new EngineNumber(new BigDecimal("2.0"), "kg / unit");
    result = result.add(pop2, int2);

    // After first addition: (10*1 + 10*2)/(10+10) = 30/20 = 1.5
    assertEquals(new BigDecimal("20"), result.getPopulation().getValue(),
        "After first add: population should be 20");
    BigDecimal expectedIntensity1 = new BigDecimal("1.5");
    BigDecimal difference1 = result.getIntensity().getValue().subtract(expectedIntensity1).abs();
    assertEquals(true, difference1.compareTo(new BigDecimal("0.000001")) < 0,
        "After first add: intensity should be 1.5");

    // Second addition: add 20% at 3.0
    EngineNumber pop3 = new EngineNumber(BigDecimal.valueOf(20), "%");
    EngineNumber int3 = new EngineNumber(new BigDecimal("3.0"), "kg / unit");
    result = result.add(pop3, int3);

    // After second addition: (20*1.5 + 20*3.0)/(20+20) = (30+60)/40 = 90/40 = 2.25
    assertEquals(new BigDecimal("40"), result.getPopulation().getValue(),
        "After second add: population should be 40");
    BigDecimal expectedIntensity2 = new BigDecimal("2.25");
    BigDecimal difference2 = result.getIntensity().getValue().subtract(expectedIntensity2).abs();
    assertEquals(true, difference2.compareTo(new BigDecimal("0.000001")) < 0,
        "After second add: intensity should be 2.25");
  }

  /**
   * Test that add() returns a new instance (immutability).
   */
  @Test
  public void testAddReturnsNewInstance() {
    EngineNumber initialPopulation = new EngineNumber(BigDecimal.TEN, "%");
    EngineNumber initialIntensity = new EngineNumber(BigDecimal.ONE, "kg / unit");
    RechargeInformation original = new RechargeInformation(initialPopulation, initialIntensity);

    EngineNumber newPopulation = new EngineNumber(BigDecimal.valueOf(5), "%");
    EngineNumber newIntensity = new EngineNumber(new BigDecimal("2.0"), "kg / unit");

    RechargeInformation result = original.add(newPopulation, newIntensity);

    // Original should be unchanged
    assertEquals(new BigDecimal("10"), original.getPopulation().getValue(),
        "Original population should remain 10");
    assertEquals(BigDecimal.ONE, original.getIntensity().getValue(),
        "Original intensity should remain 1.0");

    // Result should be new values
    assertEquals(new BigDecimal("15"), result.getPopulation().getValue(),
        "Result population should be 15");
  }

  /**
   * Test with very small population values and precision.
   */
  @Test
  public void testSmallPopulationPrecision() {
    // Start with very small positive population
    EngineNumber initialPopulation = new EngineNumber(new BigDecimal("0.001"), "%");
    EngineNumber initialIntensity = new EngineNumber(new BigDecimal("1.5"), "kg / unit");
    RechargeInformation info = new RechargeInformation(initialPopulation, initialIntensity);

    // Add another small population
    EngineNumber newPopulation = new EngineNumber(new BigDecimal("0.002"), "%");
    EngineNumber newIntensity = new EngineNumber(new BigDecimal("2.5"), "kg / unit");

    RechargeInformation result = info.add(newPopulation, newIntensity);

    assertEquals(new BigDecimal("0.003"), result.getPopulation().getValue(),
        "Population should be 0.003");

    // Expected: (0.001 * 1.5 + 0.002 * 2.5) / 0.003 = (0.0015 + 0.005) / 0.003
    // = 0.0065 / 0.003 = 2.1666...
    BigDecimal expectedIntensity = new BigDecimal("2.1666666667");
    BigDecimal difference = result.getIntensity().getValue().subtract(expectedIntensity).abs();
    assertEquals(true, difference.compareTo(new BigDecimal("0.000001")) < 0,
        "Intensity should maintain precision with small values");
  }

  /**
   * Test with negative initial population and positive addition.
   */
  @Test
  public void testNegativeInitialPopulation() {
    // Start with negative population (unusual but possible)
    EngineNumber initialPopulation = new EngineNumber(new BigDecimal("-5"), "%");
    EngineNumber initialIntensity = new EngineNumber(new BigDecimal("2.0"), "kg / unit");
    RechargeInformation info = new RechargeInformation(initialPopulation, initialIntensity);

    // Add positive population
    EngineNumber newPopulation = new EngineNumber(BigDecimal.TEN, "%");
    EngineNumber newIntensity = new EngineNumber(new BigDecimal("3.0"), "kg / unit");

    RechargeInformation result = info.add(newPopulation, newIntensity);

    assertEquals(BigDecimal.valueOf(5), result.getPopulation().getValue(),
        "Population should be 5 (-5 + 10)");

    // Expected: (|-5| * 2.0 + |10| * 3.0) / (|-5| + |10|) = (10 + 30) / 15 = 40 / 15
    // = 2.6666...
    BigDecimal expectedIntensity = new BigDecimal("2.6666666667");
    BigDecimal difference = result.getIntensity().getValue().subtract(expectedIntensity).abs();
    assertEquals(true, difference.compareTo(new BigDecimal("0.000001")) < 0,
        "Intensity should use absolute weights correctly");
  }

  /**
   * Test that units are preserved correctly in results.
   */
  @Test
  public void testUnitsPreserved() {
    EngineNumber initialPopulation = new EngineNumber(new BigDecimal("10"), "%");
    EngineNumber initialIntensity = new EngineNumber(new BigDecimal("1.5"), "kg / unit");
    RechargeInformation info = new RechargeInformation(initialPopulation, initialIntensity);

    EngineNumber newPopulation = new EngineNumber(new BigDecimal("5"), "%");
    EngineNumber newIntensity = new EngineNumber(new BigDecimal("2.5"), "kg / unit");

    RechargeInformation result = info.add(newPopulation, newIntensity);

    assertEquals("%", result.getPopulation().getUnits(),
        "Population units should remain %");
    assertEquals("kg / unit", result.getIntensity().getUnits(),
        "Intensity units should remain kg / unit");
  }
}
