/**
 * QubecTalk language definition for Prism.js syntax highlighting
 * Defines syntax highlighting rules for the QubecTalk domain-specific language
 */
Prism.languages.qubectalk = {
  "comment": {
    pattern: /#.*/,
    greedy: true,
  },
  "string": {
    pattern: /"[^"]*"/,
    greedy: true,
  },
  "number": {
    pattern: /\b\d+(?:\.\d+)?\b/,
    greedy: true,
  },
  "keyword": {
    pattern: new RegExp("\\b(?:" + [
      "start", "end", "default", "policy", "simulations", "application",
      "substance", "define", "uses", "modify", "simulate", "about",
      "variables", "across", "as", "by", "cap", "change", "charge",
      "during", "enable", "equals", "floor", "for", "from", "get", "in", "initial",
      "of", "recharge", "recover", "replace", "retire", "reuse", "set",
      "then", "to", "trials", "using", "with", "displacing", "and", "or",
      "xor", "if", "else", "endif", "normally", "sample", "std",
      "uniformly", "limit", "annually", "beginning", "day", "days",
      "each", "month", "months", "onwards", "year", "years", "unit",
      "units", "kg", "mt", "tCO2e", "kwh", "priorEquipment", "equipment",
      "export", "import", "manufacture", "sales", "mean",
    ].join("|") + ")\\b"),
    greedy: true,
  },
  "operator": {
    pattern: /[=<>!]=?|[+\-*\/^%]/,
    greedy: true,
  },
  "punctuation": {
    pattern: /[{}[\];(),]/,
    greedy: true,
  },
};
