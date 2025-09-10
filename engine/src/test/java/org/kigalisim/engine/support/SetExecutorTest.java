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
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for SetExecutor class.
 *
 * <p>Tests the sales distribution logic and proper delegation to engine methods.</p>
 */
class SetExecutorTest {

  private Engine mockEngine;
  private StreamKeeper mockStreamKeeper;
  private UseKey mockUseKey;
  private YearMatcher mockYearMatcher;
  private SetExecutor setExecutor;

  @BeforeEach
  void setUp() {
    mockEngine = mock(Engine.class);
    mockStreamKeeper = mock(StreamKeeper.class);
    mockUseKey = mock(UseKey.class);
    mockYearMatcher = mock(YearMatcher.class);

    when(mockEngine.getStreamKeeper()).thenReturn(mockStreamKeeper);
    when(mockEngine.getYear()).thenReturn(2025);

    setExecutor = new SetExecutor(mockEngine);
  }

  @Test
  void testHandleSalesSetWithEqualDistribution() {
    // Arrange
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.5"), // 50% domestic
        new BigDecimal("0.5")  // 50% import
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    when(mockYearMatcher.getInRange(2025)).thenReturn(true);
    
    EngineNumber value = new EngineNumber(new BigDecimal("10"), "mt");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.of(mockYearMatcher));

    // Assert - capture the arguments to verify behavior
    ArgumentCaptor<String> streamCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<EngineNumber> valueCaptor = ArgumentCaptor.forClass(EngineNumber.class);
    ArgumentCaptor<Optional> matcherCaptor = ArgumentCaptor.forClass(Optional.class);
    
    verify(mockEngine, times(2)).setStream(streamCaptor.capture(), valueCaptor.capture(), matcherCaptor.capture());
    
    // Check domestic call
    assertEquals("domestic", streamCaptor.getAllValues().get(0));
    assertEquals(new BigDecimal("5.0"), valueCaptor.getAllValues().get(0).getValue());
    assertEquals("mt", valueCaptor.getAllValues().get(0).getUnits());
    
