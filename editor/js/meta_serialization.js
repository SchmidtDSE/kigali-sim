/**
 * Serialization logic for substance metadata CSV import/export.
 *
 * Provides classes and utilities for converting between SubstanceMetadata
 * objects and CSV format for bulk import/export operations.
 *
 * @license BSD, see LICENSE.md
 */

import {
  SubstanceMetadata,
  SubstanceMetadataBuilder,
  Program,
  Application,
  Substance,
  SubstanceBuilder,
  Command,
} from "ui_translator";
import {EngineNumber} from "engine_number";

/**
 * Standard CSV column order for substance metadata export/import.
 * Used consistently across serialization and deserialization operations.
 */
const META_COLUMNS = [
  "substance",
  "equipment",
  "application",
  "ghg",
  "hasDomestic",
  "hasImport",
  "hasExport",
  "energy",
  "initialChargeDomestic",
  "initialChargeImport",
  "initialChargeExport",
  "retirement",
  "key",
];

/**
 * Boolean value mapping for CSV parsing.
 * Maps various string representations to boolean values.
 * Throws exception if value not found in either true or false sets.
 */
const BOOLEAN_VALUES = new Map([
  ["t", true],
  ["1", true],
  ["y", true],
  ["true", true],
  ["yes", true],
  ["f", false],
  ["0", false],
  ["n", false],
  ["false", false],
  ["no", false],
]);

/**
 * Error information for substance metadata parsing issues.
 *
 * Provides structured error information that distinguishes between user data errors
 * (invalid CSV content) and system programming errors. Includes row and column
 * information to help users locate and fix issues in their data.
 */
class SubstanceMetadataError {
  /**
   * Create a new SubstanceMetadataError.
   *
   * @param {number} rowNumber - Row number where error occurred
   *   (0 for header, 1-based for data rows)
   * @param {string} column - Column name where error occurred
   * @param {string} message - Human-readable error description
   * @param {string} errorType - 'USER' for data errors or 'SYSTEM' for programming errors
   */
  constructor(rowNumber, column, message, errorType = "USER") {
    const self = this;
    self._rowNumber = rowNumber;
    self._column = column;
    self._message = message;
    self._errorType = errorType;
  }

  /**
   * Get the row number where the error occurred.
   *
   * @returns {number} Row number (0 for header, 1-based for data rows)
   */
  getRowNumber() {
    const self = this;
    return self._rowNumber;
  }

  /**
   * Get the column name where the error occurred.
   *
   * @returns {string} Column name
   */
  getColumn() {
    const self = this;
    return self._column;
  }

  /**
   * Get the human-readable error message.
   *
   * @returns {string} Error message
   */
  getMessage() {
    const self = this;
    return self._message;
  }

  /**
   * Get the error type.
   *
   * @returns {string} 'USER' or 'SYSTEM'
   */
  getErrorType() {
    const self = this;
    return self._errorType;
  }

  /**
   * Check if this is a user data error.
   *
   * @returns {boolean} True if this is a user error
   */
  isUserError() {
    const self = this;
    return self._errorType === "USER";
  }

  /**
   * Check if this is a system programming error.
   *
   * @returns {boolean} True if this is a system error
   */
  isSystemError() {
    const self = this;
    return self._errorType === "SYSTEM";
  }

  /**
   * Convert error to human-readable string.
   *
   * @returns {string} Formatted error description
   */
  toString() {
    const self = this;
    if (self._rowNumber === 0) {
      return `Header error in column '${self._column}': ${self._message}`;
    }
    return `Row ${self._rowNumber}, column '${self._column}': ${self._message}`;
  }
}

/**
 * Result container for substance metadata parsing operations.
 *
 * Contains successfully parsed metadata updates and any errors that occurred
 * during parsing. Provides methods to check for different types of errors
 * and access parsed results.
 */
class SubstanceMetadataParseResult {
  /**
   * Create a new SubstanceMetadataParseResult.
   *
   * @param {SubstanceMetadataUpdate[]} updates - Array of successfully parsed updates
   * @param {SubstanceMetadataError[]} errors - Array of parsing errors
   */
  constructor(updates = [], errors = []) {
    const self = this;
    self._updates = updates;
    self._errors = errors;
  }

