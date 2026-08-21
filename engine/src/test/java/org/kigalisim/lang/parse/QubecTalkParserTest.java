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
   * Helper to wrap a retire statement in a valid program skeleton.
   *
   * @param retireLine the retire statement to embed
   * @return a full QubecTalk program containing the statement
   */
  private String wrapRetire(String retireLine) {
    return "start default\n"
        + "  define application \"test\"\n"
        + "    uses substance \"test\"\n"
        + "      enable domestic\n"
        + "      initial charge with 1 kg / unit for domestic\n"
        + "      " + retireLine + "\n"
        + "      set domestic to 100 units\n"
        + "      equals 5 tCO2e / mt\n"
        + "    end substance\n"
        + "  end application\n"
        + "end default\n"
        + "start simulations\n"
        + "  simulate \"business as usual\" from years 1 to 12\n"
        + "end simulations\n";
  }

  /**
   * Test that a Weibull retire without the full tail is a parse error.
   */
  @Test
  public void testParseInvalidWeibullMissingTail() {
    ParseResult result = parser.parse(wrapRetire("retire 5 year old"));
    assertTrue(result.hasErrors(), "Missing 'mean weibull' tail should be a parse error");
    assertFalse(result.getProgram().isPresent(), "Invalid Weibull retire should not produce a program");
  }

  /**
   * Test that a Weibull retire missing 'weibull' is a parse error.
   */
  @Test
  public void testParseInvalidWeibullMissingWeibull() {
    ParseResult result = parser.parse(wrapRetire("retire 5 year old mean"));
    assertTrue(result.hasErrors(), "Missing 'weibull' should be a parse error");
    assertFalse(result.getProgram().isPresent(), "Invalid Weibull retire should not produce a program");
  }

  /**
   * Test that a Weibull retire from a percentage form is a parse error.
   */
  @Test
  public void testParseInvalidWeibullPercentForm() {
    ParseResult result = parser.parse(wrapRetire("retire 5 % / year old mean weibull"));
    assertTrue(result.hasErrors(), "Percentage form with weibull tail should be a parse error");
    assertFalse(result.getProgram().isPresent(), "Invalid Weibull retire should not produce a program");
  }

  /**
   * Test that a Weibull retire with 'with replacement' is a parse error.
   */
  @Test
  public void testParseInvalidWeibullWithReplacement() {
    ParseResult result = parser.parse(wrapRetire("retire 5 year old mean weibull with replacement"));
    assertTrue(result.hasErrors(), "Weibull with replacement should be a parse error");
    assertFalse(result.getProgram().isPresent(), "Invalid Weibull retire should not produce a program");
  }

  /**
   * Test that a plain retire with a temporal unit still parses as before.
   */
  @Test
  public void testParseWeibullRegressionPlainYearRetire() {
    ParseResult result = parser.parse(wrapRetire("retire 5 year"));
    assertNotNull(result, "Parse result should not be null");
    assertFalse(result.hasErrors(), "Plain 'retire 5 year' should still parse");
    assertTrue(result.getProgram().isPresent(), "Plain 'retire 5 year' should produce a program");
  }

  /**
   * Test that a valid exact retire parses with an integer age.
   */
  @Test
  public void testParseValidExactRetire() {
    ParseResult result = parser.parse(wrapRetire("retire 5 year old exact"));
    assertFalse(result.hasErrors(), "'retire 5 year old exact' should parse");
    assertTrue(result.getProgram().isPresent(), "Valid exact retire should produce a program");
  }

  /**
   * Test that a valid exact retire parses with plural 'years' and a duration.
   */
  @Test
  public void testParseValidExactRetireDurationPlural() {
    ParseResult result = parser.parse(
        wrapRetire("retire 10 years old exact during years 2 to 5"));
    assertFalse(result.hasErrors(), "Plural exact retire with duration should parse");
    assertTrue(result.getProgram().isPresent(), "Valid exact retire should produce a program");
  }

  /**
   * Test that an exact retire missing 'exact' is a parse error.
   */
  @Test
  public void testParseInvalidExactMissingExact() {
    ParseResult result = parser.parse(wrapRetire("retire 5 year old"));
    assertTrue(result.hasErrors(), "Missing 'exact' should be a parse error");
    assertFalse(result.getProgram().isPresent(), "Invalid exact retire should not produce a program");
  }

  /**
   * Test that an exact retire with a decimal age is a parse error since only whole years
   * of equipment age are meaningful for the underlying years-ago lookup.
   */
  @Test
  public void testParseInvalidExactDecimalAge() {
    ParseResult result = parser.parse(wrapRetire("retire 5.5 year old exact"));
    assertTrue(result.hasErrors(), "Decimal age exact retire should be a parse error");
    assertFalse(result.getProgram().isPresent(), "Invalid exact retire should not produce a program");
  }

  /**
   * Test that an exact retire with 'with replacement' is a parse error since the grammar
   * has no exact-with-replacement form.
   */
  @Test
  public void testParseInvalidExactWithReplacement() {
    ParseResult result = parser.parse(wrapRetire("retire 5 year old exact with replacement"));
    assertTrue(result.hasErrors(), "Exact retire with replacement should be a parse error");
    assertFalse(result.getProgram().isPresent(), "Invalid exact retire should not produce a program");
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
