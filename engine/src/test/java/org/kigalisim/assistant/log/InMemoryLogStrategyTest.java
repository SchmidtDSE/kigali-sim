/**
 * Unit tests for the InMemoryLogStrategy class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/**
 * Tests for the InMemoryLogStrategy class.
 */
public class InMemoryLogStrategyTest {

  /**
   * Test creating and reading a chat.
   */
  @Test
  public void testCreateAndRead() {
    InMemoryLogStrategy strategy = new InMemoryLogStrategy();
    String name = strategy.createChat("chat one");
    assertEquals("chat one", name);
    assertEquals("[]", strategy.readChat("chat one").getWireTranscript());
  }

  /**
   * Test that duplicate names are rejected.
   */
  @Test
  public void testDuplicateCreateThrows() {
    InMemoryLogStrategy strategy = new InMemoryLogStrategy();
    strategy.createChat("chat");
    assertThrows(IllegalArgumentException.class, () -> strategy.createChat("chat"));
  }

  /**
   * Test that operations on a missing chat throw.
   */
  @Test
  public void testMissingChatThrows() {
    InMemoryLogStrategy strategy = new InMemoryLogStrategy();
    assertThrows(NoSuchElementException.class, () -> strategy.readChat("missing"));
    assertThrows(NoSuchElementException.class, () -> strategy.appendChat("missing", "{}"));
    assertThrows(NoSuchElementException.class, () -> strategy.setChat("missing", "[]"));
    assertThrows(NoSuchElementException.class, () -> strategy.deleteChat("missing"));
  }

  /**
   * Test that a single object payload is appended.
   *
   * @throws Exception If the transcript cannot be parsed.
   */
  @Test
  public void testAppendObjectPayload() throws Exception {
    InMemoryLogStrategy strategy = new InMemoryLogStrategy();
    strategy.createChat("chat");

    JsonObject message = new JsonObject();
    message.put("role", "user");
    message.put("content", "hi");
    ChatLog log = strategy.appendChat("chat", Jsoner.serialize(message));

    JsonArray parsed = (JsonArray) Jsoner.deserialize(log.getWireTranscript());
    assertEquals(1, parsed.size());
    assertEquals(message, parsed.get(0));
  }

  /**
   * Test that an array payload is appended element by element.
   *
   * @throws Exception If the transcript cannot be parsed.
   */
  @Test
  public void testAppendArrayPayload() throws Exception {
    InMemoryLogStrategy strategy = new InMemoryLogStrategy();
    strategy.createChat("chat");

    strategy.appendChat("chat", "[{\"role\":\"user\"},{\"role\":\"assistant\"}]");
    ChatLog log = strategy.appendChat("chat", "{\"role\":\"tool\"}");

    JsonArray parsed = (JsonArray) Jsoner.deserialize(log.getWireTranscript());
    assertEquals(3, parsed.size());
    assertEquals("tool", ((JsonObject) parsed.get(2)).get("role"));
  }

  /**
   * Test that setChat replaces the transcript verbatim.
   */
  @Test
  public void testSetReplacesTranscript() {
    InMemoryLogStrategy strategy = new InMemoryLogStrategy();
    strategy.createChat("chat");
    strategy.appendChat("chat", "{\"role\":\"user\"}");

    String replacement = "[{\"role\":\"system\"}]";
    ChatLog log = strategy.setChat("chat", replacement);

    assertEquals(replacement, log.getWireTranscript());
    assertEquals(replacement, strategy.readChat("chat").getWireTranscript());
  }

  /**
   * Test that delete removes a chat.
   */
  @Test
  public void testDeleteRemovesChat() {
    InMemoryLogStrategy strategy = new InMemoryLogStrategy();
    strategy.createChat("chat");
    strategy.deleteChat("chat");
    assertThrows(NoSuchElementException.class, () -> strategy.readChat("chat"));
  }

  /**
   * Test that malformed stored content is surfaced rather than silently ignored.
   */
  @Test
  public void testMalformedStoredContentThrows() {
    InMemoryLogStrategy strategy = new InMemoryLogStrategy();
    strategy.createChat("chat");
    strategy.setChat("chat", "{not an array");
    assertThrows(
        IllegalStateException.class, () -> strategy.appendChat("chat", "{\"role\":\"user\"}"));
  }

  /**
   * Test that an untouched transcript remains a JSON array.
   */
  @Test
  public void testEmptyTranscriptRemainsArray() {
    InMemoryLogStrategy strategy = new InMemoryLogStrategy();
    strategy.createChat("chat");
    assertTrue(strategy.readChat("chat").getWireTranscript().startsWith("["));
  }
}
