/**
 * Result of interpreting QubecTalk code.
 *
 * <p>This record holds either a successfully parsed program or an error message.</p>
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
 * optionals will be present.</p>
 *
 * @param program The successfully parsed program, if interpretation succeeded
 * @param errorMessage The error message, if interpretation failed
 */
public record CommandInterpretResult(
    Optional<ParsedProgram> program,
    Optional<String> errorMessage
) {

  /**
   * Creates a successful interpretation result.
   *
   * @param program The successfully parsed program
   * @return A CommandInterpretResult with the program
   */
  public static CommandInterpretResult success(ParsedProgram program) {
    return new CommandInterpretResult(Optional.of(program), Optional.empty());
  }

  /**
   * Creates a failed interpretation result.
   *
   * @param errorMessage The error message
   * @return A CommandInterpretResult with the error message
   */
  public static CommandInterpretResult failure(String errorMessage) {
    return new CommandInterpretResult(Optional.empty(), Optional.of(errorMessage));
  }

  /**
   * Checks if the interpretation was successful.
   *
   * @return true if a program is present, false otherwise
   */
  public boolean isSuccess() {
    return program.isPresent();
  }

  /**
   * Checks if the interpretation failed.
   *
   * @return true if an error message is present, false otherwise
   */
  public boolean isFailure() {
    return errorMessage.isPresent();
  }
}
