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

      result = numberParser.parseFlexibleNumber("1,234.0");
      assert.ok(result.isSuccess(), "US format thousands separator with .0 should succeed");
      assert.equal(result.getNumber(), 1234.0, "US format thousands separator with .0");

      result = numberParser.parseFlexibleNumber("12,345,678.90");
      assert.ok(result.isSuccess(), "US format with multiple thousands should succeed");
      assert.equal(result.getNumber(), 12345678.90, "US format with multiple thousands");

      result = numberParser.parseFlexibleNumber("123.45");
      assert.ok(result.isSuccess(), "Simple decimal number should succeed");
      assert.equal(result.getNumber(), 123.45, "Simple decimal number");
    });

    QUnit.test("rejects previously supported European formats", function (assert) {
      // These should now fail instead of succeeding
      let result = numberParser.parseFlexibleNumber("123.456,78");
      assert.ok(!result.isSuccess(), "European format should now fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");
      assert.ok(result.getError().includes("123,456.78"), "Should suggest UK equivalent");

      result = numberParser.parseFlexibleNumber("1.234.567,89");
      assert.ok(!result.isSuccess(), "European format with multiple thousands should now fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");
      assert.ok(result.getError().includes("1,234,567.89"), "Should suggest UK equivalent");

      // Note: Single comma decimals like "123,45" are European format and should be rejected
      result = numberParser.parseFlexibleNumber("123,45");
      assert.ok(!result.isSuccess(), "Single comma decimal should be rejected as European format");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");
      assert.ok(result.getError().includes("123.45"), "Should suggest UK equivalent");
    });

    QUnit.test("rejects European format 1.000.000,0 with proper error message", function (assert) {
      // Test the specific European format mentioned in the issue
      const result = numberParser.parseFlexibleNumber("1.000.000,0");

      assert.ok(!result.isSuccess(), "European format 1.000.000,0 should fail");
      assert.ok(result.getError().includes("Unsupported number format"),
        "Should detect as unsupported number format");
      assert.ok(result.getError().includes("1.000.000,0"),
        "Error should mention the original input");
      assert.ok(result.getError().includes("1,000,000.0"),
        "Should suggest UK equivalent 1,000,000.0");
      assert.ok(result.getError().includes("Kigali Sim requires comma for thousands separator and period for decimal point"),
        "Should explain the required format");

      // Also verify the error is NOT considered ambiguous
      assert.notOk(numberParser.isAmbiguous("1.000.000,0"),
        "European format should not be considered ambiguous, but an error");
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
      assert.ok(result.isSuccess(), "Negative UK format should succeed");
      assert.equal(result.getNumber(), -123456.78, "Negative UK format");

      result = numberParser.parseFlexibleNumber("-123.456,78");
      assert.ok(!result.isSuccess(), "Negative European format should now fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");

      result = numberParser.parseFlexibleNumber("-123456");
      assert.ok(result.isSuccess(), "Negative integer should succeed");
      assert.equal(result.getNumber(), -123456, "Negative integer");

      result = numberParser.parseFlexibleNumber("-1,234.0");
      assert.ok(result.isSuccess(), "Negative thousands with .0 should succeed");
      assert.equal(result.getNumber(), -1234.0, "Negative thousands with .0");
    });

    QUnit.test("handles positive sign numbers correctly", function (assert) {
      let result = numberParser.parseFlexibleNumber("+123,456.78");
      assert.ok(result.isSuccess(), "Positive UK format should succeed");
      assert.equal(result.getNumber(), 123456.78, "Positive UK format");

      result = numberParser.parseFlexibleNumber("+123.456,78");
      assert.ok(!result.isSuccess(), "Positive European format should now fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");

      result = numberParser.parseFlexibleNumber("+123456");
      assert.ok(result.isSuccess(), "Positive integer should succeed");
      assert.equal(result.getNumber(), 123456, "Positive integer");
    });

    QUnit.test("rejects European format with UK suggestions", function (assert) {
      // European mixed format should now error
      let result = numberParser.parseFlexibleNumber("1.234,56");
      assert.ok(!result.isSuccess(), "European mixed format should fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");
      assert.ok(result.getError().includes("1,234.56"), "Should suggest UK equivalent");

      // European decimal comma should error
      result = numberParser.parseFlexibleNumber("123,45");
      assert.ok(!result.isSuccess(), "European decimal comma should fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");
      assert.ok(result.getError().includes("123.45"), "Should suggest UK decimal equivalent");

      // Multiple periods as thousands should error
      result = numberParser.parseFlexibleNumber("1.234.567");
      assert.ok(!result.isSuccess(), "European thousands periods should fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");
      assert.ok(result.getError().includes("1,234,567"), "Should suggest UK thousands equivalent");
    });

    QUnit.test("previously ambiguous cases now work as UK format", function (assert) {
      // These should now parse successfully as UK format (no ambiguity errors)
      let result = numberParser.parseFlexibleNumber("123,456");
      assert.ok(result.isSuccess(), "123,456 should now parse as UK thousands separator");
      assert.equal(result.getNumber(), 123456, "123,456 should equal 123456");

      result = numberParser.parseFlexibleNumber("123.456");
      assert.ok(result.isSuccess(), "123.456 should now parse as UK decimal");
      assert.equal(result.getNumber(), 123.456, "123.456 should equal 123.456");

      result = numberParser.parseFlexibleNumber("1,000");
      assert.ok(result.isSuccess(), "1,000 should now parse as UK thousands");
      assert.equal(result.getNumber(), 1000, "1,000 should equal 1000");

      result = numberParser.parseFlexibleNumber("25,000");
      assert.ok(result.isSuccess(), "25,000 should now parse as UK thousands");
      assert.equal(result.getNumber(), 25000, "25,000 should equal 25000");
    });

    QUnit.test("error messages follow consistent format", function (assert) {
      const result = numberParser.parseFlexibleNumber("1.234,56");
      assert.ok(!result.isSuccess(), "European format should fail");

      const error = result.getError();
      assert.ok(error.includes("Unsupported number format: '1.234,56'"), "Should include detected format");
      assert.ok(error.includes("Please use: '1,234.56'"), "Should include suggestion");
      assert.ok(error.includes("Kigali Sim requires comma for thousands separator and period for decimal point"), "Should include explanation");
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

    QUnit.test("isAmbiguous reflects new UK-only behavior", function (assert) {
      // These should NO LONGER be identified as ambiguous
      assert.notOk(numberParser.isAmbiguous("123,456"), "123,456 should not be ambiguous in UK-only mode");
      assert.notOk(numberParser.isAmbiguous("123.456"), "123.456 should not be ambiguous in UK-only mode");
      assert.notOk(numberParser.isAmbiguous("1,234"), "1,234 should not be ambiguous in UK-only mode");
      assert.notOk(numberParser.isAmbiguous("10,000"), "10,000 should not be ambiguous in UK-only mode");

      // These should still work (non-ambiguous UK formats)
      assert.notOk(numberParser.isAmbiguous("123,456.78"), "UK mixed format should not be ambiguous");
      assert.notOk(numberParser.isAmbiguous("123456"), "Integer should not be ambiguous");
      assert.notOk(numberParser.isAmbiguous("123.45"), "UK decimal should not be ambiguous");

      // European formats should now fail (not be ambiguous, but give errors)
      assert.notOk(numberParser.isAmbiguous("123.456,78"), "European format should not be ambiguous (should error instead)");
      assert.notOk(numberParser.isAmbiguous(""), "Empty string should not be ambiguous");
      assert.notOk(numberParser.isAmbiguous(null), "Null should not be ambiguous");
    });

    QUnit.test("getDisambiguationSuggestion reflects UK-only behavior", function (assert) {
      // Previously ambiguous numbers are no longer ambiguous in UK-only mode
      const suggestion1 = numberParser.getDisambiguationSuggestion("123,456");
      assert.ok(suggestion1.includes("not ambiguous"), "123,456 should not be ambiguous in UK-only mode");

      const suggestion2 = numberParser.getDisambiguationSuggestion("123.456");
      assert.ok(suggestion2.includes("not ambiguous"), "123.456 should not be ambiguous in UK-only mode");

      const suggestion3 = numberParser.getDisambiguationSuggestion("123,456.78");
      assert.ok(suggestion3.includes("not ambiguous"), "UK mixed format should not be ambiguous");
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
      assert.ok(!result.isSuccess(), "European decimal comma should now fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");

      // Large numbers
      result = numberParser.parseFlexibleNumber("1,000,000,000");
      assert.ok(result.isSuccess(), "Billion with commas should succeed");
      assert.equal(result.getNumber(), 1000000000, "Billion with commas");

      result = numberParser.parseFlexibleNumber("1.000.000.000");
      assert.ok(!result.isSuccess(), "European thousands with periods should now fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");
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
      const result = numberParser.parseFlexibleNumber("123,45");
      assert.ok(!result.isSuccess(), "Should fail for European format input");
      assert.ok(result.getError().includes("123,45"), "Error message should contain original string");
      assert.ok(result.getError().includes("Unsupported number format"), "Error message should indicate unsupported format");

      const result2 = numberParser.parseFlexibleNumber("invalid");
      assert.ok(!result2.isSuccess(), "Should fail for invalid input");
      assert.ok(result2.getError().includes("invalid"), "Error message should contain original invalid string");
    });

    QUnit.test("mixed separator precedence rules - UK only", function (assert) {
      // Valid UK format: comma as thousands, period as decimal
      let result = numberParser.parseFlexibleNumber("1,234.56");
      assert.ok(result.isSuccess(), "UK format: comma thousands, period decimal should succeed");
      assert.equal(result.getNumber(), 1234.56, "UK format: comma thousands, period decimal");

      // European format should now fail
      result = numberParser.parseFlexibleNumber("1.234,56");
      assert.ok(!result.isSuccess(), "European format should now fail");
      assert.ok(result.getError().includes("Unsupported number format"), "Should detect unsupported format");
      assert.ok(result.getError().includes("1,234.56"), "Should suggest UK equivalent");
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
