/**
 * Result of interpreting QubecTalk code.
 *
 * <p>This class holds either a successfully parsed program or an error message.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.command;

import java.util.Optional;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Result of interpreting QubecTalk code.
 *
 * <p>Contains either a successfully parsed program or an error message. Exactly one of the two
 * will be present.</p>
 */
public class CommandInterpretResult {

  private final Optional<ParsedProgram> program;
  private final Optional<String> errorMessage;

  /**
   * Creates a successful interpretation result.
   *
   * @param program The successfully parsed program
   */
  public CommandInterpretResult(ParsedProgram program) {
    this.program = Optional.of(program);
    this.errorMessage = Optional.empty();
  }

  /**
   * Creates a failed interpretation result.
   *
   * @param errorMessage The error message
   */
  public CommandInterpretResult(String errorMessage) {
    this.program = Optional.empty();
    this.errorMessage = Optional.of(errorMessage);
  }

  /**
   * Returns the parsed program if interpretation succeeded.
   *
   * @return An Optional containing the successfully parsed program, or empty if interpretation
   *     failed
   */
  public Optional<ParsedProgram> getProgram() {
    return program;
  }

  /**
   * Returns the error message if interpretation failed.
   *
   * @return An Optional containing the error message, or empty if interpretation succeeded
   */
  public Optional<String> getErrorMessage() {
    return errorMessage;
  }

  /**
   * Checks if the interpretation was successful.
   *
   * @return true if a program is present, false otherwise
   */
  public boolean getIsSuccess() {
    return program.isPresent();
  }

  /**
   * Checks if the interpretation failed.
   *
   * @return true if an error message is present, false otherwise
   */
  public boolean getIsFailure() {
    return errorMessage.isPresent();
  }
}
