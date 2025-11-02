import {SubstanceMetadata, SubstanceMetadataBuilder} from "ui_translator_components";

function buildSubstanceMetadataTests() {
  QUnit.module("SubstanceMetadata", function () {
    QUnit.test("creates with basic properties", function (assert) {
      const metadata = new SubstanceMetadata(
        "HFC-134a",
        "High Energy",
        "Domestic Refrigeration",
        "1430 kgCO2e / kg",
        true,
        false,
        false,
        "500 kwh / unit",
        "0.15 kg / unit",
        "0 kg / unit",
        "0 kg / unit",
        "10% / year",
      );

      assert.equal(metadata.getSubstance(), "HFC-134a", "substance name should match");
      assert.equal(metadata.getEquipment(), "High Energy", "equipment should match");
      assert.equal(metadata.getApplication(), "Domestic Refrigeration", "application should match");
      assert.equal(metadata.getGhg(), "1430 kgCO2e / kg", "GHG value should match");
      assert.equal(metadata.getHasDomestic(), true, "domestic should be enabled");
      assert.equal(metadata.getHasImport(), false, "import should be disabled");
      assert.equal(metadata.getHasExport(), false, "export should be disabled");
      assert.equal(metadata.getEnergy(), "500 kwh / unit", "energy value should match");
      assert.equal(
        metadata.getInitialChargeDomestic(),
        "0.15 kg / unit",
        "domestic charge should match",
      );
      assert.equal(metadata.getInitialChargeImport(), "0 kg / unit", "import charge should match");
      assert.equal(metadata.getInitialChargeExport(), "0 kg / unit", "export charge should match");
      assert.equal(metadata.getRetirement(), "10% / year", "retirement should match");
    });

    QUnit.test("handles empty values gracefully", function (assert) {
      const metadata = new SubstanceMetadata();

      assert.equal(metadata.getSubstance(), "", "empty substance should default to empty string");
      assert.equal(metadata.getEquipment(), "", "empty equipment should default to empty string");
      assert.equal(
        metadata.getApplication(),
        "",
        "empty application should default to empty string",
      );
      assert.equal(metadata.getGhg(), "", "empty GHG should default to empty string");
      assert.equal(metadata.getHasDomestic(), false, "domestic should default to false");
      assert.equal(metadata.getHasImport(), false, "import should default to false");
      assert.equal(metadata.getHasExport(), false, "export should default to false");
      assert.equal(metadata.getEnergy(), "", "empty energy should default to empty string");
      assert.equal(
        metadata.getInitialChargeDomestic(),
        "",
        "domestic charge should default to empty string",
      );
      assert.equal(
        metadata.getInitialChargeImport(),
        "",
        "import charge should default to empty string",
      );
      assert.equal(
        metadata.getInitialChargeExport(),
        "",
        "export charge should default to empty string",
      );
      assert.equal(metadata.getRetirement(), "", "retirement should default to empty string");
    });

    QUnit.test("getName() combines substance and equipment properly", function (assert) {
      const metadataWithEquipment = new SubstanceMetadata(
        "HFC-134a",
        "High Energy",
        "Domestic Refrigeration",
      );

      const metadataWithoutEquipment = new SubstanceMetadata(
        "HFC-134a",
        "",
        "Domestic Refrigeration",
      );

      const metadataWithNullEquipment = new SubstanceMetadata(
        "HFC-134a",
        null,
        "Domestic Refrigeration",
      );

      assert.equal(metadataWithEquipment.getName(), "HFC-134a - High Energy",
        "should combine substance and equipment with dash separator");
      assert.equal(metadataWithoutEquipment.getName(), "HFC-134a",
        "should return just substance name when equipment is empty");
      assert.equal(metadataWithNullEquipment.getName(), "HFC-134a",
        "should return just substance name when equipment is null");
    });

    QUnit.test("getKey() generates correct CSV key format", function (assert) {
      const metadata = new SubstanceMetadata(
        "HFC-134a",
        "High Energy",
        "Domestic Refrigeration",
      );

      const expectedKey = '"HFC-134a - High Energy" for "Domestic Refrigeration"';
      assert.equal(metadata.getKey(), expectedKey, "should generate correct CSV key format");
    });

    QUnit.test("getKey() handles substance without equipment", function (assert) {
      const metadata = new SubstanceMetadata(
        "HFC-134a",
        "",
        "Domestic Refrigeration",
      );

      const expectedKey = '"HFC-134a" for "Domestic Refrigeration"';
      assert.equal(
        metadata.getKey(),
        expectedKey,
        "should generate correct CSV key without equipment",
      );
    });
  });

  QUnit.module("SubstanceMetadataBuilder", function () {
    QUnit.test("creates with required values", function (assert) {
      const builder = new SubstanceMetadataBuilder();
      builder.setSubstance("TestSubstance");
      builder.setApplication("TestApp");
      const metadata = builder.build();

      assert.equal(metadata.getSubstance(), "TestSubstance", "substance should match");
      assert.equal(metadata.getEquipment(), "", "equipment should default to empty");
      assert.equal(metadata.getApplication(), "TestApp", "application should match");
      assert.equal(metadata.getGhg(), "", "GHG should default to empty");
      assert.equal(metadata.getHasDomestic(), false, "domestic should default to false");
      assert.equal(metadata.getHasImport(), false, "import should default to false");
      assert.equal(metadata.getHasExport(), false, "export should default to false");
      assert.equal(metadata.getEnergy(), "", "energy should default to empty");
      assert.equal(
        metadata.getInitialChargeDomestic(),
        "",
        "domestic charge should default to empty",
      );
      assert.equal(metadata.getInitialChargeImport(), "", "import charge should default to empty");
      assert.equal(metadata.getInitialChargeExport(), "", "export charge should default to empty");
      assert.equal(metadata.getRetirement(), "", "retirement should default to empty");
    });

    QUnit.test("supports fluent interface", function (assert) {
      const builder = new SubstanceMetadataBuilder();

      const result = builder
        .setSubstance("HFC-134a")
        .setEquipment("High Energy")
        .setApplication("Domestic Refrigeration");

      assert.strictEqual(result, builder, "setter should return builder instance for chaining");
    });

    QUnit.test("builds complete metadata with all properties", function (assert) {
      const metadata = new SubstanceMetadataBuilder()
        .setSubstance("HFC-134a")
        .setEquipment("High Energy")
        .setApplication("Domestic Refrigeration")
        .setGhg("1430 kgCO2e / kg")
        .setHasDomestic(true)
        .setHasImport(false)
        .setHasExport(true)
        .setEnergy("500 kwh / unit")
        .setInitialChargeDomestic("0.15 kg / unit")
        .setInitialChargeImport("0.30 kg / unit")
        .setInitialChargeExport("0.10 kg / unit")
        .setRetirement("10% / year")
        .build();

      assert.equal(metadata.getSubstance(), "HFC-134a");
      assert.equal(metadata.getEquipment(), "High Energy");
      assert.equal(metadata.getApplication(), "Domestic Refrigeration");
      assert.equal(metadata.getGhg(), "1430 kgCO2e / kg");
      assert.equal(metadata.getHasDomestic(), true);
      assert.equal(metadata.getHasImport(), false);
      assert.equal(metadata.getHasExport(), true);
      assert.equal(metadata.getEnergy(), "500 kwh / unit");
      assert.equal(metadata.getInitialChargeDomestic(), "0.15 kg / unit");
      assert.equal(metadata.getInitialChargeImport(), "0.30 kg / unit");
      assert.equal(metadata.getInitialChargeExport(), "0.10 kg / unit");
      assert.equal(metadata.getRetirement(), "10% / year");
    });

    QUnit.test("validates required fields and handles optional nulls", function (assert) {
      // Test that required fields validation works
      try {
        new SubstanceMetadataBuilder().build();
        assert.ok(false, "Should have thrown error for missing required fields");
      } catch (e) {
        assert.ok(e.message.includes("Substance name is required"), "Should throw substance error");
      }

      // Test with valid required fields but null optional fields
      const metadata = new SubstanceMetadataBuilder()
        .setSubstance("TestSub")
        .setEquipment(undefined)
        .setApplication("TestApp")
        .setGhg(null)
        .setHasDomestic(null)
        .setHasImport(undefined)
        .setHasExport(false)
        .setEnergy(null)
        .setInitialChargeDomestic(undefined)
        .setInitialChargeImport("")
        .setInitialChargeExport(null)
        .setRetirement(undefined)
        .build();

      assert.equal(metadata.getSubstance(), "TestSub", "substance should be preserved");
      assert.equal(metadata.getEquipment(), "", "undefined equipment should become empty string");
      assert.equal(metadata.getApplication(), "TestApp", "application should be preserved");
      assert.equal(metadata.getGhg(), "", "null GHG should become empty string");
      assert.equal(metadata.getHasDomestic(), false, "null domestic should become false");
      assert.equal(metadata.getHasImport(), false, "undefined import should become false");
      assert.equal(metadata.getHasExport(), false, "explicit false should remain false");
      assert.equal(metadata.getEnergy(), "", "null energy should become empty string");
      assert.equal(
        metadata.getInitialChargeDomestic(),
        "",
        "undefined domestic charge should become empty string",
      );
      assert.equal(
        metadata.getInitialChargeImport(),
        "",
        "empty import charge should remain empty string",
      );
      assert.equal(
        metadata.getInitialChargeExport(),
        "",
        "null export charge should become empty string",
      );
      assert.equal(metadata.getRetirement(), "", "undefined retirement should become empty string");
    });

    QUnit.test("can build multiple instances from same builder", function (assert) {
      const builder = new SubstanceMetadataBuilder()
        .setSubstance("HFC-134a")
        .setApplication("Domestic Refrigeration");

      const metadata1 = builder.build();
      const metadata2 = builder.build();

      assert.notStrictEqual(metadata1, metadata2, "should create distinct instances");
      assert.equal(
        metadata1.getSubstance(),
        metadata2.getSubstance(),
        "instances should have same content",
      );
      assert.equal(
        metadata1.getApplication(),
        metadata2.getApplication(),
        "instances should have same content",
      );
    });

    QUnit.test("builder modifications affect subsequent builds", function (assert) {
      const builder = new SubstanceMetadataBuilder()
        .setSubstance("HFC-134a")
        .setApplication("TestApp");

      const metadata1 = builder.build();

      builder.setEquipment("High Energy");
      const metadata2 = builder.build();

      assert.equal(metadata1.getEquipment(), "", "first build should not have equipment");
      assert.equal(metadata2.getEquipment(), "High Energy", "second build should have equipment");
      assert.equal(
        metadata1.getSubstance(),
        metadata2.getSubstance(),
        "substance should be same in both",
      );
    });
  });

  QUnit.module("SubstanceMetadata Integration", function () {
    QUnit.test("getName() matches getEffectiveName logic from ui_editor", function (assert) {
      // Test the same logic that getEffectiveName uses in ui_editor.js

      // Case 1: Both substance and equipment present
      const metadata1 = new SubstanceMetadata("HFC-134a", "High Energy", "App");
      assert.equal(metadata1.getName(), "HFC-134a - High Energy",
        "should match getEffectiveName with both parts");

      // Case 2: Only substance present
      const metadata2 = new SubstanceMetadata("HFC-134a", "", "App");
      assert.equal(metadata2.getName(), "HFC-134a",
        "should match getEffectiveName with only substance");

      // Case 3: Equipment with only whitespace
      const metadata3 = new SubstanceMetadata("HFC-134a", "   ", "App");
      assert.equal(metadata3.getName(), "HFC-134a",
        "should treat whitespace-only equipment as empty");
    });

    QUnit.test("getKey() matches ConsumptionListPresenter format", function (assert) {
      // Test that the key format matches what ConsumptionListPresenter
      // ._getConsumptionNames generates
      const metadata = new SubstanceMetadata("HFC-134a", "High Energy", "Domestic Refrigeration");
      const expectedFormat = '"HFC-134a - High Energy" for "Domestic Refrigeration"';

      assert.equal(metadata.getKey(), expectedFormat,
        "should match the format used by ConsumptionListPresenter");
    });

    QUnit.test("CSV column mapping completeness", function (assert) {
      // Ensure all CSV columns from the task specification are covered
      const metadata = new SubstanceMetadata(
        "substance_val", // substance
        "equipment_val", // equipment
        "application_val", // application
        "ghg_val", // ghg
        true, // hasDomestic
        false, // hasImport
        true, // hasExport
        "energy_val", // energy
        "domestic_charge", // initialChargeDomestic
        "import_charge", // initialChargeImport
        "export_charge", // initialChargeExport
        "retirement_val", // retirement
      );

      // Verify all CSV columns are accessible
      assert.equal(metadata.getSubstance(), "substance_val", "substance column accessible");
      assert.equal(metadata.getEquipment(), "equipment_val", "equipment column accessible");
      assert.equal(metadata.getApplication(), "application_val", "application column accessible");
      assert.equal(metadata.getGhg(), "ghg_val", "ghg column accessible");
      assert.equal(metadata.getHasDomestic(), true, "hasDomestic column accessible");
      assert.equal(metadata.getHasImport(), false, "hasImport column accessible");
      assert.equal(metadata.getHasExport(), true, "hasExport column accessible");
      assert.equal(metadata.getEnergy(), "energy_val", "energy column accessible");
      assert.equal(
        metadata.getInitialChargeDomestic(),
        "domestic_charge",
        "initialChargeDomestic column accessible",
      );
      assert.equal(
        metadata.getInitialChargeImport(),
        "import_charge",
        "initialChargeImport column accessible",
      );
      assert.equal(
        metadata.getInitialChargeExport(),
        "export_charge",
        "initialChargeExport column accessible",
      );
      assert.equal(metadata.getRetirement(), "retirement_val", "retirement column accessible");

      // Key is generated, not stored directly
      assert.ok(metadata.getKey().length > 0, "key column accessible via getKey()");
    });
  });
}

export {buildSubstanceMetadataTests};