  /**
   * Get the successfully parsed metadata updates.
   *
   * @returns {SubstanceMetadataUpdate[]} Array of parsed updates
   */
  getUpdates() {
    const self = this;
    return self._updates;
  }

  /**
   * Get all parsing errors.
   *
   * @returns {SubstanceMetadataError[]} Array of errors
   */
  getErrors() {
    const self = this;
    return self._errors;
  }

  /**
   * Check if any errors occurred during parsing.
   *
   * @returns {boolean} True if errors exist
   */
  hasErrors() {
    const self = this;
    return self._errors.length > 0;
  }

  /**
   * Check if any user data errors occurred.
   *
   * @returns {boolean} True if user errors exist
   */
  hasUserErrors() {
    const self = this;
    return self._errors.some((e) => e.isUserError());
  }

  /**
   * Check if any system errors occurred.
   *
   * @returns {boolean} True if system errors exist
   */
  hasSystemErrors() {
    const self = this;
    return self._errors.some((e) => e.isSystemError());
  }

  /**
   * Check if parsing was completely successful.
   *
   * @returns {boolean} True if no errors occurred
   */
  isSuccess() {
    const self = this;
    return self._errors.length === 0;
  }

  /**
   * Get only user data errors.
   *
   * @returns {SubstanceMetadataError[]} Array of user errors
   */
  getUserErrors() {
    const self = this;
    return self._errors.filter((e) => e.isUserError());
  }

  /**
   * Get only system programming errors.
   *
   * @returns {SubstanceMetadataError[]} Array of system errors
   */
  getSystemErrors() {
    const self = this;
    return self._errors.filter((e) => e.isSystemError());
  }

  /**
   * Add a successfully parsed metadata update.
   *
   * @param {SubstanceMetadataUpdate} update - Update to add
   */
  addUpdate(update) {
    const self = this;
    self._updates.push(update);
  }

  /**
   * Add a parsing error.
   *
   * @param {SubstanceMetadataError} error - Error to add
   */
  addError(error) {
    const self = this;
    self._errors.push(error);
  }

  /**
   * Get a summary of all errors as a formatted string.
   *
   * @returns {string} Multi-line error summary
   */
  getErrorSummary() {
    const self = this;
    if (self._errors.length === 0) return "No errors";
    return self._errors.map((e) => e.toString()).join("\n");
  }
}

/**
 * Serializer for converting SubstanceMetadata objects to CSV format.
 *
 * This class handles the conversion of SubstanceMetadata arrays to CSV-compatible
 * Maps and generates data URIs for browser downloads. The CSV format follows the
 * specification defined in the import_export_meta task.
 */
class MetaSerializer {
  /**
   * Helper function to get value or return empty string.
   *
   * @param {*} value - The value to check
   * @returns {string} The value or empty string if falsy
   * @private
   */
  _getOrEmpty(value) {
    const self = this;
    return value || "";
  }

  /**
   * Convert array of SubstanceMetadata to array of Maps for CSV export.
   *
   * Each Map represents a CSV row with column names as keys and string values.
   * The column structure matches the specification: substance, equipment, application,
   * ghg, hasDomestic, hasImport, hasExport, energy, initialChargeDomestic,
   * initialChargeImport, initialChargeExport, retirement, key.
   *
   * @param {SubstanceMetadata[]} metadataArray - Array of metadata objects
   * @returns {Map<string, string>[]} Array of Maps with CSV column mappings
   */
  serialize(metadataArray) {
    const self = this;

    // Convert each metadata object to a Map
    return metadataArray.map((metadata) => {
      const rowMap = new Map();

      // Set columns in the exact order specified in task
      rowMap.set("substance", self._getOrEmpty(metadata.getSubstance()));
      rowMap.set("equipment", self._getOrEmpty(metadata.getEquipment()));
      rowMap.set("application", self._getOrEmpty(metadata.getApplication()));
      rowMap.set("ghg", self._getOrEmpty(metadata.getGhg()));
      rowMap.set("hasDomestic", metadata.getHasDomestic().toString());
      rowMap.set("hasImport", metadata.getHasImport().toString());
      rowMap.set("hasExport", metadata.getHasExport().toString());
      rowMap.set("energy", self._getOrEmpty(metadata.getEnergy()));
      rowMap.set("initialChargeDomestic", self._getOrEmpty(metadata.getInitialChargeDomestic()));
      rowMap.set("initialChargeImport", self._getOrEmpty(metadata.getInitialChargeImport()));
      rowMap.set("initialChargeExport", self._getOrEmpty(metadata.getInitialChargeExport()));
      rowMap.set("retirement", self._getOrEmpty(metadata.getRetirement()));
      rowMap.set("key", self._getOrEmpty(metadata.getKey()));

      return rowMap;
    });
  }

