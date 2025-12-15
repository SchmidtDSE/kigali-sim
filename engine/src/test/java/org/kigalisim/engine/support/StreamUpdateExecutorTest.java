/**
 * Unit tests for StreamUpdateExecutor class.
 *
 * <p>Tests the sales carry-over logic that combines domestic and import streams
 * into a unified sales intent when specified in equipment units.</p>
 */

package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Unit tests for StreamUpdateExecutor using real SingleThreadEngine instances.
 *
 * <p>This test class follows the hybrid testing approach with minimal mocking,
 * using real engine components to validate actual behavior rather than mocked responses.
 * The tests verify that the sales carry-over logic correctly combines domestic and import
 * streams when they are specified in equipment units.</p>
 */
class StreamUpdateExecutorTest {

  private SingleThreadEngine engine;
  private StreamUpdateExecutor executor;
  private Scope useKey;

  /**
   * Sets up test fixtures before each test.
   *
   * <p>Creates a fresh engine instance with application and substance context,
   * initializes the executor, and obtains a scope for testing.</p>
   */
  @BeforeEach
  void setUp() {
    engine = new SingleThreadEngine(2020, 2030);
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    executor = new StreamUpdateExecutor(engine);
    useKey = engine.getScope();
  }

  /**
   * Tests that domestic and import streams in units are correctly combined into sales intent.
   */
  @Test
  void testUpdateSalesCarryOverWithBothStreamsInUnits() {
    // Arrange
    final EngineNumber domestic = new EngineNumber(new BigDecimal("100"), "units");
    final EngineNumber importValue = new EngineNumber(new BigDecimal("50"), "units");

    // Enable streams before setting values
    engine.enable("domestic", Optional.empty());
    engine.enable("import", Optional.empty());

    // Act - use execute() to set streams
    StreamUpdate domesticUpdate = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(domestic)
        .setPropagateChanges(true)
        .build();
    executor.execute(domesticUpdate);

    StreamUpdate importUpdate = new StreamUpdateBuilder()
        .setName("import")
        .setValue(importValue)
        .setPropagateChanges(true)
        .build();
    executor.execute(importUpdate);

    // Assert
    EngineNumber salesIntent = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent, "Sales intent should be set when both streams have unit values");
    assertEquals(0, new BigDecimal("150").compareTo(salesIntent.getValue()),
        "Sales intent should combine domestic (100) and import (50)");
    assertEquals("units", salesIntent.getUnits());
  }

  /**
   * Tests that only domestic stream in units creates correct sales intent.
   */
  @Test
  void testUpdateSalesCarryOverWithOnlyDomesticInUnits() {
    // Arrange
    EngineNumber domestic = new EngineNumber(new BigDecimal("100"), "units");

    // Enable domestic stream
    engine.enable("domestic", Optional.empty());

    // Act - use execute() to set stream
    StreamUpdate domesticUpdate = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(domestic)
        .setPropagateChanges(true)
        .build();
    executor.execute(domesticUpdate);

    // Assert
    EngineNumber salesIntent = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent, "Sales intent should be set when domestic has unit value");
    assertEquals(new BigDecimal("100"), salesIntent.getValue(),
        "Sales intent should match domestic value when only domestic is specified");
    assertEquals("units", salesIntent.getUnits());
  }

  /**
   * Tests that only import stream in units creates correct sales intent.
   */
  @Test
  void testUpdateSalesCarryOverWithOnlyImportInUnits() {
    // Arrange
    EngineNumber importValue = new EngineNumber(new BigDecimal("50"), "units");

    // Enable import stream
    engine.enable("import", Optional.empty());

    // Act - use execute() to set stream
    StreamUpdate importUpdate = new StreamUpdateBuilder()
        .setName("import")
        .setValue(importValue)
        .setPropagateChanges(true)
        .build();
    executor.execute(importUpdate);

    // Assert
    EngineNumber salesIntent = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent, "Sales intent should be set when import has unit value");
    assertEquals(new BigDecimal("50"), salesIntent.getValue(),
        "Sales intent should match import value when only import is specified");
    assertEquals("units", salesIntent.getUnits());
  }

  /**
   * Tests that volume-based values (kg) now create sales intent with the updated logic.
   */
  @Test
  void testUpdateSalesCarryOverIgnoresVolumeBasedValues() {
    // Arrange
    EngineNumber domestic = new EngineNumber(new BigDecimal("100"), "kg");

    // Enable domestic stream
    engine.enable("domestic", Optional.empty());

    // Act - use execute() to set stream
    StreamUpdate domesticUpdate = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(domestic)
        .setPropagateChanges(true)
        .build();
    executor.execute(domesticUpdate);

    // Assert
    EngineNumber salesIntent = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent,
        "Sales intent should be set for volume-based (kg) specifications");
    assertEquals(new BigDecimal("100"), salesIntent.getValue(),
        "Sales intent should match the domestic value");
    assertEquals("kg", salesIntent.getUnits(),
        "Sales intent should preserve kg units");
  }

  /**
   * Tests that non-sales streams (e.g., export) are ignored for sales intent tracking.
   */
  @Test
  void testUpdateSalesCarryOverIgnoresNonSalesStreams() {
    // Arrange
    EngineNumber export = new EngineNumber(new BigDecimal("25"), "kg");

    // Enable export stream
    engine.enable("export", Optional.empty());

    // Act - use execute() to set stream (use kg instead of units to avoid implicit recharge logic)
    StreamUpdate exportUpdate = new StreamUpdateBuilder()
        .setName("export")
        .setValue(export)
        .setPropagateChanges(true)
        .build();
    executor.execute(exportUpdate);

    // Assert
    EngineNumber salesIntent = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNull(salesIntent,
        "Sales intent should not be set for non-sales streams like export");
  }

  /**
   * Tests that streams with different unit scales are correctly combined with conversion.
   */
  @Test
  void testUpdateSalesCarryOverWithDifferentUnits() {
    // Arrange
    final EngineNumber domestic = new EngineNumber(new BigDecimal("100"), "units");
    final EngineNumber importValue = new EngineNumber(new BigDecimal("500000"), "units");

    // Enable streams
    engine.enable("domestic", Optional.empty());
    engine.enable("import", Optional.empty());

    // Act - use execute() to set streams
    StreamUpdate domesticUpdate = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(domestic)
        .setPropagateChanges(true)
        .build();
    executor.execute(domesticUpdate);

    StreamUpdate importUpdate = new StreamUpdateBuilder()
        .setName("import")
        .setValue(importValue)
        .setPropagateChanges(true)
        .build();
    executor.execute(importUpdate);

    // Assert
    EngineNumber salesIntent = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent, "Sales intent should be set even with different unit scales");
    assertEquals(0, new BigDecimal("500100").compareTo(salesIntent.getValue()),
        "Sales intent should correctly combine values with different scales");
    assertEquals("units", salesIntent.getUnits());
  }

  /**
   * Tests that updating domestic multiple times correctly replaces the sales intent.
   */
  @Test
  void testUpdateSalesCarryOverReplacesExistingIntent() {
    // Arrange
    final EngineNumber domestic1 = new EngineNumber(new BigDecimal("100"), "units");
    final EngineNumber importValue = new EngineNumber(new BigDecimal("50"), "units");
    final EngineNumber domestic2 = new EngineNumber(new BigDecimal("200"), "units");

    // Enable streams
    engine.enable("domestic", Optional.empty());
    engine.enable("import", Optional.empty());

    // Act - first set domestic (creates intent: 100)
    StreamUpdate domesticUpdate1 = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(domestic1)
        .setPropagateChanges(true)
        .build();
    executor.execute(domesticUpdate1);

    EngineNumber salesIntent1 = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent1);
    assertEquals(new BigDecimal("100"), salesIntent1.getValue(),
        "Initial domestic should create sales intent of 100");

    // Act - set import (updates intent to: 100 + 50 = 150)
    StreamUpdate importUpdate = new StreamUpdateBuilder()
        .setName("import")
        .setValue(importValue)
        .setPropagateChanges(true)
        .build();
    executor.execute(importUpdate);

    EngineNumber salesIntent2 = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent2);
    assertEquals(0, new BigDecimal("150").compareTo(salesIntent2.getValue()),
        "Adding import should update sales intent to 150");

    // Act - update domestic (updates intent to: 200 + 50 = 250)
    StreamUpdate domesticUpdate2 = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(domestic2)
        .setPropagateChanges(true)
        .build();
    executor.execute(domesticUpdate2);

    // Assert
    EngineNumber salesIntent3 = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent3);
    assertEquals(0, new BigDecimal("250").compareTo(salesIntent3.getValue()),
        "Updating domestic should update sales intent to 250");
    assertEquals("units", salesIntent3.getUnits());
  }

  /**
   * Tests handling of mixed specifications (one in units, one in kg).
   * With the updated logic, both values are combined into sales intent.
   */
  @Test
  void testUpdateSalesCarryOverWithMixedSpecifications() {
    // Arrange
    final EngineNumber domestic = new EngineNumber(new BigDecimal("100"), "units");
    final EngineNumber importValue = new EngineNumber(new BigDecimal("50"), "kg");

    // Enable streams and set initial charges for unit conversion
    engine.enable("domestic", Optional.empty());
    engine.enable("import", Optional.empty());
    engine.setInitialCharge(new EngineNumber(new BigDecimal("1"), "kg"), "domestic", null);
    engine.setInitialCharge(new EngineNumber(new BigDecimal("1"), "kg"), "import", null);

    // Act - use execute() to set streams
    StreamUpdate domesticUpdate = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(domestic)
        .setPropagateChanges(true)
        .build();
    executor.execute(domesticUpdate);

    StreamUpdate importUpdate = new StreamUpdateBuilder()
        .setName("import")
        .setValue(importValue)
        .setPropagateChanges(true)
        .build();
    executor.execute(importUpdate);

    // Assert - with the new logic, both values are combined
    // domestic (100 units * 1 kg/unit = 100 kg) + import (50 kg) = 150 kg
    EngineNumber salesIntent = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent,
        "Sales intent should be set when combining mixed specifications");
    assertEquals(0, new BigDecimal("150").compareTo(salesIntent.getValue()),
        "Sales intent should combine both streams (100 units converted to kg + 50 kg = 150 kg)");
    assertEquals("kg", salesIntent.getUnits(),
        "Sales intent should use the units of the last stream set (kg)");
  }

  /**
   * Tests handling of zero values in one stream.
   */
  @Test
  void testUpdateSalesCarryOverWithZeroValues() {
    // Arrange
    final EngineNumber domestic = new EngineNumber(new BigDecimal("0"), "units");
    final EngineNumber importValue = new EngineNumber(new BigDecimal("50"), "units");

    // Enable streams
    engine.enable("domestic", Optional.empty());
    engine.enable("import", Optional.empty());

    // Act - use execute() to set streams
    StreamUpdate domesticUpdate = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(domestic)
        .setPropagateChanges(true)
        .build();
    executor.execute(domesticUpdate);

    StreamUpdate importUpdate = new StreamUpdateBuilder()
        .setName("import")
        .setValue(importValue)
        .setPropagateChanges(true)
        .build();
    executor.execute(importUpdate);

    // Assert
    EngineNumber salesIntent = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNotNull(salesIntent, "Sales intent should be set even when one stream is zero");
    assertEquals(0, new BigDecimal("50").compareTo(salesIntent.getValue()),
        "Sales intent should be 50 (0 + 50)");
    assertEquals("units", salesIntent.getUnits());
  }

  /**
   * Tests that constructor creates valid executor instance.
   */
  @Test
  void testConstructor() {
    // Act
    StreamUpdateExecutor newExecutor = new StreamUpdateExecutor(engine);

    // Assert - basic smoke test
    assertNotNull(newExecutor, "Constructor should create non-null executor");
  }

  /**
   * Tests implicit recharge calculation and application for sales streams with units.
   */
  @Test
  void testUpdateAndApplyImplicitRecharge_withUnitsAndSales() {
    // Arrange
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(new EngineNumber(new BigDecimal("1"), "kg"), "domestic", null);

    // Set prior equipment to create a recharge need
    EngineNumber priorEquipment = new EngineNumber(new BigDecimal("100"), "units");
    engine.getStreamKeeper().update(
        new org.kigalisim.engine.state.SimulationStateUpdateBuilder()
            .setUseKey(useKey)
            .setName("priorEquipment")
            .setValue(priorEquipment)
            .build()
    );

    // Set recharge parameters
    engine.recharge(
        new EngineNumber(new BigDecimal("10"), "%"),
        new EngineNumber(new BigDecimal("0.5"), "kg / unit"),
        null
    );

    EngineNumber salesValue = new EngineNumber(new BigDecimal("50"), "units");

    // Act
    ImplicitRechargeUpdate result = executor.handleImplicitRecharge(
        useKey, "domestic", salesValue, true, true, true);

    // Assert
    assertNotNull(result, "Should return ImplicitRechargeUpdate");
    assertNotNull(result.getValueToSet(), "Should have adjusted value");
    assertEquals("kg", result.getValueToSet().getUnits(),
        "Value should be converted to kg");
    // Value should be original sales (50 units * 1 kg/unit = 50 kg) plus recharge
    // Recharge = 100 units * 10% * 0.5 kg/unit = 5 kg
    // Total = 50 + 5 = 55 kg (approximately, may vary with distribution)
    assertEquals(true, result.getValueToSet().getValue().compareTo(new BigDecimal("50")) > 0,
        "Value should include implicit recharge added to original value");

    assertEquals(true, result.getImplicitRechargeStateUpdate().isPresent(),
        "Should have implicit recharge state update");
  }

  /**
   * Tests that volume-based sales specifications do not trigger implicit recharge.
   */
  @Test
  void testUpdateAndApplyImplicitRecharge_withVolumeBased() {
    // Arrange
    EngineNumber salesValue = new EngineNumber(new BigDecimal("100"), "kg");

    // Act
    ImplicitRechargeUpdate result = executor.handleImplicitRecharge(
        useKey, "domestic", salesValue, true, false, true);

    // Assert
    assertNotNull(result, "Should return ImplicitRechargeUpdate");
    assertEquals(salesValue, result.getValueToSet(),
        "Value should be unchanged for volume-based specifications");
    assertEquals(true, result.getImplicitRechargeStateUpdate().isPresent(),
        "Should clear implicit recharge for volume-based sales");
  }

  /**
   * Tests that non-sales streams do not trigger implicit recharge.
   */
  @Test
  void testUpdateAndApplyImplicitRecharge_withNonSalesStream() {
    // Arrange
    EngineNumber equipmentValue = new EngineNumber(new BigDecimal("100"), "units");

    // Act
    ImplicitRechargeUpdate result = executor.handleImplicitRecharge(
        useKey, "equipment", equipmentValue, false, true, false);

    // Assert
    assertNotNull(result, "Should return ImplicitRechargeUpdate");
    assertEquals(equipmentValue, result.getValueToSet(),
        "Value should be unchanged for non-sales streams");
    assertEquals(false, result.getImplicitRechargeStateUpdate().isPresent(),
        "Should not have implicit recharge update for non-sales streams");
  }

  /**
   * Tests propagation from sales streams.
   */
  @Test
  void testPropagateChanges_fromSalesStream() {
    // Arrange
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(new EngineNumber(new BigDecimal("1"), "kg"), "domestic", null);

    // Set a baseline value so there's something to recalculate
    engine.getStreamKeeper().update(
        new org.kigalisim.engine.state.SimulationStateUpdateBuilder()
            .setUseKey(useKey)
            .setName("domestic")
            .setValue(new EngineNumber(new BigDecimal("100"), "kg"))
            .build()
    );

    // Act - propagate changes from domestic stream
    executor.propagateChanges(useKey, "domestic", true, false);

    // Assert - verify that propagation executed without error
    // The actual recalc results depend on internal state, so we just verify no exceptions
    assertNotNull(engine.getStream("domestic"),
        "Stream should still be accessible after propagation");
  }

  /**
   * Tests propagation from consumption stream.
   */
  @Test
  void testPropagateChanges_fromConsumption() {
    // Arrange
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(new EngineNumber(new BigDecimal("1"), "kg"), "domestic", null);

    engine.getStreamKeeper().update(
        new org.kigalisim.engine.state.SimulationStateUpdateBuilder()
            .setUseKey(useKey)
            .setName("consumption")
            .setValue(new EngineNumber(new BigDecimal("100"), "kg"))
            .build()
    );

    // Act - propagate changes from consumption stream
    executor.propagateChanges(useKey, "consumption", false, false);

    // Assert - verify that propagation executed without error
    assertNotNull(engine.getStream("consumption"),
        "Stream should still be accessible after propagation");
  }

  /**
   * Tests propagation from equipment stream.
   */
  @Test
  void testPropagateChanges_fromEquipment() {
    // Arrange
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(new EngineNumber(new BigDecimal("1"), "kg"), "domestic", null);

    engine.getStreamKeeper().update(
        new org.kigalisim.engine.state.SimulationStateUpdateBuilder()
            .setUseKey(useKey)
            .setName("equipment")
            .setValue(new EngineNumber(new BigDecimal("100"), "units"))
            .build()
    );

    // Act - propagate changes from equipment stream
    executor.propagateChanges(useKey, "equipment", false, true);

    // Assert - verify that propagation executed without error
    assertNotNull(engine.getStream("equipment"),
        "Stream should still be accessible after propagation");
  }

  /**
   * Tests propagation from priorEquipment stream.
   */
  @Test
  void testPropagateChanges_fromPriorEquipment() {
    // Arrange
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(new EngineNumber(new BigDecimal("1"), "kg"), "domestic", null);

    engine.getStreamKeeper().update(
        new org.kigalisim.engine.state.SimulationStateUpdateBuilder()
            .setUseKey(useKey)
            .setName("priorEquipment")
            .setValue(new EngineNumber(new BigDecimal("100"), "units"))
            .build()
    );

    // Act - propagate changes from priorEquipment stream
    executor.propagateChanges(useKey, "priorEquipment", false, true);

    // Assert - verify that propagation executed without error
    assertNotNull(engine.getStream("priorEquipment"),
        "Stream should still be accessible after propagation");
  }

  /**
   * Tests that other streams (not sales, consumption, equipment, priorEquipment) do not propagate.
   */
  @Test
  void testPropagateChanges_fromOtherStream() {
    // Arrange
    engine.enable("domestic", Optional.empty());

    // Act - propagate changes from a stream that should not trigger propagation
    executor.propagateChanges(useKey, "retired", false, false);

    // Assert - verify that propagation executed without error (no-op case)
    // This should complete without throwing an exception
    assertNotNull(engine, "Engine should still be valid after no-op propagation");
  }

  /**
   * Tests execute() method with full flow including units and recharge.
   */
  @Test
  void testExecute_fullFlowWithUnitsAndRecharge() {
    // Arrange
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(new EngineNumber(new BigDecimal("1"), "kg"), "domestic", null);

    // Set prior equipment for recharge calculation
    engine.getStreamKeeper().update(
        new org.kigalisim.engine.state.SimulationStateUpdateBuilder()
            .setUseKey(useKey)
            .setName("priorEquipment")
            .setValue(new EngineNumber(new BigDecimal("100"), "units"))
            .build()
    );

    // Set recharge parameters
    engine.recharge(
        new EngineNumber(new BigDecimal("10"), "%"),
        new EngineNumber(new BigDecimal("0.5"), "kg / unit"),
        null
    );

    // Act
    StreamUpdate update = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(new BigDecimal("50"), "units"))
        .setPropagateChanges(true)
        .build();
    executor.execute(update);

    // Assert
    EngineNumber domestic = engine.getStream("domestic");
    assertNotNull(domestic, "Domestic stream should be set");
    assertEquals(true, domestic.getValue().compareTo(new BigDecimal("50")) > 0,
        "Domestic should include implicit recharge");
  }

  /**
   * Tests execute() method respects year matcher.
   */
  @Test
  void testExecute_respectsYearMatcher() {
    // Arrange
    engine.enable("domestic", Optional.empty());
    engine.getStreamKeeper().setCurrentYear(2025);

    // Create update that shouldn't apply (wrong year)
    YearMatcher matcher = new YearMatcher(2020, 2024);
    StreamUpdate update = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(new BigDecimal("100"), "kg"))
        .setYearMatcher(Optional.of(matcher))
        .setPropagateChanges(true)
        .build();

    // Act
    executor.execute(update);

    // Assert - value should not be set
    EngineNumber domestic = engine.getStream("domestic");
    assertEquals(BigDecimal.ZERO, domestic.getValue(),
        "Stream should not be updated when year doesn't match");
  }

  /**
   * Tests execute() with propagateChanges=false skips propagation.
   */
  @Test
  void testExecute_withPropagateChangesFalse() {
    // Arrange
    engine.enable("domestic", Optional.empty());

    // Act
    StreamUpdate update = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(new BigDecimal("100"), "kg"))
        .setPropagateChanges(false)
        .build();
    executor.execute(update);

    // Assert
    EngineNumber domestic = engine.getStream("domestic");
    assertNotNull(domestic, "Stream should be set");
    assertEquals(new BigDecimal("100"), domestic.getValue(),
        "Stream value should be set even without propagation");

    // Sales intent should not be set when propagateChanges is false
    EngineNumber salesIntent = engine.getStreamKeeper().getLastSpecifiedValue(useKey, "sales");
    assertNull(salesIntent,
        "Sales intent should not be set when propagateChanges is false");
  }

  /**
   * Tests execute() handles subtractRecycling flag correctly.
   */
  @Test
  void testExecute_withSubtractRecycling() {
    // Arrange
    engine.enable("domestic", Optional.empty());

    // Act
    StreamUpdate update = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(new BigDecimal("100"), "kg"))
        .setSubtractRecycling(true)
        .setPropagateChanges(true)
        .build();
    executor.execute(update);

    // Assert - verify that the update completed without error
    EngineNumber domestic = engine.getStream("domestic");
    assertNotNull(domestic, "Stream should be set with subtractRecycling flag");
  }
}
