/**
 * WASM backend for executing QubecTalk simulations via WASM web worker.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";
import {EngineResult, TradeSupplement} from "engine_struct";
import {UiTranslatorCompiler} from "ui_translator";

/**
 * Parser for handling CSV report data returned from the WASM worker.
 * Uses the same parsing logic as the legacy backend.
 */
class ReportDataParser {
  /**
   * Parse the response from the WASM worker.
   *
   * @param {string} response - The response string from worker containing status and CSV data.
   * @returns {Array<EngineResult>} Parsed engine results.
   * @throws {Error} If response indicates an error status.
   */
  static parseResponse(response) {
    const lines = response.split("\n");

    if (lines.length < 2) {
      throw new Error("Invalid response format: missing status line or data");
    }

    const status = lines[0].trim();
    if (status !== "OK") {
      throw new Error(status);
    }

    // Skip empty line after status
    const csvData = lines.slice(2).join("\n").trim();

    if (!csvData) {
      return [];
    }

    // Check if we have only headers (for testing/placeholder mode)
    const csvLines = csvData.split("\n").filter((line) => line.trim());
    if (csvLines.length <= 1) {
      return []; // Only headers, no data
    }

    return ReportDataParser._parseCsvData(csvData);
  }

  /**
   * Parse CSV data into EngineResult objects.
   *
   * @private
   * @param {string} csvData - The CSV data to parse.
   * @returns {Array<EngineResult>} Array of parsed engine results.
   */
  static _parseCsvData(csvData) {
    const lines = csvData.split("\n").filter((line) => line.trim());

    if (lines.length === 0) {
      return [];
    }

    // Parse header to understand column structure
    const headers = lines[0].split(",").map((h) => h.trim());
    const results = [];

    for (let i = 1; i < lines.length; i++) {
      const values = lines[i].split(",").map((v) => v.trim());

      if (values.length !== headers.length) {
        continue; // Skip malformed rows
      }

      const row = {};
      headers.forEach((header, index) => {
        row[header] = values[index];
      });

      try {
        const engineResult = ReportDataParser._createEngineResult(row);
        results.push(engineResult);
      } catch (e) {
        console.warn("Failed to parse row:", row, e);
        // Continue parsing other rows
      }
    }

    return results;
  }

  /**
   * Create an EngineResult from a parsed CSV row.
   *
   * @private
   * @param {Object} row - The parsed CSV row data.
   * @returns {EngineResult} The created engine result.
   */
  static _createEngineResult(row) {
    // Helper function to parse Java EngineNumber.toString() format: "value units"
    const parseEngineNumber = (valueStr, defaultUnits = "units") => {
      if (!valueStr || valueStr.trim() === "") {
        return new EngineNumber(0, defaultUnits);
      }
      const parts = valueStr.trim().split(/\s+/);
      if (parts.length >= 2) {
        // Format: "value units" - preserve original value string
        const value = parseFloat(parts[0]) || 0;
        const units = parts.slice(1).join(" "); // Handle multi-word units
        const originalValueString = parts[0]; // Preserve original formatting
        return new EngineNumber(value, units, originalValueString);
      } else {
        // Only value, use default units - preserve original value string
        const value = parseFloat(parts[0]) || 0;
        const originalValueString = parts[0]; // Preserve original formatting
        return new EngineNumber(value, defaultUnits, originalValueString);
      }
    };

    // Extract fields matching Java CSV format
    const application = row["application"] || "";
    const substance = row["substance"] || "";
    const year = parseInt(row["year"] || "0");
    const scenarioName = row["scenario"] || ""; // Java uses "scenario", not "scenarioName"
    const trialNumber = parseInt(row["trial"] || "0"); // Java uses "trial", not "trialNumber"

    // Parse EngineNumber fields from Java's "value units" format
    const domesticValue = parseEngineNumber(row["domestic"], "kg");
    const importValue = parseEngineNumber(row["import"], "kg");
    const exportValue = parseEngineNumber(row["export"], "kg");
    const recycleValue = parseEngineNumber(row["recycle"], "kg");
    const domesticConsumptionValue = parseEngineNumber(row["domesticConsumption"], "tCO2e");
    const importConsumptionValue = parseEngineNumber(row["importConsumption"], "tCO2e");
    const exportConsumptionValue = parseEngineNumber(row["exportConsumption"], "tCO2e");
    const recycleConsumptionValue = parseEngineNumber(row["recycleConsumption"], "tCO2e");
    const populationValue = parseEngineNumber(row["population"], "units");
    const populationNew = parseEngineNumber(row["populationNew"], "units");
    const rechargeEmissions = parseEngineNumber(row["rechargeEmissions"], "tCO2e");
    const eolEmissions = parseEngineNumber(row["eolEmissions"], "tCO2e");
    const initialChargeEmissions = parseEngineNumber(row["initialChargeEmissions"], "tCO2e");
    const energyConsumption = parseEngineNumber(row["energyConsumption"], "kwh");

    // Parse bank fields from Java CSV
    const bankKg = parseEngineNumber(row["bankKg"], "kg");
    const bankTco2e = parseEngineNumber(row["bankTCO2e"], "tCO2e");
    const bankChangeKg = parseEngineNumber(row["bankChangeKg"], "kg");
    const bankChangeTco2e = parseEngineNumber(row["bankChangeTCO2e"], "tCO2e");

    // Handle TradeSupplement fields from Java CSV
    const importInitialChargeValue = parseEngineNumber(
      row["importInitialChargeValue"],
      "kg",
    );
    const importInitialChargeConsumption = parseEngineNumber(
      row["importInitialChargeConsumption"],
      "tCO2e",
    );
    const importPopulation = parseEngineNumber(row["importPopulation"], "units");
    const exportInitialChargeValue = parseEngineNumber(
      row["exportInitialChargeValue"],
      "kg",
    );
    const exportInitialChargeConsumption = parseEngineNumber(
      row["exportInitialChargeConsumption"],
      "tCO2e",
    );

    // Create tradeSupplement object using the TradeSupplement class
    const tradeSupplement = new TradeSupplement(
      importInitialChargeValue,
      importInitialChargeConsumption,
      importPopulation,
      exportInitialChargeValue,
      exportInitialChargeConsumption,
    );

    return new EngineResult(
      application,
      substance,
      year,
      scenarioName,
      trialNumber,
      domesticValue,
      importValue,
      exportValue,
      recycleValue,
      domesticConsumptionValue,
      importConsumptionValue,
      exportConsumptionValue,
      recycleConsumptionValue,
      populationValue,
      populationNew,
      rechargeEmissions,
      eolEmissions,
      initialChargeEmissions,
      energyConsumption,
      tradeSupplement,
      bankKg,
      bankTco2e,
      bankChangeKg,
      bankChangeTco2e,
    );
  }
}

