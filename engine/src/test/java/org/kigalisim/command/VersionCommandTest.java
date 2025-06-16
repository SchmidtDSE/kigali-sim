/**
 * Unit tests for the VersionCommand class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for the VersionCommand class.
 */
public class VersionCommandTest {

  /**
   * Test that VersionCommand can be constructed.
   */
  @Test
  public void testVersionCommandConstruction() {
    VersionCommand command = new VersionCommand();
    assertNotNull(command, "VersionCommand should be constructable");
  }
}
