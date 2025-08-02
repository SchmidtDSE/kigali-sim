package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Unit tests for EngineSupportUtils class.
 */
class EngineSupportUtilsTest {

  @Test
  void testIsInRangeWithNullYearMatcher() {
    // Act & Assert
    assertTrue(EngineSupportUtils.isInRange(null, 2023));
  }

  @Test
  void testIsInRangeWithValidYearMatcher() {
    // Arrange
    YearMatcher mockYearMatcher = mock(YearMatcher.class);
    when(mockYearMatcher.getInRange(2023)).thenReturn(true);

    // Act & Assert
    assertTrue(EngineSupportUtils.isInRange(mockYearMatcher, 2023));
  }

  @Test
  void testIsInRangeWithInvalidYearMatcher() {
    // Arrange
    YearMatcher mockYearMatcher = mock(YearMatcher.class);
    when(mockYearMatcher.getInRange(2023)).thenReturn(false);

    // Act & Assert
    assertFalse(EngineSupportUtils.isInRange(mockYearMatcher, 2023));
  }

  @Test
  void testIsSalesSubstreamDomestic() {
    // Act & Assert
    assertTrue(EngineSupportUtils.isSalesSubstream("domestic"));
  }

  @Test
  void testIsSalesSubstreamImport() {
    // Act & Assert
    assertTrue(EngineSupportUtils.isSalesSubstream("import"));
  }

  @Test
  void testIsSalesSubstreamFalseForOtherStreams() {
    // Act & Assert
    assertFalse(EngineSupportUtils.isSalesSubstream("sales"));
    assertFalse(EngineSupportUtils.isSalesSubstream("export"));
    assertFalse(EngineSupportUtils.isSalesSubstream("equipment"));
    assertFalse(EngineSupportUtils.isSalesSubstream("priorEquipment"));
    assertFalse(EngineSupportUtils.isSalesSubstream("unknown"));
    assertFalse(EngineSupportUtils.isSalesSubstream(null));
  }

  @Test
  void testHasUnitBasedSalesSpecificationsWithUnitBasedSales() {
    // Arrange
    StreamKeeper mockStreamKeeper = mock(StreamKeeper.class);
    UseKey mockScope = mock(UseKey.class);
    EngineNumber mockEngineNumber = mock(EngineNumber.class);

    when(mockStreamKeeper.hasLastSpecifiedValue(mockScope, "sales")).thenReturn(true);
    when(mockStreamKeeper.getLastSpecifiedValue(mockScope, "sales")).thenReturn(mockEngineNumber);
    when(mockEngineNumber.hasEquipmentUnits()).thenReturn(true);

    // Act & Assert
    assertTrue(EngineSupportUtils.hasUnitBasedSalesSpecifications(mockStreamKeeper, mockScope));
  }

  @Test
  void testHasUnitBasedSalesSpecificationsWithVolumeBasedSales() {
    // Arrange
    StreamKeeper mockStreamKeeper = mock(StreamKeeper.class);
    UseKey mockScope = mock(UseKey.class);
    EngineNumber mockEngineNumber = mock(EngineNumber.class);

    when(mockStreamKeeper.hasLastSpecifiedValue(mockScope, "sales")).thenReturn(true);
    when(mockStreamKeeper.getLastSpecifiedValue(mockScope, "sales")).thenReturn(mockEngineNumber);
    when(mockEngineNumber.hasEquipmentUnits()).thenReturn(false);

    // Act & Assert
    assertFalse(EngineSupportUtils.hasUnitBasedSalesSpecifications(mockStreamKeeper, mockScope));
  }

  @Test
  void testHasUnitBasedSalesSpecificationsWithNoLastSpecifiedValue() {
    // Arrange
    StreamKeeper mockStreamKeeper = mock(StreamKeeper.class);
    UseKey mockScope = mock(UseKey.class);

    when(mockStreamKeeper.hasLastSpecifiedValue(mockScope, "sales")).thenReturn(false);

    // Act & Assert
    assertFalse(EngineSupportUtils.hasUnitBasedSalesSpecifications(mockStreamKeeper, mockScope));
  }

  @Test
  void testHasUnitBasedSalesSpecificationsWithNullLastSpecifiedValue() {
    // Arrange
    StreamKeeper mockStreamKeeper = mock(StreamKeeper.class);
    UseKey mockScope = mock(UseKey.class);

    when(mockStreamKeeper.hasLastSpecifiedValue(mockScope, "sales")).thenReturn(true);
    when(mockStreamKeeper.getLastSpecifiedValue(mockScope, "sales")).thenReturn(null);

    // Act & Assert - should handle null gracefully without NPE
    assertFalse(EngineSupportUtils.hasUnitBasedSalesSpecifications(mockStreamKeeper, mockScope));
  }

  // Note: testCreateUnitConverterWithTotal removed as it requires access to package-private method
  // The method is tested indirectly through integration tests
}
