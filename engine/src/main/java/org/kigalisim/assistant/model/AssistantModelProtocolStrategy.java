/**
 * Protocol strategy for forming requests and parsing responses.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.List;
import org.kigalisim.assistant.tool.AssistantToolStrategy;

/**
 * Handles request formation and response parsing for a model provider protocol.
 *
 * <p>This is separated from transport so that the OpenAI-compatible wire format can be exercised
 * in tests without any network by mocking only the wire strategy.</p>
 */
public interface AssistantModelProtocolStrategy {

  /**
   * Forms a provider request.
   *
   * @param history The verbatim history of prior messages.
   * @param tools The tools offered to the model.
   * @param systemContext The system context supplied to the model.
   * @param userMessage The new user message, or null when the history already contains it.
   * @return The serialized request body.
   */
  String buildRequest(
      List<JsonObject> history,
      Iterable<AssistantToolStrategy> tools,
      String systemContext,
      String userMessage);

  /**
   * Parses a provider response into content, tool calls, and finish reason.
   *
   * @param rawBody The raw response body.
   * @return The parsed response.
   */
  AssistantModelResponse parseResponse(String rawBody);

  /**
   * Derives a human readable transcript from a stored wire transcript.
   *
   * @param wireTranscript The stored wire transcript.
   * @return The human readable transcript.
   */
  String renderHumanTranscript(String wireTranscript);
}
