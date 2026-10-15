/**
 * Callback-driven turn loop for the assistant.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kigalisim.assistant.callback.AssistantTurnCallback;
import org.kigalisim.assistant.log.ChatLog;
import org.kigalisim.assistant.log.ChatLogStrategy;
import org.kigalisim.assistant.model.AssistantModel;
import org.kigalisim.assistant.model.AssistantModelQuery;
import org.kigalisim.assistant.model.AssistantModelResponse;
import org.kigalisim.assistant.result.AssistantResult;
import org.kigalisim.assistant.result.AssistantResultFactory;
import org.kigalisim.assistant.tool.AssistantToolInvocation;
import org.kigalisim.assistant.tool.AssistantToolStrategy;
import org.kigalisim.assistant.tool.AssistantToolbelt;
import org.kigalisim.assistant.tool.EndTurnTool;
import org.kigalisim.assistant.tool.MessageUserTool;

/**
 * The untimed turn loop which drives a single assistant turn.
 *
 * <p>This is deliberately callback driven rather than a blocking loop because the wire callback may
 * arrive on a different thread on the JVM or on the event loop under WASM / TeaVM. When the wire is
 * synchronous, as in the tests, the same code path still executes deterministically.</p>
 *
 * <p>The model can only act through tools so the protocol requests {@code tool_choice: "required"}.
 * A plain-text reply is treated as an implicit {@code message_user} call followed by an end of
 * turn so that it reaches the user interface through the same path as any other message.</p>
 */
public class UntimedAssistantTurnStrategy implements AssistantTurnStrategy {

  /**
   * The default maximum number of actions permitted within a single turn.
   */
  public static final int DEFAULT_MAX_ACTIONS = 25;

  private final AssistantModel model;
  private final ChatLogStrategy chatLogStrategy;
  private final SystemContextProvider systemContextProvider;
  private final int maxActions;

  /**
   * Constructs a turn strategy with the default maximum action count.
   *
   * @param model The model used to communicate with the provider.
   * @param chatLogStrategy The persistence mechanism for chats.
   * @param systemContextProvider The provider of the assembled system context.
   */
  public UntimedAssistantTurnStrategy(
      AssistantModel model,
      ChatLogStrategy chatLogStrategy,
      SystemContextProvider systemContextProvider) {
    this(model, chatLogStrategy, systemContextProvider, DEFAULT_MAX_ACTIONS);
  }

  /**
   * Constructs a turn strategy with an explicit maximum action count.
   *
   * @param model The model used to communicate with the provider.
   * @param chatLogStrategy The persistence mechanism for chats.
   * @param systemContextProvider The provider of the assembled system context.
   * @param maxActions The maximum number of actions permitted within a turn.
   */
  public UntimedAssistantTurnStrategy(
      AssistantModel model,
      ChatLogStrategy chatLogStrategy,
      SystemContextProvider systemContextProvider,
      int maxActions) {
    this.model = model;
    this.chatLogStrategy = chatLogStrategy;
    this.systemContextProvider = systemContextProvider;
    this.maxActions = maxActions;
  }

  @Override
  public AssistantModelQuery runTurn(
      AssistantToolbelt toolbelt,
      String chatName,
      String message,
      AssistantTurnCallback callback) {
    JsonObject userMessageObject = messageObject(message);
    List<JsonObject> history;
    try {
      ChatLog log = chatLogStrategy.readChat(chatName);
      history = parseHistory(log.getWireTranscript());
      chatLogStrategy.appendChat(chatName, Jsoner.serialize(userMessageObject));
    } catch (RuntimeException e) {
      callback.onTurnComplete(AssistantResultFactory.forLlmQueryError(describe(e)));
      return new AssistantModelQuery(chatName, "", message, model);
    }

    TurnState state = new TurnState(toolbelt, chatName, message, callback, history);

    String context;
    try {
      context = model.buildRequest(
          history, toolbelt.getTools(), systemContextProvider.getSystemContext(), message);
    } catch (RuntimeException e) {
      history.add(userMessageObject);
      AssistantModelQuery query = new AssistantModelQuery(chatName, "", message, model);
      AssistantResult error =
          AssistantResultFactory.forLlmQueryError(describe(e)).getWithMessage(query);
      reportAction(state, error);
      completeTurn(state, error);
      return query;
    }
    history.add(userMessageObject);

    AssistantModelQuery query = new AssistantModelQuery(chatName, context, message, model);
    sendQuery(state, query, context);
    return query;
  }

