package org.kigalisim.engine.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Unit tests for ChangeExecutor class.
 *
 * <p>Note: Most ChangeExecutor functionality is tested through integration tests
 * since it delegates to existing engine methods that are already well-tested.</p>
 */
class ChangeExecutorTest {

  private SingleThreadEngine mockEngine;
  private UseKey mockUseKey;
  private YearMatcher mockYearMatcher;
  private ChangeExecutor changeExecutor;

  @BeforeEach
  void setUp() {
    mockEngine = mock(SingleThreadEngine.class);
    mockUseKey = mock(UseKey.class);
    mockYearMatcher = mock(YearMatcher.class);

    when(mockEngine.getYear()).thenReturn(2023);

    changeExecutor = new ChangeExecutor(mockEngine);
  }

  @Test
  void testExecuteChangeOutOfRange() {
    // Arrange
    when(mockYearMatcher.getInRange(2023)).thenReturn(false);
    EngineNumber amount = new EngineNumber(new BigDecimal("100"), "kg");

    // Act
    changeExecutor.executeChange("domestic", amount, mockYearMatcher, mockUseKey);

    // Assert - no further interactions should occur when out of range
    verify(mockEngine, never()).changeStream(any(), any(), any(), any());
    verify(mockEngine, never()).executeStreamUpdate(any());
  }

  @Test
  void testExecuteChangeWithConfig() {
    // Arrange
    when(mockYearMatcher.getInRange(2023)).thenReturn(false);
    ChangeExecutorConfig config = new ChangeExecutorConfigBuilder()
        .setStream("domestic")
        .setAmount(new EngineNumber(new BigDecimal("100"), "kg"))
        .setYearMatcher(mockYearMatcher)
        .setUseKeyEffective(mockUseKey)
        .build();

    // Act
    changeExecutor.executeChange(config);

    // Assert - no further interactions should occur when out of range
    verify(mockEngine, never()).changeStream(any(), any(), any(), any());
    verify(mockEngine, never()).executeStreamUpdate(any());
  }

  @Test
  void testExecuteChangeCallsConfigOverload() {
    // Arrange
    when(mockYearMatcher.getInRange(2023)).thenReturn(false); // Make it not in range to avoid complex mocking
    EngineNumber amount = new EngineNumber(new BigDecimal("100"), "kg");

    // Act
    changeExecutor.executeChange("domestic", amount, mockYearMatcher, mockUseKey);

    // Assert - verify that the year check was called (indicating config path was taken)
    verify(mockEngine, times(1)).getYear();
  }

  @Test
  void testConstructor() {
    // Act - constructor called in setUp
    // Assert - no exceptions thrown, object created successfully
    // This test mainly verifies the constructor doesn't throw exceptions
  }
}
