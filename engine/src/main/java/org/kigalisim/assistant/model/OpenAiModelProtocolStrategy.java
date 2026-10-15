/**
 * OpenAI-compatible model protocol strategy.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kigalisim.assistant.tool.AssistantToolInvocation;
import org.kigalisim.assistant.tool.AssistantToolStrategy;

/**
 * Request formation and response parsing for OpenAI-compatible chat completion APIs.
 *
 * <p>This is a real implementation so that the wire format can be exercised against captured
 * provider payloads without any network. It uses only the reflection-free subset of Clifton Labs
 * json-simple ({@code JsonObject}, {@code JsonArray}, and {@code Jsoner}), which is safe under
 * TeaVM / WASM.</p>
 */
public class OpenAiModelProtocolStrategy implements AssistantModelProtocolStrategy {

  private final String modelName;

  /**
   * Constructs a protocol strategy for the given model.
   *
   * @param modelName The model name placed into requests.
   */
  public OpenAiModelProtocolStrategy(String modelName) {
    this.modelName = modelName;
  }

  @Override
  public String buildRequest(
      List<JsonObject> history,
      Iterable<AssistantToolStrategy> tools,
      String systemContext,
      String userMessage) {
    JsonObject request = new JsonObject();
    request.put("model", modelName);
    request.put("messages", buildMessages(history, systemContext, userMessage));
    request.put("tools", buildTools(tools));
    request.put("tool_choice", "required");
    return Jsoner.serialize(request);
  }

  /**
   * Assembles the message array with the system message first and the user message last.
   *
   * @param history The verbatim history of prior messages.
   * @param systemContext The system context supplied to the model.
   * @param userMessage The new user message, or null when the history already contains it.
   * @return The message array.
   */
  private JsonArray buildMessages(
      List<JsonObject> history, String systemContext, String userMessage) {
    JsonArray messages = new JsonArray();
    JsonObject system = new JsonObject();
    system.put("role", "system");
    system.put("content", systemContext);
    messages.add(system);

    for (JsonObject message : history) {
      messages.add(message);
    }

    if (userMessage != null) {
      JsonObject user = new JsonObject();
      user.put("role", "user");
      user.put("content", userMessage);
      messages.add(user);
    }

    return messages;
  }

  /**
   * Serializes the offered tools into the provider's tool definition format.
   *
   * @param tools The tools offered to the model.
   * @return The array of tool definitions.
   */
  private JsonArray buildTools(Iterable<AssistantToolStrategy> tools) {
    JsonArray definitions = new JsonArray();

    for (AssistantToolStrategy tool : tools) {
      JsonObject function = new JsonObject();
      function.put("name", tool.getName());
      function.put("description", tool.getDescription());
      function.put("parameters", tool.getParameterSchema());

      JsonObject definition = new JsonObject();
      definition.put("type", "function");
      definition.put("function", function);

      definitions.add(definition);
    }

    return definitions;
  }

  @Override
  public AssistantModelResponse parseResponse(String rawBody) {
    JsonObject root = parseObject(rawBody, "response");
    JsonArray choices = requireArray(root.get("choices"), "choices");
    if (choices.isEmpty()) {
      throw new IllegalStateException("Provider response did not include any choices");
    }
    JsonObject choice = requireObject(choices.get(0), "choice");
    JsonObject message = requireObject(choice.get("message"), "message");
    Optional<String> content = Optional.ofNullable(asString(message.get("content")));
    Optional<String> finishReason = Optional.ofNullable(asString(choice.get("finish_reason")));
    List<AssistantToolInvocation> toolCalls = parseToolCalls(message);
    return AssistantModelResponseFactory.forParsed(
        content,
        toolCalls,
        finishReason,
        message,
        rawBody
    );
  }

  @Override
  public String renderHumanTranscript(String wireTranscript) {
    throw new UnsupportedOperationException(
        "Human transcript rendering is not implemented until the human transcript component"
    );
  }

