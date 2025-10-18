/**
 * WASM web worker for executing QubecTalk simulations using Java backend.
 *
 * This worker provides the same interface as legacy.worker.js but uses
 * the Java-based QubecTalk interpreter compiled to WASM with JS fallback
 * for legacy browser support.
 *
 * @license BSD, see LICENSE.md.
 */

let wasmLayer = null;
let isInitialized = false;

// Context variables for progress reporting
let currentRequestId = null;
let currentScenarioName = null;

/**
 * Global reportProgress function called by WASM to report simulation progress.
 * This function is available globally for the Java code to call via @JSBody.
 *
 * @param {number} progress - Progress value between 0.0 and 1.0
 */
function reportProgress(progress) {
  self.postMessage({
    resultType: "progress",
    id: currentRequestId,
    scenarioName: currentScenarioName,
    progress: progress,
  });
}

// Load WASM files with fallback to JS
async function initializeWasm() {
  if (isInitialized) {
    return;
  }

  try {
    // Import TeaVM-compiled JavaScript version first (for fallback)
    importScripts("/wasm/KigaliSim.js?v=EPOCH");
    importScripts("/wasm/KigaliSim.wasm-runtime.js?v=EPOCH");

    // Try to load WASM
    wasmLayer = await TeaVM.wasmGC.load("/wasm/KigaliSim.wasm?v=EPOCH");
    console.log("WASM backend initialized successfully");
  } catch (error) {
    console.log("Failed to load WASM, falling back to TeaVM JavaScript:", error);

    // Fallback to TeaVM-compiled JavaScript implementation
    wasmLayer = {
      exports: {
        execute: execute, // Use the TeaVM-compiled execute function
        getVersion: getVersion, // Also available if needed
      },
    };
    console.log("TeaVM JavaScript fallback initialized successfully");
  }

  isInitialized = true;
}


/**
 * Execute QubecTalk code using WASM backend.
 *
 * @param {string} code - The QubecTalk code to execute.
 * @param {string} scenarioName - Scenario name to execute, or empty string for all scenarios.
 * @returns {Promise<string>} Promise resolving to status + CSV results.
 */
async function executeCode(code, scenarioName) {
  try {
    // Ensure WASM is initialized
    await initializeWasm();

    if (!wasmLayer) {
      throw new Error("WASM layer not initialized");
    }

    // Set scenario name for progress reporting
    currentScenarioName = scenarioName;

    // Determine execution mode
    const scenarioValid = scenarioName !== null && scenarioName !== undefined;
    const executeSingleScenario = scenarioValid && scenarioName !== "";

    let result;
    if (executeSingleScenario) {
      const execScenAvailLocal = typeof executeScenario === "function";
      const execScenExported = wasmLayer.exports && wasmLayer.exports.executeScenario;

      if (execScenAvailLocal) {
        result = executeScenario(code, scenarioName);
      } else if (execScenExported) {
        result = wasmLayer.exports.executeScenario(code, scenarioName);
      } else {
        throw new Error("executeScenario function not found in WASM");
      }
    } else {
      const execAvailLocal = typeof execute === "function";
      const execExported = wasmLayer.exports && wasmLayer.exports.execute;

      if (execAvailLocal) {
        result = execute(code);
      } else if (execExported) {
        result = wasmLayer.exports.execute(code);
      } else {
        throw new Error("Execute function not found in WASM");
      }
    }

    return result;
  } catch (error) {
    console.error("WASM execution error:", error);
    return `Execution Error: ${error.message}\n\n`;
  }
}

/**
 * Handle messages from the main thread.
 */
self.onmessage = async function (event) {
  const {id, command, code, scenarioName} = event.data;

  // Set request ID for progress reporting
  currentRequestId = id;

  try {
    if (command === "execute") {
      const result = await executeCode(code, scenarioName || "");

      self.postMessage({
        resultType: "dataset",
        id: id,
        scenarioName: scenarioName, // Include scenario name in response
        success: true,
        result: result,
      });
    } else {
      self.postMessage({
        resultType: "dataset",
        id: id,
        success: false,
        error: `Unknown command: ${command}`,
      });
    }
  } catch (error) {
    self.postMessage({
      resultType: "dataset",
      id: id,
      success: false,
      error: error.message,
    });
  }
};
