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
import java.util.concurrent.Callable;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.lang.program.ParsedProgram;
import picocli.CommandLine.Command;
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

  @Parameters(index = "0", description = "Path to QubecTalk file to run")
  private File file;

  @Override
  public Integer call() {
    if (!file.exists()) {
      System.err.println("Could not find file: " + file);
      return FILE_NOT_FOUND_ERROR;
    }

    try {
      // Parse and interpret the file
      ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());

      // Run all scenarios in the program
      KigaliSimFacade.runAllScenarios(program);

      System.out.println("Successfully ran all simulations");
      return 0;
    } catch (Exception e) {
      System.err.println("Error running simulation: " + e.getMessage());
      return EXECUTION_ERROR;
    }
  }
}
