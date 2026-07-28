/**
 * Tests for BuildGenerationTracker.
 *
 * @license BSD, see LICENSE.md.
 */

import {BuildGenerationTracker} from "build_generation_tracker";

function buildBuildGenerationTrackerTests() {
  QUnit.module("BuildGenerationTracker", function () {
    QUnit.test("fresh tracker has no current generation", function (assert) {
      const tracker = new BuildGenerationTracker();
      assert.notOk(tracker.isCurrent(1),
        "The ID that startNewGeneration() would return next should not be current yet");
    });

    QUnit.test("startNewGeneration returns a new, distinct ID each call", function (assert) {
      const tracker = new BuildGenerationTracker();
      const id1 = tracker.startNewGeneration();
      const id2 = tracker.startNewGeneration();
      const id3 = tracker.startNewGeneration();

      assert.notEqual(id1, id2, "First and second IDs should differ");
      assert.notEqual(id2, id3, "Second and third IDs should differ");
      assert.notEqual(id1, id3, "First and third IDs should differ");
      assert.ok(id2 > id1, "IDs should increase");
      assert.ok(id3 > id2, "IDs should increase");
    });

    QUnit.test("the just-started generation is current", function (assert) {
      const tracker = new BuildGenerationTracker();
      const id = tracker.startNewGeneration();
      assert.ok(tracker.isCurrent(id), "Just-started generation should be current");
    });

    QUnit.test("starting a new generation invalidates the previous one", function (assert) {
      const tracker = new BuildGenerationTracker();
      const id1 = tracker.startNewGeneration();
      const id2 = tracker.startNewGeneration();

      assert.notOk(tracker.isCurrent(id1), "Older generation should no longer be current");
      assert.ok(tracker.isCurrent(id2), "Newer generation should be current");
    });

    QUnit.test("only the most recent of several generations is current", function (assert) {
      const tracker = new BuildGenerationTracker();
      const ids = [];
      for (let i = 0; i < 5; i++) {
        ids.push(tracker.startNewGeneration());
      }

      ids.forEach((id, index) => {
        const isLast = index === ids.length - 1;
        assert.equal(tracker.isCurrent(id), isLast,
          `Generation at index ${index} should ${isLast ? "" : "not "}be current`);
      });
    });

    QUnit.test("repeated isCurrent calls with the same id are stable", function (assert) {
      const tracker = new BuildGenerationTracker();
      const id = tracker.startNewGeneration();

      assert.ok(tracker.isCurrent(id), "First check should be current");
      assert.ok(tracker.isCurrent(id), "Second check should still be current");
      assert.ok(tracker.isCurrent(id), "Third check should still be current");
    });
  });
}

export {buildBuildGenerationTrackerTests};
