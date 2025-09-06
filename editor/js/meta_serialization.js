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
 * Result container for metadata validation operations.
 *
 * Tracks validation state and collects validation errors for reporting.
 * Provides methods to check success state and combine multiple validation results.
 */
class ValidationResult {
  /**
   * Create a new ValidationResult.
   *
   * @param {boolean} isValid - Whether validation passed
   * @param {string[]} errors - Array of validation error messages
   */
  constructor(isValid = true, errors = []) {
    const self = this;
    self._isValid = isValid;
    self._errors = errors;
  }

  /**
   * Check if validation was successful.
   *
   * @returns {boolean} True if validation passed
   */
  isValid() {
    const self = this;
    return self._isValid;
  }

  /**
   * Get all validation error messages.
   *
   * @returns {string[]} Array of error messages
   */
  getErrors() {
    const self = this;
    return self._errors;
  }

  /**
   * Check if validation failed.
   *
   * @returns {boolean} True if validation failed
   */
  hasErrors() {
    const self = this;
    return !self._isValid;
  }

  /**
   * Create a successful validation result.
   *
   * @returns {ValidationResult} Success result
   */
  static success() {
    return new ValidationResult(true, []);
  }

  /**
   * Create a failed validation result.
   *
   * @param {string[]} errors - Array of error messages
   * @returns {ValidationResult} Failure result
   */
  static failure(errors) {
    return new ValidationResult(false, errors);
  }

  /**
   * Add an error message to this result.
   *
   * @param {string} message - Error message to add
   */
  addError(message) {
    const self = this;
    self._errors.push(message);
    self._isValid = false;
  }

  /**
   * Combine this result with another validation result.
   *
   * @param {ValidationResult} other - Other result to combine
   * @returns {ValidationResult} Combined result
   */
  combine(other) {
    const self = this;
    const combinedErrors = [...self._errors, ...other.getErrors()];
    const combinedValid = self._isValid && other.isValid();
    return new ValidationResult(combinedValid, combinedErrors);
  }

  /**
   * Get formatted error summary.
   *
   * @returns {string} Formatted error messages joined by newlines
   */
  getErrorSummary() {
    const self = this;
    return self._errors.join("\n");
  }
}

/**
 * Validator for SubstanceMetadata objects.
 *
 * Provides comprehensive validation for required fields and optional field formats.
 * Validates individual metadata objects and batches of metadata objects.
 */
class MetadataValidator {
  /**
   * Create a new MetadataValidator.
   */
  constructor() {
    const self = this;

    // Define required fields and their validation rules
    self._requiredFields = new Map([
      ["substance", self._validateSubstanceName.bind(self)],
      ["application", self._validateApplicationName.bind(self)],
    ]);

    // Define optional field validators
    self._optionalFieldValidators = new Map([
      ["ghg", self._validateGhgValue.bind(self)],
      ["energy", self._validateEnergyValue.bind(self)],
      ["retirement", self._validateRetirementValue.bind(self)],
      ["initialChargeDomestic", self._validateChargeValue.bind(self)],
      ["initialChargeImport", self._validateChargeValue.bind(self)],
      ["initialChargeExport", self._validateChargeValue.bind(self)],
    ]);
  }

  /**
   * Validate a single SubstanceMetadata object.
   *
   * @param {SubstanceMetadata} metadata - Metadata object to validate
   * @param {number} index - Index of this metadata in a batch (for error reporting)
   * @returns {ValidationResult} Validation result
   */
  validateSingle(metadata, index = 0) {
    const self = this;
    const result = new ValidationResult();

    // Validate required fields
    for (const [fieldName, validator] of self._requiredFields) {
      const fieldValue = self._getFieldValue(metadata, fieldName);
      const fieldResult = validator(fieldValue, fieldName, index);
      if (!fieldResult.isValid()) {
        result.addError(`Row ${index + 1}: ${fieldResult.getErrorSummary()}`);
      }
    }

    // Validate optional fields if they have values
    for (const [fieldName, validator] of self._optionalFieldValidators) {
      const fieldValue = self._getFieldValue(metadata, fieldName);
      if (fieldValue && fieldValue.trim()) {
        const fieldResult = validator(fieldValue, fieldName, index);
        if (!fieldResult.isValid()) {
          result.addError(`Row ${index + 1}: ${fieldResult.getErrorSummary()}`);
        }
      }
    }

    return result;
  }

