/**
 * Constants for UI-based authoring experience.
 *
 * @license BSD
 */

/**
 * Stream types that can be enabled/disabled based on substance configuration.
 * @constant {Array<string>}
 */
export const ENABLEABLE_STREAMS = ["domestic", "import", "export"];

/**
 * Stream types that are always available regardless of substance configuration.
 * @constant {Array<string>}
 */
export const ALWAYS_ON_STREAMS = ["sales", "equipment", "priorEquipment"];

/**
 * Valid QubecTalk year keywords that should not trigger validation warnings.
 * @constant {Array<string>}
 */
export const VALID_YEAR_KEYWORDS = ["beginning", "onwards"];

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
export const COMMAND_COMPATIBILITIES = {
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

export const SUPPORTED_EQUALS_UNITS = [
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
