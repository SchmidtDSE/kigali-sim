/**
 * Unit tests for the AiInfoCommand class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for the AiInfoCommand class.
 */
public class AiInfoCommandTest {

  /**
   * Test that AiInfoCommand can be constructed.
   */
  @Test
  public void testAiInfoCommandConstruction() {
    AiInfoCommand command = new AiInfoCommand();
    assertNotNull(command, "AiInfoCommand should be constructable");
  }
}
