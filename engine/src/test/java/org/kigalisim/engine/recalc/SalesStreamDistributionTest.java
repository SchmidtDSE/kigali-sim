/**
 * Tests for SalesStreamDistribution class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests for SalesStreamDistribution class.
 */
public class SalesStreamDistributionTest {

  @Test
  public void testConstructorAndGetters() {
    // Test basic constructor and getter functionality
    BigDecimal percentManufacture = new BigDecimal("0.7");
    BigDecimal percentImport = new BigDecimal("0.3");

    SalesStreamDistribution distribution = new SalesStreamDistribution(percentManufacture, percentImport);

    assertEquals(percentManufacture, distribution.getPercentManufacture(),
        "Manufacture percentage should match constructor argument");
    assertEquals(percentImport, distribution.getPercentImport(),
        "Import percentage should match constructor argument");
  }

  @Test
  public void testFiftyFiftySplit() {
    // Test the common 50/50 fallback case
    BigDecimal fifty = new BigDecimal("0.5");

    SalesStreamDistribution distribution = new SalesStreamDistribution(fifty, fifty);

    assertEquals(fifty, distribution.getPercentManufacture(),
        "Manufacture percentage should be 0.5");
    assertEquals(fifty, distribution.getPercentImport(),
        "Import percentage should be 0.5");
  }

  @Test
  public void testHundredPercentManufacture() {
    // Test 100% manufacture case
    SalesStreamDistribution distribution = new SalesStreamDistribution(BigDecimal.ONE, BigDecimal.ZERO);

    assertEquals(BigDecimal.ONE, distribution.getPercentManufacture(),
        "Manufacture percentage should be 1.0");
    assertEquals(BigDecimal.ZERO, distribution.getPercentImport(),
        "Import percentage should be 0.0");
  }

  @Test
  public void testHundredPercentImport() {
    // Test 100% import case
    SalesStreamDistribution distribution = new SalesStreamDistribution(BigDecimal.ZERO, BigDecimal.ONE);

    assertEquals(BigDecimal.ZERO, distribution.getPercentManufacture(),
        "Manufacture percentage should be 0.0");
    assertEquals(BigDecimal.ONE, distribution.getPercentImport(),
        "Import percentage should be 1.0");
  }
}