/**
 * Unit tests for the ParseResult class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.QubecTalkParser.ProgramContext;
import org.mockito.Mockito;

/**
 * Tests for the ParseResult class.
 */
public class ParseResultTest {

  /**
   * Test that the constructor with a program works correctly.
   */
  @Test
  public void testConstructorWithProgram() {
    ProgramContext mockProgram = Mockito.mock(ProgramContext.class);

    ParseResult result = new ParseResult(mockProgram);

    assertFalse(result.hasErrors(), "Result should not have errors");
    assertTrue(result.getProgram().isPresent(), "Program should be present");
    assertEquals(mockProgram, result.getProgram().get(), "Program should match");
    assertEquals(0, result.getErrors().size(), "Errors list should be empty");
  }

  /**
   * Test that the constructor with errors works correctly.
   */
  @Test
  public void testConstructorWithErrors() {
    List<ParseError> errors = new ArrayList<>();
    errors.add(new ParseError(1, "Error 1"));
    errors.add(new ParseError(2, "Error 2"));

    ParseResult result = new ParseResult(errors);

    assertTrue(result.hasErrors(), "Result should have errors");
    assertFalse(result.getProgram().isPresent(), "Program should not be present");
    assertEquals(2, result.getErrors().size(), "Errors list should have 2 items");
    assertEquals(1, result.getErrors().get(0).getLine(), "First error line should be 1");
    assertEquals("Error 1", result.getErrors().get(0).getMessage(), "First error message should match");
    assertEquals(2, result.getErrors().get(1).getLine(), "Second error line should be 2");
    assertEquals("Error 2", result.getErrors().get(1).getMessage(), "Second error message should match");
  }

  /**
   * Test that the constructor with an empty errors list throws an exception.
   */
  @Test
  public void testConstructorWithEmptyErrors() {
    List<ParseError> errors = new ArrayList<>();

    assertThrows(IllegalArgumentException.class, () -> new ParseResult(errors),
        "Constructor should throw IllegalArgumentException for empty errors list");
  }
}
