/**
 * Unit tests for the AssistantWorkspaceBuilder class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the AssistantWorkspaceBuilder class.
 */
public class AssistantWorkspaceBuilderTest {

  /**
   * Test that the default workspace supports QubecTalk files end to end.
   */
  @Test
  public void testDefaultQtaStrategy() {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder().build();
    workspace.createFile("simulation.qta", "start default\nend default");
    assertEquals(List.of("simulation.qta"), workspace.listFiles());
    assertEquals(List.of("default"), workspace.outlineFile("simulation.qta"));
  }

  /**
   * Test that an additional text file type can be registered.
   */
  @Test
  public void testAdditionalFileType() {
    AssistantWorkspace workspace = new AssistantWorkspaceBuilder()
        .addFileTypeStrategy(new PassThroughTextStrategy(".md"))
        .build();
    workspace.createFile("notes.md", "# Notes");
    assertEquals("# Notes", workspace.readFile("notes.md"));
  }

  /**
   * A minimal pass-through text strategy for an arbitrary extension.
   */
  private static final class PassThroughTextStrategy implements TextFileTypeStrategy {

    private final String extension;

    PassThroughTextStrategy(String extension) {
      this.extension = extension;
    }

    @Override
    public Set<String> getFileExtensions() {
      return Set.of(extension);
    }

    @Override
    public boolean getIsStringNative() {
      return true;
    }

    @Override
    public String interpret(String contents) {
      return contents;
    }

    @Override
    public String interpret(String contents, String section) {
      return contents;
    }

    @Override
    public List<String> outline(String contents) {
      return List.of();
    }
  }
}
