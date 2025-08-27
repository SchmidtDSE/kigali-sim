/**
 * Serialization logic for substance metadata CSV import/export.
 *
 * Provides classes and utilities for converting between SubstanceMetadata
 * objects and CSV format for bulk import/export operations.
 *
 * @license BSD, see LICENSE.md
 */

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
}

export {
  MetaSerializer,
};
