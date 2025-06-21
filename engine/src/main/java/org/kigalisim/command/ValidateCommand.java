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
import java.util.concurrent.Callable;
import org.kigalisim.KigaliSimFacade;
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
  private static final int FILE_NOT_FOUND_ERROR = 1;
  private static final int VALIDATION_ERROR = 2;

  @Parameters(index = "0", description = "Path to QubecTalk file to validate")
  private File file;

  @Override
  public Integer call() {
    if (!file.exists()) {
      System.err.println("Could not find file: " + file);
      return FILE_NOT_FOUND_ERROR;
    }

    boolean isValid = KigaliSimFacade.validate(file.getPath());

    if (isValid) {
      System.out.println("Validated QubecTalk code at " + file);
      return 0;
    } else {
      System.err.println("Validation failed for QubecTalk code at " + file);
      return VALIDATION_ERROR;
    }
  }
}
