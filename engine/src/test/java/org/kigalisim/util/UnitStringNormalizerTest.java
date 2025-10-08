/**
 * Tests for UnitStringNormalizer.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test suite for UnitStringNormalizer.
 */
class UnitStringNormalizerTest {

  @Test
  void testNormalizeWithNoSpaces() {
    assertEquals("kg", UnitStringNormalizer.normalize("kg"));
    assertEquals("mt", UnitStringNormalizer.normalize("mt"));
    assertEquals("kg/unit", UnitStringNormalizer.normalize("kg/unit"));
  }

  @Test
  void testNormalizeWithSingleSpace() {
    assertEquals("kg/unit", UnitStringNormalizer.normalize("kg / unit"));
    assertEquals("tCO2e/kg", UnitStringNormalizer.normalize("tCO2e / kg"));
  }

  @Test
  void testNormalizeWithMultipleSpaces() {
    assertEquals("kg/unit", UnitStringNormalizer.normalize("kg  /  unit"));
    assertEquals("kg/unit", UnitStringNormalizer.normalize("kg   /   unit"));
  }

  @Test
  void testNormalizeWithLeadingTrailingSpaces() {
    assertEquals("kg", UnitStringNormalizer.normalize(" kg "));
    assertEquals("kg/unit", UnitStringNormalizer.normalize(" kg / unit "));
  }


  @Test
  void testNormalizeEmptyString() {
    assertEquals("", UnitStringNormalizer.normalize(""));
  }

  @Test
  void testNormalizeComplexUnits() {
    assertEquals("kgCO2e/unit/year", UnitStringNormalizer.normalize("kgCO2e / unit / year"));
    assertEquals("mt/unit", UnitStringNormalizer.normalize("mt  /  unit"));
  }
}
