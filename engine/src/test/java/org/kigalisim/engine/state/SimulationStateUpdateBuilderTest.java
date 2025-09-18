/**
 * Unit tests for the SimulationStateUpdateBuilder class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.Scope;

/**
 * Tests for the SimulationStateUpdateBuilder class.
 */
public class SimulationStateUpdateBuilderTest {

  /**
   * Test that SimulationStateUpdateBuilder can be created.
   */
  @Test
  public void testInitializes() {
    SimulationStateUpdateBuilder builder = new SimulationStateUpdateBuilder();
    assertNotNull(builder, "SimulationStateUpdateBuilder should be constructable");
  }

  /**
   * Test building a basic stream with all required parameters.
   */
  @Test
  public void testBuildBasicStream() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("100"), "kg");

    SimulationStateUpdate stream = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("equipment")
        .setValue(value)
        .build();

    assertNotNull(stream, "Built stream should not be null");
    assertEquals(useKey, stream.getUseKey(), "UseKey should match");
    assertEquals("equipment", stream.getName(), "Name should match");
    assertEquals(value, stream.getValue(), "Value should match");
    assertTrue(stream.getSubtractRecycling(), "Should default to recycling subtraction enabled");
    assertEquals(Optional.empty(), stream.getDistribution(), "Should default to empty distribution");
    assertFalse(stream.isSalesDistributionRequired(), "Should default to no sales distribution required");
  }

  /**
   * Test building a stream with recycling subtraction enabled.
   */
  @Test
  public void testBuildWithRecyclingSubtraction() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("50"), "kg");

    SimulationStateUpdate stream = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("import")
        .setValue(value)
        .setSubtractRecycling(true)
        .build();

    assertTrue(stream.getSubtractRecycling(), "Should enable recycling subtraction");
  }

  /**
   * Test building a stream with distribution.
   */
  @Test
  public void testBuildWithDistribution() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("75"), "kg");
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.6"), new BigDecimal("0.4"));

    SimulationStateUpdate stream = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("domestic")
        .setValue(value)
        .setDistribution(distribution)
        .build();

    assertEquals(Optional.of(distribution), stream.getDistribution(), "Distribution should match");
  }

  /**
   * Test automatic inference of sales distribution for sales streams.
   */
  @Test
  public void testAutomaticSalesDistributionInference() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("25"), "kg");

    // Test sales stream names
    String[] salesStreams = {"sales", "domestic", "import", "export"};
    for (String streamName : salesStreams) {
      SimulationStateUpdate stream = new SimulationStateUpdateBuilder()
          .setUseKey(useKey)
          .setName(streamName)
          .setValue(value)
          .build();

      assertTrue(stream.isSalesDistributionRequired(),
          "Stream '" + streamName + "' should require sales distribution");
    }

    // Test non-sales stream names
    String[] nonSalesStreams = {"equipment", "consumption", "recycle", "eolEmissions"};
    for (String streamName : nonSalesStreams) {
      SimulationStateUpdate stream = new SimulationStateUpdateBuilder()
          .setUseKey(useKey)
          .setName(streamName)
          .setValue(value)
          .build();

      assertFalse(stream.isSalesDistributionRequired(),
          "Stream '" + streamName + "' should not require sales distribution");
    }
  }

  /**
   * Test explicit setSubtractRecycling method for outcome streams.
   */
  @Test
  public void testExplicitOutcomeStream() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("30"), "tCO2e");

    SimulationStateUpdate stream = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("consumption")
        .setValue(value)
        .setSubtractRecycling(false)
        .clearDistribution()
        .build();

    assertFalse(stream.getSubtractRecycling(), "Outcome stream should not subtract recycling");
    assertEquals(Optional.empty(), stream.getDistribution(), "Outcome stream should have no distribution");
    assertFalse(stream.isSalesDistributionRequired(), "Outcome stream should not require distribution");
  }

  /**
   * Test explicit setSubtractRecycling method for sales streams.
   */
  @Test
  public void testExplicitSalesStream() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("80"), "kg");

    SimulationStateUpdate stream = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("domestic")
        .setValue(value)
        .setSubtractRecycling(true)
        .build();

    assertTrue(stream.getSubtractRecycling(), "Sales stream should subtract recycling");
    assertTrue(stream.isSalesDistributionRequired(), "Sales stream should require distribution (auto-inferred)");
  }

  /**
   * Test builder throws exception when useKey is missing.
   */
  @Test
  public void testThrowsWhenUseKeyMissing() {
    EngineNumber value = new EngineNumber(new BigDecimal("100"), "kg");

    assertThrows(IllegalStateException.class, () -> {
      new SimulationStateUpdateBuilder()
          .setName("domestic")
          .setValue(value)
          .build();
    }, "Should throw when useKey is missing");
  }

  /**
   * Test builder throws exception when name is missing.
   */
  @Test
  public void testThrowsWhenNameMissing() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("100"), "kg");

    assertThrows(IllegalStateException.class, () -> {
      new SimulationStateUpdateBuilder()
          .setUseKey(useKey)
          .setValue(value)
          .build();
    }, "Should throw when name is missing");
  }

  /**
   * Test builder throws exception when value is missing.
   */
  @Test
  public void testThrowsWhenValueMissing() {
    Scope useKey = new Scope("test", "app", "substance");

    assertThrows(IllegalStateException.class, () -> {
      new SimulationStateUpdateBuilder()
          .setUseKey(useKey)
          .setName("domestic")
          .build();
    }, "Should throw when value is missing");
  }

  /**
   * Test builder method chaining works correctly.
   */
  @Test
  public void testMethodChaining() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value = new EngineNumber(new BigDecimal("90"), "kg");
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.5"), new BigDecimal("0.5"));

    SimulationStateUpdate stream = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("import")
        .setValue(value)
        .setSubtractRecycling(true)
        .setDistribution(distribution)
        .build();

    assertEquals(useKey, stream.getUseKey(), "All chained values should be set");
    assertEquals("import", stream.getName(), "All chained values should be set");
    assertEquals(value, stream.getValue(), "All chained values should be set");
    assertTrue(stream.getSubtractRecycling(), "All chained values should be set");
    assertEquals(Optional.of(distribution), stream.getDistribution(), "All chained values should be set");
    assertTrue(stream.isSalesDistributionRequired(), "All chained values should be set");
  }

  /**
   * Test builder can be reused after build.
   */
  @Test
  public void testBuilderReuse() {
    Scope useKey = new Scope("test", "app", "substance");
    EngineNumber value1 = new EngineNumber(new BigDecimal("10"), "kg");
    EngineNumber value2 = new EngineNumber(new BigDecimal("20"), "kg");

    SimulationStateUpdateBuilder builder = new SimulationStateUpdateBuilder()
        .setUseKey(useKey)
        .setName("domestic");

    SimulationStateUpdate stream1 = builder
        .setValue(value1)
        .build();

    SimulationStateUpdate stream2 = builder
        .setValue(value2)
        .build();

    assertEquals(new BigDecimal("10"), stream1.getValue().getValue(), "First stream should have first value");
    assertEquals(new BigDecimal("20"), stream2.getValue().getValue(), "Second stream should have second value");
    assertEquals("domestic", stream1.getName(), "Both streams should have same name");
    assertEquals("domestic", stream2.getName(), "Both streams should have same name");
  }
}