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
    // Import TeaVM-compiled JavaScript version first (for fallback)
    importScripts("/wasm/KigaliSim.js");
    importScripts("/wasm/KigaliSim.wasm-runtime.js");

    // Try to load WASM
    wasmLayer = await TeaVM.wasmGC.load("/wasm/KigaliSim.wasm");
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

    // The Java facade already returns the properly formatted string: "OK\n\nCSV"
    // So we just return the result as-is
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
