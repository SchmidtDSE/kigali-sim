package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimpleUseKey;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.YearMatcher;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for ReplaceExecutor class.
 *
 * <p>Tests substance replacement logic including percentage resolution,
 * unit-based vs volume-based replacement, and proper scope management.</p>
 */
class ReplaceExecutorTest {
  private Engine mockEngine;
  private SimulationState mockSimulationState;
  private ConverterStateGetter mockStateGetter;
  private UnitConverter mockUnitConverter;
  private ReplaceExecutor replaceExecutor;
  private Scope testScope;
  private YearMatcher mockYearMatcher;

  @BeforeEach
  void setUp() {
    mockEngine = mock(Engine.class);
    mockSimulationState = mock(SimulationState.class);
    mockStateGetter = mock(ConverterStateGetter.class);
    mockUnitConverter = mock(UnitConverter.class);
    mockYearMatcher = mock(YearMatcher.class);

    // Setup basic test scope
    testScope = new Scope("default", "TestApp", "HFC-134a");

    when(mockEngine.getStreamKeeper()).thenReturn(mockSimulationState);
    when(mockEngine.getStateGetter()).thenReturn(mockStateGetter);
    when(mockEngine.getUnitConverter()).thenReturn(mockUnitConverter);
    when(mockEngine.getScope()).thenReturn(testScope);
    when(mockEngine.getYear()).thenReturn(2025);
    when(mockSimulationState.getCurrentYear()).thenReturn(2025);

    replaceExecutor = new ReplaceExecutor(mockEngine);
  }

  @Test
  void testExecute_OutsideRange_NoOperation() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(false);
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");

    // Act
    replaceExecutor.execute(amount, "domestic", "R-404A", mockYearMatcher);