  /**
   * Issues a model request and routes the response to the handler.
   *
   * @param state The state of the turn in progress.
   * @param query The query being issued.
   * @param context The assembled request body.
   */
  private void sendQuery(TurnState state, AssistantModelQuery query, String context) {
    state.actions++;
    try {
      model.sendMessage(context, result -> handleModelResult(state, query, result));
    } catch (RuntimeException e) {
      completeTurn(state, AssistantResultFactory.forLlmQueryError(describe(e)));
    }
  }

  /**
   * Handles a parsed model response and continues or completes the turn.
   *
   * @param state The state of the turn in progress.
   * @param query The query which produced the response.
   * @param result The result reported by the model.
   */
  private void handleModelResult(
      TurnState state, AssistantModelQuery query, AssistantResult result) {
    if (state.completed) {
      return;
    }

    AssistantResult reported = result.getWithMessage(query);
    reportAction(state, reported);

    if (!result.getWasSuccessful()) {
      completeTurn(state, reported);
      return;
    }

    AssistantModelResponse response = result.getRawResponse().orElse(null);
    if (response == null) {
      completeTurn(
          state,
          AssistantResultFactory.forLlmQueryError("Model returned no response").getWithMessage(query));
      return;
    }

    appendToLog(state, response.getUninterpreted());

    List<AssistantToolInvocation> invocations = response.getToolCalls();
    if (invocations.isEmpty()) {
      handlePlainText(state, response);
      return;
    }

    boolean endTurn = false;
    AssistantResult endTurnResult = null;
    for (AssistantToolInvocation invocation : invocations) {
      AssistantResult invocationResult = executeTool(state, invocation);
      if (EndTurnTool.NAME.equals(invocation.getToolName())) {
        endTurn = true;
        endTurnResult = invocationResult;
      }
    }

    if (endTurn) {
      completeTurn(state, endTurnResult == null ? reported : endTurnResult);
      return;
    }

    if (state.actions >= maxActions) {
      completeTurn(
          state,
          AssistantResultFactory.forTurnActionsExceeded(
              "Turn exceeded the maximum of " + maxActions + " actions"));
      return;
    }

    String nextContext;
    try {
      nextContext = model.buildRequest(
          state.history, state.toolbelt.getTools(), systemContextProvider.getSystemContext(), null);
    } catch (RuntimeException e) {
      completeTurn(state, AssistantResultFactory.forLlmQueryError(describe(e)));
      return;
    }

    AssistantModelQuery nextQuery =
        new AssistantModelQuery(state.chatName, nextContext, state.userMessage, model);
    sendQuery(state, nextQuery, nextContext);
  }

  /**
   * Treats a plain-text reply as an implicit message to the user followed by an end of turn.
   *
   * @param state The state of the turn in progress.
   * @param response The response which contained no tool calls.
   */
  private void handlePlainText(TurnState state, AssistantModelResponse response) {
    String content = response.getContent().orElse("");
    JsonObject call = new JsonObject();
    call.put("message", content);
    AssistantToolInvocation synthetic =
        new AssistantToolInvocation(MessageUserTool.NAME, "", call);
    AssistantResult result =
        AssistantResultFactory.forToolCall(content).getWithToolInvocation(synthetic);
    state.actions++;
    reportAction(state, result);
    completeTurn(state, result);
  }

  /**
   * Executes a single tool invocation and appends its result to the transcript.
   *
   * @param state The state of the turn in progress.
   * @param invocation The invocation to execute.
   * @return The result with the invocation attached.
   */
  private AssistantResult executeTool(TurnState state, AssistantToolInvocation invocation) {
    state.actions++;
    String name = invocation.getToolName();
    Optional<AssistantToolStrategy> toolOptional = state.toolbelt.getTool(name);

    if (toolOptional.isEmpty()) {
      String error = "No tool named '" + name + "' is registered";
      appendToolMessage(state, invocation.getCallId(), error);
      return reportToolResult(
          state, AssistantResultFactory.forToolCallError(error).getWithToolInvocation(invocation));
    }

    AssistantToolStrategy tool = toolOptional.get();
    AssistantResult[] captured = new AssistantResult[1];
    try {
      tool.execute(invocation.getCall(), result -> captured[0] = result);
    } catch (RuntimeException e) {
      String error = describe(e);
      appendToolMessage(state, invocation.getCallId(), error);
      return reportToolResult(
          state, AssistantResultFactory.forToolCallError(error).getWithToolInvocation(invocation));
    }

    AssistantResult result = captured[0];
    if (result == null) {
      String error = "Tool '" + name + "' did not report a result";
      appendToolMessage(state, invocation.getCallId(), error);
      result = AssistantResultFactory.forToolCallError(error);
    } else {
      appendToolMessage(state, invocation.getCallId(), outputOf(result));
    }
    return reportToolResult(state, result.getWithToolInvocation(invocation));
  }

