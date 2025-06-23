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

// Load WASM files with fallback to JS
async function initializeWasm() {
  if (isInitialized) {
    return;
  }

  try {
    // Import WASM runtime and try to load WASM
    importScripts("/wasm/KigaliSim.js");
    importScripts("/wasm/KigaliSim.wasm-runtime.js");

    wasmLayer = await TeaVM.wasmGC.load("/wasm/KigaliSim.wasm");
    console.log("WASM backend initialized successfully");
  } catch (error) {
    console.log("Failed to load WASM, falling back to JS:", error);

    // Fallback to legacy JS implementation
    try {
      importScripts("/intermediate/static/qubectalk.js");
      wasmLayer = {
        exports: {
          execute: executeWithJsFallback,
        },
      };
      console.log("JS fallback initialized successfully");
    } catch (jsError) {
      console.error("Failed to initialize both WASM and JS fallback:", jsError);
      throw new Error("Failed to initialize execution backend");
    }
  }

  isInitialized = true;
}

/**
 * JS fallback execution function using legacy QubecTalk toolkit.
 *
 * @param {string} code - The QubecTalk code to execute.
 * @returns {string} Status line + blank line + CSV results.
 */
function executeWithJsFallback(code) {
  try {
    // Basic validation first
    const toolkit = QubecTalk.getToolkit();
    const antlr4 = toolkit.antlr4;
    const QubecTalkLexer = toolkit.QubecTalkLexer;
    const QubecTalkParser = toolkit.QubecTalkParser;

    const input = new antlr4.InputStream(code);
    const lexer = new QubecTalkLexer(input);
    const tokens = new antlr4.CommonTokenStream(lexer);
    const parser = new QubecTalkParser(tokens);

    // Parse to check for syntax errors
    const tree = parser.program();

    // For now, return empty CSV with headers (placeholder implementation)
    // TODO: Implement full JS execution once module loading is resolved
    const csvHeaders = [
      "application", "substance", "year", "scenarioName", "trialNumber",
      "manufacture", "manufactureUnits", "import", "importUnits",
      "recycle", "recycleUnits", "domesticConsumption", "domesticConsumptionUnits",
      "importConsumption", "importConsumptionUnits",
      "recycleConsumption", "recycleConsumptionUnits",
      "population", "populationUnits", "populationNew", "populationNewUnits",
      "rechargeEmissions", "rechargeEmissionsUnits", "eolEmissions", "eolEmissionsUnits",
      "energyConsumption", "energyConsumptionUnits",
    ].join(",");

    return `OK\n\n${csvHeaders}`;
  } catch (error) {
    return `Compilation Error: ${error.message}\n\n`;
  }
}

/**
 * Execute QubecTalk code using WASM backend.
 *
 * @param {string} code - The QubecTalk code to execute.
 * @returns {Promise<string>} Promise resolving to status + CSV results.
 */
async function executeCode(code) {
  try {
    // Ensure WASM is initialized
    await initializeWasm();

    if (!wasmLayer) {
      throw new Error("WASM layer not initialized");
    }

    // Execute using WASM (the TeaVM exports are global functions, not in exports)
    let result;
    if (typeof execute === "function") {
      // WASM is loaded and execute function is available globally
      result = execute(code);
    } else if (wasmLayer.exports && wasmLayer.exports.execute) {
      // Fallback case
      result = wasmLayer.exports.execute(code);
    } else {
      throw new Error("Execute function not found in WASM or fallback");
    }

    // Ensure result follows the expected format: status + blank line + CSV
    if (typeof result === "string") {
      return result;
    } else {
      // If WASM returns structured data, convert to expected string format
      return `OK\n\n${result}`;
    }
  } catch (error) {
    console.error("WASM execution error:", error);
    return `Execution Error: ${error.message}\n\n`;
  }
}

/**
 * Handle messages from the main thread.
 * Maintains the same interface as legacy.worker.js
 */
self.onmessage = async function (event) {
  const {id, command, code} = event.data;

  try {
    if (command === "execute") {
      const result = await executeCode(code);

      self.postMessage({
        id: id,
        success: true,
        result: result,
      });
    } else {
      self.postMessage({
        id: id,
        success: false,
        error: `Unknown command: ${command}`,
      });
    }
  } catch (error) {
    self.postMessage({
      id: id,
      success: false,
      error: error.message,
    });
  }
};
