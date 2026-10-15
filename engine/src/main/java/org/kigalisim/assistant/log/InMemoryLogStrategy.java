/**
 * In-memory chat log persistence.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.log;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsoner;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A chat log strategy which keeps chats in memory.
 *
 * <p>Each chat is stored as a JSON array of wire messages. This is the default strategy and is safe
 * under TeaVM / WASM as well as the JVM.</p>
 */
public final class InMemoryLogStrategy implements ChatLogStrategy {

  private final Map<String, String> chats = new LinkedHashMap<>();

  /**
   * Constructs an empty in-memory log strategy.
   */
  public InMemoryLogStrategy() {
  }

  @Override
  public String createChat(String name) {
    Objects.requireNonNull(name, "name");
    if (chats.containsKey(name)) {
      throw new IllegalArgumentException("A chat named '" + name + "' already exists");
    }
    chats.put(name, "[]");
    return name;
  }

  @Override
  public ChatLog readChat(String name) {
    return new OpenAiCompatibleChatLog(requireChat(name));
  }

  @Override
  public ChatLog appendChat(String chatName, String wireContents) {
    String existing = requireChat(chatName);
    JsonArray transcript = parseTranscript(chatName, existing);
    Object toAppend = parseAppendable(wireContents);
    if (toAppend instanceof JsonArray appendedArray) {
      transcript.addAll(appendedArray);
    } else {
      transcript.add(toAppend);
    }
    String updated = Jsoner.serialize(transcript);
    chats.put(chatName, updated);
    return new OpenAiCompatibleChatLog(updated);
  }

  @Override
  public ChatLog setChat(String chatName, String wireContents) {
    requireChat(chatName);
    chats.put(chatName, wireContents);
    return new OpenAiCompatibleChatLog(wireContents);
  }

  @Override
  public void deleteChat(String name) {
    requireChat(name);
    chats.remove(name);
  }

  /**
   * Resolves a stored transcript or throws.
   *
   * @param name The chat name.
   * @return The stored wire transcript.
   * @throws NoSuchElementException If the chat does not exist.
   */
  private String requireChat(String name) {
    String existing = chats.get(name);
    if (existing == null) {
      throw new NoSuchElementException("No chat named '" + name + "'");
    }
    return existing;
  }

  /**
   * Parses the stored transcript into an array, defaulting to empty when malformed.
   *
   * @param chatName The chat name used for error messages.
   * @param transcript The stored transcript text.
   * @return The parsed array.
   */
  private JsonArray parseTranscript(String chatName, String transcript) {
    try {
      Object parsed = Jsoner.deserialize(transcript);
      if (parsed instanceof JsonArray array) {
        return array;
      }
      return new JsonArray();
    } catch (JsonException e) {
      throw new IllegalStateException(
          "Stored transcript for chat '" + chatName + "' is not valid JSON", e);
    }
  }

  /**
   * Parses the contents to append.
   *
   * @param wireContents The message or array of messages to append.
   * @return The parsed value.
   */
  private Object parseAppendable(String wireContents) {
    try {
      return Jsoner.deserialize(wireContents);
    } catch (JsonException e) {
      throw new IllegalArgumentException(
          "Unable to parse chat contents to append: " + wireContents, e);
    }
  }
}
