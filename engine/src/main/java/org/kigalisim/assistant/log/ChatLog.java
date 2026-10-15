/**
 * A stored assistant chat.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.log;

import java.util.List;

/**
 * A chat's wire transcript and its derived human transcript.
 *
 * <p>Both human readable and machine readable versions of a chat are required. The wire transcript
 * is the single source of truth and the human transcript is derived from it.</p>
 */
public interface ChatLog {

  /**
   * Gets the stored machine readable message array.
   *
   * @return The wire transcript.
   */
  String getWireTranscript();

  /**
   * Derives the human readable transcript.
   *
   * @return The human transcript elements.
   */
  List<HumanTranscriptElement> getHumanTranscript();
}
