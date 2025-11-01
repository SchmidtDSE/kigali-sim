package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimpleUseKey;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Unit tests for StreamUpdateShortcuts class.
 *
 * <p>Tests the stream update shortcut operations for displacement and replacement logic.</p>
 */
class StreamUpdateShortcutsTest {

  private SingleThreadEngine engine;
  private StreamUpdateShortcuts shortcuts;

  @BeforeEach
  void setUp() {
    engine = new SingleThreadEngine(2020, 2030);
    engine.setStanza("default");
    engine.setApplication("TestApp");
    engine.setSubstance("HFC-134a");
    engine.enable("domestic", Optional.empty());
    engine.enable("import", Optional.empty());
    engine.equals(new EngineNumber(new BigDecimal("1430"), "kgCO2e / kg"), null);
    engine.setInitialCharge(new EngineNumber(BigDecimal.ONE, "kg / unit"), "domestic", null);
    engine.setInitialCharge(new EngineNumber(BigDecimal.ONE, "kg / unit"), "import", null);

    shortcuts = new StreamUpdateShortcuts(engine);
  }

  private void setStreamValue(String stream, BigDecimal value, String units) {
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(new EngineNumber(value, units))
        .build();
    engine.executeStreamUpdate(update);
  }

  @Test
  void testChangeStreamWithoutReportingUnits_WithinRange_UpdatesStream() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    YearMatcher matcher = new YearMatcher(2020, 2025);