    // Check import call
    assertEquals("import", streamCaptor.getAllValues().get(1));
    assertEquals(new BigDecimal("5.0"), valueCaptor.getAllValues().get(1).getValue());
    assertEquals("mt", valueCaptor.getAllValues().get(1).getUnits());
  }

  @Test  
  void testHandleSalesSetWithDomesticOnly() {
    // Arrange
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("1.0"), // 100% domestic
        new BigDecimal("0.0")  // 0% import
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    
    EngineNumber value = new EngineNumber(new BigDecimal("20"), "kg");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.empty());

    // Assert - only domestic should be called
    ArgumentCaptor<String> streamCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<EngineNumber> valueCaptor = ArgumentCaptor.forClass(EngineNumber.class);
    
    verify(mockEngine, times(1)).setStream(streamCaptor.capture(), valueCaptor.capture(), any());
    
    assertEquals("domestic", streamCaptor.getValue());
    assertEquals(new BigDecimal("20.0"), valueCaptor.getValue().getValue());
    assertEquals("kg", valueCaptor.getValue().getUnits());
  }

  @Test
  void testHandleSalesSetWithUnitsBasedValue() {
    // Arrange
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.7"), // 70% domestic
        new BigDecimal("0.3")  // 30% import
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    
    EngineNumber value = new EngineNumber(new BigDecimal("1000"), "units");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.empty());

    // Assert
    ArgumentCaptor<String> streamCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<EngineNumber> valueCaptor = ArgumentCaptor.forClass(EngineNumber.class);
    
    verify(mockEngine, times(2)).setStream(streamCaptor.capture(), valueCaptor.capture(), any());
    
    // Check domestic call (70% of 1000 = 700)
    assertEquals("domestic", streamCaptor.getAllValues().get(0));
    assertEquals(new BigDecimal("700.0"), valueCaptor.getAllValues().get(0).getValue());
    assertEquals("units", valueCaptor.getAllValues().get(0).getUnits());
    
    // Check import call (30% of 1000 = 300)
    assertEquals("import", streamCaptor.getAllValues().get(1));
    assertEquals(new BigDecimal("300.0"), valueCaptor.getAllValues().get(1).getValue());
    assertEquals("units", valueCaptor.getAllValues().get(1).getUnits());
  }

  @Test
  void testHandleSalesSetOutOfYearRange() {
    // Arrange
    when(mockYearMatcher.getInRange(2025)).thenReturn(false);
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.5"), 
        new BigDecimal("0.5")
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    
    EngineNumber value = new EngineNumber(new BigDecimal("10"), "mt");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.of(mockYearMatcher));

    // Assert - no setStream calls should be made when out of range
    verify(mockEngine, never()).setStream(any(), any(), any());
  }

  @Test
  void testHandleSalesSetWithZeroImportAllocation() {
    // Arrange - test edge case where import is disabled (0% allocation)
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("1.0"), // 100% domestic
        new BigDecimal("0.0")  // 0% import (disabled)
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    
    EngineNumber value = new EngineNumber(new BigDecimal("5.5"), "mt");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.empty());

    // Assert - only domestic should be set, no import call
    verify(mockEngine, times(1)).setStream(eq("domestic"), any(), any());
    verify(mockEngine, never()).setStream(eq("import"), any(), any());
  }

  @Test
  void testHandleSalesSetWithImportOnly() {
    // Arrange - test edge case where only import is enabled
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.0"), // 0% domestic (disabled)
        new BigDecimal("1.0")  // 100% import
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    
    EngineNumber value = new EngineNumber(new BigDecimal("15"), "kg");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.empty());

    // Assert - only import should be set, no domestic call
    verify(mockEngine, times(1)).setStream(eq("import"), any(), any());
    verify(mockEngine, never()).setStream(eq("domestic"), any(), any());
  }

  @Test
  void testHandleSalesSetWithAsymmetricDistribution() {
    // Arrange - test realistic asymmetric distribution
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.25"), // 25% domestic  
        new BigDecimal("0.75")  // 75% import
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    
    EngineNumber value = new EngineNumber(new BigDecimal("100"), "kg");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.empty());

    // Assert
    ArgumentCaptor<String> streamCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<EngineNumber> valueCaptor = ArgumentCaptor.forClass(EngineNumber.class);
    
    verify(mockEngine, times(2)).setStream(streamCaptor.capture(), valueCaptor.capture(), any());
    
    // Check domestic call (25% of 100 = 25)
    assertEquals("domestic", streamCaptor.getAllValues().get(0));
    assertEquals(new BigDecimal("25.00"), valueCaptor.getAllValues().get(0).getValue());
    assertEquals("kg", valueCaptor.getAllValues().get(0).getUnits());
    
    // Check import call (75% of 100 = 75)
    assertEquals("import", streamCaptor.getAllValues().get(1));
    assertEquals(new BigDecimal("75.00"), valueCaptor.getAllValues().get(1).getValue());
    assertEquals("kg", valueCaptor.getAllValues().get(1).getUnits());
  }

  @Test
  void testHandleSalesSetWithNoYearMatcher() {
    // Arrange - test without year matcher (should always proceed)
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.6"), // 60% domestic
        new BigDecimal("0.4")  // 40% import
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    
    EngineNumber value = new EngineNumber(new BigDecimal("50"), "mt");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.empty());

    // Assert
    ArgumentCaptor<String> streamCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<EngineNumber> valueCaptor = ArgumentCaptor.forClass(EngineNumber.class);
    
    verify(mockEngine, times(2)).setStream(streamCaptor.capture(), valueCaptor.capture(), any());
    
    // Check domestic call (60% of 50 = 30)
    assertEquals("domestic", streamCaptor.getAllValues().get(0));
    assertEquals(new BigDecimal("30.0"), valueCaptor.getAllValues().get(0).getValue());
    assertEquals("mt", valueCaptor.getAllValues().get(0).getUnits());
    
    // Check import call (40% of 50 = 20)
    assertEquals("import", streamCaptor.getAllValues().get(1));
    assertEquals(new BigDecimal("20.0"), valueCaptor.getAllValues().get(1).getValue());
    assertEquals("mt", valueCaptor.getAllValues().get(1).getUnits());
  }

  @Test
  void testHandleSalesSetWithSmallValues() {
    // Arrange - test with decimal values to ensure precision
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.33"), // 33% domestic
        new BigDecimal("0.67")  // 67% import  
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    
    EngineNumber value = new EngineNumber(new BigDecimal("1"), "kg");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.empty());

    // Assert
    ArgumentCaptor<String> streamCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<EngineNumber> valueCaptor = ArgumentCaptor.forClass(EngineNumber.class);
    
    verify(mockEngine, times(2)).setStream(streamCaptor.capture(), valueCaptor.capture(), any());
    
    // Check domestic call (33% of 1 = 0.33)
    assertEquals("domestic", streamCaptor.getAllValues().get(0));
    assertEquals(new BigDecimal("0.33"), valueCaptor.getAllValues().get(0).getValue());
    assertEquals("kg", valueCaptor.getAllValues().get(0).getUnits());
    
    // Check import call (67% of 1 = 0.67)
    assertEquals("import", streamCaptor.getAllValues().get(1));
    assertEquals(new BigDecimal("0.67"), valueCaptor.getAllValues().get(1).getValue());
    assertEquals("kg", valueCaptor.getAllValues().get(1).getUnits());
  }

  @Test
  void testHandleSalesSetWithBothStreamsDisabled() {
    // Arrange - edge case where both streams are disabled (should not happen in practice)
    SalesStreamDistribution distribution = new SalesStreamDistribution(
        new BigDecimal("0.0"), // 0% domestic
        new BigDecimal("0.0")  // 0% import
    );
    when(mockStreamKeeper.getDistribution(mockUseKey)).thenReturn(distribution);
    
    EngineNumber value = new EngineNumber(new BigDecimal("10"), "mt");

    // Act
    setExecutor.handleSalesSet(mockUseKey, "sales", value, Optional.empty());

    // Assert - no setStream calls should be made
    verify(mockEngine, never()).setStream(any(), any(), any());
  }

  @Test
  void testConstructor() {
    // Act - constructor called in setUp
    // Assert - no exceptions thrown, object created successfully
    // This test mainly verifies the constructor doesn't throw exceptions
  }
}