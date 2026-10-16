/**
 * Mock wire strategy for tests.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.result.AssistantResultBuilder;

/**
 * A hand written wire strategy which delivers queued outcomes inline.
 *
 * <p>Each queued outcome is either a raw success payload, an error string, or an exception. The
 * strategy records the request bodies it was asked to send so that tests can inspect the wire
 * format.</p>
 */
public class MockModelWireStrategy implements AssistantModelWireStrategy {

  private final Deque<Consumer<AssistantActionCallback>> queue = new ArrayDeque<>();
  private final List<String> sentBodies = new ArrayList<>();

  /**
   * Queues a successful raw payload.
   *
   * @param payload The raw provider payload.
   */
  public void enqueuePayload(String payload) {
    AssistantModelResponse response = AssistantModelResponseFactory.forRawBody(payload);
    AssistantResult result = AssistantResultBuilder.forLlmQuery(response).build();
    queue.add(callback -> callback.onActionComplete(result));
  }

  /**
   * Queues a transport error.
   *
   * @param error The error message.
   */
  public void enqueueError(String error) {
    AssistantResult result = AssistantResultBuilder.forLlmQueryError(error).build();
    queue.add(callback -> callback.onActionComplete(result));
  }

  /**
   * Queues an exception to be thrown by the wire strategy.
   *
   * @param exception The exception to throw.
   */
  public void enqueueException(RuntimeException exception) {
    queue.add(callback -> {
      throw exception;
    });
  }

  /**
   * Gets the bodies sent through this strategy.
   *
   * @return The sent bodies in order.
   */
  public List<String> getSentBodies() {
    return sentBodies;
  }

  @Override
  public AssistantRequestHandle sendMessage(String body, AssistantActionCallback callback) {
    sentBodies.add(body);
    Consumer<AssistantActionCallback> next = queue.poll();
    if (next == null) {
      throw new IllegalStateException("No mocked wire response was queued");
    }
    next.accept(callback);
    return () -> {
    };
  }
}
