/**
 * Command to display AI / LLM resource information for Kigali Sim.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.command;

import picocli.CommandLine.Command;

/**
 * Command to display information about Kigali Sim resources intended for AI assistants and LLMs.
 */
@Command(
    name = "ai-info",
    description = "Display LLM / AI assistant resource information for Kigali Sim",
    mixinStandardHelpOptions = true
)
public class AiInfoCommand implements Runnable {

  /**
   * Constructs a new AiInfoCommand.
   */
  public AiInfoCommand() {
  }

  /**
   * Executes the ai-info command by printing LLM resource URLs.
   */
  @Override
  public void run() {
    System.out.println("Additional information about Kigali Sim and how to use it is available");
    System.out.println("at the following URLs geared specifically to AI / LLMs as an importable");
    System.out.println("skill using the llms.txt protocol:");
    System.out.println();
    System.out.println("  Comprehensive overview:");
    System.out.println("    https://kigalisim.org/llms-full.txt");
    System.out.println();
    System.out.println("  Index into more targeted documentation:");
    System.out.println("    https://kigalisim.org/llms.txt");
  }
}
