package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Unit tests for ChangeExecutorConfig class.
 */
class ChangeExecutorConfigTest {

  @Test
  void testConstructorAndGetters() {
    // Arrange
    String stream = "domestic";
    EngineNumber amount = new EngineNumber(new BigDecimal("100"), "units");
    YearMatcher yearMatcher = mock(YearMatcher.class);
    UseKey useKey = mock(UseKey.class);

    // Act
    ChangeExecutorConfig config = new ChangeExecutorConfig(stream, amount, yearMatcher, useKey);

    // Assert
    assertEquals(stream, config.getStream());
    assertSame(amount, config.getAmount());
    assertTrue(config.getYearMatcher().isPresent());
    assertSame(yearMatcher, config.getYearMatcher().get());
    assertSame(useKey, config.getUseKeyEffective());
  }

  @Test
  void testWithNullYearMatcher() {
    // Arrange
    String stream = "import";
    EngineNumber amount = new EngineNumber(new BigDecimal("50"), "kg");
    UseKey useKey = mock(UseKey.class);

    // Act
    ChangeExecutorConfig config = new ChangeExecutorConfig(stream, amount, useKey);

    // Assert
    assertEquals(stream, config.getStream());
    assertSame(amount, config.getAmount());
    assertFalse(config.getYearMatcher().isPresent());
    assertSame(useKey, config.getUseKeyEffective());
  }

  @Test
  void testWithPercentageAmount() {
    // Arrange
    String stream = "sales";
    EngineNumber amount = new EngineNumber(new BigDecimal("5"), "%");
    YearMatcher yearMatcher = mock(YearMatcher.class);
    UseKey useKey = mock(UseKey.class);

    // Act
    ChangeExecutorConfig config = new ChangeExecutorConfig(stream, amount, yearMatcher, useKey);

    // Assert
    assertEquals(stream, config.getStream());
    assertEquals("5", config.getAmount().getValue().toString());
    assertEquals("%", config.getAmount().getUnits());
    assertTrue(config.getYearMatcher().isPresent());
    assertSame(yearMatcher, config.getYearMatcher().get());
    assertSame(useKey, config.getUseKeyEffective());
  }
}
