package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Unit tests for ChangeExecutorConfigBuilder class.
 */
class ChangeExecutorConfigBuilderTest {

  private ChangeExecutorConfigBuilder builder;
  private EngineNumber mockAmount;
  private YearMatcher mockYearMatcher;
  private UseKey mockUseKey;

  @BeforeEach
  void setUp() {
    builder = new ChangeExecutorConfigBuilder();
    mockAmount = new EngineNumber(new BigDecimal("100"), "units");
    mockYearMatcher = mock(YearMatcher.class);
    mockUseKey = mock(UseKey.class);
  }

  @Test
  void testBuildValidConfig() {
    // Act
    ChangeExecutorConfig config = builder
        .setStream("domestic")
        .setAmount(mockAmount)
        .setYearMatcher(mockYearMatcher)
        .setUseKeyEffective(mockUseKey)
        .build();

    // Assert
    assertEquals("domestic", config.getStream());
    assertSame(mockAmount, config.getAmount());
    assertTrue(config.getYearMatcher().isPresent());
    assertSame(mockYearMatcher, config.getYearMatcher().get());
    assertSame(mockUseKey, config.getUseKeyEffective());
  }

  @Test
  void testBuildWithNullYearMatcher() {
    // Act
    ChangeExecutorConfig config = builder
        .setStream("import")
        .setAmount(mockAmount)
        .setUseKeyEffective(mockUseKey)
        .build();

    // Assert
    assertEquals("import", config.getStream());
    assertSame(mockAmount, config.getAmount());
    assertFalse(config.getYearMatcher().isPresent());
    assertSame(mockUseKey, config.getUseKeyEffective());
  }

  @Test
  void testBuilderMethodChaining() {
    // Test that builder methods return the same instance for chaining
    final ChangeExecutorConfigBuilder result1 = builder.setStream("sales");
    final ChangeExecutorConfigBuilder result2 = result1.setAmount(mockAmount);
    final ChangeExecutorConfigBuilder result3 = result2.setYearMatcher(mockYearMatcher);
    final ChangeExecutorConfigBuilder result4 = result3.setUseKeyEffective(mockUseKey);

    assertSame(builder, result1);
    assertSame(builder, result2);
    assertSame(builder, result3);
    assertSame(builder, result4);
  }

  @Test
  void testBuildThrowsExceptionWhenStreamMissing() {
    // Arrange
    builder.setAmount(mockAmount).setUseKeyEffective(mockUseKey);

    // Act & Assert
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> builder.build()
    );
    assertEquals("Stream is required", exception.getMessage());
  }

  @Test
  void testBuildThrowsExceptionWhenAmountMissing() {
    // Arrange
    builder.setStream("domestic").setUseKeyEffective(mockUseKey);

    // Act & Assert
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> builder.build()
    );
    assertEquals("Amount is required", exception.getMessage());
  }

  @Test
  void testBuildThrowsExceptionWhenUseKeyMissing() {
    // Arrange
    builder.setStream("domestic").setAmount(mockAmount);

    // Act & Assert
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> builder.build()
    );
    assertEquals("UseKeyEffective is required", exception.getMessage());
  }

  @Test
  void testSetStreamOverride() {
    // Act
    ChangeExecutorConfig config = builder
        .setStream("domestic")
        .setStream("import") // Override previous value
        .setAmount(mockAmount)
        .setUseKeyEffective(mockUseKey)
        .build();

    // Assert
    assertEquals("import", config.getStream());
  }

  @Test
  void testSetAmountOverride() {
    // Arrange
    EngineNumber newAmount = new EngineNumber(new BigDecimal("200"), "kg");

    // Act
    ChangeExecutorConfig config = builder
        .setStream("domestic")
        .setAmount(mockAmount)
        .setAmount(newAmount) // Override previous value
        .setUseKeyEffective(mockUseKey)
        .build();

    // Assert
    assertSame(newAmount, config.getAmount());
  }

  @Test
  void testWithPercentageAmount() {
    // Arrange
    EngineNumber percentageAmount = new EngineNumber(new BigDecimal("5"), "%");

    // Act
    ChangeExecutorConfig config = builder
        .setStream("sales")
        .setAmount(percentageAmount)
        .setYearMatcher(mockYearMatcher)
        .setUseKeyEffective(mockUseKey)
        .build();

    // Assert
    assertEquals("sales", config.getStream());
    assertEquals("5", config.getAmount().getValue().toString());
    assertEquals("%", config.getAmount().getUnits());
  }
}