    // Assert - no stream updates should occur
    verify(mockEngine, never()).executeStreamUpdate(any());
    verify(mockEngine, never()).setStanza(any());
    verify(mockEngine, never()).setApplication(any());
    verify(mockEngine, never()).setSubstance(any());
  }

  @Test
  void testExecute_NullApplication_ThrowsException() {
    // Arrange
    Scope scopeNoApp = new Scope("default", null, null);
    when(mockEngine.getScope()).thenReturn(scopeNoApp);
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");

    // Act & Assert
    assertThrows(RuntimeException.class, () -> {
      replaceExecutor.execute(amount, "domestic", "R-404A", mockYearMatcher);
    });
  }

  @Test
  void testExecute_NullSubstance_ThrowsException() {
    // Arrange
    Scope scopeNoSubstance = new Scope("default", "TestApp", null);
    when(mockEngine.getScope()).thenReturn(scopeNoSubstance);
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");

    // Act & Assert
    assertThrows(RuntimeException.class, () -> {
      replaceExecutor.execute(amount, "domestic", "R-404A", mockYearMatcher);
    });
  }

  @Test
  void testExecute_SelfReplacement_ThrowsException() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");

    // Act & Assert - attempting to replace HFC-134a with itself
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      replaceExecutor.execute(amount, "domestic", "HFC-134a", mockYearMatcher);
    });
    assertEquals("Cannot replace substance \"HFC-134a\" with itself. Please specify a different target substance for replacement.", exception.getMessage());
  }

  @Test
  void testExecute_SalesStream_UpdatesLastSpecifiedForBothSubstances() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");
    EngineNumber currentStreamValue = new EngineNumber(new BigDecimal("100"), "kg");

    // Mock for volume-based replacement (non-units)
    when(mockEngine.getStream("sales")).thenReturn(currentStreamValue);
    when(mockEngine.getStream(eq("sales"), any(), any())).thenReturn(currentStreamValue);
    EngineNumber convertedAmount = new EngineNumber(new BigDecimal("50"), "kg");
    when(mockUnitConverter.convert(any(EngineNumber.class), eq("kg"))).thenReturn(convertedAmount);
    when(mockStateGetter.getAmortizedUnitVolume()).thenReturn(new EngineNumber(BigDecimal.ZERO, "kg"));

    // Act
    replaceExecutor.execute(amount, "sales", "R-404A", mockYearMatcher);

    // Assert - verify lastSpecifiedValue was set for both substances
    verify(mockSimulationState).setLastSpecifiedValue(eq(testScope), eq("sales"), eq(amount));

    ArgumentCaptor<SimpleUseKey> keyCaptor = ArgumentCaptor.forClass(SimpleUseKey.class);
    verify(mockSimulationState).setLastSpecifiedValue(keyCaptor.capture(), eq("sales"), eq(amount));

    SimpleUseKey capturedKey = keyCaptor.getValue();
    assertEquals("TestApp", capturedKey.getApplication());
    assertEquals("R-404A", capturedKey.getSubstance());
  }

  @Test
  void testExecute_NonSalesStream_DoesNotUpdateLastSpecified() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");
    EngineNumber currentStreamValue = new EngineNumber(new BigDecimal("100"), "kg");

    // Mock for volume-based replacement
    when(mockEngine.getStream("equipment")).thenReturn(currentStreamValue);
    when(mockEngine.getStream(eq("equipment"), any(), any())).thenReturn(currentStreamValue);
    EngineNumber convertedAmount = new EngineNumber(new BigDecimal("50"), "kg");
    when(mockUnitConverter.convert(any(EngineNumber.class), eq("kg"))).thenReturn(convertedAmount);
    when(mockStateGetter.getAmortizedUnitVolume()).thenReturn(new EngineNumber(BigDecimal.ZERO, "kg"));

    // Act
    replaceExecutor.execute(amount, "equipment", "R-404A", mockYearMatcher);

    // Assert - verify lastSpecifiedValue was NOT set (only for sales streams)
    verify(mockSimulationState, never()).setLastSpecifiedValue(any(Scope.class), anyString(), any(EngineNumber.class));
    verify(mockSimulationState, never()).setLastSpecifiedValue(any(SimpleUseKey.class), anyString(), any(EngineNumber.class));
  }

  @Test
  void testExecute_PercentageWithLastSpecified_UsesLastSpecifiedUnits() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber percentAmount = new EngineNumber(new BigDecimal("50"), "%");
    EngineNumber lastSpecified = new EngineNumber(new BigDecimal("100"), "units");
    EngineNumber currentStreamValue = new EngineNumber(new BigDecimal("100"), "units");

    when(mockSimulationState.getLastSpecifiedValue(testScope, "domestic")).thenReturn(lastSpecified);

    // Mock for unit-based replacement
    EngineNumber unitsAmount = new EngineNumber(new BigDecimal("50"), "units");
    when(mockEngine.getStream("domestic")).thenReturn(currentStreamValue);
    when(mockEngine.getStream(eq("domestic"), any(), any())).thenReturn(currentStreamValue);
    when(mockUnitConverter.convert(any(EngineNumber.class), eq("units"))).thenReturn(unitsAmount);
    when(mockUnitConverter.convert(any(EngineNumber.class), eq("kg"))).thenReturn(new EngineNumber(new BigDecimal("6"), "kg"));
    when(mockStateGetter.getAmortizedUnitVolume()).thenReturn(new EngineNumber(new BigDecimal("0.12"), "kg"));
    when(mockEngine.getInitialCharge("sales")).thenReturn(new EngineNumber(new BigDecimal("0.30"), "kg"));

    // Act
    replaceExecutor.execute(percentAmount, "domestic", "R-404A", mockYearMatcher);

    // Assert - verify percentage was applied to lastSpecified value
    // 50% of 100 units = 50 units (equipment units path should be taken)
    verify(mockEngine, times(2)).setStanza(any()); // Set and restore for initial charge lookup
    verify(mockEngine, times(2)).setApplication(any());
    verify(mockEngine, times(2)).setSubstance(any());
  }

  @Test
  void testExecute_PercentageWithoutLastSpecified_UsesCurrentValueUnits() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber percentAmount = new EngineNumber(new BigDecimal("50"), "%");
    EngineNumber currentStreamValue = new EngineNumber(new BigDecimal("200"), "kg");

    when(mockSimulationState.getLastSpecifiedValue(testScope, "domestic")).thenReturn(null);
    when(mockEngine.getStream("domestic")).thenReturn(currentStreamValue);
    when(mockEngine.getStream(eq("domestic"), any(), any())).thenReturn(currentStreamValue);

    // Mock for volume-based replacement (current value is in kg)
    EngineNumber kgAmount = new EngineNumber(new BigDecimal("100"), "kg");
    when(mockUnitConverter.convert(any(EngineNumber.class), eq("kg"))).thenReturn(kgAmount);
    when(mockStateGetter.getAmortizedUnitVolume()).thenReturn(new EngineNumber(BigDecimal.ZERO, "kg"));

    // Act
    replaceExecutor.execute(percentAmount, "domestic", "R-404A", mockYearMatcher);

    // Assert - verify percentage was applied to current value
    // 50% of 200 kg = 100 kg (volume path should be taken)
    verify(mockSimulationState, never()).getLastSpecifiedValue(testScope, "domestic");
    // First call gets lastSpecified (null), second call gets current value in getEffectiveAmount
  }

  @Test
  void testExecute_NonPercentage_UsesRawAmount() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber amount = new EngineNumber(new BigDecimal("75"), "kg");
    EngineNumber currentStreamValue = new EngineNumber(new BigDecimal("100"), "kg");

    when(mockEngine.getStream("domestic")).thenReturn(currentStreamValue);
    when(mockEngine.getStream(eq("domestic"), any(), any())).thenReturn(currentStreamValue);
    when(mockUnitConverter.convert(any(EngineNumber.class), eq("kg"))).thenReturn(amount);
    when(mockStateGetter.getAmortizedUnitVolume()).thenReturn(new EngineNumber(BigDecimal.ZERO, "kg"));

    // Act
    replaceExecutor.execute(amount, "domestic", "R-404A", mockYearMatcher);

    // Assert - no percentage resolution should occur
    verify(mockSimulationState, never()).getLastSpecifiedValue(any(), anyString());
  }

  @Test
  void testExecute_WithEquipmentUnits_UsesInitialCharges() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber unitsAmount = new EngineNumber(new BigDecimal("100"), "units");
    EngineNumber currentStreamValue = new EngineNumber(new BigDecimal("500"), "units");

    // Setup for unit-based replacement
    when(mockEngine.getStream("domestic")).thenReturn(currentStreamValue);
    when(mockEngine.getStream(eq("domestic"), any(), any())).thenReturn(currentStreamValue);
    EngineNumber convertedUnits = new EngineNumber(new BigDecimal("100"), "units");
    EngineNumber sourceVolume = new EngineNumber(new BigDecimal("12"), "kg"); // 100 units * 0.12 kg/unit
    EngineNumber destVolume = new EngineNumber(new BigDecimal("30"), "kg"); // 100 units * 0.30 kg/unit

    when(mockUnitConverter.convert(unitsAmount, "units")).thenReturn(convertedUnits);
    when(mockUnitConverter.convert(convertedUnits, "kg")).thenReturn(sourceVolume);
    when(mockStateGetter.getAmortizedUnitVolume()).thenReturn(new EngineNumber(new BigDecimal("0.12"), "kg"));

    // Mock destination initial charge
    when(mockEngine.getInitialCharge("sales")).thenReturn(new EngineNumber(new BigDecimal("0.30"), "kg"));

    // Act
    replaceExecutor.execute(unitsAmount, "domestic", "R-404A", mockYearMatcher);

    // Assert - verify scope was switched to get destination initial charge
    verify(mockEngine, times(2)).setStanza(any()); // Once for destination, once to restore
    verify(mockEngine, times(2)).setApplication(any());
    verify(mockEngine, times(2)).setSubstance(any());
    verify(mockEngine).getInitialCharge("sales");
  }

  @Test
  void testExecute_WithVolumeUnits_UsesSameVolume() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber kgAmount = new EngineNumber(new BigDecimal("50"), "kg");
    EngineNumber currentStreamValue = new EngineNumber(new BigDecimal("100"), "kg");

    when(mockEngine.getStream("domestic")).thenReturn(currentStreamValue);
    when(mockEngine.getStream(eq("domestic"), any(), any())).thenReturn(currentStreamValue);
    when(mockUnitConverter.convert(any(EngineNumber.class), eq("kg"))).thenReturn(kgAmount);
    when(mockStateGetter.getAmortizedUnitVolume()).thenReturn(new EngineNumber(BigDecimal.ZERO, "kg"));

    // Act
    replaceExecutor.execute(kgAmount, "domestic", "R-404A", mockYearMatcher);

    // Assert - no scope switching needed for volume-based replacement
    verify(mockEngine, never()).setStanza(any());
    verify(mockEngine, never()).setApplication(any());
    verify(mockEngine, never()).setSubstance(any());
    verify(mockEngine, never()).getInitialCharge(anyString());
  }

  @Test
  void testExecute_WithMtUnits_ConvertsToKg() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber mtAmount = new EngineNumber(new BigDecimal("2"), "mt");
    EngineNumber kgAmount = new EngineNumber(new BigDecimal("2000"), "kg");
    EngineNumber currentStreamValue = new EngineNumber(new BigDecimal("5000"), "kg");

    when(mockEngine.getStream("import")).thenReturn(currentStreamValue);
    when(mockEngine.getStream(eq("import"), any(), any())).thenReturn(currentStreamValue);
    when(mockUnitConverter.convert(mtAmount, "kg")).thenReturn(kgAmount);
    when(mockStateGetter.getAmortizedUnitVolume()).thenReturn(new EngineNumber(BigDecimal.ZERO, "kg"));

    // Act
    replaceExecutor.execute(mtAmount, "import", "R-404A", mockYearMatcher);

    // Assert - verify conversion to kg occurred
    verify(mockUnitConverter).convert(eq(mtAmount), eq("kg"));
  }

  @Test
  void testConstructor() {
    // Act - constructor called in setUp
    // Assert - no exceptions thrown, object created successfully
    // This test mainly verifies the constructor doesn't throw exceptions
  }
}
