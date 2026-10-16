/**
 * Unit tests for the MutableAssistantToolbelt class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the MutableAssistantToolbelt class.
 */
public class MutableAssistantToolbeltTest {

  /**
   * Test that tools can be registered and looked up by name.
   */
  @Test
  public void testAddAndLookup() {
    MutableAssistantToolbelt toolbelt = new MutableAssistantToolbelt();
    MockToolStrategy tool = new MockToolStrategy("echo", "ok");
    toolbelt.addTool(tool);
    assertSame(tool, toolbelt.getTool("echo").orElseThrow());
  }

  /**
   * Test that duplicate names are rejected.
   */
  @Test
  public void testDuplicateRejected() {
    MutableAssistantToolbelt toolbelt = new MutableAssistantToolbelt();
    toolbelt.addTool(new MockToolStrategy("echo", "ok"));
    assertThrows(
        IllegalArgumentException.class,
        () -> toolbelt.addTool(new MockToolStrategy("echo", "other")));
  }

  /**
   * Test that an unknown tool is reported as empty.
   */
  @Test
  public void testUnknownToolIsEmpty() {
    MutableAssistantToolbelt toolbelt = new MutableAssistantToolbelt();
    assertTrue(toolbelt.getTool("missing").isEmpty());
  }

  /**
   * Test that iteration preserves registration order.
   */
  @Test
  public void testIterationOrder() {
    MutableAssistantToolbelt toolbelt = new MutableAssistantToolbelt();
    toolbelt.addTool(new MockToolStrategy("first", "one"));
    toolbelt.addTool(new MockToolStrategy("second", "two"));

    List<String> names = new ArrayList<>();
    for (AssistantToolStrategy tool : toolbelt.getTools()) {
      names.add(tool.getName());
    }
    assertEquals(List.of("first", "second"), names);
  }
}
