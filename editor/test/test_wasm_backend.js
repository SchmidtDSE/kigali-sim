/**
 * Tests for WASM backend functionality.
 *
 * @license BSD, see LICENSE.md.
 */

import {WasmBackend, WasmLayer, ReportDataParser, BackendResult} from "wasm_backend";
import {EngineNumber} from "engine_number";
import {EngineResult, EngineResultBuilder, ImportSupplement, TradeSupplement} from "engine_struct";

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

        const parser = new ReportDataParser();
        const results = parser.parseResponse(response);

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

        const parser = new ReportDataParser();
        assert.throws(
          function () {
            parser.parseResponse(response);
          },
          /Compilation Error: Syntax error/,
          "Should throw error with the error message",
        );
      });

      QUnit.test("parseResponse handles empty CSV data", function (assert) {
        const response = "OK\n\n";

        const parser = new ReportDataParser();
        const results = parser.parseResponse(response);
        assert.equal(results.length, 0, "Should return empty array for empty CSV data");
      });

      QUnit.test("parseResponse handles headers-only CSV", function (assert) {
        const response = "OK\n\n" +
          "scenario,trial,year,application,substance," +
          "domestic,import,recycle,domesticConsumption,importConsumption," +
          "recycleConsumption,population,populationNew,rechargeEmissions," +
          "eolEmissions,energyConsumption,initialChargeValue," +
          "initialChargeConsumption,importNewPopulation";

        const parser = new ReportDataParser();
        const results = parser.parseResponse(response);
        assert.equal(results.length, 0, "Should return empty array for headers-only CSV");
      });

      QUnit.test("parseResponse handles invalid response format", function (assert) {
        const response = "InvalidFormat";

        const parser = new ReportDataParser();
        assert.throws(
          function () {
            parser.parseResponse(response);
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

        const parser = new ReportDataParser();
        const results = parser.parseResponse(response);

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
          {concurrency: undefined, expected: 2}, // Fallback to 2, then max(2, 2-1) = 2
          {concurrency: 1, expected: 2}, // max(2, 1-1) = 2
          {concurrency: 2, expected: 2}, // max(2, 2-1) = 2
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
          const scenarioTrialCounts = {
            "Scenario1": 1,
            "Scenario2": 1,
            "Scenario3": 1,
            "Scenario4": 1,
          };
          const runPromise = wasmLayer.runSimulation(
            testCode,
            scenarioNames,
            scenarioTrialCounts,
          );

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

      QUnit.test("runSimulation with progress tracking for single scenario", function (assert) {
        const done = assert.async();

        const originalWorker = window.Worker;
        const progressUpdates = [];
        let mockWorker = null;

        window.Worker = function () {
          mockWorker = {
            onmessage: null,
            onerror: null,
            postMessage: function () {},
            terminate: function () {},
          };
          return mockWorker;
        };

        const progressCallback = (progress) => {
          progressUpdates.push(progress);
        };

        const wasmLayer = new WasmLayer(progressCallback, 2);
        const testCode = "test code";
        const scenarioNames = ["TestScenario"];
        const scenarioTrialCounts = {"TestScenario": 5};

        wasmLayer.initialize().then(() => {
          const runPromise = wasmLayer.runSimulation(testCode, scenarioNames, scenarioTrialCounts);

          setTimeout(() => {
            // Find the request ID from pending requests
            const requestId = Array.from(wasmLayer._pendingRequests.keys())[0];

            // Simulate progress updates: 0%, 20%, 40%, 60%, 80%, 100%
            [0, 0.2, 0.4, 0.6, 0.8, 1.0].forEach((progress) => {
              mockWorker.onmessage({
                data: {
                  resultType: "progress",
                  id: requestId,
                  scenarioName: "TestScenario",
                  progress: progress,
                },
              });
            });

            // Simulate completion
            mockWorker.onmessage({
              data: {
                resultType: "dataset",
                id: requestId,
                scenarioName: "TestScenario",
                success: true,
                result: "OK\n\nscenario,trial,year,application,substance," +
                  "domestic,import,export,recycle,domesticConsumption," +
                  "importConsumption,exportConsumption,recycleConsumption," +
                  "population,populationNew,rechargeEmissions,eolEmissions," +
                  "initialChargeEmissions,energyConsumption," +
                  "importInitialChargeValue,importInitialChargeConsumption," +
                  "importPopulation,exportInitialChargeValue," +
                  "exportInitialChargeConsumption,bankKg,bankTCO2e,bankChangeKg," +
                  "bankChangeTCO2e\n" +
                  "TestScenario,1,2024,TestApp,TestSub,0 kg,0 kg,0 kg,0 kg," +
                  "0 tCO2e,0 tCO2e,0 tCO2e,0 tCO2e,0 units,0 units,0 tCO2e," +
                  "0 tCO2e,0 tCO2e,0 kwh,0 kg,0 tCO2e,0 units,0 kg,0 tCO2e," +
                  "0 kg,0 tCO2e,0 kg,0 tCO2e",
              },
            });

            runPromise.then(() => {
              // Verify progress was reported
              assert.equal(progressUpdates.length, 6,
                "Should have 6 progress updates");
              assert.deepEqual(progressUpdates, [0, 0.2, 0.4, 0.6, 0.8, 1.0],
                "Progress updates should match expected values");

              window.Worker = originalWorker;
              done();
            }).catch((error) => {
              window.Worker = originalWorker;
              assert.ok(false, "runSimulation should not fail: " + error.message);
              done();
            });
          }, 0);
        }).catch((error) => {
          window.Worker = originalWorker;
          assert.ok(false, "Initialization failed: " + error.message);
          done();
        });
      });

      QUnit.test("runSimulation with progress tracking for multiple scenarios", function (assert) {
        const done = assert.async();

        const originalWorker = window.Worker;
        const progressUpdates = [];
        const mockWorkers = [];

        window.Worker = function () {
          const mockWorker = {
            onmessage: null,
            onerror: null,
            postMessage: function () {},
            terminate: function () {},
          };
          mockWorkers.push(mockWorker);
          return mockWorker;
        };

        const progressCallback = (progress) => {
          progressUpdates.push(progress);
        };

        const wasmLayer = new WasmLayer(progressCallback, 2);
        const testCode = "test code";
        const scenarioNames = ["BAU", "Policy"];
        const scenarioTrialCounts = {"BAU": 3, "Policy": 2};

        wasmLayer.initialize().then(() => {
          const runPromise = wasmLayer.runSimulation(testCode, scenarioNames, scenarioTrialCounts);

          setTimeout(() => {
            const requestId = Array.from(wasmLayer._pendingRequests.keys())[0];

            // Simulate progress: BAU completes 33% (1/3), overall should be 20% (1/5)
            mockWorkers[0].onmessage({
              data: {
                resultType: "progress",
                id: requestId,
                scenarioName: "BAU",
                progress: 1 / 3,
              },
            });

            // Simulate progress: Policy completes 50% (1/2), overall should be 50% (2.5/5)
            mockWorkers[1].onmessage({
              data: {
                resultType: "progress",
                id: requestId,
                scenarioName: "Policy",
                progress: 0.5,
              },
            });

            // Simulate progress: BAU completes 67% (2/3), overall should be 70% (3.5/5)
            mockWorkers[0].onmessage({
              data: {
                resultType: "progress",
                id: requestId,
                scenarioName: "BAU",
                progress: 2 / 3,
              },
            });

            // Simulate completion
            const csvHeader = "scenario,trial,year,application,substance," +
              "domestic,import,export,recycle,domesticConsumption," +
              "importConsumption,exportConsumption,recycleConsumption," +
              "population,populationNew,rechargeEmissions,eolEmissions," +
              "initialChargeEmissions,energyConsumption," +
              "importInitialChargeValue,importInitialChargeConsumption," +
              "importPopulation,exportInitialChargeValue," +
              "exportInitialChargeConsumption,bankKg,bankTCO2e,bankChangeKg," +
              "bankChangeTCO2e\n";

            mockWorkers[0].onmessage({
              data: {
                resultType: "dataset",
                id: requestId,
                scenarioName: "BAU",
                success: true,
                result: "OK\n\n" + csvHeader +
                  "BAU,1,2024,TestApp,TestSub,0 kg,0 kg,0 kg,0 kg," +
                  "0 tCO2e,0 tCO2e,0 tCO2e,0 tCO2e,0 units,0 units,0 tCO2e," +
                  "0 tCO2e,0 tCO2e,0 kwh,0 kg,0 tCO2e,0 units,0 kg,0 tCO2e," +
                  "0 kg,0 tCO2e,0 kg,0 tCO2e",
              },
            });

            mockWorkers[1].onmessage({
              data: {
                resultType: "dataset",
                id: requestId,
                scenarioName: "Policy",
                success: true,
                result: "OK\n\n" + csvHeader +
                  "Policy,1,2024,TestApp,TestSub,0 kg,0 kg,0 kg,0 kg," +
                  "0 tCO2e,0 tCO2e,0 tCO2e,0 tCO2e,0 units,0 units,0 tCO2e," +
                  "0 tCO2e,0 tCO2e,0 kwh,0 kg,0 tCO2e,0 units,0 kg,0 tCO2e," +
                  "0 kg,0 tCO2e,0 kg,0 tCO2e",
              },
            });

            runPromise.then(() => {
              // Verify progress aggregation
              assert.equal(progressUpdates.length, 3,
                "Should have 3 progress updates");

              // BAU: 1/3 complete = 1 trial out of 3, overall: 1/5 = 0.2
              assert.ok(Math.abs(progressUpdates[0] - 0.2) < 0.01,
                "First progress should be ~0.2 (1/5 trials)");

              // BAU: 1/3, Policy: 1/2 = 1 + 1 = 2 trials, overall: 2/5 = 0.4
              // Actually: (1/3 * 3 + 0.5 * 2) / 5 = (1 + 1) / 5 = 0.4
              assert.ok(Math.abs(progressUpdates[1] - 0.4) < 0.01,
                "Second progress should be ~0.4 (2/5 trials)");

              // BAU: 2/3, Policy: 1/2 = 2 + 1 = 3 trials, overall: 3/5 = 0.6
              assert.ok(Math.abs(progressUpdates[2] - 0.6) < 0.01,
                "Third progress should be ~0.6 (3/5 trials)");

              window.Worker = originalWorker;
              done();
            }).catch((error) => {
              window.Worker = originalWorker;
              assert.ok(false, "runSimulation should not fail: " + error.message);
              done();
            });
          }, 0);
        }).catch((error) => {
          window.Worker = originalWorker;
          assert.ok(false, "Initialization failed: " + error.message);
          done();
        });
      });

      QUnit.test(
        "runSimulation with progress tracking handles variable trial counts",
        function (assert) {
          const done = assert.async();

          const originalWorker = window.Worker;
          const progressUpdates = [];
          const mockWorkers = [];

          window.Worker = function () {
            const mockWorker = {
              onmessage: null,
              onerror: null,
              postMessage: function () {},
              terminate: function () {},
            };
            mockWorkers.push(mockWorker);
            return mockWorker;
          };

          const progressCallback = (progress) => {
            progressUpdates.push(progress);
          };

          const wasmLayer = new WasmLayer(progressCallback, 2);
          const testCode = "test code";
          const scenarioNames = ["ScenarioA", "ScenarioB"];
          const scenarioTrialCounts = {"ScenarioA": 1, "ScenarioB": 9};

          wasmLayer.initialize().then(() => {
            const runPromise = wasmLayer.runSimulation(
              testCode,
              scenarioNames,
              scenarioTrialCounts,
            );

            setTimeout(() => {
              const requestId = Array.from(wasmLayer._pendingRequests.keys())[0];

              // ScenarioA completes 100% (1 trial), overall: 1/10 = 0.1
              mockWorkers[0].onmessage({
                data: {
                  resultType: "progress",
                  id: requestId,
                  scenarioName: "ScenarioA",
                  progress: 1.0,
                },
              });

              // ScenarioB completes 33% (3/9 trials), overall: (1 + 3)/10 = 0.4
              mockWorkers[1].onmessage({
                data: {
                  resultType: "progress",
                  id: requestId,
                  scenarioName: "ScenarioB",
                  progress: 1 / 3,
                },
              });

              // Simulate completion
              const csvHeader = "scenario,trial,year,application,substance," +
              "domestic,import,export,recycle,domesticConsumption," +
              "importConsumption,exportConsumption,recycleConsumption," +
              "population,populationNew,rechargeEmissions,eolEmissions," +
              "initialChargeEmissions,energyConsumption," +
              "importInitialChargeValue,importInitialChargeConsumption," +
              "importPopulation,exportInitialChargeValue," +
              "exportInitialChargeConsumption,bankKg,bankTCO2e,bankChangeKg," +
              "bankChangeTCO2e\n";

              mockWorkers[0].onmessage({
                data: {
                  resultType: "dataset",
                  id: requestId,
                  scenarioName: "ScenarioA",
                  success: true,
                  result: "OK\n\n" + csvHeader +
                  "ScenarioA,1,2024,TestApp,TestSub,0 kg,0 kg,0 kg,0 kg," +
                  "0 tCO2e,0 tCO2e,0 tCO2e,0 tCO2e,0 units,0 units,0 tCO2e," +
                  "0 tCO2e,0 tCO2e,0 kwh,0 kg,0 tCO2e,0 units,0 kg,0 tCO2e," +
                  "0 kg,0 tCO2e,0 kg,0 tCO2e",
                },
              });

              mockWorkers[1].onmessage({
                data: {
                  resultType: "dataset",
                  id: requestId,
                  scenarioName: "ScenarioB",
                  success: true,
                  result: "OK\n\n" + csvHeader +
                  "ScenarioB,1,2024,TestApp,TestSub,0 kg,0 kg,0 kg,0 kg," +
                  "0 tCO2e,0 tCO2e,0 tCO2e,0 tCO2e,0 units,0 units,0 tCO2e," +
                  "0 tCO2e,0 tCO2e,0 kwh,0 kg,0 tCO2e,0 units,0 kg,0 tCO2e," +
                  "0 kg,0 tCO2e,0 kg,0 tCO2e",
                },
              });

              runPromise.then(() => {
              // Verify ScenarioB progress is weighted 9x more than ScenarioA
                assert.equal(progressUpdates.length, 2,
                  "Should have 2 progress updates");

                // ScenarioA: 1/1 = 1 trial, overall: 1/10 = 0.1
                assert.ok(Math.abs(progressUpdates[0] - 0.1) < 0.01,
                  "First progress should be ~0.1 (1/10 trials)");

                // ScenarioA: 1, ScenarioB: 3 = 4 trials, overall: 4/10 = 0.4
                assert.ok(Math.abs(progressUpdates[1] - 0.4) < 0.01,
                  "Second progress should be ~0.4 (4/10 trials)");

                window.Worker = originalWorker;
                done();
              }).catch((error) => {
                window.Worker = originalWorker;
                assert.ok(false, "runSimulation should not fail: " + error.message);
                done();
              });
            }, 0);
          }).catch((error) => {
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
          const scenarioTrialCounts = {"TestScenario": 1};
          const runPromise = wasmLayer.runSimulation(testCode, scenarioNames, scenarioTrialCounts);

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

        // Create a minimal EngineResult using EngineResultBuilder
        const builder = new EngineResultBuilder();
        const tradeSupplement = new TradeSupplement(
          new EngineNumber(0, "kg"),
          new EngineNumber(0, "tCO2e"),
          new EngineNumber(0, "units"),
          new EngineNumber(0, "kg"),
          new EngineNumber(0, "tCO2e"),
        );
        builder.setApplication("app");
        builder.setSubstance("sub");
        builder.setYear(2024);
        builder.setScenarioName("scenario1");
        builder.setTrialNumber(1);
        builder.setDomesticValue(new EngineNumber(0, "kg"));
        builder.setImportValue(new EngineNumber(0, "kg"));
        builder.setExportValue(new EngineNumber(0, "kg"));
        builder.setRecycleValue(new EngineNumber(0, "kg"));
        builder.setDomesticConsumptionValue(new EngineNumber(0, "tCO2e"));
        builder.setImportConsumptionValue(new EngineNumber(0, "tCO2e"));
        builder.setExportConsumptionValue(new EngineNumber(0, "tCO2e"));
        builder.setRecycleConsumptionValue(new EngineNumber(0, "tCO2e"));
        builder.setPopulationValue(new EngineNumber(0, "units"));
        builder.setPopulationNew(new EngineNumber(0, "units"));
        builder.setRechargeEmissions(new EngineNumber(0, "tCO2e"));
        builder.setEolEmissions(new EngineNumber(0, "tCO2e"));
        builder.setInitialChargeEmissions(new EngineNumber(0, "tCO2e"));
        builder.setEnergyConsumption(new EngineNumber(0, "kwh"));
        builder.setTradeSupplement(tradeSupplement);
        builder.setBankKg(new EngineNumber(0, "kg"));
        builder.setBankTco2e(new EngineNumber(0, "tCO2e"));
        builder.setBankChangeKg(new EngineNumber(0, "kg"));
        builder.setBankChangeTco2e(new EngineNumber(0, "tCO2e"));

        const mockBackendResult = new BackendResult(
          "csv data",
          [builder.build()],
        );

        const mockWasmLayer = {
          runSimulation: function (code, scenarioNames, scenarioTrialCounts) {
            simulationCalled = true;
            assert.equal(code, testCode, "Code should be passed correctly");
            assert.ok(Array.isArray(scenarioNames), "Scenario names should be an array");
            assert.ok(scenarioNames.length > 0, "Should have at least one scenario name");
            assert.ok(scenarioTrialCounts, "Trial counts should be provided");
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
          runSimulation: function (code, scenarioNames, scenarioTrialCounts) {
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

      QUnit.test("execute handles empty code without errors", function (assert) {
        const done = assert.async();
        const emptyCode = "";
        const mockWasmLayer = {
          runSimulation: function (code, scenarioNames, scenarioTrialCounts) {
            assert.ok(false, "runSimulation should not be called for empty code");
            return Promise.resolve(new BackendResult("", []));
          },
        };

        const wasmBackend = new WasmBackend(mockWasmLayer);

        wasmBackend.execute(emptyCode).then((result) => {
          assert.ok(result instanceof BackendResult,
            "Should return a BackendResult for empty code");
          assert.equal(result.getCsvString(), "",
            "CSV string should be empty for empty code");
          assert.equal(result.getParsedResults().length, 0,
            "Parsed results should be empty for empty code");
          done();
        }).catch((error) => {
          assert.ok(false, "execute should not fail for empty code: " + error.message);
          done();
        });
      });

      QUnit.test("execute handles whitespace-only code without errors", function (assert) {
        const done = assert.async();
        const whitespaceCode = "   \n\t  \n  ";
        const mockWasmLayer = {
          runSimulation: function (code, scenarioNames, scenarioTrialCounts) {
            assert.ok(false, "runSimulation should not be called for whitespace-only code");
            return Promise.resolve(new BackendResult("", []));
          },
        };

        const wasmBackend = new WasmBackend(mockWasmLayer);

        wasmBackend.execute(whitespaceCode).then((result) => {
          assert.ok(result instanceof BackendResult,
            "Should return a BackendResult for whitespace-only code");
          assert.equal(result.getCsvString(), "",
            "CSV string should be empty for whitespace-only code");
          assert.equal(result.getParsedResults().length, 0,
            "Parsed results should be empty for whitespace-only code");
          done();
        }).catch((error) => {
          assert.ok(false,
            "execute should not fail for whitespace-only code: " + error.message);
          done();
        });
      });
    });
  });
}

export {buildWasmBackendTests};
