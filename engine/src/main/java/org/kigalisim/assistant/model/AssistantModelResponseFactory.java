/**
 * Factory for model responses.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.List;
import java.util.Optional;
import org.kigalisim.assistant.tool.AssistantToolInvocation;

/**
 * Creates model responses at the various stages of interpretation.
 *
 * <p>A response is created either by a wire strategy as a success envelope carrying only the raw
 * body or by a protocol strategy once the body has been parsed into content, tool calls, and a
 * finish reason.</p>
 */
public class AssistantModelResponseFactory {

  /**
   * Constructs a factory.
   *
   * <p>Marked private so that a factory is not created by accident; the static creators are the
   * intended entry points.</p>
   */
  private AssistantModelResponseFactory() {
  }

  /**
   * Creates a success envelope carrying only the raw wire body.
   *
   * <p>This is what a wire strategy reports before parsing has taken place.</p>
   *
   * @param raw The raw body returned by the provider.
   * @return A response carrying only the raw body.
   */
  public static AssistantModelResponse forRawBody(String raw) {
    return new AssistantModelResponse(
        Optional.empty(),
        List.of(),
        Optional.empty(),
        new JsonObject(),
        raw
    );
  }

  /**
   * Creates a response representing the textual output of a tool.
   *
   * @param output The text produced by the tool.
   * @return A response whose content and raw body are the tool output.
   */
  public static AssistantModelResponse forToolOutput(String output) {
    return new AssistantModelResponse(
        Optional.of(output),
        List.of(),
        Optional.empty(),
        new JsonObject(),
        output
    );
  }

  /**
   * Creates a parsed response from the values extracted by a protocol strategy.
   *
   * @param content The assistant message content, if present.
   * @param toolCalls The tool invocations requested by the assistant, in order.
   * @param finishReason The provider finish reason, if present.
   * @param uninterpreted The assistant message exactly as received for verbatim replay.
   * @param raw The raw body returned by the provider.
   * @return A fully parsed response.
   */
  public static AssistantModelResponse forParsed(
      Optional<String> content,
      List<AssistantToolInvocation> toolCalls,
      Optional<String> finishReason,
      JsonObject uninterpreted,
      String raw) {
    return new AssistantModelResponse(
        content,
        List.copyOf(toolCalls),
        finishReason,
        uninterpreted,
        raw
    );
  }
}
