/**
 * Read-only view over the tools available to the assistant.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import java.util.Optional;

/**
 * A thin wrapper around a map of tool name to tool strategy.
 *
 * <p>This exposes only read operations. Addition is offered by the concrete
 * {@link MutableAssistantToolbelt}. The belt is populated by the assistant builder and does not
 * know about built-in tools itself.</p>
 */
public interface AssistantToolbelt {

  /**
   * Looks up a tool by name.
   *
   * @param name The tool name.
   * @return The tool or empty when no tool has that name.
   */
  Optional<AssistantToolStrategy> getTool(String name);

  /**
   * Iterates the registered tools.
   *
   * @return The registered tools in registration order.
   */
  Iterable<AssistantToolStrategy> getTools();
}
