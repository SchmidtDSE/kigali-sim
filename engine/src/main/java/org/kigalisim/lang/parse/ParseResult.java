/**
 * Structures to represent the outcome of attempted parsing of a QubecTalk source.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kigalisim.lang.QubecTalkParser;

/**
 * Structure representing the result of parsing a QubecTalk source code file.
 */
public class ParseResult {

  private final Optional<QubecTalkParser.ProgramContext> program;
  private final List<ParseError> errors;

  /**
   * Constructs a ParseResult with the specified program and no errors.
   *
   * @param program the parsed program.
   */
  public ParseResult(QubecTalkParser.ProgramContext program) {
    this.program = Optional.of(program);
    this.errors = new ArrayList<>();
  }

  /**
   * Constructs a ParseResult with the specified errors and no program.
   *
   * @param errors the errors encountered which must not be empty.
   * @throws IllegalArgumentException if errors is empty.
   */
  public ParseResult(List<ParseError> errors) {
    if (errors.isEmpty()) {
      throw new IllegalArgumentException("Passed an empty errors list without parsed program.");
    }

    program = Optional.empty();
    this.errors = errors;
  }

  /**
   * Get the parsed program.
   *
   * @return An Optional containing the parsed program if available.
   */
  public Optional<QubecTalkParser.ProgramContext> getProgram() {
    return program;
  }

  /**
   * Get the list of parsing errors.
   *
   * @return A list of ParseError encountered during parsing.
   */
  public List<ParseError> getErrors() {
    return errors;
  }

  /**
   * Determine if there were errors during parsing.
   *
   * @return true if there are errors, false otherwise.
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

}
