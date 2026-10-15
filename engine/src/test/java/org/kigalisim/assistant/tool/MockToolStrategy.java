/**
 * Mock tool strategy for tests.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.kigalisim.assistant.callback.AssistantActionCallback;
import org.kigalisim.assistant.result.AssistantResult;

/**
 * A configurable tool strategy which records the calls it receives.
 */
public class MockToolStrategy implements AssistantToolStrategy {

  private final String name;
  private final String output;
  private final RuntimeException exception;
  private final JsonObject schema;
  private final List<JsonObject> invocations = new ArrayList<>();

  /**
   * Constructs a tool which returns a fixed successful output.
   *
   * @param name The tool name.
   * @param output The fixed output.
   */
  public MockToolStrategy(String name, String output) {
    this(name, output, null);
  }

  /**
   * Constructs a tool which either returns a fixed output or throws.
   *
   * @param name The tool name.
   * @param output The fixed output.
   * @param exception The exception to throw, or null to return the output.
   */
  public MockToolStrategy(String name, String output, RuntimeException exception) {
    this.name = name;
    this.output = output;
    this.exception = exception;
    this.schema = new JsonObject();
    schema.put("type", "object");
    schema.put("properties", new JsonObject());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return "Mock tool " + name;
  }

  @Override
  public JsonObject getParameterSchema() {
    return schema;
  }

  /**
   * Gets the calls recorded by this tool.
   *
   * @return The recorded calls in order.
   */
  public List<JsonObject> getInvocations() {
    return invocations;
  }

  @Override
  public void execute(JsonObject call, AssistantActionCallback callback) {
    invocations.add(call);
    if (exception != null) {
      throw exception;
    }
    callback.onActionComplete(AssistantResult.forToolCall(output));
  }
}
