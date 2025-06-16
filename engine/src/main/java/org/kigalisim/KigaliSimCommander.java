/**
 * Entrypoint for the KigaliSim command line interface application.
 *
 * <p>This class serves as the main entry point for the Kigali command line interface.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

import org.kigalisim.command.VersionCommand;
import picocli.CommandLine;

/**
 * Entry point for the KigaliSim command line.
 *
 * <p>Entry point for the KigaliSim command line application which can run simulations from outside
 * of the browser.</p>
 *
 * @command kigalisim
 * @mixinStandardHelpOptions true
 * @version 0.0.1
 * @description "KigaliSim command line interface"
 * @subcommands { VersionCommand }
 */
@CommandLine.Command(
    name = "kigalisim",
    mixinStandardHelpOptions = true,
    version = "0.0.1",
    description = "KigaliSim command line interface",
    subcommands = {
        VersionCommand.class
    }
)
public class KigaliSimCommander {

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
