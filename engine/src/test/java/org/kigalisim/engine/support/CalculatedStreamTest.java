/**
 * Unit tests for the CalculatedStream class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.Scope;

/**
 * Tests for the CalculatedStream class.
 */
public class CalculatedStreamTest {

  /**
   * Test that CalculatedStream can be created with all parameters.
   */
  @Test
  public void testCreatesWithAllParameters() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("100"), "kg");
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.7"), new BigDecimal("0.3"));

    CalculatedStream stream = new CalculatedStream(
        useKey, "domestic", value, true, Optional.of(distribution), true);

    assertNotNull(stream, "CalculatedStream should be constructable");
    assertEquals(useKey, stream.getUseKey(), "UseKey should match");
    assertEquals("domestic", stream.getName(), "Name should match");
    assertEquals(value, stream.getValue(), "Value should match");
    assertTrue(stream.getSubtractRecycling(), "SubtractRecycling should be true");
    assertEquals(Optional.of(distribution), stream.getDistribution(), "Distribution should match");
    assertTrue(stream.isSalesDistributionRequired(), "SalesDistributionRequired should be true");
  }

  /**
   * Test that CalculatedStream works with null distribution.
   */
  @Test
  public void testWorksWithNullDistribution() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("50"), "units");

    CalculatedStream stream = new CalculatedStream(
        useKey, "equipment", value, false, Optional.empty(), false);

    assertEquals(useKey, stream.getUseKey(), "UseKey should match");
    assertEquals("equipment", stream.getName(), "Name should match");
    assertEquals(value, stream.getValue(), "Value should match");
    assertFalse(stream.getSubtractRecycling(), "SubtractRecycling should be false");
    assertEquals(Optional.empty(), stream.getDistribution(), "Distribution should be empty");
    assertFalse(stream.isSalesDistributionRequired(), "SalesDistributionRequired should be false");
  }

  /**
   * Test that CalculatedStream preserves EngineNumber immutability.
   */
  @Test
  public void testPreservesEngineNumberImmutability() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber originalValue = new EngineNumber(new BigDecimal("75"), "tCO2e");

    CalculatedStream stream = new CalculatedStream(
        useKey, "consumption", originalValue, false, null, false);

    EngineNumber retrievedValue = stream.getValue();
    assertEquals(originalValue.getValue(), retrievedValue.getValue(), "Value should be preserved");
    assertEquals(originalValue.getUnits(), retrievedValue.getUnits(), "Units should be preserved");
  }

  /**
   * Test that CalculatedStream works with different stream types.
   */
  @Test
  public void testWorksWithDifferentStreamTypes() {
    Scope useKey = new Scope("test", "app", "substance");

    // Test outcome stream
    EngineNumber outcomeValue = new EngineNumber(new BigDecimal("200"), "kg");
    CalculatedStream outcomeStream = new CalculatedStream(
        useKey, "recycle", outcomeValue, false, Optional.empty(), false);
    assertEquals("recycle", outcomeStream.getName(), "Outcome stream name should match");

    // Test sales stream
    EngineNumber salesValue = new EngineNumber(new BigDecimal("150"), "kg");
    SalesStreamDistribution dist = new SalesStreamDistribution(
        new BigDecimal("0.8"), new BigDecimal("0.2"));
    CalculatedStream salesStream = new CalculatedStream(
        useKey, "import", salesValue, true, Optional.of(dist), true);
    assertEquals("import", salesStream.getName(), "Sales stream name should match");
    assertTrue(salesStream.isSalesDistributionRequired(), "Sales stream should require distribution");
  }
}