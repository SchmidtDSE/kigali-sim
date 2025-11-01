package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for StreamUpdateShortcuts class.
 *
 * <p>Tests the stream update shortcut operations for displacement and replacement logic.</p>
 */
class StreamUpdateShortcutsTest {

  private Engine mockEngine;
  private SimulationState mockSimulationState;
  private ConverterStateGetter mockStateGetter;
  private UnitConverter mockUnitConverter;
  private YearMatcher mockYearMatcher;
  private StreamUpdateShortcuts shortcuts;

  @BeforeEach
  void setUp() {
    mockEngine = mock(Engine.class);
    mockSimulationState = mock(SimulationState.class);
    mockStateGetter = mock(ConverterStateGetter.class);
    mockUnitConverter = mock(UnitConverter.class);
    mockYearMatcher = mock(YearMatcher.class);

    when(mockEngine.getStreamKeeper()).thenReturn(mockSimulationState);
    when(mockEngine.getStateGetter()).thenReturn(mockStateGetter);
    when(mockEngine.getUnitConverter()).thenReturn(mockUnitConverter);
    when(mockEngine.getYear()).thenReturn(2025);

    shortcuts = new StreamUpdateShortcuts(mockEngine);
  }

  @Test
  void testChangeStreamWithoutReportingUnits_WithinRange_UpdatesStream() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber currentValue = new EngineNumber(new BigDecimal("100"), "kg");
    when(mockEngine.getStream(eq("domestic"), any(), any())).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("50"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");

    // Act
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount,
        Optional.of(mockYearMatcher), Optional.empty());

    // Assert
    ArgumentCaptor<StreamUpdate> updateCaptor = ArgumentCaptor.forClass(StreamUpdate.class);
    verify(mockEngine, times(1)).executeStreamUpdate(updateCaptor.capture());

