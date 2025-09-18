import {ParsedYear, YearMatcher} from "duration";

function buildDurationTests() {
  QUnit.module("ParsedYear", function () {
    QUnit.module("constructor", function () {
      QUnit.test("initializes with numeric year", function (assert) {
        const parsedYear = new ParsedYear(2025);
        assert.notDeepEqual(parsedYear, undefined);
      });

      QUnit.test("initializes with string year", function (assert) {
        const parsedYear = new ParsedYear("beginning");
        assert.notDeepEqual(parsedYear, undefined);
      });

      QUnit.test("initializes with null year", function (assert) {
        const parsedYear = new ParsedYear(null);
        assert.notDeepEqual(parsedYear, undefined);
      });

      QUnit.test("preserves original string when provided", function (assert) {
        const parsedYear = new ParsedYear(2025, "2025");
        assert.strictEqual(parsedYear.getYearStr(), "2025");
      });

      QUnit.test("derives string representation when original not provided", function (assert) {
        const parsedYear = new ParsedYear(2025);
        assert.strictEqual(parsedYear.getYearStr(), "2025");
      });
    });

    QUnit.module("hasFiniteNumericYear", function () {
      QUnit.test("returns true for positive integer", function (assert) {
        const parsedYear = new ParsedYear(2025);
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), true);
      });

      QUnit.test("returns true for negative integer", function (assert) {
        const parsedYear = new ParsedYear(-5);
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), true);
      });

      QUnit.test("returns true for zero", function (assert) {
        const parsedYear = new ParsedYear(0);
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), true);
      });

      QUnit.test("returns true for decimal numbers", function (assert) {
        const parsedYear = new ParsedYear(2025.5);
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), true);
      });

      QUnit.test("returns false for special string 'beginning'", function (assert) {
        const parsedYear = new ParsedYear("beginning");
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), false);
      });

      QUnit.test("returns false for special string 'onwards'", function (assert) {
        const parsedYear = new ParsedYear("onwards");
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), false);
      });

      QUnit.test("returns false for null", function (assert) {
        const parsedYear = new ParsedYear(null);
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), false);
      });

      QUnit.test("returns false for NaN", function (assert) {
        const parsedYear = new ParsedYear(NaN);
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), false);
      });

      QUnit.test("returns false for Infinity", function (assert) {
        const parsedYear = new ParsedYear(Infinity);
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), false);
      });

      QUnit.test("returns false for negative Infinity", function (assert) {
        const parsedYear = new ParsedYear(-Infinity);
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), false);
      });

      QUnit.test("returns true for numeric strings", function (assert) {
        const parsedYear = new ParsedYear("2029");
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), true);
      });

      QUnit.test("should handle numeric strings as numeric years", function (assert) {
        // This test verifies the fix: numeric strings should be treated as numbers
        const parsedYear = new ParsedYear("2029");

        // After fix - these should pass:
        assert.strictEqual(parsedYear.hasFiniteNumericYear(), true);
        assert.strictEqual(parsedYear.getNumericYear(), 2029);
        assert.strictEqual(parsedYear.getYearStr(), "2029");
      });
    });

    QUnit.module("getNumericYear", function () {
      QUnit.test("returns numeric value for finite numbers", function (assert) {
        const parsedYear = new ParsedYear(2025);
        assert.strictEqual(parsedYear.getNumericYear(), 2025);
      });

      QUnit.test("returns numeric value for negative numbers", function (assert) {
        const parsedYear = new ParsedYear(-5);
        assert.strictEqual(parsedYear.getNumericYear(), -5);
      });

      QUnit.test("returns numeric value for zero", function (assert) {
        const parsedYear = new ParsedYear(0);
        assert.strictEqual(parsedYear.getNumericYear(), 0);
      });

      QUnit.test("returns numeric value for decimals", function (assert) {
        const parsedYear = new ParsedYear(2025.5);
        assert.strictEqual(parsedYear.getNumericYear(), 2025.5);
      });

      QUnit.test("returns null for special string", function (assert) {
        const parsedYear = new ParsedYear("beginning");
        assert.strictEqual(parsedYear.getNumericYear(), null);
      });

      QUnit.test("returns null for null year", function (assert) {
        const parsedYear = new ParsedYear(null);
        assert.strictEqual(parsedYear.getNumericYear(), null);
      });

      QUnit.test("returns null for NaN", function (assert) {
        const parsedYear = new ParsedYear(NaN);
        assert.strictEqual(parsedYear.getNumericYear(), null);
      });

      QUnit.test("returns null for Infinity", function (assert) {
        const parsedYear = new ParsedYear(Infinity);
        assert.strictEqual(parsedYear.getNumericYear(), null);
      });
    });

    QUnit.module("getYearStr", function () {
      QUnit.test("returns original string when provided", function (assert) {
        const parsedYear = new ParsedYear(2025, "2025");
        assert.strictEqual(parsedYear.getYearStr(), "2025");
      });

      QUnit.test("returns original string with formatting preserved", function (assert) {
        const parsedYear = new ParsedYear(2025, "2,025");
        assert.strictEqual(parsedYear.getYearStr(), "2,025");
      });

      QUnit.test("returns string representation when original not provided", function (assert) {
        const parsedYear = new ParsedYear(2025);
        assert.strictEqual(parsedYear.getYearStr(), "2025");
      });

      QUnit.test("returns special string value", function (assert) {
        const parsedYear = new ParsedYear("beginning");
        assert.strictEqual(parsedYear.getYearStr(), "beginning");
      });

      QUnit.test("returns empty string for null year", function (assert) {
        const parsedYear = new ParsedYear(null);
        assert.strictEqual(parsedYear.getYearStr(), "");
      });

      QUnit.test("handles negative numbers", function (assert) {
        const parsedYear = new ParsedYear(-5);
        assert.strictEqual(parsedYear.getYearStr(), "-5");
      });

      QUnit.test("handles zero", function (assert) {
        const parsedYear = new ParsedYear(0);
        assert.strictEqual(parsedYear.getYearStr(), "0");
      });
    });

    QUnit.module("equals", function () {
      QUnit.test("returns true for same numeric years", function (assert) {
        const year1 = new ParsedYear(2025);
        const year2 = new ParsedYear(2025);
        assert.strictEqual(year1.equals(year2), true);
      });

      QUnit.test("returns false for different numeric years", function (assert) {
        const year1 = new ParsedYear(2025);
        const year2 = new ParsedYear(2026);
        assert.strictEqual(year1.equals(year2), false);
      });

      QUnit.test("compares numeric values ignoring original string differences", function (assert) {
        const year1 = new ParsedYear(2025, "2025");
        const year2 = new ParsedYear(2025, "2,025");
        assert.strictEqual(year1.equals(year2), true);
      });

      QUnit.test("returns true for same special strings", function (assert) {
        const year1 = new ParsedYear("beginning");
        const year2 = new ParsedYear("beginning");
        assert.strictEqual(year1.equals(year2), true);
      });

      QUnit.test("returns false for different special strings", function (assert) {
        const year1 = new ParsedYear("beginning");
        const year2 = new ParsedYear("onwards");
        assert.strictEqual(year1.equals(year2), false);
      });

      QUnit.test("returns true for both null years", function (assert) {
        const year1 = new ParsedYear(null);
        const year2 = new ParsedYear(null);
        assert.strictEqual(year1.equals(year2), true);
      });

      QUnit.test("returns false for numeric vs special string", function (assert) {
        const year1 = new ParsedYear(2025);
        const year2 = new ParsedYear("beginning");
        assert.strictEqual(year1.equals(year2), false);
      });

      QUnit.test("returns false for numeric vs null", function (assert) {
        const year1 = new ParsedYear(2025);
        const year2 = new ParsedYear(null);
        assert.strictEqual(year1.equals(year2), false);
      });

      QUnit.test("returns false for special string vs null", function (assert) {
        const year1 = new ParsedYear("beginning");
        const year2 = new ParsedYear(null);
        assert.strictEqual(year1.equals(year2), false);
      });

      QUnit.test("returns false for non-ParsedYear object", function (assert) {
        const year1 = new ParsedYear(2025);
        const notParsedYear = {year: 2025};
        assert.strictEqual(year1.equals(notParsedYear), false);
      });

      QUnit.test("handles NaN values", function (assert) {
        const year1 = new ParsedYear(NaN);
        const year2 = new ParsedYear(NaN);
        // NaN values should be compared by string representation
        assert.strictEqual(year1.equals(year2), true);
      });

      QUnit.test("handles zero values", function (assert) {
        const year1 = new ParsedYear(0);
        const year2 = new ParsedYear(0);
        assert.strictEqual(year1.equals(year2), true);
      });
    });
  });

  QUnit.module("YearMatcher", function () {
    QUnit.module("constructor", function () {
      QUnit.test("initializes with numeric start and end", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), 2025);
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), 2030);
      });

      QUnit.test("initializes with same start and end", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2025));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), 2025);
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), 2025);
      });

      QUnit.test("rearranges start and end when start > end", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2030), new ParsedYear(2025));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), 2025);
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), 2030);
      });

      QUnit.test("handles null start with numeric end", function (assert) {
        const yearMatcher = new YearMatcher(null, new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getStart(), null);
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), 2030);
      });

      QUnit.test("handles numeric start with null end", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), null);
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), 2025);
        assert.strictEqual(yearMatcher.getEnd(), null);
      });

      QUnit.test("handles both null start and end", function (assert) {
        const yearMatcher = new YearMatcher(null, null);
        assert.strictEqual(yearMatcher.getStart(), null);
        assert.strictEqual(yearMatcher.getEnd(), null);
      });

      QUnit.test("handles special string 'beginning' as start", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear("beginning"), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getStart().getYearStr(), "beginning");
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), 2030);
      });

      QUnit.test("handles special string 'onwards' as end", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear("onwards"));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), 2025);
        assert.strictEqual(yearMatcher.getEnd().getYearStr(), "onwards");
      });

      QUnit.test("handles both special strings", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear("beginning"), new ParsedYear("onwards"));
        assert.strictEqual(yearMatcher.getStart().getYearStr(), "beginning");
        assert.strictEqual(yearMatcher.getEnd().getYearStr(), "onwards");
      });

      QUnit.test("does not rearrange when start is special string", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear("beginning"), new ParsedYear(2025));
        assert.strictEqual(yearMatcher.getStart().getYearStr(), "beginning");
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), 2025);
      });

      QUnit.test("does not rearrange when end is special string", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2030), new ParsedYear("onwards"));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), 2030);
        assert.strictEqual(yearMatcher.getEnd().getYearStr(), "onwards");
      });

      QUnit.test("handles negative years", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(-10), new ParsedYear(-5));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), -10);
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), -5);
      });

      QUnit.test("rearranges negative years when needed", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(-5), new ParsedYear(-10));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), -10);
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), -5);
      });

      QUnit.test("handles zero years", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(0), new ParsedYear(0));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), 0);
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), 0);
      });
    });

    QUnit.module("getInRange", function () {
      QUnit.test("returns true for year within range", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getInRange(2027), true);
      });

      QUnit.test("returns true for year at start boundary", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getInRange(2025), true);
      });

      QUnit.test("returns true for year at end boundary", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getInRange(2030), true);
      });

      QUnit.test("returns false for year before range", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getInRange(2024), false);
      });

      QUnit.test("returns false for year after range", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getInRange(2031), false);
      });

      QUnit.test("returns true for any year when start is null", function (assert) {
        const yearMatcher = new YearMatcher(null, new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getInRange(2000), true);
        assert.strictEqual(yearMatcher.getInRange(2030), true);
        assert.strictEqual(yearMatcher.getInRange(2031), false);
      });

      QUnit.test("returns true for any year when end is null", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), null);
        assert.strictEqual(yearMatcher.getInRange(2025), true);
        assert.strictEqual(yearMatcher.getInRange(2100), true);
        assert.strictEqual(yearMatcher.getInRange(2024), false);
      });

      QUnit.test("returns true for any year when both start and end are null", function (assert) {
        const yearMatcher = new YearMatcher(null, null);
        assert.strictEqual(yearMatcher.getInRange(1000), true);
        assert.strictEqual(yearMatcher.getInRange(3000), true);
        assert.strictEqual(yearMatcher.getInRange(-500), true);
      });

      QUnit.test("handles single year range", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2025));
        assert.strictEqual(yearMatcher.getInRange(2025), true);
        assert.strictEqual(yearMatcher.getInRange(2024), false);
        assert.strictEqual(yearMatcher.getInRange(2026), false);
      });

      QUnit.test("handles negative year ranges", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(-10), new ParsedYear(-5));
        assert.strictEqual(yearMatcher.getInRange(-7), true);
        assert.strictEqual(yearMatcher.getInRange(-10), true);
        assert.strictEqual(yearMatcher.getInRange(-5), true);
        assert.strictEqual(yearMatcher.getInRange(-11), false);
        assert.strictEqual(yearMatcher.getInRange(-4), false);
      });

      QUnit.test("handles zero in range", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(-5), new ParsedYear(5));
        assert.strictEqual(yearMatcher.getInRange(0), true);
      });

      QUnit.test("handles decimal years", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025.1), new ParsedYear(2025.9));
        assert.strictEqual(yearMatcher.getInRange(2025.5), true);
        assert.strictEqual(yearMatcher.getInRange(2025.0), false);
        assert.strictEqual(yearMatcher.getInRange(2026.0), false);
      });
    });

    QUnit.module("getStart", function () {
      QUnit.test("returns numeric start value", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), 2025);
      });

      QUnit.test("returns null start value", function (assert) {
        const yearMatcher = new YearMatcher(null, new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getStart(), null);
      });

      QUnit.test("returns special string start value", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear("beginning"), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getStart().getYearStr(), "beginning");
      });

      QUnit.test("returns negative start value", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(-10), new ParsedYear(-5));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), -10);
      });

      QUnit.test("returns zero start value", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(0), new ParsedYear(5));
        assert.strictEqual(yearMatcher.getStart().getNumericYear(), 0);
      });
    });

    QUnit.module("getEnd", function () {
      QUnit.test("returns numeric end value", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2030));
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), 2030);
      });

      QUnit.test("returns null end value", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), null);
        assert.strictEqual(yearMatcher.getEnd(), null);
      });

      QUnit.test("returns special string end value", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear("onwards"));
        assert.strictEqual(yearMatcher.getEnd().getYearStr(), "onwards");
      });

      QUnit.test("returns negative end value", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(-10), new ParsedYear(-5));
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), -5);
      });

      QUnit.test("returns zero end value", function (assert) {
        const yearMatcher = new YearMatcher(new ParsedYear(-5), new ParsedYear(0));
        assert.strictEqual(yearMatcher.getEnd().getNumericYear(), 0);
      });
    });

    QUnit.module("numeric string handling bug", function () {
      QUnit.test("verifies fix for numeric string years from UI", function (assert) {
        // This verifies the fix for the UI bug where "2029" and "2034" from HTML inputs
        // now work properly with YearMatcher
        const startYear = new ParsedYear("2029"); // From HTML input .value
        const endYear = new ParsedYear("2034"); // From HTML input .value
        const yearMatcher = new YearMatcher(startYear, endYear);

        // After fix - these should work correctly
        const start = yearMatcher.getStart();
        const end = yearMatcher.getEnd();

        // Now properly returns numeric values for processing
        assert.strictEqual(start.getNumericYear(), 2029, "Start year now correctly parsed as 2029");
        assert.strictEqual(end.getNumericYear(), 2034, "End year now correctly parsed as 2034");

        // String representations should still work
        assert.strictEqual(start.getYearStr(), "2029", "String representation should still work");
        assert.strictEqual(end.getYearStr(), "2034", "String representation should still work");
      });

      QUnit.test("preserves special string keywords", function (assert) {
        // Verify that special QubecTalk keywords are not converted to numbers
        const beginningYear = new ParsedYear("beginning");
        const onwardsYear = new ParsedYear("onwards");

        // Special strings should not be treated as numeric
        assert.strictEqual(beginningYear.hasFiniteNumericYear(), false);
        assert.strictEqual(beginningYear.getNumericYear(), null);
        assert.strictEqual(beginningYear.getYearStr(), "beginning");

        assert.strictEqual(onwardsYear.hasFiniteNumericYear(), false);
        assert.strictEqual(onwardsYear.getNumericYear(), null);
        assert.strictEqual(onwardsYear.getYearStr(), "onwards");
      });

      QUnit.test("handles edge cases of numeric strings", function (assert) {
        // Test various numeric string formats
        const decimalYear = new ParsedYear("2029.5");
        assert.strictEqual(decimalYear.hasFiniteNumericYear(), true);
        assert.strictEqual(decimalYear.getNumericYear(), 2029.5);

        const negativeYear = new ParsedYear("-100");
        assert.strictEqual(negativeYear.hasFiniteNumericYear(), true);
        assert.strictEqual(negativeYear.getNumericYear(), -100);

        const zeroYear = new ParsedYear("0");
        assert.strictEqual(zeroYear.hasFiniteNumericYear(), true);
        assert.strictEqual(zeroYear.getNumericYear(), 0);
      });
    });
  });
}

export {buildDurationTests};
