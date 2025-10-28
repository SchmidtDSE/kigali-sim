/**
 * Command to display the version of KigaliSim.
 *
 * <p>This command displays the current version of KigaliSim when executed.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.command;

import picocli.CommandLine.Command;

/**
 * Command to display the version of KigaliSim.
 */
@Command(
    name = "version",
    description = "Display the version of KigaliSim",
    mixinStandardHelpOptions = true
)
public class VersionCommand implements Runnable {

  /**
   * Constructs a new VersionCommand.
   */
  public VersionCommand() {
  }

  @Override
  public void run() {
    System.out.println("0.0.1");
  }
}