  /**
   * Create data URI with CSV content for download.
   *
   * This method serializes the metadata array to CSV format and wraps it
   * in a data URI that can be used for browser downloads. The CSV format
   * includes proper header row and RFC 4180 compliant escaping.
   *
   * @param {SubstanceMetadata[]} metadataArray - Array of metadata objects
   * @returns {string} Data URI string (data:text/csv;charset=utf-8,...)
   */
  renderMetaToCsvUri(metadataArray) {
    const self = this;

    const serializedMaps = self.serialize(metadataArray);
    const csvContent = self._generateCsvString(serializedMaps);
    return "data:text/csv;charset=utf-8," + encodeURIComponent(csvContent);
  }

  /**
   * Generate CSV string from array of Maps.
   *
   * Creates a complete CSV string with header row and data rows,
   * properly escaping values that contain special CSV characters.
   *
   * @param {Map<string, string>[]} serializedMaps - Array of row Maps
   * @returns {string} Complete CSV content string
   * @private
   */
  _generateCsvString(serializedMaps) {
    const self = this;

    // Generate all rows using flatMap and unshift for header
    const csvRows = [
      META_COLUMNS.join(","),
      ...serializedMaps.flatMap((rowMap) => {
        const rowValues = META_COLUMNS.map((column) => {
          const value = rowMap.get(column) || "";
          return self._escapeCsvValue(value);
        });
        return rowValues.join(",");
      }),
    ];

    return csvRows.join("\n");
  }

  /**
   * Escape a CSV value according to RFC 4180 specification.
   *
   * Based on ANTLR grammar analysis, strings are defined as '"' ~["',]* '"' which means
   * they cannot contain quotes or commas, so most escaping is unnecessary for our use case.
   * However, we maintain basic comma escaping for compatibility.
   *
   * @param {string} value - The value to escape
   * @returns {string} Escaped CSV value
   * @private
   */
  _escapeCsvValue(value) {
    const self = this;

    if (!value) {
      return "";
    }

    const stringValue = String(value);

    // Based on grammar limitations, only escape commas as they're the main CSV delimiter issue
    if (stringValue.includes(",")) {
      return "\"" + stringValue + "\"";
    }

    return stringValue;
  }

  /**
   * Convert array of Maps to structured result with SubstanceMetadata for CSV import.
   *
   * Each Map represents a CSV row with column names as keys and string values.
   * Boolean fields are converted from string representation to boolean values.
   * Empty or missing values are handled gracefully with defaults. Parsing errors
   * are collected and returned in the result rather than throwing exceptions.
   *
   * @param {Map<string, string>[]} arrayOfMaps - Array of Maps with CSV data
   * @returns {SubstanceMetadataParseResult} Result containing parsed metadata and errors
   */
  deserialize(arrayOfMaps) {
    const self = this;
    const result = new SubstanceMetadataParseResult();

    if (!Array.isArray(arrayOfMaps)) {
      result.addError(new SubstanceMetadataError(0, "input", "Input must be an array", "SYSTEM"));
      return result;
    }

    for (let i = 0; i < arrayOfMaps.length; i++) {
      const rowMap = arrayOfMaps[i];
      const rowNumber = i + 1; // 1-based for user display

      try {
        if (!(rowMap instanceof Map)) {
          result.addError(new SubstanceMetadataError(rowNumber, "input",
            "Row data must be a Map object", "SYSTEM"));
          continue;
        }

        const metadata = self._parseRowToMetadata(rowMap, rowNumber, result);
        if (metadata) {
          result.addUpdate(new SubstanceMetadataUpdate("", metadata));
        }
      } catch (systemError) {
        // Unexpected system errors that shouldn't happen in normal operation
        result.addError(new SubstanceMetadataError(rowNumber, "system",
          `Unexpected error: ${systemError.message}`, "SYSTEM"));
      }
    }

    return result;
  }

