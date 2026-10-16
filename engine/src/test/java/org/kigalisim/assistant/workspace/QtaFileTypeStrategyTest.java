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
import org.kigalisim.assistant.AssistantTestResources;

/**
 * Tests for the QtaFileTypeStrategy class.
 */
public class QtaFileTypeStrategyTest {

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
    String sample = AssistantTestResources.readQta("qta_sample.qta");
    assertEquals(sample, strategy.interpret(sample));
  }

  /**
   * Test that the outline lists top-level stanzas and ignores nested constructs.
   */
  @Test
  public void testOutline() {
    String sample = AssistantTestResources.readQta("qta_sample.qta");
    assertEquals(
        List.of("about", "default", "policy \"P\"", "simulations"),
        strategy.outline(sample));
  }

  /**
   * Test extraction of each non-policy section.
   */
  @Test
  public void testInterpretSimpleSections() {
    String sample = AssistantTestResources.readQta("qta_sample.qta");
    String expectedAbout = """
        start about
          note "hello"
        end about
        """.stripTrailing();
    assertEquals(expectedAbout, strategy.interpret(sample, "ABOUT"));

    String expectedDefault = """
        start default
          define application "Refrigeration"
            uses substance "HFC-134a"
              set sales to 1
            end substance
          end application
        end default
        """.stripTrailing();
    assertEquals(expectedDefault, strategy.interpret(sample, "default"));
  }

  /**
   * Test extraction of a policy by several accepted spellings.
   */
  @Test
  public void testInterpretPolicyAliases() {
    String sample = AssistantTestResources.readQta("qta_sample.qta");
    String expected = """
        start policy "P"
          modify application "Refrigeration"
        end policy
        """.stripTrailing();
    assertEquals(expected, strategy.interpret(sample, "policy \"P\""));
    assertEquals(expected, strategy.interpret(sample, "policy P"));
    assertEquals(expected, strategy.interpret(sample, "\"P\""));
    assertEquals(expected, strategy.interpret(sample, " P "));
  }

  /**
   * Test that an unknown section throws.
   */
  @Test
  public void testInterpretMissingSection() {
    String sample = AssistantTestResources.readQta("qta_sample.qta");
    assertThrows(NoSuchElementException.class, () -> strategy.interpret(sample, "nope"));
  }

  /**
   * Test that line endings are normalized when extracting sections.
   */
  @Test
  public void testInterpretNormalizesLineEndings() {
    String windows = "start default\r\n  set sales to 1\r\nend default";
    String expected = """
        start default
          set sales to 1
        end default
        """.stripTrailing();
    assertEquals(expected, strategy.interpret(windows, "default"));
  }

  /**
   * Test that an unterminated stanza closes at end of file.
   */
  @Test
  public void testUnterminatedStanza() {
    String unterminated = """
        start default
          set sales to 1
        """.stripTrailing();
    assertEquals(List.of("default"), strategy.outline(unterminated));
    assertEquals(unterminated, strategy.interpret(unterminated, "default"));
  }

  /**
   * Test that a trailing comment on the policy start line is ignored.
   */
  @Test
  public void testPolicyWithTrailingComment() {
    String commented = """
        start policy "P" # my policy
        end policy
        """.stripTrailing();
    assertEquals(List.of("policy \"P\""), strategy.outline(commented));
  }

  /**
   * Test that a valid program is outlined and read through the ANTLR visitor.
   */
  @Test
  public void testOutlineAndInterpretValidProgram() {
    String sample = AssistantTestResources.readQta("qta_valid_sample.qta");
    assertEquals(
        List.of("default", "policy \"P\"", "simulations"),
        strategy.outline(sample));

    String expectedPolicy = """
        start policy "P"
          modify application "Refrigeration"
            modify substance "HFC-134a"
              cap sales to 80 % during years 1 to 5
            end substance
          end application
        end policy
        """.stripTrailing();
    assertEquals(expectedPolicy, strategy.interpret(sample, "P"));
  }
}
