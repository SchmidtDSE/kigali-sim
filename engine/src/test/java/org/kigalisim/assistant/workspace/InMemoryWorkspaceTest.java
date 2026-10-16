/**
 * Unit tests for the InMemoryWorkspace class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the InMemoryWorkspace class.
 */
public class InMemoryWorkspaceTest {

  private static final String SAMPLE = String.join("\n",
      "start default",
      "  set sales to 1",
      "end default");

  private InMemoryWorkspace workspace() {
    return new InMemoryWorkspace(List.of(new QtaFileTypeStrategy()));
  }

  /**
   * Test creating and reading empty and populated files.
   */
  @Test
  public void testCreateAndRead() {
    InMemoryWorkspace workspace = workspace();
    assertEquals("empty.qta", workspace.createFile("empty.qta"));
    assertEquals("populated.qta", workspace.createFile("populated.qta", SAMPLE));
    assertEquals("", workspace.readFile("empty.qta"));
    assertEquals(SAMPLE, workspace.readFile("populated.qta"));
  }

  /**
   * Test creating a file from binary contents.
   */
  @Test
  public void testCreateBinary() {
    InMemoryWorkspace workspace = workspace();
    workspace.createFile("binary.qta", SAMPLE.getBytes(StandardCharsets.UTF_8));
    assertEquals(SAMPLE, workspace.readFile("binary.qta"));
  }

  /**
   * Test that a duplicate create and an unsupported extension throw.
   */
  @Test
  public void testCreateErrors() {
    InMemoryWorkspace workspace = workspace();
    workspace.createFile("a.qta");
    assertThrows(IllegalArgumentException.class, () -> workspace.createFile("a.qta"));
    assertThrows(IllegalArgumentException.class, () -> workspace.createFile("a.txt"));
  }

  /**
   * Test that missing operations throw NoSuchElementException.
   */
  @Test
  public void testMissingOperations() {
    InMemoryWorkspace workspace = workspace();
    workspace.createFile("a.qta", SAMPLE);
    assertThrows(NoSuchElementException.class, () -> workspace.readFile("missing.qta"));
    assertThrows(NoSuchElementException.class,
        () -> workspace.readFile("a.qta", "nope"));
    assertThrows(NoSuchElementException.class, () -> workspace.outlineFile("missing.qta"));
    assertThrows(NoSuchElementException.class,
        () -> workspace.writeFile("missing.qta", "x"));
    assertThrows(NoSuchElementException.class, () -> workspace.deleteFile("missing.qta"));
  }

  /**
   * Test section reads, outlining, and the wrapped missing-section message.
   */
  @Test
  public void testSectionAndOutline() {
    InMemoryWorkspace workspace = workspace();
    workspace.createFile("a.qta", SAMPLE);
    assertEquals(List.of("default"), workspace.outlineFile("a.qta"));
    assertEquals(SAMPLE, workspace.readFile("a.qta", "default"));
    NoSuchElementException error = assertThrows(NoSuchElementException.class,
        () -> workspace.readFile("a.qta", "nope"));
    assertTrue(error.getMessage().contains("a.qta"));
  }

  /**
   * Test that writes overwrite, deletes remove, and listing preserves order.
   */
  @Test
  public void testWriteDeleteAndList() {
    InMemoryWorkspace workspace = workspace();
    workspace.createFile("a.qta", "old");
    workspace.createFile("b.qta", "second");
    assertEquals(List.of("a.qta", "b.qta"), workspace.listFiles());

    workspace.writeFile("a.qta", "new");
    assertEquals("new", workspace.readFile("a.qta"));

    workspace.deleteFile("a.qta");
    assertEquals(List.of("b.qta"), workspace.listFiles());
    assertThrows(NoSuchElementException.class, () -> workspace.readFile("a.qta"));
  }

  /**
   * Test that the constructor rejects duplicate extensions.
   */
  @Test
  public void testDuplicateExtension() {
    AssistantFileTypeStrategy duplicate = mock(AssistantFileTypeStrategy.class);
    when(duplicate.getFileExtensions()).thenReturn(Set.of(".qta"));
    when(duplicate.getIsStringNative()).thenReturn(true);
    assertThrows(IllegalArgumentException.class,
        () -> new InMemoryWorkspace(List.of(new QtaFileTypeStrategy(), duplicate)));
  }

  /**
   * Test that a non-text strategy is reported as unsupported for reads.
   */
  @Test
  public void testBinaryStrategyReadUnsupported() {
    AssistantFileTypeStrategy binary = mock(AssistantFileTypeStrategy.class);
    when(binary.getFileExtensions()).thenReturn(Set.of(".pdf"));
    when(binary.getIsStringNative()).thenReturn(false);
    InMemoryWorkspace workspace = new InMemoryWorkspace(List.of(binary));
    workspace.createFile("doc.pdf", new byte[] {1, 2, 3});
    assertFalse(workspace.listFiles().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> workspace.readFile("doc.pdf"));
  }
}
