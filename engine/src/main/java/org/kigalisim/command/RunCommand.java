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
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.ProgressReportCallback;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.parse.ParseResult;
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
  private static final int INVALID_REPLICATES_ERROR = 6;

  @Parameters(index = "0", description = "Path to QubecTalk file to run")
  private File file;

  @Option(names = {"-o", "--output"}, description = "Path to CSV output file", required = true)
  private File csvOutputFile;

  @Option(names = {"-r", "--parallel-replicates"}, description = "Number of times to run each scenario (default: 1)", defaultValue = "1")
  private int replicates;

  @Override
  public Integer call() {
    if (!file.exists()) {
      System.err.println("Could not find file: " + file);
      return FILE_NOT_FOUND_ERROR;
    }

    if (replicates < 1) {
      System.err.println("Replicates must be at least 1, got: " + replicates);
      return INVALID_REPLICATES_ERROR;
    }

    try {
      // Read the file content
      String code = new String(Files.readAllBytes(file.toPath()));

      // Parse the code to get detailed error information
      ParseResult parseResult = KigaliSimFacade.parse(code);

      if (parseResult.hasErrors()) {
        String detailedError = KigaliSimFacade.getDetailedErrorMessage(parseResult);
        System.err.println("Failed to parse QubecTalk code at " + file);
        System.err.println(detailedError);
        return PARSE_ERROR;
      }

      // Interpret the parsed code
      ParsedProgram program;
      try {
        program = KigaliSimFacade.interpret(parseResult);
      } catch (Exception e) {
        System.err.println("Failed to interpret QubecTalk code at " + file);
        System.err.println("Interpretation error: " + e.getMessage());
        return PARSE_ERROR;
      }

      // Create progress callback that prints to stdout
      ProgressReportCallback progressCallback = progress -> {
        int percentage = (int) (progress * 100);
        System.out.print("\rProgress: " + percentage + "%");
        System.out.flush();
      };

      // Run all scenarios in the program with replicates and collect results
      Stream<EngineResult> allResults = program.getScenarios().stream()
          .flatMap(scenarioName ->
              IntStream.range(0, replicates)
                  .boxed()
                  .flatMap(replicateIndex -> {
                    if (replicates > 1) {
                      System.out.println("Running scenario '" + scenarioName + "' - replicate " + (replicateIndex + 1) + " of " + replicates);
                    }
                    return KigaliSimFacade.runScenario(program, scenarioName, progressCallback);
                  })
          );

      // Collect to a list to see how many results we have
      List<EngineResult> resultsList = allResults.collect(Collectors.toList());

      // Print a newline after progress is complete
      System.out.println();

      // Convert results to CSV and write to file
      String csvContent = KigaliSimFacade.convertResultsToCsv(resultsList);
      try (FileWriter writer = new FileWriter(csvOutputFile)) {
        writer.write(csvContent);
      } catch (IOException e) {
        System.err.println("Failed to write CSV output to " + csvOutputFile);
        System.err.println("Error: " + e.getMessage());
        return CSV_WRITE_ERROR;
      }

      System.out.println("Successfully ran all simulations and wrote results to " + csvOutputFile);
      return 0;
    } catch (IOException e) {
      System.err.println("Could not read file: " + file);
      System.err.println("Error: " + e.getMessage());
      return FILE_NOT_FOUND_ERROR;
    } catch (Exception e) {
      System.err.println("Error running simulation: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      if (e.getCause() != null) {
        System.err.println("Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
      }
      return EXECUTION_ERROR;
    }
  }

}
