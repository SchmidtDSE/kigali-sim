/**
 * Serialization logic for substance metadata CSV import/export.
 *
 * Provides classes and utilities for converting between SubstanceMetadata
 * objects and CSV format for bulk import/export operations.
 *
 * @license BSD, see LICENSE.md
 */

import {SubstanceMetadata, SubstanceMetadataBuilder, Program, Application, Substance, SubstanceBuilder, Command} from "ui_translator";
import {EngineNumber} from "engine_number";

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
   * Convert CSV string to array of SubstanceMetadataUpdate for import.
   *
   * Uses Papa Parse to parse CSV string with headers into objects,
   * then converts to SubstanceMetadataUpdate instances. The CSV key column
   * is used as the oldName to identify existing substances for updates.
   *
   * @param {string} csvString - CSV content string with headers
   * @returns {SubstanceMetadataUpdate[]} Array of SubstanceMetadataUpdate instances
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

      // Convert parsed data to SubstanceMetadataUpdate objects
      return parseResult.data.map(rowData => {
        // Extract key column for oldName (used for updates)
        const oldName = rowData["key"] || "";
        
        // Create Map for SubstanceMetadata creation
        const rowMap = new Map();
        
        // Set all expected columns (excluding key since that's used for oldName)
        const expectedColumns = [
          "substance", "equipment", "application", "ghg",
          "hasDomestic", "hasImport", "hasExport", "energy",
          "initialChargeDomestic", "initialChargeImport", "initialChargeExport",
          "retirement"
        ];

        for (const column of expectedColumns) {
          rowMap.set(column, rowData[column] || "");
        }

        // Convert Map to SubstanceMetadata using existing deserialize method
        const metadataArray = self.deserialize([rowMap]);
        const newMetadata = metadataArray[0];
        
        // Create and return SubstanceMetadataUpdate with oldName and newMetadata
        return new SubstanceMetadataUpdate(oldName, newMetadata);
      });

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
    
    if (!program || typeof program.getApplications !== "function") {
      throw new Error("MetaChangeApplier requires a valid Program instance");
    }
    
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
    
    // Validate input
    if (!updateArray || !Array.isArray(updateArray)) {
      throw new Error("upsertMetadata requires an array of SubstanceMetadataUpdate objects");
    }
    
    // Process each update object
    for (const update of updateArray) {
      if (!update || typeof update.getOldName !== "function" || typeof update.getNewMetadata !== "function") {
        throw new Error("Array must contain valid SubstanceMetadataUpdate instances");
      }
      
      try {
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
          const existingSubstance = app.getSubstances().find(s => s.getName() === oldName.trim());
          if (existingSubstance) {
            existingSubstance.updateMetadata(newMetadata, newMetadata.getApplication());
            continue;
          } else {
            console.warn(`No existing substance found for key "${oldName}". Creating new substance instead.`);
            // Fall through to INSERT CASE
          }
        }
        
        // INSERT CASE: Create new substance
        // Check for naming conflicts with new substance name
        const newName = newMetadata.getName();
        const conflictingSubstance = app.getSubstances().find(s => s.getName() === newName);
        if (conflictingSubstance) {
          console.warn(`Substance "${newName}" already exists. Skipping insertion.`);
          continue;
        }
        
        // Create and insert new substance (existing logic)
        const substance = self._createSubstanceFromMetadata(newMetadata);
        self._addSubstanceToApplication(substance, newMetadata.getApplication());
        
      } catch (error) {
        // Log warning but continue processing other substances
        const substanceName = update.getNewMetadata() ? update.getNewMetadata().getSubstance() : "unknown";
        console.warn(`Failed to process substance ${substanceName}: ${error.message}`);
      }
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
    if (existingApp === null) {
      // Create new application with empty substances array
      const newApplication = new Application(
        applicationName,   // name
        [],              // substances (empty)
        false,           // isModification
        true             // isCompatible
      );
      
      self._program.addApplication(newApplication);
    }
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
    
    // Helper function to parse unit values from metadata strings
    const parseUnitValue = (valueString) => {
      if (!valueString || typeof valueString !== "string" || !valueString.trim()) {
        return null;
      }
      
      // Find the first space after the numeric value to separate value from units
      const trimmed = valueString.trim();
      // Match number (with optional commas, decimals, and %) followed by units
      const match = trimmed.match(/^([+-]?[\d,]+(?:\.\d+)?%?)\s+(.+)$/);
      
      if (!match) {
        return null; // Need both value and units in the format "number units"
      }
      
      const valueString_clean = match[1];
      const unitsString = match[2];
      
      // Remove commas and percentage signs from value, then parse the numeric value
      const cleanedValue = valueString_clean.replace(/[,%]/g, "");
      const numericValue = parseFloat(cleanedValue);
      
      if (isNaN(numericValue)) {
        return null;
      }
      
      // If the original value had a %, include it in the units
      const finalUnits = valueString_clean.includes('%') ? '% ' + unitsString : unitsString;
      
      return new EngineNumber(numericValue, finalUnits);
    };
    
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
    
    // Validate inputs
    if (newMetadata && !(newMetadata instanceof SubstanceMetadata)) {
      throw new Error("newMetadata must be a SubstanceMetadata instance");
    }
    
    self._oldName = oldName || "";
    self._newMetadata = newMetadata;
  }

  /**
   * Get the old name (key) that identifies which existing substance to update.
   *
   * @returns {string} The old name from CSV key column
   */
  getOldName() {
    return this._oldName;
  }

  /**
   * Get the new metadata to apply to the substance.
   *
   * @returns {SubstanceMetadata} The new metadata object
   */
  getNewMetadata() {
    return this._newMetadata;
  }
}

export {
  MetaSerializer,
  MetaChangeApplier,
  SubstanceMetadataUpdate,
};
