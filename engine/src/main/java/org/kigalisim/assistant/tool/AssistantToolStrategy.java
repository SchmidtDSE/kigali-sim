/**
 * Strategy representing a tool available to the language model.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonObject;
import org.kigalisim.assistant.callback.AssistantActionCallback;

/**
 * A tool which may be invoked by the language model.
 *
 * <p>The parameter schema returned by {@link #getParameterSchema()} is a JSON Schema object which
 * the protocol strategy serializes into the request's tool definitions.</p>
 */
public interface AssistantToolStrategy {

  /**
   * Gets the unique name of the tool.
   *
   * @return The tool name.
   */
  String getName();

  /**
   * Gets the description shown to the model.
   *
   * @return The tool description.
   */
  String getDescription();

  /**
   * Gets the JSON Schema describing the tool's parameters.
   *
   * @return The parameter schema.
   */
  JsonObject getParameterSchema();

  /**
   * Executes the tool.
   *
   * @param call The arguments provided by the model.
   * @param callback The callback through which the outcome is reported.
   */
  void execute(JsonObject call, AssistantActionCallback callback);
}