  /**
   * Convert CSV string to structured result with SubstanceMetadataUpdate for import.
   *
   * Uses Papa Parse to parse CSV string with headers into objects,
   * then converts to SubstanceMetadataUpdate instances. The CSV key column
   * is used as the oldName to identify existing substances for updates.
   * Parsing errors are collected and returned in the result rather than throwing exceptions.
   *
   * @param {string} csvString - CSV content string with headers
   * @returns {SubstanceMetadataParseResult} Result containing parsed updates and errors
   */
  deserializeMetaFromCsvString(csvString) {
    const self = this;
    const result = new SubstanceMetadataParseResult();

    if (typeof csvString !== "string") {
      result.addError(new SubstanceMetadataError(0, "input", "CSV input must be a string", "USER"));
      return result;
    }

    if (!csvString.trim()) {
      return result; // Empty string is valid, just return empty result
    }

    // Parse CSV using Papa Parse
    const parseResult = Papa.parse(csvString, {
      header: true,
      dynamicTyping: false,
      skipEmptyLines: true,
    });

    // Check for Papa Parse errors (user data issues)
    if (parseResult.errors && parseResult.errors.length > 0) {
      for (const error of parseResult.errors) {
        const rowNum = error.row ? error.row + 1 : 0; // Papa Parse uses 0-based rows
        result.addError(new SubstanceMetadataError(rowNum, "parsing", error.message, "USER"));
      }
    }

    // Validate required columns exist
    if (parseResult.data.length > 0) {
      const firstRow = parseResult.data[0];
      const missingColumns = META_COLUMNS.filter((col) => !(col in firstRow));
      if (missingColumns.length > 0) {
        result.addError(new SubstanceMetadataError(0, "columns",
          `Missing required columns: ${missingColumns.join(", ")}`, "USER"));
        return result; // Can't continue without required columns
      }
    }

    // Process each data row
    for (let i = 0; i < parseResult.data.length; i++) {
      const rowData = parseResult.data[i];
      // +2 because Papa Parse excludes header, and we want 1-based counting
      const rowNumber = i + 2;

      try {
        const oldName = self._getOrEmpty(rowData["key"]);
        const rowMap = self._createRowMapFromCsvData(rowData, rowNumber, result);

        if (rowMap) {
          const metadataResult = self._parseRowToMetadata(rowMap, rowNumber, result);
          if (metadataResult) {
            result.addUpdate(new SubstanceMetadataUpdate(oldName, metadataResult));
          }
        }
      } catch (systemError) {
        result.addError(new SubstanceMetadataError(rowNumber, "system",
          `Unexpected error processing row: ${systemError.message}`, "SYSTEM"));
      }
    }

    return result;
  }

  /**
   * Parse a string value to boolean using BOOLEAN_VALUES map.
   *
   * Handles various string representations and throws exception for invalid values.
   *
   * @param {string} value - String value to parse as boolean
   * @returns {boolean} Parsed boolean value
   * @private
   */
  _parseBoolean(value) {
    const self = this;

    if (!value || typeof value !== "string") {
      return false;
    }

    const trimmed = value.trim().toLowerCase();

    if (BOOLEAN_VALUES.has(trimmed)) {
      return BOOLEAN_VALUES.get(trimmed);
    }

    const validValues = Array.from(BOOLEAN_VALUES.keys()).join(", ");
    throw new Error(`Invalid boolean value: ${value}. Expected one of: ${validValues}`);
  }

