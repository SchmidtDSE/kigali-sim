/**
 * Unit tests for the CloudQubectalkPreprocessor class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the CloudQubectalkPreprocessor pipe-to-space substitution.
 */
public class CloudQubectalkPreprocessorTest {

  /**
   * Test that a single pipe character in the middle of a script fragment is replaced with a space.
   */
  @Test
  public void testPipeReplacedWithSpace() {
    CloudQubectalkPreprocessor preprocessor = new CloudQubectalkPreprocessor();
    String result = preprocessor.preprocess("start|default");
    assertEquals("start default", result,
        "A single pipe should be replaced with a space");
  }

  /**
   * Test that multiple pipe characters scattered through the string are all replaced with spaces.
   */
  @Test
  public void testMultiplePipesReplacedWithSpaces() {
    CloudQubectalkPreprocessor preprocessor = new CloudQubectalkPreprocessor();
    String result = preprocessor.preprocess("start|default|end|default");
    assertEquals("start default end default", result,
        "All pipe characters should be replaced with spaces");
  }

  /**
   * Test that a script fragment containing no pipe characters is returned unchanged.
   */
  @Test
  public void testNoPipesUnchanged() {
    CloudQubectalkPreprocessor preprocessor = new CloudQubectalkPreprocessor();
    String input = "start default end default";
    String result = preprocessor.preprocess(input);
    assertEquals(input, result,
        "A script with no pipes should be returned unchanged");
  }

  /**
   * Test that an empty string input returns an empty string.
   */
  @Test
  public void testEmptyStringUnchanged() {
    CloudQubectalkPreprocessor preprocessor = new CloudQubectalkPreprocessor();
    String result = preprocessor.preprocess("");
    assertEquals("", result,
        "An empty string input should return an empty string");
  }

  /**
   * Test that a string composed entirely of pipes becomes an equal-length string of spaces.
   */
  @Test
  public void testOnlyPipesBecomesSpaces() {
    CloudQubectalkPreprocessor preprocessor = new CloudQubectalkPreprocessor();
    String result = preprocessor.preprocess("|||");
    assertEquals("   ", result,
        "A string of only pipes should become an equal-length string of spaces");
  }

  /**
   * Test that pipes at the start and end of the string are replaced.
   */
  @Test
  public void testPipeAtBoundaries() {
    CloudQubectalkPreprocessor preprocessor = new CloudQubectalkPreprocessor();
    String result = preprocessor.preprocess("|hello|");
    assertEquals(" hello ", result,
        "Pipes at the start and end of the string should be replaced with spaces");
  }

}
