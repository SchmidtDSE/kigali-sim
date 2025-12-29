import {EngineNumber} from "engine_number";
import {YearMatcher, ParsedYear} from "duration";
import {
  AboutStanza,
  Application,
  Command,
  DefinitionalStanza,
  LimitCommand,
  Program,
  RechargeCommand,
  RecycleCommand,
  ReplaceCommand,
  RetireCommand,
  SimulationScenario,
  SimulationStanza,
  SubstanceBuilder,
} from "ui_translator_components";
import {
  buildAddCode,
  finalizeCodePieces,
  indent,
} from "ui_translator_util";

function createWithCommands(name, isModification, commands) {
  const substanceBuilder = new SubstanceBuilder(name, isModification);
  commands.forEach((command) => {
    substanceBuilder.addCommand(command);
  });
  return substanceBuilder.build(true);
}

function createWithCommand(name, isModification, command) {
  return createWithCommands(name, isModification, [command]);
}

function buildTestApplication(isMod) {
  const command = new Command("setVal", "domestic", new EngineNumber(5, "kg / unit"), null);
  const substance = createWithCommand("sub", isMod, command);
  const application = new Application("app", [substance], isMod, true);
  return application;
}

function buildUiTranslatorReverseTests() {
  QUnit.module("UiTranslatorCompilerReverse", function () {
    QUnit.test("indents", function (assert) {
      const result = indent(["a", "b"], 2);
      assert.equal(result.length, 2);
      assert.deepEqual(result[0], "  a");
      assert.deepEqual(result[1], "  b");
    });

    QUnit.test("builds add code", function (assert) {
      const pieces = [];
      const addCode = buildAddCode(pieces);
      addCode("a", 2);
      assert.equal(pieces.length, 1);
      assert.deepEqual(pieces[0], "  a");
    });

    QUnit.test("finalizes code pieces", function (assert) {
      const pieces = ["a", "b", "c"];
      const result = finalizeCodePieces(pieces);
      assert.deepEqual(result, "a\nb\nc");
    });

    QUnit.test("initial charges substances", function (assert) {
      const command = new Command(
        "initial charge",
        "domestic",
        new EngineNumber(5, "kg / unit"),
        null,
      );
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf('uses substance "test"'), -1);
      assert.notEqual(code.indexOf("initial charge with 5 kg / unit for domestic"), -1);
    });

    QUnit.test("caps substances", function (assert) {
      const command = new LimitCommand(
        "cap", "domestic", new EngineNumber(5, "mt"), null, null, "",
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("cap domestic to 5 mt"), -1);
    });
    QUnit.test("recharges substances", function (assert) {
      const populationEngineNumber = new EngineNumber("10", "%", "10");
      const volumeEngineNumber = new EngineNumber("0.12", "kg / unit", "0.12");
      const command = new RechargeCommand(populationEngineNumber, volumeEngineNumber, null);
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("recharge 10 % with 0.12 kg / unit"), -1);
    });
    QUnit.test("recharges substances with duration", function (assert) {
      const yearMatcher = new YearMatcher(new ParsedYear(2025), new ParsedYear(2030));
      const populationEngineNumber = new EngineNumber("5", "% / year", "5");
      const volumeEngineNumber = new EngineNumber("0.85", "kg / unit", "0.85");
      const command = new RechargeCommand(populationEngineNumber, volumeEngineNumber, yearMatcher);
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      const expectedText = "recharge 5 % / year with 0.85 kg / unit during years 2025 to 2030";
      assert.notEqual(code.indexOf(expectedText), -1);
    });

    QUnit.test("recharges in policy modification context", function (assert) {
      const yearMatcher = new YearMatcher(new ParsedYear(2), new ParsedYear(5));
      const populationEngineNumber = new EngineNumber("15", "%", "15");
      const volumeEngineNumber = new EngineNumber("0.2", "kg / unit", "0.2");
      const command = new RechargeCommand(populationEngineNumber, volumeEngineNumber, yearMatcher);
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf('modify substance "test"'), -1);
      assert.notEqual(code.indexOf("recharge 15 % with 0.2 kg / unit during years 2 to 5"), -1);
    });

    QUnit.test("changes substances", function (assert) {
      const command = new Command(
        "change",
        "domestic",
        new EngineNumber("+5", "% / year"),
        null,
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("change domestic by +5 % / year"), -1);
    });

    QUnit.test("equalss from substances", function (assert) {
      const command = new Command("equals", null, new EngineNumber("5", "tCO2e / unit"), null);
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("equals 5 tCO2e / unit"), -1);
    });

    QUnit.test("recharges substances", function (assert) {
      const populationEngineNumber = new EngineNumber("10", "% / year", "10");
      const volumeEngineNumber = new EngineNumber("5", "kg / unit", "5");
      const command = new RechargeCommand(populationEngineNumber, volumeEngineNumber, null);
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("recharge 10 % / year with 5 kg / unit"), -1);
    });

    QUnit.test("recycles substances", function (assert) {
      const command = new Command(
        "recycle",
        new EngineNumber("10", "%"),
        new EngineNumber("100", "%"),
        null,
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("recover 10 % with 100 % reuse"), -1);
    });

    QUnit.test("replaces substances", function (assert) {
      const command = new ReplaceCommand(new EngineNumber("10", "%"), "domestic", "other", null);
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf('replace 10 % of domestic with "other"'), -1);
    });

    QUnit.test("retires substances", function (assert) {
      const command = new RetireCommand(new EngineNumber("10", "%"), null, false);
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("retire 10 %"), -1);
    });

    QUnit.test("retires substances with replacement", function (assert) {
      const command = new RetireCommand(new EngineNumber("5", "% / year"), null, true);
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("retire 5 % / year with replacement"), -1);
    });

    QUnit.test("retires substances without replacement flag", function (assert) {
      const command = new RetireCommand(new EngineNumber("5", "% / year"), null, false);
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.equal(code.indexOf("with replacement"), -1);
      assert.notEqual(code.indexOf("retire 5 % / year"), -1);
    });

    QUnit.test("retires substances with replacement and duration", function (assert) {
      const command = new RetireCommand(
        new EngineNumber("10", "%"),
        new YearMatcher(new ParsedYear(1), new ParsedYear(5)),
        true,
      );
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("retire 10 % with replacement during years 1 to 5"), -1);
    });

    QUnit.test("sets values in substances", function (assert) {
      const command = new Command("setVal", "domestic", new EngineNumber("10", "mt"), null);
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("set domestic to 10 mt"), -1);
    });

    QUnit.test("supports duration single year", function (assert) {
      const command = new Command(
        "setVal",
        "domestic",
        new EngineNumber("10", "mt"),
        new YearMatcher(new ParsedYear(1), new ParsedYear(1)),
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("set domestic to 10 mt during year 1"), -1);
    });

    QUnit.test("supports duration multiple years", function (assert) {
      const command = new RetireCommand(
        new EngineNumber("10", "%"),
        new YearMatcher(new ParsedYear(2), new ParsedYear(5)),
        false,
      );
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("retire 10 % during years 2 to 5"), -1);
    });

    QUnit.test("supports duration multiple years reversed", function (assert) {
      const command = new RetireCommand(
        new EngineNumber("10", "%"),
        new YearMatcher(new ParsedYear(5), new ParsedYear(2)),
        false,
      );
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("retire 10 % during years 2 to 5"), -1);
    });

    QUnit.test("supports duration with min year", function (assert) {
      const command = new ReplaceCommand(
        new EngineNumber("10", "%"),
        "domestic",
        "other",
        new YearMatcher(new ParsedYear(2), null),
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(
        code.indexOf('replace 10 % of domestic with "other" during years 2 to onwards'),
        -1,
      );
    });

    QUnit.test("supports duration with max year", function (assert) {
      const command = new Command(
        "recycle",
        new EngineNumber("10", "%"),
        new EngineNumber("100", "%"),
        new YearMatcher(null, new ParsedYear(5)),
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(
        code.indexOf("recover 10 % with 100 % reuse during years beginning to 5"),
        -1,
      );
    });

    QUnit.test("supports complex substances", function (assert) {
      const setVal = new Command(
        "setVal",
        "domestic",
        new EngineNumber(5, "kg / unit"),
        new YearMatcher(new ParsedYear(1), new ParsedYear(1)),
      );
      const cap = new LimitCommand(
        "cap",
        "domestic",
        new EngineNumber(5, "mt"),
        new YearMatcher(new ParsedYear(3), new ParsedYear(4)),
        "import",
        "",
      );
      const substance = createWithCommands("test", true, [setVal, cap]);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf('modify substance "test"'), -1);
      assert.notEqual(code.indexOf("set domestic to 5 kg / unit during year 1"), -1);
      assert.notEqual(
        code.indexOf('cap domestic to 5 mt displacing "import" during years 3 to 4'),
        -1,
      );
    });

    QUnit.test("converts applications to code", function (assert) {
      const application = buildTestApplication(false);
      const code = application.toCode(0);
      assert.notEqual(code.indexOf('define application "app"'), -1);
      assert.notEqual(code.indexOf('uses substance "sub"'), -1);
    });

    QUnit.test("converts simulation stanzas to code", function (assert) {
      const scenario = new SimulationScenario("scenario", ["policy1", "policy2"], 1, 5, true);
      const stanza = new SimulationStanza([scenario], true);
      const code = stanza.toCode(0);
      assert.notEqual(code.indexOf("start simulations"), -1);
      assert.notEqual(code.indexOf('simulate "scenario"'), -1);
      assert.notEqual(code.indexOf('using "policy1"'), -1);
      assert.notEqual(code.indexOf('then "policy2"'), -1);
      assert.notEqual(code.indexOf("from years 1 to 5"), -1);
    });

    QUnit.test("converts simulation stanzas to code reverse backwards ranges", function (assert) {
      const scenario = new SimulationScenario("scenario", ["policy1", "policy2"], 5, 1, true);
      const stanza = new SimulationStanza([scenario], true);
      const code = stanza.toCode(0);
      assert.notEqual(code.indexOf("start simulations"), -1);
      assert.notEqual(code.indexOf('simulate "scenario"'), -1);
      assert.notEqual(code.indexOf('using "policy1"'), -1);
      assert.notEqual(code.indexOf('then "policy2"'), -1);
      assert.notEqual(code.indexOf("from years 1 to 5"), -1);
    });

    QUnit.test("converts default to code", function (assert) {
      const application = buildTestApplication(false);
      const stanza = new DefinitionalStanza("default", [application], true);
      const code = stanza.toCode(0);
      assert.notEqual(code.indexOf("start default"), -1);
      assert.notEqual(code.indexOf('define application "app"'), -1);
      assert.notEqual(code.indexOf("end default"), -1);
    });

    QUnit.test("converts policy to code", function (assert) {
      const application = buildTestApplication(false);
      const stanza = new DefinitionalStanza("inervention", [application], true);
      const code = stanza.toCode(0);
      assert.notEqual(code.indexOf('start policy "inervention"'), -1);
      assert.notEqual(code.indexOf('define application "app"'), -1);
      assert.notEqual(code.indexOf("end policy"), -1);
    });

    QUnit.test("converts about stanza to code", function (assert) {
      const stanza = new AboutStanza();
      const code = stanza.toCode(0);
      assert.notEqual(code.indexOf("start about"), -1);
      assert.notEqual(code.indexOf("end about"), -1);
    });

    QUnit.test("converts program to code", function (assert) {
      const application = buildTestApplication();
      const applicationMod = buildTestApplication(true);
      const policy = new DefinitionalStanza("intervention", [applicationMod], true);
      const scenario = new SimulationScenario("scenario", ["intervention"], 1, 5, true);
      const program = new Program([application], [policy], [scenario], true);
      const code = program.toCode(0);
      assert.notEqual(code.indexOf("start default"), -1);
      assert.notEqual(code.indexOf('start policy "intervention"'), -1);
      assert.notEqual(code.indexOf('define application "app"'), -1);
      assert.notEqual(code.indexOf('uses substance "sub"'), -1);
      assert.notEqual(code.indexOf('modify application "app"'), -1);
      assert.notEqual(code.indexOf('modify substance "sub"'), -1);
      assert.notEqual(code.indexOf('simulate "scenario"'), -1);
    });

    QUnit.test("allows multiple set statements", function (assert) {
      const commands = [
        new Command("setVal", "domestic", new EngineNumber("1", "mt"), null),
        new Command("setVal", "import", new EngineNumber("2", "mt"), null),
        new Command("setVal", "sales", new EngineNumber("3", "mt"), null),
      ];
      const substance = createWithCommands("test", false, commands);
      assert.ok(substance.getIsCompatible());

      if (substance.getIsCompatible()) {
        const code = substance.toCode(0);
        assert.notEqual(code.indexOf("set domestic to 1 mt"), -1);
        assert.notEqual(code.indexOf("set import to 2 mt"), -1);
        assert.notEqual(code.indexOf("set sales to 3 mt"), -1);
      }
    });

    QUnit.test("allows multiple change statements", function (assert) {
      const commands = [
        new Command("change", "domestic", new EngineNumber("+1", "mt / yr"), null),
        new Command("change", "import", new EngineNumber("+2", "mt / yr"), null),
        new Command("change", "sales", new EngineNumber("+3", "mt / yr"), null),
      ];
      const substance = createWithCommands("test", false, commands);
      assert.ok(substance.getIsCompatible());

      if (substance.getIsCompatible()) {
        const code = substance.toCode(0);
        assert.notEqual(code.indexOf("change domestic by +1 mt / yr"), -1);
        assert.notEqual(code.indexOf("change import by +2 mt / yr"), -1);
        assert.notEqual(code.indexOf("change sales by +3 mt / yr"), -1);
      }
    });

    QUnit.test("allows multiple initial charge statements", function (assert) {
      const commands = [
        new Command("initial charge", "domestic", new EngineNumber(1, "kg / unit"), null),
        new Command("initial charge", "import", new EngineNumber(2, "kg / unit"), null),
        new Command("initial charge", "sales", new EngineNumber(3, "kg / unit"), null),
      ];
      const substance = createWithCommands("test", false, commands);
      assert.ok(substance.getIsCompatible());

      if (substance.getIsCompatible()) {
        const code = substance.toCode(0);
        assert.notEqual(code.indexOf("initial charge with 1 kg / unit for domestic"), -1);
        assert.notEqual(code.indexOf("initial charge with 2 kg / unit for import"), -1);
        assert.notEqual(code.indexOf("initial charge with 3 kg / unit for sales"), -1);
      }
    });

    QUnit.test("prohibits overlapping initial charge statements", function (assert) {
      const commands = [
        new Command("initial charge", "domestic", new EngineNumber(1, "kg / unit"), null),
        new Command("initial charge", "domestic", new EngineNumber(2, "kg / unit"), null),
      ];
      const substance = createWithCommands("test", false, commands);
      assert.ok(!substance.getIsCompatible());
    });

    QUnit.test("handles numbers with commas correctly", function (assert) {
      const commands = [
        new Command("setVal", "domestic", new EngineNumber("1,000", "kg"), null),
        new Command("setVal", "import", new EngineNumber("12,34.5,6", "mt"), null),
        new Command("change", "sales", new EngineNumber(",123", "kg"), null),
      ];
      const substance = createWithCommands("test", false, commands);
      assert.ok(substance.getIsCompatible());

      if (substance.getIsCompatible()) {
        const code = substance.toCode(0);
        assert.notEqual(code.indexOf("set domestic to 1,000 kg"), -1);
        assert.notEqual(code.indexOf("set import to 12,34.5,6 mt"), -1);
        assert.notEqual(code.indexOf("change sales by ,123 kg"), -1);
      }
    });

    QUnit.test("recharge commands preserve number formatting", function (assert) {
      // Test that RechargeCommand preserves original number formatting like 1.2340
      const populationEngineNumber = new EngineNumber("1.2340", "%", "1.2340");
      const volumeEngineNumber = new EngineNumber("0.8500", "kg / unit", "0.8500");
      const command = new RechargeCommand(populationEngineNumber, volumeEngineNumber, null);
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);

      // Verify original formatting is preserved in generated code
      assert.notEqual(code.indexOf("recharge 1.2340 % with 0.8500 kg / unit"), -1,
        "RechargeCommand should preserve original number formatting");

      // Test with thousands separator formatting
      const populationEngineNumber2 = new EngineNumber("1,234.0", "%", "1,234.0");
      const volumeEngineNumber2 = new EngineNumber("2.5000", "mt / unit", "2.5000");
      const command2 = new RechargeCommand(populationEngineNumber2, volumeEngineNumber2, null);
      const substance2 = createWithCommand("test2", false, command2);
      const code2 = substance2.toCode(0);

      assert.notEqual(code2.indexOf("recharge 1,234.0 % with 2.5000 mt / unit"), -1,
        "RechargeCommand should preserve thousands separator formatting");
    });

    QUnit.test("recycle commands preserve number formatting", function (assert) {
      // Test that RecycleCommand preserves original number formatting
      const targetEngineNumber = new EngineNumber("1.2340", "%", "1.2340");
      const valueEngineNumber = new EngineNumber("0.8500", "%", "0.8500");
      const inductionEngineNumber = new EngineNumber("2.5000", "%", "2.5000");
      const command = new RecycleCommand(
        targetEngineNumber,
        valueEngineNumber,
        null,
        "recharge",
        inductionEngineNumber,
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);

      // Verify original formatting is preserved in generated code
      assert.notEqual(
        code.indexOf("recover 1.2340 % with 0.8500 % reuse with 2.5000 % induction"),
        -1,
        "RecycleCommand should preserve original number formatting",
      );
    });

    QUnit.test("replace commands preserve number formatting", function (assert) {
      // Test that ReplaceCommand preserves original number formatting
      const volumeEngineNumber = new EngineNumber("1.2340", "%", "1.2340");
      const command = new ReplaceCommand(volumeEngineNumber, "domestic", "replacement", null);
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);

      // Verify original formatting is preserved in generated code
      assert.notEqual(
        code.indexOf('replace 1.2340 % of domestic with "replacement"'),
        -1,
        "ReplaceCommand should preserve original number formatting",
      );
    });
  });
}

export {buildUiTranslatorReverseTests};
