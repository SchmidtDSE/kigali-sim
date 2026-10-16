/**
 * Data transfer object describing a parsed or raw model response.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.kigalisim.assistant.tool.AssistantToolInvocation;

/**
 * The result of communicating with a language model provider.
 *
 * <p>This is deliberately a data transfer object. It is produced either by the wire strategy as a
 * success envelope carrying only the raw body or by the protocol strategy once the body has been
 * parsed into content, tool calls, and a finish reason.</p>
 */
public class AssistantModelResponse {

  private final Optional<String> content;
  private final List<AssistantToolInvocation> toolCalls;
  private final Optional<String> finishReason;
  private final JsonObject uninterpreted;
  private final String raw;

  /**
   * Constructs a response.
   *
   * <p>Most callers should use {@link AssistantModelResponseFactory} instead of constructing
   * responses directly.</p>
   *
   * @param content The textual content of the assistant message, or empty when none was supplied.
   * @param toolCalls The tool invocations requested by the assistant, in order.
   * @param finishReason The provider-reported finish reason, or empty when none was supplied.
   * @param uninterpreted The assistant message exactly as received for verbatim replay.
   * @param raw The raw body returned by the provider.
   */
  public AssistantModelResponse(
      Optional<String> content,
      List<AssistantToolInvocation> toolCalls,
      Optional<String> finishReason,
      JsonObject uninterpreted,
      String raw) {
    this.content = content;
    this.toolCalls = toolCalls;
    this.finishReason = finishReason;
    this.uninterpreted = uninterpreted;
    this.raw = raw;
  }

  /**
   * Gets the textual content of the assistant message.
   *
   * @return The content or empty when the provider supplied none.
   */
  public Optional<String> getContent() {
    return content;
  }

  /**
   * Gets the tool invocations requested by the assistant.
   *
   * @return The invocations in the order returned, never null but possibly empty.
   */
  public List<AssistantToolInvocation> getToolCalls() {
    return Collections.unmodifiableList(toolCalls);
  }

  /**
   * Gets the provider-reported finish reason.
   *
   * @return The finish reason or empty when the provider supplied none.
   */
  public Optional<String> getFinishReason() {
    return finishReason;
  }

  /**
   * Gets the assistant message exactly as received.
   *
   * <p>This is required for verbatim replay into history: an assistant message carrying tool calls
   * must never be rebuilt from parsed fields because that can silently drop provider fields and
   * produce a request the provider rejects.</p>
   *
   * @return The uninterpreted assistant message, possibly empty.
   */
  public JsonObject getUninterpreted() {
    return uninterpreted;
  }

  /**
   * Gets the raw provider payload.
   *
   * @return The raw String payload provided.
   */
  public String getRaw() {
    return raw;
  }
}
