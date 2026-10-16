/**
 * Shared helpers for assistant tool implementations.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonObject;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.result.AssistantResultBuilder;

/**
 * Small helpers shared by the tool strategy implementations.
 *
 * <p>Tool argument objects are loosely typed, so the required-parameter check and the failed-call
 * result which reports a missing parameter are centralized here.</p>
 */
final class AssistantToolUtils {

  private AssistantToolUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Determines whether a required string parameter is absent, non-string, or blank.
   *
   * @param call The tool call arguments.
   * @param name The name of the parameter.
   * @return True when the parameter cannot be used as a non-blank string.
   */
  static boolean isMissing(JsonObject call, String name) {
    Object raw = call.get(name);
    return !(raw instanceof String value) || value.isBlank();
  }

  /**
   * Reads a string parameter, returning null when it is absent or not a string.
   *
   * @param call The tool call arguments.
   * @param name The name of the parameter.
   * @return The string value, or null when the parameter is absent or not a string.
   */
  static String getString(JsonObject call, String name) {
    Object raw = call.get(name);
    return raw instanceof String value ? value : null;
  }

  /**
   * Builds the failed tool call result reported for a missing required parameter.
   *
   * @param name The name of the missing parameter.
   * @return The failed tool call result.
   */
  static AssistantResult buildMissingParameterResult(String name) {
    return AssistantResultBuilder.forToolCallError("Missing required parameter: " + name).build();
  }
}
