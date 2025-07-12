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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.ProgressReportCallback;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;
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

      // Create progress callback that prints to stdout
      ProgressReportCallback progressCallback = progress -> {
        int percentage = (int) (progress * 100);
        System.out.print("\rProgress: " + percentage + "%");
        System.out.flush();
      };

      // Run all scenarios in the program and collect results
      Stream<EngineResult> allResults = program.getScenarios().stream()
          .flatMap(scenarioName -> KigaliSimFacade.runScenario(program, scenarioName, progressCallback));

      // Collect to a list to see how many results we have
      List<EngineResult> resultsList = allResults.collect(java.util.stream.Collectors.toList());

      // Print a newline after progress is complete
      System.out.println();

      // Convert results to CSV and write to file
      String csvContent = KigaliSimFacade.convertResultsToCsv(resultsList);
      try (FileWriter writer = new FileWriter(csvOutputFile)) {
        writer.write(csvContent);
      }

      System.out.println("Successfully ran all simulations and wrote results to " + csvOutputFile);
      return 0;
    } catch (Exception e) {
      System.err.println("Error running simulation: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      if (e.getCause() != null) {
        System.err.println("Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
      }
      e.printStackTrace();
      return EXECUTION_ERROR;
    }
  }

}