  /**
   * Parse a single row Map to SubstanceMetadata object with error handling.
   *
   * Validates and parses boolean fields, collects any parsing errors, and builds
   * a SubstanceMetadata object using default values for invalid fields.
   *
   * @param {Map<string, string>} rowMap - Map containing row data
   * @param {number} rowNumber - Row number for error reporting
   * @param {SubstanceMetadataParseResult} result - Result object to collect errors
   * @returns {SubstanceMetadata|null} Parsed metadata or null if critical errors
   * @private
   */
  _parseRowToMetadata(rowMap, rowNumber, result) {
    const self = this;
    let hasErrors = false;

    // Validate and parse boolean fields
    const booleanFields = ["hasDomestic", "hasImport", "hasExport"];
    const parsedBooleans = {};

    for (const field of booleanFields) {
      try {
        parsedBooleans[field] = self._parseBoolean(rowMap.get(field));
      } catch (error) {
        result.addError(new SubstanceMetadataError(rowNumber, field, error.message, "USER"));
        parsedBooleans[field] = false; // Default value
        hasErrors = true;
      }
    }

    // Build metadata object with error handling
    const builder = new SubstanceMetadataBuilder();
    builder.setSubstance(self._getOrEmpty(rowMap.get("substance")))
      .setEquipment(self._getOrEmpty(rowMap.get("equipment")))
      .setApplication(self._getOrEmpty(rowMap.get("application")))
      .setGhg(self._getOrEmpty(rowMap.get("ghg")))
      .setHasDomestic(parsedBooleans.hasDomestic)
      .setHasImport(parsedBooleans.hasImport)
      .setHasExport(parsedBooleans.hasExport)
      .setEnergy(self._getOrEmpty(rowMap.get("energy")))
      .setInitialChargeDomestic(self._getOrEmpty(rowMap.get("initialChargeDomestic")))
      .setInitialChargeImport(self._getOrEmpty(rowMap.get("initialChargeImport")))
      .setInitialChargeExport(self._getOrEmpty(rowMap.get("initialChargeExport")))
      .setRetirement(self._getOrEmpty(rowMap.get("retirement")));

    return builder.build();
  }

  /**
   * Create a row Map from CSV data object for processing.
   *
   * Filters out the key column and creates a Map with metadata columns only.
   * Reports any missing column data as user errors.
   *
   * @param {Object} rowData - Parsed CSV row data object
   * @param {number} rowNumber - Row number for error reporting
   * @param {SubstanceMetadataParseResult} result - Result object to collect errors
   * @returns {Map<string, string>|null} Row Map or null if critical errors
   * @private
   */
  _createRowMapFromCsvData(rowData, rowNumber, result) {
    const self = this;
    const rowMap = new Map();
    const metadataColumns = META_COLUMNS.filter((col) => col !== "key");

    for (const column of metadataColumns) {
      if (column in rowData) {
        rowMap.set(column, self._getOrEmpty(rowData[column]));
      } else {
        // Column missing - this would be caught earlier in header validation
        result.addError(new SubstanceMetadataError(rowNumber, column,
          "Missing column data", "USER"));
      }
    }

    return rowMap;
  }
}

/**
 * Applier for inserting substances from metadata into a Program.
 *
 * This class handles the insertion of new substances into a Program based on
 * SubstanceMetadata objects. It ensures applications exist, creates substances
 * with metadata-derived commands, and handles the integration with existing
 * program structure. Name conflicts are ignored for now (Component 6 scope).
 */
class MetaChangeApplier {
  /**
   * Create a new MetaChangeApplier instance.
   *
   * @param {Program} program - The Program instance to modify in-place
   */
  constructor(program) {
    const self = this;
    self._program = program;
  }

