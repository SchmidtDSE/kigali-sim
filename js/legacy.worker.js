/**
 * Web worker for executing QubecTalk simulations in isolation.
 *
 * This worker receives QubecTalk code and executes it using the existing
 * compiler and engine, returning results as CSV format.
 *
 * @license BSD, see LICENSE.md.
 */

// Import the global QubecTalk toolkit
importScripts("/intermediate/static/qubectalk.js");

// For now, let's use a simpler approach that just validates compilation
// and returns a simple response to prove the worker architecture works.
// This allows us to verify the worker pattern is functional before
// tackling the module loading complexity.

// Simple compilation test without full execution
function validateCode(code) {
  try {
    const toolkit = QubecTalk.getToolkit();
    const antlr4 = toolkit.antlr4;
    const QubecTalkLexer = toolkit.QubecTalkLexer;
    const QubecTalkParser = toolkit.QubecTalkParser;

    // Basic lexical and syntactic validation
    const input = new antlr4.InputStream(code);
    const lexer = new QubecTalkLexer(input);
    const tokens = new antlr4.CommonTokenStream(lexer);
    const parser = new QubecTalkParser(tokens);

    // Parse to check for syntax errors
    const tree = parser.program();

    return {valid: true, errors: []};
  } catch (error) {
    return {valid: false, errors: [error.message]};
  }
}

/**
 * Convert EngineResult array to CSV format.
 *
 * @param {Array<EngineResult>} results - The engine results to convert.
 * @returns {string} CSV representation of the results.
 */
function convertResultsToCSV(results) {
  if (!results || results.length === 0) {
    return "";
  }

  // Define CSV headers
  const headers = [
    "application",
    "substance",
    "year",
    "scenarioName",
    "trialNumber",
    "manufacture",
    "manufactureUnits",
    "import",
    "importUnits",
    "recycle",
    "recycleUnits",
    "domesticConsumption",
    "domesticConsumptionUnits",
    "importConsumption",
    "importConsumptionUnits",
    "recycleConsumption",
    "recycleConsumptionUnits",
    "population",
    "populationUnits",
    "populationNew",
    "populationNewUnits",
    "rechargeEmissions",
    "rechargeEmissionsUnits",
    "eolEmissions",
    "eolEmissionsUnits",
    "energyConsumption",
    "energyConsumptionUnits",
  ];

  // Build CSV rows
  const rows = [headers.join(",")];

  for (const result of results) {
    const row = [
      `"${result.getApplication()}"`,
      `"${result.getSubstance()}"`,
      result.getYear(),
      `"${result.getScenarioName()}"`,
      result.getTrialNumber(),
      result.getManufacture().getValue(),
      `"${result.getManufacture().getUnits()}"`,
      result.getImport().getValue(),
      `"${result.getImport().getUnits()}"`,
      result.getRecycle().getValue(),
      `"${result.getRecycle().getUnits()}"`,
      result.getDomesticConsumption().getValue(),
      `"${result.getDomesticConsumption().getUnits()}"`,
      result.getImportConsumption().getValue(),
      `"${result.getImportConsumption().getUnits()}"`,
      result.getRecycleConsumption().getValue(),
      `"${result.getRecycleConsumption().getUnits()}"`,
      result.getPopulation().getValue(),
      `"${result.getPopulation().getUnits()}"`,
      result.getPopulationNew().getValue(),
      `"${result.getPopulationNew().getUnits()}"`,
      result.getRechargeEmissions().getValue(),
      `"${result.getRechargeEmissions().getUnits()}"`,
      result.getEolEmissions().getValue(),
      `"${result.getEolEmissions().getUnits()}"`,
      result.getEnergyConsumption().getValue(),
      `"${result.getEnergyConsumption().getUnits()}"`,
    ];

    rows.push(row.join(","));
  }

  return rows.join("\n");
}

/**
 * Execute QubecTalk code and return results.
 *
 * For now, this is a placeholder that validates syntax and returns empty results
 * to test the worker architecture. Full execution will be implemented once
 * module loading issues are resolved.
 *
 * @param {string} code - The QubecTalk code to execute.
 * @returns {Promise<string>} Promise resolving to status + CSV results.
 */
async function executeCode(code) {
  try {
    // Validate the code syntax
    const validation = validateCode(code);

    if (!validation.valid) {
      return `Compilation Error: ${validation.errors[0]}\n\n`;
    }

    // For now, return empty results to test the architecture
    // TODO: Implement full execution once module loading is resolved
    const csvData = [
      "application", "substance", "year", "scenarioName", "trialNumber",
      "manufacture", "manufactureUnits", "import", "importUnits",
      "recycle", "recycleUnits", "domesticConsumption", "domesticConsumptionUnits",
      "importConsumption", "importConsumptionUnits",
      "recycleConsumption", "recycleConsumptionUnits",
      "population", "populationUnits", "populationNew", "populationNewUnits",
      "rechargeEmissions", "rechargeEmissionsUnits", "eolEmissions", "eolEmissionsUnits",
      "energyConsumption", "energyConsumptionUnits",
    ].join(",");

    // Return status + CSV
    return `OK\n\n${csvData}`;
  } catch (error) {
    console.error("Worker execution error:", error);
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
