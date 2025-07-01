import {EngineNumber} from "engine_number";

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
  });
}

export {buildEngineNumberTests};
