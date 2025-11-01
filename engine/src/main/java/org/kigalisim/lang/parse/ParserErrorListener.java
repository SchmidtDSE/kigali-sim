/**
 * Error listener for ANTLR parsing errors.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.parse;

import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Listener for ANTLR errors in parsing.
 *
 * <p>Captures syntax errors encountered during ANTLR parsing and collects them into a list for
 * processing by the parser.</p>
 */
public class ParserErrorListener extends BaseErrorListener {

  private final List<ParseError> parseErrors;

  /**
   * Create a new listener for errors encountered in ANTLR parsing.
   *
   * @param parseErrors The list in which to report ANTLR errors.
   */
  public ParserErrorListener(List<ParseError> parseErrors) {
    this.parseErrors = parseErrors;
  }

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
      int charPositionInLine, String msg, RecognitionException e) {
    parseErrors.add(new ParseError(line, msg));
  }
}
