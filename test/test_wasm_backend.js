/**
 * Tests for WASM backend functionality.
 *
 * @license BSD, see LICENSE.md.
 */

import {WasmBackend, WasmLayer, ReportDataParser, BackendResult} from "wasm_backend";
import {EngineNumber} from "engine_number";
import {EngineResult, ImportSupplement} from "engine_struct";

function buildWasmBackendTests() {
  QUnit.module("WasmBackend", function () {
    QUnit.module("ReportDataParser", function () {
      QUnit.test("parseResponse handles OK status with CSV data", function (assert) {
        const response = "OK\n\n" +
          "scenario,trial,year,application,substance," +
          "manufacture,import,recycle,domesticConsumption,importConsumption," +
          "recycleConsumption,population,populationNew,rechargeEmissions," +
          "eolEmissions,energyConsumption,initialChargeValue," +
          "initialChargeConsumption,importNewPopulation\n" +
          "TestScenario,1,2024,TestApp,TestSub," +
          "100 kg,50 kg,0 kg,0 tCO2e,0 tCO2e," +
          "0 tCO2e,0 units,0 units,0 tCO2e," +
          "0 tCO2e,0 kwh,0 kg,0 tCO2e,0 units";

        const results = ReportDataParser.parseResponse(response);

        assert.equal(results.length, 1, "Should parse one result");
        assert.equal(results[0].getApplication(), "TestApp",
          "Application should be parsed correctly");
        assert.equal(results[0].getSubstance(), "TestSub",
          "Substance should be parsed correctly");
        assert.equal(results[0].getYear(), 2024, "Year should be parsed correctly");
        assert.equal(results[0].getScenarioName(), "TestScenario",
          "Scenario name should be parsed correctly");
        assert.equal(results[0].getTrialNumber(), 1,
          "Trial number should be parsed correctly");
        assert.equal(results[0].getManufacture().getValue(), 100,
          "Manufacture value should be parsed correctly");
        assert.equal(results[0].getManufacture().getUnits(), "kg",
          "Manufacture units should be parsed correctly");
        assert.equal(results[0].getImport().getValue(), 50,
          "Import value should be parsed correctly");
        assert.equal(results[0].getImport().getUnits(), "kg",
          "Import units should be parsed correctly");
      });

      QUnit.test("parseResponse handles error status", function (assert) {
        const response = "Compilation Error: Syntax error\n\n";

        assert.throws(
          function () {
            ReportDataParser.parseResponse(response);
          },
          /Compilation Error: Syntax error/,
          "Should throw error with the error message",
        );
      });

      QUnit.test("parseResponse handles empty CSV data", function (assert) {
        const response = "OK\n\n";

        const results = ReportDataParser.parseResponse(response);
        assert.equal(results.length, 0, "Should return empty array for empty CSV data");
      });

      QUnit.test("parseResponse handles headers-only CSV", function (assert) {
        const response = "OK\n\n" +
          "scenario,trial,year,application,substance," +
          "manufacture,import,recycle,domesticConsumption,importConsumption," +
          "recycleConsumption,population,populationNew,rechargeEmissions," +
          "eolEmissions,energyConsumption,initialChargeValue," +
          "initialChargeConsumption,importNewPopulation";

        const results = ReportDataParser.parseResponse(response);
        assert.equal(results.length, 0, "Should return empty array for headers-only CSV");
      });

      QUnit.test("parseResponse handles invalid response format", function (assert) {
        const response = "InvalidFormat";

        assert.throws(
          function () {
            ReportDataParser.parseResponse(response);
          },
          /Invalid response format/,
          "Should throw error for invalid response format",
        );
      });

      QUnit.test("parseResponse creates EngineNumber objects correctly", function (assert) {
        const response = "OK\n\n" +
          "scenario,trial,year,application,substance," +
          "manufacture,import,recycle,domesticConsumption,importConsumption," +
          "recycleConsumption,population,populationNew,rechargeEmissions," +
          "eolEmissions,energyConsumption,initialChargeValue," +
          "initialChargeConsumption,importNewPopulation\n" +
          "TestScenario,1,2024,TestApp,TestSub," +
          "100.5 kg,50.25 kg,0 kg,0 tCO2e,0 tCO2e," +
          "0 tCO2e,1000 units,0 units,0 tCO2e," +
          "0 tCO2e,500.75 kwh,0 kg,0 tCO2e,0 units";

        const results = ReportDataParser.parseResponse(response);

        assert.equal(results.length, 1, "Should parse one result");

        const result = results[0];
        assert.ok(result.getManufacture() instanceof EngineNumber,
          "Manufacture should be EngineNumber");
        assert.equal(result.getManufacture().getValue(), 100.5,
          "Manufacture value should be correct");
        assert.equal(result.getManufacture().getUnits(), "kg",
          "Manufacture units should be correct");

        assert.ok(result.getPopulation() instanceof EngineNumber,
          "Population should be EngineNumber");
        assert.equal(result.getPopulation().getValue(), 1000,
          "Population value should be correct");
        assert.equal(result.getPopulation().getUnits(), "units",
          "Population units should be correct");

        assert.ok(result.getEnergyConsumption() instanceof EngineNumber,
          "Energy consumption should be EngineNumber");
        assert.equal(result.getEnergyConsumption().getValue(), 500.75,
          "Energy consumption value should be correct");
        assert.equal(result.getEnergyConsumption().getUnits(), "kwh",
          "Energy consumption units should be correct");

        // Test ImportSupplement
        const importSupplement = result.getImportSupplement();
        assert.ok(importSupplement, "ImportSupplement should be defined");
        assert.ok(importSupplement.getInitialChargeValue() instanceof EngineNumber,
          "InitialChargeValue should be EngineNumber");
        assert.equal(importSupplement.getInitialChargeValue().getValue(), 0,
          "InitialChargeValue should be 0");
        assert.equal(importSupplement.getInitialChargeValue().getUnits(), "kg",
          "InitialChargeValue units should be kg");
        assert.ok(importSupplement.getInitialChargeConsumption() instanceof EngineNumber,
          "InitialChargeConsumption should be EngineNumber");
        assert.equal(importSupplement.getNewPopulation().getUnits(), "units",
          "NewPopulation units should be units");
      });
    });

    QUnit.module("WasmLayer", function () {
      QUnit.test("constructor initializes correctly", function (assert) {
        const wasmLayer = new WasmLayer();

        assert.ok(wasmLayer, "WasmLayer should be created");
        assert.equal(wasmLayer._worker, null, "Worker should be null initially");
        assert.equal(wasmLayer._initPromise, null,
          "Init promise should be null initially");
        assert.ok(wasmLayer._pendingRequests instanceof Map,
          "Pending requests should be a Map");
        assert.equal(wasmLayer._nextRequestId, 1, "Next request ID should start at 1");
      });

      QUnit.test("initialize creates worker correctly", function (assert) {
        const done = assert.async();
        const wasmLayer = new WasmLayer();

        // Mock Worker to avoid actual worker creation in test
        const originalWorker = window.Worker;
        let workerCreated = false;

        window.Worker = function (scriptUrl) {
          workerCreated = true;
          assert.ok(
            scriptUrl.startsWith("/js/wasm.worker.js"),
            "Should create worker with correct script URL",
          );

          // Mock worker object
          return {
            onmessage: null,
            onerror: null,
            postMessage: function () {},
            terminate: function () {},
          };
        };

        wasmLayer.initialize().then(() => {
          assert.ok(workerCreated, "Worker should be created");
          assert.ok(wasmLayer._worker, "Worker should be assigned");

          // Restore original Worker
          window.Worker = originalWorker;
          done();
        }).catch((error) => {
          // Restore original Worker
          window.Worker = originalWorker;
          assert.ok(false, "Initialize should not fail: " + error.message);
          done();
        });
      });

      QUnit.test("terminate cleans up resources", function (assert) {
        const wasmLayer = new WasmLayer();

        // Mock worker
        const mockWorker = {
          terminate: function () {
            this.terminated = true;
          },
          terminated: false,
        };

        wasmLayer._worker = mockWorker;
        wasmLayer._initPromise = Promise.resolve();

        // Add a pending request
        const mockRequest = {
          resolve: function () {},
          reject: function (error) {
            assert.equal(error.message, "WASM Worker terminated",
              "Should reject with termination error");
          },
        };
        wasmLayer._pendingRequests.set(1, mockRequest);

        wasmLayer.terminate();

        assert.ok(mockWorker.terminated, "Worker should be terminated");
        assert.equal(wasmLayer._worker, null, "Worker reference should be cleared");
        assert.equal(wasmLayer._initPromise, null, "Init promise should be cleared");
        assert.equal(wasmLayer._pendingRequests.size, 0,
          "Pending requests should be cleared");
      });

      QUnit.test("runSimulation generates correct request", function (assert) {
        const done = assert.async();

        // Mock Worker before creating the layer
        const originalWorker = window.Worker;
        let workerMessage = null;
        const mockWorker = {
          onmessage: null,
          onerror: null,
          postMessage: function (message) {
            workerMessage = message;
          },
          terminate: function () {},
        };

        window.Worker = function () {
          return mockWorker;
        };

        const wasmLayer = new WasmLayer();
        const testCode = "test QubecTalk code";

        wasmLayer.initialize().then(() => {
          const runPromise = wasmLayer.runSimulation(testCode);

          // Wait a tick for postMessage to be called
          setTimeout(() => {
            // Check the message sent to worker
            assert.ok(workerMessage, "Message should be sent to worker");
            assert.equal(workerMessage.command, "execute", "Command should be execute");
            assert.equal(workerMessage.code, testCode, "Code should be passed correctly");
            assert.ok(workerMessage.id > 0, "Request ID should be positive");

            // Simulate successful response
            const responseData = {
              id: workerMessage.id,
              success: true,
              result: "OK\n\n" +
                "scenario,trial,year,application,substance,manufacture,import," +
                "recycle,domesticConsumption,importConsumption," +
                "recycleConsumption,population,populationNew,rechargeEmissions," +
                "eolEmissions,energyConsumption,initialChargeValue," +
                "initialChargeConsumption,importNewPopulation\n" +
                "TestScenario,1,2024,TestApp,TestSub,0 kg,0 kg,0 kg,0 tCO2e," +
                "0 tCO2e,0 tCO2e,0 units,0 units,0 tCO2e,0 tCO2e,0 kwh," +
                "0 kg,0 tCO2e,0 units",
            };

            mockWorker.onmessage({data: responseData});

            runPromise.then((backendResult) => {
              assert.ok(backendResult instanceof BackendResult,
                "Should return BackendResult instance");
              assert.ok(Array.isArray(backendResult.getParsedResults()),
                "Should contain array of parsed results");
              // Restore original Worker
              window.Worker = originalWorker;
              done();
            }).catch((error) => {
              // Restore original Worker
              window.Worker = originalWorker;
              assert.ok(false, "runSimulation should not fail: " + error.message);
              done();
            });
          }, 0);

          // Return a resolved promise since we handle everything in setTimeout
          return Promise.resolve();
        }).catch((error) => {
          // Restore original Worker in case of initialization failure
          window.Worker = originalWorker;
          assert.ok(false, "Initialization failed: " + error.message);
          done();
        });
      });
    });

    QUnit.module("WasmBackend", function () {
      QUnit.test("constructor initializes correctly", function (assert) {
        const mockWasmLayer = {};
        const wasmBackend = new WasmBackend(mockWasmLayer);

        assert.ok(wasmBackend, "WasmBackend should be created");
        assert.equal(wasmBackend._wasmLayer, mockWasmLayer, "WasmLayer should be assigned");
      });

      QUnit.test("execute calls wasmLayer.runSimulation", function (assert) {
        const done = assert.async();
        let simulationCalled = false;
        const testCode = "test code";
        const mockResults = [new EngineResult("app", "sub", 2024, "scenario", 1)];

        const mockWasmLayer = {
          runSimulation: function (code) {
            simulationCalled = true;
            assert.equal(code, testCode, "Code should be passed correctly");
            return Promise.resolve(mockResults);
          },
        };

        const wasmBackend = new WasmBackend(mockWasmLayer);

        wasmBackend.execute(testCode).then((results) => {
          assert.ok(simulationCalled, "runSimulation should be called");
          assert.equal(results, mockResults, "Results should be returned correctly");
          done();
        }).catch((error) => {
          assert.ok(false, "execute should not fail: " + error.message);
          done();
        });
      });

      QUnit.test("execute handles wasmLayer errors", function (assert) {
        const done = assert.async();
        const testError = new Error("WASM execution failed");

        const mockWasmLayer = {
          runSimulation: function () {
            return Promise.reject(testError);
          },
        };

        const wasmBackend = new WasmBackend(mockWasmLayer);

        wasmBackend.execute("test code").then(() => {
          assert.ok(false, "execute should reject when wasmLayer fails");
          done();
        }).catch((error) => {
          assert.ok(error.message.includes("WASM simulation execution failed"),
            "Should wrap error with descriptive message");
          assert.ok(error.message.includes(testError.message),
            "Should include original error message");
          done();
        });
      });
    });
  });
}

export {buildWasmBackendTests};
