/**
 * Data transfer object describing a tool invocation.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * A tool invocation requested by the language model.
 */
public final class AssistantToolInvocation {

  private final String toolName;
  private final String callId;
  private final JsonObject call;

  /**
   * Constructs an invocation.
   *
   * @param toolName The name of the tool invoked.
   * @param callId The provider-assigned identifier used to match the result back to the request.
   * @param call The arguments provided by the model.
   */
  public AssistantToolInvocation(String toolName, String callId, JsonObject call) {
    this.toolName = toolName;
    this.callId = callId;
    this.call = call;
  }

  /**
   * Gets the name of the tool invoked.
   *
   * @return The tool name.
   */
  public String getToolName() {
    return toolName;
  }

  /**
   * Gets the provider-assigned identifier.
   *
   * @return The call identifier.
   */
  public String getCallId() {
    return callId;
  }

  /**
   * Gets the arguments provided by the model.
   *
   * @return The argument object.
   */
  public JsonObject getCall() {
    return call;
  }
}
