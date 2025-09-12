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
    assertParseEquals(new BigDecimal("-1000.0"), "-1,000.0");
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
    assertParseEquals(new BigDecimal("1000.0"), "1,000.0");
  }

  /**
   * Test European format rejection with helpful error messages.
   */
  @Test
  public void testEuropeanFormatRejection() {
    // Test European format detection and error messages
    
    // Mixed European format (periods for thousands, comma for decimal)
    FlexibleNumberParseResult result1 = numberParser.parseFlexibleNumber("123.456,789");
    assertTrue(result1.isError());
    assertTrue(result1.getError().get().contains("European number format detected"));
    assertTrue(result1.getError().get().contains("123,456.789"));
    
    FlexibleNumberParseResult result2 = numberParser.parseFlexibleNumber("1.234.567,89");
    assertTrue(result2.isError());
    assertTrue(result2.getError().get().contains("European number format detected"));
    assertTrue(result2.getError().get().contains("1,234,567.89"));
    
    // Note: Single comma decimals like "123,45" are now accepted as UK format
    // The main European format patterns we reject are mixed formats and multiple periods
    
    // European thousands separators (multiple periods)
    FlexibleNumberParseResult result3 = numberParser.parseFlexibleNumber("1.234.567");
    assertTrue(result3.isError());
    assertTrue(result3.getError().get().contains("European number format detected"));
    assertTrue(result3.getError().get().contains("1,234,567"));
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
   * Test that multiple periods are rejected as European format.
   */
  @Test
  public void testMultiplePeriodsRejected() {
    // Multiple periods should be rejected as European thousands separators
    FlexibleNumberParseResult result1 = numberParser.parseFlexibleNumber("1.234.567");
    assertTrue(result1.isError());
    assertTrue(result1.getError().get().contains("European number format detected"));
    
    FlexibleNumberParseResult result2 = numberParser.parseFlexibleNumber("12.345.678");
    assertTrue(result2.isError());
    assertTrue(result2.getError().get().contains("European number format detected"));
  }

  /**
   * Test that European mixed formats are rejected with helpful suggestions.
   */
  @Test
  public void testEuropeanMixedFormatRejected() {
    // European mixed formats (period before comma) should be rejected with UK format suggestions
    FlexibleNumberParseResult result1 = numberParser.parseFlexibleNumber("1.234,56");
    assertTrue(result1.isError());
    assertTrue(result1.getError().get().contains("European number format detected"));
    assertTrue(result1.getError().get().contains("1,234.56"));
    
    FlexibleNumberParseResult result2 = numberParser.parseFlexibleNumber("123.456,789");
    assertTrue(result2.isError());
    assertTrue(result2.getError().get().contains("European number format detected"));
    assertTrue(result2.getError().get().contains("123,456.789"));
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
    assertParseEquals(new BigDecimal("1000.0"), "  1,000.0  ");
    assertParseEquals(new BigDecimal("-123.45"), "  -123.45  ");
  }

  /**
   * Test that previously ambiguous number formats now work as UK format.
   */
  @Test
  public void testPreviouslyAmbiguousCasesNowWork() {
    // These patterns were previously ambiguous but now parse successfully as UK format
    assertParseEquals(new BigDecimal("1000"), "1,000");      // UK thousands separator
    assertParseEquals(new BigDecimal("25000"), "25,000");    // UK thousands separator
    assertParseEquals(new BigDecimal("1.000"), "1.000");     // UK decimal separator
    assertParseEquals(new BigDecimal("25.000"), "25.000");   // UK decimal separator
    assertParseEquals(new BigDecimal("123456"), "123,456");  // UK thousands separator
    assertParseEquals(new BigDecimal("123.456"), "123.456"); // UK decimal separator
  }

  /**
   * Test large numbers.
   */
  @Test
  public void testLargeNumbers() {
    // UK format should work
    assertParseEquals(new BigDecimal("123456789012345"), "123,456,789,012,345");
    assertParseEquals(new BigDecimal("123456789012345.67"), "123,456,789,012,345.67");
    
    // European format should be rejected
    FlexibleNumberParseResult result = numberParser.parseFlexibleNumber("123.456.789.012.345,67");
    assertTrue(result.isError());
    assertTrue(result.getError().get().contains("European number format detected"));
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
   * Test mixed separator precedence rules - UK format accepted, European format rejected.
   */
  @Test
  public void testMixedSeparatorPrecedence() {
    // UK format: Comma before period: comma = thousands, period = decimal - should work
    assertParseEquals(new BigDecimal("123456.789"), "123,456.789");

    // UK format: Multiple commas, then period - should work
    assertParseEquals(new BigDecimal("1234567.89"), "1,234,567.89");

    // European format: Period before comma - should be rejected
    FlexibleNumberParseResult result1 = numberParser.parseFlexibleNumber("123.456,789");
    assertTrue(result1.isError());
    assertTrue(result1.getError().get().contains("European number format detected"));

    // European format: Multiple periods, then comma - should be rejected
    FlexibleNumberParseResult result2 = numberParser.parseFlexibleNumber("1.234.567,89");
    assertTrue(result2.isError());
    assertTrue(result2.getError().get().contains("European number format detected"));
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