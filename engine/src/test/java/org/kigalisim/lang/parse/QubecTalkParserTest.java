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

  /**
   * Test that parsing enable statements works correctly.
   */
  @Test
  public void testParseEnableStatements() {
    String code = """
        start default

        define application "Test"

          uses substance "TestSub"
            enable domestic
            enable import during year 2020
            enable export during years 2020 to 2025
          end substance

        end application

        end default
        """;
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for enable statements");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing complex enable statement with set operations works correctly.
   */
  @Test
  public void testParseEnableWithSetStatements() {
    String code = """
        start default

        define application "Test"

          uses substance "TestSub"
            enable domestic
            set domestic to 100 kg
            enable import
            recharge 5 % each year with 1 kg / unit
          end substance

        end application

        end default
        """;
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for enable with set statements");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing numbers with commas works correctly.
   */
  @Test
  public void testParseNumbersWithCommas() {
    String code = """
        start default

        define application "Test"

          uses substance "TestSub"
            enable domestic
            set domestic to 1,000 kg
            set import to 12,34.5,6 kg
            recharge 1,5 % each year with ,123 kg / unit
          end substance

        end application

        end default
        """;
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for comma numbers");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing cap with "displacing by volume" works correctly.
   */
  @Test
  public void testParseCapDisplacingByVolume() {
    String code = """
        start default

        define application "Test"

          uses substance "TestSub"
            enable domestic
            set domestic to 100 kg
          end substance

          uses substance "OtherSub"
            enable domestic
            set domestic to 0 kg
          end substance

        end application

        end default

        start policy "TestPolicy"

        modify application "Test"

          modify substance "TestSub"
            cap sales to 50 kg displacing by volume "OtherSub"
          end substance

        end application

        end policy

        start simulations

        simulate "test" from years 1 to 10

        end simulations
        """;
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for cap displacing by volume");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing cap with "displacing by units" works correctly.
   */
  @Test
  public void testParseCapDisplacingByUnits() {
    String code = """
        start default

        define application "Test"

          uses substance "TestSub"
            enable domestic
            set domestic to 100 kg
          end substance

          uses substance "OtherSub"
            enable domestic
            set domestic to 0 kg
          end substance

        end application

        end default

        start policy "TestPolicy"

        modify application "Test"

          modify substance "TestSub"
            cap sales to 50 kg displacing by units "OtherSub"
          end substance

        end application

        end policy

        start simulations

        simulate "test" from years 1 to 10

        end simulations
        """;
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for cap displacing by units");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing floor with "displacing by volume" with duration works correctly.
   */
  @Test
  public void testParseFloorDisplacingByVolumeDuration() {
    String code = """
        start default

        define application "Test"

          uses substance "TestSub"
            enable domestic
            set domestic to 100 kg
          end substance

          uses substance "OtherSub"
            enable domestic
            set domestic to 0 kg
          end substance

        end application

        end default

        start policy "TestPolicy"

        modify application "Test"

          modify substance "TestSub"
            floor import to 50 mt displacing by volume domestic during years 3 to 5
          end substance

        end application

        end policy

        start simulations

        simulate "test" from years 1 to 10

        end simulations
        """;
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for floor displacing by volume with duration");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing cap with "displacing by units" with duration works correctly.
   */
  @Test
  public void testParseCapDisplacingByUnitsDuration() {
    String code = """
        start default

        define application "Test"

          uses substance "TestSub"
            enable domestic
            set domestic to 100 kg
          end substance

          uses substance "OtherSub"
            enable domestic
            set domestic to 0 kg
          end substance

        end application

        end default

        start policy "TestPolicy"

        modify application "Test"

          modify substance "TestSub"
            cap sales to 80 % displacing by units "R-600a" during years 3 to 5
          end substance

        end application

        end policy

        start simulations

        simulate "test" from years 1 to 10

        end simulations
        """;
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for cap displacing by units with duration");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that existing "displacing" (without by volume/units) still parses correctly.
   */
  @Test
  public void testParseCapDisplacingEquivalent() {
    String code = """
        start default

        define application "Test"

          uses substance "TestSub"
            enable domestic
            set domestic to 100 kg
          end substance

          uses substance "OtherSub"
            enable domestic
            set domestic to 0 kg
          end substance

        end application

        end default

        start policy "TestPolicy"

        modify application "Test"

          modify substance "TestSub"
            cap sales to 50 kg displacing "OtherSub"
          end substance

        end application

        end policy

        start simulations

        simulate "test" from years 1 to 10

        end simulations
        """;
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for legacy displacing syntax");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

}
