import {KnownSubstance, SubstanceLibraryKeeper, GwpLookupPresenter} from "known_substance";

function buildKnownSubstanceTests() {
  QUnit.module("KnownSubstance", function () {
    QUnit.test("creates with basic properties", function (assert) {
      const substance = new KnownSubstance("HFC-134a", 1430);

      assert.equal(substance.getName(), "HFC-134a", "substance name should match");
      assert.equal(substance.getGwp(), 1430, "GWP value should match");
    });

    QUnit.test("handles numeric GWP values", function (assert) {
      const substance = new KnownSubstance("HFC-32", 675.5);

      assert.equal(substance.getName(), "HFC-32", "substance name should match");
      assert.equal(substance.getGwp(), 675.5, "GWP value should handle decimals");
    });
  });

  QUnit.module("SubstanceLibraryKeeper", function () {
    const sampleData = {
      "HFC-134a": 1430.000,
      "R-404A": 3921.600,
      "HFC-32": 675.000,
      "CustMix-110": 2366.400,
      "R-407C": 1773.850,
    };

    QUnit.test("initializes with JSON data", function (assert) {
      const keeper = new SubstanceLibraryKeeper(sampleData);

      assert.ok(keeper, "keeper should be created");
      assert.ok(keeper.getSubstance("HFC-134a"), "should find exact match");
    });

    QUnit.test("normalizes substance keys correctly", function (assert) {
      const keeper = new SubstanceLibraryKeeper(sampleData);

      // Test the private method indirectly through getSubstance
      const substance1 = keeper.getSubstance("HFC-134a");
      const substance2 = keeper.getSubstance("hfc134a"); // lowercase, no hyphen
      const substance3 = keeper.getSubstance("HFC 134a"); // space instead of hyphen
      const substance4 = keeper.getSubstance("hfc_134a"); // underscore
      const substance5 = keeper.getSubstance("HFC(134a)"); // parentheses

      assert.ok(substance1, "should find original format");
      assert.ok(substance2, "should find lowercase without punctuation");
      assert.ok(substance3, "should find with space");
      assert.ok(substance4, "should find with underscore");
      assert.ok(substance5, "should find with parentheses");

      // All should return the same substance
      assert.equal(substance1.getName(), "HFC-134a", "all variations should return same substance");
      assert.equal(substance2.getName(), "HFC-134a", "all variations should return same substance");
      assert.equal(substance3.getName(), "HFC-134a", "all variations should return same substance");
      assert.equal(substance4.getName(), "HFC-134a", "all variations should return same substance");
      assert.equal(substance5.getName(), "HFC-134a", "all variations should return same substance");
    });

    QUnit.test("handles successful lookups", function (assert) {
      const keeper = new SubstanceLibraryKeeper(sampleData);

      const substance = keeper.getSubstance("HFC-134a");

      assert.ok(substance instanceof KnownSubstance, "should return KnownSubstance instance");
      assert.equal(substance.getName(), "HFC-134a", "should return correct name");
      assert.equal(substance.getGwp(), 1430, "should return correct GWP");
    });

    QUnit.test("handles failed lookups", function (assert) {
      const keeper = new SubstanceLibraryKeeper(sampleData);

      const result = keeper.getSubstance("UNKNOWN-SUBSTANCE");

      assert.equal(result, null, "should return null for unknown substances");
    });

    QUnit.test("handles edge cases for input validation", function (assert) {
      const keeper = new SubstanceLibraryKeeper(sampleData);

      assert.equal(keeper.getSubstance(""), null, "empty string should return null");
      assert.equal(keeper.getSubstance(null), null, "null should return null");
      assert.equal(keeper.getSubstance("   "), null, "whitespace only should return null");
    });

    QUnit.test("handles whitespace in input", function (assert) {
      const keeper = new SubstanceLibraryKeeper(sampleData);

      const substance = keeper.getSubstance("  HFC-134a  ");

      assert.ok(substance, "should handle leading/trailing whitespace");
      assert.equal(substance.getName(), "HFC-134a", "should return correct substance");
    });

    QUnit.test("normalizes complex punctuation", function (assert) {
      const keeper = new SubstanceLibraryKeeper({
        "R-407C": 1773.850,
      });

      // Test various punctuation variations
      const variations = [
        "R407C",
        "r-407c",
        "R.407C",
        "R_407C",
        "R(407)C",
        "R[407]C",
        " R - 407 C ",
      ];

      for (const variation of variations) {
        const substance = keeper.getSubstance(variation);
        assert.ok(substance, `should find substance for variation: ${variation}`);
        assert.equal(substance.getName(), "R-407C", `should return correct name for: ${variation}`);
      }
    });
  });

  QUnit.module("GwpLookupPresenter", function () {
    QUnit.test("initializes correctly", function (assert) {
      // Create mock DOM elements
      const mockElements = {
        lookupLink: {
          addEventListener: function (event, handler) {
            this.clickHandler = handler;
          },
          click: function () {
            if (this.clickHandler) {
              this.clickHandler({preventDefault: () => {}});
            }
          },
        },
        substanceInput: {
          value: "HFC-134a",
        },
        ghgInput: {
          value: "",
        },
        ghgUnitsInput: {
          value: "",
        },
      };

      const presenter = new GwpLookupPresenter(
        mockElements.lookupLink,
        mockElements.substanceInput,
        mockElements.ghgInput,
        mockElements.ghgUnitsInput,
        "test/path.json",
      );

      assert.ok(presenter, "presenter should be created");
      assert.ok(mockElements.lookupLink.clickHandler, "click handler should be registered");
    });

    QUnit.test("handles empty substance input", function (assert) {
      const done = assert.async();
      let alertMessage = "";

      const mockLink = document.createElement("a");
      const presenter = new GwpLookupPresenter(
        mockLink,
        {value: "   "}, // empty/whitespace input
        document.createElement("input"),
        document.createElement("select"),
        "test/path.json",
      );

      // Mock alert function
      presenter._showAlert = function (message) {
        alertMessage = message;
      };

      // Trigger click
      presenter._onLookupClick({preventDefault: () => {}}).then(() => {
        assert.ok(
          alertMessage.includes("Please enter a substance name"),
          "should prompt for substance name - actual message: '" + alertMessage + "'",
        );
        done();
      }).catch((error) => {
        assert.ok(false, "Should not throw error: " + error.message);
        done();
      });
    });

    QUnit.test("gets substance through public interface", function (assert) {
      const mockLink = document.createElement("a");
      const presenter = new GwpLookupPresenter(
        mockLink,
        document.createElement("input"),
        document.createElement("input"),
        document.createElement("select"),
        "test/path.json",
      );

      // Should return null when library not loaded
      const result = presenter.getSubstance("HFC-134a");
      assert.equal(result, null, "should return null when library not loaded");
    });
  });

  QUnit.module("GwpLookupPresenter Integration", function () {
    QUnit.test("loads library and performs successful lookup", function (assert) {
      const done = assert.async();
      let alertMessage = "";
      let ghgValue = "";
      let unitsValue = "";

      // Mock fetch
      const originalFetch = window.fetch;
      window.fetch = function (url) {
        const mockData = {
          "HFC-134a": 1430.000,
          "R-404A": 3921.600,
        };

        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockData),
        });
      };

      const mockElements = {
        lookupLink: {
          addEventListener: function (event, handler) {
            this.clickHandler = handler;
          },
        },
        substanceInput: {
          value: "HFC-134a",
        },
        ghgInput: {
          get value() {
            return ghgValue;
          },
          set value(v) {
            ghgValue = v;
          },
        },
        ghgUnitsInput: {
          get value() {
            return unitsValue;
          },
          set value(v) {
            unitsValue = v;
          },
        },
      };

      const presenter = new GwpLookupPresenter(
        mockElements.lookupLink,
        mockElements.substanceInput,
        mockElements.ghgInput,
        mockElements.ghgUnitsInput,
        "test/path.json",
      );

      // Mock alert function
      presenter._showAlert = function (message) {
        alertMessage = message;
      };

      // Perform lookup
      presenter._onLookupClick({preventDefault: () => {}}).then(() => {
        assert.equal(ghgValue, "1430", "should set GHG input value");
        assert.equal(unitsValue, "kgCO2e / kg", "should set units to kgCO2e / kg");
        assert.ok(
          alertMessage.includes("Found GWP value for HFC-134a: 1430"),
          "should show success message",
        );
        assert.ok(
          alertMessage.includes("Please confirm this value is correct"),
          "should include confirmation reminder",
        );

        // Restore fetch and complete test
        window.fetch = originalFetch;
        done();
      }).catch((error) => {
        window.fetch = originalFetch;
        assert.ok(false, "Should not throw error: " + error.message);
        done();
      });
    });

    QUnit.test("handles substance not found", function (assert) {
      const done = assert.async();
      let alertMessage = "";

      // Mock fetch
      const originalFetch = window.fetch;
      window.fetch = function (url) {
        const mockData = {
          "HFC-134a": 1430.000,
          "R-404A": 3921.600,
        };

        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockData),
        });
      };

      const mockElements = {
        lookupLink: {addEventListener: function () {}},
        substanceInput: {value: "UNKNOWN-SUBSTANCE"},
        ghgInput: {value: ""},
        ghgUnitsInput: {value: ""},
      };

      const presenter = new GwpLookupPresenter(
        mockElements.lookupLink,
        mockElements.substanceInput,
        mockElements.ghgInput,
        mockElements.ghgUnitsInput,
        "test/path.json",
      );

      presenter._showAlert = function (message) {
        alertMessage = message;
      };

      presenter._onLookupClick({preventDefault: () => {}}).then(() => {
        assert.ok(
          alertMessage.includes("No GWP value found for 'UNKNOWN-SUBSTANCE'"),
          "should show not found message",
        );
        assert.ok(
          alertMessage.includes("check the substance name spelling"),
          "should suggest checking spelling",
        );

        // Restore fetch and complete test
        window.fetch = originalFetch;
        done();
      }).catch((error) => {
        window.fetch = originalFetch;
        assert.ok(false, "Should not throw error: " + error.message);
        done();
      });
    });
  });

  QUnit.module("GwpLookupPresenter Error Handling", function () {
    QUnit.test("handles fetch errors", function (assert) {
      const done = assert.async();
      let alertMessage = "";

      // Mock failing fetch
      const originalFetch = window.fetch;
      window.fetch = function (url) {
        return Promise.resolve({
          ok: false,
          status: 404,
        });
      };

      const mockElements = {
        lookupLink: {addEventListener: function () {}},
        substanceInput: {value: "HFC-134a"},
        ghgInput: {value: ""},
        ghgUnitsInput: {value: ""},
      };

      const presenter = new GwpLookupPresenter(
        mockElements.lookupLink,
        mockElements.substanceInput,
        mockElements.ghgInput,
        mockElements.ghgUnitsInput,
        "test/path.json",
      );

      presenter._showAlert = function (message) {
        alertMessage = message;
      };

      presenter._onLookupClick({preventDefault: () => {}}).then(() => {
        assert.ok(
          alertMessage.includes("Error loading substance database"),
          "should show error message",
        );
        assert.ok(
          alertMessage.includes("try again or enter the value manually"),
          "should suggest manual entry",
        );

        // Restore fetch and complete test
        window.fetch = originalFetch;
        done();
      }).catch((error) => {
        window.fetch = originalFetch;
        assert.ok(false, "Should not throw error: " + error.message);
        done();
      });
    });

    QUnit.test("handles network errors", function (assert) {
      const done = assert.async();
      let alertMessage = "";

      // Mock network error
      const originalFetch = window.fetch;
      window.fetch = function (url) {
        return Promise.reject(new Error("Network error"));
      };

      const mockElements = {
        lookupLink: {addEventListener: function () {}},
        substanceInput: {value: "HFC-134a"},
        ghgInput: {value: ""},
        ghgUnitsInput: {value: ""},
      };

      const presenter = new GwpLookupPresenter(
        mockElements.lookupLink,
        mockElements.substanceInput,
        mockElements.ghgInput,
        mockElements.ghgUnitsInput,
        "test/path.json",
      );

      presenter._showAlert = function (message) {
        alertMessage = message;
      };

      presenter._onLookupClick({preventDefault: () => {}}).then(() => {
        assert.ok(
          alertMessage.includes("Error loading substance database"),
          "should show error message for network error",
        );

        // Restore fetch and complete test
        window.fetch = originalFetch;
        done();
      }).catch((error) => {
        window.fetch = originalFetch;
        assert.ok(false, "Should not throw error: " + error.message);
        done();
      });
    });
  });
}

export {buildKnownSubstanceTests};
