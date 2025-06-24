/**
 * Unit tests for the ParseError class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the ParseError class.
 */
public class ParseErrorTest {

  /**
   * Test that the constructor and getters work correctly.
   */
  @Test
  public void testConstructorAndGetters() {
    int line = 42;
    String message = "Test error message";

    ParseError error = new ParseError(line, message);

    assertEquals(line, error.getLine(), "Line number should match");
    assertEquals(message, error.getMessage(), "Error message should match");
  }
}
