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
 * <p>Each chat is stored as a JSON array of wire messages along with a cached parsed form so that
 * appending does not re-deserialize the transcript on every call. This is the default strategy and
 * is safe under TeaVM / WASM as well as the JVM.</p>
 */
public class InMemoryLogStrategy implements ChatLogStrategy {

  private final Map<String, StoredChat> chats = new LinkedHashMap<>();

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
    chats.put(name, new StoredChat("[]", new JsonArray()));
    return name;
  }

  @Override
  public ChatLog readChat(String name) {
    return new OpenAiCompatibleChatLog(requireChat(name).getWire());
  }

  @Override
  public ChatLog appendChat(String chatName, String wireContents) {
    StoredChat chat = requireChat(chatName);
    JsonArray transcript = chat.getParsed(chatName);
    Object toAppend = parseAppendable(wireContents);
    if (toAppend instanceof JsonArray appendedArray) {
      transcript.addAll(appendedArray);
    } else {
      transcript.add(toAppend);
    }
    chat.setWire(Jsoner.serialize(transcript));
    return new OpenAiCompatibleChatLog(chat.getWire());
  }

  @Override
  public ChatLog setChat(String chatName, String wireContents) {
    requireChat(chatName);
    chats.put(chatName, new StoredChat(wireContents, null));
    return new OpenAiCompatibleChatLog(wireContents);
  }

  @Override
  public void deleteChat(String name) {
    requireChat(name);
    chats.remove(name);
  }

  /**
   * Resolves a stored chat or throws.
   *
   * @param name The chat name.
   * @return The stored chat.
   * @throws NoSuchElementException If the chat does not exist.
   */
  private StoredChat requireChat(String name) {
    StoredChat existing = chats.get(name);
    if (existing == null) {
      throw new NoSuchElementException("No chat named '" + name + "'");
    }
    return existing;
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
          "Unable to parse chat contents to append: " + wireContents,
          e
      );
    }
  }
}