/**
 * Web worker layer for managing communication with the WASM execution worker.
 */
class WasmLayer {
  /**
   * Create a new WasmLayer instance.
   *
   * @param {Function} reportProgressCallback - Callback for progress updates.
   * @param {number|null} poolSize - Optional worker pool size. If null, auto-calculated
   *                                  from navigator.hardwareConcurrency.
   */
  constructor(reportProgressCallback, poolSize = null) {
    const self = this;

    /**
     * Number of workers in the pool.
     * @private
     * @type {number}
     */
    // Calculate optimal pool size: (CPU cores - 1) with minimum of 2
    if (poolSize === null) {
      const cores = navigator.hardwareConcurrency || 4;
      self._poolSize = Math.max(2, cores - 1);
    } else {
      self._poolSize = Math.max(2, poolSize);
    }

    /**
     * Array of Web Worker instances for parallel execution.
     * @private
     * @type {Array<Worker>}
     */
    self._workers = [];

    /**
     * Round-robin index for distributing tasks across workers.
     * @private
     * @type {number}
     */
    self._nextWorkerIndex = 0;

    self._initPromise = null;
    self._pendingRequests = new Map();
    self._nextRequestId = 1;
    self._reportProgressCallback = reportProgressCallback;
  }

  /**
   * Initialize the worker pool and prepare for execution.
   *
   * @returns {Promise<void>} Promise that resolves when all workers are ready.
   */
  initialize() {
    const self = this;

    if (self._initPromise !== null) {
      return self._initPromise;
    }

    self._initPromise = new Promise((resolve, reject) => {
      try {
        // Create worker pool
        for (let i = 0; i < self._poolSize; i++) {
          const worker = new Worker("/js/wasm.worker.js?v=EPOCH");

          worker.onmessage = (event) => {
            self._handleWorkerMessage(event);
          };

          worker.onerror = (error) => {
            console.error(`WASM Worker ${i} error:`, error);
            reject(new Error(`WASM Worker ${i} failed to load: ${error.message}`));
          };

          self._workers.push(worker);
        }

        console.log(`WASM worker pool initialized with ${self._poolSize} workers`);

        // Workers are ready immediately - WASM initialization happens inside each worker
        resolve();
      } catch (error) {
        reject(error);
      }
    });

    return self._initPromise;
  }