  /**
   * Insert or update substances from metadata update array into the program.
   *
   * This method processes an array of SubstanceMetadataUpdate objects, ensures
   * required applications exist, and either updates existing substances or
   * creates new ones based on the oldName field. If oldName matches an existing
   * substance, it will be updated; otherwise a new substance is created.
   *
   * @param {SubstanceMetadataUpdate[]} updateArray - Array of metadata update objects
   * @returns {Program} The modified program instance (for chaining)
   */
  upsertMetadata(updateArray) {
    const self = this;

    // Process each update object
    for (const update of updateArray) {
      const oldName = update.getOldName();
      const newMetadata = update.getNewMetadata();

      // Skip if required fields are empty
      if (!newMetadata.getSubstance().trim() || !newMetadata.getApplication().trim()) {
        continue;
      }

      // Ensure application exists
      self._ensureApplicationExists(newMetadata.getApplication());
      const app = self._program.getApplication(newMetadata.getApplication());

      if (oldName && oldName.trim()) {
        // UPDATE CASE: Find and update existing substance
        const existingSubstance = app.getSubstances().find((s) => s.getName() === oldName.trim());
        if (existingSubstance) {
          existingSubstance.updateMetadata(newMetadata, newMetadata.getApplication());
          continue;
        } else {
          console.warn(
            `No existing substance found for key "${oldName}". ` +
            "Creating new substance instead.",
          );
          // Fall through to INSERT CASE
        }
      }

      // INSERT CASE: Create new substance
      // Check for naming conflicts with new substance name
      const newName = newMetadata.getName();
      const conflictingSubstance = app.getSubstances().find((s) => s.getName() === newName);
      if (conflictingSubstance) {
        console.warn(`Substance "${newName}" already exists. Skipping insertion.`);
        continue;
      }

      // Create and insert new substance (existing logic)
      const substance = self._createSubstanceFromMetadata(newMetadata);
      self._addSubstanceToApplication(substance, newMetadata.getApplication());
    }

    return self._program;
  }

  /**
   * Ensure an application exists in the program, creating it if missing.
   *
   * @param {string} applicationName - Name of the application to ensure exists
   * @private
   */
  _ensureApplicationExists(applicationName) {
    const self = this;

    const existingApp = self._program.getApplication(applicationName);
    if (existingApp !== null) {
      return;
    }

    // Create new application with empty substances array
    const newApplication = new Application(
      applicationName, // name
      [], // substances (empty)
      false, // isModification
      true, // isCompatible
    );

    self._program.addApplication(newApplication);
  }

  /**
   * Create a Substance object from SubstanceMetadata.
   *
   * This method converts metadata fields to appropriate Command objects
   * and uses SubstanceBuilder to create the substance definition.
   *
   * @param {SubstanceMetadata} metadata - Metadata to convert to substance
   * @returns {Substance} Created substance object
   * @private
   */
  _createSubstanceFromMetadata(metadata) {
    const self = this;

    const substanceName = metadata.getName();
    const builder = new SubstanceBuilder(substanceName, false); // Not a modification

    // Use the exported parseUnitValue function for consistent parsing

    // Add GHG equals command if present
    const ghgValue = parseUnitValue(metadata.getGhg());
    if (ghgValue) {
      // For GHG commands, we need to ensure the routing works correctly by using specific target
      builder.setEqualsGhg(new Command("equals", null, ghgValue, null));
    }

    // Add energy equals command if present
    const energyValue = parseUnitValue(metadata.getEnergy());
    if (energyValue) {
      // For energy commands, we need to ensure the routing works correctly by using specific target
      builder.setEqualsKwh(new Command("equals", null, energyValue, null));
    }

    // Add enable commands based on stream flags
    if (metadata.getHasDomestic()) {
      builder.addCommand(new Command("enable", "domestic", null, null));
    }
    if (metadata.getHasImport()) {
      builder.addCommand(new Command("enable", "import", null, null));
    }
    if (metadata.getHasExport()) {
      builder.addCommand(new Command("enable", "export", null, null));
    }

    // Add initial charge commands for each stream (skip zero values)
    const domesticCharge = parseUnitValue(metadata.getInitialChargeDomestic());
    if (domesticCharge && domesticCharge.getValue() > 0) {
      builder.addCommand(new Command("initial charge", "domestic", domesticCharge, null));
    }

    const importCharge = parseUnitValue(metadata.getInitialChargeImport());
    if (importCharge && importCharge.getValue() > 0) {
      builder.addCommand(new Command("initial charge", "import", importCharge, null));
    }

    const exportCharge = parseUnitValue(metadata.getInitialChargeExport());
    if (exportCharge && exportCharge.getValue() > 0) {
      builder.addCommand(new Command("initial charge", "export", exportCharge, null));
    }

    // Add retirement command if present
    const retirementValue = parseUnitValue(metadata.getRetirement());
    if (retirementValue) {
      builder.addCommand(new Command("retire", null, retirementValue, null));
    }

    // Build the substance (compatible with UI editing)
    return builder.build(true);
  }

