/**
 * Unit tests for the QubecTalkParser class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.parse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
   * Helper method to load QubecTalk code from a file.
   *
   * @param filePath The path to the .qta file relative to the project root
   * @return The contents of the file as a String
   * @throws IOException if the file cannot be read
   */
  private String loadQtaFile(String filePath) throws IOException {
    return new String(Files.readAllBytes(Paths.get(filePath)));
  }

  /**
   * Test that parsing valid code returns a successful result.
   */
  @Test
  public void testParseValidCode() throws IOException {
    String code = loadQtaFile("../examples/parser_test_valid.qta");
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
  public void testParseEnableStatements() throws IOException {
    String code = loadQtaFile("../examples/parser_test_enable.qta");
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for enable statements");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing complex enable statement with set operations works correctly.
   */
  @Test
  public void testParseEnableWithSetStatements() throws IOException {
    String code = loadQtaFile("../examples/parser_test_enable_with_set.qta");
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for enable with set statements");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing numbers with commas works correctly.
   */
  @Test
  public void testParseNumbersWithCommas() throws IOException {
    String code = loadQtaFile("../examples/parser_test_numbers_with_commas.qta");
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for comma numbers");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing cap with "displacing by volume" works correctly.
   */
  @Test
  public void testParseCapDisplacingByVolume() throws IOException {
    String code = loadQtaFile("../examples/parser_test_cap_displacing_by_volume.qta");
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for cap displacing by volume");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing cap with "displacing by units" works correctly.
   */
  @Test
  public void testParseCapDisplacingByUnits() throws IOException {
    String code = loadQtaFile("../examples/parser_test_cap_displacing_by_units.qta");
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for cap displacing by units");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing floor with "displacing by volume" with duration works correctly.
   */
  @Test
  public void testParseFloorDisplacingByVolumeDuration() throws IOException {
    String code = loadQtaFile("../examples/parser_test_floor_displacing_by_volume_duration.qta");
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for floor displacing by volume with duration");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that parsing cap with "displacing by units" with duration works correctly.
   */
  @Test
  public void testParseCapDisplacingByUnitsDuration() throws IOException {
    String code = loadQtaFile("../examples/parser_test_cap_displacing_by_units_duration.qta");
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for cap displacing by units with duration");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that existing "displacing" (without by volume/units) still parses correctly.
   */
  @Test
  public void testParseCapDisplacingEquivalent() throws IOException {
    String code = loadQtaFile("../examples/parser_test_cap_displacing_equivalent.qta");
    ParseResult result = parser.parse(code);

    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Parse result should not have errors for legacy displacing syntax");
    assertTrue(result.getProgram().isPresent(), "Parse result should have a program");
  }

}
