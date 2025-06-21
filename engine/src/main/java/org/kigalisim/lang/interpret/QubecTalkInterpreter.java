/**
 * Structures for interpreting a QubecTalk source.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.interpret;

import org.kigalisim.lang.QubecTalkEngineVisitor;
import org.kigalisim.lang.fragment.Fragment;
import org.kigalisim.lang.parse.ParseResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Strategy to interpret a QubecTalk program into a single ParsedProgram.
 */
public class QubecTalkInterpreter {

  /**
   * Interpret a QubecTalk source into a ParsedProgram.
   *
   * @param parseResult The result of parsing to interpret.
   * @return Parsed program.
   */
  public ParsedProgram interpret(ParseResult parseResult) {
    if (parseResult.hasErrors()) {
      throw new RuntimeException("Cannot interpret program with parse errors.");
    }

    QubecTalkEngineVisitor visitor = new QubecTalkEngineVisitor();
    Fragment fragment = visitor.visit(parseResult.getProgram().orElseThrow());

    return fragment.getProgram();
  }

}
