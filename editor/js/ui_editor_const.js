/**
 * Constants for UI-based authoring experience.
 *
 * @license BSD
 */

/**
 * Stream types that are always available regardless of substance configuration.
 * @constant {Array<string>}
 */
const ALWAYS_ON_STREAMS = ["sales", "equipment", "priorEquipment"];

/**
 * Command compatibility mapping to compatibility modes:
 *
 * - "any": Compatible with both policy and definition contexts
 * - "none": Not compatible with simplified UI
 * - "definition": Only compatible with substance definitions.
 * - "policy": Only compatible with policy modifications.
 *
 * @type {Object.<string, string>}
 */
const COMMAND_COMPATIBILITIES = {
  "change": "any",
  "define var": "none",
  "retire": "any",
  "setVal": "any",
  "cap": "any",
  "floor": "any",
  "limit": "any",
  "initial charge": "definition",
  "equals": "definition",
  "recharge": "any",
  "recycle": "policy",
  "replace": "policy",
  "enable": "definition",
  "assume": "definition",
};

/**
 * Stream types that can be enabled/disabled based on substance configuration.
 * @constant {Array<string>}
 */
const ENABLEABLE_STREAMS = ["domestic", "import", "export"];

const SUPPORTED_EQUALS_UNITS = [
  "tCO2e / unit",
  "tCO2e / kg",
  "tCO2e / mt",
  "kgCO2e / unit",
  "kgCO2e / kg",
  "kgCO2e / mt",
  "kwh / kg",
  "kwh / mt",
  "kwh / unit",
];

/**
 * Stream target selectors used throughout the application for updating dropdown states.
 * @constant {Array<string>}
 */
const STREAM_TARGET_SELECTORS = [
  ".set-target-input",
  ".change-target-input",
  ".limit-target-input",
  ".replace-target-input",
  ".displacing-input",
];

/**
 * Valid QubecTalk year keywords that should not trigger validation warnings.
 * @constant {Array<string>}
 */
const VALID_YEAR_KEYWORDS = ["beginning", "onwards"];

export {
  ALWAYS_ON_STREAMS,
  COMMAND_COMPATIBILITIES,
  ENABLEABLE_STREAMS,
  STREAM_TARGET_SELECTORS,
  SUPPORTED_EQUALS_UNITS,
  VALID_YEAR_KEYWORDS,
};
