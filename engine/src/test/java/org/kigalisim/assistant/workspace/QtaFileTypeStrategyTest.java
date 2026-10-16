/**
 * Unit tests for the QtaFileTypeStrategy class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the QtaFileTypeStrategy class.
 */
public class QtaFileTypeStrategyTest {

  private static final String SAMPLE = String.join("\n",
      "start about",
      "  note \"hello\"",
      "end about",
      "",
      "start default",
      "  define application \"Refrigeration\"",
      "    uses substance \"HFC-134a\"",
      "      set sales to 1",
      "    end substance",
      "  end application",
      "end default",
      "",
      "start policy \"P\"",
      "  modify application \"Refrigeration\"",
      "end policy",
      "",
      "start simulations",
      "  simulate \"BAU\"",
      "end simulations");

  private final QtaFileTypeStrategy strategy = new QtaFileTypeStrategy();

  /**
   * Test the supported extension and string-native flag.
   */
  @Test
  public void testExtensionsAndStringNative() {
    assertEquals(Set.of(".qta"), strategy.getFileExtensions());
    assertEquals(".qta", QtaFileTypeStrategy.EXTENSION);
    assertTrue(strategy.getIsStringNative());
  }

  /**
   * Test that whole-file interpretation is a pass-through.
   */
  @Test
  public void testInterpretWholeFile() {
    assertEquals(SAMPLE, strategy.interpret(SAMPLE));
  }

  /**
   * Test that the outline lists top-level stanzas and ignores nested constructs.
   */
  @Test
  public void testOutline() {
    assertEquals(
        List.of("about", "default", "policy \"P\"", "simulations"),
        strategy.outline(SAMPLE));
  }

  /**
   * Test extraction of each non-policy section.
   */
  @Test
  public void testInterpretSimpleSections() {
    assertEquals(
        String.join("\n", "start default", "  define application \"Refrigeration\"",
            "    uses substance \"HFC-134a\"", "      set sales to 1", "    end substance",
            "  end application", "end default"),
        strategy.interpret(SAMPLE, "default"));
    assertEquals(
        String.join("\n", "start about", "  note \"hello\"", "end about"),
        strategy.interpret(SAMPLE, "ABOUT"));
  }

  /**
   * Test extraction of a policy by several accepted spellings.
   */
  @Test
  public void testInterpretPolicyAliases() {
    String expected = String.join("\n", "start policy \"P\"",
        "  modify application \"Refrigeration\"", "end policy");
    assertEquals(expected, strategy.interpret(SAMPLE, "policy \"P\""));
    assertEquals(expected, strategy.interpret(SAMPLE, "policy P"));
    assertEquals(expected, strategy.interpret(SAMPLE, "\"P\""));
    assertEquals(expected, strategy.interpret(SAMPLE, " P "));
  }

  /**
   * Test that an unknown section throws.
   */
  @Test
  public void testInterpretMissingSection() {
    assertThrows(NoSuchElementException.class, () -> strategy.interpret(SAMPLE, "nope"));
  }

  /**
   * Test that line endings are normalized when extracting sections.
   */
  @Test
  public void testInterpretNormalizesLineEndings() {
    String windows = "start default\r\n  set sales to 1\r\nend default";
    assertEquals(
        String.join("\n", "start default", "  set sales to 1", "end default"),
        strategy.interpret(windows, "default"));
  }

  /**
   * Test that an unterminated stanza closes at end of file.
   */
  @Test
  public void testUnterminatedStanza() {
    String unterminated = "start default\n  set sales to 1";
    assertEquals(List.of("default"), strategy.outline(unterminated));
    assertEquals(unterminated, strategy.interpret(unterminated, "default"));
  }

  /**
   * Test that a trailing comment on the policy start line is ignored.
   */
  @Test
  public void testPolicyWithTrailingComment() {
    String commented = "start policy \"P\" # my policy\nend policy";
    assertEquals(List.of("policy \"P\""), strategy.outline(commented));
  }
}
