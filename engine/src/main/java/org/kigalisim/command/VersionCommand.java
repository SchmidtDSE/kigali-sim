/**
 * Command to display the version of KigaliSim.
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

  /**
   * Executes the version command by printing the version number.
   */
  @Override
  public void run() {
    System.out.println("0.1.3");
  }
}
