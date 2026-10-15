/**
 * Transport strategy for communicating with a language model provider.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

import org.kigalisim.assistant.callback.AssistantActionCallback;

/**
 * Handles transport for a model request.
 *
 * <p>A wire strategy does not build requests, does not know about tools, and does not parse
 * responses. Streaming is out of scope for this prototype: the complete body is delivered in a
 * single callback. Success is reported with a query result carrying
 * {@code AssistantModelResponse.forRawBody(raw)} while failure is reported with a failed query
 * result.</p>
 */
public interface AssistantModelWireStrategy {

  /**
   * Sends a request body to the provider.
   *
   * @param body The full context including history plus the new message.
   * @param callback The callback invoked with the transport outcome.
   * @return A handle which allows the in-flight request to be cancelled.
   */
  AssistantRequestHandle sendMessage(String body, AssistantActionCallback callback);
}
