/**
 * Builder for the Kigali Sim AI assistant facade.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import org.kigalisim.assistant.log.ChatLogStrategy;
import org.kigalisim.assistant.model.AssistantModel;
import org.kigalisim.assistant.tool.AssistantToolStrategy;
import org.kigalisim.assistant.tool.EndTurnTool;
import org.kigalisim.assistant.tool.MessageUserTool;
import org.kigalisim.assistant.tool.MutableAssistantToolbelt;
import org.kigalisim.assistant.workspace.AssistantWorkspace;

/**
 * Builds a {@link KigaliAssistant}.
 *
 * <p>The built-in tools are registered first so that they are always available. Additional tools
 * are added through {@link #addTool(AssistantToolStrategy)} and duplicate names are rejected.</p>
 */
public class KigaliAssistantBuilder {

  private final MutableAssistantToolbelt toolbelt = new MutableAssistantToolbelt();
  private AssistantModel model;
  private AssistantTurnStrategy turnStrategy;
  private ChatLogStrategy chatLogStrategy;
  private AssistantWorkspace workspace;

  /**
   * Constructs a builder with the built-in tools registered first.
   */
  public KigaliAssistantBuilder() {
    addDefaultTools();
  }

  /**
   * Registers the tools required by the harness itself.
   *
   * <p>These are registered before any additional tool so that the turn loop can always speak to
   * the user through {@code message_user} and conclude a turn through {@code end_turn}, no matter
   * which additional tools a caller registers afterwards.</p>
   */
  private void addDefaultTools() {
    toolbelt.addTool(new EndTurnTool());
    toolbelt.addTool(new MessageUserTool());
  }

  /**
   * Registers an additional tool.
   *
   * @param tool The tool to register.
   * @return This builder for chaining.
   * @throws IllegalArgumentException If a tool with the same name is already registered.
   */
  public KigaliAssistantBuilder addTool(AssistantToolStrategy tool) {
    toolbelt.addTool(tool);
    return this;
  }

  /**
   * Sets the model used to communicate with the provider.
   *
   * @param model The model.
   * @return This builder for chaining.
   */
  public KigaliAssistantBuilder setModel(AssistantModel model) {
    this.model = model;
    return this;
  }

  /**
   * Sets the turn strategy.
   *
   * @param turnStrategy The turn strategy.
   * @return This builder for chaining.
   */
  public KigaliAssistantBuilder setTurnStrategy(AssistantTurnStrategy turnStrategy) {
    this.turnStrategy = turnStrategy;
    return this;
  }

  /**
   * Sets the chat log persistence strategy.
   *
   * @param chatLogStrategy The chat log strategy.
   * @return This builder for chaining.
   */
  public KigaliAssistantBuilder setChatLogStrategy(ChatLogStrategy chatLogStrategy) {
    this.chatLogStrategy = chatLogStrategy;
    return this;
  }

  /**
   * Sets the optional workspace.
   *
   * @param workspace The workspace.
   * @return This builder for chaining.
   */
  public KigaliAssistantBuilder setWorkspace(AssistantWorkspace workspace) {
    this.workspace = workspace;
    return this;
  }

  /**
   * Builds the assistant.
   *
   * @return The configured assistant.
   * @throws IllegalStateException If the model, turn strategy, or chat log strategy is unset.
   */
  public KigaliAssistant build() {
    checkReadyToBuild();
    return new KigaliAssistant(model, turnStrategy, chatLogStrategy, toolbelt, workspace);
  }

  /**
   * Fails unless the collaborators which cannot be defaulted have been set.
   *
   * @throws IllegalStateException If the model, turn strategy, or chat log strategy is unset.
   */
  private void checkReadyToBuild() {
    if (model == null) {
      throw new IllegalStateException("An AssistantModel must be set before building");
    }
    if (turnStrategy == null) {
      throw new IllegalStateException("An AssistantTurnStrategy must be set before building");
    }
    if (chatLogStrategy == null) {
      throw new IllegalStateException("A ChatLogStrategy must be set before building");
    }
  }
}