  /**
   * Execute QubecTalk code and return backend result.
   *
   * @param {string} code - The QubecTalk code to execute.
   * @param {string[]} scenarioNames - Array of scenario names to execute.
   * @param {Object.<string, number>} scenarioTrialCounts - Map of scenario name to trial count.
   * @returns {Promise<BackendResult>} Promise resolving to backend result with CSV and parsed data.
   */
  async runSimulation(code, scenarioNames, scenarioTrialCounts) {
    const self = this;

    await self.initialize();

    return new Promise((resolve, reject) => {
      const requestId = self._nextRequestId++;

      // Create scenario completion tracker
      const scenarioTracker = {};
      scenarioNames.forEach((name) => {
        // Use "null" as key for null/undefined scenario names (legacy compatibility)
        const key = name !== null ? name : "null";
        scenarioTracker[key] = false;
      });

      // Calculate total trials across all scenarios
      const totalTrials = Object.values(scenarioTrialCounts).reduce((sum, count) => sum + count, 0);

      // Store request with scenario tracking and progress tracking
      self._pendingRequests.set(requestId, {
        resolve,
        reject,
        scenarioTracker: scenarioTracker,
        results: [], // Accumulate results from each scenario
        csvParts: [], // Accumulate CSV parts from each scenario
        code: code,
        remainingScenarios: scenarioNames.length,
        // Progress tracking fields
        scenarioTrialCounts: scenarioTrialCounts,
        totalTrials: totalTrials,
        scenarioProgressMap: {}, // Track current progress per scenario
      });

      // Send requests for each scenario to workers in round-robin fashion
      scenarioNames.forEach((scenarioName, index) => {
        // Select worker using round-robin
        const workerIndex = self._nextWorkerIndex % self._poolSize;
        const worker = self._workers[workerIndex];
        self._nextWorkerIndex++;

        worker.postMessage({
          id: requestId,
          command: "execute",
          code: code,
          scenarioName: scenarioName,
        });
      });

      // Set timeout for long-running simulations
      setTimeout(() => {
        if (self._pendingRequests.has(requestId)) {
          self._pendingRequests.delete(requestId);
          reject(new Error("Simulation timeout"));
        }
      }, 45000 * scenarioNames.length); // Scale timeout with number of scenarios
    });
  }

  /**
   * Handle messages from the worker.
   *
   * @private
   * @param {MessageEvent} event - The message event from worker.
   */
  _handleWorkerMessage(event) {
    const self = this;
    const {resultType, id, success, result, error, progress, scenarioName} = event.data;

    // Handle progress messages
    if (resultType === "progress") {
      const request = self._pendingRequests.get(id);
      if (!request) {
        return; // Progress for unknown/completed request
      }

      // Update per-scenario progress
      const scenarioKey = scenarioName || "null";
      request.scenarioProgressMap[scenarioKey] = progress || 0;

      // Calculate overall progress: weighted average by trial count
      let completedTrials = 0;
      Object.keys(request.scenarioProgressMap).forEach((key) => {
        const scenarioProgress = request.scenarioProgressMap[key];
        const trialCount = request.scenarioTrialCounts[key] || 1;
        completedTrials += scenarioProgress * trialCount;
      });

      const overallProgress = request.totalTrials > 0 ? completedTrials / request.totalTrials : 0;

      // Report aggregated progress to callback
      if (self._reportProgressCallback) {
        self._reportProgressCallback(overallProgress);
      }
      return;
    }

    // Handle regular result messages
    const request = self._pendingRequests.get(id);
    if (!request) {
      console.warn("Received response for unknown request:", id);
      return;
    }

    if (!success) {
      self._pendingRequests.delete(id);
      request.reject(new Error(error || "Unknown WASM worker error"));
      return;
    }

    try {
      // Parse results from this scenario
      const parsedResults = ReportDataParser.parseResponse(result);

      // Extract CSV string from the response
      const lines = result.split("\n");
      const csvData = lines.slice(2).join("\n").trim();

      // Accumulate results
      request.results.push(...parsedResults);

      // Store CSV data (we'll combine them later)
      if (csvData) {
        request.csvParts.push(csvData);
      }

      // Mark scenario as complete
      if (request.scenarioTracker) {
        // For null/undefined scenario names (legacy path), mark as complete immediately
        const key = scenarioName || "null";
        if (request.scenarioTracker[key] !== undefined) {
          request.scenarioTracker[key] = true;
        }
        request.remainingScenarios--;
      }

      // Check if all scenarios are complete
      const allComplete = request.remainingScenarios === 0;

      if (allComplete) {
        // Combine CSV parts - use first part's header, then all data rows
        let combinedCsv = "";
        if (request.csvParts.length > 0) {
          const firstPart = request.csvParts[0];
          const firstLines = firstPart.split("\n");
          const header = firstLines[0];

          // Start with header
          const allDataRows = [header];

          // Add data rows from all parts
          request.csvParts.forEach((part) => {
            const partLines = part.split("\n");
            // Skip header (first line) and add data rows
            for (let i = 1; i < partLines.length; i++) {
              if (partLines[i].trim()) {
                allDataRows.push(partLines[i]);
              }
            }
          });

          combinedCsv = allDataRows.join("\n");
        }

        // Create BackendResult with combined data
        const backendResult = new BackendResult(combinedCsv, request.results);

        self._pendingRequests.delete(id);
        request.resolve(backendResult);
      }
    } catch (parseError) {
      self._pendingRequests.delete(id);
      request.reject(parseError);
    }
  }

