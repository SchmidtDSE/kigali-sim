ace.define("ace/mode/qubectalk", [
  "require", "exports", "module", "ace/lib/oop", "ace/mode/text", "ace/mode/text_highlight_rules",
], function (require, exports, module) {
  "use strict";

  const oop = require("../lib/oop");
  const TextMode = require("./text").Mode;
  const TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

  const QubecTalkHighlightRules = function () {
    // Keywords from QubecTalk.g4 grammar - structure keywords
    const structureKeywords = "about|application|default|define|end|policy|simulations|start|" +
      "substance|uses|variables";

    // Keywords from QubecTalk.g4 grammar - command keywords
    const commandKeywords = "across|as|by|cap|change|charge|during|floor|for|from|get|in|initial|" +
      "modify|of|recharge|recover|replace|retire|reuse|set|simulate|then|to|trials|using|with";

    // Keywords from QubecTalk.g4 grammar - conditional and logical
    const conditionalKeywords = "and|else|endif|if|or|xor";

    // Keywords from QubecTalk.g4 grammar - sampling
    const samplingKeywords = "mean|normally|sample|std|uniformly|limit";

    // Built-in streams
    const streams = "priorEquipment|equipment|export|import|manufacture|sales";

    // Units
    const units = "annually|beginning|day|days|each|kg|kwh|month|months|mt|onwards|percent|" +
      "tCO2e|unit|units|year|years";

    // Special keywords
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
