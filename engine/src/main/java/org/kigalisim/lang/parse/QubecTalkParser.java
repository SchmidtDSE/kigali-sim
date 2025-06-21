/**
 * Entrypoint into parser machinery for the QubecTalk DSL.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.parse;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.kigalisim.lang.QubecTalkLexer;
import org.kigalisim.lang.QubecTalkParser.ProgramContext;

/**
 * Entrypoint for the QubecTalk DSL parser step.
 *
 * <p>Facade acting as an entry point to the parser machinery for the QubecTalk DSL (Domain Specific
 * Language). It leverages ANTLR for, capturing any syntax errors encountered during parsing.</p>
 */
public class QubecTalkParser {

  /**
   * Attempt to parse a QubecTalk source.
   *
   * @param inputCode The code to parse.
   * @return a parse result which may contain error information.
   */
  public ParseResult parse(String inputCode) {
    CharStream input = CharStreams.fromString(inputCode);
    QubecTalkLexer lexer = new QubecTalkLexer(input);
    // Remove default error listeners that print to console
    lexer.removeErrorListeners();

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    org.kigalisim.lang.QubecTalkParser parser = new org.kigalisim.lang.QubecTalkParser(tokens);
    parser.removeErrorListeners();

    List<ParseError> parseErrors = new ArrayList<>();
    BaseErrorListener listener = new ParserErrorListener(parseErrors);

    // Add our error listener to both lexer and parser
    lexer.addErrorListener(listener);
    parser.addErrorListener(listener);

    ProgramContext program = parser.program();

    return parseErrors.isEmpty() ? new ParseResult(program) : new ParseResult(parseErrors);
  }

  /**
   * Listener for ANTLR errors in parsing.
   */
  private static class ParserErrorListener extends BaseErrorListener {

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

}
