/**
 * Command line interface handler for running QubecTalk simulations.
 *
 * <p>This class implements the 'run' command which executes a specified simulation from a QubecTalk
 * script file.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;
import org.kigalisim.lang.program.ParsedPolicy;
import org.kigalisim.lang.program.ParsedApplication;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command handler for executing QubecTalk simulations.
 *
 * <p>Processes command line arguments to run a specified simulation from a QubecTalk script file.</p>
 */
@Command(
    name = "run",
    description = "Run a simulation file"
)
public class RunCommand implements Callable<Integer> {
  private static final int FILE_NOT_FOUND_ERROR = 1;
  private static final int PARSE_ERROR = 2;
  private static final int SIMULATION_NOT_FOUND_ERROR = 3;
  private static final int EXECUTION_ERROR = 4;
  private static final int CSV_WRITE_ERROR = 5;

  @Parameters(index = "0", description = "Path to QubecTalk file to run")
  private File file;

  @Option(names = {"-o", "--output"}, description = "Path to CSV output file", required = true)
  private File csvOutputFile;

  @Override
  public Integer call() {
    if (!file.exists()) {
      System.err.println("Could not find file: " + file);
      return FILE_NOT_FOUND_ERROR;
    }

    try {
      // Parse and interpret the file
      ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());

      // Debug: print program info
      System.out.println("Scenarios: " + program.getScenarios());
      ParsedPolicy defaultPolicy = program.getPolicy("default");
      if (defaultPolicy != null) {
        System.out.println("Default policy applications: " + defaultPolicy.getApplications());
        for (String appName : defaultPolicy.getApplications()) {
          ParsedApplication app = defaultPolicy.getApplication(appName);
          System.out.println("Application " + appName + " substances: " + app.getSubstances());
        }
      }

      // Run all scenarios in the program and collect results
      Stream<EngineResult> allResults = program.getScenarios().stream()
          .flatMap(scenarioName -> KigaliSimFacade.runScenarioWithResults(program, scenarioName));

      // Collect to a list to see how many results we have
      List<EngineResult> resultsList = allResults.collect(java.util.stream.Collectors.toList());
      System.out.println("Collected " + resultsList.size() + " results");

      // Write results to CSV
      writeResultsToCsv(resultsList.stream(), csvOutputFile);

      System.out.println("Successfully ran all simulations and wrote results to " + csvOutputFile);
      return 0;
    } catch (Exception e) {
      System.err.println("Error running simulation: " + e.getMessage());
      return EXECUTION_ERROR;
    }
  }

  /**
   * Write EngineResult objects to CSV file.
   *
   * @param results Stream of EngineResult objects to write
   * @param outputFile File to write CSV data to
   * @throws IOException If there is an error writing to the file
   */
  private void writeResultsToCsv(Stream<EngineResult> results, File outputFile) throws IOException {
    try (FileWriter fileWriter = new FileWriter(outputFile);
         CSVPrinter printer = new CSVPrinter(fileWriter, CSVFormat.DEFAULT)) {

      // Write header
      printer.printRecord("Application", "Substance", "Year", "Manufacture", "Import",
          "Recycle", "DomesticConsumption", "ImportConsumption",
          "RecycleConsumption", "Population", "PopulationNew",
          "RechargeEmissions", "EolEmissions", "EnergyConsumption");

      // Write data rows
      results.forEach(result -> {
        try {
          Map<String, String> row = new LinkedHashMap<>();
          row.put("Application", result.getApplication());
          row.put("Substance", result.getSubstance());
          row.put("Year", String.valueOf(result.getYear()));
          row.put("Manufacture", result.getManufacture().toString());
          row.put("Import", result.getImport().toString());
          row.put("Recycle", result.getRecycle().toString());
          row.put("DomesticConsumption", result.getDomesticConsumption().toString());
          row.put("ImportConsumption", result.getImportConsumption().toString());
          row.put("RecycleConsumption", result.getRecycleConsumption().toString());
          row.put("Population", result.getPopulation().toString());
          row.put("PopulationNew", result.getPopulationNew().toString());
          row.put("RechargeEmissions", result.getRechargeEmissions().toString());
          row.put("EolEmissions", result.getEolEmissions().toString());
          row.put("EnergyConsumption", result.getEnergyConsumption().toString());

          printer.printRecord(row.values());
        } catch (IOException e) {
          throw new RuntimeException("Error writing CSV row", e);
        }
      });
    }
  }
}