  /**
   * Add a substance to an application.
   *
   * @param {Substance} substance - Substance to add
   * @param {string} applicationName - Name of application to add substance to
   * @private
   */
  _addSubstanceToApplication(substance, applicationName) {
    const self = this;

    const application = self._program.getApplication(applicationName);
    if (application === null) {
      throw new Error(`Application ${applicationName} not found`);
    }

    // Use insertSubstance method (null means no prior substance to replace)
    application.insertSubstance(null, substance);
  }
}

/**
 * Container for substance metadata updates with old and new information.
 *
 * Used to distinguish between inserting new substances and updating existing ones.
 * The oldName corresponds to the key column in CSV files and identifies which
 * existing substance should be updated.
 */
class SubstanceMetadataUpdate {
  /**
   * Create a new SubstanceMetadataUpdate instance.
   *
   * @param {string} oldName - The name of the existing substance to update (from CSV key column)
   * @param {SubstanceMetadata} newMetadata - The new metadata to apply
   */
  constructor(oldName, newMetadata) {
    const self = this;
    self._oldName = oldName || "";
    self._newMetadata = newMetadata;
  }

  /**
   * Get the old name (key) that identifies which existing substance to update.
   *
   * @returns {string} The old name from CSV key column
   */
  getOldName() {
    const self = this;
    return self._oldName;
  }

  /**
   * Get the new metadata to apply to the substance.
   *
   * @returns {SubstanceMetadata} The new metadata object
   */
  getNewMetadata() {
    const self = this;
    return self._newMetadata;
  }
}

/**
 * Parse unit value strings like "5 kgCO2e / kg" into EngineNumber instances.
 * Handles numeric values with optional commas, decimals, and percentage signs.
 *
 * @param {string} valueString - String containing numeric value and units
   *   (e.g., "1430 kgCO2e / kg", "10% / year")
 * @param {boolean} throwOnError - Whether to throw errors for invalid formats (default: false)
 * @returns {EngineNumber|null} Parsed EngineNumber instance, or null if invalid/empty
 */
function parseUnitValue(valueString, throwOnError = false) {
  if (!valueString || typeof valueString !== "string" || !valueString.trim()) {
    return null;
  }

  const trimmed = valueString.trim();

  // For backward compatibility with original ui_translator behavior
  if (throwOnError) {
    // Find the first space to separate value from units
    const firstSpaceIndex = trimmed.indexOf(" ");
    if (firstSpaceIndex === -1) {
      throw new Error(`Invalid unit value format: ${valueString}`);
    }

    const valueStringLocal = trimmed.substring(0, firstSpaceIndex);
    const unitsString = trimmed.substring(firstSpaceIndex + 1);

    // Parse numeric value (removing commas and handling signs)
    const cleanedValue = valueStringLocal.replace(/,/g, "");
    const numericValue = parseFloat(cleanedValue);

    if (isNaN(numericValue)) {
      throw new Error(`Invalid numeric value: ${valueStringLocal}`);
    }

    return new EngineNumber(numericValue, unitsString);
  }

  // Enhanced regex-based parsing for better handling of percentages and complex formats
  // Match number (with optional commas, decimals, and %) followed by units
  const match = trimmed.match(/^([+-]?[\d,]+(?:\.\d+)?%?)\s+(.+)$/);

  if (!match) {
    return null; // Need both value and units in the format "number units"
  }

  const valueStringClean = match[1];
  const unitsString = match[2];

  // Remove commas and percentage signs from value, then parse the numeric value
  const cleanedValue = valueStringClean.replace(/[,%]/g, "");
  const numericValue = parseFloat(cleanedValue);

  if (isNaN(numericValue)) {
    return null;
  }

  // If the original value had a %, include it in the units
  const finalUnits = valueStringClean.includes("%") ?
    "% " + unitsString : unitsString;

  return new EngineNumber(numericValue, finalUnits);
}

export {
  MetaSerializer,
  MetaChangeApplier,
  SubstanceMetadataUpdate,
  SubstanceMetadataError,
  SubstanceMetadataParseResult,
  parseUnitValue,
};
