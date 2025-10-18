/**
 * Tests for scenario name parsing functionality.
 *
 * @license BSD, see LICENSE.md.
 */

import {UiTranslatorCompiler} from "ui_translator";

function buildScenarioNameTests() {
  QUnit.module("Scenario Name Parsing", function () {
    QUnit.test("parses scenario names from UI-compatible simulation", function (assert) {
      const code = `
start default
  define application "test"
    uses substance "test"
      enable domestic
      equals 1 tCO2e / mt
      initial charge with 1 kg / unit for domestic
      set domestic to 100 mt during year 1
    end substance
  end application
end default

start simulations
  simulate "business as usual" from years 1 to 10
  simulate "with policy" using "test policy" from years 1 to 10
  simulate "alternative scenario" from years 5 to 15
end simulations
`;

      const compiler = new UiTranslatorCompiler();
      const result = compiler.compile(code);

      assert.strictEqual(result.getErrors().length, 0, "Should compile without errors");

      const program = result.getProgram();
      assert.ok(program, "Should have a program object");
      assert.ok(program.getIsCompatible(), "Program should be UI-compatible");

      const scenarioNames = program.getScenarioNames();
      assert.strictEqual(scenarioNames.length, 3, "Should have 3 scenarios");
      assert.strictEqual(scenarioNames[0], "business as usual", "First scenario name");
      assert.strictEqual(scenarioNames[1], "with policy", "Second scenario name");
      assert.strictEqual(scenarioNames[2], "alternative scenario", "Third scenario name");
    });

    QUnit.test("parses scenario names with multiple policies", function (assert) {
      const code = `
start default
  define application "test"
    uses substance "test"
      enable domestic
      equals 1 tCO2e / mt
      initial charge with 1 kg / unit for domestic
      set domestic to 100 mt during year 1
    end substance
  end application
end default

start policy "policy1"
  modify application "test"
    modify substance "test"
      cap domestic to 50 % during years 1 to 5
    end substance
  end application
end policy

start policy "policy2"
  modify application "test"
    modify substance "test"
      cap domestic to 25 % during years 6 to 10
    end substance
  end application
end policy

start simulations
  simulate "baseline" from years 1 to 10
  simulate "with policy1" using "policy1" from years 1 to 10
  simulate "combined" using "policy1" then "policy2" from years 1 to 10
end simulations
`;

      const compiler = new UiTranslatorCompiler();
      const result = compiler.compile(code);

      assert.strictEqual(result.getErrors().length, 0, "Should compile without errors");

      const program = result.getProgram();
      assert.ok(program, "Should have a program object");

      const scenarioNames = program.getScenarioNames();
      assert.strictEqual(scenarioNames.length, 3, "Should have 3 scenarios");
      assert.strictEqual(scenarioNames[0], "baseline", "First scenario name");
      assert.strictEqual(scenarioNames[1], "with policy1", "Second scenario name");
      assert.strictEqual(scenarioNames[2], "combined", "Third scenario name");
    });

    QUnit.test("handles simulations with trials correctly", function (assert) {
      const code = `
start default
  define application "test"
    uses substance "test"
      enable domestic
      equals 1 tCO2e / mt
      initial charge with 1 kg / unit for domestic
      set domestic to 100 mt during year 1
    end substance
  end application
end default

start simulations
  simulate "probabilistic test" from years 1 to 10 across 100 trials
  simulate "normal test" from years 1 to 10
end simulations
`;

      const compiler = new UiTranslatorCompiler();
      const result = compiler.compile(code);

      assert.strictEqual(result.getErrors().length, 0, "Should compile without errors");

      const program = result.getProgram();
      assert.ok(program, "Should have a program object");
      assert.notOk(program.getIsCompatible(), "Program should NOT be UI-compatible due to trials");

      const scenarioNames = program.getScenarioNames();
      // Simulations with "across X trials" are not parsed as SimulationScenario objects
      // Only the normal simulation without trials should be in the list
      assert.strictEqual(scenarioNames.length, 1,
        "Should have 1 scenario (trials simulation not included)");
      assert.strictEqual(scenarioNames[0], "normal test", "Scenario name");
    });

    QUnit.test("handles empty simulations stanza", function (assert) {
      const code = `
start default
  define application "test"
    uses substance "test"
      enable domestic
      equals 1 tCO2e / mt
      initial charge with 1 kg / unit for domestic
      set domestic to 100 mt during year 1
    end substance
  end application
end default
`;

      const compiler = new UiTranslatorCompiler();
      const result = compiler.compile(code);

      assert.strictEqual(result.getErrors().length, 0, "Should compile without errors");

      const program = result.getProgram();
      assert.ok(program, "Should have a program object");

      const scenarioNames = program.getScenarioNames();
      assert.strictEqual(scenarioNames.length, 0, "Should have 0 scenarios");
    });

    QUnit.test("preserves scenario name order", function (assert) {
      const code = `
start default
  define application "test"
    uses substance "test"
      enable domestic
      equals 1 tCO2e / mt
      initial charge with 1 kg / unit for domestic
      set domestic to 100 mt during year 1
    end substance
  end application
end default

start simulations
  simulate "Zebra" from years 1 to 10
  simulate "Apple" from years 1 to 10
  simulate "Mango" from years 1 to 10
end simulations
`;

      const compiler = new UiTranslatorCompiler();
      const result = compiler.compile(code);

      const program = result.getProgram();
      const scenarioNames = program.getScenarioNames();

      assert.deepEqual(scenarioNames, ["Zebra", "Apple", "Mango"],
        "Should preserve scenario order as defined, not alphabetical");
    });
  });
}

export {buildScenarioNameTests};
