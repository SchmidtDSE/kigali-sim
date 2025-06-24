/**
 * WASM backend for executing QubecTalk simulations via WASM web worker.
 *
 * @license BSD, see LICENSE.md.
 */

import {EngineNumber} from "engine_number";
import {EngineResult} from "engine_struct";

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
        // Format: "value units"
        const value = parseFloat(parts[0]) || 0;
        const units = parts.slice(1).join(" "); // Handle multi-word units
        return new EngineNumber(value, units);
      } else {
        // Only value, use default units
        const value = parseFloat(parts[0]) || 0;
        return new EngineNumber(value, defaultUnits);
      }
    };

    // Extract fields matching Java CSV format
    const application = row["application"] || "";
    const substance = row["substance"] || "";
    const year = parseInt(row["year"] || "0");
    const scenarioName = row["scenario"] || ""; // Java uses "scenario", not "scenarioName"
    const trialNumber = parseInt(row["trial"] || "0"); // Java uses "trial", not "trialNumber"

    // Parse EngineNumber fields from Java's "value units" format
    const manufactureValue = parseEngineNumber(row["manufacture"], "kg");
    const importValue = parseEngineNumber(row["import"], "kg");
    const recycleValue = parseEngineNumber(row["recycle"], "kg");
    const domesticConsumptionValue = parseEngineNumber(row["domesticConsumption"], "tCO2e");
    const importConsumptionValue = parseEngineNumber(row["importConsumption"], "tCO2e");
    const recycleConsumptionValue = parseEngineNumber(row["recycleConsumption"], "tCO2e");
    const populationValue = parseEngineNumber(row["population"], "units");
    const populationNew = parseEngineNumber(row["populationNew"], "units");
    const rechargeEmissions = parseEngineNumber(row["rechargeEmissions"], "tCO2e");
    const eolEmissions = parseEngineNumber(row["eolEmissions"], "tCO2e");
    const energyConsumption = parseEngineNumber(row["energyConsumption"], "kwh");

    // Handle importSupplement fields from Java CSV
    const initialChargeValue = parseEngineNumber(row["initialChargeValue"], "kg");
    const initialChargeConsumption = parseEngineNumber(row["initialChargeConsumption"], "tCO2e");
    const importNewPopulation = parseEngineNumber(row["importNewPopulation"], "units");

    // Create importSupplement object
    const importSupplement = {
      getInitialChargeValue: () => initialChargeValue,
      getInitialChargeConsumption: () => initialChargeConsumption,
      getNewPopulation: () => importNewPopulation,
    };

    return new EngineResult(
      application,
      substance,
      year,
      scenarioName,
      trialNumber,
      manufactureValue,
      importValue,
      recycleValue,
      domesticConsumptionValue,
      importConsumptionValue,
      recycleConsumptionValue,
      populationValue,
      populationNew,
      rechargeEmissions,
      eolEmissions,
      energyConsumption,
      importSupplement,
    );
  }
}

/**
 * Web worker layer for managing communication with the WASM execution worker.
 */
class WasmLayer {
  /**
   * Create a new WasmLayer instance.
   */
  constructor() {
    const self = this;
    self._worker = null;
    self._initPromise = null;
    self._pendingRequests = new Map();
    self._nextRequestId = 1;
  }

  /**
   * Initialize the worker and prepare for execution.
   *
   * @returns {Promise<void>} Promise that resolves when worker is ready.
   */
  initialize() {
    const self = this;

    if (self._initPromise !== null) {
      return self._initPromise;
    }

    self._initPromise = new Promise((resolve, reject) => {
      try {
        self._worker = new Worker("/js/wasm.worker.js");

        self._worker.onmessage = (event) => {
          self._handleWorkerMessage(event);
        };

        self._worker.onerror = (error) => {
          console.error("WASM Worker error:", error);
          reject(new Error("WASM Worker failed to load: " + error.message));
        };

        // Worker is ready immediately - WASM initialization happens inside worker
        resolve();
      } catch (error) {
        reject(error);
      }
    });

    return self._initPromise;
  }

  /**
   * Execute QubecTalk code and return parsed results.
   *
   * @param {string} code - The QubecTalk code to execute.
   * @returns {Promise<Array<EngineResult>>} Promise resolving to execution results.
   */
  async runSimulation(code) {
    const self = this;

    await self.initialize();

    return new Promise((resolve, reject) => {
      const requestId = self._nextRequestId++;

      self._pendingRequests.set(requestId, {resolve, reject});

      self._worker.postMessage({
        id: requestId,
        command: "execute",
        code: code,
      });

      // Set timeout for long-running simulations
      setTimeout(() => {
        if (self._pendingRequests.has(requestId)) {
          self._pendingRequests.delete(requestId);
          reject(new Error("Simulation timeout"));
        }
      }, 30000); // 30 second timeout
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
    const {id, success, result, error} = event.data;

    const request = self._pendingRequests.get(id);
    if (!request) {
      console.warn("Received response for unknown request:", id);
      return;
    }

    self._pendingRequests.delete(id);

    if (success) {
      try {
        const parsedResults = ReportDataParser.parseResponse(result);
        request.resolve(parsedResults);
      } catch (parseError) {
        request.reject(parseError);
      }
    } else {
      request.reject(new Error(error || "Unknown WASM worker error"));
    }
  }

  /**
   * Terminate the worker and clean up resources.
   */
  terminate() {
    const self = this;

    if (self._worker) {
      self._worker.terminate();
      self._worker = null;
    }

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
   */
  constructor(wasmLayer) {
    const self = this;
    self._wasmLayer = wasmLayer;
  }

  /**
   * Execute QubecTalk simulation code.
   *
   * @param {string} simCode - The QubecTalk code to execute.
   * @returns {Promise<Array<EngineResult>>} Promise resolving to simulation results.
   */
  async execute(simCode) {
    const self = this;

    try {
      const results = await self._wasmLayer.runSimulation(simCode);
      return results;
    } catch (error) {
      throw new Error("WASM simulation execution failed: " + error.message);
    }
  }
}

export {WasmBackend, WasmLayer, ReportDataParser};
