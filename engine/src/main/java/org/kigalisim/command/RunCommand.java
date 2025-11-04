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
import java.util.Optional;
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

  /**
   * Constructs a new RunCommand.
   */
  public RunCommand() {
  }

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

  @Option(names = {"-r", "--replicates"}, description = "Number of times to run each scenario (default: 1)", defaultValue = "1")
  private int replicates;

  /**
   * Executes the run command.
   *
   * @return 0 on success, non-zero error code on failure
   */
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

    CommandInterpretResult interpretResult = interpret(file);
    if (interpretResult.getIsFailure()) {
      System.err.println(interpretResult.getErrorMessage().orElse("Unknown error"));
      return PARSE_ERROR;
    }

    ParsedProgram program = interpretResult.getProgram().orElseThrow();

    Stream<EngineResult> allResults = runAndStreamResults(program, x -> {});
    List<EngineResult> resultsList = allResults.collect(Collectors.toList());

    System.out.println();

    Optional<String> writeError = writeResultsToCsv(resultsList, csvOutputFile);
    if (writeError.isPresent()) {
      System.err.println(writeError.get());
      return CSV_WRITE_ERROR;
    }

    System.out.println("Successfully ran all simulations and wrote results to " + csvOutputFile);
    return 0;
  }

  /**
   * Runs all scenarios in the program and returns a stream of results.
   *
   * <p>This method executes each scenario in the parsed program with the specified number of
   * replicates, applying the given progress callback to track execution progress.</p>
   *
   * @param program The parsed program containing scenarios to run
   * @param progressCallback The callback for progress reporting
   * @return A stream of EngineResult objects from running all scenarios and replicates
   */
  private Stream<EngineResult> runAndStreamResults(ParsedProgram program, ProgressReportCallback progressCallback) {
    return program.getScenarios()
        .stream()
        .parallel()
        .flatMap(scenarioName ->
            IntStream.range(0, replicates)
                .boxed()
                .flatMap(replicateIndex -> {
                  System.out.println("Running scenario '" + scenarioName);
                  return KigaliSimFacade.runScenario(program, scenarioName, progressCallback);
                })
        );
  }

  /**
   * Interprets QubecTalk code from a file without exception handling.
   *
   * @param file The file containing QubecTalk code
   * @return The parsed program
   * @throws IOException If the file cannot be read
   * @throws RuntimeException If parsing or interpretation fails
   */
  private ParsedProgram interpretUnsafe(File file) throws IOException {
    String code = new String(Files.readAllBytes(file.toPath()));

    ParseResult parseResult = KigaliSimFacade.parse(code);

    if (parseResult.hasErrors()) {
      String detailedError = KigaliSimFacade.getDetailedErrorMessage(parseResult);
      throw new RuntimeException("Failed to parse QubecTalk code:\n" + detailedError);
    }

    return KigaliSimFacade.interpret(parseResult);
  }

  /**
   * Interprets QubecTalk code from a file with exception handling.
   *
   * @param file The file containing QubecTalk code
   * @return A CommandInterpretResult containing either the program or an error message
   */
  private CommandInterpretResult interpret(File file) {
    try {
      ParsedProgram program = interpretUnsafe(file);
      return new CommandInterpretResult(program);
    } catch (IOException e) {
      return new CommandInterpretResult("Could not read file: " + file + "\nError: " + e.getMessage());
    } catch (RuntimeException e) {
      return new CommandInterpretResult("Failed to interpret QubecTalk code at " + file + "\nInterpretation error: " + e.getMessage());
    }
  }

  /**
   * Writes simulation results to a CSV file without exception handling.
   *
   * @param resultsList The list of simulation results
   * @param csvOutputFile The file to write to
   * @throws IOException If the file cannot be written
   */
  private void writeResultsToCsvUnsafe(List<EngineResult> resultsList, File csvOutputFile) throws IOException {
    String csvContent = KigaliSimFacade.convertResultsToCsv(resultsList);
    try (FileWriter writer = new FileWriter(csvOutputFile)) {
      writer.write(csvContent);
    }
  }

  /**
   * Writes simulation results to a CSV file with exception handling.
   *
   * @param resultsList The list of simulation results
   * @param csvOutputFile The file to write to
   * @return An Optional containing an error message if writing failed, or empty if successful
   */
  private Optional<String> writeResultsToCsv(List<EngineResult> resultsList, File csvOutputFile) {
    try {
      writeResultsToCsvUnsafe(resultsList, csvOutputFile);
      return Optional.empty();
    } catch (IOException e) {
      return Optional.of("Failed to write CSV output to " + csvOutputFile + "\nError: " + e.getMessage());
    }
  }

}
