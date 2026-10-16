/**
 * Unit tests for the KigaliAssistant class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.assistant.callback.AssistantTurnCallback;
import org.kigalisim.assistant.log.ChatLog;
import org.kigalisim.assistant.log.InMemoryLogStrategy;
import org.kigalisim.assistant.model.AssistantModel;
import org.kigalisim.assistant.model.AssistantModelQuery;
import org.kigalisim.assistant.model.MockModelWireStrategy;
import org.kigalisim.assistant.model.OpenAiModelProtocolStrategy;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.tool.AssistantToolbelt;

/**
 * Tests for the KigaliAssistant class.
 */
public class KigaliAssistantTest {

  /**
   * A turn strategy which captures the callback without completing.
   */
  private static final class CapturingTurnStrategy implements AssistantTurnStrategy {

    private final AssistantModel model;
    private AssistantTurnCallback captured;

    private CapturingTurnStrategy(AssistantModel model) {
      this.model = model;
    }

    @Override
    public AssistantModelQuery runTurn(
        AssistantToolbelt toolbelt,
        String chatName,
        String message,
        AssistantTurnCallback callback) {
      this.captured = callback;
      return new AssistantModelQuery(chatName, "context", message, model);
    }
  }

  /**
   * A callback which records every result it receives.
   */
  private static final class RecordingCallback implements AssistantTurnCallback {

    private final List<AssistantResult> actions = new ArrayList<>();
    private final List<AssistantResult> completions = new ArrayList<>();

    @Override
    public void onActionComplete(AssistantResult result) {
      actions.add(result);
    }

    @Override
    public void onTurnComplete(AssistantResult result) {
      completions.add(result);
    }
  }

  /**
   * Builds a model for tests.
   *
   * @return The model.
   */
  private AssistantModel buildModel() {
    return new AssistantModel(
        new MockModelWireStrategy(),
        new OpenAiModelProtocolStrategy("test-model"),
        "test-model",
        "test-service");
  }

  /**
   * Builds an assistant over the given turn strategy and log strategy.
   *
   * @param turnStrategy The turn strategy.
   * @param logStrategy The log strategy.
   * @return The assistant.
   */
  private KigaliAssistant buildAssistant(
      AssistantTurnStrategy turnStrategy, InMemoryLogStrategy logStrategy) {
    return new KigaliAssistantBuilder()
        .setModel(buildModel())
        .setTurnStrategy(turnStrategy)
        .setChatLogStrategy(logStrategy)
        .build();
  }

  /**
   * Test that chat operations are delegated to the log strategy.
   */
  @Test
  public void testChatDelegation() {
    InMemoryLogStrategy logStrategy = new InMemoryLogStrategy();
    KigaliAssistant assistant = buildAssistant(
        (toolbelt, chatName, message, callback) -> null, logStrategy);

    assistant.createChat("chat");
    ChatLog log = assistant.readChat("chat");
    assertEquals("[]", log.getWireTranscript());

    assistant.deleteChat("chat");
    assertThrows(NoSuchElementException.class, () -> assistant.readChat("chat"));
  }

  /**
   * Test that sendMessage delegates to the turn strategy and returns its query.
   */
  @Test
  public void testSendMessageDelegates() {
    InMemoryLogStrategy logStrategy = new InMemoryLogStrategy();
    AssistantModel model = buildModel();
    CapturingTurnStrategy turnStrategy = new CapturingTurnStrategy(model);
    KigaliAssistant assistant = buildAssistant(turnStrategy, logStrategy);
    logStrategy.createChat("chat");

    RecordingCallback callback = new RecordingCallback();
    Optional<AssistantModelQuery> query = assistant.sendMessage("chat", "hello", callback);

    assertTrue(query.isPresent());
    assertEquals("chat", query.get().getChatName());
    assertEquals("hello", query.get().getMessage());
    assertNotNull(turnStrategy.captured);
  }

  /**
   * Test that a second message on an in-flight chat is rejected.
   */
  @Test
  public void testInFlightRejection() {
    InMemoryLogStrategy logStrategy = new InMemoryLogStrategy();
    AssistantModel model = buildModel();
    CapturingTurnStrategy turnStrategy = new CapturingTurnStrategy(model);
    KigaliAssistant assistant = buildAssistant(turnStrategy, logStrategy);
    logStrategy.createChat("chat");
    logStrategy.createChat("other");

    RecordingCallback firstCallback = new RecordingCallback();
    Optional<AssistantModelQuery> first = assistant.sendMessage("chat", "first", firstCallback);
    assertTrue(first.isPresent());

    RecordingCallback secondCallback = new RecordingCallback();
    Optional<AssistantModelQuery> second =
        assistant.sendMessage("chat", "second", secondCallback);
    assertTrue(second.isEmpty(), "Rejected message should return empty");
    assertEquals(1, secondCallback.completions.size());
    assertFalse(secondCallback.completions.get(0).getWasSuccessful());

    RecordingCallback otherCallback = new RecordingCallback();
    Optional<AssistantModelQuery> other = assistant.sendMessage("other", "other", otherCallback);
    assertTrue(other.isPresent(), "Other chats should be unaffected");
  }

  /**
   * Test that completing a turn clears the in-flight state.
   */
  @Test
  public void testCompletionClearsInFlight() {
    InMemoryLogStrategy logStrategy = new InMemoryLogStrategy();
    AssistantModel model = buildModel();
    CapturingTurnStrategy turnStrategy = new CapturingTurnStrategy(model);
    KigaliAssistant assistant = buildAssistant(turnStrategy, logStrategy);
    logStrategy.createChat("chat");

    RecordingCallback firstCallback = new RecordingCallback();
    assistant.sendMessage("chat", "first", firstCallback);
    turnStrategy.captured.onTurnComplete(AssistantResultBuilder.forToolCall("done").build());
    assertEquals(1, firstCallback.completions.size());

    RecordingCallback secondCallback = new RecordingCallback();
    Optional<AssistantModelQuery> second =
        assistant.sendMessage("chat", "second", secondCallback);
    assertTrue(second.isPresent(), "Chat should be usable after completion");
    assertTrue(secondCallback.completions.isEmpty());
  }
}