  /**
   * Terminate all workers and clean up resources.
   */
  terminate() {
    const self = this;

    if (self._workers && self._workers.length > 0) {
      self._workers.forEach((worker) => {
        worker.terminate();
      });
      self._workers = [];
    }
    self._nextWorkerIndex = 0;

    // Reject any pending requests
    for (const [id, request] of self._pendingRequests) {
      request.reject(new Error("WASM Worker terminated"));
    }
    self._pendingRequests.clear();

    self._initPromise = null;
  }
}

/**
 * WASM backend for executing QubecTalk simulations.
 *
 * This backend executes QubecTalk code in a WASM web worker for high-performance
 * execution and returns the results as parsed EngineResult objects.
 */
class WasmBackend {
  /**
   * Create a new WasmBackend instance.
   *
   * @param {WasmLayer} wasmLayer - The layer for worker communication.
   * @param {Function} reportProgressCallback - Callback for progress updates.
   */
  constructor(wasmLayer, reportProgressCallback) {
    const self = this;
    self._wasmLayer = wasmLayer;
    self._reportProgressCallback = reportProgressCallback;

    // Set the progress callback on the WASM layer
    if (self._wasmLayer && reportProgressCallback) {
      self._wasmLayer._reportProgressCallback = reportProgressCallback;
    }
  }

  /**
   * Execute QubecTalk simulation code.
   *
   * @param {string} simCode - The QubecTalk code to execute.
   * @returns {Promise<BackendResult>} Promise resolving to backend result with CSV and parsed data.
   */
  async execute(simCode) {
    const self = this;

    try {
      // Parse code to extract scenario names using UiTranslatorCompiler
      const compiler = new UiTranslatorCompiler();
      const translationResult = compiler.compile(simCode);

      if (translationResult.getErrors().length > 0) {
        throw new Error("Syntax error in code: " + translationResult.getErrors().join("; "));
      }

      const program = translationResult.getProgram();
      const scenarioNames = program.getScenarioNames();

      // If no scenario names found (e.g., code uses "across X trials" syntax),
      // fall back to legacy behavior by passing single empty name to trigger
      // the worker's backward compatibility path
      if (!scenarioNames || scenarioNames.length === 0) {
        // Legacy path: send without scenario name to execute all scenarios at once
        // Use default trial count of 1 for progress tracking
        const scenarioTrialCounts = {"null": 1};
        const backendResult = await self._wasmLayer.runSimulation(
          simCode,
          [null],
          scenarioTrialCounts,
        );
        return backendResult;
      }

      // Extract trial counts for each scenario (default to 1 trial for UI-compatible scenarios)
      const scenarioTrialCounts = {};
      scenarioNames.forEach((scenarioName) => {
        // UI-compatible scenarios (without "across X trials") always have 1 trial
        scenarioTrialCounts[scenarioName] = 1;
      });

      // Execute all scenarios in parallel
      const backendResult = await self._wasmLayer.runSimulation(
        simCode,
        scenarioNames,
        scenarioTrialCounts,
      );
      return backendResult;
    } catch (error) {
      throw new Error("WASM simulation execution failed: " + error.message);
    }
  }
}

/**
 * Result object containing both CSV string and parsed results from backend execution.
 */
class BackendResult {
  /**
   * Create a new BackendResult instance.
   *
   * @param {string} csvString - The raw CSV string from the backend.
   * @param {Array<EngineResult>} parsedResults - The parsed engine results.
   */
  constructor(csvString, parsedResults) {
    const self = this;
    self._csvString = csvString;
    self._parsedResults = parsedResults;
  }

  /**
   * Get the raw CSV string from the backend.
   *
   * @returns {string} The CSV string.
   */
  getCsvString() {
    const self = this;
    return self._csvString;
  }

  /**
   * Get the parsed results array.
   *
   * @returns {Array<EngineResult>} The parsed engine results.
   */
  getParsedResults() {
    const self = this;
    return self._parsedResults;
  }
}

export {WasmBackend, WasmLayer, ReportDataParser, BackendResult};
