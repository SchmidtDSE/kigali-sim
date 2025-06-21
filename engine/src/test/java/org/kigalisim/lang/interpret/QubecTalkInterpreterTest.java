/**
 * Unit tests for the QubecTalkInterpreter class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.parse.ParseError;
import org.kigalisim.lang.parse.ParseResult;
import org.kigalisim.lang.parse.QubecTalkParser;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Tests for the QubecTalkInterpreter class.
 */
public class QubecTalkInterpreterTest {

  private QubecTalkInterpreter interpreter;

  /**
   * Set up the interpreter before each test.
   */
  @BeforeEach
  public void setUp() {
    interpreter = new QubecTalkInterpreter();
  }

  /**
   * Test that interpret throws an exception when the parse result has errors.
   */
  @Test
  public void testInterpretWithErrors() {
    List<ParseError> errors = new ArrayList<>();
    errors.add(new ParseError(1, "Test error"));
    ParseResult parseResult = new ParseResult(errors);

    assertThrows(RuntimeException.class, () -> interpreter.interpret(parseResult),
        "Interpret should throw RuntimeException when parse result has errors");
  }

  /**
   * Test that interpret returns a program when the parse result is valid.
   */
  @Test
  public void testInterpretWithValidResult() {
    // This test is simplified to avoid mocking static methods
    // We'll use a real parse result with a simple valid program
    String validCode = "start default\nend default";
    QubecTalkParser parser = new QubecTalkParser();
    ParseResult parseResult = parser.parse(validCode);

    // Call the method under test
    ParsedProgram result = interpreter.interpret(parseResult);

    // Verify the result
    assertNotNull(result, "Result should not be null");
  }
}
