/**
 * OpenAI-compatible chat log.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.log;

import java.util.List;

/**
 * The OpenAI-compatible implementation of a stored chat log.
 *
 * <p>The wire transcript is stored verbatim and the human transcript is derived from it. Human
 * transcript rendering is not implemented until the human transcript components, so
 * {@link #getHumanTranscript()} currently throws.</p>
 */
public final class OpenAiCompatibleChatLog implements ChatLog {

  private final String wireTranscript;

  /**
   * Constructs a chat log over a wire transcript.
   *
   * @param wireTranscript The stored wire transcript.
   */
  public OpenAiCompatibleChatLog(String wireTranscript) {
    this.wireTranscript = wireTranscript;
  }

  @Override
  public String getWireTranscript() {
    return wireTranscript;
  }

  @Override
  public List<HumanTranscriptElement> getHumanTranscript() {
    throw new UnsupportedOperationException(
        "Human transcript rendering is not implemented until the human transcript component");
  }
}
