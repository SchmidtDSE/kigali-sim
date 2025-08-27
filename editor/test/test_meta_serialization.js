import {MetaSerializer, MetaChangeApplier} from "meta_serialization";
import {SubstanceMetadata, Program, Application, Substance, SubstanceBuilder} from "ui_translator";

function buildMetaSerializationTests() {
  QUnit.module("MetaSerializer", function () {
    QUnit.test("initializes", function (assert) {
      const serializer = new MetaSerializer();
      assert.ok(serializer !== undefined);
    });

    QUnit.module("serialize", function () {
      QUnit.test("handles empty array", function (assert) {
        const serializer = new MetaSerializer();
        const result = serializer.serialize([]);

        assert.ok(Array.isArray(result));
        assert.equal(result.length, 0);
      });

      QUnit.test("throws error for null input", function (assert) {
        const serializer = new MetaSerializer();

        assert.throws(() => {
          serializer.serialize(null);
        }, Error, "Should throw error for null input");
      });

      QUnit.test("throws error for non-array input", function (assert) {
        const serializer = new MetaSerializer();

        assert.throws(() => {
          serializer.serialize("not an array");
        }, Error, "Should throw error for non-array input");
      });

      QUnit.test("throws error for invalid metadata object", function (assert) {
        const serializer = new MetaSerializer();

        assert.throws(() => {
          serializer.serialize([{invalid: "object"}]);
        }, Error, "Should throw error for invalid metadata object");
      });

      QUnit.test("serializes single metadata object", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "HFC-134a", // substance
          "High Energy", // equipment
          "Domestic Refrigeration", // application
          "1430 kgCO2e / kg", // ghg
          true, // hasDomestic
          false, // hasImport
          false, // hasExport
          "500 kwh / unit", // energy
          "0.15 kg / unit", // initialChargeDomestic
          "0 kg / unit", // initialChargeImport
          "0 kg / unit", // initialChargeExport
          "10% / year", // retirement
        );

        const result = serializer.serialize([testMetadata]);

        assert.equal(result.length, 1);

        const rowMap = result[0];
        assert.ok(rowMap instanceof Map);

        // Check all columns are present with correct values
        assert.equal(rowMap.get("substance"), "HFC-134a");
        assert.equal(rowMap.get("equipment"), "High Energy");
        assert.equal(rowMap.get("application"), "Domestic Refrigeration");
        assert.equal(rowMap.get("ghg"), "1430 kgCO2e / kg");
        assert.equal(rowMap.get("hasDomestic"), "true");
        assert.equal(rowMap.get("hasImport"), "false");
        assert.equal(rowMap.get("hasExport"), "false");
        assert.equal(rowMap.get("energy"), "500 kwh / unit");
        assert.equal(rowMap.get("initialChargeDomestic"), "0.15 kg / unit");
        assert.equal(rowMap.get("initialChargeImport"), "0 kg / unit");
        assert.equal(rowMap.get("initialChargeExport"), "0 kg / unit");
        assert.equal(rowMap.get("retirement"), "10% / year");
        assert.equal(rowMap.get("key"), '"HFC-134a - High Energy" for "Domestic Refrigeration"');
      });

      QUnit.test("serializes multiple metadata objects", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata1 = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );
        const testMetadata2 = new SubstanceMetadata(
          "R-404A", "", "Commercial Refrigeration", "3922 kgCO2e / kg",
          false, true, false, "800 kwh / unit", "0 kg / unit", "0.25 kg / unit", "0 kg / unit", "8% / year",
        );

        const result = serializer.serialize([testMetadata1, testMetadata2]);

        assert.equal(result.length, 2);

        // Check first row
        const row1 = result[0];
        assert.equal(row1.get("substance"), "HFC-134a");
        assert.equal(row1.get("equipment"), "High Energy");
        assert.equal(row1.get("hasDomestic"), "true");
        assert.equal(row1.get("hasImport"), "false");

        // Check second row
        const row2 = result[1];
        assert.equal(row2.get("substance"), "R-404A");
        assert.equal(row2.get("equipment"), "");
        assert.equal(row2.get("hasDomestic"), "false");
        assert.equal(row2.get("hasImport"), "true");
      });

      QUnit.test("handles empty/null values correctly", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "HFC-134a", "", "Domestic Refrigeration", "",
          false, false, false, "", "", "", "", "",
        );

        const result = serializer.serialize([testMetadata]);
        const rowMap = result[0];

        assert.equal(rowMap.get("substance"), "HFC-134a");
        assert.equal(rowMap.get("equipment"), "");
        assert.equal(rowMap.get("ghg"), "");
        assert.equal(rowMap.get("energy"), "");
        assert.equal(rowMap.get("initialChargeDomestic"), "");
        assert.equal(rowMap.get("initialChargeImport"), "");
        assert.equal(rowMap.get("initialChargeExport"), "");
        assert.equal(rowMap.get("retirement"), "");
        assert.equal(rowMap.get("hasDomestic"), "false");
        assert.equal(rowMap.get("hasImport"), "false");
        assert.equal(rowMap.get("hasExport"), "false");
      });

      QUnit.test("maintains correct column order", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );

        const result = serializer.serialize([testMetadata]);
        const rowMap = result[0];

        // Convert Map to array to check order
        const keys = Array.from(rowMap.keys());
        const expectedOrder = [
          "substance", "equipment", "application", "ghg",
          "hasDomestic", "hasImport", "hasExport", "energy",
          "initialChargeDomestic", "initialChargeImport", "initialChargeExport",
          "retirement", "key",
        ];

        assert.deepEqual(keys, expectedOrder);
      });
    });

    QUnit.module("renderMetaToCsvUri", function () {
      QUnit.test("generates valid data URI format", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata]);

        assert.ok(typeof result === "string");
        assert.ok(result.startsWith("data:text/csv;charset=utf-8,"));
      });

      QUnit.test("generates CSV with correct header row", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "HFC-134a", "", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata]);

        // Decode the URI to check content
        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");

        // Check header row
        const expectedHeader = "substance,equipment,application,ghg,hasDomestic,hasImport,hasExport,energy,initialChargeDomestic,initialChargeImport,initialChargeExport,retirement,key";
        assert.equal(lines[0], expectedHeader);
      });

      QUnit.test("generates CSV with data rows", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "HFC-134a", "", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata]);

        // Decode the URI to check content
        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");

        assert.equal(lines.length, 2); // Header + 1 data row

        // Check data row contains expected values
        const dataRow = lines[1];
        assert.ok(dataRow.includes("HFC-134a"));
        assert.ok(dataRow.includes("Domestic Refrigeration"));
        assert.ok(dataRow.includes("1430 kgCO2e / kg"));
        assert.ok(dataRow.includes("true"));
        assert.ok(dataRow.includes("false"));
      });

      QUnit.test("handles empty array", function (assert) {
        const serializer = new MetaSerializer();
        const result = serializer.renderMetaToCsvUri([]);

        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");

        assert.equal(lines.length, 1); // Only header row
        assert.equal(lines[0], "substance,equipment,application,ghg,hasDomestic,hasImport,hasExport,energy,initialChargeDomestic,initialChargeImport,initialChargeExport,retirement,key");
      });

      QUnit.test("escapes special characters in CSV", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "Test,Substance", 'Equip"ment', "App\nName", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata]);

        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");
        const dataRow = lines[1];

        // Check that special characters are properly escaped
        assert.ok(dataRow.includes('"Test,Substance"')); // Comma escaped
        assert.ok(dataRow.includes('"Equip""ment"')); // Quote escaped by doubling
        // Note: Newlines in CSV cause the content to span multiple lines, so we check the full CSV
        assert.ok(csvContent.includes('"App\nName"')); // Newline escaped
      });

      QUnit.test("generates multiple rows correctly", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata1 = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );
        const testMetadata2 = new SubstanceMetadata(
          "R-404A", "", "Commercial Refrigeration", "3922 kgCO2e / kg",
          false, true, false, "800 kwh / unit", "0 kg / unit", "0.25 kg / unit", "0 kg / unit", "8% / year",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata1, testMetadata2]);

        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");

        assert.equal(lines.length, 3); // Header + 2 data rows

        // Check first data row
        assert.ok(lines[1].includes("HFC-134a"));
        assert.ok(lines[1].includes("High Energy"));

        // Check second data row
        assert.ok(lines[2].includes("R-404A"));
        assert.ok(lines[2].includes("Commercial Refrigeration"));
      });
    });

    QUnit.module("CSV escaping", function () {
      QUnit.test("handles values without special characters", function (assert) {
        const serializer = new MetaSerializer();

        // Test _escapeCsvValue method indirectly through public methods
        const testMetadata = new SubstanceMetadata(
          "SimpleValue", "NoSpecialChars", "PlainText", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata]);
        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");
        const dataRow = lines[1];

        // Values without special characters should not be quoted
        assert.ok(dataRow.includes("SimpleValue"));
        assert.ok(dataRow.includes("NoSpecialChars"));
        assert.ok(dataRow.includes("PlainText"));
      });

      QUnit.test("escapes commas correctly", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "Sub,stance", "Equip,ment", "App,Name", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata]);
        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");
        const dataRow = lines[1];

        assert.ok(dataRow.includes('"Sub,stance"'));
        assert.ok(dataRow.includes('"Equip,ment"'));
        assert.ok(dataRow.includes('"App,Name"'));
      });

      QUnit.test("escapes quotes correctly", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          'Sub"stance', 'Equip"ment', 'App"Name', "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata]);
        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");
        const dataRow = lines[1];

        assert.ok(dataRow.includes('"Sub""stance"'));
        assert.ok(dataRow.includes('"Equip""ment"'));
        assert.ok(dataRow.includes('"App""Name"'));
      });

      QUnit.test("escapes newlines correctly", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "Sub\nstance", "Equip\nment", "App\nName", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata]);
        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");

        // Note: Due to newlines in values, we need to parse more carefully
        // The escaped newlines should be within quotes
        assert.ok(csvContent.includes('"Sub\nstance"'));
        assert.ok(csvContent.includes('"Equip\nment"'));
        assert.ok(csvContent.includes('"App\nName"'));
      });

      QUnit.test("handles empty and null values", function (assert) {
        const serializer = new MetaSerializer();
        const testMetadata = new SubstanceMetadata(
          "HFC-134a", "", "Domestic Refrigeration", "",
          true, false, false, "", "", "", "", "",
        );

        const result = serializer.renderMetaToCsvUri([testMetadata]);
        const csvContent = decodeURIComponent(result.replace("data:text/csv;charset=utf-8,", ""));
        const lines = csvContent.split("\n");
        const dataRow = lines[1];

        // Empty values should be represented as empty fields
        const fields = dataRow.split(",");
        assert.equal(fields[1], ""); // equipment field should be empty
        assert.equal(fields[3], ""); // ghg field should be empty
      });
    });

    QUnit.module("deserialize", function () {
      QUnit.test("handles empty array", function (assert) {
        const serializer = new MetaSerializer();
        const result = serializer.deserialize([]);

        assert.ok(Array.isArray(result));
        assert.equal(result.length, 0);
      });

      QUnit.test("throws error for null input", function (assert) {
        const serializer = new MetaSerializer();

        assert.throws(() => {
          serializer.deserialize(null);
        }, Error, "Should throw error for null input");
      });

      QUnit.test("throws error for non-array input", function (assert) {
        const serializer = new MetaSerializer();

        assert.throws(() => {
          serializer.deserialize("not an array");
        }, Error, "Should throw error for non-array input");
      });

      QUnit.test("throws error for invalid Map object", function (assert) {
        const serializer = new MetaSerializer();

        assert.throws(() => {
          serializer.deserialize([{invalid: "object"}]);
        }, Error, "Should throw error for invalid Map object");
      });

      QUnit.test("deserializes single Map to SubstanceMetadata", function (assert) {
        const serializer = new MetaSerializer();
        const testMap = new Map();
        testMap.set("substance", "HFC-134a");
        testMap.set("equipment", "High Energy");
        testMap.set("application", "Domestic Refrigeration");
        testMap.set("ghg", "1430 kgCO2e / kg");
        testMap.set("hasDomestic", "true");
        testMap.set("hasImport", "false");
        testMap.set("hasExport", "false");
        testMap.set("energy", "500 kwh / unit");
        testMap.set("initialChargeDomestic", "0.15 kg / unit");
        testMap.set("initialChargeImport", "0 kg / unit");
        testMap.set("initialChargeExport", "0 kg / unit");
        testMap.set("retirement", "10% / year");
        testMap.set("key", "");

        const result = serializer.deserialize([testMap]);

        assert.equal(result.length, 1);
        
        const metadata = result[0];
        assert.ok(metadata instanceof SubstanceMetadata);
        assert.equal(metadata.getSubstance(), "HFC-134a");
        assert.equal(metadata.getEquipment(), "High Energy");
        assert.equal(metadata.getApplication(), "Domestic Refrigeration");
        assert.equal(metadata.getGhg(), "1430 kgCO2e / kg");
        assert.equal(metadata.getHasDomestic(), true);
        assert.equal(metadata.getHasImport(), false);
        assert.equal(metadata.getHasExport(), false);
        assert.equal(metadata.getEnergy(), "500 kwh / unit");
        assert.equal(metadata.getInitialChargeDomestic(), "0.15 kg / unit");
        assert.equal(metadata.getInitialChargeImport(), "0 kg / unit");
        assert.equal(metadata.getInitialChargeExport(), "0 kg / unit");
        assert.equal(metadata.getRetirement(), "10% / year");
      });

      QUnit.test("deserializes multiple Maps", function (assert) {
        const serializer = new MetaSerializer();
        
        const testMap1 = new Map();
        testMap1.set("substance", "HFC-134a");
        testMap1.set("equipment", "High Energy");
        testMap1.set("application", "Domestic Refrigeration");
        testMap1.set("hasDomestic", "true");
        testMap1.set("hasImport", "false");
        testMap1.set("hasExport", "false");

        const testMap2 = new Map();
        testMap2.set("substance", "R-404A");
        testMap2.set("equipment", "");
        testMap2.set("application", "Commercial Refrigeration");
        testMap2.set("hasDomestic", "false");
        testMap2.set("hasImport", "true");
        testMap2.set("hasExport", "false");

        const result = serializer.deserialize([testMap1, testMap2]);

        assert.equal(result.length, 2);
        
        assert.equal(result[0].getSubstance(), "HFC-134a");
        assert.equal(result[0].getEquipment(), "High Energy");
        assert.equal(result[0].getHasDomestic(), true);
        
        assert.equal(result[1].getSubstance(), "R-404A");
        assert.equal(result[1].getEquipment(), "");
        assert.equal(result[1].getHasImport(), true);
      });

      QUnit.test("handles missing Map values with defaults", function (assert) {
        const serializer = new MetaSerializer();
        const testMap = new Map();
        testMap.set("substance", "HFC-134a");
        testMap.set("application", "Domestic Refrigeration");
        // Intentionally leave out other fields

        const result = serializer.deserialize([testMap]);
        const metadata = result[0];

        assert.equal(metadata.getSubstance(), "HFC-134a");
        assert.equal(metadata.getEquipment(), "");
        assert.equal(metadata.getApplication(), "Domestic Refrigeration");
        assert.equal(metadata.getGhg(), "");
        assert.equal(metadata.getHasDomestic(), false);
        assert.equal(metadata.getHasImport(), false);
        assert.equal(metadata.getHasExport(), false);
        assert.equal(metadata.getEnergy(), "");
        assert.equal(metadata.getInitialChargeDomestic(), "");
        assert.equal(metadata.getInitialChargeImport(), "");
        assert.equal(metadata.getInitialChargeExport(), "");
        assert.equal(metadata.getRetirement(), "");
      });

      QUnit.test("parses boolean values correctly", function (assert) {
        const serializer = new MetaSerializer();
        const testMap = new Map();
        testMap.set("substance", "Test");
        testMap.set("application", "Test App");
        testMap.set("hasDomestic", "true");
        testMap.set("hasImport", "TRUE");
        testMap.set("hasExport", "false");

        const result = serializer.deserialize([testMap]);
        const metadata = result[0];

        assert.equal(metadata.getHasDomestic(), true);
        assert.equal(metadata.getHasImport(), true); // Case insensitive
        assert.equal(metadata.getHasExport(), false);
      });

      QUnit.test("handles edge case boolean values", function (assert) {
        const serializer = new MetaSerializer();
        const testMap = new Map();
        testMap.set("substance", "Test");
        testMap.set("application", "Test App");
        testMap.set("hasDomestic", "1");
        testMap.set("hasImport", "0");
        testMap.set("hasExport", "");

        const result = serializer.deserialize([testMap]);
        const metadata = result[0];

        assert.equal(metadata.getHasDomestic(), true); // "1" should be true
        assert.equal(metadata.getHasImport(), false); // "0" should be false
        assert.equal(metadata.getHasExport(), false); // Empty string should be false
      });
    });

    QUnit.module("deserializeMetaFromCsvString", function () {
      QUnit.test("handles empty string", function (assert) {
        const serializer = new MetaSerializer();
        const result = serializer.deserializeMetaFromCsvString("");

        assert.ok(Array.isArray(result));
        assert.equal(result.length, 0);
      });

      QUnit.test("throws error for non-string input", function (assert) {
        const serializer = new MetaSerializer();

        assert.throws(() => {
          serializer.deserializeMetaFromCsvString(null);
        }, Error, "Should throw error for non-string input");

        assert.throws(() => {
          serializer.deserializeMetaFromCsvString(123);
        }, Error, "Should throw error for numeric input");
      });

      // Note: These tests will only work if Papa Parse is loaded
      QUnit.test("handles CSV with header only", function (assert) {
        const serializer = new MetaSerializer();
        const csvString = "substance,equipment,application,ghg,hasDomestic,hasImport,hasExport,energy,initialChargeDomestic,initialChargeImport,initialChargeExport,retirement,key";
        
        // Skip test if Papa Parse is not available
        if (typeof Papa === "undefined") {
          assert.ok(true, "Skipping test - Papa Parse not available in test environment");
          return;
        }

        const result = serializer.deserializeMetaFromCsvString(csvString);
        assert.equal(result.length, 0);
      });

      QUnit.test("parses single row CSV correctly", function (assert) {
        const serializer = new MetaSerializer();
        const csvString = `substance,equipment,application,ghg,hasDomestic,hasImport,hasExport,energy,initialChargeDomestic,initialChargeImport,initialChargeExport,retirement,key
HFC-134a,High Energy,Domestic Refrigeration,1430 kgCO2e / kg,true,false,false,500 kwh / unit,0.15 kg / unit,0 kg / unit,0 kg / unit,10% / year,""`;

        // Skip test if Papa Parse is not available
        if (typeof Papa === "undefined") {
          assert.ok(true, "Skipping test - Papa Parse not available in test environment");
          return;
        }

        const result = serializer.deserializeMetaFromCsvString(csvString);
        
        assert.equal(result.length, 1);
        const metadata = result[0];
        
        assert.equal(metadata.getSubstance(), "HFC-134a");
        assert.equal(metadata.getEquipment(), "High Energy");
        assert.equal(metadata.getApplication(), "Domestic Refrigeration");
        assert.equal(metadata.getGhg(), "1430 kgCO2e / kg");
        assert.equal(metadata.getHasDomestic(), true);
        assert.equal(metadata.getHasImport(), false);
        assert.equal(metadata.getHasExport(), false);
        assert.equal(metadata.getEnergy(), "500 kwh / unit");
        assert.equal(metadata.getInitialChargeDomestic(), "0.15 kg / unit");
        assert.equal(metadata.getInitialChargeImport(), "0 kg / unit");
        assert.equal(metadata.getInitialChargeExport(), "0 kg / unit");
        assert.equal(metadata.getRetirement(), "10% / year");
      });

      QUnit.test("parses multiple row CSV correctly", function (assert) {
        const serializer = new MetaSerializer();
        const csvString = `substance,equipment,application,ghg,hasDomestic,hasImport,hasExport,energy,initialChargeDomestic,initialChargeImport,initialChargeExport,retirement,key
HFC-134a,High Energy,Domestic Refrigeration,1430 kgCO2e / kg,true,false,false,500 kwh / unit,0.15 kg / unit,0 kg / unit,0 kg / unit,10% / year,""
R-404A,,Commercial Refrigeration,3922 kgCO2e / kg,false,true,false,800 kwh / unit,0 kg / unit,0.25 kg / unit,0 kg / unit,8% / year,""`;

        // Skip test if Papa Parse is not available
        if (typeof Papa === "undefined") {
          assert.ok(true, "Skipping test - Papa Parse not available in test environment");
          return;
        }

        const result = serializer.deserializeMetaFromCsvString(csvString);
        
        assert.equal(result.length, 2);
        
        assert.equal(result[0].getSubstance(), "HFC-134a");
        assert.equal(result[0].getEquipment(), "High Energy");
        assert.equal(result[0].getHasDomestic(), true);
        
        assert.equal(result[1].getSubstance(), "R-404A");
        assert.equal(result[1].getEquipment(), "");
        assert.equal(result[1].getHasImport(), true);
      });

      QUnit.test("handles CSV with special characters", function (assert) {
        const serializer = new MetaSerializer();
        const csvString = `substance,equipment,application,ghg,hasDomestic,hasImport,hasExport,energy,initialChargeDomestic,initialChargeImport,initialChargeExport,retirement,key
"Test,Substance","Equip""ment","App
Name",1430 kgCO2e / kg,true,false,false,500 kwh / unit,0.15 kg / unit,0 kg / unit,0 kg / unit,10% / year,""`;

        // Skip test if Papa Parse is not available
        if (typeof Papa === "undefined") {
          assert.ok(true, "Skipping test - Papa Parse not available in test environment");
          return;
        }

        const result = serializer.deserializeMetaFromCsvString(csvString);
        
        assert.equal(result.length, 1);
        const metadata = result[0];
        
        assert.equal(metadata.getSubstance(), "Test,Substance");
        assert.equal(metadata.getEquipment(), 'Equip"ment');
        assert.equal(metadata.getApplication(), "App\nName");
      });

      QUnit.test("throws error for missing required columns", function (assert) {
        const serializer = new MetaSerializer();
        const csvString = `equipment,ghg,hasDomestic
High Energy,1430 kgCO2e / kg,true`;

        // Skip test if Papa Parse is not available
        if (typeof Papa === "undefined") {
          assert.ok(true, "Skipping test - Papa Parse not available in test environment");
          return;
        }

        assert.throws(() => {
          serializer.deserializeMetaFromCsvString(csvString);
        }, Error, "Should throw error for missing required columns");
      });

      QUnit.test("round-trip compatibility", function (assert) {
        const serializer = new MetaSerializer();
        
        // Create test metadata
        const originalMetadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );

        // Skip test if Papa Parse is not available
        if (typeof Papa === "undefined") {
          assert.ok(true, "Skipping test - Papa Parse not available in test environment");
          return;
        }

        // Serialize to CSV URI and extract CSV content
        const csvUri = serializer.renderMetaToCsvUri([originalMetadata]);
        const csvContent = decodeURIComponent(csvUri.replace("data:text/csv;charset=utf-8,", ""));

        // Deserialize back to metadata
        const deserializedMetadata = serializer.deserializeMetaFromCsvString(csvContent);

        assert.equal(deserializedMetadata.length, 1);
        const roundTripMetadata = deserializedMetadata[0];

        // Verify all fields match
        assert.equal(roundTripMetadata.getSubstance(), originalMetadata.getSubstance());
        assert.equal(roundTripMetadata.getEquipment(), originalMetadata.getEquipment());
        assert.equal(roundTripMetadata.getApplication(), originalMetadata.getApplication());
        assert.equal(roundTripMetadata.getGhg(), originalMetadata.getGhg());
        assert.equal(roundTripMetadata.getHasDomestic(), originalMetadata.getHasDomestic());
        assert.equal(roundTripMetadata.getHasImport(), originalMetadata.getHasImport());
        assert.equal(roundTripMetadata.getHasExport(), originalMetadata.getHasExport());
        assert.equal(roundTripMetadata.getEnergy(), originalMetadata.getEnergy());
        assert.equal(roundTripMetadata.getInitialChargeDomestic(), originalMetadata.getInitialChargeDomestic());
        assert.equal(roundTripMetadata.getInitialChargeImport(), originalMetadata.getInitialChargeImport());
        assert.equal(roundTripMetadata.getInitialChargeExport(), originalMetadata.getInitialChargeExport());
        assert.equal(roundTripMetadata.getRetirement(), originalMetadata.getRetirement());
      });
    });

    QUnit.module("Boolean parsing helper", function () {
      QUnit.test("parses true values correctly", function (assert) {
        const serializer = new MetaSerializer();
        
        // Test via deserialize method since _parseBoolean is private
        const testCases = ["true", "TRUE", "True", "1"];
        
        for (const testValue of testCases) {
          const testMap = new Map();
          testMap.set("substance", "Test");
          testMap.set("application", "Test App");
          testMap.set("hasDomestic", testValue);
          
          const result = serializer.deserialize([testMap]);
          assert.equal(result[0].getHasDomestic(), true, `"${testValue}" should parse to true`);
        }
      });

      QUnit.test("parses false values correctly", function (assert) {
        const serializer = new MetaSerializer();
        
        // Test via deserialize method since _parseBoolean is private
        const testCases = ["false", "FALSE", "False", "0", "", "invalid", null, undefined];
        
        for (const testValue of testCases) {
          const testMap = new Map();
          testMap.set("substance", "Test");
          testMap.set("application", "Test App");
          testMap.set("hasDomestic", testValue);
          
          const result = serializer.deserialize([testMap]);
          assert.equal(result[0].getHasDomestic(), false, `"${testValue}" should parse to false`);
        }
      });
    });
  });

  QUnit.module("MetaChangeApplier", function () {
    QUnit.test("constructor validates Program instance", function (assert) {
      assert.throws(() => {
        new MetaChangeApplier(null);
      }, Error, "Should throw error for null program");

      assert.throws(() => {
        new MetaChangeApplier("not a program");
      }, Error, "Should throw error for invalid program");

      assert.throws(() => {
        new MetaChangeApplier({invalid: "object"});
      }, Error, "Should throw error for object without getApplications method");
    });

    QUnit.test("constructor accepts valid Program instance", function (assert) {
      const program = new Program([], [], [], true);
      const applier = new MetaChangeApplier(program);
      
      assert.ok(applier instanceof MetaChangeApplier);
    });

    QUnit.module("upsertMetadata", function () {
      QUnit.test("validates input array", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        assert.throws(() => {
          applier.upsertMetadata(null);
        }, Error, "Should throw error for null input");

        assert.throws(() => {
          applier.upsertMetadata("not an array");
        }, Error, "Should throw error for non-array input");

        assert.throws(() => {
          applier.upsertMetadata([{invalid: "object"}]);
        }, Error, "Should throw error for invalid metadata objects");
      });

      QUnit.test("handles empty array", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const result = applier.upsertMetadata([]);
        
        assert.equal(result, program, "Should return the same program instance");
        assert.equal(program.getApplications().length, 0, "No applications should be added for empty array");
      });

      QUnit.test("creates missing applications", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );

        applier.upsertMetadata([metadata]);

        assert.equal(program.getApplications().length, 1, "Should create one application");
        const app = program.getApplication("Domestic Refrigeration");
        assert.ok(app !== null, "Application should exist");
        assert.equal(app.getName(), "Domestic Refrigeration", "Application name should match");
      });

      QUnit.test("preserves existing applications", function (assert) {
        const existingApp = new Application("Existing App", [], false, true);
        const program = new Program([existingApp], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "New App", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );

        applier.upsertMetadata([metadata]);

        assert.equal(program.getApplications().length, 2, "Should have both existing and new applications");
        assert.ok(program.getApplication("Existing App") !== null, "Existing application should remain");
        assert.ok(program.getApplication("New App") !== null, "New application should be created");
      });

      QUnit.test("adds substances to applications", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );

        applier.upsertMetadata([metadata]);

        const app = program.getApplication("Domestic Refrigeration");
        const substances = app.getSubstances();
        assert.equal(substances.length, 1, "Should add one substance");
        
        const substance = substances[0];
        assert.equal(substance.getName(), "HFC-134a - High Energy", "Substance name should match metadata");
      });

      QUnit.test("handles multiple metadata objects", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata1 = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );
        
        const metadata2 = new SubstanceMetadata(
          "R-404A", "", "Commercial Refrigeration", "3922 kgCO2e / kg",
          false, true, false, "800 kwh / unit", "0 kg / unit", "0.25 kg / unit", "0 kg / unit", "8% / year"
        );

        applier.upsertMetadata([metadata1, metadata2]);

        assert.equal(program.getApplications().length, 2, "Should create two applications");
        
        const domesticApp = program.getApplication("Domestic Refrigeration");
        const commercialApp = program.getApplication("Commercial Refrigeration");
        
        assert.ok(domesticApp !== null, "Domestic Refrigeration app should exist");
        assert.ok(commercialApp !== null, "Commercial Refrigeration app should exist");
        
        assert.equal(domesticApp.getSubstances().length, 1, "Domestic app should have one substance");
        assert.equal(commercialApp.getSubstances().length, 1, "Commercial app should have one substance");
      });

      QUnit.test("skips substances with empty required fields", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata1 = new SubstanceMetadata(
          "", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );
        
        const metadata2 = new SubstanceMetadata(
          "HFC-134a", "High Energy", "", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );

        const metadata3 = new SubstanceMetadata(
          "R-404A", "", "Commercial Refrigeration", "3922 kgCO2e / kg",
          false, true, false, "800 kwh / unit", "0 kg / unit", "0.25 kg / unit", "0 kg / unit", "8% / year"
        );

        applier.upsertMetadata([metadata1, metadata2, metadata3]);

        // Only metadata3 should be processed since metadata1 has empty substance and metadata2 has empty application
        assert.equal(program.getApplications().length, 1, "Should only create one application");
        
        const commercialApp = program.getApplication("Commercial Refrigeration");
        assert.ok(commercialApp !== null, "Commercial Refrigeration app should exist");
        assert.equal(commercialApp.getSubstances().length, 1, "Should have one valid substance");
      });

      QUnit.test("skips substances with name conflicts", function (assert) {
        const existingBuilder = new SubstanceBuilder("HFC-134a - High Energy", false);
        const existingSubstance = existingBuilder.build(true);
        const existingApp = new Application("Domestic Refrigeration", [existingSubstance], false, true);
        const program = new Program([existingApp], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );

        applier.upsertMetadata([metadata]);

        const app = program.getApplication("Domestic Refrigeration");
        const substances = app.getSubstances();
        assert.equal(substances.length, 1, "Should still have only the original substance");
        assert.equal(substances[0], existingSubstance, "Should be the same existing substance instance");
      });

      QUnit.test("processes substances in different applications despite conflicts", function (assert) {
        const existingBuilder = new SubstanceBuilder("HFC-134a - High Energy", false);
        const existingSubstance = existingBuilder.build(true);
        const existingApp = new Application("Domestic Refrigeration", [existingSubstance], false, true);
        const program = new Program([existingApp], [], [], true);
        const applier = new MetaChangeApplier(program);

        const conflictingMetadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );
        
        const newMetadata = new SubstanceMetadata(
          "R-404A", "", "Commercial Refrigeration", "3922 kgCO2e / kg",
          false, true, false, "800 kwh / unit", "0 kg / unit", "0.25 kg / unit", "0 kg / unit", "8% / year"
        );

        applier.upsertMetadata([conflictingMetadata, newMetadata]);

        assert.equal(program.getApplications().length, 2, "Should have both applications");
        
        const domesticApp = program.getApplication("Domestic Refrigeration");
        const commercialApp = program.getApplication("Commercial Refrigeration");
        
        assert.equal(domesticApp.getSubstances().length, 1, "Domestic app should still have only original substance");
        assert.equal(commercialApp.getSubstances().length, 1, "Commercial app should have new substance");
      });

      QUnit.test("creates substances with correct properties", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );

        applier.upsertMetadata([metadata]);

        const app = program.getApplication("Domestic Refrigeration");
        const substance = app.getSubstances()[0];
        
        assert.equal(substance.getName(), "HFC-134a - High Energy", "Name should match");
        
        // Check that substance has appropriate commands
        const ghgCommand = substance.getEqualsGhg();
        const energyCommand = substance.getEqualsKwh();
        const retireCommand = substance.getRetire();
        const enableCommands = substance.getEnables();
        const chargeCommands = substance.getInitialCharges();
        
        assert.ok(ghgCommand !== null, "Should have GHG command");
        assert.equal(ghgCommand.getValue().getValue(), 1430, "GHG value should be 1430");
        assert.equal(ghgCommand.getValue().getUnits(), "kgCO2e / kg", "GHG units should match");
        
        assert.ok(energyCommand !== null, "Should have energy command");
        assert.equal(energyCommand.getValue().getValue(), 500, "Energy value should be 500");
        assert.equal(energyCommand.getValue().getUnits(), "kwh / unit", "Energy units should match");
        
        assert.ok(retireCommand !== null, "Should have retire command");
        assert.equal(retireCommand.getValue().getValue(), 10, "Retire value should be 10");
        assert.equal(retireCommand.getValue().getUnits(), "% / year", "Retire units should match");
        
        assert.equal(enableCommands.length, 1, "Should have one enable command");
        assert.equal(enableCommands[0].getTarget(), "domestic", "Should enable domestic");
        
        assert.equal(chargeCommands.length, 1, "Should have one charge command");
        assert.equal(chargeCommands[0].getTarget(), "domestic", "Should have domestic charge");
        assert.equal(chargeCommands[0].getValue().getValue(), 0.15, "Domestic charge should be 0.15");
        assert.equal(chargeCommands[0].getValue().getUnits(), "kg / unit", "Domestic charge units should match");
      });

      QUnit.test("handles metadata with multiple streams enabled", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "R-404A", "", "Commercial Refrigeration", "3922 kgCO2e / kg",
          true, true, true, "800 kwh / unit", "0.10 kg / unit", "0.25 kg / unit", "0.30 kg / unit", "8% / year"
        );

        applier.upsertMetadata([metadata]);

        const app = program.getApplication("Commercial Refrigeration");
        const substance = app.getSubstances()[0];
        
        const enableCommands = substance.getEnables();
        const chargeCommands = substance.getInitialCharges();
        
        assert.equal(enableCommands.length, 3, "Should have three enable commands");
        
        const enableTargets = enableCommands.map(cmd => cmd.getTarget()).sort();
        assert.deepEqual(enableTargets, ["domestic", "export", "import"], "Should enable all three streams");
        
        assert.equal(chargeCommands.length, 3, "Should have three charge commands");
        
        const chargeTargets = chargeCommands.map(cmd => cmd.getTarget()).sort();
        assert.deepEqual(chargeTargets, ["domestic", "export", "import"], "Should have charges for all three streams");
        
        // Check specific charge values
        const domesticCharge = chargeCommands.find(cmd => cmd.getTarget() === "domestic");
        const importCharge = chargeCommands.find(cmd => cmd.getTarget() === "import");
        const exportCharge = chargeCommands.find(cmd => cmd.getTarget() === "export");
        
        assert.equal(domesticCharge.getValue().getValue(), 0.10, "Domestic charge should be 0.10");
        assert.equal(importCharge.getValue().getValue(), 0.25, "Import charge should be 0.25");
        assert.equal(exportCharge.getValue().getValue(), 0.30, "Export charge should be 0.30");
      });

      QUnit.test("handles metadata with empty optional fields", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "",
          false, false, false, "", "", "", "", ""
        );

        applier.upsertMetadata([metadata]);

        const app = program.getApplication("Domestic Refrigeration");
        const substance = app.getSubstances()[0];
        
        assert.equal(substance.getName(), "HFC-134a - High Energy", "Name should still work");
        
        // Check that no commands are created for empty fields
        assert.equal(substance.getEqualsGhg(), null, "Should have no GHG command for empty field");
        assert.equal(substance.getEqualsKwh(), null, "Should have no energy command for empty field");
        assert.equal(substance.getRetire(), null, "Should have no retire command for empty field");
        assert.equal(substance.getEnables().length, 0, "Should have no enable commands");
        assert.equal(substance.getInitialCharges().length, 0, "Should have no charge commands");
      });

      QUnit.test("returns program instance for method chaining", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "HFC-134a", "High Energy", "Domestic Refrigeration", "1430 kgCO2e / kg",
          true, false, false, "500 kwh / unit", "0.15 kg / unit", "0 kg / unit", "0 kg / unit", "10% / year"
        );

        const result = applier.upsertMetadata([metadata]);
        
        assert.equal(result, program, "Should return the same program instance for chaining");
      });
    });

    QUnit.module("Unit value parsing", function () {
      QUnit.test("parses various unit formats correctly", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        // Test different unit formats
        const testCases = [
          {
            metadata: new SubstanceMetadata(
              "Test1", "", "Test App 1", "1430 kgCO2e / kg",
              false, false, false, "500 kwh / unit", "", "", "", "10% / year"
            ),
            expectedGhg: {value: 1430, units: "kgCO2e / kg"},
            expectedEnergy: {value: 500, units: "kwh / unit"},
            expectedRetire: {value: 10, units: "% / year"}
          },
          {
            metadata: new SubstanceMetadata(
              "Test2", "", "Test App 2", "0.5 tCO2e / mt",
              false, false, false, "1.5 mwh / unit", "", "", "", "5.5% / year"
            ),
            expectedGhg: {value: 0.5, units: "tCO2e / mt"},
            expectedEnergy: {value: 1.5, units: "mwh / unit"},
            expectedRetire: {value: 5.5, units: "% / year"}
          },
          {
            metadata: new SubstanceMetadata(
              "Test3", "", "Test App 3", "1,500 kgCO2e / kg",
              false, false, false, "2,000.5 kwh / unit", "", "", "", "12.3% / year"
            ),
            expectedGhg: {value: 1500, units: "kgCO2e / kg"}, // Comma removed
            expectedEnergy: {value: 2000.5, units: "kwh / unit"}, // Comma removed
            expectedRetire: {value: 12.3, units: "% / year"}
          }
        ];

        for (let i = 0; i < testCases.length; i++) {
          const testCase = testCases[i];
          applier.upsertMetadata([testCase.metadata]);

          const app = program.getApplication(`Test App ${i+1}`);
          const substances = app.getSubstances();
          const substance = substances[0];

          if (testCase.expectedGhg) {
            const ghgCommand = substance.getEqualsGhg();
            assert.ok(ghgCommand !== null, `Test ${i+1}: Should have GHG command`);
            assert.equal(ghgCommand.getValue().getValue(), testCase.expectedGhg.value, `Test ${i+1}: GHG value should match`);
            assert.equal(ghgCommand.getValue().getUnits(), testCase.expectedGhg.units, `Test ${i+1}: GHG units should match`);
          }

          if (testCase.expectedEnergy) {
            const energyCommand = substance.getEqualsKwh();
            assert.ok(energyCommand !== null, `Test ${i+1}: Should have energy command`);
            assert.equal(energyCommand.getValue().getValue(), testCase.expectedEnergy.value, `Test ${i+1}: Energy value should match`);
            assert.equal(energyCommand.getValue().getUnits(), testCase.expectedEnergy.units, `Test ${i+1}: Energy units should match`);
          }

          if (testCase.expectedRetire) {
            const retireCommand = substance.getRetire();
            assert.ok(retireCommand !== null, `Test ${i+1}: Should have retire command`);
            assert.equal(retireCommand.getValue().getValue(), testCase.expectedRetire.value, `Test ${i+1}: Retire value should match`);
            assert.equal(retireCommand.getValue().getUnits(), testCase.expectedRetire.units, `Test ${i+1}: Retire units should match`);
          }
        }
      });

      QUnit.test("handles malformed unit values gracefully", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "Test", "", "Test App", "invalid-value",
          false, false, false, "no-units", "", "", "", "not-a-number"
        );

        // Should not throw error, just skip creating commands for malformed values
        applier.upsertMetadata([metadata]);

        const app = program.getApplication("Test App");
        const substance = app.getSubstances()[0];
        
        assert.equal(substance.getEqualsGhg(), null, "Should not create GHG command for invalid value");
        assert.equal(substance.getEqualsKwh(), null, "Should not create energy command for invalid value");
        assert.equal(substance.getRetire(), null, "Should not create retire command for invalid value");
      });

      QUnit.test("handles charge values correctly", function (assert) {
        const program = new Program([], [], [], true);
        const applier = new MetaChangeApplier(program);

        const metadata = new SubstanceMetadata(
          "Test", "", "Test App", "",
          true, true, true, "", "0.15 kg / unit", "0.25 kg / unit", "0.35 kg / unit", ""
        );

        applier.upsertMetadata([metadata]);

        const app = program.getApplication("Test App");
        const substance = app.getSubstances()[0];
        const chargeCommands = substance.getInitialCharges();
        
        assert.equal(chargeCommands.length, 3, "Should have three charge commands");
        
        const domesticCharge = chargeCommands.find(cmd => cmd.getTarget() === "domestic");
        const importCharge = chargeCommands.find(cmd => cmd.getTarget() === "import");
        const exportCharge = chargeCommands.find(cmd => cmd.getTarget() === "export");
        
        assert.ok(domesticCharge !== null, "Should have domestic charge");
        assert.equal(domesticCharge.getValue().getValue(), 0.15, "Domestic charge value should be 0.15");
        assert.equal(domesticCharge.getValue().getUnits(), "kg / unit", "Domestic charge units should match");
        
        assert.ok(importCharge !== null, "Should have import charge");
        assert.equal(importCharge.getValue().getValue(), 0.25, "Import charge value should be 0.25");
        
        assert.ok(exportCharge !== null, "Should have export charge");
        assert.equal(exportCharge.getValue().getValue(), 0.35, "Export charge value should be 0.35");
      });
    });
  });
}

export {
  buildMetaSerializationTests,
};