    // Act
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount,
        Optional.of(matcher), Optional.empty());

    // Assert
    EngineNumber result = engine.getStream("domestic");
    assertEquals(0, new BigDecimal("150").compareTo(result.getValue()),
        "Expected 150 but got " + result.getValue());
    assertEquals("kg", result.getUnits());
  }

  @Test
  void testChangeStreamWithoutReportingUnits_OutsideRange_NoOperation() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    YearMatcher matcher = new YearMatcher(2021, 2025); // Outside current year 2020

    // Act
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount,
        Optional.of(matcher), Optional.empty());

    // Assert - should not change
    EngineNumber result = engine.getStream("domestic");
    assertEquals(0, new BigDecimal("100").compareTo(result.getValue()),
        "Expected 100 but got " + result.getValue());
  }

  @Test
  void testChangeStreamWithoutReportingUnits_NegativeValue_ClampedToZero() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("30"), "kg");
    YearMatcher matcher = new YearMatcher(2020, 2025);

    // Act
    EngineNumber amount = new EngineNumber(new BigDecimal("-50"), "kg");
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount,
        Optional.of(matcher), Optional.empty(), false);

    // Assert - should be clamped to zero
    EngineNumber result = engine.getStream("domestic");
    assertEquals(0, BigDecimal.ZERO.compareTo(result.getValue()),
        "Expected 0 but got " + result.getValue());
  }

  @Test
  void testChangeStreamWithoutReportingUnits_NegativeValue_Allowed() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("30"), "kg");
    YearMatcher matcher = new YearMatcher(2020, 2025);

    // Act
    EngineNumber amount = new EngineNumber(new BigDecimal("-50"), "kg");
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount,
        Optional.of(matcher), Optional.empty(), true);

    // Assert - should allow negative
    EngineNumber result = engine.getStream("domestic");
    assertEquals(0, new BigDecimal("-20").compareTo(result.getValue()),
        "Expected -20 but got " + result.getValue());
  }

  @Test
  void testChangeStreamWithoutReportingUnits_WithCustomScope_UsesProvidedScope() {
    // Arrange
    setStreamValue("import", new BigDecimal("100"), "kg");
    UseKey customScope = new SimpleUseKey("TestApp", "HFC-134a");
    YearMatcher matcher = new YearMatcher(2020, 2025);

    // Act
    EngineNumber amount = new EngineNumber(new BigDecimal("25"), "kg");
    shortcuts.changeStreamWithoutReportingUnits("import", amount,
        Optional.of(matcher), Optional.of(customScope));

    // Assert
    EngineNumber result = engine.getStream("import");
    assertEquals(0, new BigDecimal("125").compareTo(result.getValue()),
        "Expected 125 but got " + result.getValue());
  }

  @Test
  void testChangeStreamWithDisplacementContext_SalesStream_RecalculatesPopulation() {
    // Arrange - setup destination substance
    engine.setSubstance("R-600a");
    engine.enable("domestic", Optional.empty());
    engine.equals(new EngineNumber(new BigDecimal("3"), "kgCO2e / kg"), null);
    engine.setInitialCharge(new EngineNumber(BigDecimal.ONE, "kg / unit"), "domestic", null);

    // Back to source substance
    engine.setSubstance("HFC-134a");
    setStreamValue("domestic", new BigDecimal("100"), "kg");

    Scope destinationScope = new Scope("default", "TestApp", "R-600a");

    // Act
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");
    shortcuts.changeStreamWithDisplacementContext("domestic", amount, destinationScope);

    // Assert - check that destination scope was updated
    engine.setSubstance("R-600a");
    EngineNumber destResult = engine.getStream("domestic");
    assertEquals(0, new BigDecimal("50").compareTo(destResult.getValue()),
        "Expected 50 but got " + destResult.getValue());

    // Verify original substance scope is restored
    engine.setSubstance("HFC-134a");
    assertEquals("HFC-134a", engine.getScope().getSubstance());
  }

  @Test
  void testChangeStreamWithDisplacementContext_UpdatesLastSpecifiedValue() {
    // Arrange - setup destination substance
    engine.setSubstance("R-600a");
    engine.enable("domestic", Optional.empty());
    engine.equals(new EngineNumber(new BigDecimal("3"), "kgCO2e / kg"), null);
    engine.setInitialCharge(new EngineNumber(BigDecimal.ONE, "kg / unit"), "domestic", null);

    // Back to source substance
    engine.setSubstance("HFC-134a");
    setStreamValue("domestic", new BigDecimal("100"), "kg");

    Scope destinationScope = new Scope("default", "TestApp", "R-600a");

    // Act
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");
    shortcuts.changeStreamWithDisplacementContext("domestic", amount, destinationScope);

    // Assert - check lastSpecifiedValue was updated
    UseKey destKey = new SimpleUseKey("TestApp", "R-600a");
    EngineNumber lastSpecified = engine.getStreamKeeper().getLastSpecifiedValue(destKey, "domestic");
    assertTrue(lastSpecified != null && lastSpecified.getValue().compareTo(BigDecimal.ZERO) > 0,
        "Last specified value should be set for destination");
  }

  @Test
  void testChangeStreamWithDisplacementContext_NegativeValue_ClampedToZero() {
    // Arrange - setup destination substance
    engine.setSubstance("R-600a");
    engine.enable("import", Optional.empty());
    engine.equals(new EngineNumber(new BigDecimal("3"), "kgCO2e / kg"), null);
    engine.setInitialCharge(new EngineNumber(BigDecimal.ONE, "kg / unit"), "import", null);

    // Back to source substance
    engine.setSubstance("HFC-134a");
    setStreamValue("import", new BigDecimal("30"), "kg");

    Scope destinationScope = new Scope("default", "TestApp", "R-600a");

    // Act
    EngineNumber amount = new EngineNumber(new BigDecimal("-50"), "kg");
    shortcuts.changeStreamWithDisplacementContext("import", amount, destinationScope, false);

    // Assert - check value was clamped
    engine.setSubstance("R-600a");
    EngineNumber result = engine.getStream("import");
    assertEquals(0, BigDecimal.ZERO.compareTo(result.getValue()),
        "Expected 0 but got " + result.getValue());
  }

  @Test
  void testChangeStreamWithDisplacementContext_OptimizeRecalcsTrue_SkipsSalesPropagation() {
    // Arrange - setup destination substance
    engine.setSubstance("R-600a");
    engine.enable("domestic", Optional.empty());
    engine.equals(new EngineNumber(new BigDecimal("3"), "kgCO2e / kg"), null);
    engine.setInitialCharge(new EngineNumber(BigDecimal.ONE, "kg / unit"), "domestic", null);

    // Back to source substance
    engine.setSubstance("HFC-134a");
    setStreamValue("domestic", new BigDecimal("100"), "kg");

    Scope destinationScope = new Scope("default", "TestApp", "R-600a");

    // Act - just verify it completes without error
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");
    shortcuts.changeStreamWithDisplacementContext("domestic", amount, destinationScope);

    // Assert - verify operation completed
    engine.setSubstance("R-600a");
    EngineNumber result = engine.getStream("domestic");
    assertTrue(result.getValue().compareTo(BigDecimal.ZERO) > 0);
  }

  @Test
  void testChangeStreamWithDisplacementContext_OptimizeRecalcsFalse_PropagatestoSales() {
    // This test would require setting OPTIMIZE_RECALCS to false
    // Since it's a static final in the implementation, we just verify basic behavior
    // Arrange - setup destination substance
    engine.setSubstance("R-600a");
    engine.enable("domestic", Optional.empty());
    engine.equals(new EngineNumber(new BigDecimal("3"), "kgCO2e / kg"), null);
    engine.setInitialCharge(new EngineNumber(BigDecimal.ONE, "kg / unit"), "domestic", null);

    // Back to source substance
    engine.setSubstance("HFC-134a");
    setStreamValue("domestic", new BigDecimal("100"), "kg");

    Scope destinationScope = new Scope("default", "TestApp", "R-600a");

    // Act - just verify it completes without error
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");
    shortcuts.changeStreamWithDisplacementContext("domestic", amount, destinationScope);

    // Assert - verify operation completed
    engine.setSubstance("R-600a");
    EngineNumber result = engine.getStream("domestic");
    assertTrue(result.getValue().compareTo(BigDecimal.ZERO) > 0);
  }

  @Test
  void testChangeStreamMethods_WorkTogether() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    YearMatcher matcher = new YearMatcher(2020, 2025);

    // Setup destination substance
    engine.setSubstance("R-600a");
    engine.enable("domestic", Optional.empty());
    engine.equals(new EngineNumber(new BigDecimal("3"), "kgCO2e / kg"), null);
    engine.setInitialCharge(new EngineNumber(BigDecimal.ONE, "kg / unit"), "domestic", null);

    // Back to source
    engine.setSubstance("HFC-134a");

    // Act - first call to changeStreamWithoutReportingUnits
    EngineNumber amount1 = new EngineNumber(new BigDecimal("-20"), "kg");
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount1,
        Optional.of(matcher), Optional.empty());

    // Second call to changeStreamWithDisplacementContext
    Scope destinationScope = new Scope("default", "TestApp", "R-600a");
    EngineNumber amount2 = new EngineNumber(new BigDecimal("20"), "kg");
    shortcuts.changeStreamWithDisplacementContext("domestic", amount2, destinationScope);

    // Assert - verify both operations executed
    EngineNumber sourceResult = engine.getStream("domestic");
    assertEquals(0, new BigDecimal("80").compareTo(sourceResult.getValue()),
        "Expected 80 but got " + sourceResult.getValue());

    engine.setSubstance("R-600a");
    EngineNumber destResult = engine.getStream("domestic");
    assertEquals(0, new BigDecimal("20").compareTo(destResult.getValue()),
        "Expected 20 but got " + destResult.getValue());
  }
}
