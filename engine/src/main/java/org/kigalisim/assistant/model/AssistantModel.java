/**
 * Access to the language model through wire and protocol strategies.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.List;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.result.AssistantResultBuilder;
import org.kigalisim.assistant.tool.AssistantToolStrategy;

/**
 * Provides access to the language model.
 *
 * <p>This manages the complexities of the OpenAI-compatible API and abstracts over the JVM and
 * WASM network differences. It is a template method which delegates transport to an
 * {@link AssistantModelWireStrategy} and request formation / response parsing to an
 * {@link AssistantModelProtocolStrategy}.</p>
 */
public class AssistantModel {

  private static final AssistantRequestHandle NO_OP_HANDLE = () -> {};

  private final AssistantModelWireStrategy wire;
  private final AssistantModelProtocolStrategy protocol;
  private final String modelName;
  private final String modelService;

  /**
   * Constructs a model from its strategies.
   *
   * @param wire The transport strategy.
   * @param protocol The request formation and response parsing strategy.
   * @param modelName The name of the model in use.
   * @param modelService Information about where the model is running.
   */
  public AssistantModel(
      AssistantModelWireStrategy wire,
      AssistantModelProtocolStrategy protocol,
      String modelName,
      String modelService) {
    this.wire = wire;
    this.protocol = protocol;
    this.modelName = modelName;
    this.modelService = modelService;
  }

  /**
   * Gets the name of the model in use.
   *
   * @return The model name.
   */
  public String getModelName() {
    return modelName;
  }

  /**
   * Gets information about where the model is running.
   *
   * @return A description of the model service.
   */
  public String getModelService() {
    return modelService;
  }

  /**
   * Forms a provider request.
   *
   * <p>This is a thin delegate so that the turn strategy never touches the protocol strategy
   * directly.</p>
   *
   * @param history The verbatim history of prior messages.
   * @param tools The tools offered to the model.
   * @param systemContext The system context supplied to the model.
   * @param userMessage The new user message, or null when the history already contains it.
   * @return The serialized request body.
   */
  public String buildRequest(
      List<JsonObject> history,
      Iterable<AssistantToolStrategy> tools,
      String systemContext,
      String userMessage) {
    return protocol.buildRequest(history, tools, systemContext, userMessage);
  }

  /**
   * Sends a request and parses the response.
   *
   * <p>Transport is delegated to the wire strategy. On a successful transport outcome the raw body
   * is parsed by the protocol strategy and a fully populated result is forwarded to the caller. A
   * transport failure is forwarded unchanged, and any exception thrown while parsing becomes a
   * failed query result.</p>
   *
   * @param body The full request body.
   * @param callback The callback invoked with the parsed outcome.
   * @return A handle which allows the in-flight request to be cancelled.
   */
  public AssistantRequestHandle sendMessage(String body, AssistantActionCallback callback) {
    AssistantActionCallback forwardingCallback = result -> {
      boolean wireSucceeded = result.getWasSuccessful() && result.getRawResponse().isPresent();
      if (wireSucceeded) {
        sendMessageWireSucceeded(result, callback);
      } else {
        callback.onActionComplete(result);
      }
    };

    try {
      return wire.sendMessage(body, forwardingCallback);
    } catch (RuntimeException e) {
      AssistantResult failure = AssistantResultBuilder.forLlmQueryError(describe(e)).build();
      forwardingCallback.onActionComplete(failure);
      return NO_OP_HANDLE;
    }
  }

  /**
   * Forwards a successful transport outcome, parsing the raw body on the way.
   *
   * <p>Any exception thrown while parsing becomes a failed query result so that a malformed
   * provider payload is reported rather than crashing the turn.</p>
   *
   * @param result The successful result reported by the wire strategy.
   * @param callback The callback invoked with the parsed outcome.
   */
  private void sendMessageWireSucceeded(
      AssistantResult result, AssistantActionCallback callback) {
    AssistantModelResponse rawResponse = result.getRawResponse().orElseThrow();
    AssistantModelResponse parsed;
    try {
      parsed = protocol.parseResponse(rawResponse.getRaw());
    } catch (RuntimeException e) {
      AssistantResult failure = AssistantResultBuilder.forLlmQueryError(describe(e)).build();
      callback.onActionComplete(failure);
      return;
    }
    AssistantResult parsedResult = AssistantResultBuilder.forLlmQuery(parsed).build();
    callback.onActionComplete(parsedResult);
  }

  /**
   * Describes a throwable, falling back to its class name when there is no message.
   *
   * @param throwable The throwable to describe.
   * @return The throwable message, or its class name as a fallback.
   */
  private static String describe(Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null || message.isBlank()) {
      return throwable.getClass().getName();
    } else {
      return message;
    }
  }
}
