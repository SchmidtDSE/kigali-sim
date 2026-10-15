/**
 * Built-in tool which signals the end of a turn.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonObject;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResult;

/**
 * Signals the turn strategy to conclude the current turn.
 *
 * <p>This tool does not end the turn itself. It reports an action result so that the batch has a
 * result to append, and the turn strategy concludes the turn once the whole batch is complete.</p>
 */
public final class EndTurnTool implements AssistantToolStrategy {

  /**
   * The name by which the tool is offered to the model.
   */
  public static final String NAME = "end_turn";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "Conclude the assistant's turn and return control to the user";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject schema = new JsonObject();
    schema.put("type", "object");
    schema.put("properties", new JsonObject());
    return schema;
  }

  @Override
  public void execute(JsonObject call, AssistantActionCallback callback) {
    callback.onActionComplete(AssistantResult.forToolCall("Turn complete."));
  }
}
