/**
 * Utility functions and constants for UI translation.
 *
 * @license BSD, see LICENSE.md
 */

import {EngineNumber} from "engine_number";
import {NumberParseUtil} from "number_parse_util";

/**
 * Formats an EngineNumber for code generation, preserving original string when available.
 *
 * @param {EngineNumber} engineNumber - The EngineNumber to format.
 * @returns {string} Formatted string with value and units.
 */
function formatEngineNumber(engineNumber) {
  if (engineNumber.hasOriginalString()) {
    return engineNumber.getOriginalString() + " " + engineNumber.getUnits();
  } else {
    return engineNumber.getValue() + " " + engineNumber.getUnits();
  }
}


/**
 * Indent a single piece of text by the specified number of spaces.
 *
 * @param {string} piece - The text to indent
 * @param {number} spaces - Number of spaces to indent. Defaults to 0.
 * @returns {string} The indented text
 */
function indentSingle(piece, spaces) {
  if (spaces === undefined) {
    spaces = 0;
  }

  let prefix = "";
  for (let i = 0; i < spaces; i++) {
    prefix += " ";
  }

  return prefix + piece;
}

/**
 * Indent an array of text pieces by the specified number of spaces.
 *
 * @param {string[]} pieces - Array of text pieces to indent
 * @param {number} spaces - Number of spaces to indent each piece
 * @returns {string[]} Array of indented text pieces
 */
function indent(pieces, spaces) {
  return pieces.map((piece) => indentSingle(piece, spaces));
}

/**
 * Create a function that adds indented code pieces to a target array.
 *
 * @param {string[]} target - Target array to add code pieces to
 * @returns {Function} Function that takes a code piece and spaces count
 */
function buildAddCode(target) {
  return (x, spaces) => {
    target.push(indentSingle(x, spaces));
  };
}

/**
 * Join code pieces into a single string with newlines.
 *
 * @param {string[]} target - Array of code pieces to join
 * @returns {string} Combined code string
 */
function finalizeCodePieces(target) {
  return target.join("\n");
}

/**
 * Preprocesses "each year" syntax in input code.
 *
 * Removes unsupported "each years?" syntax from the end of statements.
 * Note: Complex year range syntax like "2025 to 2035" is handled differently
 * and not preprocessed here - see implementation comments for details.
 *
 * @param {string} input - The raw QubecTalk code
 * @returns {string} The preprocessed QubecTalk code
 */
function preprocessEachYearSyntax(input) {
  return input.replace(/\s+each\s+years?\s*$/gm, "");
}

export {
  formatEngineNumber,
  indentSingle,
  indent,
  buildAddCode,
  finalizeCodePieces,
  preprocessEachYearSyntax,
};
