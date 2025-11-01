/**
 * Entrypoint into parser machinery for the QubecTalk DSL.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.parse;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.kigalisim.lang.QubecTalkLexer;
import org.kigalisim.lang.QubecTalkParser.ProgramContext;

/**
 * Entrypoint for the QubecTalk DSL parser step.
 *
 * <p>Facade acting as an entry point to the parser machinery for the QubecTalk DSL (Domain Specific
 * Language). It leverages ANTLR for parsing, capturing any syntax errors encountered during the
 * process.</p>
 */
public class QubecTalkParser {

  /**
   * Preprocesses QubecTalk input to handle "each year" syntax sugar.
   *
   * <p>Removes standalone "each year" or "each years" at the end of lines to prevent
   * parser ambiguity while preserving them within "during" clauses. Uses multiline mode
   * to correctly match end-of-line anchors.</p>
   *
   * <p>Handles cases like:</p>
   * <ul>
   *   <li>"retire 5 % each year" → "retire 5 %"</li>
   *   <li>"recharge 10 % with 0.15 kg / unit each year" → "recharge 10 % with 0.15 kg / unit"</li>
   * </ul>
   *
   * <p>But preserves:</p>
   * <ul>
   *   <li>"change domestic by +5 % each year during years 2025 to 2035" (unchanged)</li>
   * </ul>
   *
   * @param input The raw QubecTalk code
   * @return The preprocessed QubecTalk code
   */
  private String preprocessInput(String input) {
    return input.replaceAll("(?m)\\s+each\\s+years?\\s*$", "");
  }

  /**
   * Attempt to parse a QubecTalk source.
   *
   * <p>Preprocesses the input to handle syntax sugar, removes default error listeners to prevent
   * console output, and uses a custom error listener to capture any parse errors.</p>
   *
   * @param inputCode The code to parse.
   * @return a parse result which may contain error information.
   */
  public ParseResult parse(String inputCode) {
    CharStream input = CharStreams.fromString(preprocessInput(inputCode));
    QubecTalkLexer lexer = new QubecTalkLexer(input);
    lexer.removeErrorListeners();

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    org.kigalisim.lang.QubecTalkParser parser = new org.kigalisim.lang.QubecTalkParser(tokens);
    parser.removeErrorListeners();

    List<ParseError> parseErrors = new ArrayList<>();
    ParserErrorListener listener = new ParserErrorListener(parseErrors);

    lexer.addErrorListener(listener);
    parser.addErrorListener(listener);

    ProgramContext program = parser.program();

    return parseErrors.isEmpty() ? new ParseResult(program) : new ParseResult(parseErrors);
  }

}
