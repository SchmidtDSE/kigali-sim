/**
 * Entrypoint for the KigaliSim command line interface application.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

import org.kigalisim.command.RunCommand;
import org.kigalisim.command.ValidateCommand;
import org.kigalisim.command.VersionCommand;
import picocli.CommandLine;

/**
 * Entry point for the KigaliSim command line.
 *
 * @command kigalisim
 * @mixinStandardHelpOptions true
 * @version 0.1.0
 * @description "KigaliSim command line interface"
 * @subcommands { VersionCommand }
 */
@CommandLine.Command(
    name = "kigalisim",
    mixinStandardHelpOptions = true,
    version = "0.0.1",
    description = "KigaliSim command line interface",
    subcommands = {
        VersionCommand.class,
        RunCommand.class,
        ValidateCommand.class
    }
)
public class KigaliSimCommander {

  /**
   * Constructs a new KigaliSimCommander.
   */
  public KigaliSimCommander() {
  }

  /**
   * Main entry point for the KigaliSim command line interface.
   *
   * @param args Command line arguments passed to the program
   */
  public static void main(String[] args) {
    int exitCode = new CommandLine(new KigaliSimCommander()).execute(args);
    System.exit(exitCode);
  }
}
