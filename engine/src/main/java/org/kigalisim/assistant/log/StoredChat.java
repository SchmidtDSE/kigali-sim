/**
 * A stored chat transcript which caches its parsed form.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.log;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * A stored chat transcript which caches its parsed form.
 *
 * <p>The wire transcript is the source of truth and the parsed form is derived from it on demand
 * so that appending does not re-deserialize the transcript on every call.</p>
 */
class StoredChat {

  private String wire;
  private JsonArray parsed;

  /**
   * Constructs stored chat state.
   *
   * @param wire The wire transcript.
   * @param parsed The cached parsed transcript, or null when it has not been parsed yet.
   */
  StoredChat(String wire, JsonArray parsed) {
    this.wire = wire;
    this.parsed = parsed;
  }

  /**
   * Gets the wire transcript.
   *
   * @return The wire transcript.
   */
  String getWire() {
    return wire;
  }

  /**
   * Replaces the wire transcript.
   *
   * @param wire The new wire transcript.
   */
  void setWire(String wire) {
    this.wire = wire;
  }

  /**
   * Gets the parsed transcript, parsing it only when it has not been parsed before.
   *
   * @param chatName The chat name used for error messages.
   * @return The parsed transcript.
   */
  JsonArray getParsed(String chatName) {
    if (parsed == null) {
      parsed = parseTranscript(chatName, wire);
    }
    return parsed;
  }

  /**
   * Parses the stored transcript into an array.
   *
   * <p>Valid JSON which is not an array parses to an empty array. Malformed JSON is surfaced so
   * that corrupt storage is not silently overwritten.</p>
   *
   * @param chatName The chat name used for error messages.
   * @param transcript The stored transcript text.
   * @return The parsed array.
   * @throws IllegalStateException If the stored transcript is not valid JSON.
   */
  private static JsonArray parseTranscript(String chatName, String transcript) {
    try {
      Object parsed = Jsoner.deserialize(transcript);
      if (parsed instanceof JsonArray array) {
        return array;
      }
      return new JsonArray();
    } catch (JsonException e) {
      throw new IllegalStateException(
          "Stored transcript for chat '" + chatName + "' is not valid JSON",
          e
      );
    }
  }
}