/**
 * Constants used across the tool.
 *
 * @license BSD, see LICENSE.md.
 */

const GLOBAL_CONTEXT = 0;
const STANZA_CONTEXT = 1;
const APPLICATION_CONTEXT = 2;
const SUBSTANCE_CONTEXT = 3;

const STREAM_BASE_UNITS = new Map();
STREAM_BASE_UNITS.set("manufacture", "kg");
STREAM_BASE_UNITS.set("import", "kg");
STREAM_BASE_UNITS.set("sales", "kg");
STREAM_BASE_UNITS.set("consumption", "tCO2e");
STREAM_BASE_UNITS.set("rechargeEmissions", "tCO2e");
STREAM_BASE_UNITS.set("eolEmissions", "tCO2e");
STREAM_BASE_UNITS.set("equipment", "units");
STREAM_BASE_UNITS.set("priorEquipment", "units");
STREAM_BASE_UNITS.set("newEquipment", "units");

export {GLOBAL_CONTEXT, STANZA_CONTEXT, APPLICATION_CONTEXT, SUBSTANCE_CONTEXT, STREAM_BASE_UNITS};
