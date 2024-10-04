import {EngineNumber} from "engine_number";
import {YearMatcher} from "engine_state";
import {
  AboutStanza,
  Application,
  Command,
  DefinitionalStanza,
  LimitCommand,
  Program,
  ReplaceCommand,
  SimulationScenario,
  SimulationStanza,
  SubstanceBuilder,
  buildAddCode,
  finalizeCodePieces,
  indent,
} from "ui_translator";


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
  const command = new Command(
    "setVal",
    "manufacture",
    new EngineNumber(5, "kg / unit"),
    null,
  );
  const substance = createWithCommand("sub", isMod, command);
  const application = new Application("app", [substance], isMod, true);
  return application;
}


function buildUiTranslatorReverseTests() {
  QUnit.module("UiTranslatorCompilerReverse", function() {
    QUnit.test("indents", function(assert) {
      const result = indent(["a", "b"], 2);
      assert.equal(result.length, 2);
      assert.deepEqual(result[0], "  a");
      assert.deepEqual(result[1], "  b");
    });

    QUnit.test("builds add code", function(assert) {
      const pieces = [];
      const addCode = buildAddCode(pieces);
      addCode("a", 2);
      assert.equal(pieces.length, 1);
      assert.deepEqual(pieces[0], "  a");
    });

    QUnit.test("finalizes code pieces", function(assert) {
      const pieces = ["a", "b", "c"];
      const result = finalizeCodePieces(pieces);
      assert.deepEqual(result, "a\nb\nc");
    });

    QUnit.test("initial charges substances", function(assert) {
      const command = new Command(
        "initial charge",
        "manufacture",
        new EngineNumber(5, "kg / unit"),
        null,
      );
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("define substance \"test\""), -1);
      assert.notEqual(code.indexOf("initial charge with 5 kg / unit for manufacture"), -1);
    });

    QUnit.test("caps substances", function(assert) {
      const command = new LimitCommand(
        "cap",
        "manufacture",
        new EngineNumber(5, "mt"),
        null,
        null,
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("cap manufacture to 5 mt"), -1);
    });

    QUnit.test("changes substances", function(assert) {
      const command = new Command(
        "change",
        "manufacture",
        new EngineNumber("+5", "% / year"),
        null,
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("change manufacture by +5 % / year"), -1);
    });

    QUnit.test("emits from substances", function(assert) {
      const command = new Command(
        "emit",
        null,
        new EngineNumber("5", "tCO2e / unit"),
        null,
      );
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("emit 5 tCO2e / unit"), -1);
    });

    QUnit.test("recharges substances", function(assert) {
      const command = new Command(
        "recharge",
        new EngineNumber("10", "% / year"),
        new EngineNumber("5", "kg / unit"),
        null,
      );
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("recharge 10 % / year with 5 kg / unit"), -1);
    });

    QUnit.test("recycles substances", function(assert) {
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

    QUnit.test("replaces substances", function(assert) {
      const command = new ReplaceCommand(
        new EngineNumber("10", "%"),
        "manufacture",
        "other",
        null,
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("replace 10 % of manufacture with \"other\""), -1);
    });

    QUnit.test("retires substances", function(assert) {
      const command = new Command(
        "retire",
        null,
        new EngineNumber("10", "%"),
        null,
      );
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("retire 10 %"), -1);
    });

    QUnit.test("sets values in substances", function(assert) {
      const command = new Command(
        "setVal",
        "manufacture",
        new EngineNumber("10", "mt"),
        null,
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("set manufacture to 10 mt"), -1);
    });

    QUnit.test("supports duration single year", function(assert) {
      const command = new Command(
        "setVal",
        "manufacture",
        new EngineNumber("10", "mt"),
        new YearMatcher(1, 1),
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("set manufacture to 10 mt during year 1"), -1);
    });

    QUnit.test("supports duration muiltiple years", function(assert) {
      const command = new Command(
        "retire",
        null,
        new EngineNumber("10", "%"),
        new YearMatcher(2, 5),
      );
      const substance = createWithCommand("test", false, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("retire 10 % during years 2 to 5"), -1);
    });

    QUnit.test("supports duration with min year", function(assert) {
      const command = new ReplaceCommand(
        new EngineNumber("10", "%"),
        "manufacture",
        "other",
        new YearMatcher(2, null),
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf(
        "replace 10 % of manufacture with \"other\" during years 2 to onwards",
      ), -1);
    });

    QUnit.test("supports duration with max year", function(assert) {
      const command = new Command(
        "recycle",
        new EngineNumber("10", "%"),
        new EngineNumber("100", "%"),
        new YearMatcher(null, 5),
      );
      const substance = createWithCommand("test", true, command);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf(
        "recover 10 % with 100 % reuse during years beginning to 5",
      ), -1);
    });

    QUnit.test("supports complex substances", function(assert) {
      const setVal = new Command(
        "setVal",
        "manufacture",
        new EngineNumber(5, "kg / unit"),
        new YearMatcher(1, 1),
      );
      const cap = new LimitCommand(
        "cap",
        "manufacture",
        new EngineNumber(5, "mt"),
        new YearMatcher(3, 4),
        "import",
      );
      const substance = createWithCommands("test", true, [setVal, cap]);
      const code = substance.toCode(0);
      assert.notEqual(code.indexOf("modify substance \"test\""), -1);
      assert.notEqual(code.indexOf("set manufacture to 5 kg / unit during year 1"), -1);
      assert.notEqual(code.indexOf("cap manufacture to 5 mt during years 3 to 4"), -1);
    });

    QUnit.test("converts applications to code", function(assert) {
      const application = buildTestApplication(false);
      const code = application.toCode(0);
      assert.notEqual(code.indexOf("define application \"app\""), -1);
      assert.notEqual(code.indexOf("define substance \"sub\""), -1);
    });

    QUnit.test("converts simulation stanzas to code", function(assert) {
      const scenario = new SimulationScenario("scenario", ["policy1", "policy2"], 1, 5, true);
      const stanza = new SimulationStanza([scenario], true);
      const code = stanza.toCode(0);
      assert.notEqual(code.indexOf("start simulations"), -1);
      assert.notEqual(code.indexOf("simulate \"scenario\""), -1);
      assert.notEqual(code.indexOf("using \"policy1\""), -1);
      assert.notEqual(code.indexOf("then \"policy2\""), -1);
      assert.notEqual(code.indexOf("from years 1 to 5"), -1);
    });

    QUnit.test("converts default to code", function(assert) {
      const application = buildTestApplication(false);
      const stanza = new DefinitionalStanza("default", [application], true);
      const code = stanza.toCode(0);
      assert.notEqual(code.indexOf("start default"), -1);
      assert.notEqual(code.indexOf("define application \"app\""), -1);
      assert.notEqual(code.indexOf("end default"), -1);
    });

    QUnit.test("converts policy to code", function(assert) {
      const application = buildTestApplication(false);
      const stanza = new DefinitionalStanza("inervention", [application], true);
      const code = stanza.toCode(0);
      assert.notEqual(code.indexOf("start policy \"inervention\""), -1);
      assert.notEqual(code.indexOf("define application \"app\""), -1);
      assert.notEqual(code.indexOf("end policy"), -1);
    });

    QUnit.test("converts about stanza to code", function(assert) {
      const stanza = new AboutStanza();
      const code = stanza.toCode(0);
      assert.notEqual(code.indexOf("start about"), -1);
      assert.notEqual(code.indexOf("end about"), -1);
    });

    QUnit.test("converts program to code", function(assert) {
      const application = buildTestApplication();
      const applicationMod = buildTestApplication(true);
      const policy = new DefinitionalStanza("intervention", [applicationMod], true);
      const scenario = new SimulationScenario("scenario", ["intervention"], 1, 5, true);
      const program = new Program([application], [policy], [scenario], true);
      const code = program.toCode(0);
      assert.notEqual(code.indexOf("start default"), -1);
      assert.notEqual(code.indexOf("start policy \"intervention\""), -1);
      assert.notEqual(code.indexOf("define application \"app\""), -1);
      assert.notEqual(code.indexOf("define substance \"sub\""), -1);
      assert.notEqual(code.indexOf("modify application \"app\""), -1);
      assert.notEqual(code.indexOf("modify substance \"sub\""), -1);
      assert.notEqual(code.indexOf("simulate \"scenario\""), -1);
    });

    QUnit.test("allows multiple set statements", function(assert) {
      const commands = [
        new Command(
          "setVal",
          "manufacture",
          new EngineNumber("1", "mt"),
          null,
        ),
        new Command(
          "setVal",
          "import",
          new EngineNumber("2", "mt"),
          null,
        ),
        new Command(
          "setVal",
          "sales",
          new EngineNumber("3", "mt"),
          null,
        ),
      ];
      const substance = createWithCommands("test", false, commands);
      assert.ok(substance.getIsCompatible());

      if (substance.getIsCompatible()) {
        const code = substance.toCode(0);
        assert.notEqual(code.indexOf("set manufacture to 1 mt"), -1);
        assert.notEqual(code.indexOf("set import to 2 mt"), -1);
        assert.notEqual(code.indexOf("set sales to 3 mt"), -1);
      }
    });

    QUnit.test("allows multiple change statements", function(assert) {
      const commands = [
        new Command(
          "change",
          "manufacture",
          new EngineNumber("+1", "mt / year"),
          null,
        ),
        new Command(
          "change",
          "import",
          new EngineNumber("+2", "mt / year"),
          null,
        ),
        new Command(
          "change",
          "sales",
          new EngineNumber("+3", "mt / year"),
          null,
        ),
      ];
      const substance = createWithCommands("test", false, commands);
      assert.ok(substance.getIsCompatible());

      if (substance.getIsCompatible()) {
        const code = substance.toCode(0);
        assert.notEqual(code.indexOf("change manufacture by +1 mt / year"), -1);
        assert.notEqual(code.indexOf("change import by +2 mt / year"), -1);
        assert.notEqual(code.indexOf("change sales by +3 mt / year"), -1);
      }
    });

    QUnit.test("allows multiple initial charge statements", function(assert) {
      const commands = [
        new Command(
          "initial charge",
          "manufacture",
          new EngineNumber(1, "kg / unit"),
          null,
        ),
        new Command(
          "initial charge",
          "import",
          new EngineNumber(2, "kg / unit"),
          null,
        ),
        new Command(
          "initial charge",
          "sales",
          new EngineNumber(3, "kg / unit"),
          null,
        ),
      ];
      const substance = createWithCommands("test", false, commands);
      assert.ok(substance.getIsCompatible());

      if (substance.getIsCompatible()) {
        const code = substance.toCode(0);
        assert.notEqual(code.indexOf("initial charge with 1 kg / unit for manufacture"), -1);
        assert.notEqual(code.indexOf("initial charge with 2 kg / unit for import"), -1);
        assert.notEqual(code.indexOf("initial charge with 3 kg / unit for sales"), -1);
      }
    });

    QUnit.test("prohibits overlapping initial charge statements", function(assert) {
      const commands = [
        new Command(
          "initial charge",
          "manufacture",
          new EngineNumber(1, "kg / unit"),
          null,
        ),
        new Command(
          "initial charge",
          "manufacture",
          new EngineNumber(2, "kg / unit"),
          null,
        ),
      ];
      const substance = createWithCommands("test", false, commands);
      assert.ok(!substance.getIsCompatible());
    });
  });
}


export {buildUiTranslatorReverseTests};