  /**
   * Validate a batch of SubstanceMetadata objects.
   *
   * @param {SubstanceMetadata[]} metadataArray - Array of metadata objects to validate
   * @returns {ValidationResult} Overall validation result
   */
  validateBatch(metadataArray) {
    const self = this;
    let overallResult = ValidationResult.success();

    // Validate each metadata object individually
    for (let i = 0; i < metadataArray.length; i++) {
      const singleResult = self.validateSingle(metadataArray[i], i);
      overallResult = overallResult.combine(singleResult);
    }

    // Check for duplicate substance names within batch
    const duplicateResult = self._validateNoDuplicateNames(metadataArray);
    overallResult = overallResult.combine(duplicateResult);

    return overallResult;
  }

  /**
   * Get field value from metadata object using appropriate getter method.
   *
   * @param {SubstanceMetadata} metadata - Metadata object
   * @param {string} fieldName - Name of field to get
   * @returns {string} Field value or empty string
   * @private
   */
  _getFieldValue(metadata, fieldName) {
    const self = this;
    const getterMap = {
      "substance": () => metadata.getSubstance(),
      "application": () => metadata.getApplication(),
      "ghg": () => metadata.getGhg(),
      "energy": () => metadata.getEnergy(),
      "retirement": () => metadata.getRetirement(),
      "initialChargeDomestic": () => metadata.getInitialChargeDomestic(),
      "initialChargeImport": () => metadata.getInitialChargeImport(),
      "initialChargeExport": () => metadata.getInitialChargeExport(),
    };

    const getter = getterMap[fieldName];
    return getter ? getter() : "";
  }

  /**
   * Validate substance name field.
   *
   * @param {string} value - Field value
   * @param {string} fieldName - Field name
   * @param {number} index - Row index
   * @returns {ValidationResult} Validation result
   * @private
   */
  _validateSubstanceName(value, fieldName, index) {
    const self = this;

    if (!value || !value.trim()) {
      return ValidationResult.failure(["Substance name is required"]);
    }

    if (value.trim().length < 2) {
      return ValidationResult.failure(["Substance name must be at least 2 characters long"]);
    }

    // Check for invalid characters based on QubecTalk grammar
    if (value.includes("\"") || value.includes("\n") || value.includes("\r")) {
      return ValidationResult.failure(["Substance name cannot contain quotes or newlines"]);
    }

    return ValidationResult.success();
  }

  /**
   * Validate application name field.
   *
   * @param {string} value - Field value
   * @param {string} fieldName - Field name
   * @param {number} index - Row index
   * @returns {ValidationResult} Validation result
   * @private
   */
  _validateApplicationName(value, fieldName, index) {
    const self = this;

    if (!value || !value.trim()) {
      return ValidationResult.failure(["Application name is required"]);
    }

    if (value.trim().length < 2) {
      return ValidationResult.failure(["Application name must be at least 2 characters long"]);
    }

    return ValidationResult.success();
  }

  /**
   * Validate GHG value format.
   *
   * @param {string} value - Field value
   * @param {string} fieldName - Field name
   * @param {number} index - Row index
   * @returns {ValidationResult} Validation result
   * @private
   */
  _validateGhgValue(value, fieldName, index) {
    const self = this;

    if (!parseUnitValue(value)) {
      return ValidationResult.failure([
        "GHG value must be in format \"number unit\" (e.g., \"1430 kgCO2e / kg\")",
      ]);
    }
    return ValidationResult.success();
  }

  /**
   * Validate energy value format.
   *
   * @param {string} value - Field value
   * @param {string} fieldName - Field name
   * @param {number} index - Row index
   * @returns {ValidationResult} Validation result
   * @private
   */
  _validateEnergyValue(value, fieldName, index) {
    const self = this;

    if (!parseUnitValue(value)) {
      return ValidationResult.failure([
        "Energy value must be in format \"number unit\" (e.g., \"500 kwh / unit\")",
      ]);
    }
    return ValidationResult.success();
  }

