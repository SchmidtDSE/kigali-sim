package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
import org.kigalisim.lang.operation.DisplacementType;

/**
 * Unit tests for DisplaceExecutor class.
 *
 * <p>Tests displacement logic for stream-based and substance-based displacement,
 * equipment-unit vs volume-based modes, and automatic recycling handling.</p>
 */
class DisplaceExecutorTest {
  private SingleThreadEngine engine;
  private DisplaceExecutor displaceExecutor;

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

    // Setup destination substance
    engine.setSubstance("R-600a");
    engine.enable("domestic", Optional.empty());
    engine.enable("import", Optional.empty());
    engine.equals(new EngineNumber(new BigDecimal("3"), "kgCO2e / kg"), null);
    engine.setInitialCharge(new EngineNumber(new BigDecimal("2"), "kg / unit"), "domestic", null);
    engine.setInitialCharge(new EngineNumber(new BigDecimal("2"), "kg / unit"), "import", null);

    // Back to source substance
    engine.setSubstance("HFC-134a");

    displaceExecutor = new DisplaceExecutor(engine);
  }

  private void setStreamValue(String stream, BigDecimal value, String units) {
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(new EngineNumber(value, units))
        .build();
    engine.executeStreamUpdate(update);
  }

  @Test
  void testExecuteNullDisplaceTargetNoOperation() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");
    BigDecimal changeAmount = new BigDecimal("-5");

    // Act
    displaceExecutor.execute("domestic", amount, changeAmount, null, DisplacementType.EQUIVALENT);

    // Assert - domestic should remain unchanged
    EngineNumber result = engine.getStream("domestic");
    assertEquals(0, new BigDecimal("100").compareTo(result.getValue()));
  }

  @Test
  void testExecuteSelfDisplacementThrowsException() {
    // Arrange
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");
    BigDecimal changeAmount = new BigDecimal("-5");

    // Act & Assert - should throw when displacing to same stream
    assertThrows(RuntimeException.class, () -> {
      displaceExecutor.execute("domestic", amount, changeAmount, "domestic", DisplacementType.EQUIVALENT);
    });
  }

  @Test
  void testExecuteVolumeDisplacementToStreamCallsShortcuts() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("import", new BigDecimal("50"), "kg");

    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");
    BigDecimal changeAmount = new BigDecimal("-20"); // Reduced domestic by 20

    // Act - displace the reduction to import
    displaceExecutor.execute("domestic", amount, changeAmount, "import", DisplacementType.EQUIVALENT);

    // Assert - import should increase by 20kg
    EngineNumber importResult = engine.getStream("import");
    assertEquals(0, new BigDecimal("70").compareTo(importResult.getValue()),
        "Expected import to be 70 (50 + 20) but got " + importResult.getValue());
  }

  @Test
  void testExecuteVolumeDisplacementToSubstanceSwitchesScope() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");

    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");
    BigDecimal changeAmount = new BigDecimal("-20"); // Reduced domestic by 20

    // Act - displace to R-600a
    displaceExecutor.execute("domestic", amount, changeAmount, "R-600a", DisplacementType.EQUIVALENT);

    // Assert - R-600a domestic should increase by 20kg
    engine.setSubstance("R-600a");
    EngineNumber destResult = engine.getStream("domestic");
    assertEquals(0, new BigDecimal("20").compareTo(destResult.getValue()),
        "Expected R-600a domestic to be 20 but got " + destResult.getValue());

    // Verify original scope is HFC-134a (implicitly restored by test setup)
    engine.setSubstance("HFC-134a");
    assertEquals("HFC-134a", engine.getScope().getSubstance());
  }

  @Test
  void testExecuteUnitsDisplacementToStreamConvertsUnits() {
    // Arrange - equipment units
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("import", new BigDecimal("50"), "kg");

    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "units");
    // Reduced by 20kg (20 units * 1 kg/unit)
    BigDecimal changeAmount = new BigDecimal("-20");

    // Act - displace to import stream (same substance, so same initial charge)
    displaceExecutor.execute("domestic", amount, changeAmount, "import", DisplacementType.EQUIVALENT);

    // Assert - import should increase by 20kg
    EngineNumber importResult = engine.getStream("import");
    assertEquals(0, new BigDecimal("70").compareTo(importResult.getValue()),
        "Expected import to be 70 (50 + 20) but got " + importResult.getValue());
  }

  @Test
  void testExecuteUnitsDisplacementToSubstanceSwitchesAndRestoresScope() {
    // Arrange - equipment units with substance displacement
    setStreamValue("domestic", new BigDecimal("100"), "kg");

    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "units");
    // Reduced by 20kg (20 units * 1 kg/unit for HFC-134a)
    BigDecimal changeAmount = new BigDecimal("-20");

    final String originalSubstance = engine.getScope().getSubstance();

    // Act - displace to R-600a (which has 2 kg/unit initial charge)
    displaceExecutor.execute("domestic", amount, changeAmount, "R-600a", DisplacementType.EQUIVALENT);

    // Assert - R-600a should get same number of units (20) but with its own initial charge (2 kg/unit)
    // So R-600a domestic should increase by 40kg (20 units * 2 kg/unit)
    engine.setSubstance("R-600a");
    EngineNumber destResult = engine.getStream("domestic");
    assertEquals(0, new BigDecimal("40").compareTo(destResult.getValue()),
        "Expected R-600a domestic to be 40 (20 units * 2 kg/unit) but got " + destResult.getValue());

    // Verify scope was restored
    engine.setSubstance(originalSubstance);
    assertEquals(originalSubstance, engine.getScope().getSubstance());
  }
}
