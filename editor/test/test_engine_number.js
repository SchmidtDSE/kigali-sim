import {EngineNumber, makeNumberUnambiguousString} from "engine_number";

function buildEngineNumberTests() {
  QUnit.module("EngineNumber", function () {
    const makeExample = () => {
      return new EngineNumber(1.23, "kg");
    };

    QUnit.test("initializes", function (assert) {
      const number = makeExample();
      assert.notDeepEqual(number, undefined);
    });

    QUnit.test("getValue", function (assert) {
      const number = makeExample();
      assert.closeTo(number.getValue(), 1.23, 0.0001);
    });

    QUnit.test("getUnits", function (assert) {
      const number = makeExample();
      assert.deepEqual(number.getUnits(), "kg");
    });

    QUnit.test("hasOriginalString - false when not provided", function (assert) {
      const number = new EngineNumber(1.23, "kg");
      assert.strictEqual(number.hasOriginalString(), false);
    });

    QUnit.test("hasOriginalString - true when provided", function (assert) {
      const number = new EngineNumber(1.23, "kg", "1.230");
      assert.strictEqual(number.hasOriginalString(), true);
    });

    QUnit.test("getOriginalString - returns original string", function (assert) {
      const number = new EngineNumber(1.23, "kg", "1.230");
      assert.strictEqual(number.getOriginalString(), "1.230");
    });

    QUnit.test("getOriginalString - returns null when not set", function (assert) {
      const number = new EngineNumber(1.23, "kg");
      assert.strictEqual(number.getOriginalString(), null);
    });
  });

  QUnit.module("makeNumberUnambiguousString", function () {
    QUnit.test("handles null, undefined, and NaN inputs", function (assert) {
      assert.strictEqual(makeNumberUnambiguousString(null), "null");
      assert.strictEqual(makeNumberUnambiguousString(undefined), "undefined");
      assert.strictEqual(makeNumberUnambiguousString(NaN), "NaN");
    });

    QUnit.test("formats integers with thousands separators and .0", function (assert) {
      assert.strictEqual(makeNumberUnambiguousString(0), "0.0");
      assert.strictEqual(makeNumberUnambiguousString(123), "123.0");
      assert.strictEqual(makeNumberUnambiguousString(1234), "1,234.0");
      assert.strictEqual(makeNumberUnambiguousString(123456), "123,456.0");
      assert.strictEqual(makeNumberUnambiguousString(1234567), "1,234,567.0");
    });

    QUnit.test("formats decimals with trailing zero for disambiguation", function (assert) {
      assert.strictEqual(makeNumberUnambiguousString(1.2), "1.20");
      assert.strictEqual(makeNumberUnambiguousString(1.234), "1.2340");
      assert.strictEqual(makeNumberUnambiguousString(123.456), "123.4560");
      assert.strictEqual(makeNumberUnambiguousString(0.5), "0.50");
      assert.strictEqual(makeNumberUnambiguousString(0.123), "0.1230");
    });

    QUnit.test("handles decimals that already end in zero", function (assert) {
      assert.strictEqual(makeNumberUnambiguousString(1.0), "1.0");
      assert.strictEqual(makeNumberUnambiguousString(1.20), "1.20");
      assert.strictEqual(makeNumberUnambiguousString(1.2340), "1.2340");
      assert.strictEqual(makeNumberUnambiguousString(123.4560), "123.4560");
    });

    QUnit.test("formats large decimals with thousands separators", function (assert) {
      assert.strictEqual(makeNumberUnambiguousString(1234.56), "1,234.560");
      assert.strictEqual(makeNumberUnambiguousString(123456.789), "123,456.7890");
      assert.strictEqual(makeNumberUnambiguousString(1234567.12), "1,234,567.120");
    });

    QUnit.test("preserves negative signs", function (assert) {
      assert.strictEqual(makeNumberUnambiguousString(-123), "-123.0");
      assert.strictEqual(makeNumberUnambiguousString(-1234), "-1,234.0");
      assert.strictEqual(makeNumberUnambiguousString(-1.234), "-1.2340");
      assert.strictEqual(makeNumberUnambiguousString(-123.456), "-123.4560");
      assert.strictEqual(makeNumberUnambiguousString(-1234.56), "-1,234.560");
    });

    QUnit.test("handles very small and very large numbers", function (assert) {
      assert.strictEqual(makeNumberUnambiguousString(0.001), "0.0010");
      assert.strictEqual(makeNumberUnambiguousString(0.0001), "0.00010");
      assert.strictEqual(makeNumberUnambiguousString(1000000), "1,000,000.0");
      assert.strictEqual(makeNumberUnambiguousString(1000000.1), "1,000,000.10");
    });

    QUnit.test("real-world formatting examples", function (assert) {
      // Test cases that solve the original 1.234 vs 1.2340 ambiguity issue
      assert.strictEqual(makeNumberUnambiguousString(1.234), "1.2340"); // Clearly decimal
      assert.strictEqual(makeNumberUnambiguousString(1234), "1,234.0"); // Clearly thousands
      assert.strictEqual(makeNumberUnambiguousString(123.45), "123.450"); // Clearly decimal
      assert.strictEqual(makeNumberUnambiguousString(12345), "12,345.0"); // Clearly thousands
    });
  });
}

export {buildEngineNumberTests};
