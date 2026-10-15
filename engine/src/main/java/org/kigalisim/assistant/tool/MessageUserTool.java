/**
 * Built-in tool through which the assistant speaks to the user.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResult;

/**
 * Delivers a markdown message from the assistant to the user.
 *
 * <p>The message text is delivered through the action callback so that a user interface can render
 * it, and it is also returned to the model as the tool result.</p>
 */
public final class MessageUserTool implements AssistantToolStrategy {

  /**
   * The name by which the tool is offered to the model.
   */
  public static final String NAME = "message_user";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "Send a markdown message to the user";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject message = new JsonObject();
    message.put("type", "string");
    message.put("description", "The markdown message to show the user");
    JsonObject properties = new JsonObject();
    properties.put("message", message);
    JsonArray required = new JsonArray();
    required.add("message");
    JsonObject schema = new JsonObject();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", required);
    return schema;
  }

  @Override
  public void execute(JsonObject call, AssistantActionCallback callback) {
    Object raw = call.get("message");
    if (raw instanceof String message && !message.isBlank()) {
      callback.onActionComplete(AssistantResult.forToolCall(message));
    } else {
      callback.onActionComplete(
          AssistantResult.forToolCallError("Missing required parameter: message"));
    }
  }
}
