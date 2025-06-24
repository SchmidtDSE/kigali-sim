/**
 * Unit tests for the QubecTalkParser class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.parse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the QubecTalkParser class.
 */
public class QubecTalkParserTest {

  private QubecTalkParser parser;

  /**
   * Set up the parser before each test.
   */
  @BeforeEach
  public void setUp() {
    parser = new QubecTalkParser();
  }

  /**
   * Test that parsing valid code returns a successful result.
   */
  @Test
  public void testParseValidCode() {
    String code = "start default\nend default";
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing invalid code returns a result with errors.
   */
  @Test
  public void testParseInvalidCode() {
    String code = "invalid code";
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertTrue(result.hasErrors(), "Parse result should have errors");
    assertFalse(result.getProgram().isPresent(), "Parse result should not have a program");
  }
}
