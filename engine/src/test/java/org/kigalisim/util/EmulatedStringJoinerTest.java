/**
 * Tests for EmulatedStringJoiner.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the EmulatedStringJoiner class.
 */
public class EmulatedStringJoinerTest {

  @Test
  public void testEmptyJoiner() {
    EmulatedStringJoiner joiner = new EmulatedStringJoiner(",");
    assertEquals("", joiner.toString());
  }

  @Test
  public void testSingleElement() {
    EmulatedStringJoiner joiner = new EmulatedStringJoiner(",");
    joiner.add("test");
    assertEquals("test", joiner.toString());
  }

  @Test
  public void testMultipleElements() {
    EmulatedStringJoiner joiner = new EmulatedStringJoiner(",");
    joiner.add("one").add("two").add("three");
    assertEquals("one,two,three", joiner.toString());
  }

  @Test
  public void testDifferentDelimiter() {
    EmulatedStringJoiner joiner = new EmulatedStringJoiner(" | ");
    joiner.add("apple").add("banana").add("cherry");
    assertEquals("apple | banana | cherry", joiner.toString());
  }

  @Test
  public void testEmptyElements() {
    EmulatedStringJoiner joiner = new EmulatedStringJoiner(",");
    joiner.add("").add("").add("");
    assertEquals(",,", joiner.toString());
  }

  @Test
  public void testNullElements() {
    EmulatedStringJoiner joiner = new EmulatedStringJoiner(",");
    joiner.add(null).add(null);
    assertEquals("null,null", joiner.toString());
  }
}
