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
      "variables", "across", "all", "ago", "as", "assume", "assuming", "at", "by",
      "cap", "change", "charge", "continued", "current",
      "during", "enable", "eol", "equals", "exact", "floor", "for", "from", "get",
      "in", "induction", "initial", "new", "no", "old", "only",
      "of", "prior", "recharge", "recover", "replace", "replacement", "retire", "reuse", "set",
      "then", "to", "trials", "using", "volume", "weibull", "with", "displacing", "and", "or",
      "xor", "if", "else", "endif", "normally", "sample", "std",
      "uniformly", "limit", "annually", "beginning", "day", "days",
      "each", "month", "months", "onwards", "year", "years", "yr", "yrs", "unit",
      "units", "kg", "mt", "tCO2e", "kgCO2e", "kwh", "priorEquipment", "priorBank",
      "newEquipment", "equipment", "bank", "age",
      "export", "import", "domestic", "sales", "virgin", "mean",
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
