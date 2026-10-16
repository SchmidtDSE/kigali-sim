/**
 * Unit tests for the OpenAiCompatibleChatLog class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for the OpenAiCompatibleChatLog class.
 */
public class OpenAiCompatibleChatLogTest {

  /**
   * Test that the wire transcript is returned verbatim.
   */
  @Test
  public void testGetWireTranscript() {
    OpenAiCompatibleChatLog log = new OpenAiCompatibleChatLog("[{\"role\":\"user\"}]");
    assertEquals("[{\"role\":\"user\"}]", log.getWireTranscript());
  }

  /**
   * Test that the human transcript is not yet implemented.
   */
  @Test
  public void testHumanTranscriptThrows() {
    OpenAiCompatibleChatLog log = new OpenAiCompatibleChatLog("[]");
    assertThrows(UnsupportedOperationException.class, log::getHumanTranscript);
  }
}
