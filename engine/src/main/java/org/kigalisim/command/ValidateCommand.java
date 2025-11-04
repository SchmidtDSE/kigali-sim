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
   * Executes the validate command.
   *
   * @return 0 on success, non-zero error code on failure
   */
  @Override
  public Integer call() {
    if (!file.exists()) {
      System.err.println("Could not find file: " + file);
      return FILE_NOT_FOUND_ERROR;
    }

    // Interpret the code
    CommandInterpretResult interpretResult = interpret(file);
    if (interpretResult.getIsFailure()) {
      System.err.println(interpretResult.getErrorMessage().orElse("Unknown error"));
      return VALIDATION_ERROR;
    }

    System.out.println("Validated QubecTalk code at " + file);
    return 0;
  }

  /**
   * Validates QubecTalk code from a file without exception handling.
   *
   * <p>This method parses and interprets the code but does not return the program.
   * It succeeds silently or throws an exception on error.</p>
   *
   * @param file The file containing QubecTalk code
   * @throws IOException If the file cannot be read
   * @throws RuntimeException If parsing or interpretation fails
   */
  private void interpretUnsafe(File file) throws IOException {
    String code = new String(Files.readAllBytes(file.toPath()));

    ParseResult parseResult = KigaliSimFacade.parse(code);

    if (parseResult.hasErrors()) {
      String detailedError = KigaliSimFacade.getDetailedErrorMessage(parseResult);
      throw new RuntimeException("Failed to parse QubecTalk code:\n" + detailedError);
    }

    // Interpret the parsed code
    KigaliSimFacade.interpret(parseResult);
  }

  /**
   * Validates QubecTalk code from a file with exception handling.
   *
   * @param file The file containing QubecTalk code
   * @return A CommandInterpretResult indicating success or containing an error message
   */
  private CommandInterpretResult interpret(File file) {
    try {
      interpretUnsafe(file);
      return new CommandInterpretResult();
    } catch (IOException e) {
      return new CommandInterpretResult("Could not read file: " + file + "\nError: " + e.getMessage());
    } catch (RuntimeException e) {
      return new CommandInterpretResult("Validation failed for QubecTalk code at " + file + "\nInterpretation error: " + e.getMessage());
    }
  }
}
