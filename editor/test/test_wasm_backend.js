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
          "domestic,import,recycle,domesticConsumption,importConsumption," +
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
        assert.equal(results[0].getDomestic().getValue(), 100,
          "Domestic value should be parsed correctly");
        assert.equal(results[0].getDomestic().getUnits(), "kg",
          "Domestic units should be parsed correctly");
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
          "domestic,import,recycle,domesticConsumption,importConsumption," +
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
          "domestic,import,recycle,domesticConsumption,importConsumption," +
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
        assert.ok(result.getDomestic() instanceof EngineNumber,
          "Domestic should be EngineNumber");
        assert.equal(result.getDomestic().getValue(), 100.5,
          "Domestic value should be correct");
        assert.equal(result.getDomestic().getUnits(), "kg",
          "Domestic units should be correct");

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
        const tradeSupplement = result.getTradeSupplement();
        assert.ok(tradeSupplement, "TradeSupplement should be defined");
        assert.ok(tradeSupplement.getImportInitialChargeValue() instanceof EngineNumber,
          "ImportInitialChargeValue should be EngineNumber");
        assert.equal(tradeSupplement.getImportInitialChargeValue().getValue(), 0,
          "ImportInitialChargeValue should be 0");
        assert.equal(tradeSupplement.getImportInitialChargeValue().getUnits(), "kg",
          "ImportInitialChargeValue units should be kg");
        assert.ok(tradeSupplement.getImportInitialChargeConsumption() instanceof EngineNumber,
          "ImportInitialChargeConsumption should be EngineNumber");
        assert.equal(tradeSupplement.getImportPopulation().getUnits(), "units",
          "ImportPopulation units should be units");
      });
    });

    QUnit.module("WasmLayer", function () {
      QUnit.test("constructor initializes correctly", function (assert) {
        const wasmLayer = new WasmLayer();

        assert.ok(wasmLayer, "WasmLayer should be created");
        assert.ok(Array.isArray(wasmLayer._workers), "Workers should be an array");
        assert.equal(wasmLayer._workers.length, 0, "Workers array should be empty initially");
        assert.equal(wasmLayer._initPromise, null,
          "Init promise should be null initially");
        assert.ok(wasmLayer._pendingRequests instanceof Map,
          "Pending requests should be a Map");
        assert.equal(wasmLayer._nextRequestId, 1, "Next request ID should start at 1");
        assert.equal(wasmLayer._nextWorkerIndex, 0,
          "Next worker index should start at 0");
      });

      QUnit.test("constructor calculates pool size from hardwareConcurrency", function (assert) {
        // Save original value
        const originalConcurrency = navigator.hardwareConcurrency;

        // Test with different hardwareConcurrency values
        const testCases = [
          {concurrency: undefined, expected: 3}, // Fallback to 4, then 4-1=3
          {concurrency: 1, expected: 2}, // min(2, 1-1) = 2
          {concurrency: 2, expected: 2}, // 2-1=1, min is 2
          {concurrency: 4, expected: 3}, // 4-1=3
          {concurrency: 8, expected: 7}, // 8-1=7
          {concurrency: 16, expected: 15}, // 16-1=15
        ];

        testCases.forEach(({concurrency, expected}) => {
          // Mock navigator.hardwareConcurrency
          Object.defineProperty(navigator, "hardwareConcurrency", {
            value: concurrency,
            configurable: true,
          });

          const wasmLayer = new WasmLayer();
          assert.equal(wasmLayer._poolSize, expected,
            `Pool size should be ${expected} when hardwareConcurrency is ${concurrency}`);
        });

        // Restore original value
        Object.defineProperty(navigator, "hardwareConcurrency", {
          value: originalConcurrency,
          configurable: true,
        });
      });

      QUnit.test("constructor accepts custom pool size", function (assert) {
        const customPoolSize = 5;
        const wasmLayer = new WasmLayer(null, customPoolSize);

        assert.equal(wasmLayer._poolSize, customPoolSize,
          "Pool size should match custom value");
      });

      QUnit.test("constructor enforces minimum pool size of 2", function (assert) {
        const wasmLayer = new WasmLayer(null, 1);

        assert.equal(wasmLayer._poolSize, 2,
          "Pool size should be at least 2 even if custom value is 1");
      });

      QUnit.test("initialize creates worker pool correctly", function (assert) {
        const done = assert.async();
        const wasmLayer = new WasmLayer(null, 3); // Use custom pool size for testing

        // Mock Worker to avoid actual worker creation in test
        const originalWorker = window.Worker;
        let workersCreated = 0;

        window.Worker = function (scriptUrl) {
          workersCreated++;
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
          assert.equal(workersCreated, 3, "Should create 3 workers");
          assert.equal(wasmLayer._workers.length, 3, "Workers array should have 3 workers");
          assert.ok(wasmLayer._workers[0], "First worker should be assigned");
          assert.ok(wasmLayer._workers[1], "Second worker should be assigned");
          assert.ok(wasmLayer._workers[2], "Third worker should be assigned");

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

      QUnit.test("terminate cleans up all workers in pool", function (assert) {
        const wasmLayer = new WasmLayer(null, 3);

        // Mock workers
        const mockWorkers = [
          {
            terminate: function () {
              this.terminated = true;
            },
            terminated: false,
          },
          {
            terminate: function () {
              this.terminated = true;
            },
            terminated: false,
          },
          {
            terminate: function () {
              this.terminated = true;
            },
            terminated: false,
          },
        ];

        wasmLayer._workers = mockWorkers;
        wasmLayer._nextWorkerIndex = 5; // Simulate some usage
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

        assert.ok(mockWorkers[0].terminated, "First worker should be terminated");
        assert.ok(mockWorkers[1].terminated, "Second worker should be terminated");
        assert.ok(mockWorkers[2].terminated, "Third worker should be terminated");
        assert.equal(wasmLayer._workers.length, 0, "Workers array should be empty");
        assert.equal(wasmLayer._nextWorkerIndex, 0, "Worker index should be reset");
        assert.equal(wasmLayer._initPromise, null, "Init promise should be cleared");
        assert.equal(wasmLayer._pendingRequests.size, 0,
          "Pending requests should be cleared");
      });

      QUnit.test("runSimulation uses round-robin distribution", function (assert) {
        const done = assert.async();

        // Mock Worker before creating the layer
        const originalWorker = window.Worker;
        const workerMessages = [[], [], []]; // Track messages per worker
        const mockWorkers = [];

        window.Worker = function () {
          const workerIndex = mockWorkers.length;
          const mockWorker = {
            onmessage: null,
            onerror: null,
            postMessage: function (message) {
              workerMessages[workerIndex].push(message);
            },
            terminate: function () {},
          };
          mockWorkers.push(mockWorker);
          return mockWorker;
        };

        const wasmLayer = new WasmLayer(null, 3); // 3 workers
        const testCode = "test QubecTalk code";
        const scenarioNames = ["Scenario1", "Scenario2", "Scenario3", "Scenario4"];

        wasmLayer.initialize().then(() => {
          const runPromise = wasmLayer.runSimulation(testCode, scenarioNames);

          // Wait a tick for postMessage to be called
          setTimeout(() => {
            // Check round-robin distribution: 4 scenarios across 3 workers
            // Worker 0 should get Scenario1, Scenario4
            // Worker 1 should get Scenario2
            // Worker 2 should get Scenario3
            assert.equal(workerMessages[0].length, 2,
              "Worker 0 should receive 2 messages");
            assert.equal(workerMessages[1].length, 1,
              "Worker 1 should receive 1 message");
            assert.equal(workerMessages[2].length, 1,
              "Worker 2 should receive 1 message");

            assert.equal(workerMessages[0][0].scenarioName, "Scenario1",
              "Worker 0 first message should be Scenario1");
            assert.equal(workerMessages[0][1].scenarioName, "Scenario4",
              "Worker 0 second message should be Scenario4");
            assert.equal(workerMessages[1][0].scenarioName, "Scenario2",
              "Worker 1 should get Scenario2");
            assert.equal(workerMessages[2][0].scenarioName, "Scenario3",
              "Worker 2 should get Scenario3");

            // Simulate successful responses from all workers
            const responseTemplate = "OK\n\n" +
              "scenario,trial,year,application,substance,domestic,import," +
              "recycle,domesticConsumption,importConsumption," +
              "recycleConsumption,population,populationNew,rechargeEmissions," +
              "eolEmissions,energyConsumption,initialChargeValue," +
              "initialChargeConsumption,importNewPopulation\n";

            scenarioNames.forEach((scenarioName, index) => {
              const workerIndex = index % 3;
              const message = workerMessages[workerIndex][Math.floor(index / 3)];
              const responseData = {
                resultType: "dataset",
                id: message.id,
                scenarioName: scenarioName,
                success: true,
                result: responseTemplate +
                  `${scenarioName},1,2024,TestApp,TestSub,0 kg,0 kg,0 kg,0 tCO2e,` +
                  "0 tCO2e,0 tCO2e,0 units,0 units,0 tCO2e,0 tCO2e,0 kwh," +
                  "0 kg,0 tCO2e,0 units",
              };
              mockWorkers[workerIndex].onmessage({data: responseData});
            });

            runPromise.then((backendResult) => {
              assert.ok(backendResult instanceof BackendResult,
                "Should return BackendResult instance");
              assert.ok(Array.isArray(backendResult.getParsedResults()),
                "Should contain array of parsed results");
              assert.equal(backendResult.getParsedResults().length, 4,
                "Should have results for all 4 scenarios");

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

      QUnit.test("runSimulation handles single scenario", function (assert) {
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

        const wasmLayer = new WasmLayer(null, 2);
        const testCode = "test QubecTalk code";
        const scenarioNames = ["TestScenario"];

        wasmLayer.initialize().then(() => {
          const runPromise = wasmLayer.runSimulation(testCode, scenarioNames);

          // Wait a tick for postMessage to be called
          setTimeout(() => {
            // Check the message sent to worker
            assert.ok(workerMessage, "Message should be sent to worker");
            assert.equal(workerMessage.command, "execute", "Command should be execute");
            assert.equal(workerMessage.code, testCode, "Code should be passed correctly");
            assert.ok(workerMessage.id > 0, "Request ID should be positive");
            assert.equal(workerMessage.scenarioName, "TestScenario",
              "Scenario name should be passed correctly");

            // Simulate successful response
            const responseData = {
              resultType: "dataset",
              id: workerMessage.id,
              scenarioName: "TestScenario",
              success: true,
              result: "OK\n\n" +
                "scenario,trial,year,application,substance,domestic,import," +
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
        const testCode = "start default\nend default\nstart simulations\n" +
          "simulate \"scenario1\" from years 1 to 10\nend simulations";
        const mockBackendResult = new BackendResult(
          "csv data",
          [new EngineResult("app", "sub", 2024, "scenario1", 1)],
        );

        const mockWasmLayer = {
          runSimulation: function (code, scenarioNames) {
            simulationCalled = true;
            assert.equal(code, testCode, "Code should be passed correctly");
            assert.ok(Array.isArray(scenarioNames), "Scenario names should be an array");
            assert.ok(scenarioNames.length > 0, "Should have at least one scenario name");
            return Promise.resolve(mockBackendResult);
          },
        };

        const wasmBackend = new WasmBackend(mockWasmLayer);

        wasmBackend.execute(testCode).then((results) => {
          assert.ok(simulationCalled, "runSimulation should be called");
          assert.equal(results, mockBackendResult, "Results should be returned correctly");
          done();
        }).catch((error) => {
          assert.ok(false, "execute should not fail: " + error.message);
          done();
        });
      });

      QUnit.test("execute handles wasmLayer errors", function (assert) {
        const done = assert.async();
        const testError = new Error("WASM execution failed");
        const testCode = "start default\nend default\nstart simulations\n" +
          "simulate \"scenario1\" from years 1 to 10\nend simulations";

        const mockWasmLayer = {
          runSimulation: function (code, scenarioNames) {
            return Promise.reject(testError);
          },
        };

        const wasmBackend = new WasmBackend(mockWasmLayer);

        wasmBackend.execute(testCode).then(() => {
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
