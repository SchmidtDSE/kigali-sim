/**
 * Command line interface handler for validating QubecTalk simulation files.
 *
 * <p>This class implements the 'validate' command which checks QubecTalk script files for syntax
 * errors and other validation issues.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.lang.parse.ParseResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Command handler for validating QubecTalk simulation files.
 *
 * <p>Processes command line arguments to validate QubecTalk script files, checking for syntax errors
 * and other validation issues.</p>
 */
@Command(
    name = "validate",
    description = "Validate a simulation file"
)
public class ValidateCommand implements Callable<Integer> {

  /**
   * Constructs a new ValidateCommand.
   */
  public ValidateCommand() {
  }

  private static final int FILE_NOT_FOUND_ERROR = 1;
  private static final int VALIDATION_ERROR = 2;

  @Parameters(index = "0", description = "Path to QubecTalk file to validate")
  private File file;

  /**
   * Interprets QubecTalk code from a file without exception handling.
   *
   * @param file The file containing QubecTalk code
   * @throws IOException If the file cannot be read
   * @throws Exception If parsing or interpretation fails
   */
  private void interpretUnsafe(File file) throws IOException, Exception {
    // Read the file content
    String code = new String(Files.readAllBytes(file.toPath()));

    // Parse the code to get detailed error information
    ParseResult parseResult = KigaliSimFacade.parse(code);

    if (parseResult.hasErrors()) {
      String detailedError = KigaliSimFacade.getDetailedErrorMessage(parseResult);
      throw new Exception("Failed to parse QubecTalk code:\n" + detailedError);
    }

    // Interpret the parsed code
    KigaliSimFacade.interpret(parseResult);
  }

  /**
   * Interprets QubecTalk code from a file with exception handling.
   *
   * @param file The file containing QubecTalk code
   * @return A CommandInterpretResult containing success or an error message
   */
  private CommandInterpretResult interpret(File file) {
    try {
      interpretUnsafe(file);
      return CommandInterpretResult.success(null);
    } catch (IOException e) {
      return CommandInterpretResult.failure("Could not read file: " + file + "\nError: " + e.getMessage());
    } catch (Exception e) {
      return CommandInterpretResult.failure("Validation failed for QubecTalk code at " + file + "\nInterpretation error: " + e.getMessage());
    }
  }

  @Override
  public Integer call() {
    if (!file.exists()) {
      System.err.println("Could not find file: " + file);
      return FILE_NOT_FOUND_ERROR;
    }

    // Interpret the code
    CommandInterpretResult interpretResult = interpret(file);
    if (interpretResult.isFailure()) {
      System.err.println(interpretResult.errorMessage().get());
      return VALIDATION_ERROR;
    }

    System.out.println("Validated QubecTalk code at " + file);
    return 0;
  }
}