  /**
   * Reports a tool result to the action callback.
   *
   * @param state The state of the turn in progress.
   * @param result The result to report.
   * @return The reported result.
   */
  private AssistantResult reportToolResult(TurnState state, AssistantResult result) {
    reportAction(state, result);
    return result;
  }

  /**
   * Gets the textual output carried by a tool result.
   *
   * @param result The tool result.
   * @return The output text or the error message when the tool failed.
   */
  private String outputOf(AssistantResult result) {
    return result
        .getRawResponse()
        .flatMap(AssistantModelResponse::getContent)
        .orElseGet(() -> result.getError().orElse(""));
  }

  /**
   * Appends a tool result message to the transcript.
   *
   * @param state The state of the turn in progress.
   * @param callId The provider-assigned call identifier.
   * @param content The textual tool output.
   */
  private void appendToolMessage(TurnState state, String callId, String content) {
    JsonObject toolMessage = new JsonObject();
    toolMessage.put("role", "tool");
    toolMessage.put("tool_call_id", callId);
    toolMessage.put("content", content);
    appendToLog(state, toolMessage);
  }

  /**
   * Appends a message to both the persisted transcript and the in-memory history.
   *
   * @param state The state of the turn in progress.
   * @param message The message to append.
   */
  private void appendToLog(TurnState state, JsonObject message) {
    chatLogStrategy.appendChat(state.chatName, Jsoner.serialize(message));
    state.history.add(message);
  }

  /**
   * Reports an action result to the caller.
   *
   * @param state The state of the turn in progress.
   * @param result The result to report.
   */
  private void reportAction(TurnState state, AssistantResult result) {
    state.callback.onActionComplete(result);
  }

  /**
   * Completes the turn exactly once.
   *
   * @param state The state of the turn in progress.
   * @param result The terminal result.
   */
  private void completeTurn(TurnState state, AssistantResult result) {
    if (!state.completed) {
      state.completed = true;
      state.callback.onTurnComplete(result);
    }
  }

  /**
   * Parses a stored wire transcript into individual messages.
   *
   * @param transcript The stored wire transcript.
   * @return The parsed messages in order.
   */
  private List<JsonObject> parseHistory(String transcript) {
    List<JsonObject> history = new ArrayList<>();
    try {
      Object parsed = Jsoner.deserialize(transcript);
      if (parsed instanceof JsonArray array) {
        for (Object item : array) {
          if (item instanceof JsonObject object) {
            history.add(object);
          }
        }
      }
    } catch (JsonException e) {
      throw new IllegalStateException("Stored transcript is not valid JSON", e);
    }
    return history;
  }

  /**
   * Builds a user message object.
   *
   * @param message The user message text.
   * @return The message object.
   */
  private JsonObject messageObject(String message) {
    JsonObject user = new JsonObject();
    user.put("role", "user");
    user.put("content", message);
    return user;
  }

  /**
   * Describes a throwable, falling back to its class name when there is no message.
   *
   * @param throwable The throwable to describe.
   * @return The throwable message, or its class name as a fallback.
   */
  private static String describe(Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null || message.isBlank()) {
      return throwable.getClass().getName();
    } else {
      return message;
    }
  }

  /**
   * Mutable state for a single turn.
   */
  private static final class TurnState {

    private final AssistantToolbelt toolbelt;
    private final String chatName;
    private final String userMessage;
    private final AssistantTurnCallback callback;
    private final List<JsonObject> history;
    private int actions;
    private boolean completed;

    /**
     * Constructs turn state.
     *
     * @param toolbelt The tools available to the assistant.
     * @param chatName The chat name.
     * @param userMessage The user message which started the turn.
     * @param callback The callback which observes the turn.
     * @param history The in-memory message history.
     */
    private TurnState(
        AssistantToolbelt toolbelt,
        String chatName,
        String userMessage,
        AssistantTurnCallback callback,
        List<JsonObject> history) {
      this.toolbelt = toolbelt;
      this.chatName = chatName;
      this.userMessage = userMessage;
      this.callback = callback;
      this.history = history;
    }
  }
}