  /**
   * Validate retirement value format.
   *
   * @param {string} value - Field value
   * @param {string} fieldName - Field name
   * @param {number} index - Row index
   * @returns {ValidationResult} Validation result
   * @private
   */
  _validateRetirementValue(value, fieldName, index) {
    const self = this;

    if (!parseUnitValue(value)) {
      return ValidationResult.failure([
        "Retirement value must be in format \"number unit\" (e.g., \"10% / year\")",
      ]);
    }
    return ValidationResult.success();
  }

  /**
   * Validate charge value format.
   *
   * @param {string} value - Field value
   * @param {string} fieldName - Field name
   * @param {number} index - Row index
   * @returns {ValidationResult} Validation result
   * @private
   */
  _validateChargeValue(value, fieldName, index) {
    const self = this;

    if (!parseUnitValue(value)) {
      return ValidationResult.failure([
        "Charge value must be in format \"number unit\" (e.g., \"0.15 kg / unit\")",
      ]);
    }
    return ValidationResult.success();
  }

  /**
   * Validate no duplicate names in metadata batch.
   *
   * @param {SubstanceMetadata[]} metadataArray - Array of metadata objects
   * @returns {ValidationResult} Validation result
   * @private
   */
  _validateNoDuplicateNames(metadataArray) {
    const self = this;
    const names = new Map(); // name -> first occurrence index
    const errors = [];

    for (let i = 0; i < metadataArray.length; i++) {
      const metadata = metadataArray[i];
      const name = metadata.getName(); // This combines substance and equipment

      if (names.has(name)) {
        const firstIndex = names.get(name);
        errors.push(`Duplicate substance "${name}" found at rows ${firstIndex + 1} and ${i + 1}`);
      } else {
        names.set(name, i);
      }
    }

    return errors.length > 0 ? ValidationResult.failure(errors) : ValidationResult.success();
  }
}

/**
 * Custom error class for validation failures.
 *
 * Extends the standard Error class with validation-specific functionality.
 * Always indicates user-correctable errors rather than system errors.
 */
class ValidationError extends Error {
  /**
   * Create a new ValidationError.
   *
   * @param {string} message - Error message
   * @param {string[]} validationErrors - Array of detailed validation error messages
   */
  constructor(message, validationErrors = []) {
    super(message);
    const self = this;
    self.name = "ValidationError";
    self._validationErrors = validationErrors;
  }

  /**
   * Get detailed validation error messages.
   *
   * @returns {string[]} Array of validation error messages
   */
  getValidationErrors() {
    const self = this;
    return self._validationErrors;
  }

  /**
   * Check if this is a user-correctable error.
   *
   * @returns {boolean} Always true for validation errors
   */
  isUserError() {
    return true; // Validation errors are always user-fixable
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
    self._validator = new MetadataValidator(); // Add validator
  }

  /**
   * Enhanced validation method with detailed field checking.
   * Validates all updates comprehensively before any processing begins.
   *
   * @param {SubstanceMetadataUpdate[]} updateArray - Array of updates to validate
   * @throws {ValidationError} If any validation fails with detailed field information
   * @private
   */
  _validateUpdatesComprehensive(updateArray) {
    const self = this;
    const allErrors = [];

    for (let i = 0; i < updateArray.length; i++) {
      const update = updateArray[i];
      const metadata = update.getNewMetadata();

      // Get field-specific validation errors
      const fieldErrors = self._validateRequiredFields(metadata, i);
      allErrors.push(...fieldErrors);
    }

    if (allErrors.length > 0) {
      const errorMessage = "Validation failed for metadata updates. " +
        "Please correct the following issues:\n" + allErrors.join("\n");
      throw new ValidationError(errorMessage, allErrors);
    }
  }

  /**
   * Check if a metadata object has all required fields populated.
   *
   * @param {SubstanceMetadata} metadata - Metadata to validate
   * @param {number} index - Index in batch for error reporting
   * @returns {string[]} Array of validation error messages (empty if valid)
   * @private
   */
  _validateRequiredFields(metadata, index) {
    const self = this;
    const errors = [];
    const rowNumber = index + 1;

    // Check substance name
    const substanceName = metadata.getSubstance();
    if (!substanceName || !substanceName.trim()) {
      errors.push(`Row ${rowNumber}: Substance name is required and cannot be empty`);
    } else if (substanceName.trim().length < 2) {
      errors.push(`Row ${rowNumber}: Substance name must be at least 2 characters long`);
    }

    // Check application name
    const applicationName = metadata.getApplication();
    if (!applicationName || !applicationName.trim()) {
      errors.push(`Row ${rowNumber}: Application name is required and cannot be empty`);
    } else if (applicationName.trim().length < 2) {
      errors.push(`Row ${rowNumber}: Application name must be at least 2 characters long`);
    }

    return errors;
  }

