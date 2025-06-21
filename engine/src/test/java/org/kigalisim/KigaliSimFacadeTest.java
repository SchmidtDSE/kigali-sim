/**
 * Unit tests for the KigaliSimFacade class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kigalisim.lang.parse.ParseResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Tests for the KigaliSimFacade class.
 */
public class KigaliSimFacadeTest {

  /**
   * Test that parse method returns a valid parse result.
   */
  @Test
  public void testParse() {
    String code = "start default\nend default";
    ParseResult parseResult = KigaliSimFacade.parse(code);
    assertNotNull(parseResult, "Parse result should not be null");
    assertFalse(parseResult.hasErrors(), "Parse result should not have errors");
    assertTrue(parseResult.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that interpret method returns a valid program.
   */
  @Test
  public void testInterpret() {
    String code = "start default\nend default";
    ParseResult parseResult = KigaliSimFacade.parse(code);
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should not be null");
  }

  /**
   * Test that validate method returns true for valid code.
   */
  @Test
  public void testValidateValidCode(@TempDir Path tempDir) throws IOException {
    String code = "start default\nend default";
    File file = tempDir.resolve("valid.qta").toFile();
    Files.writeString(file.toPath(), code);

    boolean isValid = KigaliSimFacade.validate(file.getPath());
    assertTrue(isValid, "Valid code should validate successfully");
  }

  /**
   * Test that validate method returns false for invalid code.
   */
  @Test
  public void testValidateInvalidCode(@TempDir Path tempDir) throws IOException {
    String code = "invalid code";
    File file = tempDir.resolve("invalid.qta").toFile();
    Files.writeString(file.toPath(), code);

    boolean isValid = KigaliSimFacade.validate(file.getPath());
    assertFalse(isValid, "Invalid code should fail validation");
  }

  /**
   * Test that parseAndInterpret method returns a valid program.
   */
  @Test
  public void testParseAndInterpret(@TempDir Path tempDir) throws IOException {
    String code = "start default\nend default";
    File file = tempDir.resolve("test.qta").toFile();
    Files.writeString(file.toPath(), code);

    ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());
    assertNotNull(program, "Program should not be null");
  }

  /*
   * Test that runSimulation method executes without errors.
   *
  @Test
  public void testRunSimulation(@TempDir Path tempDir) throws IOException {
    String code = "start default\nend default\n\nstart simulations\n  simulate \"test\" from years 1 to 3\nend simulations";
    File file = tempDir.resolve("simulation.qta").toFile();
    Files.writeString(file.toPath(), code);

    ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());
    assertNotNull(program, "Program should not be null");

    // This should not throw an exception
    KigaliSimFacade.runSimulation(program, "test");
  }*/
}
