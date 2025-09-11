/**
 * Unit tests for the NumberParseUtil class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.localization.FlexibleNumberParseResult;
import org.kigalisim.lang.localization.NumberParseUtil;

/**
 * Tests for the NumberParseUtil class.
 */
public class NumberParseUtilTest {

  private NumberParseUtil numberParser;

  /**
   * Set up a new NumberParseUtil instance before each test.
   */
  @BeforeEach
  public void setUp() {
    numberParser = new NumberParseUtil();
  }

  /**
   * Helper method to assert successful parsing result.
   */
  private void assertParseEquals(BigDecimal expected, String input) {
    FlexibleNumberParseResult result = numberParser.parseFlexibleNumber(input);
    assertTrue(result.isSuccess(), "Parsing should succeed for input: " + input + ". Error: "
        + (result.isError() ? result.getError().get() : "N/A"));
    assertEquals(expected, result.getParsedNumber().get());
  }

  /**
   * Helper method to assert parsing fails.
   */
  private void assertParseFails(String input) {
    FlexibleNumberParseResult result = numberParser.parseFlexibleNumber(input);
    assertTrue(result.isError(), "Parsing should fail for input: " + input);
  }

  /**
   * Test that NumberParseUtil can be initialized.
   */
  @Test
  public void testInitializes() {
    assertNotNull(numberParser, "NumberParseUtil should be constructable");
  }

  /**
   * Test parsing numbers with no separators.
   */
  @Test
  public void testParseSimpleNumbers() {
    assertParseEquals(new BigDecimal("123"), "123");
    assertParseEquals(new BigDecimal("0"), "0");
    assertParseEquals(new BigDecimal("999999"), "999999");
  }

  /**
   * Test parsing negative numbers.
   */
  @Test
  public void testParseNegativeNumbers() {
    assertParseEquals(new BigDecimal("-123"), "-123");
    assertParseEquals(new BigDecimal("-123.45"), "-123.45");
    assertParseEquals(new BigDecimal("-1000"), "-1,000");
  }

  /**
   * Test parsing numbers with positive sign.
   */
  @Test
  public void testParsePositiveNumbers() {
    assertParseEquals(new BigDecimal("123"), "+123");
    assertParseEquals(new BigDecimal("123.45"), "+123.45");
  }

  /**
   * Test parsing US format numbers (comma as thousands separator).
   */
  @Test
  public void testParseUsFormat() {
    assertParseEquals(new BigDecimal("123456.789"), "123,456.789");
    assertParseEquals(new BigDecimal("1234567"), "1,234,567");
    assertParseEquals(new BigDecimal("1000"), "1,000");
  }

  /**
   * Test parsing European format numbers (period as thousands separator).
   */
  @Test
  public void testParseEuropeanFormat() {
    assertParseEquals(new BigDecimal("123456.789"), "123.456,789");
    assertParseEquals(new BigDecimal("1234567.89"), "1.234.567,89");
    assertParseEquals(new BigDecimal("1000"), "1.000");
  }

  /**
   * Test parsing numbers with single decimal separator (period).
   */
  @Test
  public void testParseSingleDecimalPeriod() {
    assertParseEquals(new BigDecimal("123.45"), "123.45");
    assertParseEquals(new BigDecimal("0.5"), "0.5");
    assertParseEquals(new BigDecimal("123.4567"), "123.4567");
  }

  /**
   * Test parsing numbers with single decimal separator (comma).
   */
  @Test
  public void testParseSingleDecimalComma() {
    assertParseEquals(new BigDecimal("123.45"), "123,45");
    assertParseEquals(new BigDecimal("0.5"), "0,5");
    assertParseEquals(new BigDecimal("123.4567"), "123,4567");
  }

  /**
   * Test parsing numbers with multiple thousands separators (commas).
   */
  @Test
  public void testParseMultipleThousandsCommas() {
    assertParseEquals(new BigDecimal("1234567"), "1,234,567");
    assertParseEquals(new BigDecimal("12345678"), "12,345,678");
    assertParseEquals(new BigDecimal("1234567890"), "1,234,567,890");
  }

  /**
   * Test parsing numbers with multiple thousands separators (periods).
   */
  @Test
  public void testParseMultipleThousandsPeriods() {
    assertParseEquals(new BigDecimal("1234567"), "1.234.567");
    assertParseEquals(new BigDecimal("12345678"), "12.345.678");
    assertParseEquals(new BigDecimal("1234567890"), "1.234.567.890");
  }

