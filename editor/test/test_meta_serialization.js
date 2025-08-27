import {MetaSerializer} from "meta_serialization";
import {SubstanceMetadata} from "ui_translator";

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
  });
}

export {
  buildMetaSerializationTests,
};
