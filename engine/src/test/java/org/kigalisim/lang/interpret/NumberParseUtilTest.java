/**
 * Unit tests for the NumberParseUtil class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    assertEquals(new BigDecimal("123"), numberParser.parseFlexibleNumber("123"));
    assertEquals(new BigDecimal("0"), numberParser.parseFlexibleNumber("0"));
    assertEquals(new BigDecimal("999999"), numberParser.parseFlexibleNumber("999999"));
  }

  /**
   * Test parsing negative numbers.
   */
  @Test
  public void testParseNegativeNumbers() {
    assertEquals(new BigDecimal("-123"), numberParser.parseFlexibleNumber("-123"));
    assertEquals(new BigDecimal("-123.45"), numberParser.parseFlexibleNumber("-123.45"));
    assertEquals(new BigDecimal("-1000"), numberParser.parseFlexibleNumber("-1,000"));
  }

  /**
   * Test parsing numbers with positive sign.
   */
  @Test
  public void testParsePositiveNumbers() {
    assertEquals(new BigDecimal("123"), numberParser.parseFlexibleNumber("+123"));
    assertEquals(new BigDecimal("123.45"), numberParser.parseFlexibleNumber("+123.45"));
  }

  /**
   * Test parsing US format numbers (comma as thousands separator).
   */
  @Test
  public void testParseUsFormat() {
    assertEquals(new BigDecimal("123456.789"),
        numberParser.parseFlexibleNumber("123,456.789"));
    assertEquals(new BigDecimal("1234567"),
        numberParser.parseFlexibleNumber("1,234,567"));
    assertEquals(new BigDecimal("1000"),
        numberParser.parseFlexibleNumber("1,000"));
  }

  /**
   * Test parsing European format numbers (period as thousands separator).
   */
  @Test
  public void testParseEuropeanFormat() {
    assertEquals(new BigDecimal("123456.789"),
        numberParser.parseFlexibleNumber("123.456,789"));
    assertEquals(new BigDecimal("1234567.89"),
        numberParser.parseFlexibleNumber("1.234.567,89"));
    assertEquals(new BigDecimal("1000"),
        numberParser.parseFlexibleNumber("1.000"));
  }

  /**
   * Test parsing numbers with single decimal separator (period).
   */
  @Test
  public void testParseSingleDecimalPeriod() {
    assertEquals(new BigDecimal("123.45"), numberParser.parseFlexibleNumber("123.45"));
    assertEquals(new BigDecimal("0.5"), numberParser.parseFlexibleNumber("0.5"));
    assertEquals(new BigDecimal("123.4567"), numberParser.parseFlexibleNumber("123.4567"));
  }

  /**
   * Test parsing numbers with single decimal separator (comma).
   */
  @Test
  public void testParseSingleDecimalComma() {
    assertEquals(new BigDecimal("123.45"), numberParser.parseFlexibleNumber("123,45"));
    assertEquals(new BigDecimal("0.5"), numberParser.parseFlexibleNumber("0,5"));
    assertEquals(new BigDecimal("123.4567"), numberParser.parseFlexibleNumber("123,4567"));
  }

  /**
   * Test parsing numbers with multiple thousands separators (commas).
   */
  @Test
  public void testParseMultipleThousandsCommas() {
    assertEquals(new BigDecimal("1234567"), numberParser.parseFlexibleNumber("1,234,567"));
    assertEquals(new BigDecimal("12345678"), numberParser.parseFlexibleNumber("12,345,678"));
    assertEquals(new BigDecimal("1234567890"),
        numberParser.parseFlexibleNumber("1,234,567,890"));
  }

  /**
   * Test parsing numbers with multiple thousands separators (periods).
   */
  @Test
  public void testParseMultipleThousandsPeriods() {
    assertEquals(new BigDecimal("1234567"), numberParser.parseFlexibleNumber("1.234.567"));
    assertEquals(new BigDecimal("12345678"), numberParser.parseFlexibleNumber("12.345.678"));
    assertEquals(new BigDecimal("1234567890"),
        numberParser.parseFlexibleNumber("1.234.567.890"));
  }

  /**
   * Test ambiguous cases should throw exceptions.
   */
  @Test
  public void testAmbiguousCasesThrowExceptions() {
    // Single comma with exactly 3 digits after - ambiguous
    NumberFormatException exception1 = assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber("123,456"));
    assertTrue(exception1.getMessage().contains("Ambiguous number format"));
    assertTrue(exception1.getMessage().contains("123,456"));

    // Single period with exactly 3 digits after - ambiguous
    NumberFormatException exception2 = assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber("123.456"));
    assertTrue(exception2.getMessage().contains("Ambiguous number format"));
    assertTrue(exception2.getMessage().contains("123.456"));
  }

  /**
   * Test invalid input cases.
   */
  @Test
  public void testInvalidInputThrowsExceptions() {
    // Null input
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber(null));

    // Empty string
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber(""));

    // Whitespace only
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber("   "));
  }

  /**
   * Test invalid number formats.
   */
  @Test
  public void testInvalidNumberFormats() {
    // Multiple decimal separators
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber("12.34.56,78"));

    // Invalid thousands separator positioning
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber("1,23,456"));

    // Non-numeric characters
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber("123abc"));

    // Separator at start
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber(",123"));

    // Separator at end
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber("123,"));
  }

  /**
   * Test whitespace handling.
   */
  @Test
  public void testWhitespaceHandling() {
    assertEquals(new BigDecimal("123.45"), numberParser.parseFlexibleNumber("  123.45  "));
    assertEquals(new BigDecimal("1000"), numberParser.parseFlexibleNumber("  1,000  "));
    assertEquals(new BigDecimal("-123.45"), numberParser.parseFlexibleNumber("  -123.45  "));
  }

  /**
   * Test large numbers.
   */
  @Test
  public void testLargeNumbers() {
    assertEquals(new BigDecimal("123456789012345"),
        numberParser.parseFlexibleNumber("123,456,789,012,345"));
    assertEquals(new BigDecimal("123456789012345.67"),
        numberParser.parseFlexibleNumber("123,456,789,012,345.67"));
    assertEquals(new BigDecimal("123456789012345.67"),
        numberParser.parseFlexibleNumber("123.456.789.012.345,67"));
  }

  /**
   * Test edge cases with single digits.
   */
  @Test
  public void testSingleDigitCases() {
    assertEquals(new BigDecimal("1"), numberParser.parseFlexibleNumber("1"));
    assertEquals(new BigDecimal("-1"), numberParser.parseFlexibleNumber("-1"));
    assertEquals(new BigDecimal("0.1"), numberParser.parseFlexibleNumber("0.1"));
    assertEquals(new BigDecimal("0.1"), numberParser.parseFlexibleNumber("0,1"));
  }

  /**
   * Test validation of thousands separator positioning.
   */
  @Test
  public void testThousandsSeparatorValidation() {
    // Valid positioning
    assertEquals(new BigDecimal("1234567"),
        numberParser.parseFlexibleNumber("1,234,567"));

    // Invalid positioning - wrong number of digits between separators
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber("12,3456,789"));

    // Invalid positioning - first group too long
    assertThrows(NumberFormatException.class, () ->
        numberParser.parseFlexibleNumber("1234,567,890"));
  }

  /**
   * Test mixed separator precedence rules.
   */
  @Test
  public void testMixedSeparatorPrecedence() {
    // Comma before period: comma = thousands, period = decimal
    assertEquals(new BigDecimal("123456.789"),
        numberParser.parseFlexibleNumber("123,456.789"));

    // Period before comma: period = thousands, comma = decimal
    assertEquals(new BigDecimal("123456.789"),
        numberParser.parseFlexibleNumber("123.456,789"));

    // Multiple periods, then comma
    assertEquals(new BigDecimal("1234567.89"),
        numberParser.parseFlexibleNumber("1.234.567,89"));

    // Multiple commas, then period
    assertEquals(new BigDecimal("1234567.89"),
        numberParser.parseFlexibleNumber("1,234,567.89"));
  }

  /**
   * Test zero and decimal zero cases.
   */
  @Test
  public void testZeroCases() {
    assertEquals(new BigDecimal("0"), numberParser.parseFlexibleNumber("0"));
    assertEquals(new BigDecimal("0.0"), numberParser.parseFlexibleNumber("0.0"));
    assertEquals(new BigDecimal("0.0"), numberParser.parseFlexibleNumber("0,0"));
    assertEquals(new BigDecimal("0"), numberParser.parseFlexibleNumber("0000"));
  }

  /**
   * Test numbers starting with decimal point.
   */
  @Test
  public void testNumbersStartingWithDecimal() {
    assertEquals(new BigDecimal("0.5"), numberParser.parseFlexibleNumber(".5"));
    assertEquals(new BigDecimal("-0.5"), numberParser.parseFlexibleNumber("-.5"));
    assertEquals(new BigDecimal("0.123"), numberParser.parseFlexibleNumber(".123"));
    assertEquals(new BigDecimal("0.999"), numberParser.parseFlexibleNumber(".999"));
  }
}