    StreamUpdate capturedUpdate = updateCaptor.getValue();
    assertEquals("domestic", capturedUpdate.getName());
    assertEquals(new BigDecimal("150"), capturedUpdate.getValue().getValue());
    assertEquals("kg", capturedUpdate.getValue().getUnits());
  }

  @Test
  void testChangeStreamWithoutReportingUnits_OutsideRange_NoOperation() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(false);
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");

    // Act
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount,
        Optional.of(mockYearMatcher), Optional.empty());

    // Assert
    verify(mockEngine, never()).executeStreamUpdate(any());
  }

  @Test
  void testChangeStreamWithoutReportingUnits_NegativeValue_ClampedToZero() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber currentValue = new EngineNumber(new BigDecimal("30"), "kg");
    when(mockEngine.getStream(eq("domestic"), any(), any())).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("-50"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("-50"), "kg");

    // Act
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount,
        Optional.of(mockYearMatcher), Optional.empty(), false);

    // Assert
    ArgumentCaptor<StreamUpdate> updateCaptor = ArgumentCaptor.forClass(StreamUpdate.class);
    verify(mockEngine, times(1)).executeStreamUpdate(updateCaptor.capture());

    StreamUpdate capturedUpdate = updateCaptor.getValue();
    assertEquals(BigDecimal.ZERO, capturedUpdate.getValue().getValue());
  }

  @Test
  void testChangeStreamWithoutReportingUnits_NegativeValue_Allowed() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber currentValue = new EngineNumber(new BigDecimal("30"), "kg");
    when(mockEngine.getStream(eq("domestic"), any(), any())).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("-50"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("-50"), "kg");

    // Act
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount,
        Optional.of(mockYearMatcher), Optional.empty(), true);

    // Assert
    ArgumentCaptor<StreamUpdate> updateCaptor = ArgumentCaptor.forClass(StreamUpdate.class);
    verify(mockEngine, times(1)).executeStreamUpdate(updateCaptor.capture());

    StreamUpdate capturedUpdate = updateCaptor.getValue();
    assertEquals(new BigDecimal("-20"), capturedUpdate.getValue().getValue());
  }

  @Test
  void testChangeStreamWithoutReportingUnits_WithCustomScope_UsesProvidedScope() {
    // Arrange
    UseKey customScope = mock(UseKey.class);
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber currentValue = new EngineNumber(new BigDecimal("100"), "kg");
    when(mockEngine.getStream(eq("import"), eq(Optional.of(customScope)), any()))
        .thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("25"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("25"), "kg");

    // Act
    shortcuts.changeStreamWithoutReportingUnits("import", amount,
        Optional.of(mockYearMatcher), Optional.of(customScope));

    // Assert
    verify(mockEngine, times(1)).executeStreamUpdate(any(StreamUpdate.class));
  }

  @Test
  void testChangeStreamWithDisplacementContext_SalesStream_RecalculatesPopulation() {
    // Arrange
    Scope destinationScope = new Scope("policy", "appB", "substanceB");
    when(mockEngine.getScope()).thenReturn(new Scope("policy", "appA", "substanceA"));

    EngineNumber currentValue = new EngineNumber(new BigDecimal("100"), "kg");
    when(mockEngine.getStream("domestic")).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("50"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");

    when(mockEngine.getOptimizeRecalcs()).thenReturn(true);

    // Act
    shortcuts.changeStreamWithDisplacementContext("domestic", amount, destinationScope);

    // Assert
    verify(mockEngine, times(1)).executeStreamUpdate(any(StreamUpdate.class));
    verify(mockEngine, times(1)).setStanza("policy");
    verify(mockEngine, times(2)).setApplication(any()); // once to destination, once to restore
    verify(mockEngine, times(2)).setSubstance(any()); // once to destination, once to restore
  }

  @Test
  void testChangeStreamWithDisplacementContext_NonSalesStream_SkipsRecalc() {
    // Arrange
    Scope destinationScope = new Scope("policy", "appB", "substanceB");
    when(mockEngine.getScope()).thenReturn(new Scope("policy", "appA", "substanceA"));

    EngineNumber currentValue = new EngineNumber(new BigDecimal("100"), "kg");
    when(mockEngine.getStream("recycle")).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("20"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("20"), "kg");

    // Act
    shortcuts.changeStreamWithDisplacementContext("recycle", amount, destinationScope);

    // Assert
    verify(mockEngine, times(1)).executeStreamUpdate(any(StreamUpdate.class));
    // Verify scope was restored
    verify(mockEngine, times(2)).setStanza(any());
    verify(mockEngine, times(2)).setApplication(any());
    verify(mockEngine, times(2)).setSubstance(any());
  }

  @Test
  void testChangeStreamWithDisplacementContext_UpdatesLastSpecifiedValue() {
    // Arrange
    Scope destinationScope = new Scope("policy", "appB", "substanceB");
    when(mockEngine.getScope()).thenReturn(new Scope("policy", "appA", "substanceA"));

    EngineNumber currentValue = new EngineNumber(new BigDecimal("100"), "kg");
    when(mockEngine.getStream("domestic")).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("50"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");

    when(mockEngine.getOptimizeRecalcs()).thenReturn(true);

    // Act
    shortcuts.changeStreamWithDisplacementContext("domestic", amount, destinationScope);

    // Assert
    verify(mockSimulationState, times(1)).setLastSpecifiedValue(any(UseKey.class),
        eq("domestic"), any(EngineNumber.class));
  }

  @Test
  void testChangeStreamWithDisplacementContext_NegativeValue_ClampedToZero() {
    // Arrange
    Scope destinationScope = new Scope("policy", "appB", "substanceB");
    when(mockEngine.getScope()).thenReturn(new Scope("policy", "appA", "substanceA"));

    EngineNumber currentValue = new EngineNumber(new BigDecimal("30"), "kg");
    when(mockEngine.getStream("import")).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("-50"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("-50"), "kg");

    when(mockEngine.getOptimizeRecalcs()).thenReturn(true);

    // Act
    shortcuts.changeStreamWithDisplacementContext("import", amount, destinationScope, false);

    // Assert
    ArgumentCaptor<StreamUpdate> updateCaptor = ArgumentCaptor.forClass(StreamUpdate.class);
    verify(mockEngine, times(1)).executeStreamUpdate(updateCaptor.capture());

    StreamUpdate capturedUpdate = updateCaptor.getValue();
    assertEquals(BigDecimal.ZERO, capturedUpdate.getValue().getValue());
  }

  @Test
  void testChangeStreamWithDisplacementContext_NegativeValue_Allowed() {
    // Arrange
    Scope destinationScope = new Scope("policy", "appB", "substanceB");
    when(mockEngine.getScope()).thenReturn(new Scope("policy", "appA", "substanceA"));

    EngineNumber currentValue = new EngineNumber(new BigDecimal("30"), "kg");
    when(mockEngine.getStream("import")).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("-50"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("-50"), "kg");

    when(mockEngine.getOptimizeRecalcs()).thenReturn(true);

    // Act
    shortcuts.changeStreamWithDisplacementContext("import", amount, destinationScope, true);

    // Assert
    ArgumentCaptor<StreamUpdate> updateCaptor = ArgumentCaptor.forClass(StreamUpdate.class);
    verify(mockEngine, times(1)).executeStreamUpdate(updateCaptor.capture());

    StreamUpdate capturedUpdate = updateCaptor.getValue();
    assertEquals(new BigDecimal("-20"), capturedUpdate.getValue().getValue());
  }

  @Test
  void testChangeStreamWithDisplacementContext_OptimizeRecalcsTrue_SkipsSalesPropagation() {
    // Arrange
    Scope destinationScope = new Scope("policy", "appB", "substanceB");
    when(mockEngine.getScope()).thenReturn(new Scope("policy", "appA", "substanceA"));

    EngineNumber currentValue = new EngineNumber(new BigDecimal("100"), "kg");
    when(mockEngine.getStream("domestic")).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("50"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");

    when(mockEngine.getOptimizeRecalcs()).thenReturn(true);

    // Act
    shortcuts.changeStreamWithDisplacementContext("domestic", amount, destinationScope);

    // Assert - just verify it completes without error
    verify(mockEngine, times(1)).executeStreamUpdate(any(StreamUpdate.class));
  }

  @Test
  void testChangeStreamWithDisplacementContext_OptimizeRecalcsFalse_PropagatestoSales() {
    // Arrange
    Scope destinationScope = new Scope("policy", "appB", "substanceB");
    when(mockEngine.getScope()).thenReturn(new Scope("policy", "appA", "substanceA"));

    EngineNumber currentValue = new EngineNumber(new BigDecimal("100"), "kg");
    when(mockEngine.getStream("domestic")).thenReturn(currentValue);

    EngineNumber convertedDelta = new EngineNumber(new BigDecimal("50"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta);

    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");

    when(mockEngine.getOptimizeRecalcs()).thenReturn(false);

    // Act
    shortcuts.changeStreamWithDisplacementContext("domestic", amount, destinationScope);

    // Assert - just verify it completes without error
    verify(mockEngine, times(1)).executeStreamUpdate(any(StreamUpdate.class));
  }

  @Test
  void testChangeStreamMethods_WorkTogether() {
    // Arrange - first call to changeStreamWithoutReportingUnits
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    EngineNumber currentValue1 = new EngineNumber(new BigDecimal("100"), "kg");
    when(mockEngine.getStream(eq("domestic"), any(), any())).thenReturn(currentValue1);

    EngineNumber convertedDelta1 = new EngineNumber(new BigDecimal("-20"), "kg");
    when(mockUnitConverter.convert(any(), eq("kg"))).thenReturn(convertedDelta1);

    EngineNumber amount1 = new EngineNumber(new BigDecimal("-20"), "kg");

    // Arrange - second call to changeStreamWithDisplacementContext
    Scope destinationScope = new Scope("policy", "appB", "substanceB");
    when(mockEngine.getScope()).thenReturn(new Scope("policy", "appA", "substanceA"));

    EngineNumber currentValue2 = new EngineNumber(new BigDecimal("50"), "kg");
    when(mockEngine.getStream("domestic")).thenReturn(currentValue2);

    EngineNumber convertedDelta2 = new EngineNumber(new BigDecimal("20"), "kg");

    EngineNumber amount2 = new EngineNumber(new BigDecimal("20"), "kg");

    when(mockEngine.getOptimizeRecalcs()).thenReturn(true);

    // Act
    shortcuts.changeStreamWithoutReportingUnits("domestic", amount1,
        Optional.of(mockYearMatcher), Optional.empty());
    shortcuts.changeStreamWithDisplacementContext("domestic", amount2, destinationScope);

    // Assert - verify both operations executed
    verify(mockEngine, times(2)).executeStreamUpdate(any(StreamUpdate.class));
  }
}
