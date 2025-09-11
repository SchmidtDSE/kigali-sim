/* eslint-disable max-len */
import {NumberParseUtil, NumberParseResult} from "number_parse_util";

/**
 * Comprehensive unit tests for NumberParseUtil class.
 * Tests flexible number parsing with various thousands and decimal separator formats.
 */
function buildNumberParseUtilTests() {
  QUnit.module("NumberParseUtil", function () {
    let numberParser;

    QUnit.testStart(function () {
      numberParser = new NumberParseUtil();
    });

    QUnit.test("initializes correctly", function (assert) {
      assert.ok(numberParser !== undefined, "NumberParseUtil should initialize");
      assert.ok(typeof numberParser.parseFlexibleNumber === "function", "parseFlexibleNumber method should exist");
      assert.ok(typeof numberParser.isAmbiguous === "function", "isAmbiguous method should exist");
      assert.ok(typeof numberParser.getDisambiguationSuggestion === "function", "getDisambiguationSuggestion method should exist");
    });

    // Test valid number formats
    QUnit.test("parses valid US format numbers", function (assert) {
      let result = numberParser.parseFlexibleNumber("123,456.78");
      assert.ok(result.isSuccess(), "US format with thousands and decimal should succeed");
      assert.equal(result.getNumber(), 123456.78, "US format with thousands and decimal");

      result = numberParser.parseFlexibleNumber("1,234");
      assert.ok(result.isSuccess(), "US format thousands separator should succeed");
      assert.equal(result.getNumber(), 1234, "US format thousands separator");

      result = numberParser.parseFlexibleNumber("12,345,678.90");
      assert.ok(result.isSuccess(), "US format with multiple thousands should succeed");
      assert.equal(result.getNumber(), 12345678.90, "US format with multiple thousands");

      result = numberParser.parseFlexibleNumber("123.45");
      assert.ok(result.isSuccess(), "Simple decimal number should succeed");
      assert.equal(result.getNumber(), 123.45, "Simple decimal number");
    });

    QUnit.test("parses valid European format numbers", function (assert) {
      let result = numberParser.parseFlexibleNumber("123.456,78");
      assert.ok(result.isSuccess(), "European format with thousands and decimal should succeed");
      assert.equal(result.getNumber(), 123456.78, "European format with thousands and decimal");

      result = numberParser.parseFlexibleNumber("1.234.567,89");
      assert.ok(result.isSuccess(), "European format with multiple thousands should succeed");
      assert.equal(result.getNumber(), 1234567.89, "European format with multiple thousands");

      result = numberParser.parseFlexibleNumber("123,45");
      assert.ok(result.isSuccess(), "European decimal separator should succeed");
      assert.equal(result.getNumber(), 123.45, "European decimal separator");
    });

    QUnit.test("parses numbers without separators", function (assert) {
      let result = numberParser.parseFlexibleNumber("123456");
      assert.ok(result.isSuccess(), "Integer without separators should succeed");
      assert.equal(result.getNumber(), 123456, "Integer without separators");

      result = numberParser.parseFlexibleNumber("123456.78");
      assert.ok(result.isSuccess(), "Decimal without thousands separator should succeed");
      assert.equal(result.getNumber(), 123456.78, "Decimal without thousands separator");

      result = numberParser.parseFlexibleNumber("1000");
      assert.ok(result.isSuccess(), "Round thousand without separator should succeed");
      assert.equal(result.getNumber(), 1000, "Round thousand without separator");
    });

    QUnit.test("handles negative numbers correctly", function (assert) {
      let result = numberParser.parseFlexibleNumber("-123,456.78");
      assert.ok(result.isSuccess(), "Negative US format should succeed");
      assert.equal(result.getNumber(), -123456.78, "Negative US format");

      result = numberParser.parseFlexibleNumber("-123.456,78");
      assert.ok(result.isSuccess(), "Negative European format should succeed");
      assert.equal(result.getNumber(), -123456.78, "Negative European format");

      result = numberParser.parseFlexibleNumber("-123456");
      assert.ok(result.isSuccess(), "Negative integer should succeed");
      assert.equal(result.getNumber(), -123456, "Negative integer");

      result = numberParser.parseFlexibleNumber("-1,234");
      assert.ok(result.isSuccess(), "Negative thousands should succeed");
      assert.equal(result.getNumber(), -1234, "Negative thousands");
    });

    QUnit.test("handles positive sign numbers correctly", function (assert) {
      let result = numberParser.parseFlexibleNumber("+123,456.78");
      assert.ok(result.isSuccess(), "Positive US format should succeed");
      assert.equal(result.getNumber(), 123456.78, "Positive US format");

      result = numberParser.parseFlexibleNumber("+123.456,78");
      assert.ok(result.isSuccess(), "Positive European format should succeed");
      assert.equal(result.getNumber(), 123456.78, "Positive European format");

      result = numberParser.parseFlexibleNumber("+123456");
      assert.ok(result.isSuccess(), "Positive integer should succeed");
      assert.equal(result.getNumber(), 123456, "Positive integer");
    });

    QUnit.test("handles likely thousands separator patterns", function (assert) {
      let result = numberParser.parseFlexibleNumber("1,000");
      assert.ok(result.isSuccess(), "Classic thousands: 1,000 should succeed");
      assert.equal(result.getNumber(), 1000, "Classic thousands: 1,000");

      result = numberParser.parseFlexibleNumber("10,000");
      assert.ok(result.isSuccess(), "Classic thousands: 10,000 should succeed");
      assert.equal(result.getNumber(), 10000, "Classic thousands: 10,000");

      result = numberParser.parseFlexibleNumber("1000,000");
      assert.ok(result.isSuccess(), "Large thousands pattern should succeed");
      assert.equal(result.getNumber(), 1000000, "Large thousands pattern");

      result = numberParser.parseFlexibleNumber("1.000");
      assert.ok(result.isSuccess(), "European thousands: 1.000 should succeed");
      assert.equal(result.getNumber(), 1000, "European thousands: 1.000");

      result = numberParser.parseFlexibleNumber("10.000");
      assert.ok(result.isSuccess(), "European thousands: 10.000 should succeed");
      assert.equal(result.getNumber(), 10000, "European thousands: 10.000");
    });

    QUnit.test("returns errors for ambiguous cases", function (assert) {
      // These cases are truly ambiguous - could be thousands or decimal
      let result = numberParser.parseFlexibleNumber("123,456");
      assert.ok(!result.isSuccess(), "Should fail for ambiguous comma case: 123,456");
      assert.ok(result.getError().includes("Ambiguous number format"), "Should return ambiguous error for comma case");

      result = numberParser.parseFlexibleNumber("123.456");
      assert.ok(!result.isSuccess(), "Should fail for ambiguous period case: 123.456");
      assert.ok(result.getError().includes("Ambiguous number format"), "Should return ambiguous error for period case");
    });

    QUnit.test("returns errors for invalid formats", function (assert) {
      let result = numberParser.parseFlexibleNumber("");
      assert.ok(!result.isSuccess(), "Should fail for empty string");
      assert.ok(result.getError().includes("empty"), "Should return empty error");

      result = numberParser.parseFlexibleNumber(null);
      assert.ok(!result.isSuccess(), "Should fail for null input");
      assert.ok(result.getError().includes("null"), "Should return null error");

      result = numberParser.parseFlexibleNumber("abc");
      assert.ok(!result.isSuccess(), "Should fail for non-numeric string");
      assert.ok(result.getError().includes("Invalid number format"), "Should return invalid format error");

      result = numberParser.parseFlexibleNumber("12,34.56,78");
      assert.ok(!result.isSuccess(), "Should fail for malformed mixed separators");
      assert.ok(result.getError().includes("number format"), "Should return format error");
    });

    QUnit.test("isAmbiguous correctly identifies ambiguous numbers", function (assert) {
      // These should be identified as ambiguous
      assert.ok(numberParser.isAmbiguous("123,456"), "123,456 should be ambiguous");
      assert.ok(numberParser.isAmbiguous("123.456"), "123.456 should be ambiguous");

      // These should NOT be ambiguous
      assert.notOk(numberParser.isAmbiguous("123,456.78"), "123,456.78 should not be ambiguous");
      assert.notOk(numberParser.isAmbiguous("123.456,78"), "123.456,78 should not be ambiguous");
      assert.notOk(numberParser.isAmbiguous("123456"), "123456 should not be ambiguous");
      assert.notOk(numberParser.isAmbiguous("123.45"), "123.45 should not be ambiguous");
      assert.notOk(numberParser.isAmbiguous("1,234"), "1,234 should not be ambiguous (common thousands pattern)");
      assert.ok(numberParser.isAmbiguous("123,456"), "123,456 should be ambiguous");
      assert.notOk(numberParser.isAmbiguous("10,000"), "10,000 should not be ambiguous (clear thousands)");
      assert.notOk(numberParser.isAmbiguous(""), "Empty string should not be ambiguous");
      assert.notOk(numberParser.isAmbiguous(null), "Null should not be ambiguous");
    });

    QUnit.test("getDisambiguationSuggestion provides helpful suggestions", function (assert) {
      const suggestion1 = numberParser.getDisambiguationSuggestion("123,456");
      assert.ok(suggestion1.includes("123,456,0") || suggestion1.includes("123,456.0"), "Should suggest adding ,0 or .0 for comma case");
      assert.ok(suggestion1.includes("disambiguate"), "Should mention disambiguate");

      const suggestion2 = numberParser.getDisambiguationSuggestion("123.456");
      assert.ok(suggestion2.includes("123.456.0") || suggestion2.includes("123.456,0"), "Should suggest adding .0 or ,0 for period case");
      assert.ok(suggestion2.includes("disambiguate"), "Should mention disambiguate");

      const suggestion3 = numberParser.getDisambiguationSuggestion("123,456.78");
      assert.ok(suggestion3.includes("not ambiguous"), "Should indicate non-ambiguous numbers");
    });

    QUnit.test("handles edge cases correctly", function (assert) {
      // Single digits
      let result = numberParser.parseFlexibleNumber("5");
      assert.ok(result.isSuccess(), "Single digit should succeed");
      assert.equal(result.getNumber(), 5, "Single digit");

      result = numberParser.parseFlexibleNumber("0");
      assert.ok(result.isSuccess(), "Zero should succeed");
      assert.equal(result.getNumber(), 0, "Zero");

      // Decimal edge cases
      result = numberParser.parseFlexibleNumber("0.5");
      assert.ok(result.isSuccess(), "Decimal less than one should succeed");
      assert.equal(result.getNumber(), 0.5, "Decimal less than one");

      result = numberParser.parseFlexibleNumber("0,5");
      assert.ok(result.isSuccess(), "European decimal less than one should succeed");
      assert.equal(result.getNumber(), 0.5, "European decimal less than one");

      // Large numbers
      result = numberParser.parseFlexibleNumber("1,000,000,000");
      assert.ok(result.isSuccess(), "Billion with commas should succeed");
      assert.equal(result.getNumber(), 1000000000, "Billion with commas");

      result = numberParser.parseFlexibleNumber("1.000.000.000");
      assert.ok(result.isSuccess(), "Billion with periods should succeed");
      assert.equal(result.getNumber(), 1000000000, "Billion with periods");
    });

    QUnit.test("handles whitespace correctly", function (assert) {
      let result = numberParser.parseFlexibleNumber("  123,456.78  ");
      assert.ok(result.isSuccess(), "Whitespace should be trimmed and succeed");
      assert.equal(result.getNumber(), 123456.78, "Whitespace should be trimmed");

      result = numberParser.parseFlexibleNumber("\t123456\t");
      assert.ok(result.isSuccess(), "Tabs should be trimmed and succeed");
      assert.equal(result.getNumber(), 123456, "Tabs should be trimmed");
    });

    QUnit.test("error messages contain original string", function (assert) {
      const result = numberParser.parseFlexibleNumber("123,456");
      assert.ok(!result.isSuccess(), "Should fail for ambiguous input");
      assert.ok(result.getError().includes("123,456"), "Error message should contain original string");
      assert.ok(result.getError().includes("Ambiguous"), "Error message should indicate ambiguity");

      const result2 = numberParser.parseFlexibleNumber("invalid");
      assert.ok(!result2.isSuccess(), "Should fail for invalid input");
      assert.ok(result2.getError().includes("invalid"), "Error message should contain original invalid string");
    });

    QUnit.test("mixed separator precedence rules", function (assert) {
      // Valid US format: comma as thousands, period as decimal
      let result = numberParser.parseFlexibleNumber("1,234.56");
      assert.ok(result.isSuccess(), "US format: comma thousands, period decimal should succeed");
      assert.equal(result.getNumber(), 1234.56, "US format: comma thousands, period decimal");

      // Valid European format: period as thousands, comma as decimal
      result = numberParser.parseFlexibleNumber("1.234,56");
      assert.ok(result.isSuccess(), "European format: period thousands, comma decimal should succeed");
      assert.equal(result.getNumber(), 1234.56, "European format: period thousands, comma decimal");
    });

    QUnit.test("performance with various input sizes", function (assert) {
      // Test that parsing doesn't hang or crash with various input patterns
      const testInputs = [
        "1",
        "12",
        "123",
        "1234",
        "12345",
        "123456",
        "1234567",
        "12345678",
        "123456789",
        "1234567890",
        "1,2,3,4,5,6,7,8,9,0",
        "1.2.3.4.5.6.7.8.9.0",
      ];

      testInputs.forEach((input) => {
        const result = numberParser.parseFlexibleNumber(input);
        if (result.isSuccess()) {
          assert.ok(typeof result.getNumber() === "number", `Should parse ${input} to a number`);
        } else {
          // Some inputs may legitimately fail (like malformed ones)
          assert.ok(result.getError().length > 0, `Error message should not be empty for input: ${input}`);
        }
      });
    });
  });
}

// Export for use in test runner
if (typeof window !== "undefined") {
  window.buildNumberParseUtilTests = buildNumberParseUtilTests;
}

export {buildNumberParseUtilTests};