  /**
   * Insert or update substances from metadata update array into the program.
   *
   * This method processes an array of SubstanceMetadataUpdate objects, ensures
   * required applications exist, and either updates existing substances or
   * creates new ones based on the oldName field. If oldName matches an existing
   * substance, it will be updated; otherwise a new substance is created.
   *
   * All metadata is validated before any changes are applied to prevent partial updates.
   *
   * @param {SubstanceMetadataUpdate[]} updateArray - Array of metadata update objects
   * @returns {Program} The modified program instance (for chaining)
   * @throws {ValidationError} If validation fails for any metadata
   * @throws {Error} If input is invalid
   */
  upsertMetadata(updateArray) {
    const self = this;

    // Input validation
    if (!Array.isArray(updateArray)) {
      throw new Error("Input must be an array of SubstanceMetadataUpdate objects");
    }

    if (updateArray.length === 0) {
      return self._program; // No work to do
    }

    // Validate instance types
    for (const update of updateArray) {
      if (!(update instanceof SubstanceMetadataUpdate)) {
        throw new Error("All items must be SubstanceMetadataUpdate instances");
      }
    }

    // Enhanced comprehensive validation - checks required fields upfront
    self._validateUpdatesComprehensive(updateArray);

    // Extract metadata for additional validation
    const metadataArray = updateArray.map((update) => update.getNewMetadata());

    // Pre-validate all metadata before making any changes (optional field validation)
    const validationResult = self._validator.validateBatch(metadataArray);

    if (!validationResult.isValid()) {
      const errorMessage =
        "Validation failed for metadata updates:\n" + validationResult.getErrorSummary();
      throw new ValidationError(errorMessage, validationResult.getErrors());
    }

    // All validation passed - proceed with updates
    for (const update of updateArray) {
      self._upsertMetadataSingle(update);
    }

    return self._program;
  }

  /**
   * Process a single metadata update (insert or update).
   *
   * @param {SubstanceMetadataUpdate} update - Single update to process
   * @private
   */
  _upsertMetadataSingle(update) {
    const self = this;
    const oldName = update.getOldName();
    const newMetadata = update.getNewMetadata();

    // Ensure application exists
    self._ensureApplicationExists(newMetadata.getApplication());

    const oldNameGiven = oldName && oldName.trim();
    const existingSubstance = oldNameGiven &&
      self._getSubstanceExists(oldName, newMetadata.getApplication());

    if (existingSubstance) {
      self._updateMetadataSingle(newMetadata, existingSubstance);
    } else {
      // If oldName was given but no substance found, warn the user
      if (oldNameGiven) {
        console.warn(
          `No existing substance found for key "${oldName}". ` +
          "Creating new substance instead.",
        );
      }
      self._insertMetadataSingle(newMetadata);
    }
  }

  /**
   * Update an existing substance with new metadata.
   *
   * @param {SubstanceMetadata} metadata - New metadata to apply
   * @param {Substance} existingSubstance - Existing substance to update
   * @private
   */
  _updateMetadataSingle(metadata, existingSubstance) {
    const self = this;
    // Call existing updateMetadata method on substance
    existingSubstance.updateMetadata(metadata, metadata.getApplication());
  }

  /**
   * Insert a new substance from metadata.
   *
   * @param {SubstanceMetadata} metadata - Metadata for new substance
   * @private
   */
  _insertMetadataSingle(metadata) {
    const self = this;
    // Check for naming conflicts
    const newName = metadata.getName();
    const application = self._program.getApplication(metadata.getApplication());
    const conflictingSubstance = application.getSubstances()
      .find((s) => s.getName() === newName);

    if (conflictingSubstance) {
      console.warn(`Substance "${newName}" already exists. Skipping insertion.`);
      return;
    }

    // Create and insert new substance
    const substance = self._createSubstanceFromMetadata(metadata);
    self._addSubstanceToApplication(substance, metadata.getApplication());
  }

