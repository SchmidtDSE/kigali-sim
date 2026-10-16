/**
 * Tool which evaluates a mathematical expression.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.parser.ParseException;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResultBuilder;

/**
 * Evaluates a single mathematical expression and returns the numeric result.
 *
 * <p>This uses EvalEx, which is a temporary dependency that is not TeaVM/WASM-safe and must be
 * replaced before the assistant is wired into the WASM facade.</p>
 */
public class CalculateTool implements AssistantToolStrategy {

  /**
   * The name by which the tool is offered to the model.
   */
  public static final String NAME = "calculate";

  private final ExpressionConfiguration configuration = ExpressionConfiguration.defaultConfiguration();

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "Evaluate a single mathematical expression and return the numeric result";
  }

  @Override
  public JsonObject getParameterSchema() {
    JsonObject expression = new JsonObject();
    expression.put("type", "string");
    expression.put("description", "The mathematical expression to evaluate, e.g. (1 + 2) * 3");

    JsonObject properties = new JsonObject();
    properties.put("expression", expression);

    JsonArray required = new JsonArray();
    required.add("expression");

    JsonObject schema = new JsonObject();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", required);
    return schema;
  }

  @Override
  public void execute(JsonObject call, AssistantActionCallback callback) {
    if (AssistantToolUtils.isMissing(call, "expression")) {
      callback.onActionComplete(AssistantToolUtils.buildMissingParameterResult("expression"));
      return;
    }
    String expression = AssistantToolUtils.getString(call, "expression");

    try {
      EvaluationValue value = new Expression(expression, configuration).evaluate();
      if (!value.isNumberValue()) {
        callback.onActionComplete(AssistantResultBuilder.forToolCallError(
            "The expression did not evaluate to a number: " + expression).build());
        return;
      }
      String output = value.getNumberValue().stripTrailingZeros().toPlainString();
      callback.onActionComplete(AssistantResultBuilder.forToolCall(output).build());
    } catch (EvaluationException | ParseException | ArithmeticException | IllegalArgumentException e) {
      callback.onActionComplete(AssistantResultBuilder.forToolCallError(e.getMessage()).build());
    }
  }
}
