/**
 * Builder for assistant workspaces.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds an {@link AssistantWorkspace}.
 *
 * <p>The default in-memory workspace is preconfigured with the QubecTalk simulation file type so
 * that callers can use the built-in tools without further configuration. Additional file types can
 * be registered through {@link #addFileTypeStrategy(AssistantFileTypeStrategy)}.</p>
 */
public class AssistantWorkspaceBuilder {

  private final List<AssistantFileTypeStrategy> fileTypeStrategies = new ArrayList<>();

  /**
   * Constructs a builder with the default QubecTalk file type registered.
   */
  public AssistantWorkspaceBuilder() {
    addFileTypeStrategy(new QtaFileTypeStrategy());
  }

  /**
   * Registers a file type strategy.
   *
   * @param strategy The strategy to register.
   * @return This builder for chaining.
   */
  public AssistantWorkspaceBuilder addFileTypeStrategy(AssistantFileTypeStrategy strategy) {
    fileTypeStrategies.add(Objects.requireNonNull(strategy, "strategy"));
    return this;
  }

  /**
   * Builds the workspace.
   *
   * @return An in-memory workspace over the registered file types.
   * @throws IllegalArgumentException If two strategies claim the same extension.
   */
  public AssistantWorkspace build() {
    return new InMemoryWorkspace(fileTypeStrategies);
  }
}
