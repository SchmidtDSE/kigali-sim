/**
 * Mutable toolbelt implementation.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A mutable collection of tool strategies keyed by name.
 *
 * <p>Duplicate names are rejected at insertion time so that lookup stays unambiguous.</p>
 */
public class MutableAssistantToolbelt implements AssistantToolbelt {

  private final Map<String, AssistantToolStrategy> tools = new LinkedHashMap<>();

  /**
   * Constructs an empty toolbelt.
   */
  public MutableAssistantToolbelt() {
  }

  /**
   * Adds a tool to the belt.
   *
   * @param tool The tool to add.
   * @throws IllegalArgumentException If a tool with the same name is already registered.
   */
  public void addTool(AssistantToolStrategy tool) {
    Objects.requireNonNull(tool, "tool");
    String name = Objects.requireNonNull(tool.getName(), "tool name");
    if (tools.containsKey(name)) {
      throw new IllegalArgumentException("A tool named '" + name + "' is already registered");
    } else {
      tools.put(name, tool);
    }
  }

  @Override
  public Optional<AssistantToolStrategy> getTool(String name) {
    return Optional.ofNullable(tools.get(name));
  }

  @Override
  public Iterable<AssistantToolStrategy> getTools() {
    return Collections.unmodifiableCollection(tools.values());
  }
}