  /**
   * Check if a substance exists by name in an application.
   *
   * @param {string} substanceName - Name to search for
   * @param {string} applicationName - Application to search in
   * @returns {Substance|null} Found substance or null
   * @private
   */
  _getSubstanceExists(substanceName, applicationName) {
    const self = this;
    const application = self._program.getApplication(applicationName);
    if (!application) return null;

    return application.getSubstances()
      .find((s) => s.getName() === substanceName.trim()) || null;
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

    // Use the enhanced _parseUnitValue method for consistent and robust parsing

    // Add GHG equals command if present
    const ghgValue = self._parseUnitValue(metadata.getGhg());
    if (ghgValue) {
      // For GHG commands, we need to ensure the routing works correctly by using specific target
      builder.setEqualsGhg(new Command("equals", null, ghgValue, null));
    }

    // Add energy equals command if present
    const energyValue = self._parseUnitValue(metadata.getEnergy());
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
    const domesticCharge = self._parseUnitValue(metadata.getInitialChargeDomestic());
    if (domesticCharge && domesticCharge.getValue() > 0) {
      builder.addCommand(new Command("initial charge", "domestic", domesticCharge, null));
    }

    const importCharge = self._parseUnitValue(metadata.getInitialChargeImport());
    if (importCharge && importCharge.getValue() > 0) {
      builder.addCommand(new Command("initial charge", "import", importCharge, null));
    }

    const exportCharge = self._parseUnitValue(metadata.getInitialChargeExport());
    if (exportCharge && exportCharge.getValue() > 0) {
      builder.addCommand(new Command("initial charge", "export", exportCharge, null));
    }

    // Add retirement command if present
    const retirementValue = self._parseUnitValue(metadata.getRetirement());
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

  /**
   * Parse unit value strings with comprehensive validation and error handling.
   *
   * This method provides enhanced parsing capabilities for unit value strings
   * commonly used in substance metadata. It supports various numeric formats
   * including integers, decimals, negative numbers, comma separators, and
   * percentages. The method is designed to be more robust and maintainable
   * than the global parseUnitValue function.
   *
   * Supported formats include:
   * - "1430 kgCO2e / kg" (GHG values)
   * - "10% / year" (percentage values)
   * - "0.15 kg / unit" (charge values)
   * - "500 kwh / unit" (energy values)
   * - "-5.5 mt / year" (negative values)
   * - "1,500.25 units" (comma-separated values)
   *
   * @param {string} valueString - String containing numeric value and units
   *   Must be in format "number units" where number can include decimals,
   *   commas, signs, and optional percentage symbol
   * @param {boolean} throwOnError - Whether to throw errors for invalid formats.
   *   When false, returns null for invalid inputs. When true, throws detailed
   *   error messages for debugging and validation purposes.
   * @returns {EngineNumber|null} Parsed EngineNumber instance with numeric value
   *   and units string, or null if invalid/empty and throwOnError is false
   * @throws {Error} If throwOnError is true and parsing fails. Error messages
   *   include specific details about what was invalid for user feedback.
   * @private
   */
  _parseUnitValue(valueString, throwOnError = false) {
    const self = this;

    // Input validation - check for null, undefined, and non-string types
    if (valueString === null || valueString === undefined) {
      if (throwOnError) {
        const actualType = valueString === null ? "null" : "undefined";
        throw new Error(`Value string must be a non-empty string, got: ${actualType}`);
      }
      return null;
    }

    if (typeof valueString !== "string") {
      if (throwOnError) {
        throw new Error(`Value string must be a non-empty string, got: ${typeof valueString}`);
      }
      return null;
    }

    const trimmed = valueString.trim();
    if (!trimmed) {
      if (throwOnError) {
        throw new Error("Value string cannot be empty or whitespace-only");
      }
      return null;
    }

    // Enhanced regex-based parsing with better error detection
    // Matches: optional sign, digits with optional commas, optional decimal,
    // optional %, space, non-whitespace units
    // Examples: "+1,500.25% kwh / unit", "-42.5 kgCO2e / kg", "10% / year"
    const match = trimmed.match(/^([+-]?[\d,]+(?:\.\d+)?%?)\s+(\S.*)$/);

    if (!match) {
      if (throwOnError) {
        // Provide specific guidance based on common error patterns
        // Order matters: check more specific patterns first
        if (/^(Infinity|-?Infinity)\s+/.test(trimmed)) {
          // Special case for Infinity values - check this first
          throw new Error(
            `Invalid numeric value: "${trimmed.split(/\s+/)[0]}" in "${valueString}". ` +
            "Number must be finite (not infinite or NaN).",
          );
        } else if (/^\d+\s*$/.test(trimmed)) {
          // Special case for numbers with trailing space but no units
          throw new Error(
            `Invalid unit value format: "${valueString}". ` +
            "Units portion cannot be empty. " +
            "Example: \"1430 kgCO2e / kg\"",
          );
        } else if (/\d+\.\d+\.\d+/.test(trimmed)) {
          // Special case for malformed decimals like "12.34.56"
          const parts = trimmed.split(/\s+/);
          if (parts.length >= 2) {
            throw new Error(
              `Invalid numeric value: "${parts[0]}" in "${valueString}". ` +
              "Must be a valid number (integer or decimal). " +
              "Examples: \"1430\", \"10.5\", \"-42\", \"1,500.25\"",
            );
          }
        } else if (!trimmed.includes(" ")) {
          throw new Error(
            `Invalid unit value format: "${valueString}". ` +
            "Expected format: \"number units\" with a space between number and units. " +
            "Example: \"1430 kgCO2e / kg\"",
          );
        } else if (/^\s*[^\d+-]/.test(trimmed)) {
          throw new Error(
            `Invalid unit value format: "${valueString}". ` +
            "Must start with a number (optionally signed). " +
            "Example: \"10% / year\" or \"-5.5 mt / year\"",
          );
        } else {
          throw new Error(
            `Invalid unit value format: "${valueString}". ` +
            "Expected format: \"number units\". " +
            "Examples: \"1430 kgCO2e / kg\", \"10% / year\", \"0.15 kg / unit\"",
          );
        }
      }
      return null;
    }

    const [, valueStr, unitsStr] = match;

    // Validate units string is not empty
    if (!unitsStr || !unitsStr.trim()) {
      if (throwOnError) {
        throw new Error(
          `Invalid unit value format: "${valueString}". ` +
          "Units portion cannot be empty. " +
          "Example: \"1430 kgCO2e / kg\"",
        );
      }
      return null;
    }

    // Parse numeric value with enhanced validation
    const cleanedValue = valueStr.replace(/[,%]/g, "");

    // Special check for Infinity before parsing
    if (cleanedValue.toLowerCase() === "infinity" || cleanedValue.toLowerCase() === "-infinity") {
      if (throwOnError) {
        throw new Error(
          `Invalid numeric value: "${valueStr}" in "${valueString}". ` +
          "Number must be finite (not infinite or NaN).",
        );
      }
      return null;
    }

    const numericValue = parseFloat(cleanedValue);

    if (isNaN(numericValue)) {
      if (throwOnError) {
        throw new Error(
          `Invalid numeric value: "${valueStr}" in "${valueString}". ` +
          "Must be a valid number (integer or decimal). " +
          "Examples: \"1430\", \"10.5\", \"-42\", \"1,500.25\"",
        );
      }
      return null;
    }

    // Additional numeric validation for edge cases
    if (!isFinite(numericValue)) {
      if (throwOnError) {
        throw new Error(
          `Invalid numeric value: "${valueStr}" in "${valueString}". ` +
          "Number must be finite (not infinite or NaN).",
        );
      }
      return null;
    }

    // Handle percentage units - if original value had %, include it in units
    const finalUnits = valueStr.includes("%") ? `% ${unitsStr.trim()}` : unitsStr.trim();

    // Validate final units string format
    if (!finalUnits) {
      if (throwOnError) {
        throw new Error(
          `Invalid units: "${unitsStr}" in "${valueString}". ` +
          "Units cannot be empty after processing.",
        );
      }
      return null;
    }

    try {
      return new EngineNumber(numericValue, finalUnits);
    } catch (engineError) {
      if (throwOnError) {
        throw new Error(
          `Failed to create EngineNumber from "${valueString}": ${engineError.message}`,
        );
      }
      return null;
    }
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
  ValidationResult,
  MetadataValidator,
  ValidationError,
  parseUnitValue,
};
