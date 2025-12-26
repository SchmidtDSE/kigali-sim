ace.define("ace/mode/qubectalk", [
  "require", "exports", "module", "ace/lib/oop", "ace/mode/text", "ace/mode/text_highlight_rules",
], function (require, exports, module) {
  "use strict";

  const oop = require("../lib/oop");
  const TextMode = require("./text").Mode;
  const TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

  const QubecTalkHighlightRules = function () {
    const structureKeywords = "about|application|default|define|end|policy|simulations|start|" +
      "substance|uses|variables";

    const commandKeywords = "across|as|assume|at|by|cap|change|charge|current|" +
      "continued|during|enable|eol|floor|for|from|get|in|induction|initial|" +
      "modify|no|of|only|prior|recharge|recover|replace|replacement|retire|reuse|" +
      "set|simulate|then|to|trials|using|with";

    const conditionalKeywords = "and|else|endif|if|or|xor";

    const samplingKeywords = "mean|normally|sample|std|uniformly|limit";

    const streams = "priorEquipment|equipment|priorBank|bank|export|import|domestic|sales|age";

    const units = "annually|beginning|day|days|each|kg|kwh|month|months|mt|onwards|percent|" +
      "tCO2e|kgCO2e|unit|units|year|years|yr|yrs";

    const specialKeywords = "equals|displacing";

    this.$rules = {
      "start": [
        {
          token: "comment",
          regex: "#.*$",
        },
        {
          token: "string",
          regex: '"(?:[^"\\\\]|\\\\.)*"',
        },
        {
          token: "constant.numeric",
          regex: "\\b\\d*\\.\\d+\\b",
        },
        {
          token: "constant.numeric",
          regex: "\\b\\d+\\b",
        },
        {
          token: "keyword.control.structure",
          regex: "\\b(?:" + structureKeywords + ")\\b",
          caseInsensitive: true,
        },
        {
          token: "keyword.control.conditional",
          regex: "\\b(?:" + conditionalKeywords + ")\\b",
          caseInsensitive: true,
        },
        {
          token: "keyword.other.command",
          regex: "\\b(?:" + commandKeywords + ")\\b",
          caseInsensitive: true,
        },
        {
          token: "keyword.other.sampling",
          regex: "\\b(?:" + samplingKeywords + ")\\b",
          caseInsensitive: true,
        },
        {
          token: "keyword.other.special",
          regex: "\\b(?:" + specialKeywords + ")\\b",
          caseInsensitive: true,
        },
        {
          token: "support.type.stream",
          regex: "\\b(?:" + streams + ")\\b",
          caseInsensitive: true,
        },
        {
          token: "support.type.unit",
          regex: "\\b(?:" + units + ")\\b",
          caseInsensitive: true,
        },
        {
          token: "keyword.operator.arithmetic",
          regex: "[+\\-*/^%]",
        },
        {
          token: "keyword.operator.comparison",
          regex: "==|!=|<=|>=|<|>",
        },
        {
          token: "punctuation.definition.brackets",
          regex: "[\\[\\]()]",
        },
        {
          token: "punctuation.separator",
          regex: ",",
        },
        {
          token: "text",
          regex: "\\s+",
        },
        {
          token: "identifier",
          regex: "[a-zA-Z][a-zA-Z0-9]*",
        },
      ],
    };
  };

  oop.inherits(QubecTalkHighlightRules, TextHighlightRules);

  const QubecTalkMode = function () {
    this.HighlightRules = QubecTalkHighlightRules;
    this.$behaviour = this.$defaultBehaviour;
  };
  oop.inherits(QubecTalkMode, TextMode);

  (function () {
    this.lineCommentStart = "#";
    this.$id = "ace/mode/qubectalk";
  }).call(QubecTalkMode.prototype);

  exports.Mode = QubecTalkMode;
});
