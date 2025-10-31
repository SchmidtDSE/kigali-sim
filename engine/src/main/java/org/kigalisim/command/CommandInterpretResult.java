/**
 * Result of interpreting QubecTalk code.
 *
 * <p>This class holds either a successfully parsed program or an error message.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.command;

import org.kigalisim.lang.program.ParsedProgram;

/**
 * Result of interpreting QubecTalk code.
 *
 * <p>Contains either a successfully parsed program or an error message. Exactly one of the two
 * will be present.</p>
 */
public class CommandInterpretResult {

  private final ParsedProgram program;
  private final String errorMessage;

  /**
   * Creates a successful interpretation result.
   *
   * @param program The successfully parsed program
   */
  public CommandInterpretResult(ParsedProgram program) {
    this.program = program;
    this.errorMessage = null;
  }

  /**
   * Creates a failed interpretation result.
   *
   * @param errorMessage The error message
   */
  public CommandInterpretResult(String errorMessage) {
    this.program = null;
    this.errorMessage = errorMessage;
  }

  /**
   * Returns the parsed program if interpretation succeeded.
   *
   * @return The successfully parsed program, or null if interpretation failed
   */
  public ParsedProgram getProgram() {
    return program;
  }

  /**
   * Returns the error message if interpretation failed.
   *
   * @return The error message, or null if interpretation succeeded
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Checks if the interpretation was successful.
   *
   * @return true if a program is present, false otherwise
   */
  public boolean getIsSuccess() {
    return program != null;
  }

  /**
   * Checks if the interpretation failed.
   *
   * @return true if an error message is present, false otherwise
   */
  public boolean getIsFailure() {
    return errorMessage != null;
  }
}
