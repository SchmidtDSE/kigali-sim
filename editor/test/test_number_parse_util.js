/* eslint-disable max-len */
import {NumberParseUtil} from "number_parse_util";

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
      assert.equal(numberParser.parseFlexibleNumber("123,456.78"), 123456.78, "US format with thousands and decimal");
      assert.equal(numberParser.parseFlexibleNumber("1,234"), 1234, "US format thousands separator");
      assert.equal(numberParser.parseFlexibleNumber("12,345,678.90"), 12345678.90, "US format with multiple thousands");
      assert.equal(numberParser.parseFlexibleNumber("123.45"), 123.45, "Simple decimal number");
    });

    QUnit.test("parses valid European format numbers", function (assert) {
      assert.equal(numberParser.parseFlexibleNumber("123.456,78"), 123456.78, "European format with thousands and decimal");
      assert.equal(numberParser.parseFlexibleNumber("1.234.567,89"), 1234567.89, "European format with multiple thousands");
      assert.equal(numberParser.parseFlexibleNumber("123,45"), 123.45, "European decimal separator");
    });

    QUnit.test("parses numbers without separators", function (assert) {
      assert.equal(numberParser.parseFlexibleNumber("123456"), 123456, "Integer without separators");
      assert.equal(numberParser.parseFlexibleNumber("123456.78"), 123456.78, "Decimal without thousands separator");
      assert.equal(numberParser.parseFlexibleNumber("1000"), 1000, "Round thousand without separator");
    });

    QUnit.test("handles negative numbers correctly", function (assert) {
      assert.equal(numberParser.parseFlexibleNumber("-123,456.78"), -123456.78, "Negative US format");
      assert.equal(numberParser.parseFlexibleNumber("-123.456,78"), -123456.78, "Negative European format");
      assert.equal(numberParser.parseFlexibleNumber("-123456"), -123456, "Negative integer");
      assert.equal(numberParser.parseFlexibleNumber("-1,234"), -1234, "Negative thousands");
    });

    QUnit.test("handles positive sign numbers correctly", function (assert) {
      assert.equal(numberParser.parseFlexibleNumber("+123,456.78"), 123456.78, "Positive US format");
      assert.equal(numberParser.parseFlexibleNumber("+123.456,78"), 123456.78, "Positive European format");
      assert.equal(numberParser.parseFlexibleNumber("+123456"), 123456, "Positive integer");
    });

    QUnit.test("handles likely thousands separator patterns", function (assert) {
      assert.equal(numberParser.parseFlexibleNumber("1,000"), 1000, "Classic thousands: 1,000");
      assert.equal(numberParser.parseFlexibleNumber("10,000"), 10000, "Classic thousands: 10,000");
      assert.equal(numberParser.parseFlexibleNumber("1000,000"), 1000000, "Large thousands pattern");
      assert.equal(numberParser.parseFlexibleNumber("1.000"), 1000, "European thousands: 1.000");
      assert.equal(numberParser.parseFlexibleNumber("10.000"), 10000, "European thousands: 10.000");
    });

    QUnit.test("throws errors for ambiguous cases", function (assert) {
      // These cases are truly ambiguous - could be thousands or decimal
      assert.throws(
        function () {
          numberParser.parseFlexibleNumber("123,456");
        },
        /Ambiguous number format/,
        "Should throw for ambiguous comma case: 123,456",
      );

      assert.throws(
        function () {
          numberParser.parseFlexibleNumber("123.456");
        },
        /Ambiguous number format/,
        "Should throw for ambiguous period case: 123.456",
      );
    });

    QUnit.test("throws errors for invalid formats", function (assert) {
      assert.throws(
        function () {
          numberParser.parseFlexibleNumber("");
        },
        /empty/,
        "Should throw for empty string",
      );

      assert.throws(
        function () {
          numberParser.parseFlexibleNumber(null);
        },
        /null/,
        "Should throw for null input",
      );

      assert.throws(
        function () {
          numberParser.parseFlexibleNumber("abc");
        },
        /Invalid number format/,
        "Should throw for non-numeric string",
      );

      assert.throws(
        function () {
          numberParser.parseFlexibleNumber("12,34.56,78");
        },
        /number format/,
        "Should throw for malformed mixed separators",
      );
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
      assert.notOk(numberParser.isAmbiguous("1,234"), "1,234 should not be ambiguous (likely thousands)");
      assert.notOk(numberParser.isAmbiguous("10,000"), "10,000 should not be ambiguous (clear thousands)");
      assert.notOk(numberParser.isAmbiguous(""), "Empty string should not be ambiguous");
      assert.notOk(numberParser.isAmbiguous(null), "Null should not be ambiguous");
    });

    QUnit.test("getDisambiguationSuggestion provides helpful suggestions", function (assert) {
      const suggestion1 = numberParser.getDisambiguationSuggestion("123,456");
      assert.ok(suggestion1.includes("123,456.0"), "Should suggest adding .0 for comma case");
      assert.ok(suggestion1.includes("thousands separator"), "Should mention thousands separator");

      const suggestion2 = numberParser.getDisambiguationSuggestion("123.456");
      assert.ok(suggestion2.includes("123.456.0"), "Should suggest adding .0 for period case");
      assert.ok(suggestion2.includes("thousands separator"), "Should mention thousands separator");

      const suggestion3 = numberParser.getDisambiguationSuggestion("123,456.78");
      assert.ok(suggestion3.includes("not ambiguous"), "Should indicate non-ambiguous numbers");
    });

    QUnit.test("handles edge cases correctly", function (assert) {
      // Single digits
      assert.equal(numberParser.parseFlexibleNumber("5"), 5, "Single digit");
      assert.equal(numberParser.parseFlexibleNumber("0"), 0, "Zero");

      // Decimal edge cases
      assert.equal(numberParser.parseFlexibleNumber("0.5"), 0.5, "Decimal less than one");
      assert.equal(numberParser.parseFlexibleNumber("0,5"), 0.5, "European decimal less than one");

      // Large numbers
      assert.equal(numberParser.parseFlexibleNumber("1,000,000,000"), 1000000000, "Billion with commas");
      assert.equal(numberParser.parseFlexibleNumber("1.000.000.000"), 1000000000, "Billion with periods");
    });

    QUnit.test("handles whitespace correctly", function (assert) {
      assert.equal(numberParser.parseFlexibleNumber("  123,456.78  "), 123456.78, "Whitespace should be trimmed");
      assert.equal(numberParser.parseFlexibleNumber("\t123456\t"), 123456, "Tabs should be trimmed");
    });

    QUnit.test("error messages contain original string", function (assert) {
      try {
        numberParser.parseFlexibleNumber("123,456");
        assert.ok(false, "Should have thrown an error");
      } catch (error) {
        assert.ok(error.message.includes("123,456"), "Error message should contain original string");
        assert.ok(error.message.includes("Ambiguous"), "Error message should indicate ambiguity");
      }

      try {
        numberParser.parseFlexibleNumber("invalid");
        assert.ok(false, "Should have thrown an error");
      } catch (error) {
        assert.ok(error.message.includes("invalid"), "Error message should contain original invalid string");
      }
    });

    QUnit.test("mixed separator precedence rules", function (assert) {
      // Valid US format: comma as thousands, period as decimal
      assert.equal(numberParser.parseFlexibleNumber("1,234.56"), 1234.56, "US format: comma thousands, period decimal");

      // Valid European format: period as thousands, comma as decimal
      assert.equal(numberParser.parseFlexibleNumber("1.234,56"), 1234.56, "European format: period thousands, comma decimal");
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
        try {
          const result = numberParser.parseFlexibleNumber(input);
          assert.ok(typeof result === "number", `Should parse ${input} to a number`);
        } catch (error) {
          // Some inputs may legitimately throw errors (like malformed ones)
          assert.ok(error.message.length > 0, `Error message should not be empty for input: ${input}`);
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
