/**
 * Serialization logic for substance metadata CSV import/export.
 *
 * Provides classes and utilities for converting between SubstanceMetadata
 * objects and CSV format for bulk import/export operations.
 *
 * @license BSD, see LICENSE.md
 */

import {SubstanceMetadata, SubstanceMetadataBuilder} from "ui_translator";

/**
 * Serializer for converting SubstanceMetadata objects to CSV format.
 *
 * This class handles the conversion of SubstanceMetadata arrays to CSV-compatible
 * Maps and generates data URIs for browser downloads. The CSV format follows the
 * specification defined in the import_export_meta task.
 */
class MetaSerializer {
  /**
   * Create a new MetaSerializer instance.
   */
  constructor() {
    // No initialization needed
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

    // Validate input
    if (!metadataArray || !Array.isArray(metadataArray)) {
      throw new Error("Input must be an array of SubstanceMetadata objects");
    }

    // Convert each metadata object to a Map
    return metadataArray.map((metadata) => {
      if (!metadata || typeof metadata.getSubstance !== "function") {
        throw new Error("Array must contain valid SubstanceMetadata instances");
      }

      const rowMap = new Map();

      // Set columns in the exact order specified in task
      rowMap.set("substance", metadata.getSubstance() || "");
      rowMap.set("equipment", metadata.getEquipment() || "");
      rowMap.set("application", metadata.getApplication() || "");
      rowMap.set("ghg", metadata.getGhg() || "");
      rowMap.set("hasDomestic", metadata.getHasDomestic().toString());
      rowMap.set("hasImport", metadata.getHasImport().toString());
      rowMap.set("hasExport", metadata.getHasExport().toString());
      rowMap.set("energy", metadata.getEnergy() || "");
      rowMap.set("initialChargeDomestic", metadata.getInitialChargeDomestic() || "");
      rowMap.set("initialChargeImport", metadata.getInitialChargeImport() || "");
      rowMap.set("initialChargeExport", metadata.getInitialChargeExport() || "");
      rowMap.set("retirement", metadata.getRetirement() || "");
      rowMap.set("key", metadata.getKey() || "");

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

    // Get serialized data
    const serializedMaps = self.serialize(metadataArray);

    // Generate CSV content
    const csvContent = self._generateCsvString(serializedMaps);

    // Create and return data URI
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

    // Define column order as specified in task
    const columns = [
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

    const csvRows = [];

    // Add header row
    csvRows.push(columns.join(","));

    // Add data rows
    for (const rowMap of serializedMaps) {
      const rowValues = columns.map((column) => {
        const value = rowMap.get(column) || "";
        return self._escapeCsvValue(value);
      });
      csvRows.push(rowValues.join(","));
    }

    return csvRows.join("\n");
  }

  /**
   * Escape a CSV value according to RFC 4180 specification.
   *
   * Values containing commas, quotes, or newlines are wrapped in quotes.
   * Internal quotes are escaped by doubling them.
   *
   * @param {string} value - The value to escape
   * @returns {string} Escaped CSV value
   * @private
   */
  _escapeCsvValue(value) {
    if (!value) {
      return "";
    }

    const stringValue = String(value);

    // Check if escaping is needed
    if (stringValue.includes("\"") || stringValue.includes(",") ||
        stringValue.includes("\n") || stringValue.includes("\r")) {
      // Escape internal quotes by doubling them and wrap in quotes
      return "\"" + stringValue.replace(/"/g, "\"\"") + "\"";
    }

    return stringValue;
  }

  /**
   * Convert array of Maps to array of SubstanceMetadata for CSV import.
   *
   * Each Map represents a CSV row with column names as keys and string values.
   * Boolean fields are converted from string representation to boolean values.
   * Empty or missing values are handled gracefully with defaults.
   *
   * @param {Map<string, string>[]} arrayOfMaps - Array of Maps with CSV data
   * @returns {SubstanceMetadata[]} Array of SubstanceMetadata instances
   */
  deserialize(arrayOfMaps) {
    const self = this;

    // Validate input
    if (!arrayOfMaps || !Array.isArray(arrayOfMaps)) {
      throw new Error("Input must be an array of Map objects");
    }

    // Convert each Map to a SubstanceMetadata object
    return arrayOfMaps.map((rowMap, index) => {
      if (!rowMap || typeof rowMap.get !== "function") {
        throw new Error(`Row ${index} must be a Map instance`);
      }

      try {
        const builder = new SubstanceMetadataBuilder();

        // Extract values from Map with defaults
        builder.setSubstance(rowMap.get("substance") || "")
               .setEquipment(rowMap.get("equipment") || "")
               .setApplication(rowMap.get("application") || "")
               .setGhg(rowMap.get("ghg") || "")
               .setHasDomestic(self._parseBoolean(rowMap.get("hasDomestic")))
               .setHasImport(self._parseBoolean(rowMap.get("hasImport")))
               .setHasExport(self._parseBoolean(rowMap.get("hasExport")))
               .setEnergy(rowMap.get("energy") || "")
               .setInitialChargeDomestic(rowMap.get("initialChargeDomestic") || "")
               .setInitialChargeImport(rowMap.get("initialChargeImport") || "")
               .setInitialChargeExport(rowMap.get("initialChargeExport") || "")
               .setRetirement(rowMap.get("retirement") || "");

        return builder.build();
      } catch (error) {
        throw new Error(`Failed to deserialize row ${index}: ${error.message}`);
      }
    });
  }

  /**
   * Convert CSV string to array of SubstanceMetadata for import.
   *
   * Uses Papa Parse to parse CSV string with headers into objects,
   * then converts to SubstanceMetadata instances. Handles parsing errors
   * and validates CSV structure.
   *
   * @param {string} csvString - CSV content string with headers
   * @returns {SubstanceMetadata[]} Array of SubstanceMetadata instances
   */
  deserializeMetaFromCsvString(csvString) {
    const self = this;

    // Validate input
    if (typeof csvString !== "string") {
      throw new Error("Input must be a CSV string");
    }

    if (!csvString.trim()) {
      return []; // Empty CSV returns empty array
    }

    // Check if Papa Parse is available
    if (typeof Papa === "undefined") {
      throw new Error("Papa Parse library is not available. Please ensure papaparse.min.js is loaded.");
    }

    try {
      // Parse CSV using Papa Parse
      const parseResult = Papa.parse(csvString, {
        header: true,           // Use first row as column names
        dynamicTyping: false,   // Keep values as strings for manual conversion
        skipEmptyLines: true,   // Ignore blank lines
        delimiter: ",",         // Standard CSV delimiter
        quoteChar: "\"",        // Standard CSV quote character
        escapeChar: "\""        // RFC 4180 quote escaping
      });

      // Check for parsing errors
      if (parseResult.errors && parseResult.errors.length > 0) {
        const errorMessages = parseResult.errors.map(error => 
          `Row ${error.row}: ${error.message}`
        ).join("; ");
        throw new Error(`CSV parsing failed: ${errorMessages}`);
      }

      // Validate that we have data
      if (!parseResult.data || !Array.isArray(parseResult.data)) {
        throw new Error("CSV parsing returned invalid data structure");
      }

      // Validate required columns are present
      if (parseResult.data.length > 0) {
        const firstRow = parseResult.data[0];
        const requiredColumns = ["substance", "application"];
        for (const column of requiredColumns) {
          if (!(column in firstRow)) {
            throw new Error(`Missing required column: ${column}`);
          }
        }
      }

      // Convert parsed data to Maps
      const arrayOfMaps = parseResult.data.map(rowData => {
        const rowMap = new Map();
        
        // Set all expected columns
        const expectedColumns = [
          "substance", "equipment", "application", "ghg",
          "hasDomestic", "hasImport", "hasExport", "energy",
          "initialChargeDomestic", "initialChargeImport", "initialChargeExport",
          "retirement", "key"
        ];

        for (const column of expectedColumns) {
          rowMap.set(column, rowData[column] || "");
        }

        return rowMap;
      });

      // Convert Maps to SubstanceMetadata using existing deserialize method
      return self.deserialize(arrayOfMaps);

    } catch (error) {
      if (error.message.includes("CSV parsing failed") || error.message.includes("Failed to deserialize")) {
        throw error; // Re-throw our custom errors
      }
      throw new Error(`CSV deserialization failed: ${error.message}`);
    }
  }

  /**
   * Parse a string value to boolean.
   *
   * Handles case-insensitive "true"/"false" strings and provides
   * sensible defaults for invalid or empty values.
   *
   * @param {string} value - String value to parse as boolean
   * @returns {boolean} Parsed boolean value
   * @private
   */
  _parseBoolean(value) {
    if (!value || typeof value !== "string") {
      return false;
    }

    const trimmed = value.trim().toLowerCase();
    return trimmed === "true" || trimmed === "1";
  }
}

export {
  MetaSerializer,
};