  /**
   * Parses the tool calls carried by an assistant message.
   *
   * @param message The assistant message as received.
   * @return The invocations in order, never null but possibly empty.
   */
  private List<AssistantToolInvocation> parseToolCalls(JsonObject message) {
    Object toolCallsRaw = message.get("tool_calls");
    if (!(toolCallsRaw instanceof JsonArray toolCallsArray)) {
      return List.of();
    }
    List<AssistantToolInvocation> invocations = new ArrayList<>();
    for (Object toolCallRaw : toolCallsArray) {
      if (toolCallRaw instanceof JsonObject toolCall) {
        invocations.add(parseToolCall(toolCall));
      }
    }
    return invocations;
  }

  /**
   * Parses a single tool call.
   *
   * @param toolCall The tool call object as received.
   * @return The parsed invocation.
   */
  private AssistantToolInvocation parseToolCall(JsonObject toolCall) {
    String callId = asString(toolCall.get("id"));
    String name = null;
    JsonObject arguments = new JsonObject();
    Object functionRaw = toolCall.get("function");
    if (functionRaw instanceof JsonObject function) {
      name = asString(function.get("name"));
      arguments = parseArguments(function.get("arguments"));
    }
    return new AssistantToolInvocation(name, callId, arguments);
  }

  /**
   * Parses tool arguments, requiring them to be a JSON object.
   *
   * @param argumentsRaw The arguments value as received.
   * @return The parsed argument object.
   * @throws IllegalStateException If the arguments are not a JSON object.
   */
  private JsonObject parseArguments(Object argumentsRaw) {
    if (argumentsRaw instanceof JsonObject arguments) {
      return arguments;
    } else if (argumentsRaw instanceof String argumentsString) {
      try {
        Object parsed = Jsoner.deserialize(argumentsString);
        if (parsed instanceof JsonObject arguments) {
          return arguments;
        } else {
          throw new IllegalStateException(
              "Tool arguments were not a JSON object: " + abbreviate(argumentsString));
        }
      } catch (JsonException e) {
        throw new IllegalStateException(
            "Unable to parse tool arguments: " + abbreviate(argumentsString), e);
      }
    } else {
      throw new IllegalStateException(
          "Tool arguments were not a JSON object: " + abbreviate(String.valueOf(argumentsRaw)));
    }
  }

  /**
   * Parses a String as a JSON object.
   *
   * @param raw The JSON text.
   * @param label A label used in the error message.
   * @return The parsed object.
   */
  private JsonObject parseObject(String raw, String label) {
    try {
      Object parsed = Jsoner.deserialize(raw);
      if (parsed instanceof JsonObject object) {
        return object;
      }
      throw new IllegalStateException(
          "Provider " + label + " was not a JSON object: " + abbreviate(raw));
    } catch (JsonException e) {
      throw new IllegalStateException(
          "Unable to parse provider " + label + ": " + abbreviate(raw), e);
    }
  }

  /**
   * Requires a value to be a JSON array.
   *
   * @param value The value to inspect.
   * @param label A label used in the error message.
   * @return The array.
   */
  private JsonArray requireArray(Object value, String label) {
    if (value instanceof JsonArray array) {
      return array;
    }
    throw new IllegalStateException("Provider response did not include a " + label + " array");
  }

  /**
   * Requires a value to be a JSON object.
   *
   * @param value The value to inspect.
   * @param label A label used in the error message.
   * @return The object.
   */
  private JsonObject requireObject(Object value, String label) {
    if (value instanceof JsonObject object) {
      return object;
    }
    throw new IllegalStateException("Provider response did not include a " + label + " object");
  }

  /**
   * Converts a JSON value to a String when it is a string.
   *
   * @param value The value to convert.
   * @return The string or null when the value is not a string.
   */
  private String asString(Object value) {
    if (value instanceof String string) {
      return string;
    } else {
      return null;
    }
  }

  /**
   * Shortens a payload for use in an error message.
   *
   * @param raw The payload text.
   * @return The payload, truncated when long.
   */
  private String abbreviate(String raw) {
    if (raw == null) {
      return "null";
    } else if (raw.length() <= 200) {
      return raw;
    } else {
      return raw.substring(0, 200) + "...";
    }
  }
}