  /**
   * Test ambiguous cases should return errors.
   */
  @Test
  public void testAmbiguousCasesReturnErrors() {
    // Single comma with exactly 3 digits after - ambiguous
    FlexibleNumberParseResult result1 = numberParser.parseFlexibleNumber("123,456");
    assertTrue(result1.isError());
    assertTrue(result1.getError().get().contains("Ambiguous number format"));
    assertTrue(result1.getError().get().contains("123,456"));

    // Single period with exactly 3 digits after - ambiguous
    FlexibleNumberParseResult result2 = numberParser.parseFlexibleNumber("123.456");
    assertTrue(result2.isError());
    assertTrue(result2.getError().get().contains("Ambiguous number format"));
    assertTrue(result2.getError().get().contains("123.456"));
  }

  /**
   * Test invalid input cases.
   */
  @Test
  public void testInvalidInputReturnsErrors() {
    // Null input
    assertParseFails(null);

    // Empty string
    assertParseFails("");

    // Whitespace only
    assertParseFails("   ");
  }

  /**
   * Test invalid number formats.
   */
  @Test
  public void testInvalidNumberFormats() {
    // Multiple decimal separators
    assertParseFails("12.34.56,78");

    // Invalid thousands separator positioning
    assertParseFails("1,23,456");

    // Non-numeric characters
    assertParseFails("123abc");

    // Separator at start (comma)
    assertParseFails(",123");

    // Separator at end
    assertParseFails("123,");
  }

  /**
   * Test whitespace handling.
   */
  @Test
  public void testWhitespaceHandling() {
    assertParseEquals(new BigDecimal("123.45"), "  123.45  ");
    assertParseEquals(new BigDecimal("1000"), "  1,000  ");
    assertParseEquals(new BigDecimal("-123.45"), "  -123.45  ");
  }

  /**
   * Test large numbers.
   */
  @Test
  public void testLargeNumbers() {
    assertParseEquals(new BigDecimal("123456789012345"), "123,456,789,012,345");
    assertParseEquals(new BigDecimal("123456789012345.67"), "123,456,789,012,345.67");
    assertParseEquals(new BigDecimal("123456789012345.67"), "123.456.789.012.345,67");
  }

  /**
   * Test edge cases with single digits.
   */
  @Test
  public void testSingleDigitCases() {
    assertParseEquals(new BigDecimal("1"), "1");
    assertParseEquals(new BigDecimal("-1"), "-1");
    assertParseEquals(new BigDecimal("0.1"), "0.1");
    assertParseEquals(new BigDecimal("0.1"), "0,1");
  }

  /**
   * Test validation of thousands separator positioning.
   */
  @Test
  public void testThousandsSeparatorValidation() {
    // Valid positioning
    assertParseEquals(new BigDecimal("1234567"), "1,234,567");

    // Invalid positioning - wrong number of digits between separators
    assertParseFails("12,3456,789");

    // Invalid positioning - first group too long
    assertParseFails("1234,567,890");
  }

  /**
   * Test mixed separator precedence rules.
   */
  @Test
  public void testMixedSeparatorPrecedence() {
    // Comma before period: comma = thousands, period = decimal
    assertParseEquals(new BigDecimal("123456.789"), "123,456.789");

    // Period before comma: period = thousands, comma = decimal
    assertParseEquals(new BigDecimal("123456.789"), "123.456,789");

    // Multiple periods, then comma
    assertParseEquals(new BigDecimal("1234567.89"), "1.234.567,89");

    // Multiple commas, then period
    assertParseEquals(new BigDecimal("1234567.89"), "1,234,567.89");
  }

  /**
   * Test zero and decimal zero cases.
   */
  @Test
  public void testZeroCases() {
    assertParseEquals(new BigDecimal("0"), "0");
    assertParseEquals(new BigDecimal("0.0"), "0.0");
    assertParseEquals(new BigDecimal("0.0"), "0,0");
    assertParseEquals(new BigDecimal("0"), "0000");
  }

  /**
   * Test numbers starting with decimal point.
   */
  @Test
  public void testNumbersStartingWithDecimal() {
    assertParseEquals(new BigDecimal("0.5"), ".5");
    assertParseEquals(new BigDecimal("-0.5"), "-.5");
    assertParseEquals(new BigDecimal("0.123"), ".123");
    assertParseEquals(new BigDecimal("0.999"), ".999");
  }
}