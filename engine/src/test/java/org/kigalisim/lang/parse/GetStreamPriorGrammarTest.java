/**
 * Unit tests for the get stream N years ago grammar rules.
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
 * Tests for the get stream N years ago grammar rules.
 * Verifies that the new syntax can be parsed without errors.
 */
public class GetStreamPriorGrammarTest {

  private QubecTalkParser parser;

  /**
   * Set up the parser before each test.
   */
  @BeforeEach
  public void setUp() {
    parser = new QubecTalkParser();
  }

  /**
   * Test that parsing "get equipment 5 years ago as units" works correctly.
   */
  @Test
  public void testParseGetStreamPriorConversion() {
    String code = "start default\n"
        + "  define application \"test app\"\n"
        + "    uses substance \"test substance\"\n"
        + "      enable domestic\n"
        + "      set priorEquipment to get equipment 5 years ago as units * 1 units\n"
        + "    end substance\n"
        + "  end application\n"
        + "end default\n";
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for get stream prior conversion syntax");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing "get equipment 5 years ago of \"SubA\" as units" works correctly.
   */
  @Test
  public void testParseGetStreamIndirectPriorConversion() {
    String code = "start default\n"
        + "  define application \"test app\"\n"
        + "    uses substance \"test substance\"\n"
        + "      enable domestic\n"
        + "      set priorEquipment to get equipment 5 years ago of \"SubA\" as units * 1 units\n"
        + "    end substance\n"
        + "  end application\n"
        + "end default\n";
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for get stream indirect prior conversion syntax");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing "get equipment 5 years ago" works correctly in an expression.
   */
  @Test
  public void testParseGetStreamPrior() {
    String code = "start default\n"
        + "  define application \"test app\"\n"
        + "    uses substance \"test substance\"\n"
        + "      enable domestic\n"
        + "      set priorEquipment to get equipment 5 years ago + 1 units\n"
        + "    end substance\n"
        + "  end application\n"
        + "end default\n";
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for get stream prior syntax");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing "get equipment 5 years ago of \"SubA\"" works correctly in an expression.
   */
  @Test
  public void testParseGetStreamIndirectPrior() {
    String code = "start default\n"
        + "  define application \"test app\"\n"
        + "    uses substance \"test substance\"\n"
        + "      enable domestic\n"
        + "      set priorEquipment to get equipment 5 years ago of \"SubA\" + 1 units\n"
        + "    end substance\n"
        + "  end application\n"
        + "end default\n";
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for get stream indirect prior syntax");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }
}
